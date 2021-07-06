import ai.djl.translate.TranslateException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

public class AudioPreprocessor
{
    /**
     * @param audio_input: AudioInputStream that will be downsampled
     * @param new_fs: The desired sample rate (frames/sec)
     * @return: An AudioInputStream with sample rate (frames/sec) of new_fs
     */
    public static AudioInputStream resampleAudio(AudioInputStream audio_input, float new_fs)
    {
        AudioFormat current_format = audio_input.getFormat();

        AudioFormat desired_format = new AudioFormat(current_format.getEncoding(), new_fs,
                current_format.getSampleSizeInBits(), 1, current_format.getFrameSize(),
                new_fs, current_format.isBigEndian());

        if (AudioSystem.isConversionSupported(desired_format, current_format))
        {
            return AudioSystem.getAudioInputStream(desired_format, audio_input);
        }

        else throw new IllegalStateException("Conversion not supported!");

    }

    /**
     * @param audio: audio values expressed as array of floats
     * @param sample_rate: rate at which the audio array was sampled (frames/sec)
     * @param window_size: How long (in seconds) the desired windows should be
     * @param window_ovl: How long (in seconds) each window should overlap the preceeding
     * @return: List of floats that represent the windows of audios (with length of window_length)
     */
    public static ArrayList<Float[]> makeWindows(float[] audio, float sample_rate, float window_size, float window_ovl)
    {
        // Make sure there is less overlap than the size of the actual window
        assert window_ovl < window_size;
        // Define the shape of the windows
        int window_size_samples = (int) (window_size * sample_rate);
        int window_overlap_samples = (int) (window_ovl * sample_rate);
        int window_delta_samples = window_size_samples - window_overlap_samples;
        int window_amount = (int) Math.floor(audio.length / (float) window_delta_samples) - 1;

        // Create list of windows that will be returned
        ArrayList<Float[]> windows = new ArrayList<>();
        int idx;
        boolean stop_windows = false;
        for (int i = 0; i < window_amount; i++)
        {
            // Clear temp array
            Float[] temp = new Float[window_size_samples];
            // Copy data to temp array
            for (int j = 0; j < window_size_samples; j++)
            {
                // Check to make sure data exists
                idx = j + (i * window_size_samples) - (i * window_overlap_samples);
                if (idx < audio.length)
                {
                    temp[j] = audio[idx];
                }
                else
                {
                    temp[j] = 0f;
                    stop_windows = true;
                }

            }

            // Add temp array to window list
            windows.add(temp);
            if(stop_windows) break;
        }

        return windows;
    }

    /**
     * @param audio_input: AudioInputStream object that will be converted to floats
     * @return audio_floats: The byte values of audio_input converted to floats based on their encoding
     * @throws IOException: Thrown if AudioInputStream is null
     */
    public static float[] convertToFloats(AudioInputStream audio_input, int original_length,
                                          float input_fs, boolean padding) throws IOException
    {
        // Grab the format of the input audio
        AudioFormat input_format = audio_input.getFormat();

        // Perform checks in case audio was downsampled (bug where frame length shows zero)
        int frame_length;
        int bytes_available;
        if ((int)audio_input.getFrameLength() <= 0)
        {
            frame_length = (int) ((input_format.getSampleRate() / input_fs) * original_length);
            bytes_available = frame_length * input_format.getFrameSize();
        }
        else
        {
            frame_length = (int)audio_input.getFrameLength();
            bytes_available = audio_input.available();
        }

        // Create byte array from the audio input
        byte [] audio_bytes = new byte[bytes_available];

        // Loop through input and store the bytes
        for (int i = 0; i < audio_bytes.length; i++)
        {
            //noinspection ResultOfMethodCallIgnored
            audio_input.read(audio_bytes);
        }

        // Convert the bytes to floats
        // Pad the end of the float array with zeros to the nearest multiple of the sample rate
        int padding_needed = 0;
        if (padding)
        {
            padding_needed = (int) (input_format.getSampleRate() - (frame_length % input_format.getSampleRate()));
        }
        // Define float array that will be returned
        float[] audio_floats = new float[frame_length + padding_needed];
        for (int i = 0; i < audio_floats.length - padding_needed; i++)
        {
            short temp_short = 0;
            for (int j = 0; j < input_format.getFrameSize(); j++)
            {
                temp_short = (short) (temp_short & 0x00FF | (short) (audio_bytes[(i * input_format.getFrameSize()) + j] << (8 * j)));
            }
            audio_floats[i] = temp_short;
        }

        for (int i = audio_floats.length - padding_needed; i < audio_floats.length; i++)
        {
            audio_floats[i] = 0f;
        }

        // Normalize the float array
        float max = MatrixOperations.getMaxElement(audio_floats);
        for (int i = 0; i < audio_floats.length; i++)
        {
            audio_floats[i] /= max;
        }


        return audio_floats;
    }

