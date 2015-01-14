## resizer

* an _on the fly_ image resizer build with [spray.io](http://spray.io)
* without any database dependencies
* just using the filesystem for caching (use a distributed filesystem for a multi instance setup)
* works only with whitelisted domains / urls

### API

#### GET /:realm/i/:options/:urlsnippet

Proxy an image for a whitelisted domain identified with :realmid
by replacing {{url}} in the target url with :urlsnippet

Options - comma separated:

* MAXDIMENSION - will scale the image to fit either h or w the requested MAXDIMENSION
* HxW - will fit the image to the requestes h and w. The background will be filled
* q75 - Jpeg compression quality 1-99 - defaults to 75
* c100-0-255 - define the background color use the fill when fitting the image - defaults to white

#### DELETE /:realm/i/:urlsnippet

Will purge all cached files (original and resized ones) related
to the _urlsnippet_

### Getting started

1. Git-clone this repository.

        $ git clone git@bitbucket.org:/resizer.git

2. Change directory into your clone:

        $ cd resizer

3. Launch SBT:

        $ sbt

4. Compile everything and run all tests (just kidding - no tests there yet):

        > test

5. Configuration

Copy contrib/example.conf to something like /etc/resizer.conf and open it in your favorite editor.

6. Start the application:

        # with custom configuration
        export CONFIG=PATH_TO_CONFIG
        > re-start

7. Browse to [http://localhost:8080](http://localhost:8080/)

8. Stop the application:

        > re-stop

### Prod

    sbt assembly
    cd target/scala-2.11
    CONFIG=/etc/resizer.conf java -jar resizer.jar

### Docker - WIP

    sbt assembly
    docker build --tag=octojon/resizer .
    docker run -p 127.0.0.1:8080:8080 ...

### Random notes / Links

* https://github.com/sbt/sbt-native-packager

### Issues / Limitations

* You can proxy the original image - the image is always being processed
* Resizing is slow and memory hungry => better threadpool configuration is needed
* Better purge unused image as _resizer_ has no build in time to live or any other access stats
* No real dos protection - maybe use a signed token to validate the options
* Setting a maximum image size is not possible yet - use could scale a 1x1 image to infinity and beyond
* Reads many formats but serves only jpeg without a seo friendly filename

### TODO

[ ] Write tests!
[ ] Serve better error messages
[ ] Write some basic meta data to a yml file on image creation
[ ] Use a proper logger - avoid println
[ ] Profiling
[ ] Add Newrelic support
[ ] Add Airbrake support
