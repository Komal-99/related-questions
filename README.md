# related-questions

#RelatedQuestions
This repo contains solutions to this problem: http://www.quora.com/challenges#related_questions

##Problem Statement
From here: http://www.quora.com/challenges#related_questions
For the purposes of this problem, suppose Quora has N questions, and question i(1≤i≤N) takes Ti time to read. There exists exactly one path from any question to another, and related questions form undirected pairs between themselves. In other words, the graph of related questions is a tree. 
Each time Steve reads a question, he will see a list of related questions and navigate to one that he hasn't read yet at random. Steve will stop reading once there are no unread related questions.
Which question should we show first to Steve so that we minimize his total expected reading time? It is guaranteed that there is one unique question that is optimal.

Input Format
Line 1: A single integer, N
Line 2: N integers, Ti
Line 3...N+1: Each line contains two integers A, B indicating that question A and B are related

Output Format
Line 1: A single integer, X, the best question to show first.

Constraints
1≤N≤10^5
1≤Ti≤10^6

Sample Input
1
2
3
4
5
6
5
2 2 1 2 2
1 2
2 3
3 4
4 5

Sample Output
3

## Solutions
This repo contains two solutions:
* Depth-limited DFS search: performs a DFS search rooted at every vertex to compute the expected cost if starting at that vertex. Prunes the search space by limiting the depth of the current DFS run if the current cost exceeds the minimum cost seen so far. from previo.
* Iterative update solution: that iteratively updates the incoming and outgoing cost per "question" (vertex) until all vertices have been updated.

The code does *not* currently accept the input/output formats described below, so you cannot submit it to the Quora challenges website.
