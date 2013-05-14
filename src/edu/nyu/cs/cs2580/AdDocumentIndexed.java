package edu.nyu.cs.cs2580;

public class AdDocumentIndexed extends DocumentIndexed {

	private static final long serialVersionUID = 1896454434577643975L;
	private String keywords = "";
	
	public AdDocumentIndexed(int docid) {
		super(docid);
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	public String getKeywords() {
		return keywords;
	}
}
