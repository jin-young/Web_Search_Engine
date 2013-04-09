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
    protected Map<String, Document> t_documents;
    protected Map<String, Integer> t_dictionary;
    protected int t_numDocs;
    protected long t_totalTermFrequency;

    protected boolean underTest = false;
    protected Map<Integer, Integer[]> lastProcessedDocInfo;
    
    public IndexerInvertedCompressed(Options options) {
        super(options);
        _index = new CompressedIndex();
        _skipPointer = new SkipPointer();
        lastProcessedDocInfo = new HashMap<Integer, Integer[]>();

        DIV = 500;

        System.out.println("Using Indexer: " + this.getClass().getSimpleName());
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
        ObjectInputStream reader = createObjInStream(getIndexerFileName());
        IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();

        if (!underTest)
            System.out.println("Load Indexer from: " + getIndexerFileName());

        this._documents = loaded.t_documents;
        this._dictionary = loaded.t_dictionary;
        this._numDocs = loaded.t_numDocs;
        this._totalTermFrequency = loaded.t_totalTermFrequency;

        reader.close();

        if (!underTest) {
            System.out.println(Integer.toString(_numDocs) + " documents loaded " + "with "
                    + Long.toString(_totalTermFrequency) + " terms!");
        }
    }

    protected String getIndexerFileName() {
        return _options._indexPrefix + "/indexer.idx";
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
        for (Document doc : _documents.values()) {
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
    public void writeIndexerToFile() throws IOException {
        ObjectOutputStream writer = createObjOutStream(getIndexerFileName());

        // back-up variables from Indexer class
        t_documents = _documents;
        t_dictionary = _dictionary;
        t_numDocs = _numDocs;
        t_totalTermFrequency = _totalTermFrequency;

        writer.writeObject(this);
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

    // ///////////////////////////////////////////////////////////////////////////////////////
    // //////////////// TEST DONE OR NOT NEED TO BE TESTED
    // //////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void mergePartialIndex(int lastRound) {
        
        //below is not necessary any longer. save memory!!
        lastProcessedDocInfo.clear(); 
        pageRanks.clear();
        numViews.clear();        
        //above is not necessary any longer. save memory!!
        
        for (int idx = 0; idx < MAXCORPUS; idx++) {
            CompressedIndex finalIndex = new CompressedIndex();
            SkipPointer finalSkipPointer = new SkipPointer();

            for (int round = 1; round <= lastRound; round++) {
                CompressedIndex currIndex = loadIndex(idx, round);
                SkipPointer currSkipPointer = loadSkipPointer(idx, round);
                
                if(currIndex == null || currSkipPointer == null) continue;

                if (finalIndex.isEmpty()) {
                    finalIndex = currIndex;
                    finalSkipPointer = currSkipPointer;

                    continue;
                } else {
                    Integer[] keys = currIndex.keySet().toArray(new Integer[1]);
                    //the reason that using "for (int wordId : keys)" instead of
                    // "for (int wordId : currIndex.keySet())" is to use remove
                    //method in loop. Second method does not allow change length
                    //of collection/list within loop.
                    for (int wordId : keys) {
                        ArrayList<Short> posting = currIndex.remove(wordId);
                        ArrayList<Integer> skipInfo = currSkipPointer.remove(wordId);

                        if (!finalIndex.containsKey(wordId)) {
                            finalIndex.put(wordId, posting);
                            finalSkipPointer.put(wordId, skipInfo);
                        } else {
                            ArrayList<Integer> prevSkipInfo = finalSkipPointer.get(wordId);
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

    protected void writeFinalSkipPointer(int idx, Object target) {
        // write final skip pointer
        ObjectOutputStream writer = null;
        try {
            if (!underTest)
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
        if (!underTest)
            System.out.println("Cleaning partial skip pointer files");

        for (int round = 1; round <= lastRound; round++) {
            File partialIdx = new File(getPartialSkipPointerName(idx, round));
            if (partialIdx.exists()) {
                partialIdx.delete();
            }
        }
    }
    
    protected int lastDocId(ArrayList<Integer> prevSkipInfo) {
        if (prevSkipInfo == null || prevSkipInfo.isEmpty())
            return 0;
        else {
            return prevSkipInfo.get(prevSkipInfo.size() - 2);
        }
    }
    
    protected CompressedIndex loadIndex(int indexId) {
        return loadIndex(indexId, -1);
    }

    protected CompressedIndex loadIndex(int indexId, int round) {
        CompressedIndex index = null;
        String filePath = round > 0 ?
                getPartialIndexName(indexId, round) :
                getPartialIndexName(indexId);
                
        if(new File(filePath).exists()) {
            ObjectInputStream reader = createObjInStream(filePath);
            
            try {
                index = (CompressedIndex) reader.readObject();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error during load partial index");
            }
        } else {
            System.out.println("WARNING: Index File " 
                    + getPartialIndexName(indexId, round) + " does not exist");
        }

        return index;
    }

    protected SkipPointer loadSkipPointer(int indexId) {
        return loadSkipPointer(indexId, -1);
    }
    
    protected SkipPointer loadSkipPointer(int indexId, int round) {
        SkipPointer skip = null;
        
        String filePath = round > 0 ?
                getPartialSkipPointerName(indexId, round) :
                    getPartialSkipPointerName(indexId);
                
        if(new File(filePath).exists()) {
            ObjectInputStream reader = createObjInStream(filePath);
    
            try {
                skip = (SkipPointer) reader.readObject();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error during load partial index");
            }
        } else {
            System.out.println("WARNING: Skip Pointer File " 
                        + getPartialSkipPointerName(indexId, round) + " does not exist");
        }

        return skip;
    }

    public int makeIndex(String content, int docId) {
        Map<Integer, ArrayList<Integer>> wordsPositionsInDoc = wordsPositionsInDoc(content);
        
        int numOfTokens = 0;
        for (int wordId : wordsPositionsInDoc.keySet()) {
            initIndex(wordId);
            initSkipPointer(wordId);
            
            numOfTokens += wordsPositionsInDoc.get(wordId).size();

            int offset = addPositionsToIndex(wordsPositionsInDoc.get(wordId), docId, wordId);
            addSkipInfo(wordId, docId, offset);
            
        }
        return numOfTokens;
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

        //Make sure that all partial index and skip pointer files are generated
        //regardless of empty of tempIndex
        int idx = 0;
        for(int corpusId = 0; corpusId < MAXCORPUS; corpusId++) {
            CompressedIndex tempIndex = new CompressedIndex();
            SkipPointer tempSkipPointer = new SkipPointer();
            
            while(idx < wordIds.length && corpusId == (wordIds[idx]% MAXCORPUS)) {
                tempIndex.put(wordIds[idx], _index.remove(wordIds[idx]));
                tempSkipPointer.put(wordIds[idx], _skipPointer.remove(wordIds[idx]));
                
                idx++;
            }
            
            flushCurrentIndex(tempIndex, corpusId, round);
            flushCurrentSkipPointer(tempSkipPointer, corpusId, round);
        }
    }

    protected void flushCurrentIndex(CompressedIndex tempIndex, int corpusId, int round) {
        ObjectOutputStream writer = null;
        if (tempIndex != null) {
            if (!underTest)
                System.out.println("Save partial index " + corpusId + " of round " + round);

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
        if (sp != null) {
            if (!underTest)
                System.out.println("Save partial skip pointer " + corpusId + " of round " + round);

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

        int nextStart = lastPosition(wordId) + offset;
        _skipPointer.get(wordId).addAll(Arrays.asList(new Integer[] { docId, nextStart }));
        
        recordLastProcessedInfo(wordId, docId, nextStart);
        return nextStart;
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

        int delta = docId - lastDocId(wordId);

        int offset = ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), delta);
        offset += ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), positions.size());

        for (int p : positions) {
            offset += ByteAlignUtil.appendEncodedValueToList(_index.get(wordId), p);
        }
        
        return offset;
    }
    
    protected void recordLastProcessedInfo(int wordId, int docId, int nextStartPosition) {
        //record last processed doc id and next start position
        Integer[] info = null;
        if(lastProcessedDocInfo.containsKey(wordId)) {
            info = lastProcessedDocInfo.get(wordId);
        } else {
            info = new Integer[]{0, 0};
        }
        
        info[0] = docId;
        info[1] = nextStartPosition;
        lastProcessedDocInfo.put(wordId, info);
    }

    protected int lastPosition(int wordId) {
        if (lastProcessedDocInfo.containsKey(wordId)) {
            return lastProcessedDocInfo.get(wordId)[1];
        } else {
            return 0;
        }
    }

    protected int lastDocId(int wordId) {
        if (lastProcessedDocInfo.containsKey(wordId)) {
            return lastProcessedDocInfo.get(wordId)[0];
        } else {
            return 0;
        }
    }

    protected Map<Integer, ArrayList<Integer>> wordsPositionsInDoc(String content) {
        Scanner s = new Scanner(content); // Uses white space by default.

        // word id and position of the word in current doc
        Map<Integer, ArrayList<Integer>> wordsPositions = new HashMap<Integer, ArrayList<Integer>>();
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

    protected String getPartialSkipPointerName(int idx) {
        String indexPrefix = _options._indexPrefix + "/skip_";
        return indexPrefix + String.format("%02d", idx) + ".idx";
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

    public Map<Integer, Integer[]> getLastProcessedDocId() {
        return lastProcessedDocInfo;
    }

    public void setLastProcessedDocId(Map<Integer, Integer[]> lastProcessedDocId) {
        this.lastProcessedDocInfo = lastProcessedDocId;
    }
}
