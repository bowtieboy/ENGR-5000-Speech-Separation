package com.eyehearyouspeak.transcription;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.translate.TranslateException;
import android.content.Context;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class EyeHearYouSpeak
{
    // Store the models that the class will use
    private VAD vad;
    private OverlappingSpeech ovl;
    private SpeakerEmbedder emb;
    private SpeakerIdentification iden;
    private SpeechTranscriber stt;

    // Define standard sample rate that all the models will use
    float fs = 16000;

    public EyeHearYouSpeak(String model_path, int num_speakers) throws MalformedModelException, IOException
    {
        // Create the device that the models will be loaded onto. If a GPU is available, use that
        Device device = Device.cpu();

        // Load the models into memory
        this.vad = new VAD(model_path, device);
        this.emb = new SpeakerEmbedder(model_path, device);
        this.ovl = new OverlappingSpeech(model_path, device);
        this.iden = new SpeakerIdentification();
        this.stt = new SpeechTranscriber(".\\stunning-strand-301922-21659aac8e8b.json", num_speakers);
    }

    // Getters
    public ArrayList<String> getSpeakerNames()
    {
        return this.iden.getNames();
    }

    // TODO: Implement STT into the function
    public ArrayList<String> annotateAudio(AudioInputStream audio) throws TranslateException, IOException
    {
        // Create variable that will contain the final speech array
        ArrayList<String> captions = new ArrayList<>();

        // Create the format for audio that will be used
        AudioFormat original_format = audio.getFormat();
        AudioFormat standard_format = new AudioFormat(original_format.getEncoding(), this.fs,
                original_format.getSampleSizeInBits(), 1, original_format.getFrameSize(),
                this.fs, original_format.isBigEndian());

        // Preprocess the audio stream
        AudioInputStream pre_processed = AudioPreprocessor.preprocessAudio(audio, this.vad);

        // If no speech was detected in the audio, don't waste time processing the stream
        if (pre_processed == null) return null;

        // Detect any overlapping speech within the audio
        Timeline overlapping_speech_tl = this.ovl.detectOverlappingSpeech(pre_processed);
        Timeline non_overlapping_speech_tl = this.ovl.invertTimeline(overlapping_speech_tl);

        // Grab the AudioInputStreams that do not contain overlapping speech. If overlapping speech exists, use timeline
        // to get the data. Otherwise, use the original data
        if (!(overlapping_speech_tl.set.size() > 0))
        {
            // Fix the non_overlapping timeline since it will have an empty set if no ovl occurred
            non_overlapping_speech_tl = new Timeline(pre_processed);
        }
        ArrayList<AudioInputStream> non_mixed_speakers = this.ovl.getAudioFromTimeline(non_overlapping_speech_tl,
                non_overlapping_speech_tl.getFrames(), fs, standard_format);


        // TODO: Implement threading here to process both the mixed and non-mixed at the same time

        // If overlapping speech was detected
        ArrayList<String[]> annotated_mixed = new ArrayList<>();
        if (overlapping_speech_tl.set.size() > 0)
        {
            // Grab the AudioInputStreams that contain overlapping speech segments
            ArrayList<AudioInputStream> mixed_speakers = this.ovl.getAudioFromTimeline(overlapping_speech_tl,
                    overlapping_speech_tl.getFrames(), fs, standard_format);
            // Separate out the overlapping speech
            ArrayList<AudioInputStream[]> separated_speakers = this.ovl.separateOverlappingSpeech(mixed_speakers);

            // Fix the broken AudioInputStreams
            ArrayList<AudioInputStream[]> separated_speakers_fixed = new ArrayList<>();
            AudioInputStream[] temp_arr = new AudioInputStream[2];
            int length;
            for (int i = 0; i < separated_speakers.size(); i++)
            {
                length = (int) mixed_speakers.get(i).getFrameLength();
                temp_arr[0] = AudioPreprocessor.fixBrokenStream(separated_speakers.get(i)[0], length);
                temp_arr[1] = AudioPreprocessor.fixBrokenStream(separated_speakers.get(i)[1], length);
                separated_speakers_fixed.add(temp_arr);
            }

            // Calculate embeddings for the mixed segments of speech
            ArrayList<Float[][]> mixed_embeddings = new ArrayList<>();
            for (int i = 0; i < separated_speakers_fixed.size(); i++)
            {
                mixed_embeddings.add(this.emb.calculateEmbeddings(separated_speakers_fixed.get(i)[0]));
                mixed_embeddings.add(this.emb.calculateEmbeddings(separated_speakers_fixed.get(i)[1]));
            }

            // Identify the speakers for each embedding
            ArrayList<ArrayList<String>> speaker_names = new ArrayList<>();
            for (Float[][] mat: mixed_embeddings)
            {
                speaker_names.add(this.iden.identifySpeakers(mat));
            }

            // Since it is known that the overlapping speech segments only contain one speaker, convert the lists to
            // single strings
            ArrayList<String> unique_speaker_names = new ArrayList<>();
            for (ArrayList<String> candidates: speaker_names)
            {
                unique_speaker_names.add(this.iden.getMostLikely(candidates, 1)[0]);
            }

            // Transcribe the separated speech segments
            ArrayList<String> channel_0, channel_1;
            String[] transcribed_channels = new String[2];
            for (int i = 0; i < separated_speakers_fixed.size(); i++)
            {
                channel_0 = this.stt.transcribeAudio(separated_speakers_fixed.get(i)[0]);
                channel_1 = this.stt.transcribeAudio(separated_speakers_fixed.get(i)[1]);

                transcribed_channels[0] = unique_speaker_names.get(i * separated_speakers_fixed.size()) + ": " + channel_0.get(0);
                transcribed_channels[1] = unique_speaker_names.get(i * separated_speakers_fixed.size() + 1) + ": " + channel_1.get(0);

                annotated_mixed.add(transcribed_channels);
            }

        }

        // Calculate embeddings for the non-overlapping speech
        ArrayList<Float[][]> non_mixed_embeddings = this.emb.calculateBatchEmbeddings(non_mixed_speakers);

        // Identify the speakers for each embedding
        ArrayList<String> non_mixed_names = new ArrayList<>();
        for (Float[][] emb: non_mixed_embeddings)
        {
            // Grab the speakers for each embedding
            ArrayList<String> current_names = this.iden.identifySpeakers(emb);

            // Add the names for the embeddings to the non_mixed_names list
            non_mixed_names.addAll(current_names);
        }

        // Annotate the sections of non-overlapping speech
        ArrayList<String> annotated_non_mixed = new ArrayList<>();
        int num_speakers;
        for (AudioInputStream non_mixed_audio: non_mixed_speakers)
        {
            ArrayList<String> diarized_speech = this.stt.transcribeAudio(non_mixed_audio);

            // Use the number of segments in the list to assign speakers to text
            num_speakers = diarized_speech.size();

            String[] most_likely_speakers = this.iden.getMostLikely(non_mixed_names, num_speakers);

            // Loop through the diarized speech and assign speakers
            for (int i = 0; i < most_likely_speakers.length; i++)
            {
                annotated_non_mixed.add(most_likely_speakers[i] + ": " + diarized_speech.get(i));
            }
        }

        // If no overlapping speech was detected, assign captions based on diarization
        if (!(overlapping_speech_tl.set.size() > 0))
        {
            captions = annotated_non_mixed;
        }
        else
        {
            // Use the timelines to determine the order of speech (overlapping vs non-overlapping)
            for (int i = 0; i < non_overlapping_speech_tl.set.size(); i++)
            {
                // Make sure no out of bounds exceptions occur
                if (overlapping_speech_tl.set.size() > i)
                {
                    // If non-ovl segment starts before ovl segment
                    if (non_overlapping_speech_tl.set.get(i).getStart() < overlapping_speech_tl.set.get(i).getStart())
                    {
                        captions.add(annotated_non_mixed.get(i));
                        captions.add(annotated_mixed.get(i)[0]);
                        captions.add(annotated_mixed.get(i)[1]);
                    }
                    else
                    {
                        captions.add(annotated_mixed.get(i)[0]);
                        captions.add(annotated_mixed.get(i)[1]);
                        captions.add(annotated_non_mixed.get(i));
                    }
                }
                else captions.add(annotated_non_mixed.get(i));
            }
            // If overlapping speech has more segments than non-overlapping, add the last segment to the captions
            if (overlapping_speech_tl.set.size() > non_overlapping_speech_tl.set.size())
            {
                captions.add(annotated_mixed.get(annotated_mixed.size() - 1)[0]);
                captions.add(annotated_mixed.get(annotated_mixed.size() - 1)[1]);
            }
        }

        return captions;
    }

    /**
     * Adds a new speaker into the system from the given AudioInputStream
     * @param audio: AudioInputStream containing speech from the new speaker and no one else. Must be > 1 min
     * @param name: The name of the new speaker
     * @return: Either the total amount of speakers in the system, or -1 if there was not enough audio data
     * @throws TranslateException: no idea
     * @throws IOException: no idea
     */
    public int addNewSpeaker(AudioInputStream audio, String name) throws TranslateException, IOException
    {
        // Pre-process the audio
        AudioInputStream processed_audio = AudioPreprocessor.preprocessAudio(audio, this.vad);

        // Ensure the processed audio includes at least 1-min of audio
        if (processed_audio.getFrameLength() / processed_audio.getFormat().getFrameRate() < 50)
        {
            // Return -1 to notify that not enough speech is in the input stream
            return -1;
        }

        // Turn the audio into a series of embeddings
        ArrayList<AudioInputStream> audio_list = new ArrayList<>();
        audio_list.add(processed_audio);
        ArrayList<Float[][]> embeddings = this.emb.calculateBatchEmbeddings(audio_list);

        // Create speaker out of the embeddings
        Speaker new_speaker = new Speaker(name, embeddings);

        // Create list out of the speakers
        ArrayList<Speaker> speakers = new ArrayList<>();
        speakers.add(new_speaker);

        // Create SpeakerIdentification class
        for (Speaker s: speakers)
        {
            this.iden.addNewSpeaker(s);
        }

        // Return the total amount of speakers in the system
        return this.iden.getClusters();
    }

    /**
     * Saves the speakers that have been recorded by the system
     * @param context: Context of the application (will most likely be activity or application)
     * @throws IOException: no idea
     */
    public void saveSpeakers(Context context) throws IOException
    {
        FileOutputStream fos = context.openFileOutput("./speakers", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(this.iden);
        os.close();
        fos.close();
    }

    /**
     * Loads the set of speakers if they exist already
     * @param context: Context of the application (will most likely be activity or application)
     * @throws IOException: no idea
     * @throws ClassNotFoundException: no idea
     */
    public void loadSpeakers(Context context) throws IOException, ClassNotFoundException
    {
        FileInputStream fis = context.openFileInput("./speakers");
        ObjectInputStream is = new ObjectInputStream(fis);
        this.iden = (SpeakerIdentification) is.readObject();
        is.close();
        fis.close();
    }

}
