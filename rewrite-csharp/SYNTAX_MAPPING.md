# Roslyn Syntax → OpenRewrite LST Mapping

Comprehensive mapping of every Roslyn C# syntax element to its OpenRewrite LST representation.

**Legend:**
- **J.Xxx** = Reused from the Java LST model
- **Cs.Xxx** = Custom C#-specific LST type
- **Linq.Xxx** = LINQ-specific LST type
- **+ MarkerName** = J type augmented with a marker for C#-specific semantics
- **Cs Justification** = Why a custom Cs type is needed instead of reusing J

---

## Declarations

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `CompilationUnitSyntax` | `Cs.CompilationUnit` | `using System; class C {}` | Has `externs`, `usings`, `attributeLists` sections not in J |
| `FileScopedNamespaceDeclarationSyntax` | `J.Package` | `namespace MyApp;` | |
| `NamespaceDeclarationSyntax` | `Cs.BlockScopeNamespaceDeclaration` | `namespace MyApp { }` | Block-scoped with nested usings/externs not in J.Package |
| `UsingDirectiveSyntax` | `Cs.UsingDirective` | `global using static System.Math;` | `global`, `static`, `unsafe` modifiers + alias syntax |
| `ExternAliasDirectiveSyntax` | `Cs.ExternAlias` | `extern alias LibA;` | No Java equivalent |
| `ClassDeclarationSyntax` | `J.ClassDeclaration` | `public class Foo { }` | |
| `InterfaceDeclarationSyntax` | `J.ClassDeclaration` | `public interface IFoo { }` | |
| `StructDeclarationSyntax` | `J.ClassDeclaration` + `Struct` | `public struct Point { }` | |
| `RecordDeclarationSyntax` | `J.ClassDeclaration` (kind=Record) | `public record Person(string Name);` | |
| `RecordDeclarationSyntax` (record class) | `J.ClassDeclaration` + `RecordClass` | `public record class Person(string Name);` | |
| `RecordDeclarationSyntax` (record struct) | `J.ClassDeclaration` + `Struct` | `public record struct Point(int X, int Y);` | |
| `EnumDeclarationSyntax` | `Cs.EnumDeclaration` | `enum Color : byte { Red, Green }` | C# enums support base types (`: byte`) |
| `EnumMemberDeclarationSyntax` | `Cs.EnumMemberDeclaration` | `Red = 1` | Has attribute lists per member |
| `DelegateDeclarationSyntax` | `Cs.DelegateDeclaration` | `delegate void Handler(int x);` | No Java equivalent for delegate types |
| `MethodDeclarationSyntax` | `Cs.MethodDeclaration` | `public int Get() => 42;` | Explicit interface specifier (`IFoo.Get()`), expression body |
| `ConstructorDeclarationSyntax` | `Cs.MethodDeclaration` | `public Foo(int x) { }` | Reuses MethodDeclaration with null return type |
| `DestructorDeclarationSyntax` | `Cs.DestructorDeclaration` | `~Foo() { }` | Wraps Cs.MethodDeclaration; `~` prefix |
| `LocalFunctionStatementSyntax` | `Cs.MethodDeclaration` | `int Add(int a, int b) => a + b;` | Local functions are method declarations |
| `OperatorDeclarationSyntax` | `Cs.OperatorDeclaration` | `public static int operator +(A a, A b) => ...;` | No Java operator overloading |
| `ConversionOperatorDeclarationSyntax` | `Cs.ConversionOperatorDeclaration` | `public static implicit operator int(A a) => ...;` | No Java implicit/explicit conversion |
| `PropertyDeclarationSyntax` | `Cs.PropertyDeclaration` | `public int X { get; set; } = 5;` | No Java properties; has accessors, expression body, initializer |
| `IndexerDeclarationSyntax` | `Cs.IndexerDeclaration` | `public int this[int i] { get; set; }` | No Java indexers |
| `EventDeclarationSyntax` | `Cs.EventDeclaration` | `public event Action OnClick { add {} remove {} }` | No Java events |
| `EventFieldDeclarationSyntax` | `J.VariableDeclarations` | `public event Action /*~~>*/OnClick;` | Field-like event uses standard variable declaration |
| `AccessorDeclarationSyntax` | `Cs.AccessorDeclaration` | `get { return _x; }` | get/set/init/add/remove not in Java |
| `FieldDeclarationSyntax` | `J.VariableDeclarations` | `private int _x = 5;` | |
| `AttributeListSyntax` | `Cs.AttributeList` | `[Obsolete("msg"), Serializable]` | Has target specifier (`[assembly: ...]`, `[return: ...]`) |
| `AttributeSyntax` | `J.Annotation` | `Obsolete("msg")` | |
| `ParameterSyntax` | `J.VariableDeclarations` | `int x` | |
| `TypeParameterSyntax` | `Cs.ConstrainedTypeParameter` | `T where T : class, new()` | Variance (`in`/`out`) + `where` constraints |
| Primary constructor | `J.MethodDeclaration` + `PrimaryConstructor` | `class Foo(int x /*~~>*/) { }` | Synthesized method in class body |

