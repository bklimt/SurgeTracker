
var express = require('express');
var app = express();

app.set('views', 'cloud/views');
app.set('view engine', 'jade');
app.use(express.bodyParser());

var Surge = Parse.Object.extend("Surge", {
  duration: function() {
    var start = this.get("start");
    var end = this.get("end") || new Date();
    var durationMillis = end - start;
    var durationSeconds = Math.floor(durationMillis / 1000);
    var durationMinutes = Math.floor(durationSeconds / 60);
    durationSeconds = durationSeconds % 60;
    var durationString = durationMinutes.toString();
    if (durationString.length < 2) {
      durationString = "0" + durationString;
    }
    durationString += (":" + ("0" + durationSeconds).substr(-2));
    return durationString;
  }
});

var getSurgesForId = function(uuid) {
  return Parse.Promise.as().then(function() {
    var query = new Parse.Query(Parse.User);
    query.equalTo("username", uuid);
    return query.first({ useMasterKey: true });

  }).then(function(user) {
    return Parse.User.become(user.getSessionToken());

  }).then(function(user) {
    var query = new Parse.Query("Surge");
    query.limit(1000);
    return query.find();

  });
};

app.get('/surges/:id', function(req, res) {
  var uuid = req.params.id;
  getSurgesForId(uuid).then(function(surges) {
    res.render('surges', { surges: surges });
  }, function(error) {
    res.render('error', {
      message: "Error: " + error.code + ": " + error.message
    });
  });
});

app.listen();
