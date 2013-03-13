all: compile

SOURCES:=find ./src -name "*.java"

compile:
	javac -cp ./src -classpath ./src:./libs/jsoup-1.7.2.jar $$($(SOURCES))

buildindex: compile
	java -cp src -classpath ./src:./libs/jsoup-1.7.2.jar edu.nyu.cs.cs2580.SearchEngine \
--mode=index --options=conf/engine.conf

clean:
	find ./src -type f -name "*.class" -delete
