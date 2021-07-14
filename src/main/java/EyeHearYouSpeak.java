import ai.djl.Device;
import ai.djl.MalformedModelException;

import java.io.IOException;
import java.nio.file.Path;

public class EyeHearYouSpeak
{
    // Store the models that the class will use
    private VAD vad;
    private OverlappingSpeech ovl;
    private SpeakerEmbedder emb;
    private SpeakerIdentification iden;

    public EyeHearYouSpeak(String model_path) throws MalformedModelException, IOException
    {
        // Create the device that the models will be loaded onto. If a GPU is available, use that
        Device device;
        if (Device.getGpuCount() > 0)
        {
            device = Device.gpu();
        } else device = Device.cpu();
        // FIXME: Bug where data isn't being loaded onto the GPU for calculations, but it needs to be. Use CPU for now
        device = Device.cpu();

        this.vad = new VAD(model_path, device);
        this.emb = new SpeakerEmbedder(model_path, device);
        this.ovl = new OverlappingSpeech(model_path, device);
    }
}
