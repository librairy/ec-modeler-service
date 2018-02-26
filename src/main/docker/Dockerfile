FROM anapsix/alpine-java:8u162b12_server-jre
VOLUME /tmp
ADD datos-gob-model.jar /app.jar
ADD bin /bin
ENTRYPOINT exec java $JAVA_OPTS -server -jar /app.jar
