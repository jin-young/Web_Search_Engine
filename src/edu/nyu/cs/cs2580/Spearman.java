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

    public static Float calculate(List<Document> sortednumview, List<Document> sortedpagerank) {
        float result = 0;
        float temp = 0;
        for(int idx = 0;idx<sortednumview.size();idx++) {
        	String doc = sortednumview.get(idx).getTitle();
            temp = temp + (float) Math.pow(sortedpagerank.indexOf(doc) - sortednumview.indexOf(doc), 2.0d);
        }
        result = (float) (1 - 6 * temp / (sortednumview.size() * Math.pow(sortednumview.size() - 1, 2.0d)));
        return result;
    }

    public static List<Document> sorting(Map<String, Document> temp) {
        List<Document> sortedList = new LinkedList<Document>(temp.values());
        Collections.sort(sortedList, new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                if (o1.getPageRank() - o2.getPageRank() == 0) {
                    return o1.getTitle().compareTo(o2.getTitle());
                } else {
                    if (o1.getPageRank() - o2.getPageRank() < 0)
                        return 1;
                    else
                        return -1;
                }
            }
        });

        return sortedList;

    }

    public static Object load(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        ObjectInputStream reader = new ObjectInputStream(fis);
        Map<String, Document> new_data = null;
        try {
            new_data = (Map<String, Document>) reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        reader.close();
        return new_data;

    }

    public static void main(String[] args) throws IOException {
         if(args.length!=2){
        	 System.out.println("Please provide 2 parameters"); } 
         else{
	         Map<String, Document> numview=(Map<String, Document>) load(args[1]); 
	         Map<String, Document> pagerank=(Map<String,Document>) load(args[0]);
	         List<Document> sortednumview =sorting(numview);
	         List<Document> sortedpagerank =sorting(pagerank);
	         Float result=calculate(sortednumview,sortedpagerank);
	         System.out.println(result);
         }
    }
}