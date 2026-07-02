/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.trait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * The value of an annotation attribute (a JLS §9.7.1 "element value"), in any of the
 * syntactic shapes annotations permit — not only the plain literals covered by {@link Literal}:
 * <ul>
 *   <li>{@link Kind#LITERAL} — {@code @Foo(name = "s")}, {@code @Foo(count = -1)}</li>
 *   <li>{@link Kind#CLASS_LITERAL} — {@code @Foo(clazz = A.class)}, {@code int.class},
 *       {@code String[].class}; Kotlin {@code A::class} and {@code A::class.java}</li>
 *   <li>{@link Kind#ENUM_CONSTANT} — {@code @Retention(RetentionPolicy.RUNTIME)}, in
 *       qualified, fully qualified, or statically imported spelling</li>
 *   <li>{@link Kind#CONSTANT_REFERENCE} — {@code @Foo(name = Constants.NAME)} or a
 *       statically imported {@code NAME}</li>
 *   <li>{@link Kind#NESTED_ANNOTATION} — {@code @Foo(bar = @Bar("x"))}</li>
 *   <li>{@link Kind#ARRAY} — {@code @Foo(exclude = {A.class, B.class})}</li>
 *   <li>{@link Kind#EXPRESSION} — everything else, e.g. {@code @Foo(name = "a" + "b")}</li>
 * </ul>
 * Classification is syntax-first and never throws on missing type attribution; only the
 * distinction between enum constants and other constant references requires attribution
 * ({@link Flag#Enum} on the referenced {@link JavaType.Variable}). Unattributed references
 * degrade to {@link Kind#CONSTANT_REFERENCE} with a {@code null}
 * {@link #getReferencedField()}.
 * <p>
 * The trait is cursor-bearing: {@link #getTree()} / {@link #getCursor()} give recipes the
 * exact expression to edit, and for arrays {@link #getElements()} yields one cursor-bearing
 * value per element. Parentheses are transparent for classification and value access;
 * {@link #getTree()} still returns the original expression.
 * <p>
 * {@link Optional#empty()} from {@link Annotated#getAttributeValue(String)} means only that
 * the attribute is not explicitly present in the source. A present value whose typed
 * accessors return {@code null} means "present but not resolvable that way" — the two
 * conditions the {@code Optional<Literal>} accessors used to conflate.
 *
 * @see JavaType.Annotation.ElementValue
 * @see Annotated#getAttributeValue(String)
 */
@Incubating(since = "8.87.0")
@RequiredArgsConstructor
public class AttributeValue implements Trait<Expression> {

    /**
     * The syntactic shape of an annotation attribute value. Exactly one kind applies to
     * any value; unrecognized or compound shapes classify as {@link #EXPRESSION}.
     */
    public enum Kind {
        LITERAL,
        CLASS_LITERAL,
        ENUM_CONSTANT,
        CONSTANT_REFERENCE,
        NESTED_ANNOTATION,
        ARRAY,
        EXPRESSION
    }

    @Getter
    private final Cursor cursor;

    private final ObjectMapper mapper;

    public Kind getKind() {
        Expression e = unwrapped();
        if (e instanceof J.Literal) {
            // note: `-1` parses as a single J.Literal, not a J.Unary
            return Kind.LITERAL;
        }
        if (e instanceof J.NewArray) {
            return Kind.ARRAY;
        }
        if (e instanceof J.Annotation) {
            return Kind.NESTED_ANNOTATION;
        }
        if (e instanceof J.MemberReference) {
            // Kotlin `A::class`
            return isClassMemberReference((J.MemberReference) e) ? Kind.CLASS_LITERAL : Kind.EXPRESSION;
        }
        if (e instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) e;
            if ("class".equals(fieldAccess.getSimpleName())) {
                return Kind.CLASS_LITERAL;
            }
            if ("java".equals(fieldAccess.getSimpleName()) &&
                fieldAccess.getTarget() instanceof J.MemberReference &&
                isClassMemberReference((J.MemberReference) fieldAccess.getTarget())) {
                // Kotlin `A::class.java`
                return Kind.CLASS_LITERAL;
            }
            return referenceKind(fieldAccess.getName().getFieldType());
        }
        if (e instanceof J.Identifier) {
            // in annotation value position a bare name can only reference a constant
            return referenceKind(((J.Identifier) e).getFieldType());
        }
        return Kind.EXPRESSION;
    }

    private static Kind referenceKind(JavaType.@Nullable Variable fieldType) {
        return fieldType != null && fieldType.hasFlags(Flag.Enum) ?
                Kind.ENUM_CONSTANT :
                Kind.CONSTANT_REFERENCE;
    }

    public boolean isLiteral() {
        return getKind() == Kind.LITERAL;
    }

    public boolean isClassLiteral() {
        return getKind() == Kind.CLASS_LITERAL;
    }

    /**
     * @param fullyQualifiedName The fully qualified name of a reference type.
     * @return {@code true} if this value is a class literal referencing the given type.
     * Requires type attribution; inner class {@code $} and {@code .} spellings are
     * considered equal. Primitive and array class literals never match.
     */
    public boolean isClassLiteral(String fullyQualifiedName) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(getClassValue());
        return fq != null && TypeUtils.fullyQualifiedNamesAreEqual(fq.getFullyQualifiedName(), fullyQualifiedName);
    }

    /**
     * @return {@code true} if this value references an enum constant, in any spelling.
     * Requires type attribution; without it enum constants are indistinguishable from
     * other constant references and classify as {@link Kind#CONSTANT_REFERENCE}.
     */
    public boolean isEnumConstant() {
        return getKind() == Kind.ENUM_CONSTANT;
    }

    /**
     * @param fullyQualifiedEnumName The fully qualified name of the enum type.
     * @param constantName           The name of the enum constant.
     * @return {@code true} if this value is the given enum constant, e.g.
     * {@code isEnumConstant("java.lang.annotation.RetentionPolicy", "RUNTIME")}. Covers
     * the qualified, fully qualified, and statically imported spellings uniformly.
     */
    public boolean isEnumConstant(String fullyQualifiedEnumName, String constantName) {
        JavaType.Variable field = getReferencedField();
        return field != null &&
               field.hasFlags(Flag.Enum) &&
               constantName.equals(field.getName()) &&
               TypeUtils.isOfClassType(field.getOwner(), fullyQualifiedEnumName);
    }

    public boolean isConstantReference() {
        return getKind() == Kind.CONSTANT_REFERENCE;
    }

    public boolean isNestedAnnotation() {
        return getKind() == Kind.NESTED_ANNOTATION;
    }

    /**
     * @return {@code true} if this value is written as an explicit array initializer
     * ({@code J.NewArray}). The brace-less single-element form {@code @Foo(exclude = A.class)}
     * of an array-typed attribute is NOT syntactically an array; use {@link #getElements()}
     * for uniformly normalized element access.
     */
    public boolean isArray() {
        return getKind() == Kind.ARRAY;
    }

    /**
     * @return the name of the annotation attribute this value is assigned to, or
     * {@code "value"} for a positional (implicit) value. Array elements report the name
     * of their containing attribute.
     */
    public String getName() {
        for (Cursor c = cursor.getParentTreeCursor(); !c.isRoot(); c = c.getParentTreeCursor()) {
            Object value = c.getValue();
            if (value instanceof J.Assignment) {
                Expression variable = ((J.Assignment) value).getVariable();
                if (variable instanceof J.Identifier) {
                    return ((J.Identifier) variable).getSimpleName();
                }
                break;
            }
            if (!(value instanceof J.NewArray || value instanceof J.Parentheses)) {
                break;
            }
        }
        return "value";
    }

    /**
     * The values of this attribute normalized to a list: an array initializer yields one
     * {@code AttributeValue} per element, each with its own cursor, so that both reads and
     * surgical edits can address individual elements; every scalar shape yields
     * {@code singletonList(this)}. This mirrors the normalization the JLS applies to
     * array-typed attributes, where the brace-less single-element form
     * {@code @Foo(exclude = A.class)} means {@code {A.class}} — both forms iterate
     * identically here. An empty initializer {@code {}} yields an empty list.
     *
     * @return the individual values of this attribute.
     */
    public List<AttributeValue> getElements() {
        Expression e = unwrapped();
        if (!(e instanceof J.NewArray)) {
            return singletonList(this);
        }
        List<Expression> initializer = ((J.NewArray) e).getInitializer();
        if (initializer == null) {
            return emptyList();
        }
        Cursor arrayCursor = unwrappedCursor();
        List<AttributeValue> elements = new ArrayList<>(initializer.size());
        for (Expression element : initializer) {
            if (!(element instanceof J.Empty)) {
                elements.add(new AttributeValue(new Cursor(arrayCursor, element), mapper));
            }
        }
        return elements;
    }

    /**
     * @return this value as the {@link Literal} trait when it is a {@code J.Literal} or an
     * array composed entirely of literals — exactly the shapes {@link Literal.Matcher}
     * matches. Unlike the {@code Optional<Literal>} accessors on {@link Annotated}, this
     * sees through parentheses. Note this answers "is it {@link Literal}-compatible", which
     * differs from {@link #isLiteral()} for all-literal arrays.
     */
    public Optional<Literal> asLiteral() {
        return new Literal.Matcher().mapper(mapper).get(unwrappedCursor());
    }

    /**
     * @return this value as the {@link Annotated} trait when it is a nested annotation,
     * enabling recursive attribute access.
     */
    public Optional<Annotated> asAnnotated() {
        Cursor c = unwrappedCursor();
        return c.getValue() instanceof J.Annotation ?
                Optional.of(new Annotated(c)) :
                Optional.empty();
    }

    /**
     * @return for a class literal, the referenced type: a {@link JavaType.FullyQualified}
     * for {@code A.class}, a {@link JavaType.Primitive} for {@code int.class}, a
     * {@link JavaType.Array} for {@code String[].class}. {@code null} when this value is
     * not a class literal or type attribution is missing.
     */
    public @Nullable JavaType getClassValue() {
        if (getKind() != Kind.CLASS_LITERAL) {
            return null;
        }
        Expression e = unwrapped();
        if (e instanceof J.MemberReference) {
            // Kotlin `A::class`
            return nullIfUnknown(((J.MemberReference) e).getContaining().getType());
        }
        J.FieldAccess fieldAccess = (J.FieldAccess) e;
        if (fieldAccess.getTarget() instanceof J.MemberReference) {
            // Kotlin `A::class.java`
            return nullIfUnknown(((J.MemberReference) fieldAccess.getTarget()).getContaining().getType());
        }
        JavaType targetType = nullIfUnknown(fieldAccess.getTarget().getType());
        if (targetType != null) {
            return targetType;
        }
        // the class literal's own type is `Class<A>`
        if (fieldAccess.getType() instanceof JavaType.Parameterized &&
            ((JavaType.Parameterized) fieldAccess.getType()).getTypeParameters().size() == 1) {
            return nullIfUnknown(((JavaType.Parameterized) fieldAccess.getType()).getTypeParameters().get(0));
        }
        JavaType.Variable classField = fieldAccess.getName().getFieldType();
        return classField != null ? nullIfUnknown(classField.getOwner()) : null;
    }

    private static @Nullable JavaType nullIfUnknown(@Nullable JavaType type) {
        return type instanceof JavaType.Unknown ? null : type;
    }

    /**
     * @return the static field referenced by this value when it is an enum constant or a
     * constant reference: the variable's {@code owner} is the declaring type and its
     * {@code name} the constant's name — identical across the qualified, fully qualified,
     * and statically imported spellings. Distinguish enums via {@link #isEnumConstant()}.
     * {@code null} when this value is not a reference or attribution is missing; class
     * literals return {@code null} here (their synthetic {@code Variable{name="class"}}
     * is deliberately hidden — use {@link #getClassValue()}).
     */
    public JavaType.@Nullable Variable getReferencedField() {
        Kind kind = getKind();
        if (kind != Kind.ENUM_CONSTANT && kind != Kind.CONSTANT_REFERENCE) {
            return null;
        }
        Expression e = unwrapped();
        if (e instanceof J.FieldAccess) {
            return ((J.FieldAccess) e).getName().getFieldType();
        }
        if (e instanceof J.Identifier) {
            return ((J.Identifier) e).getFieldType();
        }
        return null;
    }

    /**
     * The compile-time constant represented by this value, if one can be determined:
     * <ul>
     *   <li>a {@link J.Literal}'s value directly;</li>
     *   <li>for constant references and constant expressions ({@code Constants.NAME},
     *       a statically imported {@code NAME}, {@code "a" + "b"}), the constant the
     *       compiler folded into the {@link JavaType.Annotation.ElementValue}s on the
     *       annotated declaration's type attribution — best-effort, see below.</li>
     * </ul>
     * Enum constants, class literals, nested annotations, and arrays are references or
     * containers, not constants — use {@link #getReferencedField()},
     * {@link #getClassValue()}, {@link #asAnnotated()}, or {@link #getElements()} for those.
     * <p>
     * The constant fold is only available where the parser records
     * {@link JavaType.Annotation} element values on the annotated declaration: sources
     * attributed by javac (including constants from binary dependencies). It is
     * unavailable — and this method returns {@code null} — for Groovy sources and
     * reflection-mapped types, for annotations in positions other than variable, method,
     * and class declarations, and for annotations whose values cannot be unambiguously
     * located on the declaration (e.g. repeated annotations, which javac stores under
     * their container type).
     *
     * @return the constant value, or {@code null} when this value does not represent a
     * determinable constant.
     */
    public @Nullable Object getConstantValue() {
        switch (getKind()) {
            case LITERAL:
                return ((J.Literal) unwrapped()).getValue();
            case CONSTANT_REFERENCE:
            case EXPRESSION:
                return foldedConstantValue();
            default:
                return null;
        }
    }

    /**
     * Resolves the compiler's constant fold for this value from the annotated
     * declaration's type attribution. Element values are not carried by
     * {@code J.Annotation#getType()} at the use site; they live on the annotated
     * element's {@link JavaType.Variable}/{@link JavaType.Method}/{@link JavaType.FullyQualified}.
     */
    private @Nullable Object foldedConstantValue() {
        // walk up to the enclosing annotation, recording the attribute name and the
        // position of this value inside an array initializer
        String attributeName = null;
        int index = -1;
        Cursor annotationCursor = null;
        Tree child = cursor.getValue();
        for (Cursor c = cursor.getParentTreeCursor(); !c.isRoot(); c = c.getParentTreeCursor()) {
            Object value = c.getValue();
            if (value instanceof J.Annotation) {
                annotationCursor = c;
                break;
            }
            if (value instanceof J.Assignment) {
                Expression variable = ((J.Assignment) value).getVariable();
                if (!(variable instanceof J.Identifier)) {
                    return null;
                }
                attributeName = ((J.Identifier) variable).getSimpleName();
            } else if (value instanceof J.NewArray) {
                if (index != -1) {
                    // annotation values cannot contain nested array initializers
                    return null;
                }
                List<Expression> initializer = ((J.NewArray) value).getInitializer();
                if (initializer == null) {
                    return null;
                }
                int i = 0;
                int found = -1;
                for (Expression element : initializer) {
                    if (element == child) {
                        found = i;
                        break;
                    }
                    if (!(element instanceof J.Empty)) {
                        i++;
                    }
                }
                if (found == -1) {
                    return null;
                }
                index = found;
            } else if (!(value instanceof J.Parentheses)) {
                return null;
            }
            child = (Tree) value;
        }
        if (annotationCursor == null) {
            return null;
        }
        J.Annotation annotation = annotationCursor.getValue();
        JavaType.FullyQualified annotationType = TypeUtils.asFullyQualified(annotation.getType());
        if (annotationType == null) {
            return null;
        }

        List<JavaType.FullyQualified> declaredAnnotations = declaredAnnotations(annotationCursor.getParentTreeCursor().getValue());
        if (declaredAnnotations == null) {
            return null;
        }
        JavaType.Annotation match = null;
        for (JavaType.FullyQualified declared : declaredAnnotations) {
            if (declared instanceof JavaType.Annotation &&
                TypeUtils.fullyQualifiedNamesAreEqual(declared.getFullyQualifiedName(), annotationType.getFullyQualifiedName())) {
                if (match != null) {
                    // ambiguous; never risk returning another occurrence's value
                    return null;
                }
                match = (JavaType.Annotation) declared;
            }
        }
        if (match == null) {
            return null;
        }

        String attribute = attributeName == null ? "value" : attributeName;
        for (JavaType.Annotation.ElementValue elementValue : match.getValues()) {
            JavaType element = elementValue.getElement();
            if (element instanceof JavaType.Method && attribute.equals(((JavaType.Method) element).getName())) {
                if (elementValue instanceof JavaType.Annotation.SingleElementValue) {
                    return index < 0 ?
                            ((JavaType.Annotation.SingleElementValue) elementValue).getConstantValue() :
                            null;
                }
                if (elementValue instanceof JavaType.Annotation.ArrayElementValue) {
                    Object[] constantValues = ((JavaType.Annotation.ArrayElementValue) elementValue).getConstantValues();
                    if (constantValues == null) {
                        return null;
                    }
                    if (index < 0) {
                        // attribution normalizes the brace-less single-element form to an array
                        return constantValues.length == 1 ? constantValues[0] : null;
                    }
                    return index < constantValues.length ? constantValues[index] : null;
                }
                return null;
            }
        }
        return null;
    }

    private static @Nullable List<JavaType.FullyQualified> declaredAnnotations(Object declaration) {
        if (declaration instanceof J.VariableDeclarations) {
            for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) declaration).getVariables()) {
                if (variable.getVariableType() != null) {
                    return variable.getVariableType().getAnnotations();
                }
            }
        } else if (declaration instanceof J.MethodDeclaration) {
            JavaType.Method methodType = ((J.MethodDeclaration) declaration).getMethodType();
            return methodType != null ? methodType.getAnnotations() : null;
        } else if (declaration instanceof J.ClassDeclaration) {
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(((J.ClassDeclaration) declaration).getType());
            return type != null ? type.getAnnotations() : null;
        }
        return null;
    }

    /**
     * {@link #getConstantValue()} coerced with Jackson, using the same mapper and
     * scalar-to-collection normalization as {@link Literal#getValue(Class)}.
     *
     * @return the coerced constant value, or {@code null} when unresolvable or not coercible.
     */
    public <T> @Nullable T getValue(Class<T> type) {
        return convert(getConstantValue(), mapper.constructType(type));
    }

    /**
     * @see #getValue(Class)
     */
    public <T> @Nullable T getValue(TypeReference<T> type) {
        return convert(getConstantValue(), mapper.constructType(type));
    }

    private <T> @Nullable T convert(@Nullable Object value, com.fasterxml.jackson.databind.JavaType type) {
        if (value == null) {
            return null;
        }
        Object normalized = value;
        if (type.isCollectionLikeType() && !(value instanceof Collection)) {
            normalized = singletonList(value);
        }
        try {
            return mapper.convertValue(normalized, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Expression unwrapped() {
        return unwrappedCursor().getValue();
    }

    private Cursor unwrappedCursor() {
        Cursor c = cursor;
        while (c.getValue() instanceof J.Parentheses) {
            J inner = ((J.Parentheses<?>) c.getValue()).getTree();
            if (!(inner instanceof Expression)) {
                break;
            }
            c = new Cursor(c, inner);
        }
        return c;
    }

    private static boolean isClassMemberReference(J.MemberReference memberReference) {
        return "class".equals(memberReference.getReference().getSimpleName());
    }

    /**
     * Matches expressions in annotation attribute value position: a positional argument of
     * a {@link J.Annotation}, or the right-hand side of a named argument's
     * {@link J.Assignment}. Attribute name identifiers, {@link J.Empty} (as in
     * {@code @Foo()}), and array initializer ELEMENTS are not matched standalone —
     * elements are reachable through {@link AttributeValue#getElements()}.
     */
    public static class Matcher extends SimpleTraitMatcher<AttributeValue> {
        private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

        private ObjectMapper mapper = DEFAULT_MAPPER;

        /**
         * @param mapper A customized mapper, which should be rare. See {@link Literal.Matcher#mapper(ObjectMapper)}.
         * @return This matcher with a customized mapper set.
         */
        @SuppressWarnings("unused")
        public Matcher mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        @Override
        protected @Nullable AttributeValue test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Expression) ||
                value instanceof J.Assignment ||
                value instanceof J.Empty) {
                return null;
            }
            Cursor parentCursor = cursor.getParentTreeCursor();
            Object parent = parentCursor.getValue();
            if (parent instanceof J.Annotation) {
                List<Expression> arguments = ((J.Annotation) parent).getArguments();
                //noinspection SuspiciousMethodCalls
                return arguments != null && arguments.contains(value) ?
                        new AttributeValue(cursor, mapper) :
                        null;
            }
            if (parent instanceof J.Assignment && ((J.Assignment) parent).getAssignment() == value) {
                Cursor grandparentCursor = parentCursor.getParentTreeCursor();
                if (grandparentCursor.getValue() instanceof J.Annotation) {
                    List<Expression> arguments = ((J.Annotation) grandparentCursor.getValue()).getArguments();
                    //noinspection SuspiciousMethodCalls
                    return arguments != null && arguments.contains(parent) ?
                            new AttributeValue(cursor, mapper) :
                            null;
                }
            }
            return null;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AttributeValue, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public @Nullable J preVisit(J tree, P p) {
                    if (tree instanceof Expression) {
                        AttributeValue value = test(getCursor());
                        if (value != null) {
                            return (J) visitor.visit(value, p);
                        }
                    }
                    return tree;
                }
            };
        }
    }
}
