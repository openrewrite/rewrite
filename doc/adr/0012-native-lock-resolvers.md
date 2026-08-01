# 12. Native lock file resolvers

Date: 2026-08-01

## Status

Proposed

## Context

[ADR 0011](0011-native-node-lock-regeneration.md) regenerates a lock file by *surgically patching* the
existing one for a specific edit (bump, add, cascade, orphan-prune, promotion, nesting). That path is
byte-exact for the edits it recognises and fails loud on everything else. A corpus sweep over ~800 real
repositories shows it natively handles ~23% of aggressive `^latest` bumps and ~42–67% (npm) of realistic
in-major bumps, deferring the rest with **zero** wrong locks.

The remaining deferrals are genuine *resolution* — fork/dedup reshuffles, peer auto-install, re-selecting a
transitive across a changed constraint union. These cannot be pattern-matched; they require running the
package manager's actual (deterministic, greedy) resolution algorithm and diffing the result against the old
lock. Each package manager's algorithm is open-source, deterministic given its inputs, and — crucially —
already has an oracle: the differential harness that runs the real PM and byte-compares.

## Decision

Introduce a `LockResolver` — one interface each of the five package managers implements — that resolves the
**whole** closure for the edited manifests and produces the new lock byte-exact, or fails loud. It is the
deeper complement to the surgical patchers, not a replacement: `regenerate` tries the fast surgical path first
and falls back to the resolver for edits the patchers defer on.

```
public interface LockResolver {
    PackageManager packageManager();
    String resolve(ResolveRequest request);   // byte-exact lock content, or throws EngineFailure to defer
}
```

**Reuse, do not reinvent.** All version/constraint work is delegated to rewrite-core's existing node-semver
(`NodeSemver`, `NodeRange`, `NodeComparator`, `NodeVersion`) — a corpus-tested constraint-satisfaction system.
Package metadata is the existing `VersionManifest`; the registry is `NpmRegistryClient`; byte-exact writing
reuses `LockJson`/`LockYaml` and the field-ordering already proven in the patchers. The resolver adds only
what is genuinely new: the resolution graph and the per-PM resolve/layout algorithms.

**Shared vs. per-PM.** The resolution *algorithm* (dedup preference, hoisting vs. content-addressing vs. flat
descriptors, peer placement) genuinely differs per PM and is deliberately **not** forced into one shared
skeleton (that was tried and rejected as a leaky maze). What is shared is thin and stable:

- `LockResolver` (the interface) and `ResolveRequest` (importer manifests + existing lock + registry).
- `ResolutionGraph` — a plain value model: importers (workspace roots and their declared→resolved edges) and
  resolved nodes (a `VersionManifest` plus its resolved dependency edges), keyed `name@version`. It is rich
  enough for every PM: layout (npm/bun hoisting, pnpm content-addressing, yarn descriptors) is computed by
  each PM's serializer from the graph, not baked into the model.
- node-semver, `VersionManifest`, `NpmRegistryClient`, `EngineFailure` (the fail-loud contract).

Each `<Pm>Resolver` is self-contained and decomposes into heavily unit-tested classes — typically a graph
builder (resolve + dedup + peer, driven by node-semver, tested against a stub registry) and a serializer
(graph → lock bytes, tested against recorded goldens). The differential corpus harness gates the whole.

## Consequences

- **Coverage rises without risking accuracy.** Every case the resolver does not yet reproduce byte-exact falls
  back to the existing fail-loud. It is built PM-by-PM, case-by-case, corpus-validated — the levers gone deeper.
- **The ceiling is input fidelity, not the algorithm.** A resolver's output is a function of the PM version,
  `.npmrc`/`.yarnrc` config (overrides/resolutions/hoisting), platform (optional/os/cpu), and workspace/catalog
  protocols. Where those cannot be modelled exactly the resolver detects the mismatch and defers; it never emits
  a wrong lock. This caps coverage in exotic configs but is always safe.
- **Five algorithms to build and track.** npm (arborist hoisting), pnpm (content-addressed + peer suffixes),
  yarn-classic (flat merged descriptors), yarn-berry (flat descriptors + reproduced checksums), bun (hoisted
  tuples). npm is the reference (largest share, most existing machinery); the other four follow the same shape.
