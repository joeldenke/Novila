#!/usr/bin/env bash
# Bootstraps Google Cloud and Firebase for Novila. Safe to run again: every step
# checks what already exists and only creates what is missing.
#
# Easiest from a phone: open Google Cloud Shell (already logged in, has gcloud and jq),
# clone the repo and run:
#
#   ./scripts/bootstrap-firebase.sh <project-id> [tester-email,...]
#
# It sets up:
#   1. The Google Cloud project (created if missing) and the APIs it needs
#   2. Firebase on that project and an Android app for se.novila.debug
#   3. The App Distribution group "testare" with the given testers (default: you)
#   4. A service account that may only distribute builds
#   5. Workload Identity Federation so the "Distribute to testers" workflow in
#      joeldenke/Novila (environment "firebase") can act as that service account
#      without any JSON key
#
# Finally it sets the GitHub Actions variables with gh, or prints them if gh is not logged in.
set -euo pipefail

PROJECT_ID="${1:-${PROJECT_ID:-}}"
TESTER_EMAILS="${2:-${TESTER_EMAILS:-}}"

GITHUB_REPO="joeldenke/Novila"
# Numeric repo id survives renames and transfers, unlike the name.
GITHUB_REPO_ID="1419684243"
GITHUB_ENVIRONMENT="firebase"
ANDROID_PACKAGE="se.novila.debug"
ANDROID_DISPLAY_NAME="Novila (debug)"
TESTER_GROUP="testare"
POOL_ID="github"
PROVIDER_ID="novila"
SA_ID="github-distribute"

FIREBASE_API="https://firebase.googleapis.com/v1beta1"
APPDIST_API="https://firebaseappdistribution.googleapis.com/v1"

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[ -n "$PROJECT_ID" ] || die "usage: $0 <project-id> [tester-email,...]  (project id is globally unique, e.g. novila-joel)"
command -v gcloud >/dev/null || die "gcloud is missing (Cloud Shell has it)"
command -v jq >/dev/null || die "jq is missing (Cloud Shell has it)"
command -v curl >/dev/null || die "curl is missing"

ACCOUNT="$(gcloud config get-value account 2>/dev/null || true)"
[ -n "$ACCOUNT" ] || die "not logged in, run: gcloud auth login"
TESTER_EMAILS="${TESTER_EMAILS:-$ACCOUNT}"

# Calls a Google REST API as the logged-in user. Prints the body; fails on HTTP errors.
api() {
    local method=$1 url=$2 body=${3:-}
    local args=(-sS --fail-with-body -X "$method"
        -H "Authorization: Bearer $(gcloud auth print-access-token)"
        -H "x-goog-user-project: $PROJECT_ID"
        -H "Content-Type: application/json")
    [ -n "$body" ] && args+=(--data "$body")
    curl "${args[@]}" "$url"
}

# Prints only the HTTP status of a GET, so callers can tell "missing" (404) from "exists" (200).
api_status() {
    curl -sS -o /dev/null -w '%{http_code}' \
        -H "Authorization: Bearer $(gcloud auth print-access-token)" \
        -H "x-goog-user-project: $PROJECT_ID" \
        "$1"
}

# Waits for a long-running operation returned by the Firebase Management API.
wait_operation() {
    local name=$1 op
    for _ in $(seq 1 60); do
        op="$(api GET "$FIREBASE_API/$name")"
        if [ "$(jq -r '.done // false' <<<"$op")" = "true" ]; then
            if jq -e '.error' <<<"$op" >/dev/null; then
                die "operation $name failed: $(jq -c '.error' <<<"$op")"
            fi
            return 0
        fi
        sleep 2
    done
    die "operation $name timed out"
}

log "Project $PROJECT_ID"
if ! gcloud projects describe "$PROJECT_ID" >/dev/null 2>&1; then
    gcloud projects create "$PROJECT_ID" --name="Novila"
fi
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
gcloud config set project "$PROJECT_ID" >/dev/null

log "Enabling APIs"
gcloud services enable --project="$PROJECT_ID" \
    cloudresourcemanager.googleapis.com \
    serviceusage.googleapis.com \
    firebase.googleapis.com \
    firebaseappdistribution.googleapis.com \
    iam.googleapis.com \
    iamcredentials.googleapis.com \
    sts.googleapis.com

log "Firebase"
if [ "$(api_status "$FIREBASE_API/projects/$PROJECT_ID")" != "200" ]; then
    op="$(api POST "$FIREBASE_API/projects/$PROJECT_ID:addFirebase" '{}')" ||
        die "addFirebase failed. If it mentions terms of service, open console.firebase.google.com once, accept, and rerun."
    wait_operation "$(jq -r '.name' <<<"$op")"
fi

