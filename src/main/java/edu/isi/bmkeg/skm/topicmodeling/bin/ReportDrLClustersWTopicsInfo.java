package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.digitalLibrary.controller.DigitalLibraryEngine;
import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.dao.CoreDaoImpl;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
/**
 * Makes a report of DrL clusters information including cluster topics words,
 * top topics per documents, and 
 * citation information of selected documents per cluster.
 * 
 * It is based on data computed by a related R module.
 * 
 * Input files description:
 * 
 * clusterTopicsFile: Computed by our R function "write.diagTopics()"
 * Format: cluster diagTopic words (separated by \t).
 *  
 * clusterDocsFile: Computed by our R function "write.closestNodesWithTopics()"
 * Format: docid, cluster, topici, propi ... (separated by \t).
 *  
 * @author tallis
 *
 */
public class ReportDrLClustersWTopicsInfo {

	public static class Options extends Options_ImplBase {
		@Option(name = "-clusterTopics", usage = "File containing cluster topic words (one topic per cluster)", required = true, 
				metaVar = "FILE" )
		public File clusterTopicsFile = null;

		@Option(name = "-clusterDocs", usage = "File containing the vpdmfIds of selected documents per cluster + top topics", required = true, 
				metaVar = "FILE" )
		public File clusterDocsFile = null;

		@Option(name = "-f", usage = "Output file. Will use Std Output if not provided", required = false, 
				metaVar = "FILE" )
		public File reportFile = null;

		@Option(name = "-wiki", usage = "If present the report will use wiki syntax", required = false)
		public boolean wiki = false;

