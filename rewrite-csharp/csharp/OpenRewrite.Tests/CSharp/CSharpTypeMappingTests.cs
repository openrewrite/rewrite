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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Testing;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;
namespace OpenRewrite.Tests.CSharp;

/// <summary>
/// Tests that the C# parser's type mapping correctly handles generic types,
/// especially how parameterized interfaces store type arguments.
/// </summary>
public class CSharpTypeMappingTests : RewriteTest
{
    /// <summary>
    /// Parse source code with reference assemblies and return the CompilationUnit.
    /// </summary>
    private static readonly SyntaxTree ImplicitUsingsSyntaxTree = CSharpSyntaxTree.ParseText(
        """
        global using System;
        global using System.Collections.Generic;
        global using System.IO;
        global using System.Linq;
        global using System.Net.Http;
        global using System.Threading;
        global using System.Threading.Tasks;
        """,
        path: "__GlobalUsings__.g.cs");

    private static CompilationUnit ParseWithSemanticModel(string code,
        ReferenceAssemblies? referenceAssemblies = null)
    {
        var refs = (referenceAssemblies ?? Assemblies.Net90)
            .ResolveAsync(LanguageNames.CSharp, CancellationToken.None)
            .GetAwaiter().GetResult();
        var syntaxTree = CSharpSyntaxTree.ParseText(code, path: "source.cs");
        var compilation = CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(ImplicitUsingsSyntaxTree, syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);

        var parser = new CSharpParser();
        return parser.Parse(code, semanticModel: semanticModel);
    }

    /// <summary>
    /// Find the first variable declaration in a compilation unit by variable name.
    /// </summary>
    private static VariableDeclarations? FindVariableDeclaration(CompilationUnit cu, string varName)
    {
        var finder = new VarFinder(varName);
        finder.Cursor = new OpenRewrite.Core.Cursor(null, OpenRewrite.Core.Cursor.ROOT_VALUE);
        finder.Visit(cu, 0);
        return finder.Found;
    }

    private class VarFinder(string name) : CSharpVisitor<int>
    {
        public VariableDeclarations? Found { get; private set; }

        public override J VisitVariableDeclarations(VariableDeclarations multiVariable, int p)
        {
            if (multiVariable.Variables.Any(v => v.Element.Name.SimpleName == name))
                Found = multiVariable;
            return base.VisitVariableDeclarations(multiVariable, p);
        }
    }

    [Fact]
    public void GenericClassField_HasParameterizedTypeWithGenericTypeVariables()
    {
        // A class with type parameters and a field whose type references them.
        // The field's type should be Parameterized with GenericTypeVariable entries.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Foo<TKey, TValue>
            {
                IDictionary<TKey, TValue> dict;
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        // The declared type should be Parameterized(IDictionary, [GTV(TKey), GTV(TValue)])
        var declType = varDecl!.TypeExpression?.Type;
        Assert.NotNull(declType);
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);

        Assert.NotNull(paramType.Type);
        Assert.Contains("IDictionary", TypeUtils.GetFullyQualifiedName(paramType.Type));

        Assert.NotNull(paramType.TypeParameters);
        Assert.Equal(2, paramType.TypeParameters!.Count);

        var tp0 = Assert.IsType<JavaType.GenericTypeVariable>(paramType.TypeParameters[0]);
        Assert.Equal("TKey", tp0.Name);

        var tp1 = Assert.IsType<JavaType.GenericTypeVariable>(paramType.TypeParameters[1]);
        Assert.Equal("TValue", tp1.Name);
    }

    [Fact]
    public void ConcreteGenericVariable_HasParameterizedTypeWithConcreteArgs()
    {
        // A local variable with a concrete generic type.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.NotNull(declType);
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);

        Assert.Contains("Dictionary", TypeUtils.GetFullyQualifiedName(paramType.Type));

        // Type args should be concrete: String, Int32
        Assert.NotNull(paramType.TypeParameters);
        Assert.Equal(2, paramType.TypeParameters!.Count);

