# Integrated perf gate — 2026-07-09 descriptor-grade (slice F bounded experiment)

Companion to `integrated-2026-07-09-descriptor-grade.json`. Tests whether building the **transitive descriptor path**
with `locationTracking` off (descriptor-grade, mirroring Maven's own `DefaultArtifactDescriptorReader.loadPom`) recovers
any of slice E's residual 1.26–1.49× warm gap. Full write-up: `phase4-results-f.md`.

## Result — no recovery above the noise floor; tracking was not the cost

Measured on a **shared dev box under heavy ambient load** (15-min load avg peaked ~40; maven mode is far more
GC-sensitive under memory pressure than legacy). The reference-host absolutes (slice E) could not be reproduced, so the F
measurement is a **same-host E-vs-F A/B** — revert to provenance-grade, publish, measure; reapply descriptor-grade,
publish, measure — adjacent in time so both arms share the ambient load. Legacy is the load normalizer (rock-stable
across runs); maven swung 2–3× iteration-to-iteration.

### D → E → F within-tier ratio progression

`warm ×` = maven/legacy warm median. D/E are the published reference-host ratios (slices D/E). F is this-host A/B.

| tier | D warm × | E warm × | F warm × (this-host A/B) | E loopCtx × | F loopCtx × |
|---|---:|---:|---|---:|---:|
| camel 150 | 1.85 | 1.42 | E-arm 1.60 ↔ F-arm 2.34\* — within noise | 1.10 | ~1.16 (grade-independent) |
| camel 400 | 1.65 | 1.49 | E-arm 1.40 ↔ F-arm 1.12 — within noise | 1.04 | ~0.90 (grade-independent) |

\* F camel-150 arm caught a GC storm (warm array `[4393, 5561, 7194, 5929, 11728]`). E and F ratios bracket each other in
both directions — the descriptor-grade delta is below per-run variance. No recovery attributable.

### Same-host A/B raw (warmups=2/iters=5 at 150, 1/3 at 400; -Xmx2g)

| tier | arm | legacy warm ms | maven warm ms | maven/legacy | maven loopCtx ms | loopCtx × | modelBuilds | descriptorReads | modelBuildMs (last-iter) |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| camel 150 | E provenance | 3035 | 4851 | 1.60 | 2372 | 1.16 | 2416 | 2258 | 4426 |
| camel 150 | F descriptor | 2539 | 5929 | 2.34 | 2237 | 1.39 | 2416 | 2258 | 14650 (GC) |
| camel 400 | E provenance | 7198 | 10099 | 1.40 | 3616 | 0.90 | 3215 | 2803 | 8833 |
| camel 400 | F descriptor | 8735 | 9754 | 1.12 | 4094 | 0.89 | 3215 | 2803 | 5578 |

`modelBuilds`/`descriptorReads` are byte-identical across grades (structural invariance). `modelBuildMs` swings ±60% in
both directions — GC/last-iteration lottery, not signal.

## Corpus correctness spot-check (hermetic replay, twice-run determinism, -Xmx6g)

Descriptor-grade build; both reactors **green + byte-deterministic** in both modes:

| entry | modules | legacy 2× ms | maven 2× ms | note |
|---|---:|---:|---:|---|
| apache-maven-3.9.16 | 15 | 1022 | 1131 | green, deterministic |
| apache-dubbo-3.2.10 | ~190 | 4625 | 8209 | green, deterministic; maven 2× incl. ~195 MB snapshot serialization |

(Corpus twice-run wall includes snapshot serialization and is a rough figure, not a resolution benchmark.)

## Verdict vs DESIGN §6 (unchanged from slice E)

- loop ≥ legacy — met/near-met (~1.0×, grade-independent).
- ≥ legacy warm at camel-150+ — **not met; no crossover.** The warm floor is the per-descriptor `DefaultModelBuilder`
  pipeline (~1.8 ms/effective model: inheritance assembly + interpolation + import-DM), not `InputLocation` bookkeeping.
  Closing it needs the Phase-5 descriptor-only projection, not a build-request toggle.

The descriptor-grade change is retained (correctness-neutral, aligns with Maven's own reader, removes transitive
allocation) but is not a performance lever.
