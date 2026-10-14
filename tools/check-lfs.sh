#!/usr/bin/env bash
# Fails if any staged file carries the git-lfs filter attribute but is stored
# in the index as a raw blob rather than an LFS pointer.
#
# Audit-only: never modifies the index or working tree.
set -euo pipefail

raw=$(
  git diff --cached --name-only --diff-filter=ACMR \
    | while IFS= read -r f; do
        [[ -f "$f" ]] || continue
        attr=$(git check-attr filter -- "$f" | awk -F': ' '{print $NF}')
        [[ "$attr" == "lfs" ]] || continue
        if ! git cat-file -p ":$f" 2>/dev/null \
            | head -c 100 \
            | grep -q '^version https://git-lfs'; then
          echo "$f"
        fi
      done
)

if [[ -n "$raw" ]]; then
  echo "ERROR: these staged files should be in git-lfs but are stored as raw blobs:" >&2
  printf '%s\n' "$raw" | sed 's/^/  /' >&2
  echo >&2
  echo "Fix: git add --renormalize . && git commit" >&2
  exit 1
fi

exit 0