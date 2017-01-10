#!/bin/bash

set -e

cd build/kura/org.eclipse.kura.linux.usb/src/main/c/udev
make JAVA_HOME=/usr/lib/jvm/java-openjdk/
mkdir -p /output/rhel7/x86_64
cp libEurotechLinuxUdev.so /output/rhel7/x86_64/
