package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer {

	public IndexerInvertedCompressed(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}
	
	private int _prevDocId = 0;

	@Override
	public void constructIndex() throws IOException {
		
	}
	
	private void makeIndex(String content, int docId) {
		// Use Delta Encoding with v-bytes
		int deltaDocId = docId - _prevDocId;
		
		Scanner s = new Scanner(content); // Uses white space by default.
		
		while (s.hasNext()) {
			//String token = porterAlg(s.next());
			_stemmer.setCurrent(s.next());
			_stemmer.stem();
			String token = _stemmer.getCurrent();

			//....... need more!!!
		}
		
		s.close();
		
		_prevDocId = docId;
	}

	@Override
	public Document getDoc(int docid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		// TODO Auto-generated method stub
		return 0;
	}

}
