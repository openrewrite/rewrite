# rewrite-coldfusion Guidelines

## Parser front end

The grammar is `src/main/antlr/CFMLLexer.g4` and `CFMLParser.g4`. After changing either, regenerate
`internal/grammar` with `./gradlew :rewrite-coldfusion:generateAntlrSources` and commit the result.
`ColdFusionParserVisitor` maps the parse tree to the LST. Its cursor walks the source behind the
tokens, so the whitespace and comments before a token become the prefix of the element it starts.

The lexer's modes:

- `TEMPLATE`: text, `<!--- --->` comments and tags. `#` opens an interpolation only in the body of
  `<cfoutput>`, `<cfquery>` or `<cfmail>`. To know that, the lexer folds start and end tags the way
  the visitor does.
- `TAG`, `ATTRIBUTE_VALUE`, `UNQUOTED` and `END_TAG`: a tag's attributes and its end tag.
- `DQ_STRING` and `SQ_STRING`: strings, where `#` interpolates.
- `DEFAULT_MODE`: script and expressions. A stack of contexts records why the lexer is there: a
  script component, a `<cfscript>` body, a tag's expression or a `#...#`. Each context counts its
  open brackets. Outside them, `>` ends a tag's expression and `#` ends an interpolation. `//` and
  `/*` start comments only in script and inside braces.

Whitespace and comments in a tag's expression, outside brackets, go to the `TAG_HIDDEN` channel.
There a call or an index may continue on the next line.

The lexer decides what the parser cannot cheaply decide, from what follows a token and what came
before it:

- A word is a keyword when what follows makes it one: `if` before `(`, `var` before a name, `catch`
  before `(`, `break` before the end of its statement. After `.` or `var`, and in a tag's expression
  outside a block, statement keywords are names.
- After an operand, `(` and `[` on the same line are a call and an index (`CALL_LPAREN`,
  `INDEX_LBRACKET`), `+` and `-` are binary, and `mod` or `eq` are operators. Elsewhere `(` and `[`
  start an expression, `+` and `-` are signs (`UNARY_PLUS`), and those words are names.
- Right after an arithmetic, concatenation or comparison operator, `!` and NOT bind tightly
  (`TIGHT_BANG`). Elsewhere they bind more loosely than comparison.

The parser retypes what depends on where a statement starts:

- `classifyStatement()` retypes a statement's first word: `component`, `property`, a function's
  modifiers and return type (`TYPE_WORD`), or a tag name such as `lock` (`SCRIPT_TAG_NAME`). A `{`
  that starts a statement becomes `BLOCK_LBRACE`. Modifiers may follow the return type too, as in
  `boolean public function f()`.
- The `{` of a tag's body, in `lock name="x" {` or `cfloop(...) {`, becomes `BODY_LBRACE`, so a block
  on the next line can never be read as the body.
- `markAttribute()` retypes the first word of each attribute of a property or a tag written as a
  statement, so the attribute loop needs no lookahead to see where its statement ends. After a comma, as in
  `thread name="t", action="run"`, it retypes the word after the comma.

All of this keeps every prediction short. Statements need no semicolon between them, so if a
decision could go either way across a block, ANTLR would look ahead through the whole block, and its
shared DFA would grow with every file parsed. Check a grammar change with `GrammarProfileTest`.

`ColdFusionParser` parses with SLL prediction first. After a syntax error it lexes again, since the
first pass retyped tokens, and parses with LL.

A template is parsed flat and folded into a tree by the visitor. A start tag pushes a frame unless it
never takes a body. Its end tag pops to it, and a tag left open between them gives its children to its
parent. An end tag for a tag that takes no body, as in `<cfthrow ...></cfthrow>`, closes the last such
tag.

A tree prints the file's bytes, not only its text. The printer writes a byte order mark from `charsetBomMarked`, and
`Cf.CompilationUnit.printEqualsInput` compares bytes. `ColdFusionParser.text` fails a file whose bytes do not decode to
text that encodes back to them, so a zip or a `cfencode`d template is a parse error and no recipe touches it. A NUL
alone does not fail a file, since real templates hold a stray one. The corpus tests read files through it too.

## Mapping rules

- **Nothing maps to `J.Unknown`.** Syntax the grammar does not accept throws a
  `ColdFusionParsingException` naming the unexpected token and the rule it was in. The corpus test
  buckets failures by that text.
- **Never put raw source text in a `J` field.** An identifier holds a name, a literal holds its value
  and its value source.
- **Prefer a marker for a spelling difference.** `EQ` is a `J.Binary` with `OperatorSpelling`,
  `If` a `J.If` with `KeywordSpelling`. A `Cf` type is for structure `J` cannot hold.
