/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Static entry point for conditionally wrapping an expression in parentheses
/// based on its context in the tree.
/// </summary>
public static class CSharpParenthesizeVisitor
{
    /// <summary>
    /// Determines whether <paramref name="newTree"/> needs parentheses when placed at the
    /// position indicated by <paramref name="cursor"/> (which points to the original node
    /// being replaced). Returns either the original <paramref name="newTree"/> or a
    /// <see cref="Parentheses{T}"/> wrapping it.
    /// </summary>
    public static Expression MaybeParenthesize(Expression newTree, Cursor cursor)
    {
        // Fast exit: simple expression types never need parenthesization
        if (newTree is not (Binary or Unary or Ternary or Assignment or AssignmentOperation
            or TypeCast or CsBinary or CsUnary or IsPattern or RangeExpression
            or SwitchExpression or WithExpression or Lambda or AwaitExpression or QueryExpression or InstanceOf
            or NullSafeExpression))
            return newTree;

        var originalTree = cursor.Value;
        if (originalTree is not Tree original) return newTree;

        // Give newTree the original's ID so the non-recursive visitor can locate it
        var newTreeWithOriginalId = (Expression)((Tree)newTree).WithId(original.Id);

        // Run a non-recursive visitor with the parent cursor context
        var result = new CSharpParenthesizeVisitor<int>(false)
            .Visit(newTreeWithOriginalId, 0, cursor.Parent!);

        if (result is Parentheses<Expression>)
            return CSharpPrecedences.Parenthesize(newTree);

        return newTree;
    }

    /// <summary>
    /// Recursively walks <paramref name="tree"/> and adds parentheses wherever operator
    /// precedence requires them within the tree itself (not considering the graft site).
    /// Call this on template results before <see cref="MaybeParenthesize"/>.
    /// </summary>
    public static J ParenthesizeDeep(J tree)
    {
        return new CSharpParenthesizeVisitor<int>(true).Visit(tree, 0)!;
    }
}

/// <summary>
/// Visitor that wraps expressions in parentheses when operator precedence requires it.
/// When <c>_recursive</c> is true (the default), it walks the entire tree.
/// When false, it only processes the root node without descending into children.
/// </summary>
public class CSharpParenthesizeVisitor<P> : CSharpVisitor<P>
{
    private readonly bool _recursive;

    public CSharpParenthesizeVisitor()
    {
        _recursive = true;
    }

    internal CSharpParenthesizeVisitor(bool recursive)
    {
        _recursive = recursive;
    }

    // -----------------------------------------------------------------------
    // J types
    // -----------------------------------------------------------------------

    public override J VisitBinary(Binary binary, P p)
    {
        var b = _recursive ? (Binary)base.VisitBinary(binary, p) : binary;
        return MaybeWrapBinaryLike(b);
    }

    public override J VisitUnary(Unary unary, P p)
    {
        var u = _recursive ? (Unary)base.VisitUnary(unary, p) : unary;
        return MaybeWrapUnaryInContext(u);
    }

    public override J VisitTernary(Ternary ternary, P p)
    {
        var t = _recursive ? (Ternary)base.VisitTernary(ternary, p) : ternary;
        return MaybeWrapBinaryLike(t);
    }

    public override J VisitTypeCast(TypeCast cast, P p)
    {
        var c = _recursive ? (TypeCast)base.VisitTypeCast(cast, p) : cast;
        // TypeCast is precedence 13 (unary level), so it only needs parens
        // when the parent binds tighter — use normal precedence comparison
        return MaybeWrapBinaryLike(c);
    }

    public override J VisitInstanceOf(InstanceOf instanceOf, P p)
    {
        var i = _recursive ? (InstanceOf)base.VisitInstanceOf(instanceOf, p) : instanceOf;
        return MaybeWrapBinaryLike(i);
    }

    public override J VisitAssignment(Assignment assignment, P p)
    {
        var a = _recursive ? (Assignment)base.VisitAssignment(assignment, p) : assignment;
        return MaybeWrapAssignment(a);
    }

