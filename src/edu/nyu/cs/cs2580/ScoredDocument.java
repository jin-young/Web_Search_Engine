package edu.nyu.cs.cs2580;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
  private Document _doc;
  private double _score;

  public ScoredDocument(Document doc, double score) {
    _doc = doc;
    _score = score;
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();
    buf.append(_doc._docid).append("\t");
    buf.append(_doc.getTitle()).append("\t");
    buf.append(_score).append("\t");
    buf.append(_doc.getPageRank()).append("\t");
    buf.append(_doc.getNumViews());
    return buf.toString();
  }

  public String getUrl(){
      return _doc.getUrl();
  }
  public double getScore(){
      return _score;
  }

  /**
   * @CS2580: Student should implement {@code asHtmlResult} for final project.
   */
  public String asHtmlResult() {
	String ret = "<a href='/transfer?redirect_to=" + _doc.getUrl() + "'>";
	ret += "<div>";
	ret += _doc.getTitle();
	ret += "</div>";
	ret += "<div>";
	ret += _doc.getTextToDisplay();
	ret += "</div>";
	ret += "</a>";
    return ret;
  }

  @Override
  public int compareTo(ScoredDocument o) {
    if (this._score == o._score) {
      return 0;
    }
    return (this._score > o._score) ? 1 : -1;
  }
}
