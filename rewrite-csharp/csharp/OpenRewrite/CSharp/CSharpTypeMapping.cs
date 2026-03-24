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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Maps Roslyn semantic symbols to OpenRewrite JavaType.
/// Uses a cache with shell objects to handle cyclic type references.
/// </summary>
internal class CSharpTypeMapping
{
    private readonly SemanticModel _model;
    private readonly Dictionary<ISymbol, JavaType> _typeCache = new(SymbolEqualityComparer.Default);

    public CSharpTypeMapping(SemanticModel model)
    {
        _model = model;
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
    /// Gets the method type for a method declaration or invocation.
    /// </summary>
    public JavaType.Method? MethodType(SyntaxNode node)
    {
        ISymbol? symbol = node switch
        {
            MethodDeclarationSyntax => _model.GetDeclaredSymbol(node),
            InvocationExpressionSyntax => _model.GetSymbolInfo(node).Symbol,
            ObjectCreationExpressionSyntax => _model.GetSymbolInfo(node).Symbol,
            _ => _model.GetSymbolInfo(node).Symbol
        };

        if (symbol is IMethodSymbol methodSymbol)
        {
            return MapMethod(methodSymbol);
        }
        return null;
    }

    /// <summary>
    /// Gets the variable type for a variable declarator.
    /// </summary>
    public JavaType.Variable? VariableType(SyntaxNode node)
    {
        var symbol = _model.GetDeclaredSymbol(node);
        return VariableFromSymbol(symbol);
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

        var fieldType = VariableFromSymbol(symbolInfo.Symbol);
        return (type, fieldType);
    }

    private JavaType.Variable? VariableFromSymbol(ISymbol? symbol)
    {
        return symbol switch
        {
            IFieldSymbol f => MapVariable(f, f.Name, MapType(f.ContainingType), MapType(f.Type)),
            ILocalSymbol l => MapVariable(l, l.Name, null, MapType(l.Type)),
            IParameterSymbol p => MapVariable(p, p.Name, null, MapType(p.Type)),
            IPropertySymbol prop => MapVariable(prop, prop.Name, MapType(prop.ContainingType), MapType(prop.Type)),
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

        // Check cache first
        if (_typeCache.TryGetValue(symbol, out var cached))
            return cached;

        return symbol switch
        {
            IArrayTypeSymbol arrayType => MapArrayType(arrayType),
            ITypeParameterSymbol typeParam => MapTypeParameter(typeParam),
            INamedTypeSymbol namedType => MapNamedType(namedType),
            _ => JavaType.Unknown.Instance
        };
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

        var members = symbol.GetMembers()
            .OfType<IFieldSymbol>()
            .Where(f => !f.IsImplicitlyDeclared)
            .Select(f => MapVariable(f, f.Name, cls, MapType(f.Type)))
            .ToList();
        var methods = symbol.GetMembers()
            .OfType<IMethodSymbol>()
            .Where(m => m.MethodKind is MethodKind.Ordinary or MethodKind.Constructor)
            .Select(MapMethod)
            .ToList();

        cls.UnsafeSet(flags, kind, fqn, typeParameters, supertype, owningClass,
            null, interfaces,
            members.Count > 0 ? members : null,
            methods.Count > 0 ? methods : null);

        return cls;
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

        var declaringType = symbol.ContainingType != null
            ? MapType(symbol.ContainingType) as JavaType.FullyQualified
            : null;
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
        return variable;
    }

    private static JavaType.Primitive? MapPrimitive(INamedTypeSymbol symbol)
    {
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
            SpecialType.System_String => JavaType.Primitive.Of(JavaType.PrimitiveKind.String),
            _ => null
        };
    }

    private static long MapFlags(ISymbol symbol)
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

    private static JavaType.FullyQualified.FullyQualifiedKind MapClassKind(INamedTypeSymbol symbol)
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

    private static string GetFullyQualifiedName(INamedTypeSymbol symbol)
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
