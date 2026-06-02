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

public class ParenthesesTests : RewriteTest
{
    [Fact]
    public void ParenthesizedLiteral()
    {
        RewriteRun(
            CSharp(
                """
                (42);
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedIdentifier()
    {
        RewriteRun(
            CSharp(
                """
                (foo);
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedBinary()
    {
        RewriteRun(
            CSharp(
                """
                (1 + 2);
                """
            )
        );
    }

    [Fact]
    public void NestedParentheses()
    {
        RewriteRun(
            CSharp(
                """
                ((42));
                """
            )
        );
    }

    [Fact]
    public void ParenthesesWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                ( 42 );
                """
            )
        );
    }

    [Fact]
    public void ParenthesesInBinary()
    {
        RewriteRun(
            CSharp(
                """
                (1 + 2) * 3;
                """
            )
        );
    }

    [Fact]
    public void ComplexExpression()
    {
        RewriteRun(
            CSharp(
                """
                (a + b) * (c - d);
                """
            )
        );
    }
}
