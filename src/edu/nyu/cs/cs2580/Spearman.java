package edu.nyu.cs.cs2580;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class Spearman {

	public static Integer calculate(Map<String, Integer> numvew, Map<String, Integer> pagerank){
	
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
	        Map<String, Integer> new_pagerank=null;
			try {
				new_pagerank = (Map<String, Integer>) reader.readObject();
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
			Map<String, Integer> pagerank=(Map<String, Integer>) loadpagerank(args[0]);
			Integer result=calculate(numview,pagerank);
		}
	}
}