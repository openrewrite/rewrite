namespace Rewrite.Java;

/// <summary>
/// Type attribution for LST elements.
/// Matches the Java-side org.openrewrite.java.tree.JavaType hierarchy.
/// Uses classes (not records) to support mutable fields needed for
/// the shell-cache pattern when handling cyclic type references.
/// </summary>
public abstract class JavaType
{
    /// <summary>
    /// Primitive types (int, string, bool, etc.)
    /// </summary>
    public sealed class Primitive : JavaType
    {
        public PrimitiveKind Kind { get; }

        public Primitive(PrimitiveKind kind)
        {
            Kind = kind;
        }

        public string Keyword => Kind switch
        {
            PrimitiveKind.Boolean => "boolean",
            PrimitiveKind.Byte => "byte",
            PrimitiveKind.Char => "char",
            PrimitiveKind.Double => "double",
            PrimitiveKind.Float => "float",
            PrimitiveKind.Int => "int",
            PrimitiveKind.Long => "long",
            PrimitiveKind.Short => "short",
            PrimitiveKind.Void => "void",
            PrimitiveKind.String => "String",
            PrimitiveKind.Null => "null",
            PrimitiveKind.None => "",
            _ => throw new ArgumentException($"Unknown primitive kind: {Kind}")
        };
    }

    /// <summary>
    /// Abstract base for fully-qualified types (Class, Parameterized, Annotation, Unknown).
    /// Matches Java's JavaType.FullyQualified.
    /// </summary>
    public abstract class FullyQualified : JavaType
    {
        public enum FullyQualifiedKind
        {
            Class,
            Enum,
            Interface,
            Annotation,
            Record,
            Value
        }
    }

    /// <summary>
    /// A class, interface, enum, record, or other named type.
    /// Matches Java's JavaType.Class. Mutable fields for shell-cache pattern.
    /// </summary>
    public class Class : FullyQualified
    {
        public long FlagsBitMap { get; set; }
        public FullyQualifiedKind ClassKind { get; set; }
        public string FullyQualifiedName { get; set; } = "";
        public IList<JavaType>? TypeParameters { get; set; }
        public FullyQualified? Supertype { get; set; }
        public FullyQualified? OwningClass { get; set; }
        public IList<FullyQualified>? Annotations { get; set; }
        public IList<FullyQualified>? Interfaces { get; set; }
        public IList<Variable>? Members { get; set; }
        public IList<Method>? Methods { get; set; }

        public Class() { }

        public Class(long flagsBitMap, FullyQualifiedKind classKind, string fullyQualifiedName,
            IList<JavaType>? typeParameters, FullyQualified? supertype, FullyQualified? owningClass,
            IList<FullyQualified>? annotations, IList<FullyQualified>? interfaces,
            IList<Variable>? members, IList<Method>? methods)
        {
            FlagsBitMap = flagsBitMap;
            ClassKind = classKind;
            FullyQualifiedName = fullyQualifiedName;
            TypeParameters = typeParameters;
            Supertype = supertype;
            OwningClass = owningClass;
            Annotations = annotations;
            Interfaces = interfaces;
            Members = members;
            Methods = methods;
        }

        public Class UnsafeSet(long flags, FullyQualifiedKind kind, string fqn,
            IList<JavaType>? typeParameters, FullyQualified? supertype, FullyQualified? owningClass,
            IList<FullyQualified>? annotations, IList<FullyQualified>? interfaces,
            IList<Variable>? members, IList<Method>? methods)
        {
            FlagsBitMap = flags;
            ClassKind = kind;
            FullyQualifiedName = fqn;
            TypeParameters = typeParameters;
            Supertype = supertype;
            OwningClass = owningClass;
            Annotations = annotations;
            Interfaces = interfaces;
            Members = members;
            Methods = methods;
            return this;
        }
    }

    /// <summary>
    /// A generic type instantiation (e.g., List&lt;int&gt;).
    /// </summary>
    public class Parameterized : FullyQualified
    {
        public FullyQualified? Type { get; set; }
        public IList<JavaType>? TypeParameters { get; set; }

        public Parameterized() { }

        public Parameterized(FullyQualified? type, IList<JavaType>? typeParameters)
        {
            Type = type;
            TypeParameters = typeParameters;
        }

        public Parameterized UnsafeSet(FullyQualified? type, IList<JavaType>? typeParameters)
        {
            Type = type;
            TypeParameters = typeParameters;
            return this;
        }
    }

    /// <summary>
    /// A type variable used in generics (e.g., T in List&lt;T&gt;).
    /// </summary>
    public class GenericTypeVariable : JavaType
    {
        public enum VarianceKind
        {
            Invariant,
            Covariant,
            Contravariant
        }

        public string Name { get; set; } = "";
        public VarianceKind Variance { get; set; }
        public IList<JavaType>? Bounds { get; set; }

        public GenericTypeVariable() { }

        public GenericTypeVariable(string name, VarianceKind variance, IList<JavaType>? bounds)
        {
            Name = name;
            Variance = variance;
            Bounds = bounds;
        }

        public GenericTypeVariable UnsafeSet(string name, VarianceKind variance, IList<JavaType>? bounds)
        {
            Name = name;
            Variance = variance;
            Bounds = bounds;
            return this;
        }
    }

