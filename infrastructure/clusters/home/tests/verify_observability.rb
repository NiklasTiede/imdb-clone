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

def container_environment(deployment, container_name)
  container = deployment.dig("spec", "template", "spec", "containers").find do |candidate|
    candidate["name"] == container_name
  end
  raise "missing container #{container_name}" if container.nil?

  container.fetch("env", []).to_h { |entry| [entry["name"], entry["value"]] }
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
    %w[local-blocks service-graphs span-metrics],
  "Tempo TraceQL, service graph, and span metrics must remain enabled"
)
assert_contract(
  tempo_values.dig(
    "tempo",
    "metricsGenerator",
    "processor",
    "local_blocks",
    "filter_server_spans"
  ) == false,
  "Tempo TraceQL metrics must include every span kind"
)

pyroscope = resource(documents, "Application", "pyroscope", "argocd")
pyroscope_values = pyroscope.dig("spec", "source", "helm", "valuesObject")
assert_contract(
  pyroscope.dig("spec", "source", "targetRevision") == "1.20.3",
  "Pyroscope chart drifted"
)
assert_contract(
  pyroscope_values.dig("architecture", "storage") == { "v1" => false, "v2" => true },
  "Pyroscope must use storage v2"
)
assert_contract(
  pyroscope_values.dig("architecture", "microservices", "enabled") == false,
  "Pyroscope must stay single binary"
)
assert_contract(
  pyroscope_values.dig("pyroscope", "persistence", "enabled") == true,
  "Pyroscope persistence required"
)
assert_contract(
  pyroscope_values.dig("pyroscope", "persistence", "storageClassName") == "local-path",
  "Pyroscope must use the home-cluster storage class"
)
assert_contract(
  pyroscope_values.dig("alloy", "enabled") == false,
  "Pyroscope must reuse the existing Alloy deployment"
)
pyroscope_policy = resource(
  documents,
  "NetworkPolicy",
  "pyroscope-private-ingress",
  "observability"
)
assert_contract(
  pyroscope_policy.dig("spec", "policyTypes") == ["Ingress"],
  "Pyroscope must remain ingress-isolated"
)
profile_ingress = pyroscope_policy.dig("spec", "ingress", 1)
allowed_profile_sources = profile_ingress.fetch("from").map do |source|
  [
    source.dig("namespaceSelector", "matchLabels", "kubernetes.io/metadata.name") ||
      "observability",
    source.dig("podSelector", "matchLabels", "app.kubernetes.io/name")
  ]
end
assert_contract(
  allowed_profile_sources.sort == [
    ["imdb-clone", "imdb-clone-agent"],
    ["imdb-clone", "imdb-clone-backend"],
    ["observability", "grafana"],
    ["observability", "prometheus"]
  ],
  "Pyroscope ingress allowlist drifted"
)
assert_contract(
  profile_ingress.fetch("ports") == [{ "protocol" => "TCP", "port" => 4040 }],
  "Pyroscope ingestion/query port drifted"
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
assert_contract(
  datasources.map { |source| source["uid"] }.sort == %w[loki pyroscope tempo],
  "datasources drifted"
)
tempo_datasource = datasources.find { |source| source["uid"] == "tempo" }
assert_contract(
  tempo_datasource.dig("jsonData", "tracesToProfiles", "datasourceUid") == "pyroscope",
  "Tempo must link traces to Pyroscope profiles"
)
pyroscope_datasource = datasources.find { |source| source["uid"] == "pyroscope" }
assert_contract(
  pyroscope_datasource["url"] == "http://pyroscope.observability.svc.cluster.local:4040",
  "Grafana must query the private Pyroscope service"
)
grafana_plugins = observability.dig(
  "spec",
  "source",
  "helm",
  "valuesObject",
  "grafana",
  "grafana.ini",
  "plugins"
)
assert_contract(
  grafana_plugins["preinstall_auto_update"] == false &&
    grafana_plugins["preinstall_sync"].include?("grafana-pyroscope-app@2.3.0"),
  "Grafana drilldown plugins must be installed deterministically"
)
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
  pyroscope
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

backend = resource(documents, "Deployment", "imdb-clone-backend", "imdb-clone")
backend_environment = container_environment(backend, "backend")
assert_contract(
  !backend_environment.key?("JAVA_TOOL_OPTIONS"),
  "profiling activation must remain compatible with the previously published backend image"
)
dockerfile = File.read(File.join(repository_root, "Dockerfile"))
backend_entrypoint = File.read(File.join(repository_root, "scripts/backend-entrypoint"))
assert_contract(
  dockerfile.include?('ENTRYPOINT ["/usr/local/bin/backend-entrypoint"]') &&
    backend_entrypoint.include?("-javaagent:/opt/pyroscope/pyroscope.jar"),
  "new backend image must activate its bundled Pyroscope agent through the safe entrypoint"
)
assert_contract(
  backend_environment["PYROSCOPE_SERVER_ADDRESS"] ==
    "http://pyroscope.observability.svc.cluster.local:4040",
  "backend must use the private Pyroscope service"
)
assert_contract(
  backend_environment.values_at(
    "PYROSCOPE_FORMAT",
    "PYROSCOPE_PROFILING_INTERVAL",
    "PYROSCOPE_PROFILER_ALLOC",
    "PYROSCOPE_PROFILER_LOCK",
    "PYROSCOPE_UPLOAD_INTERVAL",
    "PYROSCOPE_PROFILE_EXPORT_TIMEOUT",
    "PYROSCOPE_INGEST_MAX_TRIES"
  ) == ["jfr", "20ms", "512k", "10ms", "15s", "5s", "2"],
  "backend profiling overhead bounds drifted"
)

agent = resource(documents, "Deployment", "imdb-clone-agent", "imdb-clone")
agent_environment = container_environment(agent, "agent")
assert_contract(
  agent_environment.keys.none? { |name| name.start_with?("IMDB_AGENT_PROFILING_") },
  "profiling activation must remain compatible with the previously published agent image"
)

agent_policy = resource(
  documents,
  "NetworkPolicy",
  "imdb-clone-agent-least-privilege",
  "imdb-clone"
)
pyroscope_egress = agent_policy.dig("spec", "egress").any? do |rule|
  rule.fetch("ports", []).any? { |port| port["port"] == 4040 } &&
    rule.fetch("to", []).any? do |destination|
      destination.dig(
        "namespaceSelector",
        "matchLabels",
        "kubernetes.io/metadata.name"
      ) == "observability" &&
        destination.dig("podSelector", "matchLabels", "app.kubernetes.io/name") == "pyroscope"
    end
end
assert_contract(pyroscope_egress, "agent NetworkPolicy must allow only private Pyroscope egress")

rules = resource(documents, "PrometheusRule", "observability-stack-alerts", "observability")
alert_names = rules.dig("spec", "groups").flat_map do |group|
  group.fetch("rules").map { |rule| rule["alert"] }.compact
end
%w[LokiUnavailable TempoUnavailable PyroscopeUnavailable AlloyUnavailable LokiDiscardingLogs].each do |alert_name|
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
