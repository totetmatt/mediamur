# Mediamur
## Disclamer
The purpose of this tools is to ease the visualization of media (photo, video) in twitter in real time.

Mediamur is reflecting the video activity of twitter users. It might contains unadapted contents for youth.

The tool doesn't have any control on what is send over Twitter. As **no** filter is applied, the results can contain sensitive, choking, or disturbing media.
Please be aware and use the tool in the correct place with the adequate participants.

## Twitter API
You will need to create a Twitter API Application Key https://developer.twitter.com/en/apps.

## Quickstart
* Download here
* Unzip somewhere in your disk
* Edit the `application.conf` 
*   ```yaml
      # Twitter App info, if oyu don't have go to https://developer.twitter.com/en/apps and create an app.
      # If you already have one (like for Twitter Streaming Importer) you can reuse it, but you won't be able to use the 2
      # scripts it at the same time
      twitter {
        consumer_key = " "
        consumer_secret = " "
        access_token = " "
        access_token_secret = " "
      }
      # What are the words mediamur should track in real time
      query {
        track = ["MurDeBerlin"]
      }
    
      # Use the sample stream, which is a sample of all tweet in real time
      sampleStream = true
    
      # Should it save all the tweet and media
      recordActive = false
    ```
    
* With a console, go to `cd $PATH_WHERE_YOU_UNZIP/mediamur-0.1/` and run `bin/mediamur` (or windows `bin\mediamur.bat`) 
* Open a web browser to `http://localhost:48099/`
## Example
### Media Flow
https://youtu.be/4LMWtduAhgE

Each new media is append on the top. You can click to see it.
### Media Stack
https://youtu.be/Qe5Kyz_bzfs

Each media is getting stack per number of time it has been seen.
### TV & Hashcloud
https://youtu.be/6phDxLYHZ9k

Will play automatically each video 1 by 1 by date Mediamur receive it and make a hashtag co-occurence cloud. 
Each hashtag can be clicked to display video under this hashtag.

## Configuration
### query
The `track` represent the set of words that twitter will fetch as stream. It could be just words, hashtags etc...
### recordActive
If set to `true`, the application will store all media and tweets in a directory.
### sampleStream
If set to `true` and if the `query.track` is empty, it will use the twitter sample stream endpoint, which represent a random set 
of tweets.