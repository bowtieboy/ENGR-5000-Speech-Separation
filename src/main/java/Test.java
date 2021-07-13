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

        String input_speaker_path = ".\\matt.wav";
        File input_speaker = new File(input_speaker_path);

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
        AudioInputStream speaker = AudioSystem.getAudioInputStream(input_speaker);

        // Apply pre-processing
        AudioInputStream speech_only = AudioPreprocessor.preprocessAudio(audio, vad_model);
        AudioInputStream speaker_only = AudioPreprocessor.preprocessAudio(speaker, vad_model);

        // Test the overlapping speech separation
        //testSeparationPath(audio, ovl_models);

        // Test the speaker embedding model
        testSpeakerEmbedding(speaker_only, speech_only, emb_model);

    }

    private static void testSeparationPath(AudioInputStream speech_only, OverlappingSpeech ovl_models) throws
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
        Timeline non_mixed = ovl_models.invertTimeline(ovl_timeline);
        ArrayList<AudioInputStream> non_mixed_speakers = ovl_models.getAudioFromTimeline(non_mixed,
                ovl_timeline.getFrames(), fs, standard_format);

        // Separated the mixed speech
        ArrayList<AudioInputStream[]> separated_speakers = ovl_models.separateOverlappingSpeech(mixed_speakers);

        // Record this as the end of the execution time, since files wont be written in the final build
        Instant finish = Instant.now();
        long time_elapsed = Duration.between(start, finish).toMillis();
        System.out.printf("Time elapsed during ovl model path: %f", (float) time_elapsed / 1000f);

        // Save the output files
        String output_file_path0 = ".\\sep0.wav";
        File output_file0 = new File(output_file_path0);
        AudioSystem.write(separated_speakers.get(0)[0], AudioFileFormat.Type.WAVE, output_file0);
        String output_file_path1 = ".\\sep1.wav";
        File output_file1 = new File(output_file_path1);
        AudioSystem.write(separated_speakers.get(0)[1], AudioFileFormat.Type.WAVE, output_file1);
        String output_file_path2 = ".\\mixed.wav";
        File output_file2 = new File(output_file_path2);
        mixed_speakers.get(0).reset(); // Reset the byte stream
        AudioSystem.write(mixed_speakers.get(0), AudioFileFormat.Type.WAVE, output_file2);
    }

    private static void testSpeakerEmbedding(AudioInputStream speaker_audio, AudioInputStream audio, SpeakerEmbedder embedder) throws
                                                                                        TranslateException, IOException
    {
        // Turn the audio into a series of embeddings
        ArrayList<AudioInputStream> audio_list = new ArrayList<>();
        audio_list.add(speaker_audio);
        ArrayList embeddings = embedder.calculateBatchEmbeddings(audio_list);

        // Create speaker out of the embeddings
        Speaker new_speaker = new Speaker("Matt", embeddings);

        // Create list out of the single speaker
        ArrayList<Speaker> speakers = new ArrayList<>();
        speakers.add(new_speaker);

        // Create SpeakerIdentification class
        SpeakerIdentification identifier = new SpeakerIdentification(speakers);

        //
    }
}
