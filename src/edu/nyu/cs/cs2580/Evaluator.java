package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

class Evaluator {
    private static double precision[] = new double[3];   // Precision metrics
    private static double recall[] = new double[3];      // Recall metrics
    private static double fmeasure[] = new double[3];    // F-measures metrics
    private static double pr[] = new double[11];         // Precision at recal points metrics
    private static double avgPrecision;                  // Average Precision metrics
    private static double ndcg[] = new double[3];        // NDCG metrics
    private static double reciprocal;                    // Reciprocal rank metrics
    private static String query;
    private static String ranker_type = "";
    private static String resultStr = "";

    public static void main(String[] args) throws IOException {
	setInitial();
	HashMap < String , HashMap < Integer , Double > > relevance_judgments =
	    new HashMap < String , HashMap < Integer , Double > >();
	if (args.length < 2){
	    System.out.println("need to provide relevance_judgments, ranker type");
	    return;
	}
	String p = args[0];
	String ranker_type = args[1];
	// first read the relevance judgments into the HashMap
	readRelevanceJudgments(p,relevance_judgments);
	// now evaluate the results from stdin
	evaluateStdin(relevance_judgments);
	// print result on screen
	printResults();
	// print result on file
	printResultsFile();
    }

    // Initialize all metrics variables
    public static void setInitial(){
	avgPrecision = 0.0;
	reciprocal = 0.0;
	for(int i=0; i<3; i++){
	    precision[i] = 0.0;
	    recall[i] = 0.0;
	    fmeasure[i] = 0.0;
	    ndcg[i] = 0.0;
	}
	for(int i=0; i<11; i++)
	    pr[i] = 0.0;
    }

    public static void readRelevanceJudgments(
	String p, HashMap < String , HashMap < Integer , Double > > relevance_judgments){
	try {
	    BufferedReader reader = new BufferedReader(new FileReader(p));
 	    try {
		String line = null;
		while ((line = reader.readLine()) != null){
		    // parse the query,did,relevance line
		    Scanner s = new Scanner(line).useDelimiter("\t");
		    String query = s.next();
		    int did = Integer.parseInt(s.next());
		    String grade = s.next();
		    double rel = 0.0;

		    if(grade.equals("Perfect"))
			rel = 10;   
		    else if(grade.equals("Excellent"))
			rel = 7; 
		    else if(grade.equals("Good"))
			rel = 5;      
		    else if(grade.equals("Fair"))
			rel = 1;       
		    else if(grade.equals("Bad"))
			rel = 0;        

		    if (relevance_judgments.containsKey(query) == false){
			HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
			relevance_judgments.put(query,qr);
		    }
		    HashMap < Integer , Double > qr = relevance_judgments.get(query);
		    qr.put(did,rel);
		}
	    } finally {
		reader.close();
	    }
	} catch (IOException ioe){
	    System.err.println("Oops " + ioe.getMessage());
	}
    }

    // Evaluate with Standard metrics
    public static void evaluateStdin(
	HashMap < String , HashMap < Integer , Double > > relevance_judgments){
	HashMap<Double, Double> _precisions = new HashMap<Double, Double>();

	// only consider one query per call    
	try {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	    String line = null;
	    double RR = 0.0;
	    double N = 0.0;
	    double dcg = 0.0;
	    List<Double> idcgList = new ArrayList<Double>();
	   
	    while ((line = reader.readLine()) != null){
		Scanner s = new Scanner(line).useDelimiter("\t");
		query = s.next();
		int did = Integer.parseInt(s.next());
		String title = s.next();
		double rel = Double.parseDouble(s.next());
		if (relevance_judgments.containsKey(query) == false){
		    throw new IOException("query not found");
		}
		HashMap < Integer , Double > qr = relevance_judgments.get(query);
		++N;

		double rel_grade = 0.0;
		if(qr.containsKey(did) != false)
		    rel_grade = qr.get(did);
		
		if(rel_grade>=5){    // convert to binary relevance
		    RR += 1;
		    avgPrecision += RR/N;  // Average Precision
		    if(reciprocal == 0.0) 
			reciprocal = 1.0/N;  // Reciprocal Rank
		}

		if(N==1)
		    dcg += rel_grade;
		else
		    dcg += rel_grade / ( Math.log(N) / Math.log(2) );
		idcgList.add(rel_grade);

		_precisions.put(N, RR);  // Precision at recall points

		if(N==1 || N==5 || N==10){
		    precision[(int)N/5] = RR/N;  // Precision metrics
		    recall[(int)N/5] = RR;       // Recall metrics
		    ndcg[(int)N/5] = dcg;
		}
	    }	    
	    calEvaluations(RR, N, _precisions, idcgList);
	    
	} catch (Exception e){
	    System.err.println("Error:" + e.getMessage());
	}
    }    

