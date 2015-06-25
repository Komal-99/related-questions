#!/bin/bash

# create bin/ directory for class files
mkdir -v bin;

# compiles java files into bin/ directory
javac -d bin/ -sourcepath src/ src/org/smram/sandbox/*java src/org/smram/utils/*java

# creates a jar named related_questions.jar in the current directory
jar cf related_questions.jar -C bin/ org

