package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
	
	private static final long serialVersionUID = 6104700553407520025L;
	
	private Map<Integer, ArrayList<Short>> _index;
    private Map<Integer, ArrayList<Integer>> _skipPointer;
	
	public IndexerInvertedCompressed(Options options) {
		super(options);
		_index = new TreeMap<Integer, ArrayList<Short>>();
		_skipPointer = new HashMap<Integer, ArrayList<Integer>>();
		
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}
	
	@Override
	public void constructIndex() throws IOException {
		// Get All Files list in the Corpus Folder (data/wiki)
		File folder = new File(_options._corpusPrefix);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				processDocument(file);
			}
		}
		
		String indexFile = _options._indexPrefix + "/corpus.idx";
		System.out.println("Store index to: " + indexFile);
		ObjectOutputStream writer = new ObjectOutputStream(
				new FileOutputStream(indexFile));
		writer.writeObject(this);
		writer.close();
		
		System.out.println("Indexed " + Integer.toString(_numDocs)
				+ " docs with " + Long.toString(_totalTermFrequency)
				+ " terms.");		
	}
	
	/**
	 * Document processing must satisfy the following: 1) Non-visible page
	 * content is removed, e.g., those inside <script> tags 2) Tokens are
	 * stemmed with Step 1 of the Porter's algorithm 3) No stop word is removed,
	 * you need to dynamically determine whether to drop the processing of a
	 * certain inverted list.
	 */
	private void processDocument(File file) {
		int did = _documents.size();
		try {
			System.out.println(did +". " + file.getName());
			org.jsoup.nodes.Element body = Jsoup.parse(file, "UTF-8", _options._corpusPrefix + "/" + file.getName()).body();
			// Remove all script and style elements and those of class "hidden".
			body.select("script, style, .hidden").remove();

			// Remove all style and event-handler attributes from all elements.
			Elements all = body.select("*");
			for (Element el : all) { 
			  for (Attribute attr : el.attributes()) { 
			    String attrKey = attr.getKey();
			    if (attrKey.equals("style") || attrKey.startsWith("on")) { 
			      el.removeAttr(attrKey);
			    } 
			  }
			}
			String content = body.text();
			if(content.trim().length() > 0) {
				makeIndex(content, did);
			}
//			String content = retrieveContent(file);
//			content = removeNonVisible(content);
	
			DocumentIndexed doc = new DocumentIndexed(did);
			doc.setTitle(file.getName());
			doc.setUrl(_options._corpusPrefix + "/" + file.getName());
			_documents.add(doc);
			++_numDocs;
		} catch (IOException e) {
			System.err.println("Error Occurred while process document '" + file.getName() + "'");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// Use Delta Encoding with v-bytes
	private void makeIndex(String content, int docId) {
		
		Scanner s = new Scanner(content); // Uses white space by default.
		
		// <id of word, its' position in given doc>
		Map<Integer, Vector<Integer>> currentDocIndex = new HashMap<Integer, Vector<Integer>>();
		
		// positions of given word id
		Vector<Integer> positionsOfWordInCurrentDoc = null;
		
		int position = 1;
		
		// Build index for current document
		while (s.hasNext()) {
			_stemmer.setCurrent(s.next());
			_stemmer.stem();
			String token = _stemmer.getCurrent();

			int postingId;
			if(_dictionary.get(token) != null) {
				postingId = _dictionary.get(token);
			} else {
				postingId = _dictionary.size() + 1;
				_dictionary.put(token, _dictionary.size() + 1);
				_index.put(postingId, new ArrayList<Short>());
			}
			
			if(currentDocIndex.containsKey(postingId)) {
				positionsOfWordInCurrentDoc = currentDocIndex.get(postingId);
			} else {
				positionsOfWordInCurrentDoc = new Vector<Integer>();
				currentDocIndex.put(postingId, positionsOfWordInCurrentDoc);
			}
			
			positionsOfWordInCurrentDoc.add(position);
			
			position++;
			++_totalTermFrequency;
		}
		s.close();
		
		for(int wordId : currentDocIndex.keySet()) {
			positionsOfWordInCurrentDoc =  currentDocIndex.get(wordId);
			
			//Apply delta encoding
			int prevPosition = 0;
			for(int i=0; i<positionsOfWordInCurrentDoc.size(); i++) {
				int temp = positionsOfWordInCurrentDoc.get(i);
				positionsOfWordInCurrentDoc.set(i, temp - prevPosition);
				prevPosition = temp;
			}
			
			//Add current doc's index to posting list from here
			
			ArrayList<Short> postingList = _index.get(wordId);
			
			// Assume all documents are processed sequentially. It means that
			// all delta value of document id are 1 (0x81)
			postingList.add((short)0x81); 
			
			// add encoded # of occurrence after doc id
			for(short v : encodeVbyte(positionsOfWordInCurrentDoc.size())) {
				postingList.add(v); 
			}
			
			// add actual positions after # of occurrence
			int shortOffset = 1;
			for(int val : positionsOfWordInCurrentDoc) {
				for(short v : encodeVbyte(val)) {
					shortOffset++;
					postingList.add(v); 
				}
			}
			
			if(!_skipPointer.containsKey(wordId)) {
				_skipPointer.put(wordId, new ArrayList<Integer>());
			}
			
			_skipPointer.get(wordId).add(docId);
			_skipPointer.get(wordId).add(shortOffset);
		}
	}
	
	private short[] encodeVbyte(int value) {
		short[] alignedCode;
		
		if(value < Math.pow(2,7)) {
			alignedCode = new short[1];
			alignedCode[0] = (short)((value & 0x0000007F) | 0x00000080);
		} else if( Math.pow(2,7) <= value && value < Math.pow(2,14) ) {
			alignedCode = new short[2];
			alignedCode[1] = (short)((value & 0x0000007F) | 0x00000080);
			alignedCode[0] = (short)((value>>7) & 0x0000007F);
		} else if( Math.pow(2,14) <= value && value < Math.pow(2,21) ) {
			alignedCode = new short[3];
			alignedCode[2] = (short)((value & 0x0000007F) | 0x00000080);
			alignedCode[1] = (short)((value>>7) & 0x0000007F);
			alignedCode[0] = (short)((value>>14) & 0x0000007F);
		} else if( Math.pow(2,21) <= value && value < Math.pow(2,28) ) {
			alignedCode = new short[4];
			alignedCode[3] = (short)((value & 0x0000007F) | 0x00000080);
			alignedCode[2] = (short)((value>>7) & 0x0000007F);
			alignedCode[1] = (short)((value>>14) & 0x0000007F);
			alignedCode[0] = (short)((value>>21) & 0x0000007F);
		} else {
			throw new RuntimeException("Value : " + value + " cannot be handled by shortAlignedCode");
		}
		
		return alignedCode;
	}

	@Override
	public Document getDoc(int docid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		// TODO Auto-generated method stub
		return 0;
	}

}
