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

public abstract class IndexerCommon extends Indexer {
    // Stores all Document in memory.
    public Vector<Document> _documents = new Vector<Document>();

    public IndexerCommon(Options options) {
	super(options);
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
    public void processDocument(File file){
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
    public abstract void makeIndex(String content, int did);

    /**
     * Porter Algorithm : step1
     * remove plurals
     */
    public String porterAlg(String word){
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
    public String retrieveContent(File file){
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
    public String removeNonVisible(String content){
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
    public abstract void loadIndex() 
	throws IOException, ClassNotFoundException;

    @Override
    public abstract Document getDoc(int docid);

    /**
     * In HW2, you should be using {@link DocumentIndexed}
     */
    @Override
    public abstract Document nextDoc(Query query, int docid);

    @Override
    public abstract int corpusDocFrequencyByTerm(String term); 

    @Override
    public abstract int corpusTermFrequency(String term);

    @Override
    public abstract int documentTermFrequency(String term, String url);
}
