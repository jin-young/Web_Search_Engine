package edu.nyu.cs.cs2580;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * This is the abstract Ranker class for all concrete Ranker implementations.
 *
 * Use {@link Ranker.Factory} to create your concrete Ranker implementation. Do
 * NOT change the interface in this class!
 *
 * In HW1: {@link RankerFullScan} is the instructor's simple ranker and students
 * implement four additional concrete Rankers.
 *
 * In HW2: students will pick a favorite concrete Ranker other than
 * {@link RankerPhrase}, and re-implement it using the more efficient
 * concrete Indexers.
 *
 * 2013-02-16: The instructor's code went through substantial refactoring
 * between HW1 and HW2, students are expected to refactor code accordingly.
 * Refactoring is a common necessity in real world and part of the learning
 * experience.
 *
 * @author congyu
 * @author fdiaz
 */
public abstract class Ranker {
    // Options to configure each concrete Ranker.
    protected Options _options;
    // CGI arguments user provide through the URL.
    protected CgiArguments _arguments;

    // The Indexer via which documents are retrieved, see {@code IndexerFullScan}
    // for a concrete implementation. N.B. Be careful about thread safety here.
    protected Indexer _indexer;

    /**
     * Constructor: the construction of the Ranker requires an Indexer.
     */
    protected Ranker(Options options, CgiArguments arguments, Indexer indexer) {
	_options = options;
	_arguments = arguments;
	_indexer = indexer;
    }

    /**
     * Processes one query.
     * @param query the parsed user query
     * @param numResults number of results to return
     * @return Up to {@code numResults} scored documents in ranked order
     */
    public abstract Vector<ScoredDocument> runQuery(Query query, int numResults);
    
    /**
     * Compute Query Representations for PRF mode
     * @param docs
     * @param response
     * @param _numterms
     */
    public void computeQueryRep(Vector<ScoredDocument> docs, StringBuffer response, int _numterms){
        System.out.println("Ranker: compute Query Representation ...");
        
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
            double prob = termProb.get(token) / (double)totalTermNums;
            termProb.put(token, prob);      
            
            // insertion sorting
            if(topTerms.size() < _numterms || prob > termProb.get(topTerms.lastElement())){
                if(topTerms.isEmpty()){ topTerms.add(token);    continue; }
                
                for(int i=0; i<topTerms.size(); i++){
                    if(prob > termProb.get(topTerms.get(i)).floatValue()){
                        topTerms.add(i, token);
                        break;
                    }else if(i == topTerms.size()-1)
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
    
    /**
     * All Rankers must be created through this factory class based on the
     * provided {@code arguments}.
     */
    public static class Factory {
		public static Ranker getRankerByArguments(CgiArguments arguments,
												  Options options, Indexer indexer) {
			switch (arguments._rankerType) {
			case FULLSCAN:
				return new RankerFullScan(options, arguments, indexer);
			case CONJUNCTIVE:
				return new RankerConjunctive(options, arguments, indexer);
			case FAVORITE:
				return new RankerFavorite(options, arguments, indexer);
			case COSINE:
				return new RankerCosine(options, arguments, indexer);
			case QL:
				return new RankerQL(options, arguments, indexer);
			case PHRASE:
				return new RankerPhrase(options, arguments, indexer);
			case LINEAR:
				return new RankerLinear(options, arguments, indexer);
			case NONE:
				// Fall through intended
			default:
				// Do nothing.
			}
			return null;
		}
	}  
}
