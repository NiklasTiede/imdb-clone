# frozen_string_literal: true

require "json"
require "open3"
require "yaml"

def render(directory)
  output, status = Open3.capture2("kubectl", "kustomize", directory)
  raise "Kustomize failed for #{directory}" unless status.success?

  YAML.load_stream(output).compact
end

def find(documents, kind, name)
  documents.find { |doc| doc["kind"] == kind && doc.dig("metadata", "name") == name && doc.dig("metadata", "namespace") == "imdb-clone" } ||
    raise("Missing #{kind}/#{name}")
end

def check(condition, message)
  raise message unless condition
end

def env(documents, deploy, container)
  find(documents, "Deployment", deploy).dig("spec", "template", "spec", "containers")
    .find { |item| item["name"] == container }.fetch("env")
    .to_h { |item| [item["name"], item["value"]] }
end

preview = render(File.join(__dir__, "preview"))
cutover = render(File.join(__dir__, "cutover"))
public_ingress = find(preview, "Ingress", "popcorn-society-public")
check(public_ingress.dig("spec", "tls", 0, "secretName") == "popcorn-society-public-tls", "New TLS must not replace the legacy certificate")
check(public_ingress.dig("spec", "tls", 0, "hosts").sort == ["popcornsociety.app", "www.popcornsociety.app"], "Certificate names drifted")
paths = public_ingress.dig("spec", "rules", 0, "http", "paths")
expected = {"/api" => "imdb-clone-backend", "/oauth2" => "imdb-clone-backend", "/login/oauth2" => "imdb-clone-backend", "/webauthn" => "imdb-clone-backend", "/login/webauthn" => "imdb-clone-backend", "/" => "imdb-clone-frontend"}
check(paths.to_h { |p| [p["path"], p.dig("backend", "service", "name")] } == expected, "New domain must route API, OAuth and WebAuthn to the backend")

agent_env = env(preview, "imdb-clone-agent", "agent")
origins = JSON.parse(agent_env.fetch("IMDB_AGENT_VOICE_ALLOWED_ORIGINS"))
check(origins.sort == ["https://imdb-clone.the-coding-lab.com", "https://popcornsociety.app"], "Voice must accept exactly the migration origins")
check(JSON.parse(agent_env.fetch("IMDB_AGENT_ALLOWED_HOSTS")).include?("popcornsociety.app"), "Agent must trust the new hostname")
check(env(preview, "imdb-clone-backend", "backend")["SPRING_PROFILES_ACTIVE"] == "prod,popcorn", "New RP/email origin profile must be activated")
voice = find(preview, "Ingress", "popcorn-society-concierge-public")
check(voice.dig("spec", "rules", 0, "host") == "popcornsociety.app", "Voice ingress host drifted")
check(voice.dig("spec", "rules", 0, "http", "paths", 0, "backend", "service", "name") == "imdb-clone-agent", "Voice ingress must reach the agent")
csp = find(preview, "Middleware", "imdb-clone-security-headers").dig("spec", "headers", "contentSecurityPolicy")
check(csp.include?("wss://popcornsociety.app"), "CSP must allow the new voice WebSocket")

preview_middleware = "imdb-clone-popcorn-society-preview@kubernetescrd"
check(public_ingress.dig("metadata", "annotations", "traefik.ingress.kubernetes.io/router.middlewares").include?(preview_middleware), "Preview must prevent indexing")
live = find(cutover, "Ingress", "popcorn-society-public")
check(!live.dig("metadata", "annotations", "traefik.ingress.kubernetes.io/router.middlewares").include?(preview_middleware), "Cutover must remove preview noindex")

legacy = find(cutover, "Ingress", "imdb-clone-public")
legacy_paths = legacy.dig("spec", "rules", 0, "http", "paths").map { |p| p["path"] }
check(legacy_paths.sort == expected.keys.reject { |p| p == "/" }.sort, "Legacy API/auth routes must remain; only its page route moves")
redirect = find(cutover, "Middleware", "popcorn-society-canonical").dig("spec", "redirectRegex")
regex = Regexp.new(redirect.fetch("regex"))
check(redirect["permanent"], "Search migration requires permanent redirects")
{
  "https://imdb-clone.the-coding-lab.com/movie?id=123&utm_source=cv" => "https://popcornsociety.app/movie?id=123&utm_source=cv",
  "https://www.popcornsociety.app/reset-password?token=a%2Bb" => "https://popcornsociety.app/reset-password?token=a%2Bb",
  "http://imdb-clone.the-coding-lab.com/" => "https://popcornsociety.app/"
}.each do |source, expected_url|
  match = regex.match(source)
  check(!match.nil?, "Missing redirect for #{source}")
  result = redirect.fetch("replacement").gsub(/\$\{(\d+)\}/) { match[Regexp.last_match(1).to_i] }
  check(result == expected_url, "Redirect lost path or query")
end
check(!regex.match?("https://popcornsociety.app/movie?id=123"), "Canonical domain must never redirect to itself")
check(!regex.match?("https://the-coding-lab.com/"), "Blog must not be redirected")
puts "Popcorn Society migration: routing, TLS separation, voice, noindex removal and redirects passed."
