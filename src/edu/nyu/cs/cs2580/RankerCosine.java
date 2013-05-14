package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

class RankerCosine extends Ranker {

	public RankerCosine(Options options, CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
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

		double score = calScore(query, doc);
		return new ScoredDocument(doc, score);
	}

	public double calScore(Query query, Document doc) {
		double score = 0.0;
		Vector<String> docTokens = ((DocumentFull) doc)
				.getConvertedTitleTokens();
		// Variables
		double sumWeight = 0.0, sumWeight2 = 0.0;
		Vector<Double> _weights = new Vector<Double>();

		// Score the document.
		for (String queryToken : query._tokens) {
			double tf = _indexer
					.documentTermFrequency(queryToken, doc.getUrl());
			double n = _indexer.numDocs();
			double dt = _indexer.corpusDocFrequencyByTerm(queryToken);
			double idf = 1 + (Math.log(n / dt) / Math.log(2));
			double weight = (double) tf * idf;
			_weights.add(weight);
		}
		normalize(_weights);

		for (int i = 0; i < _weights.size(); i++) {
			sumWeight += _weights.get(i);
			sumWeight2 += _weights.get(i) * _weights.get(i);
		}

		if (sumWeight == 0.0)
			score = 0.0;
		else
			score = sumWeight
					/ Math.sqrt(sumWeight2 * (double) query._tokens.size());
		return score;
	}

	public void normalize(Vector<Double> _weights) {
		double sum = 0.0;
		for (int i = 0; i < _weights.size(); i++)
			sum += _weights.get(i) * _weights.get(i);
		if (sum == 0)
			return;
		sum = Math.sqrt(sum);
		for (int i = 0; i < _weights.size(); i++) {
			double newWeight = _weights.get(i) / sum;
			_weights.set(i, newWeight);
		}
	}

	@Override
	public void runQuery(Query query, int numResults, ScoredDocs scoredDocs) {
		Vector<ScoredDocument> all = new Vector<ScoredDocument>();
		long count = 0;
		for (int i = 0; i < _indexer.numDocs(); ++i) {
			count ++;
			all.add(scoreDocument(query, i));
		}
		Collections.sort(all, Collections.reverseOrder());
		for (int i = 0; i < all.size() && i < numResults; ++i) {
			scoredDocs.add(all.get(i));
		}
		scoredDocs.set_num_of_result(count);
	}
	
	@Override
	public void runQueryForAd(Query processedQuery, int _numResults,
			ScoredDocs scoredAdDocs) {
		throw new UnsupportedOperationException("should be implemented first");
	}	
}
