// YOU CAN USE THIS FILE AS REFERENCE FOR SERVER DEVELOPMENT
// include the express module
var express = require("express");
// create an express application
var app = express();
// helps in extracting the body portion of an incoming request stream
var bodyparser = require('body-parser');
// fs module - provides an API for interacting with the file system
var fs = require("fs");
// helps in managing user sessions
var session = require('express-session');
// native js function for hashing messages with the SHA-256 algorithm
var crypto = require('crypto');
// include the mysql module
// var mysql = require("mysql");
// var mydb = require("./db.js");
var net = require('net');

var partitionsize = 1000;
var nodecount = 5;
var nodelist; // hold all worker nodes
var nodeworks = {};

alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
charArray = {};
var a = 97;
for (var i = 0; i<26; i++)
    charArray[String.fromCharCode(a + i)] = i;
a = 65;
for (var i = 26; i<52; i++)
    charArray[String.fromCharCode(a + i)] = i;

// apply the body-parser middleware to all incoming requests
app.use(bodyparser());

// use express-session
// in mremory session is sufficient for this assignment
app.use(session({
  secret: "csci4131secretkey",
  saveUninitialized: true,
  resave: false}
));

// server listens on port 9007 for incoming connections
app.listen(9007, () => console.log('Listening on port 9007!'));

// GET method route for the addEvents page.
// It serves addSchedule.html present in client folder
app.get('/',function(req, res) {
  res.sendFile(__dirname+'/client/ReqForm.html');
});

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.get('/status', function(req, res) {
  res.sendFile(__dirname+'/client/status.html');
});

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.post('/getProgess', function(req, res) {
  console.log(req.body.Hashed_MD5);
  var client = new net.Socket();
  client.connect(1337, 'localhost', function() {
  	console.log('Connected');
  	client.write("r/"+req.body.Hashed_MD5+"/"+req.body.Number_of_Node+"/"+req.body.Size_of_Partition);
  });

  client.on('data', function(data) {
    console.log(data.toString());
    var formData = {
        'Password'              : data.toString()
    };
    res.send(formData);
  	client.destroy(); // kill client after server's response
  });

  client.on('close', function() {
  	console.log('Connection closed');
  });
});

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.post('/reConfigPartitionSize', function(req, res) {
  // console.log(req.body.Hashed_MD5);
  var client = new net.Socket();
  client.connect(1337, 'localhost', function() {
  	console.log('Connected');
  	client.write("p/"+req.body.Size_of_Partition);
  });

  client.on('data', function(data) {
    console.log(data.toString());
    if(data.toString()==="OK") {
      let formData = {
          'result'              : data.toString()
      };
    } else {
      let formData = {
          'result'              : "failed"
      };
    }
    res.send(formData);
  	client.destroy(); // kill client after server's response
  });

  client.on('close', function() {
  	console.log('Connection closed');
  });
});

app.post('/reConfigNumberOfNode', function(req, res) {
  // console.log(req.body.Hashed_MD5);
  var client = new net.Socket();
  client.connect(1337, 'localhost', function() {
  	console.log('Connected');
  	client.write("n/"+req.body.Number_of_Node);
  });

  client.on('data', function(data) {
    console.log(data.toString());
    if(data.toString()==="OK") {
      let formData = {
          'result'              : data.toString()
      };
    } else {
      let formData = {
          'result'              : "failed"
      };
    }
    res.send(formData);
  	client.destroy(); // kill client after server's response
  });

  client.on('close', function() {
  	console.log('Connection closed');
  });
});

// POST method to insert details of a new event to tbl_events table
// app.post('/postMD5', function(req, res) {
//   var que = "INSERT INTO tbl_jobs (Hashed_MD5, FinCount) VALUES (?,?)";
//   mydb.get().query(que, [req.body.Hashed_MD5, 0], function (err, result) {
//     if (err) throw err;
//     console.log("1 record for Hashed_MD5 inserted");
//   });

  //establish tcp connection to master!!!!!!!!!!!!!!!!!!!!

  //=====================================================
  // partitionsize = parseInt(req.body.Size_of_Partition, 10)
  // nodecount = parseInt(req.body.Number_of_Node, 10)
  // // send jobs
  //
  // for(let i = 0, i < nodecount; i++) {
  //   var client = new net.Socket();
  //   client.connect(58000, nodelist[i], function() {
  //   	console.log('Connected');
  //   	client.write('0');
  //   });
  //
  //   client.on('data', function(data) {
  //   	console.log('Received: ' + data);
  //   	client.destroy(); // kill client after server's response
  //   });
  //
  //   client.on('close', function() {
  //   	console.log('Connection closed');
  //   });
  //   nodeworks[nodelist[i]] = partitionsize;
  // }
  ////=====================================================

//   res.redirect("/submitted");
// });

// middle ware to serve static files
app.use('/client', express.static(__dirname + '/client'));


// function to return the 404 message and error to client
app.get('*', function(req, res) {
  // add details
  res.sendFile(__dirname+'/client/404.html');
});
