package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * The basic implementation of a Document. Only the most basic information are
 * maintained in this class. Subclass should implement additional information
 * for display or ranking, such as snippet, term vectors, anchors, etc.
 * 
 * In HW1: instructors provide {@link DocumentFull}.
 * 
 * In HW2: students must implement the more efficient {@link DocumentIndexed}.
 * 
 * In HW3: students must incorporate the PageRank and NumViews based on corpus
 * and log analyses.
 * 
 * @author fdiaz
 * @author congyu
 */
class Document implements Serializable {
	private static final long serialVersionUID = -539495106357836976L;

	/**
	 * A simple checker to see if a given document is present in our corpus.
	 * This is provided for illustration only.
	 */
	public static class HeuristicDocumentChecker {
		private static MessageDigest MD = null;

		private Set<BigInteger> _docsInCorpus = null;

		public HeuristicDocumentChecker() throws NoSuchAlgorithmException {
			if (MD == null) {
				MD = MessageDigest.getInstance("MD5");
			}
			_docsInCorpus = new HashSet<BigInteger>();
		}

		public void addDoc(String name) {
			if (MD != null) {
				_docsInCorpus.add(new BigInteger(MD.digest(name.getBytes())));
			}
		}

		public int getNumDocs() {
			return _docsInCorpus.size();
		}

		public boolean checkDoc(String name) {
			if (MD == null) {
				return false;
			}
			return _docsInCorpus.contains(new BigInteger(MD.digest(name
					.getBytes())));
		}
	}

	@Override
	public boolean equals(Object object) {
		boolean sameSame = false;

		if (object != null && object instanceof Document) {
			sameSame = this._docid == ((Document) object)._docid;
		}

		return sameSame;
	}

	public int _docid;

	// Basic information for display
	private String _title = "";
	private String _url = "";
	private List<String> texts2Display = null;

	// Basic information for ranking
	private float _pageRank = 0.0f;
	private int _numViews = 0;

	public Document(int docid) {
		_docid = docid;
	}

	public String getTitle() {
		return _title;
	}
	
	public void setTitle(String title) {
		this._title = title;
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		this._url = url;
	}

	public float getPageRank() {
		return _pageRank;
	}

	public void setPageRank(float pageRank) {
		this._pageRank = pageRank;
	}

	public int getNumViews() {
		return _numViews;
	}

	public void setNumViews(int numViews) {
		this._numViews = numViews;
	}

	public void addDisplaySentence(String txt) {
		if (texts2Display == null) {
			texts2Display = new ArrayList<String>();
		}
		texts2Display.add(txt);
	}

	public String getTextToDisplay() {
		String ret = "";
		for (String txt : texts2Display) {
			ret += txt;
		}
		return ret;
	}

    private class Text implements Comparable<Text> {
    	public int position=0;
    	public String value=null;
    	
    	public Text(int position, String value) {
    		this.position = position;
    		this.value = value;
    	}

		@Override
		public int compareTo(Text o) {
			return position - o.position;
		}
    }
    
    public void clearText2Display() {
    	if(texts2Display != null)
    		texts2Display.clear();
    }
    
	public void loadTextToDisplay(Query query, IndexerCommon indexer) {
		
		clearText2Display();
		
    	File targetFile = new File(getUrl());
    	
    	String content = indexer.getFileContent(targetFile);
    	
    	String[] words = content.split("\\s+");
    	
    	TreeMap<Integer, String> positions = new TreeMap<Integer, String>();
    	
    	for (String token : query._tokens) {
    		if (token.contains(" ")) {
    			String[] terms = token.split("\\s+");
    			
    			int wordId = indexer.getDictionary().get(terms[0]);
    			List<Integer> termPositions = indexer.getTermPositions(wordId, _docid);
    			//remove doc id and frequency
    			termPositions = termPositions.subList(2, termPositions.size());
    			
    			for(int position : termPositions) {
    				int offset = 0;
    				if(position <= (words.length - terms.length)) {
        				boolean found = true;
        				for(String term: terms) {
        					String w = IndexerCommon.trimPunctuation(words[position + offset - 1]).toLowerCase();
        					if(!term.equals(w)) {
        						found = false;
        						break;
        					}
        					offset += 1;
        				}
        				if(found) {
        					positions.put(position, token);
        				}
    				} else {
    					break;
    				}
    			}
    		} else {
    			List<Integer> termPositions = indexer.getTermPositions(indexer.getDictionary().get(token), _docid);
    			//remove doc id and frequency
    			termPositions = termPositions.subList(2, termPositions.size());
    			
    			for(int position : termPositions) {
    				positions.put(position, token);
    			}
			}
		}
    	
    	List<Text> closestPosition = new ArrayList<Text>();
    	int minDistance = Integer.MAX_VALUE;
    	
    	for(Integer position : positions.keySet()) {
    		String currentTerm = positions.get(position);
    		int existIndex = -1;
    		for(int idx = 0; idx < closestPosition.size(); idx++) {
				if(closestPosition.get(idx).value.equals(currentTerm)) {
					existIndex = idx;
    				break;
    			}
    		}
    		
    		if(existIndex >= 0) {
    			if(closestPosition.size() == query._tokens.size()) {
    				int distance = 0;
    				int prevPosition = -1;
    				if(existIndex == 0) {
    					prevPosition = closestPosition.get(1).position;
    					
        				for(int idx = 2; idx < closestPosition.size(); idx++) {
        					distance += closestPosition.get(idx).position - prevPosition;
        					prevPosition = closestPosition.get(idx).position;
        				}
    				} else {
    					prevPosition = closestPosition.get(0).position;
    					
    					for(int idx = 1; idx < closestPosition.size(); idx++) {
    						if(idx != existIndex) {
    							distance += closestPosition.get(idx).position - prevPosition;
    							prevPosition = closestPosition.get(idx).position;
    						}
        				}
    				}
    				distance += position - prevPosition;
    				
    				if(distance < minDistance) {
    					minDistance = distance;
    					Text removed = closestPosition.remove(existIndex);
    					removed.position = position;
    					closestPosition.add(removed);
    				}
    			} else {
    				//not fulled yet. Just replace
					Text removed = closestPosition.remove(existIndex);
					removed.position = position;
					closestPosition.add(removed);
    			}
    		} else {
    			//new one must be added
    			closestPosition.add(new Text(position, currentTerm));
    			
    			if(query._tokens.size() == 1) break;
    		}
    	}
    	
    	for(int i = 0; i <closestPosition.size(); i++) {
    		Text t = closestPosition.get(i);
    		
    		int start_idx = t.position - 3; 
    		//don't forget. Array starts from 0.
    		start_idx = (start_idx < 1) ? 0 : start_idx - 1;
    		
    		int end_idx = t.position + 10;
    		end_idx = (end_idx > words.length) ? words.length-1 : end_idx - 1;
    		
    		String sentence = "";
    		for(int idx = start_idx; idx <= end_idx; idx++) {
    			if(idx < end_idx)
    				sentence += words[idx] + " ";
    			else
    				sentence += words[idx];
    		}
    		if(end_idx < words.length - 1) sentence += "... ";
    		
    		addDisplaySentence(sentence);
    	}
	}
}