## Statements

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `BlockSyntax` | `J.Block` | `{ stmt1; stmt2; }` | |
| `GlobalStatementSyntax` | *(delegates to inner statement)* | `Console.WriteLine("hi");` | Transparent wrapper |
| `EmptyStatementSyntax` | `J.Empty` | `;` | |
| `ExpressionStatementSyntax` | `Cs.ExpressionStatement` | `x = 5;` | Could use J pattern but J lacks a standalone ExpressionStatement type |
| `ReturnStatementSyntax` | `J.Return` | `return 42;` | |
| `ThrowStatementSyntax` | `J.Throw` | `throw new Exception();` | |
| `IfStatementSyntax` | `J.If` | `if (x > 0) { }` | |
| `SwitchStatementSyntax` | `J.Switch` | `switch (x) { case 1: break; }` | |
| `SwitchExpressionSyntax` | `Cs.SwitchExpression` | `x switch { 1 => "one", _ => "other" }` | Switch expressions with pattern arms not in Java |
| `WhileStatementSyntax` | `J.WhileLoop` | `while (x > 0) { }` | |
| `DoStatementSyntax` | `J.DoWhileLoop` | `do { } while (x > 0);` | |
| `ForStatementSyntax` | `J.ForLoop` | `for (int i = 0; i < n; i++) { }` | |
| `ForEachStatementSyntax` | `J.ForEachLoop` | `foreach (var x in list) { }` | |
| `ForEachVariableStatementSyntax` | `Cs.ForEachVariableLoop` | `foreach (var (x, y) in points) { }` | Deconstruction variable in foreach |
| `TryStatementSyntax` | `Cs.Try` | `try { } catch (Exception e) when (e.Message != null) { }` | Catch filters (`when`) not in Java |
| `BreakStatementSyntax` | `J.Break` | `break;` | |
| `ContinueStatementSyntax` | `J.Continue` | `continue;` | |
| `GotoStatementSyntax` | `Cs.GotoStatement` | `goto case 1;` | `goto case`/`goto default` variants |
| `YieldStatementSyntax` | `Cs.Yield` | `yield return 42;` | `yield return` vs `yield break` keyword |
| `LabeledStatementSyntax` | `J.Label` | `myLabel: stmt;` | |
| `LockStatementSyntax` | `J.Synchronized` | `lock (obj) { }` | |
| `UsingStatementSyntax` | `Cs.UsingStatement` | `using (var s = new Stream()) { }` | `await using`, resource management pattern |
| `UnsafeStatementSyntax` | `Cs.UnsafeStatement` | `unsafe { int* p = &x; }` | No Java unsafe blocks |
| `FixedStatementSyntax` | `Cs.FixedStatement` | `fixed (int* p = arr) { }` | No Java fixed/pinning |
| `CheckedStatementSyntax` | `Cs.CheckedStatement` | `checked { int x = int.MaxValue + 1; }` | No Java checked/unchecked blocks |

