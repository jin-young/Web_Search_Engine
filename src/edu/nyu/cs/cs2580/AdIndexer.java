package edu.nyu.cs.cs2580;

import java.io.IOException;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class AdIndexer extends Indexer {
	private String connectionString = "jdbc:mysql://localhost:3306/";
	private String userId;
	private String userPwd;
	
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
		// TODO Auto-generated method stub

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
