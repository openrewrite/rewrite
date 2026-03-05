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
}
