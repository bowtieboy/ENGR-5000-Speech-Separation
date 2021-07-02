import java.sql.Time;
import java.util.ArrayList;

public class Binarize
{
    private static float onset = 0.4009569122662475f;
    private static float offset = 0.4009569122662475f;
    private String scale = "absolute";
    private boolean log_scale = false;
    private static float pad_onset = 0.0f;
    private static float pad_offset = 0.0f;
    private static float min_duration_on = 0.1f;
    private static float min_duration_off = 0.1f;

    public static Timeline apply(SlidingWindowFeature predictions, int dimension)
    {
        // Grab the needed data
        float[] data = predictions.getData();
        int n_samples = predictions.getNumber();
        SlidingWindow window = predictions.getWindow();

        // Populate the timestamps
        float[] timestamps = new float[n_samples];
        for (int i = 0; i < n_samples; i++)
        {
            timestamps[i] = window.getItem(i).getMiddle();
        }
        float start = timestamps[0];
        boolean label = data[0] > onset;

        int mini = 0;
        int maxi = 1;

        float onset_local = mini + onset * (maxi - mini);
        float offset_local = mini + offset * (maxi - mini);

        // Timeline meant to store 'active' segments
        Timeline active = new Timeline(new ArrayList<Segment>());

        for (int i = 0; i < n_samples; i++)
        {
            float t = timestamps[i];
            float y = data[i];

            // If currently active
            if (label)
            {
                // Switching from active to inactive
                if (y < offset_local)
                {
                    Segment segment = new Segment(start - pad_onset, t + pad_offset);
                    active.add(segment);
                    start = t;
                    label = false;
                }
            }
            // If currently inactive
            else
            {
                // Switching from inactive to active
                if (y > onset_local)
                {
                    start = t;
                    label = true;
                }
            }
        }

        // If active at the end, add the final segment
        if (label)
        {
            Segment segment = new Segment(start - pad_onset, data[n_samples - 1] + pad_offset);
            active.add(segment);
        }

        // Merge any overlapping segments
        active = active.support(0);

        // Remove short 'active' segments
        active = new Timeline(active.removeShort(min_duration_on));

        // Fill short inactive gaps
        Timeline inactive = active.gaps();
        for (Segment segment: inactive.set)
        {
            if (segment.getDuration() < min_duration_off) active.add(segment);
        }

        active = active.support(0);
        return active;
    }
}
