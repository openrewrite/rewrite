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
using Microsoft.CodeAnalysis.CSharp.Syntax;
using Microsoft.CodeAnalysis.Operations;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Maps Roslyn semantic symbols to OpenRewrite JavaType.
/// Uses a cache with shell objects to handle cyclic type references.
/// </summary>
internal class CSharpTypeMapping
{
    private readonly SemanticModel _model;
    private readonly Dictionary<ISymbol, JavaType> _typeCache;

    /// <param name="model">Semantic model for the document being parsed.</param>
    /// <param name="sharedTypeCache">
    /// Optional symbol→JavaType cache shared across all documents in the same project
    /// (Roslyn Compilation). Symbols are interned per Compilation, so sharing this cache
    /// makes a given type resolve to a single JavaType instance across the whole project,
    /// letting the RPC layer (asRef) serialize it once instead of once per referencing file.
    /// When null, a fresh per-document cache is used (single-file parsing, tests).
    /// </param>
    public CSharpTypeMapping(SemanticModel model, Dictionary<ISymbol, JavaType>? sharedTypeCache = null)
    {
        _model = model;
        _typeCache = sharedTypeCache ?? new Dictionary<ISymbol, JavaType>(SymbolEqualityComparer.Default);
    }

    /// <summary>
    /// Gets the type for a syntax node, or null if unavailable.
    /// </summary>
    public JavaType? Type(SyntaxNode node)
    {
        var typeInfo = _model.GetTypeInfo(node);
        if (typeInfo.Type != null)
        {
            return MapType(typeInfo.Type);
        }

        // For type references (e.g., in `new X()` or type constraints),
        // GetTypeInfo may return null. Fall back to GetSymbolInfo.
        var symbolInfo = _model.GetSymbolInfo(node);
        if (symbolInfo.Symbol is INamedTypeSymbol namedType)
        {
            return MapType(namedType);
        }

        // A method group has no type of its own. Its value is the delegate its conversion creates, such as the
        // Func<int, int> that `Handler` becomes when passed as a `Delegate`.
        if (_model.GetOperation(node) is IMethodReferenceOperation { Parent: IDelegateCreationOperation creation })
        {
            return MapType(creation.Type);
        }

        return null;
    }

    /// <summary>
    /// Gets the fully-qualified type for a type declaration (class, struct, interface, etc.).
    /// </summary>
    public JavaType.FullyQualified? ClassType(SyntaxNode node)
    {
        var symbol = _model.GetDeclaredSymbol(node);
        if (symbol is INamedTypeSymbol namedType)
        {
            return MapType(namedType) as JavaType.FullyQualified;
        }
        return null;
    }

    /// <summary>
    /// Gets the raw (unparameterized) type for a generic name, e.g., List for List&lt;int&gt;.
    /// Returns the type itself if it's not a generic instantiation.
    /// </summary>
    public JavaType? RawType(SyntaxNode node)
    {
        var typeInfo = _model.GetTypeInfo(node);
        var typeSymbol = typeInfo.Type ?? (_model.GetSymbolInfo(node).Symbol as INamedTypeSymbol);
        if (typeSymbol is INamedTypeSymbol named && named.IsGenericType && !named.IsDefinition)
        {
            return MapType(named.OriginalDefinition);
        }
        return typeSymbol != null ? MapType(typeSymbol) : null;
    }

