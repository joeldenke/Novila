#!/bin/bash
# Starts the Firebase MCP server. Prefers a preinstalled firebase-tools (installed by the cloud
# environment's setup script) because `npx` downloads take longer than the MCP connect timeout.
VERSION=15.33.0
if command -v firebase >/dev/null 2>&1; then
  exec firebase mcp --dir "${CLAUDE_PROJECT_DIR:-.}"
fi
exec npx -y "firebase-tools@${VERSION}" mcp --dir "${CLAUDE_PROJECT_DIR:-.}"
