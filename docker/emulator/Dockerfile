FROM fedora:24

MAINTAINER Jens Reimann <jreimann@redhat.com>

RUN dnf -y install git java-1.8.0-openjdk-devel maven procps && dnf -y clean all
ENV JAVA_HOME=/usr/lib/jvm/jre-1.8.0

RUN git clone https://github.com/ctron/kura.git -b feature/karaf

RUN ls /kura && \
    cd /kura && ./build-all.sh -Pspeedup && \
    cd /kura/karaf && mvn clean install && \
    cd /kura/karaf/deployment/fragments && mvn clean install && \
    cd /kura/karaf/deployment/targets/docker-emulator-fedora && mvn clean install && \
    dnf install -y /kura/karaf/deployment/targets/docker-emulator-fedora/target/*.rpm && \
    rm -Rf /kura

EXPOSE 8181

CMD /opt/eclipse/kura/bin/karaf console