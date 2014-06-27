package headliner.treedistance;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;



public class DB {

	private Connection connWebex21;
	private boolean isConnWebex21Valid;

	private Connection guanjieConn;
	private boolean isConnGuanjieValid;


	public void connectToWebex21()
	{
		  String url = api.Constants.DB.WEBEX_URL;
		  String driver = api.Constants.DB.DRIVER;
		  String userName = api.Constants.DB.USER;
		  String password = api.Constants.DB.PASSWORD;
		  try {
		  Class.forName(driver);
		  connWebex21 = DriverManager.getConnection(url,userName,password);
		  isConnWebex21Valid = true;
		  //System.out.println("Connected to the database webex21");
		  } catch (Exception e) {
		  e.printStackTrace();
		  }	
	}	
	
	public void connectToGuangie()
	{
		  String url = api.Constants.DB.GUANGIE_URL;
		  String driver = api.Constants.DB.DRIVER;
		  String userName = api.Constants.DB.USER;
		  String password = api.Constants.DB.PASSWORD;
		  
		  try {
		  Class.forName(driver).newInstance();
		  guanjieConn = DriverManager.getConnection(url,userName,password);
		  isConnGuanjieValid = true;
		  //System.out.println("Connected to the database guangie");
		  } catch (Exception e) {
		  e.printStackTrace();
		  }	
	}
	
