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

// var partitionsize = 1000;
// var nodecount = 5;
var nodelist; // hold all worker nodes
var nodeworks = {};

UserID = 0;
UserDB = new Map();

// alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
// charArray = {};
// var a = 97;
// for (var i = 0; i<26; i++)
//     charArray[String.fromCharCode(a + i)] = i;
// a = 65;
// for (var i = 26; i<52; i++)
//     charArray[String.fromCharCode(a + i)] = i;




// apply the body-parser middleware to all incoming requests
app.use(bodyparser());

// use express-session
// in mremory session is sufficient for this assignment
app.use(session({
  secret: "CS655GENIProj",
  saveUninitialized: true,
  resave: false}
));

// server listens on port 9007 for incoming connections
app.listen(9007, () => console.log('Listening on port 9007!'));

// GET method route for the addEvents page.
// It serves addSchedule.html present in client folder
// app.get('/',function(req, res) {
//   res.sendFile(__dirname+'/client/ReqForm.html');
// });

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.get('/', function(req, res) {
  req.session.value=UserID;
  var t = new Map();
  t.set("partitionsize", 1000);
  t.set("nodecount", 5);
  UserDB.set(req.session.value, new Map());
  UserDB.set(req.session.value, UserDB.get(req.session.value).set("partitionsize", req.body.Size_of_Partition));
  UserDB.set(req.session.value, UserDB.get(req.session.value).set("nodecount", req.body.Number_of_Node));
  UserID = UserID+1;
  res.sendFile(__dirname+'/client/status.html');
});

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.post('/getProgess', function(req, res) {
  if (req.session.value) {
    console.log(req.body.Hashed_MD5);
    client = new net.Socket();
    client.setKeepAlive(true, 60000);
    client.connect(8000, 'localhost', function() {
    	console.log('Connected');
    	client.write("r/"+req.body.Hashed_MD5+"/"+req.body.Number_of_Node+"/"+req.body.Size_of_Partition+"\n");
    });
    UserDB.set(req.session.value, UserDB.get(req.session.value).set("Cs", client));
    UserDB.set(req.session.value, UserDB.get(req.session.value).set("partitionsize", req.body.Size_of_Partition));
    UserDB.set(req.session.value, UserDB.get(req.session.value).set("nodecount", req.body.Number_of_Node));

    client.on('data', function(data) {
      console.log(data.toString());
      // console.log(data.toString());
      var formData = {
          'Password'              : data.toString()
      };
      res.send(formData);
    	// client.destroy(); // kill client after server's response
      // console.log("Cs closed");
    });

    // client.on('close', function() {
    // 	console.log('Connection closed');
    // });
  } else {
    console.log("haven't login");
    res.redirect('/');
  }
});

// GET method to return the status
// The function queries the table events for the list of places and sends the response back to client
app.post('/reConfigPartitionSize', function(req, res) {
  console.log("in reConfigPartitionSize");
  console.log(req.session.value);
  if (req.session.value) {
    // console.log(req.body.Hashed_MD5);
    client = UserDB.get(req.session.value).get("Cs");
    if(client){
      // client.connect(1337, 'localhost', function() {
      	// console.log('Connected');
      	client.write("p/"+req.body.Size_of_Partition+"\n");
        UserDB.set(req.session.value, UserDB.get(req.session.value).set("partitionsize", req.body.Size_of_Partition));
      // });

      // client.on('data', function(data) {
      //   console.log(data.toString());
      //   if(data.toString()==="OK") {
      //     var formData = {
      //         'result'              : data.toString()
      //     };
      //   } else {
      //     var formData = {
      //         'result'              : "failed"
      //     };
      //   }
      //   res.send(formData);
      // 	// client.destroy(); // kill client after server's response
      // });
    } else {
        // var formData = {
        //     'result'              : "t"
        // };
        // res.send(formData);
    }
    res.end("done");
    // client.on('close', function() {
    // 	console.log('Connection closed');
    // });
  } else {
    console.log("haven't login");
    res.redirect('/');
  }
});

app.post('/reConfigNumberOfNode', function(req, res) {
  console.log("in reConfigNumberOfNode");
  console.log(req.session.value);
  if (req.session.value) {
    // console.log(req.body.Hashed_MD5);
    client = UserDB.get(req.session.value).get("Cs");
    if(client) {
      // client.connect(1337, 'localhost', function() {
      // 	console.log('Connected');
      console.log("n/"+req.body.Number_of_Node+"\n");
      client.write("n/"+req.body.Number_of_Node+"\n");
      UserDB.set(req.session.value, UserDB.get(req.session.value).set("nodecount", req.body.Number_of_Node));
      // });

      // client.on('data', function(data) {
      //   console.log(data);
      //   if(data.toString()==="OK") {
      //     var formData = {
      //         'result'              : data.toString()
      //     };
      //   } else {
      //     var formData = {
      //         'result'              : "failed"
      //     };
      //   }
      //   res.send(formData);
      // 	// client.destroy(); // kill client after server's response
      // });

      // client.on('close', function() {
      // 	console.log('Connection closed');
      // });
    } else {
      //   var formData = {
      //       'result'              : "t"
      //   };
      //   res.send(formData);
    }
    res.end("done");
  } else {
    console.log("haven't login");
    res.redirect('/');
  }
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
