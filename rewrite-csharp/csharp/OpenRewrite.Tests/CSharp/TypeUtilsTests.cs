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
    public void IsAssignableTo_PrimitiveString()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String);
        Assert.True(TypeUtils.IsAssignableTo(prim, "System.String"));
    }

    [Fact]
    public void IsAssignableTo_PrimitiveString_NotAssignableToOther()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String);
        Assert.False(TypeUtils.IsAssignableTo(prim, "System.Int32"));
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
    public void GetFullyQualifiedName_PrimitiveString()
    {
        Assert.Equal("System.String",
            TypeUtils.GetFullyQualifiedName(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String)));
    }

    [Fact]
    public void GetFullyQualifiedName_PrimitiveInt()
    {
        Assert.Equal("int",
            TypeUtils.GetFullyQualifiedName(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)));
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
    // IsAssignableTo — primitives beyond String
    // =============================================================

    [Fact]
    public void IsAssignableTo_PrimitiveInt()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        Assert.True(TypeUtils.IsAssignableTo(prim, "System.Int32"));
    }

    [Fact]
    public void IsAssignableTo_PrimitiveBool()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Boolean);
        Assert.True(TypeUtils.IsAssignableTo(prim, "System.Boolean"));
    }

    [Fact]
    public void IsAssignableTo_PrimitiveDouble()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Double);
        Assert.True(TypeUtils.IsAssignableTo(prim, "System.Double"));
    }

    [Fact]
    public void IsAssignableTo_PrimitiveInt_NotAssignableToOther()
    {
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        Assert.False(TypeUtils.IsAssignableTo(prim, "System.String"));
    }

    // =============================================================
    // IsAssignableTo — JavaType target overload
    // =============================================================

    [Fact]
    public void IsAssignableTo_JavaType_SamePrimitive()
    {
        var from = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        var to = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        Assert.True(TypeUtils.IsAssignableTo(from, to));
    }

    [Fact]
    public void IsAssignableTo_JavaType_DifferentPrimitive()
    {
        var from = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        var to = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String);
        Assert.False(TypeUtils.IsAssignableTo(from, to));
    }

    [Fact]
    public void IsAssignableTo_JavaType_ClassTarget()
    {
        var iface = MakeClass("System.IDisposable");
        var candidate = MakeClass("MyClass", interfaces: [iface]);
        var target = MakeClass("System.IDisposable");
        Assert.True(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_JavaType_ParameterizedTarget_RawFqnMatch()
    {
        // Target is Parameterized(IDictionary) with no type params — should match by base FQN
        var idict = MakeClass("System.Collections.Generic.IDictionary");
        var dict = MakeClass("System.Collections.Generic.Dictionary", interfaces: [idict]);
        var target = new JavaType.Parameterized
        {
            Type = MakeClass("System.Collections.Generic.IDictionary")
        };
        Assert.True(TypeUtils.IsAssignableTo(dict, target));
    }

    [Fact]
    public void IsAssignableTo_JavaType_NullTarget()
    {
        var cls = MakeClass("System.String");
        Assert.False(TypeUtils.IsAssignableTo(cls, (JavaType?)null));
    }

    // =============================================================
    // IsAssignableTo — GenericTypeVariable (open generic matching)
    // =============================================================

    [Fact]
    public void IsAssignableTo_GenericTypeVariable_UnboundedMatchesAny()
    {
        // Target: IDictionary<TKey, TValue> (both unbounded GenericTypeVariables)
        var idictClass = MakeClass("System.Collections.Generic.IDictionary");
        var target = new JavaType.Parameterized(
            idictClass,
            [
                new JavaType.GenericTypeVariable("TKey", JavaType.GenericTypeVariable.VarianceKind.Invariant, null),
                new JavaType.GenericTypeVariable("TValue", JavaType.GenericTypeVariable.VarianceKind.Invariant, null)
            ]);

        // Candidate: Dictionary<string, int> implementing IDictionary<string, int>
        var idictStringInt = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IDictionary"),
            [MakeClass("System.String"), MakeClass("System.Int32")]);
        var dictClass = MakeClass("System.Collections.Generic.Dictionary",
            interfaces: [idictStringInt]);
        var candidate = new JavaType.Parameterized(
            dictClass,
            [MakeClass("System.String"), MakeClass("System.Int32")]);

        Assert.True(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_PartialGeneric_FixedFirstParam()
    {
        // Target: IDictionary<string, TValue> — first param fixed, second open
        var idictClass = MakeClass("System.Collections.Generic.IDictionary");
        var target = new JavaType.Parameterized(
            idictClass,
            [
                MakeClass("System.String"),
                new JavaType.GenericTypeVariable("TValue", JavaType.GenericTypeVariable.VarianceKind.Invariant, null)
            ]);

        // Candidate: Dictionary<string, int> implementing IDictionary<string, int> — should match
        var idictStringInt = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IDictionary"),
            [MakeClass("System.String"), MakeClass("System.Int32")]);
        var dictClass = MakeClass("System.Collections.Generic.Dictionary",
            interfaces: [idictStringInt]);
        var candidate = new JavaType.Parameterized(
            dictClass,
            [MakeClass("System.String"), MakeClass("System.Int32")]);

        Assert.True(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_PartialGeneric_FixedParamMismatch()
    {
        // Target: IDictionary<string, TValue> — first param must be string
        var idictClass = MakeClass("System.Collections.Generic.IDictionary");
        var target = new JavaType.Parameterized(
            idictClass,
            [
                MakeClass("System.String"),
                new JavaType.GenericTypeVariable("TValue", JavaType.GenericTypeVariable.VarianceKind.Invariant, null)
            ]);

        // Candidate: Dictionary<int, string> implementing IDictionary<int, string> — should NOT match
        var idictIntString = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IDictionary"),
            [MakeClass("System.Int32"), MakeClass("System.String")]);
        var dictClass = MakeClass("System.Collections.Generic.Dictionary",
            interfaces: [idictIntString]);
        var candidate = new JavaType.Parameterized(
            dictClass,
            [MakeClass("System.Int32"), MakeClass("System.String")]);

        Assert.False(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_GenericTypeVariable_WithBound()
    {
        // Target: IEnumerable<T> where T : IComparable
        var ienumClass = MakeClass("System.Collections.Generic.IEnumerable");
        var icomparable = MakeClass("System.IComparable");
        var target = new JavaType.Parameterized(
            ienumClass,
            [new JavaType.GenericTypeVariable("T", JavaType.GenericTypeVariable.VarianceKind.Invariant,
                [icomparable])]);

        // Candidate: List<string> implementing IEnumerable<string>
        // string implements IComparable → should match
        var stringClass = MakeClass("System.String", interfaces: [MakeClass("System.IComparable")]);
        var ienumString = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IEnumerable"),
            [stringClass]);
        var listClass = MakeClass("System.Collections.Generic.List",
            interfaces: [ienumString]);
        var candidate = new JavaType.Parameterized(listClass, [stringClass]);

        Assert.True(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_GenericTypeVariable_BoundNotSatisfied()
    {
        // Target: IEnumerable<T> where T : IComparable
        var ienumClass = MakeClass("System.Collections.Generic.IEnumerable");
        var icomparable = MakeClass("System.IComparable");
        var target = new JavaType.Parameterized(
            ienumClass,
            [new JavaType.GenericTypeVariable("T", JavaType.GenericTypeVariable.VarianceKind.Invariant,
                [icomparable])]);

        // Candidate: List<object> implementing IEnumerable<object>
        // object does NOT implement IComparable → should NOT match
        var objectClass = MakeClass("System.Object");
        var ienumObject = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IEnumerable"),
            [objectClass]);
        var listClass = MakeClass("System.Collections.Generic.List",
            interfaces: [ienumObject]);
        var candidate = new JavaType.Parameterized(listClass, [objectClass]);

        Assert.False(TypeUtils.IsAssignableTo(candidate, target));
    }

    [Fact]
    public void IsAssignableTo_ConcreteGeneric_StillRequiresExactTypeArgs()
    {
        // Target: IDictionary<string, string> — NO GenericTypeVariables, all concrete
        var idictClass = MakeClass("System.Collections.Generic.IDictionary");
        var target = new JavaType.Parameterized(
            idictClass,
            [MakeClass("System.String"), MakeClass("System.String")]);

        // Candidate: Dictionary<string, int> implementing IDictionary<string, int>
        // Type args don't match → should NOT match
        var idictStringInt = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IDictionary"),
            [MakeClass("System.String"), MakeClass("System.Int32")]);
        var dictClass = MakeClass("System.Collections.Generic.Dictionary",
            interfaces: [idictStringInt]);
        var candidate = new JavaType.Parameterized(
            dictClass,
            [MakeClass("System.String"), MakeClass("System.Int32")]);

        Assert.False(TypeUtils.IsAssignableTo(candidate, target));
    }

    // =============================================================
    // IsAssignableTo — Array types
    // =============================================================

    [Fact]
    public void IsAssignableTo_Array_SystemArray()
    {
        var arr = new JavaType.Array(MakeClass("System.String"), null);
        Assert.True(TypeUtils.IsAssignableTo(arr, "System.Array"));
    }

    [Fact]
    public void IsAssignableTo_Array_SystemObject()
    {
        var arr = new JavaType.Array(MakeClass("System.String"), null);
        Assert.True(TypeUtils.IsAssignableTo(arr, "System.Object"));
    }

    [Fact]
    public void IsAssignableTo_Array_NonGenericIEnumerable()
    {
        var arr = new JavaType.Array(MakeClass("System.String"), null);
        Assert.True(TypeUtils.IsAssignableTo(arr, "System.Collections.IEnumerable"));
    }

    [Fact]
    public void IsAssignableTo_Array_ICloneable()
    {
        var arr = new JavaType.Array(MakeClass("System.String"), null);
        Assert.True(TypeUtils.IsAssignableTo(arr, "System.ICloneable"));
    }

    [Fact]
    public void IsAssignableTo_Array_NotAssignableToUnrelated()
    {
        var arr = new JavaType.Array(MakeClass("System.String"), null);
        Assert.False(TypeUtils.IsAssignableTo(arr, "System.IDisposable"));
    }

    [Fact]
    public void IsAssignableTo_Array_GenericIEnumerable()
    {
        // string[] should be assignable to IEnumerable<string>
        var stringClass = MakeClass("System.String");
        var arr = new JavaType.Array(stringClass, null);
        var target = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IEnumerable"),
            [MakeClass("System.String")]);
        Assert.True(TypeUtils.IsAssignableTo(arr, target));
    }

    [Fact]
    public void IsAssignableTo_Array_GenericIList()
    {
        // string[] should be assignable to IList<string>
        var stringClass = MakeClass("System.String");
        var arr = new JavaType.Array(stringClass, null);
        var target = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IList"),
            [MakeClass("System.String")]);
        Assert.True(TypeUtils.IsAssignableTo(arr, target));
    }

    [Fact]
    public void IsAssignableTo_Array_GenericIReadOnlyList()
    {
        // string[] should be assignable to IReadOnlyList<string>
        var stringClass = MakeClass("System.String");
        var arr = new JavaType.Array(stringClass, null);
        var target = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IReadOnlyList"),
            [MakeClass("System.String")]);
        Assert.True(TypeUtils.IsAssignableTo(arr, target));
    }

    [Fact]
    public void IsAssignableTo_Array_GenericIEnumerable_OpenTypeVariable()
    {
        // string[] should be assignable to IEnumerable<T> (open generic)
        var stringClass = MakeClass("System.String");
        var arr = new JavaType.Array(stringClass, null);
        var target = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IEnumerable"),
            [new JavaType.GenericTypeVariable("T", JavaType.GenericTypeVariable.VarianceKind.Invariant, null)]);
        Assert.True(TypeUtils.IsAssignableTo(arr, target));
    }

    [Fact]
    public void IsAssignableTo_Array_GenericMismatch()
    {
        // string[] should NOT be assignable to IEnumerable<int>
        var stringClass = MakeClass("System.String");
        var arr = new JavaType.Array(stringClass, null);
        var target = new JavaType.Parameterized(
            MakeClass("System.Collections.Generic.IEnumerable"),
            [MakeClass("System.Int32")]);
        Assert.False(TypeUtils.IsAssignableTo(arr, target));
    }

    [Fact]
    public void IsAssignableTo_Array_Self()
    {
        // given
        var arr = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(arr, arr);

        // then
        Assert.True(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_EquivalentDistinctArrays()
    {
        // given
        var from = new JavaType.Array(MakeClass("System.String"), null);
        var target = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.True(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_PrimitiveElementMatchesItsClassElement()
    {
        // given
        var from = new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null);
        var target = new JavaType.Array(MakeClass("System.Int32"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.True(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_DifferentElementTypes()
    {
        // given
        var from = new JavaType.Array(MakeClass("System.String"), null);
        var target = new JavaType.Array(MakeClass("System.Uri"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.False(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_ReferenceElementCovariance()
    {
        // given
        var animal = MakeClass("Zoo.Animal");
        var from = new JavaType.Array(MakeClass("Zoo.Dog", supertype: animal), null);
        var toBase = new JavaType.Array(MakeClass("Zoo.Animal"), null);
        var toObject = new JavaType.Array(MakeClass("System.Object"), null);

        // when
        var assignableToBase = TypeUtils.IsAssignableTo(from, toBase);
        var assignableToObject = TypeUtils.IsAssignableTo(from, toObject);

        // then
        Assert.True(assignableToBase);
        Assert.True(assignableToObject);
    }

    [Fact]
    public void IsAssignableTo_Array_StringElementCovariantToObjectArray()
    {
        // given
        var from = new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.String), null);
        var target = new JavaType.Array(MakeClass("System.Object"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.True(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_NoContravariance()
    {
        // given
        var animal = MakeClass("Zoo.Animal");
        var from = new JavaType.Array(animal, null);
        var target = new JavaType.Array(MakeClass("Zoo.Dog", supertype: animal), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.False(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_ValueElementNotCovariant()
    {
        // given
        var intArray = new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null);
        var valueType = MakeClass("System.ValueType", supertype: MakeClass("System.Object"));
        var point = MakeClass("Geo.Point", supertype: valueType, interfaces: [MakeClass("System.IEquatable")]);
        point.ClassKind = JavaType.FullyQualified.FullyQualifiedKind.Value;
        var pointArray = new JavaType.Array(point, null);

        // when
        var intToObjects = TypeUtils.IsAssignableTo(intArray, new JavaType.Array(MakeClass("System.Object"), null));
        var pointToValueTypes = TypeUtils.IsAssignableTo(pointArray, new JavaType.Array(MakeClass("System.ValueType"), null));
        var pointToInterfaces = TypeUtils.IsAssignableTo(pointArray, new JavaType.Array(MakeClass("System.IEquatable"), null));

        // then
        Assert.False(intToObjects);
        Assert.False(pointToValueTypes);
        Assert.False(pointToInterfaces);
    }

    [Fact]
    public void IsAssignableTo_Array_EnumElementInvariantButSystemEnumElementCovariant()
    {
        // given
        var systemEnum = MakeClass("System.Enum", supertype: MakeClass("System.ValueType"));
        var color = MakeClass("Paint.Color", supertype: systemEnum);
        color.ClassKind = JavaType.FullyQualified.FullyQualifiedKind.Enum;
        var objectArray = new JavaType.Array(MakeClass("System.Object"), null);

        // when
        var colorsToObjects = TypeUtils.IsAssignableTo(new JavaType.Array(color, null), objectArray);
        var enumsToObjects = TypeUtils.IsAssignableTo(new JavaType.Array(systemEnum, null), objectArray);

        // then
        Assert.False(colorsToObjects);
        Assert.True(enumsToObjects);
    }

    [Fact]
    public void IsAssignableTo_Array_ValueElementNotAssignableToNullableElement()
    {
        // given
        var from = new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null);
        var target = new JavaType.Array(new JavaType.Parameterized(MakeClass("System.Nullable"),
            [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.False(assignable);
    }

    [Fact]
    public void IsAssignableTo_Array_Jagged()
    {
        // given
        var intArrayArray = new JavaType.Array(
            new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null), null);
        var sameShape = new JavaType.Array(
            new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null), null);
        var longArrayArray = new JavaType.Array(
            new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Long), null), null);
        var objectArray = new JavaType.Array(MakeClass("System.Object"), null);
        var arrayOfArrays = new JavaType.Array(MakeClass("System.Array"), null);
        var flatIntArray = new JavaType.Array(JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int), null);

        // when
        var toSameShape = TypeUtils.IsAssignableTo(intArrayArray, sameShape);
        var toLongShape = TypeUtils.IsAssignableTo(intArrayArray, longArrayArray);
        var toObjects = TypeUtils.IsAssignableTo(intArrayArray, objectArray);
        var toArrays = TypeUtils.IsAssignableTo(intArrayArray, arrayOfArrays);
        var toFlat = TypeUtils.IsAssignableTo(intArrayArray, flatIntArray);
        var flatToJagged = TypeUtils.IsAssignableTo(flatIntArray, intArrayArray);

        // then
        Assert.True(toSameShape);
        Assert.False(toLongShape);
        Assert.True(toObjects);
        Assert.True(toArrays);
        Assert.False(toFlat);
        Assert.False(flatToJagged);
    }

    [Fact]
    public void IsAssignableTo_Array_JaggedReferenceCovariance()
    {
        // given
        var from = new JavaType.Array(new JavaType.Array(MakeClass("System.String"), null), null);
        var target = new JavaType.Array(new JavaType.Array(MakeClass("System.Object"), null), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.True(assignable);
    }

    [Fact]
    public void IsAssignableTo_NonArrayNotAssignableToArray()
    {
        // given
        var from = MakeClass("System.String");
        var target = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(from, target);

        // then
        Assert.False(assignable);
    }

    [Fact]
    public void IsAssignableTo_ArrayNotAssignableToNull()
    {
        // given
        var arr = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var assignable = TypeUtils.IsAssignableTo(arr, (JavaType?)null);

        // then
        Assert.False(assignable);
    }

    [Theory]
    [InlineData("System.Array")]
    [InlineData("System.Object")]
    [InlineData("System.ICloneable")]
    [InlineData("System.Collections.IList")]
    [InlineData("System.Collections.ICollection")]
    [InlineData("System.Collections.IEnumerable")]
    [InlineData("System.Collections.IStructuralComparable")]
    [InlineData("System.Collections.IStructuralEquatable")]
    public void IsAssignableTo_Array_NonGenericSupertypes_AgreeAcrossOverloads(string fqn)
    {
        // given
        var arr = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var byName = TypeUtils.IsAssignableTo(arr, fqn);
        var byNames = TypeUtils.IsAssignableTo(arr, new[] { fqn });
        var byType = TypeUtils.IsAssignableTo(arr, MakeClass(fqn));

        // then
        Assert.True(byName);
        Assert.True(byNames);
        Assert.True(byType);
    }

    [Theory]
    [InlineData("System.IDisposable")]
    [InlineData("System.String")]
    [InlineData("System.Collections.Generic.IEnumerable")]
    public void IsAssignableTo_Array_UnrelatedNames_AgreeAcrossOverloads(string fqn)
    {
        // given
        var arr = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var byName = TypeUtils.IsAssignableTo(arr, fqn);
        var byNames = TypeUtils.IsAssignableTo(arr, new[] { fqn });

        // then
        Assert.False(byName);
        Assert.False(byNames);
    }

    [Fact]
    public void IsAssignableTo_Array_MultipleCandidateNames()
    {
        // given
        var arr = new JavaType.Array(MakeClass("System.String"), null);

        // when
        var anyMatches = TypeUtils.IsAssignableTo(arr, new[] { "System.IDisposable", "System.Collections.IList" });
        var noneMatch = TypeUtils.IsAssignableTo(arr, new[] { "System.IDisposable", "System.String" });
        var empty = TypeUtils.IsAssignableTo(arr, System.Array.Empty<string>());

        // then
        Assert.True(anyMatches);
        Assert.False(noneMatch);
        Assert.False(empty);
    }

    [Fact]
    public void IsAssignableTo_MultipleCandidateNames_ClassAndPrimitive()
    {
        // given
        var cls = MakeClass("Zoo.Dog", supertype: MakeClass("Zoo.Animal"));
        var prim = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);

        // when
        var clsMatches = TypeUtils.IsAssignableTo(cls, new[] { "System.IDisposable", "Zoo.Animal" });
        var primMatches = TypeUtils.IsAssignableTo(prim, new[] { "System.String", "System.Int32" });
        var primMisses = TypeUtils.IsAssignableTo(prim, new[] { "System.String", "System.Int64" });

        // then
        Assert.True(clsMatches);
        Assert.True(primMatches);
        Assert.False(primMisses);
    }

    // =============================================================
    // IsAssignableTo — Nullable<T> (value type assignable to Nullable<T>)
    // =============================================================

    [Fact]
    public void IsAssignableTo_PrimitiveInt_AssignableToNullableInt()
    {
        var from = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        var nullable = MakeClass("System.Nullable");
        var target = new JavaType.Parameterized(nullable,
            [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.True(TypeUtils.IsAssignableTo(from, target));
    }

    [Fact]
    public void IsAssignableTo_ClassInt_AssignableToNullableInt()
    {
        var from = MakeClass("System.Int32");
        var nullable = MakeClass("System.Nullable");
        var target = new JavaType.Parameterized(nullable,
            [MakeClass("System.Int32")]);
        Assert.True(TypeUtils.IsAssignableTo(from, target));
    }

    [Fact]
    public void IsAssignableTo_Struct_AssignableToNullableStruct()
    {
        var from = MakeClass("System.DateTime",
            supertype: MakeClass("System.ValueType"));
        var nullable = MakeClass("System.Nullable");
        var target = new JavaType.Parameterized(nullable, [MakeClass("System.DateTime")]);
        Assert.True(TypeUtils.IsAssignableTo(from, target));
    }

    [Fact]
    public void IsAssignableTo_WrongType_NotAssignableToNullable()
    {
        // int is NOT assignable to Nullable<double>
        var from = JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int);
        var nullable = MakeClass("System.Nullable");
        var target = new JavaType.Parameterized(nullable,
            [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Double)]);
        Assert.False(TypeUtils.IsAssignableTo(from, target));
    }

    [Fact]
    public void IsAssignableTo_NullableInt_AssignableToNullableInt()
    {
        // Nullable<int> is assignable to Nullable<int>
        var nullable = MakeClass("System.Nullable");
        var from = new JavaType.Parameterized(nullable,
            [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        var target = new JavaType.Parameterized(MakeClass("System.Nullable"),
            [JavaType.Primitive.Of(JavaType.Primitive.PrimitiveKind.Int)]);
        Assert.True(TypeUtils.IsAssignableTo(from, target));
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
