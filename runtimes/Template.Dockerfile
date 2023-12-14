FROM eclipse-temurin:17-jdk
# 変数

USER root

# 依存パッケージインストール

# ユーザー設定
ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

# その他実行環境インストールなど(Option)

# 初期ユーザー設定
USER $USERNAME
RUN mkdir /home/$USERNAME/work
WORKDIR /home/$USERNAME/work
CMD ["/bin/bash"]
