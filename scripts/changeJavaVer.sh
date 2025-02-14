#!/bin/bash

javaVer=$1

update-alternatives --set java /usr/lib/jvm/java-$javaVer-openjdk-amd64/bin/java && \
export JAVA_HOME=/usr/lib/jvm/java-$javaVer-openjdk-amd64 && \
echo 'Switched to Java '$javaVer