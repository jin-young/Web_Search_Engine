package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public abstract class IndexerCommon extends Indexer {

	public IndexerCommon(Options options) {
		super(options);
	}

	@Override
	public void constructIndex() throws IOException {

		// Get All Files list in the Corpus Folder (data/wiki)
		File folder = new File(_options._corpusPrefix);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				processDocument(file);
			}
		}

		System.out.println("Indexed " + Integer.toString(_numDocs)
				+ " docs with " + Long.toString(_totalTermFrequency)
				+ " terms.");

		String indexFile = _options._indexPrefix + "/corpus.idx";
		System.out.println("Store index to: " + indexFile);
		ObjectOutputStream writer = new ObjectOutputStream(
				new FileOutputStream(indexFile));
		writer.writeObject(this);
		writer.close();

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
		String content = retrieveContent(file);
		content = removeNonVisible(content);
		makeIndex(content, did);

		DocumentIndexed doc = new DocumentIndexed(did);
		doc.setTitle(file.getName());
		doc.setUrl(_options._corpusPrefix + "/" + file.getName());
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