log "Android app $ANDROID_PACKAGE"
find_app_id() {
    api GET "$FIREBASE_API/projects/$PROJECT_ID/androidApps?pageSize=100" |
        jq -r --arg pkg "$ANDROID_PACKAGE" '[.apps[]? | select(.packageName == $pkg) | .appId][0] // empty'
}
APP_ID="$(find_app_id)"
if [ -z "$APP_ID" ]; then
    body="$(jq -nc --arg pkg "$ANDROID_PACKAGE" --arg name "$ANDROID_DISPLAY_NAME" \
        '{packageName: $pkg, displayName: $name}')"
    op="$(api POST "$FIREBASE_API/projects/$PROJECT_ID/androidApps" "$body")"
    wait_operation "$(jq -r '.name' <<<"$op")"
    APP_ID="$(find_app_id)"
fi
[ -n "$APP_ID" ] || die "could not find the Android app after creating it"
echo "App ID: $APP_ID"

log "App Distribution group $TESTER_GROUP"
if [ "$(api_status "$APPDIST_API/projects/$PROJECT_NUMBER/groups/$TESTER_GROUP")" != "200" ]; then
    api POST "$APPDIST_API/projects/$PROJECT_NUMBER/groups?groupId=$TESTER_GROUP" '{"displayName":"Testare"}' >/dev/null ||
        die "could not create the group. Open App Distribution in the Firebase console, press Get started, and rerun."
fi
emails="$(jq -nc --arg list "$TESTER_EMAILS" '$list | split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(. != ""))')"
api POST "$APPDIST_API/projects/$PROJECT_NUMBER/groups/$TESTER_GROUP:batchJoin" \
    "$(jq -nc --argjson emails "$emails" '{emails: $emails, createMissingTesters: true}')" >/dev/null
echo "Testers: $TESTER_EMAILS"

SA_EMAIL="$SA_ID@$PROJECT_ID.iam.gserviceaccount.com"
log "Service account $SA_EMAIL"
if ! gcloud iam service-accounts describe "$SA_EMAIL" --project="$PROJECT_ID" >/dev/null 2>&1; then
    gcloud iam service-accounts create "$SA_ID" --project="$PROJECT_ID" \
        --display-name="GitHub Actions: distribute to testers"
fi
# A new service account can take a few seconds before IAM accepts it in bindings.
for attempt in 1 2 3 4 5; do
    if gcloud projects add-iam-policy-binding "$PROJECT_ID" \
        --member="serviceAccount:$SA_EMAIL" \
        --role="roles/firebaseappdistro.admin" \
        --condition=None >/dev/null; then
        break
    fi
    [ "$attempt" -lt 5 ] || die "could not grant Firebase App Distribution Admin"
    sleep $((attempt * 5))
done

log "Workload Identity Federation for $GITHUB_REPO"
if ! gcloud iam workload-identity-pools describe "$POOL_ID" \
    --project="$PROJECT_ID" --location=global >/dev/null 2>&1; then
    gcloud iam workload-identity-pools create "$POOL_ID" \
        --project="$PROJECT_ID" --location=global --display-name="GitHub Actions"
fi
provider_args=(
    --project="$PROJECT_ID" --location=global --workload-identity-pool="$POOL_ID"
    --display-name="GitHub $GITHUB_REPO"
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_id=assertion.repository_id,attribute.environment=assertion.environment"
    # Only jobs in this repo that run in the "firebase" environment may get tokens.
    --attribute-condition="assertion.repository_id == '$GITHUB_REPO_ID' && assertion.environment == '$GITHUB_ENVIRONMENT'"
)
if gcloud iam workload-identity-pools providers describe "$PROVIDER_ID" \
    --project="$PROJECT_ID" --location=global --workload-identity-pool="$POOL_ID" >/dev/null 2>&1; then
    gcloud iam workload-identity-pools providers update-oidc "$PROVIDER_ID" "${provider_args[@]}"
else
    gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_ID" "${provider_args[@]}" \
        --issuer-uri="https://token.actions.githubusercontent.com"
fi
PROVIDER="projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/providers/$PROVIDER_ID"
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" --project="$PROJECT_ID" \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/attribute.repository_id/$GITHUB_REPO_ID" \
    >/dev/null

log "GitHub Actions variables"
vars=(
    "FIREBASE_APP_ID=$APP_ID"
    "FIREBASE_TESTER_GROUPS=$TESTER_GROUP"
    "GCP_WORKLOAD_IDENTITY_PROVIDER=$PROVIDER"
    "GCP_SERVICE_ACCOUNT=$SA_EMAIL"
)
if command -v gh >/dev/null && gh auth status >/dev/null 2>&1; then
    for kv in "${vars[@]}"; do
        gh variable set "${kv%%=*}" --repo "$GITHUB_REPO" --body "${kv#*=}"
    done
    echo "Variables set on $GITHUB_REPO."
else
    echo "Add these under $GITHUB_REPO → Settings → Secrets and variables → Actions → Variables:"
    printf '  %s\n' "${vars[@]}"
fi

log "Done. No JSON key is needed; delete the FIREBASE_SERVICE_ACCOUNT secret if it exists."
