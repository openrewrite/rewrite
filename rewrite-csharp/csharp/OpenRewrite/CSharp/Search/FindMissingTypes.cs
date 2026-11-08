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
using OpenRewrite.Java;
using static OpenRewrite.CSharp.Recipes.Categories;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Search;

[Category, CSharp, Search]
public class FindMissingTypes : Recipe
{
    public override string DisplayName => "Find missing type information on C# LSTs";

    public override string Description =>
        "A diagnostic recipe that marks where a C# LST lacks type attribution, such as a class declaration, " +
        "identifier or field access without a type, or a method or operator declaration or an invocation without a method type. " +
        "It also marks a type with a missing part, such as a method without a declaring type, and a type whose name " +
        "differs from the name the source gives it.";

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        new Visitor<ExecutionContext>((j, message, _) => SearchResult.Found(j, message));

    /// <summary>Each node of the source that the recipe would mark, as the reason and the node's first line.</summary>
    public static List<string> Find(SourceFile source)
    {
        var found = new List<string>();
        new Visitor<List<string>>((j, message, list) =>
        {
            list.Add($"{message}: {FirstLine(j)}");
            return j;
        }).Visit(source, found);
        return found;
    }

    private static string FirstLine(J j)
    {
        var printed = new CSharpPrinter<object>().Print(J.SetPrefix(j, Space.Empty));
        var end = printed.IndexOf('\n');
        return end < 0 ? printed : printed[..end].TrimEnd() + " ...";
    }

    private sealed class Visitor<P>(Func<J, string, P, J> missing) : CSharpVisitor<P>
    {
        /// <summary>The parser attributes types in the first branch only.</summary>
        public override J VisitConditionalDirective(ConditionalDirective directive, P p) =>
            directive.WithBranches(ListUtils.Map(directive.Branches, (i, b) => i == 0 ? VisitRightPadded(b, p) : b));

        public override J VisitClassDeclaration(ClassDeclaration classDecl, P p)
        {
            var cd = base.VisitClassDeclaration(classDecl, p);
            return cd is ClassDeclaration declaration && Defect(declaration.Type) is { } defect
                ? missing(cd, $"ClassDeclaration type is {defect}", p)
                : cd;
        }

        public override J VisitMethodDeclaration(MethodDeclaration method, P p)
        {
            var md = base.VisitMethodDeclaration(method, p);
            if (md is not MethodDeclaration declaration)
                return md;
            if (Defect(declaration.MethodType) is { } defect)
                return missing(md, $"MethodDeclaration type is {defect}", p);
            var name = declaration.MethodType!.Name;
            return name != ValueText(declaration.Name) && !IsConstructorOrDestructor(declaration)
                ? missing(md, $"MethodDeclaration type has a different name '{name}'", p)
                : md;
        }

        public override J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
        {
            var od = base.VisitOperatorDeclaration(operatorDeclaration, p);
            return od is OperatorDeclaration declaration && Defect(declaration.MethodType) is { } defect
                ? missing(od, $"OperatorDeclaration type is {defect}", p)
                : od;
        }

        public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
        {
            var cod = base.VisitConversionOperatorDeclaration(conversion, p);
            return cod is ConversionOperatorDeclaration declaration && Defect(declaration.MethodType) is { } defect
                ? missing(cod, $"ConversionOperatorDeclaration type is {defect}", p)
                : cod;
        }

        public override J VisitMethodInvocation(MethodInvocation mi, P p)
        {
            var visited = base.VisitMethodInvocation(mi, p);
            if (visited is not MethodInvocation invocation)
                return visited;
            if (invocation.MethodType == null)
                return !OnUnknownValue(invocation) && !OperatorWrittenAsCall(invocation)
                    ? missing(visited, "MethodInvocation type is missing", p)
                    : visited;
            if (Defect(invocation.MethodType) is { } defect)
                return missing(visited, $"MethodInvocation type is {defect}", p);
            return ReferenceEquals(invocation.Name.Type, invocation.MethodType)
                ? visited
                : missing(visited, "MethodInvocation name type is not its method type", p);
        }

        public override J VisitNewClass(NewClass newClass, P p)
        {
            var visited = base.VisitNewClass(newClass, p);
            // `new T()` of a type parameter T calls no constructor known before run time.
            return visited is NewClass n && Defect(n.ConstructorType) is { } defect &&
                   (n.ConstructorType != null || n.Clazz is not Expression { Type: JavaType.GenericTypeVariable } &&
                       !n.Arguments.Elements.Any(a => Unknown(a.Element)))
                ? missing(visited, $"NewClass type is {defect}", p)
                : visited;
        }

