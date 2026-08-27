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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class LiteralTests : RewriteTest
{
    [Fact]
    public void StringLiteral()
    {
        RewriteRun(
            CSharp(
                """
                "hello";
                """
            )
        );
    }

    [Fact]
    public void NumericLiteral()
    {
        RewriteRun(
            CSharp(
                """
                42;
                """
            )
        );
    }

    [Fact]
    public void BooleanLiteral()
    {
        RewriteRun(
            CSharp(
                """
                true;
                """
            )
        );
    }

    [Fact]
    public void NullLiteral()
    {
        RewriteRun(
            CSharp(
                """
                null;
                """
            )
        );
    }

    [Fact]
    public void CharLiteral()
    {
        RewriteRun(
            CSharp(
                """
                'a';
                """
            )
        );
    }

    [Fact]
    public void StringLiteralWithLeadingWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                  "hello";
                """
            )
        );
    }

    [Fact]
    public void MultipleLiterals()
    {
        RewriteRun(
            CSharp(
                """
                "hello";
                42;
                """
            )
        );
    }

    [Theory]
    [InlineData("0L", JavaType.PrimitiveKind.Long)]
    [InlineData("0l", JavaType.PrimitiveKind.Long)]
    [InlineData("42", JavaType.PrimitiveKind.Int)]
    [InlineData("1.0f", JavaType.PrimitiveKind.Float)]
    [InlineData("1.0F", JavaType.PrimitiveKind.Float)]
    [InlineData("1.0d", JavaType.PrimitiveKind.Double)]
    [InlineData("1.0D", JavaType.PrimitiveKind.Double)]
    [InlineData("1.0", JavaType.PrimitiveKind.Double)]
    public void NumericLiteralTypeAttribution(string literal, JavaType.PrimitiveKind expectedKind)
    {
        var cu = Parse($"{literal};");
        var lit = FindFirst<Literal>(cu);
        Assert.NotNull(lit);
        Assert.NotNull(lit.Type);
        Assert.IsType<JavaType.Primitive>(lit.Type);
        Assert.Equal(expectedKind, ((JavaType.Primitive)lit.Type).Kind);
    }

    [Theory]
    [InlineData("234.7m", JavaType.PrimitiveKind.Double, 234.7d)]
    [InlineData("42u", JavaType.PrimitiveKind.Long, 42L)]
    [InlineData("42UL", JavaType.PrimitiveKind.Long, 42L)]
    [InlineData("18446744073709551615UL", JavaType.PrimitiveKind.Long, -1L)] // ulong bits reinterpreted as signed
    [InlineData("4.5f", JavaType.PrimitiveKind.Float, 4.5f)]
    [InlineData("5L", JavaType.PrimitiveKind.Long, 5L)]
    [InlineData("1_000_000", JavaType.PrimitiveKind.Int, 1_000_000)]
    public void NumericLiteralValueMatchesDeclaredType(string literal, JavaType.PrimitiveKind expectedKind, object expectedValue)
    {
        var cu = Parse($"{literal};");
        var lit = FindFirst<Literal>(cu);
        Assert.NotNull(lit);
        var primitive = Assert.IsType<JavaType.Primitive>(lit.Type);
        Assert.Equal(expectedKind, primitive.Kind);
        Assert.NotNull(lit.Value);
        Assert.Equal(expectedValue.GetType(), lit.Value!.GetType());
        Assert.Equal(expectedValue, lit.Value);
        Assert.Equal(literal, lit.ValueSource);
        Assert.Equal($"{literal};", new CSharpPrinter<object>().Print(cu));
    }
}
