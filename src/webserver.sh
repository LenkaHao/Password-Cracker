wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/client/404.html
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/client/status.html
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/client/css/style.css
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/index.js
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/package.json
wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/Web+WebServer/package-lock.json
mkdir client
mkdir client/css
mv 404.html client/
mv status.html client/
mv style.css client/css/
echo "Download done!"

echo "Setting up environment for web server..."
sudo apt-get update
sudo apt-get install nodejs
sudo apt-get install npm
sudo npm install
echo "Environment (NodeJS) is set up!"

echo "Please run $ ./index.js to start!"
