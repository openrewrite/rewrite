# Phase 0 slice A — results

2026-07-08. Snapshot/diff infrastructure + determinism/identity/serialization gates per
`PHASE-0-SPEC.md` §1–§2. All parity tests green: **75 tests (74 executed, 1 skipped manual
regenerator), 0 failures** — `DeterminismTest` 23, `IdentityContractsTest` 46,
`SerializedLstCompatibilityTest` 6. `:rewrite-maven:license` green.

## Findings (the interesting part)

1. **Determinism: zero masks needed.** Every fixture resolved twice with fresh isolated
   contexts + in-memory caches produces byte-identical snapshots. `masks.txt` is empty. The
   study-a1 suspect (plugin goal merge via `HashSet`) is order-*scrambling* but *stable*
   (String hash order is deterministic across JVMs), so it surfaced as divergence rows, not
   masks:
2. **L-P0-001 — classifier shadowing (ledgered).** Direct dependencies dedupe by
   groupId:artifactId only ("last declaration wins"), so two direct deps differing only by
   classifier collapse to the later declaration; the earlier requested `Dependency` gets
   `getResolvedDependency(...) == null`. Maven keys conflicts on g:a:classifier:type. Pinned by
   `IdentityContractsTest.classifierShadowedByGaDeduplication`.
3. **L-P0-002 — plugin goal order (ledgered).** Same-id execution merge unions goals through a
   `HashSet`: child `[enforce-child, display-info]` + parent `[enforce]` →
   `[display-info, enforce, enforce-child]`.
4. **L-P0-003 — plugin list order (ledgered).** A child plugin merging with a parent
   declaration is removed and re-appended, moving it to the *end* of `ResolvedPom.plugins`.
5. **Historical wire-format facts pinned by the old-release payload (8.41.1):** scopes map was
   written in `{Compile, Test, Runtime, Provided}` order (today: Compile, Runtime, Test,
   Provided) — old-LST readers must not assume today's order; GAV types had no
   `@JsonIdentityInfo` yet (payload exercises `BackwardCompatibleObjectIdModule`'s
   objects-without-`@ref` path); `dependencyManagementSorted` absent (payload exercises the
   lazy re-sort + binary-search lookup); `managedReference: null` was written (current
   `WRITE_ONLY` no longer writes it — pinned via `doesNotContain` on the current payload).
6. **Wire format is field-visibility-based.** Default getter detection would serialize derived
   properties (`getProjectPoms()` — whose `Map<Path,…>` keys cannot even deserialize — and
   `isMultiModulePom()`). The in-repo precedent is `RocksdbMavenPomCache` (field ANY,
   getter/is-getter/setter NONE, creator PUBLIC_ONLY); the test mapper is
   `ObjectMappers.propertyBasedMapper` + that visibility. Phase 2's shadow-mode serializer must
   use the same convention.
7. **Snapshot observation:** legacy fills `ResolvedGroupArtifactVersion.datedSnapshotVersion`
   with the plain version for every downloaded (non-snapshot) pom — visible as `"dated": "1.1"`
   on release nodes. Captured faithfully; the new engine must reproduce it (facade folding per
   DESIGN §4.2).
8. **file:// repo quirk (affects fixture authoring):** `MavenPomDownloader` skips a
   jar-packaging pom in a file repo unless a non-empty sibling `.jar` exists — hence stub jars
   in every fixture repo.

## What was built

Production (inert until Phase 2):
- `rewrite-maven/src/main/java/org/openrewrite/maven/internal/ResolutionEngineSelector.java`

Test infrastructure (`rewrite-maven/src/test/java/org/openrewrite/maven/parity/`):
- `ResolutionSnapshot` — spec §2 JSON: per-scope ordered lists with depth + `children` indices,
  `requestedRef` identity threading (`root[i]`/`node[j]`/`dm[i]`/`bom[i]` + `val:`/`inherited:`
  fallbacks for non-root declarations), event multiset, normalized errors, identity probes
  (`resolveNoChange` probed against the hermetic ctx via an overload; spec factory kept),
  pom projection (raw properties, DM with `bomGav`+`requestedRef`, ordered repos, plugins with
  executions). Additive-to-spec fields (more shape, deliberately): `type`, `classifier`,
  `optional`, `effectiveExclusions` on nodes; a `pluginManagement` array beside `plugins`.
