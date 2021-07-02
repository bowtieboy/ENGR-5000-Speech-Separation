import ai.djl.*;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main (String[] args) throws MalformedModelException, IOException, UnsupportedAudioFileException, TranslateException {
        String input_file_path = ".\\detection_sep_test.wav";
        File input_file = new File(input_file_path);

        // Load the models
        OverlappingSpeech model = new OverlappingSpeech(".\\src\\main\\resources");

        // Load the audio data
        AudioInputStream audio = AudioSystem.getAudioInputStream(input_file);

        // Locate the mixed speech
        AudioInputStream mixed_speakers = model.detectOverlappingSpeech(audio);
        ArrayList<AudioInputStream> copy = AudioPreprocessor.copyStream(mixed_speakers, (int) Math.floor(mixed_speakers.available() / 2));

        // Separated the mixed speech
        AudioInputStream[] separated_speakers = model.separateOverlappingSpeech(copy.get(0));

        // Save the output files
        String output_file_path0 = ".\\sep0.wav";
        File output_file0 = new File(output_file_path0);
        AudioSystem.write(separated_speakers[0], AudioFileFormat.Type.WAVE, output_file0);
        String output_file_path1 = ".\\sep1.wav";
        File output_file1 = new File(output_file_path1);
        AudioSystem.write(separated_speakers[1], AudioFileFormat.Type.WAVE, output_file1);
        String output_file_path2 = ".\\mixed.wav";
        File output_file2 = new File(output_file_path2);
        AudioSystem.write(copy.get(1), AudioFileFormat.Type.WAVE, output_file2);
    }
}
