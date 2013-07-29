package edu.isi.bmkeg.skm.topicmodeling.testutil;

import java.net.URL;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Corpus;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.lapdf.dao.vpdmf.VpdmfFtdDao;

/**
 * Loads Citations and Documents from an XML file. This class was originally
 * intended to import citations and documents from the SOLR legacy Digital
 * Library system. The XML file was generated with the class:
 * /SciKnowMineTopicModeling/src/main/java/edu/isi/bmkeg/uima/cpe/CitationsAndDocumentsDumpPipeline.java
 * 
 */
public class CitationsAndDocumentsLoader {

	@Autowired
	private VpdmfCitationsDao cdao;

    public void setCitationsDao(VpdmfCitationsDao citationsDao) {
        this.cdao = citationsDao;
    }

	@SuppressWarnings("unused")
	@Autowired
	private VpdmfFtdDao ddao;

    public void setDocumentsDao(VpdmfFtdDao documentsDao) {
        this.ddao = documentsDao;
    }

    public void loadCitationsAndDocuments(URL file, String corpusName, String corpusDesc) throws Exception {
    	
    	// Checks that there is no corpus with that name
    	Corpus c1 = cdao.findCorpusByName(corpusName);
    	
    	if (c1 != null) {
    		throw new Exception("Corpus already exists: " + corpusName);
    	}
    	
    	CitationAndDocumentSource source = new CitationAndDocumentSource(file);
    	
    	// Creates corpus
    	c1 = new Corpus();
    	c1.setName(corpusName);
    	c1.setDescription(corpusDesc);
    	
    	cdao.insertCorpus(c1);
    	    	
    	ArrayList<Corpus> cs = new ArrayList<Corpus>();
    	cs.add(c1);
    	
    	while (source.hasNext()) {
    		ArticleCitation a = source.getNext();
    		
    		System.out.println("Article Title: " + a.getTitle());
    		
    		if (a.getJournal() != null) {
        		Journal j = cdao.findJournalByAbbr(a.getJournal().getAbbr());
        		if (j == null) {
        			j = a.getJournal();
        			cdao.insertJournal(j);
        		}
    			
        		a.setJournal(j);
        		
    		}
    		
    		a.setCorpora(cs);
    		cdao.insertArticleCitation(a);
    		
    		FTD d = a.getFullText();
    		if (d != null) {
    			// TODO implement insertFTD
//    			ddao.insertFullTextDocument(d);
    		}
    	}
    	
    	source.close();    	
    }
}
