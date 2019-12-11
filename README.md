## Run password cracker

If you're running the project on nodes that are already reserved by us, please go to Section 2 directly.
Otherwise, start with Section 1 to set up servers on GENI.

### Section 1: Deploy the project on GENI

1. Reserve resources on GENI

   Use the Rspec "password-cracker" to reserve nodes.

2. Set up the web server

   Log into the node with clientID "Webserver" and run the following one by one:

   \$ wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/webserver.sh
   
   \$ chmod +x webserver.sh
   
   \$ ./webserver.sh
   
   \$ node index.js

3. Set up the master

   Log into the node with clientID "master"

   \$ wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/master.sh
   
   \$ chmod +x master.sh
   
   \$ ./master.sh
   
   \$ java Server

4. Set up workers

   Log into the nodes with clientID "workerx", where x is number 1 to 5.

   Run the following commands:

   \$ wget https://raw.github.com/LenkaHao/Password-Cracker/master/src/worker.sh
   
   \$ chmod +x worker.sh
   
   \$ ./worker.sh
   
   \$ ./worker.out

   
### Section 2: Use the web interface to interact

1.  In a web browser, go to "webserver_ip:9007", where webserver_ip can be found on GENI.

    For our resource, go to http://72.36.65.84:9007/
    
    Enter md5 hash, number of workers (1-5) and size of a partition (e.g. 1000)
   
    Wait until the result comes back. 
   
    To change the number of workers on the fly, enter a number for workers. Do no click "Submit". The web interface will dynamically detect the change.