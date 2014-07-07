package headliner.treedistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SIM_V2 {
	
	public static void main (String[] args)
	{
		DB db  = new DB();
		db.connect();
		if (db.isConnectedToLabstudy())
        {
			//storeLDAOutputToDB(db);			
			String[] eList = db.getExamplesName();
			List<String> list = db.getQuestionNames();
			String[] qList = list.toArray(new String[list.size()]);
//			String[] qList = {"j2D_Arrays1"};			
//			String[] eList = {"create_2d_array_v2"};
			//String[] eList = {"poly_v2","inheritance_casting_1","inheritance_polymorphism_1",
			//		"inheritance_polymorphism_2","inheritance_constructors_1","simple_inheritance_1"};
			for (String q : qList)
			{
				for (String e : eList) {
					calStructuralContentSim(db, q, e);
					//calConceptSimilarity(db,q,e);
					//calLDASimilarity(db,q,e);
				}
			}
			db.disconnect();
		}

	}

	private static void calConceptSimilarity(DB db, String q, String e) {
		List<String> qConcepts = db.getConcepts(q);
		List<String> eConcepts = db.getConcepts(e);
		List<String> conceptSpaceList = db.getContentConcepts();
		int count = 0;
		double[] qvec = new double[conceptSpaceList.size()];		
		double[] evec = new double[conceptSpaceList.size()];
		for (String c : conceptSpaceList)
		{
			qvec[count] = 0;
			evec[count] = 0;
			if (qConcepts.contains(c))
			{
				qvec[count] = db.getTFIDF(q,c);				
			}				
			if (eConcepts.contains(c))
			{
				evec[count] = db.getTFIDF(e,c);				
			}
			count++;			
		}
		double denom1 = 0.0;
		double denom2 = 0.0;
		double numerator = 0.0;
		for (int i = 0; i < conceptSpaceList.size(); i++)
		{
			numerator += qvec[i]*evec[i];
			denom1 += Math.pow(qvec[i],2);
			denom2 += Math.pow(evec[i],2);
		}
		double denom = Math.sqrt(denom1)*Math.sqrt(denom2);
		if (denom == 0.0)
			denom += Double.MIN_VALUE;
		double sim = numerator / denom;
		db.insertContentSim(q, e,sim,-1,"CONCSIM");
//		System.out.println("q:"+q+" e:"+e+" sim:"+sim);
	}

	private static void calStructuralContentSim(DB db, String q, String e) {
		String[] qsubList;
		String[] esubList;
		String qtree;
		String etree;
		double temp;
		double mincost;
		Map<String, Double> q2edistMap;
		double sim;
		double dist = 0.0;
		q2edistMap = new HashMap<String,Double>();
		qtree = db.getTree(q);
		etree = db.getTree(e);
		qsubList = qtree.split("@");
		esubList = etree.split("@");
		
		double sizeQ = db.getAllConcepts(q).size();
		double sizeE = db.getAllConcepts(e).size();
		
		double size_es;
		double size_qs;
		
		HashMap<String,Double> q_vector = db.getConceptsVector(q); //keys are concept and values are tf-idf value for concepts
		
        List<String> es_concepts;
		//find distance of q to e
		for (String qs : qsubList)
		{
			size_qs = getConceptsInSubtree(qs).size();
			mincost = Double.POSITIVE_INFINITY;
			for (String es : esubList)
			{	
				temp  = calculateDist(es, qs, e, q);
				size_es = getConceptsInSubtree(es).size();
				//calculate size ratio
				temp = temp * Math.pow((size_qs/sizeQ)*(size_es/sizeE), -1);
				//calculate cos(qs,es)
				es_concepts = getConceptsInSubtree(es);
				HashMap<String,Double> es_vector = new HashMap<String,Double>();
				for (String con : q_vector.keySet())
				{
					if (es_concepts.contains(con))
						es_vector.put(con, q_vector.get(con));
					else
						es_vector.put(con,0.0);
				}
				double numerator = 0.0;
				double demoninator_es = 0.0;
				double denominator_q = 0.0;
				double cosine = 0.0;
				for (String con :  q_vector.keySet())
				{
					numerator += q_vector.get(con) * es_vector.get(con);
					demoninator_es += Math.pow(es_vector.get(con), 2);
					denominator_q += Math.pow(q_vector.get(con), 2);
				}
				if ((Math.sqrt(denominator_q)*Math.sqrt(demoninator_es)) == 0.0)
				{
					cosine = 0; //cosine =0 so temp *1/0 = temp*positive infitiy as follows
					temp = Double.POSITIVE_INFINITY;
				}
				else
				{
					cosine = numerator/(Math.sqrt(denominator_q)*Math.sqrt(demoninator_es));
					temp *= Math.pow(cosine,-1);					
				}
				
				if (temp < mincost)
				{
					mincost = temp;
				}
			}
			q2edistMap.put(qs, mincost);
//			System.out.println("qs: "+qs);
//			System.out.println("es: "+tmp);
//			System.out.println("mincost: "+mincost);
//			System.out.println("******************");
		}

		for (Map.Entry<String, Double> entry : q2edistMap.entrySet())
		{
			dist += entry.getValue();
		}
		if (dist == Double.POSITIVE_INFINITY)
			dist = Double.MAX_VALUE;//just for storing in db
		sim = 1.0/(Math.exp(0.01*dist));
		db.insertContentSim(q, e,sim,dist,"CSSIM");
	}



	private static List<String> getConceptsInSubtree(String subtree) {
		String[] edges = subtree.split(";");
		List<String> subtreeConcepts = new ArrayList<String>();
		String[] temp;
		for (String edge : edges)
		{
			temp = edge.split("-");
			if (temp[0].equals("ROOT"))
				subtreeConcepts.add(temp[1]);
			else 
			{
				subtreeConcepts.add(temp[0]);	
				subtreeConcepts.add(temp[1]);	
			}
		}
		return subtreeConcepts;
	}

	private static double calculateDist(String es, String qs,String e, String q) {
		try
		{
			TreeDefinition eTree = CreateTreeHelper.makeTree(es);
			TreeDefinition qTree = CreateTreeHelper.makeTree(qs);
			ComparisonZhangShasha treeCorrector = new ComparisonZhangShasha();
			OpsZhangShasha costs = new OpsZhangShasha();
			Transformation transform = treeCorrector.findDistance(eTree, qTree, costs);
			double dist = transform.getCost();		
			return dist;
		}catch(Exception ex)
		{
			System.out.println(e+" "+es);
			System.out.println("");
			System.out.println(q+" "+qs);
		}
		return 0;
	}
}
