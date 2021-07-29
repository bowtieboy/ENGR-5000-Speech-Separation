package com.eyehearyouspeak.transcription;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;

public class DetectionBatchifier implements Batchifier
{
    @Override
    public NDList batchify(NDList[] inputs)
    {
        // Modify the NDArray to get it into the correct shape (batches, data, 1)
        NDArray array = inputs[0].get(0);
        array = array.expandDims(2);
        array.setName("Speech");
        return new NDList(array);
    }

    @Override
    public NDList[] unbatchify(NDList inputs)
    {
        // Return list of size 1
        NDList[] list = new NDList[1];
        list[0] = inputs;
        return list;
    }
}
