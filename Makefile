all: compile

SOURCES:=find ./src -name "*.java"

compile:
	javac -cp ./src $$($(SOURCES))

buildindex:
	java -cp src edu.nyu.cs.cs2580.SearchEngine \
--mode=index --options=conf/engine.conf

clean:
	find ./src -type f -name "*.class" -delete
