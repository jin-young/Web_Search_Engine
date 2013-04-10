#!/bin/bash

rm -f prf*.tsv
i=0
while read q; do
    i=$((i + 1));
    prfout=./data/prf-$i.tsv;
    Q=`echo $q | sed "s/ /%20/g"`;
    curl 'localhost:25804/prf?query='$Q'&ranker=favorite&numdocs=10&numterms=5' > $prfout;
    echo $q:$prfout >> ./data/prf.tsv
done < ./data/queries.tsv

java -cp ./src edu.nyu.cs.cs2580.Bhattacharyya ./data/prf.tsv ./data/qsim.tsv