## Expressions

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `LiteralExpressionSyntax` | `J.Literal` | `42`, `"hello"`, `true`, `null` | |
| `IdentifierNameSyntax` | `J.Identifier` | `myVar` | |
| `ThisExpressionSyntax` | `J.Identifier` | `this` | |
| `BaseExpressionSyntax` | `J.Identifier` | `base` | |
| `PrefixUnaryExpressionSyntax` | `J.Unary` | `!x`, `++i`, `-n` | |
| `PostfixUnaryExpressionSyntax` (standard) | `J.Unary` | `i++`, `i--` | |
| `PostfixUnaryExpressionSyntax` (`!`) | `Cs.NullSafeExpression` | `obj!` | Null-forgiving operator has no Java equivalent |
| `PrefixUnaryExpressionSyntax` (`*`) | `Cs.Unary` (PointerIndirection) | `*ptr` | Pointer dereference |
| `PrefixUnaryExpressionSyntax` (`&`) | `Cs.Unary` (AddressOf) | `&x` | Address-of operator |
| `PrefixUnaryExpressionSyntax` (`^`) | `Cs.Unary` (FromEnd) | `^3` *(in `arr[^3]`)* | Index-from-end operator |
| `BinaryExpressionSyntax` (standard) | `J.Binary` | `a + b`, `x && y` | |
| `BinaryExpressionSyntax` (`as`) | `Cs.Binary` | `obj as string` | `as` operator not in Java (Java uses cast) |
| `BinaryExpressionSyntax` (`??`) | `J.Ternary` + `NullCoalescing` | `a ?? b` | Reuses Ternary shape with marker |
| `ConditionalExpressionSyntax` | `J.Ternary` | `x > 0 ? "pos" : "neg"` | |
| `AssignmentExpressionSyntax` (`=`) | `J.Assignment` | `x = 5` | |
| `AssignmentExpressionSyntax` (compound) | `Cs.AssignmentOperation` | `x += 5`, `x ??= default` | `??=` operator not in Java |
| `ParenthesizedExpressionSyntax` | `J.Parentheses<Expression>` | `(x + y)` | |
| `CastExpressionSyntax` | `J.TypeCast` | `(int)x` | |
| `MemberAccessExpressionSyntax` (`.`) | `J.FieldAccess` | `obj.Property` | |
| `MemberAccessExpressionSyntax` (`->`) | `Cs.PointerFieldAccess` | `ptr->Field` | Pointer dereference not in Java |
| `ElementAccessExpressionSyntax` | `J.ArrayAccess` | `arr[0]` | |
| `ElementAccessExpressionSyntax` (multi-dim) | `J.ArrayAccess` + `MultiDimensionalArray` | `matrix[i, j]` | Multi-dimensional indexing via marker |
| `ConditionalAccessExpressionSyntax` (`?.`) | `J.FieldAccess`/`J.MethodInvocation` + `NullSafe` | `obj?.Prop`, `obj?.Method()` | Null-conditional via marker on existing types |
| `ImplicitElementAccessSyntax` | `Cs.ImplicitElementAccess` | `{ [key] = value }` | Dictionary initializer indexer |
| `InvocationExpressionSyntax` | `J.MethodInvocation` | `Foo(1, 2)` | |
| `InvocationExpressionSyntax` (delegate) | `J.MethodInvocation` + `DelegateInvocation` | `action()` | Delegate call sugar via marker |
| `ObjectCreationExpressionSyntax` | `Cs.NewClass` | `new Foo() { X = 1 }` | Wraps J.NewClass; adds object/collection initializer |
| `ImplicitObjectCreationExpressionSyntax` | `Cs.NewClass` + `OmitParentheses` | `Foo f = new() { X = 1 };` | Target-typed `new()` |
| `ArrayCreationExpressionSyntax` | `J.NewArray` | `new int[] { 1, 2, 3 }` | |
| `ImplicitArrayCreationExpressionSyntax` | `J.NewArray` | `new[] { 1, 2, 3 }` | |
| `StackAllocArrayCreationExpressionSyntax` | `Cs.StackAllocExpression` | `stackalloc int[10]` | No Java stack allocation |
| `ImplicitStackAllocArrayCreationExpressionSyntax` | `Cs.StackAllocExpression` | `stackalloc[] { 1, 2, 3 }` | No Java stack allocation |
| `CollectionExpressionSyntax` | `Cs.CollectionExpression` | `[1, 2, 3]` | C# 12 collection expressions |
| `InitializerExpressionSyntax` | `Cs.InitializerExpression` | `{ X = 1, Y = 2 }` | Object/collection initializer syntax |
| `SimpleLambdaExpressionSyntax` | `J.Lambda` | `x => x * 2` | |
| `ParenthesizedLambdaExpressionSyntax` | `J.Lambda` | `(x, y) => x + y` | |
| Lambda with modifiers/return type | `Cs.CsLambda` | `async static (x) => await x` | `async`/`static` modifiers, explicit return type |
| `AnonymousMethodExpressionSyntax` | `J.Lambda` + `AnonymousMethod` | `delegate(int x) { return x; }` | Legacy syntax via marker |
| `TupleExpressionSyntax` | `Cs.TupleExpression` | `(1, "hello")`, `(x: 1, y: 2)` | No Java tuples |
| `InterpolatedStringExpressionSyntax` | `Cs.InterpolatedString` | `$"Hello {name}!"` | No Java string interpolation |
| `InterpolatedStringTextSyntax` | `J.Literal` | `Hello ` *(text part)* | |
| `InterpolationSyntax` | `Cs.Interpolation` | `{name,10:F2}` | Alignment + format specifiers |
| `TypeOfExpressionSyntax` | `J.InstanceOf` | `typeof(int)` | Reuses InstanceOf shape |
| `SizeOfExpressionSyntax` | `Cs.SizeOf` | `sizeof(int)` | No Java sizeof |
| `DefaultExpressionSyntax` | `Cs.DefaultExpression` | `default(int)`, `default` | No Java default expression |
| `AwaitExpressionSyntax` | `Cs.AwaitExpression` | `await Task.Delay(100)` | No Java await keyword |
| `RefExpressionSyntax` | `Cs.RefExpression` | `ref x`, `out result`, `in value` | Pass-by-reference semantics |
| `ThrowExpressionSyntax` | `Cs.StatementExpression(J.Throw)` | `x ?? throw new Exception()` | Wraps statement as expression |
| `RangeExpressionSyntax` | `Cs.RangeExpression` | `1..5`, `..^1` | No Java range operator |
| `CheckedExpressionSyntax` | `Cs.CheckedExpression` | `checked(x + y)` | No Java checked expressions |
| `WithExpressionSyntax` | `Cs.WithExpression` | `person with { Name = "Bob" }` | Record `with` expression |
| `SpreadElementSyntax` | `Cs.SpreadExpression` | `[..list, 4, 5]` | C# 12 spread operator |
| `AnonymousObjectCreationExpressionSyntax` | `Cs.AnonymousObjectCreationExpression` | `new { Name = "Jo", Age = 30 }` | Anonymous types |
| `AnonymousObjectMemberDeclaratorSyntax` | `J.Assignment` or `Expression` | `Name = "Jo"` or `existingVar` | |
| `DeclarationExpressionSyntax` | `Cs.DeclarationExpression` | `out var x`, `out int result` | Inline variable declaration in expressions |

