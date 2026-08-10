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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Enumerates the public API of a set of .NET assemblies into OpenRewrite
/// <see cref="JavaType"/> for a per-coordinate set of exported types. Public types defined
/// in the <c>own</c> assemblies get complete bodies — members, methods, supertype, interfaces;
/// every other referenced type (BCL, other packages) comes back as a shallow FQN-only class
/// for the caller to resolve.
/// <para>
/// FQN, flag, and kind formatting reuse <see cref="CSharpTypeMapping"/>'s helpers so the
/// enumerated types match what the parser emits for the same symbols.
/// </para>
/// </summary>
internal static class AssemblyTypeEnumerator
{
    public static List<JavaType.FullyQualified> Enumerate(
        IReadOnlyList<string> ownAssemblies, IReadOnlyList<string> referenceAssemblies)
    {
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var ownRefs = new List<PortableExecutableReference>();
        var allRefs = new List<MetadataReference>();
        foreach (var path in ownAssemblies)
        {
            if (!seen.Add(Path.GetFullPath(path))) continue;
            var reference = MetadataReference.CreateFromFile(path);
            ownRefs.Add(reference);
            allRefs.Add(reference);
        }
        foreach (var path in referenceAssemblies)
        {
            if (!seen.Add(Path.GetFullPath(path))) continue;
            allRefs.Add(MetadataReference.CreateFromFile(path));
        }

        var compilation = CSharpCompilation.Create("ExportedTypes",
                options: new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(allRefs);

        // The public types the own assemblies actually define (walking namespaces and
        // nested types). Forwarded types belong to their target assembly's symbol, so
        // they don't appear here.
        var ownTypes = new List<INamedTypeSymbol>();
        foreach (var reference in ownRefs)
        {
            if (compilation.GetAssemblyOrModuleSymbol(reference) is IAssemblySymbol assembly)
            {
                CollectPublicTypes(assembly.GlobalNamespace, ownTypes);
            }
        }

        // The Java type model keys types by FQN and doesn't encode generic arity, so a
        // non-generic type and a same-named generic (e.g. JsonConverter and JsonConverter<T>)
        // collapse to one FQN. Keep one per FQN — the lowest-arity (the base) — deterministically,
        // matching JVM class-uniqueness and the writer's first-wins FQN dedup.
        var byFqn = new Dictionary<string, INamedTypeSymbol>(ownTypes.Count, StringComparer.Ordinal);
        foreach (var type in ownTypes)
        {
            var fqn = CSharpTypeMapping.GetFullyQualifiedName(type);
            if (!byFqn.TryGetValue(fqn, out var existing) || type.Arity < existing.Arity)
            {
                byFqn[fqn] = type;
            }
        }

        var ownFqns = new HashSet<string>(byFqn.Keys, StringComparer.Ordinal);
        var mapping = new AssemblyTypeMapping(ownFqns);
        var result = new List<JavaType.FullyQualified>(byFqn.Count);
        foreach (var type in byFqn.Values)
        {
            if (mapping.MapDefined(type) is { } fq)
            {
                result.Add(fq);
            }
        }
        return result;
    }

    private static void CollectPublicTypes(INamespaceSymbol ns, List<INamedTypeSymbol> into)
    {
        foreach (var type in ns.GetTypeMembers())
        {
            CollectPublicTypes(type, into);
        }
        foreach (var child in ns.GetNamespaceMembers())
        {
            CollectPublicTypes(child, into);
        }
    }

    private static void CollectPublicTypes(INamedTypeSymbol type, List<INamedTypeSymbol> into)
    {
        if (type.DeclaredAccessibility != Accessibility.Public)
        {
            return;
        }
        into.Add(type);
        foreach (var nested in type.GetTypeMembers())
        {
            CollectPublicTypes(nested, into);
        }
    }
}

/// <summary>
/// Maps Roslyn symbols to <see cref="JavaType"/> for a set of exported types. Own types
/// (FQN in <c>ownFqns</c>) get full bodies; everything else is a shallow FQN-only class.
/// A per-compilation symbol cache handles cycles and makes each type map once.
/// </summary>
internal sealed class AssemblyTypeMapping
{
    private readonly HashSet<string> _ownFqns;
    private readonly Dictionary<ISymbol, JavaType> _cache = new(SymbolEqualityComparer.Default);

