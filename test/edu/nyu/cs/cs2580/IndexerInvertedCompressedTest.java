package edu.nyu.cs.cs2580;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedCompressedTest {
	private ArrayList<Short> shortList;

	private IndexerInvertedCompressed indexer;

	@Before
	public void setUp() {
		Options _options = createMock(SearchEngine.Options.class);
		_options._indexerType = "inverted-compressed";
		_options._corpusAnalyzerType = "";
		_options._logMinerType = "";
		
		indexer = (IndexerInvertedCompressed) Indexer.Factory
				.getIndexerByOption(_options);
		
		// (2 1 2) (4 4 132 3 20000 11) (5 130 1 2 3 4 5 6)
		shortList = new ArrayList<Short>();
		shortList.add((short) 0x82);
		shortList.add((short) 0x81);
		shortList.add((short) 0x82);
		
		shortList.add((short) 0x84);
		shortList.add((short) 0x84);
		shortList.add((short) 0x01);
		shortList.add((short) 0x84);
		shortList.add((short) 0x83);
		shortList.add((short) 0x01);
		shortList.add((short) 0x1C);
		shortList.add((short) 0xA0);
		shortList.add((short) 0x8B);
		
		shortList.add((short) 0x85);
		shortList.add((short) 0x01);
		shortList.add((short) 0x82);
		shortList.add((short) 0x81);
		shortList.add((short) 0x82);
		shortList.add((short) 0x83);
		shortList.add((short) 0x84);
		shortList.add((short) 0x85);
		shortList.add((short) 0x86);
	}

	@Test
	public void testEncodeVbyte() {
		assertThat(indexer.encodeVbyte(1), is(new short[] { 0x81 }));
		assertThat(indexer.encodeVbyte(6), is(new short[] { 0x86 }));
		assertThat(indexer.encodeVbyte(127), is(new short[] { 0xFF }));
		assertThat(indexer.encodeVbyte(128), is(new short[] { 0x01, 0x80 }));
		assertThat(indexer.encodeVbyte(130), is(new short[] { 0x01, 0x82 }));
		assertThat(indexer.encodeVbyte(20000), is(new short[] { 0x01, 0x1C,
				0xA0 }));
	}

	@Test
	public void testDecodeVbyte() {
		assertThat("Number of occurance of first group was wrong",
				indexer.decodeVbyte(1, shortList), is(1));

		assertThat("Number of occurance of second group was wrong",
				indexer.decodeVbyte(4, shortList), is(4));

		assertThat("Decoding single byte at position 2 was incorrect ",
				indexer.decodeVbyte(2, shortList), is(2));

		assertThat("Decoding single byte at position 7 was incorrect ",
				indexer.decodeVbyte(7, shortList), is(3));

		assertThat("Decoding single byte at position 11 was incorrect ",
				indexer.decodeVbyte(11, shortList), is(11));

		assertThat("Decoding multibytes from position 5 was incorrect ",
				indexer.decodeVbyte(5, shortList), is(132));

		assertThat("Decoding multibytes from position 9 was incorrect ",
				indexer.decodeVbyte(8, shortList), is(20000));
	}

	@Test
	public void testNextPosition() {
		// (2 1 2) (4 4 123 3 20000 11)
		for (int i = 0; i <= 2; i++) {
			assertThat("Next position after one byte value at " + i,
					indexer.nextPosition(i, shortList), is(i + 1));
		}

		assertThat("Next position after multi-byte value at 5",
				indexer.nextPosition(5, shortList), is(7));

		assertThat("Next position after multi-byte value at 8",
				indexer.nextPosition(8, shortList), is(11));
	}
	
	@Test
	public void testHowManyAppeared() {
		assertThat(indexer.howManyAppeared(0, shortList), is(1));
		assertThat(indexer.howManyAppeared(3, shortList), is(4));
		assertThat(indexer.howManyAppeared(12, shortList), is(130));
	}
	

	/*
     {
         Brief 1=[1], 
         2=[2], 
         Search 3=[3, 29, 21], 
         engines 4=[4, 29], 
         5=[5, 38], 
         6=[6, 40], 
         7=[7, 31], 
         8=[8], 
         9=[9], 
         10=[10, 12, 7],
         11=[11], 
         12=[12], 
         13=[13], 
         14=[14, 35], 
         15=[15], 
         17=[17, 17], 
         16=[16], 
         19=[19], 
         18=[18, 17], 
         21=[21], 
         20=[20, 6], 
         23=[24], 
         22=[23], 
         25=[27], 
         24=[25], 
         27=[30], 
         26=[28], 
         29=[36], 
         28=[31, 21], 
         31=[39], 
         30=[37], 
         34=[42], 
         35=[44], 
         32=[40], 
         33=[41], 
         38=[48], 
         39=[50], 
         36=[45, 6], 
         37=[47]}>
	 */
	@Test
	public void testWordsPositionsInDoc() {
	    String doc = "Brief Description: Search engines have become a " + 
	                "core part of our daily lives. In this course, we will " +
	                "study the foundations of information retrieval and the " +
	                "technical aspects of modern Web search engines. We will " +
	                "also explore a few advanced topics that have emerged " + 
	                "to become highly influential in relation to Web search. ";
	    
	    // doc's words' positions are described above current method 
	    
	    Map<Integer, ArrayList<Integer>> result = indexer.wordsPositionsInDoc(doc, 1);
	    
	    assertThat("Result shouhd have 39 kyes", result.keySet().size(), is(39));
	    assertThat("Brief shouhd exist only one", result.get(1).size(), is(1));
	    
	    assertThat("Search shouhd exist three", result.get(3).size(), is(3));
	    assertThat("Search's position should be [3, 29, 21]", 
	                    result.get(3).toArray(new Integer[1]), is(new Integer[]{3, 29, 21} ));
	    
	    assertThat("Engine shouhd exist two", result.get(4).size(), is(2));
        assertThat("Engine's position should be [4, 29]", 
                        result.get(4).toArray(new Integer[1]), is(new Integer[]{4, 29} ));	    
        
        assertThat("Web shouhd exist two", result.get(28).size(), is(2));
        assertThat("Web's position should be [31, 21]", 
                        result.get(28).toArray(new Integer[1]), is(new Integer[]{31, 21} ));
        
        assertThat("relation shouhd exist one", result.get(39).size(), is(1));
        assertThat("relation's position should be [50]", 
                        result.get(39).toArray(new Integer[1]), is(new Integer[]{50} ));
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
}
