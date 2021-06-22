import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;
import ai.djl.pytorch.engine.*;

import java.io.IOException;
import java.nio.file.*;

public class OverlappingSpeech
{

    private final Predictor<Double, Double> ovl_predictor;
    private final Predictor<Double, Double> enc_predictor;
    private final Predictor<Double, Double> mask_predictor;
    private final Predictor<Double, Double> dec_predictor;

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

    public double detectOverlappingSpeech(double[] audio, double sample_rate)
    {
        return 0.0;
    }

    public double separateOverlappingSpeech(double[] audio, double sample_rate)
    {
        return 0;
    }
}
