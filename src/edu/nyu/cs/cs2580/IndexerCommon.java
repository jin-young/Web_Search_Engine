package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
    protected Map<String, Document> _documents = new HashMap<String, Document>();

    // Maps each term to their integer representation
    protected Map<String, Integer> _dictionary = new TreeMap<String, Integer>();

    protected Map<Integer, Integer> _cachedIndex = new HashMap<Integer, Integer>();

    protected Map<Integer, Document> pageRanks = null;
    protected Map<Integer, Document> numViews = null;
    
    // Provided for serialization
    public IndexerCommon() {
    }

    // The real constructor
    public IndexerCommon(Options options) {
        super(options);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void constructIndex() throws IOException {
        // load mining data
        try {
            pageRanks = (Map<Integer, Document>)new CorpusAnalyzerPagerank(this._options).load();
            numViews = (Map<Integer, Document>)new LogMinerNumviews(this._options).load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during load page rank / num view info");
        }
        
        // Get All Files list in the Corpus Folder (data/wiki)
        File folder = new File(_options._corpusPrefix);
        File[] listOfFiles = folder.listFiles();

        int count = 0;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                processDocument(file);
                // write to file
                if (count >= DIV && (count % DIV == 0)) {
                    writeToFile(count / DIV);
                }
                count++;
            }
        }

        int lastRound;
        if (count % DIV != 0) {
            lastRound = (count / DIV) + 1;
            writeToFile((count / DIV) + 1);
        } else {
            lastRound = count / DIV;
        }

        mergePartialIndex(lastRound);

        writeIndexerToFile();
    }

    protected String getPartialIndexName(int idx, int round) {
        String indexPrefix = _options._indexPrefix + "/index_";
        return indexPrefix + String.format("%02d", idx) + "_" + round + ".idx";
    }

    protected String getPartialIndexName(int idx) {
        String indexPrefix = _options._indexPrefix + "/index_";
        return indexPrefix + String.format("%02d", idx) + ".idx";
    }

    protected abstract void mergePartialIndex(int lastRound);

    /**
     * Get Pure content from file
     * @param file
     * @return content
     */
    public String getFileContent(File file) {
        org.jsoup.nodes.Element body = null;
        try{
            body = Jsoup.parse(file, "UTF-8", _options._corpusPrefix + "/" + file.getName()).body();
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
        } catch (IOException e) {
            System.err.println("Error Occurred while process document '" + file.getName() + "'");
            e.printStackTrace();
            System.exit(1);
        }
        return body.text();
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
        System.out.println(did + ". " + file.getName());

        String content = getFileContent(file);
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
        
        doc.setNumViews(numViews.get(doc.getTitle()).getNumViews());
        doc.setPageRank(numViews.get(doc.getTitle()).getPageRank());
        
        _documents.put(file.getName(), doc);
        ++_numDocs;        
    }

    /**
     * After 1000 document reading, input _index into file to flush memory
     **/
    public abstract void writeToFile(int round);

    /**
     * After making index files, save _dictionary and other data into file
     **/
    public abstract void writeIndexerToFile() throws IOException;

    /**
     * Next Phrase position
     * 
     * @param query
     *            (contain phrase)
     * @param document
     *            id
     * @param current
     *            position
     **/
    public abstract int nextPhrase(String phrase, int docid);

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

    protected abstract Vector<Integer> retriveDocList(String word) throws IOException, ClassNotFoundException;

    // galloping search
    protected int next(String word, int current) throws IOException, ClassNotFoundException {

        int low, high, jump;
        int idx = _dictionary.get(word);

        Vector<Integer> docList = retriveDocList(word);
        int lt = docList.size() - 1;

        if (docList.isEmpty() || docList.lastElement() <= current)
            return -1;

        if (docList.firstElement() > current) {
            _cachedIndex.put(idx, 1);
            return docList.firstElement();
        }

        if (_cachedIndex.containsKey(idx) && _cachedIndex.get(idx) > 1
                        && docList.get(_cachedIndex.get(idx) - 1) <= current)
            low = _cachedIndex.get(idx) - 1;
        else
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

    protected int binarySearch(Vector<Integer> docList, int low, int high, int current) {
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
        if (docs.isEmpty())
            return false;
        int docid = docs.get(0);
        for (int comp : docs)
            if (comp != docid)
                return false;
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

    protected ObjectOutputStream createObjOutStream(String filePath) {
        try {
            return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath), 1024));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during object output stream creation");
        }
    }

    protected static ObjectInputStream createObjInStream(String filePath) {
        try {
            return new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath), 1024));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during object output stream creation");
        }
    }

    protected void writeFinalIndex(int idx, Object target) {
        // write final index
        ObjectOutputStream writer = null;
        try {
            System.out.println("Writing final index " + idx);
            writer = createObjOutStream(getPartialIndexName(idx));
            writer.writeObject(target);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during writing final index");
        }
    }

    protected void cleaningPartialIndex(int idx, int lastRound) {
        System.out.println("Cleaning partial index files");
        for (int round = 1; round <= lastRound; round++) {
            File partialIdx = new File(getPartialIndexName(idx, round));
            if (partialIdx.exists()) {
                partialIdx.delete();
            }
        }
    }
    
    @Override
    public Document nextDoc(Query query, int curDocId) {
        Vector<Integer> _nextDocIds = new Vector<Integer>();
        int nextDocId = -1;

        // find next document for each query
        for (String token : query._tokens) {
            try {
                if (token.contains(" "))
                    nextDocId = nextPhrase(token, curDocId);
                else
                    nextDocId = next(token, curDocId);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } 
            if (nextDocId == -1) return null;
            _nextDocIds.add(nextDocId);
        }
        // found!
        if (equal(_nextDocIds)) return getDoc(_nextDocIds.get(0));

        // search next
        return nextDoc(query, Max(_nextDocIds) - 1);
    }
}
