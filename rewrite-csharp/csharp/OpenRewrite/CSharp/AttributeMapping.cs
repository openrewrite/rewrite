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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Maps a Roslyn symbol's attributes onto <see cref="JavaType"/>'s <c>Annotations</c>, mirroring
/// <c>ReloadableJava21TypeMapping</c>'s <c>listAnnotations</c>/<c>annotationType</c>/
/// <c>annotationElementValue</c>.
/// <para>
/// Shared by the two mappers that build <see cref="JavaType"/> from Roslyn symbols —
/// <see cref="CSharpTypeMapping"/> for the file being parsed and <c>AssemblyTypeMapping</c> for
/// the dependency type table — so a control's <c>[TemplatePart]</c>s look the same whether the
/// control is in the analysed source or in a referenced assembly. Each caller supplies its own
/// type and member mappings, so an attribute's type resolves to the same instance every other
/// reference to that type does, and so each mapper keeps its own cycle-breaking cache.
/// </para>
/// </summary>
internal static class AttributeMapping
{
    /// <summary>
    /// Maps a member (property, field or enum constant) of an attribute class.
    /// </summary>
    internal delegate JavaType.Variable MapMember(
        ISymbol symbol, string name, JavaType? owner, JavaType? type);

    /// <summary>
    /// Maps a method (an attribute class' constructor, for positional arguments that name no
    /// property).
    /// </summary>
    internal delegate JavaType.Method? MapMethodSymbol(IMethodSymbol symbol);

    /// <summary>
    /// The attributes applied to a symbol, or null when it carries none, matching
    /// <c>ReloadableJava21TypeMapping.listAnnotations</c>' null-for-empty convention.
    /// </summary>
    internal static IList<JavaType.FullyQualified>? ListAnnotations(
        ISymbol symbol, Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember,
        MapMethodSymbol mapMethod)
    {
        var attributes = symbol.GetAttributes();
        if (attributes.Length == 0) return null;

        List<JavaType.FullyQualified>? annotations = null;
        foreach (var attribute in attributes)
        {
            if (MapAnnotation(attribute, mapType, mapMember, mapMethod) is not { } mapped) continue;
            (annotations ??= new List<JavaType.FullyQualified>(attributes.Length)).Add(mapped);
        }
        return annotations;
    }

    /// <summary>
    /// Maps one attribute application, mirroring <c>ReloadableJava21TypeMapping.annotationType</c>.
    /// </summary>
    /// <remarks>
    /// Java annotation elements are always named, because they are interface methods. C# splits
    /// them into positional constructor arguments and named property/field initializers. Named
    /// arguments resolve to the property or field they name. A positional argument has no name of
    /// its own, so it is resolved to the property or field the constructor parameter feeds, matched
    /// case-insensitively on the parameter name — the near-universal C# convention
    /// (<c>ObsoleteAttribute(string message)</c> -> <c>Message</c>). When no such member exists
    /// (<c>ObsoleteAttribute(..., bool error)</c> feeds <c>IsError</c>) the value's element is the
    /// constructor itself: guessing a differently-named member would misattribute the value, and
    /// Java's <c>ElementValue.getElement()</c> contract is non-null, so an element-less value
    /// cannot cross the RPC boundary. A value whose element resolves to nothing at all (only
    /// possible for code that does not compile) is omitted entirely.
    /// </remarks>
    private static JavaType.Annotation? MapAnnotation(
        AttributeData attribute, Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember,
        MapMethodSymbol mapMethod)
    {
        if (attribute.AttributeClass is not { } attributeClass ||
            attributeClass is IErrorTypeSymbol ||
            mapType(attributeClass) is not JavaType.FullyQualified annotationType)
        {
            return null;
        }

        List<JavaType.Annotation.ElementValue>? values = null;

        if (attribute.AttributeConstructor is { } constructor)
        {
            for (int i = 0; i < attribute.ConstructorArguments.Length && i < constructor.Parameters.Length; i++)
            {
                JavaType? element =
                    MapAnnotationElement(attributeClass, constructor.Parameters[i].Name, mapType, mapMember)
                    ?? (JavaType?)mapMethod(constructor);
                if (element == null) continue;
                (values ??= []).Add(
                    MapAnnotationElementValue(element, attribute.ConstructorArguments[i], mapType, mapMember));
            }
        }

        foreach (var named in attribute.NamedArguments)
        {
            if (MapAnnotationElement(attributeClass, named.Key, mapType, mapMember) is not { } element) continue;
            (values ??= []).Add(MapAnnotationElementValue(element, named.Value, mapType, mapMember));
        }

        return new JavaType.Annotation(annotationType, values);
    }

