sudo apt-get update
sudo apt install default-jre
sudo apt install default-jdk


javac ./master/*.java
# g++ ./worker/worker.cpp -std=c++1y  -lpthread -o ./worker/worker.out

./master/Server
