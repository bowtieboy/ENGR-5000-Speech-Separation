package com.eyehearyouspeak.transcription;

import java.util.ArrayList;


public class SpeakerIdentification
{
    private ArrayList<Speaker> speakers;
    private ArrayList<String> names;
    private int clusters;

    public SpeakerIdentification()
    {
        this.speakers = new ArrayList<Speaker>();
        this.names = new ArrayList<String>();
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
    public ArrayList<String> getNames()
    {
        return this.getNames();
    }

    /**
     * Adds a new speaker to the classifier
     * @param new_speaker: The new speaker that will be added
     * @return: How many speakers are now in the system. Will return -1 if the speaker is already in the system
     */
    public int addNewSpeaker(Speaker new_speaker)
    {
        // Check to make sure the speaker isnt already in the system
        if (this.names != null)
        {
            for (String speaker: this.names)
            {
                if (speaker.equalsIgnoreCase(new_speaker.getName())) return -1;
            }
        }

        // If the speaker is not in the list
        this.speakers.add(new_speaker);
        this.clusters++;
        return this.clusters;
    }

    /**
     * Determines the most likely speaker for each embedding
     * @param embeddings: Matrix of floats, where each row is an embedding vector
     * @return: The most likely speaker for each embedding
     */
    public ArrayList<String> identifySpeakers(Float[][] embeddings)
    {
        ArrayList<String> speakers = new ArrayList<>();

        // Classify each embedding vector in the matrix
        float[] probabilities;
        float most_likely_idx;
        for (Float[] emb: embeddings)
        {

            // Grab the probabilities from the classifier
            probabilities = classify(emb);

            // Grab the minimum speakers idx
            most_likely_idx = MatrixOperations.getIndexForElement(probabilities,
                                                                        MatrixOperations.getMinElement(probabilities));

            // Add the name of the speaker to the list
            speakers.add(this.speakers.get((int) most_likely_idx).getName());

        }

        return speakers;
    }

    /**
     * Calculates the likelihood that the given embedding belongs to one of the known speakers
     * @param embedding: Unknown speaker embedding
     * @return: Array of speaker probabilities
     */
    private float[] classify(Float[] embedding)
    {
        // Calculate the distances between this embedding and each speakers known embeddings
        ArrayList<float[]> distances = new ArrayList<>();
        for (Speaker s: this.speakers)
        {
            distances.add(euclideanDistance(embedding, s.getEmbeddings()));
        }

        // Determine the minimum n distances for each row, where n is the number of known speakers
        ArrayList<float[]> min_distances = new ArrayList<>();
        float current_min;
        float current_idx;
        for (float[] row: distances)
        {
            float[] current_min_distances = new float[this.clusters];
            for (int i = 0; i < this.clusters; i++)
            {
                // Grab the minimum value and the index for it
                current_min = MatrixOperations.getMinElement(row);
                current_idx = MatrixOperations.getIndexForElement(row, current_min);

                // Store the values
                current_min_distances[i] = current_min;

                // Set the index to be infinity so it is no longer the minimum
                row[(int) current_idx] = Float.POSITIVE_INFINITY;
            }
            // Store the current minimum distances
            min_distances.add(current_min_distances);

        }

        // Determine the average lowest value for each of the speakers
        float[] avg_min = new float[this.clusters];
        for (int i = 0; i < avg_min.length; i++)
        {
            avg_min[i] = MatrixOperations.getAverageElement(min_distances.get(i));
        }

        // Return the average minimums
        return avg_min;
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

    public String[] getMostLikely(ArrayList<String> names, int num_speakers)
    {
        String[] speaker_names = new String[num_speakers];

        ArrayList<String> unique_names = getUniqueNames(names);
        // If the number of unique speakers is exactly equal to the number of unique, return the unique speakers
        if (unique_names.size() == num_speakers)
        {
            for (int i = 0; i < num_speakers; i++)
            {
                speaker_names[i] = unique_names.get(i);
            }
            return speaker_names;
        }

        // If there are more unique speakers than diarized speaker (error within the identification), sum the unique
        // speaker embeddings to get the most likely speakers
        int[] occurrences = new int[unique_names.size()];
        for (String name: names)
        {
            for (int i = 0; i < unique_names.size(); i++)
            {
                if (name == unique_names.get(i)) occurrences[i] += 1;
            }
        }

        // Use how often the names occurred to determine the most likely speakers
        int max_idx;
        // If there are more num_speakers than unique speakers, use the regular allocation method
        if (num_speakers < unique_names.size())
        {
            for (int i = 0; i < speaker_names.length; i++)
            {
                max_idx = MatrixOperations.getMaxElementIdx(occurrences);
                speaker_names[i] = unique_names.get(max_idx);
                occurrences[max_idx] = Integer.MIN_VALUE;
            }
        }
        else
        {
            // If there are less unique speakers than the diarization separation, use the speaker with the most ids
            // as the repeated speaker
            int repeated_idx = MatrixOperations.getMaxElementIdx(occurrences);
            for (int i = 0; i < speaker_names.length - 1; i++)
            {
                max_idx = MatrixOperations.getMaxElementIdx(occurrences);
                speaker_names[i] = unique_names.get(max_idx);
                occurrences[max_idx] = Integer.MIN_VALUE;
            }
            speaker_names[speaker_names.length - 1] = unique_names.get(repeated_idx);
        }


        return speaker_names;
    }

    /**
     * Parses through the input and creates a new list containing only the unique names
     * @param names: List of all names
     * @return: Names that occur in the input at least once
     */
    private ArrayList<String> getUniqueNames(ArrayList<String> names)
    {
        ArrayList<String> unique_names = new ArrayList<>();
        for (String name: names)
        {
            if (!unique_names.contains(name))
            {
                unique_names.add(name);
            }
        }

        return unique_names;
    }

}
