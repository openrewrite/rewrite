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

namespace OpenRewrite.Recipes.Net9;

/// <summary>
/// Shared utilities for .NET 9 migration recipes.
/// </summary>
internal static class Net9RecipeHelpers
{
    /// <summary>
    /// Returns true if the given type is a fully qualified type with the specified name.
    /// </summary>
    internal static bool IsTypeReference(JavaType? type, string fullyQualifiedName)
    {
        return type switch
        {
            JavaType.Class cls => cls.FullyQualifiedName == fullyQualifiedName,
            JavaType.Parameterized p => p.Type != null && IsTypeReference(p.Type, fullyQualifiedName),
            _ => false
        };
    }

    /// <summary>
    /// Returns true if the given method type has the specified declaring type and method name.
    /// </summary>
    internal static bool IsMethodCall(JavaType.Method? methodType, string declaringTypeFqn, string methodName)
    {
        return methodType != null
               && methodType.Name == methodName
               && methodType.DeclaringType != null
               && IsTypeReference(methodType.DeclaringType, declaringTypeFqn);
    }

    /// <summary>
    /// Gets the fully qualified type name of an expression (if available).
    /// </summary>
    internal static string? GetExpressionTypeFqn(Expression target)
    {
        var type = target switch
        {
            Identifier id => id.Type,
            FieldAccess fa => fa.Type,
            MethodInvocation mi => mi.MethodType?.ReturnType,
            _ => null
        };

        return type switch
        {
            JavaType.Class cls => cls.FullyQualifiedName,
            JavaType.Parameterized p => (p.Type as JavaType.Class)?.FullyQualifiedName,
            _ => null
        };
    }

    /// <summary>
    /// Adds a Markup.Warn marker to a tree node.
    /// </summary>
    internal static T AddWarnMarker<T>(T tree, string message) where T : J
    {
        var newMarkers = tree.Markers.Add(new Markup.Warn(Guid.NewGuid(), message, null));
        var withMarkers = tree.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        return withMarkers != null ? (T)withMarkers.Invoke(tree, [newMarkers])! : tree;
    }
}
