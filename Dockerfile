FROM ubuntu:latest

# Install required packages
RUN apt-get update && \
    apt-get install -y nano openjdk-17-jdk openjdk-21-jdk && \
    apt-get clean

# Set up environment variables for Java versions
ENV JAVA17_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV JAVA21_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV JAVA_HOME=$JAVA21_HOME 

WORKDIR /app

# Create file system
RUN mkdir -p /app/Qilin
RUN mkdir -p /app/Tai-E
RUN mkdir -p /app/TestEval/out
RUN mkdir -p /app/WALA
RUN mkdir /app/volume

# Copy files
COPY ./QilinAnalysis/src/main/resources/QilinAnalysis-fat.jar /app/Qilin/QilinAnalysis-fat.jar
COPY ./Tai-e/src/main/resources/tai-e-all-0.5.1-SNAPSHOT.jar /app/Tai-E/tai-e-all-0.5.1-SNAPSHOT.jar
COPY ./WalaTest/src/main/resources/walatest.jar /app/WALA/walatest.jar
COPY ./TestEvaluation/src/main/resources/TestEvaluation.jar /app/TestEval/TestEvaluation.jar
COPY ./resources /app/resources

COPY ./scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY ./scripts/changeJavaVer.sh /usr/local/bin/changeJavaVer
RUN chmod +x /usr/local/bin/changeJavaVer


#execute analysis on java 21
RUN java -jar /app/Qilin/QilinAnalysis-fat.jar -tj ./resources/tempJson -cc ./resources/TestCases/CompiledClasses -p testSourceCode -jr ./resources/TestCases/PolyglotTest.jar -jre ./resources/javaJRE/jre-1.8  -o ./resources/resultJsons/qilin_out.json
RUN java -jar /app/WALA/walatest.jar -t ./resources/TestCases/CompiledClasses -e ./resources/exclusions.txt -o ./resources/resultJsons/wala_out.json

RUN changeJavaVer 17

RUN java -jar /app/Tai-E/tai-e-all-0.5.1-SNAPSHOT.jar -tj ./resources/tempJson -cc ./resources/TestCases/CompiledClasses -p testSourceCode -jr ./resources/TestCases/PolyglotTest.jar -o ./resources/resultJsons/taie_out.json
RUN java -jar /app/TestEval/TestEvaluation.jar -j ./resources/resultJsons/taie_out.json,./resources/resultJsons/qilin_out.json,./resources/resultJsons/wala_out.json -i ./resources/TestCases/TestCasesJson/testCases.json -l ./resources/valueMethods.txt -o /app/TestEval/out

VOLUME /app/volume

ENTRYPOINT ["/entrypoint.sh"]
CMD ["/bin/bash"]
