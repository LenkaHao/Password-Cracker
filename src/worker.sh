wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/worker/MD5.cpp
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/worker/worker.cpp
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/worker/worker.h
echo "Download done!"

echo "Compiling ..."
g++ ./worker.cpp -std=c++1y  -lpthread -o ./worker.out
echo "Compile done. Please run $ ./worker.out hostname to start!"
