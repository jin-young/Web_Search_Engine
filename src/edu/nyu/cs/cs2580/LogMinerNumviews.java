package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

	public LogMinerNumviews(Options options) {
		super(options);
	}

	/**
	 * This function processes the logs within the log directory as specified by
	 * the {@link _options}. The logs are obtained from Wikipedia dumps and have
	 * the following format per line: [language]<space>[article]<space>[#views].
	 * Those view information are to be extracted for documents in our corpus
	 * and stored somewhere to be used during indexing.
	 * 
	 * Note that the log contains view information for all articles in Wikipedia
	 * and it is necessary to locate the information about articles within our
	 * corpus.
	 * 
	 * @throws IOException
	 */
    @Override
	public void compute() throws IOException {
		System.out.println("Computing using " + this.getClass().getName());

		File folder = new File(_options._corpusPrefix);
		File[] listOfFiles = folder.listFiles();
		
		Map<String, Integer> _numview = new HashMap<String, Integer>();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				_numview.put(file.getName(), 0);
			}
		}
		
		String path = _options._logPrefix + "/20130301-160000.log";
		BufferedReader read = new BufferedReader(new FileReader(path));
		String line;

		while ((line = read.readLine()) != null) {
            String[] lineSplit = line.split(" ");
            if(lineSplit.length == 3){
                try{
                	String data = lineSplit[1];
                	String[] slashSplit = data.split("/");
                	String data2="";
                	if(slashSplit!=null && slashSplit.length!=0)
                		data2=slashSplit[slashSplit.length-1];
                	StringBuffer tempBuffer = new StringBuffer();
                	int incrementor = 0;
                    int dataLength = data2.length();
                    while (incrementor < dataLength) {
                    	 char charecterAt = data2.charAt(incrementor);
                         if (charecterAt == '%') {
                            tempBuffer.append("<percentage>");
                         } else {
                            tempBuffer.append(charecterAt);
                         }
                         incrementor++;
                    }
                    
                    data2 = tempBuffer.toString();
                    
                    String temp = URLDecoder.decode(data2, "UTF-8");
                    if(_numview.containsKey(temp)){
                    	String numview =  lineSplit[2];
                    	if(numview.equals("")||numview==null||numview.length()==0)
                    		 numview="0";
                    	
                    	int numviewInt = Integer.parseInt(numview);
                    	
                    	 _numview.put(temp, numviewInt);
                    }
                } catch(Exception e){
                    System.err.println(e.getMessage());
                }
            }else{
            	//wrong format log
            }
		}
		read.close();
		
		String dicFile = _options._indexPrefix + "/numView.dat";
		ObjectOutputStream writer = 
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dicFile)));
		writer.writeObject(_numview);
		writer.close();
		
		return;
	}

	/**
	 * During indexing mode, this function loads the NumViews values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
    @Override
	public Object load() throws IOException {
		System.out.println("Loading using " + this.getClass().getName());
		String filePath = _options._indexPrefix + "/numView.dat";
		
		ObjectInputStream reader = 
		        new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
		Map<String, Integer> numviews = null;
		
		try {
		    numviews = (Map<String, Integer>)reader.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Error during reading numview data");
        } finally {
            reader.close();
        }
		
		return numviews;
	}

}
