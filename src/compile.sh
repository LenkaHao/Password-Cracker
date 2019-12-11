

javac ./master/*.java
g++ ./worker/worker.cpp -std=c++1y  -lpthread -o ./worker/worker.out
