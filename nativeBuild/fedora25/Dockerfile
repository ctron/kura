FROM fedora:25

RUN dnf install -y \
	wget cpio make \
	gcc-arm-linux-gnu

COPY build/ /build

RUN mkdir /output
VOLUME ["/output"]

COPY fedora25/build.sh /
RUN chmod a+x build.sh

CMD ["/build.sh"]