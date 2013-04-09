#!/bin/bash

if [ $# -lt 3 ]
then
    echo ""
    echo "Use this command : "
    echo "      ./bin/start_query_represent.sh <RANKER-TYPE> <NUMDOCS> <NUMTERMS>"
    echo ""
else
    while read line; do
	echo ""
	echo "curl 'localhost:25804/prf?query=$line\&ranker=$1\&numdocs=$2\&numterms=$3'"
	curl 'localhost:25804/prf?query=$line\&ranker=$1\&numdocs=$2\&numterms=$3' > ./data/$line.tsv
    done < ./data/queries.tsv
fi