	public boolean isConnectedToWebex21()
	{
		if (connWebex21 != null) {
			try {
				if (connWebex21.isClosed() == false & isConnWebex21Valid)
					return true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean isConnectedToGuanjie()
	{
		if (guanjieConn != null) {
			try {
				if (guanjieConn.isClosed() == false & isConnGuanjieValid)
					return true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
		
	public void disconnectFromWebex21()
	{
		if (connWebex21 != null)
			try {
				connWebex21.close();
			    //System.out.println("Database webex21 Connection Closed");
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	
	public void disconnectFromGuanjie()
	{
		if (guanjieConn != null)
			try {
				guanjieConn.close();
			    //System.out.println("Database guangie Connection Closed");
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	public List<String> getContentsRdfs() {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> rdfs = new ArrayList<String>();
		try
		{
			sqlCommand = "SELECT content_name FROM guanjie.ent_content";
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				rdfs.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}	
		return rdfs;	
	}

	public void insertContentTree(String c, String tree) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		try
		{
			sqlCommand = "insert into ent_content_tree (content_name,tree) values ('"+c+"','"+tree+"')";
			ps = guanjieConn.prepareStatement(sqlCommand);
			ps.executeUpdate();		
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}				
	}
	
	public String[] getExamplesName() {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		String[] list = null;
		int count = -1;
		sqlCommand = "SELECT count(distinct content_name) FROM ent_content where content_type = 'example' and domain = 'java'";
		try {
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				count = rs.getInt(1);
			list = new String[count];
			rs.close();
			ps.close();
			sqlCommand = "SELECT content_name FROM ent_content where content_type = 'example' and domain = 'java'";
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			int i = 0;
			while (rs.next())
			{
				list[i] = rs.getString(1);
				i++;
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
		return list;
	}

	public List<String> getAdjacentConcept(String content, String concept) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		String title = "";
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		title = content;
		
		if (isConnectedToWebex21())
		{
			try
			{
				if (isExample)
				{
					conceptTable = "ent_jexample_concept";
					sqlCommand = "select name from ent_dissection where rdfID ='"+content+"'";
					ps = connWebex21.prepareStatement(sqlCommand);
					rs = ps.executeQuery();
					while (rs.next())
						title = rs.getString(1);
					rs.close();
					ps.close();
				}
				
				int s=-1,e=-1 ;
				sqlCommand = "select sline,eline from "+conceptTable+" where title ='"+title+"' and concept = '"+concept+"'";
				ps = connWebex21.prepareStatement(sqlCommand);
				rs = ps.executeQuery();
				rs.close();
				ps.close();
				ResultSet rs2;
				while (rs.next())
				{
					s = rs.getInt(1);
					e = rs.getInt(2);
					sqlCommand = "select concept from "+conceptTable+" where title ='"+title+"' and sline >= "+s+" and eline <= "+e +" and concept != '"+concept+"'";
					ps = connWebex21.prepareStatement(sqlCommand);
					rs2 = ps.executeQuery();
					while (rs2.next())
					{
						if (conceptList.contains(rs2.getString(1)) == false)
							conceptList.add(rs2.getString(1));
					}
					rs.close();
					ps.close();
				}
				
			}catch (SQLException e) {
				 e.printStackTrace();
			}
		}			
		return conceptList;	
	}

	public List<String> getConcepts(String content) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		if (isExample)
			conceptTable = "ent_jexample_concept";					
		if (isConnectedToWebex21())
		{
			try
			{				
				sqlCommand = "select distinct concept from "+conceptTable+" where title ='"+content+"'";
				ps = connWebex21.prepareStatement(sqlCommand);
				rs = ps.executeQuery();
				while (rs.next())
					conceptList.add(rs.getString(1));
				rs.close();
				ps.close();
			}catch (SQLException e) {
				 e.printStackTrace();
			}
		}			
		return conceptList;	
	}

	public ArrayList<String> getQuestionNames() {

		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		ArrayList<String> list = null;
		int count = -1;
//		sqlCommand = "SELECT count(distinct content_name) FROM ent_content where content_type = 'question' and domain = 'java' order by content_name";
		try {
//			ps = guanjieConn.prepareStatement(sqlCommand);
//			rs = ps.executeQuery();
//			while (rs.next())
//				count = rs.getInt(1);
			list = new ArrayList<String>();
			//TODO you should change the limit
			sqlCommand = "SELECT content_name FROM ent_content where content_type = 'question' and domain = 'java' order by content_name";
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				list.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			try {
				ps.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		return list;
	
	}
	
	public String getTree(String c) {
        PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		String tree = "";
		
		try {
			sqlCommand = "SELECT tree  FROM ent_content_tree where content_name = '"+c+"'";
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
				 tree = rs.getString(1);
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
			
		return tree;
	}

	public void insertContentSim(String question, String example, double sim,String method) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		try
		{
			String example_topic_id = "";
			sqlCommand = "select topic_id from rel_topic_content where content_name = '"+example+"'";
			ps = guanjieConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				example_topic_id = rs.getString(1);
			}

			sqlCommand = "insert into rel_con_con_sim (question_content_name,example_content_name,example_topic_id,sim,method) values ('"+question+"','"+example+"','"+example_topic_id+"',"+sim+",'"+method+"')";
			ps = guanjieConn.prepareStatement(sqlCommand);
			ps.executeUpdate();
			rs.close();
			ps.close();
			
		}catch (SQLException e) {
			 e.printStackTrace();
		}			
	}

	public List<String> getOutcomeConcept(String content) {

		PreparedStatement ps = null;
		String sqlCommand = "";
		String topic = "";
		List<String> topicOutcomeConceptList = new ArrayList<String>();		
		List<String> contentOutcomeConceptList = new ArrayList<String>();	
		List<String> temp ;
		try
		{
				sqlCommand = "SELECT topic_name FROM guanjie.rel_topic_content where content_name = '"+content+"'";
				ps = guanjieConn.prepareStatement(sqlCommand);
				ResultSet rs = ps.executeQuery();
				while(rs.next())
				{
					topic = rs.getString(1);
				}
				rs.close();
				ps.close();
				sqlCommand = " SELECT distinct concept_name" +
						     " FROM guanjie.rel_topic_concept_agg"+
						     " where topic_name = '"+topic+"' and direction = 'outcome'";						
				ps = guanjieConn.prepareStatement(sqlCommand);
				rs = ps.executeQuery();
				rs.close();
				ps.close();
				while(rs.next())
				{
					topicOutcomeConceptList.add(rs.getString(1));
				}
				temp = 	getConcepts(content);
				for (String t : temp)
				{
					if (topicOutcomeConceptList.contains(t))
						contentOutcomeConceptList.add(t);
				}
			
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		return contentOutcomeConceptList ;
	}

	public List<String> getAllConcepts(String content) {
		PreparedStatement ps = null;
		String sqlCommand = "";
		ResultSet rs = null;
		List<String> conceptList = new ArrayList<String>();
		boolean isExample = false;
		sqlCommand = "select content_type from ent_content where content_name ='"+content+"'";
		try {
			ps = guanjieConn.prepareStatement(sqlCommand);
			rs = ps.executeQuery();
			while (rs.next())
			{
				 if (rs.getString(1).equals("example"))
					 isExample = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String conceptTable = "ent_jquiz_concept";
		if (isExample)
			conceptTable = "ent_jexample_concept";					
		if (isConnectedToWebex21())
		{
			try
			{				
				sqlCommand = "select concept from "+conceptTable+" where title ='"+content+"'";
				ps = connWebex21.prepareStatement(sqlCommand);
				rs = ps.executeQuery();
				while (rs.next())
					conceptList.add(rs.getString(1));
				rs.close();
				ps.close();
			}catch (SQLException e) {
				 e.printStackTrace();
			}
		}			
		return conceptList;	
	

	}

	public float getTopicVal(String content, int i) {
		float val = 0.0f;
		try
		{				
			String sqlCommand = "select value from rel_doc_topic_LDA where content ='"+content+"'";
			PreparedStatement ps = guanjieConn.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				val = (float)rs.getDouble(1);
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
		return val;
	}

	public void insertContentTopicVal(String content, String topic, String value) {
		try
		{				
			String sqlCommand = "insert into rel_doc_topic_LDA(content,topic,value) values (?,?,?)";
			PreparedStatement ps = guanjieConn.prepareStatement(sqlCommand);
			ps.setString(1, content);
			ps.setString(2, topic);
			double val = Double.parseDouble(value);
			val = Math.round(val*100000.0)/100000.0;
			ps.setDouble(3,val);
			ps.executeUpdate();	
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
	}

	public List<String> getContentConcepts() {
		List<String> list = new ArrayList<String>();

		try
		{				
			String sqlCommand = "(SELECT distinct concept FROM ent_jexample_concept) union (SELECT distinct concept FROM ent_jquiz_concept) ";
			PreparedStatement ps = connWebex21.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				list.add(rs.getString(1));
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
	
		return list;
	}

		public double getTFIDF(String q, String c) {
		double val = 0.0;

		try
		{				
			String sqlCommand = "SELECT `log(tf+1)idf` FROM webex21.temp2_ent_jcontent_tfidf where title = '"+q+"' and concept = '"+c+"';";
			PreparedStatement ps = connWebex21.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				val = rs.getDouble(1);
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}	
		return val;
	}

	public double getWeightInSubtree(String subtree, String content, DB db) {

		String[] edges = subtree.split(";");
		ArrayList<String> subtreeConcepts = new ArrayList<String>();
		String[] temp;
		for (String edge : edges)
		{
			temp = edge.split("-");
			if (temp[0].equals("JAVA"))
				subtreeConcepts.add(temp[1]);
			else 
			{
				subtreeConcepts.add(temp[0]);	
				subtreeConcepts.add(temp[1]);	
			}
		}
		String concepts = ""; 
		for (int i = 0; i < subtreeConcepts.size(); i++)
		{
			concepts += "'"+subtreeConcepts.get(i)+"'";
			if (i < subtreeConcepts.size() - 1)
				concepts += ",";
		}
		double weight = 0.0;
		try
		{				
			String sqlCommand = "SELECT `log(tf+1)idf` FROM webex21.temp2_ent_jcontent_tfidf where title = '"+content+"' and concept in ("+concepts+");";
			PreparedStatement ps = connWebex21.prepareStatement(sqlCommand);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				weight += rs.getDouble(1);
			rs.close();
			ps.close();
		}catch (SQLException e) {
			 e.printStackTrace();
		}
			
		return weight;
	}

	public void connect() {
		connectToGuangie();
		connectToWebex21();
		
	}
	public void disconnect() {
		disconnectFromGuanjie();
		disconnectFromWebex21();
	}
}
