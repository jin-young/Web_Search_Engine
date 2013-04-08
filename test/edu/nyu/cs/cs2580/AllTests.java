package edu.nyu.cs.cs2580;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ CorpusAnalyzerPagerankTest.class, IndexerInvertedCompressedTest.class, ByteAlignUtilTest.class })
public class AllTests {

}
