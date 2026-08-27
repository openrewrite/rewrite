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
using System.Text.Json;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;
using OpenRewrite.Test;
using Rewrite.Core.Rpc;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A type may carry several annotations of the <em>same</em> annotation type, told apart only by
/// their element values — <c>System.Windows.Controls.ComboBox</c> declares two
/// <c>[TemplatePart]</c>s. The identity function <see cref="JavaSender"/> hands to
/// <c>GetAndSendListAsRef</c> must therefore fold the values in, as Java's
/// <c>DefaultJavaTypeSignatureBuilder.annotationSignature</c> does; keying on the annotation type
/// alone makes the two indistinguishable to the before/after diff.
/// </summary>
public class JavaTypeAnnotationRpcTest
{
    private static JavaType.Annotation TemplatePart(string partName, string partType)
    {
        var attributeType = JavaType.ShallowClass.Build("System.Windows.TemplatePartAttribute");
        return new JavaType.Annotation(attributeType,
        [
            new JavaType.Annotation.SingleElementValue(
                new JavaType.Variable("Name", attributeType,
                    JavaType.ShallowClass.Build("System.String"), null),
                partName, null),
            new JavaType.Annotation.SingleElementValue(
                new JavaType.Variable("Type", attributeType,
                    JavaType.ShallowClass.Build("System.Type"), null),
                null, JavaType.ShallowClass.Build(partType))
        ]);
    }

    private static JavaType.Class ComboBox() =>
        new JavaType.Class().UnsafeSet(1, JavaType.FullyQualified.FullyQualifiedKind.Class,
            "System.Windows.Controls.ComboBox", null, null, null,
            [
                TemplatePart("PART_EditableTextBox", "System.Windows.Controls.TextBox"),
                TemplatePart("PART_Popup", "System.Windows.Controls.Primitives.Popup")
            ],
            null, null, null);

    /// <summary>
    /// Both annotations must reach the other side, each with its own values.
    /// </summary>
    [Fact]
    public void RepeatedAnnotationsOfTheSameTypeRoundTrip()
    {
        var received = RoundTrip(ComboBox());

        Assert.NotNull(received.Annotations);
        var parts = received.Annotations!.OfType<JavaType.Annotation>().ToList();
        Assert.Equal(2, parts.Count);

        var names = parts
            .Select(a => a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
                .Single(v => v.Element is JavaType.Variable { Name: "Name" }).ConstantValue)
            .ToList();
        Assert.Equal(["PART_EditableTextBox", "PART_Popup"], names);

        var types = parts
            .Select(a => Assert.IsAssignableFrom<JavaType.Class>(
                    a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
                        .Single(v => v.Element is JavaType.Variable { Name: "Type" }).ReferenceValue)
                .FullyQualifiedName)
            .ToList();
        Assert.Equal(
            ["System.Windows.Controls.TextBox", "System.Windows.Controls.Primitives.Popup"],
            types);
    }

    /// <summary>
    /// The identity function is what pairs an after-item with its before-item when the annotation
    /// list is diffed. Two annotations that differ only in their element values must land on
    /// distinct positions; keying on the annotation type alone folds them onto one another, so
    /// the second one's position is reported as the first one's.
    /// </summary>
    [Fact]
    public void SenderKeysRepeatedAnnotationsDistinctly()
    {
        var before = ComboBox();
        var after = ComboBox();

        var data = new List<RpcObjectData>();
        var q = new RpcSendQueue(1024, batch => data.AddRange(batch),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);

        q.Send(after, before, () => new JavaSender().VisitType(after, q));
        q.Flush();

        var positionLists = data
            .Select(d => d.Value)
            .OfType<List<int>>()
            .Where(p => p.Count == 2)
            .ToList();
        Assert.NotEmpty(positionLists);
        Assert.Contains(positionLists, p => p is [0, 1]);
        Assert.DoesNotContain(positionLists, p => p is [1, 1] or [0, 0]);
    }

    private static List<RpcObjectData> Send(JavaType.Class after, JavaType.Class? before = null)
    {
        var data = new List<RpcObjectData>();
        var q = new RpcSendQueue(1024, batch => data.AddRange(batch),
            new Dictionary<object, int>(ReferenceEqualityComparer.Instance), null, false);
        q.Send(Reference.AsRef(after), before == null ? null : Reference.AsRef(before),
            () => new JavaSender().VisitType(after, q));
        q.Flush();
        return data;
    }

    private static JavaType.Class RoundTrip(JavaType.Class after, JavaType.Class? before = null)
    {
        var data = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(Send(after, before), RpcJson.Options), RpcJson.Options)!;
        var receiveQueue = new RpcReceiveQueue(data, new Dictionary<int, object>(), null);
        return Assert.IsAssignableFrom<JavaType.Class>(
            receiveQueue.Receive<JavaType>(before, t => new JavaReceiver().VisitType(t, receiveQueue)!));
    }
}

