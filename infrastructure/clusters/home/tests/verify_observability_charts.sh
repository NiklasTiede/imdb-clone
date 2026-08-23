#!/usr/bin/env bash

set -euo pipefail

readonly repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
readonly verification_dir="$(mktemp -d /tmp/imdb-clone-observability-charts.XXXXXX)"
readonly helm_image="alpine/helm:3.20.0"
readonly kubeconform_image="ghcr.io/yannh/kubeconform:v0.6.7"

cleanup() {
  rm -rf "${verification_dir}"
}
trap cleanup EXIT

render_chart() {
  local application_file="$1"
  local release="$2"
  local chart="$3"
  local repository="$4"
  local version="$5"
  local values_file="${verification_dir}/${release}-values.yaml"
  local rendered_file="${verification_dir}/${release}-rendered.yaml"

  ruby -ryaml -e \
    'document = YAML.load_file(ARGV.fetch(0)); puts YAML.dump(document.dig("spec", "source", "helm", "valuesObject"))' \
    "${repository_root}/${application_file}" >"${values_file}"

  docker run --rm -i "${helm_image}" template "${release}" "${chart}" \
    --repo "${repository}" \
    --version "${version}" \
    --namespace observability \
    -f - \
    <"${values_file}" >"${rendered_file}"

  docker run --rm -i "${kubeconform_image}" \
    -strict \
    -summary \
    -ignore-missing-schemas \
    <"${rendered_file}"
}

render_chart \
  infrastructure/clusters/home/apps/loki.yaml \
  loki \
  loki \
  https://grafana.github.io/helm-charts \
  7.3.0

render_chart \
  infrastructure/clusters/home/apps/tempo.yaml \
  tempo \
  tempo \
  https://grafana-community.github.io/helm-charts \
  2.2.4

render_chart \
  infrastructure/clusters/home/apps/alloy.yaml \
  alloy \
  alloy \
  https://grafana.github.io/helm-charts \
  1.11.1

ruby -ryaml -e '
  documents = YAML.load_stream(File.read(ARGV.fetch(0))).compact
  daemonset = documents.find { |document| document["kind"] == "DaemonSet" }
  raise "Alloy DaemonSet missing" if daemonset.nil?
  pod_spec = daemonset.dig("spec", "template", "spec")
  raise "journal group missing" unless pod_spec.dig("securityContext", "supplementalGroups").include?(999)
  volume = pod_spec.fetch("volumes").find { |candidate| candidate["name"] == "varlog" }
  raise "read-only host journal volume missing" unless volume&.dig("hostPath", "path") == "/var/log"
  container = pod_spec.fetch("containers").find { |candidate| candidate["name"] == "alloy" }
  mount = container.fetch("volumeMounts").find { |candidate| candidate["name"] == "varlog" }
  raise "host journal mount must be read-only" unless mount&.dig("readOnly") == true
  node_env = container.fetch("env").find { |candidate| candidate["name"] == "ALLOY_NODE_NAME" }
  raise "Alloy node identity missing" unless node_env&.dig("valueFrom", "fieldRef", "fieldPath") == "spec.nodeName"
  role = documents.find { |document| document["kind"] == "ClusterRole" }
  resources = role.fetch("rules").flat_map { |rule| rule.fetch("resources", []) }
  raise "pod log permission missing" unless resources.include?("pods/log")
  raise "event permission missing" unless resources.include?("events")
' "${verification_dir}/alloy-rendered.yaml"

ruby -ryaml -e \
  'document = YAML.load_file(ARGV.fetch(0)); puts document.dig("spec", "source", "helm", "valuesObject", "alloy", "configMap", "content")' \
  "${repository_root}/infrastructure/clusters/home/apps/alloy.yaml" \
  >"${verification_dir}/config.alloy"

docker run --rm \
  -v "${verification_dir}:/work:ro" \
  grafana/alloy:v1.18.1 \
  validate /work/config.alloy

echo "Observability Helm charts and Alloy configuration passed."
