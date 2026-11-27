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

namespace OpenRewrite.CSharp;

/// <summary>
/// Utility methods for working with <see cref="JavaType"/> instances.
/// C# equivalent of Java's <c>org.openrewrite.java.tree.TypeUtils</c>.
/// </summary>
public static partial class TypeUtils
{
    /// <summary>
    /// Check if a type is a specific fully-qualified class type, without inheritance.
    /// </summary>
    public static bool IsOfClassType(JavaType? type, string fullyQualifiedName) =>
        FullyQualifiedNameMatches(GetFullyQualifiedName(type), fullyQualifiedName);

    /// <summary>
    /// Whether a type's fully-qualified name matches <paramref name="name"/>, as a recipe writes it.
    /// A name without arity matches every arity, so <c>System.Collections.Generic.List</c> matches
    /// <c>System.Collections.Generic.List`1</c>. That is how recipes name generic types.
    /// </summary>
    public static bool FullyQualifiedNameMatches(string? typeName, string? name)
    {
        if (typeName == null || name == null) return false;
        return string.Equals(typeName, name, StringComparison.Ordinal)
            || string.Equals(WithoutArity(typeName), name, StringComparison.Ordinal);
    }

    /// <summary>
    /// Removes every arity suffix, so <c>N.Outer`1$Inner`2</c> becomes <c>N.Outer$Inner</c>.
    /// </summary>
    internal static string WithoutArity(string fullyQualifiedName) =>
        fullyQualifiedName.Contains('`') ? AritySuffix().Replace(fullyQualifiedName, "") : fullyQualifiedName;

    [System.Text.RegularExpressions.GeneratedRegex(@"`\d+")]
    private static partial System.Text.RegularExpressions.Regex AritySuffix();

    /// <summary>
    /// Check if a type is assignable to the target type — i.e., the type IS the target,
    /// extends it, or implements it. Walks the supertype and interface chains.
    /// </summary>
    public static bool IsAssignableTo(JavaType? type, string fullyQualifiedName) =>
        type != null && IsAssignableToName(type, fullyQualifiedName, exact: false);

    /// <summary>
    /// Check if a type is assignable to the target type, where the target is specified
    /// as a <see cref="JavaType"/> rather than a string FQN. This is the preferred overload
    /// when the target type comes from a parsed AST (e.g., a typed capture's scaffold).
    /// <para>
    /// When the target is a <see cref="JavaType.Parameterized"/> type with
    /// <see cref="JavaType.GenericTypeVariable"/> type parameters, those positions are treated
    /// as wildcards: any concrete type argument satisfies an unbounded type variable, and
    /// bounded type variables check that the candidate's type argument satisfies the bound.
    /// Concrete type parameters must match exactly, recursing into nested generic arguments.
    /// </para>
    /// </summary>
    public static bool IsAssignableTo(JavaType? type, JavaType? targetType)
    {
        if (type == null || targetType == null) return false;

        // Both primitives: same kind means match
        if (type is JavaType.Primitive candPrim && targetType is JavaType.Primitive targetPrim)
            return candPrim.Kind == targetPrim.Kind;

        // Nullable<T>: both Nullable<T> and T are assignable to Nullable<T>
        if (targetType is JavaType.Parameterized { TypeParameters: [var innerType] } nullableParam
            && IsOfClassType(nullableParam.Type, "System.Nullable"))
        {
            // Nullable<T> → Nullable<T>
            if (type is JavaType.Parameterized { TypeParameters: [var sourceInner] } sourceParam
                && IsOfClassType(sourceParam.Type, "System.Nullable"))
                return IsAssignableTo(sourceInner, innerType);

            // T → Nullable<T>
            return IsAssignableTo(type, innerType);
        }

        if (targetType is JavaType.Array targetArray)
            return type is JavaType.Array sourceArray && IsArrayAssignableToArray(sourceArray, targetArray);

        // When target is parameterized with type parameters, do parameter-aware matching.
        // This handles both open generics (with GenericTypeVariable wildcards) and concrete
        // generics (where all type args must match exactly).
        if (targetType is JavaType.Parameterized targetParam
            && targetParam.TypeParameters is { Count: > 0 })
            return IsAssignableToParameterized(type, targetParam);

        // Fall back to raw FQN comparison
        var targetFqn = GetFullyQualifiedName(targetType);
        if (targetFqn == null) return false;

        // The target is a type, so only that type matches.
        return IsAssignableToName(type, targetFqn, exact: true);
    }

