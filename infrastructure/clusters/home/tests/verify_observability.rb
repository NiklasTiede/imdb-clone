# frozen_string_literal: true

require "json"
require "yaml"

rendered_path = ARGV.fetch(0, "/tmp/imdb-clone-home-apps.yaml")
repository_root = File.expand_path("../../../..", __dir__)
documents = YAML.load_stream(File.read(rendered_path)).compact

def resource(documents, kind, name, namespace = nil)
  match = documents.find do |document|
    document["kind"] == kind &&
      document.dig("metadata", "name") == name &&
      (namespace.nil? || document.dig("metadata", "namespace") == namespace)
  end
  raise "missing #{kind}/#{name}" if match.nil?

  match
end

def assert_contract(condition, message)
  raise message unless condition
end

loki = resource(documents, "Application", "loki", "argocd")
loki_values = loki.dig("spec", "source", "helm", "valuesObject")
assert_contract(loki.dig("spec", "source", "targetRevision") == "7.3.0", "Loki chart drifted")
assert_contract(loki_values["deploymentMode"] == "SingleBinary", "Loki must stay single binary")
assert_contract(
  loki_values.dig("loki", "limits_config", "retention_period") == "168h",
  "Loki retention drifted"
)
assert_contract(
  loki_values.dig("singleBinary", "persistence", "whenDeleted") == "Retain",
  "Loki PVC must survive Application deletion"
)

tempo = resource(documents, "Application", "tempo", "argocd")
tempo_values = tempo.dig("spec", "source", "helm", "valuesObject")
assert_contract(
  tempo.dig("spec", "source", "repoURL") ==
    "https://grafana-community.github.io/helm-charts",
  "Tempo must use the maintained community chart repository"
)
assert_contract(tempo.dig("spec", "source", "targetRevision") == "2.2.4", "Tempo chart drifted")
assert_contract(tempo_values.dig("tempo", "retention") == "72h", "Tempo retention drifted")
assert_contract(tempo_values.dig("persistence", "enabled") == true, "Tempo persistence required")
assert_contract(
  tempo_values.dig("tempo", "overrides", "defaults", "metrics_generator", "processors") ==
    %w[service-graphs span-metrics],
  "Tempo service graph and span metrics must remain enabled"
)

alloy = resource(documents, "Application", "alloy", "argocd")
alloy_values = alloy.dig("spec", "source", "helm", "valuesObject")
assert_contract(alloy.dig("spec", "source", "targetRevision") == "1.11.1", "Alloy drifted")
assert_contract(alloy_values.dig("controller", "type") == "daemonset", "Alloy must cover every node")
assert_contract(
  alloy_values.dig("global", "podSecurityContext", "supplementalGroups").include?(999),
  "Alloy needs read-only systemd journal group access"
)
assert_contract(alloy_values.dig("alloy", "mounts", "varlog") == true, "node logs must be mounted")
assert_contract(
  alloy_values.dig("alloy", "extraPorts").any? { |port| port["port"] == 4318 },
  "Alloy OTLP/HTTP port missing"
)
alloy_config = alloy_values.dig("alloy", "configMap", "content")
%w[loki.source.kubernetes loki.source.kubernetes_events loki.source.journal otelcol.receiver.otlp].each do |component|
  assert_contract(alloy_config.include?(component), "Alloy config missing #{component}")
end
assert_contract(
  alloy_config.include?("_SYSTEMD_UNIT=k3s.service"),
  "journal collection must remain limited to the k3s service"
)

observability = resource(documents, "Application", "observability", "argocd")
datasources = observability.dig(
  "spec",
  "source",
  "helm",
  "valuesObject",
  "grafana",
  "additionalDataSources"
)
assert_contract(datasources.map { |source| source["uid"] }.sort == %w[loki tempo], "datasources drifted")
assert_contract(
  observability.dig(
    "spec",
    "source",
    "helm",
    "valuesObject",
    "prometheus",
    "prometheusSpec",
    "enableRemoteWriteReceiver"
  ) == true,
  "Prometheus remote-write receiver is required for Tempo service metrics"
)

rustfs = resource(documents, "Application", "rustfs", "argocd")
assert_contract(
  rustfs.dig("spec", "source", "helm", "valuesObject", "config", "rustfs", "console_enable") ==
    "true",
  "RustFS console must be enabled for private tunnel access"
)

public_ingresses = documents.select { |document| document["kind"] == "Ingress" }
private_service_names = %w[
  imdb-clone-postgresql
  imdb-clone-opensearch
  imdb-clone-rustfs-svc
  loki-gateway
  tempo
  observability-kube-prometh-prometheus
]
public_backends = public_ingresses.flat_map do |ingress|
  ingress.fetch("spec").fetch("rules", []).flat_map do |rule|
    rule.dig("http", "paths")&.map { |path| path.dig("backend", "service", "name") } || []
  end
end
assert_contract(
  (public_backends & private_service_names).empty?,
  "an operator-only service was exposed through ingress"
)

rules = resource(documents, "PrometheusRule", "observability-stack-alerts", "observability")
alert_names = rules.dig("spec", "groups").flat_map do |group|
  group.fetch("rules").map { |rule| rule["alert"] }.compact
end
%w[LokiUnavailable TempoUnavailable AlloyUnavailable LokiDiscardingLogs].each do |alert_name|
  assert_contract(alert_names.include?(alert_name), "missing alert #{alert_name}")
end

dashboard_manifest = YAML.load_file(
  File.join(
    repository_root,
    "infrastructure/clusters/home/apps/observability/dashboards/cluster-logs.yaml"
  )
)
dashboard = JSON.parse(dashboard_manifest.dig("data", "cluster-logs.json"))
assert_contract(dashboard["uid"] == "imdb-clone-cluster-logs", "log dashboard UID drifted")
assert_contract(dashboard.fetch("panels").length >= 4, "log dashboard is incomplete")

traefik = resource(documents, "HelmChartConfig", "traefik", "kube-system")
traefik_values = YAML.safe_load(traefik.dig("spec", "valuesContent"))
assert_contract(
  traefik_values.dig("logs", "general", "format") == "json",
  "Traefik logs must be JSON"
)
assert_contract(
  traefik_values.dig("logs", "access", "enabled") == true,
  "access logs must be on"
)
assert_contract(
  traefik_values.dig("logs", "access", "fields", "headers", "defaultmode") == "drop",
  "Traefik must not log request headers"
)
%w[ClientAddr ClientHost ClientPort ClientUsername RequestAddr RequestLine RequestPath].each do |field|
  assert_contract(
    traefik_values.dig("logs", "access", "fields", "general", "names", field) == "drop",
    "Traefik access logs must drop #{field}"
  )
end

puts "Observability production manifest contracts passed."
