import java.io.*;
import java.util.*;
import java.lang.*;

public class FileReadTest{
    public static void main(String [] args) throws IOException{
	File file = new File("Zeus");
	FileReadTest test = new FileReadTest();
	try{
	    test.processDocument(file);
	}finally{
	    System.out.println("error1");
	}
    }
    
    private void processDocument(File file){
	String content = retrieveContent(file);
	content = removeNonVisible(content);
	readTermVector(content);
    }
    
    private void readTermVector(String content) {
	Scanner s = new Scanner(content); // Uses white space by default.
	while (s.hasNext()) {
	    String token = porterAlg( s.next() );
	    System.out.println(token + " ");
	}
	s.close();
	return;
    }

    private String porterAlg(String word){
	String ret = word;
	if(word.endsWith("s")){
	    if(word.endsWith("sses"))
		ret = word.substring(0, word.length()-5);
	    else if(word.endsWith("ies"))
		ret = word.substring(0, word.length()-4);
	    else if(word.endsWith("s"))
		ret = word.substring(0, word.length()-2);
	}
	return ret;
    }
    
    private String retrieveContent(File file){
	try{
	    Scanner scanner = new Scanner(file);
	    //Scanner s = scanner.useDelimiter("\t");
	    String content="";
	    CharSequence csBodyStart = "<body", csBodyEnd = "</body>";
	    boolean readBodyFlag=false;
	    while (scanner.hasNextLine()) {
		String line = scanner.nextLine();
		if(readBodyFlag || line.contains(csBodyStart)){
		    content += line;
		    readBodyFlag = true;
		    if(line.contains(csBodyEnd))
			readBodyFlag = false;
		}	    
	    }
	    //s.close();
	    scanner.close();
	    return content;
	
	}finally{
	    System.out.println("error2");
	    return null;
	}
    }

    private String removeNonVisible(String content){
	StringBuffer sb = new StringBuffer();
	boolean readFlag = true;
	for(int i=0; i<content.length(); i++){
	    char ch = content.charAt(i);
	    if(ch == '<')
		readFlag = false;
	    else if(ch == '>')
		readFlag = true;
	    else if(readFlag)
		sb.append(ch);
	}
	return sb.toString();
    }
}