    public override J VisitLambda(Lambda lambda, P p)
    {
        var l = _recursive ? (Lambda)base.VisitLambda(lambda, p) : lambda;
        return MaybeWrapAssignment(l);
    }

    // -----------------------------------------------------------------------
    // Cs types
    // -----------------------------------------------------------------------

    public override J VisitCsBinary(CsBinary csBinary, P p)
    {
        var b = _recursive ? (CsBinary)base.VisitCsBinary(csBinary, p) : csBinary;
        return MaybeWrapBinaryLike(b);
    }

    public override J VisitCsUnary(CsUnary csUnary, P p)
    {
        var u = _recursive ? (CsUnary)base.VisitCsUnary(csUnary, p) : csUnary;
        return MaybeWrapUnaryInContext(u);
    }

    public override J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        var a = _recursive ? (AssignmentOperation)base.VisitAssignmentOperation(assignmentOperation, p) : assignmentOperation;
        return MaybeWrapAssignment(a);
    }

    public override J VisitIsPattern(IsPattern isPattern, P p)
    {
        var ip = _recursive ? (IsPattern)base.VisitIsPattern(isPattern, p) : isPattern;
        return MaybeWrapBinaryLike(ip);
    }

    public override J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        var r = _recursive ? (RangeExpression)base.VisitRangeExpression(rangeExpression, p) : rangeExpression;
        return MaybeWrapBinaryLike(r);
    }

    public override J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        var s = _recursive ? (SwitchExpression)base.VisitSwitchExpression(switchExpression, p) : switchExpression;
        return MaybeWrapBinaryLike(s);
    }

    public override J VisitWithExpression(WithExpression withExpression, P p)
    {
        var w = _recursive ? (WithExpression)base.VisitWithExpression(withExpression, p) : withExpression;
        return MaybeWrapBinaryLike(w);
    }

    public override J VisitAwaitExpression(AwaitExpression awaitExpression, P p)
    {
        var a = _recursive ? (AwaitExpression)base.VisitAwaitExpression(awaitExpression, p) : awaitExpression;
        return NeedsParenthesesInSlot(a) ? CSharpPrecedences.Parenthesize(a) : a;
    }

    public override J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        var q = _recursive ? (QueryExpression)base.VisitQueryExpression(queryExpression, p) : queryExpression;
        // A query extends as far to the right as a lambda body does
        return MaybeWrapAssignment(q);
    }

    public override J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        var n = _recursive ? base.VisitNullSafeExpression(nullSafeExpression, p) : nullSafeExpression;
        return n is NullSafeExpression nse && NeedsParenthesesInSlot(nse) ? CSharpPrecedences.Parenthesize(nse) : n;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private const int ShiftPrecedence = 8;
    private const int SwitchPrecedence = 11;
    private const int RangePrecedence = 12;
    private const int UnaryPrecedence = 13;
    private const int PrimaryPrecedence = 14;

    private J MaybeWrapBinaryLike(Expression expr)
    {
        if (NeedsParenthesesInSlot(expr) || NeedsParenthesesInContext(expr))
            return CSharpPrecedences.Parenthesize(expr);
        return expr;
    }

    private J MaybeWrapUnaryInContext(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        if (WouldFuseSigns(parent, expr) || NeedsParenthesesInSlot(expr))
            return CSharpPrecedences.Parenthesize(expr);

        // All prefix unary operators are at the same precedence level (13),
        // so a unary inside another unary is unambiguous once its sign can't fuse: !!x, -~x, ~!x, etc.
        if (parent is Unary or CsUnary)
            return expr;

        // Unary inside binary/other contexts follows normal precedence rules
        if (NeedsParenthesesInContext(expr))
            return CSharpPrecedences.Parenthesize(expr);

        return expr;
    }

    private J MaybeWrapAssignment(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        // A conditional's branches take any expression, so c ? a : b = 1 assigns in its branch
        if (parent is Ternary conditional && conditional.Markers.FindFirst<NullCoalescing>() == null &&
            conditional.Condition.Id != expr.Id)
            return expr;

        // Assignment/AssignmentOperation/Lambda need parens inside Binary, Unary, Ternary, CsBinary, CsUnary, IsPattern
        if (parent is Binary or Unary or Ternary or CsBinary or CsUnary or IsPattern or InstanceOf or ConstantPattern
            or RelationalPattern || NeedsParenthesesInSlot(expr))
            return CSharpPrecedences.Parenthesize(expr);

        return expr;
    }

    /// <summary>
    /// Whether <paramref name="expr"/> binds looser than its slot allows: a cast, <c>await</c> and a range take a
    /// unary expression, a switch expression a range, a with expression a switch expression, and a member access,
    /// element access or postfix <c>!</c> a primary one, so <c>(object)(b + c)</c> and <c>(b + c).ToString()</c> keep
    /// their parentheses.
    /// </summary>
    private bool NeedsParenthesesInSlot(Expression expr)
    {
        var parent = Cursor.ParentTree.Value;
        int? required = parent switch
        {
            TypeCast cast when cast.Expression.Id == expr.Id => UnaryPrecedence,
            AwaitExpression aw when aw.Expression.Id == expr.Id => UnaryPrecedence,
            RangeExpression range when range.Start?.Element.Id == expr.Id || range.End?.Id == expr.Id =>
                UnaryPrecedence,
            SwitchExpression sw when sw.ExpressionPadded.Element.Id == expr.Id => RangePrecedence,
            WithExpression we when we.Target.Id == expr.Id => SwitchPrecedence,
            FieldAccess fa when fa.Target.Id == expr.Id => PrimaryPrecedence,
            MethodInvocation mi when mi.Select?.Element.Id == expr.Id => PrimaryPrecedence,
            ArrayAccess aa when aa.Indexed.Id == expr.Id => PrimaryPrecedence,
            NullSafeExpression nse when nse.Expression.Id == expr.Id => PrimaryPrecedence,
            CsUnary { Operator.Element: CsUnary.OperatorKind.SuppressNullableWarning } cu
                when cu.Expression.Id == expr.Id => PrimaryPrecedence,
            _ => null
        };
        if (required == null)
            return false;
        // Bare, a member access after x?.y! would join the conditional access, as x?.y!.z does
        if (parent is FieldAccess or MethodInvocation or ArrayAccess && expr is NullSafeExpression receiver &&
            IsConditionalAccess(receiver.Expression))
            return true;
        // x as T ends in a type, so the parser applies a switch or a with after it to all of x as T
        if (expr is CsBinary { Operator.Element: CsBinary.OperatorType.As } &&
            parent is SwitchExpression or WithExpression)
            return false;
        // So do x is string and x is int i, but the parser reads x is Foo switch { ... } as a test against a constant
        if (parent is SwitchExpression && expr is InstanceOf or IsPattern && EndsInTypeOrPattern(expr))
            return false;
        // x as string.. too, but the parser reads the Foo.. of x as Foo.. as a qualified name and int?.. as ?.
        if (parent is RangeExpression { Start: not null } startOf && startOf.Start.Element.Id == expr.Id &&
            expr is CsBinary { Operator.Element: CsBinary.OperatorType.As } asType &&
            (asType.Right is ArrayType or PointerType or TupleType || IsKeywordType(asType.Right)))
            return false;
        var precedence = expr switch
        {
            Unary { Operator.Element: Unary.OperatorType.PostIncrement or Unary.OperatorType.PostDecrement } =>
                PrimaryPrecedence,
            // x?.y!.z: parentheses around x?.y! would end the conditional access at the !
            CsUnary { Operator.Element: CsUnary.OperatorKind.SuppressNullableWarning } => PrimaryPrecedence,
            AwaitExpression => UnaryPrecedence,
            QueryExpression => int.MinValue,
            _ => CSharpPrecedences.GetPrecedence(expr)
        };
        return precedence < required || parent is TypeCast cast2 && CastReadsAsValue(cast2, expr);
    }

    /// <summary>
    /// Whether <paramref name="expr"/> is a conditional access such as <c>x?.y</c> or <c>x?[0].y!</c>, which a member
    /// access printed after it would extend.
    /// </summary>
    private static bool IsConditionalAccess(Expression? expr)
    {
        while (expr != null)
        {
            if (expr is FieldAccess or MethodInvocation or ArrayAccess && expr.Markers.FindFirst<NullSafe>() != null)
                return true;
            expr = expr switch
            {
                FieldAccess fa => fa.Target,
                MethodInvocation mi => mi.Select?.Element,
                ArrayAccess aa => aa.Indexed,
                NullSafeExpression nse => nse.Expression,
                CsUnary { Operator.Element: CsUnary.OperatorKind.SuppressNullableWarning } cu => cu.Expression,
                _ => null
            };
        }
        return false;
    }

    /// <summary>
    /// Whether the parser reads <c>(T)</c> before <paramref name="expr"/> as a parenthesized value and a binary
    /// operator, as it reads <c>(Foo)-x</c>: a name in parentheses is a cast only when a token that can't continue
    /// an expression follows it.
    /// </summary>
    private static bool CastReadsAsValue(TypeCast cast, Expression expr)
    {
        var type = cast.Clazz.Tree.Element;
        if (type is NullableType or ArrayType or PointerType || IsKeywordType(type))
            return false;
        Expression? leftmost = expr;
        while (LeftOperand(leftmost) is { } next)
            leftmost = next;
        return leftmost is Unary { Operator.Element: Unary.OperatorType.Negative or Unary.OperatorType.Positive or
                   Unary.OperatorType.PreDecrement or Unary.OperatorType.PreIncrement } ||
               leftmost is CsUnary { Operator.Element: CsUnary.OperatorKind.AddressOf or
                   CsUnary.OperatorKind.PointerType or CsUnary.OperatorKind.FromEnd };
    }

    private static bool IsKeywordType(J type) =>
        type is Primitive || type is Identifier { SimpleName: "object" or "string" or "bool" or "byte" or "sbyte" or
            "char" or "decimal" or "double" or "float" or "int" or "uint" or "long" or "ulong" or "short" or "ushort" };

    /// <summary>
    /// Whether the type test or pattern <paramref name="expr"/> ends in a token that can't continue an expression: a
    /// type other than a name, a designation, or a closing brace, bracket or parenthesis.
    /// </summary>
    private static bool EndsInTypeOrPattern(J expr) => expr switch
    {
        InstanceOf io => io.Pattern != null || io.Clazz is TypeTree && EndsInTypeOrPattern(io.Clazz),
        IsPattern ip => EndsInTypeOrPattern(ip.Pattern.Element),
        CsBinary { Operator.Element: CsBinary.OperatorType.Or or CsBinary.OperatorType.And } combined =>
            EndsInTypeOrPattern(combined.Right),
        CsUnary { Operator.Element: CsUnary.OperatorKind.Not } negated => EndsInTypeOrPattern(negated.Expression),
        StatementExpression or PropertyPattern or ListPattern or DeconstructionPattern or Parentheses<Expression> =>
            true,
        Identifier or FieldAccess or PointerFieldAccess => IsKeywordType(expr),
        TypeTree => true,
        _ => false
    };

    private bool NeedsParenthesesInContext(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        if (parent is Unary or CsUnary || WouldFuseSigns(parent, expr))
            return true;

        if (parent is Binary parentBinary)
        {
            bool isRight = parentBinary.Right.Id == expr.Id;
            return CSharpPrecedences.NeedsParentheses(expr, parentBinary, isRight);
        }

        if (parent is CsBinary parentCsBinary)
        {
            bool isRight = parentCsBinary.Right.Id == expr.Id;
            return CSharpPrecedences.NeedsParentheses(expr, parentCsBinary, isRight);
        }

        // The parser models a ?? b as a Ternary
        if (parent is Ternary parentTernary && parentTernary.Markers.FindFirst<NullCoalescing>() != null)
        {
            bool isRight = parentTernary.FalsePart.Element.Id == expr.Id;
            return CSharpPrecedences.NeedsParentheses(expr, parentTernary, isRight);
        }

        // A conditional's branches take any expression, and its condition one that binds at least as tightly as ??
        if (parent is Ternary conditional)
            return conditional.Condition.Id == expr.Id && CSharpPrecedences.GetPrecedence(expr) < 0;

        // An interpolation's format starts at its first : that no parentheses enclose, so $"{c ? a : b}" doesn't parse
        if (parent is Interpolation)
            return expr is Ternary hole && hole.Markers.FindFirst<NullCoalescing>() == null;

        if (parent is IsPattern)
        {
            // CsBinary with Or/And inside IsPattern is a pattern combinator, not a
            // boolean expression — it must NOT be wrapped in parentheses.
            if (expr is CsBinary csb && csb.Operator.Element is CsBinary.OperatorType.Or or CsBinary.OperatorType.And)
                return false;

            return expr is Binary or CsBinary or Ternary or Assignment or AssignmentOperation or Lambda;
        }

        // A type test is a left-associative relational operator: a == b is Foo parses as a == (b is Foo)
        if (parent is InstanceOf instanceOf && instanceOf.Expression.Element.Id == expr.Id)
            return CSharpPrecedences.GetPrecedence(expr) < CSharpPrecedences.GetPrecedence(instanceOf);

        // A pattern takes a shift expression: x is a == b parses as (x is a) == b
        if (parent is ConstantPattern or RelationalPattern)
            return CSharpPrecedences.GetPrecedence(expr) < ShiftPrecedence;

        return false;
    }

    /// <summary>
    /// Whether <paramref name="expr"/> starts with the sign <paramref name="parent"/> prints right before it,
    /// so that the two would lex as <c>--</c> or <c>++</c>: <c>-(-x)</c> printed bare is <c>--x</c>.
    /// </summary>
    private static bool WouldFuseSigns(object? parent, Expression expr)
    {
        var preceding = parent switch
        {
            Unary { Operator.Element: Unary.OperatorType.Negative } => '-',
            Unary { Operator.Element: Unary.OperatorType.Positive } => '+',
            Binary { Operator.Element: Binary.OperatorType.Subtraction } b when b.Right.Id == expr.Id => '-',
            Binary { Operator.Element: Binary.OperatorType.Addition } b when b.Right.Id == expr.Id => '+',
            _ => (char?)null
        };
        return preceding != null && preceding == LeadingSign(expr);
    }

    /// <summary>
    /// The sign <paramref name="expr"/> prints first, found down its left operands, unless whitespace or a comment
    /// comes before it.
    /// </summary>
    private static char? LeadingSign(Expression expr)
    {
        for (Expression? e = expr; e != null; e = LeftOperand(e))
        {
            if (!e.Prefix.IsEmpty)
                return null;
            if (e is Unary u)
            {
                switch (u.Operator.Element)
                {
                    case Unary.OperatorType.Negative or Unary.OperatorType.PreDecrement:
                        return '-';
                    case Unary.OperatorType.Positive or Unary.OperatorType.PreIncrement:
                        return '+';
                }
            }
        }
        return null;
    }

    private static Expression? LeftOperand(Expression expr) => expr switch
    {
        Binary b => b.Left,
        CsBinary b => b.Left,
        Ternary t => t.Condition,
        Assignment a => a.Variable,
        AssignmentOperation a => a.Variable,
        InstanceOf i => i.Expression.Element,
        IsPattern ip => ip.Expression,
        FieldAccess fa => fa.Target,
        MethodInvocation mi => mi.Select?.Element,
        ArrayAccess aa => aa.Indexed,
        Unary { Operator.Element: Unary.OperatorType.PostIncrement or Unary.OperatorType.PostDecrement } u =>
            u.Expression,
        _ => null
    };
}
