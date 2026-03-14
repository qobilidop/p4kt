FROM ubuntu:24.04

ARG TARGETARCH

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-${TARGETARCH}

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        curl \
        git \
        openjdk-21-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

ARG BAZELISK_VERSION=v1.28.1
RUN curl -fsSL "https://github.com/bazelbuild/bazelisk/releases/download/${BAZELISK_VERSION}/bazelisk-linux-${TARGETARCH}" \
        -o /usr/local/bin/bazel \
    && chmod +x /usr/local/bin/bazel

USER ubuntu
