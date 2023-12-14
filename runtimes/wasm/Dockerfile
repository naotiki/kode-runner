FROM ubuntu:22.04

USER root

# 依存パッケージインストール
RUN apt update && apt install curl xz-utils -y

# ユーザー設定
ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME



USER $USERNAME
ENV HOME /home/$USERNAME
WORKDIR $HOME

# インストール
RUN curl https://wasmtime.dev/install.sh -sSf | bash
ENV WASMTIME_HOME $HOME/.wasmtime
ENV PATH $WASMTIME_HOME/bin:$PATH
# 初期ユーザー設定
RUN mkdir $HOME/work
WORKDIR $HOME/work
CMD ["/bin/bash"]
