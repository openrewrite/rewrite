using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Java;

/// <summary>
/// The base interface for all Java-family LST elements.
/// C# LST elements extend J where the syntax is isomorphic.
/// </summary>
public interface J : Tree
{
    Space Prefix { get; }
    Markers Markers { get; }
}

/// <summary>
/// Marker interface for expressions that produce a value.
/// </summary>
public interface Expression : J
{
}

/// <summary>
/// Marker interface for statements.
/// </summary>
public interface Statement : J
{
}

/// <summary>
/// Marker indicating null-safe navigation (?. operator).
/// Used by languages with safe navigation: C#, Kotlin, Groovy.
/// When present on a MethodInvocation.Name or FieldAccess.Name, prints ?. instead of .
/// </summary>
public sealed class NullSafe : Marker, IRpcCodec<NullSafe>, IEquatable<NullSafe>
{
    public Guid Id { get; }
    /// <summary>Whitespace between '?' and '.' when they are separated (e.g., by a newline).</summary>
    public Space DotPrefix { get; }

    public NullSafe(Guid id, Space? dotPrefix = null)
    {
        Id = id;
        DotPrefix = dotPrefix ?? Space.Empty;
    }

    public NullSafe WithId(Guid id) =>
        id == Id ? this : new(id, DotPrefix);
    public NullSafe WithDotPrefix(Space dotPrefix) =>
        ReferenceEquals(dotPrefix, DotPrefix) ? this : new(Id, dotPrefix);

    public static NullSafe Instance { get; } = new(Guid.Empty);

    public void RpcSend(NullSafe after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.DotPrefix, s => new Rpc.JavaSender().VisitSpace(s, q));
    }

    public NullSafe RpcReceive(NullSafe before, RpcReceiveQueue q) =>
        before
            .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
            .WithDotPrefix(q.Receive(before.DotPrefix, s => new Rpc.JavaReceiver().VisitSpace(s, q)));

    public bool Equals(NullSafe? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NullSafe);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating parentheses should be omitted in NewClass.
/// Used for object initializers without constructor arguments: new Foo { X = 1 }
/// </summary>
public sealed class OmitParentheses(
    Guid id
) : Marker, IRpcCodec<OmitParentheses>, IEquatable<OmitParentheses>
{
    public Guid Id { get; } = id;

    public OmitParentheses WithId(Guid id) =>
        id == Id ? this : new(id);

    public static OmitParentheses Instance { get; } = new(Guid.Empty);
    public void RpcSend(OmitParentheses after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public OmitParentheses RpcReceive(OmitParentheses before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(OmitParentheses? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as OmitParentheses);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker on a <see cref="Binary"/> indicating it was produced from a C# pattern combinator
/// (<c>and</c>/<c>or</c> keywords) rather than a logical operator (<c>&amp;&amp;</c>/<c>||</c>).
/// </summary>
public sealed class PatternCombinator(Guid id)
    : Marker, IRpcCodec<PatternCombinator>, IEquatable<PatternCombinator>
{
    public Guid Id { get; } = id;

    public PatternCombinator WithId(Guid id) =>
        id == Id ? this : new(id);

    public static PatternCombinator Instance { get; } = new(Guid.Empty);
    public void RpcSend(PatternCombinator after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public PatternCombinator RpcReceive(PatternCombinator before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(PatternCombinator? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as PatternCombinator);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker on a <see cref="ConstrainedTypeParameter"/> that records the source-order index
/// of its <c>where</c> clause, so the printer can output constraints in source order
/// rather than type-parameter declaration order.
/// </summary>
public sealed class WhereClauseOrder : Marker, IRpcCodec<WhereClauseOrder>, IEquatable<WhereClauseOrder>
{
    public Guid Id { get; }
    public int Order { get; }

    public WhereClauseOrder(Guid id, int order)
    {
        Id = id;
        Order = order;
    }

    public WhereClauseOrder WithId(Guid id) =>
        id == Id ? this : new(id, Order);

    public WhereClauseOrder WithOrder(int order) =>
        order == Order ? this : new(Id, order);

    public void RpcSend(WhereClauseOrder after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.Order);
    }

    public WhereClauseOrder RpcReceive(WhereClauseOrder before, RpcReceiveQueue q) =>
        before
            .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
            .WithOrder(q.Receive(before.Order));

    public bool Equals(WhereClauseOrder? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as WhereClauseOrder);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker on an <see cref="ArrayDimension"/> indicating it is a continuation within the same
/// rank specifier (e.g., the second dimension of <c>new int[n, m]</c>).
/// The printer uses this to emit a comma instead of opening a new bracket pair.
/// </summary>
public sealed class MultiDimensionContinuation(Guid id)
    : Marker, IRpcCodec<MultiDimensionContinuation>, IEquatable<MultiDimensionContinuation>
{
    public Guid Id { get; } = id;

    public MultiDimensionContinuation WithId(Guid id) =>
        id == Id ? this : new(id);

    public static MultiDimensionContinuation Instance { get; } = new(Guid.Empty);
    public void RpcSend(MultiDimensionContinuation after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public MultiDimensionContinuation RpcReceive(MultiDimensionContinuation before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(MultiDimensionContinuation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MultiDimensionContinuation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Marker indicating a semicolon-terminated element.
/// Used for positional records (record Person(string Name);) and other contexts
/// where a semicolon replaces the expected body/braces.
/// </summary>
public sealed class Semicolon(
    Guid id
) : Marker, IRpcCodec<Semicolon>, IEquatable<Semicolon>
{
    public Guid Id { get; } = id;

    public Semicolon WithId(Guid id) =>
        id == Id ? this : new(id);

    public void RpcSend(Semicolon after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public Semicolon RpcReceive(Semicolon before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));

    public bool Equals(Semicolon? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Semicolon);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A package/namespace declaration.
/// Java: package com.example;
/// C#: namespace MyApp.Services; (file-scoped)
/// </summary>
public sealed class Package(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expression,
    IList<Annotation> annotations
) : J, Statement, IEquatable<Package>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Expression { get; } = expression;
    public IList<Annotation> Annotations { get; } = annotations;

    public Package WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Annotations);
    public Package WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Annotations);
    public Package WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Annotations);
    public Package WithExpression(JRightPadded<Expression> expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Annotations);
    public Package WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, Expression, annotations);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Package? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Package);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A literal value (string, number, boolean, char, null).
/// </summary>
public sealed class Literal(
    Guid id,
    Space prefix,
    Markers markers,
    object? value,
    string? valueSource,
    IList<Literal.UnicodeEscape>? unicodeEscapes,
    JavaType.Primitive? type
) : J, Expression, IEquatable<Literal>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public object? Value { get; } = value;
    public string? ValueSource { get; } = valueSource;
    public IList<Literal.UnicodeEscape>? UnicodeEscapes { get; } = unicodeEscapes;
    public JavaType.Primitive? Type { get; } = type;

    public Literal WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Value, ValueSource, UnicodeEscapes, Type);
    public Literal WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Value, ValueSource, UnicodeEscapes, Type);
    public Literal WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Value, ValueSource, UnicodeEscapes, Type);
    public Literal WithValue(object? value) =>
        ReferenceEquals(value, Value) ? this : new(Id, Prefix, Markers, value, ValueSource, UnicodeEscapes, Type);
    public Literal WithValueSource(string? valueSource) =>
        string.Equals(valueSource, ValueSource, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, Value, valueSource, UnicodeEscapes, Type);
    public Literal WithUnicodeEscapes(IList<Literal.UnicodeEscape>? unicodeEscapes) =>
        ReferenceEquals(unicodeEscapes, UnicodeEscapes) ? this : new(Id, Prefix, Markers, Value, ValueSource, unicodeEscapes, Type);
    public Literal WithType(JavaType.Primitive? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Value, ValueSource, UnicodeEscapes, type);

    public record UnicodeEscape(int ValueSourceIndex, string CodePoint);


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Literal? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Literal);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An expression used as a statement.
/// </summary>
public sealed class ExpressionStatement(
    Guid id,
    Expression expression
) : J, Statement, IEquatable<ExpressionStatement>
{
    public Guid Id { get; } = id;
    public Expression Expression { get; } = expression;

    public ExpressionStatement WithId(Guid id) =>
        id == Id ? this : new(id, Expression);
    public ExpressionStatement WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, expression);

    // ExpressionStatement delegates prefix/markers to its expression
    public Space Prefix => Expression.Prefix;
    public Markers Markers => Expression.Markers;


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ExpressionStatement? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ExpressionStatement);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A block of statements enclosed in braces { }.
/// </summary>
public sealed class Block(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<bool> @static,
    IList<JRightPadded<Statement>> statements,
    Space end
) : J, Statement, IEquatable<Block>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<bool> Static { get; } = @static;
    public IList<JRightPadded<Statement>> Statements { get; } = statements;
    public Space End { get; } = end;

    public Block WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Static, Statements, End);
    public Block WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Static, Statements, End);
    public Block WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Static, Statements, End);
    public Block WithStatic(JRightPadded<bool> @static) =>
        ReferenceEquals(@static, Static) ? this : new(Id, Prefix, Markers, @static, Statements, End);
    public Block WithStatements(IList<JRightPadded<Statement>> statements) =>
        ReferenceEquals(statements, Statements) ? this : new(Id, Prefix, Markers, Static, statements, End);
    public Block WithEnd(Space end) =>
        ReferenceEquals(end, End) ? this : new(Id, Prefix, Markers, Static, Statements, end);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Block? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Block);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A return statement (e.g., return 1;).
