package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
	@Override
	public void writeToFile(int round) {
	    Integer[] idxList = _index.keySet().toArray(new Integer[1]);
        Arrays.sort(idxList, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Math.abs(o1.intValue() % MAXCORPUS) - Math.abs(o2.intValue() % MAXCORPUS);
            }
            
        });
        
        int corpusId = Math.abs(idxList[0] % MAXCORPUS);
        ObjectOutputStream writer = null;
        
        Map<Integer, Vector<Integer>> tempIndex = null;

        for(int wId : idxList) {
            if( corpusId != Math.abs(wId % MAXCORPUS) ) {
                if(! tempIndex.isEmpty() ) {
                    System.out.println("Writing partial index " + corpusId);
                    try {
                        writer = createObjOutStream(getPartialIndexName(corpusId, round));
                        writer.writeObject(tempIndex);
                        writer.close();
                        writer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Error during partial index writing");
                    }
                    
                    tempIndex.clear();
                    tempIndex = null;
                }
                
                corpusId = Math.abs(wId % MAXCORPUS);
            }
            
            if(tempIndex == null) {
                tempIndex = new HashMap<Integer, Vector<Integer>>();
            }
            
            tempIndex.put(wId, _index.remove(wId));
        }
        
        // last partial index
        System.out.println("Writing partial index " + corpusId);
        try {
            writer = createObjOutStream(getPartialIndexName(corpusId, round));
            writer.writeObject(tempIndex);
            writer.close();
            writer = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during partial index writing");
        }
        
        _index.clear();
	}
	
    @SuppressWarnings("unchecked")
    @Override
    protected void mergePartialIndex(int lastRound) {
        ObjectInputStream reader = null;
        
        for(int idx = 0; idx < MAXCORPUS; idx++) {
            Map<Integer, Vector<Integer>> finalIndex = 
                    new HashMap<Integer, Vector<Integer>>();
            
            for(int round=1; round <= lastRound; round++) {
                File partialIdx = new File(getPartialIndexName(idx, round));
                if(partialIdx.exists()) {
                    System.out.println("Merging partial index " + idx + " of round " + round);
                    reader = createObjInStream(partialIdx.getAbsolutePath());
                    try {
                        Map<Integer, Vector<Integer>> pIdx = 
                                (Map<Integer, Vector<Integer>>)reader.readObject();
                        for(int wordId : pIdx.keySet()) {
                            if(finalIndex.containsKey(wordId)) {
                                Vector<Integer> old = finalIndex.get(wordId);
                                Vector<Integer> curr = pIdx.get(wordId);
                                
                                old.addAll(curr);
                                
                                //do we need below line, really, again?
                                finalIndex.put(wordId, old);
                            } else {
                                finalIndex.put(wordId, pIdx.get(wordId));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Error during reading partial index");
                    }
                }
            }
            
            writeFinalIndex(idx, finalIndex);
            cleaningPartialIndex(idx, lastRound);
        }
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
	 * Not used in Document Only Indexer
	 */
	@Override
	public int nextPhrase(String phrase, int docid) {
		return -1;
	}

	@Override
	public Document nextDoc(Query query, int curDocId) {

	    // If query contains phrase, convert it as conjunctive words.
	    int tokenSize = query._tokens.size();
	    for(int i=0; i<tokenSize; i++){
	        if(query._tokens.get(i).contains(" ")){
	            Scanner scan = new Scanner(query._tokens.get(i));
	            while(scan.hasNext())
	                query._tokens.add( scan.next() );
	        }
	    }
	    
		Vector<Integer> _nextDocIds = new Vector<Integer>();
		int nextDocId = -1;

		// find next document for each query
		for (String token : query._tokens) {
			try {
			    nextDocId = next(token, curDocId);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} 
			if (nextDocId == -1)  return null;
			_nextDocIds.add(nextDocId);
		}

		// found!
		if (equal(_nextDocIds))  return _documents.get(_nextDocIds.get(0));
		
		// search next
		return nextDoc(query, Max(_nextDocIds) - 1);
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
		String indexFile = _options._indexPrefix + "/index_" + pageNum
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
