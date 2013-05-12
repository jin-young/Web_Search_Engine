package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

class RankerLinear extends Ranker {
	private RankerCosine rankerCosine;
	private RankerQL rankerQL;
	private RankerPhrase rankerPhrase;

	public RankerLinear(Options options, CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
		rankerCosine = new RankerCosine(options, arguments, indexer);
		rankerQL = new RankerQL(options, arguments, indexer);
		rankerPhrase = new RankerPhrase(options, arguments, indexer);
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    	ScoredDocs scoredDocs = new ScoredDocs();
    	runQuery(query, numResults, scoredDocs);
        
    	return scoredDocs.getScoredDocs();
	}

	private ScoredDocument scoreDocument(Query query, int did) {
		// Process the raw query into tokens.
		query.processQuery();
		// Get the document tokens.
		Document doc = _indexer.getDoc(did);
		// Calculate Score
		double score = calScore(query, doc);

		return new ScoredDocument(doc, score);
	}

	public double calScore(Query query, Document doc) {
		double score = 0.0;
		double b_cos = 0.5, b_lm = 0.49, b_phrase = 0.0999, b_numviews = 0.0001;
		// Score the document.
		score += b_cos * rankerCosine.calScore(query, doc);
		score += b_lm * rankerQL.calScore(query, doc);
		score += b_phrase * rankerPhrase.calScore(query, doc);
		score += b_numviews * doc.getNumViews();
		return score;
	}

	@Override
	public void runQuery(Query query, int numResults, ScoredDocs scoredDocs) {
		Vector<ScoredDocument> all = new Vector<ScoredDocument>();
		long count = 0;
		for (int i = 0; i < _indexer.numDocs(); ++i) {
			count++;
			all.add(scoreDocument(query, i));
		}
		Collections.sort(all, Collections.reverseOrder());
		for (int i = 0; i < all.size() && i < numResults; ++i) {
			scoredDocs.add(all.get(i));
		}
		scoredDocs.set_num_of_result(count);
	}
}
