#!/usr/bin/env bash

set -euo pipefail

ANDROID_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_CORE_REPO="/Users/aritxonly/Codes/Agent/deadliner_core"
DEFAULT_RELEASE_REPO="DeadlinerStudio/LifiAI-Core"
DEFAULT_RELEASE_TAG="nightly"
ARTIFACT_NAME="deadliner-android.zip"
SYNC_ROOT="${ANDROID_REPO_ROOT}/.deadliner-core/android"
CACHE_ROOT="${ANDROID_REPO_ROOT}/.deadliner-core/cache"
STATE_FILE="${ANDROID_REPO_ROOT}/.deadliner-core/android-sync-state.json"

MODE="${1:-${DEADLINER_CORE_SOURCE:-release}}"
RELEASE_TAG="${DEADLINER_CORE_TAG:-$DEFAULT_RELEASE_TAG}"
RELEASE_REPO="${DEADLINER_CORE_RELEASE_REPO:-$DEFAULT_RELEASE_REPO}"
LOCAL_CORE_REPO="${DEADLINER_CORE_LOCAL_REPO:-$DEFAULT_CORE_REPO}"
GITHUB_API_ROOT="${DEADLINER_CORE_GITHUB_API_ROOT:-https://api.github.com}"
GITHUB_CONNECT_TIMEOUT="${DEADLINER_CORE_GITHUB_CONNECT_TIMEOUT:-10}"
GITHUB_MAX_TIME="${DEADLINER_CORE_GITHUB_MAX_TIME:-900}"
GITHUB_RETRY_COUNT="${DEADLINER_CORE_GITHUB_RETRY_COUNT:-3}"

DEST_JNI_LIBS="${SYNC_ROOT}/jniLibs"
DEST_BINDINGS="${SYNC_ROOT}/bindings"

AUTH_TOKEN=""
GH_AVAILABLE="false"

resolve_github_token() {
  if [[ -n "${DEADLINER_CORE_GITHUB_TOKEN:-}" ]]; then
    printf '%s' "${DEADLINER_CORE_GITHUB_TOKEN}"
    return 0
  fi

  if [[ -n "${GITHUB_TOKEN:-}" ]]; then
    printf '%s' "${GITHUB_TOKEN}"
    return 0
  fi

  if command -v gh >/dev/null 2>&1; then
    gh auth token 2>/dev/null || true
    return 0
  fi

  return 0
}

detect_gh_auth() {
  if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    return 0
  fi

  return 1
}

require_path() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "error: missing required path: $path" >&2
    exit 1
  fi
}

ensure_tools() {
  command -v curl >/dev/null 2>&1 || {
    echo "error: curl is required" >&2
    exit 1
  }
  command -v python3 >/dev/null 2>&1 || {
    echo "error: python3 is required" >&2
    exit 1
  }
}

auth_hint() {
  cat >&2 <<'EOF'
hint: this repo is private, so release sync needs GitHub auth.
hint: set DEADLINER_CORE_GITHUB_TOKEN (recommended) or GITHUB_TOKEN before building.
hint: example:
hint:   export DEADLINER_CORE_GITHUB_TOKEN=ghp_xxx
EOF
}

has_cached_sync() {
  [[ -d "${DEST_JNI_LIBS}" && -d "${DEST_BINDINGS}" ]] || return 1
  [[ -n "$(find "${DEST_JNI_LIBS}" -mindepth 1 -print -quit 2>/dev/null)" ]] || return 1
  [[ -n "$(find "${DEST_BINDINGS}" -mindepth 1 -print -quit 2>/dev/null)" ]] || return 1
}

fallback_to_cached_sync() {
  local reason="$1"
  if has_cached_sync; then
    echo "warning: ${reason}" >&2
    echo "warning: failed to refresh release artifact, falling back to cached Android core files" >&2
    return 0
  fi

  echo "error: ${reason}" >&2
  echo "error: no usable cached Android core files found in ${SYNC_ROOT}" >&2
  return 1
}

clean_destinations() {
  rm -rf "${DEST_JNI_LIBS}" "${DEST_BINDINGS}"
  mkdir -p "${DEST_JNI_LIBS}" "${DEST_BINDINGS}"
}

write_state_file() {
  local mode="$1"
  local source="$2"
  local tag="${3:-}"
  local commit="${4:-}"
  local asset_fingerprint="${5:-}"

  mkdir -p "$(dirname "${STATE_FILE}")"
  python3 - <<'PY' "${STATE_FILE}" "${mode}" "${source}" "${tag}" "${commit}" "${asset_fingerprint}"
from pathlib import Path
import json
import sys

state_path = Path(sys.argv[1])
mode = sys.argv[2]
source = sys.argv[3]
tag = sys.argv[4]
commit = sys.argv[5]
asset_fingerprint = sys.argv[6]

payload = {
    "mode": mode,
    "source": source,
}
if tag:
    payload["tag"] = tag
if commit:
    payload["commit"] = commit
if asset_fingerprint:
    payload["assetFingerprint"] = asset_fingerprint

state_path.write_text(json.dumps(payload, indent=2) + "\n")
PY
}

