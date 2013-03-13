package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Iterator;
import java.io.File;
import java.io.FileNotFoundException;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends IndexerCommon {

    private Map<Integer, Integer> _cachedIndex
	= new HashMap<Integer, Integer>();
    
    // Inverted Index, key is the integer representation of the term and value
    // is the id list of document which appear this term.
    private Map<Integer, Vector<Integer>> _index
	= new HashMap<Integer, Vector<Integer>>();

    // Term frequency, key is the integer representation of the term and value
    // is the number of times the term appears in the corpus.
    private Map<Integer, Integer> _termCorpusFrequency 
	= new HashMap<Integer, Integer>();
    // Stores only necessary information.
    private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
    
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
    public void makeIndex(String content, int did){
	
	Scanner s = new Scanner(content); // Uses white space by default.
	while (s.hasNext()) {
	    String token = porterAlg( s.next() );
	    int idx = -1;
	    if (_dictionary.containsKey(token)) {
		idx = _dictionary.get(token);
		if(!_index.containsKey(idx)){
		    Vector<Integer> tmp = new Vector<Integer>();
		    _index.put(idx, tmp);
		}		    
		Vector<Integer> docList = _index.get(idx);
		if(!docList.contains(did))
		    docList.add(did);
	    } else {
		idx = _dictionary.size();
		_dictionary.put(token, idx);
		Vector<Integer> docList = new Vector<Integer>();
		docList.add(did);
		_index.put(idx, docList);
		_cachedIndex.put(idx, 0);
		_termCorpusFrequency.put(idx, 0);
	    }
	    _termCorpusFrequency.put(idx, _termCorpusFrequency.get(idx)+1);
	    ++_totalTermFrequency;
	}
	s.close();
	
	// write to file
	if(did>=1000 && (did+1)%1000 == 1){
	    try{
		writeToFile();
	    }catch(IOException ie){
		System.err.println(ie.getMessage());
	    }catch(ClassNotFoundException ce){
		System.err.println(ce.getMessage());
	    }
	}
    }

    // Wirte memory data into file
    // after 1000 documents processing, it saved in one file
    @Override	 
    public void writeToFile() throws IOException, ClassNotFoundException{
	if(_index.isEmpty())
	    return;

	for(int i=0; i<27; i++){
	    // Read corpus file
	    String indexFile = _options._indexPrefix + "/corpus_";
	    if(i < 26)
		indexFile += (char)('a'+i) + ".idx";
	    else
		indexFile += "0.idx";
	    ObjectInputStream reader 
		= new ObjectInputStream(new FileInputStream(indexFile));

	    Map<Integer, Vector<Integer>> _tmpIndex
		= (HashMap<Integer, Vector<Integer>>) reader.readObject();

	    reader.close();

	    // processing
	    Iterator it = _dictionary.keySet().iterator(); 
	    while (it.hasNext()) { 
		String key = (String)it.next();
		if(key.charAt(0) == (char)('a'+i) || key.charAt(0) == (char)('A'+i) || i==26){
		    int keyId = _dictionary.get(key);
		    // look at this key is exist on the _index
		    if(_index.containsKey( keyId )){
			// If there are no this key in the file
			if(!_tmpIndex.containsKey(keyId)){
			    Vector<Integer> docList 
				= new Vector<Integer>();
			    _tmpIndex.put(keyId, docList);
			}
			_tmpIndex.get(keyId).addAll( _index.get(keyId) );
			_index.remove(keyId); 
		    }
		}
	    }
	    // write corpus file
	    ObjectOutputStream writer = new ObjectOutputStream(
					   new FileOutputStream(indexFile));
	    writer.writeObject(_tmpIndex);
	    writer.close();
	}
    }

    // It should be changed!!!
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
	return _documents.get(docid);
    }

    /**
     * In HW2, you should be using {@link DocumentIndexed}
     */
    @Override
    public Document nextDoc(Query query, int docid) {
    	Vector<Integer> docs = new Vector<Integer>();
    	int doc;

    	//find next document for each query
    	for(int i=0; i<query._tokens.size();i++){
	    doc=next(query._tokens.get(i),docid);
	    if(doc!=-1)
		docs.add(doc);
    	}
    	
    	if(docs.size()<query._tokens.size())//no more document
	    return null;
    	if (equal(docs))//found!
	    return _documents.get(docs.get(0));
    	//search next
	return nextDoc(query,Max(docs)-1);
    }
    
    //galloping search
    private int next2(String word, int docid){
    	int low, high, jump;
    	int key=_dictionary.get(word);
    	if(!_index.containsKey(key)||_index.get(key).lastElement()<=docid)
    		return -1;
    	if(_index.get(key).firstElement()>docid){
    		_cachedIndex.put(key, 1);
    		return _index.get(key).firstElement();    		
    	}
    	if(_cachedIndex.get(key)>1
	   &&_index.get(key).get(_cachedIndex.get(key)-1)<=docid)
    		low=_cachedIndex.get(key)-1;
    	else
    		low=1;
    	jump=1;
    	high=low+jump;
    	while(high<_index.get(key).lastElement()
	      &&_index.get(key).get(high)<=docid){
    		low=high;
    		jump=2*jump;
    		high=low+jump;
    	}
    	if(high>_index.get(key).lastElement())
    		high=_index.get(key).lastElement();
    	_cachedIndex.put(key, binarySearch(key,low,high,docid));    	
    	return _index.get(key).get(_cachedIndex.get(key));
    	
    }

    //binary search
    private int next(String word, int docid){
    	int key=_dictionary.get(word);
    	if(!_index.containsKey(key)||_index.get(key).lastElement()<=docid)
    		return -1;
    	if(_index.get(key).firstElement()>docid)
    		return _index.get(key).firstElement();
    	return _index.get(key).get(binarySearch(key,1,_index.size()-1,docid));
    	
    }
    
    private int binarySearch(int key, int low, int high, int docid){
    	int mid;
    	while(high-low>1){
    		mid=(int) Math.floor((low+high)/2);
    		if(_index.get(key).get(mid)<=docid)
    			low=mid;
    		else
    			high=mid;
    	}
    	return high;
    }
        
    private boolean equal(Vector<Integer> docs){
    	int docid=docs.get(0);
    	for(int i=1;i<docs.size();i++){
	    if(docs.get(i)!=docid){
		return false;
	    }
    	}
    	return true;
    }
    
    private int Max(Vector<Integer> docs){
    	int max=0;
    	for(int i=0;i<docs.size();i++){
	    if(docs.get(i)>max)
		max=docs.get(i);
    	}
    	return max;
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
