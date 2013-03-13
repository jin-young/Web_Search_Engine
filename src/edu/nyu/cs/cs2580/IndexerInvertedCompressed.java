package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends IndexerCommon implements	Serializable {

	private static final long serialVersionUID = -7002359116603747368L;

	private Map<Integer, ArrayList<Short>> _index;
	private Map<Integer, ArrayList<Integer>> _skipPointer;

	// Back-up variables for serializable file write.
	protected Vector<Document> t_documents;
	protected Map<String, Integer> t_dictionary;
	protected int t_numDocs;
	protected long t_totalTermFrequency;

	public IndexerInvertedCompressed(Options options) {
		super(options);
		_index = new TreeMap<Integer, ArrayList<Short>>();
		_skipPointer = new HashMap<Integer, ArrayList<Integer>>();

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
			} else {
				postingId = _dictionary.size() + 1;
				_dictionary.put(token, _dictionary.size() + 1);
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

			// Assume all documents are processed sequentially. It means that
			// all delta value of document id are 1 (0x81)
			postingList.add((short) 0x81);

			// add encoded # of occurrence after doc id
			for (short v : encodeVbyte(positionsOfWordInCurrentDoc.size())) {
				postingList.add(v);
			}

			// add actual positions after # of occurrence
			int shortOffset = 1;
			for (int val : positionsOfWordInCurrentDoc) {
				for (short v : encodeVbyte(val)) {
					shortOffset++;
					postingList.add(v);
				}
			}

			if (!_skipPointer.containsKey(wordId)) {
				_skipPointer.put(wordId, new ArrayList<Integer>());
			}

			_skipPointer.get(wordId).add(docId);
			_skipPointer.get(wordId).add(shortOffset);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		// TODO Auto-generated method stub
		return 0;
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

	// Wirte memory data into file
	// after 1000 documents processing, it saved in one file
//	private int tmpId = 0;

	@Override
	public void writeToFile(int fileIdx) throws IOException, ClassNotFoundException {
		if (_index.isEmpty())
			return;
		String indexFile = _options._indexPrefix + "/tmp_" + fileIdx;
		ObjectOutputStream writer = new ObjectOutputStream(
				new FileOutputStream(indexFile));
		writer.writeObject(_index);
		_index.clear();
		writer.close();
//		tmpId++;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void mergeFile() throws IOException, ClassNotFoundException {
		File folder = new File(_options._indexPrefix);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile() && file.getName().contains("tmp_")) {
				ObjectInputStream tmpFileReader = new ObjectInputStream(
						new FileInputStream(file));
				_index = (TreeMap<Integer, ArrayList<Short>>) tmpFileReader.readObject();
				tmpFileReader.close();

				for (int i = 0; i < MAXCORPUS; i++) {
					// Read corpus file
					String indexFile = _options._indexPrefix + "/corpus_" + i
							+ ".idx";
					ObjectInputStream reader = new ObjectInputStream(
							new FileInputStream(indexFile));
					System.out.println("Write " + indexFile);

					Map<Integer, ArrayList<Short>>_tmpIndex = (TreeMap<Integer, ArrayList<Short>>) reader
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
									ArrayList<Short> tmp = new ArrayList<Short>();
									_tmpIndex.put(idx, tmp);
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
				file.delete();
			}
		}
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
}
