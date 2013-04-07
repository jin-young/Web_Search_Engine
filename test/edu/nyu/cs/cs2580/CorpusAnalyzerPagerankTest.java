package edu.nyu.cs.cs2580;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class CorpusAnalyzerPagerankTest {

    @Test
    public void testMatrixMulti() {
        Options _options = createMock(SearchEngine.Options.class);
        _options._indexerType = "inverted-compressed";
        
        CorpusAnalyzerPagerank anal = new CorpusAnalyzerPagerank(_options);
                
        Map<Integer, Map<Integer, Integer>> org = new HashMap<Integer, Map<Integer, Integer>>();
        Map<Integer, Integer> inner = new HashMap<Integer, Integer>();
        inner.put(1, 1);
        inner.put(2, 1);
        org.put(0, inner);
        
        inner = new HashMap<Integer, Integer>();
        inner.put(0, 1);
        inner.put(1, 1);
        org.put(1, inner);
        
        inner = new HashMap<Integer, Integer>();
        inner.put(1, 1);
        org.put(2, inner);
        
        Map<Integer, Map<Integer, Integer>> result = anal.matrixMulti(org);
        
        Map<Integer, Map<Integer, Integer>> expect = new HashMap<Integer, Map<Integer, Integer>>();
        inner = new HashMap<Integer, Integer>();
        inner.put(0, 1);
        inner.put(1, 2);
        expect.put(0, inner);
        
        inner = new HashMap<Integer, Integer>();
        inner.put(0, 1);
        inner.put(1, 2);
        inner.put(2, 1);
        expect.put(1, inner);
        
        inner = new HashMap<Integer, Integer>();
        inner.put(0, 1);
        inner.put(1, 1);
        expect.put(2, inner);
        
        assertThat("", result, is(expect));
    }

}
