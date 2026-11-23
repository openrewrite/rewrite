# C# Parser Whitespace Violations Report

**Date:** 2026-03-13
**Validator:** `WhitespaceValidator` (`CSharpVisitor<List<WhitespaceViolation>>`)
**Test suite:** 1241 tests, **76 failures** across 12 distinct violation categories

## Summary

The `WhitespaceValidator` walks every AST node, inspects all `Space` fields (including inside `JRightPadded`, `JLeftPadded`, and `JContainer`), and asserts that `Space.Whitespace` and `Comment.Suffix` contain only whitespace characters. Any non-whitespace content in a Space indicates that source code was not properly parsed into the AST and instead leaked into formatting metadata.

---

## Violation Categories

### 1. `unsafe` keyword in using directives (19 failures)

**Symptom:** `Identifier.Prefix` contains `" unsafe "`

**Example input:**
```csharp
using unsafe int* = int*;
```

**Failing tests:** `UsingDirectiveParsingTests.AliasUsingDirectiveNamePointer2`, `AliasUsingDirectiveFunctionPointer2`, `UsingUnsafeNonAlias`, `AliasUsingDirectivePredefinedTypePointer2`–`7`, `AliasUsingVoidPointer1`, etc.

**Root cause:** `VisitUsingDirective()` at `CSharpParser.cs:355` handles `global` (line 360) and `static` (line 372) keywords explicitly, but does **not** check for `node.UnsafeKeyword`. After `static` handling (line 378), the parser jumps straight to parsing the namespace/type (line 401), so the `unsafe` keyword and its trailing space are consumed as part of the type identifier's prefix.

**Proposed fix:** Add `unsafe` keyword handling between lines 378 and 380:
```csharp
// Handle 'unsafe' keyword (C# 12 using directives)
bool isUnsafe = node.UnsafeKeyword.IsKind(SyntaxKind.UnsafeKeyword);
if (isUnsafe)
{
    // Extract space before 'unsafe', advance cursor past it
    var unsafeSpace = ExtractSpaceBefore(node.UnsafeKeyword);
    _cursor = node.UnsafeKeyword.Span.End;
}
```

This requires either adding an `Unsafe` field to the `UsingDirective` AST class, or modeling it as a `Keyword` node or `Modifier`.

**Alternative:** Model `unsafe` as a modifier on `UsingDirective` (add a modifiers list), similar to how `ClassDeclaration` handles modifiers. This would be more general but a larger change to `Cs.cs`.

---

### 2. Assembly/module-level attributes (10 failures)

**Symptom:** `CompilationUnit.Eof` contains `"[assembly:a]"`, `"[module:a]"`, `"[@assembly:a]"`, `"[assembly:a(b, c)]"`, etc.

**Example input:**
```csharp
[assembly: MyAttribute]
```

**Failing tests:** `DeclarationParsingTests.TestGlobalAttribute`, `TestGlobalAttribute_Verbatim`, `TestGlobalAttribute_Escape`, `TestGlobalModuleAttribute`, `TestGlobalModuleAttribute_Verbatim`, `TestGlobalAttributeWithParentheses`, `TestGlobalAttributeWithMultipleArguments`, `TestGlobalAttributeWithNamedArguments`, `TestGlobalAttributeWithMultipleAttributes`, `TestMultipleGlobalAttributeDeclarations`

**Root cause:** `VisitCompilationUnit()` at `CSharpParser.cs:283` iterates `node.Usings` (line 297) and `node.Members` (line 305) but **never** iterates `node.AttributeLists`. Roslyn's `CompilationUnitSyntax.AttributeLists` holds assembly/module-level attribute lists, which are left unparsed and captured by `ExtractRemaining()` at line 334 into the `eof` space.

**Proposed fix:** Add attribute list processing before using directives:
```csharp
// Handle assembly/module-level attributes
foreach (var attrList in node.AttributeLists)
{
    members.AddRange(ProcessGapDirectives(attrList.SpanStart));
    var visited = VisitAttributeList(attrList);
    if (visited is Statement stmt)
        members.Add(stmt);
}
```

