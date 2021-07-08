public class Segment
{
    public static final float SEGMENT_PRECISION = (float) Math.pow(10, -6);
    private float start;
    private float end;

    public Segment(float start, float end)
    {
        this.start = start;
        this.end = end;
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

    /**
     * A segment is empty if the end is smaller than the start, or the duration is smaller than 1us
     * @return Whether the segment is empty or not
     */
    public boolean isNotEmpty()
    {
        return (this.end - this.start) > SEGMENT_PRECISION;
    }

    /**
     * Determines the length of the segment
     * @return: The segment duration
     */
    public float getDuration()
    {
        if (isNotEmpty())
        {
            return this.end - this.start;
        }
        else return 0;
    }

    /**
     * Calculates the middle of the segment
     * @return: The middle of the segment
     */
    public float getMiddle()
    {
        return 0.5f * (this.start + this.end);
    }

    /**
     * Creates a copy of the segment
     * @return: A new segment with the same parameters
     */
    public Segment copy()
    {
        return new Segment(this.start, this.end);
    }

    /**
     * Determine if one segment completely contains another
     * @param other: Segment that is possibly within the current one
     * @return: Whether other is contained within this
     */
    public boolean contains(Segment other)
    {
        return (this.start <= other.start) && (this.end >= other.end);
    }

    /**
     * Combines the two sections entirely
     * @param other: Other segment that will be combined
     * @return: Combination of the two segments
     */
    public Segment combine(Segment other)
    {
        float start = Math.max(this.start, other.start);
        float end = Math.min(this.end, other.end);
        return new Segment(start, end);
    }

    /**
     * Determines if two segments intersect
     * @param other: Segment that will be checked against for intersection
     * @return If the two segments intersect or not
     */
    public boolean intersects(Segment other)
    {
        return (this.start < other.start && other.start < this.end - SEGMENT_PRECISION) ||
                (this.start > other.start && this.start < other.end - SEGMENT_PRECISION) ||
                (this.start == other.start);
    }

    /**
     * Checkl if segment overlaps a given time
     * @param t: Time, in seconds
     * @return: If segment overlaps t
     */
    public boolean overlap(float t)
    {
        return this.start <= t && this.end >= t;
    }

    /**
     * Creates the union of the two segments
     * @param other: Other segment being or-ed
     * @return: The union of the two segments
     */
    public Segment or(Segment other)
    {
        if (!isNotEmpty()) return other;
        else if (!other.isNotEmpty()) return this;
        else
        {
            float start = Math.min(this.start, other.start);
            float end = Math.max(this.end, other.end);
            return new Segment(start, end);
        }
    }

    /**
     * Creates a segment based on the gap of the two segments
     * @param other: Other segment being xor-ed
     * @return: The gap between the two segments
     */
    public Segment xor(Segment other)
    {
        if (!isNotEmpty() || !other.isNotEmpty()) throw new IllegalArgumentException("The gap between a segment and an empty" +
                                                                            " segment is undefined.");
        float start = Math.min(this.end, other.end);
        float end = Math.max(this.start, other.start);
        return new Segment(start, end);
    }

}
