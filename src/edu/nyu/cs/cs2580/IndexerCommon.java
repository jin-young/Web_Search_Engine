package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public abstract class IndexerCommon extends Indexer {
	protected static final int MAXCORPUS = 30;
	protected int DIV = 1000; //can be override 

	// Provided for serialization
	public IndexerCommon() {
	}

	// The real constructor
	public IndexerCommon(Options options) {
		super(options);
	}

	@Override
	public void constructIndex() throws IOException {

		makeCorpusFiles();

		// Get All Files list in the Corpus Folder (data/wiki)
		File folder = new File(_options._corpusPrefix);
		File[] listOfFiles = folder.listFiles();

		int count = 0;
		for (File file : listOfFiles) {
			if (file.isFile()) {
				processDocument(file);
				// write to file
				if (count >= DIV && (count % DIV == 0)) {
					try {
						writeToFile(count / DIV);
					} catch (IOException ie) {
						System.err.println(ie.getMessage());
					} catch (ClassNotFoundException ce) {
						System.err.println(ce.getMessage());
					}
				}
				count++;
			}
		}

		try {
			if(count % DIV != 0) {
				writeToFile((count / DIV)+1);
			}
			mergeFile();
			writeDicToFile();
		} catch (ClassNotFoundException e) {
			System.err.println();
		}
	}

	/**
	 * Make New Corpus Files to save Index.
	 **/
	public abstract void makeCorpusFiles() throws IOException;

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
			System.out.println(did + ". " + file.getName());
			org.jsoup.nodes.Element body = Jsoup.parse(file, "UTF-8",
					_options._corpusPrefix + "/" + file.getName()).body();
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
			int tokenSize = 0;
			if (content.trim().length() > 0) {
				tokenSize = makeIndex(content, did);
			}
			// String content = retrieveContent(file);
			// content = removeNonVisible(content);

			DocumentIndexed doc = new DocumentIndexed(did);
			doc.setTitle(file.getName());
			doc.setUrl(_options._corpusPrefix + "/" + file.getName());
			doc.setTokenSize(tokenSize);
			_documents.add(doc);
			++_numDocs;
		} catch (IOException e) {
			System.err.println("Error Occurred while process document '"
					+ file.getName() + "'");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * After 1000 document reading, input _index into file to flush memory
	 **/
	public abstract void writeToFile(int fileIdx) throws IOException,
			ClassNotFoundException;

	/**
	 * Merge Temporary files
	 **/
	public abstract void mergeFile() throws IOException, ClassNotFoundException;

	/**
	 * After making index files, save _dictionary into file
	 **/
	public abstract void writeDicToFile() throws IOException;

	/**
	 * Make Index with content string of html.
	 * 
	 * @param content
	 * @param did
	 * @return token size
	 */
	public abstract int makeIndex(String content, int did);

	/**
	 * Porter Algorithm : step1 remove plurals
	 * 
	 * @Deprecated Even though, this method has been modified to use porter2
	 *             library, it is strongly recommended to use stemmer class
	 *             directly in your code instead of calling this method because
	 *             of avoiding unnecessary method calling.
	 */
	public String porterAlg(String word) {
		/*
		 * String ret = word; if (word.endsWith("s")) { int len = word.length();
		 * if (word.endsWith("sses")) ret = (len - 5 < 0) ? "" :
		 * word.substring(0, len - 5); else if (word.endsWith("ies")) ret = (len
		 * - 4 < 0) ? "" : word.substring(0, len - 4); else if
		 * (word.endsWith("s")) ret = (len - 2 < 0) ? "" : word.substring(0, len
		 * - 2); } return ret;
		 */
		_stemmer.setCurrent(word);
		_stemmer.stem();
		return _stemmer.getCurrent();
	}
}
