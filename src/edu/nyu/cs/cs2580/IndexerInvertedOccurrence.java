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
import java.util.Iterator;
import java.util.Collections;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends IndexerCommon implements Serializable{
    private static final long serialVersionUID = 1077111905740085030L;

    // Inverted Index, 
    //      key is the integer representation of the term 
    //      value is HashMap
    //             key is the Document ID
    //             Value is occurence postion list.
    private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _index
	= new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();

    // Back-up variables for serializable file write.
    protected Vector<Document> t_documents;    
    protected Map<String, Integer> t_dictionary; 
    protected int t_numDocs;
    protected long t_totalTermFrequency;

    private Map<Integer, Integer> _cachedIndex
	= new HashMap<Integer, Integer>();

    // Provided for serialization
    public IndexerInvertedOccurrence(){
    }

    // The real constructor
    public IndexerInvertedOccurrence(Options options) {
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
	t_documents = _documents;
	t_dictionary = _dictionary;
	t_numDocs = _numDocs;
	t_totalTermFrequency = _totalTermFrequency;

	writer.writeObject(this);
	writer.close();
    }

    /**
     * Make Index with content string of html.
     *
     * @param content
     * @param did
     * @return token size
     **/
    @Override
    public int makeIndex(String content, int did) {
	Scanner s = new Scanner(content); // Uses white space by default.
	int position = 1;
	int tokenSize = 0;
	while (s.hasNext()) {
	    String token = porterAlg( s.next() );
	    int idx = -1;
	    if (_dictionary.containsKey(token)) {
		idx = _dictionary.get(token);
		if(!_index.containsKey(idx)){
		    HashMap<Integer, ArrayList<Integer>> tmp 
			= new HashMap<Integer, ArrayList<Integer>>();
		    _index.put(idx, tmp);
		}
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
	    ++tokenSize;
	}
	s.close();
	
	return tokenSize;
    }

    // Wirte memory data into file
    // after 1000 documents processing, it saved in one file
//    private int tmpId = 0;

    @Override	 
    public void writeToFile(int fileIdx) throws IOException, ClassNotFoundException{
	if(_index.isEmpty())
	    return;
	String indexFile = _options._indexPrefix + "/tmp_" + fileIdx;
	ObjectOutputStream writer = new ObjectOutputStream(
				   new FileOutputStream(indexFile));
	writer.writeObject(_index);
	_index.clear();
	writer.close();
//	tmpId++;
    }

    @Override
    public void mergeFile() throws IOException, ClassNotFoundException{
	File folder = new File(_options._indexPrefix);
	File[] listOfFiles = folder.listFiles();

	for (File file : listOfFiles) {
	    if (file.isFile() && file.getName().contains("tmp_")) {
		ObjectInputStream tmpFileReader 
		    = new ObjectInputStream(new FileInputStream(file));
		_index = (HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>) tmpFileReader.readObject();
		tmpFileReader.close();

		for(int i=0; i<MAXCORPUS; i++){
		    // Read corpus file
		    String indexFile = _options._indexPrefix + "/corpus_"+i+".idx";
		    ObjectInputStream reader 
			= new ObjectInputStream(new FileInputStream(indexFile));
		    System.out.println("Write " + indexFile);

		    HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _tmpIndex
			= (HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>) reader.readObject();
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
				    HashMap<Integer, ArrayList<Integer>> tmp
					= new HashMap<Integer, ArrayList<Integer>>();
				    _tmpIndex.put(idx, tmp);
				}
				_tmpIndex.get(idx).putAll( _index.get(idx) );
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
	/*
	String docFile = _options._indexPrefix + "/documents.idx";
	ObjectInputStream reader 
	    = new ObjectInputStream(new FileInputStream(docFile));
	_documents = (Vector<Document>)reader.readObject();
	reader.close();
	System.out.println("Load documents from: " + docFile);	
	*/

	// Load Dictionary file
	String dicFile = _options._indexPrefix + "/dictionary.idx";
	ObjectInputStream reader = new ObjectInputStream(new FileInputStream(dicFile));
	IndexerInvertedOccurrence loaded 
	    = (IndexerInvertedOccurrence) reader.readObject();
	System.out.println("Load dictionary from: " + dicFile);

	this._documents = loaded.t_documents;
	this._dictionary = loaded.t_dictionary;
	this._numDocs = loaded.t_numDocs;
	this._totalTermFrequency = loaded.t_totalTermFrequency;

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
     * In HW2, you should be using {@link DocumentIndexed}.
     */
    @Override
    public int nextPhrase(Query query, int docid, int pos){
	int docidVer = nextDoc(query, docid-1)._docid;
	if(docidVer != docid)
	    return -1;

	Vector<Integer> posList = new Vector<Integer>();
	for(int i=0; i<query._tokens.size(); i++){
	    int tmpPos = next_pos(query._tokens.get(i), docid, pos);
	    if(tmpPos == -1)
		return -1;
	    posList.add(tmpPos);
	}
	boolean isSuccess = true;
	for(int i=1; i<posList.size(); i++)
	    if(posList.get(i-1)+1 != posList.get(i))
		isSuccess = false;
	if(isSuccess)
	    return posList.get(0);
	return nextPhrase(query, docid, posList.get(1));
    }

    public int next_pos(String term, int docid, int pos){
	try{
	    int idx = _dictionary.get(term);
	    HashMap<Integer, ArrayList<Integer>> docMap
		= getDocMap(idx);
	    ArrayList<Integer> posList = docMap.get(docid);
	    for(int i=0; i<posList.size(); i++){
		if(posList.get(i) > pos)
		    return posList.get(i);
	    }
	}catch(IOException ie){
	    System.err.println(ie.getMessage());
	}catch(ClassNotFoundException ce){
	    System.err.println(ce.getMessage());
	}
	return -1;
    }

    @Override
    public Document nextDoc(Query query, int docid) {
	Vector<Integer> docs = new Vector<Integer>();
    	int doc = -1;

    	//find next document for each query
    	for(int i=0; i<query._tokens.size(); i++){
	    try{
		doc = next(query._tokens.get(i), docid);
	    }catch(IOException ie){
		System.err.println(ie.getMessage());
	    }catch(ClassNotFoundException ce){
		System.err.println(ce.getMessage());
	    }
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
	return nextDoc(query, Max(docs)-1);
    }

    //galloping search
    private int next(String word, int current) 
	throws IOException, ClassNotFoundException{

    	int low, high, jump;
    	int idx = _dictionary.get(word);
	HashMap<Integer, ArrayList<Integer>> docMap = getDocMap(idx);
	Vector<Integer> docList = new Vector<Integer>();

	Set<Integer> keySet = docMap.keySet();
	for(Integer key : keySet)
	    docList.add(key);

	// Sort the doc list
	Collections.sort(docList);

	int lt = docList.size()-1;

    	if(docList.size()<=1 || docList.lastElement() <= current){
	    return -1;
	}

    	if(docList.firstElement() > current){
	    _cachedIndex.put(idx, 1);
	    return docList.firstElement();    		
    	}

    	if(_cachedIndex.containsKey(idx) && _cachedIndex.get(idx) > 1
	   && docList.get(_cachedIndex.get(idx)-1) <= current){
	    low = _cachedIndex.get(idx)-1;
    	}else
	    low = 1;

    	jump = 1;
    	high = low + jump;
    	while(high < lt && docList.get(high) <= current){
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
    private HashMap<Integer, ArrayList<Integer>> getDocMap(int idx) 
	throws IOException, ClassNotFoundException{

	if(_index.containsKey(idx))
	    return _index.get(idx);
	
	int pageNum = idx % MAXCORPUS;

	// Read corpus file
	String indexFile = _options._indexPrefix + "/corpus_"+pageNum+".idx";
	ObjectInputStream reader 
	    = new ObjectInputStream(new FileInputStream(indexFile));
	HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _tmpIndex
	    = (HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>) reader.readObject();
	reader.close();
	
	if(!_tmpIndex.containsKey(idx))
	    return new HashMap<Integer, ArrayList<Integer>>();
	else{
	    HashMap<Integer, ArrayList<Integer>> docMap = _tmpIndex.get(idx);
	    _index.put(idx, docMap);
	    return docMap;
	}
    }

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
    	int max = 0;
    	for(int i=0; i<docs.size(); i++){
	    if(docs.get(i) > max)
		max = docs.get(i);
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
	int docid = 0;
	for(Document doc : _documents){
	    if(doc.getUrl().equals(url))
		docid = doc._docid;
	}
	int idx = _dictionary.get(term);
	HashMap<Integer, ArrayList<Integer>> docMap = null;
	try{
	    docMap = getDocMap(idx);
	}catch(IOException ie){
	    System.err.println(ie.getMessage());
	}catch(ClassNotFoundException ce){
	    System.err.println(ce.getMessage());
	}
	return docMap.get(docid).size();
    }
}