    /// <summary>
    /// Check if a type is assignable to any of the given fully-qualified type names.
    /// </summary>
    public static bool IsAssignableTo(JavaType? type, IReadOnlyCollection<string> fullyQualifiedNames)
    {
        if (type == null) return false;

        foreach (var fqn in fullyQualifiedNames)
        {
            if (IsAssignableToName(type, fqn, exact: false))
                return true;
        }
        return false;
    }

    /// <summary>
    /// Check if a type implements a specific interface (not including the type itself).
    /// </summary>
    public static bool Implements(JavaType? type, string interfaceFqn)
    {
        var cls = AsClass(type);
        if (cls == null) return false;

        return ImplementsInternal(cls, interfaceFqn, new HashSet<string>());
    }

    /// <summary>
    /// Check if a type inherits from a specific base class (not including the type itself).
    /// </summary>
    public static bool InheritsFrom(JavaType? type, string baseClassFqn)
    {
        var cls = AsClass(type);
        if (cls == null) return false;

        var current = AsClass(cls.Supertype);
        var seen = new HashSet<string>();
        while (current != null)
        {
            if (!seen.Add(current.FullyQualifiedName))
                break; // Cycle protection

            if (FullyQualifiedNameMatches(current.FullyQualifiedName, baseClassFqn))
                return true;

            current = AsClass(current.Supertype);
        }
        return false;
    }

    /// <summary>
    /// Check if a type has a method with the given name.
    /// </summary>
    public static bool HasMethod(JavaType? type, string methodName)
    {
        var cls = AsClass(type);
        if (cls == null) return false;

        return HasMethodInternal(cls, methodName, new HashSet<string>());
    }

    /// <summary>
    /// Check if a type is a string type.
    /// </summary>
    public static bool IsString(JavaType? type) =>
        type is JavaType.Primitive { Kind: JavaType.Primitive.PrimitiveKind.String } ||
        IsOfClassType(type, "System.String");

    /// <summary>
    /// Check if a type is System.Object.
    /// </summary>
    public static bool IsObject(JavaType? type) =>
        IsOfClassType(type, "System.Object") || IsOfClassType(type, "object");

    /// <summary>
    /// Get the fully-qualified name of a type, or null if not resolvable.
    /// Handles Class, Parameterized, and primitive types.
    /// </summary>
    public static string? GetFullyQualifiedName(JavaType? type)
    {
        return type switch
        {
            JavaType.Class cls => cls.FullyQualifiedName,
            JavaType.Parameterized p => p.Type != null ? GetFullyQualifiedName(p.Type) : null,
            JavaType.Primitive { Kind: JavaType.Primitive.PrimitiveKind.String } => "System.String",
            JavaType.Primitive prim => prim.Keyword,
            _ => null
        };
    }

    /// <summary>
    /// Try to cast a JavaType to JavaType.Class, unwrapping Parameterized if needed.
    /// </summary>
    public static JavaType.Class? AsClass(JavaType? type)
    {
        return type switch
        {
            JavaType.Class cls => cls,
            JavaType.Parameterized p => p.Type as JavaType.Class,
            _ => null
        };
    }

    /// <summary>
    /// Get the type of an expression, handling common node types.
    /// </summary>
    public static JavaType? GetType(Expression expr)
    {
        return expr switch
        {
            MethodInvocation mi => mi.MethodType?.ReturnType,
            NewClass nc => nc.ConstructorType?.DeclaringType,
            Identifier id => id.Type,
            FieldAccess fa => fa.Type,
            Literal lit => lit.Type,
            _ => TryGetTypeDynamic(expr)
        };
    }

    private static bool IsAssignableToName(JavaType type, string fullyQualifiedName, bool exact)
    {
        // Primitives (int, bool, string, etc.) have no Class representation —
        // map to their .NET FQN and compare directly
        if (type is JavaType.Primitive prim)
        {
            var primFqn = PrimitiveToFqn(prim.Kind);
            return primFqn != null && string.Equals(primFqn, fullyQualifiedName, StringComparison.Ordinal);
        }

        // Arrays implement a known set of non-generic interfaces
        if (type is JavaType.Array)
            return IsArrayAssignableTo(fullyQualifiedName);

        var cls = AsClass(type);
        if (cls == null) return false;

        return IsAssignableToInternal(cls, fullyQualifiedName, exact, new HashSet<string>());
    }

