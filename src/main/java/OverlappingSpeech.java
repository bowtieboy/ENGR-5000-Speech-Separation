import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
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
import java.util.Arrays;
import java.util.List;


public class OverlappingSpeech
{

    // Stores the model predictors
    private final Predictor<List<Float[]>, List<Float[][]>> ovl_predictor;
    private final Predictor<Float[], Float[][]> separator;

    // Store model parameters
    private final int separator_fs = 8000;
    private final int ovl_det_fs = 16000;
    private final float window_length = 1; // Unit is seconds

    // Used for storing both of the masked outputs from masknet
    private boolean first_speaker = false;
    private NDArray sep_speakers;

    /**
     * Creates the OverlappingSpeech object which is able to detect overlapping speech in an AudioInputStream and also
     * separate out the overlapping speech into two AudioInputStream objects.
     *
     * @param model_dir: Directory where the model zip files are being stored.
     * @throws MalformedModelException: Not a clue
     * @throws IOException: Not a clue
     */
    public OverlappingSpeech(String model_dir, Device device) throws MalformedModelException, IOException {

        // Define locations of the saved model paths
        Path model_path = Paths.get(model_dir);
        Model ovl_model = Model.newInstance("overlap_detection.zip", device);
        Model enc_model = Model.newInstance("traced_encoder.zip", device);
        Model mask_model = Model.newInstance("traced_masknet.zip", device);
        Model dec_model = Model.newInstance("traced_decoder.zip", device);

        // Load the models
        ovl_model.load(model_path);
        enc_model.load(model_path);
        mask_model.load(model_path);
        dec_model.load(model_path);

        // Define ovl model path
        Translator<List<Float[]>, List<Float[][]>> ovl_translator = new Translator<List<Float[]>, List<Float[][]>>() {
            @Override
            public Batchifier getBatchifier() {
                return new DetectionBatchifier();
            }

            @Override

            public List<Float[][]> processOutput(TranslatorContext ctx, NDList list) {
                // Create list that will be returned
                List<Float[][]> batches = new ArrayList<>();
                NDArray array = list.get(0);

                // Loop through the batches
                for (int i = 0; i < array.size(0); i++) {
                    // Grab the batch array
                    NDArray temp_array = array.get(i);
                    float[] temp_float_array = temp_array.toFloatArray();

                    // Useful variables for shortening line length. Will hopefully be optimized out by compiler
                    int length = temp_float_array.length;
                    int first = (int) temp_array.size(0);
                    int second = (int) temp_array.size(1);

                    // Populate batch array
                    Float[][] temp_float_mat = new Float[first][second];
                    for (int j = 0; j < length; j += 2) {
                        temp_float_mat[j / 2][0] = temp_float_array[j];
                        temp_float_mat[j / 2][1] = temp_float_array[j + 1];
                    }

                    // Add the batch to the list
                    batches.add(temp_float_mat);
                }

                return batches;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, List<Float[]> input) {
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
        Predictor<List<Float[]>, List<Float[][]>> ovl_predictor = ovl_model.newPredictor(ovl_translator);
        // Define masknet model path
        Translator<NDArray, NDArray> translator_mask = new Translator<NDArray, NDArray>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public NDArray processOutput(TranslatorContext ctx, NDList list) {
                if (!first_speaker) {
                    sep_speakers = list.head();
                    first_speaker = true;
                } else {
                    sep_speakers = sep_speakers.concat(list.head());
                    first_speaker = false;
                }
                return sep_speakers;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, NDArray input) {
                return new NDList(input);
            }
        };
        Predictor<NDArray, NDArray> mask_predictor = mask_model.newPredictor(translator_mask);
        // Define decoder model path
        Translator<NDArray, NDArray> translator_decoder = new Translator<NDArray, NDArray>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public NDArray processOutput(TranslatorContext ctx, NDList list) {
                return list.head();
            }

            @Override
            public NDList processInput(TranslatorContext ctx, NDArray input) {
                return new NDList(input);
            }
        };
        Predictor<NDArray, NDArray> dec_predictor = dec_model.newPredictor(translator_decoder);
        // Define encoder translator
        Translator<Float[], Float[][]> translator_separator = new Translator<Float[], Float[][]>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public Float[][] processOutput(TranslatorContext ctx, NDList list) throws Exception {
                // Apply the mask
                NDArray mix_w = list.head();
                // Need to grab the combined matrix output, which requires a "batch predict" on a single input
                ArrayList<NDArray> mix_w_list = new ArrayList<>();
                mix_w_list.add(mix_w);
                NDArray masked = mask_predictor.batchPredict(mix_w_list).get(1);

                // Decode the mask
                mix_w = mix_w.stack(mix_w);
                NDArray sep_h = mix_w.mul(masked);
                NDArray dec_0 = dec_predictor.predict(sep_h.get(0));
                NDArray dec_1 = dec_predictor.predict(sep_h.get(1));

                // Convert the mask to a float matrix
                float[] temp_float_0 = dec_0.toFloatArray();
                float[] temp_float_1 = dec_1.toFloatArray();
                Shape dec_shape = dec_0.getShape();
                Float[][] separated_speech = new Float[2][(int) dec_shape.size()];
                for (int sample = 0; sample < (int) dec_shape.size(); sample++) {
                    separated_speech[0][sample] = temp_float_0[sample];
                    separated_speech[1][sample] = temp_float_1[sample];
                }

                return separated_speech;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input) {
                float[] temp_Float = new float[input.length];
                for (int i = 0; i < temp_Float.length; i++) {
                    temp_Float[i] = input[i];
                }
                NDArray array = ctx.getNDManager().create(temp_Float);
                //array = array.reshape(1, array.size());
                return new NDList(array);
            }
        };
        Predictor<Float[], Float[][]> separator_predictor = enc_model.newPredictor(translator_separator);

