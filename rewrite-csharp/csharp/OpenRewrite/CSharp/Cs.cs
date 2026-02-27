using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// The base interface for C#-specific LST elements.
/// Most C# syntax maps to J elements; Cs is for C#-specific constructs.
/// </summary>
public interface Cs : J
{
}

public interface Pattern : Expression, Cs { }
public interface VariableDesignation : Expression, Cs { }
public interface SwitchLabel : Expression { }
// AllowsConstraint and TypeParameterConstraint interfaces DELETED — constraint types implement Expression directly

/// <summary>
/// A C# using directive.
/// Examples:
///   using System;
///   global using System.Collections.Generic;
///   using static System.Math;
///   using Sys = System;
/// </summary>
public sealed class UsingDirective(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<bool> global,
    JLeftPadded<bool> @static,
    JRightPadded<Identifier>? alias,
    TypeTree namespaceOrType
) : Cs, Statement, IEquatable<UsingDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<bool> Global { get; } = global;
    public JLeftPadded<bool> Static { get; } = @static;
    public JRightPadded<Identifier>? Alias { get; } = alias;
    public TypeTree NamespaceOrType { get; } = namespaceOrType;

    public UsingDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Global, Static, Alias, NamespaceOrType);
    public UsingDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Global, Static, Alias, NamespaceOrType);
    public UsingDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Global, Static, Alias, NamespaceOrType);
    public UsingDirective WithGlobal(JRightPadded<bool> global) =>
        ReferenceEquals(global, Global) ? this : new(Id, Prefix, Markers, global, Static, Alias, NamespaceOrType);
    public UsingDirective WithStatic(JLeftPadded<bool> @static) =>
        ReferenceEquals(@static, Static) ? this : new(Id, Prefix, Markers, Global, @static, Alias, NamespaceOrType);
    public UsingDirective WithAlias(JRightPadded<Identifier>? alias) =>
        ReferenceEquals(alias, Alias) ? this : new(Id, Prefix, Markers, Global, Static, alias, NamespaceOrType);
    public UsingDirective WithNamespaceOrType(TypeTree namespaceOrType) =>
        ReferenceEquals(namespaceOrType, NamespaceOrType) ? this : new(Id, Prefix, Markers, Global, Static, Alias, namespaceOrType);

    /// <summary>
    /// Whether this is a 'global using' directive.
    /// </summary>
    public bool IsGlobal => Global.Element;

    /// <summary>
    /// Whether this is a 'using static' directive.
    /// </summary>
    public bool IsStatic => Static.Element;


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(UsingDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as UsingDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# property declaration.
/// Examples:
///   public int X { get; set; }
///   public string Name { get; }
///   public int X => 42;
/// </summary>
public sealed class PropertyDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Modifier> modifiers,
    TypeTree typeExpression,
    Identifier name,
    Block? accessors,
    JLeftPadded<Expression>? expressionBody
) : Cs, Statement, IEquatable<PropertyDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public TypeTree TypeExpression { get; } = typeExpression;
    public Identifier Name { get; } = name;
    public Block? Accessors { get; } = accessors;
    public JLeftPadded<Expression>? ExpressionBody { get; } = expressionBody;

    public PropertyDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Modifiers, TypeExpression, Name, Accessors, ExpressionBody);
    public PropertyDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Modifiers, TypeExpression, Name, Accessors, ExpressionBody);
    public PropertyDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Modifiers, TypeExpression, Name, Accessors, ExpressionBody);
    public PropertyDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, modifiers, TypeExpression, Name, Accessors, ExpressionBody);
    public PropertyDeclaration WithTypeExpression(TypeTree typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, Modifiers, typeExpression, Name, Accessors, ExpressionBody);
    public PropertyDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, name, Accessors, ExpressionBody);
    public PropertyDeclaration WithAccessors(Block? accessors) =>
        ReferenceEquals(accessors, Accessors) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, Name, accessors, ExpressionBody);
    public PropertyDeclaration WithExpressionBody(JLeftPadded<Expression>? expressionBody) =>
        ReferenceEquals(expressionBody, ExpressionBody) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, Name, Accessors, expressionBody);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PropertyDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PropertyDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
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
public sealed class AccessorDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Modifier> modifiers,
    JLeftPadded<AccessorKind> kind,
    Block? body,
    JLeftPadded<Expression>? expressionBody
) : Cs, Statement, IEquatable<AccessorDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JLeftPadded<AccessorKind> Kind { get; } = kind;
    public Block? Body { get; } = body;
    public JLeftPadded<Expression>? ExpressionBody { get; } = expressionBody;

    public AccessorDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Modifiers, Kind, Body, ExpressionBody);
    public AccessorDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Modifiers, Kind, Body, ExpressionBody);
    public AccessorDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Modifiers, Kind, Body, ExpressionBody);
    public AccessorDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, modifiers, Kind, Body, ExpressionBody);
    public AccessorDeclaration WithKind(JLeftPadded<AccessorKind> kind) =>
        ReferenceEquals(kind, Kind) ? this : new(Id, Prefix, Markers, Modifiers, kind, Body, ExpressionBody);
    public AccessorDeclaration WithBody(Block? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Modifiers, Kind, body, ExpressionBody);
    public AccessorDeclaration WithExpressionBody(JLeftPadded<Expression>? expressionBody) =>
        ReferenceEquals(expressionBody, ExpressionBody) ? this : new(Id, Prefix, Markers, Modifiers, Kind, Body, expressionBody);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AccessorDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AccessorDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# attribute list, e.g., [Serializable], [Obsolete("message")], [assembly: AssemblyVersion("1.0")]
/// </summary>
public sealed class AttributeList(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier>? target,
    IList<JRightPadded<Annotation>> attributes
) : Cs, Statement, IEquatable<AttributeList>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier>? Target { get; } = target;
    public IList<JRightPadded<Annotation>> Attributes { get; } = attributes;

    public AttributeList WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Target, Attributes);
    public AttributeList WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Target, Attributes);
    public AttributeList WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Target, Attributes);
    public AttributeList WithTarget(JRightPadded<Identifier>? target) =>
        ReferenceEquals(target, Target) ? this : new(Id, Prefix, Markers, target, Attributes);
    public AttributeList WithAttributes(IList<JRightPadded<Annotation>> attributes) =>
        ReferenceEquals(attributes, Attributes) ? this : new(Id, Prefix, Markers, Target, attributes);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AttributeList? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AttributeList);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating delegate invocation uses syntactic sugar (no explicit .Invoke).
/// When present: action() prints as action()
/// When absent: action.Invoke() prints as action.Invoke()
/// </summary>
public sealed class DelegateInvocation(
    Guid id
) : Marker, IRpcCodec<DelegateInvocation>, IEquatable<DelegateInvocation>
{
    public Guid Id { get; } = id;

    public DelegateInvocation WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(DelegateInvocation after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public DelegateInvocation RpcReceive(DelegateInvocation before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public static DelegateInvocation Instance { get; } = new(Guid.Empty);

    public bool Equals(DelegateInvocation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DelegateInvocation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating a primary constructor on a class/struct/record.
/// Applied to both the ClassDeclaration and the synthesized MethodDeclaration in the body.
/// Following the Kotlin pattern for primary constructor representation.
/// </summary>
public sealed class PrimaryConstructor(
    Guid id
) : Marker, IRpcCodec<PrimaryConstructor>, IEquatable<PrimaryConstructor>
{
    public Guid Id { get; } = id;

    public PrimaryConstructor WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(PrimaryConstructor after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public PrimaryConstructor RpcReceive(PrimaryConstructor before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(PrimaryConstructor? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PrimaryConstructor);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating a struct declaration.
/// Applied to ClassDeclaration to distinguish structs from classes.
/// The printer checks for this marker and prints "struct" instead of "class".
/// For record structs, both KindType.Record and Struct marker are used.
/// </summary>
public sealed class Struct(
    Guid id
) : Marker, IRpcCodec<Struct>, IEquatable<Struct>
{
    public Guid Id { get; } = id;

    public Struct WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(Struct after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public Struct RpcReceive(Struct before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(Struct? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Struct);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating "record class" syntax was used (vs. just "record").
/// Applied to ClassDeclaration to preserve the explicit "class" keyword.
/// The printer checks for this marker and prints "record class" instead of "record".
/// </summary>
public sealed class RecordClass(
    Guid id
) : Marker, IRpcCodec<RecordClass>, IEquatable<RecordClass>
{
    public Guid Id { get; } = id;

    public RecordClass WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(RecordClass after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public RecordClass RpcReceive(RecordClass before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(RecordClass? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RecordClass);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating an expression-bodied method declaration.
/// Expression-bodied methods are syntactic sugar for a single return statement.
/// The body Block contains a single J.Return, and the printer prints "=>" instead of "{ return ...; }".
/// Block.Prefix = space before "=>", Return.Expression.Prefix = space before the expression.
/// </summary>
public sealed class ExpressionBodied(
    Guid id
) : Marker, IRpcCodec<ExpressionBodied>, IEquatable<ExpressionBodied>
{
    public Guid Id { get; } = id;

    public ExpressionBodied WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(ExpressionBodied after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public ExpressionBodied RpcReceive(ExpressionBodied before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(ExpressionBodied? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ExpressionBodied);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class AnonymousMethod(
    Guid id
) : Marker, IRpcCodec<AnonymousMethod>, IEquatable<AnonymousMethod>
{
    public Guid Id { get; } = id;

    public AnonymousMethod WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(AnonymousMethod after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public AnonymousMethod RpcReceive(AnonymousMethod before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(AnonymousMethod? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AnonymousMethod);
    public override int GetHashCode() => Id.GetHashCode();
}

// TypeParameterBound DELETED — replaced by ConstrainedTypeParameter

/// <summary>
/// Marker indicating an implicit/synthesized element that should not be printed directly.
/// Used on the method name identifier of a primary constructor.
/// </summary>
public sealed class Implicit(
    Guid id
) : Marker, IRpcCodec<Implicit>, IEquatable<Implicit>
{
    public Guid Id { get; } = id;

    public Implicit WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(Implicit after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public Implicit RpcReceive(Implicit before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(Implicit? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Implicit);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating a null-coalescing expression (??).
/// Applied to J.Ternary to indicate it represents a ?? b instead of a ? b : c.
/// </summary>
public sealed class NullCoalescing(
    Guid id
) : Marker, IRpcCodec<NullCoalescing>, IEquatable<NullCoalescing>
{
    public Guid Id { get; } = id;

    public NullCoalescing WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(NullCoalescing after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public NullCoalescing RpcReceive(NullCoalescing before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public static NullCoalescing Instance { get; } = new(Guid.Empty);

    public bool Equals(NullCoalescing? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NullCoalescing);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating a multi-dimensional array access.
/// Applied to J.ArrayAccess for matrix[i, j] style indexing where this access
/// represents an intermediate dimension (not the innermost).
/// </summary>
public sealed class MultiDimensionalArray(
    Guid id
) : Marker, IRpcCodec<MultiDimensionalArray>, IEquatable<MultiDimensionalArray>
{
    public Guid Id { get; } = id;

    public MultiDimensionalArray WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(MultiDimensionalArray after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public MultiDimensionalArray RpcReceive(MultiDimensionalArray before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public static MultiDimensionalArray Instance { get; } = new(Guid.Empty);

    public bool Equals(MultiDimensionalArray? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MultiDimensionalArray);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A named expression: Name: Expression
/// Used for named arguments and property pattern elements.
/// Examples:
///   name: "foo"       (named argument)
///   Length: > 5       (property pattern)
/// </summary>
public sealed class NamedExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> name,
    Expression expression
) : Cs, Expression, IEquatable<NamedExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> Name { get; } = name;
    public Expression Expression { get; } = expression;

    public NamedExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name, Expression);
    public NamedExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name, Expression);
    public NamedExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name, Expression);
    public NamedExpression WithName(JRightPadded<Identifier> name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, name, Expression);
    public NamedExpression WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, Name, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NamedExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NamedExpression);
    public override int GetHashCode() => Id.GetHashCode();
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
public sealed class RefExpression(
    Guid id,
    Space prefix,
    Markers markers,
    RefKind kind,
    Expression expression
) : Cs, Expression, IEquatable<RefExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public RefKind Kind { get; } = kind;
    public Expression Expression { get; } = expression;

    public RefExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Kind, Expression);
    public RefExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Kind, Expression);
    public RefExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Kind, Expression);
    public RefExpression WithKind(RefKind kind) =>
        kind == Kind ? this : new(Id, Prefix, Markers, kind, Expression);
    public RefExpression WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, Kind, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RefExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RefExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An inline variable declaration in an expression context.
/// Examples:
///   out var x      (within RefExpression)
///   out int result (within RefExpression)
/// </summary>
public sealed class DeclarationExpression(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree? typeExpression,
    Expression variables
) : Cs, Expression, IEquatable<DeclarationExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree? TypeExpression { get; } = typeExpression;
    public Expression Variables { get; } = variables;

    public DeclarationExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeExpression, Variables);
    public DeclarationExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeExpression, Variables);
    public DeclarationExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeExpression, Variables);
    public DeclarationExpression WithTypeExpression(TypeTree? typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, typeExpression, Variables);
    public DeclarationExpression WithVariables(Expression variables) =>
        ReferenceEquals(variables, Variables) ? this : new(Id, Prefix, Markers, TypeExpression, variables);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DeclarationExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DeclarationExpression);
    public override int GetHashCode() => Id.GetHashCode();
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
public sealed class CsLambda(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    IList<Modifier> modifiers,
    TypeTree? returnType,
    Lambda lambdaExpression
) : Cs, Expression, Statement, IEquatable<CsLambda>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public TypeTree? ReturnType { get; } = returnType;
    public Lambda LambdaExpression { get; } = lambdaExpression;

    public CsLambda WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Modifiers, ReturnType, LambdaExpression);
    public CsLambda WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Modifiers, ReturnType, LambdaExpression);
    public CsLambda WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Modifiers, ReturnType, LambdaExpression);
    public CsLambda WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Modifiers, ReturnType, LambdaExpression);
    public CsLambda WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, AttributeLists, modifiers, ReturnType, LambdaExpression);
    public CsLambda WithReturnType(TypeTree? returnType) =>
        ReferenceEquals(returnType, ReturnType) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, returnType, LambdaExpression);
    public CsLambda WithLambdaExpression(Lambda lambdaExpression) =>
        ReferenceEquals(lambdaExpression, LambdaExpression) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ReturnType, lambdaExpression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsLambda? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsLambda);
    public override int GetHashCode() => Id.GetHashCode();
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
public sealed class RelationalPattern(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<RelationalPattern.Type> @operator,
    Expression value
) : Cs, Pattern, IEquatable<RelationalPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<RelationalPattern.Type> Operator { get; } = @operator;
    public Expression Value { get; } = value;

    public RelationalPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Operator, Value);
    public RelationalPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Operator, Value);
    public RelationalPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Operator, Value);
    public RelationalPattern WithOperator(JLeftPadded<RelationalPattern.Type> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, @operator, Value);
    public RelationalPattern WithValue(Expression value) =>
        ReferenceEquals(value, Value) ? this : new(Id, Prefix, Markers, Operator, value);

    public enum Type
    {
        LessThan,
        LessThanOrEqual,
        GreaterThan,
        GreaterThanOrEqual
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RelationalPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RelationalPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# is-pattern expression that performs pattern matching.
/// The expression tests a value against a pattern using the 'is' keyword.
/// Examples:
///   obj is string s          // Type pattern with declaration
///   obj is int n and > 0     // Binary pattern
///   obj is null              // Constant pattern
/// </summary>
public sealed class IsPattern(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression,
    JLeftPadded<J> pattern
) : Cs, Expression, IEquatable<IsPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;
    public JLeftPadded<J> Pattern { get; } = pattern;

    public IsPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Pattern);
    public IsPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Pattern);
    public IsPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Pattern);
    public IsPattern WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Pattern);
    public IsPattern WithPattern(JLeftPadded<J> pattern) =>
        ReferenceEquals(pattern, Pattern) ? this : new(Id, Prefix, Markers, Expression, pattern);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(IsPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as IsPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Wraps a Statement as an Expression, allowing statements to appear
/// in expression contexts (e.g., VariableDeclarations in pattern matching).
/// </summary>
public sealed class StatementExpression(
    Guid id,
    Space prefix,
    Markers markers,
    Statement statement
) : Pattern, IEquatable<StatementExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Statement Statement { get; } = statement;

    public StatementExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Statement);
    public StatementExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Statement);
    public StatementExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Statement);
    public StatementExpression WithStatement(Statement statement) =>
        ReferenceEquals(statement, Statement) ? this : new(Id, Prefix, Markers, statement);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(StatementExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as StatementExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# sizeof expression, e.g. sizeof(int).
/// </summary>
public sealed class SizeOf(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression,
    JavaType? type
) : Cs, Expression, IEquatable<SizeOf>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;
    public JavaType? Type { get; } = type;

    public SizeOf WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Type);
    public SizeOf WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Type);
    public SizeOf WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Type);
    public SizeOf WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Type);
    public SizeOf WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Expression, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SizeOf? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SizeOf);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# unsafe statement block, e.g. unsafe { int* p = &amp;x; }
