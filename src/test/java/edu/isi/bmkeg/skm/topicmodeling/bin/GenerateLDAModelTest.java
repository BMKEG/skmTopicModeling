package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.
public class GenerateLDAModelTest {

	@Autowired
	ApplicationContext ctx;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void test() throws Exception {
		
		File inputFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/inputInstances.dat").getFile();
		File outputModelfile = new File("target/ldaModel.dat");
		File outputDTfile = new File("target/ldaDt.txt");
		File outputTopicfile = new File("target/ldaTopic.txt");
		
		System.out.println("OutputModelfile: " + outputModelfile.getAbsolutePath());
		if (outputModelfile.exists()) {
			
			System.out.println("outputDTfile already exists - deleting it");
			outputModelfile.delete();
			
		}

		System.out.println("outputDTfile: " + outputDTfile.getAbsolutePath());
		if (outputDTfile.exists()) {
			
			System.out.println("outputDTfile already exists - deleting it");
			outputDTfile.delete();
			
		}

		System.out.println("outputTopicfile: " + outputTopicfile.getAbsolutePath());
		if (outputTopicfile.exists()) {
			
			System.out.println("outputTopicfile already exists - deleting it");
			outputTopicfile.delete();
			
		}

		String[] args = new String[] { 
				"-i", inputFile.getAbsolutePath(), 
				"-k", "10", 
				"-m", outputModelfile.getAbsolutePath() ,
				"-dt", outputDTfile.getAbsolutePath(),
				"-t", outputTopicfile.getAbsolutePath()
				};

		GenerateLDAModel.main(args);

		Assert.assertTrue(outputModelfile.exists());
		
	}
}
