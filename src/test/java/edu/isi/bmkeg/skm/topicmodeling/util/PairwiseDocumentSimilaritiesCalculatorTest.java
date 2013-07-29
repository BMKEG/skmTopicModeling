package edu.isi.bmkeg.skm.topicmodeling.util;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import edu.isi.bmkeg.skm.topicmodeling.util.PairwiseDocumentSimilaritiesCalculator.WeightedEdge;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/Testcontext-bmkeg.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)

public class PairwiseDocumentSimilaritiesCalculatorTest {
	
	private static int test_corpus_cnt = 41; 
	private static int test_topics_cnt = 10; 

	@Autowired
	ApplicationContext ctx;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void testLoadMalletModel() throws Exception {
		
		File inputFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/ldaModel.dat").getFile();
	
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.loadMalletModel(inputFile);
		
		Assert.assertNotNull(c.documentIds);
		Assert.assertEquals(test_corpus_cnt, c.documentIds.length);

		Assert.assertNotNull(c.doumentTopicDsitribution);
		Assert.assertEquals(test_corpus_cnt, c.doumentTopicDsitribution.rows());
		Assert.assertEquals(test_topics_cnt, c.doumentTopicDsitribution.columns());
		
		System.out.println("DocTopic Dist");
		for (int i = 0; i < c.documentIds.length; i++) {
			System.out.print("\n" + i + "\t" + c.documentIds[i] + "\t");
			
			for (int j = 0; j < c.doumentTopicDsitribution.columns(); j++) {
				System.out.print(c.doumentTopicDsitribution.getQuick(i, j) + "\t");
			}
		}
		System.out.println();
	}

	@Test
	public void testcomputeCosineSimilaritiesCutoff3() throws Exception {
		

		Long[] docIds = new Long[] {1111l,2222l,3333l,4444l,5555l,6666l};
		double[][] documentTopics = new double[][] {
				{0.5, 0.1, 0.1, 0.5, 0.5},
				{0.5, 0.2, 0.1, 0.5, 0.5},
				{0.5, 0.1, 0.3, 0.5, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.5},
				{0.5, 0.5, 0.1, 0.2, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.3}
		};
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.documentIds = docIds;
		c.doumentTopicDsitribution = new DenseDoubleMatrix2D(documentTopics);

		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph(3);
			
		System.out.println("cutoff: 3");
		System.out.println(g);

	}
	
	@Test
	public void testcomputeCosineSimilaritiesCutoff10() throws Exception {
		

		Long[] docIds = new Long[] {1111l,2222l,3333l,4444l,5555l,6666l};
		double[][] documentTopics = new double[][] {
				{0.5, 0.1, 0.1, 0.5, 0.5},
				{0.5, 0.2, 0.1, 0.5, 0.5},
				{0.5, 0.1, 0.3, 0.5, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.5},
				{0.5, 0.5, 0.1, 0.2, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.3}
		};
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.documentIds = docIds;
		c.doumentTopicDsitribution = new DenseDoubleMatrix2D(documentTopics);

		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph(10);
			
		System.out.println("cutoff: 10");
		System.out.println(g);

	}
	
	@Test
	public void testcomputeCosineSimilaritiesCutoffp0() throws Exception {
		

		Long[] docIds = new Long[] {1111l,2222l,3333l,4444l,5555l,6666l};
		double[][] documentTopics = new double[][] {
				{0.5, 0.1, 0.1, 0.5, 0.5},
				{0.5, 0.2, 0.1, 0.5, 0.5},
				{0.5, 0.1, 0.3, 0.5, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.5},
				{0.5, 0.5, 0.1, 0.2, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.3}
		};
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.documentIds = docIds;
		c.doumentTopicDsitribution = new DenseDoubleMatrix2D(documentTopics);

		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph((float) 0.01);
			
		System.out.println("cutoff: 0.01");
		System.out.println(g);

	}

	@Test
	public void testcomputeCosineSimilaritiesCutoffp0_7() throws Exception {
		

		Long[] docIds = new Long[] {1111l,2222l,3333l,4444l,5555l,6666l};
		double[][] documentTopics = new double[][] {
				{0.5, 0.1, 0.1, 0.5, 0.5},
				{0.5, 0.2, 0.1, 0.5, 0.5},
				{0.5, 0.1, 0.3, 0.5, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.5},
				{0.5, 0.5, 0.1, 0.2, 0.5},
				{0.5, 0.5, 0.1, 0.1, 0.3}
		};
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.documentIds = docIds;
		c.doumentTopicDsitribution = new DenseDoubleMatrix2D(documentTopics);

		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph((float) 0.7);
			
		System.out.println("cutoff: 0.7");
		System.out.println(g);

	}

	@Test
	public void testWriteGraphML() throws Exception {
		
		File file = new File("target/docsimilarities.graphml");
		
		File inputFile = ctx.getResource("classpath:/edu/isi/bmkeg/skm/topicmodeling/ldaModel.dat").getFile();
		
		System.out.println("Output file: " + file.getAbsolutePath());
		if (file.exists()) {
			
			System.out.println("Output file already exists - deleting it");
			file.delete();
			
		}
		
		PairwiseDocumentSimilaritiesCalculator c = new PairwiseDocumentSimilaritiesCalculator();
		
		c.loadMalletModel(inputFile);

		UndirectedSparseGraph<Long, WeightedEdge> g = c.computeDocumentSimilaritiesSubgraph(3);

		PairwiseDocumentSimilaritiesCalculator.writeGraphMl(g,  file);
		
		Assert.assertTrue(file.exists());
		
	}

}
