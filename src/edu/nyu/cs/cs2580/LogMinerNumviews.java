package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

	protected Map<String, Integer> _numview = new HashMap<String, Integer>();

	public LogMinerNumviews(Options options) {
		super(options);
	}

	/**
	 * This function processes the logs within the log directory as specified by
	 * the {@link _options}. The logs are obtained from Wikipedia dumps and have
	 * the following format per line: [language]<space>[article]<space>[#views].
	 * Those view information are to be extracted for documents in our corpus
	 * and stored somewhere to be used during indexing.
	 * 
	 * Note that the log contains view information for all articles in Wikipedia
	 * and it is necessary to locate the information about articles within our
	 * corpus.
	 * 
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		System.out.println("Computing using " + this.getClass().getName());

		String path = _options._logPrefix;
		// Get All Files list in the Corpus Folder (data/wiki)
		File folder = new File(_options._corpusPrefix);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				_numview.put(file.getName(), 0);
			}
		}

		return;
	}

	/**
	 * During indexing mode, this function loads the NumViews values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		System.out.println("Loading using " + this.getClass().getName());
		return null;
	}

	/**
	 * Document processing must satisfy the following: 1) Non-visible page
	 * content is removed, e.g., those inside <script> tags 2) Tokens are
	 * stemmed with Step 1 of the Porter's algorithm 3) No stop word is removed,
	 * you need to dynamically determine whether to drop the processing of a
	 * certain inverted list.
	 */
}