- **Never store LST elements in a marker.** A marker may hold a `Space`, as `FunctionKeyword` does.
- **A declaration's attributes are its annotations.** A component, function, parameter or property
  holds them as `J.Annotation`s, printed as `name=value`. One the language defines has the type of its
  tag's attribute, `cfcomponent.accessors`, from `LanguageAttributes`. Any other is typed by its own
  name, as WireBox's `singleton` is. A tag's own attributes are arguments, so they stay
  `Cf.TagAttribute`.
- **A wrapper only when it is needed.** `Cf.Closure` wraps a `J.Lambda` only when it has attributes,
  since a lambda holds no annotations.
- **A component is a `J.ClassDeclaration` named for its file.** Its name is never printed.
  `extends` and `implements` are its supertypes when their values are dotted names, with an
  `AttributeClause` recording where they are among the attributes and how they are written.
- **A declaration written as a tag is the `J` type its script form is.** A `<cfcomponent>`, a
  `<cffunction>`, the `<cfargument>` tags a function starts with and a component's `<cfproperty>` tags
  carry a `TagSyntax` marker. An attribute naming the declaration's name or type is that part, with an
  `AttributeClause` among the declaration's markers. A script `property` is a field in the same way.
  A declaration whose name is not a plain name stays a `Cf.Tag`, or a `Cf.ScriptTag` in script.
- **Types come from the repository and its mappings.** `ComponentIndex` holds every component of one
  `parseInputs` call and resolves the names files give them, in CFML's order: the caller's directory, the
  mappings an `Application.cfc` or `ModuleConfig.cfc` declares (`DeclaredMappings`), the application's root,
  the repository's root. A builder `mapping` is a dependency, as a classpath entry is for Java. `TypeAttribution`
  types each file from the index. A name that does not resolve to exactly one component stays untyped. WireBox's
  `inject` and ColdBox's `getInstance` follow WireBox's convention that an ID names a component.
- **A template is a type, and an include is a mixin.** The index holds templates beside components, by path with
  their extensions, and method lookup takes an includer's included templates after its own methods. A file's
  `Cf.CompilationUnit` carries its type, the component's or the template's, which `TypeAttribution` sets.
- **ColdBox's conventions need a ColdBox application.** ColdBox resolving is not enough, since the census maps it for
  every repository and another framework's application has `views` and an `event` of its own.
  `ComponentIndex.isColdBoxApplication` looks above the file for a `ModuleConfig.cfc`, ColdBox's configuration in
  `config/`, or a template that includes ColdBox's `coldbox.cfm`. A configuration counts when it or a supertype sets
  `coldbox` to a struct, so `link()` decides it once supertypes resolve. Views, layouts and the virtual supertypes of
  handlers and interceptors all ask it.
- **A function assigned to another object runs as that object's.** `ComponentIndex` notes the functions a component
  assigns away, as MockBox's `obj.$ = variables.$`, and a `this` such a function returns has no type.
  `TypeContext.isAssignedToOtherObjects` tells an inference which they are, so calling one is no evidence of a type.
- **A file's own type is declared, not used.** `ColdFusionVisitor` visits the `Cf.CompilationUnit`'s type, and
  `TypesInUse` skips a type visited at a source file's root, as it skips a class declaration's.
- **A DSL ID or a tag is typed only when it gives one type everywhere.** Take what WireBox's DSL gives from ColdBox's
  own builders, `system/ioc/Builder.cfc` and `system/ioc/dsl/*.cfc`, and count an ID or a tag in the corpus before
  adding it.
- **A member function is the built-in it aliases.** `BuiltIns` reads each function's member names and receiver types
  from `functions.tsv`, generated from Lucee's function libraries, and types CFML's values as `cfml.Array`,
  `cfml.Struct` and `cfml.Query`.
- **Inference types only what agrees.** A variable or struct key keeps a type while every assignment agrees, and a
  function's inferred return type needs every return to evaluate to the same type. Return chains are inferred to a
  fixpoint across the repository in `link()`, round after round until none changes. A cap on the rounds would leave
  what gets typed to the order of the functions, which differs between threads. A `var` declared as `""` or `0` is a
  placeholder (`ComponentIndex.isPlaceholder`). Its type stands until the first assignment, which replaces it rather
  than disagreeing with it. The locals, the return chains and the corpus test's classifier all treat it so.
- **What parses share stays as it was linked.** A dependency's `ComponentIndex` and its types are shared by every
  parse that maps the same packages, from any thread, as are `BuiltIns` and `JavaClasses`. Nothing changes them after
  `link()`. `JavaClasses` maps a JDK class under a lock, and the mapping is complete before another thread can see it.
