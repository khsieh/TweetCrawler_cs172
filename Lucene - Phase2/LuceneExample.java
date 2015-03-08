import java.io.File;
import java.io.Reader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


class WebDocument {
	public String title;
	public String body;
	public String url;	
	public WebDocument(String t, String b, String u) {
		this.title = t;
		this.body = b;
		this.url = u;
	}
}


public class LuceneExample {
	public static final int MAX_FILE_COUNT = 1;
	public static final String INDEX_DIR = "Index";
	public static void main(String[] args) throws CorruptIndexException, IOException {
		//get input from file and make page entries
		//index all pages
		//search per user query request
		
		 // WebDocument page = new WebDocument("This is test Title 1", "body of the test web page", "http://www.dummy.edu");
		 // index(page);		
		 // search("test", 5);
	}
	
	public static void getFile (String filename){
		for(int i = 0; i < MAX_FILE_COUNT; i++){
			Reader reader = new FileReader("D:\\Java Workspace\\LuceneExample\\file" + i + "\.txt");
			
		}
	}

	public static void index (WebDocument page) {
		File index = new File(INDEX_DIR);	
		IndexWriter writer = null;
		try {	
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_34, new StandardAnalyzer(Version.LUCENE_34));
			writer = new IndexWriter(FSDirectory.open(index), indexConfig);
			System.out.println("Indexing to directory '" + index + "'...");	
			Document luceneDoc = new Document();
			//get fields from page and insert into document
			luceneDoc.add(new Field("text", page.body, Field.Store.YES, Field.Index.ANALYZED));
			luceneDoc.add(new Field("url", page.url, Field.Store.YES, Field.Index.NO));
			luceneDoc.add(new Field("title", page.title, Field.Store.YES, Field.Index.ANALYZED));
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
	
	public static TopDocs search (String queryString, int topk) throws CorruptIndexException, IOException {
		
		IndexReader indexReader = IndexReader.open(FSDirectory.open(new File(INDEX_DIR)), true);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		QueryParser queryparser = new QueryParser(Version.LUCENE_34, "text", new StandardAnalyzer(Version.LUCENE_34));

		try {
			StringTokenizer strtok = new StringTokenizer(queryString, " ~`!@#$%^&*()_-+={[}]|:;'<>,./?\"\'\\/\n\t\b\f\r");
			String querytoparse = "";
			while(strtok.hasMoreElements()) {
				String token = strtok.nextToken();
				querytoparse += "text:" + token + "^1" + "title:" + token+ "^1.5";
				//querytoparse += "text:" + token;
			}		
			Query query = queryparser.parse(querytoparse);
			// System.out.println(query.toString());
			TopDocs results = indexSearcher.search(query, topk);
			// System.out.println(results.scoreDocs.length);	
			// System.out.println(indexSearcher.doc(results.scoreDocs[0].doc).getFieldable("text").stringValue());
			
			return results;			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			indexSearcher.close();
		}
		return null;
	}
	
}