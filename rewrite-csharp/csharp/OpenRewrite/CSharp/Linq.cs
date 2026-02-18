using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// LINQ query expression types. These form a mini-DSL within C#.
/// </summary>
public interface Linq : Cs
{
}

public interface SelectOrGroupClause : Linq
{
}

public interface QueryClause : Linq
{
}

/// <summary>
/// A LINQ query expression: from x in source [clauses] select/group
/// </summary>
public sealed class QueryExpression(
    Guid id,
    Space prefix,
    Markers markers,
    FromClause fromClause,
    QueryBody body
) : Linq, Expression, IEquatable<QueryExpression>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public FromClause FromClause { get; } = fromClause;
    public QueryBody Body { get; } = body;

    public QueryExpression WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, FromClause, Body);
    public QueryExpression WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, FromClause, Body);
    public QueryExpression WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, FromClause, Body);
    public QueryExpression WithFromClause(FromClause fromClause) =>
        ReferenceEquals(fromClause, FromClause) ? this : new(Id, Prefix, Markers, fromClause, Body);
    public QueryExpression WithBody(QueryBody body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, FromClause, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(QueryExpression? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as QueryExpression);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// The body of a LINQ query: clauses + select/group + optional continuation.
/// </summary>
public sealed class QueryBody(
    Guid id,
    Space prefix,
    Markers markers,
    IList<QueryClause> clauses,
    SelectOrGroupClause? selectOrGroup,
    QueryContinuation? continuation
) : Linq, IEquatable<QueryBody>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<QueryClause> Clauses { get; } = clauses;
    public SelectOrGroupClause? SelectOrGroup { get; } = selectOrGroup;
    public QueryContinuation? Continuation { get; } = continuation;

    public QueryBody WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Clauses, SelectOrGroup, Continuation);
    public QueryBody WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Clauses, SelectOrGroup, Continuation);
    public QueryBody WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Clauses, SelectOrGroup, Continuation);
    public QueryBody WithClauses(IList<QueryClause> clauses) =>
        ReferenceEquals(clauses, Clauses) ? this : new(Id, Prefix, Markers, clauses, SelectOrGroup, Continuation);
    public QueryBody WithSelectOrGroup(SelectOrGroupClause? selectOrGroup) =>
        ReferenceEquals(selectOrGroup, SelectOrGroup) ? this : new(Id, Prefix, Markers, Clauses, selectOrGroup, Continuation);
    public QueryBody WithContinuation(QueryContinuation? continuation) =>
        ReferenceEquals(continuation, Continuation) ? this : new(Id, Prefix, Markers, Clauses, SelectOrGroup, continuation);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(QueryBody? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as QueryBody);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ from clause: from [type] identifier in expression