/// </summary>
public sealed class UnsafeStatement(
    Guid id,
    Space prefix,
    Markers markers,
    Block block
) : Cs, Statement, IEquatable<UnsafeStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Block Block { get; } = block;

    public UnsafeStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Block);
    public UnsafeStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Block);
    public UnsafeStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Block);
    public UnsafeStatement WithBlock(Block block) =>
        ReferenceEquals(block, Block) ? this : new(Id, Prefix, Markers, block);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(UnsafeStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as UnsafeStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# default expression: default(T) or standalone default.
/// TypeOperator is null for standalone default, contains the type for default(T).
/// </summary>
public sealed class DefaultExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<TypeTree>? typeOperator
) : Cs, Expression, IEquatable<DefaultExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<TypeTree>? TypeOperator { get; } = typeOperator;

    public DefaultExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeOperator);
    public DefaultExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeOperator);
    public DefaultExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeOperator);
    public DefaultExpression WithTypeOperator(JContainer<TypeTree>? typeOperator) =>
        ReferenceEquals(typeOperator, TypeOperator) ? this : new(Id, Prefix, Markers, typeOperator);

    public JavaType? Type => null; // Type is derived from the TypeOperator elements on the Java side

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DefaultExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DefaultExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# pointer type, e.g. int*, byte*.
/// </summary>
public sealed class PointerType(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<TypeTree> elementType
) : Cs, TypeTree, Expression, IEquatable<PointerType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<TypeTree> ElementType { get; } = elementType;

    public PointerType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ElementType);
    public PointerType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ElementType);
    public PointerType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ElementType);
    public PointerType WithElementType(JRightPadded<TypeTree> elementType) =>
        ReferenceEquals(elementType, ElementType) ? this : new(Id, Prefix, Markers, elementType);

    public JavaType? Type => null;

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PointerType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PointerType);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# fixed statement, e.g. fixed (int* p = array) { ... }
/// </summary>
public sealed class FixedStatement(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<VariableDeclarations> declarations,
    Block block
) : Cs, Statement, IEquatable<FixedStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<VariableDeclarations> Declarations { get; } = declarations;
    public Block Block { get; } = block;

    public FixedStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Declarations, Block);
    public FixedStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Declarations, Block);
    public FixedStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Declarations, Block);
    public FixedStatement WithDeclarations(ControlParentheses<VariableDeclarations> declarations) =>
        ReferenceEquals(declarations, Declarations) ? this : new(Id, Prefix, Markers, declarations, Block);
    public FixedStatement WithBlock(Block block) =>
        ReferenceEquals(block, Block) ? this : new(Id, Prefix, Markers, Declarations, block);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(FixedStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as FixedStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# property pattern that matches object properties.
/// Examples:
///   { Length: > 5 }                  // Property pattern
///   string { Length: > 5 }           // Type + property pattern
///   { Length: > 5, Count: 0 }        // Multiple properties
///   { Length: var len }              // Property with variable binding
/// </summary>
public sealed class PropertyPattern(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree? typeQualifier,
    JContainer<Expression> subpatterns,
    Identifier? designation = null
) : Cs, Pattern, IEquatable<PropertyPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree? TypeQualifier { get; } = typeQualifier;
    public JContainer<Expression> Subpatterns { get; } = subpatterns;
    /// <summary>Optional variable designation (e.g., "varBlock" in "is { } varBlock").</summary>
    public Identifier? Designation { get; } = designation;

    public PropertyPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeQualifier, Subpatterns, Designation);
    public PropertyPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeQualifier, Subpatterns, Designation);
    public PropertyPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeQualifier, Subpatterns, Designation);
    public PropertyPattern WithTypeQualifier(TypeTree? typeQualifier) =>
        ReferenceEquals(typeQualifier, TypeQualifier) ? this : new(Id, Prefix, Markers, typeQualifier, Subpatterns, Designation);
    public PropertyPattern WithSubpatterns(JContainer<Expression> subpatterns) =>
        ReferenceEquals(subpatterns, Subpatterns) ? this : new(Id, Prefix, Markers, TypeQualifier, subpatterns, Designation);
    public PropertyPattern WithDesignation(Identifier? designation) =>
        ReferenceEquals(designation, Designation) ? this : new(Id, Prefix, Markers, TypeQualifier, Subpatterns, designation);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PropertyPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PropertyPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// The action for a #pragma warning directive.
/// </summary>
public enum PragmaWarningAction
{
    Disable,
    Restore
}

/// <summary>
/// The setting for a #nullable directive.
/// </summary>
public enum NullableSetting
{
    Enable,
    Disable,
    Restore
}

/// <summary>
/// The optional target for a #nullable directive.
/// </summary>
public enum NullableTarget
{
    Annotations,
    Warnings
}

/// <summary>
/// The kind of #line directive.
/// </summary>
public enum LineKind
{
    Numeric,
    Hidden,
    Default
}

/// <summary>
/// Wraps multiple parsed branches of a file containing #if/#elif/#else/#endif directives.
/// Each branch is a complete CompilationUnit parsed from a clean source (directives stripped).
/// The printer reconstructs the original source using line-level interleaving.
/// </summary>
public sealed class ConditionalDirective(
    Guid id,
    Space prefix,
    Markers markers,
    IList<DirectiveLine> directiveLines,
    IList<JRightPadded<CompilationUnit>> branches
) : Cs, Statement, IEquatable<ConditionalDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<DirectiveLine> DirectiveLines { get; } = directiveLines;
    public IList<JRightPadded<CompilationUnit>> Branches { get; } = branches;

    public ConditionalDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, DirectiveLines, Branches);
    public ConditionalDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, DirectiveLines, Branches);
    public ConditionalDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, DirectiveLines, Branches);
    public ConditionalDirective WithDirectiveLines(IList<DirectiveLine> directiveLines) =>
        ReferenceEquals(directiveLines, DirectiveLines) ? this : new(Id, Prefix, Markers, directiveLines, Branches);
    public ConditionalDirective WithBranches(IList<JRightPadded<CompilationUnit>> branches) =>
        ReferenceEquals(branches, Branches) ? this : new(Id, Prefix, Markers, DirectiveLines, branches);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ConditionalDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConditionalDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Metadata about a single preprocessor directive line in the original source.
/// </summary>
public sealed record DirectiveLine(
    int LineNumber,
    string Text,
    PreprocessorDirectiveKind Kind,
    int GroupId,
    int ActiveBranchIndex
);

public enum PreprocessorDirectiveKind
{
    If,
    Elif,
    Else,
    Endif
}

