package com.eyehearyouspeak.transcription;

import java.util.ArrayList;

public class Timeline
{

    ArrayList<Segment> set;
    float[] timeline_boundaries;
    private float[] frames;

    public Timeline(ArrayList<Segment> segments)
    {
        this.set = segments;
        this.timeline_boundaries = findBoundaries();
    }

    public void add (Segment segment)
    {
        this.set.add(segment);

        // Check if segment bounaries need to be modified
        this.timeline_boundaries = findBoundaries();
    }

    public void setFrames(float[] frames)
    {
        this.frames = frames;
    }
    public float[] getFrames()
    {
        return this.frames;
    }

    /**
     * Creates a com.eyehearyouspeak.transcription.Timeline that containts the gaps (lack of segments) of this com.eyehearyouspeak.transcription.Timeline
     * @return
     */
    public Timeline gaps()
    {
        ArrayList<Segment> segments = gaps_iter();
        Timeline gap_tl = new Timeline(segments);
        gap_tl.setFrames(this.frames);
        return gap_tl;
    }

    /**
     * Same as gaps but returns an ArrayList of segments
     * @return: ArrayList of gaps within the timeline
     */
    private ArrayList<Segment> gaps_iter()
    {
        ArrayList<Segment> gap = new ArrayList<Segment>();

        Segment support = this.extent();

        float end = support.getEnd();

        // If the start of support isn't 0, create a segment that goes from 0 to start
        if (support.getStart() >= support.getSegmentPrecision())
        {
            gap.add(new Segment(0, support.getStart()));
        }

        for (Segment segment: crop(support).support_iter(0))
        {
            Segment gap_segment = new Segment(end, segment.getStart());
            if (gap_segment.isNotEmpty())
            {
                gap.add(gap_segment);
            }
            end = segment.getEnd();
        }

        // Add final gap if it is not empty
        Segment gap_segment = new Segment(end, support.getEnd());
        if (gap_segment.isNotEmpty())
        {
            gap.add(gap_segment);
        }

        return gap;
    }

    /**
     * Creates a timeline from this with the minimum number of segments
     * @param collar: Merge segments separated by less than collar seconds
     * @return: A timeline with minimal segments
     */
    public Timeline support(float collar)
    {
        return new Timeline(support_iter(0));
    }

    /**
     * Does the same as the support function but returns the array of segments rather than a timeline
     * @param collar: Merge segments separated by less than collar seconds
     * @return: List of segments with no overlap
     */
    private ArrayList<Segment> support_iter(float collar)
    {
        ArrayList<Segment> no_overlap = new ArrayList<Segment>();

        // Initialize first segment
        Segment new_segment = this.set.get(0);

        for (Segment segment: this.set)
        {
            // Determine if there is a gap
            Segment possible_overlap = segment.xor(new_segment);
            // If there is no gap or the gap duration is less than collar, combine it with the existing segment
            if (!possible_overlap.isNotEmpty() || possible_overlap.getDuration() < collar)
            {
                new_segment = new_segment.or(segment);
            }
            // If there is a gap and the duration is > collar seconds
            else
            {
                no_overlap.add(new_segment);
                new_segment = segment;
            }
        }

        // Add last segment after all is said and done
        no_overlap.add(new_segment);

        return no_overlap;
    }

    /**
     * Creates array list of all segments that are longer than the duration
     * @param min_duration: How long a segment must be
     * @return: List of all segments longer than min_duration
     */
    public ArrayList<Segment> removeShort(float min_duration)
    {
        ArrayList<Segment> no_short = new ArrayList<Segment>();

        // If the segment is on for longer than the minimum duration, add it to the list
        for (Segment segment: this.set)
        {
            if (segment.getDuration() > min_duration)
            {
                no_short.add(segment);
            }
        }

        return no_short;
    }

    /**
     * Creates a segment that spans the entire timeline
     * @return: com.eyehearyouspeak.transcription.Segment that is as long as the entire timeline
     */
    private Segment extent()
    {
        return new Segment(this.timeline_boundaries[0], this.timeline_boundaries[1]);
    }

    /**
     * Creates timeline of all segments that are contained within the given segment
     * @param support: com.eyehearyouspeak.transcription.Segment that all segments in the timeline will be contained in
     * @return: com.eyehearyouspeak.transcription.Timeline of segments contained in support
     */
    public Timeline crop(Segment support)
    {
        return new Timeline(crop_iter(support));
    }

    /**
     * Creates list of segments that are intersecting with the support segment
     * @param support
     * @return: List of intersecting segments
     */
    private ArrayList<Segment> crop_iter(Segment support)
    {
        ArrayList<Segment> segments = new ArrayList<Segment>();

        float start = 0;
        float end = 0;
        for (Segment segment: this.set)
        {
            // If the segments intersect
            if (segment.contains(support))
            {
                segments.add(segment);
            }
        }

        return segments;
    }

    /**
     * Calculates the boundaries of the com.eyehearyouspeak.transcription.Timeline
     * @return: Float array of the boundaries (first entry is the start, second entry is the end)
     */
    private float[] findBoundaries()
    {
        float[] boundaries = new float[2];
        Float start = Float.POSITIVE_INFINITY;
        Float end = Float.NEGATIVE_INFINITY;
        for (Segment segment: this.set)
        {
            if (segment.getStart() < start)
            {
                boundaries[0] = segment.getStart();
                start = boundaries[0];
            }
            if (segment.getEnd() > end)
            {
                boundaries[1] = segment.getEnd();
                end = boundaries[1];
            }
        }

        return boundaries;
    }

    /**
     * Calculates the duration of the entire timeline
     * @return: The duration (in seconds) of the timeline
     */
    public float getDuration()
    {
        return timeline_boundaries[1] - timeline_boundaries[0];
    }
}
