# rewrite-coldfusion

OpenRewrite support for ColdFusion (CFML). It parses templates (`.cfm`, `.cfml`) and components
(`.cfc`) into a Lossless Semantic Tree, in tag syntax and in script syntax. Script, every
expression and the declarations of either syntax reuse the Java `J` model. Other tags and what Java has
no counterpart for are `Cf` types.

## Parser front end

An ANTLR grammar, `src/main/antlr/CFMLLexer.g4` and `CFMLParser.g4`, parses the source.
`ColdFusionParserVisitor` maps the parse tree to the LST, so every character lands in exactly one
prefix, suffix or value. The generated sources are in `internal/grammar`. Regenerate them with
`./gradlew :rewrite-coldfusion:generateAntlrSources`. See
`doc/adr/0015-coldfusion-parsing-with-an-antlr-grammar.md` for how the grammar handles CFML's
context-dependent syntax.

A `.cfc` that begins with `component` or `interface`, after any comments and imports, is a script
component. Every other file is a template:

- Text between tags is `Cf.Text`, HTML included. The whitespace and `<!--- --->` comments around it
  and around tags are space: the prefix of what follows them, the end of a tag's body before its end
  tag, or the end of the file.
- A tag closed by its end tag holds its body, a `J.Block` written without braces. A tag with no end
  tag stands alone, so `<cfelse>` is a tag in the body of its `<cfif>`.
- `#expression#` interpolates in strings, attribute values and the bodies of `<cfoutput>`,
  `<cfquery>` and `<cfmail>`. Elsewhere in a template it is text.
- A `<cfscript>` body is script statements.

A tree prints the file's bytes back. A UTF-8 byte order mark is the `Cf.CompilationUnit`'s `charsetBomMarked`, which
prints even in a file of nothing else, and a second U+FEFF after it is text. A file whose bytes do not decode to text
that encodes back to them, in the charset detected for them, fails to parse. So no recipe rewrites a zip archive or a
template `cfencode` encrypted. The check that a tree prints its file compares bytes, where comparing the text read with
the text printed would miss a lost byte order mark.

Syntax the grammar does not accept throws a `ColdFusionParsingException` naming the unexpected token.
Nothing maps to `J.Unknown`.

## ColdFusion-specific LST types (`Cf.*`)

| CFML syntax | LST element | Example(s) |
|---|---|---|
| Source file | `Cf.CompilationUnit` | any `.cfm` or `.cfc` |
| Template text | `Cf.Text` | `<p>Hello</p>` |
| Tag | `Cf.Tag` | `<cfquery name="q">...</cfquery>`, `<cfset x = 1>`, `<cfif a GT b>` |
| Tag attribute | `Cf.TagAttribute` | `name="q"`, `maxrows=10`, a bare `debug` |
| Interpolation | `Cf.Interpolation` | `#user.getName()#` |
| Interpolated string | `Cf.StringTemplate` | `"Hello #name#"` |
| CFML-only binary operators | `Cf.Binary` | `a & b`, `2 ^ 8`, `a \ b`, `s CONTAINS t`, `a ?: b`, `EQV`, `IMP`, `XOR`, `===` |
| `&=` | `Cf.AssignmentOperation` | `s &= "x"` |
| Struct literal | `Cf.StructLiteral`, `Cf.KeyValue` | `{a: 1, "b" = 2}`, ordered `[a: 1]`, `[:]` |
| Named argument | `Cf.KeyValue` | `foo(a = 1, b: 2)` |
| Tag as a script statement | `Cf.ScriptTag` | `lock name="x" { }`, `param name="x" default="";`, `include "a.cfm";`, `abort;` |
| Tag as a call with a body | `Cf.TagCall` | `cfloop(array=items, index="i") { }` |
| Closure with attributes | `Cf.Closure` | `function() localmode="classic" { }` |
| Type named by a string | `Cf.TypeLiteral` | `catch ("App.NotFound" e)`, `new "models.#type#"()` |
| Call of an expression | `Cf.Invocation` | `handlers[key](event)` |
| Expression as a statement | `Cf.ExpressionStatement` | a bare `x;` |

## ColdFusion-specific markers

