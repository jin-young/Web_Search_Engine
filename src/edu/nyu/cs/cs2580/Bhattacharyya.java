package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Bhattacharyya {
    HashMap<String, String> queryUrl = new HashMap<String, String>();
    HashMap<String, HashMap<String, Double>> queryProb = new HashMap<String, HashMap<String, Double>>();
    
    /**
     * Read prf.tsv file and make Map<Query : prf-url>
     * @param args
     */
    public void readQueryUrl(String fileUrl){
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileUrl));
            String line;
            while ((line = in.readLine()) != null) {
                String[] splits = line.split(":");
                queryUrl.put(splits[0], splits[1]);
            }
            in.close();
          } catch (IOException e) {
              System.err.println(e); 
          }
    }
    
    /**
     * Read each query - related prf file
     */
    public void readQueryProb(){
        for(String query : queryUrl.keySet()){
            try{
                BufferedReader in = new BufferedReader(new FileReader( queryUrl.get(query) ));
                HashMap<String, Double> termProb = new HashMap<String, Double>();
                String line;
                while((line = in.readLine()) != null){
                    String[] splits = line.split("\t");
                    termProb.put(splits[0], Double.parseDouble(splits[1]));
                }
                queryProb.put(query, termProb);
            }catch(IOException e){
                System.err.println(e);
            }
        }
    }
    
    /**
     * Compute Query Similarity with all queries which a user inputs
     */
    public void computeQuerySimilarity(StringBuffer result) {
        ArrayList<String> queryList = new ArrayList<String>();
        queryList.addAll(queryUrl.keySet());
        HashMap<String, Double> query1, query2;
        
        for(int i=0; i<queryList.size(); i++){
            for(int j=i+1; j<queryList.size(); j++){
                double coefficient = 0.0;
                query1 = queryProb.get(queryList.get(i));
                query2 = queryProb.get(queryList.get(j));
                for(String term : query1.keySet()){
                    if(query2.containsKey(term))
                        coefficient += Math.sqrt(query1.get(term).doubleValue() * query2.get(term).doubleValue());
                }
                
                //  Test : it show saved in file
                String outLine = queryList.get(i) + "\t" + queryList.get(j) + "\t" + coefficient + "\n";
                System.out.println(outLine);
                
                result.append(outLine); 
            }
        }
    }
    
    public void writeToFile(String fileName, String result){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(result);                
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }  
    }
    
    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("Format : java -cp src edu.nyu.cs.cs2580.Bhattacharyya " +
            		"<PATH-TO-PRF-OUTPUT> <PATH-TO-OUTPUT>");
            System.exit(1);
        }
        
        // Read prf.tsv file
        Bhattacharyya bhattacharyya = new Bhattacharyya();
        bhattacharyya.readQueryUrl(args[0]);
        
        // Compute Query Similarity
        StringBuffer result = new StringBuffer();
        bhattacharyya.computeQuerySimilarity(result);
        bhattacharyya.writeToFile(args[1], result.toString());
    }
}
