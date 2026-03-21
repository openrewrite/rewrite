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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

internal static class TestHelpers
{
    public static Identifier MakeId(string name) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, [], name, null, null);

    public static Identifier MakeIdWithType(string name, JavaType type) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, [], name, type, null);

    public static FieldAccess MakeFieldAccess(string target, string name) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty,
            MakeId(target),
            new JLeftPadded<Identifier>(Space.Empty, MakeId(name)),
            null);

    public static Annotation MakeAnnotation(NameTree annotationType) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, annotationType, null);

    public static JRightPadded<T> Pad<T>(T element) =>
        new(element, Space.Empty, Markers.Empty);

    public static Binary MakeBinary(Binary.OperatorType op, Expression left, Expression right) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, left,
            new JLeftPadded<Binary.OperatorType>(Space.Empty, op),
            right, null);

    public static CsBinary MakeCsBinary(CsBinary.OperatorType op, Expression left, Expression right) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, left,
            new JLeftPadded<CsBinary.OperatorType>(Space.Empty, op),
            right, null);

    public static Unary MakeUnary(Unary.OperatorType op, Expression expr) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new JLeftPadded<Unary.OperatorType>(Space.Empty, op),
            expr, null);

    public static Ternary MakeTernary(Expression cond, Expression trueExpr, Expression falseExpr) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, cond,
            new JLeftPadded<Expression>(Space.Empty, trueExpr),
            new JLeftPadded<Expression>(Space.Empty, falseExpr),
            null);

    public static TypeCast MakeTypeCast(Expression expr) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new ControlParentheses<TypeTree>(Guid.NewGuid(), Space.Empty, Markers.Empty,
                new JRightPadded<TypeTree>(MakeId("int"), Space.Empty, Markers.Empty)),
            expr);

    public static Assignment MakeAssignment(Expression variable, Expression value) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, variable,
            new JLeftPadded<Expression>(Space.Empty, value),
            null);

    public static Parentheses<Expression> MakeParens(Expression expr) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new JRightPadded<Expression>(expr, Space.Empty, Markers.Empty));
}