## Type Syntax

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `PredefinedTypeSyntax` | `J.Identifier` | `int`, `string`, `bool` | |
| `QualifiedNameSyntax` | `J.FieldAccess` (nested) | `System.Collections.Generic` | |
| `GenericNameSyntax` | `J.ParameterizedType` | `List<int>` | |
| `NullableTypeSyntax` | `J.ParameterizedType` | `int?` | Mapped as `Nullable<int>` |
| `ArrayTypeSyntax` | `Cs.ArrayType` | `int[,][]` | Multi-dimensional rank specifiers |
| `ArrayRankSpecifierSyntax` | `Cs.ArrayRankSpecifier` | `[,]` *(in `int[,]`)* | Rank dimensions |
| `TupleTypeSyntax` | `Cs.TupleType` | `(int x, string y)` | No Java tuple types |
| `TupleElementSyntax` | `Cs.TupleElement` | `int x` *(element within tuple type)* | Named tuple element |
| `PointerTypeSyntax` | `Cs.PointerType` | `int*` | No Java pointer types |
| `RefTypeSyntax` | `Cs.RefType` | `ref int`, `ref readonly int` | No Java ref types |
| `AliasQualifiedNameSyntax` | `Cs.AliasQualifiedName` | `global::System.String` | `::` alias qualifier |
| `FunctionPointerTypeSyntax` | `Cs.FunctionPointerType` | `delegate*<int, void>` | No Java function pointers |
| `OmittedTypeArgumentSyntax` | `J.Empty` | `typeof(List<>)` | |