/// <summary>
/// Marker attached to inner CompilationUnit branches within a ConditionalDirective
/// to distinguish them from top-level source files and record which symbols were defined.
/// </summary>
public sealed class ConditionalBranchMarker(
    Guid id,
    IList<string> definedSymbols
) : Marker, IRpcCodec<ConditionalBranchMarker>, IEquatable<ConditionalBranchMarker>
{
    public Guid Id { get; } = id;
    public IList<string> DefinedSymbols { get; } = definedSymbols;

    public ConditionalBranchMarker WithId(Guid id) =>
        id == Id ? this : new(id, DefinedSymbols);
    public ConditionalBranchMarker WithDefinedSymbols(IList<string> definedSymbols) =>
        ReferenceEquals(definedSymbols, DefinedSymbols) ? this : new(Id, definedSymbols);

    public void RpcSend(ConditionalBranchMarker after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSendList(after, m => m.DefinedSymbols, s => s, s =>
            q.GetAndSend(s, x => x));
    }

    public ConditionalBranchMarker RpcReceive(ConditionalBranchMarker before, RpcReceiveQueue q)
    {
        return before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse)).WithDefinedSymbols(q.ReceiveList(before.DefinedSymbols, s => q.ReceiveAndGet<string, string>(s, x => x)!) ?? []);
    }

    public bool Equals(ConditionalBranchMarker? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConditionalBranchMarker);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marks a tree node adjacent to one or more conditional preprocessor directive boundaries.
/// Each directive index references a position in the <see cref="ConditionalDirective.DirectiveLines"/> list.
/// </summary>
public sealed class DirectiveBoundaryMarker(
    Guid id,
    IList<int> directiveIndices
) : Marker, IRpcCodec<DirectiveBoundaryMarker>, IEquatable<DirectiveBoundaryMarker>
{
    public Guid Id { get; } = id;
    public IList<int> DirectiveIndices { get; } = directiveIndices;

    public DirectiveBoundaryMarker WithId(Guid id) =>
        id == Id ? this : new(id, DirectiveIndices);
    public DirectiveBoundaryMarker WithDirectiveIndices(IList<int> directiveIndices) =>
        ReferenceEquals(directiveIndices, DirectiveIndices) ? this : new(Id, directiveIndices);

    public void RpcSend(DirectiveBoundaryMarker after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSendList(after, m => m.DirectiveIndices, i => i.ToString(), i =>
            q.GetAndSend(i, x => x.ToString()));
    }

    public DirectiveBoundaryMarker RpcReceive(DirectiveBoundaryMarker before, RpcReceiveQueue q)
    {
        return before
            .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
            .WithDirectiveIndices(q.ReceiveList(before.DirectiveIndices, idx =>
                q.ReceiveAndGet<int, string>(idx, int.Parse))!);
    }

    public bool Equals(DirectiveBoundaryMarker? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DirectiveBoundaryMarker);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #pragma warning disable/restore directive.
/// </summary>
public sealed class PragmaWarningDirective(
    Guid id,
    Space prefix,
    Markers markers,
    PragmaWarningAction action,
    IList<JRightPadded<Expression>> warningCodes
) : Cs, Statement, IEquatable<PragmaWarningDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public PragmaWarningAction Action { get; } = action;
    public IList<JRightPadded<Expression>> WarningCodes { get; } = warningCodes;

    public PragmaWarningDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Action, WarningCodes);
    public PragmaWarningDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Action, WarningCodes);
    public PragmaWarningDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Action, WarningCodes);
    public PragmaWarningDirective WithAction(PragmaWarningAction action) =>
        action == Action ? this : new(Id, Prefix, Markers, action, WarningCodes);
    public PragmaWarningDirective WithWarningCodes(IList<JRightPadded<Expression>> warningCodes) =>
        ReferenceEquals(warningCodes, WarningCodes) ? this : new(Id, Prefix, Markers, Action, warningCodes);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PragmaWarningDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PragmaWarningDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #pragma checksum directive.
/// </summary>
public sealed class PragmaChecksumDirective(
    Guid id,
    Space prefix,
    Markers markers,
    string arguments
) : Cs, Statement, IEquatable<PragmaChecksumDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string Arguments { get; } = arguments;

    public PragmaChecksumDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Arguments);
    public PragmaChecksumDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Arguments);
    public PragmaChecksumDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Arguments);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PragmaChecksumDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PragmaChecksumDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #nullable directive.
/// </summary>
public sealed class NullableDirective(
    Guid id,
    Space prefix,
    Markers markers,
    NullableSetting setting,
    NullableTarget? target,
    string hashSpacing = "",
    string trailingComment = ""
) : Cs, Statement, IEquatable<NullableDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public NullableSetting Setting { get; } = setting;
    public NullableTarget? Target { get; } = target;
    public string HashSpacing { get; } = hashSpacing;
    public string TrailingComment { get; } = trailingComment;

    public NullableDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Setting, Target, HashSpacing, TrailingComment);
    public NullableDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Setting, Target, HashSpacing, TrailingComment);
    public NullableDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Setting, Target, HashSpacing, TrailingComment);
    public NullableDirective WithSetting(NullableSetting setting) =>
        setting == Setting ? this : new(Id, Prefix, Markers, setting, Target, HashSpacing, TrailingComment);
    public NullableDirective WithTarget(NullableTarget? target) =>
        target == Target ? this : new(Id, Prefix, Markers, Setting, target, HashSpacing, TrailingComment);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NullableDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NullableDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #region directive.
/// </summary>
public sealed class RegionDirective(
    Guid id,
    Space prefix,
    Markers markers,
    string? name,
    string hashSpacing = ""
) : Cs, Statement, IEquatable<RegionDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string? Name { get; } = name;
    /// <summary>Whitespace between '#' and 'region' (e.g., " " for "# region").</summary>
    public string HashSpacing { get; } = hashSpacing;

    public RegionDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name, HashSpacing);
    public RegionDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name, HashSpacing);
    public RegionDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name, HashSpacing);
    public RegionDirective WithName(string? name) =>
        string.Equals(name, Name, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, name, HashSpacing);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RegionDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RegionDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #endregion directive.
/// </summary>
public sealed class EndRegionDirective(
    Guid id,
    Space prefix,
    Markers markers,
    string? name = null,
    string hashSpacing = ""
) : Cs, Statement, IEquatable<EndRegionDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string? Name { get; } = name;
    /// <summary>Whitespace between '#' and 'endregion' (e.g., " " for "# endregion").</summary>
    public string HashSpacing { get; } = hashSpacing;

    public EndRegionDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name, HashSpacing);
    public EndRegionDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name, HashSpacing);
    public EndRegionDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name, HashSpacing);
    public EndRegionDirective WithName(string? name) =>
        string.Equals(name, Name, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, name, HashSpacing);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EndRegionDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EndRegionDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #define directive.
/// </summary>
public sealed class DefineDirective(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier symbol
) : Cs, Statement, IEquatable<DefineDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Symbol { get; } = symbol;

    public DefineDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Symbol);
    public DefineDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Symbol);
    public DefineDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Symbol);
    public DefineDirective WithSymbol(Identifier symbol) =>
        ReferenceEquals(symbol, Symbol) ? this : new(Id, Prefix, Markers, symbol);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DefineDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DefineDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #undef directive.
/// </summary>
public sealed class UndefDirective(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier symbol
) : Cs, Statement, IEquatable<UndefDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Symbol { get; } = symbol;

    public UndefDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Symbol);
    public UndefDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Symbol);
    public UndefDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Symbol);
    public UndefDirective WithSymbol(Identifier symbol) =>
        ReferenceEquals(symbol, Symbol) ? this : new(Id, Prefix, Markers, symbol);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(UndefDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as UndefDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #error directive.
/// </summary>
public sealed class ErrorDirective(
    Guid id,
    Space prefix,
    Markers markers,
    string message
) : Cs, Statement, IEquatable<ErrorDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string Message { get; } = message;

    public ErrorDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Message);
    public ErrorDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Message);
    public ErrorDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Message);
    public ErrorDirective WithMessage(string message) =>
        string.Equals(message, Message, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, message);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ErrorDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ErrorDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #warning directive.
/// </summary>
public sealed class WarningDirective(
    Guid id,
    Space prefix,
    Markers markers,
    string message
) : Cs, Statement, IEquatable<WarningDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string Message { get; } = message;

    public WarningDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Message);
    public WarningDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Message);
    public WarningDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Message);
    public WarningDirective WithMessage(string message) =>
        string.Equals(message, Message, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, message);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(WarningDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as WarningDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A #line directive.
/// </summary>
public sealed class LineDirective(
    Guid id,
    Space prefix,
    Markers markers,
    LineKind kind,
    Expression? line,
    Expression? file
) : Cs, Statement, IEquatable<LineDirective>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public LineKind Kind { get; } = kind;
    public Expression? Line { get; } = line;
    public Expression? File { get; } = file;

    public LineDirective WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Kind, Line, File);
    public LineDirective WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Kind, Line, File);
    public LineDirective WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Kind, Line, File);
    public LineDirective WithKind(LineKind kind) =>
        kind == Kind ? this : new(Id, Prefix, Markers, kind, Line, File);
    public LineDirective WithLine(Expression? line) =>
        ReferenceEquals(line, Line) ? this : new(Id, Prefix, Markers, Kind, line, File);
    public LineDirective WithFile(Expression? file) =>
        ReferenceEquals(file, File) ? this : new(Id, Prefix, Markers, Kind, Line, file);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(LineDirective? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as LineDirective);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# compilation unit (source file).
/// </summary>
public sealed class CompilationUnit(
    Guid id,
    Space prefix,
    Markers markers,
    string sourcePath,
    IList<Statement> members,
    Space eof
) : Cs, SourceFile, IEquatable<CompilationUnit>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string SourcePath { get; } = sourcePath;
    public IList<Statement> Members { get; } = members;
    public Space Eof { get; } = eof;

    public CompilationUnit WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, SourcePath, Members, Eof);
    public CompilationUnit WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, SourcePath, Members, Eof);
    public CompilationUnit WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, SourcePath, Members, Eof);
    public CompilationUnit WithSourcePath(string sourcePath) =>
        string.Equals(sourcePath, SourcePath, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, sourcePath, Members, Eof);
    public CompilationUnit WithMembers(IList<Statement> members) =>
        ReferenceEquals(members, Members) ? this : new(Id, Prefix, Markers, SourcePath, members, Eof);
    public CompilationUnit WithEof(Space eof) =>
        ReferenceEquals(eof, Eof) ? this : new(Id, Prefix, Markers, SourcePath, Members, eof);

    Tree Tree.WithId(Guid id) => WithId(id);
    SourceFile SourceFile.WithSourcePath(string sourcePath) => WithSourcePath(sourcePath);

    public bool Equals(CompilationUnit? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CompilationUnit);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# interpolated string expression.
/// Examples:
///   $"Hello {name}"
///   $"Value: {value:F2}"
///   $@"Path: {path}\file"
///   @$"Path: {path}\file"
/// </summary>
public sealed class InterpolatedString(
    Guid id,
    Space prefix,
    Markers markers,
    string delimiter,
    string endDelimiter,
    IList<J> parts
) : Cs, Expression, Statement, IEquatable<InterpolatedString>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string Delimiter { get; } = delimiter;
    public string EndDelimiter { get; } = endDelimiter;
    public IList<J> Parts { get; } = parts;

    public InterpolatedString WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Delimiter, EndDelimiter, Parts);
    public InterpolatedString WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Delimiter, EndDelimiter, Parts);
    public InterpolatedString WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Delimiter, EndDelimiter, Parts);
    public InterpolatedString WithDelimiter(string delimiter) =>
        string.Equals(delimiter, Delimiter, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, delimiter, EndDelimiter, Parts);
    public InterpolatedString WithEndDelimiter(string endDelimiter) =>
        string.Equals(endDelimiter, EndDelimiter, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, Delimiter, endDelimiter, Parts);
    public InterpolatedString WithParts(IList<J> parts) =>
        ReferenceEquals(parts, Parts) ? this : new(Id, Prefix, Markers, Delimiter, EndDelimiter, parts);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(InterpolatedString? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as InterpolatedString);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An interpolation element inside an interpolated string.