    /// <summary>
    /// Gets the method type for a method, constructor, destructor or operator declaration, or for an invocation.
    /// A primary constructor is identified by its parameter list and exact declaring syntax.
    /// </summary>
    public JavaType.Method? MethodType(SyntaxNode node)
    {
        ISymbol? symbol = node switch
        {
            BaseMethodDeclarationSyntax => _model.GetDeclaredSymbol(node),
            LocalFunctionStatementSyntax => _model.GetDeclaredSymbol(node),
            ParameterListSyntax { Parent: TypeDeclarationSyntax declaration } =>
                (_model.GetDeclaredSymbol(declaration) as INamedTypeSymbol)?.InstanceConstructors
                    .SingleOrDefault(c => c.DeclaringSyntaxReferences.Any(r =>
                        r.SyntaxTree == declaration.SyntaxTree && r.Span == declaration.Span)),
            InvocationExpressionSyntax => _model.GetSymbolInfo(node).Symbol,
            // Roslyn binds a delegate creation to no constructor, while the compiled code calls the delegate's one.
            BaseObjectCreationExpressionSyntax => _model.GetSymbolInfo(node).Symbol ??
                (_model.GetTypeInfo(node).Type is INamedTypeSymbol { TypeKind: TypeKind.Delegate } delegateType
                    ? delegateType.InstanceConstructors.FirstOrDefault()
                    : null),
            _ => _model.GetSymbolInfo(node).Symbol
        };

        if (symbol is IMethodSymbol methodSymbol)
        {
            return MapMethod(methodSymbol);
        }
        return null;
    }

    /// <summary>
    /// The type of the name an invocation calls: its method, or <see cref="JavaType.Unknown"/> when the invoked value
    /// is <c>dynamic</c> and the method is picked at run time.
    /// </summary>
    public JavaType? InvokedNameType(InvocationExpressionSyntax node) =>
        (JavaType?)MethodType(node) ??
        (_model.GetTypeInfo(node.Expression).Type is IDynamicTypeSymbol ? JavaType.Unknown.Instance : null);

    /// <summary>
    /// Gets the variable type for a variable declarator.
    /// </summary>
    public JavaType.Variable? VariableType(SyntaxNode node)
    {
        var symbol = _model.GetDeclaredSymbol(node);
        return VariableFromSymbol(symbol);
    }

    /// <summary>
    /// The variable that a query clause declares, such as <c>w</c> in <c>from w in words</c>. Roslyn gives a range
    /// variable no type of its own. The query translates to method calls. There a <c>let</c> variable holds its
    /// expression, and every other range variable is a lambda parameter declared where the variable is.
    /// </summary>
    public JavaType.Variable? RangeVariableType(SyntaxNode clause)
    {
        if (_model.GetDeclaredSymbol(clause) is not IRangeVariableSymbol variable)
            return null;
        var type = clause is LetClauseSyntax let
            ? _model.GetTypeInfo(let.Expression).Type
            : _model.GetOperation(clause.FirstAncestorOrSelf<QueryExpressionSyntax>()!)?.Descendants()
                .OfType<IAnonymousFunctionOperation>()
                .SelectMany(lambda => lambda.Symbol.Parameters)
                .FirstOrDefault(parameter => parameter.Locations.SequenceEqual(variable.Locations))?.Type;
        return MapVariable(variable, variable.Name, Owner(variable), MapType(type));
    }

    /// <summary>
    /// Gets the variable type for an identifier reference (not a declaration).
    /// Returns null if the identifier doesn't reference a field, local, parameter, or property.
    /// </summary>
    public JavaType.Variable? FieldType(SyntaxNode node)
    {
        var symbol = _model.GetSymbolInfo(node).Symbol;
        return VariableFromSymbol(symbol);
    }

    /// <summary>
    /// Resolves both the type and fieldType for an identifier reference in a single
    /// GetSymbolInfo call, avoiding redundant Roslyn API queries.
    /// </summary>
    public (JavaType? type, JavaType.Variable? fieldType) TypeAndFieldType(SyntaxNode node)
    {
        var typeInfo = _model.GetTypeInfo(node);
        var type = typeInfo.Type != null ? MapType(typeInfo.Type) : null;

        var symbolInfo = _model.GetSymbolInfo(node);

        // If Type() didn't resolve via GetTypeInfo, fall back to symbol
        if (type == null && symbolInfo.Symbol is INamedTypeSymbol namedType)
        {
            type = MapType(namedType);
        }
        // The name of a method group, such as `Console.WriteLine` in `Action a = Console.WriteLine;`, has no type of its
        // own. It names the method that its conversion picks, as the name of a call does. A group that no conversion
        // narrows, as in `nameof(M)` or an argument of type `Delegate`, names its method when it has only one.
        else if (type == null && (symbolInfo.Symbol ?? SoleMember(node)) is IMethodSymbol method)
        {
            type = MapMethod(method);
        }

        var fieldType = VariableFromSymbol(symbolInfo.Symbol);
        return (type, fieldType);
    }

