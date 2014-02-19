package com.sambhav.distributedcorenlp;

import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.sambhav.util.PropertyFileReader;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class ProcessArticles implements Callable<String> {

	private String articleID;
	private Set<String> NERs;
	private StanfordCoreNLP pipeline;
	private Annotation document;
	
	/*
	 * other string helps in checking if the token is a NamedEntity or not.
	 */
	static final String other = "O";
	static Logger log = Logger.getLogger(ProcessArticles.class.getName());
	
	/*
	 * Read the path of log4j.properties file.
	 */
	private static FileInputStream fileInputStreamForReadingConfigFile = null;
	private final static  String log4jPropertiesFilePath = PropertyFileReader.GetPropertyFromConfigFile(fileInputStreamForReadingConfigFile)
			.getProperty("Log4jPropertiesFilePath");
	
	ProcessArticles(String articleID, Annotation document, StanfordCoreNLP pipeline)
	{
		this.articleID = articleID;
		this.document = document;
		this.pipeline = pipeline;
		this.NERs = new HashSet<String>();
	}
	
	@Override
	public String call() throws Exception{
		
			/*
			 * Create PropertyConfigurator for logging the output.
			 */
			PropertyConfigurator.configure(log4jPropertiesFilePath);
		
			/*
			 * Start time as in when the call method is executed by the thread.
			 */
			String startTime = new DateTime().toLocalTime().toString();

			pipeline.annotate(document);
			
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			
			/*
			 * Flag: if previous token was a NER or not.
			 */
			boolean wasPreviousNER = false;
			
			/*
			 * Variable: value of the last NER
			 */
			String previousNamedEntity = null;
			
			/*
			 * Variable: Entity Name for the last NER
			 */
			String previousEntityName = null;
			
			for(CoreMap sentence: sentences) {
			      
		    	for (CoreLabel token: sentence.get(TokensAnnotation.class)){

		    		String word = token.get(TextAnnotation.class);

		    		String ne = token.get(NamedEntityTagAnnotation.class);    //NER Output in a For Loop
		    		
		    		/*
		    		 * Check if token is a Named Entity or not
		    		 */
			    	if(!(ne.contentEquals(other))){
			    		
			    		/* 
			    		 * Named Entity found!
			    		 * Check if the previous token was a Named Entity or not
			    		 */
			    		if(wasPreviousNER == false){
			    			/*
			    			 * Add the NER to Set
			    			 */
			    			NERs.add(word);
			    			
			    			/*
			    			 * Update flag and variables 
			    			 */
			    			wasPreviousNER = true;
			    			previousNamedEntity = word;
			    			previousEntityName = ne;
			    			
			    		}
			    		else{
			    			if(previousEntityName.equals(ne)){
			    				
			    				/*
			    				 * If the current NER is of same type as of previous NER
			    				 * than remove previousNamedEntity and add new concatenated one.
			    				 */
			    				
			    				NERs.remove(previousNamedEntity);
			    				
			    				/*
			    				 * Update previousNamedEntity to the new one.
			    				 */
			    				previousNamedEntity = previousNamedEntity.concat(" "+word);
			    				NERs.add(previousNamedEntity);
			    			}
			    			else{
			    				NERs.add(word);
			    				
			    				/*
				    			 * Update flag and variables 
				    			 */
			    				previousNamedEntity = word;
			    				previousEntityName = ne;
			    			}
			    			
			    		}
			    	}
			    	else {
			    		
			    		/*
			    		 * Update the flag to false if previous token was a NER
			    		 */
			    		if(wasPreviousNER == true)
			    			wasPreviousNER = false;
			    	}
		    	}
		}
			
		/*
		 *	Article processing is finished, create a new logEntry and log it. 
		 */
		String logEntry = "Article ID : "+this.articleID+", Start Time : "+startTime+", End Time : "+ new DateTime().toLocalTime()+", ThreadID : "+Thread.currentThread().getId()+", Number of NERs = "+NERs.size() + ", NER : " + NERs.toString();
		log.debug(logEntry);

		return null;
	}

}
