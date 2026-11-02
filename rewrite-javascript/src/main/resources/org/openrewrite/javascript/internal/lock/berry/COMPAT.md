# Builtin compat patches

`compat-patches.json.gz` maps every supported Yarn release and builtin name to the
SHA-256 of its original patch text. `compat/<sha>.patch.gz` holds that exact text,
Brotli-decoded from the release's `plugin-compat/sources/patches/*.patch.ts` module.
There are 14 unique texts across 40 releases (4.0.0–4.18.1). Patches load lazily.

Regenerate with the development-only `generate/compat-patches.js` (Node required
for generation only). Its inventory is the existing `yarn-releases.json.gz`.
`BuiltinPatchesTest` checks native makePatchHash against the separately generated
release hash inventory, including TypeScript 7.0.2 and older affected versions.
The full corpus checks actual patched archive checksums across every release.

PatchResolver now hashes these texts, including semver exclusions, instead of
requiring a previously locked locator. PatchFetcher copies the source archive in
UTF-16 lexicographic directory traversal order, then applies patch effects with
Yarn's hunk search, whitespace comparison and final-newline rules. It rolls back
an entire optional patch only for UnmatchedHunkError. Missing files and other
native errors still fail; “optional” does not suppress them. In particular,
TypeScript 7.0.2 is accepted by Yarn 4.0.2 after optional hunk rollback, while
Yarn 4.6.0 and 4.12.0 fail reading its absent lib/_tsc.js (hash 5786d5).

Source archives are supported for npm semver, npm archive URLs and nested patches.
Other source protocols remain explicit unknowns. The production code runs no
package manager or Node process. Archive bytes use the same codec as Cache.

Audited upstream sources (apply.ts and PatchFetcher.ts are identical at the three
real-project releases):
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.0.2/packages/plugin-patch/sources/PatchFetcher.ts
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.0.2/packages/plugin-patch/sources/tools/apply.ts
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.6.0/packages/plugin-compat/sources/patches/typescript.patch.ts
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.12.0/packages/plugin-compat/sources/patches/typescript.patch.ts

Yarn's BSD-2-Clause license is reproduced in LIBZIP.md. The supplemented npm
TypeScript and resolve test tarballs are unmodified published archives.
