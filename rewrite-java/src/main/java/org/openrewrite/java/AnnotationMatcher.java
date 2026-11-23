/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.ThrowingErrorListener;
import org.openrewrite.java.internal.grammar.AnnotationSignatureLexer;
import org.openrewrite.java.internal.grammar.AnnotationSignatureParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This matcher will find all annotations matching the annotation pattern
 * <p>
 * The annotation pattern, expressed as a method pattern, is used to find matching annotations. The format of the
 * expression is as follows:
 * <P><P><B>
 * {@literal @}#annotationClass#(#parameterName#=#parameterValue#, #parameterName#=#parameterValue#...)
 * </B><P>
 * <li>The annotationClass must be fully qualified.</li>
 * <li>The parameter name/value pairs can be in any order</li>
 *
 * <P><PRE>
 * EXAMPLES:
 * <p>
 * {@literal @}java.lang.SuppressWarnings                                 - Matches java.lang.SuppressWarnings with no parameters.
 * {@literal @}myhttp.Get(serviceName="payments", path="recentPayments")  - Matches references to myhttp.Get where the parameters are also matched.
 * {@literal @}myhttp.Get(path="recentPayments", serviceName="payments")  - Exactly the same results from the previous example, order of parameters does not matter.
 * {@literal @}java.lang.SuppressWarnings("deprecation")                  - Matches java.langSuppressWarning with a parameter "deprecation", values in array initializer match as well.
 * {@literal @}org.junit.runner.RunWith(org.junit.runners.JUnit4.class)   - Matches JUnit4's @RunWith(JUnit4.class)
 * </PRE>
 */
public class AnnotationMatcher {
    private final AnnotationSignatureParser.AnnotationContext match;
    private final TypeNameMatcher matcher;
    private final boolean matchMetaAnnotations;

    public AnnotationMatcher(String signature, @Nullable Boolean matchesMetaAnnotations) {
        ANTLRErrorListener errorListener = new ThrowingErrorListener(signature);
        AnnotationSignatureLexer lexer = new AnnotationSignatureLexer(CharStreams.fromString(signature));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AnnotationSignatureParser parser = new AnnotationSignatureParser(new CommonTokenStream(lexer));
        parser.addErrorListener(errorListener);
        this.match = parser.annotation();
        this.matcher = TypeNameMatcher.fromPattern(match.annotationName().getText());
        this.matchMetaAnnotations = Boolean.TRUE.equals(matchesMetaAnnotations);
    }

    public AnnotationMatcher(Class<?> annotationType) {
        this("@" + annotationType.getName());
        if (!annotationType.isAnnotation()) {
            throw new IllegalArgumentException(annotationType.getName() + " is not an annotation.");
        }
    }

    public AnnotationMatcher(String signature) {
        this(signature, false);
    }

    /**
     * @return The annotation type pattern this matcher was constructed with, without the leading
     * {@code @} or any parameter constraints (e.g. {@code java.lang.SuppressWarnings}). Suitable for
     * use as a {@link org.openrewrite.java.search.UsesType} pattern.
     */
    public String getAnnotationName() {
        return match.annotationName().getText();
    }

