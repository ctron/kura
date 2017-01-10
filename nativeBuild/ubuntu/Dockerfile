FROM ubuntu:16.04

RUN dpkg --add-architecture i386
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
	make vim \
	gcc-multilib g++-multilib \
	openjdk-8-jdk libudev-dev \
	openjdk-8-jdk:i386  libudev-dev:i386

COPY ubuntu/build.sh /
RUN chmod a+x build.sh

COPY build/ /build

RUN mkdir /output

VOLUME ["/output"]

CMD ["/build.sh"]