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

internal static class CSharpPrecedences
{
    public static int GetPrecedence(Expression expr) => expr switch
    {
        Parentheses<Expression> => int.MaxValue,
        Binary b => b.Operator.Element switch
        {
            Binary.OperatorType.Multiplication or Binary.OperatorType.Division or Binary.OperatorType.Modulo => 10,
            Binary.OperatorType.Addition or Binary.OperatorType.Subtraction => 9,
            Binary.OperatorType.LeftShift or Binary.OperatorType.RightShift or Binary.OperatorType.UnsignedRightShift => 8,
            Binary.OperatorType.LessThan or Binary.OperatorType.GreaterThan or Binary.OperatorType.LessThanOrEqual or Binary.OperatorType.GreaterThanOrEqual => 7,
            Binary.OperatorType.Equal or Binary.OperatorType.NotEqual => 6,
            Binary.OperatorType.BitAnd => 5,
            Binary.OperatorType.BitXor => 4,
            Binary.OperatorType.BitOr => 3,
            Binary.OperatorType.And => 2,
            Binary.OperatorType.Or => 1,
            _ => 0
        },
        CsBinary csb => csb.Operator.Element switch
        {
            CsBinary.OperatorType.As => 7,
            CsBinary.OperatorType.NullCoalescing => 0,
            CsBinary.OperatorType.And => 2,
            CsBinary.OperatorType.Or => 1,
            _ => 0
        },
        Unary => 13,
        CsUnary => 13,
        TypeCast => 13,
        IsPattern => 7,
        RangeExpression => 12,
        SwitchExpression => 11,
        WithExpression => 11,
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => 0,
        Ternary => -1,
        Assignment => -2,
        AssignmentOperation => -2,
        Lambda => -2,
        _ => int.MaxValue
    };

    public static bool NeedsParentheses(Expression child, Expression parent, bool isRightOperand)
    {
        int childPrec = GetPrecedence(child);
        int parentPrec = GetPrecedence(parent);
        if (childPrec > parentPrec) return false;
        if (childPrec < parentPrec) return true;
        if (isRightOperand && IsRightAssociative(child) && IsRightAssociative(parent)) return false;
        if (IsSameOperator(child, parent) && IsAssociative(child)) return false;
        if (IsInSameMathGroup(child, parent)) return isRightOperand;
        return !IsSameOperator(child, parent);
    }

    public static Parentheses<Expression> Parenthesize(Expression expr)
    {
        if (expr is Parentheses<Expression> p) return p;
        return new Parentheses<Expression>(
            Guid.NewGuid(), expr.Prefix, Markers.Empty,
            new JRightPadded<Expression>(J.SetPrefix(expr, Space.Empty), Space.Empty, Markers.Empty));
    }

    public static bool IsAssociative(Expression expr) => expr switch
    {
        Binary b => b.Operator.Element is
            Binary.OperatorType.Addition or Binary.OperatorType.Multiplication or
            Binary.OperatorType.BitAnd or Binary.OperatorType.BitOr or Binary.OperatorType.BitXor or
            Binary.OperatorType.And or Binary.OperatorType.Or,
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => true,
        CsBinary csb => csb.Operator.Element is CsBinary.OperatorType.NullCoalescing,
        _ => false
    };

    private static bool IsRightAssociative(Expression expr) => expr switch
    {
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => true,
        CsBinary csb => csb.Operator.Element is CsBinary.OperatorType.NullCoalescing,
        Assignment => true,
        AssignmentOperation => true,
        _ => false
    };

    private static bool IsSameOperator(Expression a, Expression b) => (a, b) switch
    {
        (Binary ba, Binary bb) => ba.Operator.Element == bb.Operator.Element,
        (CsBinary ca, CsBinary cb) => ca.Operator.Element == cb.Operator.Element,
        (Ternary ta, Ternary tb) when
            ta.Markers.MarkerList.Any(m => m is NullCoalescing) &&
            tb.Markers.MarkerList.Any(m => m is NullCoalescing) => true,
        _ => false
    };

    private static bool IsInSameMathGroup(Expression a, Expression b)
    {
        if (a is not Binary ba || b is not Binary bb) return false;
        return (IsAddSub(ba.Operator.Element) && IsAddSub(bb.Operator.Element)) ||
               (IsMulDivMod(ba.Operator.Element) && IsMulDivMod(bb.Operator.Element));
    }

    private static bool IsAddSub(Binary.OperatorType op) =>
        op is Binary.OperatorType.Addition or Binary.OperatorType.Subtraction;

    private static bool IsMulDivMod(Binary.OperatorType op) =>
        op is Binary.OperatorType.Multiplication or Binary.OperatorType.Division or Binary.OperatorType.Modulo;
}
