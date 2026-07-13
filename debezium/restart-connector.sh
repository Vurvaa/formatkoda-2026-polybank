#!/usr/bin/env bash

set -euo pipefail

host="${1:-}"
connector_name="${2:-application-outbox-connector}"

if [[ -z "$host" ]]; then
    echo "usage: $0 <host> [connector_name]"
    echo "example: $0 http://localhost:8083"
    exit 1
fi

url="${host%/}/connectors/${connector_name}/restart?includeTasks=true&onlyFailed=false"

echo "restarting connector: ${connector_name}"

curl \
    --fail-with-body \
    --silent \
    --show-error \
    -X POST \
    "$url"

echo
echo "connector restart requested"

curl \
    --fail-with-body \
    --silent \
    --show-error \
    "${host%/}/connectors/${connector_name}/status" | jq

echo
