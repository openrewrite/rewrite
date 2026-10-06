<p align="center" style="margin-top: 3rem;"><img src="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png" width="300px" alt="OpenRewrite Logo"></p>

Greetings!

The OpenRewrite project is a mass refactoring ecosystem designed to eliminate technical debt across an engineering organization. Full documentation and information is available at [OpenRewrite](https://docs.openrewrite.org/).

This project contains the LST models as well as the runtime required to execute OpenRewrite recipes.

### How to install

```
npm i -D @openrewrite/rewrite
```

### Inspecting type attribution

Most recipe debugging is one question: what type did this expression get, and if none, where was it lost? Set `REWRITE_JAVASCRIPT_DUMP_TYPES` and any `RecipeSpec` test prints the attribution its own parse produced, which is the attribution a `MethodMatcher` pattern written for that test has to match:

```
$ REWRITE_JAVASCRIPT_DUMP_TYPES=1 npx vitest run test/my-recipe.test.ts

--- type attribution: source-0.ts ---
line:col  kind                   source                      type
1:1       MethodDeclaration      function f(arr, s: string)  source-0 f(..) -> <unknown>
1:12      NamedVariable          arr                         ⚠ <unknown>
1:16      NamedVariable          s: string                   String
2:5       MethodInvocation       s.charAt(0)                 String charAt(number) -> String
2:14        └ arg0:Literal       0                           double (Primitive)
3:12      MethodInvocation       arr.tostring()              ⚠ <unknown> tostring(..) -> <unknown>
3:12        └ select:Identifier  arr                         <unknown>
```

The text before `->` is a pattern that `new MethodMatcher(...)` or `usesMethod(...)` accepts for that call. It names the resolved overload's parameter types where the matcher can, and `(..)` where one has no name it compares. A type without a package, such as `Array`, matches that name in any package. Each pattern is checked against the matcher, and a call whose pattern cannot match it, such as one declared on an anonymous class, carries a note saying so.

The `arg` rows give each argument's type and its kind, which is what a capture constraint over that argument reads. Patterns spell primitives the TypeScript way, which the matcher accepts. The type column and `arg` rows use the model's names, so a `number` is the primitive `double` and a `string` is `String`. A kind only JavaScript has carries its namespace, as in `JS.Binary`, since a visitor reaches it through a method of its own.

`arr.tostring()` has no declaring type, and the indented `select` row names the receiver that lost it. `<none>` is a slot the parser left empty and `<unknown>` is `Type.unknownType`.

The variable accepts comma-separated flags. `missing` lists only unattributed nodes, `all` widens the listing beyond calls and declarations, and `supertypes` shows each declaring type's ancestry, which bounds how general a pattern can be.

A recipe gated on a type that never resolved is the usual reason a test sees no change, so that failure names the unattributed nodes without being asked:

```
Nodes with no type attribution (a recipe gated on one of these cannot fire):
  line:col  kind                   source          type
  1:12      NamedVariable          arr             ⚠ <unknown>
  3:12      MethodInvocation       arr.tostring()  ⚠ <unknown> tostring(..) -> <unknown>
  3:12        └ select:Identifier  arr             <unknown>
```

`npx rewrite-javascript-types <file>` runs the same report on a file on disk. It resolves `node_modules` and `tsconfig.json` from the nearest directory with a `package.json`, or from `--project-root`. A project without its dependencies installed attributes differently from a test, so prefer the test output when writing a pattern for a test.

| flag | |
| --- | --- |
| `--only-missing` | list only the nodes whose type is missing |
| `--all` | every node with a type slot, not just calls and declarations |
| `--supertypes` | show each declaring type's ancestry |
| `--tree` | the nested structure with each node's type, for structural rather than type questions |
| `--json` | the listing as JSON |

To see what a recipe's output carries, call `printTypes` from a spec's `afterRecipe` hook. From code, `printTypes(sourceFile)` writes the same listing, `buildTypeReport(sourceFile)` returns it as data, and `new LstDebugPrinter({includeTypes: true})` adds types to the tree view.

### When making changes to JavaScript code

You can run this command from rewrite/ directory to build the JavaScript code. A
subsequent run of JavaScriptRewriteRpc will use that.

```
npm run build
```

Alternatively, `./gradlew npmBuild`.
