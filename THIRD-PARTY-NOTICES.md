# Third-party material and unresolved release notices

The Gradle wrapper JAR includes `META-INF/LICENSE`. Its exact license text is
also preserved in `GRADLE-WRAPPER-LICENSE.txt`. This does not license application
code or the Android SDK under the wrapper's terms.

`DEPENDENCY-LICENSE-INVENTORY.json` lists both version catalog declarations and
142 resolved `releaseRuntimeClasspath` artifacts, with artifact/POM hashes and
license declarations. Three Guava artifacts obtain declarations from their
parent POMs; those parent coordinates/hashes are recorded rather than silently
inventing terms. Every resolved artifact has a POM license declaration.

`THIRD-PARTY-LICENSES.json` preserves 14 embedded license/NOTICE texts found in
the resolved JAR/AAR archives, including nested classes JARs. Most upstream
artifacts provide declarations rather than full embedded texts; preservation
of available notices is not complete binary-distribution clearance. For example,
the external-protobuf artifact declares BSD-3-Clause, and OkHttp includes its
public-suffix-data notice. Exact license texts/source-offer obligations for data,
SDK and final APK distribution still require release-specific review.

The dependency/artifact resolution tasks passed locally without downloading or
auto-accepting an Android SDK license. A new APK build or instrumentation run is
not claimed. Vendor SDK and app-store terms remain independent.

No external logo, font or customer image is bundled in this candidate. Vector
and layout code still require the same first-party ownership confirmation as
other application source. No licensing authority is inferred from a Git author.