/// Examples:
///   {name}              - basic
///   {value:F2}          - with format specifier
///   {value,10}          - with alignment
///   {value,10:F2}       - with alignment and format
/// </summary>
public sealed class Interpolation(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression,
    JLeftPadded<Expression>? alignment,
    JLeftPadded<Identifier>? format,
    Space after
) : Cs, J, IEquatable<Interpolation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;
    public JLeftPadded<Expression>? Alignment { get; } = alignment;
    public JLeftPadded<Identifier>? Format { get; } = format;
    public Space After { get; } = after;

    public Interpolation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Alignment, Format, After);
    public Interpolation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Alignment, Format, After);
    public Interpolation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Alignment, Format, After);
    public Interpolation WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Alignment, Format, After);
    public Interpolation WithAlignment(JLeftPadded<Expression>? alignment) =>
        ReferenceEquals(alignment, Alignment) ? this : new(Id, Prefix, Markers, Expression, alignment, Format, After);
    public Interpolation WithFormat(JLeftPadded<Identifier>? format) =>
        ReferenceEquals(format, Format) ? this : new(Id, Prefix, Markers, Expression, Alignment, format, After);
    public Interpolation WithAfter(Space after) =>
        ReferenceEquals(after, After) ? this : new(Id, Prefix, Markers, Expression, Alignment, Format, after);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Interpolation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Interpolation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# await expression.
/// Examples:
///   await task
///   await SomeMethodAsync()
///   await Task.Delay(1000)
/// </summary>
public sealed class AwaitExpression(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression
) : Cs, Expression, Statement, IEquatable<AwaitExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers; 
    public Expression Expression { get; } = expression;

    public AwaitExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression);
    public AwaitExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression);
    public AwaitExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression);
    public AwaitExpression WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AwaitExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AwaitExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// YieldStatementKind removed - Yield now uses Keyword with KeywordKind.Return/Break

/// <summary>
/// A C# yield statement.
/// Examples:
///   yield return value;
///   yield break;
/// </summary>
public sealed class Yield(
    Guid id,
    Space prefix,
    Markers markers,
    Keyword returnOrBreakKeyword,
    Expression? expression
) : Cs, Statement, IEquatable<Yield>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Keyword ReturnOrBreakKeyword { get; } = returnOrBreakKeyword;
    public Expression? Expression { get; } = expression;

    public Yield WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ReturnOrBreakKeyword, Expression);
    public Yield WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ReturnOrBreakKeyword, Expression);
    public Yield WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ReturnOrBreakKeyword, Expression);
    public Yield WithReturnOrBreakKeyword(Keyword returnOrBreakKeyword) =>
        ReferenceEquals(returnOrBreakKeyword, ReturnOrBreakKeyword) ? this : new(Id, Prefix, Markers, returnOrBreakKeyword, Expression);
    public Yield WithExpression(Expression? expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, ReturnOrBreakKeyword, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Yield? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Yield);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# block-scoped namespace declaration.
/// Example:
///   namespace MyApp.Services
///   {
///       class Foo { }
///   }
/// </summary>
public sealed class NamespaceDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> name,
    IList<JRightPadded<Statement>> members,
    Space end
) : Cs, Statement, IEquatable<NamespaceDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Name { get; } = name;
    public IList<JRightPadded<Statement>> Members { get; } = members;
    public Space End { get; } = end;

    public NamespaceDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name, Members, End);
    public NamespaceDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name, Members, End);
    public NamespaceDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name, Members, End);
    public NamespaceDeclaration WithName(JRightPadded<Expression> name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, name, Members, End);
    public NamespaceDeclaration WithMembers(IList<JRightPadded<Statement>> members) =>
        ReferenceEquals(members, Members) ? this : new(Id, Prefix, Markers, Name, members, End);
    public NamespaceDeclaration WithEnd(Space end) =>
        ReferenceEquals(end, End) ? this : new(Id, Prefix, Markers, Name, Members, end);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NamespaceDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NamespaceDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
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
public sealed class TupleType(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<VariableDeclarations> elements
) : Cs, TypeTree, Expression, IEquatable<TupleType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<VariableDeclarations> Elements { get; } = elements;

    public TupleType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Elements);
    public TupleType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Elements);
    public TupleType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Elements);
    public TupleType WithElements(JContainer<VariableDeclarations> elements) =>
        ReferenceEquals(elements, Elements) ? this : new(Id, Prefix, Markers, elements);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(TupleType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TupleType);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# extern alias directive.
/// Examples:
///   extern alias MyAlias;
/// </summary>
public sealed class ExternAlias(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<Identifier> identifier
) : Cs, Statement, IEquatable<ExternAlias>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<Identifier> Identifier { get; } = identifier;

    public ExternAlias WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Identifier);
    public ExternAlias WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Identifier);
    public ExternAlias WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Identifier);
    public ExternAlias WithIdentifier(JLeftPadded<Identifier> identifier) =>
        ReferenceEquals(identifier, Identifier) ? this : new(Id, Prefix, Markers, identifier);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ExternAlias? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ExternAlias);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# initializer expression (object/collection initializer).
/// Examples:
///   { Name = "John", Age = 25 }      // object initializer
///   { 1, 2, 3 }                      // collection initializer
///   { { "a", 1 }, { "b", 2 } }      // dictionary initializer
/// </summary>
public sealed class InitializerExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> expressions
) : Cs, Expression, IEquatable<InitializerExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> Expressions { get; } = expressions;

    public InitializerExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expressions);
    public InitializerExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expressions);
    public InitializerExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expressions);
    public InitializerExpression WithExpressions(JContainer<Expression> expressions) =>
        ReferenceEquals(expressions, Expressions) ? this : new(Id, Prefix, Markers, expressions);

    public JavaType? Type => null;

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(InitializerExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as InitializerExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A null-forgiving (null-suppression) expression: expr!
/// The right padding on the expression holds any space before the '!'.
/// </summary>
public sealed class NullSafeExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expressionPadded
) : Cs, Expression, IEquatable<NullSafeExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> ExpressionPadded { get; } = expressionPadded;

    public NullSafeExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ExpressionPadded);
    public NullSafeExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ExpressionPadded);
    public NullSafeExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ExpressionPadded);
    public NullSafeExpression WithExpressionPadded(JRightPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, expressionPadded);

    public Expression Expression => ExpressionPadded.Element;

    public JavaType? Type => null;

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NullSafeExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NullSafeExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A C# tuple expression (literal or deconstruction target).
/// Examples:
///   (1, 2)                          // tuple literal
///   (name: "John", age: 25)         // named elements use NamedExpression
///   (var x, var y) = tuple;         // deconstruction pattern
/// </summary>
public sealed class TupleExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> arguments
) : Cs, Expression, IEquatable<TupleExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> Arguments { get; } = arguments;

    public TupleExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Arguments);
    public TupleExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Arguments);
    public TupleExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Arguments);
    public TupleExpression WithArguments(JContainer<Expression> arguments) =>
        ReferenceEquals(arguments, Arguments) ? this : new(Id, Prefix, Markers, arguments);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(TupleExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TupleExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Keyword ----

public enum KeywordKind { Ref, Out, Await, Base, This, Break, Return, Not, Default, Case, Checked, Unchecked, Operator }

public sealed class Keyword(
    Guid id,
    Space prefix,
    Markers markers,
    KeywordKind kind
) : Cs, IEquatable<Keyword>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public KeywordKind Kind { get; } = kind;

    public Keyword WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Kind);
    public Keyword WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Kind);
    public Keyword WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Kind);
    public Keyword WithKind(KeywordKind kind) =>
        kind == Kind ? this : new(Id, Prefix, Markers, kind);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Keyword? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Keyword);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Argument ----

public sealed class CsArgument(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier>? nameColumn,
    Keyword? refKindKeyword,
    Expression expression
) : Cs, Expression, IEquatable<CsArgument>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier>? NameColumn { get; } = nameColumn;
    public Keyword? RefKindKeyword { get; } = refKindKeyword;
    public Expression Expression { get; } = expression;

    public CsArgument WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, NameColumn, RefKindKeyword, Expression);
    public CsArgument WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, NameColumn, RefKindKeyword, Expression);
    public CsArgument WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, NameColumn, RefKindKeyword, Expression);
    public CsArgument WithNameColumn(JRightPadded<Identifier>? nameColumn) =>
        ReferenceEquals(nameColumn, NameColumn) ? this : new(Id, Prefix, Markers, nameColumn, RefKindKeyword, Expression);
    public CsArgument WithRefKindKeyword(Keyword? refKindKeyword) =>
        ReferenceEquals(refKindKeyword, RefKindKeyword) ? this : new(Id, Prefix, Markers, NameColumn, refKindKeyword, Expression);
    public CsArgument WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, NameColumn, RefKindKeyword, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsArgument? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsArgument);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- NameColon ----

public sealed class NameColon(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> name
) : Cs, IEquatable<NameColon>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> Name { get; } = name;

    public NameColon WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name);
    public NameColon WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name);
    public NameColon WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name);
    public NameColon WithName(JRightPadded<Identifier> name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, name);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NameColon? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NameColon);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- AnnotatedStatement ----

public sealed class AnnotatedStatement(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    Statement statement
) : Cs, Statement, IEquatable<AnnotatedStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public Statement Statement { get; } = statement;

    public AnnotatedStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Statement);
    public AnnotatedStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Statement);
    public AnnotatedStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Statement);
    public AnnotatedStatement WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Statement);
    public AnnotatedStatement WithStatement(Statement statement) =>
        ReferenceEquals(statement, Statement) ? this : new(Id, Prefix, Markers, AttributeLists, statement);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AnnotatedStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AnnotatedStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ArrayRankSpecifier ----

public sealed class ArrayRankSpecifier(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> sizes
) : Cs, Expression, IEquatable<ArrayRankSpecifier>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> Sizes { get; } = sizes;

    public ArrayRankSpecifier WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Sizes);
    public ArrayRankSpecifier WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Sizes);
    public ArrayRankSpecifier WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Sizes);
    public ArrayRankSpecifier WithSizes(JContainer<Expression> sizes) =>
        ReferenceEquals(sizes, Sizes) ? this : new(Id, Prefix, Markers, sizes);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ArrayRankSpecifier? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ArrayRankSpecifier);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- AssignmentOperation ----

public sealed class AssignmentOperation(
    Guid id,
    Space prefix,
    Markers markers,
    Expression variable,
    JLeftPadded<AssignmentOperation.OperatorType> @operator,
    Expression assignmentValue,
    JavaType? type
) : Cs, Statement, Expression, IEquatable<AssignmentOperation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Variable { get; } = variable;
    public JLeftPadded<AssignmentOperation.OperatorType> Operator { get; } = @operator;
    public Expression AssignmentValue { get; } = assignmentValue;
    public JavaType? Type { get; } = type;

    public AssignmentOperation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Variable, Operator, AssignmentValue, Type);
    public AssignmentOperation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Variable, Operator, AssignmentValue, Type);
    public AssignmentOperation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Variable, Operator, AssignmentValue, Type);
    public AssignmentOperation WithVariable(Expression variable) =>
        ReferenceEquals(variable, Variable) ? this : new(Id, Prefix, Markers, variable, Operator, AssignmentValue, Type);
    public AssignmentOperation WithOperator(JLeftPadded<AssignmentOperation.OperatorType> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, Variable, @operator, AssignmentValue, Type);
    public AssignmentOperation WithAssignmentValue(Expression assignmentValue) =>
        ReferenceEquals(assignmentValue, AssignmentValue) ? this : new(Id, Prefix, Markers, Variable, Operator, assignmentValue, Type);
    public AssignmentOperation WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Variable, Operator, AssignmentValue, type);

    public enum OperatorType
    {
        Addition,
        Subtraction,
        Multiplication,
        Division,
        Modulo,
        BitAnd,
        BitOr,
        BitXor,
        LeftShift,
        RightShift,
        UnsignedRightShift,
        NullCoalescing,
        Coalesce
    }

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AssignmentOperation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AssignmentOperation);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- StackAllocExpression ----

