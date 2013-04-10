package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
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
    
    private SkipPointer[] _loadedSkipPointer;
    //private int[] _skipPointerIdxs;
    private CompressedIndex[] _loadedIndex;
    //private int[] _indexIdxs;
    //private int cacheSize = 30;
    
    private Map<Integer, Document> _documentsById = null;
    private Map<String, TreeMap<Integer, ArrayList<Integer>>> _phraseDocMap = null;
    
    public IndexerInvertedCompressed(Options options) {
        super(options);
        _index = new CompressedIndex();
        _skipPointer = new SkipPointer();
        lastProcessedDocInfo = new HashMap<Integer, Integer[]>();

        System.out.println("Using Indexer: " + this.getClass().getSimpleName());
    }
    
    protected TreeMap<Integer, ArrayList<Integer>> gerPhraseDocs(String phrase) {
        
        // If it is already existed in the _phraseDocMap, return this.
        if(_phraseDocMap.containsKey(phrase))
            return _phraseDocMap.get(phrase);
        
        String[] words = phrase.split("\\s+");
        int firstId = _dictionary.get(words[0]);
        
        Vector<Integer> candidates = null;
        
        for(String word : words) {
            if(candidates == null)
                candidates = retriveDocList(word);
            else {
                Vector<Integer> ca = retriveDocList(word);
                candidates.retainAll(ca);
            }
        }
        
        TreeMap<Integer, ArrayList<Integer>> found = new TreeMap<Integer, ArrayList<Integer>>();
        
        for(int docId : candidates) {
            //posting consist of (doc id, frequency, [positions])
            ArrayList<Integer> posting = retrivePosting(firstId, docId);
            if(posting != null && !posting.isEmpty()) {
                for(int position : posting.subList(2, posting.size())) {
                    if(findPhraseInDoc(position, docId, Arrays.copyOfRange(words, 1, words.length), candidates) > 0) {
                        //System.out.println(_documentsById.get(docId).getTitle() + " includes the given phrase!!");
                        if(!found.containsKey(docId)) {
                            found.put(docId, new ArrayList<Integer>());
                        }
                        
                        found.get(docId).add(position);
                    }
                }
            }
        }
        
        _phraseDocMap.put(phrase, found);
        
        return found;
    }
 
    protected int findPhraseInDoc(int prevPosition, int targetDocId, String[] words, final Vector<Integer> searchRange) {
        int wordId = _dictionary.get(words[0]);
        int result = -1;
        
        Vector<Integer> candidates = retriveDocList(words[0]);
        candidates.retainAll(searchRange);
        
        for(int docId : candidates) {
            //posting consist of (doc id, frequency, [positions])
            ArrayList<Integer> posting = retrivePosting(wordId, docId);
            if(posting != null && !posting.isEmpty()) {
                for(int position : posting.subList(2, posting.size()-1)) {
                    if(words.length == 1) {
                        if(position - prevPosition == -1) {
                            result = position;
                        }
                    } else {
                        result = findPhraseInDoc(position, docId, Arrays.copyOfRange(words, 1, words.length), searchRange);
                    }
                    if(result > 0) {
                        break;
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Corpus Term Frequency
     * @param : String term
     * @return : total # of term appearance in corpus
     **/
    @Override
    public int corpusTermFrequency(String term) {
        int frequency = 0;
        
        if(_dictionary.get(term) == null)
            return frequency;
        
        if(term.contains(" ")) {
            Map<Integer, ArrayList<Integer>> result = gerPhraseDocs(term);
            if(result != null && !result.isEmpty()) {
                for(ArrayList<Integer> positions : result.values()) {
                    frequency += positions.size();
                }
            }
        } else {
            int wordId = _dictionary.get(term);
            ArrayList<Short> list = null;
            list = getPostingList(wordId);
    
            ArrayList<Integer> skipInfo = getSkipInfo(wordId);
            
            int startPoint = 0;
            for (int i = 0; i < skipInfo.size(); i = i + 2) {
                frequency += ByteAlignUtil.howManyAppeared(startPoint, list);
                startPoint = skipInfo.get(i + 1);
            }
        }
        return frequency;
    }

    /**
     * Document Term Frequency
     * @param : String term, String url
     * @return : total # of term appearance in a document
     **/
    @Override
    public int documentTermFrequency(String term, String url) {
        int docid = 0;
        for (Document doc : _documents.values()) {
            if (doc.getUrl().equals(url)) {
                docid = doc._docid;
                break;
            }
        }
        
        if (docid == 0)
            return 0; // we could not find given doc        
        
        if(term.contains(" ")) {
            // Phrase
            Map<Integer, ArrayList<Integer>> result = gerPhraseDocs(term);
            if(result == null || result.isEmpty()) {
                return 0;
            } else {
                if(result.containsKey(docid)) {
                    return result.get(docid).size();
                } else {
                    return 0;
                }
            }
        } else {
            // Single Word
            int wordId = _dictionary.get(term);
            ArrayList<Integer> posting = retrivePosting(wordId, docid);
            
            if(posting == null || posting.isEmpty())
                return 0;
            else {
                //second element of posting is number of occurrence
                return posting.get(1).intValue(); 
            }
        }
    }
    
    @Override
    public int nextPhrase(String phrase, int prevDocId) {
        TreeMap<Integer, ArrayList<Integer>> phraseDocMap = gerPhraseDocs(phrase);
        
        for(Integer docId : phraseDocMap.keySet()) {
            if(prevDocId < docId) return docId;
        }
        
        return -1;
    }
    
    // ///////////////////////////////////////////////////////////////////////////////////////
    // //////////////// MAYBE CONVERTED WELL
    // ///////////////////////////////////////////////////////////////////////////////////////
    
    protected Vector<Integer> retriveDocList(String word) {
        Object result = _dictionary.get(word);
        
        Vector<Integer> docList = new Vector<Integer>();

        if(result != null) {
            int idx = ((Integer)result).intValue();
            for (int i = 0; i < getSkipInfo(idx).size(); i = i + 2) {
                docList.add(getSkipInfo(idx).get(i));
            }
        }

        return docList;
    }
    
    private Map<Integer, Map<Integer, ArrayList<Integer>>> postingCache
     = new HashMap<Integer, Map<Integer, ArrayList<Integer>>>();
    
    protected ArrayList<Integer> retrivePosting(int wordId, int docId) {
        if(postingCache.containsKey(wordId)) {
            if(postingCache.get(wordId).containsKey(docId))
                return postingCache.get(wordId).get(docId);
        }
        
        ArrayList<Integer> skipInfo = getSkipInfo(wordId);
        if(skipInfo == null || skipInfo.isEmpty()) {
            return null;
        } else {
            int i = 0;
            for (; i < skipInfo.size(); i = i + 2) {
                if (skipInfo.get(i) == docId) {
                    break;
                }
            }
            
            if (i > skipInfo.size())
                return null; // we could not find given doc
            
            int startPosition = skipInfo.get(i + 1);
            
            ArrayList<Short> postingList = getPostingList(wordId);
            ArrayList<Integer> result = ByteAlignUtil.getPosting(startPosition, postingList);
            
            if(!postingCache.containsKey(wordId)) {
                postingCache.put(wordId, new HashMap<Integer, ArrayList<Integer>>());
            }
            
            postingCache.get(wordId).put(docId, result);
            
            return result;
        }
    }
    
    /**
     * Corpus Doc Frequency By Term
     * @param : String term
     * @return : total # of documents with term
     **/
    @Override
    public int corpusDocFrequencyByTerm(String term) {
        if(term.contains(" ")){ 
            // Phrase
            Map<Integer, ArrayList<Integer>> found = gerPhraseDocs(term);
            if(found == null)
                return 0;
            else {
                return -10000;
            }
        }else{ 
            // Single Word
            return _dictionary.containsKey(term) ? (getSkipInfo(term).size() / 2) : 0;
        }
    }
    
    @Override
    public Document getDoc(int docid) {
        return _documentsById.get(docid);
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
    
    protected ArrayList<Integer> getSkipInfo(String term) {
        if(_dictionary.containsKey(term)) 
            return getSkipInfo(_dictionary.get(term));
        else
            return null;
    }
    
    protected ArrayList<Integer> getSkipInfo(int wordId) {
        int corpusId = wordId % MAXCORPUS;
        //int cacheId = corpusId % cacheSize;

        /*
        if(_skipPointerIdxs[cacheId] != -1) {
            if(_skipPointerIdxs[cacheId] == corpusId) {
                //System.out.println("CACHE HIT: Use skip pointer #" + corpusId);
            } else {
                //System.out.println("CACHE MISS: Load skip pointer #" + corpusId);
                _loadedSkipPointer[cacheId] = loadSkipPointer(corpusId);
            }
        } else {
            //System.out.println("CACHE MISS: Load skip pointer #" + corpusId);
            _loadedSkipPointer[cacheId] = loadSkipPointer(corpusId);
        }

        _skipPointerIdxs[cacheId] = corpusId;
        */
        
        return _loadedSkipPointer[corpusId].get(wordId);
    }
    
    protected ArrayList<Short> getPostingList(String term) {
        if(_dictionary.containsKey(term)) 
            return getPostingList(_dictionary.get(term));
        else
            return null;
    }    
    
    private ArrayList<Short> getPostingList(int wordId) {
        int corpusId = wordId % MAXCORPUS;
        //int cacheId = corpusId % cacheSize;
        
        /*
        if(_indexIdxs[cacheId] != -1) {
            if(_indexIdxs[cacheId] == corpusId) {
                //System.out.println("CACHE HIT: Use compressed index #" + corpusId);
            } else {
                //System.out.println("CACHE MISS: compressed index #" + corpusId);
                _loadedIndex[cacheId] = loadIndex(corpusId);
            }
        } else {
            System.out.println("CACHE MISS: Load compressed index #" + corpusId);
            _loadedIndex[cacheId] = loadIndex(corpusId);
        }

        _indexIdxs[cacheId] = corpusId;
        */
        
        return _loadedIndex[corpusId].get(wordId);
    }

    @Override
    public void loadIndex() throws IOException, ClassNotFoundException {
        ObjectInputStream reader = createObjInStream(getIndexerFileName());
        IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();
        reader.close();
        System.out.println("Load Indexer from: " + getIndexerFileName());

        this._documents = loaded.t_documents;
        this._dictionary = loaded.t_dictionary;
        this._numDocs = loaded.t_numDocs;
        this._totalTermFrequency = loaded.t_totalTermFrequency;

        _documentsById = new HashMap<Integer, Document>();
        
        for(Document d : _documents.values()) {
            _documentsById.put(d._docid, d);
        }
        
        _phraseDocMap = new HashMap<String, TreeMap<Integer, ArrayList<Integer>>>();

        // CACHE for improve performance
        _loadedSkipPointer = new SkipPointer[MAXCORPUS];
        _loadedIndex = new CompressedIndex[MAXCORPUS];
        //_skipPointerIdxs = new int[cacheSize];
        //_indexIdxs = new int[cacheSize];
        
        for(int i=0; i<MAXCORPUS; i++) {
            System.out.println("Load partial index " + i);
            _loadedIndex[i] = loadIndex(i);
            System.out.println("Load partial skip pointer " + i);
            _loadedSkipPointer[i] = loadSkipPointer(i);
            //_skipPointerIdxs[i] = i;
            //_indexIdxs[i] = i;
        }
        
        System.out.println(Integer.toString(_numDocs) + " documents loaded " + "with "
                + Long.toString(_totalTermFrequency) + " terms!");        
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // //////////////// TEST DONE OR NOT NEED TO BE TESTED
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

    protected String getIndexerFileName() {
        return _options._indexPrefix + "/indexer.idx";
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
