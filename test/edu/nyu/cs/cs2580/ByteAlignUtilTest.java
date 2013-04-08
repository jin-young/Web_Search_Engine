package edu.nyu.cs.cs2580;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class ByteAlignUtilTest {
    private ArrayList<Short> shortList;
    
    @Before
    public void setUp() {
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
    public void testEncodeVbyte() {
        assertThat(ByteAlignUtil.encodeVbyte(1), is(new short[] { 0x81 }));
        assertThat(ByteAlignUtil.encodeVbyte(6), is(new short[] { 0x86 }));
        assertThat(ByteAlignUtil.encodeVbyte(127), is(new short[] { 0xFF }));
        assertThat(ByteAlignUtil.encodeVbyte(128), is(new short[] { 0x01, 0x80 }));
        assertThat(ByteAlignUtil.encodeVbyte(130), is(new short[] { 0x01, 0x82 }));
        assertThat(ByteAlignUtil.encodeVbyte(20000), is(new short[] { 0x01, 0x1C,
                0xA0 }));
    }
    
    @Test
    public void testDecodeVbyte() {
        assertThat("Number of occurance of first group was wrong",
                ByteAlignUtil.decodeVbyte(1, shortList), is(1));

        assertThat("Number of occurance of second group was wrong",
                ByteAlignUtil.decodeVbyte(4, shortList), is(4));

        assertThat("Decoding single byte at position 2 was incorrect ",
                ByteAlignUtil.decodeVbyte(2, shortList), is(2));

        assertThat("Decoding single byte at position 7 was incorrect ",
                ByteAlignUtil.decodeVbyte(7, shortList), is(3));

        assertThat("Decoding single byte at position 11 was incorrect ",
                ByteAlignUtil.decodeVbyte(11, shortList), is(11));

        assertThat("Decoding multibytes from position 5 was incorrect ",
                ByteAlignUtil.decodeVbyte(5, shortList), is(132));

        assertThat("Decoding multibytes from position 9 was incorrect ",
                ByteAlignUtil.decodeVbyte(8, shortList), is(20000));
    }
    
    @Test
    public void testNextPosition() {
        // (2 1 2) (4 4 123 3 20000 11)
        for (int i = 0; i <= 2; i++) {
            assertThat("Next position after one byte value at " + i,
                    ByteAlignUtil.nextPosition(i, shortList), is(i + 1));
        }

        assertThat("Next position after multi-byte value at 5",
                ByteAlignUtil.nextPosition(5, shortList), is(7));

        assertThat("Next position after multi-byte value at 8",
                ByteAlignUtil.nextPosition(8, shortList), is(11));
    }
    
    @Test
    public void testHowManyAppeared() {
        assertThat(ByteAlignUtil.howManyAppeared(0, shortList), is(1));
        assertThat(ByteAlignUtil.howManyAppeared(3, shortList), is(4));
        assertThat(ByteAlignUtil.howManyAppeared(12, shortList), is(130));
    }
    
    @Test
    public void testAppendEncodedValueToList() {
        ArrayList<Short> expect = new ArrayList<Short>(shortList);
        
        int offset = ByteAlignUtil.appendEncodedValueToList(shortList, 1);
        assertThat("One byte added", offset, is(1));
        
        expect.add((short)0x81);
        
        assertThat(shortList, is(expect));
        
        offset = ByteAlignUtil.appendEncodedValueToList(shortList, 20000);
        assertThat("Three bytes added", offset, is(3));
        
        expect.add((short)0x01);
        expect.add((short)0x1C);
        expect.add((short)0xA0);
        
        assertThat(shortList, is(expect));
    }
}
