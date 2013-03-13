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

    // Back-up vairable of Indexer class to write dictionary file.
    protected Map<String, Integer> t_dictionary;

    public IndexerInvertedOccurrence(Options options) {
	super(options);
	System.out.println("Using Indexer: " + this.getClass().getSimpleName());
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
	t_dictionary = _dictionary;
	writer.writeObject(this);
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
    public void makeCorpusFiles() throws IOException{
	// make corpus files
	for(int i=0; i<27; i++){
	    String indexFile = _options._indexPrefix + "/corpus_";
	    if(i < 26)
		indexFile += (char)('a'+i) + ".idx";
	    else
		indexFile += "0.idx";
	    ObjectOutputStream writer = new ObjectOutputStream(
				       new FileOutputStream(indexFile));
	    HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _tmpIndex
	       = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
	    writer.writeObject(_tmpIndex);
	    writer.close();
	}
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
	tmpId++;
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

		for(int i=0; i<27; i++){
		    // Read corpus file
		    String indexFile = _options._indexPrefix + "/corpus_";
		    if(i < 26)
			indexFile += (char)('a'+i) + ".idx";
		    else
			indexFile += "0.idx";
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
			if(key.charAt(0) == (char)('a'+i) 
			   || key.charAt(0) == (char)('A'+i) 
			   || i == 26){

			    int idx = _dictionary.get(key);
			    // look at this key is exist on the _index
			    if(_index.containsKey( idx )){
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
	IndexerInvertedOccurrence loaded 
	    = (IndexerInvertedOccurrence) reader.readObject();
	System.out.println("Load dictionary from: " + dicFile);

	this._dictionary = loaded.t_dictionary;
	this._numDocs = _documents.size();
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
	return _documents.get(docid);
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
