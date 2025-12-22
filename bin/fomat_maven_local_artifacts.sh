#!/usr/bin/env bash

# Uncompress download.zip from artifacts
# ./format_maven_local_artifacts.sh io/github/jmatsu/license-list-schema/<version>
# Compress io/ as a zip

set -euo pipefail

f() { md5 "$1" | awk '$0=$4' > "$1".md5; sha1 "$1" | awk '$0=$4' > "$1".sha1 }

cd "$1"

while read name; do
  if [[ "$name" =~ \.md5$ ]] || [[ "$name" =~ \.sha1$ ]]; then
    continue
  fi

  f "$name"
done < <(find . -type f)

cd ../
mv "maven-metadata-local.xml" "maven-metadata.xml"

