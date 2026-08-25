# frozen_string_literal: true

repository_root = File.expand_path("../../../..", __dir__)
ci = File.read(File.join(repository_root, ".github/workflows/continuous-integration.yaml"))
release = File.read(File.join(repository_root, ".github/workflows/continuous-deployment.yaml"))

def assert_contract(condition, message)
  raise message unless condition
end

assert_contract(
  ci.match?(/on:\n  pull_request:\n    branches:\n      - master\n/),
  "CI must verify pull requests targeting master"
)
assert_contract(
  ci.match?(/  merge_group:\n    types:\n      - checks_requested\n/),
  "CI must verify merge queue candidates"
)
assert_contract(
  !ci.match?(/^  push:\n    branches:\n      - master$/),
  "CI must not repeat the full suite after merging to master"
)
assert_contract(
  ci.include?('group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}') &&
    ci.include?("cancel-in-progress: true"),
  "CI must cancel superseded pull-request runs"
)
%w[backend-build-test frontend-build-test agent-build-test infrastructure-validate].each do |job|
  assert_contract(ci.match?(/^  #{Regexp.escape(job)}:$/), "CI is missing required job #{job}")
end
%w[
  docker-build-backend
  container-smoke-backend
  docker-build-frontend
  container-smoke-frontend
].each do |target|
  assert_contract(
    ci.include?("run: make #{target}"),
    "CI must run the #{target} runtime image gate"
  )
end

assert_contract(release.include?("contents: write"), "release requires contents write permission")
assert_contract(
  release.include?("pull-requests: write"),
  "release requires pull-request write permission"
)
assert_contract(
  release.include?("group: versioned-app-release") &&
    release.include?("cancel-in-progress: false"),
  "releases must be serialized without cancellation"
)
assert_contract(
  release.include?('branch=release/v${version}-deployment'),
  "release must create a version-specific deployment branch"
)
%w[make\ verify-agent imdb-clone-agent APP_VERSION agent.yaml].each do |contract|
  assert_contract(
    release.include?(contract.tr("\\", "")),
    "release agent contract is missing #{contract}"
  )
end
assert_contract(
  release.include?("APP_VERSION=${{ steps.version.outputs.value }}"),
  "agent build metadata must use the unprefixed release version"
)
assert_contract(
  release.include?("EXPECTED_APP_VERSION: ${{ steps.version.outputs.value }}"),
  "release must strictly verify every published image version"
)
assert_contract(
  release.include?('git push --set-upstream origin "$RELEASE_BRANCH"'),
  "release must push manifests to a deployment branch"
)
assert_contract(
  release.include?("gh pr create") &&
    release.include?("--base master") &&
    release.include?('--head "$RELEASE_BRANCH"'),
  "release must open a deployment pull request"
)
push_commands = release.lines.grep(/\bgit push\b/).map(&:strip)
expected_push_commands = [
  'git push --set-upstream origin "$RELEASE_BRANCH"',
  'git push origin "${{ steps.version.outputs.tag }}"'
]
assert_contract(
  push_commands.sort == expected_push_commands.sort,
  "release may push only its deployment branch and annotated tag"
)
assert_contract(!release.include?("[skip ci]"), "deployment commits must run pull-request CI")

tag_step = release.index("- name: Create release tag")
pull_request_step = release.index("- name: Create deployment pull request")
assert_contract(!tag_step.nil? && !pull_request_step.nil?, "release completion steps are missing")
assert_contract(
  tag_step < pull_request_step,
  "release tag must exist before the deployment pull request is offered"
)

puts "Protected-branch release workflow contracts passed."
