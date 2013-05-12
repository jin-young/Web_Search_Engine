package edu.nyu.cs.cs2580;

import java.util.Vector;

public class ScoredDocs {
	private long _num_of_result;
	private Vector<ScoredDocument> _sDocs;
	private String _run_time;

	public ScoredDocs() {
		_num_of_result = 0;
		_sDocs = new Vector<ScoredDocument>();
	}
	
	public long get_num_of_result() {
		return _num_of_result;
	}

	public void set_num_of_result(long _num_of_result) {
		this._num_of_result = _num_of_result;
	}

	public int size() {
		return _sDocs.size();
	}
	
	public Vector<ScoredDocument> getScoredDocs() {
		return _sDocs;
	}

	public void add(ScoredDocument sd) {
		_sDocs.add(sd);
	}
	
	public String get_run_time() {
		return _run_time;
	}

	public void set_run_time(String _run_time) {
		this._run_time = _run_time;
	}	
}
