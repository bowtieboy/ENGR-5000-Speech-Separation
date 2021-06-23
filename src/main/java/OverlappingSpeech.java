import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;

import javax.sound.sampled.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;


public class OverlappingSpeech
{

    // Stores the model predictors
    private final Predictor<Double, Double> ovl_predictor;
    private final Predictor<Double, Double> enc_predictor;
    private final Predictor<Double, Double> mask_predictor;
    private final Predictor<Double, Double> dec_predictor;

    // Store model parameters
    private final int separator_fs = 8000;
    private final int ovl_det_fs = 16000;
    private final double window_length = 1; // Unit is seconds

    public OverlappingSpeech(String model_dir) throws MalformedModelException, IOException {

        // Define locations of the saved model paths
        Path model_path = Paths.get(model_dir);
        Model ovl_model = Model.newInstance("overlap_detection.zip");
        Model enc_model = Model.newInstance("traced_encoder.zip");
        Model mask_model = Model.newInstance("traced_masknet.zip");
        Model dec_model = Model.newInstance("traced_decoder.zip");

        // Load the models
        ovl_model.load(model_path);
        enc_model.load(model_path);
        mask_model.load(model_path);
        dec_model.load(model_path);

        // Define translator
        Translator<Double, Double> translator = new Translator<Double, Double>() {
            @Override
            public Batchifier getBatchifier() {
                return Batchifier.STACK;
            }

            @Override
            public Double processOutput(TranslatorContext ctx, NDList list) throws Exception {
                NDArray temp_arr = list.get(0);
                return temp_arr.getDouble();
            }

            @Override
            public NDList processInput(TranslatorContext ctx, Double input) throws Exception {
                NDManager manager = ctx.getNDManager();
                NDArray array = manager.create(new double[] {input});
                return new NDList(array);
            }
        };

        // Define predictors
        Predictor<Double, Double> ovl_predictor = ovl_model.newPredictor(translator);
        Predictor<Double, Double> enc_predictor = enc_model.newPredictor(translator);
        Predictor<Double, Double> mask_predictor = mask_model.newPredictor(translator);
        Predictor<Double, Double> dec_predictor = dec_model.newPredictor(translator);
        
        // Store the predictors
        this.ovl_predictor = ovl_predictor;
        this.enc_predictor = enc_predictor;
        this.mask_predictor = mask_predictor;
        this.dec_predictor = dec_predictor;

    }

    // TODO Implement this function
    public AudioInputStream detectOverlappingSpeech(AudioInputStream audio_input)
    {
        AudioFormat input_format = audio_input.getFormat();

        // Return this to prevent error for now
        return audio_input;
    }

    // TODO Finish this function
    public AudioInputStream[] separateOverlappingSpeech(AudioInputStream audio_input) throws IOException
    {
        // Define array that will contain the separated audio streams
        AudioInputStream separated_audio[] = new AudioInputStream[2];

        // Grab the format of the audio
        AudioFormat input_format = audio_input.getFormat();

        // Determine if audio needs to be resampled
        if (input_format.getSampleRate() != this.separator_fs)
        {
            AudioInputStream formatted_audio = resampleAudio(audio_input);
        }
        else
        {
            AudioInputStream formatted_audio = audio_input;
        }

        // Window the audio into the proper lengths to be fed through the system

        // Convert the AudioInputStream arrays to float arrays for the models

        // Run each window through the speech separation models

        // Repackage the audio into two AudioInputStreams

        return separated_audio;
    }

    private AudioInputStream resampleAudio(AudioInputStream audio_input) throws IOException
    {
        // Make the audio format easier to access
        AudioFormat input_format = audio_input.getFormat();

        // Convert the audio frames to an array of floats for manipulation
        double frames[] = convertToDoubles(audio_input);


        // Return this to prevent error for now
        return audio_input;
    }

    private AudioInputStream[] windowAudio(AudioInputStream audio_input)
    {
        // Put here to prevent errors when building. Will need to calculate amount of windows needed first.
        AudioInputStream windows[] = new AudioInputStream[2];
        return windows;
    }

    private double[] convertToDoubles(AudioInputStream audio_input) throws IOException
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

//    private  AudioInputStream convertToInputStream(double[] audio_input)
//    {
//
//    }

}
