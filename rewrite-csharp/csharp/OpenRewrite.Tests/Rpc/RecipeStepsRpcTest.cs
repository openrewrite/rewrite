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
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A Java recipe that runs before and after a C# recipe finds the methods it looks for by the types the C# recipe
/// left, which only holds when those types survive the trip to Java.
/// </summary>
public class RecipeStepsRpcTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    [Fact]
    public void JavaMatchesTheTypesTheCSharpRecipeLeft()
    {
        RewriteRun(
            spec => spec.SetRecipes(
                new ChangeMethodName("Test Touch()", "BeforeRecipe"),
                new ChangeLiteral("1", "0"),
                new ChangeMethodName("System.Security.Cryptography.HashAlgorithm ComputeHash(byte[], int, int)",
                    "ComputeHashForTest")),
            CSharp(
                """
                using System.Security.Cryptography;
                class Test
                {
                    byte[] Make(byte[] data) { Touch(); return SHA256.Create().ComputeHash(data, 1, data.Length); }
                    void Touch() { }
                }
                """,
                """
                using System.Security.Cryptography;
                class Test
                {
                    byte[] Make(byte[] data) { BeforeRecipe(); return SHA256.Create().ComputeHashForTest(data, 0, data.Length); }
                    void BeforeRecipe() { }
                }
                """
            )
        );
    }

    private class ChangeMethodName(string methodPattern, string newMethodName)
        : global::OpenRewrite.Core.Recipe, IDelegatesTo
    {
        public override string DisplayName => "Change method name";
        public override string Description => "Renames a method in Java.";

        public string JavaRecipeName => "org.openrewrite.java.ChangeMethodName";

        public Dictionary<string, object?> Options => new()
        {
            ["methodPattern"] = methodPattern,
            ["newMethodName"] = newMethodName
        };

        public override ITreeVisitor<ExecutionContext> GetVisitor()
        {
            var rpc = RewriteRpcServer.Current!;
            return new RpcVisitor(rpc, rpc.PrepareRecipeOnRemote(JavaRecipeName, Options).EditVisitor);
        }
    }

    private class ChangeLiteral(string from, string to) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change a literal";
        public override string Description => "Changes the integer literals of one value to another.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to);

        private class Visitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitLiteral(Literal literal, ExecutionContext ctx) =>
                literal.ValueSource == from
                    ? literal.WithValue(int.Parse(to)).WithValueSource(to)
                    : base.VisitLiteral(literal, ctx);
        }
    }
}
