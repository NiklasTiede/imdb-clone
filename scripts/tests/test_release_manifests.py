import importlib.util
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("release", ROOT / "scripts/update-release-manifests.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseManifestTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.apps = Path(self.temp.name)
        for service in release.SERVICES:
            shutil.copyfile(ROOT / f"infrastructure/clusters/home/apps/{service}.yaml", self.apps / f"{service}.yaml")
        self.digests = {service: "sha256:" + str(i) * 64 for i, service in enumerate(release.SERVICES)}

    def test_migrates_then_updates_new_repository_without_changing_resource_names(self):
        for version in ("1.7.0", "1.7.1"):
            release.update_manifests(self.apps, "niklastiede", version, self.digests)
            for service in release.SERVICES:
                text = (self.apps / f"{service}.yaml").read_text()
                self.assertIn(f"niklastiede/popcorn-society-{service}:v{version}@{self.digests[service]}", text)
                self.assertIn(f"name: imdb-clone-{service}\n", text)
                self.assertIn("namespace: imdb-clone\n", text)
                self.assertNotIn("name: IMDB_AGENT_", text)
                self.assertNotIn("name: IMDB_CLONE_", text)
            backend = (self.apps / "backend.yaml").read_text()
            self.assertEqual(backend.count("value: popcorn-society-backend"), 2)
            self.assertIn("imdb-clone-postgresql.databases.svc.cluster.local", backend)
            agent = (self.apps / "agent.yaml").read_text()
            self.assertIn("claimName: imdb-clone-voice-quota", agent)
            self.assertIn("name: POPCORN_SOCIETY_AGENT_VOICE_QUOTA_DATABASE", agent)

    def test_invalid_last_digest_does_not_partially_rewrite_manifests(self):
        before = {p: p.read_bytes() for p in self.apps.iterdir()}
        self.digests["agent"] = "sha256:not-a-digest"
        with self.assertRaises(ValueError):
            release.update_manifests(self.apps, "niklastiede", "1.7.0", self.digests)
        self.assertEqual(before, {p: p.read_bytes() for p in self.apps.iterdir()})

    def test_missing_or_duplicate_image_fails_before_any_write(self):
        path = self.apps / "agent.yaml"
        path.write_text(path.read_text().replace("image: niklastiede/", "image: unexpected/extra/"))
        before = {p: p.read_bytes() for p in self.apps.iterdir()}
        with self.assertRaises(ValueError):
            release.update_manifests(self.apps, "niklastiede", "1.7.0", self.digests)
        self.assertEqual(before, {p: p.read_bytes() for p in self.apps.iterdir()})


if __name__ == "__main__":
    unittest.main()