| CFML syntax | LST element + marker | Example(s) |
|---|---|---|
| Spelled-out operators | `J.Binary`, `J.Unary`, `Cf.Binary` + `OperatorSpelling` | `EQ`, `is not`, `GREATER THAN`, `<>`, `NOT`, `MOD` |
| Keywords in any case | `J.If`, `J.Return`, ... + `KeywordSpelling` | `If`, `RETURN`, `IN` |
| `function` | `J.MethodDeclaration`, `J.Lambda` + `FunctionKeyword` | `public string function x()`, `function(a) { }` |
| `?.` | `J.FieldAccess`, `J.MethodInvocation` + `SafeNavigation` | `user?.name` |
| `::` | `J.FieldAccess`, `J.MethodInvocation` + `DoubleColon` | `Util::format(x)` |
| `var` outside a declaration | `J.Assignment`, `J.AssignmentOperation`, `Cf.AssignmentOperation`, `NamedVariable` + `VarKeyword` | `var local.x = 1`, `var sql &= "x"`, `catch (any var e)` |
| A missing comma | `JRightPadded` + `OmitComma` | parameters on separate lines, `cfparam(name="x" default=1)` |
| A comma between the attributes of a tag written as a statement | `Cf.TagAttribute` + `AttributeComma` | `thread name="t", action="run" { }` |
| A return type before a modifier | `J.MethodDeclaration` + `ReturnTypePosition` | `boolean public function isPackage()` |
| An attribute naming a part of a declaration | `J.ClassDeclaration`'s `extends` and `implements`, a declaration's name or type + `AttributeClause` | `extends="models.Base"`, `implements="IFoo, IBar"`, `name="init"`, `type="string"` |
| A declaration written as a tag | `J.ClassDeclaration`, `J.MethodDeclaration`, `J.VariableDeclarations` + `TagSyntax` | `<cfcomponent>`, `<cffunction name="init">`, `<cfargument name="id">`, `<cfproperty name="x">` |
| A type a `TypeInference` gave, not the parser | the tree it typed, as a `J.MethodInvocation`, + `InferredType` | `variables.dao.find(id)` typed from what flows into `dao` |

A `<!--- --->` comment is a `TagComment` in a `Space`, in a template as in a tag or in script. Markers from
`org.openrewrite.java.marker` are reused: `Semicolon` for a `;`, which CFML lets most statements
leave off, `TrailingComma`, `OmitBraces` for a tag body, and `OmitParentheses` for `new Foo`.

## Standard `J` mappings

| CFML syntax | LST element |
|---|---|
| Component or interface, `component { }` or `<cfcomponent>`, named for its file | `J.ClassDeclaration` |
| Function declaration, `function x() { }` or `<cffunction>` | `J.MethodDeclaration` |
| Parameter or its `<cfargument>`, `var x = 1` | `J.VariableDeclarations` |
| Property, `property name="x";` or `<cfproperty>` | `J.VariableDeclarations`, a field |
| Attribute of a component, function, parameter, closure or property | `J.Annotation` |
| `if`/`else`, `elseif` | `J.If` |
| `for (;;)`, `for (x in y)`, `while`, `do`/`while` | `J.ForLoop`, `J.ForEachLoop`, `J.WhileLoop`, `J.DoWhileLoop` |
| `switch`/`case` | `J.Switch`, `J.Case` |
| `try`/`catch`/`finally` | `J.Try` |
| `return`, `break`, `continue`, `throw "x"` | `J.Return`, `J.Break`, `J.Continue`, `J.Throw` |
| Calls, members, indexes | `J.MethodInvocation`, `J.FieldAccess`, `J.ArrayAccess` |
| `new` | `J.NewClass` |
| Closures, arrow functions | `J.Lambda` |
| Literals, names | `J.Literal`, `J.Identifier` |
| Array literal, `[1, 2, 3]` | `J.NewArray`, with no type, as rewrite-javascript's |
| Operators with Java counterparts | `J.Binary`, `J.Unary`, `J.Assignment`, `J.AssignmentOperation`, `J.Ternary` |
| `static { }` | `J.Block` |
| `import com.example.*;` | `J.Import` |

A component is the class its file declares, so `models/User.cfc` is `models.User`. Its name is not
written, so a marker on it prints around the `component` keyword or the `cfcomponent` tag's name. An
application may reach the component by another name, through a mapping or from the directory of the
file that names it.

