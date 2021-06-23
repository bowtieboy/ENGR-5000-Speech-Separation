import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import java.io.IOException;
import java.nio.file.*;


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
        AudioInputStream[] separated_audio = new AudioInputStream[2];

        // Grab the format of the audio
        AudioFormat input_format = audio_input.getFormat();

        // Determine if audio needs to be resampled
        if (input_format.getSampleRate() != this.separator_fs)
        {
            AudioInputStream formatted_audio = AudioPreprocessor.downsampleAudio(audio_input, separator_fs);
        }
        else
        {
            AudioInputStream formatted_audio = audio_input;
        }

        // Window the audio into the proper lengths to be fed through the system

        // Run each window through the speech separation models

        // Repackage the audio into two AudioInputStreams

        return separated_audio;
    }



}
