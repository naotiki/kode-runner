FROM ubuntu:22.04

USER root

# 依存パッケージインストール
RUN apt update && apt install build-essential -y

# ユーザー設定
ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

# 初期ユーザー設定
USER $USERNAME
RUN mkdir /home/$USERNAME/work
WORKDIR /home/$USERNAME/work
CMD ["/bin/bash"]
