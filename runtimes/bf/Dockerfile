FROM ubuntu:22.04

USER root
RUN apt update && apt install bf -y

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

USER $USERNAME
WORKDIR /work/session
CMD ["/bin/bash"]