A declaration written as a tag is the tree its script form is. A `<cffunction>` is a
`J.MethodDeclaration`, whose `name` and `returntype` attributes are its name and its return type, and the
`<cfargument>` tags it starts with are its parameters. A component's `<cfproperty>` tags, like its script
`property` statements, are fields. A declaration whose name is interpolated, or otherwise not a plain name,
stays a `Cf.Tag`, or a `Cf.ScriptTag` in script.

An attribute of a declaration is an annotation on it, as CFML's own metadata model has it: the same
attribute can be written as `@accessors true` in the declaration's doc comment. An attribute the
language defines has the type of the tag it belongs to in tag syntax, such as `cfcomponent.accessors`
or `cffunction.output`, as Lucee's core tag library and Adobe's documentation name them. Any other
attribute, such as WireBox's `singleton`, is typed by its own name, `singleton`. A tag's attributes, as
in `<cfquery name="q">`, are arguments to the tag and stay `Cf.TagAttribute`.

## Types

The parser types what resolves within the repository it parses, so a recipe or code search can follow a name or a
call to what it names:

- A component's type is named for its path, so `models/User.cfc` is `models.User`. Its supertype, interfaces,
  methods and variables come from its file. Its variables are its properties and what its functions assign to
  `variables.x` or `this.x` with `new` or `createObject`.
- A template is a type named for its path as well, so `includes/udfs.cfm` is `includes.udfs`, and the functions it
  declares are its methods. A template or a component that includes it by a literal path,
  `<cfinclude template="../includes/udfs.cfm">` or `include "/includes/udfs.cfm";`, has its functions as its own.
  A path from the web root resolves through the mappings, the application's root and the repository's root.
- A file's `Cf.CompilationUnit` has the type the file is: its component's, which its class declaration has too, or
  the template's own, so a template that declares no function has a type as well. The file declares that type, so its
  `TypesInUse` does not count it as used, as Java's does not count a class's own.
- A name a file gives a component, in `extends`, `implements`, `new`, `createObject("component", ...)` or a
  declared type, resolves as CFML looks for it: in the directory of the file naming it, then from the repository's
  root, then as the one component whose path ends with the name. A name that no component matches, or more than
  one, stays untyped.
- A call to a built-in function is a method of one type, `cfml`, as in `cfml.arrayLen`. The functions are Lucee's,
  from its core and its ESAPI, image and Hibernate extensions, listed in `internal/functions.tsv`.
- CFML's own values are typed: an array is `cfml.Array`, a struct `cfml.Struct`, a query `cfml.Query`, a string
  `java.lang.String` and a number a `double`, from literals, declared types and what built-in functions return. A
  member function on one, as `items.append(x)`, has the method type of the built-in function it aliases,
  `cfml.arrayAppend`, so one method matcher finds both forms. One that returns its receiver, as `append` does,
  returns the value's type. A string also has `java.lang.String`'s methods.
- A query's result is a query. `<cfquery name="q">` assigns it to `q`, as `query name="q" { }` and
  `cfquery(name = "q") { }` do in script, and `queryExecute(sql, params, options)` returns it. A `returntype` of
  `array` or `struct` makes it one of those, and options only known when the code runs give it no type.
  `<cfdirectory action="list">`, `<cfdbinfo>`, `<cfprocresult>`, `<cfldap action="query">` and `<cfzip action="list">`
  assign a query to the variable they name too. `directoryList(path, recurse, "query")` returns one, and an array of
  paths or names for the other `listInfo` values.
- A call to a function of the component, of one of its supertypes, interfaces or included templates, or of a
  component that a parameter, a local variable or a struct's key holds, has that function's method type. A
  template's top-level variables count as a function's locals do. A variable or key a function assigns two types,
  or a value whose type is not known, has no type.
- A `var` declared as an empty string or a zero is a placeholder. Old CFML declares each variable this way at the
  top of a function. The variable keeps the placeholder's type only until the function assigns it, as in
  `var user = ""` then `user = new User()`. A concatenation, `a & b`, is a string.
- A component with `accessors="true"`, or an ORM entity with `persistent="true"`, has the accessors the engines
  generate for each property: `getName()`, `setName(name)`, which returns the component, and for an ORM
  relationship `hasName()`, `addName(item)` and `removeName(item)`. A function the component declares by the same
  name replaces one.
