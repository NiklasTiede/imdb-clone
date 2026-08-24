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
  infrastructure/clusters/home/apps/pyroscope.yaml \
  pyroscope \
  pyroscope \
  https://grafana.github.io/helm-charts \
  1.20.3

ruby -ryaml -e '
  documents = YAML.load_stream(File.read(ARGV.fetch(0))).compact
  stateful_set = documents.find do |document|
    document["kind"] == "StatefulSet" && document.dig("metadata", "name") == "pyroscope"
  end
  raise "Pyroscope StatefulSet missing" if stateful_set.nil?
  container = stateful_set.dig("spec", "template", "spec", "containers").find do |candidate|
    candidate["name"] == "pyroscope"
  end
  raise "Pyroscope resource limits missing" unless container.dig("resources", "limits", "memory") == "1Gi"
  claims = stateful_set.dig("spec", "volumeClaimTemplates") || []
  data_claim = claims.find { |claim| claim.dig("metadata", "name") == "data" }
  raise "Pyroscope persistent data volume missing" if data_claim.nil?
  raise "Pyroscope storage drifted" unless data_claim.dig("spec", "resources", "requests", "storage") == "10Gi"
  service = documents.find do |document|
    document["kind"] == "Service" && document.dig("metadata", "name") == "pyroscope"
  end
  raise "private Pyroscope service missing" unless service&.dig("spec", "type") == "ClusterIP"
  monitor = documents.find do |document|
    document["kind"] == "ServiceMonitor" && document.dig("metadata", "name") == "pyroscope"
  end
  raise "Pyroscope ServiceMonitor missing" if monitor.nil?
  embedded_alloy = documents.any? do |document|
    document.dig("metadata", "name")&.start_with?("pyroscope-alloy")
  end
  raise "Pyroscope must not deploy a duplicate Alloy" if embedded_alloy
' "${verification_dir}/pyroscope-rendered.yaml"

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
  'documents = YAML.load_stream(File.read(ARGV.fetch(0))).compact
   config_map = documents.find do |document|
     document["kind"] == "ConfigMap" && document.dig("data", "config.alloy")
   end
   raise "rendered Alloy config missing" if config_map.nil?
   puts config_map.dig("data", "config.alloy")' \
  "${verification_dir}/alloy-rendered.yaml" \
  >"${verification_dir}/config.alloy"

docker run --rm \
  -v "${verification_dir}:/work:ro" \
  grafana/alloy:v1.18.1 \
  validate /work/config.alloy

echo "Observability Helm charts and Alloy configuration passed."
