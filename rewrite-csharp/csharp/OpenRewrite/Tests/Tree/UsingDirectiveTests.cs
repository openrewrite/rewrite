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

public class UsingDirectiveTests : RewriteTest
{
    [Fact]
    public void SimpleUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                """
            )
        );
    }

    [Fact]
    public void QualifiedUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                """
            )
        );
    }

    [Fact]
    public void UsingWithClass()
    {
        RewriteRun(
            CSharp(
                """
                using System;

                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleUsings()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                using System.Collections.Generic;
                using System.Linq;
                """
            )
        );
    }

    [Fact]
    public void UsingStatic()
    {
        RewriteRun(
            CSharp(
                """
                using static System.Math;
                """
            )
        );
    }

    [Fact]
    public void UsingAlias()
    {
        RewriteRun(
            CSharp(
                """
                using Sys = System;
                """
            )
        );
    }

    [Fact]
    public void UsingAliasQualified()
    {
        RewriteRun(
            CSharp(
                """
                using Col = System.Collections.Generic;
                """
            )
        );
    }

    [Fact]
    public void UsingUnsafe()
    {
        RewriteRun(
            CSharp(
                """
                using unsafe System.Int32;
                """
            )
        );
    }

    [Fact]
    public void UsingStaticUnsafe()
    {
        RewriteRun(
            CSharp(
                """
                using static unsafe System.Int32;
                """
            )
        );
    }

    [Fact]
    public void UsingUnsafeAlias()
    {
        RewriteRun(
            CSharp(
                """
                using unsafe Ptr = int*;
                """
            )
        );
    }

}
