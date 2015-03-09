import java.io.BufferedReader;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.Reader;
import java.io.IOException;
//import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
//import org.apache.lucene.index.TermFreqVector;
//import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


class WebDocument {
	public String user;
	public String tweet;
	public String time;
	public String url;
	public String title;
	public String geo;
	
	public WebDocument(String usr, String twt, String time, String u, String title, String g) {
		this.user=usr;
		this.tweet=twt;
		this.time=time;
		this.url = u;
		this.title = title;
		this.geo = g;
	}
}

public class LuceneSearch{
	public static final int MAX_FILE_COUNT = 5;
	public static final String INDEX_DIR = "Index";
	public static void main(String[] args) throws CorruptIndexException, IOException {
		//get input from file and make page entries
		//index all pages
		//search per user query request
		getFile(); // read from file and index it
	}
	
	public static void getFile () throws FileNotFoundException{
		for(int i = 1; i <= MAX_FILE_COUNT; i++){
			try{
				System.out.println(i);
				//C:\Users\Kevin\Documents\SCHOOL SHIT\CS172\CS172 dummy\dummy data
				File file = new File("C:\\Users\\Kevin\\Documents\\SCHOOL SHIT\\CS172\\CS172 dummy\\dummy data\\file" + i + ".txt");
				FileReader fileReader = new FileReader(file);
				BufferedReader buff = new BufferedReader(fileReader);
				String line ="";
				while ((line = buff.readLine() ) != null){

					String User = line.substring(line.indexOf("\"User\"")+8, line.indexOf("\"Time\"")-2);
					String Time = line.substring(line.indexOf("\"Time\"")+7, line.indexOf("\"Tweet\"")-1);
					String Tweet = line.substring(line.indexOf("\"Tweet\"")+9, line.indexOf("\"LinkUrl\"")-2);
					String LinkUrl = line.substring(line.indexOf("\"LinkUrl\"")+10, line.indexOf("\"LinkTitle\"")-1);
					String LinkTitle = line.substring(line.indexOf("\"LinkTitle\"")+12, line.indexOf("\"Geo\"")-1);
					String Geo = line.substring(line.indexOf("\"Geo\"")+6, line.length()-1);

					WebDocument page = new WebDocument(User,Tweet,Time,LinkUrl,LinkTitle,Geo);
					index(page);
				}

				fileReader.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void index (WebDocument page) {
		File index = new File(INDEX_DIR);	
		IndexWriter writer = null;
		try {	
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_34, new StandardAnalyzer(Version.LUCENE_34));
			writer = new IndexWriter(FSDirectory.open(index), indexConfig);
			Document luceneDoc = new Document();
			//get fields from page and insert into document
			luceneDoc.add(new Field("user", page.user, Field.Store.YES, Field.Index.NOT_ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("tweet", page.tweet, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("time", page.time, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("url", page.url, Field.Store.YES, Field.Index.NOT_ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("title", page.title, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("geo", page.geo, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			//write into lucene
			writer.addDocument(luceneDoc);			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (writer !=null)
				try {
					writer.close();
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static void search (String queryString, int topk) throws CorruptIndexException, IOException {
		
		IndexReader indexReader = IndexReader.open(FSDirectory.open(new File(INDEX_DIR)), true);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		QueryParser queryparser = new QueryParser(Version.LUCENE_34, "tweet", new StandardAnalyzer(Version.LUCENE_34));

		try {
			StringTokenizer strtok = new StringTokenizer(queryString, " ~`!#$%^&*()_-+={[}]|;'<>,./?\"\'\\/\n\t\b\f\r");
			String querytoparse = "";
			while(strtok.hasMoreElements()) {
				String token = strtok.nextToken();
				querytoparse += "tweet:" + token + "^1" + "user:" + token+ "^1.5";
				querytoparse += token + "\n";
			}		
			Query query = queryparser.parse(querytoparse); 
		    System.out.println(query.toString());
			
		    TopDocs results = indexSearcher.search(query, topk);
		    
			System.out.println("Total Hits: " + results.totalHits);
			System.out.println("Max Score: " + results.getMaxScore() + "\n");
			
			ScoreDoc[] hits = results.scoreDocs;
			for(int h = 0; h < topk; ++h){
				
				System.out.println(hits[h].toString()); //doc id and score of each hit
				//get tweet content
				Document d = indexSearcher.doc(hits[h].doc);
				System.out.println("User: " + d.get("user") + "\tTweet: " + d.get("tweet")
						 + "\tTime: " + d.get("time"));
			}
			return;			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			indexSearcher.close();
			indexReader.close();
		}
		return ;
	}
	
}