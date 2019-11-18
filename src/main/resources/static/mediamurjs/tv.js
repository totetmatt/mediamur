var hashtags = new AdvancedTextSearch("hashGraphContent");


hashtags.onWordClickSelect = function(w) {
    fetch('/stream/mediahashtag/'+w)
     .then(function(response) {

     return response.json(); })
     .then(function(data) {
           console.log(data)
        hashGraphVideoVue.result = data.filter(x=> x['media']['videoVariants'][0] != undefined && x['media']['videoVariants'].size != 0)

     })
};

hashtags.onWordClickUnselect = function(w) {
  hashGraphVideoVue.result =[]
};

function send_to_verified() {
  fetch("/verified", {
    method: "POST",
    body: JSON.stringify(mediaPlaylist[indexPlaylist])
  });
}

var statusVue = new Vue({
  el: "#status",
  data: {
    content: undefined,
    link: ""
  }
});
var query = undefined;

fetch('/query')
.then(function(response) {
   response.json().then(function(data) {
          query = data.map(x => x.toLowerCase());
          queryVue.query =  data.map(x => x.toLowerCase());
        });

});

var queryVue = new Vue({
  el: "#query",
  data: {
    query: []
  }
});

var hashGraphVideoVue = new Vue({
  el: "#hashGraphVideo",
  data: {
    result: []
  },
  methods: {
    play : function(data) {
        statusVue.content = data["status"]
        statusVue.link = data.media.displayURL;
        myPlayer.src(data["media"]["videoVariants"][0]["url"]);
        myPlayer.play();
    }
  }
 });
var playlistVue = new Vue({
  el: "#playlist",
  data: {
    before: [],
    current: [],
    after: []
  },
  methods: {
    generate: function(data, index) {
      this.after = Array.prototype.slice
        .call(data, index + 1, index + 10)
        .map( function(x,i) { return {index:i + index + 1,media:x["media"]}  } );
      this.current = Array.prototype.slice
        .call(data, index, index + 1)
        .map( function(x,i) { return {index:index,media:x["media"]}  } );
      this.before = Array.prototype.slice
        .call(data, Math.max(index - 5, 0), index)
        .map(function(x,i) { return {index:Math.max(index - 5, 0) + i,media:x["media"]}  } );
    },
    play : function(index) {

        indexPlaylist = index;
        var playnow = mediaPlaylist[indexPlaylist];

        statusVue.link = playnow.media.displayURL;

        statusVue.content = playnow["status"];

        myPlayer.src(playnow["media"]["videoVariants"][0]["url"]);
        myPlayer.play();

        playlistVue.generate(mediaPlaylist, indexPlaylist);
    }
  }
});
document.onkeydown = key_event;
// Skip on press Right Arrow
function key_event(e) {
  e = e || window.event;
  if (e.keyCode == "39") {
    next_video();
  }
  if (e.keyCode == "37") {
    previous_video();
  }
}
var mediaPlaylist = [];
var indexPlaylist = -1;
var playlistedMedia = new Set();
var myPlayer = videojs("my-video", {
  autoplay: false,
  preload: "auto"
})

var source = new EventSource("/stream/media");

source.addEventListener(
  "video",
  function(e) {
    var data = JSON.parse(e.data);
    console.log(data);
    if (!playlistedMedia.has(data["media"]["id"])) {
      mediaPlaylist.push(data);
      playlistedMedia.add(data["media"]["id"]);
      playlistVue.generate(mediaPlaylist, indexPlaylist);
      hashtags.updateList(

        data["status"]["hashtagEntities"].map(x => x["text"].toLowerCase()).filter(x=> !query.includes(x) )
      );
    } else {
    }
    if ( indexPlaylist==-1 || myPlayer.paused() && mediaPlaylist.length > indexPlaylist && myPlayer.currentTime() >= myPlayer.duration() ) {
      next_video();
    }
  },
  false
);

function next_video() {

  if (mediaPlaylist.length > indexPlaylist + 1) {

    indexPlaylist++;
    var play = mediaPlaylist[indexPlaylist];

    statusVue.link = play.media.displayURL;

    statusVue.content = play["status"];

    myPlayer.src(play["media"]["videoVariants"][0]["url"]);
    myPlayer.play();

    playlistVue.generate(mediaPlaylist, indexPlaylist);

  }
}
function previous_video() {
  if (0 < indexPlaylist) {

    indexPlaylist = Math.max(indexPlaylist - 1, 0);

    var play = mediaPlaylist[indexPlaylist];
    statusVue.link = play.media.displayURL;
    statusVue.content = play["status"];

    myPlayer.src(play["media"]["videoVariants"][0]["url"]);
    myPlayer.play();

    playlistVue.generate(mediaPlaylist, indexPlaylist);

  }
}

myPlayer.on("ended", function() {

  next_video();
});
myPlayer.on("error", function(e) {

  myPlayer.pause();
  next_video();
});
