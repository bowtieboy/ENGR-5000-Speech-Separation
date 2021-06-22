import ai.djl.*;

import java.io.IOException;

public class Test {

    public static void main (String args[]) throws MalformedModelException, IOException
    {
        OverlappingSpeech model = new OverlappingSpeech(".\\src\\main\\resources");
    }
}
