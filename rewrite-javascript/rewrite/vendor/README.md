# Vendored packages

`picomatch-4.0.8-backpatch-00001.tgz` is picomatch 4.0.7 with CVE-2026-112366 fixed, from the
[Moderne Backpatch Alliance](https://backpatch.moderne.io). Its source is the `backpatch/4.0.x` branch of
https://github.com/moderne-backpatch-alliance/backpatch-picomatch.

`package.json` bundles it, so an install of `@openrewrite/rewrite` takes it from the package itself. Once picomatch
releases its own fix, depend on that release again and delete this directory.
