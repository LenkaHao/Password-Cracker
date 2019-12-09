/*
TO DO:
-----
READ ALL COMMENTS AND REPLACE VALUES ACCORDINGLY
*/

var mysql = require("mysql");
var crypto = require('crypto');

var con = mysql.createConnection({
  host: "localhost",
  user: "foo", // replace with the database user provided to you
  password: "bar", // replace with the database password provided to you
  database: "GENIProj", // replace with the database user provided to you
  port: 3306
});

con.connect(function(err) {
  if (err) {
    throw err;
  };
  console.log("Connected!");

  var rowToBeInserted = {
    Hashed_MD5: crypto.createHash('MD5').update("tango").digest('base64'), // replace with acc_name chosen by you OR retain the same value
    FinCount: 100, // replace with acc_login chosen by you OR retain the same vallue
  };

  var sql = ``;
  con.query('INSERT tbl_jobs SET ?', rowToBeInserted, function(err, result) {
    if(err) {
      throw err;
    }
    console.log("Value inserted");
  });
});