/// </summary>
public sealed class Return(
    Guid id,
    Space prefix,
    Markers markers,
    Expression? expression
) : J, Statement, IEquatable<Return>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression? Expression { get; } = expression;

    public Return WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression);
    public Return WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression);
    public Return WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression);
    public Return WithExpression(Expression? expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Return? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Return);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An if statement (e.g., if (condition) { } else { }).
/// </summary>
public sealed class If(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<Expression> condition,
    JRightPadded<Statement> thenPart,
    If.Else? elsePart
) : J, Statement, IEquatable<If>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<Expression> Condition { get; } = condition;
    public JRightPadded<Statement> ThenPart { get; } = thenPart;
    public If.Else? ElsePart { get; } = elsePart;

    public If WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Condition, ThenPart, ElsePart);
    public If WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Condition, ThenPart, ElsePart);
    public If WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Condition, ThenPart, ElsePart);
    public If WithCondition(ControlParentheses<Expression> condition) =>
        ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, condition, ThenPart, ElsePart);
    public If WithThenPart(JRightPadded<Statement> thenPart) =>
        ReferenceEquals(thenPart, ThenPart) ? this : new(Id, Prefix, Markers, Condition, thenPart, ElsePart);
    public If WithElsePart(If.Else? elsePart) =>
        ReferenceEquals(elsePart, ElsePart) ? this : new(Id, Prefix, Markers, Condition, ThenPart, elsePart);

        public sealed class Else(
        Guid id,
        Space prefix,
        Markers markers,
        JRightPadded<Statement> body
    ) : J, IEquatable<Else>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public JRightPadded<Statement> Body { get; } = body;

        public Else WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Body);
        public Else WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Body);
        public Else WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Body);
        public Else WithBody(JRightPadded<Statement> body) =>
            ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, body);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Else? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Else);
        public override int GetHashCode() => Id.GetHashCode();
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(If? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as If);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Parentheses used in control structures (if, while, etc.).
/// </summary>
public sealed class ControlParentheses<T>(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<T> tree
) : J, Expression, IEquatable<ControlParentheses<T>> where T : J
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<T> Tree { get; } = tree;

    public ControlParentheses<T> WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Tree);
    public ControlParentheses<T> WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Tree);
    public ControlParentheses<T> WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Tree);
    public ControlParentheses<T> WithTree(JRightPadded<T> tree) =>
        ReferenceEquals(tree, Tree) ? this : new(Id, Prefix, Markers, tree);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ControlParentheses<T>? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ControlParentheses<T>);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A for loop (e.g., for (int i = 0; i &lt; 10; i++) { }).
/// </summary>
public sealed class ForLoop(
    Guid id,
    Space prefix,
    Markers markers,
    ForLoop.Control loopControl,
    JRightPadded<Statement> body
) : J, Statement, IEquatable<ForLoop>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ForLoop.Control LoopControl { get; } = loopControl;
    public JRightPadded<Statement> Body { get; } = body;

    public ForLoop WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LoopControl, Body);
    public ForLoop WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LoopControl, Body);
    public ForLoop WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LoopControl, Body);
    public ForLoop WithLoopControl(ForLoop.Control loopControl) =>
        ReferenceEquals(loopControl, LoopControl) ? this : new(Id, Prefix, Markers, loopControl, Body);
    public ForLoop WithBody(JRightPadded<Statement> body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, LoopControl, body);

        public sealed class Control(
        Guid id,
        Space prefix,
        Markers markers,
        IList<JRightPadded<Statement>> init,
        JRightPadded<Expression> condition,
        IList<JRightPadded<Statement>> update
    ) : J, IEquatable<Control>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public IList<JRightPadded<Statement>> Init { get; } = init;
        public JRightPadded<Expression> Condition { get; } = condition;
        public IList<JRightPadded<Statement>> Update { get; } = update;

        public Control WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Init, Condition, Update);
        public Control WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Init, Condition, Update);
        public Control WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Init, Condition, Update);
        public Control WithInit(IList<JRightPadded<Statement>> init) =>
            ReferenceEquals(init, Init) ? this : new(Id, Prefix, Markers, init, Condition, Update);
        public Control WithCondition(JRightPadded<Expression> condition) =>
            ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, Init, condition, Update);
        public Control WithUpdate(IList<JRightPadded<Statement>> update) =>
            ReferenceEquals(update, Update) ? this : new(Id, Prefix, Markers, Init, Condition, update);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Control? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Control);
        public override int GetHashCode() => Id.GetHashCode();
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ForLoop? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ForLoop);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A foreach loop (e.g., foreach (var x in items) { }).
/// </summary>
public sealed class ForEachLoop(
    Guid id,
    Space prefix,
    Markers markers,
    ForEachLoop.Control loopControl,
    JRightPadded<Statement> body
) : J, Statement, IEquatable<ForEachLoop>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ForEachLoop.Control LoopControl { get; } = loopControl;
    public JRightPadded<Statement> Body { get; } = body;

    public ForEachLoop WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LoopControl, Body);
    public ForEachLoop WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LoopControl, Body);
    public ForEachLoop WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LoopControl, Body);
    public ForEachLoop WithLoopControl(ForEachLoop.Control loopControl) =>
        ReferenceEquals(loopControl, LoopControl) ? this : new(Id, Prefix, Markers, loopControl, Body);
    public ForEachLoop WithBody(JRightPadded<Statement> body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, LoopControl, body);

        public sealed class Control(
        Guid id,
        Space prefix,
        Markers markers,
        JRightPadded<VariableDeclarations> variable,
        JRightPadded<Expression> iterable
    ) : J, IEquatable<Control>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public JRightPadded<VariableDeclarations> Variable { get; } = variable;
        public JRightPadded<Expression> Iterable { get; } = iterable;

        public Control WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Variable, Iterable);
        public Control WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Variable, Iterable);
        public Control WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Variable, Iterable);
        public Control WithVariable(JRightPadded<VariableDeclarations> variable) =>
            ReferenceEquals(variable, Variable) ? this : new(Id, Prefix, Markers, variable, Iterable);
        public Control WithIterable(JRightPadded<Expression> iterable) =>
            ReferenceEquals(iterable, Iterable) ? this : new(Id, Prefix, Markers, Variable, iterable);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Control? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Control);
        public override int GetHashCode() => Id.GetHashCode();
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ForEachLoop? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ForEachLoop);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A while loop (e.g., while (condition) { }).
/// </summary>
public sealed class WhileLoop(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<Expression> condition,
    JRightPadded<Statement> body
) : J, Statement, IEquatable<WhileLoop>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<Expression> Condition { get; } = condition;
    public JRightPadded<Statement> Body { get; } = body;

    public WhileLoop WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Condition, Body);
    public WhileLoop WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Condition, Body);
    public WhileLoop WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Condition, Body);
    public WhileLoop WithCondition(ControlParentheses<Expression> condition) =>
        ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, condition, Body);
    public WhileLoop WithBody(JRightPadded<Statement> body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Condition, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(WhileLoop? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as WhileLoop);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A do-while loop (e.g., do { } while (condition);).
/// </summary>
public sealed class DoWhileLoop(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Statement> body,
    JLeftPadded<ControlParentheses<Expression>> condition
) : J, Statement, IEquatable<DoWhileLoop>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Statement> Body { get; } = body;
    public JLeftPadded<ControlParentheses<Expression>> Condition { get; } = condition;

    public DoWhileLoop WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Body, Condition);
    public DoWhileLoop WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Body, Condition);
    public DoWhileLoop WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Body, Condition);
    public DoWhileLoop WithBody(JRightPadded<Statement> body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, body, Condition);
    public DoWhileLoop WithCondition(JLeftPadded<ControlParentheses<Expression>> condition) =>
        ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, Body, condition);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DoWhileLoop? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DoWhileLoop);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A synchronized/lock block statement.
/// In Java: synchronized (lock) { body }
/// In C#: lock (lock) { body }
/// </summary>
public sealed class Synchronized(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<Expression> @lock,
    Block body
) : J, Statement, IEquatable<Synchronized>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<Expression> Lock { get; } = @lock;
    public Block Body { get; } = body;

    public Synchronized WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Lock, Body);
    public Synchronized WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Lock, Body);
    public Synchronized WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Lock, Body);
    public Synchronized WithLock(ControlParentheses<Expression> @lock) =>
        ReferenceEquals(@lock, Lock) ? this : new(Id, Prefix, Markers, @lock, Body);
    public Synchronized WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Lock, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Synchronized? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Synchronized);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A try statement (e.g., try { } catch { } finally { }).
