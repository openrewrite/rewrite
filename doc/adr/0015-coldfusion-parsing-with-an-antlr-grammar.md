# 15. ColdFusion parsing with an ANTLR grammar

## Status

Accepted

## Context

CFML mixes two syntaxes in one file. Tag syntax is markup: `<cfquery>`, `<cfif x GT 1>`, `<cfset
y = x>`. Script syntax is a C-like language, in `<cfscript>` blocks and in components written as
`component { ... }`. Most expressions can appear in either: in a tag's expression, an attribute value,
a `#...#` interpolation or a script statement.

Lexing CFML depends on context:

- `#name#` interpolates in strings, attribute values and the bodies of `<cfoutput>`, `<cfquery>` and
  `<cfmail>`. Elsewhere in a template it is text.
- `>` ends a tag's expression unless it is inside brackets. In script it compares.
- `<!--- --->` comments nest.
- Keywords and spelled-out operators are case-insensitive, and some span several words
  (`GREATER THAN OR EQUAL TO`, `DOES NOT CONTAIN`).

Most keywords may also name a variable, and statements need no semicolon between them. A grammar
that leaves those choices to the parser is ambiguous in a costly way. If one reading of a token can
end a statement and start another, ANTLR's prediction keeps both readings alive until they meet
again. That is often the end of the next block, so prediction would scan whole function bodies.

The engines also accept code that a strict grammar would reject, and real code depends on it. ColdBox
leaves the comma out between parameters on separate lines. CommandBox writes `var` inside an
expression and `elseif` in script. Lucee allows `<!--- --->` in script.

No CFML parser is available as a library. Adobe ColdFusion's is closed. Lucee's is LGPL and part of
its runtime. BoxLang's ANTLR grammar depends on its runtime to tell which names are tags.
`rewrite-xml` parses CFML as XML only by accident: on 14,038 files from the 50 most-starred
ColdFusion repositories on GitHub, 8.5% came out fully structured.

## Decision

`rewrite-coldfusion` parses with an ANTLR grammar, `CFMLLexer.g4` and `CFMLParser.g4`.
`ColdFusionParserVisitor` maps the parse tree to the LST, as the HCL, Dockerfile and TOML modules do.

The lexer resolves what depends on context, by modes and by the tokens it has emitted:

- Modes separate template text, a tag's attributes, strings and script. A stack of contexts records
  why the lexer is lexing script: a component, a `<cfscript>` body, a tag's expression or a `#...#`.
  Each context counts its open brackets, which decides whether `>` ends a tag and `#` an
  interpolation.
- To know where `#` interpolates, the lexer folds start and end tags the way the visitor does.
- A word is a keyword only when what follows makes it one: `if` before `(`, `var` before a name.
- What the previous token was decides what is next. After an operand, `(` and `[` on the same line
  call and index it, `+` adds, and `mod` is an operator. Elsewhere `(` and `[` start an expression, `+`
  is a sign, and `mod` is a name.

The parser classifies the first word of each statement, since only there does `component`,
`property`, `public` or a tag name like `lock` mean something. It retypes that token, and the brace
of a tag's body, so that every statement starts with a token only it can start with. The rest of the
grammar keeps each decision local. An `else` is matched in one place, so that SLL prediction sees at
once which `if` it may belong to.

The visitor builds the LST:

- Tag syntax becomes `Cf` types: `Cf.Tag`, `Cf.TagAttribute`, `Cf.Text`. The template is parsed
  flat and folded: a tag closed by its end tag holds its body, and a tag with no end tag stands
  alone. Whitespace and `<!--- --->` comments between tags are space, as they are in script.
- Declarations are `J` types in either syntax. A component is a `J.ClassDeclaration` named for its
  file, as a Java compact source file's implicit class is. A function is a `J.MethodDeclaration`, its
  arguments are its parameters, and a property is a field. A `TagSyntax` marker prints
  `<cfcomponent>`, `<cffunction>`, `<cfargument>` and `<cfproperty>` as tags. A declaration's
  attributes are `J.Annotation`s typed by the tag that defines them.
- Script and every expression become `J` types, wherever they are written. A call is a
  `J.MethodInvocation` in a `<cfset>`, an interpolation and a script block alike.
- Where CFML spells a `J` construct differently, a marker records the spelling: `OperatorSpelling`
  for `EQ` or `AND`, `KeywordSpelling` for `If` or `RETURN`, `FunctionKeyword`, `SafeNavigation`.
- Constructs with no `J` counterpart are `Cf` types: string interpolation, struct literals, tags
  written as script statements, and the CFML-only operators. An array literal is a `J.NewArray` with
  no type, as it is in rewrite-javascript.
- Syntax the grammar does not accept throws a `ColdFusionParsingException`. Nothing maps to
  `J.Unknown`.

`ColdFusionCorpusTest` measures the parser against real code. It reports a parse rate, a histogram
of failure causes, and a census of tree shapes that suggest a misparse a round trip cannot catch.

## Consequences

- The module depends on the ANTLR runtime, as other modules do. Its main code compiles to Java 8
  bytecode.
- Recipes see one model for an expression in either syntax, and Java recipes over `J` types apply to
  CFML script.
- CFML syntax is taught by changing the grammar, regenerating, and adding a round-trip test. A change
  must keep prediction local. `GrammarProfileTest` reports each decision's longest lookahead.
- On the 35,440 CFML files of 250 open-source ColdFusion repositories, 99.0% parse and print back
  byte for byte. Most of the rest are broken code, scaffolder meta-templates, or Lucee's tests of its
  newest syntax. After the whole corpus ANTLR's shared DFA holds about 2,900 states, and a file takes
  0.3 ms to parse on average.
- A hand-written recursive-descent parser came first and reached the same parse rate. On the files
  both accept, the grammar builds the same tree for all but two, where it reads more correctly. The
  grammar keeps the module's parser in the same form as the other language modules'.
