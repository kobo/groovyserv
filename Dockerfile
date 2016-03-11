#
# Dockerfile for testing
#
# Usage:
#
#  $ docker build -t kobo/groovyserv .
#  $ docker run -it --rm -v $PWD:/usr/src/app -v $HOME/.gradle:/root/.gradle -v $HOME/.m2:/root/.m2 kobo/groovyserv
#
#  or just
#
#  $ docker-compose up
#
FROM java:8

# APT
RUN apt-get update && \
    apt-get install -y --no-install-recommends golang curl locales

# Locale
RUN echo 'en_US UTF-8' >> /etc/locale.gen && \
    locale-gen
ENV LANG en_US.UTF-8

# SDKMAN: Groovy
RUN curl -s get.sdkman.io | bash && \
    bash -lc "sdk install groovy"
ENV PATH $PATH:/root/.sdkman/candidates/groovy/current/bin

# Copy all here. Or you can replace it with PWD at runtime:
COPY . /usr/src/app
WORKDIR /usr/src/app

CMD ["./gradlew"]