    /// <summary>
    /// Resolves the annotation element a value belongs to: the property or field of the attribute
    /// class with that name, matched case-insensitively so that a constructor parameter finds the
    /// property it feeds. Base types are searched too, since an attribute may inherit its elements.
    /// </summary>
    private static JavaType.Variable? MapAnnotationElement(
        INamedTypeSymbol attributeClass, string name,
        Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember)
    {
        for (INamedTypeSymbol? type = attributeClass; type != null; type = type.BaseType)
        {
            foreach (var member in type.GetMembers())
            {
                var memberType = member switch
                {
                    IPropertySymbol p => p.Type,
                    IFieldSymbol f => f.Type,
                    _ => null
                };

                if (memberType == null ||
                    !string.Equals(member.Name, name, StringComparison.OrdinalIgnoreCase))
                {
                    continue;
                }

                return mapMember(member, member.Name, mapType(type), mapType(memberType));
            }
        }
        return null;
    }

    private static JavaType.Annotation.ElementValue MapAnnotationElementValue(
        JavaType? element, TypedConstant constant,
        Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember)
    {
        if (constant.Kind == TypedConstantKind.Array)
        {
            if (constant.IsNull)
            {
                return new JavaType.Annotation.ArrayElementValue(element, null, null);
            }

            List<object?>? constantValues = null;
            List<JavaType>? referenceValues = null;
            foreach (var item in constant.Values)
            {
                var (itemConstant, itemReference) = MapAnnotationConstant(item, mapType, mapMember);
                if (itemReference != null)
                {
                    (referenceValues ??= []).Add(itemReference);
                }
                else
                {
                    (constantValues ??= []).Add(itemConstant);
                }
            }
            return new JavaType.Annotation.ArrayElementValue(element, constantValues, referenceValues);
        }

        var (value, reference) = MapAnnotationConstant(constant, mapType, mapMember);
        return new JavaType.Annotation.SingleElementValue(element, value, reference);
    }

    /// <summary>
    /// Maps a single attribute argument, mirroring
    /// <c>ReloadableJava21TypeMapping.annotationElementValue</c>: primitives and strings become
    /// constants, <c>typeof(X)</c> and enum members become type references.
    /// </summary>
    private static (object? Constant, JavaType? Reference) MapAnnotationConstant(
        TypedConstant constant, Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember)
    {
        switch (constant.Kind)
        {
            case TypedConstantKind.Type:
                // typeof(X). A null argument (`typeof` cannot be null, but a `Type`-typed element
                // left at its default can) carries no type to reference.
                return constant.Value is ITypeSymbol typeValue
                    ? (null, mapType(typeValue))
                    : (null, null);

            case TypedConstantKind.Enum:
                // Java maps an enum-valued element to the enum constant's VarSymbol, i.e. a
                // JavaType.Variable. Roslyn only carries the underlying numeric value, so the
                // member has to be recovered from it. A combination of [Flags] members matches no
                // single member; rather than invent one, the underlying value is kept as the
                // constant so nothing is silently mis-named.
                return MapEnumConstant(constant, mapType, mapMember) is { } member
                    ? (null, member)
                    : (constant.Value, null);

            case TypedConstantKind.Primitive:
                return (constant.Value, null);

            case TypedConstantKind.Array:
                // A nested array (jagged attribute arguments are not expressible in C#).
                return (null, JavaType.Unknown.Instance);

            default:
                // TypedConstantKind.Error - the argument did not compile.
                return (null, JavaType.Unknown.Instance);
        }
    }

    private static JavaType.Variable? MapEnumConstant(
        TypedConstant constant, Func<ITypeSymbol?, JavaType?> mapType, MapMember mapMember)
    {
        if (constant.Type is not INamedTypeSymbol { TypeKind: TypeKind.Enum } enumType) return null;

        foreach (var member in enumType.GetMembers())
        {
            if (member is IFieldSymbol { IsConst: true, HasConstantValue: true } field &&
                Equals(field.ConstantValue, constant.Value))
            {
                return mapMember(field, field.Name, mapType(enumType), mapType(enumType));
            }
        }
        return null;
    }
}
