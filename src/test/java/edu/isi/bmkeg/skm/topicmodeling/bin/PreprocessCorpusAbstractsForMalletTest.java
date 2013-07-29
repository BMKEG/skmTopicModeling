package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

import edu.isi.bmkeg.utils.springContext.BmkegProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml","/testApplicationContext-controllerVpdmfPopulated.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.
public class PreprocessCorpusAbstractsForMalletTest {

	private static String test_corpus_name = "testCorpus"; 
	private static int test_corpus_cnt = 39; 
	
	@Autowired
	private BmkegProperties prop;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void testNoStopwords() throws Exception {
		
		String outputFn = "target/test.dat";
		String idsMappingFn = "target/idsMapping.txt";
		
		File file = new File(outputFn);
		
		System.out.println("Output file: " + file.getAbsolutePath());
		if (file.exists()) {
			
			System.out.println("Output file already exists - deleting it");
			file.delete();
			
		}

		File idsMappinfFile = new File(idsMappingFn);
		
		System.out.println("Output idsMapping file: " + idsMappinfFile.getAbsolutePath());
		if (idsMappinfFile.exists()) {
			
			System.out.println("Output idsMapping file already exists - deleting it");
			idsMappinfFile.delete();
			
		}

		String[] args = new String[] { 
				"-corpus", test_corpus_name, 
				"-f", outputFn, 
				"-l", prop.getDbUser(), 
				"-p", prop.getDbPassword(), 
				"-db", prop.getDbUrl(),
				"-ids", idsMappingFn
				};

		PreprocessCorpusAbstractsForMallet.main(args);

		Assert.assertTrue(file.exists());
		
		InstanceList instances = InstanceList.load (file);
		
		Assert.assertEquals(test_corpus_cnt, instances.size());
		
		for (Instance inst : instances) {

			FeatureSequence features = (FeatureSequence) inst.getData();
			
			Assert.assertNotNull(features);
			
			System.out.print(inst.getName() + ":");
			for (int i = 0; i < features.size(); i++) {
				System.out.print(" " + features.getObjectAtPosition(i));
			}
			System.out.println();
			
		}
		
		Assert.assertTrue(idsMappinfFile.exists());
		
		BufferedReader reader = new BufferedReader(new FileReader(idsMappinfFile));
		
		String line = reader.readLine();	// Reads heading line
		System.out.println(line);
		
		int counter = 0;
		line = reader.readLine();
		while (line != null) {			
			String[] ids = line.split("\t");
			
			Assert.assertEquals(2, ids.length);
			
			long id1 = Long.parseLong(ids[0]);
			long id2 = Long.parseLong(ids[1]);
			System.out.println(id1 + "\t" + id2);
			
			counter++;
			line = reader.readLine();
		}
		
		Assert.assertEquals(test_corpus_cnt, counter);
	}

	@Test
	public void testWithStopwords() throws Exception {
		
		String outputFn = "target/test.dat";
		
		File file = new File(outputFn);
		
		System.out.println("Output file: " + file.getAbsolutePath());
		if (file.exists()) {
			
			System.out.println("Output file already exists - deleting it");
			file.delete();
			
		}

		String[] args = new String[] { 
				"-corpus", test_corpus_name, 
				"-f", outputFn, 
				"-l", prop.getDbUser(), 
				"-p", prop.getDbPassword(), 
				"-db", prop.getDbUrl(),
				"-keepstopwords"
				};

		PreprocessCorpusAbstractsForMallet.main(args);

		Assert.assertTrue(file.exists());
		
		InstanceList instances = InstanceList.load (file);
		
		Assert.assertEquals(test_corpus_cnt, instances.size());
		
		for (Instance inst : instances) {

			FeatureSequence features = (FeatureSequence) inst.getData();
			
			Assert.assertNotNull(features);
			
			System.out.print(inst.getName() + ":");
			for (int i = 0; i < features.size(); i++) {
				System.out.print(" " + features.getObjectAtPosition(i));
			}
			System.out.println();
			
		}
		
	}
}