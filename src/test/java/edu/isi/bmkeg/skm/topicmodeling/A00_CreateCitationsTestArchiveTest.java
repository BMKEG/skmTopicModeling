package edu.isi.bmkeg.skm.topicmodeling;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.digitalLibrary.bin.AddArticleCitations;
import edu.isi.bmkeg.digitalLibrary.bin.AddArticleCitationsToCorpus;
import edu.isi.bmkeg.digitalLibrary.bin.EditArticleCorpus;
import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.BuildDBBean;
import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml","/testApplicationContext-controllerVpdmf.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.

/**
 * Creates a populated VPDMf archive to be used by other test cases.
 * 
 * If running the whole test suite, this test should run before other tests
 * that rely on this archive.
 * 
 */
public class A00_CreateCitationsTestArchiveTest {

	@Autowired
	ApplicationContext ctx;	

	@Autowired
	private VpdmfCitationsDao cdao;

    public void setCitationsDao(VpdmfCitationsDao citationsDao) {
        this.cdao = citationsDao;
    }

	@Test
	public void createTestCitationsArchiveTest() throws Exception {
		
		File outputFile = new File("target/testCorpus-mysql.zip");
		
//		URL corpusDump = ctx.getResource(
//				"classpath:edu/isi/bmkeg/skm/topicmodeling/corpusDump.xml").getURL();
//
//		CitationsAndDocumentsLoader loader = new CitationsAndDocumentsLoader();
//		
//		loader.setCitationsDao(cdao);
//		
//		loader.loadCitationsAndDocuments(corpusDump, "testCorpus", "Corpus used for Tests");
	
		
		BuildDBBean ctxBuilder = (BuildDBBean) ctx.getBean("dlVpdmfBuilder");
		
		String dbName = ctxBuilder.getUri();
		String login = ctxBuilder.getLogin();
		String password = ctxBuilder.getPassword();
		File inFile = ctxBuilder.getVpdmfArchivePath().getFile();

		File pmidFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/skm/topicmodeling/pmids.txt").getFile();;
		
		String[] args = new String[] { 
				"-name", "testCorpus", "-desc", "Corpus used for Tests", "-owner", "tester", 
				"-regex", "_(.*A.*)\\.pdf",				
				"-db", dbName, "-l", login, "-p", password
				};

		EditArticleCorpus.main(args);

		args = new String[] { 
				pmidFile.getPath(), dbName, login, password
				};

		AddArticleCitations.main(args);		
		
		args = new String[] { 
				pmidFile.getPath(), "testCorpus", dbName, login, password
				};

		AddArticleCitationsToCorpus.main(args);
		
		
		VPDMfKnowledgeBaseBuilder archBuilder = new VPDMfKnowledgeBaseBuilder(
				inFile, login, password, dbName);

		archBuilder.refreshDataToNewArchive(outputFile);
		
	}
}
