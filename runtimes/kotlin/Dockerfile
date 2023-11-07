FROM eclipse-temurin:17-jdk

ARG KOTLIN_COMPILER_URL="https://github.com/JetBrains/kotlin/releases/download/v1.9.10/kotlin-compiler-1.9.10.zip"

USER root
RUN apt update && apt install curl wget zip unzip -y

ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

RUN mkdir /work && \
    mkdir /work/tools && \
    mkdir -m 777 /work/session/ && \
    chmod +t /work/session
WORKDIR /work/tools

RUN wget $KOTLIN_COMPILER_URL -O kotlin.zip && \
    unzip kotlin.zip
ENV PATH $PATH:/work/tools/kotlinc/bin

USER $USERNAME
WORKDIR /work/session
CMD ["/bin/bash"]