public sealed class StackAllocExpression(
    Guid id,
    Space prefix,
    Markers markers,
    NewArray expression
) : Cs, Expression, IEquatable<StackAllocExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public NewArray Expression { get; } = expression;

    public StackAllocExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression);
    public StackAllocExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression);
    public StackAllocExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression);
    public StackAllocExpression WithExpression(NewArray expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(StackAllocExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as StackAllocExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- GotoStatement ----

public sealed class GotoStatement(
    Guid id,
    Space prefix,
    Markers markers,
    Keyword? caseOrDefaultKeyword,
    Expression? target
) : Cs, Statement, IEquatable<GotoStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Keyword? CaseOrDefaultKeyword { get; } = caseOrDefaultKeyword;
    public Expression? Target { get; } = target;

    public GotoStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, CaseOrDefaultKeyword, Target);
    public GotoStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, CaseOrDefaultKeyword, Target);
    public GotoStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, CaseOrDefaultKeyword, Target);
    public GotoStatement WithCaseOrDefaultKeyword(Keyword? caseOrDefaultKeyword) =>
        ReferenceEquals(caseOrDefaultKeyword, CaseOrDefaultKeyword) ? this : new(Id, Prefix, Markers, caseOrDefaultKeyword, Target);
    public GotoStatement WithTarget(Expression? target) =>
        ReferenceEquals(target, Target) ? this : new(Id, Prefix, Markers, CaseOrDefaultKeyword, target);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(GotoStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as GotoStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- EventDeclaration ----

public sealed class EventDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    IList<Modifier> modifiers,
    JLeftPadded<TypeTree> typeExpressionPadded,
    JRightPadded<TypeTree>? interfaceSpecifier,
    Identifier name,
    JContainer<Statement>? accessors
) : Cs, Statement, IEquatable<EventDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JLeftPadded<TypeTree> TypeExpressionPadded { get; } = typeExpressionPadded;
    public JRightPadded<TypeTree>? InterfaceSpecifier { get; } = interfaceSpecifier;
    public Identifier Name { get; } = name;
    public JContainer<Statement>? Accessors { get; } = accessors;

    public EventDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, AttributeLists, modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithTypeExpressionPadded(JLeftPadded<TypeTree> typeExpressionPadded) =>
        ReferenceEquals(typeExpressionPadded, TypeExpressionPadded) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, typeExpressionPadded, InterfaceSpecifier, Name, Accessors);
    public EventDeclaration WithInterfaceSpecifier(JRightPadded<TypeTree>? interfaceSpecifier) =>
        ReferenceEquals(interfaceSpecifier, InterfaceSpecifier) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, TypeExpressionPadded, interfaceSpecifier, Name, Accessors);
    public EventDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, name, Accessors);
    public EventDeclaration WithAccessors(JContainer<Statement>? accessors) =>
        ReferenceEquals(accessors, Accessors) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, TypeExpressionPadded, InterfaceSpecifier, Name, accessors);

    public TypeTree TypeExpression => TypeExpressionPadded.Element;
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EventDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EventDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsBinary ----

public sealed class CsBinary(
    Guid id,
    Space prefix,
    Markers markers,
    Expression left,
    JLeftPadded<CsBinary.OperatorType> @operator,
    Expression right,
    JavaType? type
) : Cs, Expression, IEquatable<CsBinary>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Left { get; } = left;
    public JLeftPadded<CsBinary.OperatorType> Operator { get; } = @operator;
    public Expression Right { get; } = right;
    public JavaType? Type { get; } = type;

    public CsBinary WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Left, Operator, Right, Type);
    public CsBinary WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Left, Operator, Right, Type);
    public CsBinary WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Left, Operator, Right, Type);
    public CsBinary WithLeft(Expression left) =>
        ReferenceEquals(left, Left) ? this : new(Id, Prefix, Markers, left, Operator, Right, Type);
    public CsBinary WithOperator(JLeftPadded<CsBinary.OperatorType> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, Left, @operator, Right, Type);
    public CsBinary WithRight(Expression right) =>
        ReferenceEquals(right, Right) ? this : new(Id, Prefix, Markers, Left, Operator, right, Type);
    public CsBinary WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Left, Operator, Right, type);

    public enum OperatorType
    {
        As,
        NullCoalescing
    }

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsBinary? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsBinary);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CollectionExpression ----

public sealed class CollectionExpression(
    Guid id,
    Space prefix,
    Markers markers,
    IList<JRightPadded<Expression>> elements,
    JavaType? type
) : Cs, Expression, IEquatable<CollectionExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<JRightPadded<Expression>> Elements { get; } = elements;
    public JavaType? Type { get; } = type;

    public CollectionExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Elements, Type);
    public CollectionExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Elements, Type);
    public CollectionExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Elements, Type);
    public CollectionExpression WithElements(IList<JRightPadded<Expression>> elements) =>
        ReferenceEquals(elements, Elements) ? this : new(Id, Prefix, Markers, elements, Type);
    public CollectionExpression WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Elements, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CollectionExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CollectionExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsExpressionStatement (Cs wrapper for expression-as-statement) ----

public sealed class CsExpressionStatement(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expressionPadded
) : Cs, Statement, IEquatable<CsExpressionStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> ExpressionPadded { get; } = expressionPadded;

    public CsExpressionStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ExpressionPadded);
    public CsExpressionStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ExpressionPadded);
    public CsExpressionStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ExpressionPadded);
    public CsExpressionStatement WithExpressionPadded(JRightPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, expressionPadded);

    public Expression Expression => ExpressionPadded.Element;
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsExpressionStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsExpressionStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ForEachVariableLoop ----

public sealed class ForEachVariableLoopControl(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> variable,
    JRightPadded<Expression> iterable
) : Cs, IEquatable<ForEachVariableLoopControl>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Variable { get; } = variable;
    public JRightPadded<Expression> Iterable { get; } = iterable;

    public ForEachVariableLoopControl WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Variable, Iterable);
    public ForEachVariableLoopControl WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Variable, Iterable);
    public ForEachVariableLoopControl WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Variable, Iterable);
    public ForEachVariableLoopControl WithVariable(JRightPadded<Expression> variable) =>
        ReferenceEquals(variable, Variable) ? this : new(Id, Prefix, Markers, variable, Iterable);
    public ForEachVariableLoopControl WithIterable(JRightPadded<Expression> iterable) =>
        ReferenceEquals(iterable, Iterable) ? this : new(Id, Prefix, Markers, Variable, iterable);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ForEachVariableLoopControl? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ForEachVariableLoopControl);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class ForEachVariableLoop(
    Guid id,
    Space prefix,
    Markers markers,
    ForEachVariableLoopControl controlElement,
    JRightPadded<Statement> body
) : Cs, Statement, IEquatable<ForEachVariableLoop>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ForEachVariableLoopControl ControlElement { get; } = controlElement;
    public JRightPadded<Statement> Body { get; } = body;

    public ForEachVariableLoop WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ControlElement, Body);
    public ForEachVariableLoop WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ControlElement, Body);
    public ForEachVariableLoop WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ControlElement, Body);
    public ForEachVariableLoop WithControlElement(ForEachVariableLoopControl controlElement) =>
        ReferenceEquals(controlElement, ControlElement) ? this : new(Id, Prefix, Markers, controlElement, Body);
    public ForEachVariableLoop WithBody(JRightPadded<Statement> body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, ControlElement, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ForEachVariableLoop? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ForEachVariableLoop);
    public override int GetHashCode() => Id.GetHashCode();
}

// CsClassDeclaration, ClassDeclarationKind DELETED — use J.ClassDeclaration with ConstrainedTypeParameter in J.TypeParameter.Bounds

// ---- CsMethodDeclaration ----

public sealed class CsMethodDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributes,
    IList<Modifier> modifiers,
    JContainer<TypeParameter>? typeParameters,
    TypeTree returnTypeExpression,
    JRightPadded<TypeTree>? explicitInterfaceSpecifier,
    Identifier name,
    JContainer<Statement> parameters,
    Statement? body,
    JavaType.Method? methodType
) : Cs, Statement, IEquatable<CsMethodDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> Attributes { get; } = attributes;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JContainer<TypeParameter>? TypeParameters { get; } = typeParameters;
    public TypeTree ReturnTypeExpression { get; } = returnTypeExpression;
    public JRightPadded<TypeTree>? ExplicitInterfaceSpecifier { get; } = explicitInterfaceSpecifier;
    public Identifier Name { get; } = name;
    public JContainer<Statement> Parameters { get; } = parameters;
    public Statement? Body { get; } = body;
    public JavaType.Method? MethodType { get; } = methodType;

    public CsMethodDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithAttributes(IList<AttributeList> attributes) =>
        ReferenceEquals(attributes, Attributes) ? this : new(Id, Prefix, Markers, attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, Attributes, modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithTypeParameters(JContainer<TypeParameter>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, typeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithReturnTypeExpression(TypeTree returnTypeExpression) =>
        ReferenceEquals(returnTypeExpression, ReturnTypeExpression) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, returnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithExplicitInterfaceSpecifier(JRightPadded<TypeTree>? explicitInterfaceSpecifier) =>
        ReferenceEquals(explicitInterfaceSpecifier, ExplicitInterfaceSpecifier) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, explicitInterfaceSpecifier, Name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, name, Parameters, Body, MethodType);
    public CsMethodDeclaration WithParameters(JContainer<Statement> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, parameters, Body, MethodType);
    public CsMethodDeclaration WithBody(Statement? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, body, MethodType);
    public CsMethodDeclaration WithMethodType(JavaType.Method? methodType) =>
        ReferenceEquals(methodType, MethodType) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, TypeParameters, ReturnTypeExpression, ExplicitInterfaceSpecifier, Name, Parameters, Body, methodType);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsMethodDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsMethodDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- UsingStatement ----

public sealed class UsingStatement(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<Expression> expressionPadded,
    Statement statement
) : Cs, Statement, IEquatable<UsingStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<Expression> ExpressionPadded { get; } = expressionPadded;
    public Statement Statement { get; } = statement;

    public UsingStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ExpressionPadded, Statement);
    public UsingStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ExpressionPadded, Statement);
    public UsingStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ExpressionPadded, Statement);
    public UsingStatement WithExpressionPadded(JLeftPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, expressionPadded, Statement);
    public UsingStatement WithStatement(Statement statement) =>
        ReferenceEquals(statement, Statement) ? this : new(Id, Prefix, Markers, ExpressionPadded, statement);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(UsingStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as UsingStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

// TypeParameterConstraintClause, TypeConstraint DELETED — absorbed into ConstrainedTypeParameter

// ---- AllowsConstraintClause ----

public sealed class AllowsConstraintClause(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> expressions
) : Cs, Expression, IEquatable<AllowsConstraintClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> Expressions { get; } = expressions;

    public AllowsConstraintClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expressions);
    public AllowsConstraintClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expressions);
    public AllowsConstraintClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expressions);
    public AllowsConstraintClause WithExpressions(JContainer<Expression> expressions) =>
        ReferenceEquals(expressions, Expressions) ? this : new(Id, Prefix, Markers, expressions);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AllowsConstraintClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AllowsConstraintClause);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- RefStructConstraint ----

public sealed class RefStructConstraint(
    Guid id,
    Space prefix,
    Markers markers
) : Cs, Expression, IEquatable<RefStructConstraint>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;

    public RefStructConstraint WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers);
    public RefStructConstraint WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers);
    public RefStructConstraint WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RefStructConstraint? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RefStructConstraint);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ClassOrStructConstraint ----

public sealed class ClassOrStructConstraint(
    Guid id,
    Space prefix,
    Markers markers,
    ClassOrStructConstraint.TypeKind kind,
    bool nullable = false
) : Cs, Expression, IEquatable<ClassOrStructConstraint>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ClassOrStructConstraint.TypeKind Kind { get; } = kind;
    public bool Nullable { get; } = nullable;

    public ClassOrStructConstraint WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Kind, Nullable);
    public ClassOrStructConstraint WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Kind, Nullable);
    public ClassOrStructConstraint WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Kind, Nullable);
    public ClassOrStructConstraint WithKind(ClassOrStructConstraint.TypeKind kind) =>
        kind == Kind ? this : new(Id, Prefix, Markers, kind, Nullable);
    public ClassOrStructConstraint WithNullable(bool nullable) =>
        nullable == Nullable ? this : new(Id, Prefix, Markers, Kind, nullable);

    public enum TypeKind { Class, Struct }
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ClassOrStructConstraint? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ClassOrStructConstraint);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ConstructorConstraint ----

