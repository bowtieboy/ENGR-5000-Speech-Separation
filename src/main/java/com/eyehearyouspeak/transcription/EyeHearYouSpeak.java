package com.eyehearyouspeak.transcription;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.translate.TranslateException;
import android.content.Context;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.*;
import java.util.ArrayList;

public class EyeHearYouSpeak
{
    // Store the models that the class will use
    private VAD vad;
    private OverlappingSpeech ovl;
    private SpeakerEmbedder emb;
    private SpeakerIdentification iden;

    // Define standard sample rate that all the models will use
    float fs = 16000;

    public EyeHearYouSpeak(String model_path) throws MalformedModelException, IOException
    {
        // Create the device that the models will be loaded onto. If a GPU is available, use that
        Device device = Device.cpu();

        // Load the models into memory
        this.vad = new VAD(model_path, device);
        this.emb = new SpeakerEmbedder(model_path, device);
        this.ovl = new OverlappingSpeech(model_path, device);
        this.iden = new SpeakerIdentification();
    }

    // Getters
    public ArrayList<String> getSpeakerNames()
    {
        return this.iden.getNames();
    }

    // TODO: Finish this function
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

        // Grab the AudioInputStreams that do not contain overlapping speech
        ArrayList<AudioInputStream> non_mixed_speakers = this.ovl.getAudioFromTimeline(non_overlapping_speech_tl,
                non_overlapping_speech_tl.getFrames(), fs, standard_format);

        // TODO: Implement threading here to process both the mixed and non-mixed at the same time

        // If overlapping speech was detected
        if (overlapping_speech_tl.set.size() > 0)
        {
            // Grab the AudioInputStreams that contain overlapping speech segments
            ArrayList<AudioInputStream> mixed_speakers = this.ovl.getAudioFromTimeline(overlapping_speech_tl,
                    overlapping_speech_tl.getFrames(), fs, standard_format);
            // Separate out the overlapping speech
            ArrayList<AudioInputStream[]> separated_speakers = this.ovl.separateOverlappingSpeech(mixed_speakers);

            // Convert the ArrayList of AudioInputStream arrays into two separate ArrayLists
            ArrayList<AudioInputStream> separated_speaker_0 = AudioPreprocessor.separateArrayList(separated_speakers, 0);
            ArrayList<AudioInputStream> separated_speaker_1 = AudioPreprocessor.separateArrayList(separated_speakers, 1);

            // Fix the broken AudioInputStreams
            ArrayList<AudioInputStream> separated_speaker_0_fixed = new ArrayList<>();
            ArrayList<AudioInputStream> separated_speaker_1_fixed = new ArrayList<>();
            int length;
            for (int i = 0; i < separated_speaker_0.size(); i++)
            {
                length = (int) mixed_speakers.get(i).getFrameLength();
                separated_speaker_0_fixed.add(AudioPreprocessor.fixBrokenStream(separated_speaker_0.get(i), length));
                separated_speaker_1_fixed.add(AudioPreprocessor.fixBrokenStream(separated_speaker_1.get(i), length));
            }

            // Calculate embeddings for the mixed and non-mixed segments of speech
            ArrayList<Float[][]> speaker_0_embeddings = this.emb.calculateBatchEmbeddings(separated_speaker_0_fixed);
            ArrayList<Float[][]> speaker_1_embeddings = this.emb.calculateBatchEmbeddings(separated_speaker_1_fixed);

            // Identify the speakers for each embedding
            ArrayList<String> speaker_0_names = new ArrayList<>();
            ArrayList<String> speaker_1_names = new ArrayList<>();
            for (int i = 0; i < speaker_0_embeddings.size(); i++)
            {
                // Grab the speakers for each embedding
                ArrayList<String> current_names_0 = this.iden.identifySpeakers(speaker_0_embeddings.get(i));
                ArrayList<String> current_names_1 = this.iden.identifySpeakers(speaker_1_embeddings.get(i));

                // Add the names for the embeddings to the non_mixed_names list
                for (i = 0; i < current_names_0.size(); i++)
                {
                    speaker_0_names.add(current_names_0.get(i));
                    speaker_1_names.add(current_names_1.get(i));
                }
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

        /*
          At this point, the names of each speaker should be identified for both the overlapping speech segments (if
          any existed within the AudioInputStream) and the non-overlapping speech. The next step is to get both the
          STT values, as well as the times associated with each word. After this is done, combing the word timings, the
          timeline segments, and the embedding windows should provide: who spoke, what was said, and when it was said.
         */

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
