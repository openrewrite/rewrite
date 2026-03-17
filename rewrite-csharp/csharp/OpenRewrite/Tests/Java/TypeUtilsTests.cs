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

public class TypeUtilsTests
{
    private static JavaType.Class MakeClass(
        string fqn,
        JavaType.FullyQualified? supertype = null,
        IList<JavaType.FullyQualified>? interfaces = null,
        IList<JavaType.Method>? methods = null)
    {
        return new JavaType.Class
        {
            FullyQualifiedName = fqn,
            Supertype = supertype,
            Interfaces = interfaces,
            Methods = methods
        };
    }

    private static JavaType.Method MakeMethod(string name, JavaType.FullyQualified? declaringType = null,
        JavaType? returnType = null, IList<JavaType>? parameterTypes = null,
        IList<string>? parameterNames = null)
    {
        return new JavaType.Method
        {
            Name = name,
            DeclaringType = declaringType,
            ReturnType = returnType,
            ParameterTypes = parameterTypes,
            ParameterNames = parameterNames
        };
    }

    // =============================================================
    // IsOfClassType
    // =============================================================

    [Fact]
    public void IsOfClassType_ExactMatch()
    {
        var cls = MakeClass("System.String");
        Assert.True(TypeUtils.IsOfClassType(cls, "System.String"));
    }

    [Fact]
    public void IsOfClassType_NoMatch()
    {
        var cls = MakeClass("System.String");
        Assert.False(TypeUtils.IsOfClassType(cls, "System.Int32"));
    }

    [Fact]
    public void IsOfClassType_NullType()
    {
        Assert.False(TypeUtils.IsOfClassType(null, "System.String"));
    }

    // =============================================================
    // IsAssignableTo
    // =============================================================

    [Fact]
    public void IsAssignableTo_SameType()
    {
        var cls = MakeClass("System.String");
        Assert.True(TypeUtils.IsAssignableTo(cls, "System.String"));
    }

    [Fact]
    public void IsAssignableTo_Supertype()
    {
        var obj = MakeClass("System.Object");
        var str = MakeClass("System.String", supertype: obj);
        Assert.True(TypeUtils.IsAssignableTo(str, "System.Object"));
    }

    [Fact]
    public void IsAssignableTo_Interface()
    {
        var disposable = MakeClass("System.IDisposable");
        var stream = MakeClass("System.IO.Stream", interfaces: [disposable]);
        Assert.True(TypeUtils.IsAssignableTo(stream, "System.IDisposable"));
    }

    [Fact]
    public void IsAssignableTo_TransitiveSupertype()
    {
        var obj = MakeClass("System.Object");
        var marshalByRef = MakeClass("System.MarshalByRefObject", supertype: obj);
        var stream = MakeClass("System.IO.Stream", supertype: marshalByRef);
        Assert.True(TypeUtils.IsAssignableTo(stream, "System.Object"));
    }

    [Fact]
    public void IsAssignableTo_NoMatch()
    {
        var cls = MakeClass("System.String");
        Assert.False(TypeUtils.IsAssignableTo(cls, "System.Int32"));
    }

    [Fact]
    public void IsAssignableTo_NullType()
    {
        Assert.False(TypeUtils.IsAssignableTo(null, "System.String"));
    }

    [Fact]
    public void IsAssignableTo_MultipleTargets()
    {
        var disposable = MakeClass("System.IDisposable");
        var stream = MakeClass("System.IO.Stream", interfaces: [disposable]);
        Assert.True(TypeUtils.IsAssignableTo(stream, ["System.Int32", "System.IDisposable"]));
        Assert.False(TypeUtils.IsAssignableTo(stream, ["System.Int32", "System.String"]));
    }

    // =============================================================
    // InheritsFrom
    // =============================================================

    [Fact]
    public void InheritsFrom_DirectParent()
    {
        var obj = MakeClass("System.Object");
        var str = MakeClass("System.String", supertype: obj);
        Assert.True(TypeUtils.InheritsFrom(str, "System.Object"));
    }

