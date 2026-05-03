#!/bin/sh
set -e

if [ -S /var/run/docker.sock ]; then
  DOCKER_GID="$(stat -c '%g' /var/run/docker.sock)"

  if ! getent group "${DOCKER_GID}" >/dev/null; then
    groupadd -g "${DOCKER_GID}" dockerhost
  fi

  DOCKER_GROUP="$(getent group "${DOCKER_GID}" | cut -d: -f1)"
  usermod -aG "${DOCKER_GROUP}" jenkins
fi

if [ "$1" = "test-docker" ]; then
  exec gosu jenkins docker version
fi

exec gosu jenkins /usr/local/bin/jenkins-agent "$@"