- A function that declares no return type returns what its returns agree on: `this`, a component it makes, as in
  `return new User()`, one of its component's variables, a parameter, a literal, or what a call returns, followed
  through the calls it chains, as in `return variables.dao.find(id)`. Since one function's return can follow
  another's, the index infers them across the repository until none changes. Returns that disagree give no type.
- A function a component assigns to another object, as MockBox assigns `$` to each mock with `obj.$ = variables.$`,
  runs as that object's. So one that returns `this` returns the object it is called on, which the index cannot name,
  and it has no return type. A mock is not a `MockBox`.
- A component that a component assigns to `application.users`, or a variable of the `session`, `server` or
  `request` scope, is that variable's type everywhere.
- `createObject("java", "java.io.File")` is the JDK's class, typed as Java types it. Its `init(...)` is the
  constructor, and a call on it resolves by name and argument count. Only `java.*` and `javax.*` classes count.
- A name that begins with the repository's own directory name resolves from the repository's root, as a library
  deployed into a web root under its name names its own components.
- A property WireBox injects has the type of the component its ID names, by WireBox's convention:
  `inject="UserService"` is the one component named `UserService`, preferring one in a `models` directory, and
  `inject="AuthService@cbauth"` the one in the `cbauth` module. `inject="coldbox"`, `"wirebox"`, `"cachebox"`,
  `"logbox"` and `"logbox:logger:..."` are ColdBox's own objects. ColdBox's `getInstance("UserService")` returns
  the component the same ID names. `inject="coldbox:flash"` is ColdBox's flash scope,
  `coldbox.system.web.flash.AbstractFlashScope`, which each flash scope ColdBox can be configured with extends. The
  DSL's other IDs whose object has one type in every application are that type, as ColdBox's own DSL builders make
  them: `coldbox:requestService` and `coldbox:interceptorService` (or `box:`), `wirebox:populator`, `executor`,
  `provider`, and `cachebox:name`, a cache that implements `coldbox.system.cache.providers.ICacheProvider`.
- ColdBox's conventions hold in a ColdBox application or module, when ColdBox resolves. A file is in one when a
  directory above it holds a `ModuleConfig.cfc`, ColdBox's configuration in its `config` directory, or a template that
  includes ColdBox's `coldbox.cfm`, as an older application's `index.cfm` does. The configuration is a `ColdBox.cfc`, a
  `coldbox.xml.cfm`, or a component that sets `coldbox` to a struct, as ColdBox's configuration does under any name, or
  extends one that does, as a site built on a CMS built on ColdBox does. An application of another framework, as
  Model-Glue's or Mach-II's, has `views` and an `event` of its own, so ColdBox resolving there is not enough.
  - A component in a `handlers` or `interceptors` directory that extends nothing extends `coldbox.system.EventHandler`
    or `coldbox.system.Interceptor` virtually, as ColdBox makes it. A handler's or interceptor's untyped `event` is
    `coldbox.system.web.context.RequestContext`.
  - A template in a `views` or `layouts` directory runs in `coldbox.system.web.Renderer`, whose methods it calls
    unqualified, and has `event`.

A dependency's components are reachable through mappings, which are to CFML what a classpath is to Java:

```java
ColdFusionParser.builder()
        .mapping("testbox", testboxDirectory)
        .mapping("coldbox", coldboxDirectory)
        .build();
```

With these, `extends="testbox.system.BaseSpec"` resolves to `system/BaseSpec.cfc` in `testboxDirectory`, and a
spec's `expect(...)` has `testbox.system.BaseSpec`'s method type. A package that CommandBox installs is a mapping at
the path it installs to. The parser indexes a mapping's components once and reuses them for every parse.

The repository's own mappings need no configuration. An `Application.cfc` declares them in its pseudo-constructor,
`this.mappings["/lib"] = getDirectoryFromPath(getCurrentTemplatePath()) & "vendor/lib"`, for the files under it, and
a ColdBox module's `ModuleConfig.cfc` declares one for its directory with `this.cfmapping`. A mapping's path counts
when it is built from strings, `getDirectoryFromPath(getCurrentTemplatePath())`, `expandPath` and the variables
assigned before it.

