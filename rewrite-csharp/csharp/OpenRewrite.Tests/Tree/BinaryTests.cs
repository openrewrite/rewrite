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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class BinaryTests : RewriteTest
{
    [Theory]
    [InlineData("1 + 2;")]
    [InlineData("5 - 3;")]
    [InlineData("4 * 2;")]
    [InlineData("10 / 2;")]
    [InlineData("10 % 3;")]
    [InlineData("1 < 2;")]
    [InlineData("1 > 2;")]
    [InlineData("1 <= 2;")]
    [InlineData("1 >= 2;")]
    [InlineData("x == y;")]
    [InlineData("x != y;")]
    [InlineData("a && b;")]
    [InlineData("a || b;")]
    [InlineData("a & b;")]
    [InlineData("a | b;")]
    [InlineData("a ^ b;")]
    [InlineData("a << 2;")]
    [InlineData("a >> 2;")]
    public void BinaryOperator(string source)
    {
        RewriteRun(CSharp(source));
    }

    [Fact]
    public void ChainedBinary()
    {
        RewriteRun(
            CSharp(
                """
                1 + 2 + 3;
                """
            )
        );
    }

    [Fact]
    public void BinaryWithIdentifiers()
    {
        RewriteRun(
            CSharp(
                """
                a + b;
                """
            )
        );
    }

    [Fact]
    public void BinaryWithExtraSpaces()
    {
        RewriteRun(
            CSharp(
                """
                1  +  2;
                """
            )
        );
    }
}