    /**
     * @param audio_input: float array that contains the audio information that will be encoded
     * @param max_val: The maximum value of the float array. Used to maximize the volume
     * @param desired_format: Desired audio format of the input stream. Encoding MUST BE PCM_Signed 16bit
     * @return: An AudioInputStream object
     */
    public static AudioInputStream convertToInputStream(float[] audio_input, float max_val, AudioFormat desired_format)
    {
        // Convert the float arrays to short arrays while keeping the distance between the points the same.
        // This is used for the PCM encoding later
        short[] pcm = new short[audio_input.length];
        for (int i = 0; i < audio_input.length; i++)
        {
            pcm[i] = (short) (audio_input[i] * (Short.MAX_VALUE / max_val));
        }

        // Create input streams out of the byte arrays
        InputStream speaker1_stream = new ByteArrayInputStream(AudioPreprocessor.short2byte(pcm));

        // Create AudioInputStreams
        return new AudioInputStream(speaker1_stream, desired_format, pcm.length);
    }

    /**
     * Converts the short array into a byte array
     * @param src: Short array that will be converted
     * @return: Little endian style byte array
     */
    public static byte[] short2byte(short[] src)
    {
        int srcLength = src.length;
        byte[] dst = new byte[srcLength * 2];
        int j;
        for (int i=0; i<srcLength; i++)
        {
            short x = src[i];
            j = i * 2;
            dst[j] = (byte) ((x) & 0xff);
            dst[j + 1] = (byte) ((x >>> 8) & 0xff);
        }
        return dst;
    }

    /**
     * Copies the given array list to a list of double the length. Needed because of how Java handles manipulation of
     * AudioInputStream data
     * @param input: List of AudioInputStreams that will be copied
     * @return: List that contains two of each of the original lists of audio streams
     * @throws IOException
     */
    public static ArrayList<ArrayList<AudioInputStream>> copyStreams(ArrayList<AudioInputStream> input) throws IOException
    {
        ArrayList<ArrayList<AudioInputStream>> copies = new ArrayList<>();
        int length;
        for (AudioInputStream audioInputStream : input)
        {
            length = audioInputStream.available() / 2;
            ArrayList<AudioInputStream> stream_copies = new ArrayList<>();
            float[] float_copy = convertToFloats(audioInputStream, length,
                    audioInputStream.getFormat().getSampleRate(), false);
            stream_copies.add(convertToInputStream(float_copy, MatrixOperations.getMaxElement(float_copy),
                    audioInputStream.getFormat()));
            stream_copies.add(convertToInputStream(float_copy, MatrixOperations.getMaxElement(float_copy),
                    audioInputStream.getFormat()));
            copies.add(stream_copies);
        }

        return copies;
    }

    /**
     * Splices a new audio vector from the input audio along the array of indices given
     * @param original_audio: Original audio vector that will be cut down according to the indices
     * @param indices: Indices of the original audio vector that will be appended to the new vector
     * @return: Audio vector containing audio from original_audio with the indices of indices
     */
    public static float[] getFloatsFromIndices(float[] original_audio, ArrayList<int[]> indices)
    {
        // Calculate the size of the array that will be needed
        int length = 0;
        for (int[] idx: indices)
        {
            length += idx[1] - idx[0];
        }

        // Initialize float array
        float[] floats = new float[length];

        // Place values into the float array
        int floats_idx = 0;
        for (int[] idx: indices)
        {
            for (int i = idx[0]; i < idx[1]; i++)
            {
                floats[floats_idx] = original_audio[i];
                floats_idx++;
            }
        }

        return floats;
    }

    public static AudioInputStream preprocessAudio(AudioInputStream audio, VAD model) throws IOException,
                                                                                             TranslateException
    {
        // Step 0: Record length in case of AudioInputStream bug
        int length = (int) audio.getFrameLength();
        // Step 1: Resample audio to 16kHz.
        audio = resampleAudio(audio, 16000f);

        // Step 2: Filter audio
        AudioFormat original_format = audio.getFormat();
        float[] filtered_frames = DigitalFilters.applyBandpassFilter(convertToFloats(audio, length,
                                                                    original_format.getSampleRate(), false));

        // Step 3: Create AudioInputStream out of the filtered audio
        AudioFormat standard_format = new AudioFormat(original_format.getEncoding(), 16000,
                original_format.getSampleSizeInBits(), 1, original_format.getFrameSize(),
                160000, original_format.isBigEndian());
        AudioInputStream filtered_audio = convertToInputStream(filtered_frames,
                                                    MatrixOperations.getMaxElement(filtered_frames), standard_format);

        // Apply VAD model to the filtered audio
        AudioInputStream speech_only = model.separateSpeech(filtered_audio, standard_format);

        return speech_only;
    }

}