The existing `VisitAttributeList` method can handle these — the `AttributeList` AST type already supports a `Target` field for `assembly:` / `module:` targets.

**Alternative:** Wrap assembly-level attributes in an `AnnotatedStatement` with an `Empty` statement body. This mirrors how type-level attributes work but may feel odd for file-level attributes that don't annotate a specific declaration.

---

### 3. `extern alias` directives (3 failures)

**Symptom:** `CompilationUnit.Eof` or `NamespaceDeclaration.End` contains `"extern alias a;"`

**Example input:**
```csharp
extern alias a;
```

**Failing tests:** `DeclarationParsingTests.TestExternAlias`, `TestNamespaceWithExternAlias`, `TestFileScopedNamespaceWithExternAlias`

**Root cause:** `VisitCompilationUnit()` does **not** iterate `node.Externs`. The `VisitExternAliasDirective()` method exists at line 3217 but is never called from `VisitCompilationUnit`. Similarly, namespace declarations don't visit their `Externs` collection.

**Proposed fix:** Add extern alias processing in `VisitCompilationUnit()` before using directives:
```csharp
// Handle extern alias directives
foreach (var externAlias in node.Externs)
{
    members.AddRange(ProcessGapDirectives(externAlias.SpanStart));
    var visited = VisitExternAliasDirective(externAlias);
    members.Add(visited);
}
```

And similarly in `VisitNamespaceDeclaration` / `VisitFileScopedNamespaceDeclaration`.

**Alternative:** None — extern aliases are first-class syntax and must be modeled in the AST. The existing `ExternAlias` type in `Cs.cs` already handles this.

---

### 4. Type constraints on non-generic types/methods (6 failures)

**Symptom:** `Block.Prefix` contains `" where T : class "`, `" where b : c "`, `" where T1 : U1\n"`, etc.

**Example input:**
```csharp
class Foo where T : class { }  // non-generic class with constraint (invalid C# but parseable)
void Bar() where T : IFoo { }  // non-generic method with constraint
```

**Failing tests:** `DeclarationParsingTests.TestNonGenericClassWithTypeConstraintBound`, `TestNonGenericMethodWithTypeConstraintBound`, `TestClassWithMultipleConstraints`, `TestGenericClassMethodWithTypeConstraintBound`, `Interface_SemicolonBodyAfterConstraint_01`, `Interface_SemicolonBodyAfterConstraint_02`

**Root cause:** In `MergeConstraintClauses()`, the parser intentionally leaves constraint text unprocessed for non-generic types (lines 891–895). This was a design choice for "lossless round-trip" of invalid C# code.

**Proposed fix:** Process constraint clauses even for non-generic types by creating `ConstrainedTypeParameter` nodes with synthesized type parameter names. The `ConstrainedTypeParameter` type already exists in `Cs.cs` and can represent these constraints.

```csharp
// Even for non-generic types, parse the where clause into a
// ConstrainedTypeParameter with the constraint identifier from the clause itself
```

**Alternative:** Accept this as intentional behavior for invalid C# syntax. These test cases exercise error-recovery parsing, not valid code. If the goal is to only validate well-formed C#, these tests could be annotated as expected-violations. However, even invalid code should ideally be fully structured in the AST.

---

### 5. Primary constructor base calls (5 failures)

**Symptom:** `Block.Prefix` contains `"(Name)"`, `"(x, y)"`, `"(X, Y)"`, `"(1)"`, `"(x) "`

**Example input:**
```csharp
record Foo(string Name) : Base(Name) { }
class Derived(int x) : Base(x) { }
```

**Failing tests:** `ClassDeclarationTests.RecordWithPrimaryConstructorAndBaseCall`, `PrimaryConstructorWithBaseCallMultipleArgs`, `ClassWithPrimaryConstructorAndBaseCall`, `RecordParsingTests.Base_05`, `RecordStructParsing_BaseListWithParens`

