FROM centos:7

RUN yum install -y java-1.8.0-openjdk-devel make gcc g++ systemd-devel

COPY rhel7/build.sh /
RUN chmod a+x build.sh

COPY build/ /build

RUN mkdir /output

VOLUME ["/output"]

CMD ["/build.sh"]