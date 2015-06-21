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
  // assumption: cost=time in problem statement, legitimate value of cost is >0
	final static double INVALID_COST = -1.0d; 
	
	static class Vertex {
		
		final int id;
		final int value;
		boolean visited;
		final ArrayList<Vertex> adj = new ArrayList<Vertex>();
		
		Vertex(int id, int v) {
			this.id = id; 
			this.value = v;
			this.visited = false;
		}
		
		static void addUndirectedEdge(Vertex v1, Vertex v2) {
			v1.addEdge(v2);
			v2.addEdge(v1);
		}
		
		static boolean isInvalidCost(double c) {
			return (c < 0); // check < to avoid checking == on a double	
		}
		
		int getNumNeighbors() {
			return adj.size();
		}
		
		void addEdge(Vertex v) {
			adj.add(v);
		}
		
		double computeExpCost(double neighborCost, int numNeigh) {	
			return (numNeigh > 0) ? (value + neighborCost/(double)numNeigh) : value;
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
			return root.value;
			//return (root.v > minExpCost) ? INVALID_COST : root.v;
		}
		
		double neighborExpCost = 0d;
		root.visited = true;
		
		for (Vertex child : root.adj) { // for every unvisited child			
			if (!child.visited) {
				double childExpCost = dfs(child, minExpCost);
				if (Vertex.isInvalidCost(childExpCost) || 
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
	
	static void runDFS(ArrayList<IUVertex> vList, boolean doPrune) {
		Vertex minVertex = null;
		double minExpCost = Double.MAX_VALUE;
		
		long startTime = System.currentTimeMillis();
		for(Vertex vertex : vList) {

			for (Vertex allv : vList) // mark whole graph as !visited
				allv.visited = false;
			
			double expCost = dfs(vertex, doPrune ? minExpCost : Double.MAX_VALUE);
			if (expCost < minExpCost) {
				if (!Vertex.isInvalidCost(expCost)) {
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
		else if (vList.isEmpty())
			System.out.println("Empty graph");
		else // should never happen 
			throw new IllegalStateException("Error: graph not empty but minExpCost not found");
	}

	/**
	 * Vertex with extra information for the iterative update algorithm 
	 * {@link RelatedQuestions#runIterativeUpdate(ArrayList)}
	 * 
	 * @author smram
	 *
	 */
	static class IUVertex extends Vertex {
		private double sumInCost = 0d;
		// TODO for more performance, revisit this edge traversal data structure
		// hashmap lookups can be slow
		private HashMap<Vertex, Double> outCost = new HashMap<Vertex, Double>();
		
		IUVertex(int id, int v) {
	    super(id, v);
    }
		
		@Override
		void addEdge(Vertex toVertex) {
			super.addEdge(toVertex);
			
			// set outgoing cost from toVertex to this vertex
			((IUVertex)toVertex).setOutCost(this, toVertex.value); // TODO remove cast
			
			// increment incoming cost
			sumInCost += toVertex.value;
		}
		
		double getOutCost(Vertex toVertex) {
			return outCost.get(toVertex);
		}
		
		void setOutCost(Vertex toVertex, double cost) {
			outCost.put(toVertex, cost);
		}
	}
	
	/**
	 * This implementation is inspired by a Floyd-Warshall all source shortest 
	 * paths type solution. It executes this iterative update:
	 * do #vertex-1 times or until unchanged:
	 *   foreach vertex i:
	 *   foreach edge (i,j):
	 *   	 expInCost(i) += \sum_{j \in neighbor(i)} expOutCost(j, i)
	 *   
	 *   foreach vertex i:
	 *   foreach edge (i,j):
	 *   	 expOutCost(j, i) <- value(j) + (expInCost(j) - expOutCost(i -> j))/(numNeighbor(j)-1)
	 * 
	 * where:
	 * expOutCost(j, i) is the expected outgoing cost from vertex j to vertex i
	 * sumExpInCost(i) is the expected incoming cost into vertex i
	 * numNeighbor(i) is the number of neigbors of vertex i
	 * 
	 * TODO "until unchanged" is not implemented, so this is rather slow
	 * for large graphs! Can it be implemented?
	 * 
	 * @param vList list of R3Vertex
	 */
	static void runIterativeUpdate(final ArrayList<IUVertex> vList) {
		long startTime = System.currentTimeMillis();
		System.out.println("Start:" + new Date(startTime));
		
		// init done: outgoing costs and incoming costs were init'd in addEdge()
		
		// This impl does a batch update: updates all sumIncomingCost first, then all outgoing costs
		// TODO can consolidate these two loops and still preserve batch update by 
		// using more memory: maintain prevIncomingSum and newIncomingSum
		// TODO what happens if I do a non-batch update? Is the algorithm still correct?
		for (int iter=1; iter <= vList.size()-1; iter++) {
			
			// 1. update sum of expected incoming costs for each vertex
			for (IUVertex i : vList) {
				i.sumInCost = 0;
				for (Vertex jBase : i.adj) {
					double costFromJToI = ((IUVertex) jBase).getOutCost(i); // TODO remove cast
					i.sumInCost += costFromJToI;
				}
				//System.out.println("NEC[" + i.id + "]=" + i.sumIncoming);
			}
			
			// 2. updated outgoing costs for each edge
			for (IUVertex i : vList) {
				for (Vertex jBase : i.adj) {
					IUVertex j = (IUVertex) jBase; //TODO remove cast
					final double outCost_iToj = i.getOutCost(j);					
					final double expInCost_jNoti = (j.getNumNeighbors() < 2) ? 0 :  
						(j.sumInCost - outCost_iToj)/(double)(j.getNumNeighbors()-1);
					final double outCost_jToi = j.value + expInCost_jNoti;
					//System.out.println(String.format("Setting: c_{%d -> %d}=%.1f", 
					//		j.id, i.id, costFromJToI));
					j.setOutCost(i, outCost_jToi);
				}
			}
		}

		// all done, now find the node with the minimum expected cost
		IUVertex minVertex = null;
		double minExpCost = Double.MAX_VALUE;
		for (IUVertex vertex : vList) {
			final double expCost = vertex.value + vertex.sumInCost/(double)vertex.getNumNeighbors();
			//System.out.println("MinExpCost[" + vertex.id + "]=" + vertex.expCost);
			if (expCost < minExpCost) {
				minExpCost = expCost;
				minVertex = vertex;
			}
		}
		
		final long elapsedTime = System.currentTimeMillis() - startTime;		
		System.out.println("R3: mincost = " + minExpCost + 
				" from vertex=" + minVertex.id + ", time=" + elapsedTime);
	}
}