/// </summary>
public sealed class FromClause(
    Guid id,
    Space prefix,
    Markers markers,
    TypeTree? typeIdentifier,
    JRightPadded<Identifier> identifierPadded,
    Expression expression
) : Linq, QueryClause, Expression, IEquatable<FromClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public TypeTree? TypeIdentifier { get; } = typeIdentifier;
    public JRightPadded<Identifier> IdentifierPadded { get; } = identifierPadded;
    public Expression Expression { get; } = expression;

    public Identifier Identifier => IdentifierPadded.Element;

    public FromClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, TypeIdentifier, IdentifierPadded, Expression);
    public FromClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, TypeIdentifier, IdentifierPadded, Expression);
    public FromClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, TypeIdentifier, IdentifierPadded, Expression);
    public FromClause WithTypeIdentifier(TypeTree? typeIdentifier) =>
        ReferenceEquals(typeIdentifier, TypeIdentifier) ? this : new(Id, Prefix, Markers, typeIdentifier, IdentifierPadded, Expression);
    public FromClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) =>
        ReferenceEquals(identifierPadded, IdentifierPadded) ? this : new(Id, Prefix, Markers, TypeIdentifier, identifierPadded, Expression);
    public FromClause WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, TypeIdentifier, IdentifierPadded, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(FromClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as FromClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ let clause: let identifier = expression
/// </summary>
public sealed class LetClause(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> identifierPadded,
    Expression expression
) : Linq, QueryClause, IEquatable<LetClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> IdentifierPadded { get; } = identifierPadded;
    public Expression Expression { get; } = expression;

    public Identifier Identifier => IdentifierPadded.Element;

    public LetClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, IdentifierPadded, Expression);
    public LetClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, IdentifierPadded, Expression);
    public LetClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, IdentifierPadded, Expression);
    public LetClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) =>
        ReferenceEquals(identifierPadded, IdentifierPadded) ? this : new(Id, Prefix, Markers, identifierPadded, Expression);
    public LetClause WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, IdentifierPadded, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(LetClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as LetClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ join clause: join identifier in inExpression on leftExpression equals rightExpression [into intoIdentifier]
/// </summary>
public sealed class JoinClause(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Identifier> identifierPadded,
    JRightPadded<Expression> inExpression,
    JRightPadded<Expression> leftExpression,
    Expression rightExpression,
    JLeftPadded<JoinIntoClause>? into
) : Linq, QueryClause, IEquatable<JoinClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Identifier> IdentifierPadded { get; } = identifierPadded;
    public JRightPadded<Expression> InExpression { get; } = inExpression;
    public JRightPadded<Expression> LeftExpression { get; } = leftExpression;
    public Expression RightExpression { get; } = rightExpression;
    public JLeftPadded<JoinIntoClause>? Into { get; } = into;

    public Identifier Identifier => IdentifierPadded.Element;

    public JoinClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, IdentifierPadded, InExpression, LeftExpression, RightExpression, Into);
    public JoinClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, IdentifierPadded, InExpression, LeftExpression, RightExpression, Into);
    public JoinClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, IdentifierPadded, InExpression, LeftExpression, RightExpression, Into);
    public JoinClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) =>
        ReferenceEquals(identifierPadded, IdentifierPadded) ? this : new(Id, Prefix, Markers, identifierPadded, InExpression, LeftExpression, RightExpression, Into);
    public JoinClause WithInExpression(JRightPadded<Expression> inExpression) =>
        ReferenceEquals(inExpression, InExpression) ? this : new(Id, Prefix, Markers, IdentifierPadded, inExpression, LeftExpression, RightExpression, Into);
    public JoinClause WithLeftExpression(JRightPadded<Expression> leftExpression) =>
        ReferenceEquals(leftExpression, LeftExpression) ? this : new(Id, Prefix, Markers, IdentifierPadded, InExpression, leftExpression, RightExpression, Into);
    public JoinClause WithRightExpression(Expression rightExpression) =>
        ReferenceEquals(rightExpression, RightExpression) ? this : new(Id, Prefix, Markers, IdentifierPadded, InExpression, LeftExpression, rightExpression, Into);
    public JoinClause WithInto(JLeftPadded<JoinIntoClause>? into) =>
        ReferenceEquals(into, Into) ? this : new(Id, Prefix, Markers, IdentifierPadded, InExpression, LeftExpression, RightExpression, into);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(JoinClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as JoinClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ join into clause: into identifier
/// </summary>
public sealed class JoinIntoClause(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier identifier
) : Linq, QueryClause, IEquatable<JoinIntoClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Identifier { get; } = identifier;

    public JoinIntoClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Identifier);
    public JoinIntoClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Identifier);
    public JoinIntoClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Identifier);
    public JoinIntoClause WithIdentifier(Identifier identifier) =>
        ReferenceEquals(identifier, Identifier) ? this : new(Id, Prefix, Markers, identifier);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(JoinIntoClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as JoinIntoClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ where clause: where condition
/// </summary>
public sealed class WhereClause(
    Guid id,
    Space prefix,
    Markers markers,
    Expression condition
) : Linq, QueryClause, IEquatable<WhereClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Condition { get; } = condition;

    public WhereClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Condition);
    public WhereClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Condition);
    public WhereClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Condition);
    public WhereClause WithCondition(Expression condition) =>
        ReferenceEquals(condition, Condition) ? this : new(Id, Prefix, Markers, condition);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(WhereClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as WhereClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ orderby clause: orderby ordering1, ordering2, ...
/// </summary>
public sealed class OrderByClause(
    Guid id,
    Space prefix,
    Markers markers,
    IList<JRightPadded<Ordering>> orderings
) : Linq, QueryClause, IEquatable<OrderByClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public IList<JRightPadded<Ordering>> Orderings { get; } = orderings;

    public OrderByClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Orderings);
    public OrderByClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Orderings);
    public OrderByClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Orderings);
    public OrderByClause WithOrderings(IList<JRightPadded<Ordering>> orderings) =>
        ReferenceEquals(orderings, Orderings) ? this : new(Id, Prefix, Markers, orderings);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(OrderByClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as OrderByClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// The direction of an ordering in an orderby clause.
/// </summary>
public enum DirectionKind
{
    Ascending,
    Descending
}

/// <summary>
/// A single ordering in an orderby clause: expression [ascending|descending]
/// </summary>
public sealed class Ordering(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> expressionPadded,
    DirectionKind? direction
) : Linq, IEquatable<Ordering>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> ExpressionPadded { get; } = expressionPadded;
    public DirectionKind? Direction { get; } = direction;

    public Expression Expression => ExpressionPadded.Element;

    public Ordering WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, ExpressionPadded, Direction);
    public Ordering WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, ExpressionPadded, Direction);
    public Ordering WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, ExpressionPadded, Direction);
    public Ordering WithExpressionPadded(JRightPadded<Expression> expressionPadded) =>
        ReferenceEquals(expressionPadded, ExpressionPadded) ? this : new(Id, Prefix, Markers, expressionPadded, Direction);
    public Ordering WithDirection(DirectionKind? direction) =>
        direction == Direction ? this : new(Id, Prefix, Markers, ExpressionPadded, direction);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(Ordering? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Ordering);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ select clause: select expression
