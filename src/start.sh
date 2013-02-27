#!/bin/bash

javac edu/nyu/cs/cs2580/*.java
echo "Compile complete"
java edu.nyu.cs.cs2580.SearchEngine 25804 ../data/corpus.tsv

exit 0