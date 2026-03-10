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
using static OpenRewrite.Java.J;
using static OpenRewrite.Recipes.Net9.Net9RecipeHelpers;

namespace OpenRewrite.Recipes.Net9;

/// <summary>
/// Finds new IncrementingPollingCounter() constructor calls. In .NET 9, the initial
/// callback invocation is asynchronous instead of synchronous, which may change timing behavior.
/// </summary>
class IncrementingPollingCounterSearchRecipe : Recipe
{
    private const string IncrementingPollingCounterFqn = "System.Diagnostics.Tracing.IncrementingPollingCounter";

    public override string DisplayName => "Find `IncrementingPollingCounter` async callback change";

    public override string Description =>
        "Finds `new IncrementingPollingCounter()` constructor calls. In .NET 9, the initial " +
        "callback invocation is asynchronous instead of synchronous, which may change timing behavior.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "diagnostics" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "IncrementingPollingCounter initial callback is now asynchronous in .NET 9. " +
            "Code that depends on the callback being invoked synchronously during construction may break.";

        public override J VisitNewClass(NewClass newClass, Core.ExecutionContext ctx)
        {
            newClass = (NewClass)base.VisitNewClass(newClass, ctx);

            var isMatch = newClass.ConstructorType?.DeclaringType is JavaType.Class cls &&
                          cls.FullyQualifiedName == IncrementingPollingCounterFqn;

            if (!isMatch && newClass.ConstructorType == null &&
                newClass.Clazz is Identifier clazzId &&
                clazzId.SimpleName == "IncrementingPollingCounter")
            {
                isMatch = true;
            }

            if (isMatch)
            {
                return AddWarnMarker(newClass, TodoMessage);
            }

            return newClass;
        }
    }
}
