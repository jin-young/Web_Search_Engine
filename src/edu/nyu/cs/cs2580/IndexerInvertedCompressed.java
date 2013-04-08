package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends IndexerCommon implements Serializable {

    private static final long serialVersionUID = -7002359116603747368L;

    private CompressedIndex _index;
    private SkipPointer _skipPointer;

    // Back-up variables for serializable file write.
    protected Vector<Document> t_documents;
    protected Map<String, Integer> t_dictionary;
    protected int t_numDocs;
    protected long t_totalTermFrequency;

    public IndexerInvertedCompressed(Options options) {
        super(options);
        _index = new CompressedIndex();
        _skipPointer = new SkipPointer();

        System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }

    @Override
    protected void mergePartialIndex(int lastRound) {
        ObjectInputStream reader = null;
        
        for(int idx = 0; idx < MAXCORPUS; idx++) {
            CompressedIndex finalIndex = new CompressedIndex();
            
            for(int round=1; round <= lastRound; round++) {
                File partialIdx = new File(getPartialIndexName(idx, round));
                if(partialIdx.exists()) {
                    System.out.println("Merging partial index " + idx + " of round " + round);
                    reader = createObjInStream(partialIdx.getAbsolutePath());
                    try {
                        CompressedIndex pIdx = (CompressedIndex)reader.readObject();
                        for(int wordId : pIdx.keySet()) {
                            if(finalIndex.containsKey(wordId)) {
                                ArrayList<Short> old = finalIndex.get(wordId);
                                ArrayList<Short> curr = pIdx.get(wordId);
                                
                                /*
                                for(int docId : curr.keySet()) {
                                    if(old.containsKey(docId)) {
                                        ArrayList<Integer> oldPositions = old.get(docId);
                                        ArrayList<Integer> currPositions = curr.get(docId);
                                        
                                        oldPositions.addAll(currPositions);
                                        
                                        //do we need below line, really?
                                        old.put(docId, oldPositions);
                                    } else {
                                        old.put(docId, curr.get(docId));
                                    }
                                }
                                */
                                
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
            
            writeFinalINdex(idx, finalIndex);
            cleaningPartialIndex(idx, lastRound);
        }
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

    private ArrayList<Short> getDocArray(int idx) throws IOException, ClassNotFoundException {

        if (_index.containsKey(idx))
            return _index.get(idx);

        int pageNum = idx % MAXCORPUS;

        // Read corpus file
        String indexFile = _options._indexPrefix + "/index_" + pageNum + ".idx";
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
        CompressedIndex _tmpIndex = (CompressedIndex) reader.readObject();
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
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(dicFile));
        IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();
        System.out.println("Load dictionary from: " + dicFile);

        this._documents = loaded.t_documents;
        this._dictionary = loaded.t_dictionary;
        this._numDocs = loaded.t_numDocs;
        this._totalTermFrequency = loaded.t_totalTermFrequency;
        this._skipPointer = loaded._skipPointer;

        reader.close();

        System.out.println(Integer.toString(_numDocs) + " documents loaded " + "with "
                + Long.toString(_totalTermFrequency) + " terms!");
    }

    @Override
    public int corpusDocFrequencyByTerm(String term) {
        return _dictionary.containsKey(term) ? (_skipPointer.get(_dictionary.get(term)).size() / 2) : 0;
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
            frequency += ByteAlignUtil.decodeVbyte(ByteAlignUtil.nextPosition(startPoint, list), list);
            startPoint = skipInfo.get(i + 1);
        }

        return frequency;
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

        return ByteAlignUtil.decodeVbyte(ByteAlignUtil.nextPosition(i, list), list);
    }

    @Override
    public void writeDicToFile() throws IOException {
        String dicFile = _options._indexPrefix + "/dictionary.idx";
        ObjectOutputStream writer = createObjOutStream(dicFile);
        // back-up variables from Indexer class
        // t_documents = _documents;
        // t_dictionary = _dictionary;
        t_numDocs = _numDocs;
        t_totalTermFrequency = _totalTermFrequency;

        writer.writeObject(_dictionary);
        writer.close();
    }

    @Override
    public int nextPhrase(String phrase, int docid) {

        return -1;
        /*
         * int docidVer = nextDoc(query, docid - 1)._docid; if (docidVer !=
         * docid) return -1;
         * 
         * Vector<Integer> posList = new Vector<Integer>(); for (int i = 0; i <
         * query._tokens.size(); i++) { int tmpPos =
         * next_pos(query._tokens.get(i), docid, pos); if (tmpPos == -1) return
         * -1; posList.add(tmpPos); } boolean isSuccess = true; for (int i = 1;
         * i < posList.size(); i++) if (posList.get(i - 1) + 1 !=
         * posList.get(i)) isSuccess = false; if (isSuccess) return
         * posList.get(0); return nextPhrase(query, docid, posList.get(1));
         */
    }

    /*
     * private int next_pos(String term, int docid, int pos) { try { int wordId
     * = _dictionary.get(term); ArrayList<Short> posting = getDocArray(wordId);
     * 
     * ArrayList<Integer> skipInfo = _skipPointer.get(wordId); int i = 0; for (;
     * i < skipInfo.size(); i = i + 2) { if (skipInfo.get(i) == docid) { break;
     * } }
     * 
     * if (i > skipInfo.size()) return -1; // we could not find given doc
     * 
     * int howManyOccured = howManyAppeared(i,posting);
     * 
     * ArrayList<Integer> posList = new ArrayList<Integer>(); int positionOfHead
     * = nextPosition(nextPosition(i, posting), posting); for (int j = 0; j <
     * howManyOccured; j++) { posList.add(decodeVbyte(positionOfHead, posting));
     * positionOfHead = nextPosition(positionOfHead, posting); }
     * 
     * for (i = 0; i < posList.size(); i++) { if (posList.get(i) > pos) return
     * posList.get(i); } } catch (IOException ie) {
     * System.err.println(ie.getMessage()); } catch (ClassNotFoundException ce)
     * { System.err.println(ce.getMessage()); } return -1; }
     */
    
    /////////////////////////////////////////////////////////////////////////////////////////
    // //////////////// TEST DONE OR NOT NEED TO BE TESTED //////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    
    public int makeIndex(String content, int docId) {
        SkipPointer wordsPositionsInDoc = wordsPositionsInDoc(content);

        for (int wordId : wordsPositionsInDoc.keySet()) {
            initIndex(wordId);
            initSkipPointer(wordId);

            int offset = addPositionsToIndex(wordsPositionsInDoc.get(wordId), docId, wordId);

            addSkipInfo(wordId, docId, offset);
        }
        return 0;
    }
    
    @Override
    public void writeToFile(int round) {
        if (_index.isEmpty())
            return;

        Integer[] wordIds = _index.keySet().toArray(new Integer[1]);
        Arrays.sort(wordIds, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Math.abs(o1.intValue() % MAXCORPUS) - Math.abs(o2.intValue() % MAXCORPUS);
            }
        });

        CompressedIndex tempIndex = new CompressedIndex();
        SkipPointer tempSkipPointer = new SkipPointer();

        int corpusId = Math.abs(wordIds[0] % MAXCORPUS);
        for (int wordId : wordIds) {
            if (corpusId != (Math.abs(wordId % MAXCORPUS))) {
                flushCurrentIndex(tempIndex, corpusId, round);
                flushCurrentSkipPointer(tempSkipPointer, corpusId, round);
                
                corpusId = Math.abs(wordId % MAXCORPUS);
                
                tempIndex = new CompressedIndex();
                tempSkipPointer = new SkipPointer();
            }

            tempIndex.put(wordId, _index.remove(wordId));
            tempSkipPointer.put(wordId, _skipPointer.remove(wordId));
        }
    }
    
    protected void flushCurrentIndex(CompressedIndex tempIndex, int corpusId, int round) {
        ObjectOutputStream writer = null;
        if (tempIndex != null && !tempIndex.isEmpty()) {
            System.out.println("Save partial index " + corpusId);
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
    }
    
    protected void flushCurrentSkipPointer(SkipPointer sp, int corpusId, int round) {
        ObjectOutputStream writer = null;
        if (sp != null && !sp.isEmpty()) {
            System.out.println("Save partial skip pointer " + corpusId);
            try {
                writer = createObjOutStream(getPartialSkipPointerName(corpusId, round));
                writer.writeObject(sp);
                writer.close();
                writer = null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error during partial skip pointer writing");
            }

            sp.clear();
            sp = null;
        }
    }
    
    protected int addSkipInfo(int wordId, int docId, int offset) {
        initSkipPointer(wordId);
        
        int currentEnd = offset + lastPosition(wordId);
        
        _skipPointer.get(wordId).addAll(Arrays.asList(new Integer[] { docId, currentEnd }));
        
        return currentEnd;
    }
    
    protected void initIndex(int wordId) {
        if (!_index.containsKey(wordId))
            _index.put(wordId, new ArrayList<Short>());
    }

    protected void initSkipPointer(int wordId) {
        if (!_skipPointer.containsKey(wordId))
            _skipPointer.put(wordId, new ArrayList<Integer>());
    }
    
    protected int addPositionsToIndex(ArrayList<Integer> positions, int docId, int wordId) {
        initIndex(wordId);

        // delta encoding
        int offset = ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), docId - lastDocId(wordId));
        offset += ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), positions.size());

        for (int p : positions) {
            offset += ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), p);
        }

        return offset;
    }
    
    protected int lastPosition(int wordId) {
        ArrayList<Integer> skipInfo = _skipPointer.get(wordId);
        if (skipInfo == null || skipInfo.isEmpty())
            return 0;
        else
            return skipInfo.get(skipInfo.size() - 1);
    }

    protected int lastDocId(int wordId) {
        ArrayList<Integer> skipInfo = _skipPointer.get(wordId);
        if (skipInfo == null || skipInfo.isEmpty())
            return 0;
        else
            return skipInfo.get(skipInfo.size() - 2);
    }

    protected SkipPointer wordsPositionsInDoc(String content) {
        Scanner s = new Scanner(content); // Uses white space by default.

        // word id and position of the word in current doc
        SkipPointer wordsPositions = new SkipPointer();
        Map<Integer, Integer> lastPosition = new HashMap<Integer, Integer>();

        int position = 1;

        while (s.hasNext()) {
            String term = trimPunctuation(s.next());

            _stemmer.setCurrent(term);
            _stemmer.stem();
            String token = _stemmer.getCurrent().toLowerCase();
            int postingId = -1;

            if (_dictionary.get(token) != null) {
                postingId = _dictionary.get(token);
            } else {
                postingId = _dictionary.size() + 1;
                _dictionary.put(token, postingId);
            }

            ArrayList<Integer> positions = null;
            if (!wordsPositions.containsKey(postingId)) {
                positions = new ArrayList<Integer>();
                positions.add(position);
                wordsPositions.put(postingId, positions);
            } else {
                positions = wordsPositions.get(postingId);
                // apply delta encoding
                positions.add(position - lastPosition.get(postingId));
            }
            // remember last position because of delta encoding
            lastPosition.put(postingId, position);

            position++;
            ++_totalTermFrequency;
        }
        s.close();

        return wordsPositions;
    }

    protected String trimPunctuation(String term) {
        // remove puncs in tail
        term = term.replaceAll("(.+)\\p{Punct}(\\s|$)", "$1$2");
        // remove puncs in head
        term = term.replaceAll("^\\p{Punct}(.+)(\\s|$)", "$1$2");

        return term;
    }

    protected String getPartialSkipPointerName(int idx, int round) {
        String indexPrefix = _options._indexPrefix + "/skip_";
        return indexPrefix + String.format("%02d", idx) + "_" + round + ".idx";
    }
    
    public SkipPointer getSkipPointer() {
        return _skipPointer;
    }

    public void setSkipPointer(SkipPointer p) {
        this._skipPointer = p;
    }

    public CompressedIndex getIndex() {
        return _index;
    }

    public void setIndex(CompressedIndex _index) {
        this._index = _index;
    }    
}
