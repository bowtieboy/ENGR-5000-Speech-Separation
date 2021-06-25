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
import java.util.List;


public class OverlappingSpeech
{

    // Stores the model predictors
    private final Predictor<Float[], Float[]> ovl_predictor;
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
    public OverlappingSpeech(String model_dir) throws MalformedModelException, IOException {

        // Define locations of the saved model paths
        Path model_path = Paths.get(model_dir);
        Model ovl_model = Model.newInstance("overlap_detection.zip", Device.cpu());
        Model enc_model = Model.newInstance("traced_encoder.zip", Device.cpu());
        Model mask_model = Model.newInstance("traced_masknet.zip", Device.cpu());
        Model dec_model = Model.newInstance("traced_decoder.zip", Device.cpu());

        // Load the models
        ovl_model.load(model_path);
        enc_model.load(model_path);
        mask_model.load(model_path);
        dec_model.load(model_path);

        // Define ovl model path
        Translator<Float[], Float[]> translator = new Translator<Float[], Float[]>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public Float[] processOutput(TranslatorContext ctx, NDList list)
            {
                float[] temp_Float = list.head().toFloatArray();
                Float[] return_array = new Float[temp_Float.length];
                for (int i = 0; i < return_array.length; i++)
                {
                    return_array[i] = temp_Float[i];
                }
                return return_array;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input)
            {
                float[] temp_Float = new float[input.length];
                for (int i = 0; i < temp_Float.length; i++)
                {
                    temp_Float[i] = input[i];
                }
                NDArray array = ctx.getNDManager().create(temp_Float);
                return new NDList(array);
            }
        };
        Predictor<Float[], Float[]> ovl_predictor = ovl_model.newPredictor(translator);

        // Define masknet model path
        Translator<NDArray, NDArray> translator_mask = new Translator<NDArray, NDArray>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public NDArray processOutput(TranslatorContext ctx, NDList list)
            {
                if (!first_speaker)
                {
                    sep_speakers = list.head();
                    first_speaker = true;
                }
                else
                {
                    sep_speakers = sep_speakers.concat(list.head());
                    first_speaker = false;
                }
                return sep_speakers;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, NDArray input)
            {
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
            public NDArray processOutput(TranslatorContext ctx, NDList list)
            {
                return list.head();
            }

            @Override
            public NDList processInput(TranslatorContext ctx, NDArray input)
            {
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
            public Float[][] processOutput(TranslatorContext ctx, NDList list) throws Exception
            {
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
                Float[][] separated_speech = new Float[2][(int)dec_shape.size()];
                for (int sample = 0; sample < (int)dec_shape.size(); sample++)
                {
                    separated_speech[0][sample] = temp_float_0[sample];
                    separated_speech[1][sample] = temp_float_1[sample];
                }

                return separated_speech;
            }

            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input)
            {
                float[] temp_Float = new float[input.length];
                for (int i = 0; i < temp_Float.length; i++)
                {
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

    // TODO Implement this function
    public AudioInputStream detectOverlappingSpeech(AudioInputStream audio_input)
    {
        AudioFormat input_format = audio_input.getFormat();

        // Return this to prevent error for now
        return audio_input;
    }

    /**
     * @param audio_input: The overlapping audio stream that needs to be separated
     * @return: A list of AudioInputStreams with each entry corresponding to a separated speech stream
     * @throws IOException: Thrown if file cant be read
     * @throws TranslateException: Thrown if issue with PyTorch model input form
     */
    public AudioInputStream[] separateOverlappingSpeech(AudioInputStream audio_input) throws IOException, TranslateException {

        // Define array that will contain the separated audio streams
        AudioInputStream[] separated_audio = new AudioInputStream[2];

        // Grab the format of the audio
        AudioFormat input_format = audio_input.getFormat();
        int frame_length = (int) audio_input.getFrameLength();

        // Determine if audio needs to be resampled
        if (input_format.getSampleRate() != this.separator_fs)
        {
            audio_input = AudioPreprocessor.downsampleAudio(audio_input, this.separator_fs);
        }

        // Convert the audio to Floats array
        float[] frames = AudioPreprocessor.convertToFloats(audio_input, frame_length);

        // Window the audio into the proper lengths to be fed through the system
        List<Float[]> windows = AudioPreprocessor.makeWindows(frames, this.separator_fs, this.window_length);

        // Run the audio through the models
        List<Float[][]> separated_speech = this.separator.batchPredict(windows);

        // Separate the list of speaker windows into two vectors
        float max_1 = 0f; // Used for scaling later on
        float max_2 = 0f;
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
                if (max_1 < Math.abs(speaker1_window[s])) max_1 = Math.abs(speaker1_window[s]);

                // Speaker 2
                speaker2[(w * separated_speech.get(0)[1].length) + s] = speaker2_window[s];
                if (max_2 < Math.abs(speaker2_window[s])) max_2 = Math.abs(speaker2_window[s]);
            }

        }

        // Create output audio format
        AudioFormat output_format = new AudioFormat(input_format.getEncoding(), this.separator_fs,
                input_format.getSampleSizeInBits(), 1, input_format.getFrameSize(),
                this.separator_fs, input_format.isBigEndian());

        // Create AudioInputStreams
        separated_audio[0] = AudioPreprocessor.convertToInputStream(speaker1, max_1, output_format);
        separated_audio[1] = AudioPreprocessor.convertToInputStream(speaker2, max_2, output_format);

        // Repackage the audio into two AudioInputStreams
        return separated_audio;
    }

}
