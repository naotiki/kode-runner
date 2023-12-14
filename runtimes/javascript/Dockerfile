FROM node:18-slim

USER root

ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1001
ARG GID=1001
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

# 初期ユーザー設定
USER $USERNAME
WORKDIR /home/$USERNAME
RUN mkdir /home/$USERNAME/work
WORKDIR /home/$USERNAME/work
CMD ["/bin/bash"]
