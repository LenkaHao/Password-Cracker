wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/Client.java
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/ClientHandler.java
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/JobHandler.java
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/NextPartition.java
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/Server.java
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master/WorkerInfo.java
echo "Download done!"

echo "Set up environment for master..."
sudo apt-get update
sudo apt install default-jre
sudo apt install default-jdk
echo "Environment (Java) is set up!"

echo "Compiling ..."
javac *.java
echo "Compile done. Please run $ java ./Server to start!"
