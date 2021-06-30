import java.sql.Time;

public class Binarize
{
    private float onset = 0.5f;
    private float offset = 0.5f;
    private String scale = "absolute";
    private boolean log_scale = false;
    private float pad_onset = 0.0f;
    private float pad_offset = 0.0f;
    private float min_duration_on = 0.0f;
    private float min_duration_off = 0.0f;

    public Timeline apply()
    {
        return new Timeline();
    }
}