		@Option(name = "-l", usage = "Database login", required = true)
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true)
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true)
		public String dbName = "";		
	}

	public static class ClusterTopic {
		
		public int topic;
		public String words;
		
		ClusterTopic(int topic, String words) {
			this.topic = topic;
			this.words = words;
		}
	}
	
	public static class DocInfo {
		public long VpdmfId;
		public TopicProportion[] topicProps;
		
		public DocInfo(long vpdmfId, TopicProportion[] topicProps) {
			this.VpdmfId = vpdmfId;
			this.topicProps = topicProps;
		}
	}
	
	public static class TopicProportion {
		public int topic;
		public double prop;
		
		public TopicProportion(int topic, double prop) {
			this.topic = topic;
			this.prop = prop;
		}
	}
	
	public static void execute(File clusterTopicsFile,
			File clusterDocsFile,
			File reportFile,
			boolean wiki,
			String login,
			String password,
			String dbName) throws Exception {
		
		List<ClusterTopic> clts = readClusterTopics(clusterTopicsFile);
		List<? extends List<DocInfo>> clDocs = readClusterDocs(clusterDocsFile);
		
		if (clDocs.size() != clts.size()) {
			throw new IllegalArgumentException(
					"Length of clusterTopics file (" + clts.size() + 
					") doesn't match length of clusterDocs file (" + clDocs.size() + ")");
		}
		
		DigitalLibraryEngine de = new DigitalLibraryEngine();
		de.initializeVpdmfDao(login, password, dbName);
		
		PrintWriter pWriter;
		if (reportFile != null) {
			pWriter =  new PrintWriter(new FileWriter(reportFile));
		} else {
			pWriter = new PrintWriter(System.out);
		}
		
		String docLineFormat, docLineHeadings;
		
		if (wiki) {
			docLineHeadings = "|| vpdmfId || pmid || vpdmfLabel ||top topics ||";
			docLineFormat = "| %d | [%d|http://www.ncbi.nlm.nih.gov/pubmed/?term=%s%%5Buid%%5D] | %s | %s |\n";
		} else {
			docLineHeadings = "vpdmfId\tpmid\tvpdmfLabel\ttop topics";
			docLineFormat = "%d\t%d [http://www.ncbi.nlm.nih.gov/pubmed/?term=%s%%5Buid%%5D]\t%s\t%s\n";
		}

		for (int i = 0; i < clts.size(); i++) {
			ClusterTopic clt = clts.get(i);
			int cl = i + 1;
			
			pWriter.println(String.format(
					"*Cluster %d: Topic #: %d ( %s ...)*",
					cl, clt.topic, clt.words));
			
			pWriter.println(docLineHeadings);
			
			List<DocInfo> dis = clDocs.get(i);
			for (DocInfo di : dis) {
			
				long vpdmfId = di.VpdmfId;
				ArticleCitation ac = de.getCitDao().findArticleByVpdmfId(vpdmfId);
				
				if (ac == null) {
					System.err.println("Failed to find vpdmfId: " + vpdmfId);
				} else {
					int pmid = ac.getPmid();
					String label = escapeSpecialCharacters(ac.getVpdmfLabel());
					StringBuffer sb = new StringBuffer();
					for (TopicProportion tp : di.topicProps) {
						if (tp.topic == clt.topic)
							sb.append("*" + tp.topic + "*");
						else
							sb.append(tp.topic);
						sb.append(String.format(": %.3f ", tp.prop));
					}
					pWriter.format(docLineFormat, vpdmfId, pmid, pmid, label, sb.toString());
				}
			}
			
			pWriter.println();
					
		}
		
		pWriter.close();

	}

	private static String escapeSpecialCharacters(String vpdmfLabel) {
		return vpdmfLabel.replaceAll("\\[","\\\\[").replaceAll("]", "\\\\]");
	}

	private static List<? extends List<DocInfo>> readClusterDocs(File clusterDocsFile) throws Exception {

		ArrayList<ArrayList<DocInfo>> clDocs= new ArrayList<ArrayList<DocInfo>>();
		
		System.out.println("File: " + clusterDocsFile.getAbsolutePath());
		
		BufferedReader reader =  new BufferedReader(new FileReader(clusterDocsFile));
		
		try {

			reader.readLine(); // Skips heading 
			
			String line = reader.readLine();
			int l = 1;
			
			int currClId = -1;
			ArrayList<DocInfo> cl = null;
			
			while (line != null) {
				
				l++;
				String[] fields = line.split("\t");
				
				try {
					long vpdmfId = Long.parseLong(fields[0].trim());
					int clId = Integer.parseInt(fields[1].trim());
					
					int cntTopics = (fields.length - 2) / 2;
					TopicProportion[] tprops = new TopicProportion[cntTopics];
					for (int i = 0; i < cntTopics; i++) {
						tprops[i] = new TopicProportion(Integer.parseInt(fields[i*2 + 2].trim()),
											Double.parseDouble(fields[i*2 + 3].trim()));
					}
		
					DocInfo docInfo = new DocInfo(vpdmfId, tprops);
					
					if (currClId != clId) {
						cl = new ArrayList<DocInfo>();
						clDocs.add(cl);
						currClId = clId;
					}
					
					cl.add(docInfo);

				} catch (NumberFormatException e) {
					throw new Exception("Failed to parse numeric field in file " + clusterDocsFile.getAbsolutePath() + " at line " + l,e);
				}

				line = reader.readLine();
				
			}
			
		} finally {
			reader.close();			
		}
		
		return clDocs;
	}

	private static List<ClusterTopic> readClusterTopics(File clusterTopicsFile) throws Exception {

		ArrayList<ClusterTopic> clts= new ArrayList<ClusterTopic>();
		
		BufferedReader reader =  new BufferedReader(new FileReader(clusterTopicsFile));
		
		try {

			reader.readLine(); // Skips headings line
			
			int l = 1;

			String line = reader.readLine();
			while (line != null) {
				l++;
				String[] fields = line.split("\t");
				if (fields.length < 3) {
					throw new Exception("Not enough fields in file " + clusterTopicsFile.getAbsolutePath() + " at line " + l);
				}
				int cl, topic;
				try {
					cl = Integer.parseInt(fields[0]);
					topic = Integer.parseInt(fields[1]);
				} catch (NumberFormatException e) {
					throw new Exception("Failed to parse Cluster or Topic in file " + clusterTopicsFile.getAbsolutePath() + " at line " + l);
				}
				String words = fields[2];
				
				if (cl != l - 1) {
					throw new Exception("Unexpected cluster number in file " + clusterTopicsFile.getAbsolutePath() + " at line " + l);
				}
				
				clts.add(new ClusterTopic(topic,words));
				
				line = reader.readLine();
				
			}
			
		} finally {
			reader.close();			
		}
		
		return clts;
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
	    CmdLineParser parser = new CmdLineParser(options);
	    try {
	        parser.parseArgument(args);
	        
			if (!options.clusterTopicsFile.exists()) {
				System.err.println("Cannot find clusterTopics file: " + options.clusterTopicsFile.getAbsolutePath());
		        parser.printUsage(System.err);
				System.exit(-1);
			}

			if (!options.clusterDocsFile.exists()) {
				System.err.println("Cannot find clusterDocs file: " + options.clusterDocsFile.getAbsolutePath());
		        parser.printUsage(System.err);
				System.exit(-1);
			}

			if (options.reportFile != null  && options.reportFile.exists()) {
				System.err.println("Output file already exists: " + options.reportFile.getAbsolutePath());
		        parser.printUsage(System.err);
				System.exit(-1);
			}
	        
	      } catch (CmdLineException e) {
	    	System.err.println(e.getMessage());
	        parser.printUsage(System.err);
	        System.exit(-11);
	      }

		execute(options.clusterTopicsFile,
				options.clusterDocsFile,
				options.reportFile,
				options.wiki,
				options.login,
				options.password,
				options.dbName);

	}

}