public sealed class ConstructorConstraint(
    Guid id,
    Space prefix,
    Markers markers
) : Cs, Expression, IEquatable<ConstructorConstraint>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;

    public ConstructorConstraint WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers);
    public ConstructorConstraint WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers);
    public ConstructorConstraint WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ConstructorConstraint? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConstructorConstraint);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- DefaultConstraint ----

public sealed class DefaultConstraint(
    Guid id,
    Space prefix,
    Markers markers
) : Cs, Expression, IEquatable<DefaultConstraint>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;

    public DefaultConstraint WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers);
    public DefaultConstraint WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers);
    public DefaultConstraint WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DefaultConstraint? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DefaultConstraint);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Variable Designations ----

public sealed class SingleVariableDesignation(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier name
) : VariableDesignation, Cs, IEquatable<SingleVariableDesignation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Name { get; } = name;

    public SingleVariableDesignation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name);
    public SingleVariableDesignation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name);
    public SingleVariableDesignation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name);
    public SingleVariableDesignation WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, name);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SingleVariableDesignation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SingleVariableDesignation);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class ParenthesizedVariableDesignation(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<VariableDesignation> variables,
    JavaType? type
) : VariableDesignation, Cs, IEquatable<ParenthesizedVariableDesignation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<VariableDesignation> Variables { get; } = variables;
    public JavaType? Type { get; } = type;

    public ParenthesizedVariableDesignation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Variables, Type);
    public ParenthesizedVariableDesignation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Variables, Type);
    public ParenthesizedVariableDesignation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Variables, Type);
    public ParenthesizedVariableDesignation WithVariables(JContainer<VariableDesignation> variables) =>
        ReferenceEquals(variables, Variables) ? this : new(Id, Prefix, Markers, variables, Type);
    public ParenthesizedVariableDesignation WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Variables, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ParenthesizedVariableDesignation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ParenthesizedVariableDesignation);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class DiscardVariableDesignation(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier discard
) : VariableDesignation, Cs, IEquatable<DiscardVariableDesignation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Discard { get; } = discard;

    public DiscardVariableDesignation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Discard);
    public DiscardVariableDesignation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Discard);
    public DiscardVariableDesignation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Discard);
    public DiscardVariableDesignation WithDiscard(Identifier discard) =>
        ReferenceEquals(discard, Discard) ? this : new(Id, Prefix, Markers, discard);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DiscardVariableDesignation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DiscardVariableDesignation);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsUnary ----

public sealed class CsUnary(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<CsUnary.OperatorKind> @operator,
    Expression expression,
    JavaType? type
) : Cs, Statement, Expression, IEquatable<CsUnary>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<CsUnary.OperatorKind> Operator { get; } = @operator;
    public Expression Expression { get; } = expression;
    public JavaType? Type { get; } = type;

    public CsUnary WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Operator, Expression, Type);
    public CsUnary WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Operator, Expression, Type);
    public CsUnary WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Operator, Expression, Type);
    public CsUnary WithOperator(JLeftPadded<CsUnary.OperatorKind> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, @operator, Expression, Type);
    public CsUnary WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, Operator, expression, Type);
    public CsUnary WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Operator, Expression, type);

    public enum OperatorKind { SuppressNullableWarning, PointerType, AddressOf, Spread, FromEnd }
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsUnary? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsUnary);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- TupleElement ----

public sealed class TupleElement(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree elementType,
    Identifier? name
) : Cs, IEquatable<TupleElement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree ElementType { get; } = elementType;
    public Identifier? Name { get; } = name;

    public TupleElement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ElementType, Name);
    public TupleElement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ElementType, Name);
    public TupleElement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ElementType, Name);
    public TupleElement WithElementType(TypeTree elementType) =>
        ReferenceEquals(elementType, ElementType) ? this : new(Id, Prefix, Markers, elementType, Name);
    public TupleElement WithName(Identifier? name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, ElementType, name);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(TupleElement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TupleElement);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsNewClass ----

public sealed class CsNewClass(
    Guid id,
    Space prefix,
    Markers markers,
    NewClass newClassCore,
    InitializerExpression? initializer
) : Cs, Statement, Expression, IEquatable<CsNewClass>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public NewClass NewClassCore { get; } = newClassCore;
    public InitializerExpression? Initializer { get; } = initializer;

    public CsNewClass WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, NewClassCore, Initializer);
    public CsNewClass WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, NewClassCore, Initializer);
    public CsNewClass WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, NewClassCore, Initializer);
    public CsNewClass WithNewClassCore(NewClass newClassCore) =>
        ReferenceEquals(newClassCore, NewClassCore) ? this : new(Id, Prefix, Markers, newClassCore, Initializer);
    public CsNewClass WithInitializer(InitializerExpression? initializer) =>
        ReferenceEquals(initializer, Initializer) ? this : new(Id, Prefix, Markers, NewClassCore, initializer);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsNewClass? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsNewClass);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ImplicitElementAccess ----

public sealed class ImplicitElementAccess(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> argumentList
) : Cs, Expression, IEquatable<ImplicitElementAccess>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> ArgumentList { get; } = argumentList;

    public ImplicitElementAccess WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ArgumentList);
    public ImplicitElementAccess WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ArgumentList);
    public ImplicitElementAccess WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ArgumentList);
    public ImplicitElementAccess WithArgumentList(JContainer<Expression> argumentList) =>
        ReferenceEquals(argumentList, ArgumentList) ? this : new(Id, Prefix, Markers, argumentList);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ImplicitElementAccess? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ImplicitElementAccess);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class ConstantPattern(
    Guid id,
    Space prefix,
    Markers markers,
    Expression value
) : Cs, Pattern, IEquatable<ConstantPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Value { get; } = value;

    public ConstantPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Value);
    public ConstantPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Value);
    public ConstantPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Value);
    public ConstantPattern WithValue(Expression value) =>
        ReferenceEquals(value, Value) ? this : new(Id, Prefix, Markers, value);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ConstantPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConstantPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class DiscardPattern(
    Guid id,
    Space prefix,
    Markers markers,
    JavaType? type
) : Cs, Pattern, IEquatable<DiscardPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JavaType? Type { get; } = type;

    public DiscardPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Type);
    public DiscardPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Type);
    public DiscardPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Type);
    public DiscardPattern WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DiscardPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DiscardPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class ListPattern(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Pattern> patterns,
    VariableDesignation? designation
) : Cs, Pattern, IEquatable<ListPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Pattern> Patterns { get; } = patterns;
    public VariableDesignation? Designation { get; } = designation;

    public ListPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Patterns, Designation);
    public ListPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Patterns, Designation);
    public ListPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Patterns, Designation);
    public ListPattern WithPatterns(JContainer<Pattern> patterns) =>
        ReferenceEquals(patterns, Patterns) ? this : new(Id, Prefix, Markers, patterns, Designation);
    public ListPattern WithDesignation(VariableDesignation? designation) =>
        ReferenceEquals(designation, Designation) ? this : new(Id, Prefix, Markers, Patterns, designation);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ListPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ListPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class SlicePattern(
    Guid id,
    Space prefix,
    Markers markers
) : Cs, Pattern, IEquatable<SlicePattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;

    public SlicePattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers);
    public SlicePattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers);
    public SlicePattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SlicePattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SlicePattern);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Switch types ----

public sealed class SwitchExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expressionPadded,
    JContainer<SwitchExpressionArm> arms
) : Cs, Expression, IEquatable<SwitchExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> ExpressionPadded { get; } = expressionPadded;
    public JContainer<SwitchExpressionArm> Arms { get; } = arms;

    public SwitchExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ExpressionPadded, Arms);
    public SwitchExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ExpressionPadded, Arms);
    public SwitchExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ExpressionPadded, Arms);
    public SwitchExpression WithExpressionPadded(JRightPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, expressionPadded, Arms);
    public SwitchExpression WithArms(JContainer<SwitchExpressionArm> arms) =>
        ReferenceEquals(arms, Arms) ? this : new(Id, Prefix, Markers, ExpressionPadded, arms);

    public Expression Expression => ExpressionPadded.Element;
    // Type is derived from arms' result expression types (not sent over RPC)
    public JavaType? Type => null;
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SwitchExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SwitchExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class SwitchExpressionArm(
    Guid id,
    Space prefix,
    Markers markers,
    J pattern,
    JLeftPadded<Expression>? whenExpression,
    JLeftPadded<Expression> expressionPadded
) : Cs, IEquatable<SwitchExpressionArm>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public J Pattern { get; } = pattern;
    public JLeftPadded<Expression>? WhenExpression { get; } = whenExpression;
    public JLeftPadded<Expression> ExpressionPadded { get; } = expressionPadded;

    public SwitchExpressionArm WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Pattern, WhenExpression, ExpressionPadded);
    public SwitchExpressionArm WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Pattern, WhenExpression, ExpressionPadded);
    public SwitchExpressionArm WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Pattern, WhenExpression, ExpressionPadded);
    public SwitchExpressionArm WithPattern(J pattern) =>
        ReferenceEquals(pattern, Pattern) ? this : new(Id, Prefix, Markers, pattern, WhenExpression, ExpressionPadded);
    public SwitchExpressionArm WithWhenExpression(JLeftPadded<Expression>? whenExpression) =>
        ReferenceEquals(whenExpression, WhenExpression) ? this : new(Id, Prefix, Markers, Pattern, whenExpression, ExpressionPadded);
    public SwitchExpressionArm WithExpressionPadded(JLeftPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, Pattern, WhenExpression, expressionPadded);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SwitchExpressionArm? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SwitchExpressionArm);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Checked ----

public sealed class CheckedExpression(
    Guid id,
    Space prefix,
    Markers markers,
    Keyword checkedOrUncheckedKeyword,
    ControlParentheses<Expression> expressionValue
) : Cs, Expression, IEquatable<CheckedExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Keyword CheckedOrUncheckedKeyword { get; } = checkedOrUncheckedKeyword;
    public ControlParentheses<Expression> ExpressionValue { get; } = expressionValue;

    public CheckedExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, CheckedOrUncheckedKeyword, ExpressionValue);
    public CheckedExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, CheckedOrUncheckedKeyword, ExpressionValue);
    public CheckedExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, CheckedOrUncheckedKeyword, ExpressionValue);
    public CheckedExpression WithCheckedOrUncheckedKeyword(Keyword checkedOrUncheckedKeyword) =>
        ReferenceEquals(checkedOrUncheckedKeyword, CheckedOrUncheckedKeyword) ? this : new(Id, Prefix, Markers, checkedOrUncheckedKeyword, ExpressionValue);
    public CheckedExpression WithExpressionValue(ControlParentheses<Expression> expressionValue) =>
        ReferenceEquals(expressionValue, ExpressionValue) ? this : new(Id, Prefix, Markers, CheckedOrUncheckedKeyword, expressionValue);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CheckedExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CheckedExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class CheckedStatement(
    Guid id,
    Space prefix,
    Markers markers,
    Keyword keywordValue,
    Block block
) : Cs, Statement, IEquatable<CheckedStatement>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Keyword KeywordValue { get; } = keywordValue;
    public Block Block { get; } = block;

    public CheckedStatement WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, KeywordValue, Block);
    public CheckedStatement WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, KeywordValue, Block);
    public CheckedStatement WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, KeywordValue, Block);
    public CheckedStatement WithKeywordValue(Keyword keywordValue) =>
        ReferenceEquals(keywordValue, KeywordValue) ? this : new(Id, Prefix, Markers, keywordValue, Block);
    public CheckedStatement WithBlock(Block block) =>
        ReferenceEquals(block, Block) ? this : new(Id, Prefix, Markers, KeywordValue, block);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CheckedStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CheckedStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- RangeExpression ----

public sealed class RangeExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression>? start,
    Expression? end
) : Cs, Expression, IEquatable<RangeExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression>? Start { get; } = start;
    public Expression? End { get; } = end;

    public RangeExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Start, End);
    public RangeExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Start, End);
    public RangeExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Start, End);
    public RangeExpression WithStart(JRightPadded<Expression>? start) =>
        ReferenceEquals(start, Start) ? this : new(Id, Prefix, Markers, start, End);
    public RangeExpression WithEnd(Expression? end) =>
        ReferenceEquals(end, End) ? this : new(Id, Prefix, Markers, Start, end);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RangeExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RangeExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Declaration types ----