    /// <summary>
    /// An array type.
    /// </summary>
    public class Array : JavaType
    {
        public JavaType? ElemType { get; set; }
        public IList<FullyQualified>? Annotations { get; set; }

        public Array() { }

        public Array(JavaType? elemType, IList<FullyQualified>? annotations)
        {
            ElemType = elemType;
            Annotations = annotations;
        }

        public Array UnsafeSet(JavaType? elemType, IList<FullyQualified>? annotations)
        {
            ElemType = elemType;
            Annotations = annotations;
            return this;
        }
    }

    /// <summary>
    /// A method type with declaring type, return type, parameter types, etc.
    /// </summary>
    public class Method : JavaType
    {
        public long FlagsBitMap { get; set; }
        public FullyQualified? DeclaringType { get; set; }
        public string Name { get; set; } = "";
        public JavaType? ReturnType { get; set; }
        public IList<string>? ParameterNames { get; set; }
        public IList<JavaType>? ParameterTypes { get; set; }
        public IList<JavaType>? ThrownExceptions { get; set; }
        public IList<FullyQualified>? Annotations { get; set; }
        public IList<string>? DefaultValue { get; set; }
        public IList<string>? DeclaredFormalTypeNames { get; set; }

        public Method() { }

        public Method(FullyQualified? declaringType, string name, long flagsBitMap,
            JavaType? returnType, IList<string>? parameterNames,
            IList<JavaType>? parameterTypes, IList<JavaType>? thrownExceptions,
            IList<FullyQualified>? annotations, IList<string>? defaultValue,
            IList<string>? declaredFormalTypeNames)
        {
            DeclaringType = declaringType;
            Name = name;
            FlagsBitMap = flagsBitMap;
            ReturnType = returnType;
            ParameterNames = parameterNames;
            ParameterTypes = parameterTypes;
            ThrownExceptions = thrownExceptions;
            Annotations = annotations;
            DefaultValue = defaultValue;
            DeclaredFormalTypeNames = declaredFormalTypeNames;
        }

        public Method UnsafeSet(string name, long flagsBitMap, FullyQualified? declaringType,
            JavaType? returnType, IList<string>? parameterNames,
            IList<JavaType>? parameterTypes, IList<JavaType>? thrownExceptions,
            IList<FullyQualified>? annotations, IList<string>? defaultValue,
            IList<string>? declaredFormalTypeNames)
        {
            Name = name;
            FlagsBitMap = flagsBitMap;
            DeclaringType = declaringType;
            ReturnType = returnType;
            ParameterNames = parameterNames;
            ParameterTypes = parameterTypes;
            ThrownExceptions = thrownExceptions;
            Annotations = annotations;
            DefaultValue = defaultValue;
            DeclaredFormalTypeNames = declaredFormalTypeNames;
            return this;
        }
    }

    /// <summary>
    /// A field, parameter, local variable, or property type.
    /// </summary>
    public class Variable : JavaType
    {
        public long FlagsBitMap { get; set; }
        public string Name { get; set; } = "";
        public JavaType? Owner { get; set; }
        public JavaType? Type { get; set; }
        public IList<FullyQualified>? Annotations { get; set; }

        public Variable() { }

        public Variable(string name, JavaType? owner, JavaType? type, IList<FullyQualified>? annotations)
        {
            Name = name;
            Owner = owner;
            Type = type;
            Annotations = annotations;
        }

        public Variable UnsafeSet(string name, JavaType? owner, JavaType? type,
            IList<FullyQualified>? annotations)
        {
            Name = name;
            Owner = owner;
            Type = type;
            Annotations = annotations;
            return this;
        }
    }

    /// <summary>
    /// An annotation type reference.
    /// </summary>
    public class Annotation : FullyQualified
    {
        public FullyQualified? AnnotationType { get; set; }

        public Annotation() { }

        public Annotation(FullyQualified? type)
        {
            AnnotationType = type;
        }

        public Annotation UnsafeSet(FullyQualified? type)
        {
            AnnotationType = type;
            return this;
        }
    }

    /// <summary>
    /// A multi-catch type (e.g., catch (IOException | SQLException e)).
    /// </summary>
    public class MultiCatch : JavaType
    {
        public IList<JavaType>? ThrowableTypes { get; set; }

        public MultiCatch() { }

        public MultiCatch(IList<JavaType>? throwableTypes)
        {
            ThrowableTypes = throwableTypes;
        }
    }

    /// <summary>
    /// An intersection type (e.g., T extends A &amp; B).
    /// </summary>
    public class Intersection : JavaType
    {
        public IList<JavaType>? Bounds { get; set; }

        public Intersection() { }

        public Intersection(IList<JavaType>? bounds)
        {
            Bounds = bounds;
        }
    }

    /// <summary>
    /// Represents an unresolvable type. Singleton via Instance.
    /// </summary>
    public class Unknown : FullyQualified
    {
        public static readonly Unknown Instance = new();

        private Unknown() { }
    }

    public enum PrimitiveKind
    {
        Boolean,
        Byte,
        Char,
        Double,
        Float,
        Int,
        Long,
        Short,
        Void,
        String,
        Null,
        None
    }
}
