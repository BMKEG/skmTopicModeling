package edu.isi.bmkeg.skm.topicmodeling.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.dao.CoreDaoImpl;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
/**
 * Retrieves citations data corresponding to a list of vpdmfIds
 * 
 * @author tallis
 *
 */
public class RetrieveCitationsData {

	public static class Options extends Options_ImplBase {
		@Option(name = "-ids", usage = "File with a list of vpmdfIds separated by new lines. Will use Std Input if not provided", required = false, 
				metaVar = "FILE" )
		public File idsFile = null;

		@Option(name = "-f", usage = "Output file with citations data. Will use Std Output if not provided", required = false, 
				metaVar = "FILE" )
		public File citFile = null;

		@Option(name = "-l", usage = "Database login", required = true)
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true)
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true)
		public String dbName = "";		
	}

	public static void execute(File idsFile,
			File citFile,
			String login,
			String password,
			String dbName) throws Exception {
		
		PrintWriter pWriter;
		BufferedReader reader;
		
		if (idsFile != null) {
			reader =  new BufferedReader(new FileReader(idsFile));
		} else {
			reader =  new BufferedReader(new InputStreamReader(System.in));
		}
		
		if (citFile != null) {
			pWriter =  new PrintWriter(new FileWriter(citFile));
		} else {
			pWriter = new PrintWriter(System.out);
		}
		
		pWriter.println("vpdmfId\tpmid\tvpdmfLabel\turl");

//		CoreDao coreDao = new CoreDaoImpl();
//		coreDao.init(login, password, dbName);
//
//		coreDao.getCe().connectToDB();
		
		// TODO use VPDMf to connect to DB instead of connecting to DB directly
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		String uri = "jdbc:mysql://localhost/" + dbName + "?user=" + login + "&password="
				+ password + "&useOldAliasMetadataBehavior=true";

		Class.forName("com.mysql.jdbc.Driver").newInstance();

		Connection dbConnection = DriverManager.getConnection(uri);

		if (dbConnection == null) {
			throw new VPDMfException("Can't connect to db: " + uri);
		}

		Statement stat = dbConnection.createStatement(
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

		String line = reader.readLine();
		while (line != null) {
			
			try {
				long vpdmfId = Long.parseLong(line);
				
				String sql = "SELECT DISTINCT LiteratureCitation_0__ArticleCitation.vpdmfId, " +
					     "LiteratureCitation_0__ArticleCitation.pmid, " +
					     "LiteratureCitation_0__ViewTable.vpdmfLabel " + 
					    "FROM ViewTable AS LiteratureCitation_0__ViewTable, " +
					     "ArticleCitation AS LiteratureCitation_0__ArticleCitation " +
					    "WHERE LiteratureCitation_0__ArticleCitation.vpdmfId=LiteratureCitation_0__ViewTable.vpdmfId AND " +
					     "LiteratureCitation_0__ArticleCitation.vpdmfId = '" + vpdmfId + "'";				

//				ResultSet rs = coreDao.getCe().executeRawSqlQuery(sql);
				
				// TODO use VPDMf instead of calling on DB Driver directly
				ResultSet rs = stat.executeQuery(sql);
				
				if (rs.next()) {
					int pmid = rs.getInt("pmid");
					String label = rs.getString("vpdmfLabel");
					
					pWriter.format("%d\t%d\t%s\thttp://www.ncbi.nlm.nih.gov/pubmed/?term=%s%%5Buid%%5D\n", vpdmfId, pmid, label, pmid);

				} else {
					System.err.println("Failed to find vpdmfId: " + vpdmfId);
				}
				
			} catch (NumberFormatException e) {
				System.err.println("Failed to parse vpmfId: [" + line + "] - " + e.getMessage());
			}
			
			line = reader.readLine();
			
		}
		
		pWriter.close();
		
//		coreDao.getCe().closeDbConnection();

		// TODO use VPDMf instead of calling on DB Driver directly
		stat.close();
		dbConnection.close();
		
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
	    CmdLineParser parser = new CmdLineParser(options);
	    try {
	        parser.parseArgument(args);
	        
			if (options.idsFile != null  && !options.idsFile.exists()) {
				System.err.println("Cannot find ids file: " + options.idsFile.getAbsolutePath());
		        parser.printUsage(System.err);
				System.exit(-1);
			}

			if (options.citFile != null  && options.citFile.exists()) {
				System.err.println("Output file already exists: " + options.citFile.getAbsolutePath());
		        parser.printUsage(System.err);
				System.exit(-1);
			}
	        
	      } catch (CmdLineException e) {
	    	System.err.println(e.getMessage());
	        parser.printUsage(System.err);
	        System.exit(-11);
	      }

		execute(options.idsFile,
				options.citFile,
				options.login,
				options.password,
				options.dbName);

	}

}
