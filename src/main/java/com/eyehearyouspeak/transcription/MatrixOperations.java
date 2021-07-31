package com.eyehearyouspeak.transcription;

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

    /**
     * Grabs the maximum element of the vector
     * @param vec: Vector that will be operated on
     * @return: Maximum value of vec
     */
    public static float getMaxElement(float[] vec)
    {
        float max = 0f;

        for (float v : vec)
        {
            if (max < Math.abs(v)) max = Math.abs(v);
        }

        return max;
    }

    /**
     * Grabs the index of the maximum element of the vector
     * @param vec: Vector that will be operated on
     * @return: Index of the maximum value of vec
     */
    public static int getMaxElementIdx(int[] vec)
    {
        int max = Integer.MIN_VALUE;
        int max_idx = -1;

        for (int i = 0; i < vec.length; i++)
        {
            if (max < vec[i])
            {
                max = vec[i];
                max_idx = i;
            }
        }

        return max_idx;
    }

    /**
     * Calculates the average value of the entire vector
     * @param vec: Vector that will be operated on
     * @return: The average value of vec
     */
    public static float getAverageElement(float[] vec)
    {
        float avg = 0f;

        for (float v : vec)
        {
            avg += v;
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

    /**
     * Calculates the element wise operation of a - b
     * @param a : Vector that will be subtracted from by b
     * @param b : Matrix that will subtract from a
     * @return: The matrix a-b
     */
    public static float[][] subMatrixFromVector(Float[] a, Float[][] b)
    {
        // Make sure the two vectors are the same length
        if (a.length != b[0].length) throw new IllegalStateException("Misaligned dimensions!");

        float[][] result = new float[b.length][a.length];
        for (int i = 0; i < b.length; i++)
        {
            Float[] row = b[i];
            for (int j = 0; j < row.length; j++)
            {
                result[i][j] = a[j] - b[i][j];
            }
        }

        return result;
    }

    /**
     * Sums all elements of the vector
     * @param a: Vector that will have its elements summed
     * @return: The sum of all elements of a
     */
    public static float sumVector(float[] a)
    {
        float result = 0;
        for (float v : a)
        {
            result += v;
        }
        return result;
    }

    /**
     * Raises all elements of the matrix to the given power
     * @param a: Matrix that will be operated on
     * @param b: Value that each element will be raised to
     * @return: All elements of a raised to the bth power
     */
    public static float[][] raiseMatrixToPower(float[][] a, float b)
    {
        float[][] result = new float[a.length][a[0].length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                result[i][j] = (float) Math.pow(a[i][j], b);
            }
        }

        return result;
    }

    /**
     * Raises all elements of a to the power b
     * @param a: Vector that will be operated on
     * @param b: The power that each element will be raised to
     * @return: Vector that contains all elements of a raised to b
     */
    public static float[] raiseVectorToPower(float[] a, float b)
    {
        float[] result = new float[a.length];
        for (int i = 0; i < a.length; i++)
        {
            result[i] = (float) Math.pow(a[i], b);
        }

        return result;
    }

    /**
     * Sums all the columns of the matrix.
     * @param a: Matrix that will be operated on
     * @return: The row wise summation of the matrix
     */
    public static float[] sumColumns(float[][] a)
    {
        float[] result = new float[a.length];
        float temp_result;
        for (int i = 0; i < a.length; i++)
        {
            temp_result = 0f;
            for (int j = 0; j < a[0].length; j++)
            {
                temp_result += a[i][j];
            }
            result[i] = temp_result;
        }

        return result;
    }

    /**
     * Returns the minimum element of the array
     * @param arr: Array that will be checked
     * @return: The minimum value of row
     */
    public static float getMinElement(float[] arr)
    {
        float min = Float.POSITIVE_INFINITY;

        for (float v : arr)
        {
            if (min > v) min = v;
        }

        return min;
    }

    /**
     * Returns the index in the array where the given value is located
     * @param arr: Array that contains the known value
     * @param known_value: Value in array that the index for is desired. MUST BE IN ARRAY
     * @return: The index of known_value within arr
     */
    public static float getIndexForElement(float[] arr, float known_value)
    {
        for (int i = 0; i < arr.length; i++)
        {
            if (known_value == arr[i]) return i;
        }

        // If value is not in the array, throw an error
        throw new IllegalStateException("Array did not contain the known value.");
    }
}