    public AssemblyTypeMapping(HashSet<string> ownFqns)
    {
        _ownFqns = ownFqns;
    }

    public JavaType.FullyQualified? MapDefined(INamedTypeSymbol symbol) =>
        Map(symbol) as JavaType.FullyQualified;

    private JavaType Map(ITypeSymbol? symbol)
    {
        if (symbol == null) return JavaType.Unknown.Instance;
        if (_cache.TryGetValue(symbol, out var cached)) return cached;
        return symbol switch
        {
            IArrayTypeSymbol array => MapArray(array),
            ITypeParameterSymbol typeParam => MapTypeParameter(typeParam),
            // A reference whose defining assembly wasn't loaded is a missing-metadata
            // INamedTypeSymbol (an IErrorTypeSymbol) that still carries its FQN from the .dll's
            // TypeRef, so it flows to MapNamed and comes back a shallow FQN class-ref for the
            // caller to resolve — rather than being lost to Unknown. This is what lets the public
            // API be enumerated from the artifact alone, without the referenced assemblies present.
            INamedTypeSymbol named when !string.IsNullOrEmpty(named.Name) => MapNamed(named),
            _ => JavaType.Unknown.Instance
        };
    }

    private JavaType MapNamed(INamedTypeSymbol symbol)
    {
        var primitive = CSharpTypeMapping.MapPrimitive(symbol);
        if (primitive != null) return primitive;

        if (_cache.TryGetValue(symbol, out var cached)) return cached;

        // Generic instantiation (List<int>) → Parameterized over the raw definition.
        if (symbol.IsGenericType && !symbol.IsDefinition)
        {
            var parameterized = new JavaType.Parameterized();
            _cache[symbol] = parameterized;
            var raw = MapNamed(symbol.OriginalDefinition);
            if (!ReferenceEquals(raw, parameterized) && raw is JavaType.FullyQualified rawFq)
            {
                var typeArgs = symbol.TypeArguments.Select(Map).ToList();
                parameterized.UnsafeSet(rawFq, typeArgs!);
                return parameterized;
            }
            _cache.Remove(symbol); // fall through to a plain class
        }

        var fqn = CSharpTypeMapping.GetFullyQualifiedName(symbol);
        var flags = CSharpTypeMapping.MapFlags(symbol);
        var kind = CSharpTypeMapping.MapClassKind(symbol);

        // A type the own assemblies reference but don't define: a ShallowClass carrying
        // only its FQN, for the caller to resolve.
        if (!_ownFqns.Contains(fqn))
        {
            var shallow = new JavaType.ShallowClass(flags, kind, fqn, null);
            _cache[symbol] = shallow;
            return shallow;
        }

        // An own type: shell-cache first (cycles), then the complete public API.
        var cls = new JavaType.Class();
        _cache[symbol] = cls;

        var typeParameters = symbol.TypeParameters.Length > 0
            ? symbol.TypeParameters.Select(tp => Map(tp)).ToList()
            : null;
        var supertype = symbol.BaseType != null
            ? Map(symbol.BaseType) as JavaType.FullyQualified
            : null;
        var owningClass = symbol.ContainingType != null
            ? Map(symbol.ContainingType) as JavaType.FullyQualified
            : null;
        var interfaces = symbol.Interfaces.Length > 0
            ? symbol.Interfaces.Select(i => Map(i) as JavaType.FullyQualified)
                .Where(i => i != null).Select(i => i!).ToList()
            : null;

        cls.UnsafeSet(flags, kind, fqn, typeParameters, supertype, owningClass,
            ListAnnotations(symbol), interfaces, MapMembers(symbol, cls), MapMethods(symbol, cls));
        return cls;
    }

    /// <summary>
    /// The attributes on a symbol. This is the only place a referenced control's
    /// <c>[TemplatePart]</c>s can come from once the source set has been parsed: an LST resolves
    /// dependency types through the type table this enumerator feeds, not through the per-file
    /// parse, so an attribute mapped only by <see cref="CSharpTypeMapping"/> would not survive a
    /// build.
    /// </summary>
    private IList<JavaType.FullyQualified>? ListAnnotations(ISymbol symbol) =>
        AttributeMapping.ListAnnotations(symbol, Map, MapVariable,
            m => Map(m.ContainingType) is JavaType.Class owner ? MapMethod(m, owner) : null);

