package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * 
 * N.B. This class is not thread-safe.
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

    /**
     * CGI arguments provided by the user through the URL. This will determine
     * which Ranker to use and what output format to adopt. For simplicity, all
     * arguments are publicly accessible.
     */
    public static class CgiArguments {
        // The raw user query
        public String _query = "";
        // How many results to return
        private int _numResults = 10;

        // The type of the ranker we will be using.
        public enum RankerType {
            NONE, FULLSCAN, CONJUNCTIVE, FAVORITE, COSINE, PHRASE, QL, LINEAR,
        }

        public RankerType _rankerType = RankerType.NONE;
        
        // top k document : Query Representations
        public int _numdocs = 10;
        
        // the most frequent m terms : Query Representations
        public int _numterms = 10;
        
        // The output format.
        public enum OutputFormat {
            TEXT, HTML,
        }

        public OutputFormat _outputFormat = OutputFormat.TEXT;

        public CgiArguments(String uriQuery) {
            System.out.println(uriQuery);
            
            
            String[] params = uriQuery.split("&");
            for (String param : params) {
                String[] keyval = param.split("=", 2);
                if (keyval.length < 2) {
                    continue;
                }
                String key = keyval[0].toLowerCase();
                String val = keyval[1].toLowerCase();
                if (key.equals("query")) {
                    _query = val;
                } else if (key.equals("num")) {
                    try {
                        _numResults = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        // Ignored, search engine should
                        // never fail upon invalid user
                        // input.
                    }
                } else if (key.equals("ranker")) {
                    try {
                        _rankerType = RankerType.valueOf(val.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Ignored, search engine should
                        // never fail upon invalid user
                        // input.
                    }
                } else if (key.equals("format")) {
                    try {
                        _outputFormat = OutputFormat.valueOf(val.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Ignored, search engine should
                        // never fail upon invalid user
                        // input.
                    }
                } else if (key.equals("numdocs")) {
                    try {
                        _numdocs = Integer.parseInt(val);
                    } catch (IllegalArgumentException e) {
                        // Ignored, search engine should
                        // never fail upon invalid user
                        // input.
                    }
                } else if (key.equals("numterms")) {
                    try {
                        _numterms = Integer.parseInt(val);
                    } catch (IllegalArgumentException e) {
                        // Ignored, search engine should
                        // never fail upon invalid user
                        // input.
                    }
                }
            } // End of iterating over params
        }
    }

    // For accessing the underlying documents to be used by the Ranker.
    // Since
    // we are not worried about thread-safety here, the Indexer class must
    // take
    // care of thread-safety.
    private Indexer _indexer;

    public QueryHandler(Options options, Indexer indexer) {
        _indexer = indexer;
    }

    private void respondWithMsg(HttpExchange exchange, final String message) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0); // arbitrary number of
        // bytes
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(message.getBytes());
        responseBody.close();
    }

    private void constructTextOutput(final Vector<ScoredDocument> docs, StringBuffer response) {
        for (ScoredDocument doc : docs) {
            response.append(response.length() > 0 ? "\n" : "");
            response.append(doc.asTextResult());
        }
        response.append(response.length() > 0 ? "\n" : "");
    }
        
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests
            // only.
            return;
        }

        // Print the user request header.
        Headers requestHeaders = exchange.getRequestHeaders();
        System.out.print("Incoming request: ");
        for (String key : requestHeaders.keySet()) {
            System.out.print(key + ":" + requestHeaders.get(key) + "; ");
        }
        System.out.println();

        // Validate the incoming request.
        String uriQuery = exchange.getRequestURI().getQuery();
        String uriPath = exchange.getRequestURI().getPath();
        if (uriPath == null || uriQuery == null) {
            respondWithMsg(exchange, "Something wrong with the URI!");
        }
        if (!uriPath.equals("/search") && !uriPath.equals("/prf")) {
            respondWithMsg(exchange, "Only /search or /prf are handled!");           
        }
        System.out.println("Query: " + uriQuery);

        // Process the CGI arguments.
        CgiArguments cgiArgs = new CgiArguments(uriQuery);
        if (cgiArgs._query.isEmpty()) {
            respondWithMsg(exchange, "No query is given!");
        }

        // Create the ranker.
        Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs, SearchEngine.OPTIONS, _indexer);
        if (ranker == null) {
            respondWithMsg(exchange, "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
        }

        // Processing the query.
        Query processedQuery;
        if (cgiArgs._query.contains("\""))  // Contain phrase 
            processedQuery = new QueryPhrase(cgiArgs._query);
        else                                        // Not contain phrase
            processedQuery = new Query(cgiArgs._query);
        processedQuery.processQuery();

        // Ranking.
        int numResultDocs = cgiArgs._numResults;
        if(uriPath.equals("/prf"))   numResultDocs = cgiArgs._numdocs;
        Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, numResultDocs);
        
        StringBuffer response = new StringBuffer();
        // Search Mode
        if(uriPath.equals("/search")){
            System.out.println("Search Processing ...");
            switch (cgiArgs._outputFormat) {
            case TEXT:
                constructTextOutput(scoredDocs, response);
                break;
            case HTML:
                // @CS2580: Plug in your HTML output
                break;
            default:
                // nothing
            }
        }
        // Pseudo-Relevance Feedback Mode
        else if(uriPath.equals("/prf")){
            System.out.println("PRF Processing ...");
            computeRepresent(scoredDocs, response, cgiArgs._numterms);
        }
        
        respondWithMsg(exchange, response.toString());
        System.out.println("Finished query: " + cgiArgs._query);
    }

    /**
     * Compute Query Representations
     * @param docs
     * @param response
     * @param _numterms
     */
    public void computeRepresent(final Vector<ScoredDocument> docs, StringBuffer response, int _numterms){
        HashMap<String, Double> termProb = new HashMap<String, Double>();
        Vector<String> topTerms = new Vector<String>();
        int totalTermNums = 0;
        
        // Get top ranked document with _numdocs
        for(ScoredDocument doc : docs){
            // Read File content
            File file = new File(doc.getUrl());            
            String content = ((IndexerCommon)_indexer).getFileContent(file);
            Scanner s = new Scanner(content);
        
            // Count the terms in the content
            while(s.hasNext()){
                String token = ((IndexerCommon)_indexer).porterAlg(s.next()).toLowerCase();
                if(termProb.containsKey(token))
                    termProb.put(token, termProb.get(token)+1.0);
                else
                    termProb.put(token,  1.0);
                totalTermNums++;                
            }
        }
        
        // Calculate conditional probability
        for(String token : termProb.keySet()){
            double prob = termProb.get(token) / totalTermNums;
            termProb.put(token, prob);      
            
            // insertion sorting
            if(topTerms.size() < _numterms || prob > termProb.get(topTerms.lastElement())){
                for(int i=0; i<topTerms.size(); i++){
                    if(prob > termProb.get(topTerms.get(i)))
                        topTerms.add(i, token);
                    else if(i == topTerms.size()-1)
                        topTerms.add(token);
                }
                if(topTerms.size() > _numterms)
                    topTerms.remove( topTerms.size()-1 );
            }                
        }
        
        // For normalization
        double sumOfTopTerms = 0.0;
        for(String token : topTerms)
            sumOfTopTerms += termProb.get(token);
        
        // Print Result
        for(String token : topTerms){
            double prob = termProb.get(token) / sumOfTopTerms;
            System.out.println(token + "\t" + prob);
        }            
    }

}
