package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 * 
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

	public RankerConjunctive(Options options,
							 CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    	ScoredDocs scoredDocs = new ScoredDocs();
    	runQuery(query, numResults, scoredDocs);
        
    	return scoredDocs.getScoredDocs();
	}

	@Override
	public void runQuery(Query query, int numResults, ScoredDocs scoredDocs) {
		Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
		Document doc = null;
		int docid = -1;
		long count = 0;
		while ((doc = _indexer.nextDoc(query, docid)) != null) {
			count++;
			rankQueue.add(new ScoredDocument(doc, 1.0));
			if (rankQueue.size() > numResults) {
				rankQueue.poll();
			}
			docid = doc._docid;
		}

		ScoredDocument scoredDoc = null;
		while ((scoredDoc = rankQueue.poll()) != null) {
			scoredDocs.add(scoredDoc);
		}
		scoredDocs.set_num_of_result(count);
		
		Collections.sort(scoredDocs.getScoredDocs(), Collections.reverseOrder());
	}

	@Override
	public void runQueryForAd(Query processedQuery, int _numResults,
			ScoredDocs scoredAdDocs) {
		throw new UnsupportedOperationException("should be implemented first");
	}
}
