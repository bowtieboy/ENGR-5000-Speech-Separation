import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioPreprocessor
{
    // TODO: Finish this function
    public static AudioInputStream downsampleAudio(AudioInputStream audio_input, int new_fs) throws IOException
    {
        // Make the audio format easier to access
        AudioFormat input_format = audio_input.getFormat();

        // Ensure the new sample rate is lower than the current
        assert new_fs < input_format.getSampleRate();

        // Convert the audio frames to an array of floats for manipulation
        double frames[] = convertToDoubles(audio_input);

        // Depending on the desired sample rate, apply the correct filter and amount to skip
        switch (new_fs)
        {
            case 16000:
                frames = DigitalFilters.applyBandpassFilter(frames);
                break;
            case 8000:
                frames = DigitalFilters.applyLowpassFilter(frames);
                break;
            default:
                assert false; // Break for now, will need to handle this later
                break;
        }

        // Create the new resampled array


        // Return this to prevent error for now
        return audio_input;
    }

    // TODO: Implement this function
    public static double[][] makeWindows(double[] audio, int sample_rate, double window_length)
    {
        // Put here to prevent errors when building. Will need to calculate amount of windows needed first.
        return new double[0][0];
    }

    /**
     *
     * @param audio_input: AudioInputStream object that will be converted to doubles
     * @return audio_doubles: The byte values of audio_input converted to doubles based on their encoding
     * @throws IOException
     */
    public static double[] convertToDoubles(AudioInputStream audio_input) throws IOException
    {
        // Grab the format of the input audio
        AudioFormat input_format = audio_input.getFormat();

        // Define double array that will be returned
        double audio_doubles[] = new double[(int)audio_input.getFrameLength()];

        // Create byte array from the audio input
        byte [] audio_bytes = new byte[audio_input.available()];

        // Loop through input and store the bytes
        for (int i = 0; i < audio_bytes.length; i++)
        {
            audio_input.read(audio_bytes);
        }

        // Convert the bytes to doubles
        double max = 0.0; // Grab the largest value for normalization later on
        for (int i = 0; i < audio_doubles.length; i++)
        {
            short temp_short = 0;
            for (int j = 0; j < input_format.getFrameSize(); j++)
            {
                temp_short = (short) (temp_short & 0x00FF | (short) (audio_bytes[(i * input_format.getFrameSize()) + j] << (8 * j)));
            }
            audio_doubles[i] = temp_short;
            max = Math.max(max, Math.abs(temp_short));
        }

        // Normalize the double array
        for (int i = 0; i < audio_doubles.length; i++)
        {
            audio_doubles[i] /= max;
        }


        return audio_doubles;
    }

    // TODO: Implement this function
    /*public static AudioInputStream convertToInputStream(double[] audio_input)
    {

    }*/
}
