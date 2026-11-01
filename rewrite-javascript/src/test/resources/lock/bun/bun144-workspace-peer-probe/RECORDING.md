The registry inputs are synthetic; every output lock was written by the actual Bun 1.4.4 executable. This is a
lock-only oracle: package tarballs are not fetched. The fixture isolates the resolution-tag comparison used by
Tree.rs PeerProbes, without depending on public packages.

The root links workspace @probe/host@1.0.0. Registry @probe/owner@1.0.0 depends on @probe/host@1.1.0 and
@probe/plugin@1.0.0; the plugin peers on @probe/host@^1. Bun nests the plugin under its owner. Treating the root
workspace version as satisfying the npm probe incorrectly hoists the plugin to the root instead.

With Bun 1.4.4 on PATH, run the pinned lock-fixtures recorder:

```
bash <fixtures>/record/record-bun-open.sh <this-directory>
```

The recorder stores scoped HTTP documents using `%2f` in the filename. For the offline test loader, move those
files to their decoded directory paths, e.g. `http/@probe%2fowner` to `http/@probe/owner`. Keep registry/ encoded
for the recording stub. This renames the fixture's routing files only; it changes no recorded response or lock bytes.
