package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.File;

import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.topicmodeling.util.PairwiseDocumentSimilaritiesCalculator;
import edu.isi.bmkeg.skm.topicmodeling.util.PairwiseDocumentSimilaritiesCalculator.WeightedEdge;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class ComputePairwiseDocumentSimilarities {
	
	public static class Options extends Options_ImplBase {
		@Option(name = "-m", usage = "Input LDA Model file", required = true )
		public File ldaFile;

		@Option(name = "-k", usage = "Vertex' edges cutoff number. Only one of -k or -p should be used", 
				required = false)
		public int k = -1;
		
		@Option(name = "-p", usage = "Percentage of edges to cut. It is a number between 0 and 1 where 0 means no edges will be cut and 1 means most edges will be cut. Only one of -k or -p should be used.", 
				required = false)
		public float p = Float.NaN;
		
		@Option(name = "-g", usage = "Output Document Similarities GraphML file",required = true)
		public File outputFile;
		
	}

	public static void execute(File ldaFile,
			int k,
			File outputFile) throws Exception {
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		System.out.println("Loading LDA Model: " + ldaFile.getAbsolutePath());
		long startTime = System.currentTimeMillis();
		c.loadMalletModel(ldaFile);
		long endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);

		System.out.println("Computing similarity subgraph");		
		startTime = System.currentTimeMillis();		
//		PairwiseDocumentSimilaritiesUtil u = new PairwiseDocumentSimilaritiesUtil(c.documentIds, weights);
		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph(k);
		endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);

		System.out.println("Saving GraphML document");		
		startTime = System.currentTimeMillis();		
		PairwiseDocumentSimilaritiesCalculator.writeGraphMl(g,outputFile);
		endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);
	}
	
	public static void execute(File ldaFile,
			float p,
			File outputFile) throws Exception {
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		System.out.println("Loading LDA Model: " + ldaFile.getAbsolutePath());
		long startTime = System.currentTimeMillis();
		c.loadMalletModel(ldaFile);
		long endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);

		System.out.println("Computing similarity subgraph");		
		startTime = System.currentTimeMillis();		
//		PairwiseDocumentSimilaritiesUtil u = new PairwiseDocumentSimilaritiesUtil(c.documentIds, weights);
		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph(p);
		endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);

		System.out.println("Saving GraphML document");		
		startTime = System.currentTimeMillis();		
		PairwiseDocumentSimilaritiesCalculator.writeGraphMl(g,outputFile);
		endTime = System.currentTimeMillis();
		System.out.println("Duration (min): " + (endTime - startTime) / 60000);
	}

	public static void main(String[] args) throws Exception {

		boolean isCutoffPercentage;
		
		Options options = new Options();
	    CmdLineParser parser = new CmdLineParser(options);
	    try {
	        parser.parseArgument(args);
	      } catch (CmdLineException e) {
	        e.printStackTrace();
	        parser.printUsage(System.err);
	        System.exit(1);
	      }

	    if ((options.k < 0 && Float.isNaN(options.p)) || 
	    		(options.k >= 0 && !Float.isNaN(options.p))) {
	    	System.err.println("Either -k or -p should be specified");
	        parser.printUsage(System.err);
			System.exit(-1);
	    }

	    if (!Float.isNaN(options.p) && (options.p < 0 || options.p > 1)) {
	    	System.err.println("Value of -p should be between 0 and 1");
	        parser.printUsage(System.err);
			System.exit(-1);
	    }
	    
	    isCutoffPercentage = options.k < 0;
	    
		if (!options.ldaFile.exists()) {
			System.err.println("Output file: " + options.ldaFile.getAbsolutePath());
	        parser.printUsage(System.err);
			System.exit(-1);
		}

		if (options.outputFile.exists()) {
			System.err.println("Output file already exists: " + options.outputFile.getAbsolutePath());
	        parser.printUsage(System.err);
			System.exit(-1);
		}

		if (isCutoffPercentage)
			execute(options.ldaFile,
					options.p,
					options.outputFile);			
		else
			execute(options.ldaFile,
					options.k,
					options.outputFile);

	}

}
