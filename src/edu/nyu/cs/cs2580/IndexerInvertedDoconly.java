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
import java.util.TreeMap;
import java.util.Scanner;
import java.util.Iterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends IndexerCommon implements Serializable{
    private static final long serialVersionUID = 1077111905740085031L;
    private static final int MAXCORPUS = 50;
    // Back-up vairable of Indexer class to write dictionary file.
    protected Map<String, Integer> t_dictionary;

    private Map<Integer, Integer> _cachedIndex
	= new HashMap<Integer, Integer>();
    
    // Inverted Index, key is the integer representation of the term and value
    // is the id list of document which appear this term.
    private Map<Integer, Vector<Integer>> _index
	= new HashMap<Integer, Vector<Integer>>();

    // Provided for serialization
    public IndexerInvertedDoconly(){
    }

    // The real constructor
    public IndexerInvertedDoconly(Options options) {
	super(options);
	System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }

    @Override
    public void makeCorpusFiles() throws IOException{
	// make corpus files
	for(int i=0; i<MAXCORPUS; i++){
	    String indexFile = _options._indexPrefix + "/corpus_"+i+".idx";
	    ObjectOutputStream writer = new ObjectOutputStream(
				       new FileOutputStream(indexFile));
	    Map<Integer, Vector<Integer>> _tmpIndex 
		= new HashMap<Integer, Vector<Integer>>();
	    writer.writeObject(_tmpIndex);
	    writer.close();
	}
    }

    /**
     * After making index files,
     * save _dictionary into file
     **/
    @Override
    public void writeDicToFile() throws IOException{
	String dicFile = _options._indexPrefix + "/dictionary.idx";
	ObjectOutputStream writer = new ObjectOutputStream(
				   new FileOutputStream(dicFile));
	// back-up variables from Indexer class
	//t_dictionary = _dictionary;
	writer.writeObject(_dictionary);
	writer.close();
    }

    /**
     * Save _documents into file
     **/
    @Override
    public void writeDocToFile() throws IOException{
	String docFile = _options._indexPrefix + "/documents.idx";
	ObjectOutputStream writer = new ObjectOutputStream(
				   new FileOutputStream(docFile));
	writer.writeObject(_documents);
	writer.close();
    }

    /**
     * Make Index with content string of html.
     *
     * @param content
     * @param did
     * @return token size in document
     */
    @Override
    public int makeIndex(String content, int did){
	Scanner s = new Scanner(content); // Uses white space by default.
	int tokenSize = 0;
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
	    }
	    ++_totalTermFrequency;
	    tokenSize++;
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
	return tokenSize;
    }

    // Wirte memory data into file
    // after 1000 documents processing, it saved in one file
    private int tmpId = 0;

    @Override	 
    public void writeToFile() throws IOException, ClassNotFoundException{
	if(_index.isEmpty())
	    return;
	String indexFile = _options._indexPrefix + "/tmp_" + tmpId;
	ObjectOutputStream writer = new ObjectOutputStream(
				   new FileOutputStream(indexFile));
	writer.writeObject(_index);
	_index.clear();
	writer.close();
	writer.flush();
	tmpId++;
    }

    @Override
    public void mergeFile() throws IOException, ClassNotFoundException{
	File folder = new File(_options._indexPrefix);
	File[] listOfFiles = folder.listFiles();

	for (File file : listOfFiles) {
	    if (file.isFile() && file.getName().contains("tmp_")) {
		System.out.println("Open tmporary file : " + file.getName());
		ObjectInputStream tmpFileReader 
			= new ObjectInputStream(new FileInputStream(file));
		_index = (HashMap<Integer, Vector<Integer>>) tmpFileReader.readObject();
		tmpFileReader.close();

		for(int i=0; i<MAXCORPUS; i++){
		    // Read corpus file
		    String indexFile = _options._indexPrefix + "/corpus_"+i+".idx";
		    ObjectInputStream reader 
			= new ObjectInputStream(new FileInputStream(indexFile));
		    System.out.println("Write " + indexFile);

		    Map<Integer, Vector<Integer>> _tmpIndex
			= (HashMap<Integer, Vector<Integer>>) reader.readObject();

		    reader.close();

		    // processing
		    Iterator it = _dictionary.keySet().iterator(); 
		    while (it.hasNext()) { 
			String key = (String)it.next();
			int idx = _dictionary.get(key);
			if(idx % MAXCORPUS == i){
			    // look at this key is exist on the _index
			    if(_index.containsKey(idx)){
				// If there are no this key in the file
				if(!_tmpIndex.containsKey(idx)){
				    Vector<Integer> docList 
					= new Vector<Integer>();
				    _tmpIndex.put(idx, docList);
				}
				_tmpIndex.get(idx).addAll( _index.get(idx) );
				_index.remove(idx); 
			    }
			}
		    }
		    // write corpus file
		    ObjectOutputStream writer = new ObjectOutputStream(
					     new FileOutputStream(indexFile));
		    writer.writeObject(_tmpIndex);
		    writer.close();
		    writer.flush();
		    _tmpIndex.clear();
		}
		file.delete();
	    }
	}
    }

    @Override
    public void loadIndex() throws IOException, ClassNotFoundException {
	// Load Documents file
	String docFile = _options._indexPrefix + "/documents.idx";
	ObjectInputStream reader 
	    = new ObjectInputStream(new FileInputStream(docFile));
	_documents = (Vector<Document>)reader.readObject();
	reader.close();
	System.out.println("Load documents from: " + docFile);	

	// Load Dictionary file
	String dicFile = _options._indexPrefix + "/dictionary.idx";
	reader = new ObjectInputStream(new FileInputStream(dicFile));
	_dictionary = (TreeMap<String, Integer>) reader.readObject();
	System.out.println("Load dictionary from: " + dicFile);


	/*
	  IndexerInvertedDoconly loaded
	    = (IndexerInvertedDoconly) reader.readObject();
	*/
		
	// Compute numDocs and totalTermFrequency b/c Indexer is not
	// serializable.
	this._numDocs = _documents.size();
	/*
	this._cachedIndex = loaded._cachedIndex;
	*/
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
    	for(int i=0; i<query._tokens.size(); i++){
	    doc = next(query._tokens.get(i), docid);
	    if(doc != -1)
		docs.add(doc);
    	}
    	
	//no more document
    	if(docs.size() < query._tokens.size())
	    return null;

	//found!
    	if (equal(docs))
	    return _documents.get(docs.get(0));

    	//search next
	return nextDoc(query,Max(docs)-1);
    }
    
    //galloping search
    private int next(String word, int docid){
	System.out.println("enter next : " + word + " : " + docid);
	System.out.println("dictionary size : " + _dictionary.size());
	System.out.println("cachedIndex size : " + _cachedIndex.size());
	System.out.println("totalTermFrequency : " + _totalTermFrequency);

    	int low, high, jump;
    	int key = _dictionary.get(word);

	System.out.println("key : " + key);

    	if(!_index.containsKey(key) || _index.get(key).lastElement() <= docid)
    		return -1;

    	if(_index.get(key).firstElement() > docid){
    		_cachedIndex.put(key, 1);
    		return _index.get(key).firstElement();    		
    	}

    	if(_cachedIndex.get(key) > 1
	   && _index.get(key).get(_cachedIndex.get(key)-1) <= docid)
    		low = _cachedIndex.get(key)-1;
    	else
    		low = 1;

    	jump = 1;
    	high = low + jump;
    	while(high < _index.get(key).lastElement()
	      && _index.get(key).get(high) <= docid){
    		low = high;
    		jump = 2*jump;
    		high = low+jump;
    	}

    	if(high > _index.get(key).lastElement())
    		high = _index.get(key).lastElement();

    	_cachedIndex.put(key, binarySearch(key,low,high,docid));    	
    	return _index.get(key).get(_cachedIndex.get(key));
    }
    
    /*
    //binary search
    private int next(String word, int docid){
    	int key=_dictionary.get(word);
    	if(!_index.containsKey(key)||_index.get(key).lastElement()<=docid)
    		return -1;
    	if(_index.get(key).firstElement()>docid)
    		return _index.get(key).firstElement();
    	return _index.get(key).get(binarySearch(key,1,_index.size()-1,docid));
    	
    }
    */
    private int binarySearch(int key, int low, int high, int docid){
    	int mid;
    	while(high - low > 1){
	    mid = (int) Math.floor((low+high) / 2);
	    if(_index.get(key).get(mid) <= docid)
		low = mid;
	    else
		high = mid;
    	}
    	return high;
    }
        
    private boolean equal(Vector<Integer> docs){
    	int docid = docs.get(0);
    	for(int i=1;i<docs.size();i++){
	    if(docs.get(i) != docid){
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
	return 0;
    }

    @Override
    public int documentTermFrequency(String term, String url) {
	return 1;
    }
}
