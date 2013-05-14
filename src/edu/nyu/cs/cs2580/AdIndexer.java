package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class AdIndexer extends IndexerInvertedCompressed {
	private static final long serialVersionUID = 3405003808512996691L;

	private String connectionString = "jdbc:mysql://localhost:3306/";
	private String userId;
	private String userPwd;

	// Stores all Document in memory
	protected Map<Integer, Document> _documents = new HashMap<Integer, Document>();

	public AdIndexer(Options opts) {
		super(opts);
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		connectionString += opts._addbname;
		userId = opts._addbuser;
		userPwd = opts._addbpwd;
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
	public void constructIndex() throws IOException {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			con = DriverManager
					.getConnection(connectionString, userId, userPwd);
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT id, title, url, content FROM ads_info");

			while (rs.next()) {
				int id = rs.getInt("id");
				String title = rs.getString("title");
				String url = rs.getString("url");
				String content = rs.getString("content");

				processDocument(id, title, url, content);
			}
			
			writeToFile(0);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Document processing must satisfy the following: 1) Non-visible page
	 * content is removed, e.g., those inside <script> tags 2) Tokens are
	 * stemmed with Step 1 of the Porter's algorithm 3) No stop word is removed,
	 * you need to dynamically determine whether to drop the processing of a
	 * certain inverted list.
	 */
	public void processDocument(int did, String title, String url, String content) {
		System.out.println(did + ". " + title);

		int tokenSize = 0;
		if (content.trim().length() > 0) {
			tokenSize = makeIndex(title + " " + content, did);
		}
		// String content = retrieveContent(file);
		// content = removeNonVisible(content);

		AdDocumentIndexed doc = new AdDocumentIndexed(did);

		doc.setTitle(title);
		doc.setUrl(url);
		doc.setTokenSize(tokenSize);
		doc.setKeywords(content);

		// doc.setNumViews(numViews.get(file.getName()).getNumViews());
		// doc.setPageRank(numViews.get(file.getName()).getPageRank());

		_documents.put(did, doc);
		++_numDocs;
	}

	/**
	 * Get Pure content from file
	 * 
	 * @param file
	 * @return content
	 */
	public String getFileContent(String url) {
		org.jsoup.nodes.Element body = null;
		try {
			body = Jsoup.connect(url).get().body();
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
		} catch (IOException e) {
			System.err.println("Error Occurred while process document '" + url
					+ "'");
			e.printStackTrace();
			System.exit(1);
		}
		return body.text();
	}

	public int makeIndex(String content, int docId) {
		Map<Integer, ArrayList<Integer>> wordsPositionsInDoc = wordsPositionsInDoc(content);

		int numOfTokens = 0;
		for (int wordId : wordsPositionsInDoc.keySet()) {
			initIndex(wordId);
			initSkipPointer(wordId);

			numOfTokens += wordsPositionsInDoc.get(wordId).size();

			int offset = addPositionsToIndex(wordsPositionsInDoc.get(wordId),
					docId, wordId);
			addSkipInfo(wordId, docId, offset);

		}
		return numOfTokens;
	}

	@Override
	public void writeToFile(int round) {
		if (_index.isEmpty())
			return;

		flushCurrentIndex(_index, 0, 0);
		flushCurrentSkipPointer(_skipPointer, 0, 0);
	}

	protected void flushCurrentIndex(CompressedIndex tempIndex, int corpusId, int round) {
        ObjectOutputStream writer = null;
        if (tempIndex != null) {
            try {
	        	String indexPrefix = _options._indexPrefix + "/ad_index.idx";
                 
                writer = createObjOutStream(indexPrefix);
                writer.writeObject(tempIndex);
                writer.close();
                writer = null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error during partial index writing");
            }

            tempIndex.clear();
            tempIndex = null;
        }
    }

	protected void flushCurrentSkipPointer(SkipPointer sp, int corpusId, int round) {
		ObjectOutputStream writer = null;
		if (sp != null) {
			try {
		        String indexPrefix = _options._indexPrefix + "/ad_skip.idx";
				writer = createObjOutStream(indexPrefix);
				writer.writeObject(sp);
				writer.close();
				writer = null;
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Error during partial skip pointer writing");
			}

			sp.clear();
			sp = null;
		}
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