read_cached_value() {
  local key="$1"
  python3 - <<'PY' "${STATE_FILE}" "${key}"
from pathlib import Path
import json
import sys

state_path = Path(sys.argv[1])
key = sys.argv[2]
if not state_path.exists():
    print("")
else:
    data = json.loads(state_path.read_text())
    print(data.get(key, ""))
PY
}

sync_from_local_repo() {
  local core_repo="$1"
  local src_jni src_bindings

  src_jni="${core_repo}/dist/android/jniLibs"
  src_bindings="${core_repo}/dist/android/bindings"

  echo "==> Syncing Android artifacts from local core repo"
  echo "    core repo: ${core_repo}"

  require_path "${src_jni}"
  require_path "${src_bindings}"

  clean_destinations
  cp -R "${src_jni}/." "${DEST_JNI_LIBS}/"
  cp -R "${src_bindings}/." "${DEST_BINDINGS}/"

  write_state_file "local" "${core_repo}"
}

fetch_release_metadata() {
  local response_file="$1"

  echo "==> Fetching release metadata"

  if [[ "${GH_AVAILABLE}" == "true" ]]; then
    gh api "repos/${RELEASE_REPO}/releases/tags/${RELEASE_TAG}" > "${response_file}"
    return 0
  fi

  local -a curl_args
  curl_args=(
    -fsSL
    --connect-timeout "${GITHUB_CONNECT_TIMEOUT}"
    --max-time "${GITHUB_MAX_TIME}"
    --retry "${GITHUB_RETRY_COUNT}"
    --retry-all-errors
    -H "Accept: application/vnd.github+json"
    -H "X-GitHub-Api-Version: 2022-11-28"
    -H "User-Agent: deadliner-core-android-sync"
  )

  if [[ -n "${AUTH_TOKEN}" ]]; then
    curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi

  curl_args+=(
    "${GITHUB_API_ROOT}/repos/${RELEASE_REPO}/releases/tags/${RELEASE_TAG}"
    -o "${response_file}"
  )

  curl "${curl_args[@]}"
}

release_metadata_value() {
  local metadata_file="$1"
  local key="$2"

  python3 - <<'PY' "${metadata_file}" "${ARTIFACT_NAME}" "${key}"
from pathlib import Path
import json
import re
import sys

metadata = json.loads(Path(sys.argv[1]).read_text())
asset_name = sys.argv[2]
key = sys.argv[3]

asset = None
for item in metadata.get("assets", []):
    if item.get("name") == asset_name:
        asset = item
        break

if key == "commit":
    body = metadata.get("body") or ""
    match = re.search(r"Commit:\s*([0-9a-fA-F]{7,40})", body)
    print(match.group(1) if match else metadata.get("target_commitish", "unknown"))
elif key == "asset_api_url":
    if not asset:
        raise SystemExit(f"error: asset {asset_name} not found in release metadata")
    print(asset.get("url", ""))
elif key == "asset_fingerprint":
    if not asset:
        raise SystemExit(f"error: asset {asset_name} not found in release metadata")
    digest = asset.get("digest") or ""
    updated_at = asset.get("updated_at") or ""
    asset_id = asset.get("id") or ""
    print(f"{digest}|{updated_at}|{asset_id}")
else:
    print("")
PY
}

download_release_asset() {
  local metadata_file="$1"
  local output_file="$2"
  local asset_api_url

  echo "==> Downloading Android release asset"

  if [[ "${GH_AVAILABLE}" == "true" ]]; then
    rm -f "${output_file}"
    gh release download "${RELEASE_TAG}" \
      --repo "${RELEASE_REPO}" \
      --pattern "${ARTIFACT_NAME}" \
      --output "${output_file}" \
      --clobber
    return 0
  fi

  if ! asset_api_url="$(release_metadata_value "${metadata_file}" "asset_api_url")"; then
    return 1
  fi

  local -a curl_args
  curl_args=(
    -fsSL
    --connect-timeout "${GITHUB_CONNECT_TIMEOUT}"
    --max-time "${GITHUB_MAX_TIME}"
    --retry "${GITHUB_RETRY_COUNT}"
    --retry-all-errors
    -H "Accept: application/octet-stream"
    -H "X-GitHub-Api-Version: 2022-11-28"
    -H "User-Agent: deadliner-core-android-sync"
  )

  if [[ -n "${AUTH_TOKEN}" ]]; then
    curl_args+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi

  curl_args+=("${asset_api_url}" -o "${output_file}")
  curl "${curl_args[@]}"
}

