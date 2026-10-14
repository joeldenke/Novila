#!/usr/bin/env bash
# Writes the tester release notes: every change that landed on main since the last
# build testers received, one line per PR, newest first.
#
#   scripts/release-notes.sh <head-sha> [previous-sha]
#
# previous-sha is the commit of the last successful distribution. When it is missing
# or not in the history of head-sha (first build, rewritten history), only the latest
# change is listed.
set -euo pipefail

HEAD_SHA="${1:?usage: $0 <head-sha> [previous-sha]}"
PREV_SHA="${2:-}"
MAX_LINES=20

# One line per first-parent commit on main. Merge commits ("Merge pull request #5 from …")
# are shown as their PR title plus number; squash merges already read that way.
changes() {
    git log --first-parent --format='%s%x1f%b%x1e' "$@" | awk '
        BEGIN { RS = "\036"; FS = "\037" }
        {
            subject = $1; body = $2
            sub(/^\n+/, "", subject)
            if (subject ~ /^Merge pull request #[0-9]+/) {
                number = subject
                sub(/^Merge pull request #/, "", number)
                sub(/ .*/, "", number)
                title = ""
                count = split(body, lines, "\n")
                for (i = 1; i <= count; i++) if (lines[i] ~ /[^ ]/) { title = lines[i]; break }
                if (title != "") print title " (#" number ")"
            } else if (subject !~ /^Merge (branch|remote-tracking branch) / && subject != "") {
                print subject
            }
        }'
}

if [ -n "$PREV_SHA" ] && [ "$PREV_SHA" = "$HEAD_SHA" ]; then
    heading="Rebuild, no new changes since the last build. Latest change:"
    list="$(changes -n 1 "$HEAD_SHA")"
elif [ -n "$PREV_SHA" ] && git merge-base --is-ancestor "$PREV_SHA" "$HEAD_SHA" 2>/dev/null; then
    list="$(changes "$PREV_SHA..$HEAD_SHA")"
    count="$(printf '%s\n' "$list" | grep -c . || true)"
    heading="Changes since the last build ($count):"
else
    heading="Latest change:"
    list="$(changes -n 1 "$HEAD_SHA")"
fi

printf '%s\n' "$heading"
printf '%s\n' "$list" | grep . | head -n "$MAX_LINES" | sed 's/^/• /'
total="$(printf '%s\n' "$list" | grep -c . || true)"
if [ "$total" -gt "$MAX_LINES" ]; then
    printf '…and %d more\n' "$((total - MAX_LINES))"
fi
printf '\nCommit %s\n' "$(git rev-parse --short=7 "$HEAD_SHA")"
