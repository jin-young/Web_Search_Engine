package edu.nyu.cs.cs2580;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.easymock.EasyMock.*;

import java.util.ArrayList;

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

		indexer = (IndexerInvertedCompressed) Indexer.Factory
				.getIndexerByOption(_options);
		
		// (2 1 2) (4 4 123 3 20000 11)
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
}
