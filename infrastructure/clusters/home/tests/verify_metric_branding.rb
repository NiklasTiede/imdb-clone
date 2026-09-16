# frozen_string_literal: true
require "json"
require "yaml"
require "tmpdir"

root = File.expand_path("../../../..", __dir__)
dashboards = Dir[File.join(root, "infrastructure/clusters/home/apps/observability/dashboards/*.yaml")]
expressions = dashboards.flat_map do |file|
  YAML.load_file(file).fetch("data", {}).values.flat_map do |json|
    JSON.parse(json).fetch("panels").flat_map { |panel| panel.fetch("targets", []).map { |target| target["expr"] }.compact }
  end
end.select { |expr| expr.include?("popcorn_society_") }
raise "no renamed metrics found" if expressions.empty?
# Validate the actual dashboard queries, with Grafana time macros resolved.
expressions = expressions.map { |expr| expr.gsub("$__rate_interval", "5m").gsub("$__range", "5m") }
expressions.each do |expr|
  expr.scan(/\bimdb_(?:agent|frontend|assistant|search)_\w+/).each do |legacy|
    raise "missing new metric for #{legacy}" unless expr.include?(legacy.sub("imdb_", "popcorn_society_"))
  end
end
query = expressions.find { |expr| expr.start_with?("sum by (outcome)") && expr.include?("agent_runs_total") }
raise "missing run-rate dashboard query" unless query

series = lambda do |brand, pod, step|
  {"series" => "#{brand}_agent_runs_total{pod=\"#{pod}\",outcome=\"success\"}", "values" => "0+#{step}x10"}
end
cases = [
  ["old deployment", [series.call("imdb", "old", 60)], 1],
  ["new deployment", [series.call("popcorn_society", "new", 120)], 2],
  ["rolling deployment", [series.call("imdb", "old", 60), series.call("popcorn_society", "new", 120)], 3],
  ["overlapping names on one target", [series.call("imdb", "same", 60), series.call("popcorn_society", "same", 120)], 2]
]
tests = cases.map do |name, input, expected|
  {"name" => name, "interval" => "1m", "input_series" => input,
   "promql_expr_test" => [{"expr" => query, "eval_time" => "10m",
     "exp_samples" => [{"labels" => '{outcome="success"}', "value" => expected}]}]}
end
rules = expressions.each_with_index.map { |expr, index| {"record" => "brand_query_#{index}", "expr" => expr} }
alerts = YAML.load_file(File.join(root, "infrastructure/clusters/home/apps/observability/agent-alerts.yaml"))
Dir.mktmpdir("popcorn-metric-branding-") do |dir|
  rules_path = File.join(dir, "rules.yaml")
  tests_path = File.join(dir, "tests.yaml")
  File.write(rules_path, YAML.dump({"groups" => [{"name" => "dashboard-syntax", "rules" => rules}] + alerts.dig("spec", "groups")}))
  File.write(tests_path, YAML.dump({"rule_files" => ["rules.yaml"], "evaluation_interval" => "1m", "tests" => tests}))
  File.chmod(0o755, dir)
  File.chmod(0o644, rules_path, tests_path)
  image = "prom/prometheus:v3.5.0"
  base = ["docker", "run", "--rm", "--entrypoint", "/bin/promtool", "-v", "#{dir}:/work:ro", "-w", "/work", image]
  abort "PromQL syntax check failed" unless system(*base, "check", "rules", "rules.yaml")
  abort "Metric transition checks failed" unless system(*base, "test", "rules", "tests.yaml")
end
puts "Metric rename: #{expressions.length} actual dashboard queries and old/new/mixed/overlap scenarios passed."