/// </summary>
public sealed class Try(
    Guid id,
    Space prefix,
    Markers markers,
    JContainer<NameTree>? resources,
    Block body,
    IList<Try.Catch> catches,
    JLeftPadded<Block>? @finally
) : J, Statement, IEquatable<Try>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JContainer<NameTree>? Resources { get; } = resources;
    public Block Body { get; } = body;
    public IList<Try.Catch> Catches { get; } = catches;
    public JLeftPadded<Block>? Finally { get; } = @finally;

    public Try WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Resources, Body, Catches, Finally);
    public Try WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Resources, Body, Catches, Finally);
    public Try WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Resources, Body, Catches, Finally);
    public Try WithResources(JContainer<NameTree>? resources) =>
        ReferenceEquals(resources, Resources) ? this : new(Id, Prefix, Markers, resources, Body, Catches, Finally);
    public Try WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Resources, body, Catches, Finally);
    public Try WithCatches(IList<Try.Catch> catches) =>
        ReferenceEquals(catches, Catches) ? this : new(Id, Prefix, Markers, Resources, Body, catches, Finally);
    public Try WithFinally(JLeftPadded<Block>? @finally) =>
        ReferenceEquals(@finally, Finally) ? this : new(Id, Prefix, Markers, Resources, Body, Catches, @finally);

        public sealed class Catch(
        Guid id,
        Space prefix,
        Markers markers,
        ControlParentheses<VariableDeclarations> parameter,
        Block body
    ) : J, IEquatable<Catch>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public ControlParentheses<VariableDeclarations> Parameter { get; } = parameter;
        public Block Body { get; } = body;

        public Catch WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Parameter, Body);
        public Catch WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Parameter, Body);
        public Catch WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Parameter, Body);
        public Catch WithParameter(ControlParentheses<VariableDeclarations> parameter) =>
            ReferenceEquals(parameter, Parameter) ? this : new(Id, Prefix, Markers, parameter, Body);
        public Catch WithBody(Block body) =>
            ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Parameter, body);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Catch? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Catch);
        public override int GetHashCode() => Id.GetHashCode();
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Try? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Try);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A throw statement (e.g., throw new Exception();).
/// </summary>
public sealed class Throw(
    Guid id,
    Space prefix,
    Markers markers,
    Expression exception
) : J, Statement, IEquatable<Throw>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Exception { get; } = exception;

    public Throw WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Exception);
    public Throw WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Exception);
    public Throw WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Exception);
    public Throw WithException(Expression exception) =>
        ReferenceEquals(exception, Exception) ? this : new(Id, Prefix, Markers, exception);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Throw? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Throw);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A break statement.
/// </summary>
public sealed class Break(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier? label
) : J, Statement, IEquatable<Break>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier? Label { get; } = label;

    public Break WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Label);
    public Break WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Label);
    public Break WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Label);
    public Break WithLabel(Identifier? label) =>
        ReferenceEquals(label, Label) ? this : new(Id, Prefix, Markers, label);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Break? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Break);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A continue statement.
/// </summary>
public sealed class Continue(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier? label
) : J, Statement, IEquatable<Continue>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier? Label { get; } = label;

    public Continue WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Label);
    public Continue WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Label);
    public Continue WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Label);
    public Continue WithLabel(Identifier? label) =>
        ReferenceEquals(label, Label) ? this : new(Id, Prefix, Markers, label);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Continue? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Continue);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An empty statement (just a semicolon).
/// </summary>
public sealed class Empty(
    Guid id,
    Space prefix,
    Markers markers
) : J, Statement, Expression, IEquatable<Empty>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;

    public Empty WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers);
    public Empty WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers);
    public Empty WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Empty? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Empty);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A class, struct, interface, or record declaration.
/// </summary>
public sealed class ClassDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> leadingAnnotations,
    IList<Modifier> modifiers,
    ClassDeclaration.Kind classKind,
    Identifier name,
    JContainer<TypeParameter>? typeParameters,
    JContainer<TypeTree>? primaryConstructor,
    JLeftPadded<TypeTree>? extends,
    JContainer<TypeTree>? implements,
    JContainer<TypeTree>? permits,
    Block body,
    JavaType.FullyQualified? type
) : J, Statement, IEquatable<ClassDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> LeadingAnnotations { get; } = leadingAnnotations;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public ClassDeclaration.Kind ClassKind { get; } = classKind;
    public Identifier Name { get; } = name;
    public JContainer<TypeParameter>? TypeParameters { get; } = typeParameters;
    public JContainer<TypeTree>? PrimaryConstructor { get; } = primaryConstructor;
    public JLeftPadded<TypeTree>? Extends { get; } = extends;
    public JContainer<TypeTree>? Implements { get; } = implements;
    public JContainer<TypeTree>? Permits { get; } = permits;
    public Block Body { get; } = body;
    public JavaType.FullyQualified? Type { get; } = type;

    public ClassDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithLeadingAnnotations(IList<Annotation> leadingAnnotations) =>
        ReferenceEquals(leadingAnnotations, LeadingAnnotations) ? this : new(Id, Prefix, Markers, leadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, LeadingAnnotations, modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithClassKind(ClassDeclaration.Kind classKind) =>
        ReferenceEquals(classKind, ClassKind) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, classKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithTypeParameters(JContainer<TypeParameter>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, typeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithPrimaryConstructor(JContainer<TypeTree>? primaryConstructor) =>
        ReferenceEquals(primaryConstructor, PrimaryConstructor) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, primaryConstructor, Extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithExtends(JLeftPadded<TypeTree>? extends) =>
        ReferenceEquals(extends, Extends) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, extends, Implements, Permits, Body, Type);
    public ClassDeclaration WithImplements(JContainer<TypeTree>? implements) =>
        ReferenceEquals(implements, Implements) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, implements, Permits, Body, Type);
    public ClassDeclaration WithPermits(JContainer<TypeTree>? permits) =>
        ReferenceEquals(permits, Permits) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, permits, Body, Type);
    public ClassDeclaration WithBody(Block body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, body, Type);
    public ClassDeclaration WithType(JavaType.FullyQualified? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, ClassKind, Name, TypeParameters, PrimaryConstructor, Extends, Implements, Permits, Body, type);

    public enum KindType { Class, Enum, Interface, Annotation, Record, Value }

        public sealed class Kind(
        Guid id,
        Space prefix,
        Markers markers,
        IList<Annotation> annotations,
        KindType type
    ) : J, IEquatable<Kind>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public IList<Annotation> Annotations { get; } = annotations;
        public KindType Type { get; } = type;

        public Kind WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Annotations, Type);
        public Kind WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Annotations, Type);
        public Kind WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Annotations, Type);
        public Kind WithAnnotations(IList<Annotation> annotations) =>
            ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, annotations, Type);
        public Kind WithType(KindType type) =>
            type == Type ? this : new(Id, Prefix, Markers, Annotations, type);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Kind? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Kind);
        public override int GetHashCode() => Id.GetHashCode();
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ClassDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ClassDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A single enum constant/value.
/// Examples:
///   Red
///   Green = 1
///   Blue = 2
/// </summary>
public sealed class EnumValue(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> annotations,
    Identifier name,
    JLeftPadded<Expression>? initializer
) : J, IEquatable<EnumValue>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> Annotations { get; } = annotations;
    public Identifier Name { get; } = name;
    public JLeftPadded<Expression>? Initializer { get; } = initializer;

    public EnumValue WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Annotations, Name, Initializer);
    public EnumValue WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Annotations, Name, Initializer);
    public EnumValue WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Annotations, Name, Initializer);
    public EnumValue WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, annotations, Name, Initializer);
    public EnumValue WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Annotations, name, Initializer);
    public EnumValue WithInitializer(JLeftPadded<Expression>? initializer) =>
        ReferenceEquals(initializer, Initializer) ? this : new(Id, Prefix, Markers, Annotations, Name, initializer);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EnumValue? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EnumValue);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// The set of enum values in an enum declaration.
/// Example:
///   Red, Green = 1, Blue
/// </summary>
public sealed class EnumValueSet(
    Guid id,
    Space prefix,
    Markers markers,
    IList<JRightPadded<EnumValue>> enums,
    bool terminatedWithSemicolon
) : J, Statement, IEquatable<EnumValueSet>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<JRightPadded<EnumValue>> Enums { get; } = enums;
    public bool TerminatedWithSemicolon { get; } = terminatedWithSemicolon;

    public EnumValueSet WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Enums, TerminatedWithSemicolon);
    public EnumValueSet WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Enums, TerminatedWithSemicolon);
    public EnumValueSet WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Enums, TerminatedWithSemicolon);
    public EnumValueSet WithEnums(IList<JRightPadded<EnumValue>> enums) =>
        ReferenceEquals(enums, Enums) ? this : new(Id, Prefix, Markers, enums, TerminatedWithSemicolon);
    public EnumValueSet WithTerminatedWithSemicolon(bool terminatedWithSemicolon) =>
        terminatedWithSemicolon == TerminatedWithSemicolon ? this : new(Id, Prefix, Markers, Enums, terminatedWithSemicolon);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(EnumValueSet? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as EnumValueSet);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A method declaration (e.g., void Bar() { }).
