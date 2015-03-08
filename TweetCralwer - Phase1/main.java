import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
//import twitter4j.TwitterResponse;
import twitter4j.TwitterStreamFactory;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
//import twitter4j.TweetEntity;
//import twitter4j.TwitterObjectFactory;
//import twitter4j.Query;
import twitter4j.GeoLocation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;




import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;



//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Globals{
	public static long entryCount = 0;
	public static long tweetCount = 0;
	public static int  fileCount = 1;
	public static final long MB10 = 4400;
	public static Semaphore inputToFile = new Semaphore(1,true);
}

//make a thread class to turn a tweet into a JSON to input into the file
class convertToJson implements Runnable {
	Thread t;
	final HashSet <JSONObject> hs;
	final String outputDir;
//	Iterator<JSONObject> startIndex;
//	Iterator<JSONObject> endIndex;
//	long iteration;
	public convertToJson(HashSet <JSONObject> hs_in, String o_in){//add iterator start and end
		hs = hs_in;
//		startIndex = start;
//		endIndex = end;
//		iteration = i;
		outputDir = o_in;
	}
	
	@Override
	public void run() {
		
		//write JSON into file
        FileWriter file = null;
        
        try {// loop through HashSet to get all JSONObj from startIT to endIT
        	Iterator<JSONObject> jItr = hs.iterator();
        	while( jItr.hasNext() ){
        		

        		if(Globals.entryCount == 0){
        			
        			//write start
        	        String filePath = outputDir + "/file" + Integer.toString(Globals.fileCount) + ".txt";
        			try {
        				file = new FileWriter(filePath,true);
        			} catch (IOException e1) {
        				// TODO Auto-generated catch block
        				e1.printStackTrace();
        			}
        	        
        		}
        		
        		JSONObject obj = jItr.next();
        		file.append(obj.toJSONString());
        		file.append(System.getProperty("line.separator"));
        		//System.out.println("Successfully Copied JSON Object to File...");
        		//System.out.println("\nJSON Object: " + obj);
                Globals.entryCount++;
        		if(Globals.entryCount >= Globals.MB10){
        			//change filename in outdir
        			Globals.fileCount++;
        			Globals.entryCount = 0;
        			try {
    					file.flush();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    	            try {
    					file.close();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
        		}
        	}//end while
            
 
        } catch (IOException e) {
            e.printStackTrace();
        }//write end
        
	}
	
	
}

//make a thread class to turn a tweet into a JSON to input into the file
class fillHashSet implements Runnable {
	Thread t;
	Status status;
	final HashSet<JSONObject> hset;
	public fillHashSet(Status s_in, HashSet<JSONObject> hs){
		status = s_in;
		hset = hs;
	}
	@Override
	public void run() {
		
		//get required fields from status into an JSON obj
		String username = status.getUser().getScreenName();
		GeoLocation tweetLocation = status.getGeoLocation();
		String content = status.getText();
		Date timeStamp = status.getCreatedAt();
		URLEntity[] urls = status.getURLEntities();
		
		JSONObject obj = new JSONObject();
		obj.put("User","@" + username);
		obj.put("Time",timeStamp);
		obj.put("Geo",tweetLocation);
		obj.put("Tweet",content);
      
    	getHTML a = new getHTML(obj,hset,urls);
    	Thread gHTML = new Thread(a);
    	gHTML.run();
	}//end run
	
}

class getHTML implements Runnable {
	JSONObject obj;
	final HashSet<JSONObject> hset;
	URLEntity[] urls;
	public getHTML(JSONObject s_in, HashSet<JSONObject> hs,URLEntity[] url){
		obj = s_in;
		hset = hs;
		urls = url;
	}
	@Override
	public void run() {
		
		String title = null;
		String urlStr = null;
		if(urls.length > 0 ){
			for(URLEntity url : urls){
			urlStr = url.getExpandedURL();
			if(urlStr != null){
				//use jsoup to parse html for title if it exists
				Document link;
				try{
					link = Jsoup.connect(urlStr).get();		
					title =link.title();
				}catch(IOException e){
					e.printStackTrace();
				}//end jsoup
			}
			obj.put("LinkTitle", title);
			obj.put("LinkUrl", urlStr);
			}
		}
		else{
			obj.put("LinkTitle", title);
			obj.put("LinkUrl", urlStr);
		}
	
		//add JSON obj into HashSet 
		hset.add(obj);
		Globals.tweetCount++;
    	System.out.println(Globals.tweetCount);
	}//end run
	
}

public class main {
	static long maxTweets;
	static double[][] boundingBox= { {-124.848974, 24.396308}, {-66.885444, 49.384358} };
	static String[] language = {"en"};
    public static void main(String[] args) {
    	
    	//exec query #ofTweets fileDes
    	if(args.length != 3){
    		System.out.println("Error Usage: TweetCrawler <number of tweets> <query1,query2,...> <output dir>\n");
    		System.exit(0);
    	}
    	
    	//pares user parameters
    	 maxTweets = Long.valueOf(args[0]);
    	String allQuery = args[1];
    	final String outputDir = args[2];
    	
    	//parse allQuery into String array
    	String[] Queries = allQuery.split(",");
    	
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);
        cb.setOAuthConsumerKey("TVdH1UYy5m1sGobDo8YmIr6UF");
        cb.setOAuthConsumerSecret("P77L5wVoTmUQwUpwmx9YDCx3uWA8MsQDu1xgaSuU3zoePhQc3u");
        cb.setOAuthAccessToken("2377857834-6c0mvuC8l2mAqtvN7UfLazDRnnyUiN09XuwarI8");
        cb.setOAuthAccessTokenSecret("hzOnNtiaqDH3BWb3R2T4mJRuRVXFXprbA8NavdBpXKBsS");
        cb.setJSONStoreEnabled(true);

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        final HashSet <JSONObject> jsonHash = new HashSet();
        final Object lock = new Object();
        
        StatusListener listener = new StatusListener() {

            @Override
            public void onException(Exception arg0) {
                // TODO Auto-generated method stub
            	

            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScrubGeo(long arg0, long arg1) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStatus(Status status) {
            	
            	
            	fillHashSet fillHS = new fillHashSet(status,jsonHash);
            	Thread tHash = new Thread(fillHS);
            	tHash.run();
            	
            	if(jsonHash.size() >= maxTweets){
            		synchronized(lock){
            			lock.notify();
            		}
            	}
            	
            	
                

            }

            @Override
            public void onTrackLimitationNotice(int arg0) {
                // TODO Auto-generated method stub

            }

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				
			}
			
        };
        
        FilterQuery fq = new FilterQuery();
    
//        String keywords[] = {"food"};
        fq.locations(boundingBox);
        
        twitterStream.addListener(listener);
        twitterStream.filter(fq);
        
        try {
        	synchronized (lock) {
        		lock.wait();
        	}
        } catch (InterruptedException e) {
        	e.printStackTrace();
        }
        
        twitterStream.shutdown();
        
        convertToJson converter = new convertToJson(jsonHash,outputDir);
        Thread t = new Thread(converter);
        
        t.run();

    }
}