    private static bool IsAssignableToInternal(JavaType.Class cls, string fqn, bool exact, HashSet<string> seen)
    {
        if (!seen.Add(cls.FullyQualifiedName))
            return false; // Cycle protection

        if (exact
                ? string.Equals(cls.FullyQualifiedName, fqn, StringComparison.Ordinal)
                : FullyQualifiedNameMatches(cls.FullyQualifiedName, fqn))
            return true;

        // Check supertype
        var super = AsClass(cls.Supertype);
        if (super != null && IsAssignableToInternal(super, fqn, exact, seen))
            return true;

        // Check interfaces
        if (cls.Interfaces != null)
        {
            foreach (var iface in cls.Interfaces)
            {
                var ifaceCls = AsClass(iface);
                if (ifaceCls != null && IsAssignableToInternal(ifaceCls, fqn, exact, seen))
                    return true;
            }
        }

        return false;
    }

    private static bool ImplementsInternal(JavaType.Class cls, string interfaceFqn, HashSet<string> seen)
    {
        if (!seen.Add(cls.FullyQualifiedName))
            return false;

        if (cls.Interfaces != null)
        {
            foreach (var iface in cls.Interfaces)
            {
                var ifaceCls = AsClass(iface);
                if (ifaceCls == null) continue;

                if (FullyQualifiedNameMatches(ifaceCls.FullyQualifiedName, interfaceFqn))
                    return true;

                // Check super-interfaces
                if (ImplementsInternal(ifaceCls, interfaceFqn, seen))
                    return true;
            }
        }

        // Also check supertype's interfaces
        var super = AsClass(cls.Supertype);
        if (super != null)
            return ImplementsInternal(super, interfaceFqn, seen);

        return false;
    }

    private static bool HasMethodInternal(JavaType.Class cls, string methodName, HashSet<string> seen)
    {
        if (!seen.Add(cls.FullyQualifiedName))
            return false;

        if (cls.Methods != null)
        {
            foreach (var method in cls.Methods)
            {
                if (string.Equals(method.Name, methodName, StringComparison.Ordinal))
                    return true;
            }
        }

        // Check supertype
        var super = AsClass(cls.Supertype);
        if (super != null && HasMethodInternal(super, methodName, seen))
            return true;

        return false;
    }

    /// <summary>
    /// Check if <paramref name="type"/> is assignable to a parameterized target type.
    /// Walks the candidate's supertype/interface chain looking for a
    /// <see cref="JavaType.Parameterized"/> whose raw type matches, then compares type
    /// arguments position-by-position.
    /// </summary>
    private static bool IsAssignableToParameterized(JavaType type, JavaType.Parameterized target)
    {
        var targetFqn = GetFullyQualifiedName(target.Type);
        if (targetFqn == null) return false;

        // Collect all parameterized types in the candidate's hierarchy that match the target FQN
        var matching = new List<JavaType.Parameterized>();
        CollectParameterizedMatches(type, targetFqn, matching, new HashSet<string>());

        // Check if any matching parameterized type satisfies the type argument constraints
        foreach (var match in matching)
        {
            if (TypeParametersMatch(match.TypeParameters, target.TypeParameters))
                return true;
        }

        return false;
    }

    /// <summary>
    /// Recursively collect <see cref="JavaType.Parameterized"/> types in the hierarchy
    /// of <paramref name="type"/> whose raw FQN matches <paramref name="targetFqn"/>.
    /// When walking from a <see cref="JavaType.Parameterized"/> type, resolves
    /// <see cref="JavaType.GenericTypeVariable"/> entries in supertypes/interfaces by
    /// substituting the actual type arguments (mirroring Java's <c>maybeResolveParameters</c>).
    /// </summary>
    private static void CollectParameterizedMatches(JavaType? type, string targetFqn,
        List<JavaType.Parameterized> results, HashSet<string> seen)
    {
        if (type == null) return;

        if (type is JavaType.Parameterized param)
        {
            var rawFqn = GetFullyQualifiedName(param.Type);
            if (rawFqn != null)
            {
                if (string.Equals(rawFqn, targetFqn, StringComparison.Ordinal))
                    results.Add(param);

                // Continue walking the underlying class's hierarchy, resolving
                // type parameters in supertypes/interfaces using the actual type args
                if (param.Type is JavaType.Class cls && seen.Add(cls.FullyQualifiedName))
                {
                    var resolved = MaybeResolveParameters(param, cls.Supertype as JavaType.FullyQualified);
                    CollectParameterizedMatches(resolved ?? cls.Supertype, targetFqn, results, seen);
                    if (cls.Interfaces != null)
                    {
                        foreach (var iface in cls.Interfaces)
                        {
                            resolved = MaybeResolveParameters(param, iface);
                            CollectParameterizedMatches(resolved ?? iface, targetFqn, results, seen);
                        }
                    }
                }
            }
        }
        else if (type is JavaType.Array arr)
        {
            // T[] implements IEnumerable<T>, IList<T>, etc. — synthesize matches
            if (arr.ElemType != null)
                CollectArrayParameterizedMatches(arr.ElemType, targetFqn, results);
        }
        else if (type is JavaType.Class cls)
        {
            if (!seen.Add(cls.FullyQualifiedName)) return;

            CollectParameterizedMatches(cls.Supertype, targetFqn, results, seen);
            if (cls.Interfaces != null)
            {
                foreach (var iface in cls.Interfaces)
                    CollectParameterizedMatches(iface, targetFqn, results, seen);
            }
        }
    }

