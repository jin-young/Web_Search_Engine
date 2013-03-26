package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends IndexerCommon implements
		Serializable {

	private static final long serialVersionUID = -7002359116603747368L;

	private Map<Integer, ArrayList<Short>> _index;
	private Map<Integer, ArrayList<Integer>> _skipPointer;
	private Map<String, Integer> _sessionDictionary;

	// Back-up variables for serializable file write.
	protected Vector<Document> t_documents;
	protected Map<String, Integer> t_dictionary;
	protected int t_numDocs;
	protected long t_totalTermFrequency;

	public IndexerInvertedCompressed(Options options) {
		super(options);
		_index = new TreeMap<Integer, ArrayList<Short>>();
		_skipPointer = new HashMap<Integer, ArrayList<Integer>>();
		_sessionDictionary = new TreeMap<String, Integer>();

		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	// Use Delta Encoding with v-bytes

	public int makeIndex(String content, int docId) {
		Scanner s = new Scanner(content); // Uses white space by default.
		// <id of word, its' position in given doc>
		Map<Integer, Vector<Integer>> currentDocIndex = new HashMap<Integer, Vector<Integer>>();
		// positions of given word id
		Vector<Integer> positionsOfWordInCurrentDoc = null;
		int position = 1;

		// Build index for current document
		while (s.hasNext()) {
			_stemmer.setCurrent(s.next());
			_stemmer.stem();
			String token = _stemmer.getCurrent();
			int postingId;
			if (_dictionary.get(token) != null) {
				postingId = _dictionary.get(token);
				_sessionDictionary.put(token, postingId);
			} else {
				postingId = _dictionary.size() + 1;
				_dictionary.put(token, postingId);
				_sessionDictionary.put(token, postingId);
			}

			if (!_index.containsKey(postingId)) {
				_index.put(postingId, new ArrayList<Short>());
			}

			if (currentDocIndex.containsKey(postingId)) {
				positionsOfWordInCurrentDoc = currentDocIndex.get(postingId);
			} else {
				positionsOfWordInCurrentDoc = new Vector<Integer>();
				currentDocIndex.put(postingId, positionsOfWordInCurrentDoc);
			}

			positionsOfWordInCurrentDoc.add(position);
			position++;
			++_totalTermFrequency;
		}
		s.close();

		for (int wordId : currentDocIndex.keySet()) {
			positionsOfWordInCurrentDoc = currentDocIndex.get(wordId);

			// Apply delta encoding
			int prevPosition = 0;
			for (int i = 0; i < positionsOfWordInCurrentDoc.size(); i++) {
				int temp = positionsOfWordInCurrentDoc.get(i);
				positionsOfWordInCurrentDoc.set(i, temp - prevPosition);
				prevPosition = temp;
			}

			// Add current doc's index to posting list from here

			ArrayList<Short> postingList = _index.get(wordId);
			if (postingList == null) {
				postingList = new ArrayList<Short>();
				_index.put(wordId, postingList);
			}

			if (!_skipPointer.containsKey(wordId)) {
				// appears first time
				for (short v : encodeVbyte(docId)) {
					postingList.add(v);
				}
			} else {
				int lastDocId = _skipPointer.get(wordId).get(
						_skipPointer.get(wordId).size() - 2);
				// offset from last document id
				for (short v : encodeVbyte(docId - lastDocId)) {
					postingList.add(v);
				}
			}

			// add encoded # of occurrence after doc id
			for (short v : encodeVbyte(positionsOfWordInCurrentDoc.size())) {
				postingList.add(v);
			}

			// add actual positions after # of occurrence
			for (int val : positionsOfWordInCurrentDoc) {
				for (short v : encodeVbyte(val)) {
					postingList.add(v);
				}
			}

			if (!_skipPointer.containsKey(wordId)) {
				_skipPointer.put(wordId, new ArrayList<Integer>());
			}

			_skipPointer.get(wordId).add(docId);
			_skipPointer.get(wordId).add(postingList.size());
		}

		return (position - 1); // num of tokens in this document
	}

	private short[] encodeVbyte(int value) {
		short[] alignedCode;

		if (value < Math.pow(2, 7)) {
			alignedCode = new short[1];
			alignedCode[0] = (short) ((value & 0x0000007F) | 0x00000080);
		} else if (Math.pow(2, 7) <= value && value < Math.pow(2, 14)) {
			alignedCode = new short[2];
			alignedCode[1] = (short) ((value & 0x0000007F) | 0x00000080);
			alignedCode[0] = (short) ((value >> 7) & 0x0000007F);
		} else if (Math.pow(2, 14) <= value && value < Math.pow(2, 21)) {
			alignedCode = new short[3];
			alignedCode[2] = (short) ((value & 0x0000007F) | 0x00000080);
			alignedCode[1] = (short) ((value >> 7) & 0x0000007F);
			alignedCode[0] = (short) ((value >> 14) & 0x0000007F);
		} else if (Math.pow(2, 21) <= value && value < Math.pow(2, 28)) {
			alignedCode = new short[4];
			alignedCode[3] = (short) ((value & 0x0000007F) | 0x00000080);
			alignedCode[2] = (short) ((value >> 7) & 0x0000007F);
			alignedCode[1] = (short) ((value >> 14) & 0x0000007F);
			alignedCode[0] = (short) ((value >> 21) & 0x0000007F);
		} else {
			throw new RuntimeException("Value : " + value
					+ " cannot be handled by shortAlignedCode");
		}

		return alignedCode;
	}

	@Override
	public Document getDoc(int docid) {
		return _documents.get(docid);
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		Vector<Integer> docs = new Vector<Integer>();
		int doc = -1;

		// find next document for each query
		for (int i = 0; i < query._tokens.size(); i++) {
			try {
				if (query._tokens.get(i).contains(" ")) {

				} else {
					doc = next(query._tokens.get(i), docid);
				}
			} catch (IOException ie) {
				System.err.println(ie.getMessage());
			} catch (ClassNotFoundException ce) {
				System.err.println(ce.getMessage());
			}
			if (doc != -1)
				docs.add(doc);
		}

		// no more document
		if (docs.size() < query._tokens.size())
			return null;

		// found!
		if (equal(docs))
			return _documents.get(docs.get(0));

		// search next
		return nextDoc(query, Max(docs) - 1);
	}
	
	protected Vector<Integer> retriveDocList(String word) {
		int idx = _dictionary.get(word);
		Vector<Integer> docList = new Vector<Integer>();
		
		for (int i = 0; i < _skipPointer.get(idx).size(); i = i + 2) {
			docList.add(_skipPointer.get(idx).get(i));
		}
		
		// Sort the doc list
		Collections.sort(docList);
		
		return docList;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Short> getDocArray(int idx) throws IOException,
			ClassNotFoundException {

		if (_index.containsKey(idx))
			return _index.get(idx);

		int pageNum = idx % MAXCORPUS;

		// Read corpus file
		String indexFile = _options._indexPrefix + "/index_" + pageNum + ".idx";
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
				indexFile));
		Map<Integer, ArrayList<Short>> _tmpIndex = (TreeMap<Integer, ArrayList<Short>>) reader
				.readObject();
		reader.close();

		if (!_tmpIndex.containsKey(idx))
			return new ArrayList<Short>();
		else {
			ArrayList<Short> docMap = _tmpIndex.get(idx);
			_index.put(idx, docMap);
			return docMap;
		}
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		String dicFile = _options._indexPrefix + "/dictionary.idx";
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
				dicFile));
		IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader
				.readObject();
		System.out.println("Load dictionary from: " + dicFile);

		this._documents = loaded.t_documents;
		this._dictionary = loaded.t_dictionary;
		this._numDocs = loaded.t_numDocs;
		this._totalTermFrequency = loaded.t_totalTermFrequency;
		this._skipPointer = loaded._skipPointer;

		reader.close();

		System.out.println(Integer.toString(_numDocs) + " documents loaded "
				+ "with " + Long.toString(_totalTermFrequency) + " terms!");
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return _dictionary.containsKey(term) ? (_skipPointer.get(
				_dictionary.get(term)).size() / 2) : 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		int wordId = _dictionary.get(term);
		ArrayList<Short> list = null;
		try {
			list = getDocArray(wordId);
		} catch (IOException ie) {
			System.err.println(ie.getMessage());
		} catch (ClassNotFoundException ce) {
			System.err.println(ce.getMessage());
		}

		ArrayList<Integer> skipInfo = _skipPointer.get(wordId);

		int frequency = 0;
		int startPoint = 0;
		for (int i = 0; i < skipInfo.size(); i = i + 2) {
			frequency += decodeVbyte(nextPosition(startPoint, list), list);
			startPoint = skipInfo.get(i + 1);
		}

		return frequency;
	}

	private int decodeVbyte(int startPosition, ArrayList<Short> list) {
		int value = 0;
		Short s = list.get(startPosition);
		while ((s & 0x00000080) == 0) {
			value = value << 7;
			value = value | (s & 0x0000007F);
			startPosition++;
			s = list.get(startPosition);
		}
		value = value << 7;
		value = value | (s & 0x0000007F);

		return value;
	}

	private int nextPosition(int startPosition, ArrayList<Short> list) {
		int offset = 1;
		for (; startPosition < list.size(); startPosition++) {
			if ((list.get(startPosition) & 0x00000080) == 0) {
				offset++;
			} else {
				break;
			}
		}
		return startPosition + offset;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		int docid = 0;
		for (Document doc : _documents) {
			if (doc.getUrl().equals(url))
				docid = doc._docid;
		}
		if (docid == 0)
			return 0; // we could not find given doc

		int wordId = _dictionary.get(term);

		ArrayList<Short> list = null;
		try {
			list = getDocArray(wordId);
		} catch (IOException ie) {
			System.err.println(ie.getMessage());
		} catch (ClassNotFoundException ce) {
			System.err.println(ce.getMessage());
		}
		int i = 0;
		ArrayList<Integer> skipInfo = _skipPointer.get(wordId);
		for (; i < skipInfo.size(); i = i + 2) {
			if (skipInfo.get(i) == docid) {
				break;
			}
		}

		if (i > skipInfo.size())
			return 0; // we could not find given doc

		return decodeVbyte(nextPosition(i, list), list);
	}

	@Override
	public void makeCorpusFiles() throws IOException {
		File f = new File(_options._indexPrefix);
		try {
			System.out.println("Clear files in the index directory");
			String[] tempFiles;
			if (f.isDirectory()) {
				tempFiles = f.list();
				for (int i = 0; i < tempFiles.length; i++) {
					File myFile = new File(f, tempFiles[i]);
					myFile.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < MAXCORPUS; i++) {
			String indexFile = _options._indexPrefix + "/index_" + i + ".idx";
			ObjectOutputStream writer = new ObjectOutputStream(
					new FileOutputStream(indexFile));
			writer.writeObject(new TreeMap<Integer, Vector<Integer>>());
			writer.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeToFile(int fileIdx) throws IOException,
			ClassNotFoundException {
		if (_index.isEmpty())
			return;
		String[] words = new String[_sessionDictionary.size()];
		words = _sessionDictionary.keySet().toArray(words);

		Arrays.sort(words, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int o1_mod = _sessionDictionary.get(o1) % MAXCORPUS;
				int o2_mod = _sessionDictionary.get(o2) % MAXCORPUS;

				if (o1_mod > o2_mod) {
					return 1;
				} else if (o1_mod < o2_mod) {
					return -1;
				}
				return 0;
			}
		});
		Map<Integer, ArrayList<Short>> dumpedIndex = null;
		int stopIndex = 0;
		for (int i = 0; i < MAXCORPUS; i++) {
			String fileName = _options._indexPrefix + "/index_" + i + ".idx";
			ObjectInputStream tmpFileReader = new ObjectInputStream(
					new FileInputStream(fileName));
			dumpedIndex = (TreeMap<Integer, ArrayList<Short>>) tmpFileReader
					.readObject();
			tmpFileReader.close();
			for (; stopIndex < words.length; stopIndex++) {
				int wordId = _sessionDictionary.get(words[stopIndex]);
				if (wordId % MAXCORPUS != i) {
					break;
				}
				if (dumpedIndex.get(wordId) != null) {
					dumpedIndex.get(wordId).addAll(_index.get(wordId));
				} else {
					dumpedIndex.put(wordId, _index.get(wordId));
				}
			}
			ObjectOutputStream writer = new ObjectOutputStream(
					new FileOutputStream(fileName));
			writer.writeObject(dumpedIndex);
			writer.close();
		}

		_sessionDictionary.clear();
		_index.clear();
	}

	@Override
	public void mergeFile() throws IOException, ClassNotFoundException {

	}

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

		writer.writeObject(this);
		writer.close();
	}

	@Override
	public int nextPhrase(Query query, int docid, int pos) {
		int docidVer = nextDoc(query, docid - 1)._docid;
		if (docidVer != docid)
			return -1;

		Vector<Integer> posList = new Vector<Integer>();
		for (int i = 0; i < query._tokens.size(); i++) {
			int tmpPos = next_pos(query._tokens.get(i), docid, pos);
			if (tmpPos == -1)
				return -1;
			posList.add(tmpPos);
		}
		boolean isSuccess = true;
		for (int i = 1; i < posList.size(); i++)
			if (posList.get(i - 1) + 1 != posList.get(i))
				isSuccess = false;
		if (isSuccess)
			return posList.get(0);
		return nextPhrase(query, docid, posList.get(1));
	}

	private int next_pos(String term, int docid, int pos) {
		try {
			int wordId = _dictionary.get(term);
			ArrayList<Short> docMap = getDocArray(wordId);

			ArrayList<Integer> skipInfo = _skipPointer.get(wordId);
			int i = 0;
			for (; i < skipInfo.size(); i = i + 2) {
				if (skipInfo.get(i) == docid) {
					break;
				}
			}

			if (i > skipInfo.size())
				return -1; // we could not find given doc

			int howManyOccured = decodeVbyte(nextPosition(i, docMap), docMap);

			ArrayList<Integer> posList = new ArrayList<Integer>();
			int positionOfHead = nextPosition(nextPosition(i, docMap), docMap);
			for (int j = 0; j < howManyOccured; j++) {
				posList.add(decodeVbyte(positionOfHead, docMap));
				positionOfHead = nextPosition(positionOfHead, docMap);
			}

			for (i = 0; i < posList.size(); i++) {
				if (posList.get(i) > pos)
					return posList.get(i);
			}
		} catch (IOException ie) {
			System.err.println(ie.getMessage());
		} catch (ClassNotFoundException ce) {
			System.err.println(ce.getMessage());
		}
		return -1;
	}
}
