FROM fedora:24

MAINTAINER Jens Reimann <jreimann@redhat.com>

RUN dnf -y install git java-1.8.0-openjdk-devel maven procps

COPY . /build
RUN ls /build
RUN cd /build && ./build-all.sh -Pspeedup
RUN find /root/.m2/repository/org/eclipse/kura
RUN cd /build/karaf && mvn clean install
RUN cd /build/karaf/deployment/fragments && mvn clean install
RUN cd /build/karaf/deployment/targets/docker-emulator-fedora && mvn clean install
RUN dnf install -y /build/karaf/deployment/targets/docker-emulator-fedora/target/*.rpm

EXPOSE 8181
ENV JAVA_HOME=/usr/lib/jvm/jre-1.8.0

CMD /opt/eclipse/kura/bin/karaf console