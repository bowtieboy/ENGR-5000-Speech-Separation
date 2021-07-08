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

    public static void main (String[] args) throws MalformedModelException, IOException, UnsupportedAudioFileException, TranslateException {

        // Used to record execution time
        Instant start = Instant.now();

        String input_file_path = ".\\vad_det_sep.wav";
        File input_file = new File(input_file_path);

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
        AudioFormat original_format = audio.getFormat();
        float fs = 16000f;

        // Apply pre-processing
        AudioInputStream speech_only = AudioPreprocessor.preprocessAudio(audio, vad_model);

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

        // Grab embeddings of non_mixed speech
        ArrayList<Float[][]> embeddings = emb_model.calculateBatchEmbeddings(non_mixed_speakers);

        // Separated the mixed speech
        ArrayList<AudioInputStream[]> separated_speakers = ovl_models.separateOverlappingSpeech(mixed_speakers);

        // Record this as the end of the execution time, since files wont be written in the final build
        Instant finish = Instant.now();
        long time_elapsed = Duration.between(start, finish).toMillis();
        System.out.printf("Time elapsed: %f", (float) time_elapsed / 1000f);

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
}
