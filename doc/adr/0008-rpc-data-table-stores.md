# 8. Data table stores across the Rewrite RPC boundary

Date: 2026-06-26

## Status

Accepted

## Context

Recipes emit rows into [data tables](../../rewrite-core/src/main/java/org/openrewrite/DataTable.java)
during a run. Where those rows go is decided by the [`DataTableStore`](../../rewrite-core/src/main/java/org/openrewrite/DataTableStore.java)
installed on the `ExecutionContext`. In-process this is an *open* abstraction: any
implementation works — `InMemoryDataTableStore` (default), `CsvDataTableStore`
(writes `.csv`/`.csv.gz` directly), `NoOpDataTableStore`, or an arbitrary custom store
supplied by an embedding tool.

Recipes are increasingly written in non-Java languages (TypeScript/JavaScript, Python,
Go, C#) and executed over Rewrite RPC. Each of those runtimes has already independently
ported the data table abstraction — a `DataTable` with `insertRow`, plus `InMemory`,
`Csv` and `NoOp` store implementations — but there is **no way for the process that
controls a recipe run (almost always Java) to tell a language runtime where to put its
rows.** As a result, rows emitted by a recipe written in TypeScript or Python are dropped
into a default in-memory store on the remote side and never surface to the host.

We deliberately do **not** want individual data table rows to cross the RPC boundary:
column definitions are arbitrary recipe-defined data, and serializing rows back to the
host so that the host's store handles them is both risky and contrary to the streaming,
write-as-you-go design of `CsvDataTableStore`. The host should configure *where* rows
go; each runtime should write its own rows locally.

The open question this ADR settles is: **how does the host configure an arbitrary
`DataTableStore` implementation on a remote runtime that cannot run the host's code?**

## Decision

### The RPC boundary supports a *closed, mirrored* set of stores

`DataTableStore` remains an open interface in-process. Across RPC it necessarily
collapses to a closed set of implementations that are *mirrored* in every runtime —
exactly the situation that already exists for `PrintOutputCapture.MarkerPrinter`, whose
RPC form is the closed enum [`Print.MarkerPrinter`](../../rewrite-core/src/main/java/org/openrewrite/rpc/request/Print.java)
(`DEFAULT`, `SEARCH_MARKERS_ONLY`, `FENCED`, `SANITIZED`). A remote runtime can only
reproduce behavior it has code for. We do not introduce a parallel "descriptor"
taxonomy; the abstraction is still `DataTableStore`.

The conveyable set is:

* **`Csv`** — the production path. Carries the minimal serializable configuration needed
  to reconstruct the runtime's own `CsvDataTableStore`: output directory and the ordered
  static prefix/suffix columns. The runtime always writes raw `.csv`; any compression or
  finalization of the files is the host's concern (see below), so no encoding is
  conveyed. (`CsvDataTableStore` already exists in every runtime.)
* **`NoOp`** — explicitly disable data table output on the remote side.

`InMemoryDataTableStore` is intentionally *not* conveyable: its rows would be trapped in
the remote process and never reach the host, which is meaningless across RPC.

### Mechanism: a `Print.MarkerPrinter`-style mapping

A new `SetDataTableStore` RPC request mirrors the marker-printer pattern:

* `SetDataTableStore.from(DataTableStore)` maps a host-side store to its wire form, or
  returns `null` when the store is not RPC-conveyable.
* `SetDataTableStore.toDataTableStore()` reconstructs the runtime's mirror implementation
  on the receiving side.

The host (`RewriteRpc`) exposes `dataTableStore(DataTableStore)` and lazily sends the
wire form before the first `Visit`/`BatchVisit`/`Generate`. Each runtime's handler
installs the reconstructed store on the per-run `ExecutionContext`
(`DataTableExecutionContextView`). Rows never cross the wire — only the store's
configuration does, and that configuration is host-known static metadata, never
arbitrary recipe row data.

### A truly arbitrary store is not RPC-conveyable — by the same logic as markers

If the host configures a custom Java-only store, `from()` returns `null`. There are then
exactly three outcomes, and the choice is the host's:

1. It is a mirrored kind (`Csv`, `NoOp`) → honored natively by the runtime.
2. The host accepts that remote recipes' rows are not captured by the custom store →
   the runtime keeps its default (in-memory/no-op). Graceful, lossy-by-design.
3. The host wants remote rows captured by the custom store anyway → the only mechanism
   is shipping rows back over the wire, which we have rejected as the default. If ever
   needed it would be an explicit, opt-in escape hatch, not the norm.

### One shared file per table

The Java host and the RPC processes write to **one shared file per data table**
(computed identically everywhere from `outputDir` + `fileKey` + `.csv`), including,
occasionally, the same logically-named table. This is safe because of two properties of
the runtime:

* **Recipe execution is sequential** by recipe and source file, so no two writers write
  at the same instant. (`RecipeScheduler` iterates files in plain loops; there is no
  parallel file visiting.)
* **RPC processes are per-thread** — `RewriteRpcProcessManager` keys the process by
  `ThreadLocal`. A host that runs independent units of work on separate threads gets a
  separate process, and a separate output directory, per unit, so a process-global
  output directory is correct and two units never share a file.

Writers append **raw CSV** with the file handle held open, flushing complete lines
before yielding control. Raw CSV has no cross-record state, so complete-line appends
from sequential writers interleave into one valid file regardless of order. The first
writer to create a file writes the `#`-comments and header (decided by file existence,
so it happens exactly once across processes); a fail-loud guard rejects an append whose
column layout differs from the existing header, so a shared file can never silently
misalign. Any compression or post-processing of the completed files is the host's
concern, performed after the writers have finished.

We deliberately do **not** stream a compressed format (such as `.csv.gz`) per writer:
per-member gzip framing allocates and frees a `Deflater` per member, destroys
compression (each row a fresh deflate window — 3-5x bloat at 100k rows), and two writers
cannot safely hold a compression stream open on the same file. Writing raw CSV and
leaving any compression to a single post-run pass avoids all of this. Because every
writer shares the one file, the static prefix/suffix columns are conveyed to each
runtime and written on every row (all writers must agree on the column order — enforced
by the header guard).

## Consequences

* The host can uniformly configure CSV (or no-op) data table output for Java and every
  RPC language through one builder method, `XXXRewriteRpc.Builder.dataTableStore(..)`.
* No row data crosses the RPC boundary; only host-known store configuration does.
* Adding a new conveyable store kind requires implementing it in core *and* mirroring it
  in every runtime — the same cost the marker printers already pay. This is the price of
  the closed set and is accepted deliberately.
* Custom Java-only stores are not honored for remote recipes unless rows are shipped
  back (an opt-in we do not build now).
* Existing ad hoc wiring (Go's `--data-tables-csv-dir` launch flag, Python's
  `dataTableOutputDir` `PrepareRecipe` parameter) is superseded by the unified
  `SetDataTableStore` handshake and should be retired as each runtime is migrated.
