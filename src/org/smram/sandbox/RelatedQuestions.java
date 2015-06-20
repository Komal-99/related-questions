package org.smram.sandbox;

import java.util.ArrayList;

/**
 * TODO This impl does not exploit the fact that the graph is a tree
 * @author smram
 *
 */
public class RelatedQuestions {
  //	cost=time in this RelatedQuestions problem, so a legitimate value is 
	// always positive
	final static double INVALID_COST = -1.0d; 
	
	static class Vertex {
		
		final int id;
		final int v;
		boolean visited;
		final ArrayList<Vertex> adj = new ArrayList<Vertex>();
		
		Vertex(int id, int v) {
			this.id = id; 
			this.v = v;
			this.visited = false;
		}
		
		int getNumNeighbors() {
			return adj.size();
		}
		
		static void addUndirectedEdge(Vertex v1,TestVertex v2) {
			v1.adj.add(v2);
			v2.adj.add(v1);
		}
	}

	static class TestVertex extends Vertex {
		double trueMinExpCost;
		
		private TestVertex(int id, int v) {
			super(id, v);
		}
		
		public void setTrueMinCost(double trueMinExpCost) {
			this.trueMinExpCost = trueMinExpCost;
		}
		
		private static double printTrueMinCost(ArrayList<TestVertex> vList) {
			double min = Double.MAX_VALUE;
			for (TestVertex tv : vList) {
				System.out.println("Vertex id=" + tv.id + ", trueMinCost=" + tv.trueMinExpCost);
				if (tv.trueMinExpCost < min)
					min = tv.trueMinExpCost;
			}
			System.out.println("Min(TrueMinCost)=" + min);
			return min;
		}
		
	}
	
	static boolean isInvalidCost(double c) {
		return (c < 0); // check < to avoid checking == on a double	
	}
	
	/**
	 * @param root
	 * @return expected cost of visiting all nodes in tree starting at root. 
	 * expected cost = cost of visiting a neighbor * P(visiting neighbor)
	 * P(visiting neighbor) = 1/#neighbors
	 */
	static double dfs(Vertex root, final double minExpCost) {
		if (root.getNumNeighbors() == 0) {
			return (root.v > minExpCost) ? INVALID_COST : root.v;
		}
		
		double neighborExpCost = 0d;
		root.visited = true;
		int numVisitedChild = 0;
		for (Vertex child : root.adj) { // for every unvisited child
			if (!child.visited) {
				double childExpCost = dfs(child, minExpCost);
				
				// actually this is wrong... the child cost may be > minCost... but we will weight it DOWN.. so rootCost may be < min and you've incorerectly pruned root
				if (isInvalidCost(childExpCost)) { // early stop at this root, return.
					return INVALID_COST;
				}
				neighborExpCost += childExpCost;
				numVisitedChild++;
			}
		}
		
		if (numVisitedChild > 0)
			neighborExpCost /= (double) numVisitedChild;
		
		return root.v + neighborExpCost;
	}
	
	static void computeMinExpCost(ArrayList<Vertex> vList, boolean doPrune) {
		Vertex minVertex = null;
		double minExpCost = Double.MAX_VALUE;
		
		long startTime = System.currentTimeMillis();
		for(Vertex vertex : vList) {

			for (Vertex allv : vList) // mark whole graph as !visited
				allv.visited = false;
			
			double expCost = dfs(vertex, doPrune ? minExpCost : Double.MAX_VALUE);
			
			if (isInvalidCost(expCost))
				System.out.println("Early stop for vertex=" + vertex.id);
			else if (expCost < minExpCost) {
				 minExpCost = expCost;
				 minVertex = vertex;
			}
		}
		long endTime = System.currentTimeMillis() - startTime;
		
		if (minVertex != null)
			System.out.println("MinExpCost = " + minExpCost + 
					" from vertex = " + minVertex.id + ", time = " + endTime);
		else 
			System.out.println("Empty graph");
	}

	private static void runTest_2()
	{
		// test: simple graphs
		TestVertex v1 = new TestVertex(1, 30);
		TestVertex v2 = new TestVertex(2, 20);
		TestVertex v3 = new TestVertex(3, 10);
		TestVertex v4 = new TestVertex(4, 40);
		TestVertex v5 = new TestVertex(5, 50);
		
		// construct undirected edges: {1,2}; {1,3} {1,4} {3,5}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		Vertex.addUndirectedEdge(v1, v4);
		Vertex.addUndirectedEdge(v3, v5);
		// construct vertex list
		ArrayList<Vertex> vList = new ArrayList<Vertex>();
		vList.add(v1);
		vList.add(v2);
		vList.add(v3);
		vList.add(v4);
		vList.add(v5);
		
		// test code
		v1.setTrueMinCost(v1.v + (v2.v + (v3.v + v5.v) + v4.v)/3.0);
		v2.setTrueMinCost(v2.v + (v1.v + ((v3.v + v5.v) + v4.v)/2.0));
		v3.setTrueMinCost(v3.v + (v5.v + (v1.v + (v2.v + v4.v)/2.0))/2.0);
		v4.setTrueMinCost(v4.v + (v1.v + (v2.v + (v3.v + v5.v))/2.0)/1.0);
		v5.setTrueMinCost(v5.v + (v3.v + (v1.v + (v2.v + v4.v))/2.0)/1.0);		
		ArrayList<TestVertex> testVertexList = new ArrayList<TestVertex>();
		for (Vertex v : vList)
			testVertexList.add((TestVertex) v); // yuck - cast, but we know what we're doing
		TestVertex.printTrueMinCost(testVertexList);

		computeMinExpCost(vList, false);
		System.out.println("Test with prune=true");
		computeMinExpCost(vList, true);

	}

	private static void runTest_1()
	{
		// test: simple graphs
		TestVertex v1 = new TestVertex(1, 30);
		TestVertex v2 = new TestVertex(2, 20);
		TestVertex v3 = new TestVertex(3, 10);
		
		// construct undirected edges {1,2} {1,3}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		
		// construct vertex list
		ArrayList<Vertex> vList = new ArrayList<Vertex>();
		vList.add(v1);
		vList.add(v2);
		vList.add(v3);

		// reference answer
		v1.setTrueMinCost(v1.v + (v2.v + v3.v)/2.0);
		v2.setTrueMinCost(v2.v + (v1.v/1.0 + (v3.v/1.0)));
		v3.setTrueMinCost(v3.v + (v1.v/1.0 + (v2.v/1.0)));
		ArrayList<TestVertex> testVertexList = new ArrayList<TestVertex>();
		for (Vertex v : vList)
			testVertexList.add((TestVertex) v); // yuck - cast, but we know what we're doing
		TestVertex.printTrueMinCost(testVertexList);
		
		computeMinExpCost(vList, false);
		System.out.println("Test with prune=true");
		computeMinExpCost(vList, true);
	}

	public static void main(String[] args) {
		// test: empty graph
		computeMinExpCost(new ArrayList<Vertex>(), false);
		computeMinExpCost(new ArrayList<Vertex>(), true);
		
		runTest_1();
		//runTest_2();
	}
}