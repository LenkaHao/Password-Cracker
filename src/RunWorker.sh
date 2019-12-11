sudo apt-get update
# sudo apt install default-jdk
# sudo apt-get install nodejs
# sudo apt-get install npm

# javac ./master/*.java
g++ ./worker/worker.cpp -std=c++1y  -lpthread -o ./worker/worker.out

./worker/worker.out
