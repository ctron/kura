FROM fedora:24

MAINTAINER Jens Reimann <jreimann@redhat.com>

RUN dnf -y install git java-1.8.0-openjdk-devel maven procps

RUN git clone https://github.com/ctron/kura.git -b feature/karaf

RUN ls /kura
RUN cd /kura && ./build-all.sh -Pspeedup
RUN cd /kura/karaf && mvn clean install
RUN cd /kura/karaf/deployment/fragments && mvn clean install
RUN cd /kura/karaf/deployment/targets/docker-emulator-fedora && mvn clean install
RUN dnf install -y /kura/karaf/deployment/targets/docker-emulator-fedora/target/*.rpm

EXPOSE 8181
ENV JAVA_HOME=/usr/lib/jvm/jre-1.8.0

CMD /opt/eclipse/kura/bin/karaf console