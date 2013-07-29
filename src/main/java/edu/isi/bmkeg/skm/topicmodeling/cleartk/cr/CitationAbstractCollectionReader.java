package edu.isi.bmkeg.skm.topicmodeling.cleartk.cr;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.isi.bmkeg.digitalLibrary.dao.CitationsDao;
import edu.isi.bmkeg.digitalLibrary.dao.vpdmf.VpdmfCitationsDao;
import edu.isi.bmkeg.digitalLibrary.uimaTypes.citations.ArticleCitation;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.dao.CoreDaoImpl;

/**
 * This Collection Reader runs over every citation in a given corpus to provide
 * access to the abstract.
 * 
 */
public class CitationAbstractCollectionReader extends JCasCollectionReader_ImplBase {

	private static Logger logger = Logger.getLogger(CitationAbstractCollectionReader.class);
	
	public static final String PARAM_CORPUS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"corpusName");
	@ConfigurationParameter(mandatory = true, description = "The name of the corpus to be read")
	protected String corpusName;

	public static final String PARAM_LOGIN = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"login");
	@ConfigurationParameter(mandatory = true, description = "Login for the Digital Library")
	protected String login;

	public static final String PARAM_PASSWORD = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"password");
	@ConfigurationParameter(mandatory = true, description = "Password for the Digital Library")
	protected String password;

	public static final String PARAM_DB_URL = ConfigurationParameterFactory
			.createConfigurationParameterName(CitationAbstractCollectionReader.class,
					"dbUrl");
	@ConfigurationParameter(mandatory = true, description = "The Digital Library URL")
	protected String dbUrl;

	protected CitationsDao citationsDao;

	protected int count = 0;
	
	protected boolean isConnectedToDb = false;
	
	protected ResultSet rs;

	public static CollectionReader getCollectionReader(
			String corpusName,
			String login,
			String password,
			String dbName
			)
			throws ResourceInitializationException {

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription("uimaTypes.digitalLibrary");
		
		return CollectionReaderFactory.createCollectionReader(
				CitationAbstractCollectionReader.class, typeSystem, 
				PARAM_CORPUS_NAME, corpusName,
				PARAM_LOGIN, login, 
				PARAM_PASSWORD, password, 
				PARAM_DB_URL, dbName
				);
	}

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		
		super.initialize(context);

		try {

			CoreDao coreDao = new CoreDaoImpl();
			coreDao.init(login, password, dbUrl);
			
			citationsDao = new VpdmfCitationsDao(coreDao);

			String sql = "SELECT DISTINCT LiteratureCitation_0__LiteratureCitation.vpdmfId, " +
				     "LiteratureCitation_0__ArticleCitation.pmid, " +
				     "LiteratureCitation_0__LiteratureCitation.abstractText " + 
				    "FROM ViewTable AS LiteratureCitation_0__ViewTable, " +
				     "LiteratureCitation AS LiteratureCitation_0__LiteratureCitation, " +
				     "ArticleCitation AS LiteratureCitation_0__ArticleCitation, " +
				     "Corpus AS Corpus_0__Corpus, " +
				     "Corpus_corpora__resources_LiteratureCitation AS Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0 " + 
				    "WHERE LiteratureCitation_0__ViewTable.viewType LIKE '.LiteratureCitation.%.ArticleCitation.%' AND " + 
				     "LiteratureCitation_0__LiteratureCitation.vpdmfId=LiteratureCitation_0__ViewTable.vpdmfId AND " + 
				     "LiteratureCitation_0__ArticleCitation.vpdmfId=LiteratureCitation_0__LiteratureCitation.vpdmfId AND " +
				     "Corpus_0__Corpus.name = '" + corpusName + "' AND " + 
				     "Corpus_0__Corpus.vpdmfId=Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0.corpora_id AND " + 
				     "LiteratureCitation_0__LiteratureCitation.vpdmfId=Corpus_corpora__resources_LiteratureCitation__Corpus_0__LiteratureCitation_0.resources_id ORDER BY LiteratureCitation_0__ViewTable.vpdmfLabel ";

			citationsDao.getCoreDao().getCe().connectToDB();
			
			isConnectedToDb = true;
			
			this.rs = citationsDao.getCoreDao().getCe().executeRawSqlQuery(sql);

			// Caches the first row so we can call rs.isAfterLast() to compute the result of hasNext()
			this.rs.next();
			
		} catch (Exception e) {

			throw new ResourceInitializationException(e);

		}

	}

	public void getNext(JCas jcas) throws IOException, CollectionException {

		try {
			
			long vpdmfId = this.rs.getLong("vpdmfId");
			int pmid = this.rs.getInt("pmid");
			String text = this.rs.getString("abstractText");

			if( text == null )
			    jcas.setDocumentText("");
			else
				jcas.setDocumentText( text );

			ArticleCitation cit = new ArticleCitation(jcas);
			cit.setVpdmfId(vpdmfId);
			cit.setPmid(pmid);
			cit.addToIndexes(jcas);
		
		    count++;
		    
		    if (count % 100 == 0) {
		    	info(count + " records processed");
		    }
		    
		    // Caches the next row
			this.rs.next();

		} catch (Exception e) {

			throw new CollectionException(e);

		}

	}

	public boolean hasNext() throws IOException, CollectionException {

		try {
			return ! this.rs.isAfterLast();
		} catch (SQLException e) {
			throw new IOException(e);
		}

	}

	public void close() throws IOException {
		try {
			
			if (isConnectedToDb) {
				citationsDao.getCoreDao().getCe().closeDbConnection();
				isConnectedToDb = false;
			}
			info(count + " records processed");
			info("Completed ");
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected void error(String message) {
		logger.error(message);
	}

	@SuppressWarnings("unused")
	private void warn(String message) {
		logger.warn(message);
	}

	@SuppressWarnings("unused")
	private void debug(String message) {
		logger.debug(message);
	}

	private void info(String message) {
		logger.info(message);
	}

	public Progress[] getProgress() {
		return null;
	}

}