## Pattern Matching

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `IsPatternExpressionSyntax` | `Cs.IsPattern` | `obj is string s` | `is` pattern expression |
| `DeclarationPatternSyntax` | `J.VariableDeclarations` | `string s` *(in `is string s`)* | |
| `VarPatternSyntax` | `J.VariableDeclarations` | `var x` *(in `is var x`)* | |
| `ConstantPatternSyntax` | `Cs.ConstantPattern` | `42`, `null` *(in `is null`)* | Pattern wrapper around constant value |
| `DiscardPatternSyntax` | `Cs.DiscardPattern` | `_` *(in `is _`)* | Discard pattern |
| `TypePatternSyntax` | *TypeTree* | `string` *(in `is string`)* | Returns the type directly |
| `RelationalPatternSyntax` | `Cs.RelationalPattern` | `> 5` *(in `case > 5`)* | Relational operators in patterns |
| `BinaryPatternSyntax` | `J.Binary` | `> 0 and < 100` | Reuses Binary with And/Or operators |
| `UnaryPatternSyntax` | `J.Unary` | `not null` | Reuses Unary with Not operator |
| `ParenthesizedPatternSyntax` | `J.Parentheses<Expression>` | `(> 0 and < 100)` | |
| `RecursivePatternSyntax` (positional) | `J.DeconstructionPattern` | `Point(int x, int y)` | |
| `RecursivePatternSyntax` (property) | `Cs.PropertyPattern` | `{ Length: > 5 }` | Property sub-patterns with `Cs.NamedExpression` elements |
| `ListPatternSyntax` | `Cs.ListPattern` | `[1, 2, ..]` | C# 11 list patterns |
| `SlicePatternSyntax` | `Cs.SlicePattern` | `..` *(in `[1, .., 5]`)* | Slice within list patterns |
| `SwitchExpressionArmSyntax` | `Cs.SwitchExpressionArm` | `1 => "one"` | Pattern + optional `when` + result |

## LINQ Query Expressions

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `QueryExpressionSyntax` | `Linq.QueryExpression` | `from x in list select x` | No Java LINQ |
| `QueryBodySyntax` | `Linq.QueryBody` | *(clauses + select/group)* | |
| `FromClauseSyntax` | `Linq.FromClause` | `from int x in list` | |
| `LetClauseSyntax` | `Linq.LetClause` | `let y = x * 2` | |
| `JoinClauseSyntax` | `Linq.JoinClause` | `join b in listB on a.Id equals b.Id` | |
| `JoinIntoClauseSyntax` | `Linq.JoinIntoClause` | `into g` *(in join...into)* | |
| `WhereClauseSyntax` | `Linq.WhereClause` | `where x > 0` | |
| `OrderByClauseSyntax` | `Linq.OrderByClause` | `orderby x.Name, x.Age descending` | |
| `OrderingSyntax` | `Linq.Ordering` | `x.Name ascending` | |
| `SelectClauseSyntax` | `Linq.SelectClause` | `select x` | |
| `GroupClauseSyntax` | `Linq.GroupClause` | `group x by x.Category` | |
| `QueryContinuationSyntax` | `Linq.QueryContinuation` | `into g` *(after select/group)* | |

