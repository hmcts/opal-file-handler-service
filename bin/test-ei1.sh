#!/usr/bin/env bash
# Run the existing Cucumber suite against disposable local infrastructure.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
project="opal-ei1-$$"
compose=(docker compose -p "$project" -f docker-compose.ei1.yml)
cleanup() { "${compose[@]}" down --volumes --remove-orphans; }
trap cleanup EXIT
./gradlew --no-daemon bootJar
if ! "${compose[@]}" up -d --wait; then
  "${compose[@]}" logs
  exit 1
fi
port() {
  local mapped
  mapped=$("${compose[@]}" port "$1" "$2" | awk -F: '{print $NF}')
  [[ "$mapped" =~ ^[0-9]+$ ]] || { echo "No mapped port for $1" >&2; return 1; }
  echo "$mapped"
}
export FUNCTIONAL_TEST_DB_URL="jdbc:postgresql://127.0.0.1:$(port postgres 5432)/testdb"
export FUNCTIONAL_TEST_DB_USERNAME=test FUNCTIONAL_TEST_DB_PASSWORD=test
export FUNCTIONAL_TEST_BLOB_ENDPOINT="http://127.0.0.1:$(port azurite 10000)/devstoreaccount1"
export FUNCTIONAL_TEST_BLOB_ACCOUNT_NAME=devstoreaccount1
export FUNCTIONAL_TEST_BLOB_ACCOUNT_KEY='Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=='
export FUNCTIONAL_TEST_SFTP_HOST=127.0.0.1 FUNCTIONAL_TEST_SFTP_PORT="$(port sftp 22)"
export FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH="$PWD/src/integrationTest/resources/bais-emulator/keys/bais-sftp-key"
export FUNCTIONAL_TEST_SFTP_PRIVATE_KEY="$(cat "$FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH")"
export FUNCTIONAL_TEST_EI1_ISOLATED=true
# Bootstrap migrations with the real batch application before fixture hooks query the database.
mkdir -p build
base_env=(env -i "PATH=$PATH" "JAVA_HOME=$JAVA_HOME"
  "SPRING_DATASOURCE_URL=$FUNCTIONAL_TEST_DB_URL"
  SPRING_DATASOURCE_USERNAME=test SPRING_DATASOURCE_PASSWORD=test
  "FILE_STORE_STORAGE_URL=$FUNCTIONAL_TEST_BLOB_ENDPOINT"
  "FILE_STORE_STORAGE_ACCOUNT_NAME=$FUNCTIONAL_TEST_BLOB_ACCOUNT_NAME"
  "FILE_STORE_STORAGE_KEY=$FUNCTIONAL_TEST_BLOB_ACCOUNT_KEY"
  BAIS_SFTP_CONNECTION_HOST=127.0.0.1 "BAIS_SFTP_CONNECTION_PORT=$FUNCTIONAL_TEST_SFTP_PORT"
  BAIS_SFTP_CAPS_REPORT_USERNAME=CAPS-report
  "BAIS_SFTP_PRIVATE_KEY=$(cat "$FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH")"
  LAUNCH_DARKLY_ENABLED=false RELEASE_1C_BANKING_INTERFACES_ENABLED=true
  CAPS_REPORT_FILE_TRANSFER_JOB_ENABLED=true RUN_DB_MIGRATION_ON_STARTUP=true)
if ! "${base_env[@]}" "$JAVA_HOME/bin/java" -jar build/libs/opal-file-handler-service.jar AutomatedTask:CAPSReport > build/ei1-bootstrap.log 2>&1; then
  cat build/ei1-bootstrap.log
  exit 1
fi
./gradlew --no-daemon functional "-Dcucumber.filter.tags=@EI1 and (${1:-@EI1})" -Pei1
