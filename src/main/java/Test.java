import ai.djl.*;

import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main (String args[]) throws MalformedModelException, IOException, UnsupportedAudioFileException {
        String file_path = ".\\src\\main\\resources\\mix.wav";
        File audio_file = new File(file_path);

        // Load the models
        OverlappingSpeech model = new OverlappingSpeech(".\\src\\main\\resources");

        // Load the audio data
        AudioInputStream audio = AudioSystem.getAudioInputStream(audio_file);

        // Separate the mixed speech
        model.separateOverlappingSpeech(audio);
    }
}
