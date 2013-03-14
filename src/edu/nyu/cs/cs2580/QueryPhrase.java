package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
      if (_query == null) {
	  return;
      }
      //remove '\"' 
      while(true){
	  if(!_query.contains("\""))
	      break;
	  int pos = _query.indexOf("\"");
	  _query = _query.substring(0, pos) + _query.substring(pos+1);
      }

      Scanner s = new Scanner(_query);
      while (s.hasNext()) {
	  String q = s.next();
	  _stemmer.setCurrent(q);
	  _stemmer.stem();
	  q = _stemmer.getCurrent();
	  _tokens.add(q);

	  System.out.println(q);
      }
      s.close();  
  }
}
