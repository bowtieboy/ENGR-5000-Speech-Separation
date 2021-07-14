import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.translate.TranslateException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class EyeHearYouSpeak
{
    // Store the models that the class will use
    private VAD vad;
    private OverlappingSpeech ovl;
    private SpeakerEmbedder emb;
    private SpeakerIdentification iden;

    // Define standard sample rate that all the models will use
    float fs = 16000;

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

        // Load the models into memory
        this.vad = new VAD(model_path, device);
        this.emb = new SpeakerEmbedder(model_path, device);
        this.ovl = new OverlappingSpeech(model_path, device);
    }

    public ArrayList<String> annotateAudio(AudioInputStream audio) throws TranslateException, IOException
    {
        // Create variable that will contain the final speech array
        ArrayList<String> captions = new ArrayList<>();

        // Create the format for audio that will be used
        AudioFormat original_format = audio.getFormat();
        AudioFormat standard_format = new AudioFormat(original_format.getEncoding(), this.fs,
                original_format.getSampleSizeInBits(), 1, original_format.getFrameSize(),
                this.fs, original_format.isBigEndian());

        // Preprocess the audio stream
        AudioInputStream pre_processed = AudioPreprocessor.preprocessAudio(audio, this.vad);

        // Detect any overlapping speech within the audio
        Timeline overlapping_speech_tl = this.ovl.detectOverlappingSpeech(pre_processed);
        Timeline non_overlapping_speech_tl = this.ovl.invertTimeline(overlapping_speech_tl);

        // Grab both the overlapping and non-overlapping segments of speech
        ArrayList<AudioInputStream> mixed_speakers = this.ovl.getAudioFromTimeline(overlapping_speech_tl,
                overlapping_speech_tl.getFrames(), fs, standard_format);
        ArrayList<AudioInputStream> non_mixed_speakers = this.ovl.getAudioFromTimeline(non_overlapping_speech_tl,
                non_overlapping_speech_tl.getFrames(), fs, standard_format);

        // TODO: Implement threading here to process both the mixed and non-mixed at the same time

        // Separate out the overlapping speech



        return captions;
    }

}
