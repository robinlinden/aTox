FROM ubuntu:20.04

ENV DEBIAN_FRONTEND="noninteractive"

RUN apt-get update && apt-get -y install --no-install-recommends \
    autoconf \
    automake \
    cmake \
    cmake-data \
    curl \
    g++ \
    gcc \
    git \
    gnupg \
    libtool \
    make \
    openjdk-11-jdk-headless \
    patch \
    pkg-config \
    unzip \
    yasm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /work/

RUN curl -Lo sbt.tgz https://github.com/sbt/sbt/releases/download/v1.6.1/sbt-1.6.1.tgz \
    && tar xzf sbt.tgz --strip-components=1 -C /usr/local/ \
    && rm sbt.tgz

RUN curl -Lo ndk.zip https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip \
    && unzip -q -d /opt/ndk/ ndk.zip \
    && rm ndk.zip
ENV ANDROID_NDK_HOME=/opt/ndk/android-ndk-r21e

COPY scripts/ /work/scripts/
RUN ./scripts/build-host -j$(nproc) \
    && ./scripts/build-aarch64-linux-android -j$(nproc) release \
    && ./scripts/build-arm-linux-androideabi -j$(nproc) release \
    && ./scripts/build-i686-linux-android -j$(nproc) release \
    && ./scripts/build-x86_64-linux-android -j$(nproc) release
