# Phase 0 slice C — results

2026-07-08. Synthetic behavior corpus (MockWebServer-backed parity tests for what the Maven
Central corpus cannot exercise) per `PHASE-0-SPEC.md` §4, plus the four additive
snapshot-coverage fields from the slice A review. All parity tests green: **113 tests, 0
failures** — the 75 slice-A tests unmodified (`DeterminismTest` 23, `IdentityContractsTest` 46,
`SerializedLstCompatibilityTest` 6) + **38 new synthetic tests** in
`rewrite-maven/src/test/java/org/openrewrite/maven/parity/synthetic/`.

## Findings (the interesting part)

1. **L-P0-007 — unresolved `${...}` credentials leak on the POM path (ledgered, new).**
   Settings-server credentials with unresolvable placeholders are sent verbatim as basic auth
   on the first request — the mock server observed `Authorization: Basic
   base64("${env.PARITY_NO_SUCH_USER}:${env.PARITY_NO_SUCH_PASSWORD}")` — and resolution only
   recovers through the authenticated→anonymous 4xx retry (2 requests instead of 1).
   `MavenArtifactDownloader` (jar path) skips `${`-containing credentials up front, so the two
   downloaders disagree; #6845's "treat as no credentials" intent holds on the pom path only as
   an emergent effect of the retry. Pinned by
   `AuthFallbackTest.unresolvedPlaceholderCredentialsFallBackToAnonymous`.
2. **L-P0-006 — HTML-index metadata derivation (ledgered).** Happy path pinned (3 requests:
   metadata 404 → index GET → derived-version pom GET, range `[1.0,2.0]` resolves to 2.0) and
   the self-disabling flag pinned: after a 403 on one artifact's index scrape, a second
   artifact's index on the same repository is **never requested** (3 requests total across 4
   scopes × 2 GAs — metadata negative caching covers the rest). Keep/kill remains open; the
   tests document what exists either way. `HtmlIndexMetadataTest`.
3. **Seam trap: context-injected repositories are dropped once a settings object is present.**
   `MavenExecutionContextView.getRepositories(settings, profiles)` returns *only* the settings'
   active-profile repositories; `ctx.setRepositories(...)` entries are used solely to enrich
   same-id settings repositories (credentials/knownToExist). Documented as "fallback" in a2 §4
   and the javadoc, but it silently no-ops a host's injected repos the moment
   `setMavenSettings` is called — the synthetic mirror/auth tests must declare their
   repositories in the pom for this reason. Phase 2/3 must preserve this exactly (or ledger a
   deliberate change); it is easy to get wrong in a replacement.
4. **Marker discard extends to transitive failures.** L-P0-004 talks about unresolvable
   *direct* dependencies; the synthetic tests confirm any aggregated `MavenDownloadingExceptions`
   from `resolveDependencies` — including a *transitive* jar-type pom failure
   (`MirrorMatrixTest.mirrorPolicyOverridesRepositoryPolicy`) — makes `MavenParser` attach
   `ParseExceptionResult` *instead of* the `MavenResolutionResult` marker. The synthetic
   harness therefore models failed resolutions explicitly (`Resolution.failed()`).
5. **Test-infra: the https-preference probe stalls against a plaintext MockWebServer.** The
   probe's TLS ClientHello never parses as an HTTP request line, so MockWebServer waits for
   more bytes while the client waits for a ServerHello — a read-timeout stall that the
   downloader's retry policy then retries 5×: ~125s per probed repository with the default JDK
   sender, ~365s when a settings object with `<servers>` is present (the 30s read-timeout
   override applies). A real plaintext server fails the handshake immediately. Fixed at the
   documented `HttpSender` host seam: `SyntheticHarness.FailFastHttpsSender` fails
   `https://localhost` instantly (standing in for the immediate TLS failure) and delegates
   everything else to the real `HttpUrlConnectionSender`. The https→http fallback logic is
   still exercised; only that one leg's failure is simulated. Mirror+auth classes: 6m12s → 0.7s.
6. **Spike lesson institutionalized:** `MockMavenRepo`'s dispatcher strips bodies from every
   HEAD response centrally, so no individual test can poison MockWebServer keep-alive.

## Negative-caching request-count evidence (NegativeCachingTest)

| scenario | first resolve | second resolve (same ctx/cache) |
|---|---|---|
| 404 pom (+404 jar HEAD) | exactly 2 requests (`GET .pom`, `HEAD .jar`) — the other 3 scopes hit the negative cache within the first resolve | **0 new requests**; failure still surfaced from cache (errors 1→2) |
| 408 / 425 / 429 / 500 / 503 (parameterized) | exactly 4 `GET`s (one per resolve scope — nothing cached) | 4 more (8 total) |
| dead endpoint (closed port) | probed once; `host:port` recorded in `ctx.unreachableEndpoints`; a second repo on the same host:port under a different id/path is skipped **without any connection attempt** (`repositoryAccessFailedPreviously` for it, no `repositoryAccessFailed`) | n/a (single resolve, evidenced via event multiset) |

Related counts pinned elsewhere: pom-less jar = exactly `GET .pom`(404) + `HEAD .jar`(200), then
positively cached (`PomlessJarTest`); pinned snapshot = 1 pom GET, zero metadata requests;
snapshots-disabled repo = zero requests while the enabled repo serves
(`SnapshotResolutionTest`).

## Built

- `parity/synthetic/` (new subpackage): `MockMavenRepo` (path-keyed dispatcher, HEAD-body
  stripping, request/artifact-request logs), `SyntheticHarness` (MavenParser-path resolution
  with hermetic per-session context, nullable-marker `Resolution`, `FailFastHttpsSender`), and
  test classes `MirrorMatrixTest` (10: `*`, `external:*` both directions, `!excl`,
  mirror-of-list match+non-match, never-mirror-local, first-match-wins, policy override,
  mirror id/uri attribution), `AuthFallbackTest` (5), `SnapshotResolutionTest` (5: base-version
  vs dated, classifier-aware selection, `<snapshot>` fallback, ctx pinning, snapshot-policy
  skip), `NegativeCachingTest` (7), `FileRepoTest` (5: `maven-metadata-local.xml` incl. decoy
  `maven-metadata.xml` ignored, missing-jar unusable, empty-jar unusable, pom-packaging needs
  no jar, file-repo pom-less jar), `PomlessJarTest` (1), `HtmlIndexMetadataTest` (2),
  `SnapshotCoverageTest` (3).
- MockWebServer was already a rewrite-maven test dependency (`com.squareup.okhttp3:mockwebserver:4.+`);
  no build-file change was needed.
- Additive `ResolutionSnapshot` fields (never touching existing ones): per-node `licenses`
  (`name|Type`), resolution-level `parentGav` + `moduleGavs` (flattened GAV strings — the
  comment in code warns that serializing live parent/module references recurses the reactor),
  pom-level `pluginRepositories` (id+uri) and `subprojects`. All asserted by
  `SnapshotCoverageTest` (multi-module parent/child link included); slice A's 75 tests re-ran
  green against the new fields.
- Ledger: appended L-P0-006 and L-P0-007.

## Caveats

- The https probe leg in synthetic tests is a simulated instant failure (finding 5); every
  other request, including the http probe legs, hits the real MockWebServer through the real
  `HttpUrlConnectionSender`.
- Pom-less-jar behavior matches Maven's missing-descriptor tolerance per the a4 #64 matrix, so
  no ledger row; the test javadoc names Maven as the Phase 3 parity reference.