        // string is mapped as Primitive(String) or Class(System.String)
        var tp0Fqn = TypeUtils.GetFullyQualifiedName(paramType.TypeParameters[0]);
        Assert.NotNull(tp0Fqn);
        Assert.Contains("String", tp0Fqn);

        // int is mapped as Primitive(Int) or Class(System.Int32)
        var tp1 = paramType.TypeParameters[1];
        Assert.True(
            tp1 is JavaType.Primitive { Kind: JavaType.Primitive.PrimitiveKind.Int } ||
            TypeUtils.GetFullyQualifiedName(tp1)?.Contains("Int32") == true,
            $"Expected int type but got: {tp1.GetType().Name}");
    }

    [Fact]
    public void ConcreteGenericVariable_InterfacesHaveUnsubstitutedTypeParams()
    {
        // Verify that the underlying Class for Dictionary stores interfaces
        // with unsubstituted type parameters (GenericTypeVariable), not
        // concrete types. This is the expected behavior — TypeUtils must
        // resolve these at comparison time.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var paramType = Assert.IsType<JavaType.Parameterized>(varDecl!.TypeExpression?.Type);
        var dictClass = TypeUtils.AsClass(paramType);
        Assert.NotNull(dictClass);

        // Dictionary should implement IDictionary (among other interfaces)
        Assert.NotNull(dictClass!.Interfaces);
        var idict = dictClass.Interfaces!
            .Select(TypeUtils.AsClass)
            .FirstOrDefault(c => c?.FullyQualifiedName.Contains("IDictionary") == true);
        Assert.NotNull(idict);

        // Find the Parameterized form of the IDictionary interface
        var idictParam = dictClass.Interfaces!
            .OfType<JavaType.Parameterized>()
            .FirstOrDefault(p => TypeUtils.GetFullyQualifiedName(p.Type)?.Contains("IDictionary") == true);

        // The interface should be parameterized with GenericTypeVariables
        // (TKey, TValue from the Dictionary definition), NOT concrete types
        if (idictParam != null && idictParam.TypeParameters != null)
        {
            // At least one type param should be a GenericTypeVariable
            // (confirming these are unsubstituted from the original definition)
            var hasGtv = idictParam.TypeParameters.Any(tp => tp is JavaType.GenericTypeVariable);
            Assert.True(hasGtv,
                "Expected IDictionary interface to have GenericTypeVariable type params " +
                "(unsubstituted from Dictionary's original definition), but found: " +
                string.Join(", ", idictParam.TypeParameters.Select(tp => tp.GetType().Name)));
        }
    }

    [Fact]
    public void ImplicitUsings_ShortNameResolvesWithoutExplicitUsing()
    {
        // In .NET 6+, System.Collections.Generic is an implicit using.
        // Test whether Dictionary (short name, no using directive) gets
        // type attribution. This will fail if implicit usings are not
        // configured in the compilation.
        var cu = ParseWithSemanticModel("""
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);
        var declType = varDecl!.TypeExpression?.Type;
        // If implicit usings work, this should be Parameterized.
        // If not, it will be null or Unknown (unresolved).
        Assert.NotNull(declType);
        Assert.IsType<JavaType.Parameterized>(declType);
    }

    [Fact]
    public void DictionaryClass_HasFormalTypeParameters()
    {
        // Verify that the underlying Class for Dictionary has formal type parameters
        // (needed for building substitution maps during type comparison).
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var paramType = Assert.IsType<JavaType.Parameterized>(varDecl!.TypeExpression?.Type);
        var dictClass = TypeUtils.AsClass(paramType);
        Assert.NotNull(dictClass);

        // The Class should have TypeParameters listing formal type params
        Assert.NotNull(dictClass!.TypeParameters);
        Assert.Equal(2, dictClass.TypeParameters!.Count);

        var formal0 = Assert.IsType<JavaType.GenericTypeVariable>(dictClass.TypeParameters[0]);
        Assert.Equal("TKey", formal0.Name);

        var formal1 = Assert.IsType<JavaType.GenericTypeVariable>(dictClass.TypeParameters[1]);
        Assert.Equal("TValue", formal1.Name);
    }

    [Fact]
    public void NullableValueType_IsMappedAsParameterizedNullable()
    {
        // int? should be mapped as Parameterized(System.Nullable, [Primitive(Int)])
        var cu = ParseWithSemanticModel("""
            class Test
            {
                void M()
                {
                    int? x = 42;
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "x");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.NotNull(declType);
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);
        Assert.Equal("System.Nullable", TypeUtils.GetFullyQualifiedName(paramType.Type));
        Assert.NotNull(paramType.TypeParameters);
        Assert.Single(paramType.TypeParameters!);
        Assert.IsType<JavaType.Primitive>(paramType.TypeParameters[0]);
    }

    [Fact]
    public void NullableReferenceType_String_IsNotWrappedInNullable()
    {
        // string? is a nullable reference type — Roslyn models it as plain System.String
        // (not Nullable<String>), since nullable reference types are annotation-only
        var cu = ParseWithSemanticModel("""
            #nullable enable
            class Test
            {
                void M()
                {
                    string? s = null;
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "s");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.IsType<JavaType.Class>(declType);
        Assert.Equal("System.String", TypeUtils.GetFullyQualifiedName(declType));
    }

    [Fact]
    public void NullableReferenceType_Object_IsNotWrappedInNullable()
    {
        // object? is a nullable reference type — plain System.Object, not Nullable<Object>
        var cu = ParseWithSemanticModel("""
            #nullable enable
            class Test
            {
                void M()
                {
                    object? o = null;
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "o");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.IsType<JavaType.Class>(declType);
        Assert.Equal("System.Object", TypeUtils.GetFullyQualifiedName(declType));
    }

    [Fact]
    public void NullableUserDefinedClass_IsNotWrappedInNullable()
    {
        // Foo? where Foo is a user-defined reference type — plain Class, not Nullable
        var cu = ParseWithSemanticModel("""
            #nullable enable
            class Foo { }
            class Test
            {
                void M()
                {
                    Foo? f = null;
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "f");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.IsType<JavaType.Class>(declType);
        Assert.Equal("Foo", TypeUtils.GetFullyQualifiedName(declType));
    }

    [Fact]
    public void NullableUserDefinedStruct_IsMappedAsParameterizedNullable()
    {
        // Bar? where Bar is a user-defined struct — Parameterized(Nullable, [Bar])
        var cu = ParseWithSemanticModel("""
            struct Bar { public int X; }
            class Test
            {
                void M()
                {
                    Bar? b = null;
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "b");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);
        Assert.Equal("System.Nullable", TypeUtils.GetFullyQualifiedName(paramType.Type));
        Assert.NotNull(paramType.TypeParameters);
        var innerType = Assert.IsType<JavaType.Class>(Assert.Single(paramType.TypeParameters!));
        Assert.Equal("Bar", innerType.FullyQualifiedName);
    }

    [Fact]
    public void StringTypeArgument_IsMappedAsClass()
    {
        // System.String should be mapped as JavaType.Class (not Primitive) when used
        // as a type argument, so TypeUtils.IsAssignableTo can walk its interface chain.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    List<string> items = new List<string>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "items");
        Assert.NotNull(varDecl);

        var paramType = Assert.IsType<JavaType.Parameterized>(varDecl!.TypeExpression?.Type);
        Assert.NotNull(paramType.TypeParameters);
        var stringArg = Assert.IsType<JavaType.Class>(Assert.Single(paramType.TypeParameters!));
        Assert.Equal("System.String", stringArg.FullyQualifiedName);
    }

    [Fact]
    public async Task SharedTypeCache_DedupesTypeInstancesAcrossDocumentsInProject()
    {
        // Two documents in ONE compilation both reference the same user type `Shared`.
        // Roslyn interns symbols per Compilation, so the `Shared` ISymbol is identical
        // across both documents' semantic models. A per-project shared type cache must
        // therefore yield the SAME JavaType instance for `Shared` in both documents, so
        // RPC asRef() can serialize it once instead of re-serializing it per file.
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        const string sharedSrc = "public class Shared { }";
        const string srcA = "class A { Shared f; }";
        const string srcB = "class B { Shared g; }";
        var treeShared = CSharpSyntaxTree.ParseText(sharedSrc, path: "shared.cs");
        var treeA = CSharpSyntaxTree.ParseText(srcA, path: "a.cs");
        var treeB = CSharpSyntaxTree.ParseText(srcB, path: "b.cs");
        var compilation = CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(ImplicitUsingsSyntaxTree, treeShared, treeA, treeB);
        var smA = compilation.GetSemanticModel(treeA);
        var smB = compilation.GetSemanticModel(treeB);

        var parser = new CSharpParser();

        // Baseline: parsed independently (no shared cache), each document builds its own
        // JavaType instance for `Shared` — this is the per-document duplication.
        var indepA = FindVariableDeclaration(parser.Parse(srcA, "a.cs", smA), "f")!.TypeExpression?.Type;
        var indepB = FindVariableDeclaration(parser.Parse(srcB, "b.cs", smB), "g")!.TypeExpression?.Type;
        Assert.NotNull(indepA);
        Assert.NotNull(indepB);
        Assert.NotSame(indepA, indepB);

        // With a shared per-project cache, both documents resolve `Shared` to the SAME instance.
        var sharedCache = new Dictionary<ISymbol, JavaType>(SymbolEqualityComparer.Default);
        var typeA = FindVariableDeclaration(parser.Parse(srcA, "a.cs", smA, typeCache: sharedCache), "f")!.TypeExpression?.Type;
        var typeB = FindVariableDeclaration(parser.Parse(srcB, "b.cs", smB, typeCache: sharedCache), "g")!.TypeExpression?.Type;
        Assert.NotNull(typeA);
        Assert.NotNull(typeB);
        Assert.Same(typeA, typeB);
    }

    [Fact]
    public void SourceClass_DeclaredMembersAndMethodsArePopulated()
    {
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Holder
            {
                private int _count;
                public string Name { get; set; } = "";
                public event System.EventHandler? Changed;
                public List<string> Items() => new();
                public void Reset(int to) { _count = to; }
            }
            class Test
            {
                void M() { Holder holder = new Holder(); }
            }
            """);

        var holderType = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "holder")!.TypeExpression?.Type);

        Assert.NotNull(holderType.Members);
        Assert.NotNull(holderType.Methods);

        var count = Assert.Single(holderType.Members!, v => v.Name == "_count");
        Assert.Equal(JavaType.Primitive.Of(JavaType.PrimitiveKind.Int), count.Type);
        Assert.Same(holderType, count.Owner);

        var name = Assert.Single(holderType.Members!, v => v.Name == "Name");
        Assert.Equal("System.String", Assert.IsType<JavaType.Class>(name.Type).FullyQualifiedName);
        Assert.Contains(holderType.Members!, v => v.Name == "Changed");

        Assert.DoesNotContain(holderType.Members!, v => v.Name.StartsWith('<'));
        Assert.DoesNotContain(holderType.Methods!, m => m.Name is "get_Name" or "set_Name"
            or "add_Changed" or "remove_Changed");

        var items = Assert.Single(holderType.Methods!, m => m.Name == "Items");
        Assert.Equal("System.Collections.Generic.List",
            Assert.IsType<JavaType.Class>(Assert.IsType<JavaType.Parameterized>(items.ReturnType).Type)
                .FullyQualifiedName);

        var reset = Assert.Single(holderType.Methods!, m => m.Name == "Reset");
        Assert.Equal(["to"], reset.ParameterNames);
        Assert.Same(holderType, reset.DeclaringType);

        Assert.Contains(holderType.Methods!, m => m.Name == ".ctor");

        Assert.DoesNotContain(holderType.Methods!, m => m.Name == "ToString");
        var objectType = Assert.IsType<JavaType.Class>(holderType.Supertype);
        Assert.Equal("System.Object", objectType.FullyQualifiedName);
        Assert.Contains(objectType.Methods!, m => m.Name == "ToString");
    }

    [Fact]
    public void MetadataClass_DeclaredMembersAndMethodsArePopulated()
    {
        var cu = ParseWithSemanticModel("""
            using System.Windows.Controls;
            class Test
            {
                void M() { ItemsControl control = new ItemsControl(); }
            }
            """, Assemblies.Net90.AddPackage("Microsoft.WindowsDesktop.App.Ref", "9.0.16"));

        var itemsControl = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "control")!.TypeExpression?.Type);
        Assert.Equal("System.Windows.Controls.ItemsControl", itemsControl.FullyQualifiedName);

        Assert.NotNull(itemsControl.Members);
        Assert.NotNull(itemsControl.Methods);

        var itemsSource = Assert.Single(itemsControl.Members!, v => v.Name == "ItemsSource");
        Assert.Equal("System.Collections.IEnumerable",
            Assert.IsType<JavaType.Class>(itemsSource.Type).FullyQualifiedName);

        var itemsSourceProperty = Assert.Single(itemsControl.Members!,
            v => v.Name == "ItemsSourceProperty");
        Assert.Equal("System.Windows.DependencyProperty",
            Assert.IsType<JavaType.Class>(itemsSourceProperty.Type).FullyQualifiedName);

        Assert.Contains(itemsControl.Methods!, m => m.Name == "GetItemsOwner");

        Assert.DoesNotContain(itemsControl.Members!, v => v.Name == "Visibility");
        var uiElement = Walk(itemsControl, "System.Windows.UIElement");
        Assert.Contains(uiElement.Members!, v => v.Name == "Visibility");
    }

    /// <summary>
    /// Attributes written on a type in the analysed source are mapped onto
    /// <see cref="JavaType.Class.Annotations"/>, with their named arguments resolved to the
    /// attribute class' properties and their values carried as constants or type references.
    /// </summary>
    [Fact]
    public void SourceClass_AnnotationsAreMapped()
    {
        var cu = ParseWithSemanticModel("""
            using System;

            [AttributeUsage(AttributeTargets.Class, AllowMultiple = true)]
            sealed class MarkerAttribute : Attribute
            {
                public string? Name { get; set; }
                public Type? Kind { get; set; }
            }

            [Marker(Name = "first", Kind = typeof(string))]
            class Marked { }

            class Test
            {
                void M() { Marked marked = new Marked(); }
            }
            """);

        var marked = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "marked")!.TypeExpression?.Type);

        Assert.NotNull(marked.Annotations);
        var marker = Assert.Single(marked.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "MarkerAttribute" });

        Assert.NotNull(marker.Values);
        Assert.Equal(2, marker.Values!.Count);

        var name = Assert.Single(marker.Values!.OfType<JavaType.Annotation.SingleElementValue>(),
            v => v.Element is JavaType.Variable { Name: "Name" });
        Assert.Equal("first", name.ConstantValue);
        Assert.Null(name.ReferenceValue);
        Assert.Equal("MarkerAttribute",
            Assert.IsType<JavaType.Class>(Assert.IsType<JavaType.Variable>(name.Element).Owner)
                .FullyQualifiedName);

        var kind = Assert.Single(marker.Values!.OfType<JavaType.Annotation.SingleElementValue>(),
            v => v.Element is JavaType.Variable { Name: "Kind" });
        Assert.Null(kind.ConstantValue);
        Assert.Equal("System.String",
            Assert.IsType<JavaType.Class>(kind.ReferenceValue).FullyQualifiedName);
    }

    /// <summary>
    /// A positional (constructor) attribute argument has no element name of its own. It is
    /// resolved to the property of the attribute class that the constructor parameter feeds,
    /// matched case-insensitively on name — the universal C# convention.
    /// </summary>
    [Fact]
    public void PositionalAttributeArguments_ResolveToTheMatchingProperty()
    {
        var cu = ParseWithSemanticModel("""
            using System;

            [Obsolete("use something else", true)]
            class Old { }

            class Test
            {
                void M() { Old old = new Old(); }
            }
            """);

        var old = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "old")!.TypeExpression?.Type);

        var obsolete = Assert.Single(old.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "System.ObsoleteAttribute" });

        var values = obsolete.Values!.Cast<JavaType.Annotation.SingleElementValue>().ToList();
        Assert.Equal(2, values.Count);

        Assert.Equal("Message", Assert.IsType<JavaType.Variable>(values[0].Element).Name);
        Assert.Equal("use something else", values[0].ConstantValue);

        var ctor = Assert.IsType<JavaType.Method>(values[1].Element);
        Assert.Equal(".ctor", ctor.Name);
        Assert.Equal("System.ObsoleteAttribute",
            Assert.IsAssignableFrom<JavaType.Class>(ctor.DeclaringType).FullyQualifiedName);
        Assert.Equal(true, values[1].ConstantValue);
    }

    /// <summary>
    /// Enum-valued arguments map to the enum member as a <see cref="JavaType.Variable"/>
    /// reference (matching Java's <c>Attribute.Enum</c> -> <c>VarSymbol</c> mapping), and array
    /// arguments map to an <see cref="JavaType.Annotation.ArrayElementValue"/>.
    /// </summary>
    [Fact]
    public void EnumAndArrayAttributeArguments_AreMapped()
    {
        var cu = ParseWithSemanticModel("""
            using System;

            enum Level { Low, High }

            sealed class ShapeAttribute : Attribute
            {
                public Level Level { get; set; }
                public string[] Names { get; set; } = [];
            }

            [Shape(Level = Level.High, Names = new[] { "a", "b" })]
            class Shaped { }

            class Test
            {
                void M() { Shaped shaped = new Shaped(); }
            }
            """);

        var shaped = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "shaped")!.TypeExpression?.Type);
        var shape = Assert.Single(shaped.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "ShapeAttribute" });

        var level = Assert.Single(shape.Values!.OfType<JavaType.Annotation.SingleElementValue>(),
            v => v.Element is JavaType.Variable { Name: "Level" });
        var member = Assert.IsType<JavaType.Variable>(level.ReferenceValue);
        Assert.Equal("High", member.Name);
        Assert.Equal("Level", Assert.IsType<JavaType.Class>(member.Owner).FullyQualifiedName);

        var names = Assert.Single(shape.Values!.OfType<JavaType.Annotation.ArrayElementValue>(),
            v => v.Element is JavaType.Variable { Name: "Names" });
        Assert.Equal(["a", "b"], names.ConstantValues);
    }

    /// <summary>
    /// Attributes on methods and on fields/properties are mapped too.
    /// </summary>
    [Fact]
    public void MethodAndVariableAnnotationsAreMapped()
    {
        var cu = ParseWithSemanticModel("""
            using System;

            class Holder
            {
                [Obsolete] public int Field;
                [Obsolete] public void Method() { }
            }

            class Test
            {
                void M() { Holder holder = new Holder(); }
            }
            """);

        var holder = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "holder")!.TypeExpression?.Type);

        var field = Assert.Single(holder.Members!, v => v.Name == "Field");
        Assert.Contains(field.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "System.ObsoleteAttribute" });

        var method = Assert.Single(holder.Methods!, m => m.Name == "Method");
        Assert.Contains(method.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "System.ObsoleteAttribute" });
    }

    /// <summary>
    /// The motivating case: <c>ComboBox</c> comes from a reference assembly and declares two
    /// <c>[TemplatePart]</c> attributes. Both must survive, distinguished by their values — a
    /// WPF recipe that only sees one of them silently loses <c>PART_Popup</c>.
    /// </summary>
    [Fact]
    public void MetadataClass_RepeatedAnnotationsAreMappedWithTheirValues()
    {
        var cu = ParseWithSemanticModel("""
            using System.Windows.Controls;
            class Test
            {
                void M() { ComboBox box = new ComboBox(); }
            }
            """, Assemblies.Net90.AddPackage("Microsoft.WindowsDesktop.App.Ref", "9.0.16"));

        var comboBox = Assert.IsType<JavaType.Class>(
            FindVariableDeclaration(cu, "box")!.TypeExpression?.Type);
        Assert.Equal("System.Windows.Controls.ComboBox", comboBox.FullyQualifiedName);

        Assert.NotNull(comboBox.Annotations);
        var templateParts = comboBox.Annotations!.OfType<JavaType.Annotation>()
            .Where(a => a.AnnotationType is JavaType.Class
            {
                FullyQualifiedName: "System.Windows.TemplatePartAttribute"
            })
            .ToList();
        Assert.Equal(2, templateParts.Count);

        var byName = templateParts.ToDictionary(
            a => (string)a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
                .Single(v => v.Element is JavaType.Variable { Name: "Name" }).ConstantValue!,
            a => Assert.IsType<JavaType.Class>(
                a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
                    .Single(v => v.Element is JavaType.Variable { Name: "Type" }).ReferenceValue)
                .FullyQualifiedName);

        Assert.Equal("System.Windows.Controls.TextBox", byName["PART_EditableTextBox"]);
        Assert.Equal("System.Windows.Controls.Primitives.Popup", byName["PART_Popup"]);
    }

    /// <summary>
    /// An event reference carries <see cref="JavaType.Variable"/> attribution naming the type that
    /// declares it, exactly as a field or property reference does. Without it a recipe renaming an
    /// event has nothing to key an unqualified reference on — the enclosing type is the wrong
    /// answer as soon as the reference is made from a derived type.
    /// </summary>
    [Fact]
    public void EventReferencesCarryVariableAttribution()
    {
        var cu = ParseWithSemanticModel("""
            using System;
            class Base
            {
                public event EventHandler? Changed;
            }
            class Derived : Base
            {
                void M()
                {
                    Changed += this.OnChanged;
                    this.Changed += this.OnChanged;
                }

                void OnChanged(object? sender, EventArgs e) { }
            }
            """);

        var occurrences = new IdentifierFinder("Changed").Collect(cu);
        Assert.Equal(3, occurrences.Count);

        foreach (var occurrence in occurrences)
        {
            var attribution = Assert.IsType<JavaType.Variable>(occurrence.FieldType);
            Assert.Equal("Changed", attribution.Name);
            Assert.Equal("Base", Assert.IsType<JavaType.Class>(attribution.Owner).FullyQualifiedName);
            Assert.Equal("System.EventHandler",
                Assert.IsType<JavaType.Class>(attribution.Type).FullyQualifiedName);
        }
    }

    private class IdentifierFinder(string name) : CSharpVisitor<int>
    {
        private readonly List<Identifier> _found = [];

        public List<Identifier> Collect(CompilationUnit cu)
        {
            Cursor = new OpenRewrite.Core.Cursor(null, OpenRewrite.Core.Cursor.ROOT_VALUE);
            Visit(cu, 0);
            return _found;
        }

        public override J VisitIdentifier(Identifier identifier, int p)
        {
            if (identifier.SimpleName == name)
            {
                _found.Add(identifier);
            }

            return identifier;
        }
    }

    private static JavaType.Class Walk(JavaType.Class from, string fullyQualifiedName)
    {
        for (JavaType.Class? c = from; c != null; c = c.Supertype as JavaType.Class)
        {
            if (c.FullyQualifiedName == fullyQualifiedName) return c;
        }
        throw new Xunit.Sdk.XunitException(
            $"{fullyQualifiedName} not found in the supertype chain of {from.FullyQualifiedName}");
    }
}