## Preprocessor Directives

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `#if`/`#elif`/`#else`/`#endif` | `Cs.ConditionalDirective` | `#if DEBUG`...`#endif` | Multi-branch compilation; wraps multiple parsed CompilationUnits |
| `#region` | `Cs.RegionDirective` | `#region Public Methods` | |
| `#endregion` | `Cs.EndRegionDirective` | `#endregion` | |
| `#pragma warning` | `Cs.PragmaWarningDirective` | `#pragma warning disable CS0168` | |
| `#nullable` | `Cs.NullableDirective` | `#nullable enable` | |
| `#define` | `Cs.DefineDirective` | `#define DEBUG` | |
| `#undef` | `Cs.UndefDirective` | `#undef DEBUG` | |
| `#error` | `Cs.ErrorDirective` | `#error "Stop compilation"` | |
| `#warning` | `Cs.WarningDirective` | `#warning "Check this"` | |
| `#line` | `Cs.LineDirective` | `#line 100 "file.cs"` | |

## Type Parameter Constraints

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `TypeParameterConstraintClauseSyntax` | *(folded into `Cs.ConstrainedTypeParameter`)* | `where T : class, new()` | |
| `ClassOrStructConstraintSyntax` | `Cs.ClassOrStructConstraint` | `class` / `struct` *(in `where T : class`)* | No Java generic constraints |
| `ConstructorConstraintSyntax` | `Cs.ConstructorConstraint` | `new()` *(in `where T : new()`)* | No Java `new()` constraint |
| `DefaultConstraintSyntax` | `Cs.DefaultConstraint` | `default` *(in `where T : default`)* | C# 11 default constraint |
| `AllowsConstraintClauseSyntax` | `Cs.AllowsConstraintClause` | `allows ref struct` *(C# 13)* | Anti-constraint clause |
| `RefStructConstraint` | `Cs.RefStructConstraint` | `ref struct` *(in `allows ref struct`)* | C# 13 ref struct constraint |

## Variable Designations

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `SingleVariableDesignationSyntax` | `Cs.SingleVariableDesignation` | `x` *(in `out var x`)* | Named variable in deconstruction/out |
| `ParenthesizedVariableDesignationSyntax` | `Cs.ParenthesizedVariableDesignation` | `(x, y)` *(in `var (x, y) = ...`)* | Tuple deconstruction target |
| `DiscardDesignationSyntax` | `Cs.DiscardVariableDesignation` | `_` *(in `out var _`)* | Discard in deconstruction/out |

## Helper / Wrapper Types

| Roslyn Syntax | LST Type | Example | Cs Justification |
|---|---|---|---|
| `ArgumentSyntax` (named) | `Cs.NamedExpression` | `name: "foo"` | `name:` colon syntax for named args |
| `ArgumentSyntax` (ref/out/in) | `Cs.RefExpression` | `ref x`, `out var y` | |
| `AnnotatedStatement` | `Cs.AnnotatedStatement` | `[Conditional("DEBUG")] void M(){}` | Attributes on statements |
| `Cs.Keyword` | `Cs.Keyword` | `ref`, `out`, `await`, `base`, etc. | Typed keyword wrapper |
| `Cs.NameColon` | `Cs.NameColon` | `name:` *(in named arguments)* | Colon-separated name syntax |
| `Cs.StatementExpression` | `Cs.StatementExpression` | *(wraps statement as expression)* | Bridge between Statement and Expression |
| `Cs.NullSafeExpression` | `Cs.NullSafeExpression` | `obj!` | Null-forgiving operator wrapper |

## Markers

Markers augment J types with C#-specific semantics without requiring custom Cs types.

| Marker | Applied To | Purpose | Example |
|---|---|---|---|
| `Struct` | `J.ClassDeclaration` | Distinguishes struct from class | `struct Point { }` |
| `RecordClass` | `J.ClassDeclaration` | Preserves explicit `record class` keyword | `record class Person(...)` |
| `PrimaryConstructor` | `J.MethodDeclaration` | Synthesized primary constructor in class body | `class Foo(int x /*~~>*/) { }` |
| `ExpressionBodied` | `J.Block` | Expression body (`=>`) instead of block body | `int Get() => /*~~>*/42;` |
| `NullSafe` | `J.FieldAccess`, `J.MethodInvocation`, `J.ArrayDimension` | Null-conditional access (`?.`, `?[]`) | `obj?.Prop`, `arr?[0]` |
| `NullCoalescing` | `J.Ternary` | Null-coalescing (`??`) reusing ternary shape | `a /*~~>*/?? b` |
| `DelegateInvocation` | `J.MethodInvocation` | Delegate call without `.Invoke()` | `action /*~~>*/()` |
| `AnonymousMethod` | `J.Lambda` | Legacy `delegate` anonymous method syntax | `delegate(int x) { }` |
| `MultiDimensionalArray` | `J.ArrayAccess` | Multi-dimensional indexing (`[i, j]`) | `matrix[i /*~~>*/, j]` |
| `OmitParentheses` | `J.NewClass` | Target-typed `new()` without explicit type | `Foo f = new /*~~>*/();` |
| `Implicit` | `J.Identifier` | Synthesized element (not printed) | Primary constructor name |
| `SingleExpressionBlock` | `J.Block` | Block wrapping a single expression | |
| `OmitBraces` | `J.Block` | Block without braces | |
| `Semicolon` | various | Trailing semicolon | |
| `ConditionalBranchMarker` | `Cs.CompilationUnit` | Identifies branch in `#if` compilation | `#if DEBUG` branch |
| `DirectiveBoundaryMarker` | `Space` | Marks where directive boundaries were in source | |

## Could-Use-J Analysis

The following Cs types were evaluated for potential mapping to J types:

| Cs Type | Could Use J? | Analysis |
|---|---|---|
| `Cs.MethodDeclaration` | **Partially** | J.MethodDeclaration exists but lacks explicit interface specifier (`IFoo.Method()`), expression body support, and C# modifier keywords. Would need extensive marker augmentation. |
| `Cs.EnumDeclaration` | **No** | C# enums support `: byte` base types and attribute lists per declaration. J enum model doesn't support this. |
| `Cs.Try` | **Partially** | J.Try exists but C# catch clauses have `when` filter expressions. Could add a marker, but the filter is a structural addition to the catch clause. |
| `Cs.ExpressionStatement` | **Possibly** | J doesn't have a standalone ExpressionStatement. Expressions become statements via wrapping in J, but C# needs explicit semicolon tracking. |
| `Cs.NewClass` | **Partially** | Wraps J.NewClass to add object/collection initializer support. The initializer is a significant structural addition. |
| `Cs.AssignmentOperation` | **Partially** | J.AssignmentOperation exists but C# adds `??=` (null-coalescing assignment). Could extend J's operator enum instead. |
| `Cs.Binary` | **Partially** | Only used for `as` operator. Could potentially add `As` to J.Binary's operator enum, but `as` is semantically different from Java casts. |
| `Cs.Yield` | **No** | Java has no `yield return`/`yield break`. Entirely C#-specific iterator syntax. |
| `Cs.AwaitExpression` | **No** | Java has no `await` keyword. C#'s async/await is language-level. |
| `Cs.UsingStatement` | **No** | Java's try-with-resources is structurally different (`try (var x = ...) {}`). C# `using` also supports `await using`. |
| `Cs.PropertyDeclaration` | **No** | No Java properties. Entirely C#-specific with get/set/init accessors + expression body + initializer. |
| `Cs.IndexerDeclaration` | **No** | No Java indexers. `this[int i]` syntax is unique to C#. |
| `Cs.SwitchExpression` | **No** | C# switch expressions with pattern arms are structurally different from Java switch expressions. |
| `Cs.InterpolatedString` | **No** | Java has no `$"..."` string interpolation at the language level. |
| All LINQ types | **No** | LINQ query syntax is entirely C#-specific. |
| All pattern types | **Mostly No** | C# pattern matching (relational, property, list, slice) has no Java equivalent. `BinaryPattern` and `UnaryPattern` cleverly reuse J.Binary/J.Unary. |
| All preprocessor types | **No** | Java has no preprocessor directives. |
