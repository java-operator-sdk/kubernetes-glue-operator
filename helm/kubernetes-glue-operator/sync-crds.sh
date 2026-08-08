#!/usr/bin/env bash
# Regenerates the chart's CRDs from the latest build output.
# Run `./mvnw clean package -DskipTests` first so target/kubernetes exists.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
gen_dir="$repo_root/target/kubernetes"
crd_dir="$repo_root/helm/kubernetes-glue-operator/crds"

if [[ ! -d "$gen_dir" ]]; then
  echo "ERROR: $gen_dir not found. Run './mvnw clean package -DskipTests' first." >&2
  exit 1
fi

cp "$gen_dir/glues.io.javaoperatorsdk.operator.glue-v1.yml" "$crd_dir/glues.yaml"
cp "$gen_dir/glueoperators.io.javaoperatorsdk.operator.glue-v1.yml" "$crd_dir/glueoperators.yaml"

echo "Synced CRDs into $crd_dir"