    /// <summary>
    /// The method that declares a local, parameter or range variable, a lambda or local function included. An indexer's
    /// parameter belongs to its accessor, as its uses in the accessor's body do.
    /// </summary>
    private JavaType? Owner(ISymbol variable) => variable.ContainingSymbol switch
    {
        IMethodSymbol method => MapMethod(method),
        IPropertySymbol indexer when (indexer.GetMethod ?? indexer.SetMethod) is { } accessor => MapMethod(accessor),
        _ => DeclaringType(variable.ContainingType)
    };

    private ISymbol? SoleMember(SyntaxNode node) => _model.GetMemberGroup(node) is [var member] ? member : null;

    private JavaType.Variable? VariableFromSymbol(ISymbol? symbol)
    {
        return symbol switch
        {
            IFieldSymbol f => MapVariable(f, f.Name, DeclaringType(f.ContainingType), MapType(f.Type)),
            ILocalSymbol l => MapVariable(l, l.Name, Owner(l), MapType(l.Type)),
            IParameterSymbol p => MapVariable(p, p.Name, Owner(p), MapType(p.Type)),
            IPropertySymbol prop => MapVariable(prop, prop.Name, DeclaringType(prop.ContainingType), MapType(prop.Type)),
            IEventSymbol evt => MapVariable(evt, evt.Name, DeclaringType(evt.ContainingType), MapType(evt.Type)),
            _ => null
        };
    }

    /// <summary>
    /// Maps a Roslyn ITypeSymbol to a JavaType.
    /// </summary>
    public JavaType? MapType(ITypeSymbol? symbol)
    {
        if (symbol == null) return null;
        if (symbol is IErrorTypeSymbol) return JavaType.Unknown.Instance;
        if (symbol is INamedTypeSymbol named && MapPrimitive(named) is { } primitive) return primitive;

        // Check cache first
        if (_typeCache.TryGetValue(symbol, out var cached))
            return cached;

        return symbol switch
        {
            IArrayTypeSymbol arrayType => MapArrayType(arrayType),
            ITypeParameterSymbol typeParam => MapTypeParameter(typeParam),
            INamedTypeSymbol namedType => MapNamedType(namedType),
            // The Java model has no pointers. A pointer is its element's type, as Cs.PointerType's type is.
            IPointerTypeSymbol pointer => MapType(pointer.PointedAtType),
            IFunctionPointerTypeSymbol functionPointer => MapMethod(functionPointer.Signature),
            IDynamicTypeSymbol dynamicType => MapDynamic(dynamicType),
            _ => JavaType.Unknown.Instance
        };
    }

    /// <summary>
    /// <c>dynamic</c> is an <c>object</c> whose members bind at run time. It keeps its own name, so that a recipe can
    /// tell such a value from an <c>object</c> and from a type that does not resolve.
    /// </summary>
    private JavaType MapDynamic(IDynamicTypeSymbol symbol)
    {
        var dynamic = new JavaType.Class(0, JavaType.FullyQualified.FullyQualifiedKind.Class, "dynamic", null,
            MapType(_model.Compilation.GetSpecialType(SpecialType.System_Object)) as JavaType.FullyQualified,
            null, null, null, null, null);
        _typeCache[symbol] = dynamic;
        return dynamic;
    }