public sealed class IndexerDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Modifier> modifiers,
    TypeTree typeExpression,
    JRightPadded<TypeTree>? explicitInterfaceSpecifier,
    Expression indexer,
    JContainer<Expression> parameters,
    JLeftPadded<Expression>? expressionBody,
    Block? accessors
) : Cs, Statement, IEquatable<IndexerDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public TypeTree TypeExpression { get; } = typeExpression;
    public JRightPadded<TypeTree>? ExplicitInterfaceSpecifier { get; } = explicitInterfaceSpecifier;
    public Expression Indexer { get; } = indexer;
    public JContainer<Expression> Parameters { get; } = parameters;
    public JLeftPadded<Expression>? ExpressionBody { get; } = expressionBody;
    public Block? Accessors { get; } = accessors;

    public IndexerDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithTypeExpression(TypeTree typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, Modifiers, typeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithExplicitInterfaceSpecifier(JRightPadded<TypeTree>? explicitInterfaceSpecifier) =>
        ReferenceEquals(explicitInterfaceSpecifier, ExplicitInterfaceSpecifier) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, explicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithIndexer(Expression indexer) =>
        ReferenceEquals(indexer, Indexer) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, indexer, Parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithParameters(JContainer<Expression> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, parameters, ExpressionBody, Accessors);
    public IndexerDeclaration WithExpressionBody(JLeftPadded<Expression>? expressionBody) =>
        ReferenceEquals(expressionBody, ExpressionBody) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, expressionBody, Accessors);
    public IndexerDeclaration WithAccessors(Block? accessors) =>
        ReferenceEquals(accessors, Accessors) ? this : new(Id, Prefix, Markers, Modifiers, TypeExpression, ExplicitInterfaceSpecifier, Indexer, Parameters, ExpressionBody, accessors);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(IndexerDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as IndexerDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class DelegateDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributes,
    IList<Modifier> modifiers,
    JLeftPadded<TypeTree> returnType,
    Identifier identifierName,
    JContainer<TypeParameter>? typeParameters,
    JContainer<Statement> parameters
) : Cs, Statement, IEquatable<DelegateDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> Attributes { get; } = attributes;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JLeftPadded<TypeTree> ReturnType { get; } = returnType;
    public Identifier IdentifierName { get; } = identifierName;
    public JContainer<TypeParameter>? TypeParameters { get; } = typeParameters;
    public JContainer<Statement> Parameters { get; } = parameters;

    public DelegateDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Attributes, Modifiers, ReturnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Attributes, Modifiers, ReturnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Attributes, Modifiers, ReturnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithAttributes(IList<AttributeList> attributes) =>
        ReferenceEquals(attributes, Attributes) ? this : new(Id, Prefix, Markers, attributes, Modifiers, ReturnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, Attributes, modifiers, ReturnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithReturnType(JLeftPadded<TypeTree> returnType) =>
        ReferenceEquals(returnType, ReturnType) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, returnType, IdentifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithIdentifierName(Identifier identifierName) =>
        ReferenceEquals(identifierName, IdentifierName) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, ReturnType, identifierName, TypeParameters, Parameters);
    public DelegateDeclaration WithTypeParameters(JContainer<TypeParameter>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, ReturnType, IdentifierName, typeParameters, Parameters);
    public DelegateDeclaration WithParameters(JContainer<Statement> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, Attributes, Modifiers, ReturnType, IdentifierName, TypeParameters, parameters);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DelegateDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DelegateDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class ConversionOperatorDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Modifier> modifiers,
    JLeftPadded<ConversionOperatorDeclaration.ExplicitImplicit> kind,
    JLeftPadded<TypeTree> returnType,
    JContainer<Statement> parameters,
    JLeftPadded<Expression>? expressionBody,
    Block? body
) : Cs, Statement, IEquatable<ConversionOperatorDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JLeftPadded<ConversionOperatorDeclaration.ExplicitImplicit> Kind { get; } = kind;
    public JLeftPadded<TypeTree> ReturnType { get; } = returnType;
    public JContainer<Statement> Parameters { get; } = parameters;
    public JLeftPadded<Expression>? ExpressionBody { get; } = expressionBody;
    public Block? Body { get; } = body;

    public ConversionOperatorDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Modifiers, Kind, ReturnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Modifiers, Kind, ReturnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Modifiers, Kind, ReturnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, modifiers, Kind, ReturnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithKind(JLeftPadded<ConversionOperatorDeclaration.ExplicitImplicit> kind) =>
        ReferenceEquals(kind, Kind) ? this : new(Id, Prefix, Markers, Modifiers, kind, ReturnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithReturnType(JLeftPadded<TypeTree> returnType) =>
        ReferenceEquals(returnType, ReturnType) ? this : new(Id, Prefix, Markers, Modifiers, Kind, returnType, Parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithParameters(JContainer<Statement> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, Modifiers, Kind, ReturnType, parameters, ExpressionBody, Body);
    public ConversionOperatorDeclaration WithExpressionBody(JLeftPadded<Expression>? expressionBody) =>
        ReferenceEquals(expressionBody, ExpressionBody) ? this : new(Id, Prefix, Markers, Modifiers, Kind, ReturnType, Parameters, expressionBody, Body);
    public ConversionOperatorDeclaration WithBody(Block? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Modifiers, Kind, ReturnType, Parameters, ExpressionBody, body);

    public enum ExplicitImplicit { Implicit, Explicit }
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ConversionOperatorDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConversionOperatorDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class OperatorDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    IList<Modifier> modifiers,
    JRightPadded<TypeTree>? explicitInterfaceSpecifier,
    Keyword operatorKeyword,
    Keyword? checkedKeyword,
    JLeftPadded<OperatorDeclaration.OperatorKind> operatorToken,
    TypeTree returnType,
    JContainer<Expression> parameters,
    Block body,
    JavaType.Method? methodType
) : Cs, Statement, IEquatable<OperatorDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JRightPadded<TypeTree>? ExplicitInterfaceSpecifier { get; } = explicitInterfaceSpecifier;
    public Keyword OperatorKeyword { get; } = operatorKeyword;
    public Keyword? CheckedKeyword { get; } = checkedKeyword;
    public JLeftPadded<OperatorDeclaration.OperatorKind> OperatorToken { get; } = operatorToken;
    public TypeTree ReturnType { get; } = returnType;
    public JContainer<Expression> Parameters { get; } = parameters;
    public Block Body { get; } = body;
    public JavaType.Method? MethodType { get; } = methodType;

    public OperatorDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, AttributeLists, modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithExplicitInterfaceSpecifier(JRightPadded<TypeTree>? explicitInterfaceSpecifier) =>
        ReferenceEquals(explicitInterfaceSpecifier, ExplicitInterfaceSpecifier) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, explicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithOperatorKeyword(Keyword operatorKeyword) =>
        ReferenceEquals(operatorKeyword, OperatorKeyword) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, operatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithCheckedKeyword(Keyword? checkedKeyword) =>
        ReferenceEquals(checkedKeyword, CheckedKeyword) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, checkedKeyword, OperatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithOperatorToken(JLeftPadded<OperatorDeclaration.OperatorKind> operatorToken) =>
        ReferenceEquals(operatorToken, OperatorToken) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, operatorToken, ReturnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithReturnType(TypeTree returnType) =>
        ReferenceEquals(returnType, ReturnType) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, returnType, Parameters, Body, MethodType);
    public OperatorDeclaration WithParameters(JContainer<Expression> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, parameters, Body, MethodType);
    public OperatorDeclaration WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, body, MethodType);
    public OperatorDeclaration WithMethodType(JavaType.Method? methodType) =>
        ReferenceEquals(methodType, MethodType) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, ExplicitInterfaceSpecifier, OperatorKeyword, CheckedKeyword, OperatorToken, ReturnType, Parameters, Body, methodType);

    public enum OperatorKind
    {
        Plus, Minus, Bang, Tilde, PlusPlus, MinusMinus,
        Star, Division, Percent, LeftShift, RightShift, UnsignedRightShift,
        LessThan, GreaterThan, LessThanEquals, GreaterThanEquals,
        Equals, NotEquals, Ampersand, Bar, Caret, True, False
    }
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(OperatorDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as OperatorDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- ConstrainedTypeParameter ----

/// <summary>
/// C#-specific type parameter data stored in J.TypeParameter.Bounds[0].
/// Carries attribute lists, variance (in/out), the type parameter name, and
/// optional where-clause constraint information.
/// </summary>
public sealed class ConstrainedTypeParameter(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    JLeftPadded<ConstrainedTypeParameter.VarianceKind>? variance,
    Identifier name,
    JLeftPadded<Identifier>? whereConstraint,
    JContainer<Expression>? constraints,
    JavaType? type
) : Cs, TypeTree, IEquatable<ConstrainedTypeParameter>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public JLeftPadded<ConstrainedTypeParameter.VarianceKind>? Variance { get; } = variance;
    public Identifier Name { get; } = name;
    public JLeftPadded<Identifier>? WhereConstraint { get; } = whereConstraint;
    public JContainer<Expression>? Constraints { get; } = constraints;
    public JavaType? Type { get; } = type;

    public ConstrainedTypeParameter WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Variance, Name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Variance, Name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Variance, Name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Variance, Name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithVariance(JLeftPadded<ConstrainedTypeParameter.VarianceKind>? variance) =>
        ReferenceEquals(variance, Variance) ? this : new(Id, Prefix, Markers, AttributeLists, variance, Name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, AttributeLists, Variance, name, WhereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithWhereConstraint(JLeftPadded<Identifier>? whereConstraint) =>
        ReferenceEquals(whereConstraint, WhereConstraint) ? this : new(Id, Prefix, Markers, AttributeLists, Variance, Name, whereConstraint, Constraints, Type);
    public ConstrainedTypeParameter WithConstraints(JContainer<Expression>? constraints) =>
        ReferenceEquals(constraints, Constraints) ? this : new(Id, Prefix, Markers, AttributeLists, Variance, Name, WhereConstraint, constraints, Type);
    public ConstrainedTypeParameter WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, AttributeLists, Variance, Name, WhereConstraint, Constraints, type);

    public enum VarianceKind { In, Out }
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ConstrainedTypeParameter? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ConstrainedTypeParameter);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- Enum types ----

public sealed class EnumDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList>? attributeLists,
    IList<Modifier> modifiers,
    JLeftPadded<Identifier> namePadded,
    JLeftPadded<TypeTree>? baseType,
    JContainer<Expression>? members
) : Cs, Statement, IEquatable<EnumDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList>? AttributeLists { get; } = attributeLists;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JLeftPadded<Identifier> NamePadded { get; } = namePadded;
    public JLeftPadded<TypeTree>? BaseType { get; } = baseType;
    public JContainer<Expression>? Members { get; } = members;

    public EnumDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Modifiers, NamePadded, BaseType, Members);
    public EnumDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Modifiers, NamePadded, BaseType, Members);
    public EnumDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Modifiers, NamePadded, BaseType, Members);
    public EnumDeclaration WithAttributeLists(IList<AttributeList>? attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Modifiers, NamePadded, BaseType, Members);
    public EnumDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, AttributeLists, modifiers, NamePadded, BaseType, Members);
    public EnumDeclaration WithNamePadded(JLeftPadded<Identifier> namePadded) =>
        ReferenceEquals(namePadded, NamePadded) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, namePadded, BaseType, Members);
    public EnumDeclaration WithBaseType(JLeftPadded<TypeTree>? baseType) =>
        ReferenceEquals(baseType, BaseType) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, NamePadded, baseType, Members);
    public EnumDeclaration WithMembers(JContainer<Expression>? members) =>
        ReferenceEquals(members, Members) ? this : new(Id, Prefix, Markers, AttributeLists, Modifiers, NamePadded, BaseType, members);

    public Identifier Name => NamePadded.Element;
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EnumDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EnumDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class EnumMemberDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<AttributeList> attributeLists,
    Identifier name,
    JLeftPadded<Expression>? initializer
) : Cs, Expression, IEquatable<EnumMemberDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<AttributeList> AttributeLists { get; } = attributeLists;
    public Identifier Name { get; } = name;
    public JLeftPadded<Expression>? Initializer { get; } = initializer;

    public EnumMemberDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AttributeLists, Name, Initializer);
    public EnumMemberDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AttributeLists, Name, Initializer);
    public EnumMemberDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AttributeLists, Name, Initializer);
    public EnumMemberDeclaration WithAttributeLists(IList<AttributeList> attributeLists) =>
        ReferenceEquals(attributeLists, AttributeLists) ? this : new(Id, Prefix, Markers, attributeLists, Name, Initializer);
    public EnumMemberDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, AttributeLists, name, Initializer);
    public EnumMemberDeclaration WithInitializer(JLeftPadded<Expression>? initializer) =>
        ReferenceEquals(initializer, Initializer) ? this : new(Id, Prefix, Markers, AttributeLists, Name, initializer);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EnumMemberDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EnumMemberDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- AliasQualifiedName ----

