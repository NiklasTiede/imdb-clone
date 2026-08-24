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

def deployment_container(documents, deployment_name, container_name)
  deployment = resource(documents, "Deployment", deployment_name, "imdb-clone")
  container = deployment.dig("spec", "template", "spec", "containers").find do |candidate|
    candidate["name"] == container_name
  end
  raise "missing #{container_name} container" if container.nil?

  [deployment, container]
end

def pinned_image_version(image, repository)
  match = image.match(
    /\A#{Regexp.escape(repository)}:v(?<version>\d+\.\d+\.\d+)@sha256:[0-9a-f]{64}\z/
  )
  raise "#{repository} image must use a semantic tag and immutable digest" if match.nil?

  match[:version]
end

agent = resource(documents, "Deployment", "imdb-clone-agent", "imdb-clone")
pod_spec = agent.dig("spec", "template", "spec")
container = pod_spec.fetch("containers").find { |candidate| candidate["name"] == "agent" }
raise "missing agent container" if container.nil?

backend, backend_container = deployment_container(
  documents,
  "imdb-clone-backend",
  "backend"
)
_frontend, frontend_container = deployment_container(
  documents,
  "imdb-clone-frontend",
  "frontend"
)
image_versions = {
  "agent" => pinned_image_version(
    container.fetch("image"),
    "niklastiede/imdb-clone-agent"
  ),
  "backend" => pinned_image_version(
    backend_container.fetch("image"),
    "niklastiede/imdb-clone-backend"
  ),
  "frontend" => pinned_image_version(
    frontend_container.fetch("image"),
    "niklastiede/imdb-clone-frontend"
  )
}

expected_app_version = ENV["EXPECTED_APP_VERSION"]
unless expected_app_version.nil? || expected_app_version.empty?
  assert_contract(
    expected_app_version.match?(/\A\d+\.\d+\.\d+\z/),
    "EXPECTED_APP_VERSION must be semantic"
  )
  release_version = File.read(File.join(repository_root, "VERSION")).strip
  assert_contract(
    release_version == expected_app_version,
    "strict image verification must match VERSION"
  )
  image_versions.each do |deployable, image_version|
    assert_contract(
      image_version == expected_app_version,
      "#{deployable} image must match release v#{expected_app_version}"
    )
  end
end

assert_contract(agent.dig("spec", "replicas") == 1, "agent pilot must use one replica")
assert_contract(
  agent.dig("spec", "strategy") == { "type" => "Recreate" },
  "in-memory pilot must never run overlapping replicas"
)
assert_contract(pod_spec["automountServiceAccountToken"] == false, "agent token mount must be off")
assert_contract(pod_spec.dig("securityContext", "runAsNonRoot") == true, "agent must be non-root")
assert_contract(
  pod_spec.dig("securityContext", "seccompProfile", "type") == "RuntimeDefault",
  "agent must use RuntimeDefault seccomp"
)
assert_contract(
  container.dig("securityContext", "readOnlyRootFilesystem") == true,
  "agent root filesystem must be read-only"
)
assert_contract(
  container.dig("securityContext", "allowPrivilegeEscalation") == false,
  "agent privilege escalation must be disabled"
)
assert_contract(
  container.dig("securityContext", "capabilities", "drop") == ["ALL"],
  "agent must drop every Linux capability"
)
assert_contract(container.key?("startupProbe"), "agent startup probe is required")
assert_contract(container.key?("readinessProbe"), "agent readiness probe is required")
assert_contract(container.key?("livenessProbe"), "agent liveness probe is required")
assert_contract(container.dig("resources", "requests", "memory"), "agent memory request is required")
assert_contract(container.dig("resources", "limits", "memory"), "agent memory limit is required")

environment = container.fetch("env").to_h { |entry| [entry.fetch("name"), entry["value"]] }
assert_contract(environment["IMDB_AGENT_ENVIRONMENT"] == "production", "production mode required")
assert_contract(environment["IMDB_AGENT_MAX_CONCURRENT_RUNS"] == "2", "run limit drifted")
assert_contract(environment["IMDB_AGENT_MAX_REQUEST_BODY_BYTES"] == "4096", "body limit drifted")
assert_contract(environment["IMDB_AGENT_PROJECT_COST_LIMIT_USD"] == "20.00", "cost cap drifted")
assert_contract(environment["IMDB_AGENT_OTEL_TRACING_ENABLED"] == "true", "tracing must be on")
assert_contract(
  environment["IMDB_AGENT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT"] ==
    "http://alloy.observability.svc.cluster.local:4318/v1/traces",
  "agent traces must use the cluster-local Alloy endpoint"
)
assert_contract(
  environment["IMDB_AGENT_OTEL_TRACE_SAMPLE_RATIO"] == "1.0",
  "the low-volume agent must retain complete traces"
)
assert_contract(!environment.key?("OPENAI_API_KEY"), "provider key must never be an environment value")
assert_contract(
  !environment.key?("IMDB_AGENT_MCP_BEARER_TOKEN"),
  "MCP token must never be an environment value"
)

