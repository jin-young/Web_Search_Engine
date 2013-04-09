all: compile

SOURCES:=find ./src -name "*.java"
TEST_SOURCES:=find ./test -name "*.java"
TEST_LIBS:=./libs/cglib-nodep-2.2.3.jar:./libs/jsoup-1.7.2.jar:./libs/objenesis-1.3.jar:./libs/easymock-3.1.jar:./libs/junit_4.10.0.jar:./libs/org.hamcrest.core_1.1.0.v20090501071000.jar

compile:
	javac -classpath ./src:./libs/jsoup-1.7.2.jar $$($(SOURCES))

clean:
	find ./src -type f -name "*.class" -delete; find ./test -type f -name "*.class" -delete; rm -rf ./data/index/*

jtest: jtest_compile
	java -cp ./src:./test:./libs/jsoup-1.7.2.jar:$(TEST_LIBS) org.junit.runner.JUnitCore edu.nyu.cs.cs2580.AllTests

jtest_compile: compile
	javac -cp ./test -classpath ./src:./test:./libs/jsoup-1.7.2.jar:$(TEST_LIBS) $$($(TEST_SOURCES))

index: compile mining
	java -cp ./src -classpath ./src:./libs/jsoup-1.7.2.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine \
-mode=index --port=25804 --options=conf/engine.conf

mining: compile
	java -cp ./src -classpath ./src:./libs/jsoup-1.7.2.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine \
--mode=mining --options=conf/engine.conf

spearman: compile mining
	java -cp ./src edu.nyu.cs.cs2580.Spearman data/index/pageRank.dat data/index/numView.dat
