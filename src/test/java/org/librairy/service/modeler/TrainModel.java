package org.librairy.service.modeler;

import cc.mallet.topics.CSVReader;
import cc.mallet.topics.LDALauncher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.librairy.service.modeler.service.PipeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class TrainModel {

    private static final Logger LOG = LoggerFactory.getLogger(TrainModel.class);

    private LDALauncher launcher;

    @Before
    public void setup(){

        LOG.info("ready");
        this.launcher = new LDALauncher();

        PipeBuilder pipeBuilder = new PipeBuilder();
        pipeBuilder.setNlpHost("localhost");
        pipeBuilder.setNlpPort(65111);

        CSVReader csvReader = new CSVReader();
        csvReader.setPipeBuilder(pipeBuilder);

        this.launcher.setCsvReader(csvReader);

    }

    @Test
//    @Ignore
    public void buildCorpus() throws IOException {

        String file         = "src/test/resources/input/sample.csv";
        String outputDir    = "target/output";
        String regEx        = "(.*);;(.*);;(.*)";
        Integer textIndex   = 3;
        Integer labelIndex  = 2;
        Integer idIndex     = 1;

        launcher.train(file,outputDir,regEx,textIndex,labelIndex,idIndex);

    }

    @Test
//    @Ignore
    public void buildSample() throws IOException {

        String file         = "src/test/resources/input/sample.txt";
        String outputDir    = "target/output";
        String regEx        = "(.*)\t(.*)\t(.*)";
        Integer textIndex   = 3;
        Integer labelIndex  = 2;
        Integer idIndex     = 1;

        launcher.train(file,outputDir,regEx,textIndex,labelIndex,idIndex);

    }
}
