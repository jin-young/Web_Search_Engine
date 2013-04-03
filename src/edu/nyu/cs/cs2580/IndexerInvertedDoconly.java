package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends IndexerCommon implements
		Serializable {
	private static final long serialVersionUID = 1077111905740085031L;

	// Back-up variables for serializable file write.
	protected Vector<Document> t_documents;
	protected Map<String, Integer> t_dictionary;
	protected int t_numDocs;
	protected long t_totalTermFrequency;

	// Inverted Index, key is the integer representation of the term and value
	// is the id list of document which appear this term.
	private Map<Integer, Vector<Integer>> _index = new HashMap<Integer, Vector<Integer>>();

	// Provided for serialization
	public IndexerInvertedDoconly() {
	}

	// The real constructor
	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	@Override
	public void makeCorpusFiles() throws IOException {
		// make corpus files
		for (int i = 0; i < MAXCORPUS; i++) {
			String indexFile = _options._indexPrefix + "/corpus_" + i + ".idx";
			ObjectOutputStream writer = new ObjectOutputStream(
					new FileOutputStream(indexFile));
			Map<Integer, Vector<Integer>> _tmpIndex = new HashMap<Integer, Vector<Integer>>();
			writer.writeObject(_tmpIndex);
			writer.close();
		}
	}

	/**
	 * After making index files, save _dictionary into file
	 **/
	@Override
	public void writeDicToFile() throws IOException {
		String dicFile = _options._indexPrefix + "/dictionary.idx";
		ObjectOutputStream writer = new ObjectOutputStream(
				new FileOutputStream(dicFile));
		// back-up variables from Indexer class
		t_documents = _documents;
		t_dictionary = _dictionary;
		t_numDocs = _numDocs;
		t_totalTermFrequency = _totalTermFrequency;

		// writer.writeObject(_dictionary);
		writer.writeObject(this);
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
	public int makeIndex(String content, int did) {
		Scanner s = new Scanner(content); // Uses white space by default.
		int tokenSize = 0;
		while (s.hasNext()) {
			String token = porterAlg(s.next());
			int idx = -1;
			if (_dictionary.containsKey(token)) {
				idx = _dictionary.get(token);
				if (!_index.containsKey(idx)) {
					Vector<Integer> tmp = new Vector<Integer>();
					_index.put(idx, tmp);
				}
				Vector<Integer> docList = _index.get(idx);
				if (!docList.contains(did))
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

		return tokenSize;
	}

	// Wirte memory data into file
	// after 1000 documents processing, it saved in one file
	// private int tmpId = 0;

	@SuppressWarnings("unchecked")
	public void writeToFile(int fileIdx) throws IOException,
			ClassNotFoundException {
		if (_index.isEmpty())
			return;
		/*
		 * String indexFile = _options._indexPrefix + "/tmp_" + fileIdx;
		 * ObjectOutputStream writer = new ObjectOutputStream( new
		 * FileOutputStream(indexFile)); writer.writeObject(_index);
		 * _index.clear(); writer.close(); writer.flush();
		 */
		// tmpId++;

		for (int i = 0; i < MAXCORPUS; i++) {
			// Read corpus file
			String indexFile = _options._indexPrefix + "/corpus_" + i + ".idx";
			ObjectInputStream reader = new ObjectInputStream(
					new FileInputStream(indexFile));
			System.out.println("Write " + indexFile);
			Map<Integer, Vector<Integer>> _tmpIndex = (HashMap<Integer, Vector<Integer>>) reader
					.readObject();
			reader.close();

			// processing
			Iterator<String> it = _dictionary.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				int idx = _dictionary.get(key);
				if (idx % MAXCORPUS == i) {
					// look at this key is exist on the _index
					if (_index.containsKey(idx)) {
						// If there are no this key in the file
						if (!_tmpIndex.containsKey(idx)) {
							Vector<Integer> docList = new Vector<Integer>();
							_tmpIndex.put(idx, docList);
						}
						_tmpIndex.get(idx).addAll(_index.get(idx));
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
	}
	
	@Override
	public void writeToFile() throws IOException, ClassNotFoundException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		/*
		 * // Load Documents file String docFile = _options._indexPrefix +
		 * "/documents.idx"; ObjectInputStream reader = new
		 * ObjectInputStream(new FileInputStream(docFile)); _documents =
		 * (Vector<Document>)reader.readObject(); reader.close();
		 * System.out.println("Load documents from: " + docFile);
		 */

		// Load Dictionary file
		String dicFile = _options._indexPrefix + "/dictionary.idx";
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
				dicFile));
		// _dictionary = (TreeMap<String, Integer>) reader.readObject();
		System.out.println("Load dictionary from: " + dicFile);

		IndexerInvertedDoconly loaded = (IndexerInvertedDoconly) reader
				.readObject();

		// this._numDocs = _documents.size();
		this._documents = loaded.t_documents;
		this._dictionary = loaded.t_dictionary;
		this._numDocs = loaded.t_numDocs;
		this._totalTermFrequency = loaded.t_totalTermFrequency;

		// Load Class Variables
		/*
		 * String indexFile = _options._indexPrefix + "/corpus.idx"; reader =
		 * new ObjectInputStream(new FileInputStream(indexFile));
		 * IndexerInvertedDoconly loaded = (IndexerInvertedDoconly)
		 * reader.readObject(); System.out.println("Load corpus from: " +
		 * indexFile);
		 */

		// Compute numDocs and totalTermFrequency b/c Indexer is not
		// serializable.

		reader.close();

		System.out.println(Integer.toString(_numDocs) + " documents loaded "
				+ "with " + Long.toString(_totalTermFrequency) + " terms!");
	}

	@Override
	public Document getDoc(int docid) {
		return _documents.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */
	@Override
	public int nextPhrase(Query query, int docid, int pos) {
		return 1;
	}

	@Override
	public Document nextDoc(Query query, int docid) {

		Vector<Integer> docs = new Vector<Integer>();
		int doc = -1;

		// find next document for each query
		for (int i = 0; i < query._tokens.size(); i++) {
			try {
				doc = next(query._tokens.get(i), docid);
			} catch (IOException ie) {
				System.err.println(ie.getMessage());
			} catch (ClassNotFoundException ce) {
				System.err.println(ce.getMessage());
			}
			if (doc != -1)
				docs.add(doc);
		}

		// no more document
		if (docs.size() < query._tokens.size()) {
			return null;
		}

		// found!
		if (equal(docs)) {
			return _documents.get(docs.get(0));
		}

		// search next
		return nextDoc(query, Max(docs) - 1);
	}

	protected Vector<Integer> retriveDocList(String word) throws IOException,
			ClassNotFoundException {
		int idx = _dictionary.get(word);
		return getDocList(idx);
	}

	/**
	 * Read Corpus file related with idx retrieve the document list of idx
	 * 
	 * @return Vector<Integer> Document List
	 **/
	@SuppressWarnings("unchecked")
	private Vector<Integer> getDocList(int idx) throws IOException,
			ClassNotFoundException {

		if (_index.containsKey(idx))
			return _index.get(idx);

		int pageNum = idx % MAXCORPUS;

		// Read corpus file
		String indexFile = _options._indexPrefix + "/corpus_" + pageNum
				+ ".idx";
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
				indexFile));
		Map<Integer, Vector<Integer>> _tmpIndex = (HashMap<Integer, Vector<Integer>>) reader
				.readObject();
		reader.close();

		if (!_tmpIndex.containsKey(idx))
			return new Vector<Integer>();
		else {
			Vector<Integer> docList = _tmpIndex.get(idx);
			Collections.sort(docList);
			_index.put(idx, docList);
			return docList;
		}
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return _dictionary.containsKey(term) ? _index
				.get(_dictionary.get(term)).size() : 0;
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
