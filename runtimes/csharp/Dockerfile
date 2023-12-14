FROM mcr.microsoft.com/dotnet/sdk:7.0-bookworm-slim
USER root

ARG USERNAME=runner
ARG GROUPNAME=runner
ARG UID=1000
ARG GID=1000
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME

USER $USERNAME
RUN mkdir /home/$USERNAME/work
WORKDIR /home/$USERNAME/work
RUN dotnet new console
CMD ["/bin/bash"]