/// </summary>
public sealed class MethodDeclaration(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> leadingAnnotations,
    IList<Modifier> modifiers,
    JContainer<TypeParameter>? typeParameters,
    TypeTree? returnTypeExpression,
    Identifier name,
    JContainer<Statement> parameters,
    JContainer<NameTree>? throws,
    Block? body,
    JLeftPadded<Expression>? defaultValue,
    JavaType.Method? methodType
) : J, Statement, IEquatable<MethodDeclaration>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> LeadingAnnotations { get; } = leadingAnnotations;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public JContainer<TypeParameter>? TypeParameters { get; } = typeParameters;
    public TypeTree? ReturnTypeExpression { get; } = returnTypeExpression;
    public Identifier Name { get; } = name;
    public JContainer<Statement> Parameters { get; } = parameters;
    public JContainer<NameTree>? Throws { get; } = throws;
    public Block? Body { get; } = body;
    public JLeftPadded<Expression>? DefaultValue { get; } = defaultValue;
    public JavaType.Method? MethodType { get; } = methodType;

    public MethodDeclaration WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithLeadingAnnotations(IList<Annotation> leadingAnnotations) =>
        ReferenceEquals(leadingAnnotations, LeadingAnnotations) ? this : new(Id, Prefix, Markers, leadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, LeadingAnnotations, modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithTypeParameters(JContainer<TypeParameter>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, typeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithReturnTypeExpression(TypeTree? returnTypeExpression) =>
        ReferenceEquals(returnTypeExpression, ReturnTypeExpression) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, returnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, name, Parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithParameters(JContainer<Statement> parameters) =>
        ReferenceEquals(parameters, Parameters) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, parameters, Throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithThrows(JContainer<NameTree>? throws) =>
        ReferenceEquals(throws, Throws) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, throws, Body, DefaultValue, MethodType);
    public MethodDeclaration WithBody(Block? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, body, DefaultValue, MethodType);
    public MethodDeclaration WithDefaultValue(JLeftPadded<Expression>? defaultValue) =>
        ReferenceEquals(defaultValue, DefaultValue) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, defaultValue, MethodType);
    public MethodDeclaration WithMethodType(JavaType.Method? methodType) =>
        ReferenceEquals(methodType, MethodType) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeParameters, ReturnTypeExpression, Name, Parameters, Throws, Body, DefaultValue, methodType);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(MethodDeclaration? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MethodDeclaration);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A type parameter in a generic declaration (e.g., T in class Foo&lt;T&gt;).
/// </summary>
public sealed class TypeParameter(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> annotations,
    IList<Modifier> modifiers,
    Expression name,
    JContainer<TypeTree>? bounds
) : J, IEquatable<TypeParameter>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> Annotations { get; } = annotations;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public Expression Name { get; } = name;
    public JContainer<TypeTree>? Bounds { get; } = bounds;

    public TypeParameter WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Annotations, Modifiers, Name, Bounds);
    public TypeParameter WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Annotations, Modifiers, Name, Bounds);
    public TypeParameter WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Annotations, Modifiers, Name, Bounds);
    public TypeParameter WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, annotations, Modifiers, Name, Bounds);
    public TypeParameter WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, Annotations, modifiers, Name, Bounds);
    public TypeParameter WithName(Expression name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Annotations, Modifiers, name, Bounds);
    public TypeParameter WithBounds(JContainer<TypeTree>? bounds) =>
        ReferenceEquals(bounds, Bounds) ? this : new(Id, Prefix, Markers, Annotations, Modifiers, Name, bounds);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(TypeParameter? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TypeParameter);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// Wrapper for method type parameters that matches Java's J.TypeParameters tree node.
/// On the Java side, MethodDeclaration stores type parameters wrapped in J.TypeParameters
/// (a full tree node with id/prefix/markers/annotations), while C# stores them as a raw JContainer.
/// This class bridges the RPC protocol gap.
/// </summary>
public sealed class TypeParameters(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> annotations,
    IList<JRightPadded<TypeParameter>> typeParameters
) : J, IEquatable<TypeParameters>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> Annotations { get; } = annotations;
    public IList<JRightPadded<TypeParameter>> Params { get; } = typeParameters;

    public TypeParameters WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Annotations, Params);
    public TypeParameters WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Annotations, Params);
    public TypeParameters WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Annotations, Params);
    public TypeParameters WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, annotations, Params);
    public TypeParameters WithParams(IList<JRightPadded<TypeParameter>> typeParameters) =>
        ReferenceEquals(typeParameters, Params) ? this : new(Id, Prefix, Markers, Annotations, typeParameters);

    Tree Tree.WithId(Guid id) => WithId(id);

    /// <summary>
    /// Create a TypeParameters wrapper from a JContainer (for RPC sending).
    /// </summary>
    public static TypeParameters FromContainer(JContainer<TypeParameter> container) =>
        new(Guid.NewGuid(), container.Before, Markers.Empty, [], container.Elements);

    /// <summary>
    /// Convert back to a JContainer (for RPC receiving).
    /// </summary>
    public JContainer<TypeParameter> ToContainer() =>
        new(Prefix, Params, Markers.Empty);

    public bool Equals(TypeParameters? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TypeParameters);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An identifier (variable name, type name, etc.).
/// </summary>
public sealed class Identifier(
    Guid id,
    Space prefix,
    Markers markers,
    string simpleName,
    JavaType? type
) : J, Expression, TypeTree, IEquatable<Identifier>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public string SimpleName { get; } = simpleName;
    public JavaType? Type { get; } = type;

    public Identifier WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, SimpleName, Type);
    public Identifier WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, SimpleName, Type);
    public Identifier WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, SimpleName, Type);
    public Identifier WithSimpleName(string simpleName) =>
        string.Equals(simpleName, SimpleName, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, simpleName, Type);
    public Identifier WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, SimpleName, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Identifier? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Identifier);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A field access (qualified name), e.g., System.Console or obj.field.
/// </summary>
public sealed class FieldAccess(
    Guid id,
    Space prefix,
    Markers markers,
    Expression target,
    JLeftPadded<Identifier> name,
    JavaType? type
) : J, TypeTree, Expression, Statement, IEquatable<FieldAccess>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Target { get; } = target;
    public JLeftPadded<Identifier> Name { get; } = name;
    public JavaType? Type { get; } = type;

    public FieldAccess WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Target, Name, Type);
    public FieldAccess WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Target, Name, Type);
    public FieldAccess WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Target, Name, Type);
    public FieldAccess WithTarget(Expression target) =>
        ReferenceEquals(target, Target) ? this : new(Id, Prefix, Markers, target, Name, Type);
    public FieldAccess WithName(JLeftPadded<Identifier> name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Target, name, Type);
    public FieldAccess WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Target, Name, type);

    public Identifier GetName() => Name.Element;


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(FieldAccess? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as FieldAccess);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A member reference expression (e.g., obj.Method&lt;T&gt; used as a method group reference).
/// In Java, this represents method references like obj::method.
/// In C#, this represents generic method group references like obj.Method&lt;T&gt;.
/// </summary>
public sealed class MemberReference(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> containing,
    JContainer<Expression>? typeParameters,
    JLeftPadded<Identifier> reference,
    JavaType? type,
    JavaType.Method? methodType,
    JavaType.Variable? variableType
) : J, Expression, IEquatable<MemberReference>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Containing { get; } = containing;
    public JContainer<Expression>? TypeParameters { get; } = typeParameters;
    public JLeftPadded<Identifier> Reference { get; } = reference;
    public JavaType? Type { get; } = type;
    public JavaType.Method? MethodType { get; } = methodType;
    public JavaType.Variable? VariableType { get; } = variableType;

    public MemberReference WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Containing, TypeParameters, Reference, Type, MethodType, VariableType);
    public MemberReference WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Containing, TypeParameters, Reference, Type, MethodType, VariableType);
    public MemberReference WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Containing, TypeParameters, Reference, Type, MethodType, VariableType);
    public MemberReference WithContaining(JRightPadded<Expression> containing) =>
        ReferenceEquals(containing, Containing) ? this : new(Id, Prefix, Markers, containing, TypeParameters, Reference, Type, MethodType, VariableType);
    public MemberReference WithTypeParameters(JContainer<Expression>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, Containing, typeParameters, Reference, Type, MethodType, VariableType);
    public MemberReference WithReference(JLeftPadded<Identifier> reference) =>
        ReferenceEquals(reference, Reference) ? this : new(Id, Prefix, Markers, Containing, TypeParameters, reference, Type, MethodType, VariableType);
    public MemberReference WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Containing, TypeParameters, Reference, type, MethodType, VariableType);
    public MemberReference WithMethodType(JavaType.Method? methodType) =>
        ReferenceEquals(methodType, MethodType) ? this : new(Id, Prefix, Markers, Containing, TypeParameters, Reference, Type, methodType, VariableType);
    public MemberReference WithVariableType(JavaType.Variable? variableType) =>
        ReferenceEquals(variableType, VariableType) ? this : new(Id, Prefix, Markers, Containing, TypeParameters, Reference, Type, MethodType, variableType);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(MemberReference? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MemberReference);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An assignment expression (e.g., x = 1, x += 2).
/// </summary>
public sealed class Assignment(
    Guid id,
    Space prefix,
    Markers markers,
    Expression variable,
    JLeftPadded<Expression> assignmentValue,
    JavaType? type
) : J, Statement, Expression, IEquatable<Assignment>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Variable { get; } = variable;
    public JLeftPadded<Expression> AssignmentValue { get; } = assignmentValue;
    public JavaType? Type { get; } = type;

    public Assignment WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Variable, AssignmentValue, Type);
    public Assignment WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Variable, AssignmentValue, Type);
    public Assignment WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Variable, AssignmentValue, Type);
    public Assignment WithVariable(Expression variable) =>
        ReferenceEquals(variable, Variable) ? this : new(Id, Prefix, Markers, variable, AssignmentValue, Type);
    public Assignment WithAssignmentValue(JLeftPadded<Expression> assignmentValue) =>
        ReferenceEquals(assignmentValue, AssignmentValue) ? this : new(Id, Prefix, Markers, Variable, assignmentValue, Type);
    public Assignment WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Variable, AssignmentValue, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Assignment? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Assignment);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A compound assignment expression with operator (e.g., x += 1, x -= 2).