    private List<JavaType.Variable>? MapMembers(INamedTypeSymbol symbol, JavaType.Class owner)
    {
        List<JavaType.Variable>? members = null;
        foreach (var member in symbol.GetMembers())
        {
            if (!IsAccessible(member)) continue;
            switch (member)
            {
                case IFieldSymbol { IsImplicitlyDeclared: false } field:
                    (members ??= new()).Add(MapVariable(field, field.Name, owner, Map(field.Type)));
                    break;
                case IPropertySymbol property:
                    (members ??= new()).Add(MapVariable(property, property.Name, owner, Map(property.Type)));
                    break;
            }
        }
        return members;
    }

    private List<JavaType.Method>? MapMethods(INamedTypeSymbol symbol, JavaType.Class owner)
    {
        List<JavaType.Method>? methods = null;
        foreach (var method in symbol.GetMembers().OfType<IMethodSymbol>())
        {
            if (!IsAccessible(method)) continue;
            if (method.MethodKind != MethodKind.Ordinary && method.MethodKind != MethodKind.Constructor) continue;
            (methods ??= new()).Add(MapMethod(method, owner));
        }
        return methods;
    }

    private JavaType.Method MapMethod(IMethodSymbol symbol, JavaType.Class owner)
    {
        // Shell-cache before mapping annotations: an attribute applied to an attribute class'
        // own constructor leads straight back here.
        if (_cache.TryGetValue(symbol, out var cached) && cached is JavaType.Method cachedMethod)
        {
            return cachedMethod;
        }
        var method = new JavaType.Method();
        _cache[symbol] = method;
        // Constructors return void in Roslyn, but the Java model expects the declaring type.
        var returnType = symbol.MethodKind == MethodKind.Constructor
            ? owner
            : Map(symbol.ReturnType);
        method.UnsafeSet(
            symbol.Name,
            CSharpTypeMapping.MapFlags(symbol),
            owner,
            returnType,
            symbol.Parameters.Length > 0 ? symbol.Parameters.Select(p => p.Name).ToList() : null,
            symbol.Parameters.Length > 0 ? symbol.Parameters.Select(p => Map(p.Type)).ToList()! : null,
            null,
            null, // annotations, set below once this method's shell is cached
            null,
            symbol.TypeParameters.Length > 0 ? symbol.TypeParameters.Select(tp => tp.Name).ToList() : null);
        method.Annotations = ListAnnotations(symbol);
        return method;
    }

    private JavaType.Variable MapVariable(ISymbol symbol, string name, JavaType? owner, JavaType? type)
    {
        return new JavaType.Variable(name, owner, type, ListAnnotations(symbol))
        {
            FlagsBitMap = CSharpTypeMapping.MapFlags(symbol)
        };
    }

    private JavaType MapArray(IArrayTypeSymbol symbol)
    {
        var array = new JavaType.Array();
        _cache[symbol] = array;
        array.UnsafeSet(Map(symbol.ElementType), null);
        return array;
    }

    private JavaType MapTypeParameter(ITypeParameterSymbol symbol)
    {
        var generic = new JavaType.GenericTypeVariable();
        _cache[symbol] = generic;
        var variance = symbol.Variance switch
        {
            VarianceKind.Out => JavaType.GenericTypeVariable.VarianceKind.Covariant,
            VarianceKind.In => JavaType.GenericTypeVariable.VarianceKind.Contravariant,
            _ => JavaType.GenericTypeVariable.VarianceKind.Invariant
        };
        var bounds = symbol.ConstraintTypes.Length > 0
            ? symbol.ConstraintTypes.Select(Map).ToList()
            : null;
        generic.UnsafeSet(symbol.Name, variance, bounds!);
        return generic;
    }

    private static bool IsAccessible(ISymbol symbol) =>
        symbol.DeclaredAccessibility is Accessibility.Public
            or Accessibility.Protected
            or Accessibility.ProtectedOrInternal;
}
