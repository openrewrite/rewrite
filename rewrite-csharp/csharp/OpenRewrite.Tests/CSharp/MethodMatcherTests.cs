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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

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

    // =============================================================
    // Short name matching (e.g., "String" matches "System.String")
    // =============================================================

    [Fact]
    public void MatchesShortNameArgAgainstClassType()
    {
        // Pattern uses "String" (short name), param type is JavaType.Class("System.String")
        var mm = new MethodMatcher("System.Security.Cryptography.Aes Create(String)");
        var method = MakeMethod("System.Security.Cryptography.Aes", "Create",
            parameterTypes: [MakeClass("System.String")]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void MatchesKeywordArgAgainstClassType()
    {
        // Pattern uses "string" (C# keyword), param type is JavaType.Class("System.String")
        var mm = new MethodMatcher("System.Security.Cryptography.Aes Create(string)");
        var method = MakeMethod("System.Security.Cryptography.Aes", "Create",
            parameterTypes: [MakeClass("System.String")]);
        Assert.True(mm.Matches(method));
    }

    // =============================================================
    // Array arguments
    // =============================================================

    private static JavaType.Array ArrayOf(JavaType elemType) => new(elemType, null);

    private static JavaType Int => JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);

    [Theory]
    [InlineData("string[]")]
    [InlineData("System.String[]")]
    [InlineData("String[]")]
    [InlineData("*[]")]
    [InlineData("System.*[]")]
    public void MatchesStringArrayArg(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(MakeClass("System.String"))]);
        Assert.True(mm.Matches(method));
    }

    [Theory]
    [InlineData("string[]")]
    [InlineData("System.String[]")]
    public void MatchesArrayOfStringPrimitive(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M",
            parameterTypes: [ArrayOf(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String))]);
        Assert.True(mm.Matches(method));
    }

    [Theory]
    [InlineData("int[]")]
    [InlineData("System.Int32[]")]
    public void MatchesPrimitiveArrayArgRepresentedAsClass(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(MakeClass("System.Int32"))]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void MatchesPrimitiveArrayArg()
    {
        var mm = new MethodMatcher("N.C M(int[])");
        var method = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(Int)]);
        Assert.True(mm.Matches(method));
    }

    [Fact]
    public void MatchesJaggedArrayArg()
    {
        var mm = new MethodMatcher("N.C M(int[][], string[][][])");
        var method = MakeMethod("N.C", "M", parameterTypes:
        [
            ArrayOf(ArrayOf(Int)),
            ArrayOf(ArrayOf(ArrayOf(MakeClass("System.String"))))
        ]);
        Assert.True(mm.Matches(method));
    }

    [Theory]
    [InlineData("string[]")]
    [InlineData("int[][]")]
    [InlineData("int")]
    [InlineData("*")]
    public void DoesNotMatchIntArrayArg(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(Int)]);
        Assert.False(mm.Matches(method));
    }

    [Theory]
    [InlineData("int[]")]
    [InlineData("string[][]")]
    [InlineData("string")]
    public void DoesNotMatchStringArrayArg(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(MakeClass("System.String"))]);
        Assert.False(mm.Matches(method));
    }

    [Theory]
    [InlineData("string[]")]
    [InlineData("*[]")]
    public void ArrayPatternDoesNotMatchScalarArg(string arg)
    {
        var mm = new MethodMatcher($"N.C M({arg})");
        var method = MakeMethod("N.C", "M", parameterTypes: [MakeClass("System.String")]);
        Assert.False(mm.Matches(method));
    }

    [Fact]
    public void ArrayPatternDoesNotMatchArrayWithoutElementType()
    {
        var mm = new MethodMatcher("N.C M(*[])");
        var method = MakeMethod("N.C", "M", parameterTypes: [new JavaType.Array()]);
        Assert.False(mm.Matches(method));
    }

    [Fact]
    public void SelectsArrayOverloadOverScalarOverload()
    {
        var mm = new MethodMatcher("N.C M(string[])");
        var arrayOverload = MakeMethod("N.C", "M", parameterTypes: [ArrayOf(MakeClass("System.String"))]);
        var scalarOverload = MakeMethod("N.C", "M", parameterTypes: [MakeClass("System.String")]);
        Assert.True(mm.Matches(arrayOverload));
        Assert.False(mm.Matches(scalarOverload));
    }

    [Fact]
    public void ArrayArgCombinesWithWildcardArgs()
    {
        var mm = new MethodMatcher("N.C M(.., string[])");
        var method = MakeMethod("N.C", "M",
            parameterTypes: [Int, MakeClass("System.String"), ArrayOf(MakeClass("System.String"))]);
        Assert.True(mm.Matches(method));
        Assert.True(new MethodMatcher("N.C M(..)").Matches(method));
        Assert.False(new MethodMatcher("N.C M(.., string)").Matches(method));
    }

    [Fact]
    public void NameOnlyFallbackMatchesVerbatimInvocationName()
    {
        var mm = new MethodMatcher("* event(..)");
        var mi = new MethodInvocation(
            Guid.NewGuid(), OpenRewrite.Core.Space.Empty, OpenRewrite.Core.Markers.Empty,
            null, TestHelpers.MakeId("@event"), null,
            new JContainer<Expression>(OpenRewrite.Core.Space.Empty, [], OpenRewrite.Core.Markers.Empty),
            null);
        Assert.True(mm.Matches(mi));
    }

    [Fact]
    public void NameOnlyFallbackMatchesEscapedDeclarationName()
    {
        var mm = new MethodMatcher("* ab(..)");
        var cu = new CSharpParser().Parse("class A { void \\u0061b() { } }");
        var md = (MethodDeclaration)((ClassDeclaration)cu.Members[0].Element).Body.Statements[0].Element;
        Assert.True(mm.Matches(md.WithMethodType(null)));
    }
}
