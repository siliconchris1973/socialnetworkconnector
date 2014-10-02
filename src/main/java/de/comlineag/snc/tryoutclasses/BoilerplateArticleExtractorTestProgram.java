package de.comlineag.snc.tryoutclasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//import de.l3s.boilerpipe.extractors.ArticleExtractor;
@Deprecated
public class BoilerplateArticleExtractorTestProgram {

	private BoilerplateArticleExtractorTestProgram() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1){
			throw new IllegalArgumentException("No file given as argument");
		}
/*
		for(String arg : args){
			try {

				System.out.println("Extracting "+arg);
				File file = new File(arg);
				String html = getFileContent(new FileInputStream(file), "UTF-8");
				String text = ArticleExtractor.INSTANCE.getText(html);
				System.out.println(text);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
*/
	}

	public static String getFileContent(
			   FileInputStream fis,
			   String	encoding ) throws IOException
	{
	   try( BufferedReader br =
	           new BufferedReader( new InputStreamReader(fis, encoding )))
	   {
	      StringBuilder sb = new StringBuilder();
	      String line;
	      while(( line = br.readLine()) != null ) {
	         sb.append( line );
	         sb.append( '\n' );
	      }
	      return sb.toString();
	   }
	}

}
