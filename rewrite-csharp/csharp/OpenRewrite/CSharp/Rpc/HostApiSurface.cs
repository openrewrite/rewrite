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
using System.Collections.Immutable;
using System.Reflection.Metadata;
using System.Reflection.Metadata.Ecma335;
using System.Reflection.PortableExecutable;

namespace OpenRewrite.CSharp.Rpc;

internal sealed class HostApiSurface
{
    private static readonly HashSet<string> ObjectMemberNames =
        ["ToString", "Equals", "GetHashCode", "GetType", "MemberwiseClone", "Finalize"];

    private readonly string _assemblyName;
    private readonly Dictionary<string, HostType> _types;

    private HostApiSurface(string assemblyName, Dictionary<string, HostType> types)
    {
        _assemblyName = assemblyName;
        _types = types;
    }

    public static HostApiSurface Read(string hostAssemblyPath)
    {
        using var pe = new PEReader(File.OpenRead(hostAssemblyPath));
        var reader = pe.GetMetadataReader();
        var types = new Dictionary<string, HostType>(StringComparer.Ordinal);
        foreach (var handle in reader.TypeDefinitions)
        {
            var definition = reader.GetTypeDefinition(handle);
            var type = new HostType(BaseTypeName(reader, definition.BaseType));
            foreach (var methodHandle in definition.GetMethods())
            {
                var method = reader.GetMethodDefinition(methodHandle);
                var name = reader.GetString(method.Name);
                type.MemberNames.Add(name);
                type.MemberKeys.Add(MethodKey(name, method.DecodeSignature(TypeNameProvider.Instance, null)));
            }
            foreach (var fieldHandle in definition.GetFields())
            {
                var field = reader.GetFieldDefinition(fieldHandle);
                var name = reader.GetString(field.Name);
                type.MemberNames.Add(name);
                type.MemberKeys.Add(FieldKey(name, field.DecodeSignature(TypeNameProvider.Instance, null)));
            }
            types[TypeNameProvider.FullName(reader, handle)] = type;
        }
        return new HostApiSurface(reader.GetString(reader.GetAssemblyDefinition().Name), types);
    }

    public IReadOnlyList<string> FindMissing(string assemblyPath)
    {
        using var pe = new PEReader(File.OpenRead(assemblyPath));
        var reader = pe.GetMetadataReader();
        var missing = new SortedSet<string>(StringComparer.Ordinal);

        foreach (var handle in reader.TypeReferences)
        {
            if (IsHostScoped(reader, handle))
            {
                var name = TypeNameProvider.FullName(reader, handle);
                if (!_types.ContainsKey(name))
                {
                    missing.Add(name);
                }
            }
        }

        foreach (var handle in reader.MemberReferences)
        {
            var reference = reader.GetMemberReference(handle);
            var typeName = HostParentTypeName(reader, reference.Parent);
            if (typeName == null || !_types.TryGetValue(typeName, out var type))
            {
                continue;
            }

            var name = reader.GetString(reference.Name);
            string key;
            string display;
            if (reference.GetKind() == MemberReferenceKind.Method)
            {
                var signature = reference.DecodeMethodSignature(TypeNameProvider.Instance, null);
                key = MethodKey(name, signature);
                display = $"{typeName}.{name}({string.Join(", ", signature.ParameterTypes)})";
            }
            else
            {
                key = FieldKey(name, reference.DecodeFieldSignature(TypeNameProvider.Instance, null));
                display = $"{typeName}.{name}";
            }

            if (!type.MemberKeys.Contains(key) && (name == ".ctor" || !InheritsMemberNamed(type, name)))
            {
                missing.Add(display);
            }
        }

        return missing.ToList();
    }

    private bool InheritsMemberNamed(HostType type, string name)
    {
        var current = type;
        while (current.BaseTypeName is { } baseTypeName)
        {
            if (!_types.TryGetValue(baseTypeName, out var baseType))
            {
                return baseTypeName is not ("System.Object" or "System.ValueType") || ObjectMemberNames.Contains(name);
            }
            if (baseType.MemberNames.Contains(name))
            {
                return true;
            }
            current = baseType;
        }
        return false;
    }

    private bool IsHostScoped(MetadataReader reader, TypeReferenceHandle handle)
    {
        var scope = reader.GetTypeReference(handle).ResolutionScope;
        while (scope.Kind == HandleKind.TypeReference)
        {
            scope = reader.GetTypeReference((TypeReferenceHandle)scope).ResolutionScope;
        }
        return scope.Kind == HandleKind.AssemblyReference &&
               reader.GetString(reader.GetAssemblyReference((AssemblyReferenceHandle)scope).Name) == _assemblyName;
    }

