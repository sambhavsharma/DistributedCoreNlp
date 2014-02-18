package com.sambhav.distributedcorenlp;

import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.hazelcast.core.HazelcastInstance;
import com.sambhav.util.PropertyFileReader;


public class LoadArticles implements Runnable{

	/*
	 * Start from articleCount as 1 and keep incrementing while pushing articles in
	 * the queue, also keep updating the id of the article with articleCount value.
	 */
	static int articleCount = 1;
	private static FileInputStream fileInputStreamForReadingConfigFile = null;
	
	/*
	 * Read the path of the json dump of articles file from properties/config file.
	 */
	private static final String jsonFilePath = PropertyFileReader.GetPropertyFromConfigFile(fileInputStreamForReadingConfigFile)
												.getProperty("ArticleJsonFilePath");
	
	HazelcastInstance hazelCastInstance;
	
	public LoadArticles(HazelcastInstance hazelCastInstance)
	{
		/*
		 * initialize this class with hazelCastInstance to access articles Queue
		 */
		this.hazelCastInstance = hazelCastInstance;
	}
	
	@Override
	public void run() {
		JSONParser jsonParser = new JSONParser();

		JSONArray jsonObject = null;
		
		try {
				FileReader fileReader = new FileReader(jsonFilePath);
				jsonObject = (JSONArray) jsonParser.parse(fileReader);
				
				/*
				 * Read the articles in a jsonObject and then make an iterator for the object.
				 */
				@SuppressWarnings("unchecked")
				Iterator<JSONObject>  articleItr = jsonObject.iterator();
				JSONObject articleObject = null;
				
				/*
				 * Get the articles BlockingQueue and load the articles one by one in the
				 * queue as hashmap with id and description of each article.
				 */
				BlockingQueue<Map<String,String>> articleQueue = hazelCastInstance.getQueue("articles");
				Map<String,String> articleMap = new HashMap<String,String>();
				while(articleItr.hasNext())
				{
					articleObject = articleItr.next();
					articleMap.put("description", articleObject.get("description").toString());
					articleMap.put("id", String.valueOf(articleCount++));
					articleQueue.put(articleMap);
					articleMap.clear();
				}
			
			} catch (Exception e) {
				
				e.printStackTrace();
			}
	}

}