        /// <summary>
        /// An access whose name has no type is marked at its name. A method group in <c>nameof</c> converts to no
        /// delegate, so it has no type.
        /// </summary>
        public override J VisitFieldAccess(FieldAccess fieldAccess, P p)
        {
            var visited = base.VisitFieldAccess(fieldAccess, p);
            return fieldAccess is { Type: null, Name.Element.Type: not null } && !InNameof()
                ? missing(visited, "FieldAccess type is missing", p)
                : visited;
        }

        public override J VisitIdentifier(Identifier identifier, P p)
        {
            var visited = base.VisitIdentifier(identifier, p);
            if (Defect(identifier.Type) is { } defect && !NamesWhatHasNoType(identifier))
                return missing(visited, $"Identifier type is {defect}", p);
            if (identifier.FieldType == null)
                return visited;
            if (Defect(identifier.FieldType) is { } fieldDefect)
                return missing(visited, $"Identifier field type is {fieldDefect}", p);
            return identifier.FieldType is JavaType.Variable variable && variable.Name != ValueText(identifier)
                ? missing(visited, $"Identifier field type has a different name '{variable.Name}'", p)
                : visited;
        }

        /// <summary>
        /// The name without the <c>@</c> of a verbatim identifier such as <c>@base</c>, as Roslyn's <c>ValueText</c> has it.
        /// </summary>
        private static string ValueText(Identifier id) => id.SimpleName.StartsWith('@') ? id.SimpleName[1..] : id.SimpleName;

        /// <summary>
        /// A constructor's method is <c>.ctor</c>, or <c>.cctor</c> when static, and the method of a destructor <c>~A</c> is
        /// <c>Finalize</c>.
        /// </summary>
        private static bool IsConstructorOrDestructor(MethodDeclaration md) =>
            md.MethodType!.Name is ".ctor" or ".cctor" || md.Name.SimpleName.StartsWith('~');

        /// <summary>
        /// What is wrong with a type, as Java's <c>TypeUtils.isWellFormedType</c> finds it. A type is malformed when a type
        /// it is made of is missing.
        /// </summary>
        private static string? Defect(JavaType? type) =>
            type is null or JavaType.Unknown ? "missing" :
            HasMissingPart(type, new HashSet<JavaType>(ReferenceEqualityComparer.Instance)) ? "malformed" : null;

        private static bool HasMissingPart(JavaType type, HashSet<JavaType> seen) => seen.Add(type) && type switch
        {
            JavaType.Parameterized parameterized => Missing(parameterized.Type, seen) ||
                                                    AnyMissing(parameterized.TypeParameters, seen),
            JavaType.Array array => Missing(array.ElemType, seen),
            JavaType.GenericTypeVariable variable => AnyMissing(variable.Bounds, seen),
            JavaType.Variable variable => Missing(variable.Type, seen) || Missing(variable.Owner, seen),
            JavaType.MultiCatch multiCatch => AnyMissing(multiCatch.ThrowableTypes, seen),
            JavaType.Method method => Missing(method.ReturnType, seen) || Missing(method.DeclaringType, seen) ||
                                      AnyMissing(method.ParameterTypes, seen),
            _ => false
        };

        private static bool Missing(JavaType? type, HashSet<JavaType> seen) =>
            type is null or JavaType.Unknown || HasMissingPart(type, seen);

        private static bool AnyMissing(IList<JavaType>? types, HashSet<JavaType> seen) =>
            types?.Any(t => Missing(t, seen)) == true;

        /// <summary>
        /// Whether the identifier names what has no type, such as a namespace, label, attribute target, preprocessor
        /// symbol, argument name, discard or constraint keyword such as <c>notnull</c>. The name of a call, class, method
        /// or object creation is left to the check of that node.
        /// </summary>
        private bool NamesWhatHasNoType(Identifier id)
        {
            // The parser gives an empty name to what the source leaves out, such as each type argument of
            // `typeof(Dictionary<,>)` or the variable of `catch (E) when (..)`.
            if (id.SimpleName == "" || Cursor.FirstEnclosing<UsingDirective>() != null ||
                Cursor.FirstEnclosing<DefineDirective>() != null || Cursor.FirstEnclosing<PragmaWarningDirective>() != null)
                return true;
            if (InNameof())
                return true;
            var parent = Cursor.ParentTree;
            return parent.Value switch
            {
                NamespaceDeclaration or Label or GotoStatement or DiscardVariableDesignation => true,
                FieldAccess or AliasQualifiedName => InNamespace(id, parent),
                AttributeList list => list.Target?.Element == id,
                MethodInvocation mi => mi.Name == id,
                ClassDeclaration cd => cd.Name == id,
                MethodDeclaration md => md.Name == id,
                NewClass nc => nc.Clazz == id,
                IndexerDeclaration indexer => indexer.Indexer == id,
                TypeParameter tp => tp.Name == id,
                ConstrainedTypeParameter ctp => ctp.Name == id || ctp.WhereConstraint?.Element == id ||
                                                id.SimpleName is "notnull" or "unmanaged",
                TupleElement te => te.Name == id,
                Interpolation interpolation => interpolation.Format?.Element == id,
                NamedExpression ne => ne.Name.Element == id && parent.ParentTree.Value is not PropertyPattern,
                Case => id.SimpleName == "default",
                NamedVariable => id.SimpleName == "_",
                _ => false
            };
        }

