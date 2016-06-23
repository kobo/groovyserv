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

WORKDIR /usr/src/app
CMD ["./gradlew"]

