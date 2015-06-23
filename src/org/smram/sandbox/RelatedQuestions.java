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
 * - This implementation doesn't check for cycles. Problem statement assumes tree
 * 
 * Optimizations for R3 run time:
 * - can #iterations be reduced based on depth of tree? stop when no-change?
 * - change graph storage? reduce HashMap lookup? reduce small object overhead? 
 *  
 * Optimizations for depth-bounded DFS: 
 * - BFS? bounded BFS? to avoid stack overflow for long graphs?
 * 
 * @author smram
 */
public class RelatedQuestions {
	static Logger LOG = new Logger(LogLevel.INFO);
	
	static class Vertex {
	  // assumption: cost=time in problem statement, legitimate costs are >0
		final static double INVALID_COST = -1.0d; 
		
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
			return id == ((Vertex)v).id;
		}
	}
	
	/**
	 * TODO make this tail recursive if possible
	 * @param root
	 * @return expected cost of visiting all nodes in tree starting at root. 
	 * expected cost = cost of visiting a neighbor * P(visiting neighbor)
	 * P(visiting neighbor) = 1/#neighbors
	 */
	static double dfs(Vertex root, final double minExpCost) {
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
				double childExpCost = dfs(child, minExpCost);
				if (Vertex.isInvalidCost(childExpCost) || 
						// this pruning is incorrect: even if at this current root cost > min,
						// this cost will be divided at this root's parent by parent.#unvisited
						// which may be < min. So I cannot prune this current root!
						root.computeExpCost(neighborExpCost + childExpCost, numChildToVisit) > minExpCost)
				{
					// if cost of visiting this child exceeds minExpCost, we can skip this 
					// root as too expensive, no need to check more of its children
					return Vertex.INVALID_COST;
				}
				
				neighborExpCost += childExpCost;
			}
		}
				
		return root.computeExpCost(neighborExpCost, numChildToVisit);
	}
	
	/**
	 * Runs a DFS at each vertex in the graph to compute the 
	 * expected cost of starting at that vertex. Returns the vertex with minimum
	 * expected cost.
	 * 
	 * TODO: make this tail-recursive if possible
	 * @param vList
	 * @param doPrune
	 */
	static Vertex runDFS(ArrayList<IUVertex> vList, boolean doPrune) {
		if (vList.isEmpty())
			throw new IllegalArgumentException("Graph is empty");
		
		Vertex minVertex = vList.get(0); // init to some vertex
		double minExpCost = Double.MAX_VALUE;
		for(Vertex vertex : vList) {

			for (Vertex allv : vList) // mark whole graph as !visited
				allv.visited = false;
			
			double expCost = dfs(vertex, doPrune ? minExpCost : Double.MAX_VALUE);
			if (expCost < minExpCost) {
				if (!Vertex.isInvalidCost(expCost)) {
					minExpCost = expCost;
					minVertex = vertex;
				} else {
					if (LOG.isDebugEnabled())
						LOG.debug("Early stop for vertex=" + vertex.id);
				}
			}
		}
		
		return minVertex;
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
	
	/*static class Edge {
		final IUVertex left;
		final IUVertex right;
		double l2r;
		double r2l;
		
		Edge(IUVertex left, IUVertex right, double l2r, double r2l) {
			this.left = left;
			this.right = right;
			this.l2r = l2r;
			this.r2l = r2l;
		}
		
		enum Node { LEFT, RIGHT;
			Node getOpposite() {
				return (this == LEFT) ? RIGHT : LEFT;
			}
		};
		
		IUVertex getNode(Node w) {
			return (w == Node.LEFT) ? left : right;
		}
		
		double getOutCost(Node w) {
			return (w == Node.LEFT) ? l2r : r2l;
		}
		
		void setOutCost(Node w, double cost) {
			if (w == Node.LEFT)
				l2r = cost;
			else
				r2l = cost;
		}
	}
	
	static class Graph {
		ArrayList<Edge> edgeList = new ArrayList<Edge>();
		ArrayList<IUVertex> vList = new ArrayList<IUVertex>();
		
		Graph(ArrayList<IUVertex> vList) {
			this.vList = vList;
		}
		
		void addEdge(IUVertex to, IUVertex from) {
			// this assumes to and from are in vList
			edgeList.add(new Edge(to, from, to.value, from.value));
						
			// increment incoming cost - save it in prev cost
			to.prevSumInCost += from.value;
			from.prevSumInCost += to.value;
		}
		
		void doUpdate(Edge e, Edge.Node whichVertex) {
			IUVertex i = e.getNode(whichVertex);
			IUVertex j = e.getNode(whichVertex.getOpposite());
			
			final double outCost_iToj = e.getOutCost(whichVertex);	
			// I'm going to use prevSumInCost because sumInCost = 0 initially here
			// THIS UPDATE DOESNT WORK CORRECTLY - THE DIFFERENCE is that it updates
			// per edge, then at end does vertex update. The other impl does all updates
			// of edges for a single vertex together and updates that vertex's sumIncoming
			// befor emoving to next vertex. That seems to give correct answer.
			final double expInCost_jNoti = (j.getNumNeighbors() < 2) ? 0 :  
				(j.prevSumInCost - outCost_iToj)/(double)(j.getNumNeighbors()-1);
			final double outCost_jToi = j.value + expInCost_jNoti;
			e.setOutCost(whichVertex.getOpposite(), outCost_jToi);

			//i.sumInCost += outCost_jToi;
			
			if (logLevel.isTrickleEnabled()) {
				printDebug(String.format("Setting: c_{%d -> %d}=%.1f", 
						j.id, i.id, outCost_jToi));
			}
		}
	}
	
	static Vertex runIterativeUpdateGraph(Graph graph) {
		if (graph.vList.isEmpty())
			throw new IllegalArgumentException("Graph is empty");
		
		// init done: outgoing costs and incoming costs were init'd in addEdge()
		
		boolean changed = true;
		int iter = 1;
		for (; changed && iter <= graph.vList.size()-1; iter++) 
		{	
			changed = false;
			
			// 2. updated outgoing costs for each edge
			for (Edge e : graph.edgeList) {
				// do left vertex first
				graph.doUpdate(e, Edge.Node.LEFT);
				graph.doUpdate(e, Edge.Node.RIGHT);
			}
			// 1. update sum of expected incoming costs for each vertex
			for (Edge e : graph.edgeList) {
				e.left.sumInCost += e.r2l;
				e.right.sumInCost += e.l2r;
			}
			
			for (IUVertex v : graph.vList) {
				if (Math.abs(v.sumInCost - v.prevSumInCost) > 1e-6) {
					changed = true; // at least one vertex has changed
				}
				v.prevSumInCost = v.sumInCost;
				v.sumInCost = 0d;
			}
		}

		// all done, now find the node with the minimum expected cost
		if (logLevel.isDebugEnabled())
			System.out.println("Converged: #iterations=" + iter);
		Vertex minVertex = graph.vList.get(0); // init to some vertex
		double minExpCost = Double.MAX_VALUE;
		for (IUVertex vertex : graph.vList) {
			final double expCost = 
					vertex.value + vertex.sumInCost/(double)vertex.getNumNeighbors();
			if (expCost < minExpCost) {
				minExpCost = expCost;
				minVertex = vertex;
			}
			
			if (logLevel.isDebugEnabled())
				printDebug("MinExpCost[" + vertex.id + "]=" + expCost);
		}
		
		return minVertex;
	}*/

	/**
	 * It executes this iterative update:
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
	 * @param vList list of R3Vertex
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
	public static void main(String[] args) {
		Scanner stdin = new Scanner(new BufferedInputStream(System.in));
  	ArrayList<IUVertex> vList = new ArrayList<IUVertex>();
  	//Graph graph = new Graph(vList);
  	
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
  		//graph.addEdge(v1, v2);
  	}

    stdin.close();
    
    long startTime = System.currentTimeMillis();
    // done reading input, calculate answer
    //Vertex minVertex = runDFS(vList, true);     // this is incorrect
    //Vertex minVertex = runDFS(vList, false);    // this is 2nd fasstest
    Vertex minVertex = runIterativeUpdate(vList); // this is fastest
    //Vertex minVertex = runIterativeUpdateGraph(graph); // this is incorrect
    if (LOG.isDebugEnabled())
    	LOG.debug("Time=" + (System.currentTimeMillis() - startTime));
    
    System.out.println(minVertex.id);
	}
}