    /// <summary>
    /// When a <see cref="JavaType.Parameterized"/> type (e.g., <c>Dictionary&lt;string, int&gt;</c>)
    /// has a supertype or interface that is also parameterized (e.g., <c>IDictionary&lt;TKey, TValue&gt;</c>),
    /// resolve the target's type parameters by substituting the source's actual type arguments
    /// for the formal type parameters.
    /// <para>
    /// Mirrors Java's <c>TypeUtils.maybeResolveParameters</c>.
    /// </para>
    /// </summary>
    private static JavaType.Parameterized? MaybeResolveParameters(
        JavaType.Parameterized source, JavaType.FullyQualified? target)
    {
        if (target is not JavaType.Parameterized targetParam)
            return null;

        var sourceClass = source.Type as JavaType.Class;
        if (sourceClass?.TypeParameters == null || source.TypeParameters == null)
            return null;

        if (sourceClass.TypeParameters.Count != source.TypeParameters.Count)
            return null;

        // Build substitution map: formal type param → actual type arg
        var map = new Dictionary<string, JavaType>();
        for (int i = 0; i < sourceClass.TypeParameters.Count; i++)
        {
            if (sourceClass.TypeParameters[i] is JavaType.GenericTypeVariable gtv)
                map[gtv.Name] = source.TypeParameters[i];
        }

        if (map.Count == 0 || targetParam.TypeParameters == null)
            return null;

        // Apply substitution to the target's type parameters
        var resolved = new List<JavaType>(targetParam.TypeParameters.Count);
        bool changed = false;
        foreach (var tp in targetParam.TypeParameters)
        {
            var sub = SubstituteTypeParam(tp, map);
            resolved.Add(sub);
            if (!ReferenceEquals(sub, tp)) changed = true;
        }

        return changed ? new JavaType.Parameterized(targetParam.Type, resolved) : targetParam;
    }

    /// <summary>
    /// Replace a <see cref="JavaType.GenericTypeVariable"/> with its substitution
    /// from the map. For <see cref="JavaType.Parameterized"/> types, recursively
    /// substitute type parameters.
    /// </summary>
    private static JavaType SubstituteTypeParam(JavaType type, Dictionary<string, JavaType> map)
    {
        if (type is JavaType.GenericTypeVariable gtv && map.TryGetValue(gtv.Name, out var replacement))
            return replacement;

        if (type is JavaType.Parameterized param && param.TypeParameters != null)
        {
            var substituted = new List<JavaType>(param.TypeParameters.Count);
            bool changed = false;
            foreach (var tp in param.TypeParameters)
            {
                var sub = SubstituteTypeParam(tp, map);
                substituted.Add(sub);
                if (!ReferenceEquals(sub, tp)) changed = true;
            }
            if (changed)
                return new JavaType.Parameterized(param.Type, substituted);
        }

        return type;
    }

    /// <summary>
    /// Compare type parameter lists position-by-position. A <see cref="JavaType.GenericTypeVariable"/>
    /// in the target acts as a wildcard (any concrete type matches). If the variable has bounds,
    /// the candidate's type argument must be assignable to all bounds.
    /// Concrete type parameters are compared invariantly and structurally: nested generic
    /// arguments must match recursively, and other arguments require an exact FQN match.
    /// </summary>
    private static bool TypeParametersMatch(IList<JavaType>? candidateParams, IList<JavaType>? targetParams) =>
        TypeParametersMatch(candidateParams, targetParams, new HashSet<(JavaType, JavaType)>());

