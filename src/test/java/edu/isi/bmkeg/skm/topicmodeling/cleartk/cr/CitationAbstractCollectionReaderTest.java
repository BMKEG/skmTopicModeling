package edu.isi.bmkeg.skm.topicmodeling.cleartk.cr;

import static java.util.Arrays.asList;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.util.CasCreationUtils;
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
import org.uimafit.util.CasUtil;

import edu.isi.bmkeg.digitalLibrary.uimaTypes.citations.ArticleCitation;
import edu.isi.bmkeg.skm.topicmodeling.cleartk.cr.CitationAbstractCollectionReader;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml","/testApplicationContext-controllerVpdmfPopulated.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.
public class CitationAbstractCollectionReaderTest {

	private static String test_corpus_name = "testCorpus"; 
	private static int test_corpus_cnt = 41; 
	private static int test_corpus_no_doc_cnt = 2; 
	
	@Autowired
	private BmkegProperties prop;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void testCitationsAbstractCollectionReader() throws Exception {
		
		CollectionReader cr = CitationAbstractCollectionReader.getCollectionReader(
				test_corpus_name,
				prop.getDbUser(),
				prop.getDbPassword(),
				prop.getDbUrl());

		int i = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				ArticleCitation cit = (ArticleCitation) CasUtil.selectSingle(cas, CasUtil.getType(cas, ArticleCitation.class));
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getVpdmfId() > 0);
				Assert.assertTrue(cit.getPmid() > 0);
				
				i++;	
								
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		Assert.assertEquals(test_corpus_cnt, i);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);				

	}
}
