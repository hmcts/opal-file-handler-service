#!/usr/bin/env bash
# Keep JDBC assertions on the Jenkins agent connected to the PR database.
set -euo pipefail
state_dir="${WORKSPACE:-$PWD}/.functional-test-db-tunnel"

case "${1:-}" in
  start)
    namespace="${2:?namespace required}"
    pod="${3:?database pod required}"
    mkdir -p "$state_dir"
    if [[ -f "$state_dir/pid" ]]; then
      echo "Database tunnel state already exists; refusing to start a second tunnel" >&2
      exit 1
    fi
    kubectl --namespace "$namespace" wait --for=condition=Ready "pod/$pod" --timeout=120s >&2
    # Survive Jenkins sh-step cleanup; bounded lifetime also handles an aborted build.
    JENKINS_NODE_COOKIE="${BUILD_TAG:-functional}-db-tunnel" \
      nohup timeout 45m kubectl --namespace "$namespace" port-forward \
      --address 127.0.0.1 "pod/$pod" :5432 > "$state_dir/log" 2>&1 < /dev/null &
    tunnel_pid=$!
    echo "$tunnel_pid" > "$state_dir/pid"
    ready=false
    trap 'if [[ "$ready" != true ]]; then kill "$tunnel_pid" 2>/dev/null || true; rm -f "$state_dir/pid"; fi' EXIT
    for ((attempt = 0; attempt < 60; attempt++)); do
      port=$(sed -n 's/^Forwarding from 127\.0\.0\.1:\([0-9]*\) -> 5432$/\1/p' "$state_dir/log" | head -1)
      if [[ -n "$port" ]] && kill -0 "$tunnel_pid" 2>/dev/null; then
        ready=true
        printf '%s\n' "$port"
        exit 0
      fi
      if ! kill -0 "$tunnel_pid" 2>/dev/null; then
        cat "$state_dir/log" >&2
        exit 1
      fi
      sleep 1
    done
    cat "$state_dir/log" >&2
    echo "Timed out waiting for the functional-test database tunnel" >&2
    exit 1
    ;;
  stop)
    if [[ -f "$state_dir/pid" ]]; then
      read -r tunnel_pid < "$state_dir/pid"
      kill "$tunnel_pid" 2>/dev/null || true
      rm -f "$state_dir/pid"
    fi
    ;;
  *) echo "Usage: $0 start <namespace> <database-pod> | stop" >&2; exit 2 ;;
esac