/// </summary>
public sealed class AssignmentOperation(
    Guid id,
    Space prefix,
    Markers markers,
    Expression variable,
    JLeftPadded<AssignmentOperation.OperatorType> @operator,
    Expression assignmentValue,
    JavaType? type
) : J, Statement, Expression, IEquatable<AssignmentOperation>
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
        Coalesce  // ??= in C#
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(AssignmentOperation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as AssignmentOperation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A labeled statement (e.g., myLabel: statement).
/// </summary>
public sealed class Label(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> labelName,
    Statement statement
) : J, Statement, IEquatable<Label>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> LabelName { get; } = labelName;
    public Statement Statement { get; } = statement;

    public Label WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LabelName, Statement);
    public Label WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LabelName, Statement);
    public Label WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LabelName, Statement);
    public Label WithLabelName(JRightPadded<Identifier> labelName) =>
        ReferenceEquals(labelName, LabelName) ? this : new(Id, Prefix, Markers, labelName, Statement);
    public Label WithStatement(Statement statement) =>
        ReferenceEquals(statement, Statement) ? this : new(Id, Prefix, Markers, LabelName, statement);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Label? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Label);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An instanceof expression. Also used for C# typeof(T) where expression is J.Empty.
/// </summary>
public sealed class InstanceOf(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expression,
    J clazz,
    J? pattern,
    JavaType? type,
    Modifier? instanceOfModifier
) : J, Expression, IEquatable<InstanceOf>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Expression { get; } = expression;
    public J Clazz { get; } = clazz;
    public J? Pattern { get; } = pattern;
    public JavaType? Type { get; } = type;
    public Modifier? InstanceOfModifier { get; } = instanceOfModifier;

    public InstanceOf WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression, Clazz, Pattern, Type, InstanceOfModifier);
    public InstanceOf WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression, Clazz, Pattern, Type, InstanceOfModifier);
    public InstanceOf WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression, Clazz, Pattern, Type, InstanceOfModifier);
    public InstanceOf WithExpression(JRightPadded<Expression> expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression, Clazz, Pattern, Type, InstanceOfModifier);
    public InstanceOf WithClazz(J clazz) =>
        ReferenceEquals(clazz, Clazz) ? this : new(Id, Prefix, Markers, Expression, clazz, Pattern, Type, InstanceOfModifier);
    public InstanceOf WithPattern(J? pattern) =>
        ReferenceEquals(pattern, Pattern) ? this : new(Id, Prefix, Markers, Expression, Clazz, pattern, Type, InstanceOfModifier);
    public InstanceOf WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Expression, Clazz, Pattern, type, InstanceOfModifier);
    public InstanceOf WithInstanceOfModifier(Modifier? instanceOfModifier) =>
        ReferenceEquals(instanceOfModifier, InstanceOfModifier) ? this : new(Id, Prefix, Markers, Expression, Clazz, Pattern, Type, instanceOfModifier);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(InstanceOf? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as InstanceOf);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A method invocation (e.g., foo.Bar(), Console.WriteLine("hello"), Bar&lt;T&gt;(x)).
/// </summary>
public sealed class MethodInvocation(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression>? select,
    Identifier name,
    JContainer<Expression>? typeParameters,
    JContainer<Expression> arguments,
    JavaType.Method? methodType
) : J, Statement, Expression, IEquatable<MethodInvocation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression>? Select { get; } = select;
    public Identifier Name { get; } = name;
    public JContainer<Expression>? TypeParameters { get; } = typeParameters;
    public JContainer<Expression> Arguments { get; } = arguments;
    public JavaType.Method? MethodType { get; } = methodType;

    public MethodInvocation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Select, Name, TypeParameters, Arguments, MethodType);
    public MethodInvocation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Select, Name, TypeParameters, Arguments, MethodType);
    public MethodInvocation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Select, Name, TypeParameters, Arguments, MethodType);
    public MethodInvocation WithSelect(JRightPadded<Expression>? select) =>
        ReferenceEquals(select, Select) ? this : new(Id, Prefix, Markers, select, Name, TypeParameters, Arguments, MethodType);
    public MethodInvocation WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, Select, name, TypeParameters, Arguments, MethodType);
    public MethodInvocation WithTypeParameters(JContainer<Expression>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, Select, Name, typeParameters, Arguments, MethodType);
    public MethodInvocation WithArguments(JContainer<Expression> arguments) =>
        ReferenceEquals(arguments, Arguments) ? this : new(Id, Prefix, Markers, Select, Name, TypeParameters, arguments, MethodType);
    public MethodInvocation WithMethodType(JavaType.Method? methodType) =>
        ReferenceEquals(methodType, MethodType) ? this : new(Id, Prefix, Markers, Select, Name, TypeParameters, Arguments, methodType);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(MethodInvocation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MethodInvocation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A binary expression (e.g., a + b, x == y).
/// </summary>
public sealed class Binary(
    Guid id,
    Space prefix,
    Markers markers,
    Expression left,
    JLeftPadded<Binary.OperatorType> @operator,
    Expression right,
    JavaType? type
) : J, Expression, IEquatable<Binary>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Left { get; } = left;
    public JLeftPadded<Binary.OperatorType> Operator { get; } = @operator;
    public Expression Right { get; } = right;
    public JavaType? Type { get; } = type;

    public Binary WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Left, Operator, Right, Type);
    public Binary WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Left, Operator, Right, Type);
    public Binary WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Left, Operator, Right, Type);
    public Binary WithLeft(Expression left) =>
        ReferenceEquals(left, Left) ? this : new(Id, Prefix, Markers, left, Operator, Right, Type);
    public Binary WithOperator(JLeftPadded<Binary.OperatorType> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, Left, @operator, Right, Type);
    public Binary WithRight(Expression right) =>
        ReferenceEquals(right, Right) ? this : new(Id, Prefix, Markers, Left, Operator, right, Type);
    public Binary WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Left, Operator, Right, type);

    public enum OperatorType
    {
        Addition,
        Subtraction,
        Multiplication,
        Division,
        Modulo,
        LessThan,
        GreaterThan,
        LessThanOrEqual,
        GreaterThanOrEqual,
        Equal,
        NotEqual,
        BitAnd,
        BitOr,
        BitXor,
        LeftShift,
        RightShift,
        UnsignedRightShift,
        Or,
        And
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Binary? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Binary);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A unary expression (e.g., !x, -y, ++i, i--).
/// </summary>
public sealed class Unary(
    Guid id,
    Space prefix,
    Markers markers,
    JLeftPadded<Unary.OperatorType> @operator,
    Expression expression,
    JavaType? type
) : J, Statement, Expression, IEquatable<Unary>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JLeftPadded<Unary.OperatorType> Operator { get; } = @operator;
    public Expression Expression { get; } = expression;
    public JavaType? Type { get; } = type;

    public Unary WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Operator, Expression, Type);
    public Unary WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Operator, Expression, Type);
    public Unary WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Operator, Expression, Type);
    public Unary WithOperator(JLeftPadded<Unary.OperatorType> @operator) =>
        ReferenceEquals(@operator, Operator) ? this : new(Id, Prefix, Markers, @operator, Expression, Type);
    public Unary WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, Operator, expression, Type);
    public Unary WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Operator, Expression, type);

    public enum OperatorType
    {
        PreIncrement,
        PreDecrement,
        PostIncrement,
        PostDecrement,
        Positive,
        Negative,
        Complement,
        Not
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Unary? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Unary);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A type cast expression (e.g., (int)x, (string)obj).
/// </summary>
public sealed class TypeCast(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<TypeTree> clazz,
    Expression expression
) : J, Expression, IEquatable<TypeCast>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<TypeTree> Clazz { get; } = clazz;
    public Expression Expression { get; } = expression;

    public TypeCast WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Clazz, Expression);
    public TypeCast WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Clazz, Expression);
    public TypeCast WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Clazz, Expression);
    public TypeCast WithClazz(ControlParentheses<TypeTree> clazz) =>
        ReferenceEquals(clazz, Clazz) ? this : new(Id, Prefix, Markers, clazz, Expression);
    public TypeCast WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, Clazz, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(TypeCast? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as TypeCast);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A parenthesized expression.
/// </summary>
public sealed class Parentheses<T>(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<T> tree
) : J, Expression, IEquatable<Parentheses<T>> where T : J
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<T> Tree { get; } = tree;

    public Parentheses<T> WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Tree);
    public Parentheses<T> WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Tree);
    public Parentheses<T> WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Tree);
    public Parentheses<T> WithTree(JRightPadded<T> tree) =>
        ReferenceEquals(tree, Tree) ? this : new(Id, Prefix, Markers, tree);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Parentheses<T>? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Parentheses<T>);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A variable declaration statement (e.g., var x = 1; or int x = 1, y = 2;).
