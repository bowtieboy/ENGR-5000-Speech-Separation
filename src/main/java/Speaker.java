import java.util.ArrayList;

public class Speaker
{
    private String name;
    private Float[][] embeddings;

    public Speaker(String name, ArrayList<Float[][]> embeddings)
    {
        this.name = name;
        this.embeddings = expandEntries(embeddings);
    }

    // Getters
    public String getName()
    {
        return name;
    }
    public Float[][] getEmbeddings()
    {
        return embeddings;
    }

    private Float[][] expandEntries(ArrayList<Float[][]> emb)
    {
        // Determine number of rows
        int rows = 0;
        for (int i = 0; i < emb.size(); i++)
        {
            rows += emb.get(i).length;
        }

        // Define the new array and assign the values
        int previous_size = 0;
        Float[][] all_emb = new Float[rows][512];
        for (Float[][] mat: emb)
        {
            for (int i = 0; i < mat.length; i++)
            {
                for (int j = 0; j < mat[i].length; j++)
                {
                    all_emb[i + previous_size][j] = mat[i][j];
                }
            }
            previous_size += mat.length;
        }

        return all_emb;
    }

}
