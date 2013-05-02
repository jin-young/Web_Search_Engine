#!/bin/bash
java -cp ./src -classpath ./src:./libs/jsoup-1.7.2.jar:./libs/gson-2.2.3.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine \
--mode=mining --options=conf/engine.conf
exit 0
