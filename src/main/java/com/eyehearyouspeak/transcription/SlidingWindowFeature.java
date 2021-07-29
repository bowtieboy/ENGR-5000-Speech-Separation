package com.eyehearyouspeak.transcription;

// TODO: Finish class
public class SlidingWindowFeature
{
    private SlidingWindow window;
    private float[] data;
    private int index;

    public SlidingWindowFeature(float[] data, SlidingWindow window)
    {
        this.data = data;
        this.window = window;
        this.index = -1;
    }

    // Getters
    private int length()
    {
        return this.data.length;
    }
    public float[] getData()
    {
        return this.data;
    }
    public int getNumber()
    {
        return this.data.length;
    }
    public SlidingWindow getWindow()
    {
        return this.window;
    }

    public Segment extent()
    {
        return this.window.rangeToSegment(0, length());
    }

    public Float[] crop(Segment focus, String mode, float fixed)
    {
        // Define how the cropping will take place
        int[] ranges = this.window.crop(focus, mode, fixed, true);
        int n_samples = this.data.length;
        int n_dimensions = 1; // Only using mono-audio

        // Crop the data from the start idx to the end idx
        int duration = ranges[ranges.length - 1] - ranges[0];
        Float[] cropped_data = new Float[duration];
        for (int i = 0; i < duration; i++)
        {
            cropped_data[i] = this.data[i + ranges[0]];
        }

        return cropped_data;
    }
}
