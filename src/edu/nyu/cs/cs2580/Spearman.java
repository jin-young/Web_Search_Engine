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
        int index = 0;
        float result = 0;
        float temp = 0;
        String doc = sortednumview.get(index).getTitle();
        while (doc != null) {
            temp = temp + (float) Math.exp(sortedpagerank.indexOf(doc) - sortednumview.indexOf(doc));
            index++;
            doc = sortednumview.get(index).getTitle();
        }
        result = (float) (1 - 6 * temp / (sortednumview.size() * Math.exp(sortednumview.size() - 1)));
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

    public static Object loadnumview(String numviewpath) throws IOException {
        FileInputStream fis = new FileInputStream(numviewpath);
        ObjectInputStream reader = new ObjectInputStream(fis);
        Map<String, Document> new_numview = null;
        try {
            new_numview = (Map<String, Document>) reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        reader.close();
        return new_numview;
    }

    public static Object loadpagerank(String rankpath) throws IOException {
        FileInputStream fis = new FileInputStream(rankpath);
        ObjectInputStream reader = new ObjectInputStream(fis);
        Map<String, Document> new_pagerank = null;
        try {
            new_pagerank = (Map<String, Document>) reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        reader.close();
        return new_pagerank;

    }

    public static void main(String[] args) throws IOException {
        /*
         * if(args.length!=2){
         * System.out.println("Please provide 2 parameters"); } else{
         * Map<String, Document> numview=(Map<String, Document>)
         * loadnumview(args[1]); Map<String, Document> pagerank=(Map<String,
         * Document>) loadpagerank(args[0]);
         */
        Map<String, Document> numview=(Map<String, Document>) loadnumview("data/index/numView.dat");
        Map<String, Document> pagerank=(Map<String, Document>) loadpagerank("data/index/pageRank.dat");
        // List<Document> sortednumview =sorting(numview);
        // List<Document> sortedpagerank =sorting(pagerank);
        // System.out.println(sortedpagerank);
        // Float result=calculate(sortednumview,sortedpagerank);
        // System.out.println(result);
        // }
    }
}