        // Store the predictors
        this.ovl_predictor = ovl_predictor;
        this.separator = separator_predictor;

    }

    public ArrayList<AudioInputStream> detectOverlappingSpeech(AudioInputStream audio_input) throws IOException, TranslateException {
        // Grab the format of the audio
        AudioFormat input_format = audio_input.getFormat();
        int frame_length = (int) audio_input.getFrameLength();

        // Determine if audio needs to be resampled
        if (input_format.getSampleRate() != this.ovl_det_fs)
        {
            audio_input = AudioPreprocessor.resampleAudio(audio_input, this.ovl_det_fs);
        }

        // Convert the audio to Floats array
        float[] frames = AudioPreprocessor.convertToFloats(audio_input, frame_length,
                                                            input_format.getSampleRate(), false);

        // Create window parameters
        SlidingWindow sliding_window = new SlidingWindow(1.0f / 16000f, 1.0f / 16000f,
                                                    -0.5f / 16000f, Float.POSITIVE_INFINITY);
        SlidingWindowFeature sliding_window_feature = new SlidingWindowFeature(frames, sliding_window);
        SlidingWindow resolution = new SlidingWindow(0.0203125f, 0.0016875f,
                                                    0f, Float.POSITIVE_INFINITY);

        // Break the sliding window into batches of the correct size
        Segment support = sliding_window_feature.extent();
        sliding_window = new SlidingWindow(2.0f, 0.5f, 0f, Float.POSITIVE_INFINITY);
        List<Segment> chunks = sliding_window.slideWindowOverSupport(support, true);

        // Crop the batches
        ArrayList<Float[]> batches = new ArrayList<>();
        for (Segment batch: chunks)
        {
            batches.add(sliding_window_feature.crop(batch, "center", sliding_window.getDuration()));
        }

        // Run the batches through the network
        List<Float[][]> fx = this.ovl_predictor.predict(batches);


        // Determine which frames are overlapping
        int n_frames = resolution.samples(chunks.get(chunks.size() - 1).getEnd(), "center");
        int dimension = 2;
        // Data[i] is the sum of all predictions for frame #1
        float[][] data = new float[n_frames][dimension];
        for (float[] datum : data)
        {
            Arrays.fill(datum, 0f);
        }
        // k[i] is the number of chunks that overlap with frame #1
        float[] k = new float[n_frames];
        Arrays.fill(k, 0f);

        String alignment = "strict";
        for (int i = 0; i < fx.size(); i++)
        {
            // Grab the batch
            Segment chunk = chunks.get(i);
            Float[][] fx_ = fx.get(i);

            // Indices of frames overlapped by chunk
            int[] indices = resolution.crop(chunk, alignment, sliding_window.getDuration(), false);

            // Accumulate the outputs
            for (int j = 0; j < indices.length; j++)
            {
                data[indices[j]][0] += fx_[j][0];
                data[indices[j]][1] += fx_[j][1];

                // Keep track of the number of overlapping sequences
                k[indices[j]] += 1;
            }
        }

        // Compute average embedding
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                data[i][j] /= Math.max(k[i], 1f);
                data[i][j] = (float) Math.exp(data[i][j]);
            }
        }

        SlidingWindowFeature overlap_prob = new SlidingWindowFeature(MatrixOperations.elementWiseSubtraction(
                                                MatrixOperations.getColumn(data, 0), 1.0f), resolution);

        // Create timeline of overlapping speech
        Timeline overlap = Binarize.apply(overlap_prob, 0);

        // Return the AudioInputStream containing only overlapping speech
        return getAudioFromTimeline(overlap, frames, this.ovl_det_fs, audio_input.getFormat());
    }

    /**
     * @param audio_input_list: The list of overlapping audio streams that need to be separated
     * @return: A list of AudioInputStreams with each entry corresponding to a separated speech stream
     * @throws IOException: Thrown if file cant be read
     * @throws TranslateException: Thrown if issue with PyTorch model input form
     */
    public ArrayList<AudioInputStream[]> separateOverlappingSpeech(ArrayList<AudioInputStream> audio_input_list) throws IOException, TranslateException {

        // Define final return list
        ArrayList<AudioInputStream[]> separated_streams = new ArrayList<>();

        // Loop through each of the overlapping streams and separated them
        for (AudioInputStream audio_input: audio_input_list)
        {
            // Define array that will contain the separated audio streams
            AudioInputStream[] separated_audio = new AudioInputStream[2];

            // Grab the format of the audio
            AudioFormat input_format = audio_input.getFormat();
            int frame_length = (int) audio_input.getFrameLength();

            // Determine if audio needs to be resampled
            if (input_format.getSampleRate() != this.separator_fs)
            {
                audio_input = AudioPreprocessor.resampleAudio(audio_input, this.separator_fs);
            }

            // Convert the audio to Floats array
            float[] frames = AudioPreprocessor.convertToFloats(audio_input, frame_length,
                    input_format.getSampleRate(), true);

            // Window the audio into the proper lengths to be fed through the system
            List<Float[]> windows = AudioPreprocessor.makeWindows(frames, this.separator_fs, this.window_length);

            // Run the audio through the models
            List<Float[][]> separated_speech = this.separator.batchPredict(windows);

            // Separate the list of speaker windows into two vectors
            float[] speaker1 = new float[separated_speech.size() * separated_speech.get(0)[0].length];
            float[] speaker2 = new float[separated_speech.size() * separated_speech.get(1)[0].length];
            for (int w = 0; w < separated_speech.size(); w++)
            {
                Float[] speaker1_window = separated_speech.get(w)[0];
                Float[] speaker2_window = separated_speech.get(w)[1];
                for (int s = 0; s < separated_speech.get(0)[0].length; s++)
                {
                    // Speaker 1
                    speaker1[(w * separated_speech.get(0)[0].length) + s] = speaker1_window[s];

                    // Speaker 2
                    speaker2[(w * separated_speech.get(0)[1].length) + s] = speaker2_window[s];
                }

            }

            float max_1 = MatrixOperations.getMaxElement(speaker1);
            float max_2 = MatrixOperations.getMaxElement(speaker2);
            // Create output audio format
            AudioFormat output_format = new AudioFormat(input_format.getEncoding(), this.separator_fs,
                    input_format.getSampleSizeInBits(), 1, input_format.getFrameSize(),
                    this.separator_fs, input_format.isBigEndian());

            // Create AudioInputStreams
            separated_audio[0] = AudioPreprocessor.convertToInputStream(speaker1, max_1, output_format);
            separated_audio[1] = AudioPreprocessor.convertToInputStream(speaker2, max_2, output_format);

            // Convert streams to 16k (Might have to do this depending on speaker embedding model
            //separated_audio[0] = AudioPreprocessor.resampleAudio(separated_audio[0], this.ovl_det_fs);
            //separated_audio[1] = AudioPreprocessor.resampleAudio(separated_audio[1], this.ovl_det_fs);

            // Add the separated streams to the list
            separated_streams.add(separated_audio);
        }

        return separated_streams;
    }

    private ArrayList<AudioInputStream> getAudioFromTimeline(Timeline timeline, float[] entire_audio,
                                                  float fs, AudioFormat new_format)
    {
        // Determine how many segments of overlapping speech exist in the audio
        int n_segments = timeline.set.size();
        // Determine the number of frames for each segment
        int[] n_frames = new int[n_segments];
        for (int i = 0; i < n_segments; i++)
        {
            n_frames[i] = (int) Math.floor(fs * timeline.set.get(i).getDuration());
        }

        // Define lists that will contain the audio of each overlapping segment
        ArrayList<float[]> audio = new ArrayList<>();
        ArrayList<AudioInputStream> overlapping_segments = new ArrayList<>();

        // Loop through each segment and assign the audio
        int start;
        for (int i = 0; i < n_segments; i++)
        {
            start = (int) (timeline.set.get(i).getStart() * fs);
            // Assign values
            float [] current_segment = Arrays.copyOfRange(entire_audio, start, start + n_frames[i]);

            // Add segment to the list
            overlapping_segments.add(AudioPreprocessor.convertToInputStream(current_segment,
                                                                        MatrixOperations.getMaxElement(current_segment),
                                                                        new_format));
        }

        // Return the new AudioInputStreams
        return overlapping_segments;

    }



}
