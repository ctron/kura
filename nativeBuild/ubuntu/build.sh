#!/bin/bash

set -e

cd build/kura/org.eclipse.kura.linux.usb/src/main/c/udev

mkdir -p /output/ubuntu/x86_64
mkdir -p /output/ubuntu/x86

make clean
make JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 CC="gcc -m64"
cp libEurotechLinuxUdev.so /output/ubuntu/x86_64/

make clean
make JAVA_HOME=/usr/lib/jvm/java-8-openjdk-i386 CC="gcc -m32"
cp libEurotechLinuxUdev.so /output/ubuntu/x86/
