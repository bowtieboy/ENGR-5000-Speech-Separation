package com.eyehearyouspeak.transcription;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VAD
{

    private Predictor<Float[], Float[]> predictor;
    private float fs = 16000;
    private float trig_sum = 0.25f;
    private float neg_trig_sum = 0.07f;
    private int num_steps = 4;
    private int batch_size = 200;
    private int num_samples_per_window = 4000;
    private int min_speech_samples = 10000;
    private int min_silence_samples = 500;
    private int step = 1000;


    public VAD(String model_dir, Device device) throws MalformedModelException, IOException
    {
        // Load the model data
        Path model_path = Paths.get(model_dir);
        Model model = Model.newInstance("vad_micro.zip", device);
        model.load(model_path);

        // Define the translator
        Translator<Float[], Float[]> model_translator = new Translator<Float[], Float[]>()
        {
            @Override
            public Batchifier getBatchifier() {
                return null;
            }

            @Override
            public Float[] processOutput(TranslatorContext ctx, NDList list) throws Exception
            {
                float[] array = list.head().toFloatArray();
                Float[] obj_array = new Float[array.length];
                for (int i = 0; i < array.length; i++)
                {
                    obj_array[i] = array[i];
                }
                return obj_array;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input) throws Exception
            {
                float[] temp_Float = new float[input.length];
                for (int i = 0; i < temp_Float.length; i++) {
                    temp_Float[i] = input[i];
                }
                NDArray array = ctx.getNDManager().create(temp_Float);
                return new NDList(array);
            }
        };

        // Store needed objects
        this.predictor = model.newPredictor(model_translator);
    }

    /**
     * Separates out silences from the input audio stream
     * @param audio: Input audio stream
     * @return: Only speech segments of the input audio
     */
    public AudioInputStream separateSpeech(AudioInputStream audio, AudioFormat output_format) throws IOException, TranslateException {
        // Grab the format of the audio
        AudioFormat input_format = audio.getFormat();
        int frame_length = (int) audio.getFrameLength();

        // Determine if audio needs to be resampled
        if (input_format.getSampleRate() != this.fs)
        {
            audio = AudioPreprocessor.resampleAudio(audio, this.fs);
        }

        // Convert the audio to Floats array
        float[] frames = AudioPreprocessor.convertToFloats(audio, frame_length,
                input_format.getSampleRate(), true);

        // Window the audio into the proper length and overlap
        int step = this.num_samples_per_window / this.num_steps;
        ArrayList<Float[]> windows = AudioPreprocessor.makeWindows(frames, (int) this.fs,
                                                                    0.25f, 3/16.0f);

        // Run the model on the windows
        List<Float[]> outputs = this.predictor.batchPredict(windows);

        // Convert list to matrix
        float[][] output_mat = new float[outputs.size()][2];
        for (int i = 0; i < outputs.size(); i++)
        {
            output_mat[i][0] = outputs.get(i)[0];
            output_mat[i][1] = outputs.get(i)[1];
        }

        // Get second column of matrix
        float[] col_1 = MatrixOperations.getColumn(output_mat, 1);

        // Get the speech indices from the output
        ArrayList<int[]> speech_indices = getSpeechIndices(col_1);

        // Get the floats from the corresponding indices
        float[] speech_only = AudioPreprocessor.getFloatsFromIndices(frames, speech_indices);

        // Create the return AudioInputStream
        return AudioPreprocessor.convertToInputStream(speech_only, output_format);
    }

    /**
     * Converts the output from the TorchScript model to indices of speech
     * @param column: Direct output from the TorchScript model. Only needs a single column of the two column output
     * @return: The indices of the audio data that contain human speech
     */
    private ArrayList<int[]> getSpeechIndices(float[] column)
    {
        ArrayList<int[]> indices = new ArrayList<>();

        boolean triggered = false;
        int temp_end = 0;
        float smoothed_prob = 0f;
        int idx = 1;
        Buffer buffer = new Buffer(4);
        int[] current_indices = new int[2];
        for (int i = 0; i < column.length; i++)
        {
            buffer.push(column[i]);
            smoothed_prob = buffer.sumBuffer() / (float) buffer.getIndex();
            idx++;

            if (smoothed_prob >= this.trig_sum && temp_end > 0)
            {
                temp_end = 0;
            }
            if (smoothed_prob >= trig_sum && !triggered)
            {
                triggered = true;
                current_indices[0] = this.step * Math.max(0, i - this.num_steps);
                continue;
            }
            if(smoothed_prob < neg_trig_sum && triggered)
            {
                if (temp_end == 0)
                {
                    temp_end = this.step * i;
                }
                if ((this.step * i) - temp_end < this.min_silence_samples)
                {
                    continue;
                }
                else
                {
                    current_indices[1] = temp_end;
                    if (current_indices[1] - current_indices[0] > this.min_speech_samples)
                    {
                        indices.add(current_indices);
                    }
                    temp_end = 0;
                    triggered = false;
                    current_indices = new int[2];
                }
            }
        }

        // If a start time was recorded at the end of the for loop, make the end time the end of the vector
        if (current_indices[0] != 0)
        {
            current_indices[1] = column.length * this.step;
            indices.add(current_indices);
        }

        return checkForOverlap(indices);
    }

    private ArrayList<int[]> checkForOverlap(ArrayList<int[]> indices)
    {
        ArrayList<int[]> no_overlap = new ArrayList<>();

        int[] previous_idx = indices.get(0);
        no_overlap.add(previous_idx);
        int[] current_idx = new int[2];
        int[] new_idx = new int[2];
        for (int i = 1; i < indices.size(); i++)
        {
            current_idx = indices.get(i);
            // If the number of samples between two segments is less than the number of samples in a single window,
            // count it as a single window
            if (current_idx[0] - previous_idx[1] < this.num_samples_per_window)
            {
                new_idx[0] = previous_idx[0];
                new_idx[1] = current_idx[1];
                no_overlap.remove(no_overlap.size() - 1);
            }
            // Otherwise, assume no overlap occured between indices
            else
            {
                new_idx = current_idx;
            }
            no_overlap.add(new_idx);
            previous_idx = current_idx;

        }

        return no_overlap;
    }

}
