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
/// Removes parentheses from a specific <see cref="Parentheses{Expression}"/> node when safe to do so.
/// Delegates to <see cref="IsUnwrappable"/> to decide whether removal preserves semantics.
/// </summary>
public class CSharpUnwrapParentheses<P> : CSharpVisitor<P>
{
    private readonly Parentheses<Expression> _scope;

    public CSharpUnwrapParentheses(Parentheses<Expression> scope)
    {
        _scope = scope;
    }

    public override J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        if (_scope.Id == parens.Id && IsUnwrappable(Cursor))
        {
            var tree = J.SetPrefix(parens.Tree.Element, parens.Prefix);
            if (tree.Prefix.Equals(Space.Empty))
            {
                var parent = Cursor.ParentTree.Value;
                if (parent is Return or Throw)
                    tree = J.SetPrefix(tree, Space.SingleSpace);
            }
            return tree;
        }
        return base.VisitParentheses(parens, p);
    }

    /// <summary>
    /// Determines whether the parentheses at the given cursor position can be safely removed
    /// without changing the semantics of the expression.
    /// </summary>
    public static bool IsUnwrappable(Cursor parensCursor)
    {
        if (parensCursor.Value is not Parentheses<Expression> parens) return false;
        var parent = parensCursor.ParentTree.Value;

        // Cannot unwrap structural parentheses required by control-flow statements
        if (parent is If or Switch or WhileLoop or ForLoop or ForEachLoop or TypeCast) return false;
        if (parent is DoWhileLoop doWhile && parens.Id == doWhile.Condition.Element.Id) return false;

        // Cannot unwrap inside unary when inner is a compound expression
        if (parent is Unary or CsUnary)
        {
            var inner = parens.Tree.Element;
            if (inner is Binary or CsBinary or Assignment or AssignmentOperation or Ternary or IsPattern)
                return false;
        }

        // General case: delegate to precedence check
        if (parent is Expression parentExpr)
        {
            var inner = parens.Tree.Element;
            if (inner is Expression innerExpr)
            {
                bool isRight = false;
                if (parent is Binary parentBinary)
                    isRight = parentBinary.Right.Id == parens.Id;
                else if (parent is CsBinary parentCsBinary)
                    isRight = parentCsBinary.Right.Id == parens.Id;
                if (CSharpPrecedences.NeedsParentheses(innerExpr, parentExpr, isRight))
                    return false;
            }
        }

        return true;
    }
}
