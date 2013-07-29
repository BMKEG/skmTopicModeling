package edu.isi.bmkeg.skm.topicmodeling.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.LabelSequence;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.isi.bmkeg.skm.topicmodeling.cleartk.cr.CitationAbstractCollectionReader;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class PairwiseDocumentSimilaritiesCalculator {

	private static Logger logger = Logger.getLogger(CitationAbstractCollectionReader.class);

	public DenseDoubleMatrix2D doumentTopicDsitribution;
	public Long[] documentIds;
	
	static public class WeightedEdge {
		
		public double weight;
		
		WeightedEdge(double w) {
			weight = w;
		}
		
		@Override
		public String toString() {
			return String.valueOf(weight);
		}
	}

	public void loadMalletModel(File f) throws Exception  {
		
		ParallelTopicModel topicModel = ParallelTopicModel.read(f);

		int numTopics = topicModel.getNumTopics();
		int numDocs = topicModel.getData().size();

		documentIds = new Long[numDocs];
		doumentTopicDsitribution = new DenseDoubleMatrix2D(numDocs, numTopics);
		
		int docLen;
		int[] topicCounts = new int[ numTopics ];

		for (int doc = 0; doc < numDocs; doc++) {

			TopicAssignment ta = topicModel.getData().get(doc);
			LabelSequence topicSequence = (LabelSequence) topicModel.getData().get(doc).topicSequence;
			int[] currentDocTopics = topicSequence.getFeatures();

			
			documentIds[doc] = (Long) ta.instance.getName();
			
			docLen = currentDocTopics.length;

			// Count up the tokens
			for (int token=0; token < docLen; token++) {
				topicCounts[ currentDocTopics[token] ]++;
			}

			// And normalize
			for (int topic = 0; topic < numTopics; topic++) {
				
				doumentTopicDsitribution.setQuick(doc, topic, 
						(topicModel.alpha[topic] + topicCounts[topic]) / 
						(docLen + topicModel.alphaSum));
			}

			Arrays.fill(topicCounts, 0);

		}

	}

	/**
	 * Computes pairwise document similarities with degree-based edge cutoff policies
	 * 
	 * @param cutoff
	 * @return
	 */
	public UndirectedSparseGraph<Long, WeightedEdge> computeDocumentSimilaritiesSubgraph(int cutoff) {
		
		// Pointer to a an edge. Intended for sorting edges according to their weight
		class EdgeIndex implements Comparable<EdgeIndex>{
			double weight;
			int target;
			
			EdgeIndex(double w, int i) {
				weight = w;
				target  =i;
			}

			@Override
			public int compareTo(EdgeIndex o) {
				return Double.compare(weight, o.weight);
			}
		}
		
		int numDocs = documentIds.length;
	
		UndirectedSparseGraph<Long, WeightedEdge> g = 
				new UndirectedSparseGraph<Long, WeightedEdge>();
		
		MinHeap<EdgeIndex> heaviestEdges = new MinHeap<EdgeIndex>(cutoff);
			
		// Add Vertices
		for (int i = 0; i < numDocs; i++) {

			g.addVertex(documentIds[i]);
			
		}

    	info("Number of documents to compute pairwise similarities: " + numDocs);

		for (int i = 0; i < numDocs; i++)
		{

			heaviestEdges.clear();
			
			// Compute similarities to other documents
			for (int j = 0; j < numDocs; j++)
			{
			
				if (j != i) {
					
					Double cosine = calculateCosineDistance(doumentTopicDsitribution.viewRow(i), doumentTopicDsitribution.viewRow(j));
					
					if (heaviestEdges.size() < cutoff) {
						EdgeIndex newEdge = new EdgeIndex(cosine, j);
						heaviestEdges.add(newEdge);
					} else {

						if (cosine > heaviestEdges.getMin().weight) {							
							EdgeIndex newEdge = new EdgeIndex(cosine, j);							
							heaviestEdges.replaceMin(newEdge);
						}
					}
				}
			}
			
			for (EdgeIndex e : heaviestEdges) {
				
				Long source = documentIds[i];
				Long target = documentIds[e.target];
				
				g.addEdge(new WeightedEdge(e.weight), source, target);

			}

		    if (i % 100 == 0) {
		    	info(i + " documents processed");
		    }
			
		}
		
		info("Pairwise Document similarities computation completed");
		
		return g;
		
	}

	/**
	 * Computes pairwise document similarities with overall percentile edge cutoff policies
	 * 
	 * @param percentCutoff 
	 * @return
	 */
	public UndirectedSparseGraph<Long, WeightedEdge> computeDocumentSimilaritiesSubgraph(float percentCutoff) {
		
		// Pointer to a an edge. Intended for sorting edges according to their weight
		class EdgeIndex implements Comparable<EdgeIndex>{
			double weight;
			int target;
			int source;
			
			EdgeIndex(double w, int s, int t) {
				weight = w;
				source = s;
				target  = t;
			}

			@Override
			public int compareTo(EdgeIndex o) {
				return Double.compare(weight, o.weight);
			}
		}
		
		long numDocs = documentIds.length;
	
		UndirectedSparseGraph<Long, WeightedEdge> g = 
				new UndirectedSparseGraph<Long, WeightedEdge>();
		
		long allEdgesCnt =  numDocs*(numDocs - 1)/2;
		int edgesToKeepCnt = Math.round(allEdgesCnt * (1 - percentCutoff));
		
		MinHeap<EdgeIndex> heaviestEdges = new MinHeap<EdgeIndex>(edgesToKeepCnt);
			
		// Add Vertices
		for (int i = 0; i < numDocs; i++) {

			g.addVertex(documentIds[i]);
			
		}

    	info("Number of documents to compute pairwise similarities: " + numDocs);

		for (int i = 0; i < numDocs; i++)
		{

			// Compute similarities to other documents
			for (int j = i + 1; j < numDocs; j++)
			{
			
				Double cosine = calculateCosineDistance(doumentTopicDsitribution.viewRow(i), doumentTopicDsitribution.viewRow(j));
				
				if (heaviestEdges.size() < edgesToKeepCnt) {
					EdgeIndex newEdge = new EdgeIndex(cosine,i, j);
					heaviestEdges.add(newEdge);
				} else {

					if (cosine > heaviestEdges.getMin().weight) {							
						EdgeIndex newEdge = new EdgeIndex(cosine,i, j);							
						heaviestEdges.replaceMin(newEdge);
					}
				}

			}

			
		    if (i % 100 == 0) {
		    	info(i + " documents processed");
		    }
			
		}
		
		for (EdgeIndex e : heaviestEdges) {
			
			Long source = documentIds[e.source];
			Long target = documentIds[e.target];
			
			g.addEdge(new WeightedEdge(e.weight), source, target);

		}

		info("Pairwise Document similarities computation completed");
		
		return g;
		
	}

	static public void writeGraphMl(UndirectedSparseGraph<Long, WeightedEdge> g,
			File file) throws IOException {
		
		GraphMLWriterX<Long,WeightedEdge> gw = new GraphMLWriterX<Long,WeightedEdge>();
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
		
		gw.setVertexIDs(new Transformer<Long, String>() {
			        public String transform(Long v) {
			            return v.toString();
			        }
			    });

		gw.addEdgeData("weight", "Similarity score" , "0", "weight", "double", 
				new Transformer<WeightedEdge, String>() {
			        public String transform(WeightedEdge e) {
			            return Double.toString(e.weight);
			        }
			    });

		gw.addVertexData("name", "Name", "", "name", "string",
				new Transformer<Long, String>() {
					public String transform(Long v) {
						return v.toString();
					}
	    		});

		gw.save(g, writer);
		
	}

	private static double calculateCosineDistance(DoubleMatrix1D matrix1, DoubleMatrix1D matrix2)
	{
		double score = 0.0;
		DoubleMatrix1D mat1 = matrix1.copy();
		DoubleMatrix1D mat2 = matrix2.copy();

		score = mat1.zDotProduct(mat2) / (Math.sqrt(mat1.zDotProduct(mat1)) * Math.sqrt(mat2.zDotProduct(mat2)));
		return score;
	}
	
	private void info(String message) {
		logger.info(message);
	}

}