**Root cause:** The class declaration parser handles base types via `node.BaseList` (lines 817–883) but does not handle the argument list on base type references. In Roslyn, `PrimaryConstructorBaseTypeSyntax` has an `ArgumentList` property that represents the `(Name)` part in `: Base(Name)`. The parser only visits the base type name, not its arguments.

**Proposed fix:** When processing the base list, check if a base type is `PrimaryConstructorBaseTypeSyntax` and parse its `ArgumentList`:
```csharp
if (baseType is PrimaryConstructorBaseTypeSyntax pcbt && pcbt.ArgumentList != null)
{
    // Parse the argument list as constructor arguments on the extends clause
    // Could be stored as part of the ClassDeclaration's extends padding
}
```

This needs a new field on `ClassDeclaration` or a wrapper type for the base type + argument list.

**Alternative:** Store the base arguments as a `MethodInvocation`-like node wrapping the base type reference, similar to how Java's `super()` calls work in constructor bodies. This would reuse existing AST types.

---

### 6. Explicit interface implementations on methods (1 failure from validator, more from fragment tests)

**Symptom:** `Identifier.Prefix` contains `" IFoo."` or `" I."`

**Example input:**
```csharp
class Foo : IFoo {
    void IFoo.Bar() { }
}
```

**Failing tests:** `WhitespaceValidatorTests.ExplicitInterfaceImplementation`, `DeclarationParsingTests.TestClassPropertyExplicit`

**Root cause:** `VisitMethodDeclaration()` at `CSharpParser.cs:1130` does **not** check `node.ExplicitInterfaceSpecifier`. After parsing the return type (line 1144), the parser extracts the name prefix (line 1147), which absorbs the interface name and dot. Other members like `EventDeclaration` (line 1581), `IndexerDeclaration` (line 1642), and `OperatorDeclaration` (line 1722) handle this correctly.

**Proposed fix:** Add explicit interface specifier handling after return type, before name parsing:
```csharp
// After parsing return type (line 1144):
JRightPadded<TypeTree>? explicitInterfaceSpecifier = null;
if (node.ExplicitInterfaceSpecifier != null)
{
    var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
    var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
    _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
    explicitInterfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
}
```

The `MethodDeclaration` Java AST type doesn't have an interface specifier field, so this would need to be stored differently — perhaps as a marker, or a synthetic `FieldAccess` for the qualified name.

**Alternative:** Encode the interface name as part of the method name using a `FieldAccess` (e.g., `IFoo.Bar` as `FieldAccess(Identifier("IFoo"), Identifier("Bar"))`). This avoids adding a new AST field but changes the name's type.

---

### 7. Explicit interface implementations on properties (1 failure)

**Symptom:** Same as #6 but for properties — `Identifier.Prefix` contains `" I."`

**Root cause:** `VisitPropertyDeclaration()` at `CSharpParser.cs:1911` passes `null` for the `interfaceSpecifier` parameter (line 1968). The `PropertyDeclaration` AST class already HAS an `InterfaceSpecifier` field — it just isn't populated.

**Proposed fix:** Add handling before name parsing (between lines 1925 and 1927):
```csharp
JRightPadded<TypeTree>? interfaceSpecifier = null;
if (node.ExplicitInterfaceSpecifier != null)
{
    var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
    var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
    _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
    interfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
}
```

Then pass `interfaceSpecifier` instead of `null` on line 1968.

**Alternative:** None — the AST field exists, it just needs to be populated. This is a straightforward omission.

---

### 8. Property initializers (3 failures)

**Symptom:** `Block.End` contains `" = 10;\n"`, `" = d; "`, `" = 0; "`

**Example input:**
```csharp
public int X { get; set; } = 10;
```

**Failing tests:** `WhitespaceValidatorTests.PropertyWithInitializer`, `DeclarationParsingTests.TestClassAutoPropertyWithInitializer`, `InitializerOnNonAutoProp`

