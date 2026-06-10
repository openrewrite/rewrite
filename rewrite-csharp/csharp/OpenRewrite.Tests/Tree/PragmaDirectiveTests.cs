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

public class PragmaDirectiveTests : RewriteTest
{
    [Fact]
    public void PragmaWarningDisable()
    {
        RewriteRun(
            CSharp(
                """
                #pragma warning disable CS0168
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaWarningDisableRestore()
    {
        RewriteRun(
            CSharp(
                """
                #pragma warning disable CS0168, CS0219
                class Foo {
                }
                #pragma warning restore CS0168, CS0219
                """
            )
        );
    }

    [Fact]
    public void PragmaWarningExtraSpaceBeforeWarning()
    {
        RewriteRun(
            CSharp(
                """
                #pragma  warning disable RS0026
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaWarningExtraSpaceBeforeAction()
    {
        RewriteRun(
            CSharp(
                """
                #pragma warning  disable RS0026
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaWarningExtraSpacesEverywhere()
    {
        RewriteRun(
            CSharp(
                """
                #pragma   warning   restore   RS0026
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaChecksum()
    {
        RewriteRun(
            CSharp(
                """
                #pragma checksum "file.cs" "{ff1816ec-aa5e-4d10-87f7-6f4963833460}" "ab007f1d23d9"
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaChecksumExtraSpaceBeforeKeyword()
    {
        RewriteRun(
            CSharp(
                """
                #pragma   checksum "file.cs" "{ff1816ec-aa5e-4d10-87f7-6f4963833460}" "ab007f1d23d9"
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void PragmaBeforeConditionalDirectiveWithAssembly()
    {
        RewriteRun(
            CSharp(
                """
                #pragma warning disable SA1403 // single namespace

                #if NET
                // context
                [assembly: System.Runtime.CompilerServices.TypeForwardedTo(typeof(System.Runtime.CompilerServices.IsExternalInit))]
                #endif
                """
            )
        );
    }
}