    public boolean matches(J.Annotation annotation) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
        if (type != null && matcher.matches(type.getFullyQualifiedName())) {
            return matchesSingleParameter(annotation) && matchesNamedParameters(annotation);
        }
        return matchesMetaAnnotation(type, this::matchesElementValues, null);
    }

    public boolean matchesAnnotationOrMetaAnnotation(JavaType.@Nullable FullyQualified fqn) {
        return fqn != null && (matcher.matches(fqn.getFullyQualifiedName()) ||
                               matchesMetaAnnotation(fqn, annotation -> true, null));
    }

    private boolean matchesMetaAnnotation(JavaType.@Nullable FullyQualified fqn,
                                          Predicate<JavaType.FullyQualified> matchesValues,
                                          @Nullable Set<String> seenAnnotations) {
        if (fqn == null || !matchMetaAnnotations) {
            return false;
        }
        for (JavaType.FullyQualified annotation : fqn.getAnnotations()) {
            //noinspection ConstantValue
            if (annotation == null) {
                // Workaround for parsing bug that caused these annotations
                // to sometimes be null.
                continue;
            }
            if (matcher.matches(annotation.getFullyQualifiedName())) {
                if (matchesValues.test(annotation)) {
                    return true;
                }
                continue;
            }
            if (seenAnnotations == null) {
                seenAnnotations = new HashSet<>();
            }
            if (seenAnnotations.add(annotation.getFullyQualifiedName()) &&
                matchesMetaAnnotation(annotation, matchesValues, seenAnnotations)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesElementValues(JavaType.FullyQualified annotation) {
        if (match.elementValue() != null) {
            return elementValueMatches(annotation, "value", match.elementValue().getText());
        }
        AnnotationSignatureParser.ElementValuePairsContext pairs = match.elementValuePairs();
        if (pairs != null && pairs.elementValuePair() != null) {
            for (AnnotationSignatureParser.ElementValuePairContext pair : pairs.elementValuePair()) {
                if (!elementValueMatches(annotation, pair.Identifier().getText(), pair.elementValue().getText())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean elementValueMatches(JavaType.FullyQualified annotation, String name, String matchText) {
        if (!(annotation instanceof JavaType.Annotation)) {
            return false;
        }
        for (JavaType.Annotation.ElementValue elementValue : ((JavaType.Annotation) annotation).getValues()) {
            if (elementValue.getElement() instanceof JavaType.Method &&
                name.equals(((JavaType.Method) elementValue.getElement()).getName())) {
                Object value = elementValue.getValue();
                return value instanceof List ?
                        ((List<?>) value).stream().anyMatch(v -> valueMatches(v, matchText)) :
                        valueMatches(value, matchText);
            }
        }
        return false;
    }

    private static boolean valueMatches(@Nullable Object value, String matchText) {
        if (value instanceof JavaType.Variable) {
            JavaType.Variable variable = (JavaType.Variable) value;
            JavaType.FullyQualified owner = TypeUtils.asFullyQualified(variable.getOwner());
            return owner != null && matchText.equals(owner.getFullyQualifiedName() + "." + variable.getName());
        } else if (value instanceof JavaType.FullyQualified) {
            return matchText.endsWith(".class") && TypeUtils.fullyQualifiedNamesAreEqual(
                    matchText.substring(0, matchText.length() - 6),
                    ((JavaType.FullyQualified) value).getFullyQualifiedName());
        }
        // Serialized LSTs may hold any constant as its string form, so compare text, not Java type
        String text = String.valueOf(value);
        return matchText.equals(text) || matchText.equals('"' + text + '"') || matchText.equals("'" + text + "'");
    }

    private boolean matchesNamedParameters(J.Annotation annotation) {
        AnnotationSignatureParser.ElementValuePairsContext pairs = match.elementValuePairs();
        if (pairs == null || pairs.elementValuePair() == null) {
            return true;
        }

        if (annotation.getArguments() == null) {
            return false;
        }

        for (AnnotationSignatureParser.ElementValuePairContext elementValuePair : pairs.elementValuePair()) {
            String argumentName = elementValuePair.Identifier().getText();
            String matchText = elementValuePair.elementValue().getText();
            if (annotation.getArguments().stream().noneMatch(arg -> argumentValueMatches(argumentName, arg, matchText))) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesSingleParameter(J.Annotation annotation) {
        if (match.elementValue() == null) {
            return true;
        }

        return annotation.getArguments() == null || annotation.getArguments().stream()
                .findAny()
                .map(arg -> argumentValueMatches("value", arg, match.elementValue().getText()))
                .orElse(true);
    }

    private boolean argumentValueMatches(String matchOnArgumentName, Expression arg, String matchText) {
        if ("value".equals(matchOnArgumentName)) {
            if (arg instanceof J.Literal) {
                String valueSource = ((J.Literal) arg).getValueSource();
                return valueSource != null && valueSource.equals(matchText);
            }
            if (arg instanceof J.FieldAccess) {
                J.FieldAccess fa = (J.FieldAccess) arg;
                if ("class".equals(fa.getSimpleName()) && matchText.endsWith(".class")) {
                    JavaType argType = fa.getTarget().getType();
                    if (argType instanceof JavaType.FullyQualified) {
                        String queryTypeFqn = JavaType.ShallowClass.build(matchText.substring(0, matchText.length() - 6)).getFullyQualifiedName();
                        String targetTypeFqn = ((JavaType.FullyQualified) argType).getFullyQualifiedName();
                        return TypeUtils.fullyQualifiedNamesAreEqual(queryTypeFqn, targetTypeFqn);
                    }
                    return false;
                }

                // Handle Kotlin's ::class.java syntax
                if ("java".equals(fa.getSimpleName()) && fa.getTarget() instanceof J.MemberReference) {
                    J.MemberReference mr = (J.MemberReference) fa.getTarget();
                    if ("class".equals(mr.getReference().getSimpleName()) && (matchText.endsWith(".class") || matchText.endsWith("::class.java"))) {
                        JavaType argType = mr.getContaining().getType();
                        if (argType instanceof JavaType.FullyQualified) {
                            String queryTypeFqn;
                            if (matchText.endsWith("::class.java")) {
                                queryTypeFqn = JavaType.ShallowClass.build(matchText.substring(0, matchText.length() - 12)).getFullyQualifiedName();
                            } else {
                                queryTypeFqn = JavaType.ShallowClass.build(matchText.substring(0, matchText.length() - 6)).getFullyQualifiedName();
                            }
                            String targetTypeFqn = ((JavaType.FullyQualified) argType).getFullyQualifiedName();
                            return TypeUtils.fullyQualifiedNamesAreEqual(queryTypeFqn, targetTypeFqn);
                        }
                        return false;
                    }
                }

                JavaType.Variable varType = fa.getName().getFieldType();
                if (varType != null) {
                    JavaType.FullyQualified owner = TypeUtils.asFullyQualified(varType.getOwner());
                    if (owner != null && matchText.equals(owner.getFullyQualifiedName() + "." + varType.getName())) {
                        return true;
                    }
                }
            }

            // Handle Kotlin's ::class syntax
            if (arg instanceof J.MemberReference) {
                J.MemberReference mr = (J.MemberReference) arg;
                if ("class".equals(mr.getReference().getSimpleName()) && (matchText.endsWith(".class") || matchText.endsWith("::class"))) {
                    JavaType argType = mr.getContaining().getType();
                    if (argType instanceof JavaType.FullyQualified) {
                        // Handle both Java's ".class" and Kotlin's "::class" syntax
                        String queryTypeFqn;
                        if (matchText.endsWith("::class")) {
                            queryTypeFqn = JavaType.ShallowClass.build(matchText.substring(0, matchText.length() - 7)).getFullyQualifiedName();
                        } else {
                            queryTypeFqn = JavaType.ShallowClass.build(matchText.substring(0, matchText.length() - 6)).getFullyQualifiedName();
                        }
                        String targetTypeFqn = ((JavaType.FullyQualified) argType).getFullyQualifiedName();
                        return TypeUtils.fullyQualifiedNamesAreEqual(queryTypeFqn, targetTypeFqn);
                    }
                    return false;
                }
            }
            if (arg instanceof J.NewArray) {
                J.NewArray na = (J.NewArray) arg;
                if (na.getInitializer() == null) {
                    return false;
                }
                // recursively check each initializer of the array initializer
                for (Expression expression : na.getInitializer()) {
                    if (argumentValueMatches(matchOnArgumentName, expression, matchText)) {
                        return true;
                    }
                }
                return false;
            }
        }

        if (!(arg instanceof J.Assignment)) {
            return false;
        }

        J.Assignment assignment = (J.Assignment) arg;
        if (!assignment.getVariable().printTrimmed(new JavaPrinter<>()).equals(matchOnArgumentName)) {
            return false;
        }

        // we've already matched the argument name, so recursively we just check the value matches match text.
        return argumentValueMatches("value", assignment.getAssignment(), matchText);
    }
}
