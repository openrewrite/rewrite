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
using OpenRewrite.Test;
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

public class TypeAttributionTests : RewriteTest
{
    private const string CallsFoo =
        """
        class C
        {
            void M()
            {
                Foo();
            }

            void Foo() { }
            void Bar() { }
        }
        """;

    private const string CallsBar =
        """
        class C
        {
            void M()
            {
                Bar();
            }

            void Foo() { }
            void Bar() { }
        }
        """;

    [Fact]
    public void SourceThatDoesNotCompileIsNotChecked()
    {
        RewriteRun(
            CSharp(
                """
                class A
                {
                    void M()
                    {
                        Undefined();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OutputWithoutTheTypesItsInputHadFails()
    {
        var failure = Assert.ThrowsAny<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new CallBarWithoutItsMethod()),
            CSharp(CallsFoo, CallsBar)
        ));

        Assert.Contains("MethodInvocation type is missing: Bar()", failure.Message);
    }

    [Fact]
    public void OutputIsUncheckedWhereItsInputDoesNotCompileOrWhenOptedOut()
    {
        RewriteRun(
            spec => spec.SetRecipe(new CallBarWithoutItsMethod()),
            CSharp(CallsFoo.Replace("Foo();", "Foo(); Undefined();"), CallsBar.Replace("Bar();", "Bar(); Undefined();"))
        );
        RewriteRun(
            spec => spec.SetRecipe(new CallBarWithoutItsMethod())
                .SetValidations(new Validations { OutputTypeAttribution = false }),
            CSharp(CallsFoo, CallsBar)
        );
    }

    private class CallBarWithoutItsMethod : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Call Bar without its method";
        public override string Description => "Renames calls to Foo to Bar and drops their method type.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
                return call.Name.SimpleName == "Foo"
                    ? call.WithName(call.Name.WithSimpleName("Bar")).WithMethodType(null)
                    : call;
            }
        }
    }
}
