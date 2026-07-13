# 10. Maven dependency resolution on real Maven APIs

Date: 2026-07-09

## Status

Accepted

## Context

`rewrite-maven` historically re-implemented Maven's dependency resolution — effective-POM
construction (inheritance, interpolation, profile activation, BOM import), dependency
collection, conflict mediation, and scope derivation — in its own ~4.5k lines of algorithm
(`ResolvedPom.Resolver`, `doResolveDependencies`, `VersionRequirement`). That
re-implementation drifted from Maven in dozens of observable ways. A systematic
differential study against real Maven (3.9.x sources, `mvn dependency:tree` ground truth,
and a recorded corpus of real-world POMs) catalogued every divergence in
[`doc/maven-resolution-ledger.md`](../maven-resolution-ledger.md): environment-variable-based
profile activation where Maven reads properties, nondeterministic metaversion resolution,
classifier shadowing in dependency deduplication, dropped `<pluginRepositories>`, ignored
`<distributionManagement><relocation>`, `<os>`/`<jdk>`/`<file>` profile blindness, scope
narrowing, plugin-merge ordering, and tolerance of POMs Maven rejects outright.

Every one of those divergences produces wrong answers for recipes that reason about a
project's dependency graph — and each fix to the custom resolver risked introducing new
divergence. The complete design, including the adversarial critiques and spike results that
shaped it, lives in `maven-resolution-plan/DESIGN.md`.

## Decision

**Any divergence from Maven's resolution behavior is a bug, not a feature.** Resolution
semantics are delegated to real Maven APIs; rewrite keeps only the seams it must own.

- A new **`rewrite-maven-engine`** module embeds Maven Resolver 2.0.x and Maven 3.9.16's
  model builder, shaded to `org.openrewrite.maven.engine.shaded.*` (Maven's core realm
  force-exports un-relocated `org.eclipse.aether.*` to plugins, so shading is
  non-negotiable). No shaded type appears in `org.openrewrite.maven.tree.*` signatures or
  serialized LSTs; the frozen tree API stays wire- and identity-compatible.
- **Ownership split**: anything deciding *which version/scope/property wins* is Maven's
  (`DefaultModelBuilder`, the breadth-first collector, verbose `ConflictResolver`).
  Anything deciding *where bytes come from, where they are cached, and how results are
  reported* is rewrite's (`HttpSenderTransporter` as the sole network transport,
  `MavenPomCache`/`MavenArtifactCache` bridges, the mappers projecting into the frozen
  tree types).
- **Single flip-over, no user-facing compatibility flags.** The internal
  `ResolutionEngineSelector` (dev/CI-only, never public API) defaults to the Maven engine.
  The legacy resolver stays in-tree but unreachable through any supported surface for one
  release — revert-by-release insurance — then is deleted.
- **The dual-engine shadow oracle is permanent.** SHADOW mode runs both engines, diffs a
  normalized resolution snapshot under a mask registry in which every mask must cite a
  ledger row, and fails on any unexplained difference. It returns the engine result, so
  enabling it never changes behavior. Pre-flip it proved parity; post-flip it guards
  optimization work on the engine path.
- **Behavior changes ship as ledger rows with pinning tests.** Each deliberate flip
  (classifier-aware dedup, relocation following, Maven-strict model validation,
  property-based and `<os>`/`<jdk>`/`<file>` profile activation, literal metaversions in
  `<dependencyManagement>`, declaration-order plugin merges, retained
  `<pluginRepositories>`) is pinned by a test asserting the Maven-identical behavior and
  documented for release notes. The unit suite — not the implementation — is the contract.

## Consequences

- Recipes see Maven's actual dependency graph. POMs that real Maven rejects (self-parents,
  jar-packaged parents/aggregators, missing dependency versions, recursive property
  cycles, invalid `systemPath`) now fail resolution exactly as Maven does, surfaced per
  file via `ParseExceptionResult` without aborting the run.
- Resolution inputs are explicit embedder inputs: hosts can pin
  `MavenExecutionContextView.setActivationSystemProperties` for fleet-deterministic
  `<os>`/`<jdk>` profile activation; the default (host JVM properties) matches Maven
  exactly.
- Performance: the steady-state recipe re-resolution loop is at parity with the legacy
  resolver; a warm full parse costs 1.26–1.49× (the structural price of real
  `DefaultModelBuilder` builds for transitive descriptors); cold-network parses benefit
  from parallel prefetch. The shadow oracle makes later optimization safe.
- The legacy resolver (~4.5k lines) is deleted one release after the flip, shrinking
  `MavenPomDownloader` to a transport/cache facade.
- `Pom.modelVersion` bumps (3→4, adding the pom-bytes descriptor region) in coordination
  with LST consumers; deferred until that format lands.
