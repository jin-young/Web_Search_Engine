package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends IndexerCommon {
    // Maps each term to their integer representation
    private Map<String, Integer> _dictionary 
	= new HashMap<String, Integer>();

    // Inverted Index, 
    //      key is the integer representation of the term 
    //      value is HashMap
    //             key is the Document ID
    //             Value is occurence postion list.
    private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _index
	= new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();

    public IndexerInvertedOccurrence(Options options) {
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
	int position = 1;
	while (s.hasNext()) {
	    String token = porterAlg( s.next() );
	    int idx = -1;
	    if (_dictionary.containsKey(token)) {
		idx = _dictionary.get(token);
		HashMap<Integer, ArrayList<Integer>> docMap = _index.get(idx);
		if(docMap.containsKey(did))
		    docMap.get(did).add(position);
		else{
		    ArrayList<Integer> occurList = new ArrayList<Integer>();
		    occurList.add(position);
		    docMap.put(did, occurList);
		}		
	    } else {
		idx = _dictionary.size();
		_dictionary.put(token, idx);
		HashMap<Integer, ArrayList<Integer>> docMap 
		    = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> occurList = new ArrayList<Integer>();
		occurList.add(position);
		docMap.put(did, occurList);
		_index.put(idx, docMap);
	    }
	    ++_totalTermFrequency;
	    ++position;
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

	IndexerInvertedOccurrence loaded 
	    = (IndexerInvertedOccurrence) reader.readObject();

	this._documents = loaded._documents;
	this._index = loaded._index;
	this._dictionary = loaded._dictionary;
	reader.close();

	Set<Integer> termKeySet = _index.keySet();
	for(Integer termKey : termKeySet){
	    Set<Integer> docKeySet = _index.get(termKey).keySet();
	    for(Integer docKey : docKeySet)
		this._totalTermFrequency 
		    += _index.get(termKey).get(docKey).size();
	}

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
     * In HW2, you should be using {@link DocumentIndexed}.
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
	int count = 0;
	if(_dictionary.containsKey(term)){
	    int idx = _dictionary.get(term);
	    HashMap<Integer, ArrayList<Integer>> docMap = _index.get(idx);
	    Set<Integer> keySet = docMap.keySet();

	    for(Integer key : keySet){
		count += docMap.get(key).size();
	    }
	}
	return count;
    }

    @Override
    public int documentTermFrequency(String term, String url) {
	SearchEngine.Check(false, "Not implemented!");
	return 0;
    }
}
