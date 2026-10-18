#!/usr/bin/env bash
# Bootstraps Google Play publishing for Novila. Safe to run again: every step
# checks what already exists and only creates what is missing.
#
# Easiest from a phone: open Google Cloud Shell (already logged in, has gcloud and jq),
# clone the repo and run:
#
#   ./scripts/bootstrap-play.sh <project-id>
#
# It sets up (in the same Google Cloud project as Firebase, recommended):
#   1. The Android Publisher API on that project
#   2. A service account that CI uses to publish to Google Play (the Play Console
#      permission is granted by hand, see docs/RELEASE.md)
#   3. Workload Identity Federation so jobs in the GitHub "play" environment can act
#      as that service account without any JSON key
#
# Finally it sets the GitHub Actions variables with gh, or prints them if gh is not logged in.
# The counterpart for testers is scripts/bootstrap-firebase.sh.
set -euo pipefail

PROJECT_ID="${1:-${PROJECT_ID:-}}"

GITHUB_REPO="joeldenke/Novila"
# Numeric repo id survives renames and transfers, unlike the name.
GITHUB_REPO_ID="1419684243"
GITHUB_ENVIRONMENT="play"
POOL_ID="github"
PROVIDER_ID="novila-play"
SA_ID="github-play"

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[ -n "$PROJECT_ID" ] || die "usage: $0 <project-id>  (project id is globally unique, e.g. novila-joel)"
command -v gcloud >/dev/null || die "gcloud is missing (Cloud Shell has it)"

ACCOUNT="$(gcloud config get-value account 2>/dev/null || true)"
[ -n "$ACCOUNT" ] || die "not logged in, run: gcloud auth login"

log "Project $PROJECT_ID"
gcloud projects describe "$PROJECT_ID" >/dev/null 2>&1 || die "project $PROJECT_ID does not exist"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
gcloud config set project "$PROJECT_ID" >/dev/null

log "Enabling APIs"
gcloud services enable --project="$PROJECT_ID" \
    androidpublisher.googleapis.com \
    iam.googleapis.com \
    iamcredentials.googleapis.com \
    sts.googleapis.com

SA_EMAIL="$SA_ID@$PROJECT_ID.iam.gserviceaccount.com"
log "Service account $SA_EMAIL"
if ! gcloud iam service-accounts describe "$SA_EMAIL" --project="$PROJECT_ID" >/dev/null 2>&1; then
    gcloud iam service-accounts create "$SA_ID" --project="$PROJECT_ID" \
        --display-name="GitHub Actions: publish to Google Play"
fi

log "Workload Identity Federation for $GITHUB_REPO (GitHub environment '$GITHUB_ENVIRONMENT')"
if ! gcloud iam workload-identity-pools describe "$POOL_ID" \
    --project="$PROJECT_ID" --location=global >/dev/null 2>&1; then
    gcloud iam workload-identity-pools create "$POOL_ID" \
        --project="$PROJECT_ID" --location=global --display-name="GitHub Actions"
fi
# A separate provider from bootstrap-firebase.sh's "novila": its attribute condition only lets
# jobs in the "play" environment get tokens, so the distribute job and the release job can hold
# different service accounts.
provider_args=(
    --project="$PROJECT_ID" --location=global --workload-identity-pool="$POOL_ID"
    --display-name="GitHub $GITHUB_REPO (play)"
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_id=assertion.repository_id,attribute.environment=assertion.environment"
    # Only jobs in this repo that run in the "play" environment may get tokens.
    --attribute-condition="assertion.repository_id == '$GITHUB_REPO_ID' && assertion.environment == '$GITHUB_ENVIRONMENT'"
)
if gcloud iam workload-identity-pools providers describe "$PROVIDER_ID" \
    --project="$PROJECT_ID" --location=global >/dev/null 2>&1; then
    gcloud iam workload-identity-pools providers update-oidc "$PROVIDER_ID" "${provider_args[@]}"
else
    gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_ID" "${provider_args[@]}" \
        --issuer-uri="https://token.actions.githubusercontent.com"
fi
PROVIDER="projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/providers/$PROVIDER_ID"
# Bound to the environment attribute, not the whole repository: only federated principals from
# the "play" provider may act as this service account, not every job in the repo.
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" --project="$PROJECT_ID" \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/attribute.environment/$GITHUB_ENVIRONMENT" \
    >/dev/null

log "GitHub Actions variables"
vars=(
    "GCP_PLAY_WORKLOAD_IDENTITY_PROVIDER=$PROVIDER"
    "GCP_PLAY_SERVICE_ACCOUNT=$SA_EMAIL"
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

log "Done. No JSON key is needed. Next: invite $SA_EMAIL in the Play Console"
echo "(Users and permissions, release-to-testing-tracks only) — see docs/RELEASE.md."
