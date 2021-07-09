import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.util.ArrayList;

public class Speaker
{
    private String name;
    private ArrayList<Instance> embeddings;
    private ArrayList<Attribute> attributes;

    public Speaker(String name, Float[][] embeddings)
    {
        this.name = name;

        this.attributes = new ArrayList<>();
        this.attributes.add(new Attribute("name"));
        this.attributes.add(new Attribute("embeddings"));

        this.embeddings = convertToInstances(embeddings);
    }

    // Getters
    public String getName()
    {
        return name;
    }
    public ArrayList<Instance> getEmbeddings()
    {
        return embeddings;
    }

    private ArrayList<Instance> convertToInstances(Float[][] mat)
    {
        // Create the list of instances that will be returned
        ArrayList<Instance> instance_list = new ArrayList<>();


        return instance_list;
    }
}