    private JavaType MapNamedType(INamedTypeSymbol symbol)
    {
        // Check for primitive types
        var primitive = MapPrimitive(symbol);
        if (primitive != null) return primitive;

        // Check cache
        if (_typeCache.TryGetValue(symbol, out var cached))
            return cached;

        // Handle generic instantiations as Parameterized
        if (symbol.IsGenericType && !symbol.IsDefinition)
        {
            var parameterized = new JavaType.Parameterized();
            _typeCache[symbol] = parameterized;

            var rawType = MapNamedType(symbol.OriginalDefinition);

            // Guard: if rawType resolved back to the same Parameterized (cache collision
            // between symbol and OriginalDefinition) or is not a FullyQualified, fall back
            if (ReferenceEquals(rawType, parameterized) || rawType is not JavaType.FullyQualified fq)
            {
                _typeCache.Remove(symbol);
                // Fall through to create as a plain Class below
            }
            else
            {
                var typeArgs = symbol.TypeArguments.Select(MapType).ToList()!;
                parameterized.UnsafeSet(fq, typeArgs!);
                return parameterized;
            }
        }

        return MapClass(symbol);
    }

    /// <summary>
    /// The type that declares a member. A keyword type such as <c>int</c> types its values as a primitive, but declares
    /// its members as the struct it is, <c>System.Int32</c>. The type cache holds that struct under the keyword type's
    /// symbol, and <see cref="MapType"/> answers the primitive before it reads the cache.
    /// </summary>
    private JavaType.FullyQualified? DeclaringType(INamedTypeSymbol? symbol) =>
        symbol == null ? null :
        MapPrimitive(symbol) == null ? MapType(symbol) as JavaType.FullyQualified :
        _typeCache.TryGetValue(symbol, out var declaring) ? (JavaType.FullyQualified)declaring : MapClass(symbol);

    private JavaType.Class MapClass(INamedTypeSymbol symbol)
    {
        // Shell cache pattern: create empty shell, cache it, then populate
        var cls = new JavaType.Class();
        _typeCache[symbol] = cls;

        var flags = MapFlags(symbol);
        var kind = MapClassKind(symbol);
        var fqn = GetFullyQualifiedName(symbol);
        var typeParameters = symbol.TypeParameters.Length > 0
            ? symbol.TypeParameters.Select(tp => MapType(tp)!).ToList()
            : null;
        var supertype = symbol.BaseType != null
            ? MapType(symbol.BaseType) as JavaType.FullyQualified
            : null;
        var owningClass = symbol.ContainingType != null
            ? MapType(symbol.ContainingType) as JavaType.FullyQualified
            : null;
        var interfaces = symbol.Interfaces.Length > 0
            ? symbol.Interfaces.Select(i => (JavaType.FullyQualified)MapType(i)!).ToList()
            : null;

        cls.UnsafeSet(flags, kind, fqn, typeParameters, supertype, owningClass,
            null, interfaces, null, null);

        _memberQueue.Enqueue((symbol, cls));
        DrainMemberQueue();

        return cls;
    }

    /// <summary>
    /// Types whose <see cref="JavaType.Class.Members"/>/<see cref="JavaType.Class.Methods"/>
    /// still need filling in. Populating a type's members maps the member signature types, which
    /// in turn queues those types; draining iteratively keeps the recursion depth bounded by the
    /// supertype/interface/type-argument graph rather than by the member graph.
    /// </summary>
    private readonly Queue<(INamedTypeSymbol Symbol, JavaType.Class Class)> _memberQueue = new();

    private bool _draining;

    private void DrainMemberQueue()
    {
        if (_draining) return;
        _draining = true;
        try
        {
            while (_memberQueue.Count > 0)
            {
                var (symbol, cls) = _memberQueue.Dequeue();
                PopulateMembers(symbol, cls);
            }
        }
        finally
        {
            _draining = false;
        }
    }

