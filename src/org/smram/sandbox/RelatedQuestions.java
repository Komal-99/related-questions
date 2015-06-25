package org.smram.sandbox;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.smram.utils.Logger;
import org.smram.utils.Logger.LogLevel;

/**
 * Problem specification: {@link http://www.quora.com/challenges#related_questions}
 * 
 * Caveats:
 * This implementation doesn't check for cycles. Problem statement assumes tree
 * 
 * @author smram
 */
public class RelatedQuestions {
	static Logger LOG = new Logger(LogLevel.INFO);
	
	static class Vertex {
		
		final int id;
		final int value;
		final ArrayList<Vertex> adj = new ArrayList<Vertex>();

		private boolean visited;
		protected double expCost = 0d; // this is the result, only for printing
		
		Vertex(int id, int v) {
			this.id = id; 
			this.value = v;
			this.visited = false;
		}
		
		static void addUndirectedEdge(Vertex v1, Vertex v2) {
			v1.addEdge(v2);
			v2.addEdge(v1);
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
			return id == ((Vertex)v).id;
		}
	}
	
	////////////////////////////////// DFS /////////////////////////////////////
	/**
	 * @param root
	 * @return expected cost of visiting all nodes in tree starting at root. 
	 * expected cost = cost of visiting a neighbor * P(visiting neighbor)
	 * P(visiting neighbor of vertex i) = 1/(number of neighbors of vertex i)
	 */
	static double dfs(Vertex root) {
		// pre-compute #unvisited children. Extra O(E) but helps pruning
		// alternately, i could store a state in Vertex as its neighbors are visited
		final int numChildToVisit = root.getNumUnvisitedNeighbors(); 
		
		// TODO apparently this assert is NOT true.... do i have a bug??
		// assert: for a tree, only 1 neighbor will have been visited for every node 
		// so this node's numChildToVist==0 is same as numNeighbors-1 > 0
		if (numChildToVisit == 0)
			return root.value;

		double neighborExpCost = 0d;
		root.visited = true;
		
		for (Vertex child : root.adj) { // for every unvisited child	
			if (!child.visited) { 
				double childExpCost = dfs(child);
				neighborExpCost += childExpCost;
			}
		}
				
		return root.computeExpCost(neighborExpCost, numChildToVisit);
	}
	
	/**
	 * Runs a DFS at each vertex in the graph to compute the 
	 * expected cost of starting at that vertex. 
	 * 
	 * @param vList
	 * @return Vertex with minimum expected cost
	 */
	static Vertex runDFS(ArrayList<IUVertex> vList) {
		if (vList.isEmpty())
			throw new IllegalArgumentException("Graph is empty");
		
		Vertex minVertex = vList.get(0); // init to some vertex
		minVertex.expCost = Double.MAX_VALUE;
		for(Vertex vertex : vList) {

			for (Vertex allv : vList) // mark whole graph as !visited
				allv.visited = false;
			
			vertex.expCost = dfs(vertex);
			if (vertex.expCost < minVertex.expCost)
				minVertex = vertex;
		}
		
		return minVertex;
	}
	

