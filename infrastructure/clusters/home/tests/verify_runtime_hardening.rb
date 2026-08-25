# frozen_string_literal: true

require "yaml"

rendered_path = ARGV.fetch(0, "/tmp/imdb-clone-home-apps.yaml")
documents = YAML.load_stream(File.read(rendered_path)).compact

def assert_contract(condition, message)
  raise message unless condition
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

def deployment_container(documents, deployment_name, container_name)
  deployment = resource(documents, "Deployment", deployment_name, "imdb-clone")
  container = deployment.dig("spec", "template", "spec", "containers").find do |candidate|
    candidate["name"] == container_name
  end
  raise "missing #{container_name} container" if container.nil?

  [deployment, container]
end

def assert_http_probe(container, probe_name, path, port)
  probe = container.fetch(probe_name)
  assert_contract(!probe.key?("tcpSocket"), "#{probe_name} must not use a TCP socket")
  assert_contract(
    probe["httpGet"] == { "path" => path, "port" => port },
    "#{probe_name} must use HTTP GET #{path} on #{port}"
  )
  assert_contract(probe.fetch("timeoutSeconds") <= 3, "#{probe_name} timeout is too high")
  assert_contract(probe.fetch("failureThreshold").positive?, "#{probe_name} threshold is invalid")
end

def assert_hardened_deployment(documents, deployment_name, container_name, uid)
  deployment, container = deployment_container(documents, deployment_name, container_name)
  pod_spec = deployment.dig("spec", "template", "spec")
  service_account_name = deployment_name
  service_account = resource(documents, "ServiceAccount", service_account_name, "imdb-clone")

  assert_contract(
    service_account["automountServiceAccountToken"] == false,
    "#{service_account_name} service account token mount must be disabled"
  )
  assert_contract(
    pod_spec["serviceAccountName"] == service_account_name,
    "#{deployment_name} must use its dedicated service account"
  )
  assert_contract(
    pod_spec["automountServiceAccountToken"] == false,
    "#{deployment_name} pod token mount must be disabled"
  )

  pod_security = pod_spec.fetch("securityContext")
  assert_contract(pod_security["runAsNonRoot"] == true, "#{deployment_name} must be non-root")
  assert_contract(pod_security["runAsUser"] == uid, "#{deployment_name} uid drifted")
  assert_contract(pod_security["runAsGroup"] == uid, "#{deployment_name} gid drifted")
  assert_contract(pod_security["fsGroup"] == uid, "#{deployment_name} filesystem group drifted")
  assert_contract(
    pod_security["fsGroupChangePolicy"] == "OnRootMismatch",
    "#{deployment_name} filesystem group policy drifted"
  )
  assert_contract(
    pod_security.dig("seccompProfile", "type") == "RuntimeDefault",
    "#{deployment_name} must use RuntimeDefault seccomp"
  )

  container_security = container.fetch("securityContext")
  assert_contract(
    container_security["allowPrivilegeEscalation"] == false,
    "#{deployment_name} privilege escalation must be disabled"
  )
  assert_contract(
    container_security["readOnlyRootFilesystem"] == true,
    "#{deployment_name} root filesystem must be read-only"
  )
  assert_contract(
    container_security.dig("capabilities", "drop") == ["ALL"],
    "#{deployment_name} must drop every Linux capability"
  )

  assert_contract(
    container.fetch("volumeMounts", []).any? do |mount|
      mount["name"] == "tmp" && mount["mountPath"] == "/tmp"
    end,
    "#{deployment_name} must mount writable /tmp"
  )
  assert_contract(
    pod_spec.fetch("volumes", []).any? do |volume|
      volume["name"] == "tmp" && volume.dig("emptyDir", "medium") == "Memory"
    end,
    "#{deployment_name} must back /tmp with an in-memory emptyDir"
  )

  container
end

backend = assert_hardened_deployment(
  documents,
  "imdb-clone-backend",
  "backend",
  10_001
)
assert_contract(
  backend.fetch("ports").to_h { |port| [port["name"], port["containerPort"]] } ==
    { "http" => 8080, "management" => 8081 },
  "backend ports drifted"
)
assert_http_probe(
  backend,
  "startupProbe",
  "/actuator/health/liveness",
  "management"
)
assert_http_probe(
  backend,
  "readinessProbe",
  "/actuator/health/readiness",
  "management"
)
assert_http_probe(
  backend,
  "livenessProbe",
  "/actuator/health/liveness",
  "management"
)

frontend = assert_hardened_deployment(
  documents,
  "imdb-clone-frontend",
  "frontend",
  101
)
assert_contract(
  frontend.dig("ports", 0, "containerPort") == 8080,
  "frontend must listen on an unprivileged port"
)
%w[startupProbe readinessProbe livenessProbe].each do |probe_name|
  assert_http_probe(frontend, probe_name, "/", "http")
end

frontend_service = resource(documents, "Service", "imdb-clone-frontend", "imdb-clone")
assert_contract(
  frontend_service.dig("spec", "ports", 0, "port") == 80 &&
    frontend_service.dig("spec", "ports", 0, "targetPort") == "http",
  "frontend service must preserve public port 80 and target the named container port"
)

puts "Backend and frontend runtime hardening contracts passed."
