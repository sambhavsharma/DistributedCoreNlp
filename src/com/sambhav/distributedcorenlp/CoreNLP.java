package com.sambhav.distributedcorenlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sambhav.util.PropertyFileReader;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class CoreNLP {

	private static FileInputStream fileInputStreamForReadingConfigFile = null;
	static Logger log = Logger.getLogger(ProcessArticles.class.getName());
	
	/*
	 * Read HazelCasr Config File Path.
	 */
	private final static  String hazelCastConfigFilePath = PropertyFileReader.GetPropertyFromConfigFile(fileInputStreamForReadingConfigFile)
			.getProperty("HazelCastConfigFilePath");
	
	/*
	 * Read the path of log4j.properties file.
	 */
	private final static  String log4jPropertiesFilePath = PropertyFileReader.GetPropertyFromConfigFile(fileInputStreamForReadingConfigFile)
			.getProperty("Log4jPropertiesFilePath");
	
	
	public static void main(String[] args) throws Exception{

		/*
		 * Create PropertyConfigurator for logging the output.
		 */
		PropertyConfigurator.configure(log4jPropertiesFilePath);
	
		
		try {
			Config config = new FileSystemXmlConfig(hazelCastConfigFilePath);
			
			/*
			 * Creating a HazelCast Instance. This will create a new cluster if it is the first node,
			 * else it will join the cluster. Kindly refer hazelcast.xml file for multicast and
			 * network discovery settings.
			 */
			final HazelcastInstance hazelCastInstance = Hazelcast.newHazelcastInstance(config);
			
			ExecutorService articleLoaderExecutor = null;
			
			/*
			 * Here we set a lock so that only one can load the articles from the db or in this case
			 * the json dump file. Thus, we will roll out any possibility of processing an article more
			 * than one time.
			 */
			if(!hazelCastInstance.getLock("articleLoader").isLocked())
			{
				hazelCastInstance.getLock("articleLoader").lock();
				
				/*
				 * Create a single async thread that would keep on loading the articles in the 
				 * Blocking Queue. 
				 */
				articleLoaderExecutor = Executors.newSingleThreadExecutor();
				articleLoaderExecutor.submit(new LoadArticles(hazelCastInstance));
			}		
		
			FileInputStream fileInputStreamForReadingConfigFile = null;
			
			/*
			 * Read annotators and numberOfThreads property from properties file.
			 * numberOfThreads: These are the number of threads in the thread pool that
			 * 					would run in parallel processing one article at a time 
			 */
			String annotators = PropertyFileReader.GetPropertyFromConfigFile(
					fileInputStreamForReadingConfigFile).getProperty(
					"Annotators");
			
			String numOfThreads = PropertyFileReader.GetPropertyFromConfigFile(
					fileInputStreamForReadingConfigFile).getProperty(
					"NumberOfThreads");
			
			Properties props = new Properties();
			props.put("annotators", annotators);
			
			/*
			 * Create StanfordCoreNLP pipeline only once on a node.
			 */
			final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			
			/*
			 * By this time, we already have some/all of our articles loaded in the Blocking Queue.
			 * Next we get an instance of that queue, poll articles from it and process them.
			 * This queue is shared by all the nodes in the cluster, thus they can be computed in
			 * a distributed manner.
			 */
			BlockingQueue<Map<String,String>> articleQueue = hazelCastInstance.getQueue("articles");
			
			/*
			 * Create a newFixedThreadPool to run processing of articles in parallel. 
			 * Note: If we're aware of the system memory and number of articles to be 
			 * 		 processed, we may use CachedThreadPool if we have enough memory or
			 * 		 large number of nodes.
			 */
			ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(numOfThreads));
			log.debug("\n\nStarting New Process at : "+new DateTime().toLocalTime().toString()+"\n\n");
			while(articleQueue.size() > 0)
			{
				/*
				 * Poll the queue, and spawn a thread, or wait for an emoty thread to process
				 * the article. 
				 * Note: I am using a callable here instead of nullable in case we need to 
				 * 		 get some processed data of the article back to the main thread, 
				 * 		 we can just gather them in a collection of futures. Though in the 
				 * 		 current scenario, runnables would have worked perfectly fine.
				 */
				Map<String,String> articleDescription = articleQueue.poll();
				Annotation document = new  Annotation(articleDescription.get("description"));
				executor.submit(new ProcessArticles(articleDescription.get("id"), document,pipeline));
				
				/*
				 * Sleep for 100ms so that every node in the cluster get its share of articles to
				 * be processed, thus none of the node is overwhelmed with too many articles and 
				 * others stay idle.
				 */
				Thread.sleep(100); 
			}
			
			executor.shutdown();
			
			/*
			 * If this node has articleLoaderLock, then shutdown articleLoaderExecutor
			 */
			if(articleLoaderExecutor != null)
				articleLoaderExecutor.shutdown();
			
			/*
			 * If executor is working then sleep for 5s and check again, else shutdown the
			 * hazelCastInstance.
			 */
			while(!(executor.isTerminated()))
				Thread.sleep(5000);
			
			hazelCastInstance.shutdown();
			
			
			} catch (FileNotFoundException e) {
				
				 /* 
				  * HazelCast Config file not found
				  */
				 
				e.printStackTrace();
			}
		
	}

}
