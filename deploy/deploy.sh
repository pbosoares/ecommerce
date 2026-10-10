#!/usr/bin/env bash
set -euo pipefail

root=${1:?Deployment directory required}
revision=${2:?Commit required}
[[ "$root" = /* && "$root" != / && "$root" != *..* ]]
[[ "$revision" =~ ^[a-f0-9]{40}$ ]]
cd "$root"
test -f .env
command -v flock >/dev/null
exec 9> .deploy.lock
flock -n 9 || { echo 'Another deployment is running'; exit 1; }
release="$root/releases/$revision"
mkdir -p "$release" "$root/backups"
tar -xzf "release-$revision.tar.gz" -C "$release"
ln -sfn "$root/.env" "$release/.env"
# Keep the existing Compose project name so persistent volumes are reused.
project=$(basename "$root")
if [[ -f "$root/current/compose.yaml" ]]; then
  previous="$root/current"
elif [[ -f "$root/compose.yaml" ]]; then
  previous="$root"
else
  previous=''
fi
compose() { docker compose --project-name "$project" --project-directory "$1" -f "$1/compose.yaml" "${@:2}"; }
compose "$release" config --quiet
compose "$release" build --pull
stamp=$(date -u +%Y%m%dT%H%M%SZ)
if [[ -n "$previous" ]] && [[ -n "$(compose "$previous" ps --status running -q db)" ]]; then
  compose "$previous" exec -T db pg_dump -U ecommerce -d ecommerce -Fc > "$root/backups/$stamp.dump"
  compose "$previous" cp api:/data/digital "$root/backups/digital-$stamp"
fi
compose "$release" up -d --wait --wait-timeout 180
compose "$release" exec -T web wget -q -O /dev/null http://127.0.0.1/api/actuator/health
ln -sfn "$release" "$root/current"
printf '%s\n' "$revision" > "$root/deployed-revision"
rm -f "release-$revision.tar.gz"
echo "Deployment successful: $revision"
