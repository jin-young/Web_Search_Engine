package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends IndexerCommon {
    // Maps each term to their integer representation
    private Map<String, Integer> _dictionary 
	= new HashMap<String, Integer>();

    // Inverted Index, key is the integer representation of the term and value
    // is the id list of document which appear this term.
    private Map<Integer, Vector<Integer>> _index
	= new HashMap<Integer, Vector<Integer>>();

    // Term frequency, key is the integer representation of the term and value
    // is the number of times the term appears in the corpus.
    private Map<Integer, Integer> _termCorpusFrequency 
	= new HashMap<Integer, Integer>();

    public IndexerInvertedDoconly(Options options) {
	super(options);
	System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }

    /**
     * Make Index with content string of html.
     *
     * @param content
     * @param did
     */
    @Override
    public void makeIndex(String content, int did) {
	Scanner s = new Scanner(content); // Uses white space by default.
	while (s.hasNext()) {
	    String token = porterAlg( s.next() );
	    int idx = -1;
	    if (_dictionary.containsKey(token)) {
		idx = _dictionary.get(token);
		Vector<Integer> docList = _index.get(idx);
		if(!docList.contains(did))
		    docList.add(did);
	    } else {
		idx = _dictionary.size();
		_dictionary.put(token, idx);
		Vector<Integer> docList = new Vector<Integer>();
		docList.add(did);
		_index.put(idx, docList);
		_termCorpusFrequency.put(idx, 0);
	    }
	    _termCorpusFrequency.put(idx, _termCorpusFrequency.get(idx)+1);
	    ++_totalTermFrequency;
	}
	s.close();
	return;
    }

    @Override
    public void loadIndex() throws IOException, ClassNotFoundException {
	String indexFile = _options._indexPrefix + "/corpus.idx";
	System.out.println("Load index from: " + indexFile);

	ObjectInputStream reader 
	    = new ObjectInputStream(new FileInputStream(indexFile));

	IndexerInvertedDoconly loaded 
	    = (IndexerInvertedDoconly) reader.readObject();

	this._documents = loaded._documents;

	// Compute numDocs and totalTermFrequency b/c Indexer is not
	// serializable.
	this._numDocs = _documents.size();
	for (Integer freq : loaded._termCorpusFrequency.values()) {
	    this._totalTermFrequency += freq;
	}
	this._index = loaded._index;
	this._dictionary = loaded._dictionary;
	this._termCorpusFrequency = loaded._termCorpusFrequency;
	reader.close();

	System.out.println(Integer.toString(_numDocs) + " documents loaded "
			   + "with " + Long.toString(_totalTermFrequency) 
			   + " terms!");
    }

    @Override
    public Document getDoc(int docid) {
	SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
	return null;
    }

    /**
     * In HW2, you should be using {@link DocumentIndexed}
     */
    @Override
    public Document nextDoc(Query query, int docid) {
	return null;
    }

    @Override
    public int corpusDocFrequencyByTerm(String term) {
	return _dictionary.containsKey(term) ? 
	    _index.get(_dictionary.get(term)).size() : 0;
    }

    @Override
    public int corpusTermFrequency(String term) {
	return _dictionary.containsKey(term) ? _termCorpusFrequency
	    .get(_dictionary.get(term)) : 0;
    }

    @Override
    public int documentTermFrequency(String term, String url) {
	SearchEngine.Check(false, "Not implemented!");
	return 0;
    }
}
