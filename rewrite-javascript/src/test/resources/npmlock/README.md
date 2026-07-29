# npm lock engine fixtures

Scenario fixtures for `NpmLockEngine` / `NpmLockWriter` tests. Each directory holds a
before/after manifest pair, the lock real npm produced for each, and the full
packuments for every package the edit moves, so the offline tests can assert the
engine's output **byte-identical** to real npm without network access.

## Provenance

Recorded with `./record.sh` against `https://registry.npmjs.org` on **2026-07-29**
using **npm 11.17.0** on **node v26.4.0** (macOS). Regenerating on a different npm
version may change emission details or resolution outcomes; re-record all scenarios
together and update this section.

`locale-sorted-keys.txt` was generated with Node's `Intl` collator
(`arr.sort((a, b) => a.localeCompare(b, 'en'))` over ~2800 realistic lock keys and
random strings, node v26.4.0) and pins `NpmLockWriter`'s key-ordering comparator.

## Scenarios

| Directory | Edit | Engine behavior under test |
|---|---|---|
| `upgrade-leaf` | `is-number ^4.0.0 → ^6.0.0` | version move of a leaf pin |
| `range-satisfied` | `is-number ^6.0.0 → >=4.0.0` | pin already satisfies: no network, ranges only |
| `cascade-fails` | `is-odd ^2.0.0 → ^3.0.1` | new version forces a transitive move → fail loud (`package-lock.after.json` documents the cascade npm performs) |
| `remove-orphans` | drop dev `is-even` | orphan sweep of the abandoned subtree |
| `upgrade-orphans` | `chalk ^4.1.2 → ^5.0.0` | new version drops deps → sweep; `funding` string normalization |
| `add-leaf` | add `is-buffer ^2.0.5` | top-level (hoisted) addition |
| `override` | add `overrides: {"is-buffer": "1.1.5"}` | fail loud (`package-lock.after.json` documents npm nesting the overridden copy under its dependent — placement) |
| `dev-recolor` | drop prod `kind-of`, keep dev `is-number` | dev/optional flag recomputation on survivors |
| `scoped` | `@isaacs/string-locale-compare 1.0.1 → ^1.1.0` | scoped-name registry URL encoding |
