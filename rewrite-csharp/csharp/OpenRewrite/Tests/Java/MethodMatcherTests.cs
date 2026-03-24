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
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Java;

public class MethodMatcherTests
{
    private static JavaType.Class MakeClass(string fqn, JavaType.FullyQualified? supertype = null) =>
        new() { FullyQualifiedName = fqn, Supertype = supertype };

    private static JavaType.Method MakeMethod(
        string declaringTypeFqn, string name,
        JavaType? returnType = null,
        IList<JavaType>? parameterTypes = null,
        IList<string>? parameterNames = null)
    {
        var declaring = MakeClass(declaringTypeFqn);
        return new JavaType.Method
        {
            Name = name,
            DeclaringType = declaring,
            ReturnType = returnType,
            ParameterTypes = parameterTypes,
            ParameterNames = parameterNames
        };
    }

    // =============================================================
    // Basic matching
    // =============================================================

    [Fact]
    public void MatchesExactMethodSignature()
    {
        var mm = new MethodMatcher("System.Threading.Tasks.Task Delay(int)");
        var method = MakeMethod("System.Threading.Tasks.Task", "Delay",
            parameterTypes: [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void DoesNotMatchWrongMethodName()
    {
        var mm = new MethodMatcher("System.Threading.Tasks.Task Delay(int)");
        var method = MakeMethod("System.Threading.Tasks.Task", "Run",
            parameterTypes: [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.False(mm.Matches(method));
    }

    [Fact]
    public void DoesNotMatchWrongDeclaringType()
    {
        var mm = new MethodMatcher("System.Threading.Tasks.Task Delay(int)");
        var method = MakeMethod("System.String", "Delay",
            parameterTypes: [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.False(mm.Matches(method));
    }

    // =============================================================
    // Wildcard arguments
    // =============================================================

    [Fact]
    public void MatchesWildcardArguments()
    {
        var mm = new MethodMatcher("System.Threading.Tasks.Task Delay(..)");
        var method0 = MakeMethod("System.Threading.Tasks.Task", "Delay");
        var method1 = MakeMethod("System.Threading.Tasks.Task", "Delay",
            parameterTypes: [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.True(mm.Matches(method0));
        Assert.True(mm.Matches(method1));
    }

    [Fact]
    public void MatchesNoArgs()
    {
        var mm = new MethodMatcher("System.String ToString()");
        var method = MakeMethod("System.String", "ToString");
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void NoArgsDoesNotMatchMethodWithArgs()
    {
        var mm = new MethodMatcher("System.String ToString()");
        var method = MakeMethod("System.String", "ToString",
            parameterTypes: [MakeClass("System.IFormatProvider")]);
        Assert.False(mm.Matches(method));
    }

    [Fact]
    public void MatchesTrailingWildcard()
    {
        var mm = new MethodMatcher("System.IO.File WriteAllTextAsync(string, ..)");
        var method = MakeMethod("System.IO.File", "WriteAllTextAsync",
            parameterTypes: [MakeClass("System.String"), MakeClass("System.String")]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void TrailingWildcardMatchesMinimum()
    {
        var mm = new MethodMatcher("System.IO.File WriteAllTextAsync(string, ..)");
        var method = MakeMethod("System.IO.File", "WriteAllTextAsync",
            parameterTypes: [MakeClass("System.String")]);
        Assert.True(mm.Matches(method));
    }

    // =============================================================
    // Wildcard type names
    // =============================================================

    [Fact]
    public void MatchesWildcardMethodName()
    {
        var mm = new MethodMatcher("System.String *(int)");
        var method = MakeMethod("System.String", "Substring",
            parameterTypes: [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void MatchesWildcardDeclaringType()
    {
        var mm = new MethodMatcher("*..* Delay(..)");
        var method = MakeMethod("System.Threading.Tasks.Task", "Delay");
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void MatchesPartialWildcardType()
    {
        var mm = new MethodMatcher("System.Threading.Tasks.* Delay(..)");
        var method = MakeMethod("System.Threading.Tasks.Task", "Delay");
        Assert.True(mm.Matches(method));
    }

    // =============================================================
    // matchOverrides
    // =============================================================

    [Fact]
    public void MatchesOverride_WhenEnabled()
    {
        var baseClass = MakeClass("BaseClass");
        var derived = MakeClass("DerivedClass", supertype: baseClass);
        var method = MakeMethod("DerivedClass", "DoWork");
        method.DeclaringType = derived;

        var mm = new MethodMatcher("BaseClass DoWork(..)", matchOverrides: true);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void DoesNotMatchOverride_WhenDisabled()
    {
        var baseClass = MakeClass("BaseClass");
        var derived = MakeClass("DerivedClass", supertype: baseClass);
        var method = MakeMethod("DerivedClass", "DoWork");
        method.DeclaringType = derived;

        var mm = new MethodMatcher("BaseClass DoWork(..)");
        Assert.False(mm.Matches(method));
    }

    // =============================================================
    // No declaring type in pattern
    // =============================================================

    [Fact]
    public void MatchesWithoutDeclaringType()
    {
        var mm = new MethodMatcher("Delay(..)");
        var method = MakeMethod("System.Threading.Tasks.Task", "Delay");
        Assert.True(mm.Matches(method));
    }

    // =============================================================
    // Multiple argument types
    // =============================================================

    [Fact]
    public void MatchesMultipleArgs()
    {
        var mm = new MethodMatcher("System.String Equals(string, System.StringComparison)");
        var method = MakeMethod("System.String", "Equals",
            parameterTypes: [MakeClass("System.String"), MakeClass("System.StringComparison")]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void DoesNotMatchWrongArgCount()
    {
        var mm = new MethodMatcher("System.String Equals(string)");
        var method = MakeMethod("System.String", "Equals",
            parameterTypes: [MakeClass("System.String"), MakeClass("System.StringComparison")]);
        Assert.False(mm.Matches(method));
    }
}
