# Maven resolution 2.0 — release notes draft

Staging draft for the release announcement; distilled from `doc/maven-resolution-ledger.md`.
Every behavior change below is deliberate, ledgered, and pinned by a test asserting the
Maven-identical behavior.

## Headline

rewrite-maven's dependency resolution now runs on real Maven APIs (Maven Resolver 2.0.x and
Maven 3.9.16's model builder, shaded into the new `rewrite-maven-engine` module) instead of a
custom re-implementation. **Any divergence from Maven's resolution behavior is a bug**: recipes
now see the same effective POMs, dependency graphs, and failures that `mvn` itself produces.
The `org.openrewrite.maven.tree.*` API, wire format, and LST identity are unchanged — existing
serialized LSTs and recipes keep working.

There are no configuration flags. The previous resolver remains in the codebase for one
release as revert insurance and is then deleted.

## What resolves differently (and why Maven is right)

**Profile activation.**
- Property-based activation reads properties, not environment variables. Managed dependencies
  contributed by property-negation-activated profiles in transitively imported BOMs (e.g.
  infinispan entries in `spring-boot-dependencies`) now appear, as they do for Maven.
- `<os>`, `<jdk>`, and `<file>` activation conditions are evaluated (the old resolver ignored
  them). Activation inputs default to the host JVM — exactly Maven's behavior — and hosts that
  need fleet-wide determinism can pin them via
  `MavenExecutionContextView.setActivationSystemProperties`.
- Active-profile dependencies contribute in profile declaration order, not reverse order.

**Dependency conflict resolution.**
- The conflict key is `groupId:artifactId:classifier:type`, so two direct dependencies
  differing only by classifier both resolve; previously the later declaration silently
  shadowed the earlier one.
- Duplicate `<dependencies>` declarations of the same coordinate collapse last-wins before
  collection, matching Maven's model normalizer (Maven warns "must be unique" and keeps the
  last).
- A coordinate reached at several scopes keeps Maven's widest-scope selection semantics.

**Relocations.** `<distributionManagement><relocation>` is followed (e.g.
`org.apache.commons:commons-io:1.3.2` resolves as `commons-io:commons-io`); the old resolver
never read relocations.

**Metaversions.** `LATEST`/`RELEASE` declared in `<dependencyManagement>` stay literal in the
effective model, exactly as Maven defers concrete selection to artifact resolution; the old
resolver eagerly (and nondeterministically) resolved them through metadata.

**Interpolation.**
- Value-source precedence is Maven's: user (injected) properties, then pom properties, then
  JVM system properties. A plain system property no longer overrides a pom-declared property;
  parser-injected properties still do.
- Plugin `<configuration>` values interpolate from the session system properties, as Maven's
  model interpolator does.
- Recursive property expression cycles fail model building (Maven's fatal
  "recursive expression cycle" error) instead of leaving placeholders literal.

**Model validation.** POMs that real Maven rejects now fail resolution with Maven's own
messages, surfaced per file as a `ParseExceptionResult` without aborting the parse run:
- a `<parent>` with the project's own groupId:artifactId,
- non-`pom` packaging on a parent or an aggregator (`<modules>`) POM,
- a dependency with no resolvable version,
- a `system`-scope dependency with a missing or non-absolute `<systemPath>`.

**Effective model shape.**
- Plugin and pluginManagement merges preserve declaration order and interpolate coordinates
  (the old resolver's hash-ordered goal merges and raw `${...}` coordinates are gone).
- `<pluginRepositories>` survive resolution (previously dropped).
- The projected repository list carries only declared/inherited repositories; whether the
  local cache or Maven Central participate in resolution remains a pipeline decision, not
  part of the model.

**Failure handling.** A download failure in one scope no longer discards the whole resolution
result: the marker keeps every resolvable scope populated and surfaces the failing scope's
error. Non-jar dependency types (`war`, `test-jar`, `tgz`, …) resolve instead of being
silently skipped, and a genuinely missing transitive POM for a jar-type graph member fails
exactly where Maven and the old resolver agreed it should.

## Performance

Steady-state recipe re-resolution is at parity with the old resolver. A warm full parse costs
1.26–1.49× — the structural price of building real Maven effective models for transitive
descriptors — while cold-network parses benefit from parallel prefetch. A permanent
dual-engine shadow oracle keeps future optimization honest.

## For embedders

- `HttpSender` injection remains the sole network transport; caches (`MavenPomCache`,
  `MavenArtifactCache`) remain the pluggable persistence seams.
- `MavenExecutionContextView.setActivationSystemProperties` pins os/jdk/file profile
  activation and interpolation inputs for reproducible fleets.
- Resolution event ordering changed with the new pipeline (events are emitted at
  semantically-equivalent points); event identity and once-per-coordinate semantics hold.
