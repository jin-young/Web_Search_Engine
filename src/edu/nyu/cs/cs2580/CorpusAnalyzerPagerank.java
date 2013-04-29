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
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;
/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
    protected Map<String, Document> documents = null;
    protected MapMatrix corpusGraph = null;
    
    protected int DIV = 1000;
    protected float lambda = 0.9f;    // 0.1 or 0.9 
    protected int iterateNum = 1;        // 1 or 2
    
    public CorpusAnalyzerPagerank(Options options) {
        super(options);
        documents = new HashMap<String, Document>();
        corpusGraph = new MapMatrix();
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
        
        int docId = 0;
        for (File f : folder.listFiles()) {
            if (f.isFile()) {
                Document d = new Document(docId++);
                //TODO: remove below mock code after implementing all.
                d.setTitle(f.getName());
                d.setPageRank((float)Math.random());
                //TODO: remove above mock code after implementing all.
                documents.put(f.getName(), d);
            }
        }
        
        // Make Corpus Graph
        buildCorpusGraph(documents);
        
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
        System.out.println("Start Computing ...");
        //Map<String, Float> pageRank = new HashMap<String, Float>();
        
        System.out.println("Computing using " + this.getClass().getName());
        System.out.println("Write documents info ");
        
        //for(String name : documents.keySet()) {
        //    pageRank.put(name, documents.get(name).getPageRank());
        //}
        
        // Calculate PageRank Value
        calPageRank();
        
        String filePath = _options._indexPrefix + "/pageRank.dat";
        ObjectOutputStream writer = 
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
        writer.writeObject(documents);
        writer.close();
        writer = null;
        
        return;
    }

    /**
     * During indexing mode, this function loads the PageRank values computed
     * during mining mode to be used by the indexer.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object load() throws IOException {
        System.out.println("Loading using " + this.getClass().getName());
        String filePath = _options._indexPrefix + "/pageRank.dat";
        
        ObjectInputStream reader = 
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
        Map<String, Document> pageRank = null;
        
        try {
            pageRank = (Map<String, Document>)reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during reading page rank data");
        } finally {
            reader.close();
        }
        
        return pageRank;
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

    protected void buildCorpusGraph(Map<String, Document> corpus) throws IOException {
        System.out.println("Making Corpus Graph ...");
        int count = 1;
        for (String fName : corpus.keySet()) {
            Document currentDoc = corpus.get(fName);
            int sumLinks = 0;
            Map<Integer, Float> rows = new HashMap<Integer, Float>();
            
            for (String href : getAchors(fName)) {
                if (corpus.containsKey(href)) {
                    Document targetDoc = corpus.get(href);
                   
                    if (rows.containsKey(targetDoc._docid)) {
                        rows.put(targetDoc._docid, rows.get(targetDoc._docid) + 1.0f);
                    } else {
                        rows.put(targetDoc._docid, 1.0f);
                    }
                    sumLinks++;
                }
            }
            
            // Normalization of each files
            for(Integer targetDocid : rows.keySet())
                rows.put(targetDocid, rows.get(targetDocid) / sumLinks);
            
            corpusGraph.put(currentDoc._docid, rows);
            
            /*
            // Save into File / every 1000 files
            if (count % 1000 == 0)
                writeCorpusGraph(count);
            */            
            count++;            
        }
        
        /*
        // Save remain data into File
        if(!corpusGraph.isEmpty())
            writeCorpusGraph(count);
        */
    }
    
    /*
    protected void writeCorpusGraph(int index){
        try {
            String fileName = _options._indexPrefix + "/corpusGraph_" + String.format("%02d",  index/DIV) + ".dat";
            ObjectOutputStream writer 
                    = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName), 1024));
            writer.writeObject(corpusGraph);
            writer.close();
            writer = null;
            corpusGraph.clear();
            System.out.println("Save : " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during partial index writing");
        }
    }
    */
    
    /**
     * Transition of matrix
     * @param matrix
     * @return transitioned matrix
     */
    protected MapMatrix transMatrix(MapMatrix matrix){
        System.out.println("Translate matrix");
        
        MapMatrix result = new MapMatrix();
        File folder = new File(_options._corpusPrefix);
        
        int totalDocs = documents.size();
        for(int curDocid=0; curDocid<totalDocs; curDocid++){
            Map<Integer, Float> rows = new HashMap<Integer, Float>();
            
            for(Integer targetDocid : corpusGraph.keySet()){
                Map<Integer, Float> targetDoc = corpusGraph.get(targetDocid);
                
                if(targetDoc.containsKey(curDocid)){
                    rows.put(targetDocid, targetDoc.get(curDocid));
                }
            }
            
            if(!rows.isEmpty())     
                result.put(curDocid, rows);
        }      
        return result;
    }
    
    /**
     * Calculate Page Rank Value using Random Surfer Model
     * iteration Number : 1 or 2
     * lambda : 0.10 or 0.90
     */
    protected void calPageRank(){
        corpusGraph = transMatrix(corpusGraph);
        corpusGraph = matrixTimesScala(lambda, corpusGraph);
        
        int totalDocs = documents.size();
        float addConst = (1.0f-lambda) * (1.0f/(float)totalDocs);
       
        System.out.println("Calculating Page Rank ...");
        if(iterateNum == 1){      
            for(String docName : documents.keySet()){
                Document doc = documents.get(docName);
                int docid = doc._docid;
                float value = 0.0f;
                
                if(corpusGraph.containsKey(docid))
                    for(Integer targetDocid : corpusGraph.get(docid).keySet())
                        value += corpusGraph.get(docid).get(targetDocid);
                
                value += 1.0f - lambda;
                doc.setPageRank(value);                
            }
            
        }else if(iterateNum == 2){
            // G^2
            MapMatrix matrix = matrixMulti(corpusGraph, corpusGraph);  
            
            // a * G ' E
            HashMap<Integer, Float> aGE = new HashMap<Integer, Float>();
            for(Integer docid : corpusGraph.keySet()){
                float sum = 0.0f;
                for(Integer targetDocid : corpusGraph.get(docid).keySet())
                    sum += corpusGraph.get(docid).get(targetDocid);
                aGE.put(docid,  sum * addConst);
            }
            
            // a * E ' G
            float aEG = 0.0f; 
            for(Integer docid : corpusGraph.keySet())
                for(Integer targetDocid : corpusGraph.get(docid).keySet())
                    aEG += corpusGraph.get(docid).get(targetDocid);                    
                
            // (a^2) * (E^2)
            float a2E2 = addConst * addConst * totalDocs;
            
            for(String docName : documents.keySet()){
                Document doc = documents.get(docName);
                int docid = doc._docid;
                float value = 0.0f;
                
                if(matrix.containsKey(docid))               // G^2
                    for(Integer targetDocid : matrix.get(docid).keySet())
                        value += matrix.get(docid).get(targetDocid).floatValue();
                if(aGE.containsKey(docid))
                    value += aGE.get(docid).floatValue() * (float)totalDocs;    // + a * G ' E
                value += aEG;                                   // + a * E ' G
                value += a2E2 * (float)totalDocs;                  // + (a^2) * (E^2)
                doc.setPageRank(value);                
            }
        }
        
        /*
        // Test
        for(String docName : documents.keySet())
            System.out.println(docName + ": " + documents.get(docName).getPageRank());
            */
    }
    
    // Actually, below algorithm looks like having performance O(n^3) if worst case is given.
    // However, it does not happen in real world because each document contains not much number of links.
    // Thus, the time complexity is O(n*k^2). 
    protected MapMatrix matrixMulti(MapMatrix m1, MapMatrix m2) {
        MapMatrix result = new MapMatrix();
        
        for(Integer docId1 : m1.keySet()) {
            for(Integer docId2 : m2.keySet()){
                for(Integer key : m1.get(docId1).keySet()){
                    if(m2.get(docId2).containsKey(key)){
                        if(!result.containsKey(docId1))
                            result.put(docId1, new HashMap<Integer, Float>());
                        if(!result.get(docId1).containsKey(key))
                            result.get(docId1).put(key,  0.0f);
                        
                        float value = m1.get(docId1).get(key) * m2.get(docId2).get(key);
                        value += result.get(docId1).get(key);
                        
                        result.get(docId1).put(key, value);    
                    }
                }
            }
        }
            
        /*
        for(Integer docId : m1.keySet()) {
            for(Integer key1 : m1.get(docId).keySet()) {
                if( m2.containsKey(key1)) {
                    for(Integer key2 : m2.get(key1).keySet()) {
                        float val = m1.get(docId).get(key1) * m2.get(key1).get(key2);
                        if(result.containsKey(docId)) {
                            Map<Integer, Float> inner = result.get(docId);
                            
                            if(inner.containsKey(key2)) {
                                inner.put(key2, inner.get(key2).floatValue() + val);
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
        */
        
        return result;
    }    
    
    protected MapMatrix matrixTimesScala(float v, MapMatrix m1) {
        System.out.println("Matrix Times Scala");
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
