/**
 * Borrowed list of stopwords from cleartk-summarization project.
 */

package edu.isi.bmkeg.skm.topicmodeling.cleartk.fc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.util.JCasUtil;

import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.TokenSequence;

import edu.isi.bmkeg.digitalLibrary.uimaTypes.citations.ArticleCitation;

public class Tokens2MalletInstanceFeatureConsumer extends JCasAnnotator_ImplBase {

	public final static String PARAM_MALLET_INPUT_INSTANCE_FILE = ConfigurationParameterFactory
			.createConfigurationParameterName( Tokens2MalletInstanceFeatureConsumer.class, "malletInputInstanceFile" );
	@ConfigurationParameter(mandatory = true, description = "The file where the Mallet input instances are going to be serialized and persisted.")
	File malletInputInstanceFile;

	public final static String PARAM_IDS_MAPPING_FILE = ConfigurationParameterFactory
			.createConfigurationParameterName( Tokens2MalletInstanceFeatureConsumer.class, "idsMappingFile" );
	@ConfigurationParameter(mandatory = false, description = "The file to be created with the mapping between different IDs.")
	File idsMappingFile;

	public final static String PARAM_TO_LOWERCASE = ConfigurationParameterFactory
			.createConfigurationParameterName( Tokens2MalletInstanceFeatureConsumer.class, "toLowerCase" );
	@ConfigurationParameter(mandatory = false, description = "To Lower Case?")
	protected boolean toLowerCase = true;

	public static final String PARAM_STOPWORDS_URI = ConfigurationParameterFactory.createConfigurationParameterName(
			Tokens2MalletInstanceFeatureConsumer.class,
		    "stopwordsUri");

	@ConfigurationParameter(
			mandatory = false,
		    description = "provides a URI pointing to a file containing a whitespace separated list of stopwords (e.g., file:edu/isi/bmkeg/skm/topicmodeling/stopwords.txt")
	protected URI stopwordsUri = null;

	private InstanceList instances;
	
	Set<String> stopwords;
	
	private PrintWriter idsMappingWriter;
	
	public static AnalysisEngineDescription getDescription(File malletInputInstanceFile, 
			boolean toLowerCase, URI stopwordsUri) throws ResourceInitializationException {

	    return AnalysisEngineFactory.createPrimitiveDescription(
	    		Tokens2MalletInstanceFeatureConsumer.class,
	    		PARAM_MALLET_INPUT_INSTANCE_FILE,malletInputInstanceFile,
	    		PARAM_TO_LOWERCASE,toLowerCase,
	    		PARAM_STOPWORDS_URI, stopwordsUri);
	  }

	public static AnalysisEngineDescription getDescription(File malletInputInstanceFile,
			File idsMappingFile,
			boolean toLowerCase, URI stopwordsUri) throws ResourceInitializationException {

	    return AnalysisEngineFactory.createPrimitiveDescription(
	    		Tokens2MalletInstanceFeatureConsumer.class,
	    		PARAM_MALLET_INPUT_INSTANCE_FILE,malletInputInstanceFile,
	    		PARAM_IDS_MAPPING_FILE,idsMappingFile,
	    		PARAM_TO_LOWERCASE,toLowerCase,
	    		PARAM_STOPWORDS_URI, stopwordsUri);
	  }

	// TODO generate VpdmfId 2 Pmid mapping file
	public void initialize(UimaContext context)
			throws ResourceInitializationException {

		super.initialize(context);
		
        try {
		
        	if (idsMappingFile != null) {
        		 idsMappingWriter =  new PrintWriter(idsMappingFile);
        		 idsMappingWriter.println("vpdmfId\tpmid");
        	} else 
        		idsMappingWriter = null;
        	
        	stopwords = this.readStopwords(this.stopwordsUri);
			
        	instances = new InstanceList(new TokenSequence2FeatureSequence());

        } catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

	}

	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		try {

			// Skips instances with no document text
			if (jCas.getDocumentText() == null || jCas.getDocumentText().length() == 0)
				return;	

			ArticleCitation cit = JCasUtil.selectSingle(jCas, ArticleCitation.class);
			long vpdmfId = cit.getVpdmfId();
			long pmid = cit.getPmid();
			
			if (idsMappingWriter != null) {
				idsMappingWriter.println(vpdmfId + "\t" + pmid);
			}
			
			TokenSequence malletTokens = new TokenSequence();
			
			for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
				List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);	
				
				for (Token t : tokens) {
					
					String text = t.getCoveredText();
					
					if (toLowerCase)
						text = text.toLowerCase();
					
					if (!stopwords.contains(text))
						malletTokens.add(text);

				}
				
			}
			
			Instance instance = new Instance(malletTokens, null, vpdmfId, null);

			instances.addThruPipe(instance);
					
		} catch (Exception e) {
			
			throw new AnalysisEngineProcessException(e);
			 
		}

	}
	
	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		
		if (idsMappingWriter != null)
			idsMappingWriter.close();
		
		instances.save(malletInputInstanceFile);
	}

	private Set<String> readStopwords(URI stopwordsUri) throws IOException {
		Set<String> stopwords = new HashSet<String>();
	    if (stopwordsUri == null) {
	      return stopwords;
	    }

	    BufferedReader reader = null;
	    URL url = stopwordsUri.toURL();
	    reader = new BufferedReader(new InputStreamReader(url.openStream()));

	    String line;
	    while ((line = reader.readLine()) != null) {
	      stopwords.addAll(Arrays.asList(line.split("\\s+")));
	    }
	    return stopwords;
  }

}