`ColdFusionParser.parseInputs` parses every file first to index its components and templates, then parses each file
again to type it, so it holds one file's tree at a time. An overload takes inputs that are only indexed: they resolve
as the other files' names reach them, but they are neither typed nor returned. A caller that changed a few files of a
repository types only those, and each is typed as it would be if every file were. The index takes the files in the
order of their paths for that.

## Type inference

A `TypeInference` types more of a repository than these rules do, from what all of its files say together. The
parser makes one for each `parseInputs` call with the factory given to `ColdFusionParser.builder().typeInference(...)`.
Once it has indexed the repository, it calls it in three steps:

1. `collect` with each file's typed tree. The parser parses and types each file once more for this and drops the tree
   afterward, so it still holds one tree at a time.
2. `solve`, once.
3. `attribute` with each file's typed tree, before the parser returns it.

Its `TypeContext` is what the parser knows of the repository's types: resolution, lookups and the components of the
repository and its dependencies, CFML's values and built-in functions, and the JDK's classes. It says which functions a
component assigns to other objects, as MockBox assigns `$` to each mock, so a value that calls one need not be that
component. The two parses of a file
give its trees different ids, so `callKeys` keys each call by the file's path and the call's place among the file's
calls, which both parses share.

A type the inference gives a tree carries an `InferredType` marker that names what it rests on: `FLOW`, `FRAMEWORK` or
`USAGE`. A recipe that must be certain can leave such trees out. The marker is in this module, so an LST that carries
it reads without the inference. Every tree with the same evidence shares one marker, `InferredType.of(evidence)`, so
an LST stores each kind once. In `solve` the inference may also give a function of the repository a return type
with `setReturnType`, and a parameter the component every call passes with `setParameterType`, where the function
declares none. The parser then types every file with them, so they are for what the inference is sure of. The
parser marks nothing it types through them. The inference marks it in `attribute`. Every call, declaration, parameter
or other tree whose type rests on the inference carries the marker, and a call's or declaration's name shares its
marker. So a call without the marker has only the parser's own types. A dependency's functions are shared by every parse that maps the dependency, so they keep their types.

What an inference throws goes to the context's error handler, and the parser returns the trees as it typed them.

## Formatting

`autoFormat`, `autoIndent` and `maybeAutoFormat` on a CFML tree reach `ColdFusionAutoFormatService`, which
`Cf.CompilationUnit.service(...)` returns. `AutoFormatVisitor` runs the visitors in `org.openrewrite.coldfusion.format`
in turn, and the `AutoFormat` recipe runs it on whole files. The styles are in `org.openrewrite.coldfusion.style`:
tabs and indents, spaces, blank lines, and wrapping and braces, each covering script and tags.

A file's styles come from its `NamedStyles` markers. `Autodetect` infers them from a repository's own files, as the CLI
does at build time. Each setting is what most of the repository does, weighed by the lines it covers. Where a
repository gives no evidence, a setting is `CFFormat`'s: the defaults of CFFormat, the CommandBox module that formats
CFML, configured by `.cfformat.json`. CFML has no IDE style that everyone uses, so this is the one its community shares.

A template writes the text between its tags to the response, and the whitespace around that text. Re-indenting a
`<cfif>` there changes the page, the inside of a `<pre>`, or a CSV export. `TemplateOutput` decides which whitespace is
written, and no formatting visitor changes it, not even its line breaks or trailing spaces. A template writes none of
it inside `<cfsilent>`, in a `<cffunction>` or the body of a `<cfcomponent>` whose `output` is false, or after a
`<cfsetting enablecfoutputonly="true">` outside `<cfoutput>`. Only the tags that run their body in place, as `<cfif>`
and `<cfloop>` do, pass that silence on. The body of `<cfoutput>`, `<cfsavecontent>`, `<cfquery>`, `<cfmail>`, a
custom tag or any other tag keeps its whitespace. Even where a template is silent, a body with text in it keeps its
indentation, since the text lays it out as a page.

Whitespace inside tags, in `<cfscript>` and in script components is never written, so it is formatted. Strings and
the contents of comments never change. A doc comment's lines move with it when it is re-indented, since their
indentation is not their content. Inside `#interpolations#` only the spaces a tree needs to print are added.

