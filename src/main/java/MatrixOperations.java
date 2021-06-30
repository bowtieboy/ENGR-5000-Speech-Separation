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
}
