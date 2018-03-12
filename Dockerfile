FROM golang:1.9.4-stretch

ENV JAVA_VERSION 8u161-oracle
ENV GROOVY_VERSION 2.4.14
ENV GOLANG_VERSION 1.9.4

# Prepare environment
ENV JAVA_HOME /opt/java
ENV PATH $PATH:$JAVA_HOME/bin

# SDKMAN: Java / Groovy
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip zip && \
    curl -s "https://get.sdkman.io" | bash && \
    /bin/bash -lc "sdk install java $JAVA_VERSION" && \
    /bin/bash -lc "sdk install groovy $GROOVY_VERSION"
ENV JAVA_HOME /root/.sdkman/candidates/java/current
ENV GROOVY_HOME /root/.sdkman/candidates/groovy/current
ENV PATH $PATH:$GROOVY_HOME/bin:$JAVA_HOME/bin

WORKDIR /usr/src/app
CMD ["./gradlew"]
