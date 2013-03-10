package edu.nyu.cs.cs2580;

import java.util.Vector;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

	public RankerFavorite(Options options,
						  CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
		Document doc = null;
		int docid = -1;
		while ((doc = _indexer.nextDoc(query, docid)) != null) {
			rankQueue.add(scoreDocument(query, doc));
			if (rankQueue.size() > numResults) {    // Jinil : I'm not sure, is this possible without score?
				rankQueue.poll();
			}
			docid = doc._docid;
		}

		Vector<ScoredDocument> results = new Vector<ScoredDocument>();
		ScoredDocument scoredDoc = null;
		while ((scoredDoc = rankQueue.poll()) != null) {
			results.add(scoredDoc);
		}
		Collections.sort(results, Collections.reverseOrder());
		return results;
	}

    private ScoredDocument scoreDocument(Query query, Document doc) {
		// Process the raw query into tokens.
		query.processQuery();
		double score = calScore(query, doc);
		return new ScoredDocument(doc, score);
    }

    public double calScore(Query query, Document doc){
		double score = 0.0;
		Vector<String> docTokens = ((DocumentFull) doc).getConvertedTitleTokens();
		// Variables
		double sumWeight=0.0, sumWeight2=0.0;
		Vector<Double> _weights = new Vector<Double>();
	
		// Score the document.
		for(String queryToken : query._tokens){
			double tf = _indexer.documentTermFrequency(queryToken, doc.getUrl());
			double n = _indexer.numDocs();
			double dt = _indexer.corpusDocFrequencyByTerm(queryToken);
			double idf = 1 + (Math.log(n/dt) / Math.log(2));
			double weight = (double)tf * idf;
			_weights.add(weight);
		}
		normalize(_weights);
	
		for(int i=0; i<_weights.size(); i++){
			sumWeight += _weights.get(i);
			sumWeight2 += _weights.get(i) * _weights.get(i); 
		}

		if(sumWeight == 0.0)  
			score = 0.0;
		else
			score = sumWeight / Math.sqrt(sumWeight2 
										  * (double)query._tokens.size());	    
		return score;
    }

    public void normalize(Vector<Double> _weights){
		double sum = 0.0;
		for(int i=0; i<_weights.size(); i++)
			sum += _weights.get(i) * _weights.get(i);
		if(sum==0)  return;
		sum = Math.sqrt(sum);
		for(int i=0; i<_weights.size(); i++){
			double newWeight = _weights.get(i)/sum;
			_weights.set(i, newWeight);
		}
    }
}