/// <summary>
/// The full-fidelity proof, against the real Java peer: a type carrying two same-typed,
/// value-bearing annotations goes C# &rarr; Java &rarr; C# through the production sender/receiver
/// pairs on both sides. The Java-side <c>JavaTypeAnnotationProbe</c> (test scope in the
/// rewrite-csharp Java module) renders the annotations it materialized into a SearchResult
/// description — which only comes out right if the C# sender and Java receiver agreed on every
/// element and value — and replaces the probed class' type with freshly built annotations whose
/// string constants carry a <c>probed:</c> prefix, defeating the delta cache so the return leg
/// exercises the real Java sender and real C# receiver rather than resolving to local instances.
/// </summary>
public class JavaTypeAnnotationRealRpcTest : RpcRewriteTest
{
    public JavaTypeAnnotationRealRpcTest(RpcFixture fixture) : base(fixture) { }

    private const string CsCompilationUnitType = "org.openrewrite.csharp.tree.Cs$CompilationUnit";

    [Fact]
    public void RepeatedValueBearingAnnotationsRoundTripThroughJava()
    {
        var source = """
            using System;

            [AttributeUsage(AttributeTargets.Class, AllowMultiple = true)]
            sealed class PartAttribute : Attribute
            {
                public string Name { get; set; } = "";
                public Type Type { get; set; } = typeof(object);
            }

            [Part(Name = "PART_A", Type = typeof(string))]
            [Part(Name = "PART_B", Type = typeof(Uri))]
            public class Both { }
            """;

        var syntaxTree = CSharpSyntaxTree.ParseText(source, path: "Both.cs");
        var references = Assemblies.Net90
            .ResolveAsync(Microsoft.CodeAnalysis.LanguageNames.CSharp, CancellationToken.None)
            .GetAwaiter().GetResult();
        var compilation = CSharpCompilation.Create("AnnotationRpcTest")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(references)
            .AddSyntaxTrees(syntaxTree);
        var cu = new CSharpParser().Parse(source, sourcePath: "Both.cs",
            semanticModel: compilation.GetSemanticModel(syntaxTree));

        var server = RewriteRpcServer.Current!;
        var treeId = cu.Id.ToString();
        server.StoreLocalObject(treeId, cu);
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());

        var returned = server.VisitOnRemote(
            "org.openrewrite.csharp.rpc.JavaTypeAnnotationProbe",
            treeId, CsCompilationUnitType, ctxId,
            new Dictionary<string, object?> { ["type"] = "Both" });

        var both = FindClassDeclaration((J)returned, "Both");

        var marker = both.Markers.FindFirst<SearchResult>();
        Assert.NotNull(marker);
        Assert.Equal(
            "@PartAttribute(Name=PART_A,Type=System.String);" +
            "@PartAttribute(Name=PART_B,Type=System.Uri)",
            marker!.Description);

        var cls = Assert.IsAssignableFrom<JavaType.Class>(both.Type);
        var parts = cls.Annotations!.OfType<JavaType.Annotation>()
            .Where(a => a.AnnotationType is JavaType.Class { FullyQualifiedName: "PartAttribute" })
            .ToList();
        Assert.Equal(2, parts.Count);

        var names = parts.Select(a => a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
            .Single(v => v.Element is JavaType.Variable { Name: "Name" }).ConstantValue).ToList();
        Assert.Equal(["probed:PART_A", "probed:PART_B"], names);

        var types = parts.Select(a => Assert.IsAssignableFrom<JavaType.Class>(
                a.Values!.OfType<JavaType.Annotation.SingleElementValue>()
                    .Single(v => v.Element is JavaType.Variable { Name: "Type" }).ReferenceValue)
            .FullyQualifiedName).ToList();
        Assert.Equal(["System.String", "System.Uri"], types);
    }

    private static ClassDeclaration FindClassDeclaration(J tree, string simpleName)
    {
        var finder = new ClassFinder(simpleName)
        {
            Cursor = new Cursor(null, Cursor.ROOT_VALUE)
        };
        finder.Visit(tree, 0);
        Assert.NotNull(finder.Found);
        return finder.Found!;
    }

    private class ClassFinder(string name) : CSharpVisitor<int>
    {
        public ClassDeclaration? Found { get; private set; }

        public override J VisitClassDeclaration(ClassDeclaration cd, int p)
        {
            if (cd.Name.SimpleName == name)
            {
                Found = cd;
            }
            return base.VisitClassDeclaration(cd, p);
        }
    }
}
