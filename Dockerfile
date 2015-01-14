# VERSION 1.0

# the base image is a trusted ubuntu build with java 8 (https://index.docker.io/u/dockerfile/java/)
FROM dockerfile/java:oracle-java8

MAINTAINER Felix Roser, fr@xilef.me

# we need this because the workdir is modified in dockerfile/java
WORKDIR /

# run the (java) server as the daemon user
USER daemon

# copy the locally built fat-jar to the image
ADD target/scala-2.11/resizer.jar /app/resizer.jar

# run the server when a container based on this image is being run
ENTRYPOINT [ "java", "-jar", "/app/resizer.jar" ]

# the server binds to 8080 - expose that port
EXPOSE 8080
