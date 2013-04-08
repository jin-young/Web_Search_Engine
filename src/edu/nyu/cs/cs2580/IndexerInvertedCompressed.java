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
    
    protected boolean underTest = false;

    public IndexerInvertedCompressed(Options options) {
        super(options);
        _index = new CompressedIndex();
        _skipPointer = new SkipPointer();
        
        DIV = 100;

        System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }

    @Override
    protected void mergePartialIndex(int lastRound) {
        
        for(int idx = 0; idx < MAXCORPUS; idx++) {
            CompressedIndex finalIndex = new CompressedIndex();
            SkipPointer finalSkipPointer = new SkipPointer();
            
            for(int round=1; round <= lastRound; round++) {
                CompressedIndex currIndex = loadIndex(idx, round);
                SkipPointer currSkipPointer = loadSkipPointer(idx, round);
                
                if(finalIndex.isEmpty()) {
                    finalIndex = currIndex;
                    finalSkipPointer = currSkipPointer;
                    
                    continue;
                } else {
                    for(int wordId : currIndex.keySet()) {
                        ArrayList<Short> posting = currIndex.get(wordId);
                        ArrayList<Integer> skipInfo = currSkipPointer.get(wordId);
                        
                        if(!finalIndex.containsKey(wordId)) {
                            finalIndex.put(wordId, posting);
                            finalSkipPointer.put(wordId, skipInfo);
                        } else {
                            ArrayList<Integer> prevSkipInfo = finalSkipPointer.get(wordId);
                            
                            //because of delta encoding, we need to modify first doc id
                            //in a posting list
                            ArrayList<Short> adjPosting = adjustPostingHeader(posting, prevSkipInfo);
                            
                            //skip info also should be adjusted
                            adjustSkipInfo(skipInfo, adjPosting.size() - posting.size());
                            
                            finalIndex.get(wordId).addAll(posting);
                            prevSkipInfo.addAll(skipInfo);
                        }
                    }
                }
                
            }
            
            writeFinalIndex(idx, finalIndex);
            cleaningPartialIndex(idx, lastRound);
            
            writeFinalSkipPointer(idx, finalSkipPointer);
            cleaningPartialSkipPointer(idx, lastRound);
        }
    }
    
    protected ArrayList<Short> adjustPostingHeader(ArrayList<Short> posting, ArrayList<Integer> prevSkipInfo) {
        int lastDocId = lastDocId(prevSkipInfo);
        int headDocId = ByteAlignUtil.decodeVbyte(0, posting);
        int deltaId = headDocId - lastDocId;
        
        short[] encodedDeltaId = ByteAlignUtil.encodeVbyte(deltaId);
        short[] endcodedHeadDocId = ByteAlignUtil.encodeVbyte(headDocId);
        
        for(int i=0; i<endcodedHeadDocId.length; i++) {
            posting.remove(i);
        }
        
        ArrayList<Short> header = new ArrayList<Short>();
        for(short v : encodedDeltaId)
            header.add(v);
        
        header.addAll(posting);
        
        return header;
    }

    protected int lastDocId(ArrayList<Integer> prevSkipInfo) {
        if(prevSkipInfo == null || prevSkipInfo.isEmpty())
            return 0;
        else {
            return prevSkipInfo.get(prevSkipInfo.size() - 2);
        }
    }

    protected void adjustSkipInfo(ArrayList<Integer> skipInfo, int changedLength) {
        for(int i=0; i<skipInfo.size(); i++) {
            if(i % 2 == 1) {
                skipInfo.set(i, skipInfo.get(i) + changedLength);
            }
        }
    }

    protected String getPartialSkipPointerName(int idx) {
        String indexPrefix = _options._indexPrefix + "/skip_";
        return indexPrefix + String.format("%02d", idx) + ".idx";
    }
    
    protected void writeFinalSkipPointer(int idx, Object target) {
        //write final skip pointer
        ObjectOutputStream writer = null;
        try {
            if(!underTest)
                System.out.println("Writing final skip pointer " + idx);
            
            writer = createObjOutStream(getPartialSkipPointerName(idx));
            writer.writeObject(target);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during writing final skip pointer");
        }
    }
    
    protected void cleaningPartialSkipPointer(int idx, int lastRound) {
        if(!underTest)
            System.out.println("Cleaning partial skip pointer files");
        
        for(int round=1; round <= lastRound; round++) {
            File partialIdx = new File(getPartialSkipPointerName(idx, round));
            if(partialIdx.exists()) {
                partialIdx.delete();
            }
        }
    }    
    
    protected CompressedIndex loadIndex(int indexId, int round) {
        ObjectInputStream reader = 
                createObjInStream(getPartialIndexName(indexId, round));
        CompressedIndex index = null;
        
        try {
            index = (CompressedIndex)reader.readObject();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during load partial index");
        }
        
        return index;
    }
    
    protected SkipPointer loadSkipPointer(int indexId, int round) {
        ObjectInputStream reader = 
                createObjInStream(getPartialSkipPointerName(indexId, round));
        SkipPointer skip = null;
        
        try {
            skip = (SkipPointer)reader.readObject();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during load partial index");
        }
        
        return skip;
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

        int corpusId = wordIds[0] % MAXCORPUS;
        for (int wordId : wordIds) {
            if (corpusId != (wordId % MAXCORPUS)) {
                flushCurrentIndex(tempIndex, corpusId, round);
                flushCurrentSkipPointer(tempSkipPointer, corpusId, round);
                
                corpusId = wordId % MAXCORPUS;
                
                tempIndex = new CompressedIndex();
                tempSkipPointer = new SkipPointer();
            }

            tempIndex.put(wordId, _index.remove(wordId));
            tempSkipPointer.put(wordId, _skipPointer.remove(wordId));
        }
        
        //last partial index and skip pointer
        flushCurrentIndex(tempIndex, corpusId, round);
        flushCurrentSkipPointer(tempSkipPointer, corpusId, round);
    }
    
    protected void flushCurrentIndex(CompressedIndex tempIndex, int corpusId, int round) {
        ObjectOutputStream writer = null;
        if (tempIndex != null && !tempIndex.isEmpty()) {
            if(!underTest)
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
            if(!underTest)
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
