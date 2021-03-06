package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.google.gson.Gson;
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
            TEXT, HTML, JSON
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
                //String val = keyval[1].toLowerCase();
                //value should not be lowered because it could make unexpected result
                //for example, CIMS, run stemmer after lowering cims becomes cim.
                //however, running stemmer then lowering makes cims.
                String val = keyval[1];
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

    private void respondWithHtmlMsg(HttpExchange exchange, final String message) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, 0); // arbitrary number of
        // bytes
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(message.getBytes());
        responseBody.close();
    }
    
    private void respondWithJsonMsg(HttpExchange exchange, final String message) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "application/json");
        responseHeaders.set("Content-Length", Integer.toString(message.getBytes().length));
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
        //Vector<ScoredDocument> scoredDocs = null;
        ScoredDocs scoredDocs = new ScoredDocs();
        ScoredDocs scoredAdDocs = new ScoredDocs();
        StringBuffer response = new StringBuffer();
        
        if(uriPath.equals("/search")){      // Search Mode
            System.out.println("Search Processing ...");
            //scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);
            long start = System.currentTimeMillis();
            ranker.runQuery(processedQuery, cgiArgs._numResults, scoredDocs);
            long end = System.currentTimeMillis();
            NumberFormat formatter = new DecimalFormat("#0.00000");
            scoredDocs.set_run_time(formatter.format((end - start) / 1000d));
            
            ranker.runQueryForAd(processedQuery, 20, scoredAdDocs);
            
            switch (cgiArgs._outputFormat) {
            case TEXT:
                constructTextOutput(scoredDocs.getScoredDocs(), response);
                respondWithMsg(exchange, response.toString());
                break;
            case HTML:
            	response.append("<html><head></head>");
            	response.append("<body>");
            	if(scoredDocs.size() > 0) {
            		response.append("<ul>");
            		for(ScoredDocument doc : scoredDocs.getScoredDocs()) {
            			response.append("<li>" + doc.asHtmlResult() + "</li>");
            		}
            		response.append("</ul>");
            	} else {
            		response.append("<h1>Sorry, we have nothing for your query. I owe you...</h1>");
            	}
                response.append("</body>");
                response.append("</html>");
                respondWithHtmlMsg(exchange, response.toString());                
                
                break;
            case JSON:
            	Gson gson = new Gson();
            	Map<String, ScoredDocs> res = new HashMap<String, ScoredDocs>();
            	res.put("scoredDocs", scoredDocs);
            	res.put("scoredAdDocs", scoredAdDocs);
            	response.append(gson.toJson(res));
                
                respondWithJsonMsg(exchange, response.toString());
                break;
            default:
                // nothing
            }
        }else if(uriPath.equals("/prf")){       // Pseudo-Relevance Feedback Mode
            System.out.println("PRF Processing ...");
            Vector<ScoredDocument> sds = ranker.runQuery(processedQuery, cgiArgs._numdocs);
            ranker.computeQueryRep(sds, response, cgiArgs._numterms);
        }
        
        //respondWithMsg(exchange, response.toString());
        System.out.println("Finished query: " + cgiArgs._query);
    }
}
