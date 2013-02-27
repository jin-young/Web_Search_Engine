package edu.nyu.cs.cs2580;

import java.util.Vector;
import java.util.Scanner;
import java.util.Comparator;
import java.util.Collections;
import java.io.File;

class Ranker {
    private Index _index;
    private String ranker_type;

    public Ranker(String index_source){
	_index = new Index(index_source);
	createResultDir();
    }

    public void createResultDir(){
	 File d = new File("../results");
	 if (!d.isDirectory())
	     d.mkdir();
    }

    public void setRankerType(String ranker_type){
	this.ranker_type = ranker_type;
    }

    public Vector < ScoredDocument > runquery(String query){
	Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
	for (int i = 0; i < _index.numDocs(); ++i){
	    retrieval_results.add(runquery(query, i));
	}
	// Sort with decreasing order of relevance
	Collections.sort(retrieval_results, new SdComparator());
	return retrieval_results;
    }

    public ScoredDocument runquery(String query, int did){

	// Build query vector
	Vector < String > qv = new Vector < String > ();
	Scanner s = new Scanner(query);
	while (s.hasNext()){
	    String term = s.next();
	    qv.add(term);
	}
	Document d = _index.getDoc(did);

	double score = 0.0;
	if(ranker_type.equals("cosine"))
	    score = calCosineScore(qv, d);  
	else if(ranker_type.equals("QL"))
	    score = calQLScore(qv, d);     
	else if(ranker_type.equals("phrase"))
	    score = calPhraseScore(qv, d); 
	else if(ranker_type.equals("numviews"))
	    score = calNumviewsScore(d);    
	else if(ranker_type.equals("linear"))
	    score = calLinearScore(qv, d); 

	return new ScoredDocument(did, d.get_title_string(), score);
    }

    public double calCosineScore(Vector<String> qv, Document d){
	double sumWeight=0.0, sumWeight2=0.0;
	Vector<Double> _weights = new Vector<Double>();
	for(int i=0; i<qv.size(); i++){
	    double tf = d.termFrequencyInDoc(qv.get(i));
	    double n = _index.numDocs();
	    double dt = _index.documentFrequency(qv.get(i));
	    double idf = 1 + (Math.log(n/dt) / Math.log(2));
	    double weight = (double)tf * idf;
	    //weight = (weight<0) ? -1*weight : weight;
	    _weights.add(weight);
	}
	normalize(_weights);
	
	for(int i=0; i<_weights.size(); i++){
	    sumWeight += _weights.get(i);
	    sumWeight2 += _weights.get(i) * _weights.get(i); 
	}
	if(sumWeight == 0.0)  return 0.0;
	return sumWeight / Math.sqrt(sumWeight2 * (double)qv.size());	    
    }

    public void normalize(Vector<Double> _weights){
	double sum = 0.0;
	for(int i=0; i<_weights.size(); i++)
	    sum += _weights.get(i) * _weights.get(i);
	if(sum==0)  return;
	sum = Math.sqrt(sum);
	for(int i=0; i<_weights.size(); i++){
	    double newWeight = _weights.get(i)/sum;
	    _weights.set(i, newWeight);
	}
    }
    
    // Query Likelihood (language model)
    // Smoothing : Jelinek-Mercer smoothing
    // lambda : 0.50
    public double calQLScore(Vector<String> qv, Document d){
	double score=1.0, lambda=0.50;
	int cgi = 0;
	Vector<String> dv = d.get_title_vector();
	dv.addAll(d.get_body_vector());
	for(int i=0; i<qv.size(); i++)
	    score *= ((1-lambda)*((double)d.termFrequencyInDoc(qv.get(i))
				  / (double)dv.size())
		      + (lambda)*((double)Document.termFrequency(qv.get(i))
				  / (double)Document.termFrequency()));
	return score;
    }

    public double calPhraseScore(Vector<String> qv, Document d){
	double score = 0.0;
	Vector < String > dv = d.get_title_vector();
	dv.addAll(d.get_body_vector());

	if(qv.size()<=1 || dv.size()<=1)
	    return 0;
	}else{
	    for(int i=0; i<qv.size()-1; i++)
		for(int j=0; j<dv.size()-1; j++)
		    if(qv.get(i).equals(dv.get(j)) && qv.get(i+1).equals(dv.get(j+1)))
			score++;
	    return score;
	}	
    }

    public double calNumviewsScore(Document d){
	return d.get_numviews();
    }
 
    public double calLinearScore(Vector<String> qv, Document d){
	double b_cos=0.5, b_lm=0.49, b_phrase=0.0999, b_numviews=0.0001;
	return b_cos*calCosineScore(qv, d) + b_lm*calQLScore(qv, d)
	    + b_phrase*calPhraseScore(qv, d) + b_numviews*calNumviewsScore(d);
    }

    // For sorting of ScoredDocument vector
    static class SdComparator implements Comparator<ScoredDocument> {
 	@Override
	    public int compare(ScoredDocument arg0, ScoredDocument arg1) {
	    if(arg0._score < arg1._score)  return 1;
	    else if(arg0._score > arg1._score)  return -1;
	    else  return 0;
	}
    }
}




