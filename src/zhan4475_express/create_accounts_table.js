/*
TO DO:
-----
READ ALL COMMENTS AND REPLACE VALUES ACCORDINGLY
*/

var mysql = require("mysql");

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
  var sql = `CREATE TABLE tbl_jobs(   Hashed_MD5 VARCHAR(50),
                                      FinCount INT NOT NULL,
                                      PRIMARY KEY (Hashed_MD5))`;
  con.query(sql, function(err, result) {
    if(err) {
      throw err;
    }
    console.log("Table created");
  });
});
