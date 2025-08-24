<p align="center" style="margin-top: 3rem;"><img src="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png" width="300px" alt="OpenRewrite Logo"></p>

Greetings!

The OpenRewrite project is a mass refactoring ecosystem designed to eliminate technical debt across an engineering organization. Full documentation and information is available at [OpenRewrite](https://docs.openrewrite.org/).

This project contains the LST models as well as the runtime required to execute OpenRewrite recipes.

### How to install

```
npm i -D @openrewrite/rewrite
```

### When making changes to JavaScript code

You can run this command from rewrite/ directory to build the JavaScript code. A
subsequent run of JavaScriptRewriteRpc will use that.

```
npm run build
```

Alternatively, `./gradlew npmBuild`.
