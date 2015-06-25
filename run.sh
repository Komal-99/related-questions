#!/bin/bash


echo "Test: sample_input_1.txt"
java -cp related_questions.jar org.smram.sandbox.RelatedQuestions < testdata/sample_input_1.txt

echo "Test: sample_input_3node.txt"
java -cp related_questions.jar org.smram.sandbox.RelatedQuestions < testdata/sample_input_3node.txt

echo "Test: sample_input_5node.txt"
java -cp related_questions.jar org.smram.sandbox.RelatedQuestions < testdata/sample_input_5node.txt


