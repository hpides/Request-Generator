FROM maven:3 as build
WORKDIR /app
COPY src ./src
COPY *.xml .
RUN mvn package -Dmaven.test.skip=true

#newest LTS version to support Java 13 and Java 7
FROM ubuntu:18.04

#new Java for request-generator, software-properties-common for apt-add-repository, gnupg2 for en
RUN apt update && apt install -y software-properties-common gnupg2

#ppa that contains openjdk-packages
RUN echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu xenial main" >> /etc/apt/sources.list

#libpng-12 is only available from this mirror, and openjdk depends on it
RUN echo "deb http://security.ubuntu.com/ubuntu xenial-security main" >> /etc/apt/sources.list

#key for the ppa
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys DA1A4A13543B466853BAF164EB9B1D8886F44E2A

RUN apt update   

RUN apt install -y libpng12-0 openjdk-7-jre openjdk-13-jre 
  
RUN dpkg -L openjdk-7-jre

RUN dpkg -L openjdk-13-jre

RUN ln -s /usr/lib/jvm/java-7-openjdk-amd64/bin/java /usr/bin/java7 

RUN ln -s /usr/lib/jvm/java-13-openjdk-amd64/bin/java /usr/bin/java13 

RUN java7 -version

RUN java13 -version

COPY --from=build /app/target/request-generator-1.0-SNAPSHOT.jar .

CMD ["java13","-jar", "request-generator-1.0-SNAPSHOT.jar","/pdgf", "java7"]
