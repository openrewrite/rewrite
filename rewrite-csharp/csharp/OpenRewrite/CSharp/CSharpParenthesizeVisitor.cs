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
            or SwitchExpression or WithExpression or Lambda))
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
        return MaybeWrapTernary(t);
    }

    public override J VisitTypeCast(TypeCast cast, P p)
    {
        var c = _recursive ? (TypeCast)base.VisitTypeCast(cast, p) : cast;
        // TypeCast is precedence 13 (unary level), so it only needs parens
        // when the parent binds tighter — use normal precedence comparison
        return MaybeWrapBinaryLike(c);
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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private J MaybeWrapBinaryLike(Expression expr)
    {
        if (NeedsParenthesesInContext(expr))
            return CSharpPrecedences.Parenthesize(expr);
        return expr;
    }

    private J MaybeWrapUnaryInContext(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        // All prefix unary operators are at the same precedence level (13),
        // so a unary inside another unary is always unambiguous — no parens needed.
        // This covers !!x, -(-x), -(~x), ~(!x), etc.
        if (parent is Unary or CsUnary)
            return expr;

        // Unary inside binary/other contexts follows normal precedence rules
        if (NeedsParenthesesInContext(expr))
            return CSharpPrecedences.Parenthesize(expr);

        return expr;
    }

    private J MaybeWrapTernary(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        // Ternary (prec -1) needs parens inside most expression contexts.
        // Ternary-inside-ternary: C# parses `a ? b : c ? d : e` right-to-left,
        // so a ternary used as the condition of another ternary needs parens.
        if (parent is Binary or Unary or CsBinary or CsUnary or IsPattern or Ternary)
            return CSharpPrecedences.Parenthesize(expr);

        return expr;
    }

    private J MaybeWrapAssignment(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        // Assignment/AssignmentOperation/Lambda need parens inside Binary, Unary, Ternary, CsBinary, CsUnary, IsPattern
        if (parent is Binary or Unary or Ternary or CsBinary or CsUnary or IsPattern)
            return CSharpPrecedences.Parenthesize(expr);

        return expr;
    }

    private bool NeedsParenthesesInContext(Expression expr)
    {
        var parentCursor = Cursor.ParentTree;
        var parent = parentCursor.Value;

        if (parent is Unary or CsUnary)
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

        if (parent is IsPattern)
        {
            // CsBinary with Or/And inside IsPattern is a pattern combinator, not a
            // boolean expression — it must NOT be wrapped in parentheses.
            if (expr is CsBinary csb && csb.Operator.Element is CsBinary.OperatorType.Or or CsBinary.OperatorType.And)
                return false;

            return expr is Binary or CsBinary or Ternary or Assignment or AssignmentOperation or Lambda;
        }

        return false;
    }
}