**Root cause:** `VisitPropertyDeclaration()` passes `null` for the `initializer` parameter (line 1972). After `VisitAccessorList()` returns (line 1958), the parser does not check `node.Initializer`. The initializer text and semicolon are left between the accessor block's close brace and the cursor, getting absorbed into the block's `End` space.

**Proposed fix:** Add initializer handling after the accessor list (between lines 1958 and 1960):
```csharp
JLeftPadded<Expression>? initializer = null;
if (node.Initializer != null)
{
    var equalsSpace = ExtractSpaceBefore(node.Initializer.EqualsToken);
    _cursor = node.Initializer.EqualsToken.Span.End;
    var initExpr = (Expression)Visit(node.Initializer.Value)!;
    initializer = new JLeftPadded<Expression>(equalsSpace, initExpr);
}
if (node.SemicolonToken != default && node.SemicolonToken.Span.Length > 0)
{
    SkipTo(node.SemicolonToken.SpanStart);
    SkipToken(node.SemicolonToken);
}
```

Then pass `initializer` instead of `null` on line 1972. The `PropertyDeclaration` AST class already has an `Initializer` field.

**Alternative:** None — like #7, the AST field exists and just needs to be populated.

---

### 9. Conversion operator explicit interface implementations (2 failures)

**Symptom:** `ConversionOperatorDeclaration.ReturnType.Before` contains `" N.I."`

**Example input:**
```csharp
class Foo : N.I {
    static implicit N.I.operator int(Foo f) => 0;
}
```

**Failing tests:** `MemberDeclarationParsingTests.ConversionDeclaration_ExplicitImplementation_01`, `ConversionDeclaration_ExplicitImplementation_11`

**Root cause:** `VisitConversionOperatorDeclaration()` at `CSharpParser.cs:1834` does **not** check `node.ExplicitInterfaceSpecifier`. After parsing the `implicit`/`explicit` keyword (line 1847), the parser jumps to the `operator` keyword (line 1854). The interface qualifier `N.I.` is between these tokens and gets absorbed into the `operator` keyword's prefix, which becomes `ReturnType.Before`.

**Proposed fix:** Add explicit interface specifier handling after `implicit`/`explicit` keyword:
```csharp
// After line 1851, before line 1854:
JRightPadded<TypeTree>? explicitInterfaceSpecifier = null;
if (node.ExplicitInterfaceSpecifier != null)
{
    var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
    var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
    _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
    explicitInterfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
}
```

Requires adding an `ExplicitInterfaceSpecifier` field to the `ConversionOperatorDeclaration` AST class.

**Alternative:** Encode the interface as part of the `operator` keyword or as a marker. Less clean but avoids AST changes.

---

### 10. `using` declaration statements (7 failures)

**Symptom:** `Identifier.Prefix` contains `"using "` or `"await using "`; `NullableType.Prefix` contains `"using "`

**Example input:**
```csharp
using var x = new Foo();
await using var x = new Foo();
```

**Failing tests:** `StatementParsingTests.TestUsingVarWithDeclaration`, `TestUsingVarWithVarDeclaration`, `TestAwaitUsingWithVarDeclaration`, `TestUsingVarWithDeclarationWithMultipleVariables`, `TestUsingVarSpecialCase1`, `TestUsingVarSpecialCase2`, `TestUsingVarSpecialCase3`

