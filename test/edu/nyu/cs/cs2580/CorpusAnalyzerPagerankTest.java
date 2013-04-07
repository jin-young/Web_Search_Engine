package edu.nyu.cs.cs2580;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class CorpusAnalyzerPagerankTest {

    MapMatrix miniMatrix;
    
    @Before
    public void setUp() {
        miniMatrix = array2MapMatrix( new float[][] {
            {0.0f, 2.0f, 1.0f, 0.0f, 0.0f, 3.0f},
            {1.0f, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f},
            {0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f},
            {0.0f, 3.0f, 2.0f, 0.0f, 1.0f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f},
            {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f}
        });
    }
    
    @Test
    public void testMatrixSqure() {
        Options _options = createMock(SearchEngine.Options.class);
        _options._indexerType = "inverted-compressed";
        
        CorpusAnalyzerPagerank anal = new CorpusAnalyzerPagerank(_options);
        
        MapMatrix result = anal.matrixMulti(miniMatrix, miniMatrix);
        
        MapMatrix expect = array2MapMatrix(new float[][] {
            {5.0f, 1.0f, 1.0f, 0.0f, 7.0f, 4.0f},
            {0.0f, 4.0f, 1.0f, 0.0f, 0.0f, 5.0f},
            {2.0f, 1.0f, 1.0f, 0.0f, 3.0f, 2.0f},
            {3.0f, 3.0f, 2.0f, 0.0f, 6.0f, 3.0f},
            {2.0f, 0.0f, 0.0f, 0.0f, 3.0f, 1.0f},
            {1.0f, 3.0f, 1.0f, 0.0f, 1.0f, 5.0f}
        });
        
        System.out.println("G M");
        float[][] t = new float[6000][6000];
        for(int i=0; i<t.length; i++)
            for(int j=0; j<t[i].length; j++) 
                t[i][j] = 0.0f;
        System.out.println("G M D");
        
        assertThat("M1 x M1 was failed", result, is(expect));
    }
    
    @Test
    public void testMatrixCube() {
        Options _options = createMock(SearchEngine.Options.class);
        _options._indexerType = "inverted-compressed";
        
        CorpusAnalyzerPagerank anal = new CorpusAnalyzerPagerank(_options);
        
        MapMatrix result = anal.matrixMulti(miniMatrix, miniMatrix);
        
        MapMatrix expect2 = array2MapMatrix(new float[][] {
            {5.0f, 18.0f, 6.0f, 0.0f,  6.0f, 27.0f},
            {9.0f,  1.0f, 1.0f, 0.0f, 13.0f,  6.0f},
            {3.0f,  8.0f, 3.0f, 0.0f,  4.0f, 12.0f},
            {6.0f, 14.0f, 5.0f, 0.0f,  9.0f, 20.0f},
            {1.0f,  7.0f, 2.0f, 0.0f,  1.0f, 10.0f},
            {8.0f,  4.0f, 2.0f, 0.0f, 11.0f, 10.0f}
        });
        
        assertThat("M1 x M1 x M1 was failed", anal.matrixMulti(result, miniMatrix), is(expect2));
    }
    
    @Test
    public void testMatrixTimesScala() {
        Options _options = createMock(SearchEngine.Options.class);
        _options._indexerType = "inverted-compressed";
        
        CorpusAnalyzerPagerank anal = new CorpusAnalyzerPagerank(_options);
        
        MapMatrix result = anal.matrixTimesScala(3, miniMatrix);
        
        MapMatrix expect2 = array2MapMatrix(new float[][] {
                {0.0f, 6.0f, 3.0f, 0.0f, 0.0f, 9.0f},
                {3.0f, 0.0f, 0.0f, 0.0f, 6.0f, 0.0f},
                {0.0f, 3.0f, 3.0f, 0.0f, 0.0f, 3.0f},
                {0.0f, 9.0f, 6.0f, 0.0f, 3.0f, 0.0f},
                {0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 3.0f},
                {3.0f, 0.0f, 0.0f, 0.0f, 3.0f, 3.0f}
        });
        
        assertThat("M1 x 3 was failed", result, is(expect2));
    }
    
    /*
    public void testMatrixSum() {
        Options _options = createMock(SearchEngine.Options.class);
        _options._indexerType = "inverted-compressed";
        
        CorpusAnalyzerPagerank anal = new CorpusAnalyzerPagerank(_options);
        
        MapMatrix second = array2MapMatrix(new float[][] {
            {5.0f, 1.0f, 1.0f, 0.0f, 7.0f, 4.0f},
            {0.0f, 4.0f, 1.0f, 0.0f, 0.0f, 5.0f},
            {2.0f, 1.0f, 1.0f, 0.0f, 3.0f, 2.0f},
            {3.0f, 3.0f, 2.0f, 0.0f, 6.0f, 3.0f},
            {2.0f, 0.0f, 0.0f, 0.0f, 3.0f, 1.0f},
            {1.0f, 3.0f, 1.0f, 0.0f, 1.0f, 5.0f}
        });
        
        MapMatrix result = anal.matrixSum(miniMatrix, second);
        
        MapMatrix expect = array2MapMatrix(new float[][] {
                {0.0f, 6.0f, 3.0f, 0.0f, 0.0f, 9.0f},
                {3.0f, 0.0f, 0.0f, 0.0f, 6.0f, 0.0f},
                {0.0f, 3.0f, 3.0f, 0.0f, 0.0f, 3.0f},
                {0.0f, 9.0f, 6.0f, 0.0f, 3.0f, 0.0f},
                {0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 3.0f},
                {3.0f, 0.0f, 0.0f, 0.0f, 3.0f, 3.0f}
        });
        
        assertThat("M1 + M2 was failed", result, is(expect));
    }
    */

    private MapMatrix array2MapMatrix(float[][] seedValues) {
        MapMatrix result = new MapMatrix();
        
        for(int i=0; i<seedValues.length;i ++) {
            Map<Integer, Float> inner = new HashMap<Integer, Float>();
            for(int j=0; j<seedValues[0].length; j++) {
                if(seedValues[i][j] != 0.0f) {
                    inner.put(j, seedValues[i][j]);
                }
            }
            if(inner.size() > 0) {
                result.put(i, inner);
            }
        }
        
        return result;
    }
}
