package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

/**
 * Representation of a user query.
 * 
 * In HW1: instructors provide this simple implementation.
 * 
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 * 
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
    public String _query = null;
    public Vector<String> _tokens = new Vector<String>();
    protected static SnowballStemmer _stemmer = new englishStemmer();

    public Query(String query) {
        _query = query;
    }

    public void processQuery() {
        if (_query == null) {
            return;
        }
        Scanner s = new Scanner(_query);
        while (s.hasNext()) {
            String q = s.next();
            _stemmer.setCurrent(q);
            _stemmer.stem();
            q = _stemmer.getCurrent();
            _tokens.add(q);
        }
        s.close();
    }
}
