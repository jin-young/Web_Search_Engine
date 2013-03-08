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
public class IndexerInvertedDoconly extends Indexer {
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

    // Stores all Document in memory.
    private Vector<Document> _documents = new Vector<Document>();

    public IndexerInvertedDoconly(Options options) {
	super(options);
	System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }

    @Override
    public void constructIndex() throws IOException {
	
	// Get All Files list in the Corpus Folder (data/wiki)
	File folder = new File(_options._corpusPrefix);
	File[] listOfFiles = folder.listFiles(); 

	for(File file : listOfFiles){
	    if (file.isFile()){
		processDocument(file);		    
	    }
	}	

	System.out.println("Indexed " + Integer.toString(_numDocs)
			   + " docs with " 
			   + Long.toString(_totalTermFrequency)
			   + " terms.");

	String indexFile = _options._indexPrefix + "/corpus.idx";
	System.out.println("Store index to: " + indexFile);
	ObjectOutputStream writer = new ObjectOutputStream(
					   new FileOutputStream(indexFile));
	writer.writeObject(this);
	writer.close();

    }

    /**
     * Document processing must satisfy the following:
     *   1) Non-visible page content is removed, 
             e.g., those inside <script> tags
     *   2) Tokens are stemmed with Step 1 of the Porter's algorithm
     *   3) No stop word is removed, you need to dynamically determine whether
     *       to drop the processing of a certain inverted list.
     */
    private void processDocument(File file){
	int did = _documents.size();
	String content = retrieveContent(file);
	content = removeNonVisible(content);
	makeIndex(content, did);
	
	DocumentIndexed doc = new DocumentIndexed(did);
	doc.setTitle(file.getName());
	doc.setUrl(_options._corpusPrefix + "/"+ file.getName());
	_documents.add(doc);
	++_numDocs;
    }
    
    /**
     * Make Index with content string of html.
     *
     * @param content
     * @param did
     */
    private void makeIndex(String content, int did) {
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

    /**
     * Porter Algorithm : step1
     * remove plurals
     */
    private String porterAlg(String word){
	String ret = word;
	if(word.endsWith("s")){
	    int len = word.length();
	    if(word.endsWith("sses"))
		ret = (len-5 < 0) ? "" : word.substring(0, len-5);
	    else if(word.endsWith("ies"))
		ret = (len-4 < 0) ? "" : word.substring(0, len-4);
	    else if(word.endsWith("s"))
		ret = (len-2 < 0) ? "" : word.substring(0, len-2);
	}
	return ret;
    }

    /**
     * Read HTML <body> ... </body>
     * 
     */
    private String retrieveContent(File file){
	Scanner scanner;
	String content="";
	try{
	    System.out.println(file.getName());
	    scanner = new Scanner(file);
	    CharSequence csBodyStart = "<body", csBodyEnd = "</body>";
	    boolean readBodyFlag=false;
	    while (scanner.hasNextLine()) {
		String line = scanner.nextLine();
		if(readBodyFlag || line.contains(csBodyStart)){
		    content += line;
		    readBodyFlag = true;
		    if(line.contains(csBodyEnd))
			readBodyFlag = false;
		}	    
	    }
	    scanner.close();
	}catch(FileNotFoundException ffe){
	    System.err.println(ffe.getMessage());
	}
	return content;
    }

    /**
     * remove Non-visible page content, e.g., <script>
     */
    private String removeNonVisible(String content){
	String ans = content;
	// remove <script> ... </script>
	int start, end;
	while((start = ans.indexOf("<script")) != -1){
	    if((end = ans.indexOf("</script>")) != -1){
		ans = ans.substring(0, start-1) + ans.substring(end+9);
	    }else
		break;
	}
	
	// remove other < ... >
	StringBuffer sb = new StringBuffer();
	boolean readFlag = true;
	for(int i=0; i<ans.length(); i++){
	    char ch = ans.charAt(i);
	    if(ch == '<')
		readFlag = false;
	    else if(ch == '>')
		readFlag = true;
	    else if(readFlag)
		sb.append(ch);
	}
	return sb.toString();
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
