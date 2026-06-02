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
using System.Reflection;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Validates that no Space in the AST contains non-whitespace characters.
/// Catches parser bugs where source code content is incorrectly placed into
/// Space.Whitespace or Comment.Suffix instead of being properly parsed into AST nodes.
/// </summary>
public class WhitespaceValidator : CSharpVisitor<List<WhitespaceViolation>>
{
    public override J? PreVisit(J tree, List<WhitespaceViolation> violations)
    {
        ValidateSpacesOn(tree, violations);
        return tree;
    }

    private static void ValidateSpacesOn(J node, List<WhitespaceViolation> violations)
    {
        var nodeType = node.GetType();
        var typeName = nodeType.Name;

        // Check the Prefix property directly (every J has one)
        ValidateSpace(node.Prefix, typeName, "Prefix", violations);

        // Walk all properties looking for Space, JRightPadded, JLeftPadded, JContainer fields
        foreach (var prop in nodeType.GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            if (prop.Name == "Prefix") continue; // Already checked above

            var propType = prop.PropertyType;
            object? value;
            try
            {
                value = prop.GetValue(node);
            }
            catch
            {
                continue;
            }

            if (value == null) continue;

            if (propType == typeof(Space))
            {
                ValidateSpace((Space)value, typeName, prop.Name, violations);
            }
            else if (IsJRightPaddedType(propType))
            {
                ValidateRightPadded(value, typeName, prop.Name, violations);
            }
            else if (IsJLeftPaddedType(propType))
            {
                ValidateLeftPadded(value, typeName, prop.Name, violations);
            }
            else if (IsJContainerType(propType))
            {
                ValidateContainer(value, typeName, prop.Name, violations);
            }
            else if (IsListOfJRightPadded(propType))
            {
                ValidateListOfRightPadded(value, typeName, prop.Name, violations);
            }
        }
    }

    private static void ValidateSpace(Space space, string nodeType, string fieldName, List<WhitespaceViolation> violations)
    {
        if (!IsWhitespaceOnly(space.Whitespace))
        {
            violations.Add(new WhitespaceViolation(
                nodeType, fieldName, "Whitespace", space.Whitespace));
        }

        foreach (var comment in space.Comments)
        {
            if (!IsWhitespaceOnly(comment.Suffix))
            {
                violations.Add(new WhitespaceViolation(
                    nodeType, fieldName, "Comment.Suffix", comment.Suffix));
            }
        }
    }

    private static bool IsWhitespaceOnly(string value)
    {
        if (string.IsNullOrEmpty(value)) return true;
        foreach (var c in value)
        {
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '\v')
                return false;
        }
        return true;
    }

    private static bool IsJRightPaddedType(Type type)
        => type.IsGenericType && type.GetGenericTypeDefinition() == typeof(JRightPadded<>);

    private static bool IsJLeftPaddedType(Type type)
        => type.IsGenericType && type.GetGenericTypeDefinition() == typeof(JLeftPadded<>);

    private static bool IsJContainerType(Type type)
        => type.IsGenericType && type.GetGenericTypeDefinition() == typeof(JContainer<>);

    private static bool IsListOfJRightPadded(Type type)
    {
        if (!type.IsGenericType) return false;
        var genDef = type.GetGenericTypeDefinition();
        if (genDef != typeof(IList<>) && genDef != typeof(List<>)) return false;
        var elemType = type.GetGenericArguments()[0];
        return IsJRightPaddedType(elemType);
    }

    private static void ValidateRightPadded(object padded, string nodeType, string fieldName, List<WhitespaceViolation> violations)
    {
        var type = padded.GetType();
        var afterProp = type.GetProperty("After");
        if (afterProp?.GetValue(padded) is Space afterSpace)
        {
            ValidateSpace(afterSpace, nodeType, $"{fieldName}.After", violations);
        }
    }

    private static void ValidateLeftPadded(object padded, string nodeType, string fieldName, List<WhitespaceViolation> violations)
    {
        var type = padded.GetType();
        var beforeProp = type.GetProperty("Before");
        if (beforeProp?.GetValue(padded) is Space beforeSpace)
        {
            ValidateSpace(beforeSpace, nodeType, $"{fieldName}.Before", violations);
        }
    }

    private static void ValidateContainer(object container, string nodeType, string fieldName, List<WhitespaceViolation> violations)
    {
        var type = container.GetType();
        var beforeProp = type.GetProperty("Before");
        if (beforeProp?.GetValue(container) is Space beforeSpace)
        {
            ValidateSpace(beforeSpace, nodeType, $"{fieldName}.Before", violations);
        }

        // Also check After spaces on each element in the container
        var elementsProp = type.GetProperty("Elements");
        if (elementsProp?.GetValue(container) is System.Collections.IEnumerable elements)
        {
            int i = 0;
            foreach (var elem in elements)
            {
                if (elem != null)
                {
                    ValidateRightPadded(elem, nodeType, $"{fieldName}[{i}]", violations);
                }
                i++;
            }
        }
    }

    private static void ValidateListOfRightPadded(object list, string nodeType, string fieldName, List<WhitespaceViolation> violations)
    {
        if (list is System.Collections.IEnumerable enumerable)
        {
            int i = 0;
            foreach (var elem in enumerable)
            {
                if (elem != null)
                {
                    ValidateRightPadded(elem, nodeType, $"{fieldName}[{i}]", violations);
                }
                i++;
            }
        }
    }
}

/// <summary>
/// Describes a whitespace violation found in the AST.
/// </summary>
public record WhitespaceViolation(string NodeType, string FieldName, string SpaceField, string Value)
{
    public override string ToString()
    {
        var truncated = Value.Length > 60 ? Value[..60] + "..." : Value;
        var escaped = truncated.Replace("\n", "\\n").Replace("\r", "\\r").Replace("\t", "\\t");
        return $"{NodeType}.{FieldName} ({SpaceField}): \"{escaped}\"";
    }
}
