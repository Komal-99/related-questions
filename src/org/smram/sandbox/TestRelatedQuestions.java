package org.smram.sandbox;

import java.util.ArrayList;

import org.smram.sandbox.RelatedQuestions.IUVertex;
import org.smram.sandbox.RelatedQuestions.Vertex;

public class TestRelatedQuestions {

	/**
	 * Class to store some extra test-only info e.g. the correct answer per vertex
	 * @author smram
	 *
	 */
	static class TestVertex extends IUVertex {
		double trueMinExpCost;
		
		TestVertex(int id, int v) {
			super(id, v);
		}
		
		public void setTrueMinCost(double trueMinExpCost) {
			this.trueMinExpCost = trueMinExpCost;
		}
		
		static double printTrueMinCost(ArrayList<TestVertex> vList) {
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
	/**
	 * @param vertexStartId
	 * @return a simple 5 node graph
	 */
	private static ArrayList<IUVertex> createTestGraph_g5(int vertexStartId)
	{
		// test: simple graphs
		TestVertex v1 = new TestVertex(vertexStartId++, 30);
		TestVertex v2 = new TestVertex(vertexStartId++, 20);
		TestVertex v3 = new TestVertex(vertexStartId++, 10);
		TestVertex v4 = new TestVertex(vertexStartId++, 40);
		TestVertex v5 = new TestVertex(vertexStartId++, 50);
		
		// construct undirected edges: {1,2}; {1,3} {1,4} {3,5}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		Vertex.addUndirectedEdge(v1, v4);
		Vertex.addUndirectedEdge(v3, v5);
		// construct vertex list
		ArrayList<IUVertex> vList = new ArrayList<IUVertex>();
		vList.add(v1);
		vList.add(v2);
		vList.add(v3);
		vList.add(v4);
		vList.add(v5);
		
		// test code: this is the correct answer for this graph
		v1.setTrueMinCost(v1.value + (v2.value + (v3.value + v5.value) + v4.value)/3.0);
		v2.setTrueMinCost(v2.value + (v1.value + ((v3.value + v5.value) + v4.value)/2.0));
		v3.setTrueMinCost(v3.value + (v5.value + (v1.value + (v2.value + v4.value)/2.0))/2.0);
		v4.setTrueMinCost(v4.value + (v1.value + (v2.value + (v3.value + v5.value))/2.0)/1.0);
		v5.setTrueMinCost(v5.value + (v3.value + (v1.value + (v2.value + v4.value)/2.0))/1.0);		
		ArrayList<TestVertex> testVertexList = new ArrayList<TestVertex>();
		for (Vertex v : vList)
			testVertexList.add((TestVertex) v); // yuck - cast, but we know what we're doing
		TestVertex.printTrueMinCost(testVertexList);

		return vList;
	}

	/**
	 * @param startId
	 * @return simple 3 node graph
	 */
	private static ArrayList<IUVertex> createTestGraph_g3(int startId)
	{
		// test: simple graphs
		TestVertex v1 = new TestVertex(startId++, 30);
		TestVertex v2 = new TestVertex(startId++, 20);
		TestVertex v3 = new TestVertex(startId++, 10);
		
		// construct undirected edges {1,2} {1,3}
		Vertex.addUndirectedEdge(v1, v2);
		Vertex.addUndirectedEdge(v1, v3);
		
		// construct vertex list
		ArrayList<IUVertex> vList = new ArrayList<IUVertex>();
		vList.add(v1);
		vList.add(v2);
		vList.add(v3);

		// the reference answer
		v1.setTrueMinCost(v1.value + (v2.value + v3.value)/2.0);
		v2.setTrueMinCost(v2.value + (v1.value/1.0 + (v3.value/1.0)));
		v3.setTrueMinCost(v3.value + (v1.value/1.0 + (v2.value/1.0)));
		ArrayList<TestVertex> testVertexList = new ArrayList<TestVertex>();
		for (Vertex v : vList)
			testVertexList.add((TestVertex) v); // yuck - cast, but we know what we're doing
		TestVertex.printTrueMinCost(testVertexList);
		
		return vList;
	}

	private static void runDFS(ArrayList<IUVertex> vList) {
			System.out.println("Test: DFS + noPrune");
			RelatedQuestions.runDFS(vList, false);
			
			System.out.println("Test: DFS + prune");
			RelatedQuestions.runDFS(vList, true);
	}
	
	private static void runIterativeUpdate(ArrayList<IUVertex> vList) {
		System.out.println("Test: IU");
		RelatedQuestions.runIterativeUpdate(vList);
	}
	
	public static void main(String[] args) {
		///////////////////////////////////////////////////////////////////////////
		// test: empty graph
		RelatedQuestions.runDFS(new ArrayList<IUVertex>(), false);
		RelatedQuestions.runDFS(new ArrayList<IUVertex>(), true);
		
		///////////////////////////////////////////////////////////////////////////
		// test: 3 node chain
		final ArrayList<IUVertex> g3 = createTestGraph_g3(1);
		runDFS(g3);
		runIterativeUpdate(g3);

		///////////////////////////////////////////////////////////////////////////
		// test: 5 node simple
		int startId = 1;
		final ArrayList<IUVertex> g5 = createTestGraph_g5(startId);
		runDFS(g5);
		runIterativeUpdate(g5);
		
		///////////////////////////////////////////////////////////////////////////
		// test: large: chain multiple 5-node graphs together
		final int numGraphsToChain = 550; 
		// create a new vertex list containing all graphs; add first graph to it
		final ArrayList<IUVertex> gChain = new ArrayList<IUVertex>(g5);
		ArrayList<IUVertex> gPrev = g5;
		for (int i =0; i < numGraphsToChain-1; i++) {
			startId += gPrev.size();
			// create a new graph
			ArrayList<IUVertex> gNext = createTestGraph_g5(startId);
			// link it to gPrev
			Vertex.addUndirectedEdge(gPrev.get(0), gNext.get(0));
			gChain.addAll(gNext);
			gPrev = gNext; // move on
		}
		System.out.println("Test: #vertices=" + gChain.size());
		runDFS(gChain);
		runIterativeUpdate(gChain);
		
		// PERF RESULTS: 
		// #vertices=50000 (numGraphsToChain=10000), 
		// - DFS + prune: stackoverflow error
		// - R3: still running 5+ min
		
		// Test: #vertices=25000 (numGraphsToChain=5000)
		// - DFS + prune: minCost = 73.33333333333333 from vertex = 3, time = 38105
		// - IU: still running 4+ min
		
		// Test: #vertices=12500 (numGraphsToChain=2500)
		// - DFS + prune: minCost = 73.33333333333333 from vertex = 3, time = 8398
		// - IU: mincost = 73.33333333333333 from vertex=3, time=81896
		
		// Test: #vertices=2750 (numgraphsToChain=550)
		// - DFS + noPrune: minCost = 73.33333333333333 from vertex = 3, time = 523
		// - DFS + prune:   minCost = 73.33333333333333 from vertex = 3, time = 289
		// - IU: mincost = 73.33333333333333 from vertex=3, time=1569
	}
}