- `ResolutionDiff` — ordered `(jsonPath, left, right)`; `masked(...)` applies the registry.
- `SnapshotNormalizer` — root-relative path masking (`<path>/…` preserves suffixes so distinct
  repos never collapse — losslessness), `<local>`, `<ts>`, first-line + URL-authority message
  normalization; mask registry loader (mask without ledger id throws).
- `RecordingResolutionListener` — all 13 events as `type:key`.
- `ParityHarness` — hermetic fixture resolution: file:// fixture repo only,
  `setAddCentralRepository(false)`, `setAddLocalRepository(false)`, fresh
  `InMemoryMavenPomCache` + ctx per resolution.
- `DeterminismTest`, `IdentityContractsTest`, `SerializedLstCompatibilityTest`.

Resources (`rewrite-maven/src/test/resources/parity/`):
- `fixtures/` — 11 hermetic fixture sets (project pom + `repo/`): parent-chain (3-level, DM +
  property from grandparent, in-pom `<repositories>` via injected property), profile-activation
  (jdk-range active / property-activated inactive under the env-var quirk / explicit),
  bom-import-single (Imported + Defined DM + transitive), bom-import-multi (BOM importing BOM),
  conflict-equal-depth (first wins), conflict-unequal-depth (nearest wins), exclusions,
  optional (direct resolves / transitive pruned), classifiers, plugins-executions
  (parent pluginManagement + same-id execution merge), property-indirection (chained
  `${dep.version}` → `${dep.version.actual}` across parent).
- `masks.txt` — empty registry (header comment only).
- `serialized-lsts/current.json` — current wire capture (machine-independent: fixture/user.dir
  URIs replaced) + `serialized-lsts/old-8.41.1.json` — generated by released
  `org.openrewrite:rewrite-maven:8.41.1` on an isolated classpath (throwaway Gradle project in
  the session scratchpad; 8.21.0 was tried first but predates `setAddCentralRepository` and
  `MavenParser.Builder.property`).

Ledger: `doc/maven-resolution-ledger.md` seeded with L-P0-001..003 (findings surfaced by these
tests only; DESIGN §9 seeds left to a later pass, per instructions).

## Deviations

- **`rewrite-maven/build.gradle.kts` (out of declared file scope, one addition):**
  `excludePatterns.add("**/parity/**")` in the *existing* `LicenseExtension` block.
  Forced: `licenseFormat` stamps `/** … */` onto the JSON payloads (invalid JSON, all
  serialization tests fail) and XML comments into fixture poms. Same precedent as the existing
  `**/unresolvable.txt` exclusion. Without it, `licenseFormat`/`license` cannot pass with
  verbatim payload/fixture data.
- Snapshot format: additive fields only (listed above); `requestedRef` gained the documented
  `val:`/`inherited:` fallbacks for declarations not owned by the root pom (transitives,
  parent-declared DM) — spec's `root[i]|node[j]`/`dm[i]|bom[i]` forms are used whenever the
  instance is a root-pom declaration.
- `ResolutionSnapshot.of(mrr, errors, events)` (spec signature) exists but cannot probe
  `resolveNoChange` (needs a downloader); the canonical overload takes
  `(…, SnapshotNormalizer, ExecutionContext)` and the harness always uses it.

## Remains

- Fixture gap vs spec §2 fixture list: dated/pinned **snapshot** fixtures (need
  `maven-metadata.xml` machinery; slice C's MockWebServer corpus is the better home).
- Classes stay test-scope; the Phase 2 move to `testFixtures` is planned in the spec.
- Slices B (corpus/record-replay/ground truth), C (synthetic MockWebServer corpus), and the
  benchmark baseline are other slices' work.
- Nothing committed to git (per instructions; work sits in the worktree).
