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
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.utils.springContext.BmkegProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml","/testApplicationContext-controllerVpdmfPopulated.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.
public class RetrieveCitationsDataTest {

	private static int test_ids_cnt = 5; 

	@Autowired
	private BmkegProperties prop;

	@Autowired
	ApplicationContext ctx;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void testToFile() throws Exception {
		
		File idsFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/vpdmfIds.txt").getFile();
		String outputFn = "target/citData.txt";
		
		File file = new File(outputFn);
		
		System.out.println("Output file: " + file.getAbsolutePath());
		if (file.exists()) {
			
			System.out.println("Output file already exists - deleting it");
			file.delete();
			
		}

		String[] args = new String[] { 
				"-ids", idsFile.getAbsolutePath(), 
				"-f", outputFn, 
				"-l", prop.getDbUser(), 
				"-p", prop.getDbPassword(), 
				"-db", prop.getDbUrl(),
				};

		RetrieveCitationsData.main(args);

		Assert.assertTrue(file.exists());
		
		BufferedReader reader =  new BufferedReader(new FileReader(file));

		String line = reader.readLine();	// Reads heading line

		System.out.println(line);
		
		int counter = 0;
		line = reader.readLine();
		while (line != null) {			
			String[] fields = line.split("\t");
			
			Assert.assertEquals(4, fields.length);
			
			long vpdmfId = Long.parseLong(fields[0]);
			long pmid = Long.parseLong(fields[1]);
			String label = fields[2];
			String url = fields[3];
			
			System.out.println(vpdmfId + "\t" + pmid + "\t" + label + "\t" + url);
			
			counter++;
			line = reader.readLine();
		}
		
		Assert.assertEquals(test_ids_cnt, counter);
	}

	@Test
	public void testToStdOut() throws Exception {
		
		File idsFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/vpdmfIds.txt").getFile();

		String[] args = new String[] { 
				"-ids", idsFile.getAbsolutePath(), 
				"-l", prop.getDbUser(), 
				"-p", prop.getDbPassword(), 
				"-db", prop.getDbUrl(),
				};

		RetrieveCitationsData.main(args);

	}

}