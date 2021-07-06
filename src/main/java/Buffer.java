import java.util.Arrays;

public class Buffer
{
    private float[] buff;
    private int index;
    private int max_len;

    public Buffer(int length)
    {
        this.buff = new float[length];
        Arrays.fill(this.buff, 0);
        this.max_len = length;
        this.index = 0;
    }

    /**
     * Puts a new value into the buffer. If the buffer is full, itll push all values back by 1
     * @param new_val: New value that will be pushed onto the stack
     */
    public void push (float new_val)
    {
        for (int i = this.max_len - 1; i > 0; i--)
        {
            this.buff[i] = this.buff[i - 1];
        }
        this.buff[0] = new_val;
        if (this.index < this.max_len - 1) index ++;
    }

    public float sumBuffer()
    {
        float sum = 0;
        for (int i = 0; i < this.max_len; i++)
        {
            sum += this.buff[i];
        }
        return sum;
    }

    public int getIndex()
    {
        return this.index;
    }
}