    private static bool TypeParametersMatch(IList<JavaType>? candidateParams, IList<JavaType>? targetParams,
        HashSet<(JavaType, JavaType)> inProgress)
    {
        if (targetParams == null || targetParams.Count == 0) return true;
        if (candidateParams == null || candidateParams.Count != targetParams.Count) return false;

        for (int i = 0; i < targetParams.Count; i++)
        {
            if (!TypeArgumentMatches(candidateParams[i], targetParams[i], inProgress))
                return false;
        }
        return true;
    }

    private static bool TypeArgumentMatches(JavaType? candidate, JavaType? target,
        HashSet<(JavaType, JavaType)> inProgress)
    {
        if (candidate == null || target == null) return false;

        switch (target)
        {
            case JavaType.GenericTypeVariable gtv:
                if (gtv.Bounds == null) return true;
                foreach (var bound in gtv.Bounds)
                {
                    var boundFqn = GetFullyQualifiedName(bound);
                    if (boundFqn != null && !IsAssignableToName(candidate, boundFqn, exact: true))
                        return false;
                }
                return true;

            case JavaType.Parameterized { TypeParameters.Count: > 0 } targetParam:
                if (candidate is not JavaType.Parameterized candidateParam) return false;
                var targetFqn = GetFullyQualifiedName(targetParam.Type);
                if (targetFqn == null ||
                    !string.Equals(targetFqn, GetFullyQualifiedName(candidateParam.Type), StringComparison.Ordinal))
                    return false;
                // A pair already being compared is assumed to match; any real difference
                // is still found on the path that reaches a non-cyclic argument.
                if (!inProgress.Add((candidate, target))) return true;
                return TypeParametersMatch(candidateParam.TypeParameters, targetParam.TypeParameters, inProgress);

            default:
                return AreEquivalent(candidate, target);
        }
    }

    /// <summary>
    /// Map a <see cref="JavaType.Primitive.PrimitiveKind"/> to its .NET fully-qualified type name.
    /// Returns null for non-value primitives (Null, None, Void).
    /// </summary>
    private static string? PrimitiveToFqn(JavaType.Primitive.PrimitiveKind kind) => kind switch
    {
        JavaType.Primitive.PrimitiveKind.Boolean => "System.Boolean",
        JavaType.Primitive.PrimitiveKind.Byte => "System.Byte",
        JavaType.Primitive.PrimitiveKind.Char => "System.Char",
        JavaType.Primitive.PrimitiveKind.Double => "System.Double",
        JavaType.Primitive.PrimitiveKind.Float => "System.Single",
        JavaType.Primitive.PrimitiveKind.Int => "System.Int32",
        JavaType.Primitive.PrimitiveKind.Long => "System.Int64",
        JavaType.Primitive.PrimitiveKind.Short => "System.Int16",
        JavaType.Primitive.PrimitiveKind.String => "System.String",
        _ => null
    };

    // ===============================================================
    // Array type support
    // ===============================================================

    /// <summary>
    /// Non-generic supertypes of single-dimensional .NET arrays.
    /// </summary>
    private static readonly HashSet<string> ArrayNonGenericSupertypes =
    [
        "System.Array",
        "System.Object",
        "System.ICloneable",
        "System.Collections.IList",
        "System.Collections.ICollection",
        "System.Collections.IEnumerable",
        "System.Collections.IStructuralComparable",
        "System.Collections.IStructuralEquatable"
    ];

    /// <summary>
    /// FQNs of generic interfaces that single-dimensional .NET arrays implement,
    /// parameterized by the element type.
    /// </summary>
    private static readonly string[] ArrayGenericInterfaceFqns =
    [
        "System.Collections.Generic.IEnumerable`1",
        "System.Collections.Generic.ICollection`1",
        "System.Collections.Generic.IList`1",
        "System.Collections.Generic.IReadOnlyCollection`1",
        "System.Collections.Generic.IReadOnlyList`1"
    ];

    /// <summary>
    /// Check if a <see cref="JavaType.Array"/> is assignable to a non-generic target FQN.
    /// </summary>
    private static bool IsArrayAssignableTo(string fqn) => ArrayNonGenericSupertypes.Contains(fqn);

