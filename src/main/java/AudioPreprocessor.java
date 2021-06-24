import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AudioPreprocessor
{

    private static float input_fs = 44100;

    public static AudioInputStream downsampleAudio(AudioInputStream audio_input, float new_fs) throws IOException {
        AudioFormat current_format = audio_input.getFormat();

        AudioFormat desired_format = new AudioFormat(current_format.getEncoding(), new_fs,
                current_format.getSampleSizeInBits(), 1, current_format.getFrameSize(),
                new_fs, current_format.isBigEndian());

        if (AudioSystem.isConversionSupported(desired_format, current_format))
        {
            AudioInputStream downsampled_audio = AudioSystem.getAudioInputStream(desired_format, audio_input);
            return downsampled_audio;
        }

        else throw new IllegalStateException("Conversion not supported!");

    }

    public static List<Float[]> makeWindows(float[] audio, int sample_rate, float window_length)
    {
        // Define the shape of the windows
        int frame_per_window = (int) (sample_rate / window_length);
        int num_windows = (int) (audio.length / frame_per_window);
        ArrayList<Float[]> windows = new ArrayList<Float[]>();
        //
        // Float[][] windows = new Float[num_windows][frame_per_window];

        // Assign values to the windows
        for (int n = 0; n < num_windows; n++)
        {
            Float[] temp_arr = new Float[frame_per_window];
            for (int f = 0; f < frame_per_window; f++)
            {
                temp_arr[f] = (Float) audio[(n * frame_per_window) + f];
            }
            windows.add(temp_arr);
        }

        return windows;
    }

    /**
     * @param audio_input: AudioInputStream object that will be converted to floats
     * @return audio_floats: The byte values of audio_input converted to floats based on their encoding
     * @throws IOException
     */
    public static float[] convertToFloats(AudioInputStream audio_input, int length) throws IOException
    {
        // Grab the format of the input audio
        AudioFormat input_format = audio_input.getFormat();

        // Perform checks incase audio was downsampled
        int frame_length = -1;
        int bytes_available = -1;
        if ((int)audio_input.getFrameLength() <= 0)
        {
            frame_length = (int) ((input_format.getSampleRate() / input_fs) * length);
            bytes_available = frame_length * input_format.getFrameSize();
        }
        else
        {
            frame_length = (int)audio_input.getFrameLength();
            bytes_available = audio_input.available();
        }

        // Define float array that will be returned
        float audio_floats[] = new float[frame_length];

        // Create byte array from the audio input
        byte [] audio_bytes = new byte[bytes_available];

        // Loop through input and store the bytes
        for (int i = 0; i < audio_bytes.length; i++)
        {
            audio_input.read(audio_bytes);
        }

        // Convert the bytes to floats
        float max = 0.0f; // Grab the largest value for normalization later on
        for (int i = 0; i < audio_floats.length; i++)
        {
            short temp_short = 0;
            for (int j = 0; j < input_format.getFrameSize(); j++)
            {
                temp_short = (short) (temp_short & 0x00FF | (short) (audio_bytes[(i * input_format.getFrameSize()) + j] << (8 * j)));
            }
            audio_floats[i] = temp_short;
            max = Math.max(max, Math.abs(temp_short));
        }

        // Normalize the float array
        for (int i = 0; i < audio_floats.length; i++)
        {
            audio_floats[i] /= max;
        }


        return audio_floats;
    }

    // TODO: Implement this function
    /*public static AudioInputStream convertToInputStream(float[] audio_input)
    {

    }*/

    public static byte[] short2byte(short[] src)
    {
        int srcLength = src.length;
        byte[] dst = new byte[srcLength * 2];
        int j = 0;
        for (int i=0; i<srcLength; i++)
        {
            short x = src[i];
            j = i * 2;
            dst[j] = (byte) ((x >>> 0) & 0xff);
            dst[j + 1] = (byte) ((x >>> 8) & 0xff);
        }
        return dst;
    }

}
