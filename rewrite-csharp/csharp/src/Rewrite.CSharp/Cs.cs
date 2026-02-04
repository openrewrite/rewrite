using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

/// <summary>
/// The base interface for C#-specific LST elements.
/// Most C# syntax maps to J elements; Cs is for C#-specific constructs.
/// </summary>
public interface Cs : J
{
}

/// <summary>
/// A C# using directive.
/// Examples:
///   using System;
///   global using System.Collections.Generic;
///   using static System.Math;
///   using Sys = System;
/// </summary>
public sealed record UsingDirective(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<bool> Global,    // 'global' keyword present, with space after
    JLeftPadded<bool> Static,     // 'static' keyword present, with space before
    JRightPadded<Identifier>? Alias, // alias name with space after (before '=')
    TypeTree NamespaceOrType
) : Cs, Statement
{
    /// <summary>
    /// Whether this is a 'global using' directive.
    /// </summary>
    public bool IsGlobal => Global.Element;

    /// <summary>
    /// Whether this is a 'using static' directive.
    /// </summary>
    public bool IsStatic => Static.Element;

    public UsingDirective WithId(Guid id) => this with { Id = id };
    public UsingDirective WithPrefix(Space prefix) => this with { Prefix = prefix };
    public UsingDirective WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# property declaration.
/// Examples:
///   public int X { get; set; }
///   public string Name { get; }
///   public int X => 42;
/// </summary>
public sealed record PropertyDeclaration(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Modifier> Modifiers,
    TypeTree TypeExpression,
    Identifier Name,
    Block? Accessors,                        // null when expression body is used
    JLeftPadded<Expression>? ExpressionBody  // for => expr;
) : Cs, Statement
{
    public PropertyDeclaration WithId(Guid id) => this with { Id = id };
    public PropertyDeclaration WithPrefix(Space prefix) => this with { Prefix = prefix };
    public PropertyDeclaration WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The kind of accessor (get, set, init, add, remove).
/// </summary>
public enum AccessorKind
{
    Get,
    Set,
    Init,
    Add,
    Remove
}

/// <summary>
/// A C# accessor declaration (get, set, init) within a property.
/// Examples:
///   get;
///   set;
///   get { return _value; }
///   private set { _value = value; }
///   get => _x;
/// </summary>
public sealed record AccessorDeclaration(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Modifier> Modifiers,
    JLeftPadded<AccessorKind> Kind,          // The get/set/init keyword with space before
    Block? Body,                              // null for auto-implemented or expression body
    JLeftPadded<Expression>? ExpressionBody  // for => expr
) : Cs, Statement
{
    public AccessorDeclaration WithId(Guid id) => this with { Id = id };
    public AccessorDeclaration WithPrefix(Space prefix) => this with { Prefix = prefix };
    public AccessorDeclaration WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# attribute list, e.g., [Serializable], [Obsolete("message")], [assembly: AssemblyVersion("1.0")]
/// </summary>
public sealed record AttributeList(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Identifier>? Target,    // For [assembly:], [return:], etc.
    IList<JRightPadded<Annotation>> Attributes
) : Cs, Statement
{
    public AttributeList WithId(Guid id) => this with { Id = id };
    public AttributeList WithPrefix(Space prefix) => this with { Prefix = prefix };
    public AttributeList WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// Marker indicating delegate invocation uses syntactic sugar (no explicit .Invoke).
/// When present: action() prints as action()
/// When absent: action.Invoke() prints as action.Invoke()
/// </summary>
public sealed record DelegateInvocation(Guid Id) : Marker
{
    public static DelegateInvocation Instance { get; } = new(Guid.Empty);
}

/// <summary>
/// Marker indicating a primary constructor on a class/struct/record.
/// Applied to both the ClassDeclaration and the synthesized MethodDeclaration in the body.
/// Following the Kotlin pattern for primary constructor representation.
/// </summary>
public sealed record PrimaryConstructor(Guid Id) : Marker;

/// <summary>
/// Marker indicating a struct declaration.
/// Applied to ClassDeclaration to distinguish structs from classes.
/// The printer checks for this marker and prints "struct" instead of "class".
/// For record structs, both KindType.Record and Struct marker are used.
/// </summary>
public sealed record Struct(Guid Id) : Marker;

/// <summary>
/// Marker indicating "record class" syntax was used (vs. just "record").
/// Applied to ClassDeclaration to preserve the explicit "class" keyword.
/// The printer checks for this marker and prints "record class" instead of "record".
/// </summary>
public sealed record RecordClass(Guid Id) : Marker;

/// <summary>
/// A C# type parameter constraint. Wraps a single constraint type in TypeParameter.Bounds.
/// The first bound in a constraint clause has Name/BeforeColon set (for "where T :").
/// Subsequent bounds in the same clause just wrap their constraint type.
/// Examples:
///   where T : class              → TypeParameterBound with Name=T, Bound=class
///   where T : class, IDisposable → First has Name=T, Bound=class; Second has Bound=IDisposable
/// </summary>
public sealed record TypeParameterBound(
    Guid Id,
    Space Prefix,           // First: space before "where". Others: space after ","
    Markers Markers,
    TypeTree? Name,         // First only: the T in "where T :" (with Prefix = space after "where")
    Space? BeforeColon,     // First only: space before ":"
    TypeTree Bound          // The constraint type (class, IDisposable, new(), etc.)
) : Cs, TypeTree
{
    public TypeParameterBound WithId(Guid id) => this with { Id = id };
    public TypeParameterBound WithPrefix(Space prefix) => this with { Prefix = prefix };
    public TypeParameterBound WithMarkers(Markers markers) => this with { Markers = markers };
    public TypeParameterBound WithName(TypeTree? name) => this with { Name = name };
    public TypeParameterBound WithBeforeColon(Space? beforeColon) => this with { BeforeColon = beforeColon };
    public TypeParameterBound WithBound(TypeTree bound) => this with { Bound = bound };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// Marker indicating an implicit/synthesized element that should not be printed directly.
/// Used on the method name identifier of a primary constructor.
/// </summary>
public sealed record Implicit(Guid Id) : Marker;

/// <summary>
/// Marker indicating a null-coalescing expression (??).
/// Applied to J.Ternary to indicate it represents a ?? b instead of a ? b : c.
/// </summary>
public sealed record NullCoalescing(Guid Id) : Marker
{
    public static NullCoalescing Instance { get; } = new(Guid.Empty);
}

/// <summary>
/// Marker indicating a multi-dimensional array access.
/// Applied to J.ArrayAccess for matrix[i, j] style indexing where this access
/// represents an intermediate dimension (not the innermost).
/// </summary>
public sealed record MultiDimensionalArray(Guid Id) : Marker
{
    public static MultiDimensionalArray Instance { get; } = new(Guid.Empty);
}

/// <summary>
/// A named expression: Name: Expression
/// Used for named arguments and property pattern elements.
/// Examples:
///   name: "foo"       (named argument)
///   Length: > 5       (property pattern)
/// </summary>
public sealed record NamedExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Identifier> Name,  // The name with After = space before colon
    Expression Expression           // The value/pattern with its Prefix = space after colon
) : Cs, Expression
{
    public NamedExpression WithId(Guid id) => this with { Id = id };
    public NamedExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public NamedExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public NamedExpression WithName(JRightPadded<Identifier> name) => this with { Name = name };
    public NamedExpression WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The kind of ref parameter modifier (out, ref, in).
/// </summary>
public enum RefKind
{
    Out,
    Ref,
    In
}

/// <summary>
/// A ref/out/in argument modifier in a method invocation.
/// Examples:
///   ref x
///   out result
///   out var x
///   in readOnlyValue
/// </summary>
public sealed record RefExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    RefKind Kind,           // The modifier keyword (out/ref/in)
    Expression Expression   // The target - its Prefix holds space after keyword
) : Cs, Expression
{
    public RefExpression WithId(Guid id) => this with { Id = id };
    public RefExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public RefExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public RefExpression WithKind(RefKind kind) => this with { Kind = kind };
    public RefExpression WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An inline variable declaration in an expression context.
/// Examples:
///   out var x      (within RefExpression)
///   out int result (within RefExpression)
/// </summary>
public sealed record DeclarationExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    TypeTree Type,                    // "var" or explicit type like "int"
    JLeftPadded<Identifier> Variable  // The variable name with space before
) : Cs, Expression
{
    public DeclarationExpression WithId(Guid id) => this with { Id = id };
    public DeclarationExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public DeclarationExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public DeclarationExpression WithType(TypeTree type) => this with { Type = type };
    public DeclarationExpression WithVariable(JLeftPadded<Identifier> variable) => this with { Variable = variable };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# lambda expression with optional modifiers and return type.
/// Wraps J.Lambda to add C#-specific features not present in Java lambdas.
/// Examples:
///   async x => await x
///   static x => x * 2
///   async static () => { await Task.Delay(1); }
///   int (x) => x * 2  (explicit return type)
/// </summary>
public sealed record CsLambda(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Modifier> Modifiers,        // async, static
    TypeTree? ReturnType,              // explicit return type annotation (C# 10+)
    Lambda LambdaExpression            // the underlying J.Lambda
) : Cs, Expression, Statement
{
    public CsLambda WithId(Guid id) => this with { Id = id };
    public CsLambda WithPrefix(Space prefix) => this with { Prefix = prefix };
    public CsLambda WithMarkers(Markers markers) => this with { Markers = markers };
    public CsLambda WithModifiers(IList<Modifier> modifiers) => this with { Modifiers = modifiers };
    public CsLambda WithReturnType(TypeTree? returnType) => this with { ReturnType = returnType };
    public CsLambda WithLambdaExpression(Lambda lambdaExpression) => this with { LambdaExpression = lambdaExpression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# relational pattern that matches values using comparison operators.
/// Used in switch statements/expressions and is expressions.
/// Examples:
///   case > 5:
///   case >= 10:
///   case &lt; 0:
///   case &lt;= -5:
/// </summary>
public sealed record RelationalPattern(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JLeftPadded<RelationalPattern.Type> Operator,  // The comparison operator with space before
    Expression Value                                // The value to compare against
) : Cs, Expression
{
    public enum Type
    {
        LessThan,
        LessThanOrEqual,
        GreaterThan,
        GreaterThanOrEqual
    }

    public RelationalPattern WithId(Guid id) => this with { Id = id };
    public RelationalPattern WithPrefix(Space prefix) => this with { Prefix = prefix };
    public RelationalPattern WithMarkers(Markers markers) => this with { Markers = markers };
    public RelationalPattern WithOperator(JLeftPadded<Type> op) => this with { Operator = op };
    public RelationalPattern WithValue(Expression value) => this with { Value = value };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# property pattern that matches object properties.
/// Examples:
///   { Length: > 5 }                  // Property pattern
///   string { Length: > 5 }           // Type + property pattern
///   { Length: > 5, Count: 0 }        // Multiple properties
///   { Length: var len }              // Property with variable binding
/// </summary>
public sealed record PropertyPattern(
    Guid Id,
    Space Prefix,
    Markers Markers,
    TypeTree? TypeQualifier,                   // Optional type (e.g., "string" in "string { Length: > 5 }")
    JContainer<NamedExpression> Subpatterns    // The { Name: Pattern, ... } - Before = space before {
) : Cs, Expression
{
    public PropertyPattern WithId(Guid id) => this with { Id = id };
    public PropertyPattern WithPrefix(Space prefix) => this with { Prefix = prefix };
    public PropertyPattern WithMarkers(Markers markers) => this with { Markers = markers };
    public PropertyPattern WithTypeQualifier(TypeTree? typeQualifier) => this with { TypeQualifier = typeQualifier };
    public PropertyPattern WithSubpatterns(JContainer<NamedExpression> subpatterns) => this with { Subpatterns = subpatterns };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# compilation unit (source file).
/// </summary>
public sealed record CompilationUnit(
    Guid Id,
    Space Prefix,
    Markers Markers,
    string SourcePath,
    IList<Statement> Members,
    Space Eof
) : Cs, SourceFile
{
    public CompilationUnit WithId(Guid id) => this with { Id = id };
    public CompilationUnit WithPrefix(Space prefix) => this with { Prefix = prefix };
    public CompilationUnit WithMarkers(Markers markers) => this with { Markers = markers };
    public CompilationUnit WithSourcePath(string sourcePath) => this with { SourcePath = sourcePath };
    public CompilationUnit WithMembers(IList<Statement> members) => this with { Members = members };
    public CompilationUnit WithEof(Space eof) => this with { Eof = eof };

    Tree Tree.WithId(Guid id) => WithId(id);
    SourceFile SourceFile.WithSourcePath(string sourcePath) => WithSourcePath(sourcePath);
}

/// <summary>
/// A C# interpolated string expression.
/// Examples:
///   $"Hello {name}"
///   $"Value: {value:F2}"
///   $@"Path: {path}\file"
///   @$"Path: {path}\file"
/// </summary>
public sealed record InterpolatedString(
    Guid Id,
    Space Prefix,
    Markers Markers,
    string Delimiter,      // Opening delimiter: $", $@", @$", $""", etc.
    IList<J> Parts         // Mix of J.Literal (text) and Cs.Interpolation
) : Cs, Expression, Statement
{
    public InterpolatedString WithId(Guid id) => this with { Id = id };
    public InterpolatedString WithPrefix(Space prefix) => this with { Prefix = prefix };
    public InterpolatedString WithMarkers(Markers markers) => this with { Markers = markers };
    public InterpolatedString WithDelimiter(string delimiter) => this with { Delimiter = delimiter };
    public InterpolatedString WithParts(IList<J> parts) => this with { Parts = parts };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An interpolation element inside an interpolated string.
/// Examples:
///   {name}              - basic
///   {value:F2}          - with format specifier
///   {value,10}          - with alignment
///   {value,10:F2}       - with alignment and format
/// </summary>
public sealed record Interpolation(
    Guid Id,
    Space Prefix,                            // Space after opening brace
    Markers Markers,
    Expression Expression,                   // The interpolated expression
    JLeftPadded<Expression>? Alignment,      // Optional alignment (after comma)
    JLeftPadded<Identifier>? Format,         // Optional format specifier (after colon)
    Space After                              // Space before closing brace
) : Cs, J
{
    public Interpolation WithId(Guid id) => this with { Id = id };
    public Interpolation WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Interpolation WithMarkers(Markers markers) => this with { Markers = markers };
    public Interpolation WithExpression(Expression expression) => this with { Expression = expression };
    public Interpolation WithAlignment(JLeftPadded<Expression>? alignment) => this with { Alignment = alignment };
    public Interpolation WithFormat(JLeftPadded<Identifier>? format) => this with { Format = format };
    public Interpolation WithAfter(Space after) => this with { After = after };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# await expression.
/// Examples:
///   await task
///   await SomeMethodAsync()
///   await Task.Delay(1000)
/// </summary>
public sealed record AwaitExpression(
    Guid Id,
    Space Prefix,           // Space before "await"
    Markers Markers,
    Expression Expression   // The awaited expression (Prefix = space after "await")
) : Cs, Expression, Statement
{
    public AwaitExpression WithId(Guid id) => this with { Id = id };
    public AwaitExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public AwaitExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public AwaitExpression WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The kind of yield statement (return or break).
/// </summary>
public enum YieldStatementKind
{
    Return,  // yield return expr;
    Break    // yield break;
}

/// <summary>
/// A C# yield statement.
/// Examples:
///   yield return value;
///   yield break;
/// </summary>
public sealed record YieldStatement(
    Guid Id,
    Space Prefix,                                 // Space before "yield"
    Markers Markers,
    JLeftPadded<YieldStatementKind> ReturnOrBreakKeyword,  // Space before "return"/"break", contains kind
    Expression? Value                             // Optional for yield return (Prefix = space after keyword)
) : Cs, Statement
{
    public YieldStatement WithId(Guid id) => this with { Id = id };
    public YieldStatement WithPrefix(Space prefix) => this with { Prefix = prefix };
    public YieldStatement WithMarkers(Markers markers) => this with { Markers = markers };
    public YieldStatement WithReturnOrBreakKeyword(JLeftPadded<YieldStatementKind> keyword) => this with { ReturnOrBreakKeyword = keyword };
    public YieldStatement WithValue(Expression? value) => this with { Value = value };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# block-scoped namespace declaration.
/// Example:
///   namespace MyApp.Services
///   {
///       class Foo { }
///   }
/// </summary>
public sealed record NamespaceDeclaration(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Expression> Name,           // The namespace name, After = space before {
    IList<JRightPadded<Statement>> Members,  // Types, nested namespaces, etc.
    Space End                                 // Space before closing }
) : Cs, Statement
{
    public NamespaceDeclaration WithId(Guid id) => this with { Id = id };
    public NamespaceDeclaration WithPrefix(Space prefix) => this with { Prefix = prefix };
    public NamespaceDeclaration WithMarkers(Markers markers) => this with { Markers = markers };
    public NamespaceDeclaration WithName(JRightPadded<Expression> name) => this with { Name = name };
    public NamespaceDeclaration WithMembers(IList<JRightPadded<Statement>> members) => this with { Members = members };
    public NamespaceDeclaration WithEnd(Space end) => this with { End = end };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# tuple type specification.
/// Examples:
///   (int, string) coordinates;
///   (int x, string label) namedTuple;
///   public (string name, int age) GetPersonDetails() { }
/// Each element is a VariableDeclarations (same as method parameters).
/// For unnamed elements like (int, string), the NamedVariable has an empty name.
/// </summary>
public sealed record TupleType(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JContainer<VariableDeclarations> Elements  // Before = space after (, elements padded with commas
) : Cs, TypeTree, Expression
{
    public TupleType WithId(Guid id) => this with { Id = id };
    public TupleType WithPrefix(Space prefix) => this with { Prefix = prefix };
    public TupleType WithMarkers(Markers markers) => this with { Markers = markers };
    public TupleType WithElements(JContainer<VariableDeclarations> elements) => this with { Elements = elements };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A C# tuple expression (literal or deconstruction target).
/// Examples:
///   (1, 2)                          // tuple literal
///   (name: "John", age: 25)         // named elements use NamedExpression
///   (var x, var y) = tuple;         // deconstruction pattern
/// </summary>
public sealed record TupleExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JContainer<Expression> Arguments  // Before = space after (, elements are Expression or NamedExpression
) : Cs, Expression
{
    public TupleExpression WithId(Guid id) => this with { Id = id };
    public TupleExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public TupleExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public TupleExpression WithArguments(JContainer<Expression> arguments) => this with { Arguments = arguments };

    Tree Tree.WithId(Guid id) => WithId(id);
}