	/////////////////////// ITERATIVE UPDATE ////////////////////////////////////
	/**
	 * Vertex with extra information for the iterative update algorithm 
	 * {@link RelatedQuestions#runIterativeUpdate(ArrayList)}
	 * 
	 * @author smram
	 *
	 */
	static class IUVertex extends Vertex {
		private double sumInCost = 0d;
		private double prevSumInCost = 0d;
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
	 * It executes this iterative update:
	 * do #vertex-1 times or until unchanged:
	 *   foreach vertex i:
	 *   foreach edge (i,j):
	 *   	 sumInCost(i) += \sum_{j \in neighbor(i)} expOutCost(j, i)
	 *   
	 *   foreach vertex i:
	 *   foreach edge (i,j):
	 *   	 expOutCost(j, i) <- value(j) + (sumInCost(j) - expOutCost(i, j))/(numNeighbor(j)-1)
	 * 
	 * where:
	 * expOutCost(j, i) is the expected outgoing cost from vertex j to vertex i
	 * sumInCost(i) is the expected incoming cost into vertex i
	 * neighbor(i) is the set of neighbors of vertex i and numNeighbor(i) is the number
	 * 
	 * @param vList list of vertices
	 * @return Vertex with minimum expected cost
	 */
	static Vertex runIterativeUpdate(final ArrayList<IUVertex> vList) {
		if (vList.isEmpty())
			throw new IllegalArgumentException("Graph is empty");
		
		boolean changed = true;
		int iter = 1;
		Vertex minVertex = vList.get(0); // init to some vertex
		
		for (; changed && iter <= vList.size()-1; iter++) 
		{	
			// reset per iteration, the final iter has the final min
			changed = false;
			double minExpCost = Double.MAX_VALUE; 
			
			// 1. update sum of expected incoming costs for each vertex
			// 2. updated outgoing costs for each edge
			// outgoing costs and incoming costs were init'd in addEdge()
			for (IUVertex i : vList) {
				i.prevSumInCost = i.sumInCost;
				i.sumInCost = 0;
				for (Vertex jBase : i.adj) {
					IUVertex j = (IUVertex) jBase; //TODO remove cast
					final double outCost_iToj = i.getOutCost(j);					
					final double expInCost_jNoti = (j.getNumNeighbors() < 2) ? 0 :  
						(j.sumInCost - outCost_iToj)/(double)(j.getNumNeighbors()-1);
					final double outCost_jToi = j.value + expInCost_jNoti;
					j.setOutCost(i, outCost_jToi);
					
					i.sumInCost += outCost_jToi;

					if (LOG.isTrickleEnabled()) {
						LOG.trickle(String.format("Setting: c_{%d -> %d}=%.1f", 
								j.id, i.id, outCost_jToi));
					}
				}
				
				if (Math.abs(i.sumInCost - i.prevSumInCost) > 1e-6) {
					changed = true; // at least one vertex has changed
				}
				
				i.expCost = 
						i.value + i.sumInCost/(double)i.getNumNeighbors();
				if (i.expCost < minExpCost) {
					minExpCost = i.expCost;
					minVertex = i;
				}
				if (LOG.isDebugEnabled())
					LOG.debug("Iter #" + iter + ", MinExpCost[" + i.id + "]=" + i.expCost);
			}
		}

		if (LOG.isDebugEnabled())
			LOG.debug("Converged: #iterations=" + iter);
		
		return minVertex;
	}
	
	/**
	 * Reads input from stdin in the format specified here: 
	 * http://www.quora.com/challenges#related_questions
	 */
	private static void run(String[] mainArgs) {
		Scanner stdin = new Scanner(new BufferedInputStream(System.in));
  	ArrayList<IUVertex> vList = new ArrayList<IUVertex>();
  	
		// first line: N = #vertices
  	int N = stdin.nextInt();
  	// read N vertices: vertex id starts at 1. ArrayList indx starts at 0
  	// its not stated explicitly that vertex ids are consecutive, but implied
  	// so I'll use an arraylist. otherwise i'd use a map.
  	for (int i=0; i < N; i++) { 
  		final int vertexVal = stdin.nextInt();
  		vList.add(new IUVertex(i + 1, vertexVal));
  	}
  	// read N-1 edges - the problem states there will be only N-1 edges
  	for (int i=0; i < (N-1); i++) {
  		final int vid1 = stdin.nextInt();
  		final int vid2 = stdin.nextInt();
  		final IUVertex v1 = vList.get(vid1 - 1);
  		final IUVertex v2 = vList.get(vid2 - 1);
  		Vertex.addUndirectedEdge(v1, v2);
  	}

    stdin.close();

    // done reading input, calculate answer
    long startTime = System.currentTimeMillis();
    Vertex minVertex = runDFS(vList);    // this is 2nd fastest
    //Vertex minVertex = runIterativeUpdate(vList); // this is fastest
    if (LOG.isDebugEnabled())
    	LOG.debug("Time=" + (System.currentTimeMillis() - startTime));
    
    System.out.println(minVertex.id);
	}
	
	public static void main(String[] args) {
		run(args);
	}
}