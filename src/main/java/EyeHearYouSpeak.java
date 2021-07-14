import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.file.Path;
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
        Device device;
        if (Device.getGpuCount() > 0)
        {
            device = Device.gpu();
        } else device = Device.cpu();
        // FIXME: Bug where data isn't being loaded onto the GPU for calculations, but it needs to be. Use CPU for now
        device = Device.cpu();

        // Load the models into memory
        this.vad = new VAD(model_path, device);
        this.emb = new SpeakerEmbedder(model_path, device);
        this.ovl = new OverlappingSpeech(model_path, device);
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
        // TODO: Check case where no overlapping speech occurs in the clip
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

            // Calculate embeddings for the mixed and non-mixed segments of speech
            ArrayList<Float[][]> speaker_0_embeddings = this.emb.calculateBatchEmbeddings(separated_speaker_0);
            ArrayList<Float[][]> speaker_1_embeddings = this.emb.calculateBatchEmbeddings(separated_speaker_1);

            // Identify the speakers for each embedding
            ArrayList<String> speaker_0_names = new ArrayList<>();
            ArrayList<String> speaker_1_names = new ArrayList<>();
            for (int i = 0; i < speaker_0_embeddings.size(); i++)
            {
                // Grab the speakers for each embedding
                ArrayList<String> current_names_0 = this.iden.identifySpeakers(speaker_0_embeddings.get(i));
                ArrayList<String> current_names_1 = this.iden.identifySpeakers(speaker_1_embeddings.get(i));

                // Add the names for the embeddings to the non_mixed_names list
                for (String name: current_names_0)
                {
                    speaker_0_names.add(name);
                    speaker_1_names.add(name);
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

}
