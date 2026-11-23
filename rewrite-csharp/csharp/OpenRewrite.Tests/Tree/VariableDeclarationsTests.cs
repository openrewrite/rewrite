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

public class VariableDeclarationsTests : RewriteTest
{
    [Fact]
    public void VarWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1;
                """
            )
        );
    }

    [Fact]
    public void IntWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                int x = 1;
                """
            )
        );
    }

    [Fact]
    public void StringWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                string s = "hello";
                """
            )
        );
    }

    [Fact]
    public void BoolWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                bool b = true;
                """
            )
        );
    }

    [Fact]
    public void MultipleVariables()
    {
        RewriteRun(
            CSharp(
                """
                int x = 1, y = 2;
                """
            )
        );
    }

    [Fact]
    public void ConstVariable()
    {
        RewriteRun(
            CSharp(
                """
                const int x = 1;
                """
            )
        );
    }

    [Fact]
    public void VariableWithExpression()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1 + 2;
                """
            )
        );
    }

    [Fact]
    public void VariableWithLeadingWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                  var x = 1;
                """
            )
        );
    }

    [Fact]
    public void MultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1;
                var y = 2;
                """
            )
        );
    }

    [Fact]
    public void VariableWithExtraSpaces()
    {
        RewriteRun(
            CSharp(
                """
                var  x  =  1;
                """
            )
        );
    }
}
