package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.io.*;
import java.util.Date;
import java.lang.System;  
import java.text.SimpleDateFormat; 

class QueryHandler implements HttpHandler {
    private static Map<Integer, String> sqMap = new HashMap<Integer, String>();   // SessionID - Query
    private static int curSID = 0;

    private static String plainResponse =
	"Request received, but I am not smart enough to echo yet!\n";

    private Ranker _ranker;

    public QueryHandler(Ranker ranker){
	_ranker = ranker;
    }

    public static Map<String, String> getQueryMap(String query){  
	String[] params = query.split("&");  
	Map<String, String> map = new HashMap<String, String>();  
	for (String param : params){  
	    String name = param.split("=")[0];  
	    String value = param.split("=")[1];  
	    map.put(name, value);  
	}
	return map;  
    } 
  
    public void handle(HttpExchange exchange) throws IOException {
	String requestMethod = exchange.getRequestMethod();
	if (!requestMethod.equalsIgnoreCase("GET")){  // GET requests only.
	    return;
	}

	// Print the user request header.
	Headers requestHeaders = exchange.getRequestHeaders();
	System.out.print("Incoming request: ");
	for (String key : requestHeaders.keySet()){
	    System.out.print(key + ":" + requestHeaders.get(key) + "; ");
	}
	System.out.println();
	String queryResponse = "";  
	String uriQuery = exchange.getRequestURI().getQuery();
	String uriPath = exchange.getRequestURI().getPath();
	String outFileName = "../results/simple.tsv";

	Vector < ScoredDocument > sds = new Vector<ScoredDocument>();

	if ((uriPath != null) && (uriQuery != null)){
	    if (uriPath.equals("/search")){
		Map<String,String> query_map = getQueryMap(uriQuery);
		Set<String> keys = query_map.keySet();

		if (keys.contains("query")){
		    String query = query_map.get("query").replace("+", " ");
		    query_map.put("query", query);
		    sqMap.put(curSID++, query);
		    
		    if (keys.contains("ranker")){
			String ranker_type = query_map.get("ranker");
			// @CS2580: Invoke different ranking functions inside your
			// implementation of the Ranker class.
			_ranker.setRankerType( ranker_type );
			sds = _ranker.runquery(query_map.get("query"));
			
			if(ranker_type.equals("cosine"))
			    outFileName = "../results/hw1.1-vsm";    
			else if(ranker_type.equals("QL"))
			    outFileName = "../results/hw1.1-ql";      
			else if(ranker_type.equals("phrase"))
			    outFileName = "../results/hw1.1-phrase";    
			else if(ranker_type.equals("numviews"))
			    outFileName = "../results/hw1.1-numviews";  
			else if(ranker_type.equals("linear"))
			    outFileName = "../results/hw1.2-linear";   
		    } else {
			// @CS2580: The following is instructor's simple ranker that does not
			// use the Ranker class.
			sds = _ranker.runquery(query_map.get("query"));
		    }
		    queryResponse = getQueryResponse(sds, query_map.get("format"), query_map);	
		    printResult(outFileName, queryResponse, query_map.get("format"));
		    printRenderLog(sds);
		}
	    }else if(uriPath.equals("/clickEvent")){
		Map<String,String> query_map = getQueryMap(uriQuery);
		Set<String> keys = query_map.keySet();
		if(keys.contains("sid") && keys.contains("did")){
		    int sid = Integer.parseInt(query_map.get("sid"));
		    int did = Integer.parseInt(query_map.get("did"));
		    if(sqMap.containsKey(sid)){
			String query = sqMap.get(sid);
			long nowmills = System.currentTimeMillis();
   			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time = sdf.format(new Date(nowmills));
			String content = sid + "\t" + query + "\t" + did + "\t"
			    + "click\t" + time + "\n";
			printLog(content);
		    }
		}
	    }
	}
    
	// Construct a simple response.
	Headers responseHeaders = exchange.getResponseHeaders();
	responseHeaders.set("Content-Type", "text/plain");
	exchange.sendResponseHeaders(200, 0);  // arbitrary number of bytes
	OutputStream responseBody = exchange.getResponseBody();
	responseBody.write(queryResponse.getBytes());
	responseBody.close();
    }

    public void printRenderLog(Vector<ScoredDocument> sds){
	int limit = (sds.size()<10) ? sds.size() : 10;
	String content = "";
	int sid = curSID -1;
	String query = sqMap.get(sid);
	long nowmills = System.currentTimeMillis();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String time = sdf.format(new Date(nowmills));
	for(int i=0; i<limit; i++){
	    content += sid + "\t" + query + "\t" + sds.get(i)._did
		+ "\t" + "render" + "\t" + time + "\n";
	}
	printLog(content);
    }

    public void printLog(String content){
	try{
	    BufferedWriter file = new BufferedWriter(new FileWriter("../results/hw1.4-log.tsv", true));
	    file.write(content, 0, content.length());
	    file.close();
	} catch (IOException e) {
	    System.err.println(e); 
	    System.exit(1);
	}
    }
    
    public String getQueryResponse(Vector<ScoredDocument> sds, String format, Map<String,String> query_map){
	String queryResponse = "";
	ScoredDocument sd;
	if(format.equals("html")){
	    queryResponse = "<html><body><table>";
	}
	Iterator < ScoredDocument > itr = sds.iterator();
	while (itr.hasNext()){
	    sd = itr.next();
	    if(format.equals("html")){
		queryResponse += "<tr><td>" + query_map.get("query") + "</td>";
		queryResponse += "<td>" + sd.asString() + "</td></tr>";
	    }else if(format.equals("text")){
		queryResponse += query_map.get("query") + "\t" + sd.asString();
		queryResponse += "\n";
	    }
	}
	if(format.equals("html"))
	    queryResponse += "</table></body></html>";
	return queryResponse;
    }

    public void printResult(String outFileName, String content, String format){
	String fileName = format.equals("text") ? outFileName+".tsv" : outFileName+".html";
	try {
	    BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
	    out.write(content);
	    out.close();
	} catch (IOException e) {
	    System.err.println(e);
	}
    }
}
