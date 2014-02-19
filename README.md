DistributedCoreNlp
==================

This is a parallel and distributed implementation of StanfordCore NLP. It uses HazelCast for distributed computing.
It reads articles from a json file(array of articles in JSON format) and performs StanfordCoreNLP annotations on it,
and gives a log output with number of Named Entities in the article, an array of all the NERs in the article and 
time taken to process that article, in terms of start time and end time. Following is a sample output:

" Article ID : 1, Start Time : 03:46:10.530, End Time : 03:46:25.419, ThreadID : 32, Number of NERs = 2, NER : [2008, Bitcoin Foundation]"

=============================================================================================================================================

What you need to know before running? 

There are three config/properties files in the properties folder. Each file is briefly explained below:

-> config

This file has following properties
  
  > Annotators : Annotators given to be used on data
  
  > NumberOfThreads : Each node processes articles in parallel by running these many threads in concurrent
  
  > ArticleJsonFilePath : Path to the json dump of articles file
  
  > HazelCastConfigFilePath : Path to HazelCast config file
  
  > Log4jPropertiesFilePath : Path to log4j properties file 


-> log4j.properties

This is a normal log4j properties file. Refer https://logging.apache.org/log4j/1.2/manual.html for more info.


-> hazelcast.xml

This is HazelCast configuration file. You can configure network settings in this file: multicast, tcp, ssl etc.
You can also configure discovery on EC2 instances within aws property of network settings.
This includes configurations for all the HazelCast features like Distributed java.util.{Queue, Set, List, Map}, Distributed java.util.concurrency.locks.Lock, Distributed java.util.concurrent.ExecutorService and Distributed MultiMap for one to many mapping.

For more info refer http://www.hazelcast.org/docs/latest/manual/html/


=============================================================================================================================================

Something about HazelCast

HazelCast is an open source in-memory data grid for distributed computing. It enables clustering of machines and 
sharing of resources between the nodes of the cluster. It can also prove to be a good alternative to NoSqls like
Redis and MongoDB. 
HazelCast not only allows sharing of resources but also allows replication(synchronized and asynchronized), so if
one node of the cluster crashes or leaves the cluster for some reason, no data is lost.

=============================================================================================================================================

How to Run?

This is a maven project and so will work like other maven projects. To build run the following command in root directory
  `mvn clean install` 

Note: This may take some time if you're running for the first time as maven will download all the dependencies.
      In case you want to run it in a IDE, you can remove the stanford-corenlp models dependency and if you have the
      download, just place it in the build path. This will help you to skip download of stanford-corenlp jar which
      is about 200M.
  
The above command will create a runnable jar in the target folder, you can run it with the following command:
  `java -jar /path/to/jar` , additionaly you can specify JVM options for memory, as this takes up a lot of memory.
  
Server

The first time you run the project, i.e. if the node you start is the first node of the cluster, it will acquire an ArticleLoad lock and thus will act as a server. Only the server can load articles from the file to NonBlocking queue "articles". This quque is shared within the cluster. 


Client

If a node is not the first node in the cluster, that is it does not create a new cluster, instead joins an already cluster, it will automatically act as a worker/client. This node will not load articles in the queue, it will only poll articles from it and process them, and output the log after an article has been processed.

Note:

In the current version, every node will produce an output log file. In next commit, we can make every node to write the output to a shared concurrent datastructure, and then make Server write it to log files. Thus, there will be only one output log file generated, and that will be on the server.
 
