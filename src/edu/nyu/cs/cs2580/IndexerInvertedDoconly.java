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
import java.util.Collections;
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
	reader.close();
	System.out.println("Load dictionary from: " + dicFile);

	// Load Class Variables
	/*
	String indexFile = _options._indexPrefix + "/corpus.idx";
	reader = new ObjectInputStream(new FileInputStream(indexFile));
	IndexerInvertedDoconly loaded
	    = (IndexerInvertedDoconly) reader.readObject();
	System.out.println("Load corpus from: " + indexFile);
	*/
	
	// Compute numDocs and totalTermFrequency b/c Indexer is not
	// serializable.
	this._numDocs = _documents.size();

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
    	int doc = -1;

    	//find next document for each query
    	for(int i=0; i<query._tokens.size(); i++){
	    try{
		doc = next(query._tokens.get(i), docid);
		System.out.println("nextDoc : doc result : " + doc);
	    }catch(IOException ie){
		System.err.println(ie.getMessage());
	    }catch(ClassNotFoundException ce){
		System.err.println(ce.getMessage());
	    }
	    if(doc != -1)
		docs.add(doc);
    	}
    		
	//no more document
    	if(docs.size() < query._tokens.size()){
	    System.out.println("return null");
	    return null;
	}
	
	//found!
    	if (equal(docs)){
	    return _documents.get(docs.get(0));
	}
	
    	//search next
	return nextDoc(query, Max(docs)-1);
    }
    
    //galloping search
    private int next(String word, int current) 
	throws IOException, ClassNotFoundException{

	System.out.println("Next start : current " + current);

    	int low, high, jump;
    	int idx = _dictionary.get(word);
	Vector<Integer> docList = getDocList(idx);
	int lt = docList.size()-1;

    	if(docList.size()<=1 || docList.lastElement() <= current){
	    System.out.println("doc list size : " + docList.size());
	    System.out.println("last element : " + docList.lastElement());
	    System.out.println("current : " + current);
	    return -1;
	}

    	if(docList.firstElement() > current){
	    System.out.println("next step1");
	    _cachedIndex.put(idx, 1);
	    return docList.firstElement();    		
    	}

    	if(_cachedIndex.containsKey(idx) && _cachedIndex.get(idx) > 1
	   && docList.get(_cachedIndex.get(idx)-1) <= current){
	    System.out.println("next step2");
	    low = _cachedIndex.get(idx)-1;
    	}else
	    low = 1;

    	jump = 1;
    	high = low + jump;
    	while(high < lt && docList.get(high) <= current){
	    System.out.println("next step3");
	    low = high;
	    jump = 2 * jump;
	    high = low + jump;
    	}

    	if(high > lt)
	    high = lt;

	_cachedIndex.put(idx, binarySearch(docList, low, high, current));
	return docList.get(_cachedIndex.get(idx));
    }
    
    /**
     * Read Corpus file related with idx
     * retrieve the document list of idx
     * @return Vector<Integer> Document List
     **/
    private Vector<Integer> getDocList(int idx) 
	throws IOException, ClassNotFoundException{
	
	int pageNum = idx % MAXCORPUS;

	// Read corpus file
	String indexFile = _options._indexPrefix + "/corpus_"+pageNum+".idx";
	ObjectInputStream reader 
	    = new ObjectInputStream(new FileInputStream(indexFile));
	Map<Integer, Vector<Integer>> _tmpIndex
	    = (HashMap<Integer, Vector<Integer>>) reader.readObject();
	reader.close();
	
	if(!_tmpIndex.containsKey(idx))
	    return new Vector<Integer>();
	else{
	    Vector<Integer> docList = _tmpIndex.get(idx);
	    Collections.sort(docList);
	    return docList;
	}
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
    private int binarySearch(Vector<Integer> docList, 
			     int low, int high, int current){
    	int mid;
    	while(high - low > 1){
	    mid = (int)((low+high) / 2.0);
	    if(docList.get(mid) <= current)
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
