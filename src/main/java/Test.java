import ai.djl.*;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main (String[] args) throws MalformedModelException, IOException, UnsupportedAudioFileException, TranslateException {
        String input_file_path = ".\\double_overlap.wav";
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
        OverlappingSpeech model = new OverlappingSpeech(".\\src\\main\\resources", device);

        // Load the audio data
        AudioInputStream audio = AudioSystem.getAudioInputStream(input_file);

        // Locate the mixed speech
        ArrayList<AudioInputStream> mixed_speakers = model.detectOverlappingSpeech(audio);
        // Create a copy of the streams to write to a file for debugging. This step will not be needed in the final
        // build
        ArrayList<ArrayList<AudioInputStream>> copy = AudioPreprocessor.copyStreams(mixed_speakers);

        // Separated the mixed speech
        ArrayList<AudioInputStream[]> separated_speakers = model.separateOverlappingSpeech(copy.get(0));

        // Save the output files
        String output_file_path0 = ".\\sep0.wav";
        File output_file0 = new File(output_file_path0);
        AudioSystem.write(separated_speakers.get(0)[0], AudioFileFormat.Type.WAVE, output_file0);
        String output_file_path1 = ".\\sep1.wav";
        File output_file1 = new File(output_file_path1);
        AudioSystem.write(separated_speakers.get(0)[1], AudioFileFormat.Type.WAVE, output_file1);
        String output_file_path2 = ".\\mixed.wav";
        File output_file2 = new File(output_file_path2);
        AudioSystem.write(copy.get(1).get(0), AudioFileFormat.Type.WAVE, output_file2);
    }
}
