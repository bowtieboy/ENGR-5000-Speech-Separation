public class MatrixOperations
{
    /**
     * Returns a column array (vector) along the given column
     * @param mat: Matrix of data (row x column)
     * @param col: Column that will be turned into an array
     * @return: The column array from the matrix
     */
    public static float[] getColumn(float[][] mat, int col)
    {
        float[] column = new float[mat.length];

        for (int i = 0; i < mat.length; i++)
        {
            column[i] = mat[i][col];
        }

        return column;
    }

    /**
     * Subtracts each element from the constant provided
     * @param vec: Array that will be calculated against
     * @param val: Value that will be subtracted from
     * @return: The resulting subtraction vector
     */
    public static float[] elementWiseSubtraction(float[] vec, float val)
    {
        float[] return_array = new float[vec.length];
        for (int i = 0; i < vec.length; i++)
        {
            return_array[i] = val - vec[i];
        }

        return return_array;
    }

    public static float getMaxElement(float[] vec)
    {
        float max = 0f;

        for (int i = 0; i < vec.length; i++)
        {
            if (max < Math.abs(vec[i])) max = Math.abs(vec[i]);
        }

        return max;
    }

    public static float getAverageElement(float[] vec)
    {
        float avg = 0f;

        for (int i = 0; i < vec.length; i++)
        {
            avg += vec[i];
        }

        return avg / vec.length;
    }

    /**
     * Sums entries of the vector from the start [start_idx, end_idx)
     * @param vec: Vector that will contains entries that will be added
     * @param start_idx: The start index for the summation (inclusive)
     * @param end_idx: The end index for the summation (not inclusive)
     * @return: The sum of all entries of the vector according to the indices.
     */
    public static float sumToIndex(float[] vec, int start_idx, int end_idx)
    {
        float sum = 0f;
        for (int i = start_idx; i < end_idx; i++)
        {
            sum += vec[i];
        }

        return sum;
    }
}
