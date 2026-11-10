import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class ExportSafety(unittest.TestCase):
    def test_customer_identifiers_removed(self):
        for path in (ROOT / 'app').rglob('*'):
            if path.is_file() and path.suffix in {'.kt', '.kts', '.json', '.xml'}:
                text = path.read_text().lower()
                self.assertNotIn('nazarene', text, str(path))
                self.assertNotIn('manteno', text, str(path))

    def test_defaults_are_inert(self):
        config = json.loads((ROOT / 'app/src/main/assets/app_config.json').read_text())
        self.assertTrue(config['content']['sourceUrl'].startswith('https://example.invalid/'))
        self.assertFalse(config['analytics']['enabled'])
        self.assertFalse(config['features']['analytics'])

    def test_signing_has_no_debug_fallback(self):
        build = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertNotIn('signingConfigs.getByName("debug")', build)
        self.assertIn('ENABLE_RELEASE_SIGNING', build)
        self.assertIn('KEY_PASSWORD', build)

    def test_deep_link_host_is_configurable(self):
        build = (ROOT / 'app/build.gradle.kts').read_text()
        manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
        activity = (ROOT / 'app/src/main/java/com/dominickp/firetvandroidtv/ui/MainActivity.kt').read_text()
        self.assertIn('DEEP_LINK_HOST', build)
        self.assertIn('${deepLinkHost}', manifest)
        self.assertIn('BuildConfig.DEEP_LINK_HOST', activity)

    def test_unverified_art_omitted(self):
        self.assertFalse((ROOT / 'app/src/main/res/drawable-nodpi/hd_overhang_logo.png').exists())

    def test_gradle_distribution_is_checksum_pinned(self):
        properties = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
        self.assertIn('distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6', properties)


if __name__ == '__main__':
    unittest.main()
