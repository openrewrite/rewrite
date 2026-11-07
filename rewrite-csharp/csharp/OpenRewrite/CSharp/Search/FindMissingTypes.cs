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
        "A diagnostic recipe that marks where a C# LST lacks type attribution, such as a class declaration or " +
        "identifier without a type, or a method declaration or invocation without a method type.";

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
            return cd is ClassDeclaration { Type: null or JavaType.Unknown } ? missing(cd, "ClassDeclaration type is missing", p) : cd;
        }

        public override J VisitMethodDeclaration(MethodDeclaration method, P p)
        {
            var md = base.VisitMethodDeclaration(method, p);
            return md is MethodDeclaration { MethodType: null } ? missing(md, "MethodDeclaration type is missing", p) : md;
        }

        public override J VisitMethodInvocation(MethodInvocation mi, P p)
        {
            var visited = base.VisitMethodInvocation(mi, p);
            return visited is MethodInvocation { MethodType: null } invocation &&
                   !OnUnknownValue(invocation) && !OperatorWrittenAsCall(invocation)
                ? missing(visited, "MethodInvocation type is missing", p)
                : visited;
        }

        public override J VisitNewClass(NewClass newClass, P p)
        {
            var visited = base.VisitNewClass(newClass, p);
            // `new T()` of a type parameter T calls no constructor known before run time.
            return visited is NewClass { ConstructorType: null } n &&
                   n.Clazz is not Expression { Type: JavaType.GenericTypeVariable } &&
                   !n.Arguments.Elements.Any(a => Unknown(a.Element))
                ? missing(visited, "NewClass type is missing", p)
                : visited;
        }

        public override J VisitIdentifier(Identifier identifier, P p)
        {
            var visited = base.VisitIdentifier(identifier, p);
            return identifier.Type == null && !NamesWhatHasNoType(identifier)
                ? missing(visited, "Identifier type is missing", p)
                : visited;
        }

        /// <summary>
        /// Whether the identifier names what has no type, such as a namespace, label, preprocessor symbol, argument
        /// name or discard. The name of a call, class, method or object creation is left to the check of that node.
        /// </summary>
        private bool NamesWhatHasNoType(Identifier id)
        {
            if (Cursor.FirstEnclosing<UsingDirective>() != null || Cursor.FirstEnclosing<DefineDirective>() != null ||
                Cursor.FirstEnclosing<PragmaWarningDirective>() != null)
                return true;
            var parent = Cursor.ParentTree;
            return parent.Value switch
            {
                NamespaceDeclaration or Label or GotoStatement => true,
                FieldAccess => InNamespace(id, parent),
                MethodInvocation mi => mi.Name == id,
                ClassDeclaration cd => cd.Name == id,
                MethodDeclaration md => md.Name == id,
                NewClass nc => nc.Clazz == id,
                IndexerDeclaration indexer => indexer.Indexer == id,
                TypeParameter tp => tp.Name == id,
                ConstrainedTypeParameter ctp => ctp.Name == id || ctp.WhereConstraint?.Element == id,
                TupleElement te => te.Name == id,
                Interpolation interpolation => interpolation.Format?.Element == id,
                NamedExpression ne => ne.Name.Element == id && parent.ParentTree.Value is not PropertyPattern,
                Case => id.SimpleName == "default",
                NamedVariable => id.SimpleName == "_",
                _ => false
            };
        }

        /// <summary>
        /// Whether a qualified name is a namespace, as <c>System</c> and <c>System.Collections</c> are in
        /// <c>System.Collections.Generic.List&lt;int&gt;</c>. The first type that the name goes on to qualify says
        /// what its namespace is.
        /// </summary>
        private static bool InNamespace(Identifier id, Cursor parent)
        {
            J name = id;
            if (parent.Value is FieldAccess access && access.Name.Element == id)
            {
                name = access;
                parent = parent.ParentTree;
            }
            var qualified = QualifiedName(name);
            for (; parent.Value is FieldAccess outer && outer.Target == name; parent = parent.ParentTree)
            {
                if (TypeUtils.AsClass(outer.Type) is { } type)
                {
                    var outermost = type.FullyQualifiedName.Split('$')[0];
                    var ns = outermost.Contains('.') ? outermost[..outermost.LastIndexOf('.')] : "";
                    return ns == qualified || ns.StartsWith(qualified + ".");
                }
                name = outer;
            }
            return parent.Value is NamespaceDeclaration;
        }

        private static string? QualifiedName(J name) => name switch
        {
            Identifier i => i.SimpleName,
            FieldAccess fa => $"{QualifiedName(fa.Target)}.{fa.Name.Element.SimpleName}",
            _ => null
        };

        private static bool OperatorWrittenAsCall(MethodInvocation mi) =>
            mi.Name.SimpleName is "nameof" or "__makeref" or "__reftype" or "__refvalue";

        /// <summary>
        /// <c>dynamic</c>, and a type that does not resolve, map to an unknown type. A call that invokes such a value,
        /// or takes one as its receiver or an argument, has no method to attribute, and its own value is unknown too.
        /// </summary>
        private static bool OnUnknownValue(MethodInvocation mi) =>
            mi.Name.Type is JavaType.Unknown || Unknown(mi.Select?.Element) ||
            mi.Arguments.Elements.Any(a => Unknown(a.Element));

        private static bool Unknown(Expression? e) =>
            e?.Type is JavaType.Unknown || e is MethodInvocation { MethodType: null } call && OnUnknownValue(call);
    }
}