    [Fact]
    public void InheritsFrom_DoesNotMatchSelf()
    {
        var cls = MakeClass("System.String");
        Assert.False(TypeUtils.InheritsFrom(cls, "System.String"));
    }

    // =============================================================
    // Implements
    // =============================================================

    [Fact]
    public void Implements_DirectInterface()
    {
        var disposable = MakeClass("System.IDisposable");
        var cls = MakeClass("MyClass", interfaces: [disposable]);
        Assert.True(TypeUtils.Implements(cls, "System.IDisposable"));
    }

    [Fact]
    public void Implements_DoesNotMatchSelf()
    {
        var cls = MakeClass("System.IDisposable");
        Assert.False(TypeUtils.Implements(cls, "System.IDisposable"));
    }

    [Fact]
    public void Implements_InheritedInterface()
    {
        var disposable = MakeClass("System.IDisposable");
        var baseClass = MakeClass("BaseClass", interfaces: [disposable]);
        var derived = MakeClass("DerivedClass", supertype: baseClass);
        Assert.True(TypeUtils.Implements(derived, "System.IDisposable"));
    }

    // =============================================================
    // HasMethod
    // =============================================================

    [Fact]
    public void HasMethod_DirectMethod()
    {
        var method = MakeMethod("ConfigureAwait");
        var cls = MakeClass("System.Threading.Tasks.Task", methods: [method]);
        Assert.True(TypeUtils.HasMethod(cls, "ConfigureAwait"));
    }

    [Fact]
    public void HasMethod_InheritedMethod()
    {
        var method = MakeMethod("ToString");
        var obj = MakeClass("System.Object", methods: [method]);
        var str = MakeClass("System.String", supertype: obj);
        Assert.True(TypeUtils.HasMethod(str, "ToString"));
    }

    [Fact]
    public void HasMethod_NotFound()
    {
        var cls = MakeClass("MyClass");
        Assert.False(TypeUtils.HasMethod(cls, "NonExistent"));
    }

    // =============================================================
    // IsString / IsObject
    // =============================================================

    [Fact]
    public void IsString_ClassType()
    {
        Assert.True(TypeUtils.IsString(MakeClass("System.String")));
    }

    [Fact]
    public void IsString_Primitive()
    {
        Assert.True(TypeUtils.IsString(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String)));
    }

    [Fact]
    public void IsString_NotString()
    {
        Assert.False(TypeUtils.IsString(MakeClass("System.Int32")));
    }

    // =============================================================
    // GetFullyQualifiedName
    // =============================================================

    [Fact]
    public void GetFullyQualifiedName_Class()
    {
        Assert.Equal("System.String", TypeUtils.GetFullyQualifiedName(MakeClass("System.String")));
    }

    [Fact]
    public void GetFullyQualifiedName_Null()
    {
        Assert.Null(TypeUtils.GetFullyQualifiedName(null));
    }

    // =============================================================
    // AsClass
    // =============================================================

    [Fact]
    public void AsClass_UnwrapsParameterized()
    {
        var inner = MakeClass("System.Threading.Tasks.Task");
        var parameterized = new JavaType.Parameterized { Type = inner };
        var result = TypeUtils.AsClass(parameterized);
        Assert.NotNull(result);
        Assert.Equal("System.Threading.Tasks.Task", result.FullyQualifiedName);
    }

    [Fact]
    public void AsClass_NullReturnsNull()
    {
        Assert.Null(TypeUtils.AsClass(null));
    }

    // =============================================================
    // Cycle protection
    // =============================================================

    [Fact]
    public void IsAssignableTo_HandlesCyclicTypeReference()
    {
        // Create a cycle: A -> B -> A (via mutable setters)
        var a = MakeClass("A");
        var b = MakeClass("B", supertype: a);
        a.Supertype = b;

        // Should not stack overflow — cycle protection kicks in
        Assert.False(TypeUtils.IsAssignableTo(a, "System.Object"));
    }
}
