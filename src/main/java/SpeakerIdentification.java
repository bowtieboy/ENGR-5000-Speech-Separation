import java.util.ArrayList;

// TODO: Finish implementing the classification of speakers. Add way to classify a batch of embeddings. Create public function that will return list of strings for who spoke during each embedding
public class SpeakerIdentification
{
    private ArrayList<Speaker> speakers;
    private ArrayList<String> names;
    private int clusters;

    public SpeakerIdentification(ArrayList<Speaker> initial_speakers)
    {
        this.speakers = initial_speakers;
        this.clusters = speakers.size();

        // Create list of names
        ArrayList<String> names = new ArrayList<>();
        for (Speaker s: speakers)
        {
            names.add(s.getName());
        }

        this.names = names;

    }

    // Getters
    public ArrayList<Speaker> getSpeakers()
    {
        return this.speakers;
    }
    public int getClusters()
    {
        return this.clusters;
    }

    /**
     * Adds a new speaker to the classifier
     * @param new_speaker: The new speaker that will be added
     * @return: How many speakers are now in the system. Will return -1 if the speaker is already in the system
     */
    public int addNewSpeaker(Speaker new_speaker)
    {
        // Check to make sure the speaker isnt already in the system
        for (String speaker: this.names)
        {
            if (speaker.equalsIgnoreCase(new_speaker.getName())) return -1;
        }

        // If the speaker is not in the list
        this.speakers.add(new_speaker);
        this.clusters++;
        return this.clusters;
    }

    // TODO: Implement this function
    public ArrayList<String> identifySpeakers(Float[][] embeddings)
    {
        ArrayList<String> speakers = new ArrayList<>();

        return speakers;
    }

    /**
     * Calculates the likelihood that the given embedding belongs to one of the known speakers
     * @param embedding: Unknown speaker embedding
     * @return: Array of speaker probabilities
     */
    private float[] classify(Float[] embedding)
    {
        float[] probabilities = new float[this.clusters];

        // Calculate the distances between this embedding and each speakers known embeddings
        ArrayList<float[]> distances = new ArrayList<>();
        for (Speaker s: this.speakers)
        {
            distances.add(euclideanDistance(embedding, s.getEmbeddings()));
        }

        return probabilities;
    }

    /**
     * Calculates the distance between the first vector and every row of the second vector
     * @param a: Vector that will be subtracted from
     * @param b: Matrix that contains each row that will subtract from a
     * @return: The distance between all of the vectors
     */
    private float[] euclideanDistance(Float[] a, Float[][] b)
    {
        return MatrixOperations.raiseVectorToPower(MatrixOperations.sumColumns(MatrixOperations.raiseMatrixToPower(
                MatrixOperations.subMatrixFromVector(a, b), 2f)), 0.5f);
    }
}