    /// <summary>
    /// Maps the members a type <em>declares</em> (inherited members stay reachable through
    /// <see cref="JavaType.Class.Supertype"/>/<see cref="JavaType.Class.Interfaces"/>, matching
    /// the Java type mapping). Compiler-generated members (auto-property backing fields, property
    /// and event accessors, static initializers, and anything with a mangled name) are skipped.
    /// </summary>
    private void PopulateMembers(INamedTypeSymbol symbol, JavaType.Class cls)
    {
        cls.Annotations = ListAnnotations(symbol);

        List<JavaType.Variable>? members = null;
        List<JavaType.Method>? methods = null;

        foreach (var member in symbol.GetMembers())
        {
            if (IsCompilerGenerated(member)) continue;

            switch (member)
            {
                case IFieldSymbol field:
                    (members ??= []).Add(MapVariable(field, field.Name, cls, MapType(field.Type)));
                    break;
                case IPropertySymbol property:
                    (members ??= []).Add(MapVariable(property, property.Name, cls, MapType(property.Type)));
                    break;
                case IEventSymbol evt:
                    (members ??= []).Add(MapVariable(evt, evt.Name, cls, MapType(evt.Type)));
                    break;
                case IMethodSymbol method:
                    (methods ??= []).Add(MapMethod(method));
                    break;
            }
        }

        cls.Members = members;
        cls.Methods = methods;
    }

    private static bool IsCompilerGenerated(ISymbol member)
    {
        if (member.Name.Length == 0 || member.Name[0] == '<') return true;

        return member switch
        {
            IFieldSymbol field => field.AssociatedSymbol != null,
            IMethodSymbol method => method.AssociatedSymbol != null ||
                                    method.MethodKind == MethodKind.StaticConstructor,
            INamedTypeSymbol => true,
            _ => false
        };
    }

    private JavaType MapArrayType(IArrayTypeSymbol symbol)
    {
        var array = new JavaType.Array();
        _typeCache[symbol] = array;
        array.UnsafeSet(MapType(symbol.ElementType), null);
        return array;
    }

    private JavaType MapTypeParameter(ITypeParameterSymbol symbol)
    {
        var generic = new JavaType.GenericTypeVariable();
        _typeCache[symbol] = generic;

        var variance = symbol.Variance switch
        {
            VarianceKind.Out => JavaType.GenericTypeVariable.VarianceKind.Covariant,
            VarianceKind.In => JavaType.GenericTypeVariable.VarianceKind.Contravariant,
            _ => JavaType.GenericTypeVariable.VarianceKind.Invariant
        };

        var bounds = symbol.ConstraintTypes.Length > 0
            ? symbol.ConstraintTypes.Select(MapType).ToList()!
            : null;

        generic.UnsafeSet(symbol.Name, variance, bounds!);
        return generic;
    }

    private JavaType.Method MapMethod(IMethodSymbol symbol)
    {
        if (_typeCache.TryGetValue(symbol, out var cached) && cached is JavaType.Method m)
            return m;

        var method = new JavaType.Method();
        _typeCache[symbol] = method;

        var declaringType = DeclaringType(symbol.ContainingType);
        // In Roslyn, constructor ReturnType is void, but the Java model expects
        // the declaring type as the return type for constructors
        var returnType = symbol.MethodKind == MethodKind.Constructor && symbol.ContainingType != null
            ? MapType(symbol.ContainingType)
            : MapType(symbol.ReturnType);
        var parameterNames = symbol.Parameters.Length > 0
            ? symbol.Parameters.Select(p => p.Name).ToList()
            : null;
        var parameterTypes = symbol.Parameters.Length > 0
            ? symbol.Parameters.Select(p => MapType(p.Type)!).ToList()
            : null;

        method.UnsafeSet(
            symbol.Name,
            MapFlags(symbol),
            declaringType,
            returnType,
            parameterNames,
            parameterTypes,
            null, // thrownExceptions (C# doesn't have checked exceptions)
            null, // annotations
            null, // defaultValue
            symbol.TypeParameters.Length > 0
                ? symbol.TypeParameters.Select(tp => tp.Name).ToList()
                : null
        );

        method.Annotations = ListAnnotations(symbol);

        return method;
    }

    private JavaType.Variable MapVariable(ISymbol symbol, string name, JavaType? owner, JavaType? type)
    {
        if (_typeCache.TryGetValue(symbol, out var cached) && cached is JavaType.Variable v)
            return v;

        var variable = new JavaType.Variable(name, owner, type, null)
        {
            FlagsBitMap = MapFlags(symbol)
        };
        _typeCache[symbol] = variable;
        variable.Annotations = ListAnnotations(symbol);
        return variable;
    }

