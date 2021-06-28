import ai.djl.*;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main (String[] args) throws MalformedModelException, IOException, UnsupportedAudioFileException, TranslateException {
        String input_file_path = ".\\mix.wav";
        File input_file = new File(input_file_path);

        // Load the models
        OverlappingSpeech model = new OverlappingSpeech(".\\src\\main\\resources");

        // Load the audio data
        AudioInputStream audio = AudioSystem.getAudioInputStream(input_file);

        // Separate the mixed speech
        AudioInputStream[] separated_speakers = model.separateOverlappingSpeech(audio);

        // Save the output file
        String output_file_path0 = ".\\sep0.wav";
        File output_file0 = new File(output_file_path0);
        AudioSystem.write(separated_speakers[0], AudioFileFormat.Type.WAVE, output_file0);
        String output_file_path1 = ".\\sep1.wav";
        File output_file1 = new File(output_file_path1);
        AudioSystem.write(separated_speakers[1], AudioFileFormat.Type.WAVE, output_file1);
    }
}