- **A type inference sees typed trees, one at a time.** `parseInputs` calls a `TypeInference` after `link()`:
  `collect` for each file it parses and types once more, then `solve`, then `attribute` for each tree it returns. The
  two parses of a file give its elements different ids, so `TypeContext.callKeys` keys a call by its place among the
  file's calls. Only `solve` may change a function's type, since every tree shares it, and never a dependency's. An
  inference that throws never fails a parse: the error goes to the context and the parser's own types stand.
- **A `;` is a `Semicolon` marker on the statement's `JRightPadded`**, after the space in front of it.
  The printer never adds one.
- **Space goes left and outermost.** The whitespace and comments before an element are the prefix of
  the outermost element that starts there. What starts at the same place inside it has an empty
  prefix. A wrapper such as `Cf.Closure` holds the prefix, and the `J` element it wraps has none.
  Between tags, whitespace and `<!--- --->` comments are space too, never `Cf.Text`.
- **An expression is the same tree in either syntax.** Tag expressions, attribute values,
  interpolations and script all go through the grammar's `expression` rule and the visitor's
  `expression()`.

## The printer

`ColdFusionPrinter` prints `Cf` types and hands `J` types to its inner `ColdFusionJavaPrinter`. That
class overrides the `JavaPrinter` methods whose CFML spelling differs. Each printer puts back the
other's cursor after a hand-off. It prints a function's and a parameter's annotations after their
parameters and their names, where CFML writes attributes. `visitAttributes` prints a declaration's
annotations with the parts it holds from attributes, such as a component's `extends`, among them in the
order they are written. A declaration with a `TagSyntax` marker prints as its tag.

## Formatting

- **Never change what a template writes.** Every formatting visitor extends `FormattingVisitor` and leaves a
  space alone when `isWritten` says the template writes it. `TemplateOutput` decides that, and stays
  conservative: a tag it does not know keeps its body's whitespace.
- **Only a tree built without whitespace changes structure.** On parsed code `NormalizeFormatVisitor` changes
  nothing, `MinimumViableSpacingVisitor` almost nothing, and `WrappingAndBracesVisitor` moves only braces and the
  keywords after blocks. A statement on the line of another stays there unless it has no space before it.
- **A setting is honoured, detected and defaulted.** A new style setting needs a visitor that follows it, a
  measure in `Autodetect` weighed by the lines it covers, and a default in `CFFormat` that is CFFormat's own.
- **Keep a line's characters when its width is right.** `TabsAndIndentsVisitor` rewrites indentation only
  where the width changes, so tabs followed by aligning spaces survive.

## Testing

Run tests with `./gradlew :rewrite-coldfusion:test`.

Keep fixtures inline as `cfm("...")` or `cfc("...")` text blocks. When fixing a parser issue, add a
round-trip test, and assert the shape of the tree where the parse could go two ways.

A test whose sources depend on a framework sets `spec.parser(ColdFusionParser.builder().dependencies("coldbox@^7"))`,
as a Java test sets a classpath. A test of a ColdBox convention gives its sources a ColdBox application, as a
`config/ColdBox.cfc`, and shows the other side too. `TypeInferenceTest` drives the hook with a small inference that
records what it is called with. `ForgeBoxPackages` downloads each package into `~/.rewrite/cache/forgebox`, so
`ForgeBoxDependenciesTest` needs ForgeBox the first time it runs. A test of resolution itself writes its
dependency into a `@TempDir` and maps it with `mapping`, as `TypeAttributionTest` does.

`ColdFusionCorpusTest` is opt-in. Point `-Dcoldfusion.corpus.dir` at a directory of CFML. Its
census of suspicious shapes matters as much as the parse rate, since a misparse can still round-trip.
Add `-Dcoldfusion.corpus.context=1` to write the source around each suspicious shape to
`<report>.context`.

`LstShapeCorpusTest` is opt-in too. With `-Dcoldfusion.corpus.dir` it checks every parsed file. Each
`Space` must hold only whitespace and comments, and no name may hold whitespace. The visitor must
reach every element, and the printer must print every element's markers. Each element's printed span
must start and end with its own syntax, not with space. Code search places a symbol by printing the
markers around it, so a span that starts with space puts a match on the line before it. Each directory
under the corpus is one repository, indexed and typed as one `parseInputs` call.

`AutoFormatCorpusTest` is opt-in too. After changing a formatting visitor or `Autodetect`, run it on the
corpus. No file may fail a check, and a repository a formatter keeps consistent should change a few lines in a
thousand. Its lines changed by each visitor say where the changes come from.

`GrammarProfileTest` is opt-in too. With `-Dcoldfusion.corpus.dir` and `-Dcoldfusion.profile.report`
it profiles up to `-Dcoldfusion.profile.limit` files (2,000 by default) and lists each decision's
longest SLL lookahead, with the tokens. More than a statement's worth points at a decision to fix
in the lexer or with a retype.
