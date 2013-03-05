package edu.nyu.cs.cs2580;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
    private static final long serialVersionUID = 9184892508124423115L;

    public DocumentIndexed(int docid) {
	super(docid);
    }

    // # of occurrences of string s in the doc
    public int termFrequencyInDoc(String s){
	// Implement...
	return 0;
    }

    // # of occurrences of s in the entire collection
    public static int termFrequency(String s){
	// Implement...
	return 0;
    }

    // # of words occurences in the collection
    // i.e. sum of termFrequency(s) over all words in the vocabulary
    public static int termFrequency(){
	// Implement...
	return 0;
    }
}