**Root cause:** `VisitLocalDeclarationStatement()` at `CSharpParser.cs:7614` processes `node.Modifiers` (line 7620). In Roslyn, `using` IS a modifier on `LocalDeclarationStatementSyntax` (as `SyntaxKind.UsingKeyword`). However, `MapModifier()` at line 8347 does not include `SyntaxKind.UsingKeyword` in its switch — it falls through to `LanguageExtension`. If the `using` token is actually not in `node.Modifiers` for these test inputs (e.g., due to C# version settings), the `using` keyword would remain unparsed and leak into the type prefix.

**Investigation needed:** Verify whether Roslyn places `using` in `node.Modifiers` for all C# versions tested. If not, `VisitLocalDeclarationStatement` needs explicit `UsingKeyword` handling like `VisitUsingDirective` does for `global` and `static`.

**Proposed fix (if `using` IS in Modifiers):** Add to `MapModifier`:
```csharp
SyntaxKind.UsingKeyword => Modifier.ModifierType.LanguageExtension,
```
This shouldn't be needed since it already falls through — so the issue may be that `using` isn't actually in `node.Modifiers` for some Roslyn parse modes.

**Proposed fix (if `using` is NOT in Modifiers):** Handle the `using` keyword explicitly before modifier processing, similar to `VisitUsingDirective`:
```csharp
// Before modifier loop:
if (node.UsingKeyword.IsKind(SyntaxKind.UsingKeyword))
{
    var usingSpace = ExtractSpaceBefore(node.UsingKeyword);
    _cursor = node.UsingKeyword.Span.End;
    modifiers.Add(CreateModifier(usingSpace, node.UsingKeyword));
}
if (node.AwaitKeyword.IsKind(SyntaxKind.AwaitKeyword))
{
    var awaitSpace = ExtractSpaceBefore(node.AwaitKeyword);
    _cursor = node.AwaitKeyword.Span.End;
    modifiers.Add(CreateModifier(awaitSpace, node.AwaitKeyword));
}
```

**Alternative:** Model `using` variable declarations as a `UsingStatement` wrapping a `VariableDeclarations`, similar to how the `using (var x = ...) { }` block form is handled. This would be semantically clearer but requires changing the AST output.

---

### 11. Preprocessor directive comments (2 failures)

**Symptom:** `DefineDirective.Prefix` contains `"\n//130 chars (max is 128)\n"` or `"\n//128 chars (max)\n"`

**Example input:**
```csharp
//130 chars (max is 128)
#define VERY_LONG_IDENTIFIER...
```

**Failing tests:** `DeclarationParsingTests.RegressLongDirectiveIdentifierDefn`, `RegressLongDirectiveIdentifierUse`

**Root cause:** `ProcessGapDirectives()` at `CSharpParser.cs:8389` scans source text between AST positions for preprocessor directives. Comments before a `#define` directive are captured as raw text in the directive's prefix via `ExtractPrefix`/`ExtractSpaceBefore`, but these methods call `Space.Format()` (line 54 of `Space.cs`) which treats the text as pure whitespace and does not parse comments. The comment `//130 chars...` is placed directly in `Space.Whitespace`.

**Proposed fix:** Use `Space.FormatWithComments()` instead of `Space.Format()` in the prefix extraction methods when the text comes from real source code (not synthesized). `FormatWithComments()` already exists and properly structures `//` and `/* */` comments into `Comment` objects.

This requires changing `ExtractPrefix` or `ExtractSpaceBefore` to call `FormatWithComments` when the source text between cursor and the target position contains comment markers.

**Alternative:** Change the prefix extraction to always use `FormatWithComments`. This is simpler but may have performance implications since it parses every whitespace string for comment markers. Alternatively, add a fast check: if the string contains `/`, delegate to `FormatWithComments`.

---

### 12. Miscellaneous (4 failures)

#### 12a. `in` parameter modifier in older C# versions
**Symptom:** `Identifier.Prefix` contains `"in "`
**Test:** `RefReadonlyTests.InArgs_CSharp7`
**Root cause:** When parsing with a C# version that doesn't support `in` parameters, Roslyn may not include `in` in the modifier list, causing it to leak into the type prefix.
**Fix:** Same pattern as #10 — check for the `in` keyword explicitly if it's not in `node.Modifiers`.

#### 12b. Named arguments in older C# versions
**Symptom:** `Literal.Prefix` contains `"x:"`
**Test:** `ParserErrorMessageTests.NamedArgumentBeforeCSharp4`
**Root cause:** Error recovery in older language versions doesn't produce a `NamedArgument` node; the `x:` syntax leaks.
**Fix:** Handle the `NameColonSyntax` in argument parsing even in error recovery mode.

#### 12c. `when` clause before C# 6
**Symptom:** `Block.Prefix` contains `" when (true) "`
**Test:** `ParserErrorMessageTests.ExceptionFilterBeforeVersionSix`
**Root cause:** Exception filters (`when` clause on `catch`) are not supported before C# 6; Roslyn parses it as error recovery.
**Fix:** Handle the `WhenClauseSyntax` on catch blocks even in error recovery mode.

#### 12d. Record with trailing semicolon after block body
**Symptom:** `CompilationUnit.Eof` contains `";"`
**Test:** `RecordParsingTests.RecordParsing_BlockBodyAndSemiColon`
**Root cause:** When a record has both a block body (`{ }`) and a trailing semicolon, the semicolon is not consumed.
**Fix:** After processing the class body block, check for and consume a trailing semicolon token.

#### 12e. Directive in disabled code
**Symptom:** `PragmaWarningDirective.Prefix` contains `"\n//DIRECTIVE:0\n"`
**Test:** `ParserErrorMessageTests.PragmaBeforeCSharp2_InDisabledCode`
**Fix:** Same as #11 — use `FormatWithComments` for directive prefixes.

#### 12f. File-scoped namespace with usings/extern aliases
**Symptom:** `CompilationUnit.Eof` contains `" using b.c;"` or `" extern alias b;"`
**Tests:** `DeclarationParsingTests.TestFileScopedNamespaceWithUsing`, `TestFileScopedNamespaceWithExternAlias`
**Root cause:** `VisitFileScopedNamespaceDeclaration` processes `fsns.Members` but not `fsns.Usings` or `fsns.Externs`.
**Fix:** Also iterate `fsns.Usings` and `fsns.Externs` in the file-scoped namespace handling block (lines 317–328).

#### 12g. Attribute on delegate parameters
**Symptom:** `Identifier.Prefix` contains `"[attr] "`
**Test:** `DeclarationParsingTests.TestDelegateWithParameterAttribute`
**Root cause:** `ConvertParameter()` does not handle `node.AttributeLists` on parameters.
**Fix:** Process `node.AttributeLists` in `ConvertParameter()` before parsing modifiers.

---

## Priority Order

| Priority | Category | Failures | Complexity | AST changes needed |
|----------|----------|----------|------------|-------------------|
| **P0** | #7 Property explicit interface | 1 | Low | None — field exists |
| **P0** | #8 Property initializer | 3 | Low | None — field exists |
| **P0** | #11 Directive comments | 2+ | Low | None — `FormatWithComments` exists |
| **P1** | #2 Assembly/module attributes | 10 | Medium | None — types exist |
| **P1** | #3 Extern alias | 3 | Low | None — types exist |
| **P1** | #1 `unsafe` in using | 19 | Medium | Needs `Unsafe` field on `UsingDirective` |
| **P1** | #6 Method explicit interface | 1+ | Medium | Needs AST field or encoding |
| **P1** | #9 Conversion operator explicit interface | 2 | Medium | Needs AST field |
| **P2** | #5 Primary constructor base calls | 5 | High | Needs AST field |
| **P2** | #10 `using` declarations | 7 | Medium | Investigation needed |
| **P2** | #4 Non-generic type constraints | 6 | Medium | Design decision |
| **P3** | #12 Miscellaneous | 4 | Various | Various |

**P0** = Fix is straightforward, AST types already exist, just need to wire up parser
**P1** = Fix is clear but needs some AST additions
**P2** = Needs investigation or design decisions
**P3** = Edge cases / error recovery scenarios

---

## Test Infrastructure

Two layers of protection have been added:

1. **`WhitespaceValidator`** (`OpenRewrite/CSharp/WhitespaceValidator.cs`) — standalone visitor, usable in any context
2. **`RewriteTest` integration** (`OpenRewrite/Test/RewriteTest.cs`) — runs the validator automatically after every parse, so all existing and future `RewriteTest`-based tests catch whitespace violations
3. **`WhitespaceValidatorTests`** (`OpenRewrite/Tests/Tree/WhitespaceValidatorTests.cs`) — 86 dedicated tests covering broad C# syntax surface area
