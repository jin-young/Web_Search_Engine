package edu.nyu.cs.cs2580;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedCompressedTest {
	private ArrayList<Short> shortList;
	private Options _options;

	private IndexerInvertedCompressed indexer;

	/*
	 * Brief 1=[(1, 1, [1])], 
	 * 2=[(1, 1, [2])], 
	 * Search 3=[(1, 2, [3, 11]), (1, 1, [19]), (8, 1, [20]), 
	 * engines 4=[129, 129, 132, 129, 129, 148], 
	 * 5=[129, 129, 133, 137, 129, 138], 
	 * 6=[129, 129, 134, 137, 129, 141], 
	 * 7=[129, 129, 135, 137, 129, 133], 
	 * 8=[129, 129, 136], 
	 * 9=[129, 129, 137], 
	 * 10=[(1, 1, [10]), (1, 2, [9, 7])], 
	 * 11=[129, 129, 139], 
	 * 12=[129, 129, 140], 
	 * 13=[129, 129, 141], 
	 * 14=[130, 129, 129, 136, 129, 144], 
	 * 15=[130, 129, 130], 
     * 16=[130, 129, 131], 
	 * 17=[130, 129, 132, 136, 129, 129], 
	 * 19=[130, 129, 134], 
	 * 18=[130, 129, 133, 136, 129, 130],
     * 20=[130, 130, 135, 134],  
	 * 21=[130, 129, 136], 
	 * 22=[130, 129, 138], 
     * 23=[130, 129, 139], 
     * 24=[130, 129, 140],  
	 * 25=[130, 129, 142], 
     * 26=[130, 129, 143],
	 * 27=[130, 129, 145], 
     * 28=[130, 129, 146, 136, 129, 147], 
	 * 29=[138, 129, 131], 
     * 30=[138, 129, 132], 
	 * 31=[138, 129, 134], 
     * 32=[138, 129, 135],  
	 * topics 33=[10, 1, 8],
     * 34=[138, 129, 137], 
     * 35=[138, 129, 139], 
	 * 36=[138, 130, 140, 134], 
	 * 37=[138, 129, 142]}
     * 38=[138, 129, 143], 
     * 39=[138, 129, 145]
	 */
	
	String doc1 = "Brief Description: Search engines have become a " + 
            "core part of our daily lives. search ";
	
    String doc2 = "In this course, we will " +
            "study the foundations of information retrieval and the " +
            "technical aspects of modern Web search engines. ";
    
    String doc10 = "We will " +
            "also explore a few advanced topics that have emerged " + 
            "to become highly influential in relation to Web search. ";
    
	@Before
	public void setUp() {
	    _options = createMock(SearchEngine.Options.class);
		_options._indexerType = "inverted-compressed";
		_options._corpusAnalyzerType = "";
		_options._logMinerType = "";
		_options._indexPrefix = "data/test_index";
		
		indexer = (IndexerInvertedCompressed) Indexer.Factory
				.getIndexerByOption(_options);
		indexer.underTest = true;
		
		shortList = new ArrayList<Short>(Arrays.asList(new Short[] {
		    // (2 1 2)
	        0x82, 0x81, 0x82,
	        // (4 4 132 3 20000 11)
	        0x84, 0x84, 0x01, 0x84, 0x83, 0x01, 0x1C, 0xA0, 0x8B,
	        // (5 130 1 2 3 4 5 6)
	        0x85, 0x01, 0x82, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86
		}));
	}

	@Test
	public void testWordsPositionsInDoc() {
	    /*
	    String doc = "Brief Description: Search engines have become a " + 
	                "core part of our daily lives. In this course, we will " +
	                "study the foundations of information retrieval and the " +
	                "technical aspects of modern Web search engines. We will " +
	                "also explore a few advanced topics that have emerged " + 
	                "to become highly influential in relation to Web search. ";*/
	    
	    Map<Integer, ArrayList<Integer>> result = indexer.wordsPositionsInDoc(doc1 + doc2 + doc10);
	    
	    assertThat("Result shouhd have 39 kyes", result.keySet().size(), is(39));
	    assertThat("Brief shouhd appear only one time", result.get(1).size(), is(1));
	    
	    assertThat("Search shouhd appear four times", result.get(3).size(), is(4));
	    assertThat("Search's position should be [3, 11, 19, 21]", 
	                    result.get(3).toArray(new Integer[1]), is(new Integer[]{3, 11, 19, 21} ));
	    
	    assertThat("Engine shouhd appear two times", result.get(4).size(), is(2));
        assertThat("Engine's position should be [4, 29]", 
                        result.get(4).toArray(new Integer[1]), is(new Integer[]{4, 30} ));	    
        
        assertThat("Web shouhd appear two times", result.get(28).size(), is(2));
        assertThat("Web's position should be [31, 21]", 
                        result.get(28).toArray(new Integer[1]), is(new Integer[]{32, 21} ));
        
        assertThat("relation shouhd appear one time", result.get(39).size(), is(1));
        assertThat("relation's position should be [50]", 
                        result.get(39).toArray(new Integer[1]), is(new Integer[]{51} ));
	}
	
	@Test
	public void testTrimPunctuation() {
	    String[][] tests = {
            {"don't", "don't"}, {"do'", "do"}, {"Description:", "Description"},
            {"engines.", "engines"}, {"(google", "google"}, {"te!(ed","te!(ed"} 
	    };
	    
	    for(String[] test : tests) {
	        assertThat(test[0] + " should be " + test[1], 
	                    indexer.trimPunctuation(test[0]), is(test[1]));
	    }
	}
	
	@Test
	public void testAddPositionsToIndex() {
	    ArrayList<Integer> positions = new ArrayList<Integer>(
	            Arrays.asList(new Integer[]{3, 29, 21})
        );
	    
	    indexer.setIndex(new CompressedIndex());
	    
	    int wordId = 11;
	    int docId = 20000;
	    int offset = indexer.addPositionsToIndex(positions, docId, wordId);
	    
	    assertThat("Seven bytes should be added", offset, is(7));
	    
	    ArrayList<Short> expect = new ArrayList<Short>(
	            Arrays.asList(new Short[]{0x01, 0x1C, 0xA0, 0x83, 0x83, 0x9D, 0x95})
        );
	    assertThat(indexer.getIndex().get(wordId), is(expect));
	}
	
	@Test
	public void testLastDocId() {
	    assertThat("If there is no word, last doc id should be 0", indexer.lastDocId(11), is(0));
	    
	    SkipPointer mockList = new SkipPointer();
	    
	    ArrayList<Integer> word11 = new ArrayList<Integer>();
	    //doc id, offset
	    word11.add(3);word11.add(7);
	    //doc id, offset
	    word11.add(5);word11.add(11);
	    mockList.put(11, word11);
	    
	    ArrayList<Integer> word5 = new ArrayList<Integer>();
       //doc id, offset
	    word5.add(1);word5.add(7);
        mockList.put(5, word5);
        
        ArrayList<Integer> word33 = new ArrayList<Integer>();
        //doc id, offset
        word33.add(1);word33.add(7);
        word33.add(2);word33.add(8);
        word33.add(4);word33.add(3);
        word33.add(6);word33.add(10);
         mockList.put(33, word33);
        
	    indexer.setSkipPointer(mockList);
	    
	    assertThat("Last doc id of word 11 should be 5", indexer.lastDocId(11), is(5));
	    
	    assertThat("If there is no word, last doc id should be 0", indexer.lastDocId(12), is(0));
	    
	    assertThat("Last doc id of word 5 should be 1", indexer.lastDocId(5), is(1));
	    assertThat("Last doc id of word 33 should be 6", indexer.lastDocId(33), is(6));
	}
	
	@Test
	public void testInitIndex() {
	    int wordId = 11;
	    assertThat(indexer.getIndex().get(wordId), nullValue());
	    
	    //if there is no list, then create new one
	    indexer.initIndex(11);
	    assertThat(indexer.getIndex().get(wordId).size(), is(0));
	    
	    //if exist, nothing
	    int wordId2 = 5;
	    indexer.getIndex().put(wordId2, shortList);
	    assertThat(indexer.getIndex().size(), is(2));
	    
	    indexer.initIndex(wordId2);
	    assertThat(indexer.getIndex().size(), is(2));
	    assertThat(indexer.getIndex().get(wordId2), is(shortList));
	}
	
    @Test
    public void testInitSkipPointer() {
        int wordId = 11;
        assertThat(indexer.getSkipPointer().get(wordId), nullValue());
        
        //if there is no list, then create new one
        indexer.initSkipPointer(11);
        assertThat(indexer.getSkipPointer().get(wordId).size(), is(0));
        
        
        int wordId2 = 5;
        ArrayList<Integer> skipInfo = new ArrayList<Integer>(Arrays.asList(new Integer[]{2, 3, 5, 7}));
        indexer.getSkipPointer().put(wordId2, skipInfo);
        assertThat(indexer.getSkipPointer().size(), is(2));
        
        //if exist, nothing
        indexer.initSkipPointer(wordId2);
        assertThat(indexer.getSkipPointer().size(), is(2));
        assertThat(indexer.getSkipPointer().get(wordId2), is(skipInfo));
    }	
    
    @Test
    public void testAddSkipInfo() {
        int wordId = 5, docId = 1, length = 2;
        
        assertThat(indexer.addSkipInfo(wordId, docId, length), is(2));
        assertThat(indexer.lastDocId(wordId), is(1));
        assertThat(indexer.lastPosition(wordId), is(2));
        
        assertThat(indexer.addSkipInfo(wordId, 3, 4), is(6));
        assertThat(indexer.lastDocId(wordId), is(3));
        assertThat(indexer.lastPosition(wordId), is(6));
        
        assertThat(indexer.addSkipInfo(11, 2, 4), is(4));
        assertThat(indexer.lastDocId(wordId), is(3));
        assertThat(indexer.lastPosition(wordId), is(6));
        
        assertThat(indexer.lastDocId(11), is(2));
        assertThat(indexer.lastPosition(11), is(4));
        
        assertThat(indexer.addSkipInfo(wordId, 9, 7), is(13));
        assertThat(indexer.lastDocId(wordId), is(9));
        assertThat(indexer.lastPosition(wordId), is(13));
    }
    
    @Test
    public void testGetPartialIndexName() {
        assertThat(indexer.getPartialIndexName(1, 2), is(_options._indexPrefix + "/index_01_2.idx"));
        assertThat(indexer.getPartialIndexName(29, 1), is(_options._indexPrefix + "/index_29_1.idx"));
        
        assertThat(indexer.getPartialIndexName(11), is(_options._indexPrefix + "/index_11.idx"));
        assertThat(indexer.getPartialIndexName(3), is(_options._indexPrefix + "/index_03.idx"));
    }
    
    @Test
    public void testGetPartialSkipPointerName() {
        assertThat(indexer.getPartialSkipPointerName(1, 2), is(_options._indexPrefix + "/skip_01_2.idx"));
        assertThat(indexer.getPartialSkipPointerName(29, 1), is(_options._indexPrefix + "/skip_29_1.idx"));
        
        assertThat(indexer.getPartialSkipPointerName(15), is(_options._indexPrefix + "/skip_15.idx"));
        assertThat(indexer.getPartialSkipPointerName(7), is(_options._indexPrefix + "/skip_07.idx"));
    }
    
    @Test
    public void testMakeIndex() {
        indexer.makeIndex(doc1, 1);
        assertThat(indexer._dictionary.size(), is(13));
        
        ArrayList<Short> posting = indexer.getIndex().get(3);
        ArrayList<Integer> expectedSkipPoint = 
                new ArrayList<Integer>(Arrays.asList(new Integer[]{1,4}));
                
        //this posting should be (1, 2, [3, 11])
        int[] indexV = new int[]{1, 2, 3, 11};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        
        assertThat(indexer.getSkipPointer().get(3), is(expectedSkipPoint));
        
        indexer.makeIndex(doc2, 2);
        assertThat(indexer._dictionary.size(), is(28));
        
        posting = indexer.getIndex().get(3);
        expectedSkipPoint.add(2); expectedSkipPoint.add(7);
        //this posting should be (1, 2, [3, 11]), (1, 1, [19])
        indexV = new int[]{1, 2, 3, 11, 1, 1, 19};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        
        assertThat(indexer.getSkipPointer().get(3), is(expectedSkipPoint));
        
        indexer.makeIndex(doc10, 10);
        assertThat(indexer._dictionary.size(), is(39));
      
        posting = indexer.getIndex().get(3);
        expectedSkipPoint.add(10);expectedSkipPoint.add(10);  
        //this posting should be (1, 2, [3, 11]), (1, 1, [19]), (8, 1, [20])
        indexV = new int[]{1, 2, 3, 11, 1, 1, 19, 8, 1, 20};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        
        assertThat(indexer.getSkipPointer().get(3), is(expectedSkipPoint));
    }
    
    @Test
    public void testWriteToFile() throws Exception {
        //This is not a good test because it does actual file writing and reading
        //However, I don't have enough time to refactor this method and class.
        
        File testDir = new File(_options._indexPrefix);
        deleteDirectory(testDir);
        testDir.mkdirs();
        
        indexer.makeIndex(doc1, 1);
        indexer.makeIndex(doc2, 2);
        indexer.makeIndex(doc10, 10);
        
        indexer.writeToFile(1);
        
        CompressedIndex tempIndex = indexer.loadIndex(3,1);
        SkipPointer skip = indexer.loadSkipPointer(3, 1);
        
        ArrayList<Short> posting = tempIndex.get(3);
        //this posting should be (1, 2, [3, 11]), (1, 1, [19]), (8, 1, [20])
        int[] indexV = new int[]{1, 2, 3, 11, 1, 1, 19, 8, 1, 20};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        ArrayList<Integer> skipInfo = skip.get(3);
        assertThat(skipInfo, is(new ArrayList<Integer>(
                Arrays.asList(new Integer[]{1, 4, 2, 7, 10, 10})
        )));
        
        posting = tempIndex.get(33);
        //this posting should be (10, 1, 8)
        indexV = new int[]{10, 1, 8};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        skipInfo = skip.get(33);
        assertThat(skipInfo, is(new ArrayList<Integer>(
                Arrays.asList(new Integer[]{10, 3})
        )));
        
        assertThat("word id 4 should be stored in 4th index", tempIndex.get(4), nullValue());
        
        tempIndex = indexer.loadIndex(10,1);
        skip = indexer.loadSkipPointer(10, 1);
        
        posting = tempIndex.get(10);
        //this posting should be (1, 1, [10]), (1, 2, [9, 7])
        indexV = new int[]{1, 1, 10, 1, 2, 9, 7};
        for(int i=0; i<indexV.length; i++) {
            assertThat(ByteAlignUtil.decodeVbyte(i, posting), is(indexV[i]));
        }
        skipInfo = skip.get(10);
        assertThat(skipInfo, is(new ArrayList<Integer>(
                Arrays.asList(new Integer[]{1, 3, 2, 7})
        )));
        
        deleteDirectory(testDir);
    }  
    
    @Test
    public void testAdjustPostingHeader() {
        //adjustPostingHeader(ArrayList<Short> posting, ArrayList<Integer> prevSkipInfo)
        //same encoded length case
        ArrayList<Integer> pSkipInfo = new ArrayList<Integer>();
        //(1, 4) (2, 7) (10, 10)
        pSkipInfo.add(1); pSkipInfo.add(4);
        pSkipInfo.add(2); pSkipInfo.add(7);
        pSkipInfo.add(10); pSkipInfo.add(10);
        
        //above means that previous posting might be 
        //  (1, 2, [?, ?]), (1, 1, [?]), (8, 1, [?]).
        //there are three doc ids: 1, 2, 10
        //note: currently we don't care actual value of ?
        
        //let second posting be (15, 2, [3, 17]), (2, 1, [5]).
        //there are two doc ids: 15, 17.
        
        ArrayList<Short> posting = new ArrayList<Short>();
        for(int v : new int[] {15, 2, 3, 17, 2, 1, 5}) {
            for(short s : ByteAlignUtil.encodeVbyte(v)) {
                posting.add(s);
            }
        }
        
        int[] expectedV = new int[] {5, 2, 3, 17, 2, 1, 5};
        ArrayList<Short> expected = new ArrayList<Short>();
        for(int v : expectedV) {
            for(short s : ByteAlignUtil.encodeVbyte(v)) {
                expected.add(s);
            }
        }
        
        ArrayList<Short> result = indexer.adjustPostingHeader(posting, pSkipInfo);
        assertThat("head should be 5 instead of 15 because of delta encoding", result, is(expected));
        
        int idx = 1;
        for(int i=1; i<result.size(); ) {
            assertThat("Everyelse sholud be same at " + idx, 
                    ByteAlignUtil.decodeVbyte(i, result), is(expectedV[idx++]));
            i = ByteAlignUtil.nextPosition(i, result);
        }
        //suppose last doc id of previous skip pointer is 
        pSkipInfo.add(10000); pSkipInfo.add(15);
        
        //we have new posting which will be added and have doc id 10004 and 10006
        posting.clear();
        for(int v : new int[] {10004, 2, 3, 17, 2, 1, 5}) {
            for(short s : ByteAlignUtil.encodeVbyte(v)) {
                posting.add(s);
            }
        }
        expectedV = new int[] {4, 2, 3, 17, 2, 1, 5};
        expected.clear();
        for(int v : expectedV) {
            for(short s : ByteAlignUtil.encodeVbyte(v)) {
                expected.add(s);
            }
        }
        
        result = indexer.adjustPostingHeader(posting, pSkipInfo);
        assertThat("head should be 4 instead of 10004 because of delta encoding", result, is(expected));
        
        idx = 1;
        for(int i=1; i<result.size(); ) {
            assertThat("Everyelse sholud be same at " + idx, 
                    ByteAlignUtil.decodeVbyte(i, result), is(expectedV[idx++]));
            i = ByteAlignUtil.nextPosition(i, result);
        }
    }
    
    @Test
    public void testAdjustSkipInfo() {
        ArrayList<Integer> skipInfo = new ArrayList<Integer>(
                Arrays.asList(new Integer[]{5, 4, 2, 7, 10, 10})
        );
        
        //suppose that after adjusting head of corresponded posting, head bytes reduced as -1
        indexer.adjustSkipInfo(skipInfo, -1);
        assertThat("All position should be reduced by -1", skipInfo,
                is(new ArrayList<Integer>(Arrays.asList(new Integer[]{5, 3, 2, 6, 10, 9}))
        ));
        
        //suppose that after adjusting head of corresponded posting, head bytes increased as 5
        indexer.adjustSkipInfo(skipInfo, 5);
        assertThat("All position should be reduced by -1", skipInfo,
                is(new ArrayList<Integer>(Arrays.asList(new Integer[]{5, 8, 2, 11, 10, 14}))
        ));
    }
    
    private boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

}

