# frozen_string_literal: true

require "minitest/autorun"
require "open3"
require "tmpdir"

class VerifyMetricBrandingPermissionsTest < Minitest::Test
  def test_promtool_workspace_is_readable_by_the_container_user
    Dir.mktmpdir("fake-docker-") do |bin_dir|
      docker = File.join(bin_dir, "docker")
      File.write(docker, <<~'RUBY')
        #!/usr/bin/env ruby
        mount = ARGV.each_cons(2).find { |flag, _value| flag == "-v" }&.last
        abort "missing workspace mount" unless mount

        workspace = mount.split(":", 2).first
        mode = File.stat(workspace).mode & 0o777
        abort format("workspace mode %<mode>03o is not container-readable", mode: mode) unless (mode & 0o005) == 0o005

        Dir[File.join(workspace, "*.yaml")].each do |path|
          file_mode = File.stat(path).mode & 0o777
          abort format("%<path>s mode %<mode>03o is not container-readable", path: path, mode: file_mode) if (file_mode & 0o004).zero?
        end
      RUBY
      File.chmod(0o755, docker)

      script = File.expand_path("verify_metric_branding.rb", __dir__)
      stdout, stderr, status = Open3.capture3({"PATH" => "#{bin_dir}:#{ENV.fetch("PATH")}"}, "ruby", script)

      assert status.success?, "#{stdout}\n#{stderr}"
    end
  end
end
