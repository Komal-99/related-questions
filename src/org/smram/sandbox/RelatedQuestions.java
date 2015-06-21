package org.smram.sandbox;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Problem specification: {@link http://www.quora.com/challenges#related_questions}
 * 
 * Caveats:
 * - This implementation doesn't check for cycles. Problem statement assumes tree
 * 
 * Optimizations for R3 run time:
 * - can #iterations be reduced based on depth of tree?
 * - change graph storage (edge based?), reduce small object overhead, 
 *   reduce HashMap lookups ...
 *  
 * Optimizations for depth-bounded DFS: 
 * - BFS? bounded BFS? to avoid stack overflow for long graphs?
 * 
 * @author smram
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
		
		static void addUndirectedEdge(Vertex v1, Vertex v2) {
			v1.addEdge(v2);
			v2.addEdge(v1);
		}
		
		void addEdge(Vertex v) {
			adj.add(v);
		}
		
		double computeExpCost(double neighborCost, int numNeigh) {	
			return (numNeigh > 0) ? (v + neighborCost/(double)numNeigh) : v;
		}
		
		// O(e)
		int getNumUnvisitedNeighbors() {
			int numChildToVisit = 0;
			for (Vertex child : adj) {
				if (!child.visited) 
					numChildToVisit++;
			}
			return numChildToVisit;
		}
		
		@Override
		public int hashCode() {
			return (id * 941083987 + 334214467) & Integer.MAX_VALUE;
		}
		
		@Override
		public boolean equals(Object v) {
			if (!(v instanceof Vertex))
				return false;
			return v.hashCode() == hashCode();
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
		// pre-compute #unvisited children. Extra O(E) but helps pruning
		// alternately, i could store a state in Vertex as its neighbors are visited
		final int numChildToVisit = root.getNumUnvisitedNeighbors(); 

		// fyi: for a tree, only 1 neighbor will have been visited for every node at most
		// so this numChildToVist=0 is same as numNeighbors-1 > 0
		// TODO to optimize so I need not compute numChildToVisit here.. can do below
		if (numChildToVisit == 0) {
			return root.v;
			//return (root.v > minExpCost) ? INVALID_COST : root.v;
		}
		
		double neighborExpCost = 0d;
		root.visited = true;
		
		for (Vertex child : root.adj) { // for every unvisited child			
			if (!child.visited) {
				double childExpCost = dfs(child, minExpCost);
				if (isInvalidCost(childExpCost) || 
						root.computeExpCost(neighborExpCost + childExpCost, numChildToVisit) > minExpCost)
				{
					// if cost of visiting this child exceeds minExpCost, we can skip this 
					// root as too expensive, no need to check more of its children
					return INVALID_COST;
				}
				
				neighborExpCost += childExpCost;
			}
		}
				
		return root.computeExpCost(neighborExpCost, numChildToVisit);
	}
	
	static void computeMinExpCostDFS(ArrayList<R3Vertex> vList, boolean doPrune) {
		Vertex minVertex = null;
		double minExpCost = Double.MAX_VALUE;
		
		long startTime = System.currentTimeMillis();
		for(Vertex vertex : vList) {

			for (Vertex allv : vList) // mark whole graph as !visited
				allv.visited = false;
			
			double expCost = dfs(vertex, doPrune ? minExpCost : Double.MAX_VALUE);
			
			if (expCost < minExpCost) {
				if (!isInvalidCost(expCost)) {
					minExpCost = expCost;
					minVertex = vertex;
				} else {
					//System.out.println("Early stop for vertex=" + vertex.id);
				}
			}
		}
		
		long endTime = System.currentTimeMillis() - startTime;
		
		if (minVertex != null)
			System.out.println("DFS: minCost = " + minExpCost + 
					" from vertex = " + minVertex.id + ", time = " + endTime);
		else 
			System.out.println("Empty graph");
	}

	static class R3Vertex extends TestVertex {
		double expCost = 0d;
		double sumIncoming = 0d;
		HashMap<Vertex, Double> outCost = new HashMap<Vertex, Double>();
		
		R3Vertex(int id, int v) {
	    super(id, v);
    }
		
		@Override
		void addEdge(Vertex toVertex) {
			super.addEdge(toVertex);
			// set outgoing cost toVertex -> this vertex
			((R3Vertex)toVertex).setOutCost(this, toVertex.v); // TODO remove cast
			// increment incoming cost
			sumIncoming += toVertex.v;
		}
		
		double getOutCost(Vertex toVertex) {
			return outCost.get(toVertex);
		}
		
		void setOutCost(Vertex toVertex, double cost) {
			outCost.put(toVertex, cost);
		}
	}
	
	static void r3(final ArrayList<R3Vertex> vList) {
		long startTime = System.currentTimeMillis();
		System.out.println("Start:" + new Date(startTime));
		// init costs: can this loop be done at either (a) construction time (b) below)
		// ok, moved it into construction time: addEdge()
		/*for (R3Vertex vertex : vList) {
			vertex.sumIncoming = 0;
			for (Vertex jBase : vertex.adj) {
				R3Vertex j = (R3Vertex) jBase;
				j.setOutCost(vertex, j.v);
				vertex.sumIncoming += j.v;
			}
		}*/
		
		// i am doing a batch update: update all incomingSum first, then all c_{i->}
		// TODO consolidate these two loops and still preserve batch update by 
		// (a) using more memory: prevIncomingSum and newIncomingSum
		// TODO what happens if I do a non-batch update? Is the algorithm still correct?
		for (int iter=1; iter <= vList.size()-1; iter++) {
			// nec(i) = \sum_{j \in neighbor(i)} C_{j -> i}
			for (R3Vertex i : vList) {
				i.sumIncoming = 0;
				for (Vertex jBase : i.adj) {
					double costFromJToI = ((R3Vertex) jBase).getOutCost(i);
					i.sumIncoming += costFromJToI;
				}
				//System.out.println("NEC[" + i.id + "]=" + i.sumIncoming);
			}
			
			// C_{j -> i} = j.outcost(i) = val(j) + nec(j) - C_{i -> j}
			for (R3Vertex i : vList) {
				for (Vertex jBase : i.adj) {
					R3Vertex j = (R3Vertex) jBase; //TODO i know what i am doing, but this is inelegant, clean it up
					double costFromIToJ = i.getOutCost(j);
					double incomingExpCostMinusI = // expected => divide by #all neighbors minus i
							(j.getNumNeighbors() < 2) ? 0 : (j.sumIncoming - costFromIToJ)/(double)(j.getNumNeighbors()-1);
					double costFromJToI = j.v + incomingExpCostMinusI;
					//System.out.println(String.format("Setting: c_{%d -> %d}=%.1f", 
					//		j.id, i.id, costFromJToI));
					j.setOutCost(i, costFromJToI);
				}
			}
		}

		R3Vertex min = new R3Vertex(-1, -1); // dummy
		min.expCost = Double.MAX_VALUE;
		for (R3Vertex vertex : vList) {
			vertex.expCost = vertex.v + vertex.sumIncoming/(double)vertex.getNumNeighbors();
			//System.out.println("MinExpCost[" + vertex.id + "]=" + vertex.expCost);
			if (vertex.expCost < min.expCost) {
				min = vertex;
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("End:" + new Date(endTime));
		long elapsedTime = System.currentTimeMillis() - startTime;
		System.out.println("R3: mincost = " + min.expCost + 
				" from vertex=" + min.id + ", time=" + elapsedTime);
	}
	
	private static ArrayList<R3Vertex> getTestGraph_2(int vertexStartId)
	{
		// test: simple graphs
		R3Vertex v1 = new R3Vertex(vertexStartId++, 30);
		R3Vertex v2 = new R3Vertex(vertexStartId++, 20);
		R3Vertex v3 = new R3Vertex(vertexStartId++, 10);
		R3Vertex v4 = new R3Vertex(vertexStartId++, 40);
		R3Vertex v5 = new R3Vertex(vertexStartId++, 50);
		
		// construct undirected edges: {1,2}; {1,3} {1,4} {3,5}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		Vertex.addUndirectedEdge(v1, v4);
		Vertex.addUndirectedEdge(v3, v5);
		// construct vertex list
		ArrayList<R3Vertex> vList = new ArrayList<R3Vertex>();
		vList.add(v1);
		vList.add(v2);
		vList.add(v3);
		vList.add(v4);
		vList.add(v5);
		
		// test code: this is the correct answer for this graph
		v1.setTrueMinCost(v1.v + (v2.v + (v3.v + v5.v) + v4.v)/3.0);
		v2.setTrueMinCost(v2.v + (v1.v + ((v3.v + v5.v) + v4.v)/2.0));
		v3.setTrueMinCost(v3.v + (v5.v + (v1.v + (v2.v + v4.v)/2.0))/2.0);
		v4.setTrueMinCost(v4.v + (v1.v + (v2.v + (v3.v + v5.v))/2.0)/1.0);
		v5.setTrueMinCost(v5.v + (v3.v + (v1.v + (v2.v + v4.v)/2.0))/1.0);		
		ArrayList<TestVertex> testVertexList = new ArrayList<TestVertex>();
		for (Vertex v : vList)
			testVertexList.add((TestVertex) v); // yuck - cast, but we know what we're doing
		TestVertex.printTrueMinCost(testVertexList);

		return vList;
	}

	private static ArrayList<R3Vertex> getTestGraph_1(int startId)
	{
		// test: simple graphs
		R3Vertex v1 = new R3Vertex(startId++, 30);
		R3Vertex v2 = new R3Vertex(startId++, 20);
		R3Vertex v3 = new R3Vertex(startId++, 10);
		
		// construct undirected edges {1,2} {1,3}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		
		// construct vertex list
		ArrayList<R3Vertex> vList = new ArrayList<R3Vertex>();
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
		
		return vList;
	}

	private static void run(ArrayList<R3Vertex> vList, boolean runDFS) {
		//System.out.println("Test: prune=false");
		//computeMinExpCostDFS(vList, false);
		if (runDFS) {
			System.out.println("Test: prune=true");
			computeMinExpCostDFS(vList, true);
		} else {
			System.out.println("Skipping: DFS test");
		}
		System.out.println("Test: R3");
		r3(vList);
	}
	
	public static void main(String[] args) {
		// test: empty graph
		computeMinExpCostDFS(new ArrayList<R3Vertex>(), false);
		computeMinExpCostDFS(new ArrayList<R3Vertex>(), true);
		
		//ArrayList<R3Vertex> g1 = getTestGraph_1();
		//run(g1);
		
		int startId = 1;
		ArrayList<R3Vertex> gFirst = getTestGraph_2(startId);
		//run(gFirst);
		
		// chain some graphs together
		final int numGraphsToChain = 2500; 
		// create a new vertex list containing all graphs; add first graph to it
		ArrayList<R3Vertex> gChain = new ArrayList<R3Vertex>(gFirst);
		for (int i =0; i < numGraphsToChain-1; i++) {
			startId += gFirst.size();
			ArrayList<R3Vertex> gNext = getTestGraph_2(startId);
			// link gNext to gOld
			Vertex.addUndirectedEdge(gFirst.get(0), gNext.get(0));
			gChain.addAll(gNext);
			// set g2 = gNext
			gFirst = gNext;
		}
		System.out.println("Test: #vertices=" + gChain.size());
		run(gChain, gChain.size() < 50000 ? true : false);
		
		// PERF RESULTS: 
		// #vertices=50000 (numGraphsToChain=10000), 
		// - DFS has stackoverflow error
		// - R3 is running >5 min
		// #vertices=25000, numGraphsToChain=5000
		// - DFS: minCost = 73.33333333333333 from vertex = 3, time = 38105
		// - R3: still running at 4 min
		// #vertices=12500, numGraphsToChain=2500
		// - DFS: minCost = 73.33333333333333 from vertex = 3, time = 8398
		// - R3: mincost = 73.33333333333333 from vertex=3, time=81896


	} 
}