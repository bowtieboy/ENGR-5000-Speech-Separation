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

public class SpeakerEmbedder
{
    private final Predictor<List<Float[]>, Float[][]> emb_predictor;

    public SpeakerEmbedder(String model_dir, Device device) throws MalformedModelException, IOException
    {
        Path model_path = Paths.get(model_dir);
        Model emb_model = Model.newInstance("traced_embedder.zip", device);
        emb_model.load(model_path);

        Translator<List<Float[]>, Float[][]> emb_translator = new Translator<List<Float[]>, Float[][]>() {
            @Override
            public Batchifier getBatchifier() {
                return new DetectionBatchifier();
            }

            @Override

            public Float[][] processOutput(TranslatorContext ctx, NDList list)
            {
                // Define the array that will be returned
                int windows = (int) list.head().size(0);
                int entry = (int) list.head().size(1);
                Float[][] embeddings = new Float[windows][entry];
                float[] list_array = list.head().toFloatArray();
                for (int i = 0; i < windows; i++)
                {
                    for (int j = 0; j < entry; j++)
                    {
                        embeddings[i][j] = list_array[(i * entry) + j];
                    }
                }

                return embeddings;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, List<Float[]> input)
            {
                float[][] temp_Float = new float[input.size()][input.get(0).length];
                int batch_index = 0;
                for (Float[] batch : input) {
                    for (int i = 0; i < batch.length; i++) {
                        temp_Float[batch_index][i] = batch[i];
                    }
                    batch_index++;
                }
                NDArray array = ctx.getNDManager().create(temp_Float);
                return new NDList(array);
            }
        };

        this.emb_predictor = emb_model.newPredictor(emb_translator);
    }

    /**
     * Creates a batch of embeddings for each AudioInputStream
     * @param audio_input_list: List of AudioInputStreams that will have embeddings calculated for
     * @return: List of embeddings for each AudioInputStream
     * @throws IOException: No idea
     * @throws TranslateException: No idea
     */
    public ArrayList<Float[][]> calculateBatchEmbeddings(ArrayList<AudioInputStream> audio_input_list) throws
                                                                                        IOException, TranslateException
    {
        ArrayList<Float[][]> embeddings_list = new ArrayList<>();

        for (AudioInputStream audio_input: audio_input_list)
        {
            embeddings_list.add(calculateEmbeddings(audio_input));
        }
        return embeddings_list;
    }

    /**
     * Creates embeddings for the AudioInputStream
     * @param audio_input: AudioInputStream that will have embeddings calculated
     * @return: Embeddings for the AudioInputStream
     * @throws IOException: No idea
     * @throws TranslateException: No idea
     */
    public Float[][] calculateEmbeddings(AudioInputStream audio_input) throws
            IOException, TranslateException
    {
        // Grab the format of the audio
        AudioFormat input_format = audio_input.getFormat();
        int frame_length = (int) audio_input.getFrameLength();

        // Determine if audio needs to be resampled
        float fs = 16000;
        if (input_format.getSampleRate() != fs)
        {
            audio_input = AudioPreprocessor.resampleAudio(audio_input, fs);
        }

        // Convert the audio to Floats array
        float[] frames = AudioPreprocessor.convertToFloats(audio_input, frame_length,
                input_format.getSampleRate(), false);

        // Create window parameters
        SlidingWindow sliding_window = new SlidingWindow(1.0f / 16000f, 1.0f / 16000f,
                -0.5f / 16000f, Float.POSITIVE_INFINITY);
        SlidingWindowFeature sliding_window_feature = new SlidingWindowFeature(frames, sliding_window);
        SlidingWindow resolution = new SlidingWindow(4.0f, 1.0f, 0f, Float.POSITIVE_INFINITY);

        // Break the sliding window into batches of the correct size
        Segment support = sliding_window_feature.extent();
        ArrayList<Segment> chunks =new ArrayList<>();
        float fixed;
        if (support.getDuration() < sliding_window.getDuration())
        {
            chunks.add(support);
        }
        else
        {
            chunks = resolution.slideWindowOverSupport(support, true);
        }

        // Crop the batches
        ArrayList<Float[]> batches = new ArrayList<>();
        for (Segment batch: chunks)
        {
            batches.add(sliding_window_feature.crop(batch, "center", resolution.getDuration()));
        }

        // Run the batches through the network and return the resulting matrix
        return this.emb_predictor.predict(batches);

    }
}
