FROM ubuntu:12.04

ENV JAVA_VERSION 8u92
ENV JAVA_BUILD_VERSION b14
ENV GROOVY_VERSION 2.4.7
ENV GOLANG_VERSION 1.6.3

# Prepare environment
ENV JAVA_HOME /opt/java
ENV PATH $PATH:$JAVA_HOME/bin

# APT
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl locales wget unzip

# Locale
RUN echo 'en_US UTF-8' >> /etc/locale.gen && \
    locale-gen
ENV LANG en_US.UTF-8

# Install Oracle Java
RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION}-${JAVA_BUILD_VERSION}/jdk-${JAVA_VERSION}-linux-x64.tar.gz && \
    tar -xvf jdk-${JAVA_VERSION}-linux-x64.tar.gz && \
    rm jdk*.tar.gz && \
    mv jdk* ${JAVA_HOME}

# SDKMAN: Groovy
RUN curl -s get.sdkman.io | bash
RUN bash -lc "sdk install groovy $GROOVY_VERSION"
ENV PATH $PATH:/root/.sdkman/candidates/groovy/current/bin

# Install Go
RUN apt-get update && apt-get install -y --no-install-recommends \
        g++ \
        gcc \
        libc6-dev \
        make \
    && rm -rf /var/lib/apt/lists/*
ENV GOLANG_DOWNLOAD_URL https://golang.org/dl/go$GOLANG_VERSION.linux-amd64.tar.gz
ENV GOLANG_DOWNLOAD_SHA256 cdde5e08530c0579255d6153b08fdb3b8e47caabbe717bc7bcd7561275a87aeb
RUN curl -fsSL "$GOLANG_DOWNLOAD_URL" -o golang.tar.gz \
    && echo "$GOLANG_DOWNLOAD_SHA256  golang.tar.gz" | sha256sum -c - \
    && tar -C /usr/local -xzf golang.tar.gz \
    && rm golang.tar.gz
ENV GOPATH /go
ENV PATH $GOPATH/bin:/usr/local/go/bin:$PATH
RUN mkdir -p "$GOPATH/src" "$GOPATH/bin" && chmod -R 777 "$GOPATH"

WORKDIR /usr/src/app
CMD ["./gradlew"]