/// </summary>
public sealed class VariableDeclarations(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Modifier> leadingAnnotations,
    IList<Modifier> modifiers,
    TypeTree? typeExpression,
    Space? varargs,
    IList<JLeftPadded<Space>> dimensionsBeforeName,
    IList<JRightPadded<NamedVariable>> variables
) : J, Statement, IEquatable<VariableDeclarations>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Modifier> LeadingAnnotations { get; } = leadingAnnotations;
    public IList<Modifier> Modifiers { get; } = modifiers;
    public TypeTree? TypeExpression { get; } = typeExpression;
    public Space? Varargs { get; } = varargs;
    public IList<JLeftPadded<Space>> DimensionsBeforeName { get; } = dimensionsBeforeName;
    public IList<JRightPadded<NamedVariable>> Variables { get; } = variables;

    public VariableDeclarations WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, LeadingAnnotations, Modifiers, TypeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, LeadingAnnotations, Modifiers, TypeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithLeadingAnnotations(IList<Modifier> leadingAnnotations) =>
        ReferenceEquals(leadingAnnotations, LeadingAnnotations) ? this : new(Id, Prefix, Markers, leadingAnnotations, Modifiers, TypeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithModifiers(IList<Modifier> modifiers) =>
        ReferenceEquals(modifiers, Modifiers) ? this : new(Id, Prefix, Markers, LeadingAnnotations, modifiers, TypeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithTypeExpression(TypeTree? typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, typeExpression, Varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithVarargs(Space? varargs) =>
        ReferenceEquals(varargs, Varargs) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeExpression, varargs, DimensionsBeforeName, Variables);
    public VariableDeclarations WithDimensionsBeforeName(IList<JLeftPadded<Space>> dimensionsBeforeName) =>
        ReferenceEquals(dimensionsBeforeName, DimensionsBeforeName) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeExpression, Varargs, dimensionsBeforeName, Variables);
    public VariableDeclarations WithVariables(IList<JRightPadded<NamedVariable>> variables) =>
        ReferenceEquals(variables, Variables) ? this : new(Id, Prefix, Markers, LeadingAnnotations, Modifiers, TypeExpression, Varargs, DimensionsBeforeName, variables);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(VariableDeclarations? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as VariableDeclarations);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A single variable in a variable declaration (e.g., x = 1).
/// </summary>
public sealed class NamedVariable(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier name,
    IList<JLeftPadded<Space>> dimensionsAfterName,
    JLeftPadded<Expression>? initializer,
    JavaType? type
) : J, IEquatable<NamedVariable>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Name { get; } = name;
    public IList<JLeftPadded<Space>> DimensionsAfterName { get; } = dimensionsAfterName;
    public JLeftPadded<Expression>? Initializer { get; } = initializer;
    public JavaType? Type { get; } = type;

    public NamedVariable WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Name, DimensionsAfterName, Initializer, Type);
    public NamedVariable WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Name, DimensionsAfterName, Initializer, Type);
    public NamedVariable WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Name, DimensionsAfterName, Initializer, Type);
    public NamedVariable WithName(Identifier name) =>
        ReferenceEquals(name, Name) ? this : new(Id, Prefix, Markers, name, DimensionsAfterName, Initializer, Type);
    public NamedVariable WithDimensionsAfterName(IList<JLeftPadded<Space>> dimensionsAfterName) =>
        ReferenceEquals(dimensionsAfterName, DimensionsAfterName) ? this : new(Id, Prefix, Markers, Name, dimensionsAfterName, Initializer, Type);
    public NamedVariable WithInitializer(JLeftPadded<Expression>? initializer) =>
        ReferenceEquals(initializer, Initializer) ? this : new(Id, Prefix, Markers, Name, DimensionsAfterName, initializer, Type);
    public NamedVariable WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Name, DimensionsAfterName, Initializer, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NamedVariable? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NamedVariable);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A type reference in source code.
/// </summary>
public interface TypeTree : NameTree
{
}

/// <summary>
/// A primitive type (int, long, etc.) or the var keyword.
/// </summary>
public sealed class Primitive(
    Guid id,
    Space prefix,
    Markers markers,
    JavaType.PrimitiveKind kind
) : J, TypeTree, Expression, IEquatable<Primitive>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JavaType.PrimitiveKind Kind { get; } = kind;

    public Primitive WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Kind);
    public Primitive WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Kind);
    public Primitive WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Kind);
    public Primitive WithKind(JavaType.PrimitiveKind kind) =>
        kind == Kind ? this : new(Id, Prefix, Markers, kind);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Primitive? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Primitive);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A modifier (public, static, etc.).
/// </summary>
public sealed class Modifier(
    Guid id,
    Space prefix,
    Markers markers,
    Modifier.ModifierType type,
    IList<Annotation> annotations,
    string? keyword = null
) : J, IEquatable<Modifier>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Modifier.ModifierType Type { get; } = type;
    public IList<Annotation> Annotations { get; } = annotations;
    public string? Keyword { get; } = keyword;

    public Modifier WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Type, Annotations, Keyword);
    public Modifier WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Type, Annotations, Keyword);
    public Modifier WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Type, Annotations, Keyword);
    public Modifier WithType(Modifier.ModifierType type) =>
        type == Type ? this : new(Id, Prefix, Markers, type, Annotations, Keyword);
    public Modifier WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, Type, annotations, Keyword);
    public Modifier WithKeyword(string? keyword) =>
        string.Equals(keyword, Keyword, StringComparison.Ordinal) ? this : new(Id, Prefix, Markers, Type, Annotations, keyword);

    public enum ModifierType
    {
        Default,
        Public,
        Private,
        Protected,
        Internal,
        Static,
        Abstract,
        Final,
        Sealed,
        Native,
        Strictfp,
        Synchronized,
        Transient,
        Volatile,
        Async,
        Override,
        Virtual,
        Readonly,
        Const,
        New,
        Extern,
        Unsafe,
        Partial,
        Ref,
        Out,
        In,
        LanguageExtension
    }


    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Modifier? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Modifier);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An annotation/attribute.
/// </summary>
public sealed class Annotation(
    Guid id,
    Space prefix,
    Markers markers,
    NameTree annotationType,
    JContainer<Expression>? arguments
) : J, Expression, IEquatable<Annotation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public NameTree AnnotationType { get; } = annotationType;
    public JContainer<Expression>? Arguments { get; } = arguments;

    public Annotation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, AnnotationType, Arguments);
    public Annotation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, AnnotationType, Arguments);
    public Annotation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, AnnotationType, Arguments);
    public Annotation WithAnnotationType(NameTree annotationType) =>
        ReferenceEquals(annotationType, AnnotationType) ? this : new(Id, Prefix, Markers, annotationType, Arguments);
    public Annotation WithArguments(JContainer<Expression>? arguments) =>
        ReferenceEquals(arguments, Arguments) ? this : new(Id, Prefix, Markers, AnnotationType, arguments);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Annotation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Annotation);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A name reference in source code.
/// </summary>
public interface NameTree : J
{
}

/// <summary>
/// Array/indexer access expression (e.g., arr[0], dict["key"]).
/// </summary>
public sealed class ArrayAccess(
    Guid id,
    Space prefix,
    Markers markers,
    Expression indexed,
    ArrayDimension dimension,
    JavaType? type
) : J, Expression, IEquatable<ArrayAccess>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Indexed { get; } = indexed;
    public ArrayDimension Dimension { get; } = dimension;
    public JavaType? Type { get; } = type;

    public ArrayAccess WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Indexed, Dimension, Type);
    public ArrayAccess WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Indexed, Dimension, Type);
    public ArrayAccess WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Indexed, Dimension, Type);
    public ArrayAccess WithIndexed(Expression indexed) =>
        ReferenceEquals(indexed, Indexed) ? this : new(Id, Prefix, Markers, indexed, Dimension, Type);
    public ArrayAccess WithDimension(ArrayDimension dimension) =>
        ReferenceEquals(dimension, Dimension) ? this : new(Id, Prefix, Markers, Indexed, dimension, Type);
    public ArrayAccess WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Indexed, Dimension, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ArrayAccess? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ArrayAccess);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A single dimension in an array access (the [index] part).
/// </summary>
public sealed class ArrayDimension(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> index
) : J, IEquatable<ArrayDimension>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> Index { get; } = index;

    public ArrayDimension WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Index);
    public ArrayDimension WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Index);
    public ArrayDimension WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Index);
    public ArrayDimension WithIndex(JRightPadded<Expression> index) =>
        ReferenceEquals(index, Index) ? this : new(Id, Prefix, Markers, index);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ArrayDimension? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ArrayDimension);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A parameterized type expression (e.g., List&lt;int&gt;, Dictionary&lt;string, int&gt;).
/// </summary>
public sealed class ParameterizedType(
    Guid id,
    Space prefix,
    Markers markers,
    NameTree clazz,
    JContainer<Expression>? typeParameters,
    JavaType? type
) : J, TypeTree, Expression, IEquatable<ParameterizedType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public NameTree Clazz { get; } = clazz;
    public JContainer<Expression>? TypeParameters { get; } = typeParameters;
    public JavaType? Type { get; } = type;

    public ParameterizedType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Clazz, TypeParameters, Type);
    public ParameterizedType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Clazz, TypeParameters, Type);
    public ParameterizedType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Clazz, TypeParameters, Type);
    public ParameterizedType WithClazz(NameTree clazz) =>
        ReferenceEquals(clazz, Clazz) ? this : new(Id, Prefix, Markers, clazz, TypeParameters, Type);
    public ParameterizedType WithTypeParameters(JContainer<Expression>? typeParameters) =>
        ReferenceEquals(typeParameters, TypeParameters) ? this : new(Id, Prefix, Markers, Clazz, typeParameters, Type);
    public ParameterizedType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Clazz, TypeParameters, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ParameterizedType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ParameterizedType);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A nullable type expression (e.g., int?, string?).
