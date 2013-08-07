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
import edu.isi.bmkeg.skm.topicmodeling.cleartk.fc.Tokens2MalletInstanceFeatureConsumer;
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

	public final static String PARAM_INSERT_TITLE = ConfigurationParameterFactory
			.createConfigurationParameterName( CitationAbstractCollectionReader.class, "includeTitle" );
	@ConfigurationParameter(mandatory = false, description = "Insert document title as text?")
	protected boolean includeTitle = false;

	public final static String PARAM_SKIP_NULL_ABSTRACTS = ConfigurationParameterFactory
			.createConfigurationParameterName( CitationAbstractCollectionReader.class, "skipNullAbstracts" );
	@ConfigurationParameter(mandatory = false, description = "Skip documents with null abstracts?")
	protected boolean skipNullAbstracts = false;

	protected CitationsDao citationsDao;

	protected int count = 0;
	
	protected boolean isConnectedToDb = false;
	
	protected ResultSet rs;

	private boolean eof = false;

	public static CollectionReader getCollectionReader(
			String corpusName,
			String login,
			String password,
			String dbName
			)
			throws ResourceInitializationException {

		return getCollectionReader(
					corpusName,
					false,
					false,
					login,
					password,
					dbName
				);
	}

	public static CollectionReader getCollectionReader(
			String corpusName,
			boolean skipNullAbstrtacts,
			boolean insertTitle,
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
				PARAM_SKIP_NULL_ABSTRACTS, skipNullAbstrtacts,
				PARAM_INSERT_TITLE, insertTitle,
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
				     "LiteratureCitation_0__LiteratureCitation.title, " + 
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

			
			skipExcluded();
			
		} catch (Exception e) {

			throw new ResourceInitializationException(e);

		}

	}

	public void getNext(JCas jcas) throws IOException, CollectionException {

		try {
			String text = "";
			long vpdmfId = this.rs.getLong("vpdmfId");
			int pmid = this.rs.getInt("pmid");
			String title = this.rs.getString("title");
			String abstractText = this.rs.getString("abstractText");
			if (includeTitle && title != null) 
				text = title + " ";
			if (abstractText != null)
				text += abstractText;

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
		    skipExcluded();

		} catch (Exception e) {

			throw new CollectionException(e);

		}

	}

	public boolean hasNext() throws IOException, CollectionException {

		return ! eof;
		
	}

	private void skipExcluded() throws SQLException {
		eof = !rs.next();
		while (!eof && skipNullAbstracts && rs.getString("abstractText") == null) {
			eof = !rs.next();				
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
