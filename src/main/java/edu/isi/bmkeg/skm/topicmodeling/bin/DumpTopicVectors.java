package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.LabelSequence;

public class DumpTopicVectors {

	public static class Options extends Options_ImplBase {
		@Option(name = "-m", usage = "Input LDA Model file", required = true )
		public File ldaFile;
	
		@Option(name = "-p", usage = "Output document topic proportions file")
		public File pFile;
		
		@Option(name = "-id", usage = "Output document ids file")
		public File idFile;
		
	}

	public static void execute(File ldaFile,
			File pFile,
			File idFile) throws Exception {
		
		PrintWriter pStream = null;
		PrintWriter idStream = null;
		
		if (pFile != null) {
			pStream = new PrintWriter(new FileWriter(pFile));
		}

		if (idFile != null) {
			idStream = new PrintWriter(new FileWriter(idFile));
		}

		ParallelTopicModel topicModel = ParallelTopicModel.read(ldaFile);

		int numTopics = topicModel.getNumTopics();
		int numDocs = topicModel.getData().size();

		int docLen;
		int[] topicCounts = new int[ numTopics ];

		double doumentTopicDsitribution[] = new double[numTopics];
		
		for (int doc = 0; doc < numDocs; doc++) {

			TopicAssignment ta = topicModel.getData().get(doc);
			LabelSequence topicSequence = (LabelSequence) topicModel.getData().get(doc).topicSequence;
			int[] currentDocTopics = topicSequence.getFeatures();

			Long documentId = (Long) ta.instance.getName();
			
			docLen = currentDocTopics.length;

			// Count up the tokens
			for (int token=0; token < docLen; token++) {
				topicCounts[ currentDocTopics[token] ]++;
			}

			// And normalize
			for (int topic = 0; topic < numTopics; topic++) {
				
				
				doumentTopicDsitribution[topic] = (topicModel.alpha[topic] + topicCounts[topic]) / 
						(docLen + topicModel.alphaSum);
			}
			
			if (pStream != null) 
				printTopicDistributions(pStream, doumentTopicDsitribution);

			if (idStream != null) 
				printDocumentId(idStream, documentId.longValue());

			Arrays.fill(topicCounts, 0);

		}

		if (pStream != null) pStream.close();
		if (idStream != null) idStream.close();
	}
	
	
	private static void printDocumentId(PrintWriter idStream, long docId) {

		idStream.println(docId);
		
	}

	private static void printTopicDistributions(PrintWriter pStream, double[] doumentTopicDsitribution) {
		for (int i = 0; i < doumentTopicDsitribution.length; i++) {
			
			if (i > 0)
				pStream.print('\t');

			pStream.print(doumentTopicDsitribution[i]);
			
		}
		
		pStream.println();
		
	}


	public static void main(String[] args) throws Exception {

		Options options = new Options();
	    CmdLineParser parser = new CmdLineParser(options);
	    try {
	        parser.parseArgument(args);
	      } catch (CmdLineException e) {
	        e.printStackTrace();
	        parser.printUsage(System.err);
	        System.exit(1);
	      }

		if (!options.ldaFile.exists()) {
			System.err.println("Output file: " + options.ldaFile.getAbsolutePath());
	        parser.printUsage(System.err);
			System.exit(-1);
		}

		if (options.pFile.exists()) {
			System.err.println("Output file already exists: " + options.pFile.getAbsolutePath());
	        parser.printUsage(System.err);
			System.exit(-1);
		}

		if (options.idFile.exists()) {
			System.err.println("Output file already exists: " + options.idFile.getAbsolutePath());
	        parser.printUsage(System.err);
			System.exit(-1);
		}

		execute(options.ldaFile,
				options.pFile,
				options.idFile);

	}

}
