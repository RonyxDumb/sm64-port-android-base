FROM ubuntu:18.04

ENV DEBIAN_FRONTEND=noninteractive

# ------------------------------------------------------------
# Base dependencies
# ------------------------------------------------------------

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        android-sdk \
        binutils \
        bsdmainutils \
        build-essential \
        ca-certificates \
        git \
        libaudiofile-dev \
        libglew-dev \
        libsdl2-dev \
        libusb-1.0-0-dev \
        libzstd-dev \
        openjdk-8-jdk \
        python3 \
        unzip \
        wget && \
    rm -rf /var/lib/apt/lists/*

# ------------------------------------------------------------
# Android SDK
# ------------------------------------------------------------

ENV ANDROID_HOME=/usr/lib/android-sdk
ENV ANDROID_SDK_ROOT=/usr/lib/android-sdk

ENV PATH=${ANDROID_HOME}/cmdline-tools/tools/bin:${ANDROID_HOME}/platform-tools:${PATH}

# ------------------------------------------------------------
# Android command-line tools
# ------------------------------------------------------------

RUN wget -q \
    https://dl.google.com/android/repository/commandlinetools-linux-6609375_latest.zip \
    -O /tmp/android-commandline-tools.zip && \
    echo "89f308315e041c93a37a79e0627c47f21d5c5edbe5e80ea8dc0aac8a649e0e92  /tmp/android-commandline-tools.zip" \
        | sha256sum -c - && \
    mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    unzip -q /tmp/android-commandline-tools.zip \
        -d ${ANDROID_HOME}/cmdline-tools && \
    rm /tmp/android-commandline-tools.zip

# ------------------------------------------------------------
# Android licenses
# ------------------------------------------------------------

RUN yes | sdkmanager --licenses

# ------------------------------------------------------------
# Android SDK / Build Tools / NDK
#
# Project:
#   compileSdkVersion 28
#   targetSdkVersion 28
#   APP_PLATFORM android-21
# ------------------------------------------------------------

RUN sdkmanager \
    "platform-tools" \
    "platforms;android-28" \
    "build-tools;28.0.3" \
    "ndk;21.4.7075529"

# ------------------------------------------------------------
# NDK
# ------------------------------------------------------------

ENV ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/21.4.7075529
ENV ANDROID_NDK_ROOT=${ANDROID_HOME}/ndk/21.4.7075529

# Compatibility with older Android Gradle Plugin projects
RUN ln -sfn \
    ${ANDROID_HOME}/ndk/21.4.7075529 \
    ${ANDROID_HOME}/ndk-bundle

# ------------------------------------------------------------
# Project
# ------------------------------------------------------------

WORKDIR /sm64

COPY . /sm64

RUN chmod +x /sm64/entrypoint.sh

CMD ["./entrypoint.sh"]