    public static void calEvaluations(double RR, double N, 
        HashMap<Double, Double> _precisions, List<Double> idcgList){

   	double f_alpha = 0.50;
	if(RR != 0)
	    avgPrecision /= RR;                  // Average Precision
	for(int i=0; i<3; i++){
	    if(RR != 0)
		recall[i] /= RR;                  // Recall metrics
	    fmeasure[i] = 1.0/(f_alpha*(1.0/precision[i])+(1-f_alpha)*(1.0/recall[i]));   // F-measures metrics
	}	   
	// Precision at recall points
	Set<Double> keys = _precisions.keySet();
	for(Double key : keys){
	    double tmpRecall = _precisions.get(key) / RR;
	    double tmpPrecision = _precisions.get(key) / key;
	    for(int i=0; i<11; i++){
		if(tmpRecall >= 0.1*i && tmpPrecision > pr[i])
		    pr[i] = tmpPrecision;
	    }
	}
	
	// NDCG
	ArrayList<Double> idcgList2;
	for(int i=0; i<3; i++){
	    double idcg = 0;
	    if(i==0)  
		idcgList2 = subList(idcgList, 0, 0);
	    else 
		idcgList2 = subList(idcgList, 0, i*5-1);
	    reverseSort(idcgList2);

	    for(int j=0; j<idcgList2.size(); j++){
		if(j==0)  
		    idcg += idcgList2.get(j);
		else  
		    idcg += idcgList2.get(j) / (Math.log(j+1) / Math.log(2));	    
	    }
	    if(idcg!=0)
		ndcg[i] /= idcg;
	}
    }

    public static void reverseSort(ArrayList<Double> list){
	for(int m=list.size()-1; m>0; m--)
	    for(int n=0; n<m; n++)
		if(list.get(n) < list.get(m)){
		    Double tmp = list.get(n);
		    list.set(n, list.get(m));
		    list.set(m,tmp);
		}
    }

    // Return Sub Array of original Array with start / end point
    public static ArrayList<Double> subList(List<Double> list, int from, int to){
	ArrayList<Double> ret = new ArrayList<Double>();
	for(int i=from; i<=to; i++)
	    ret.add(list.get(i));
	return ret;
    }

    public static void printResults(){
	resultStr = query;
	for(int i=0; i<3; i++)
	    resultStr += "\t" + precision[i];
	for(int i=0; i<3; i++)
	    resultStr += "\t" + recall[i];
	for(int i=0; i<3; i++)
	    resultStr += "\t" + fmeasure[i];
	for(int i=0; i<11; i++)
	    resultStr += "\t" + pr[i];
	resultStr += "\t" + avgPrecision;
	for(int i=0; i<3; i++)
	    resultStr += "\t" + ndcg[i];
	resultStr += "\t" + reciprocal + "\n";
	System.out.println(resultStr);
    }

    public static void printResultsFile(){
	String fileName = "../results/hw1.3-vsm.tsv";
	if(ranker_type.equals("cosine"))
	    fileName = "../results/hw1.3-vsm.tsv";    
	else if(ranker_type.equals("QL"))
	    fileName = "../results/hw1.3-ql.tsv";     
	else if(ranker_type.equals("phrase"))
	    fileName = "../results/hw1.3-phrase.tsv";    
	else if(ranker_type.equals("numviews"))
	    fileName = "../results/hw1.3-numviews.tsv"; 
	else if(ranker_type.equals("linear"))
	    fileName = "../results/hw1.3-linear.tsv";    

	try{
	    BufferedWriter file = new BufferedWriter(new FileWriter(fileName, true));
	    file.write(resultStr, 0, resultStr.length());
	    file.close();
	} catch (IOException e) {
	    System.err.println(e); 
	    System.exit(1);
	}
    }
}
