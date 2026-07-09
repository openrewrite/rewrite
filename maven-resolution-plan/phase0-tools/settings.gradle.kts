rootProject.name = "phase0-tools"

// Consuming THIS worktree's rewrite-maven build.
//
// The intended wiring was a composite build — `includeBuild("../..")` with an explicit
// dependencySubstitution of org.openrewrite:rewrite-maven onto :rewrite-maven. Gradle 9.5.1
// cannot include the rewrite root as a build here: resolving phase0-tools' compileClasspath forces
// configuration of the included build's `rewrite-maven` project through the root project's
// buildscript `classpath` configuration, which trips the known
// "DefaultClassLoaderScope ... must be locked before it can be used to compute a classpath" bug
// (the rewrite root applies the org.openrewrite.build.* precompiled convention plugins). The local
// build is healthy in isolation (`:rewrite-maven:jar`, `:rewrite-maven-engine:shadowJar` both
// succeed); only cross-build inclusion trips the bug.
//
// So the local build is consumed through Maven Local instead (see build.gradle.kts + RUNBOOK.md):
//   ./gradlew :rewrite-maven:publishToMavenLocal :rewrite-maven-engine:publishToMavenLocal   (+ transitive modules)
// pinned to the worktree's 8.87.0-SNAPSHOT, which resolves only from ~/.m2 — never the released
// 8.86.1. The wiring is verified at runtime: the runner loads
// org.openrewrite.maven.internal.ResolutionEngineSelector (absent from any released rewrite-maven)
// and prints the jar it came from.
