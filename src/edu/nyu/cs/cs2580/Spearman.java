package edu.nyu.cs.cs2580;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Spearman {

	public static Integer calculate(Map<String, Integer> numvew, Map<String, Document> pagerank){
		List<Document> rankList = new LinkedList<Document>(pagerank.values());
		Collections.sort(rankList, new Comparator<Document>() {
			@Override
            public int compare(Document o1, Document o2) {
				if(o1.getPageRank() - o2.getPageRank() == 0) {
					return o1.getTitle().compareTo(o2.getTitle());
				} else {
					if(o1.getPageRank() - o2.getPageRank() < 0)
						return 1;
					else
						return -1;
				}
            }
		});
		
		return 0;
		
	}


	public static Object loadnumview(String numviewpath) throws IOException {
		 FileInputStream fis = new FileInputStream(numviewpath);
	        ObjectInputStream reader = new ObjectInputStream(fis);
	        Map<String, Integer> new_numview=null;
			try {
				new_numview = (Map<String, Integer>) reader.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
	        reader.close();
	        return new_numview;
	 }

	public static Object loadpagerank(String rankpath) throws IOException {
		 FileInputStream fis = new FileInputStream(rankpath);
	        ObjectInputStream reader = new ObjectInputStream(fis);
	        Map<String, Document> new_pagerank=null;
			try {
				new_pagerank = (Map<String, Document>) reader.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
	        reader.close();
	        return new_pagerank;

	 }
	
	
	public static void main(String[] args) throws IOException{
		if(args.length!=2){
			System.out.println("Please provide 2 parameters");
		}
		else{
			Map<String, Integer> numview=(Map<String, Integer>) loadnumview(args[1]);
			Map<String, Document> pagerank=(Map<String, Document>) loadpagerank(args[0]);
			Integer result=calculate(numview,pagerank);
		}
	}
}