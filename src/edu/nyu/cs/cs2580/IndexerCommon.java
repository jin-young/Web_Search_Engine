package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public abstract class IndexerCommon extends Indexer {
	protected static final int MAXCORPUS = 30;
	protected int DIV = 500; // can be override

	protected SnowballStemmer _stemmer = new englishStemmer();

	// Stores all Document in memory
	protected Vector<Document> _documents = new Vector<Document>();

	// Maps each term to their integer representation
	protected Map<String, Integer> _dictionary = new TreeMap<String, Integer>();

	protected Map<Integer, Integer> _cachedIndex = new HashMap<Integer, Integer>();

	// Provided for serialization
	public IndexerCommon() {
	}

	// The real constructor
	public IndexerCommon(Options options) {
		super(options);
	}

	@Override
	public void constructIndex() throws IOException {

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
			if (count % DIV != 0) {
				writeToFile((count/DIV) +1);
			}
			writeDicToFile();
		} catch (ClassNotFoundException e) {
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
	public abstract void writeToFile(int round) throws IOException,
			ClassNotFoundException;

	/**
	 * After making index files, save _dictionary into file
	 **/
	public abstract void writeDicToFile() throws IOException;

	/**
	 * Next Phrase position
	 **/
	public abstract int nextPhrase(Query query, int docid, int pos);

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
		_stemmer.setCurrent(word);
		_stemmer.stem();
		return _stemmer.getCurrent();
	}

	protected abstract Vector<Integer> retriveDocList(String word)
			throws IOException, ClassNotFoundException;

	// galloping search
	protected int next(String word, int current) throws IOException,
			ClassNotFoundException {

		int low, high, jump;
		int idx = _dictionary.get(word);

		Vector<Integer> docList = retriveDocList(word);
		int lt = docList.size() - 1;

		if (docList.size() <= 1 || docList.lastElement() <= current) {
			return -1;
		}

		if (docList.firstElement() > current) {
			_cachedIndex.put(idx, 1);
			return docList.firstElement();
		}

		if (_cachedIndex.containsKey(idx) && _cachedIndex.get(idx) > 1
				&& docList.get(_cachedIndex.get(idx) - 1) <= current) {
			low = _cachedIndex.get(idx) - 1;
		} else
			low = 1;

		jump = 1;
		high = low + jump;
		while (high < lt && docList.get(high) <= current) {
			low = high;
			jump = 2 * jump;
			high = low + jump;
		}

		if (high > lt)
			high = lt;

		_cachedIndex.put(idx, binarySearch(docList, low, high, current));
		return docList.get(_cachedIndex.get(idx));
	}

	protected int binarySearch(Vector<Integer> docList, int low, int high,
			int current) {
		int mid;
		while (high - low > 1) {
			mid = (int) ((low + high) / 2.0);
			if (docList.get(mid) <= current)
				low = mid;
			else
				high = mid;
		}
		return high;
	}

	protected boolean equal(Vector<Integer> docs) {
		int docid = docs.get(0);
		for (int i = 1; i < docs.size(); i++) {
			if (docs.get(i) != docid) {
				return false;
			}
		}
		return true;
	}

	protected int Max(Vector<Integer> docs) {
		int max = 0;
		for (int i = 0; i < docs.size(); i++) {
			if (docs.get(i) > max)
				max = docs.get(i);
		}
		return max;
	}

	protected ObjectOutputStream createObjOutStream(String filePath) throws FileNotFoundException, IOException {
		return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath), 1024));
	}

	protected ObjectInputStream createObjInStream(String filePath) throws FileNotFoundException, IOException 
	{
		return new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath), 1024));
	}
}
