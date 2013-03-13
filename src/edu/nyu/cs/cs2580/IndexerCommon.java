package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public abstract class IndexerCommon extends Indexer {
    
    public IndexerCommon(Options options) {
	super(options);
    }

    @Override
    public void constructIndex() throws IOException {
	// make corpus files
	for(int i=0; i<27; i++){
	    String indexFile = _options._indexPrefix + "/corpus_";
	    if(i < 26)
		indexFile += (char)('a'+i) + ".idx";
	    else
		indexFile += "0.idx";
	    ObjectOutputStream writer = new ObjectOutputStream(
				       new FileOutputStream(indexFile));
	    Map<Integer, Vector<Integer>> _tmpIndex 
		= new HashMap<Integer, Vector<Integer>>();
	    writer.writeObject(_tmpIndex);
	    writer.close();
	}
	// Get All Files list in the Corpus Folder (data/wiki)
	File folder = new File(_options._corpusPrefix);
	File[] listOfFiles = folder.listFiles();

	for (File file : listOfFiles) {
	    if (file.isFile()) {
		processDocument(file);
	    }
	}
	
	try{
	    writeToFile();
	    writeDicToFile();
	}catch(ClassNotFoundException e){
	    System.err.println();
	}
    }

    /**
     * Document processing must satisfy the following: 1) Non-visible page
     * content is removed, e.g., those inside <script> tags 2) Tokens are
     * stemmed with Step 1 of the Porter's algorithm 3) No stop word is removed,
     * you need to dynamically determine whether to drop the processing of a
     * certain inverted list.
     */
    public void processDocument(File file) {
	int did = _documents.size();
	try {
	    System.out.println(did +". " + file.getName());
	    org.jsoup.nodes.Element body = Jsoup.parse(file, "UTF-8", _options._corpusPrefix + "/" + file.getName()).body();
	    // Remove all script and style elements and those of class "hidden".
	    body.select("script, style, .hidden").remove();

	    // Remove all style and event-handler attributes from all elements.
	    Elements all = body.select("*");
	    for (Element el : all) { 
		for (Attribute attr : el.attributes()) { 
		    String attrKey = attr.getKey();
		    if (attrKey.equals("style") || attrKey.startsWith("on")) { 
			el.removeAttr(attrKey);
		    } 
		}
	    }

	    String content = body.text();
	    if(content.trim().length() > 0) {
		makeIndex(content, did);
	    }
	    //			String content = retrieveContent(file);
	    //			content = removeNonVisible(content);
	
	    DocumentIndexed doc = new DocumentIndexed(did);
	    doc.setTitle(file.getName());
	    doc.setUrl(_options._corpusPrefix + "/" + file.getName());
	    _documents.add(doc);
	    ++_numDocs;
	} catch (IOException e) {
	    System.err.println("Error Occurred while process document '" + file.getName() + "'");
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /**
     * After 1000 document reading, input _index into file 
     * to flush memory
     **/
    public abstract void writeToFile() throws IOException, ClassNotFoundException;

    /**
     * After making index files,
     * save _dictionary into file
     **/
    public void writeDicToFile() throws IOException{
	String dicFile = _options._indexPrefix + "/dictionary.idx";
	ObjectOutputStream writer = new ObjectOutputStream(
				   new FileOutputStream(dicFile));
	writer.writeObject(_dictionary);
	writer.close();
    }

	/**
	 * Make Index with content string of html.
	 * 
	 * @param content
	 * @param did
	 */
	public abstract void makeIndex(String content, int did);

	/**
	 * Porter Algorithm : step1 remove plurals
	 * 
	 * @Deprecated Even though, this method has been modified to use porter2 library,
	 * it is strongly recommended to use stemmer class directly in your code instead of calling
	 * this method because of avoiding unnecessary method calling.
	 */
	public String porterAlg(String word) {
		/*
		String ret = word;
		if (word.endsWith("s")) {
			int len = word.length();
			if (word.endsWith("sses"))
				ret = (len - 5 < 0) ? "" : word.substring(0, len - 5);
			else if (word.endsWith("ies"))
				ret = (len - 4 < 0) ? "" : word.substring(0, len - 4);
			else if (word.endsWith("s"))
				ret = (len - 2 < 0) ? "" : word.substring(0, len - 2);
		}
		return ret;
		*/
		_stemmer.setCurrent(word);
		_stemmer.stem();
		return _stemmer.getCurrent();
	}
}
