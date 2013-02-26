#!/bin/bash

rankers=(cosine QL phrase numviews linear)
outFiles=(../results/hw1.3-vsm.tsv ../results/hw1.3-ql.tsv ../results/hw1.3-phrase.tsv ../results/hw1.3-numviews.tsv ../results/hw1.3-linear.tsv)
sed -e 's/ /+/g' ../data/queries.tsv > queries.txt

for((i=0; i<${#rankers[*]}; i++))do
  # rm -rf ${outFiles[$i]}
   while read line; do
       curl "localhost:25804/search?query=$line&ranker=${rankers[$i]}&format=text" | \java edu.nyu.cs.cs2580.Evaluator ../data/qrels.tsv ${rankers[$i]}
   done < queries.txt
done

rm -rf queries.txt

exit 0