package edu.nyu.cs.cs2580;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 */
public class RankerFavorite extends Ranker {
	private AdIndexer _adIndexer = null;
	
    public RankerFavorite(Options options, CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        _adIndexer = new AdIndexer(options);
        try {
			_adIndexer.loadIndex();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {    	
    	ScoredDocs scoredDocs = new ScoredDocs();
    	runQuery(query, numResults, scoredDocs);
        
        return scoredDocs.getScoredDocs();
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
            score *= 1.0/((1 - lambda) * 1.0/(_indexer.documentTermFrequency(queryToken, doc.getUrl()) / (double) docTokenSize) + (lambda)
                            * 1.0/((double) _indexer.corpusTermFrequency(queryToken) / (double) _indexer
                                            .totalTermFrequency()));
        }
        return score;
    }

	@Override
	public void runQuery(Query query, int numResults, ScoredDocs scoredDocs) {
		TreeSet<ScoredDocument> rankList = new TreeSet<ScoredDocument>();
    	Set<Integer> docIds = new HashSet<Integer>(); //for speed up
        
        DocumentIndexed doc = null;
        int docid = -1;
        
        long count = 0;
        while ((doc = (DocumentIndexed) _indexer.nextDoc(query, docid)) != null) {
        	docid = doc._docid;
        	count++;
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
            	
                rankList.add(newOne);
                docIds.add(doc._docid);
            }
        }

        scoredDocs.set_num_of_result(count);
        Iterator<ScoredDocument> it = rankList.descendingIterator();
        
        CountDownLatch doneSignal = new CountDownLatch(rankList.size());
        while(it.hasNext()) {
        	ScoredDocument sd = it.next();
        	scoredDocs.add(sd);
        	new Thread() {
        		private Query query;
        		private ScoredDocument sd;
        		private IndexerCommon indexer;
        		private CountDownLatch latch;
        		
        		public void run() { 
        			sd.loadSTextToDisplay(query, indexer);
        			latch.countDown();
        		}
        		private Thread init(ScoredDocument sd, Query query, IndexerCommon indexer, CountDownLatch latch) {
        			this.sd = sd;
        			this.query = query;
        			this.indexer = indexer;
        			this.latch = latch;
        			return this;
        		}
        	}.init(sd, query, (IndexerCommon)_indexer, doneSignal).start();
        }
        try {
			doneSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        //return scoredDocs;
	}
	
	/**
     * Make ScoreDocument for Ad
     * @param query
     * @param doc
     * @return ScoredDocument
     */
    private ScoredDocument scoreDocumentForAd(Query query, AdDocumentIndexed doc) {
        double score = calScoreForAd(query, doc);
        return new ScoredDocument(doc, score);
    }
   
    /**
     * Calculate Ads document score with query
     * @param query
     * @param doc
     * @return
     */
    public double calAdDocScore(Query query, AdDocumentIndexed doc) {
        double score = 1.0, lambda = 0.50;
        int docTokenSize = doc.getTokenSize();
        
        // Score the document.
        for (String queryToken : query._tokens) {
            score *= 1.0 /
                            ((1 - lambda) 
                            * 1.0/(_adIndexer.documentTermFrequency(queryToken, doc.getUrl())   // 이 _adIndexer 함수 쓰는데 문제 없는지 확인!!
                                            / (double) docTokenSize) 
                        + (lambda)
                            * 1.0/((double) _adIndexer.corpusTermFrequency(queryToken) 
                                            / (double) _adIndexer.totalTermFrequency()));
        }
        return score;
    }
    
    /**
     * Calculate Score for Ad
     * @param query
     * @param doc
     * @return score
     * Reference : https://support.google.com/adwords/answer/2454010?hl=en
     */
    public double calScoreForAd(Query query, AdDocumentIndexed doc) {
        double w_keyCTRRel=0.5, w_keySearchRel=0.5;
        double weight_title = 3;
        
        // Your keyword's past clickthrough rate (CTR): How often that keyword led to clicks on your ad
        // Cal : # click from this keyword / # num view of this ads
        double keyCTRRel = (1  / doc.getNumViews() ) * w_keyCTRRel; 
        
        // Your keyword/search relevance: How relevant your keyword is to what a customer searches for
        // Cal : F-measure (query & ads contents)
        double keySearchRel = calAdDocScore(query, doc) * w_keySearchRel;
                        
        double score = doc.getCost() * (keyCTRRel + keySearchRel);  
        
        return score;       
    }
    
	@Override
	public void runQueryForAd(Query query, int numResults, ScoredDocs scoredAdDocs) {
		TreeSet<ScoredDocument> rankList = new TreeSet<ScoredDocument>();
    	Set<Integer> docIds = new HashSet<Integer>(); //for speed up
            	
        AdDocumentIndexed doc = null;
        int docid = -1;
        
        long count = 0;
        while ((doc = (AdDocumentIndexed) _adIndexer.nextDoc(query, docid)) != null) {
        	docid = doc._docid;
        	count++;
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
            	
                rankList.add(newOne);
                docIds.add(doc._docid);
            }
        }

        scoredAdDocs.set_num_of_result(count);
        Iterator<ScoredDocument> it = rankList.descendingIterator();
        
        while(it.hasNext()) {
        	ScoredDocument sd = it.next();
        	scoredAdDocs.add(sd);
        }
	}	
}
