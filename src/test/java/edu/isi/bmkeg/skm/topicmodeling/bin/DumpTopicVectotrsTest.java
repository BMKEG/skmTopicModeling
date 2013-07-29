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
public class DumpTopicVectotrsTest {

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
		
		File inputFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/ldaModel.dat").getFile();
		File pFile = new File("target/dt.txt");
		File idFile = new File("target/ids.txt");
		
		System.out.println("DP Outputfile: " + pFile.getAbsolutePath());
		if (pFile.exists()) {
			
			System.out.println("outputFile already exists - deleting it");
			pFile.delete();
			
		}

		System.out.println("ID Outputfile: " + idFile.getAbsolutePath());
		if (idFile.exists()) {
			
			System.out.println("outputFile already exists - deleting it");
			idFile.delete();
			
		}

		String[] args = new String[] { 
				"-m", inputFile.getAbsolutePath(), 
				"-p", pFile.getAbsolutePath(), 
				"-id", idFile.getAbsolutePath() 
				};

		DumpTopicVectors.main(args);

		Assert.assertTrue(pFile.exists());
		Assert.assertTrue(idFile.exists());
		
	}
}
