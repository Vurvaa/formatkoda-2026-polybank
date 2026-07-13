#!/usr/bin/env bash

set -euo pipefail

host="${1:-}"
config_file="${2:-config.json}"

if [[ -z "$host" ]]; then
    echo "usage: $0 <host> [config_file]"
    echo "example: $0 http://localhost:8083"
    exit 1
fi

if [[ ! -f "$config_file" ]]; then
    echo "config file not found: $config_file"
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "jq is required"
    exit 1
fi

connector_name="$(jq -er '.name' "$config_file")"

jq -e '.config' "$config_file" |
    curl --fail-with-body \
        --silent \
        --show-error \
        -X PUT \
        -H 'Content-Type: application/json' \
        --data-binary @- \
        "${host%/}/connectors/${connector_name}/config" | jq

echo
