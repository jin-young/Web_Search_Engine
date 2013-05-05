package edu.nyu.cs.cs2580;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

    public RankerFavorite(Options options, CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    	TreeSet<ScoredDocument> rankList = new TreeSet<ScoredDocument>();
    	Set<Integer> docIds = new HashSet<Integer>(); //for speed up
        
        DocumentIndexed doc = null;
        int docid = -1;
        
        while ((doc = (DocumentIndexed) _indexer.nextDoc(query, docid)) != null) {
        	docid = doc._docid;
            if (!docIds.contains(doc._docid)) {
                
            	ScoredDocument newOne = scoreDocument(query, doc);
            	
            	if (rankList.size() >= numResults) { 
            		ScoredDocument currentMinScoreDoc = rankList.first();
            		if(currentMinScoreDoc.getScore() < newOne.getScore()) {
            			rankList.pollFirst();
            			docIds.remove(currentMinScoreDoc.getDocId());
            		} else {
            			//ignore new one
            			continue;
            		}
            	}
            	
                rankList.add(scoreDocument(query, doc));
                docIds.add(doc._docid);
            }
        }

        Vector<ScoredDocument> results = new Vector<ScoredDocument>();
        Iterator<ScoredDocument> it = rankList.descendingIterator();
        while(it.hasNext()) {
        	ScoredDocument sd = it.next();
        	sd.loadSTextToDisplay(query, (IndexerCommon)_indexer);
        	results.add(sd);
        }
        
        return results;
    }

    private ScoredDocument scoreDocument(Query query, DocumentIndexed doc) {
        // Process the raw query into tokens.
        //query.processQuery();
        double score = calScore(query, doc);
        return new ScoredDocument(doc, score);
    }

    public double calScore(Query query, DocumentIndexed doc) {
        double score = 1.0, lambda = 0.50;
        // Vector<String> docTokens = ((DocumentFull)
        // doc).getConvertedTitleTokens();
        // docTokens.addAll( ((DocumentFull) doc).getConvertedBodyTokens() );
        int docTokenSize = doc.getTokenSize();

        // Score the document.
        for (String queryToken : query._tokens) {
            score *= ((1 - lambda) * (_indexer.documentTermFrequency(queryToken, doc.getUrl()) / (double) docTokenSize) + (lambda)
                            * ((double) _indexer.corpusTermFrequency(queryToken) / (double) _indexer
                                            .totalTermFrequency()));
        }
        return score;
    }
}
