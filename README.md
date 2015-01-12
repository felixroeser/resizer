## resizer

an _on the fly_ image resizer build with [spray.io](http://spray.io)

### Getting started

1. Git-clone this repository.

        $ git clone git@bitbucket.org:/resizer.git

2. Change directory into your clone:

        $ cd resizer

3. Launch SBT:

        $ sbt

4. Compile everything and run all tests:

        > test

5. Start the application:

        > re-start
        # with custom configuration
        > sbt run -Dconfig.file=example.conf

6. Browse to [http://localhost:8080](http://localhost:8080/)

7. Stop the application:

        > re-stop

### Prod

    sbt run -Dconfig.file=prod.conf

### Links

* https://github.com/sbt/sbt-native-packager
