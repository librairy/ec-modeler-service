package org.librairy.service.modeler.service;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LDALauncher;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.model.Topic;
import org.librairy.service.modeler.facade.model.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MyService implements ModelerService {

    private static final Logger LOG = LoggerFactory.getLogger(MyService.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    private String resourceFolder;

    @Autowired
    LDALauncher ldaLauncher;

    @Autowired
    PipeBuilder pipeBuilder;

    private Pipe pipe;
    private TopicInferencer topicInferer;
    private ArrayList topics;
    private Map<Integer, List<Word>> words;

    @PostConstruct
    public void setup() throws Exception {

        this.topicInferer               = ldaLauncher.getTopicInferencer(resourceFolder);

        ParallelTopicModel topicModel   = ldaLauncher.getTopicModel(resourceFolder);


        this.pipe   = pipeBuilder.build();
        this.topics = new ArrayList<>();
        this.words  = getTopWords(topicModel,50);


        IntStream.range(0,topicModel.getNumTopics()).forEach(id -> {

            Topic topic = new Topic();

            topic.setId(id);
            topic.setName((String)topicModel.getTopicAlphabet().lookupObject(id));

            topic.setDescription(words.get(id).stream().limit(10).map(w->w.getValue()).collect(Collectors.joining(",")));
            topics.add(topic);

        });



        LOG.info("Service initialized");
    }

    public Map<Integer,List<Word>> getTopWords(ParallelTopicModel topicModel, int numWords) throws Exception {

        int numTopics = topicModel.getNumTopics();
        Alphabet alphabet = topicModel.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicSortedWords = topicModel.getSortedWords();

        Map<Integer,List<Word>> result = new HashMap<>();

        for (int topic = 0; topic < numTopics; topic++) {

            TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);

            Double totalWeight = sortedWords.stream().map(w -> w.getWeight()).reduce((w1, w2) -> w1 + w2).get();

            // How many words should we report? Some topics may have fewer than
            //  the default number of words with non-zero weight.
            int limit = numWords;
            if (sortedWords.size() < numWords) { limit = sortedWords.size(); }

            List<Word> words = new ArrayList<>();

            Iterator<IDSorter> iterator = sortedWords.iterator();
            for (int i=0; i < limit; i++) {
                IDSorter info = iterator.next();
                words.add(new Word(String.valueOf(alphabet.lookupObject(info.getID())),info.getWeight()/totalWeight));
            }
            result.put(topic,words);
        }

        return result;
    }


    @Override
    public List<Double> inference(String s) throws AvroRemoteException {

        if (Strings.isNullOrEmpty(s)) return Collections.emptyList();

        String data = s;
        String name = "";
        String source = "";
        String target = "";
        Integer numIterations = 100;

        Instance rawInstance = new Instance(data,target,name,source);

        Instance instance = this.pipe.instanceFrom(rawInstance);

        int thinning = 1;
        int burnIn = 0;
        double[] topicDistribution = topicInferer.getSampledDistribution(instance, numIterations, thinning, burnIn);
        LOG.info("Topic Distribution of: " + s.substring(0,10)+ ".. " + Arrays.toString(topicDistribution));

        return Doubles.asList(topicDistribution);
    }

    @Override
    public List<Topic> topics() throws AvroRemoteException {
        return topics;
    }

    @Override
    public List<Word> words(int topicId, int maxWords) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        return words.get(topicId).stream().limit(maxWords).collect(Collectors.toList());

    }
}
