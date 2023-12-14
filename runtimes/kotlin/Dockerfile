FROM eclipse-temurin:17-jdk
# 変数
ARG KOTLIN_COMPILER_URL="https://github.com/JetBrains/kotlin/releases/download/v1.9.10/kotlin-compiler-1.9.10.zip"

USER root

# 依存パッケージインストール
RUN apt update && apt install wget zip unzip -y

# ユーザー設定
ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

# その他実行環境インストールなど(Option)
WORKDIR /opt
RUN wget $KOTLIN_COMPILER_URL -O kotlin.zip && \
    unzip kotlin.zip
ENV PATH $PATH:/opt/kotlinc/bin

# 初期ユーザー設定
USER $USERNAME
RUN mkdir /home/$USERNAME/work
WORKDIR /home/$USERNAME/work
CMD ["/bin/bash"]