    /// <summary>
    /// Check if <c>S[]</c> is assignable to <c>T[]</c>: identical element types always are,
    /// and reference element types are additionally covariant (<c>string[]</c> to <c>object[]</c>).
    /// Value element types are invariant. Rank is not compared because the type model
    /// does not retain it.
    /// </summary>
    private static bool IsArrayAssignableToArray(JavaType.Array source, JavaType.Array target)
    {
        if (ReferenceEquals(source, target)) return true;

        var sourceElem = source.ElemType;
        var targetElem = target.ElemType;
        if (sourceElem == null || targetElem == null) return false;

        if (AreEquivalent(sourceElem, targetElem)) return true;

        if (!IsReferenceType(sourceElem) || !IsReferenceType(targetElem)) return false;

        return IsObject(targetElem) || IsAssignableTo(sourceElem, targetElem);
    }

    private static bool AreEquivalent(JavaType? a, JavaType? b)
    {
        if (ReferenceEquals(a, b)) return true;
        if (a == null || b == null) return false;

        switch (a, b)
        {
            case (JavaType.Array arrA, JavaType.Array arrB):
                return AreEquivalent(arrA.ElemType, arrB.ElemType);
            case (JavaType.Primitive primA, JavaType.Primitive primB):
                return primA.Kind == primB.Kind;
            case (JavaType.GenericTypeVariable gtvA, JavaType.GenericTypeVariable gtvB):
                return string.Equals(gtvA.Name, gtvB.Name, StringComparison.Ordinal);
            case (JavaType.Array, _) or (_, JavaType.Array):
            case (JavaType.GenericTypeVariable, _) or (_, JavaType.GenericTypeVariable):
                return false;
        }

        var fqnA = a is JavaType.Primitive pa ? PrimitiveToFqn(pa.Kind) : GetFullyQualifiedName(a);
        var fqnB = b is JavaType.Primitive pb ? PrimitiveToFqn(pb.Kind) : GetFullyQualifiedName(b);
        if (fqnA == null || !string.Equals(fqnA, fqnB, StringComparison.Ordinal)) return false;

        var paramsA = (a as JavaType.Parameterized)?.TypeParameters;
        var paramsB = (b as JavaType.Parameterized)?.TypeParameters;
        var countA = paramsA?.Count ?? 0;
        var countB = paramsB?.Count ?? 0;
        if (countA != countB) return false;
        for (var i = 0; i < countA; i++)
        {
            if (!AreEquivalent(paramsA![i], paramsB![i]))
                return false;
        }
        return true;
    }

    private static bool IsReferenceType(JavaType type)
    {
        switch (type)
        {
            case JavaType.Array:
                return true;
            case JavaType.Primitive prim:
                return prim.Kind == JavaType.Primitive.PrimitiveKind.String;
            case JavaType.Class or JavaType.Parameterized:
                var cls = AsClass(type);
                if (cls == null) return false;
                if (cls.FullyQualifiedName is "System.ValueType" or "System.Enum") return true;
                if (cls.ClassKind is JavaType.FullyQualified.FullyQualifiedKind.Value
                    or JavaType.FullyQualified.FullyQualifiedKind.Enum)
                    return false;
                if (ValueTypeFqns.Contains(WithoutArity(cls.FullyQualifiedName))) return false;
                return !InheritsFrom(cls, "System.ValueType");
            default:
                return false;
        }
    }

    private static readonly HashSet<string> ValueTypeFqns =
    [
        "System.Boolean",
        "System.Byte",
        "System.SByte",
        "System.Char",
        "System.Decimal",
        "System.Double",
        "System.Single",
        "System.Int16",
        "System.UInt16",
        "System.Int32",
        "System.UInt32",
        "System.Int64",
        "System.UInt64",
        "System.IntPtr",
        "System.UIntPtr",
        "System.Nullable",
        "System.ValueTuple"
    ];

    /// <summary>
    /// Synthesize the generic interfaces that <c>T[]</c> implements as
    /// <see cref="JavaType.Parameterized"/> instances with the element type substituted.
    /// Used by <see cref="CollectParameterizedMatches"/> to handle array assignability.
    /// </summary>
    private static void CollectArrayParameterizedMatches(JavaType elemType, string targetFqn,
        List<JavaType.Parameterized> results)
    {
        foreach (var ifaceFqn in ArrayGenericInterfaceFqns)
        {
            if (FullyQualifiedNameMatches(ifaceFqn, targetFqn))
            {
                // Synthesize Parameterized(IFoo<elemType>)
                var rawClass = new JavaType.Class { FullyQualifiedName = ifaceFqn };
                results.Add(new JavaType.Parameterized(rawClass, [elemType]));
            }
        }
    }

    private static JavaType? TryGetTypeDynamic(Expression expr)
    {
        try
        {
            return ((dynamic)expr).Type as JavaType;
        }
        catch
        {
            return null;
        }
    }
}
