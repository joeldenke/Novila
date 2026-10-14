#!/bin/bash
# Prepares Google Cloud credentials for the agent in cloud sessions.
#
# The cloud environment provides NOVILA_GCP_AGENT_KEY (a JSON key for the least-privileged
# "claude-agent" service account). This writes it to a private file that gcloud, the Firebase
# CLI and the Firebase MCP server (.mcp.json) all read via Application Default Credentials.
# The key is never committed and never printed.
set -euo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
[ -n "${NOVILA_GCP_AGENT_KEY:-}" ] || exit 0

dir="$HOME/.config/novila"
key="$dir/gcp-agent.json"
mkdir -p "$dir"
chmod 700 "$dir"
umask 077
printf '%s' "$NOVILA_GCP_AGENT_KEY" > "$key"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export GOOGLE_APPLICATION_CREDENTIALS=\"$key\""
    echo "export CLOUDSDK_AUTH_CREDENTIAL_FILE_OVERRIDE=\"$key\""
    if [ -n "${NOVILA_GCP_PROJECT:-}" ]; then
      echo "export CLOUDSDK_CORE_PROJECT=\"$NOVILA_GCP_PROJECT\""
    fi
  } >> "$CLAUDE_ENV_FILE"
fi
exit 0
