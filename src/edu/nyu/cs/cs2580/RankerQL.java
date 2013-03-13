package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

// Query Likelihood (language model)
// Smoothing : Jelinek-Mercer smoothing
// lambda : 0.50

class RankerQL extends Ranker {

    public RankerQL(Options options,
		    CgiArguments arguments, Indexer indexer) {
	super(options, arguments, indexer);
	System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {    
	Vector<ScoredDocument> all = new Vector<ScoredDocument>();
	for (int i = 0; i < _indexer.numDocs(); ++i) {
	    all.add(scoreDocument(query, i));
	}
	Collections.sort(all, Collections.reverseOrder());
	Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	for (int i = 0; i < all.size() && i < numResults; ++i) {
	    results.add(all.get(i));
	}
	return results;
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

    public double calScore(Query query, Document doc){
	double score=1.0, lambda=0.50;
	Vector<String> docTokens = ((DocumentFull) doc).getConvertedTitleTokens();
	docTokens.addAll( ((DocumentFull) doc).getConvertedBodyTokens() );

	// Score the document.
	for(String queryToken : query._tokens){
	    score *= ((1-lambda)
		      *(_indexer.documentTermFrequency(queryToken, doc.getUrl())
			/ (double)docTokens.size())
		      + 
		      (lambda)
		      *((double)_indexer.corpusTermFrequency(queryToken)
			/ (double)_indexer.totalTermFrequency()));
	}
	return score;
    }
}
