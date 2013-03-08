all: compile

compile:
	javac src/edu/nyu/cs/cs2580/*.java

clean:
	rm -rf src/edu/nyu/cs/cs2580/*.class
