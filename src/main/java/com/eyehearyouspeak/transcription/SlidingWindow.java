package com.eyehearyouspeak.transcription;

import java.util.ArrayList;

public class SlidingWindow
{
    private float duration;
    private float step;
    private float start;
    private float end;
    private float index;

    public SlidingWindow(float duration, float step, float start, float end)
    {
        // Check to make sure input values are valid
        if (duration <= 0f) throw new IllegalArgumentException("Duration must be a float > 0.");
        if (step <= 0f) throw new IllegalArgumentException("Step must be a float > 0.");
        if (end <= 0f && end != -1) throw new IllegalArgumentException("Step must be a float > 0 or ==-1 for inf");
        if (end <= start) throw new IllegalArgumentException("End must be a float > start.");

        // Assign values to the properties
        this.duration = duration;
        this.step = step;
        this.start = start;
        this.end = end;
        this.index = -1;
    }

    // Getters
    public float getStart()
    {
        return this.start;
    }
    public float getEnd()
    {
        return this.end;
    }
    public float getStep()
    {
        return this.step;
    }
    public float getDuration()
    {
        return this.duration;
    }

    /**
     * Returns the closest frame to a given time value
     * @param t: Timestamp, in seconds
     * @return: Index of the frame whose mid section is closest to t
     */
    public int closestFrame(float t)
    {
        return (int) Math.round((t - this.start - 0.5 * this.duration) / this.step);
    }

    /**
     * Computes maximum number of consecutive frames that can be fitted into a segment with duration from_duration
     * @param from_duration: Duration in seconds
     * @param mode: Either "strict", "loose", or "center"
     * @return
     */
    public int samples(float from_duration, String mode)
    {
        if (mode.equals("strict"))
        {
            return (int) Math.floor((from_duration - this.duration) / this.step) + 1;
        }
        else if (mode.equals("loose"))
        {
            return (int) Math.floor((from_duration - this.duration) / this.step);
        }
        else if (mode.equals("center"))
        {
            return (int) Math.round(from_duration / this.step);
        }

        else throw new IllegalArgumentException("Not a valid mode input.");
    }

    /**
     * Crops the segment along the other given segment
     * @param focus: com.eyehearyouspeak.transcription.Segment that the cropping will be applied against
     * @param mode: How the cropping will be done
     * @param fixed: Overrides focus's duration. If not desired, use -1
     * @return: Array of unique indices of matching segments
     */
    public int[] crop(Segment focus, String mode, float fixed, boolean return_ranges)
    {
        // List of arrays to be returned
        int[] indices = new int[2];

        if (mode.equals("loose"))
        {
            indices[0] = (int) Math.ceil((focus.getStart() - this.duration - this.start) / this.step);
            // If not fixed
            if (fixed == -1)
            {
                indices[1] = (int) Math.floor((focus.getEnd() - this.start) / this.step) + 1;
            }
            else indices[1] = this.samples(fixed, mode) + indices[0];
        }

        else if (mode.equals("strict"))
        {
            indices[0] = (int) Math.ceil((focus.getStart() - this.start) / this.step);
            // If not fixed
            if (fixed == -1)
            {
                indices[1] = (int) Math.floor((focus.getEnd() - this.duration - this.start) / this.step) + 1;
            }
            else indices[1] = this.samples(fixed, mode) + indices[0];
        }
        else if (mode.equals("center"))
        {
            indices[0] = this.closestFrame(focus.getStart());
            // Edge case where indices[0] is negative
            if (indices[0] < 0) indices[0] = 0;
            // If not fixed
            if (fixed == -1)
            {
                indices[1] = this.closestFrame(focus.getEnd()) + 1;
            }
            else indices[1] = this.samples(fixed, mode) + indices[0];
        }
        else throw new IllegalArgumentException("Mode is not one of the allowed values.");

        // Create array that goes from indice 0 to indice 1
        if (!return_ranges)
        {
            int[] ranges = new int[indices[1] - indices[0]];
            for (int i = 0; i < ranges.length; i++)
            {
                ranges[i] = indices[0] + i;
            }
            return ranges;
        }
        else return indices;
    }

    /**
     * Convert the segment to a 0-indexed frame range
     * @param segment: com.eyehearyouspeak.transcription.Segment that will be converted
     * @return: 1st entry: index of first frame, 2nd entry: number of frames
     */
    public int[] segmentToRange(Segment segment)
    {
        // Define return array
        int[] range = new int[2];

        range[0] = this.closestFrame(segment.getStart());
        range[1] = (int) (segment.getDuration() / this.step) + 1;

        return range;
    }

    /**
     * Convert 0-indexed frame range to segment
     * @param first_frame: Index of first frame
     * @param num_frames: Number of frames
     * @return: com.eyehearyouspeak.transcription.Segment
     */
    public Segment rangeToSegment(int first_frame, int num_frames)
    {
        float start = this.start + (first_frame - 0.5f) * this.step + 0.5f * this.duration;
        float duration = num_frames * this.step;
        float end = start + duration;

        // Extend segment to the beginning of the timeline
        if (first_frame == 0)
        {
            start = this.start;
        }

        return new Segment(start, end);
    }

    /**
     * Returns the duration of the samples
     * @param n_samples: Number of samples
     * @return: Duration of samples
     */
    public float samplesToDuration(int n_samples)
    {
        return this.rangeToSegment(0, n_samples).getDuration();
    }

    /**
     * Returns the samples in duration
     * @param duration: duration of time
     * @return: sample of the duration
     */
    public int durationToSamples(float duration)
    {
        return this.segmentToRange(new Segment(0, duration))[1];
    }

    /**
     * Creates an iterable list of segments that span the entirety of the given segment using the sliding windows parameters
     * @param support: com.eyehearyouspeak.transcription.Segment that will be broken up
     * @param align_last: Whether or not to use a final segment that aligns exactly with support regardless of step size
     * @return: An iterable list of segments
     */
    public ArrayList<Segment> slideWindowOverSupport(Segment support, boolean align_last)
    {
        // Create arraylist that will be returned
        ArrayList<Segment> segments = new ArrayList<>();

        // Define initial start and end
        float start = support.getStart();
        float end = start + this.duration;

        // Until the current segments end is greater than support's end, add more windows
        while (end < support.getEnd())
        {
            // Add new segment
            segments.add(new Segment(start, end));

            // Change start and end values
            start += this.step;
            end += this.step;
        }

        // If last segment needs to be added, then add it
        if (align_last)
        {
            end = support.getEnd();
            start = end - this.duration;
            segments.add(new Segment(start, end));
        }


        // Return the list
        return segments;
    }

    /**
     * Returns the ith com.eyehearyouspeak.transcription.Segment of the window
     * @param index: Which index in the window is desired
     * @return: The segment of ith index
     */
    public Segment getItem(int index)
    {
        // Start time for the segment
        float start = this.start + (index * this.step);

        // If the start time is after the end of the window, return null
        if (start >= this.end)
        {
            return null;
        }

        return new Segment(start, start + this.duration);

    }
}