secret = resource(documents, "Secret", "movie-concierge-runtime", "imdb-clone")
encrypted_values = secret.fetch("stringData").values
assert_contract(encrypted_values.length == 2, "runtime secret must contain exactly two fields")
assert_contract(
  encrypted_values.all? { |value| value.start_with?("ENC[AES256_GCM,") },
  "runtime secret contains plaintext"
)

backend_environment = backend_container.fetch("env").to_h do |entry|
  [entry.fetch("name"), entry["value"]]
end
assert_contract(
  backend_environment["movie_concierge_mcp_enabled"] == "true",
  "Java production MCP must be enabled"
)
assert_contract(
  backend_environment["MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT"] ==
    "http://alloy.observability.svc.cluster.local:4318/v1/traces",
  "backend traces must use the cluster-local Alloy endpoint"
)
assert_contract(
  backend_container.fetch("volumeMounts").any? do |mount|
    mount["name"] == "movie-concierge-runtime" && mount["readOnly"] == true
  end,
  "backend must mount the MCP token read-only"
)

ingress = resource(documents, "Ingress", "imdb-clone-concierge-public", "imdb-clone")
paths = ingress.fetch("spec").fetch("rules").flat_map { |rule| rule.dig("http", "paths") }
assert_contract(paths.map { |path| path["path"] } == ["/concierge-api/v1"], "public agent path drifted")

network_policy = resource(
  documents,
  "NetworkPolicy",
  "imdb-clone-agent-least-privilege",
  "imdb-clone"
)
assert_contract(
  network_policy.dig("spec", "policyTypes").sort == %w[Egress Ingress],
  "agent must be isolated in both directions"
)
assert_contract(network_policy.dig("spec", "ingress").length == 2, "agent ingress allowlist drifted")
egress = network_policy.dig("spec", "egress")
assert_contract(egress.length == 5, "agent egress allowlist drifted")
assert_contract(
  egress.any? do |rule|
    rule.fetch("ports", []).any? { |port| port["port"] == 4318 } &&
      rule.fetch("to", []).any? do |destination|
        destination.dig("namespaceSelector", "matchLabels", "kubernetes.io/metadata.name") ==
          "observability" &&
          destination.dig("podSelector", "matchLabels", "app.kubernetes.io/name") == "alloy"
      end
  end,
  "agent OTLP egress must be limited to Alloy"
)
assert_contract(
  egress.any? do |rule|
    rule.fetch("ports", []).any? { |port| port["port"] == 4040 } &&
      rule.fetch("to", []).any? do |destination|
        destination.dig("namespaceSelector", "matchLabels", "kubernetes.io/metadata.name") ==
          "observability" &&
          destination.dig("podSelector", "matchLabels", "app.kubernetes.io/name") == "pyroscope"
      end
  end,
  "agent profiling egress must be limited to Pyroscope"
)

service_monitor = resource(documents, "ServiceMonitor", "imdb-clone-agent", "imdb-clone")
assert_contract(
  service_monitor.dig("spec", "endpoints", 0, "path") == "/metrics",
  "agent metrics scrape path drifted"
)

llama_service_monitor = resource(
  documents,
  "ServiceMonitor",
  "imdb-clone-llama-cpp",
  "databases"
)
assert_contract(
  llama_service_monitor.dig("spec", "endpoints", 0, "path") == "/metrics",
  "llama.cpp metrics scrape path drifted"
)
assert_contract(
  llama_service_monitor.dig("spec", "selector", "matchLabels", "app.kubernetes.io/name") ==
    "imdb-clone-llama-cpp",
  "llama.cpp ServiceMonitor must select only the embedding service"
)

rules = resource(documents, "PrometheusRule", "imdb-clone-agent", "imdb-clone")
alert_names = rules.dig("spec", "groups").flat_map do |group|
  group.fetch("rules").map { |rule| rule["alert"] }.compact
end
%w[
  MovieConciergeDown
  MovieConciergeHighErrorRate
  MovieConciergeMcpFailures
  MovieConciergeProviderFailures
  MovieConciergeCapacityRejected
  MovieConciergeBudgetExhausted
  MovieConciergeProcessBudgetNearlyExhausted
].each do |alert_name|
  assert_contract(alert_names.include?(alert_name), "missing alert #{alert_name}")
end

dashboard_manifest = YAML.load_file(
  File.join(
    repository_root,
    "infrastructure/clusters/home/apps/observability/dashboards/agent-overview.yaml"
  )
)
dashboard = JSON.parse(dashboard_manifest.dig("data", "agent-overview.json"))
assert_contract(dashboard["uid"] == "imdb-agent-overview", "agent dashboard UID drifted")
assert_contract(dashboard.fetch("panels").length >= 10, "agent dashboard is incomplete")

puts "Movie Concierge production manifest contracts passed."
