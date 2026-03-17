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
namespace OpenRewrite.Java;

/// <summary>
/// Utility methods for working with <see cref="JavaType"/> instances.
/// C# equivalent of Java's <c>org.openrewrite.java.tree.TypeUtils</c>.
/// </summary>
public static class TypeUtils
{
    /// <summary>
    /// Check if a type is a specific fully-qualified class type (exact match, no inheritance).
    /// </summary>
    public static bool IsOfClassType(JavaType? type, string fullyQualifiedName)
    {
        var fqn = GetFullyQualifiedName(type);
        return fqn != null && string.Equals(fqn, fullyQualifiedName, StringComparison.Ordinal);
    }

    /// <summary>
    /// Check if a type is assignable to the target type — i.e., the type IS the target,
    /// extends it, or implements it. Walks the supertype and interface chains.
    /// </summary>
    public static bool IsAssignableTo(JavaType? type, string fullyQualifiedName)
    {
        if (type == null) return false;

        var cls = AsClass(type);
        if (cls == null) return false;

        return IsAssignableToInternal(cls, fullyQualifiedName, new HashSet<string>());
    }

    /// <summary>
    /// Check if a type is assignable to any of the given fully-qualified type names.
    /// </summary>
    public static bool IsAssignableTo(JavaType? type, IReadOnlyCollection<string> fullyQualifiedNames)
    {
        if (type == null) return false;

        var cls = AsClass(type);
        if (cls == null) return false;

        foreach (var fqn in fullyQualifiedNames)
        {
            if (IsAssignableToInternal(cls, fqn, new HashSet<string>()))
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

            if (string.Equals(current.FullyQualifiedName, baseClassFqn, StringComparison.Ordinal))
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

    private static bool IsAssignableToInternal(JavaType.Class cls, string fqn, HashSet<string> seen)
    {
        if (!seen.Add(cls.FullyQualifiedName))
            return false; // Cycle protection

        if (string.Equals(cls.FullyQualifiedName, fqn, StringComparison.Ordinal))
            return true;

        // Check supertype
        var super = AsClass(cls.Supertype);
        if (super != null && IsAssignableToInternal(super, fqn, seen))
            return true;

        // Check interfaces
        if (cls.Interfaces != null)
        {
            foreach (var iface in cls.Interfaces)
            {
                var ifaceCls = AsClass(iface);
                if (ifaceCls != null && IsAssignableToInternal(ifaceCls, fqn, seen))
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

                if (string.Equals(ifaceCls.FullyQualifiedName, interfaceFqn, StringComparison.Ordinal))
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