    /// <summary>
    /// Maps the attributes applied to a symbol onto <c>Annotations</c>. The type and member
    /// mappings passed in are this mapper's, so an attribute's type resolves to the same instance
    /// every other reference to it does.
    /// </summary>
    private IList<JavaType.FullyQualified>? ListAnnotations(ISymbol symbol) =>
        AttributeMapping.ListAnnotations(symbol, MapType, MapVariable, MapMethod);

    internal static JavaType.Primitive? MapPrimitive(INamedTypeSymbol symbol)
    {
        // System.String is intentionally omitted: it is a reference type with interfaces
        // (IEnumerable<char>, IComparable<string>, etc.) that TypeUtils.IsAssignableTo needs
        // to walk. Mapping it as JavaType.Class preserves the full type hierarchy.
        // J.Literal and J.Primitive (the syntax node for the `string` keyword) still use
        // JavaType.Primitive(String) directly — they don't go through this method.
        return symbol.SpecialType switch
        {
            SpecialType.System_Boolean => JavaType.Primitive.Of(JavaType.PrimitiveKind.Boolean),
            SpecialType.System_Byte => JavaType.Primitive.Of(JavaType.PrimitiveKind.Byte),
            SpecialType.System_Char => JavaType.Primitive.Of(JavaType.PrimitiveKind.Char),
            SpecialType.System_Double => JavaType.Primitive.Of(JavaType.PrimitiveKind.Double),
            SpecialType.System_Single => JavaType.Primitive.Of(JavaType.PrimitiveKind.Float),
            SpecialType.System_Int32 => JavaType.Primitive.Of(JavaType.PrimitiveKind.Int),
            SpecialType.System_Int64 => JavaType.Primitive.Of(JavaType.PrimitiveKind.Long),
            SpecialType.System_Int16 => JavaType.Primitive.Of(JavaType.PrimitiveKind.Short),
            SpecialType.System_Void => JavaType.Primitive.Of(JavaType.PrimitiveKind.Void),
            _ => null
        };
    }

    internal static long MapFlags(ISymbol symbol)
    {
        long flags = 0;
        // Map accessibility
        flags |= (long)(symbol.DeclaredAccessibility switch
        {
            Accessibility.Public => 1,       // Flag.Public
            Accessibility.Private => 2,      // Flag.Private
            Accessibility.Protected => 4,    // Flag.Protected
            _ => 0
        });
        if (symbol.IsStatic) flags |= 8;     // Flag.Static
        if (symbol.IsAbstract) flags |= 1024; // Flag.Abstract
        if (symbol.IsSealed) flags |= 16;     // Flag.Final (sealed ~ final)
        // C#-specific: mark extension methods (bit 20, not used by Java Flag enum)
        if (symbol is IMethodSymbol { IsExtensionMethod: true }) flags |= 1L << 20;
        return flags;
    }

    internal static JavaType.FullyQualified.FullyQualifiedKind MapClassKind(INamedTypeSymbol symbol)
    {
        return symbol.TypeKind switch
        {
            TypeKind.Enum => JavaType.FullyQualified.FullyQualifiedKind.Enum,
            TypeKind.Interface => JavaType.FullyQualified.FullyQualifiedKind.Interface,
            TypeKind.Struct => JavaType.FullyQualified.FullyQualifiedKind.Value,
            _ when symbol.IsRecord => JavaType.FullyQualified.FullyQualifiedKind.Record,
            _ => JavaType.FullyQualified.FullyQualifiedKind.Class
        };
    }

    internal static string GetFullyQualifiedName(INamedTypeSymbol symbol)
    {
        if (symbol.ContainingType != null)
        {
            return GetFullyQualifiedName(symbol.ContainingType) + "$" + symbol.Name;
        }

        var ns = symbol.ContainingNamespace;
        if (ns == null || ns.IsGlobalNamespace)
        {
            return symbol.Name;
        }

        return ns.ToDisplayString() + "." + symbol.Name;
    }
}
