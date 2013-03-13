#!/bin/bash
java -cp src -Xmx512m edu.nyu.cs.cs2580.SearchEngine \
-mode=serve --port=25804 --options=conf/engine.conf
exit 0
