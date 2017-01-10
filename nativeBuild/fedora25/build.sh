#!/bin/bash

# compile for ARMv7

set -e

mkdir -p /output/fedora/armv7/

# get sysroot

wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/j/java-1.8.0-openjdk-1.8.0.91-7.b14.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/j/java-1.8.0-openjdk-devel-1.8.0.91-7.b14.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/j/java-1.8.0-openjdk-headless-1.8.0.91-7.b14.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/s/systemd-libs-229-8.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/g/glibc-2.23.1-7.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/g/glibc-headers-2.23.1-7.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/g/glibc-devel-2.23.1-7.fc24.armv7hl.rpm
wget ftp://ftp.fau.de/fedora/linux/releases/24/Everything/armhfp/os/Packages/s/systemd-devel-229-8.fc24.armv7hl.rpm

# unpack sysroot

mkdir armv7
cd armv7

for i in $(ls ../*.rpm); do
  rpm2cpio "$i" | cpio -idmv  
done

# cross-compile in one step

JAVA_HOME=/armv7/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.91-7.b14.fc24.arm
cd /build/kura/org.eclipse.kura.linux.usb/src/main/c/udev
arm-linux-gnu-gcc -Wall --sysroot=/armv7 -fPIC -shared -L/armv7/lib -L/armv7/usr/lib LinuxUdev.c -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -o /output/fedora/armv7/libEurotechLinuxUdev.so -ludev

