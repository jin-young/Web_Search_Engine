package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;
/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
    public CorpusAnalyzerPagerank(Options options) {
        super(options);
    }

    /**
     * This function processes the corpus as specified inside {@link _options}
     * and extracts the "internal" graph structure from the pages inside the
     * corpus. Internal means we only store links between two pages that are
     * both inside the corpus.
     * 
     * Note that you will not be implementing a real crawler. Instead, the
     * corpus you are processing can be simply read from the disk. All you need
     * to do is reading the files one by one, parsing them, extracting the links
     * for them, and computing the graph composed of all and only links that
     * connect two pages that are both in the corpus.
     * 
     * Note that you will need to design the data structure for storing the
     * resulting graph, which will be used by the {@link compute} function.
     * Since the graph may be large, it may be necessary to store partial graphs
     * to disk before producing the final graph.
     * 
     * @throws IOException
     */
    @Override
    public void prepare() throws IOException {
        System.out.println("Preparing " + this.getClass().getName());

        File folder = new File(_options._corpusPrefix);
        Map<String, Document> corpus = new HashMap<String, Document>();

        int docId = 0;
        System.out.println("L F");
        for (File f : folder.listFiles()) {
            if (f.isFile()) {
                Document d = new Document(docId++);
                corpus.put(f.getName(), d);
            }
        }
        System.out.println("L F D");

        MapMatrix matrix = buildMatrix(corpus);

        return;
    }
    
    /**
     * This function computes the PageRank based on the internal graph generated
     * by the {@link prepare} function, and stores the PageRank to be used for
     * ranking.
     * 
     * Note that you will have to store the computed PageRank with each document
     * the same way you do the indexing for HW2. I.e., the PageRank information
     * becomes part of the index and can be used for ranking in serve mode.
     * Thus, you should store the whatever is needed inside the same directory
     * as specified by _indexPrefix inside {@link _options}.
     * 
     * @throws IOException
     */
    @Override
    public void compute() throws IOException {
        System.out.println("Computing using " + this.getClass().getName());
        return;
    }

    /**
     * During indexing mode, this function loads the PageRank values computed
     * during mining mode to be used by the indexer.
     * 
     * @throws IOException
     */
    @Override
    public Object load() throws IOException {
        System.out.println("Loading using " + this.getClass().getName());
        return null;
    }

    protected String[] getAchors(String fileName) {
        File f = new File(_options._corpusPrefix + "/" + fileName);
        org.jsoup.nodes.Element body;
        try {
            body = Jsoup.parse(f, "UTF-8", _options._corpusPrefix + "/" + fileName).body();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during anchor gathering");
        }

        Elements elems = body.select("a");
        String[] result = new String[elems.size()];
        for(int i=0; i<elems.size(); i++) {
            result[i] = elems.get(i).attr("href");
        }

        return result;
    }

    protected MapMatrix buildMatrix(Map<String, Document> corpus) throws IOException {
        System.out.println("G M");
        MapMatrix matrix = new MapMatrix();
        System.out.println("G M D");

        int count = 1;
        for (String fName : corpus.keySet()) {
            if (count % 1000 == 0)
                System.out.println(count + " have been processed");

            Document currentDoc = corpus.get(fName);
            
            for (String href : getAchors(fName)) {
                if (corpus.containsKey(href)) {
                    Map<Integer, Float> rows = null;
                    Document targetDoc = corpus.get(href);
                    if (matrix.containsKey(currentDoc._docid)) {
                        rows = matrix.get(currentDoc._docid);
                    } else {
                        rows = new HashMap<Integer, Float>();
                        matrix.put(currentDoc._docid, rows);
                    }

                    if (rows.containsKey(targetDoc._docid)) {
                        rows.put(targetDoc._docid, rows.get(targetDoc._docid) + 1.0f);
                    } else {
                        rows.put(targetDoc._docid, 1.0f);
                    }
                }
            }

            count++;
        }
        return matrix;
    }
    
    //Actually, below algorithm looks like having performance O(n^3) if worst case is given.
    //However, it does not happen in real world because each document contains not much number of links.
    protected MapMatrix matrixMulti(MapMatrix m1, MapMatrix m2) {
        MapMatrix result = new MapMatrix();
        
        for(Integer docId : m1.keySet()) {
            for(Integer key1 : m1.get(docId).keySet()) {
                if( m2.containsKey(key1)) {
                    for(Integer key2 : m2.get(key1).keySet()) {
                        float val = m1.get(docId).get(key1) * m2.get(key1).get(key2);
                        if(result.containsKey(docId)) {
                            Map<Integer, Float> inner = result.get(docId);
                            
                            if(inner.containsKey(key2)) {
                                inner.put(key2, inner.get(key2) + val);
                            } else {
                                inner.put(key2, val);
                            }
                            
                        } else {
                            Map<Integer, Float> inner = new HashMap<Integer, Float>();
                            inner.put(key2, val);
                            result.put(docId, inner);
                        }
                    }
                }
            }
        }
        
        return result;
    }    
    
    public MapMatrix matrixTimesScala(float v, MapMatrix m1) {
        MapMatrix result = new MapMatrix();
        
        for(Integer docId : m1.keySet()) {
            Map<Integer, Float> inner = new HashMap<Integer, Float>();
            result.put(docId, inner);
            for(Integer key1 : m1.get(docId).keySet()) {
                inner.put(key1, m1.get(docId).get(key1) * v);
            }
        }
            
        return result;
    }
}