        /// <summary>
        /// <c>nameof</c> takes a name, which may be a namespace or a group of overloads, and neither has a type.
        /// </summary>
        private bool InNameof() =>
            Cursor.FirstEnclosing<MethodInvocation>() is { Select: null, Name.SimpleName: "nameof" };

        /// <summary>
        /// Whether a qualified name is a namespace, as <c>System</c> and <c>System.Collections</c> are in
        /// <c>System.Collections.Generic.List&lt;int&gt;</c>. The first type that the name goes on to qualify says
        /// what its namespace is.
        /// </summary>
        private bool InNamespace(Identifier id, Cursor parent)
        {
            J name = id;
            if (parent.Value is AliasQualifiedName || parent.Value is FieldAccess access && access.Name.Element == id)
            {
                name = (J)parent.Value;
                parent = parent.ParentTree;
            }
            if (QualifiedName(name) is not { } qualified)
                return false;
            for (; parent.Value is FieldAccess outer && outer.Target == name; parent = parent.ParentTree)
            {
                if (Namespace(outer.Type) is { } ns)
                    return EnclosingNamespacePrefixes().Any(e => ns == e + qualified || ns.StartsWith(e + qualified + "."));
                name = outer;
            }
            return parent.Value is NamespaceDeclaration;
        }

        /// <summary>The namespaces a name is looked up in, each as a prefix that ends in a dot, the global one first.</summary>
        private IEnumerable<string> EnclosingNamespacePrefixes()
        {
            var prefix = "";
            yield return prefix;
            foreach (var ns in Cursor.PathToRoot().Select(c => c.Value).OfType<NamespaceDeclaration>().Reverse())
            foreach (var segment in QualifiedName(ns.Name.Element)?.Split('.') ?? [])
                yield return prefix += segment + ".";
        }

        /// <summary>The namespace of a type. C#'s keyword types map to primitives, and are types of <c>System</c>.</summary>
        private static string? Namespace(JavaType? type)
        {
            if (type is JavaType.Primitive)
                return "System";
            if (TypeUtils.AsClass(type) is not { } c)
                return null;
            var outermost = c.FullyQualifiedName.Split('$')[0];
            return outermost.Contains('.') ? outermost[..outermost.LastIndexOf('.')] : "";
        }

        private static string? QualifiedName(J name) => name switch
        {
            // `global::System` is one identifier where the parser reads it as part of a type name.
            Identifier i => i.SimpleName.Split("::")[^1],
            AliasQualifiedName aliased => QualifiedName(aliased.Name),
            FieldAccess fa => QualifiedName(fa.Target) is { } target ? $"{target}.{fa.Name.Element.SimpleName}" : null,
            _ => null
        };

        private static bool OperatorWrittenAsCall(MethodInvocation mi) =>
            mi.Name.SimpleName is "nameof" or "__makeref" or "__reftype" or "__refvalue";

        /// <summary>
        /// A value of type <c>dynamic</c>, or of a type that does not resolve, has members unknown before run time. A
        /// call that invokes such a value, or takes one as its receiver or an argument, has no method to attribute, and
        /// its own value is unknown too.
        /// </summary>
        private static bool OnUnknownValue(MethodInvocation mi) =>
            mi.Name.Type is JavaType.Unknown || Unknown(mi.Select?.Element) ||
            mi.Arguments.Elements.Any(a => Unknown(a.Element));

        private static bool Unknown(Expression? e) =>
            e?.Type is JavaType.Unknown or JavaType.Class { FullyQualifiedName: "dynamic" } ||
            e is MethodInvocation { MethodType: null } call && OnUnknownValue(call);
    }
}
