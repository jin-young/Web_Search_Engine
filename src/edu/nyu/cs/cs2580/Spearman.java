package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Spearman {

    /**
     * Calculation of Spearman's rank correlation coefficient
     * @param sortednumview
     * @param sortedpagerank
     * @return coefficient
     */
    public float calculate(List<Document> sortednumview, List<Document> sortedpagerank) {
        float temp = 0.0f;
        float numDocs = (float)sortednumview.size();
        
        for(int idx = 0; idx < numDocs; idx++){
            Document doc = sortednumview.get(idx);
            int nvIndex = idx;
            int pgIndex = sortedpagerank.indexOf(doc);

            if(pgIndex == -1) continue;
            temp += (float) Math.pow(pgIndex - nvIndex, 2.0d);
        }
        return (float) (1.0f - 6.0f * temp / (numDocs * ((float)Math.pow(numDocs, 2.0d) - 1.0f)));
    }

    /**
     * Sorting List<Document> with appropriate values 
     * Mode 0 : PageRank List
     * Mode 1 : NumView List
     * @param temp
     * @param mode
     * @return sorted List
     */
    public List<Document> sorting(Map<String, Document> temp, int mode) {
        List<Document> sortedList = new LinkedList<Document>(temp.values());
        if(mode == 0){  // sorting with Page Rank Value
            Collections.sort(sortedList, new Comparator<Document>() {
                @Override
                public int compare(Document o1, Document o2) {
                    if (o1.getPageRank() - o2.getPageRank() == 0) {
                        return o1.getTitle().compareTo(o2.getTitle());  
                    } else {
                        return (o1.getPageRank() - o2.getPageRank() < 0) ? 1 : -1;
                    }
                }
            });
        }else if(mode == 1){    // sorting with Num View
            Collections.sort(sortedList, new Comparator<Document>() {
                @Override
                public int compare(Document o1, Document o2) {
                    if (o1.getNumViews() - o2.getNumViews() == 0) {
                        return o1.getTitle().compareTo(o2.getTitle());  
                    } else {
                        return (o1.getNumViews() - o2.getNumViews() < 0) ? 1 : -1;
                    }
                }
            });
        }
        return sortedList;
    }

    /**
     * Load Map<String, Document> from files and build Sorted List
     * @param path
     * @param mode
     * @return sorted List
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public List<Document> load(String path, int mode) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(fis));
        Map<String, Document> new_data = null;
        try {
            new_data = (Map<String, Document>) reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        reader.close();
        return sorting(new_data, mode);
    }

    public static void main(String[] args) throws IOException {
        Spearman spearman = new Spearman();
        
        if(args.length!=2){
        	 System.out.println("Please provide 2 parameters"); 
        	 System.out.println("FORMAT : java edu.nyu.cs.cs2580.Spearman <PATH-TO-PAGERANKS> <PATH-TO-NUMVIEWS>");
       } else{
	         List<Document> sortednumview, sortedpagerank;
	         sortedpagerank = spearman.load(args[0], 0);
	         sortednumview = spearman.load(args[1], 1);
	         
	         Float result = spearman.calculate(sortednumview,sortedpagerank);
	         System.out.println(result);
         }
    }
}