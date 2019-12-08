// establish connection to MYSQL database

var mysql = require("mysql");

var connection = mysql.createConnection({
  host: "localhost",
  user: "foo",
  password: "bar",
  database: "GENIProj",
  port: 3306
});

connection.connect(function(err) {
  if (err) {
    throw err;
  };
  console.log("Connected to MYSQL database!");
});

// export the connection
exports.get = function() {
  return connection;
}
