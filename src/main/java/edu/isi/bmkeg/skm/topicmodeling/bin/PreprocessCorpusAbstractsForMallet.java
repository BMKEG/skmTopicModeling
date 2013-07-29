package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.File;
import java.net.URI;

import org.apache.uima.collection.CollectionReader;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;

import edu.isi.bmkeg.skm.topicmodeling.cleartk.cr.CitationAbstractCollectionReader;
import edu.isi.bmkeg.skm.topicmodeling.cleartk.fc.Tokens2MalletInstanceFeatureConsumer;

public class PreprocessCorpusAbstractsForMallet {

	public static class Options extends Options_ImplBase {
		@Option(name = "-corpus", usage = "The corpus to be evaluated", required = true)
		public String corpus = "";

		@Option(name = "-f", usage = "Output file name", required = true)
		public File outputFile;
		
		@Option(name = "-l", usage = "Database login", required = true)
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true)
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true)
		public String dbName = "";

		@Option(name = "-keepstopwords", usage = "Keep stopwords", required = false)
		public boolean removeStopwords = false;

		@Option(name = "-ids", usage = "Output Ids mapping file name", required = false)
		public File idsMappingFile = null;
		
	}

	public static void execute(String corpusName, 
			File outputFile,
			String login,
			String password,
			String dbName,
			boolean keepStopwords,
			File idsMappingFile
			) throws Exception {
		
		URI stopwordsUri = null;
		
		if (! keepStopwords) {
			ClassLoader cl = PreprocessCorpusAbstractsForMallet.class.getClassLoader();
			if (cl == null) 
				throw new Exception("Failed to get classloader for " + PreprocessCorpusAbstractsForMallet.class.getCanonicalName());
			stopwordsUri = cl.getResource("edu/isi/bmkeg/skm/topicmodeling/stopwords.txt").toURI();
		}
		
		CollectionReader reader = CitationAbstractCollectionReader.getCollectionReader(
				corpusName, 
				login, 
				password, 
				dbName
				);

	    AggregateBuilder builder = new AggregateBuilder();

	    builder.add(SentenceAnnotator.getDescription()); // Sentence segmentation
	    builder.add(TokenAnnotator.getDescription()); // Tokenization
	    builder.add(Tokens2MalletInstanceFeatureConsumer.getDescription(outputFile,
	    		idsMappingFile,
	    		true,stopwordsUri));
	    
	    // ///////////////////////////////////////////
	    // Run pipeline to create training data file
	    // ///////////////////////////////////////////
	    SimplePipeline.runPipeline(reader, builder.createAggregateDescription());
	    reader.destroy();
		
	}
	
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		if (options.outputFile.exists()) {
			System.err.println("Output file already exists: " + options.outputFile.getAbsolutePath());
			System.exit(-1);
		}

		if (options.idsMappingFile != null &&
				options.idsMappingFile.exists()) {
			System.err.println("Output Ids Mapping file already exists: " + options.idsMappingFile.getAbsolutePath());
			System.exit(-1);
		}

		long startTime = System.currentTimeMillis();
		
		execute(options.corpus, options.outputFile, options.login, 
				options.password, options.dbName,
				options.removeStopwords,
				options.idsMappingFile);

		long endTime = System.currentTimeMillis();
		
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);
	}

}
