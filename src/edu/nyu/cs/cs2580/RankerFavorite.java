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
	Vector<ScoredDocument> rankList = new Vector<ScoredDocument>();
	Vector<DocumentIndexed> docList = new Vector<DocumentIndexed>();

	DocumentIndexed doc = null;
	int docid = -1;
	while ((doc = (DocumentIndexed)_indexer.nextDoc(query, docid)) != null) {
	    // Process Phrase search
	    int resultPos = 0;
	    if(query.getClass().getSimpleName().equals("QueryPhrase")){
		resultPos = ((IndexerCommon)_indexer).nextPhrase(query, docid, -1);
	    }

	    if(!docList.contains(doc) && resultPos != -1){
		rankList.add(scoreDocument(query, doc));
		docList.add(doc);
		
		if (rankList.size() > numResults) {    
		    double minValue = rankList.get(0).getScore();
		    int id = 0;
		    for(int i=1; i<rankList.size(); i++){
			if(rankList.get(i).getScore() < minValue){
			    minValue = rankList.get(i).getScore();
			    id = i;
			}
		    }
		    rankList.remove(id);
		    docList.remove(id);
		}
	    }
	    docid = doc._docid;
	}

	Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	ScoredDocument scoredDoc = null;
	for(int i=0; i<rankList.size(); i++){
	    results.add(rankList.get(i));
	}
	Collections.sort(results, Collections.reverseOrder());
	return results;
    }

    private ScoredDocument scoreDocument(Query query, DocumentIndexed doc) {
	// Process the raw query into tokens.
	query.processQuery();
	double score = calScore(query, doc);
	return new ScoredDocument(doc, score);
    }

    public double calScore(Query query, DocumentIndexed doc){
	double score=1.0, lambda=0.50;
	//Vector<String> docTokens = ((DocumentFull) doc).getConvertedTitleTokens();
	//docTokens.addAll( ((DocumentFull) doc).getConvertedBodyTokens() );
	int docTokenSize = doc.getTokenSize();
	
	// Score the document.
	for(String queryToken : query._tokens){
	    score *= ((1-lambda)
		      *(_indexer.documentTermFrequency(queryToken, doc.getUrl())
			/ (double)docTokenSize)
		      + 
		      (lambda)
		      *((double)_indexer.corpusTermFrequency(queryToken)
			/ (double)_indexer.totalTermFrequency()));
	}
	return score;
    }
}