/// The typeTree holds the base type and the right padding holds any space before the '?'.
/// </summary>
public sealed class NullableType(
    Guid id,
    Space prefix,
    Markers markers,
    IList<Annotation> annotations,
    JRightPadded<TypeTree> typeTreePadded,
    JavaType? type
) : J, TypeTree, Expression, IEquatable<NullableType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<Annotation> Annotations { get; } = annotations;
    public JRightPadded<TypeTree> TypeTreePadded { get; } = typeTreePadded;
    public JavaType? Type { get; } = type;

    public NullableType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Annotations, TypeTreePadded, Type);
    public NullableType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Annotations, TypeTreePadded, Type);
    public NullableType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Annotations, TypeTreePadded, Type);
    public NullableType WithAnnotations(IList<Annotation> annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, annotations, TypeTreePadded, Type);
    public NullableType WithTypeTreePadded(JRightPadded<TypeTree> typeTreePadded) =>
        ReferenceEquals(typeTreePadded, TypeTreePadded) ? this : new(Id, Prefix, Markers, Annotations, typeTreePadded, Type);
    public NullableType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Annotations, TypeTreePadded, type);

    public TypeTree TypeTree => TypeTreePadded.Element;

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NullableType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NullableType);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An array type expression (e.g., int[], string[,]).
/// For multi-dimensional arrays like int[,], each dimension is nested.
/// </summary>
public sealed class ArrayType(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree elementType,
    IList<Annotation>? annotations,
    JLeftPadded<Space>? dimension,
    JavaType? type
) : J, TypeTree, Expression, IEquatable<ArrayType>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree ElementType { get; } = elementType;
    public IList<Annotation>? Annotations { get; } = annotations;
    public JLeftPadded<Space>? Dimension { get; } = dimension;
    public JavaType? Type { get; } = type;

    public ArrayType WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ElementType, Annotations, Dimension, Type);
    public ArrayType WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ElementType, Annotations, Dimension, Type);
    public ArrayType WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ElementType, Annotations, Dimension, Type);
    public ArrayType WithElementType(TypeTree elementType) =>
        ReferenceEquals(elementType, ElementType) ? this : new(Id, Prefix, Markers, elementType, Annotations, Dimension, Type);
    public ArrayType WithAnnotations(IList<Annotation>? annotations) =>
        ReferenceEquals(annotations, Annotations) ? this : new(Id, Prefix, Markers, ElementType, annotations, Dimension, Type);
    public ArrayType WithDimension(JLeftPadded<Space>? dimension) =>
        ReferenceEquals(dimension, Dimension) ? this : new(Id, Prefix, Markers, ElementType, Annotations, dimension, Type);
    public ArrayType WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, ElementType, Annotations, Dimension, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(ArrayType? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as ArrayType);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// An array creation expression.
/// Examples:
///   new int[10]
///   new int[] { 1, 2, 3 }
///   new[] { 1, 2, 3 }
///   new int[2, 3]
/// </summary>
public sealed class NewArray(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree? typeExpression,
    IList<ArrayDimension> dimensions,
    JContainer<Expression>? initializer,
    JavaType? type
) : J, Expression, IEquatable<NewArray>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree? TypeExpression { get; } = typeExpression;
    public IList<ArrayDimension> Dimensions { get; } = dimensions;
    public JContainer<Expression>? Initializer { get; } = initializer;
    public JavaType? Type { get; } = type;

    public NewArray WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeExpression, Dimensions, Initializer, Type);
    public NewArray WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeExpression, Dimensions, Initializer, Type);
    public NewArray WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeExpression, Dimensions, Initializer, Type);
    public NewArray WithTypeExpression(TypeTree? typeExpression) =>
        ReferenceEquals(typeExpression, TypeExpression) ? this : new(Id, Prefix, Markers, typeExpression, Dimensions, Initializer, Type);
    public NewArray WithDimensions(IList<ArrayDimension> dimensions) =>
        ReferenceEquals(dimensions, Dimensions) ? this : new(Id, Prefix, Markers, TypeExpression, dimensions, Initializer, Type);
    public NewArray WithInitializer(JContainer<Expression>? initializer) =>
        ReferenceEquals(initializer, Initializer) ? this : new(Id, Prefix, Markers, TypeExpression, Dimensions, initializer, Type);
    public NewArray WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, TypeExpression, Dimensions, Initializer, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NewArray? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NewArray);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A constructor invocation (new expression).
/// Examples:
///   new Foo()
///   new Foo(arg1, arg2)
///   new List&lt;int&gt;()
/// </summary>
public sealed class NewClass(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression>? enclosing,
    Space @new,
    TypeTree? clazz,
    JContainer<Expression> arguments,
    Block? body,
    JavaType.Method? constructorType
) : J, Expression, Statement, IEquatable<NewClass>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression>? Enclosing { get; } = enclosing;
    public Space New { get; } = @new;
    public TypeTree? Clazz { get; } = clazz;
    public JContainer<Expression> Arguments { get; } = arguments;
    public Block? Body { get; } = body;
    public JavaType.Method? ConstructorType { get; } = constructorType;

    public NewClass WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Enclosing, New, Clazz, Arguments, Body, ConstructorType);
    public NewClass WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Enclosing, New, Clazz, Arguments, Body, ConstructorType);
    public NewClass WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Enclosing, New, Clazz, Arguments, Body, ConstructorType);
    public NewClass WithEnclosing(JRightPadded<Expression>? enclosing) =>
        ReferenceEquals(enclosing, Enclosing) ? this : new(Id, Prefix, Markers, enclosing, New, Clazz, Arguments, Body, ConstructorType);
    public NewClass WithNew(Space @new) =>
        ReferenceEquals(@new, New) ? this : new(Id, Prefix, Markers, Enclosing, @new, Clazz, Arguments, Body, ConstructorType);
    public NewClass WithClazz(TypeTree? clazz) =>
        ReferenceEquals(clazz, Clazz) ? this : new(Id, Prefix, Markers, Enclosing, New, clazz, Arguments, Body, ConstructorType);
    public NewClass WithArguments(JContainer<Expression> arguments) =>
        ReferenceEquals(arguments, Arguments) ? this : new(Id, Prefix, Markers, Enclosing, New, Clazz, arguments, Body, ConstructorType);
    public NewClass WithBody(Block? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Enclosing, New, Clazz, Arguments, body, ConstructorType);
    public NewClass WithConstructorType(JavaType.Method? constructorType) =>
        ReferenceEquals(constructorType, ConstructorType) ? this : new(Id, Prefix, Markers, Enclosing, New, Clazz, Arguments, Body, constructorType);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(NewClass? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as NewClass);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A ternary conditional expression (e.g., condition ? trueExpr : falseExpr).
/// Also used for null-coalescing (a ?? b) with NullCoalescing marker.
///
/// For regular ternary: condition ? truePart : falsePart
/// For null-coalescing: condition ?? falsePart (truePart is empty, has NullCoalescing marker)
/// </summary>
public sealed class Ternary(
    Guid id,
    Space prefix,
    Markers markers,
    Expression condition,
    JLeftPadded<Expression> truePart,
    JLeftPadded<Expression> falsePart,
    JavaType? type
) : J, Expression, Statement, IEquatable<Ternary>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Condition { get; } = condition;
    public JLeftPadded<Expression> TruePart { get; } = truePart;
    public JLeftPadded<Expression> FalsePart { get; } = falsePart;
    public JavaType? Type { get; } = type;

    public Ternary WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Condition, TruePart, FalsePart, Type);
    public Ternary WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Condition, TruePart, FalsePart, Type);
    public Ternary WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Condition, TruePart, FalsePart, Type);
    public Ternary WithCondition(Expression condition) =>
        ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, condition, TruePart, FalsePart, Type);
    public Ternary WithTruePart(JLeftPadded<Expression> truePart) =>
        ReferenceEquals(truePart, TruePart) ? this : new(Id, Prefix, Markers, Condition, truePart, FalsePart, Type);
    public Ternary WithFalsePart(JLeftPadded<Expression> falsePart) =>
        ReferenceEquals(falsePart, FalsePart) ? this : new(Id, Prefix, Markers, Condition, TruePart, falsePart, Type);
    public Ternary WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Condition, TruePart, FalsePart, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Ternary? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Ternary);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A switch statement.
/// Example:
///   switch (x) {
///       case 1:
///           DoSomething();
///           break;
///       default:
///           DoDefault();
///           break;
///   }
/// </summary>
public sealed class Switch(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<Expression> selector,
    Block cases
) : J, Statement, IEquatable<Switch>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<Expression> Selector { get; } = selector;
    public Block Cases { get; } = cases;

    public Switch WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Selector, Cases);
    public Switch WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Selector, Cases);
    public Switch WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Selector, Cases);
    public Switch WithSelector(ControlParentheses<Expression> selector) =>
        ReferenceEquals(selector, Selector) ? this : new(Id, Prefix, Markers, selector, Cases);
    public Switch WithCases(Block cases) =>
        ReferenceEquals(cases, Cases) ? this : new(Id, Prefix, Markers, Selector, cases);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Switch? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Switch);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A switch expression (Java 14+ / C# 8+).
/// Java: switch (x) { case 1 -> "one"; default -> "other"; }
/// C#: x switch { 1 => "one", _ => "other" }
/// For C#, the Selector stores the expression and space before 'switch' keyword
/// (Selector.Prefix = space before selector for C#, Selector.Tree.After = space before 'switch').
/// </summary>
public sealed class SwitchExpression(
    Guid id,
    Space prefix,
    Markers markers,
    ControlParentheses<Expression> selector,
    Block cases,
    JavaType? type
) : J, Expression, IEquatable<SwitchExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public ControlParentheses<Expression> Selector { get; } = selector;
    public Block Cases { get; } = cases;
    public JavaType? Type { get; } = type;

    public SwitchExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Selector, Cases, Type);
    public SwitchExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Selector, Cases, Type);
    public SwitchExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Selector, Cases, Type);
    public SwitchExpression WithSelector(ControlParentheses<Expression> selector) =>
        ReferenceEquals(selector, Selector) ? this : new(Id, Prefix, Markers, selector, Cases, Type);
    public SwitchExpression WithCases(Block cases) =>
        ReferenceEquals(cases, Cases) ? this : new(Id, Prefix, Markers, Selector, cases, Type);
    public SwitchExpression WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Selector, Cases, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SwitchExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SwitchExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// The kind of case statement.
