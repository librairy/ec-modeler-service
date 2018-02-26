package cc.mallet.topics;

import cc.mallet.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class LDALauncher {

    private static final Logger LOG = LoggerFactory.getLogger(LDALauncher.class);

    @Autowired
    CSVReader csvReader;

    public void setCsvReader(CSVReader csvReader) {
        this.csvReader = csvReader;
    }

    public void train(String corpusFile, String outputDir, String regEx, int textIndex, int labelIndex, int idIndex) throws IOException {

        File outputDirFile = Paths.get(outputDir).toFile();
        if (!outputDirFile.exists()) outputDirFile.mkdirs();

        Double alpha        = 0.1;
        Double beta         = 0.001;
        int numTopics       = 15;
        Integer numTopWords = 50;
        Integer numIterations = 1000;



        //labeledLDA.setRandomSeed(100);

        InstanceList instances = csvReader.getInstances(corpusFile, regEx,textIndex, labelIndex, idIndex);

        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        ParallelTopicModel model = new ParallelTopicModel(numTopics, numTopics*alpha, beta);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(numIterations);
        model.estimate();

        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();


        FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
        LabelSequence topics = model.getData().get(0).topicSequence;

        Formatter out = new Formatter(new StringBuilder(), Locale.US);
        for (int position = 0; position < tokens.getLength(); position++) {
            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
        }
        System.out.println(out);


        // Estimate the topic distribution of the first instance,
        //  given the current Gibbs state.
        double[] topicDistribution = model.getTopicProbabilities(0);

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();


        // Show top 5 words in topics with proportions for the first document
        for (int topic = 0; topic < numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

            out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
            int rank = 0;
            while (iterator.hasNext() && rank < 5) {
                IDSorter idCountPair = iterator.next();
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                rank++;
            }
            System.out.println(out);
        }


        model.write(Paths.get(outputDir, "model-parallel.bin").toFile());


        PrintWriter e1 = new PrintWriter(Paths.get(outputDir, "diagnostic.txt").toFile());
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, numTopWords);
        e1.println(diagnostics.toXML());
        e1.close();

        //
        ObjectOutputStream e2;
        try {
            e2 = new ObjectOutputStream(new FileOutputStream(Paths.get(outputDir, "model-inferencer.bin").toFile()));
            e2.writeObject(model.getInferencer());
            e2.close();
        } catch (Exception var6) {
            LOG.warn("Couldn\'t create inferencer: " + var6.getMessage());
        }
    }

    public TopicInferencer getTopicInferencer(String baseDir) throws Exception {
        return TopicInferencer.read(Paths.get(baseDir,"model-inferencer.bin").toFile());
    }

    public ParallelTopicModel getTopicModel(String baseDir) throws Exception {
        return ParallelTopicModel.read(Paths.get(baseDir,"model-parallel.bin").toFile());
    }

}