    private string? HostParentTypeName(MetadataReader reader, EntityHandle parent)
    {
        parent = GenericTypeDefinition(reader, parent);
        return parent.Kind == HandleKind.TypeReference && IsHostScoped(reader, (TypeReferenceHandle)parent)
            ? TypeNameProvider.FullName(reader, (TypeReferenceHandle)parent)
            : null;
    }

    private static string? BaseTypeName(MetadataReader reader, EntityHandle baseType)
    {
        if (baseType.IsNil)
        {
            return null;
        }
        baseType = GenericTypeDefinition(reader, baseType);
        return baseType.Kind switch
        {
            HandleKind.TypeDefinition => TypeNameProvider.FullName(reader, (TypeDefinitionHandle)baseType),
            HandleKind.TypeReference => TypeNameProvider.FullName(reader, (TypeReferenceHandle)baseType),
            _ => null
        };
    }

    private static EntityHandle GenericTypeDefinition(MetadataReader reader, EntityHandle type)
    {
        if (type.Kind != HandleKind.TypeSpecification)
        {
            return type;
        }
        var blob = reader.GetBlobReader(reader.GetTypeSpecification((TypeSpecificationHandle)type).Signature);
        if (blob.ReadSignatureTypeCode() != SignatureTypeCode.GenericTypeInstance)
        {
            return type;
        }
        blob.ReadSignatureTypeCode();
        return blob.ReadTypeHandle();
    }

    private static string MethodKey(string name, MethodSignature<string> signature) =>
        $"{(signature.Header.IsInstance ? "instance " : "")}{signature.ReturnType} {name}`{signature.GenericParameterCount}" +
        $"({string.Join(",", signature.ParameterTypes)})";

    private static string FieldKey(string name, string type) => $"{type} {name}";

    private sealed class HostType(string? baseTypeName)
    {
        public string? BaseTypeName { get; } = baseTypeName;
        public HashSet<string> MemberNames { get; } = new(StringComparer.Ordinal);
        public HashSet<string> MemberKeys { get; } = new(StringComparer.Ordinal);
    }

    private sealed class TypeNameProvider : ISignatureTypeProvider<string, object?>
    {
        public static readonly TypeNameProvider Instance = new();

        public static string FullName(MetadataReader reader, TypeDefinitionHandle handle)
        {
            var definition = reader.GetTypeDefinition(handle);
            var name = reader.GetString(definition.Name);
            var declaringType = definition.GetDeclaringType();
            if (!declaringType.IsNil)
            {
                return $"{FullName(reader, declaringType)}+{name}";
            }
            var ns = reader.GetString(definition.Namespace);
            return ns.Length == 0 ? name : $"{ns}.{name}";
        }

        public static string FullName(MetadataReader reader, TypeReferenceHandle handle)
        {
            var reference = reader.GetTypeReference(handle);
            var name = reader.GetString(reference.Name);
            if (reference.ResolutionScope.Kind == HandleKind.TypeReference)
            {
                return $"{FullName(reader, (TypeReferenceHandle)reference.ResolutionScope)}+{name}";
            }
            var ns = reader.GetString(reference.Namespace);
            return ns.Length == 0 ? name : $"{ns}.{name}";
        }

        public string GetPrimitiveType(PrimitiveTypeCode typeCode) => $"System.{typeCode}";

        public string GetTypeFromDefinition(MetadataReader reader, TypeDefinitionHandle handle, byte rawTypeKind) =>
            FullName(reader, handle);

        public string GetTypeFromReference(MetadataReader reader, TypeReferenceHandle handle, byte rawTypeKind) =>
            FullName(reader, handle);

        public string GetTypeFromSpecification(MetadataReader reader, object? genericContext,
            TypeSpecificationHandle handle, byte rawTypeKind) =>
            reader.GetTypeSpecification(handle).DecodeSignature(this, genericContext);

        public string GetSZArrayType(string elementType) => $"{elementType}[]";

        public string GetArrayType(string elementType, ArrayShape shape) =>
            $"{elementType}[{new string(',', shape.Rank - 1)}]";

        public string GetByReferenceType(string elementType) => $"{elementType}&";

        public string GetPointerType(string elementType) => $"{elementType}*";

        public string GetPinnedType(string elementType) => elementType;

        public string GetModifiedType(string modifier, string unmodifiedType, bool isRequired) => unmodifiedType;

        public string GetGenericInstantiation(string genericType, ImmutableArray<string> typeArguments) =>
            $"{genericType}<{string.Join(",", typeArguments)}>";

        public string GetGenericTypeParameter(object? genericContext, int index) => $"!{index}";

        public string GetGenericMethodParameter(object? genericContext, int index) => $"!!{index}";

        public string GetFunctionPointerType(MethodSignature<string> signature) =>
            $"method {signature.ReturnType}({string.Join(",", signature.ParameterTypes)})";
    }
}