/// </summary>
public enum CaseType
{
    /// <summary>
    /// Traditional case with colon: case 1: statements...
    /// </summary>
    Statement,
    /// <summary>
    /// Arrow case (switch expression arm): case 1 => expression
    /// </summary>
    Rule
}

/// <summary>
/// A case within a switch statement or expression.
/// Examples:
///   case 1:                    (Statement type)
///   case 1, 2:                 (multiple labels)
///   default:                   (default case)
///   case 1 => expr             (Rule type, switch expression)
///   case > 5 when x < 10:      (pattern with guard)
/// </summary>
public sealed class Case(
    Guid id,
    Space prefix,
    Markers markers,
    CaseType caseKind,
    JContainer<J> caseLabels,
    Expression? guard,
    JContainer<Statement> statements,
    JRightPadded<J>? body
) : J, Statement, IEquatable<Case>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public CaseType CaseKind { get; } = caseKind;
    public JContainer<J> CaseLabels { get; } = caseLabels;
    public Expression? Guard { get; } = guard;
    public JContainer<Statement> Statements { get; } = statements;
    public JRightPadded<J>? Body { get; } = body;

    public Case WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, CaseKind, CaseLabels, Guard, Statements, Body);
    public Case WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, CaseKind, CaseLabels, Guard, Statements, Body);
    public Case WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, CaseKind, CaseLabels, Guard, Statements, Body);
    public Case WithCaseKind(CaseType caseKind) =>
        caseKind == CaseKind ? this : new(Id, Prefix, Markers, caseKind, CaseLabels, Guard, Statements, Body);
    public Case WithCaseLabels(JContainer<J> caseLabels) =>
        ReferenceEquals(caseLabels, CaseLabels) ? this : new(Id, Prefix, Markers, CaseKind, caseLabels, Guard, Statements, Body);
    public Case WithGuard(Expression? guard) =>
        ReferenceEquals(guard, Guard) ? this : new(Id, Prefix, Markers, CaseKind, CaseLabels, guard, Statements, Body);
    public Case WithStatements(JContainer<Statement> statements) =>
        ReferenceEquals(statements, Statements) ? this : new(Id, Prefix, Markers, CaseKind, CaseLabels, Guard, statements, Body);
    public Case WithBody(JRightPadded<J>? body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, CaseKind, CaseLabels, Guard, Statements, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Case? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Case);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A lambda expression: (params) => body
/// Examples:
///   x => x * 2
///   (x, y) => x + y
///   (int x) => { return x; }
///   () => Console.WriteLine()
/// </summary>
public sealed class Lambda(
    Guid id,
    Space prefix,
    Markers markers,
    Lambda.Parameters @params,
    Space arrow,
    J body,
    JavaType? type
) : J, Expression, Statement, IEquatable<Lambda>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Lambda.Parameters Params { get; } = @params;
    public Space Arrow { get; } = arrow;
    public J Body { get; } = body;
    public JavaType? Type { get; } = type;

    public Lambda WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Params, Arrow, Body, Type);
    public Lambda WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Params, Arrow, Body, Type);
    public Lambda WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Params, Arrow, Body, Type);
    public Lambda WithParams(Lambda.Parameters @params) =>
        ReferenceEquals(@params, Params) ? this : new(Id, Prefix, Markers, @params, Arrow, Body, Type);
    public Lambda WithArrow(Space arrow) =>
        ReferenceEquals(arrow, Arrow) ? this : new(Id, Prefix, Markers, Params, arrow, Body, Type);
    public Lambda WithBody(J body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Params, Arrow, body, Type);
    public Lambda WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Params, Arrow, Body, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    /// <summary>
    /// Lambda parameters, optionally wrapped in parentheses.
    /// For x => ..., Parenthesized is false.
    /// For (x) => ... or (x, y) => ..., Parenthesized is true.
    /// </summary>
        public sealed class Parameters(
        Guid id,
        Space prefix,
        Markers markers,
        bool parenthesized,
        IList<JRightPadded<J>> elements
    ) : J, IEquatable<Parameters>
    {
        public Guid Id { get; } = id;
        public Space Prefix { get; } = prefix;
        public Markers Markers { get; } = markers;
        public bool Parenthesized { get; } = parenthesized;
        public IList<JRightPadded<J>> Elements { get; } = elements;

        public Parameters WithId(Guid id) =>
            id == Id ? this : new(id, Prefix, Markers, Parenthesized, Elements);
        public Parameters WithPrefix(Space prefix) =>
            ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Parenthesized, Elements);
        public Parameters WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Parenthesized, Elements);
        public Parameters WithParenthesized(bool parenthesized) =>
            parenthesized == Parenthesized ? this : new(Id, Prefix, Markers, parenthesized, Elements);
        public Parameters WithElements(IList<JRightPadded<J>> elements) =>
            ReferenceEquals(elements, Elements) ? this : new(Id, Prefix, Markers, Parenthesized, elements);

        Tree Tree.WithId(Guid id) => WithId(id);

        public bool Equals(Parameters? other) => other is not null && Id == other.Id;
        public override bool Equals(object? obj) => Equals(obj as Parameters);
        public override int GetHashCode() => Id.GetHashCode();
    }

    public bool Equals(Lambda? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Lambda);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A deconstruction pattern for positional pattern matching.
/// Used for record patterns in Java and positional/tuple patterns in C#.
/// Examples:
///   case Point(int x, int y):     // Named type deconstruction
///   case (int x, int y):          // C# tuple deconstruction (Deconstructor = Empty)
///   case (> 0, > 0):              // Nested patterns
/// </summary>
public sealed class DeconstructionPattern(
    Guid id,
    Space prefix,
    Markers markers,
    Expression deconstructor,
    JContainer<J> nested,
    JavaType? type
) : J, Expression, IEquatable<DeconstructionPattern>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Deconstructor { get; } = deconstructor;
    public JContainer<J> Nested { get; } = nested;
    public JavaType? Type { get; } = type;

    public DeconstructionPattern WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Deconstructor, Nested, Type);
    public DeconstructionPattern WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Deconstructor, Nested, Type);
    public DeconstructionPattern WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Deconstructor, Nested, Type);
    public DeconstructionPattern WithDeconstructor(Expression deconstructor) =>
        ReferenceEquals(deconstructor, Deconstructor) ? this : new(Id, Prefix, Markers, deconstructor, Nested, Type);
    public DeconstructionPattern WithNested(JContainer<J> nested) =>
        ReferenceEquals(nested, Nested) ? this : new(Id, Prefix, Markers, Deconstructor, nested, Type);
    public DeconstructionPattern WithType(JavaType? type) =>
        ReferenceEquals(type, Type) ? this : new(Id, Prefix, Markers, Deconstructor, Nested, type);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(DeconstructionPattern? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DeconstructionPattern);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A container for a list of elements with shared prefix.
/// </summary>
public sealed class JContainer<T>(
    Space before,
    IList<JRightPadded<T>> elements,
    Markers markers
)
{
    public Space Before { get; } = before;
    public IList<JRightPadded<T>> Elements { get; } = elements;
    public Markers Markers { get; } = markers;

    public JContainer<T> WithBefore(Space before) =>
        ReferenceEquals(before, Before) ? this : new(before, Elements, Markers);
    public JContainer<T> WithElements(IList<JRightPadded<T>> elements) =>
        ReferenceEquals(elements, Elements) ? this : new(Before, elements, Markers);
    public JContainer<T> WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Before, Elements, markers);

    public static JContainer<T> Empty() => new(Space.Empty, [], Markers.Empty);
}

/// <summary>
/// Left-padded element with space before it.
/// </summary>
public sealed class JLeftPadded<T>(
    Space before,
    T element
)
{
    public Space Before { get; } = before;
    public T Element { get; } = element;

    public JLeftPadded<T> WithBefore(Space before) =>
        ReferenceEquals(before, Before) ? this : new(before, Element);
    public JLeftPadded<T> WithElement(T element) =>
        ReferenceEquals(element, Element) ? this : new(Before, element);
}

/// <summary>
/// Right-padded element with space after it.
/// </summary>
public sealed class JRightPadded<T>(
    T element,
    Space after,
    Markers markers
)
{
    public T Element { get; } = element;
    public Space After { get; } = after;
    public Markers Markers { get; } = markers;

    public JRightPadded<T> WithElement(T element) =>
        ReferenceEquals(element, Element) ? this : new(element, After, Markers);
    public JRightPadded<T> WithAfter(Space after) =>
        ReferenceEquals(after, After) ? this : new(Element, after, Markers);
    public JRightPadded<T> WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Element, After, markers);

    public static JRightPadded<T> Build(T element) => new(element, Space.Empty, Markers.Empty);
}