public sealed class AliasQualifiedName(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> alias,
    Expression name
) : Cs, TypeTree, Expression, Marker, IEquatable<AliasQualifiedName>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> Alias { get; } = alias;
    public Expression Name { get; } = name;

    public AliasQualifiedName WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Alias, Name);
    public AliasQualifiedName WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Alias, Name);
    public AliasQualifiedName WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Alias, Name);
    public AliasQualifiedName WithAlias(JRightPadded<Identifier> alias) =>
        ReferenceEquals(alias, Alias) ? this : new(Id, Prefix, Markers, alias, Name);
    public AliasQualifiedName WithName(Expression name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Alias, name);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AliasQualifiedName? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AliasQualifiedName);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsArrayType ----

public sealed class CsArrayType(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree? typeExpression,
    IList<ArrayDimension> dimensions,
    JavaType? type
) : Cs, Expression, TypeTree, IEquatable<CsArrayType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree? TypeExpression { get; } = typeExpression;
    public IList<ArrayDimension> Dimensions { get; } = dimensions;
    public JavaType? Type { get; } = type;

    public CsArrayType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeExpression, Dimensions, Type);
    public CsArrayType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeExpression, Dimensions, Type);
    public CsArrayType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeExpression, Dimensions, Type);
    public CsArrayType WithTypeExpression(TypeTree? typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, typeExpression, Dimensions, Type);
    public CsArrayType WithDimensions(IList<ArrayDimension> dimensions) =>
        ReferenceEquals(dimensions, Dimensions) ? this : new(Id, Prefix, Markers, TypeExpression, dimensions, Type);
    public CsArrayType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, TypeExpression, Dimensions, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsArrayType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsArrayType);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- CsTry ----

public sealed class CsTryCatch(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<VariableDeclarations> parameter,
    JLeftPadded<ControlParentheses<Expression>>? filterExpression,
    Block body
) : Cs, IEquatable<CsTryCatch>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<VariableDeclarations> Parameter { get; } = parameter;
    public JLeftPadded<ControlParentheses<Expression>>? FilterExpression { get; } = filterExpression;
    public Block Body { get; } = body;

    public CsTryCatch WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Parameter, FilterExpression, Body);
    public CsTryCatch WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Parameter, FilterExpression, Body);
    public CsTryCatch WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Parameter, FilterExpression, Body);
    public CsTryCatch WithParameter(ControlParentheses<VariableDeclarations> parameter) =>
        ReferenceEquals(parameter, Parameter) ? this : new(Id, Prefix, Markers, parameter, FilterExpression, Body);
    public CsTryCatch WithFilterExpression(JLeftPadded<ControlParentheses<Expression>>? filterExpression) =>
        ReferenceEquals(filterExpression, FilterExpression) ? this : new(Id, Prefix, Markers, Parameter, filterExpression, Body);
    public CsTryCatch WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Parameter, FilterExpression, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsTryCatch? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsTryCatch);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class CsTry(
    Guid id,
    Space prefix,
    Markers markers,
    Block body,
    IList<CsTryCatch> catches,
    JLeftPadded<Block>? @finally
) : Cs, Statement, IEquatable<CsTry>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Block Body { get; } = body;
    public IList<CsTryCatch> Catches { get; } = catches;
    public JLeftPadded<Block>? Finally { get; } = @finally;

    public CsTry WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Body, Catches, Finally);
    public CsTry WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Body, Catches, Finally);
    public CsTry WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Body, Catches, Finally);
    public CsTry WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, body, Catches, Finally);
    public CsTry WithCatches(IList<CsTryCatch> catches) =>
        ReferenceEquals(catches, Catches) ? this : new(Id, Prefix, Markers, Body, catches, Finally);
    public CsTry WithFinally(JLeftPadded<Block>? @finally) =>
        ReferenceEquals(@finally, Finally) ? this : new(Id, Prefix, Markers, Body, Catches, @finally);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(CsTry? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CsTry);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- PointerDereference ----

public sealed class PointerDereference(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression,
    JavaType? type
) : Cs, Expression, IEquatable<PointerDereference>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;
    public JavaType? Type { get; } = type;

    public PointerDereference WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Type);
    public PointerDereference WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Type);
    public PointerDereference WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Type);
    public PointerDereference WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Type);
    public PointerDereference WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Expression, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PointerDereference? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PointerDereference);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- PointerFieldAccess ----

public sealed class PointerFieldAccess(
    Guid id,
    Space prefix,
    Markers markers,
    Expression target,
    JLeftPadded<Identifier> namePadded,
    JavaType? type
) : Cs, TypeTree, Expression, Statement, IEquatable<PointerFieldAccess>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Target { get; } = target;
    public JLeftPadded<Identifier> NamePadded { get; } = namePadded;
    public JavaType? Type { get; } = type;

    public PointerFieldAccess WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Target, NamePadded, Type);
    public PointerFieldAccess WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Target, NamePadded, Type);
    public PointerFieldAccess WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Target, NamePadded, Type);
    public PointerFieldAccess WithTarget(Expression target) =>
        ReferenceEquals(target, Target) ? this : new(Id, Prefix, Markers, target, NamePadded, Type);
    public PointerFieldAccess WithNamePadded(JLeftPadded<Identifier> namePadded) =>
        ReferenceEquals(namePadded, NamePadded) ? this : new(Id, Prefix, Markers, Target, namePadded, Type);
    public PointerFieldAccess WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Target, NamePadded, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(PointerFieldAccess? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PointerFieldAccess);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- RefType ----

public sealed class RefType(
    Guid id,
    Space prefix,
    Markers markers,
    Modifier? readonlyKeyword,
    TypeTree typeIdentifier,
    JavaType? type
) : Cs, TypeTree, Expression, IEquatable<RefType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Modifier? ReadonlyKeyword { get; } = readonlyKeyword;
    public TypeTree TypeIdentifier { get; } = typeIdentifier;
    public JavaType? Type { get; } = type;

    public RefType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ReadonlyKeyword, TypeIdentifier, Type);
    public RefType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ReadonlyKeyword, TypeIdentifier, Type);
    public RefType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ReadonlyKeyword, TypeIdentifier, Type);
    public RefType WithReadonlyKeyword(Modifier? readonlyKeyword) =>
        ReferenceEquals(readonlyKeyword, ReadonlyKeyword) ? this : new(Id, Prefix, Markers, readonlyKeyword, TypeIdentifier, Type);
    public RefType WithTypeIdentifier(TypeTree typeIdentifier) =>
        ReferenceEquals(typeIdentifier, TypeIdentifier) ? this : new(Id, Prefix, Markers, ReadonlyKeyword, typeIdentifier, Type);
    public RefType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, ReadonlyKeyword, TypeIdentifier, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(RefType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RefType);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- AnonymousObjectCreationExpression ----

public sealed class AnonymousObjectCreationExpression(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<Expression> initializers,
    JavaType? type
) : Cs, Expression, IEquatable<AnonymousObjectCreationExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<Expression> Initializers { get; } = initializers;
    public JavaType? Type { get; } = type;

    public AnonymousObjectCreationExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Initializers, Type);
    public AnonymousObjectCreationExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Initializers, Type);
    public AnonymousObjectCreationExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Initializers, Type);
    public AnonymousObjectCreationExpression WithInitializers(JContainer<Expression> initializers) =>
        ReferenceEquals(initializers, Initializers) ? this : new(Id, Prefix, Markers, initializers, Type);
    public AnonymousObjectCreationExpression WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Initializers, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AnonymousObjectCreationExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AnonymousObjectCreationExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- WithExpression ----

public sealed class WithExpression(
    Guid id,
    Space prefix,
    Markers markers,
    Expression target,
    JLeftPadded<Expression> initializerPadded,
    JavaType? type
) : Cs, Expression, IEquatable<WithExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Target { get; } = target;
    public JLeftPadded<Expression> InitializerPadded { get; } = initializerPadded;
    public JavaType? Type { get; } = type;

    public WithExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Target, InitializerPadded, Type);
    public WithExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Target, InitializerPadded, Type);
    public WithExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Target, InitializerPadded, Type);
    public WithExpression WithTarget(Expression target) =>
        ReferenceEquals(target, Target) ? this : new(Id, Prefix, Markers, target, InitializerPadded, Type);
    public WithExpression WithInitializerPadded(JLeftPadded<Expression> initializerPadded) =>
        ReferenceEquals(initializerPadded, InitializerPadded) ? this : new(Id, Prefix, Markers, Target, initializerPadded, Type);
    public WithExpression WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Target, InitializerPadded, type);

    public Expression Initializer => InitializerPadded.Element;
    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(WithExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as WithExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- SpreadExpression ----

public sealed class SpreadExpression(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression,
    JavaType? type
) : Cs, Expression, IEquatable<SpreadExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;
    public JavaType? Type { get; } = type;

    public SpreadExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Type);
    public SpreadExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Type);
    public SpreadExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Type);
    public SpreadExpression WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Type);
    public SpreadExpression WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Expression, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SpreadExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SpreadExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

// ---- FunctionPointerType ----

public enum CallingConventionKind
{
    Managed,
    Unmanaged
}

public sealed class FunctionPointerType(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<CallingConventionKind>? callingConvention,
    JContainer<Identifier>? unmanagedCallingConventionTypes,
    JContainer<TypeTree> parameterTypes,
    JavaType? type
) : Cs, TypeTree, Expression, IEquatable<FunctionPointerType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<CallingConventionKind>? CallingConvention { get; } = callingConvention;
    public JContainer<Identifier>? UnmanagedCallingConventionTypes { get; } = unmanagedCallingConventionTypes;
    public JContainer<TypeTree> ParameterTypes { get; } = parameterTypes;
    public JavaType? Type { get; } = type;

    public FunctionPointerType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, CallingConvention, UnmanagedCallingConventionTypes, ParameterTypes, Type);
    public FunctionPointerType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, CallingConvention, UnmanagedCallingConventionTypes, ParameterTypes, Type);
    public FunctionPointerType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, CallingConvention, UnmanagedCallingConventionTypes, ParameterTypes, Type);
    public FunctionPointerType WithCallingConvention(JLeftPadded<CallingConventionKind>? callingConvention) =>
        ReferenceEquals(callingConvention, CallingConvention) ? this : new(Id, Prefix, Markers, callingConvention, UnmanagedCallingConventionTypes, ParameterTypes, Type);
    public FunctionPointerType WithUnmanagedCallingConventionTypes(JContainer<Identifier>? unmanagedCallingConventionTypes) =>
        ReferenceEquals(unmanagedCallingConventionTypes, UnmanagedCallingConventionTypes) ? this : new(Id, Prefix, Markers, CallingConvention, unmanagedCallingConventionTypes, ParameterTypes, Type);
    public FunctionPointerType WithParameterTypes(JContainer<TypeTree> parameterTypes) =>
        ReferenceEquals(parameterTypes, ParameterTypes) ? this : new(Id, Prefix, Markers, CallingConvention, UnmanagedCallingConventionTypes, parameterTypes, Type);
    public FunctionPointerType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, CallingConvention, UnmanagedCallingConventionTypes, ParameterTypes, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(FunctionPointerType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as FunctionPointerType);
    public override int GetHashCode() => Id.GetHashCode();
}

