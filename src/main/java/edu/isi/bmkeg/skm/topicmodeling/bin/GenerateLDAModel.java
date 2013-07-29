package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class GenerateLDAModel {

	public static String USAGE = "-i <instances file> -k <num topics> [-a <alpha>] [-b <beta>] [-m <model file>] [-dt <dt file>] [-t <topics file>]";
	
	public static class Options extends Options_ImplBase {
		@Option(name = "-i", usage = "Instances Input filename", required = true )
		public File inputInstancesFile;
		
		@Option(name = "-m", usage = "LDA Model Output filename")
		public File outputModelFile;

		@Option(name = "-dt", usage = "Document Topics Output filename")
		public File outputDTFile;

		@Option(name = "-t", usage = "Topics Output filename")
		public File outputTopicsFile;

		@Option(name = "-k", usage = "Number of Topics")
		public int numTopics;

		@Option(name = "-a", usage = "Alpha")
		public float alpha = 1.0f;

		@Option(name = "-b", usage = "Beta")
		public float beta = 0.01f;
		
	}

	public static void execute(File inputInstancesFile,
			int numTopics,
			float alpha,
			float beta,
			File outputModelFile,
			File outputDTFile, 
			File outputTopicsFile) throws Exception {
		
		ParallelTopicModel lda = new ParallelTopicModel(numTopics, alpha, beta);
		InstanceList instances = InstanceList.load(inputInstancesFile);
		System.out.println ("Training Data loaded.");
		lda.addInstances(instances);
		lda.setTopicDisplay(100, 20);
		lda.setNumIterations(1000);
		lda.setNumThreads(4);
		
		// Set Dirichlet hyperparameter optimization intervals to 0 (no optimization)
		lda.setBurninPeriod(0);
		lda.setOptimizeInterval(0);
		
		lda.estimate();
		
		if (outputDTFile != null) {
			System.out.println("Saving Document/Topics to: " + outputDTFile.getAbsolutePath());
			lda.printDocumentTopics(outputDTFile);
		}
		if (outputTopicsFile != null) {
			System.out.println("Saving Topics to: " + outputTopicsFile.getAbsolutePath());
			lda.printTopWords(outputTopicsFile,20,false);
		}
		
		if (outputModelFile != null) {
			System.out.println("Saving Model to: " + outputModelFile.getAbsolutePath());

			ObjectOutputStream oos =
					new ObjectOutputStream (new FileOutputStream (outputModelFile));
				oos.writeObject (lda);
				oos.close();
		}
	}
	
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.parseOptions(args);

		if (options.numTopics <= 0) {
			System.err.println("Invalid Num Topics: " + options.numTopics);
			System.err.print(USAGE);
			System.exit(-1);
		}
		
		if (!options.inputInstancesFile.exists()) {
			System.err.println("Not found Instances input file: " + options.inputInstancesFile.getAbsolutePath());
			System.err.print(USAGE);
			System.exit(-1);
		}

		if (options.outputModelFile != null &&
				options.outputModelFile.exists()) {
			System.err.println("Model Output file already exists: " + options.outputModelFile.getAbsolutePath());
			System.exit(-1);
		}

		if (options.outputDTFile != null &&
				options.outputDTFile.exists()) {
			System.err.println("Document Topic Output file already exists: " + options.outputDTFile.getAbsolutePath());
			System.exit(-1);
		}

		if (options.outputTopicsFile != null &&
				options.outputTopicsFile.exists()) {
			System.err.println("Topics Output file already exists: " + options.outputTopicsFile.getAbsolutePath());
			System.exit(-1);
		}
	
		execute(options.inputInstancesFile,
				options.numTopics,
				options.alpha,
				options.beta,
				options.outputModelFile,
				options.outputDTFile,
				options.outputTopicsFile);

	}

}
