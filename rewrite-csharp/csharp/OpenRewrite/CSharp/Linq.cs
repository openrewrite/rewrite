using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

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
public sealed record QueryExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    FromClause FromClause,
    QueryBody Body
) : Linq, Expression
{
    public QueryExpression WithId(Guid id) => this with { Id = id };
    public QueryExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public QueryExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public QueryExpression WithFromClause(FromClause fromClause) => this with { FromClause = fromClause };
    public QueryExpression WithBody(QueryBody body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The body of a LINQ query: clauses + select/group + optional continuation.
/// </summary>
public sealed record QueryBody(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<QueryClause> Clauses,
    SelectOrGroupClause? SelectOrGroup,
    QueryContinuation? Continuation
) : Linq
{
    public QueryBody WithId(Guid id) => this with { Id = id };
    public QueryBody WithPrefix(Space prefix) => this with { Prefix = prefix };
    public QueryBody WithMarkers(Markers markers) => this with { Markers = markers };
    public QueryBody WithClauses(IList<QueryClause> clauses) => this with { Clauses = clauses };
    public QueryBody WithSelectOrGroup(SelectOrGroupClause? selectOrGroup) => this with { SelectOrGroup = selectOrGroup };
    public QueryBody WithContinuation(QueryContinuation? continuation) => this with { Continuation = continuation };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ from clause: from [type] identifier in expression
/// </summary>
public sealed record FromClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    TypeTree? TypeIdentifier,
    JRightPadded<Identifier> IdentifierPadded,
    Expression Expression
) : Linq, QueryClause, Expression
{
    public Identifier Identifier => IdentifierPadded.Element;

    public FromClause WithId(Guid id) => this with { Id = id };
    public FromClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public FromClause WithMarkers(Markers markers) => this with { Markers = markers };
    public FromClause WithTypeIdentifier(TypeTree? typeIdentifier) => this with { TypeIdentifier = typeIdentifier };
    public FromClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) => this with { IdentifierPadded = identifierPadded };
    public FromClause WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ let clause: let identifier = expression
/// </summary>
public sealed record LetClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Identifier> IdentifierPadded,
    Expression Expression
) : Linq, QueryClause
{
    public Identifier Identifier => IdentifierPadded.Element;

    public LetClause WithId(Guid id) => this with { Id = id };
    public LetClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public LetClause WithMarkers(Markers markers) => this with { Markers = markers };
    public LetClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) => this with { IdentifierPadded = identifierPadded };
    public LetClause WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ join clause: join identifier in inExpression on leftExpression equals rightExpression [into intoIdentifier]
/// </summary>
public sealed record JoinClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Identifier> IdentifierPadded,
    JRightPadded<Expression> InExpression,
    JRightPadded<Expression> LeftExpression,
    Expression RightExpression,
    JLeftPadded<JoinIntoClause>? Into
) : Linq, QueryClause
{
    public Identifier Identifier => IdentifierPadded.Element;

    public JoinClause WithId(Guid id) => this with { Id = id };
    public JoinClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public JoinClause WithMarkers(Markers markers) => this with { Markers = markers };
    public JoinClause WithIdentifierPadded(JRightPadded<Identifier> identifierPadded) => this with { IdentifierPadded = identifierPadded };
    public JoinClause WithInExpression(JRightPadded<Expression> inExpression) => this with { InExpression = inExpression };
    public JoinClause WithLeftExpression(JRightPadded<Expression> leftExpression) => this with { LeftExpression = leftExpression };
    public JoinClause WithRightExpression(Expression rightExpression) => this with { RightExpression = rightExpression };
    public JoinClause WithInto(JLeftPadded<JoinIntoClause>? into) => this with { Into = into };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ join into clause: into identifier
/// </summary>
public sealed record JoinIntoClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Identifier Identifier
) : Linq, QueryClause
{
    public JoinIntoClause WithId(Guid id) => this with { Id = id };
    public JoinIntoClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public JoinIntoClause WithMarkers(Markers markers) => this with { Markers = markers };
    public JoinIntoClause WithIdentifier(Identifier identifier) => this with { Identifier = identifier };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ where clause: where condition
/// </summary>
public sealed record WhereClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Condition
) : Linq, QueryClause
{
    public WhereClause WithId(Guid id) => this with { Id = id };
    public WhereClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public WhereClause WithMarkers(Markers markers) => this with { Markers = markers };
    public WhereClause WithCondition(Expression condition) => this with { Condition = condition };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ orderby clause: orderby ordering1, ordering2, ...
/// </summary>
public sealed record OrderByClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<JRightPadded<Ordering>> Orderings
) : Linq, QueryClause
{
    public OrderByClause WithId(Guid id) => this with { Id = id };
    public OrderByClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public OrderByClause WithMarkers(Markers markers) => this with { Markers = markers };
    public OrderByClause WithOrderings(IList<JRightPadded<Ordering>> orderings) => this with { Orderings = orderings };

    Tree Tree.WithId(Guid id) => WithId(id);
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
public sealed record Ordering(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Expression> ExpressionPadded,
    DirectionKind? Direction
) : Linq
{
    public Expression Expression => ExpressionPadded.Element;

    public Ordering WithId(Guid id) => this with { Id = id };
    public Ordering WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Ordering WithMarkers(Markers markers) => this with { Markers = markers };
    public Ordering WithExpressionPadded(JRightPadded<Expression> expressionPadded) => this with { ExpressionPadded = expressionPadded };
    public Ordering WithDirection(DirectionKind? direction) => this with { Direction = direction };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ select clause: select expression
/// </summary>
public sealed record SelectClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Expression
) : Linq, SelectOrGroupClause
{
    public SelectClause WithId(Guid id) => this with { Id = id };
    public SelectClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public SelectClause WithMarkers(Markers markers) => this with { Markers = markers };
    public SelectClause WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ group clause: group groupExpression by key
/// </summary>
public sealed record GroupClause(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Expression> GroupExpression,
    Expression Key
) : Linq, SelectOrGroupClause
{
    public GroupClause WithId(Guid id) => this with { Id = id };
    public GroupClause WithPrefix(Space prefix) => this with { Prefix = prefix };
    public GroupClause WithMarkers(Markers markers) => this with { Markers = markers };
    public GroupClause WithGroupExpression(JRightPadded<Expression> groupExpression) => this with { GroupExpression = groupExpression };
    public GroupClause WithKey(Expression key) => this with { Key = key };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A LINQ query continuation: into identifier body
/// </summary>
public sealed record QueryContinuation(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Identifier Identifier,
    QueryBody Body
) : Linq
{
    public QueryContinuation WithId(Guid id) => this with { Id = id };
    public QueryContinuation WithPrefix(Space prefix) => this with { Prefix = prefix };
    public QueryContinuation WithMarkers(Markers markers) => this with { Markers = markers };
    public QueryContinuation WithIdentifier(Identifier identifier) => this with { Identifier = identifier };
    public QueryContinuation WithBody(QueryBody body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}
