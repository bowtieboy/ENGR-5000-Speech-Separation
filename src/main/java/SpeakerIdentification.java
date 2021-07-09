import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

public class SpeakerIdentification
{
    private IBk knn;
    private ArrayList<Speaker> speakers;
    private Instances instances;

    public SpeakerIdentification(ArrayList<Speaker> initial_speakers)
    {
        // Store initial list of speakers
        this.speakers = initial_speakers;

        // Add initial list of speakers to the instances object

        // Initialize knn to have as many clusters as there are speakers
        this.knn = new IBk(initial_speakers.size());
    }

    /**
     * Adds a new speaker to the knn classifier list
     * @param new_speaker: New speaker being added to the classifier
     * @return: The number of clusters in the algorithm
     */
    public int addSpeaker(Speaker new_speaker)
    {
        this.speakers.add(new_speaker);
        this.knn.setKNN(this.speakers.size());
        // Add the instances to the list
        for (Instance i: new_speaker.getEmbeddings())
        {
            this.instances.add(i);
        }
        return this.knn.getKNN();
    }
}
