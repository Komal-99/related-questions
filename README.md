# related-questions

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
This repo implements two algorithms:
* Iterative update solution: that iteratively updates the incoming and outgoing cost per "question" (vertex) until all vertices have been updated (hmm... factor graph?)
* DFS search: performs a DFS search rooted at every vertex to compute the expected cost if starting at that vertex. This is much slower than iterative update, and
so is not invoked by default.

## Build and Run
Dependencies: None

Building: see build.sh
On Unix, run build.sh which will create a jar file.
Or simply compile and run RelatedQuestions.java. 

Running: see run.sh for usage

Sample test data: 
See testdata/

RelatedQuestions.java reads from standard input as specified in the problem statement above. You can either type in the input or pipe in a text file. If you type in the input, you can use Ctrl-D to stop entering (or Ctrl-Z on windows).

### To run on Quora's challenges website
1. Copy RelatedQuestions.java and comment out the package declaration (first line).
2. Submit the file [here](http://www.quora.com/challenges#related_questions) and see it pass their test cases. 
Unfortunately, their website does not accept source files with package declarations.  Sorry for the inconvenience, I might upload a package-less version at some point.

*Results 06/20/2015* 
* Iterative Update algorithm passes all but 5 tests, which time out. My implementation is not heavily optimized for time.
* DFS algorithm passes all but 10 tests which time out - expected, because it is brute-force.


