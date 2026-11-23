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

/// <summary>
/// Recipes that run after a recipe find nodes by their types and ids, so the harness fails a recipe that leaves a node
/// it makes without the type the compiler gives it, or puts one node in two places.
/// </summary>
public class TypesAndIdsTests : RewriteTest
{
    private const string Calls =
        """
        class C
        {
            void Foo() { }
            void Bar() { }
            void M()
            {
                Foo();
                int x = 1;
            }
        }
        """;

    private static readonly Validations TypesAttributed = new() { TypesAttributed = true };

    [Fact]
    public void ANodeTheRecipeLeavesUntypedFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeCall(call => call.WithMethodType(null))).SetValidations(TypesAttributed),
            CSharp(Calls, Calls.Replace("Foo();", "Bar();"))
        ));

        Assert.Equal(
            "Give the nodes the recipe makes the types the compiler resolves for them.\n" +
            "The output of OpenRewrite.Tests.Core.TypesAndIdsTests+ChangeCall holds nodes without the types the " +
            "compiler resolves for the text they print:\n" +
            "  source.cs:\n" +
            "    `Bar()` has no MethodType where the compiler resolves C{name=Bar,return=void,parameters=[]}\n" +
            "A recipe that runs after it, and a Java recipe that matches types, misses a node without its type. If " +
            "the output is meant to lack them, opt out with " +
            "spec.SetValidations(new Validations { TypesAttributed = false }).",
            failure.Message);
    }

    [Fact]
    public void ANodeKeptFromUntypedInputStaysUntyped()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral()).SetValidations(TypesAttributed),
            CSharp(Calls, Calls.Replace("int x = 1;", "int x = 2;"))
                .BeforeRecipe(cu => (CompilationUnit)new StripMethodTypes().Visit(cu, 0)!)
        );
    }

    [Fact]
    public void TypeAttributionIsNotCheckedUnlessTheTestAsks()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeCall(call => call.WithMethodType(null))),
            CSharp(Calls, Calls.Replace("Foo();", "Bar();"))
        );
    }

    [Fact]
    public void ANodeInTwoPlacesFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RepeatFirstStatement(copy => copy)),
            CSharp(Calls, Calls.Replace("Foo();", "Foo();\n        Foo();"))
        ));

        Assert.StartsWith(
            "Give every node of a copy a new id, such as with a visitor whose PreVisit returns " +
            "tree.WithId(Tree.RandomId()).\n" +
            "The output of OpenRewrite.Tests.Core.TypesAndIdsTests+RepeatFirstStatement holds nodes that share an " +
            "id:\n" +
            "  source.cs:\n" +
            "    ExpressionStatement ", failure.Message);
        Assert.Contains(" appears twice\nRecipes and tools find a node by its id", failure.Message);
    }

    [Fact]
    public void ACopyWithNewIdsPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RepeatFirstStatement(copy => (Statement)new NewIds().Visit(copy, 0)!)),
            CSharp(Calls, Calls.Replace("Foo();", "Foo();\n        Foo();"))
        );
    }

    [Fact]
    public void AnOptOutDisablesTheIdCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RepeatFirstStatement(copy => copy))
                .SetValidations(new Validations { UniqueIds = false }),
            CSharp(Calls, Calls.Replace("Foo();", "Foo();\n        Foo();"))
        );
    }

    private class StripMethodTypes : CSharpVisitor<int>
    {
        public override J VisitMethodInvocation(MethodInvocation method, int p) =>
            ((MethodInvocation)base.VisitMethodInvocation(method, p)).WithMethodType(null);
    }

    private class NewIds : CSharpVisitor<int>
    {
        public override J? PreVisit(J tree, int p) => (J)tree.WithId(global::OpenRewrite.Core.Tree.RandomId());
    }

    private class ChangeCall(Func<MethodInvocation, MethodInvocation> change) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change a call";
        public override string Description => "Renames the calls to Foo to Bar.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(change);

        private class Visitor(Func<MethodInvocation, MethodInvocation> change) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                return call.Name.SimpleName == "Foo" ? change(call.WithName(call.Name.WithSimpleName("Bar"))) : call;
            }
        }
    }

    private class ChangeLiteral : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change a literal";
        public override string Description => "Changes the literal 1 to 2.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitLiteral(Literal literal, ExecutionContext ctx) =>
                literal.ValueSource == "1" ? literal.WithValue(2).WithValueSource("2") : literal;
        }
    }

    /// <summary>Repeats the first statement of the method that calls Foo, once.</summary>
    private class RepeatFirstStatement(Func<Statement, Statement> copy) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Repeat the first statement";
        public override string Description => "Repeats the first statement of M.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(copy);

        private class Visitor(Func<Statement, Statement> copy) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodDeclaration(MethodDeclaration method, ExecutionContext ctx)
            {
                if (method.Name.SimpleName != "M" || method.Body is not { } body ||
                    body.Statements.Count != 2)
                {
                    return method;
                }
                var first = body.Statements[0];
                return method.WithBody(body.WithStatements(
                    [first, first.WithElement(copy(first.Element)), ..body.Statements.Skip(1)]));
            }
        }
    }
}
