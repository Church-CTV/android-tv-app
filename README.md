# Church CTV — Android TV and Fire TV application

Kotlin, Compose and Media3 application template, exported from source revision
`fc91589b1a0078cc7b04f814ca8b5942ceb2ef23` without Git history. This is a
sanitized starting point, not a production-ready release or a parity claim.
The default feed uses `example.invalid`; analytics, deep linking, crash
reporting and live-service features are disabled in the bundled example.

## Build

Install JDK 21 and Android SDK platform 35 and matching build tools. Set
`ANDROID_HOME` or create the ignored Android `local.properties` with `sdk.dir`.
Then run `./gradlew testDebugUnitTest assembleDebug`. Run the independent
export checks with `python3 -m unittest discover -s tests -v`. Android
instrumentation tests require a separately configured emulator or device and
are not run by the hosted workflow. Debug builds use Android's debug key;
never distribute them as store releases.

## Configuration and secrets

Copy `app/src/main/assets/app_config.json` to `private-config/app_config.json`
and edit the copy for your channel. Build with
`./gradlew assembleDebug -PAPP_ASSETS_DIR=private-config` to replace the
bundled asset directory. The file is embedded in the application: **never put
credentials or private data in it**. Set the content feed, server base URL,
analytics and heartbeat URLs and feature flags deliberately; use only
authorized HTTPS endpoints. Configuration loading is bundled/fallback, not
a verified server-config or validation implementation. `example.invalid` is
intentionally nonfunctional.

Set `-PAPPLICATION_ID=com.yourcompany.channel` for your store identity, and
`-PDEEP_LINK_HOST=your-domain.example` to set both manifest and parser host.
Enable `features.deepLinking` in your config only when configured. The
namespace remains the source namespace; it is not a credential. Confirm app
link/domain ownership and store identity before release. Version inputs are
`VERSION_CODE` and `VERSION_NAME` environment variables or Gradle properties.

Release builds are unsigned unless explicitly enabled. To opt in, provide
`-PENABLE_RELEASE_SIGNING=true` plus all four values: ignored
`keystore.properties` keys `keystore.file`, `keystore.password`,
`keystore.alias`, `key.password`; or environment variables `KEYSTORE_FILE`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, respectively. Properties
take precedence; a missing value or missing keystore file fails configuration.
Protect the keystore and passwords; paths are resolved from the project root.

The included Actions workflow uses no deployment credentials and does not
sign or deploy. If adding a separate authorized release workflow, store the
keystore bytes and both passwords as Actions **secrets**, materialize the
keystore in a temporary runner path, and delete it afterward. The alias is
also safe to keep as a secret. Domain, application ID and version are
nonsecret Actions **variables**, not credentials. Do not print secrets,
commit a populated properties file, or run release signing on untrusted PRs.

## Omissions and rights

Private agent instructions, operational records, customer profiles, signing
material, packages, CI deployment automation and an unverified customer logo
are omitted. Replace branding only with assets you have rights to use. No
project license or third-party notice was present in the selected source;
rights and redistribution authorization remain unresolved. Dependencies and
versions are declared in `gradle/libs.versions.toml` and build files; review
their licenses and provide required notices before redistribution. Gradle
wrapper scripts and its jar are retained as upstream tooling. Parent export
maintainers own common legal/security documents.