/// </summary>
public sealed class SelectClause(
    Guid id,
    Space prefix,
    Markers markers,
    Expression expression
) : Linq, SelectOrGroupClause, IEquatable<SelectClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Expression Expression { get; } = expression;

    public SelectClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Expression);
    public SelectClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Expression);
    public SelectClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Expression);
    public SelectClause WithExpression(Expression expression) =>
        ReferenceEquals(expression, Expression) ? this : new(Id, Prefix, Markers, expression);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(SelectClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SelectClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ group clause: group groupExpression by key
/// </summary>
public sealed class GroupClause(
    Guid id,
    Space prefix,
    Markers markers,
    JRightPadded<Expression> groupExpression,
    Expression key
) : Linq, SelectOrGroupClause, IEquatable<GroupClause>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public JRightPadded<Expression> GroupExpression { get; } = groupExpression;
    public Expression Key { get; } = key;

    public GroupClause WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, GroupExpression, Key);
    public GroupClause WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, GroupExpression, Key);
    public GroupClause WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, GroupExpression, Key);
    public GroupClause WithGroupExpression(JRightPadded<Expression> groupExpression) =>
        ReferenceEquals(groupExpression, GroupExpression) ? this : new(Id, Prefix, Markers, groupExpression, Key);
    public GroupClause WithKey(Expression key) =>
        ReferenceEquals(key, Key) ? this : new(Id, Prefix, Markers, GroupExpression, key);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(GroupClause? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as GroupClause);
    public override int GetHashCode() => Id.GetHashCode();
}

/// <summary>
/// A LINQ query continuation: into identifier body
/// </summary>
public sealed class QueryContinuation(
    Guid id,
    Space prefix,
    Markers markers,
    Identifier identifier,
    QueryBody body
) : Linq, IEquatable<QueryContinuation>
{
    public Guid Id { get; } = id;
    public Space Prefix { get; } = prefix;
    public Markers Markers { get; } = markers;
    public Identifier Identifier { get; } = identifier;
    public QueryBody Body { get; } = body;

    public QueryContinuation WithId(Guid id) =>
        id == Id ? this : new(id, Prefix, Markers, Identifier, Body);
    public QueryContinuation WithPrefix(Space prefix) =>
        ReferenceEquals(prefix, Prefix) ? this : new(Id, prefix, Markers, Identifier, Body);
    public QueryContinuation WithMarkers(Markers markers) =>
        ReferenceEquals(markers, Markers) ? this : new(Id, Prefix, markers, Identifier, Body);
    public QueryContinuation WithIdentifier(Identifier identifier) =>
        ReferenceEquals(identifier, Identifier) ? this : new(Id, Prefix, Markers, identifier, Body);
    public QueryContinuation WithBody(QueryBody body) =>
        ReferenceEquals(body, Body) ? this : new(Id, Prefix, Markers, Identifier, body);

    Tree Tree.WithId(Guid id) => WithId(id);

    public bool Equals(QueryContinuation? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as QueryContinuation);
    public override int GetHashCode() => Id.GetHashCode();
}
