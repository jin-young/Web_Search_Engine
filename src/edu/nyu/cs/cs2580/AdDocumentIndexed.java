package edu.nyu.cs.cs2580;

public class AdDocumentIndexed extends DocumentIndexed {

	private static final long serialVersionUID = 1896454434577643975L;
	private String keywords = "";
	private double cost = 0.0;
	
	public AdDocumentIndexed(int docid) {
		super(docid);
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	public void setCost(double cost){
	    this.cost = cost;
	}
	
	public String getKeywords() {
		return keywords;
	}
	
	public double getCost(){
	    return cost;
	}
}
