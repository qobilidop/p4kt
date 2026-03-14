FROM ubuntu:24.04

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ARG TARGETARCH

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-${TARGETARCH}

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        curl \
        git \
        openjdk-21-jdk-headless \
        python3-pip \
        shellcheck \
    && rm -rf /var/lib/apt/lists/* \
    && pip3 install --no-cache-dir --break-system-packages mdformat==0.7.21 yamllint==1.37.0

ARG BAZELISK_VERSION=v1.28.1
RUN curl -fsSL "https://github.com/bazelbuild/bazelisk/releases/download/${BAZELISK_VERSION}/bazelisk-linux-${TARGETARCH}" \
        -o /usr/local/bin/bazel \
    && chmod +x /usr/local/bin/bazel

ARG KTFMT_VERSION=0.54
RUN curl -fsSL "https://github.com/facebook/ktfmt/releases/download/v${KTFMT_VERSION}/ktfmt-${KTFMT_VERSION}-jar-with-dependencies.jar" \
        -o /usr/local/lib/ktfmt.jar

ARG BUILDIFIER_VERSION=8.0.3
RUN curl -fsSL "https://github.com/bazelbuild/buildtools/releases/download/v${BUILDIFIER_VERSION}/buildifier-linux-${TARGETARCH}" \
        -o /usr/local/bin/buildifier \
    && chmod +x /usr/local/bin/buildifier

ARG SHFMT_VERSION=3.11.0
RUN curl -fsSL "https://github.com/mvdan/sh/releases/download/v${SHFMT_VERSION}/shfmt_v${SHFMT_VERSION}_linux_${TARGETARCH}" \
        -o /usr/local/bin/shfmt \
    && chmod +x /usr/local/bin/shfmt

ARG DETEKT_VERSION=1.23.8
RUN curl -fsSL "https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}-all.jar" \
        -o /usr/local/lib/detekt.jar

ARG YAMLFMT_VERSION=0.21.0
RUN YAMLFMT_ARCH=$([ "$TARGETARCH" = "arm64" ] && echo "arm64" || echo "x86_64") \
    && curl -fsSL "https://github.com/google/yamlfmt/releases/download/v${YAMLFMT_VERSION}/yamlfmt_${YAMLFMT_VERSION}_Linux_${YAMLFMT_ARCH}.tar.gz" \
        | tar xz -C /usr/local/bin yamlfmt

ARG HADOLINT_VERSION=2.14.0
RUN HADOLINT_ARCH=$([ "$TARGETARCH" = "arm64" ] && echo "arm64" || echo "x86_64") \
    && curl -fsSL "https://github.com/hadolint/hadolint/releases/download/v${HADOLINT_VERSION}/hadolint-linux-${HADOLINT_ARCH}" \
        -o /usr/local/bin/hadolint \
    && chmod +x /usr/local/bin/hadolint

USER ubuntu