`MinimumViableSpacingVisitor` gives a tree built without whitespace the spaces it needs to print as CFML that parses:
CFML's keywords double as names, so `var x` must not print as `varx`. A statement or a block a recipe builds with no
whitespace starts lines of its own, while one an author wrote on one line stays on it.

## Building and testing

```bash
./gradlew :rewrite-coldfusion:test
```

Keep fixtures inline as `cfm("...")` and `cfc("...")` text blocks, from `org.openrewrite.coldfusion.Assertions`:
`cfm` is a template, parsed as `template.cfm`, and `cfc` a component, parsed as `Component.cfc`. Set a source's path
when its name matters, as a component's does, since a component is named for its file.
`rewriteRun(cfm("..."))` with no expected output asserts that every character landed in the tree.

A test whose sources depend on a framework declares the ForgeBox packages they depend on, as a Java test declares a
classpath:

```java
@Override
public void defaults(RecipeSpec spec) {
    spec.recipe(new FindMethods("coldbox.system.FrameworkSupertype getInstance(..)", false))
      .parser(ColdFusionParser.builder().dependencies("coldbox@^7", "testbox"));
}
```

Each package is `slug` for its newest release or `slug@range` with CommandBox's ranges. It downloads from ForgeBox once
into `~/.rewrite/cache/forgebox` and is a mapping at the path CommandBox installs it to: `coldbox`, `testbox`, or
`modules/<slug>` for a ColdBox module. A version whose download is a GitHub repository, written as CommandBox writes
one, `owner/repo#ref`, comes from GitHub's archive of that ref. CommandBox clones the repository and checks the ref
out, or `master`, else `main`, when there is none, which leaves the same files. A JVM asks ForgeBox once for each package and indexes each set of packages
once. When ForgeBox cannot be reached, the newest cached version the range allows stands in.
`ForgeBoxDependenciesTest` downloads from ForgeBox.

`ColdFusionCorpusTest` measures the parser against real code. It is skipped unless
`-Dcoldfusion.corpus.dir` is set:

```bash
./gradlew :rewrite-coldfusion:test --tests "org.openrewrite.coldfusion.ColdFusionCorpusTest" \
  -Dcoldfusion.corpus.dir=/path/to/cfml \
  -Dcoldfusion.corpus.report=/tmp/corpus.txt
```

It prints a parse rate, a histogram of failure causes and a census of shapes that suggest a misparse.
Next to the report it writes `<report>.failures` (one `cause<TAB>path` line per file) and
`<report>.messages` (the full message per file).

`LstShapeCorpusTest` takes the same properties and checks the shape of every parsed file. Every
`Space` holds only whitespace and comments. Every element prints its markers, and its printed span
starts and ends with its own syntax rather than with space. Code search and recipe results find an
element's line that way. Each directory under the corpus is a repository, whose components it indexes
and types as `parseInputs` would, and its census counts what got a type. `-Dcoldfusion.corpus.limit`
checks only the first files.

`AutoFormatCorpusTest` takes the same properties and formats every parsed file with the style detected from its
repository. It checks that the result parses and prints back, that only whitespace changed, that a template writes
what it wrote before, and that formatting again changes nothing. It reports how many files and lines changed, for
each repository and for each visitor, which measures how well the detected style matches the code. Next to the
report it writes `<report>.details`, the first changes in each file.

After a grammar change, check that prediction stays local. `GrammarProfileTest` takes
`-Dcoldfusion.profile.report` and prints, for each parser decision, the most tokens it looked ahead,
with an example. A decision that looks ahead through a block will grow ANTLR's shared DFA without
bound across a large corpus.

## Known gaps

These throw a `ColdFusionParsingException` today:

- A self-closing tag with a space inside its `/ >`.
- A catch with a dotted or quoted type and no variable, `catch (java.lang.Exception)`.
- A function without a body or a semicolon, as the last statement of an interface.
- A closure called right after its body, `function() { ... }()`.
- Lucee's code islands (three backticks), `cfml:` prefixes, `new cfml:Query()`, and attributes
  written with a colon, `access:"remote"`.
- A `#` in the body of `<cfoutput>` that does not start an expression. The engines reject it too.

Tags imported with a prefix through `<cfimport>`, such as `<ui:button>`, are template text.
