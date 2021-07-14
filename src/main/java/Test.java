import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;

import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;

public class Test {

    public static void main (String[] args) throws
                                MalformedModelException, IOException, UnsupportedAudioFileException, TranslateException
    {

        // Get the needed files
        String input_file_path = ".\\vad_det_sep.wav";
        File input_file = new File(input_file_path);

        String input_speaker_path0 = ".\\matt.wav";
        File input_speaker0 = new File(input_speaker_path0);
        String input_speaker_path1 = ".\\zoe.wav";
        File input_speaker1 = new File(input_speaker_path1);

        // If a gpu is available, use that. Otherwise use the cpu
        Device device;
        if (Device.getGpuCount() > 0)
        {
            device = Device.gpu();
        } else device = Device.cpu();

        // FIXME: Bug where data isn't being loaded onto the GPU for calculations, but it needs to be. Use CPU for now
        device = Device.cpu();

        // Load the models
        String model_path = ".\\src\\main\\resources";
        VAD vad_model = new VAD(model_path, device);
        SpeakerEmbedder emb_model = new SpeakerEmbedder(model_path, device);
        OverlappingSpeech ovl_models = new OverlappingSpeech(model_path, device);

        // Load the audio data
        AudioInputStream audio = AudioSystem.getAudioInputStream(input_file);
        AudioInputStream speaker0 = AudioSystem.getAudioInputStream(input_speaker0);
        AudioInputStream speaker1 = AudioSystem.getAudioInputStream(input_speaker1);

        // Apply pre-processing
        AudioInputStream speech_only = AudioPreprocessor.preprocessAudio(audio, vad_model);
        AudioInputStream speaker_processed0 = AudioPreprocessor.preprocessAudio(speaker0, vad_model);
        AudioInputStream speaker_processed1 = AudioPreprocessor.preprocessAudio(speaker1, vad_model);

        // Test the speaker embedding model
        testSpeakerEmbedding(speaker_processed0, speaker_processed1, speech_only, emb_model, ovl_models);

    }

    private static AudioInputStream testSeparationPath(AudioInputStream speech_only, OverlappingSpeech ovl_models) throws
                                                                                        TranslateException, IOException
    {
        // Used to record execution time
        Instant start = Instant.now();

        AudioFormat original_format = speech_only.getFormat();
        float fs = 16000f;

        // Locate the mixed speech
        Timeline ovl_timeline = ovl_models.detectOverlappingSpeech(speech_only);
        AudioFormat standard_format = new AudioFormat(original_format.getEncoding(), fs,
                original_format.getSampleSizeInBits(), 1, original_format.getFrameSize(),
                fs, original_format.isBigEndian());
        ArrayList<AudioInputStream> mixed_speakers = ovl_models.getAudioFromTimeline(ovl_timeline,
                ovl_timeline.getFrames(), fs, standard_format);

        // Separate the non-mixed speech
        // FIXME: Returning an empty set, when it should have values
        Timeline non_mixed = ovl_models.invertTimeline(ovl_timeline);
        ArrayList<AudioInputStream> non_mixed_speakers = ovl_models.getAudioFromTimeline(non_mixed,
                ovl_timeline.getFrames(), fs, standard_format);

        // Separated the mixed speech
        ArrayList<AudioInputStream[]> separated_speakers = ovl_models.separateOverlappingSpeech(mixed_speakers);

        // Record this as the end of the execution time, since files wont be written in the final build
        Instant finish = Instant.now();
        long time_elapsed = Duration.between(start, finish).toMillis();
        System.out.printf("Time elapsed during ovl model path: %f\n", (float) time_elapsed / 1000f);

        // Save the output files
//        String output_file_path0 = ".\\sep0.wav";
//        File output_file0 = new File(output_file_path0);
//        separated_speakers.get(0)[0].reset();
//        AudioSystem.write(separated_speakers.get(0)[0], AudioFileFormat.Type.WAVE, output_file0);
//        String output_file_path1 = ".\\sep1.wav";
//        File output_file1 = new File(output_file_path1);
//        separated_speakers.get(0)[1].reset();
//        AudioSystem.write(separated_speakers.get(0)[1], AudioFileFormat.Type.WAVE, output_file1);
//        String output_file_path2 = ".\\mixed.wav";
//        File output_file2 = new File(output_file_path2);
//        mixed_speakers.get(0).reset(); // Reset the byte stream
//        AudioSystem.write(mixed_speakers.get(0), AudioFileFormat.Type.WAVE, output_file2);

        // Return a separated stream for testing
        AudioInputStream fixed_stream = AudioPreprocessor.fixBrokenStream(separated_speakers.get(0)[0],
                                                                        (int) mixed_speakers.get(0).getFrameLength());
        return fixed_stream;
    }

    private static void testSpeakerEmbedding(AudioInputStream speaker_audio0, AudioInputStream speaker_audio1, AudioInputStream audio,
                                                        SpeakerEmbedder embedder, OverlappingSpeech ovl_models) throws
                                                                                        TranslateException, IOException
    {
        // Turn the audio into a series of embeddings
        ArrayList<AudioInputStream> audio_list0 = new ArrayList<>();
        audio_list0.add(speaker_audio0);
        ArrayList embeddings0 = embedder.calculateBatchEmbeddings(audio_list0);
        ArrayList<AudioInputStream> audio_list1 = new ArrayList<>();
        audio_list1.add(speaker_audio1);
        ArrayList embeddings1 = embedder.calculateBatchEmbeddings(audio_list1);

        // Create speaker out of the embeddings
        Speaker new_speaker0 = new Speaker("Matt", embeddings0);
        Speaker new_speaker1 = new Speaker("Zoe", embeddings1);

        // Create list out of the speakers
        ArrayList<Speaker> speakers = new ArrayList<>();
        speakers.add(new_speaker0);
        speakers.add(new_speaker1);

        // Create SpeakerIdentification class
        SpeakerIdentification identifier = new SpeakerIdentification(speakers);

        // Get the isolated speech
        ArrayList<AudioInputStream> test_list = new ArrayList<>();
        test_list.add(testSeparationPath(audio, ovl_models));

        // Get list of test embeddings
        ArrayList<Float[][]> test_embeddings = embedder.calculateBatchEmbeddings(test_list);

        // Check to make sure the classifier is working correctly
        identifier.identifySpeakers(test_embeddings.get(0), 0.5f);
    }

    private static void saveAudio(AudioInputStream audio, String file_name) throws IOException
    {
        File output_file = new File(file_name);
        audio.reset();
        AudioSystem.write(audio, AudioFileFormat.Type.WAVE, output_file);
    }
}
