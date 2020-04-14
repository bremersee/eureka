#!/usr/bin/env sh
docker service create \
  --replicas 1 \
  --name eureka3 \
  --hostname eureka3 \
  --network proxy \
  --publish 8763:8763 \
  --secret config-server-client-user-password \
  --restart-delay 10s \
  --restart-max-attempts 10 \
  --restart-window 60s \
  --update-delay 10s \
  --constraint 'node.labels.eureka==3' \
  -e APPLICATION_NAME='eureka' \
  -e ACTIVE_PROFILES=$2 \
  -e CONFIG_CLIENT_ENABLED='true' \
  -e CONFIG_URI='http://config-server:8888' \
  -e CONFIG_USER='configclient' \
  -e CONFIG_PASSWORD_FILE='/run/secrets/config-server-client-user-password' \
  -e CONFIG_CLIENT_FAIL_FAST='true' \
  -e CONFIG_RETRY_INIT_INTERVAL='3000' \
  -e CONFIG_RETRY_MAX_INTERVAL='4000' \
  -e CONFIG_RETRY_MAX_ATTEMPTS='8' \
  -e CONFIG_RETRY_MULTIPLIER='1.1' \
  $1