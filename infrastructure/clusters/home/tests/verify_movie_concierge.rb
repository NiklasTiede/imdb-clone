# frozen_string_literal: true

require "json"
require "yaml"

rendered_path = ARGV.fetch(0, "/tmp/popcorn-society-home-apps.yaml")
repository_root = File.expand_path("../../../..", __dir__)
documents = YAML.load_stream(File.read(rendered_path)).compact
# Only the just-published compatible image release switches the active env names.
documents.each do |document|
  document.dig("spec", "template", "spec", "containers")&.each do |app|
    entries = app.fetch("env", [])
    if ENV["EXPECTED_APP_VERSION"] && !ENV["EXPECTED_APP_VERSION"].empty?
      assert_no_legacy = entries.none? { |entry| entry["name"].match?(/\AIMDB_(AGENT|CLONE)_/) }
      raise "new releases must use renamed runtime settings" unless assert_no_legacy
    end
    names = entries.map { |entry| entry["name"] }
    # A staged domain overlay can coexist with the old base before image rollout.
    # Match Settings: explicit new keys win independently of manifest array order.
    app["env"] = entries.reject do |entry|
      entry["name"].start_with?("IMDB_AGENT_") &&
        names.include?(entry["name"].sub("IMDB_AGENT_", "POPCORN_SOCIETY_AGENT_"))
    end.map do |entry|
      entry.merge("name" => entry["name"].sub("POPCORN_SOCIETY_AGENT_", "IMDB_AGENT_"))
    end
  end
end

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
    /\A#{Regexp.escape(repository).sub("imdb\\-clone", "(?:imdb-clone|popcorn-society)")}:v(?<version>\d+\.\d+\.\d+)@sha256:[0-9a-f]{64}\z/
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
if expected_app_version && !expected_app_version.empty?
  [container, backend_container, frontend_container].each do |app|
    assert_contract(app.fetch("image").include?("/popcorn-society-"),
      "new releases must use Popcorn Society repositories")
  end
end
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
assert_contract(
  secret.fetch("stringData").keys.sort == %w[
    mcp-bearer-token openai-api-key openai-live-api-key tmdb-read-access-token xai-api-key
  ],
  "runtime secret fields drifted"
)
assert_contract(
  encrypted_values.all? { |value| value.start_with?("ENC[AES256_GCM,") },
  "runtime secret contains plaintext"
)

agent_secrets = pod_spec.fetch("volumes").find { |volume| volume["name"] == "runtime-secrets" }
assert_contract(
  agent_secrets.dig("secret", "items").map { |item| item["key"] }.sort ==
    %w[mcp-bearer-token openai-api-key openai-live-api-key xai-api-key],
  "agent must receive only its own provider and MCP credentials"
)
quota = resource(documents, "PersistentVolumeClaim", "imdb-clone-voice-quota", "imdb-clone")
assert_contract(quota.dig("spec", "accessModes") == ["ReadWriteOnce"], "quota must be single-writer")
assert_contract(
  quota.dig("metadata", "annotations", "argocd.argoproj.io/sync-wave") ==
    agent.dig("metadata", "annotations", "argocd.argoproj.io/sync-wave"),
  "WaitForFirstConsumer quota PVC must sync with its consuming Deployment"
)
assert_contract(
  pod_spec.fetch("volumes").any? do |volume|
    volume.dig("persistentVolumeClaim", "claimName") == "imdb-clone-voice-quota" &&
      container.fetch("volumeMounts").any? do |mount|
        mount["name"] == volume["name"] && mount["mountPath"] == "/var/lib/movie-concierge"
      end
  end,
  "voice quota must survive pod replacement"
)

voice_enabled = %w[IMDB_AGENT_VOICE_ENABLED IMDB_AGENT_VOICE_LIVE_ENABLED].any? do |name|
  environment[name] == "true"
end
if voice_enabled
  assert_contract((image_versions["agent"].split(".").map(&:to_i) <=> [1, 5, 0]) >= 0,
                  "release the production-capable image before enabling voice")
  %w[IMDB_AGENT_VOICE_ENABLED IMDB_AGENT_VOICE_LIVE_ENABLED].each do |name|
    assert_contract(environment[name] == "true", "both voice providers must remain selectable")
  end
  assert_contract(environment["IMDB_AGENT_VOICE_SESSION_SECONDS"] == "900", "voice time cap drifted")
  assert_contract(environment["IMDB_AGENT_VOICE_MAX_SESSIONS"] == "8", "legacy rollout cap drifted")
  assert_contract(environment["IMDB_AGENT_VOICE_BROWSER_SECONDS"] == "1500", "browser voice time cap drifted")
  assert_contract(environment["IMDB_AGENT_VOICE_SHARED_SECONDS"] == "6000", "shared voice time cap drifted")
  assert_contract(environment["IMDB_AGENT_VOICE_QUOTA_DATABASE"] ==
                  "/var/lib/movie-concierge/voice-quota.db", "persistent voice quota required")
  expected_voice_origins = ["https://imdb-clone.the-coding-lab.com"]
  if documents.any? { |document| document["kind"] == "Ingress" && document.dig("metadata", "name") == "popcorn-society-concierge-public" }
    expected_voice_origins << "https://popcornsociety.app"
  end
  assert_contract(JSON.parse(environment.fetch("IMDB_AGENT_VOICE_ALLOWED_ORIGINS")).sort ==
                  expected_voice_origins.sort, "voice origin drifted")
end

backend_environment = backend_container.fetch("env").to_h do |entry|
  [entry.fetch("name"), entry["value"]]
end
backend_secrets = backend.dig("spec", "template", "spec", "volumes").find do |volume|
  volume["name"] == "movie-concierge-runtime"
end
assert_contract(
  backend_secrets.dig("secret", "items").sort_by { |item| item["key"] } == [
    {"key" => "mcp-bearer-token", "path" => "movie_concierge_mcp_bearer_token"},
    {"key" => "tmdb-read-access-token", "path" => "TMDB_READ_ACCESS_TOKEN"}
  ],
  "backend must mount only the MCP token and TMDB credential"
)
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
headers = resource(documents, "Middleware", "imdb-clone-security-headers", "imdb-clone")
assert_contract(
  headers.dig("spec", "headers", "permissionsPolicy").split(",").map(&:strip).include?(
    "microphone=(self)"
  ),
  "same-origin microphone permission must be available"
)
connect_sources = headers.dig("spec", "headers", "contentSecurityPolicy")
  .split(";").map(&:split).find { |directive| directive.first == "connect-src" }
assert_contract(
  connect_sources && connect_sources.drop(1).any? do |source|
    source == "wss://imdb-clone.the-coding-lab.com"
  end,
  "browser CSP must allow the same-origin voice WebSocket"
)

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