extract_release_artifact() {
  local archive_file="$1"
  local extract_dir="$2"

  echo "==> Extracting Android release archive"
  python3 - <<'PY' "${archive_file}" "${extract_dir}"
from pathlib import Path
import shutil
import sys
import zipfile

archive = Path(sys.argv[1])
extract_dir = Path(sys.argv[2])

if extract_dir.exists():
    shutil.rmtree(extract_dir)
extract_dir.mkdir(parents=True, exist_ok=True)

with zipfile.ZipFile(archive) as zf:
    zf.extractall(extract_dir)
PY
}

sync_from_release() {
  local metadata_file zip_file extract_dir cached_sha remote_sha cached_fingerprint remote_fingerprint

  mkdir -p "${CACHE_ROOT}" "$(dirname "${STATE_FILE}")"
  metadata_file="${CACHE_ROOT}/release-android.json"
  zip_file="${CACHE_ROOT}/${ARTIFACT_NAME}"
  extract_dir="${CACHE_ROOT}/android-unzipped"

  echo "==> Syncing Android artifacts from GitHub release"
  echo "    repo: ${RELEASE_REPO}"
  echo "    tag: ${RELEASE_TAG}"

  if [[ -z "${AUTH_TOKEN}" ]]; then
    echo "warning: no GitHub token detected; private release access may fail." >&2
  fi

  if [[ "${GH_AVAILABLE}" == "true" ]]; then
    echo "==> GitHub CLI authenticated"
  elif command -v gh >/dev/null 2>&1; then
    echo "==> GitHub CLI detected but not authenticated for this shell"
  fi

  if ! fetch_release_metadata "${metadata_file}"; then
    auth_hint
    fallback_to_cached_sync "failed to fetch release metadata for ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  if ! remote_sha="$(release_metadata_value "${metadata_file}" "commit")"; then
    fallback_to_cached_sync "failed to parse release metadata for ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  if ! remote_fingerprint="$(release_metadata_value "${metadata_file}" "asset_fingerprint")"; then
    fallback_to_cached_sync "failed to read asset fingerprint for ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  cached_sha="$(read_cached_value "commit")"
  cached_fingerprint="$(read_cached_value "assetFingerprint")"

  if [[ -n "${cached_fingerprint}" && "${cached_fingerprint}" == "${remote_fingerprint}" ]] && has_cached_sync; then
    echo "==> Android core artifacts already up to date at ${remote_sha}"
    return 0
  fi

  if [[ -z "${cached_fingerprint}" && -n "${cached_sha}" && "${cached_sha}" == "${remote_sha}" ]] && has_cached_sync; then
    echo "==> Android core artifacts already up to date at ${remote_sha}"
    return 0
  fi

  if ! download_release_asset "${metadata_file}" "${zip_file}"; then
    auth_hint
    fallback_to_cached_sync "failed to download ${ARTIFACT_NAME} from ${RELEASE_REPO}@${RELEASE_TAG}"
    return $?
  fi

  if ! extract_release_artifact "${zip_file}" "${extract_dir}"; then
    fallback_to_cached_sync "failed to extract ${zip_file}"
    return $?
  fi

  require_path "${extract_dir}/android/jniLibs"
  require_path "${extract_dir}/android/bindings"

  clean_destinations
  cp -R "${extract_dir}/android/jniLibs/." "${DEST_JNI_LIBS}/"
  cp -R "${extract_dir}/android/bindings/." "${DEST_BINDINGS}/"

  write_state_file "release" "${RELEASE_REPO}" "${RELEASE_TAG}" "${remote_sha}" "${remote_fingerprint}"
  echo "==> Synced Android core artifacts at commit ${remote_sha}"
}

main() {
  ensure_tools
  AUTH_TOKEN="$(resolve_github_token)"
  if detect_gh_auth; then
    GH_AVAILABLE="true"
  fi

  mkdir -p "${SYNC_ROOT}"

  case "${MODE}" in
    local)
      sync_from_local_repo "${LOCAL_CORE_REPO}"
      ;;
    release)
      sync_from_release
      ;;
    /*)
      sync_from_local_repo "${MODE}"
      ;;
    *)
      RELEASE_TAG="${MODE}"
      sync_from_release
      ;;
  esac

  echo "==> Android JNI libs: ${DEST_JNI_LIBS}"
  echo "==> Android Kotlin bindings: ${DEST_BINDINGS}"
}

main "$@"
