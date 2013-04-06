package edu.nyu.cs.cs2580;
import java.util.Scanner;
/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 *          ["new york city"], the presence of the phrase "new york city" must
 *          be recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

    public QueryPhrase(String query) {
        super(query);
    }

    @Override
    public void processQuery() {
        if (_query == null)
            return;
        
        String word = new String();
        boolean isPhrase = false;
        for(int i=0; i<_query.length(); i++){
            if (_query.charAt(i) == ' ') {
                if(!isPhrase && !word.isEmpty()){
                    _stemmer.setCurrent(word);
                    _stemmer.stem();
                    _tokens.add(_stemmer.getCurrent());
                    word = "";                
                }else if(isPhrase)
                    word += _query.charAt(i);
            }else if(_query.charAt(i) == '\"'){
                if(isPhrase){
                    String phrase = "";
                    Scanner scanner = new Scanner(word);
                    while(scanner.hasNext()){
                        _stemmer.setCurrent(scanner.next());
                        _stemmer.stem();
                        if(phrase.isEmpty())
                            phrase = _stemmer.getCurrent();
                        else
                            phrase += " " + _stemmer.getCurrent();
                    }                    
                    _tokens.add(phrase);
                    word = "";
                }
                isPhrase = !isPhrase;
            }else{
                word += _query.charAt(i);                
            }
        }
        if(!word.isEmpty()){
            _stemmer.setCurrent(word);
            _stemmer.stem();
            _tokens.add(_stemmer.getCurrent());
            word = "";
        }
    }
}
