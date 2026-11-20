/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

@Incubating(since = "8.63.0")
@EqualsAndHashCode(callSuper = false)
@Value
public class InlineMethodCalls extends Recipe {
    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern BARE_METHOD_CALL = Pattern.compile("^" + IDENTIFIER + "\\(");
    /// Shared by substitution and argument collection so both agree on which placeholders exist. Literals and member
    /// selects like `other.name` are consumed without a group, so a parameter's name inside them is left alone.
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile(
            "\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'" +
            "|#\\{(?:(?<declared>" + IDENTIFIER + "):any\\([^}]*\\))?[^}]*}" +
            "|\\.\\s*" + IDENTIFIER +
            "|(?<reference>" + IDENTIFIER + ")");

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "com.google.common.base.Preconditions checkNotNull(..)")
    String methodPattern;

    @Option(displayName = "Replacement template",
            description = "The replacement template for the method invocation. Parameters can be referenced using their names from the original method.",
            example = "java.util.Objects.requireNonNull(#{p0})")
    String replacement;

    @Option(displayName = "Imports",
            description = "List of regular imports to add when the replacement is made.",
            required = false,
            example = "[\"java.util.Objects\"]")
    @Nullable
    Set<String> imports;

    @Option(displayName = "Static imports",
            description = "List of static imports to add when the replacement is made.",
            required = false,
            example = "[\"java.util.Collections.emptyList\"]")
    @Nullable
    Set<String> staticImports;

    @Option(displayName = "Classpath from resources",
            description = "List of paths to JAR files on the classpath for parsing the replacement template.",
            required = false,
            example = "[\"guava-33.4.8-jre\"]")
    @Nullable
    Set<String> classpathFromResources;

    String displayName = "Inline method calls";

    String description = "Inline method calls using a template replacement pattern. " +
                "Supports both method invocations and constructor calls, with optional imports.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, true);
        return Preconditions.check(new UsesMethod<>(methodPattern), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (matcher.matches(method)) {
                    return replaceMethodCall(method, ctx);
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                if (matcher.matches(newClass)) {
                    return replaceMethodCall(newClass, ctx);
                }
                return super.visitNewClass(newClass, ctx);
            }

            private J replaceMethodCall(MethodCall methodCall, ExecutionContext ctx) {
                Set<String> importsSet = imports != null ? imports : emptySet();
                Set<String> staticImportsSet = staticImports != null ? staticImports : emptySet();
                removeAndAddImports(methodCall, importsSet, staticImportsSet);
                J applied = applyJavaTemplate(methodCall, getCursor(), importsSet, staticImportsSet, ctx);
                return avoidMethodSelfReferences(methodCall, keepAttributedClazz(methodCall, applied));
            }

            /// The template can not attribute the class it constructs without a classpath that holds it, which
            /// would otherwise leave the new call untyped and its import looking unused.
            private J keepAttributedClazz(MethodCall original, J applied) {
                if (isConstructorToConstructor(original) && applied instanceof J.NewClass) {
                    TypeTree originalClazz = ((J.NewClass) original).getClazz();
                    TypeTree appliedClazz = ((J.NewClass) applied).getClazz();
                    if (originalClazz != null && appliedClazz != null) {
                        return ((J.NewClass) applied).withClazz(originalClazz.withPrefix(appliedClazz.getPrefix()));
                    }
                }
                return applied;
            }

            private boolean isConstructorToConstructor(MethodCall original) {
                return original instanceof J.NewClass && replacement.startsWith("this(");
            }

            private void removeAndAddImports(MethodCall method, Set<String> templateImports, Set<String> templateStaticImports) {
                Set<String> originalImports = findOriginalImports(method);

                // Remove regular and static imports that are no longer needed
                for (String originalImport : originalImports) {
                    if (!templateImports.contains(originalImport) &&
                            !templateStaticImports.contains(originalImport)) {
                        maybeRemoveImport(originalImport);
                    }
                }

                // Add new regular imports needed by the template
                for (String importStr : templateImports) {
                    if (!originalImports.contains(importStr)) {
                        maybeAddImport(importStr);
                    }
                }

                // Add new static imports needed by the template
                for (String staticImport : templateStaticImports) {
                    if (!originalImports.contains(staticImport)) {
                        int lastDot = staticImport.lastIndexOf('.');
                        if (0 < lastDot) {
                            maybeAddImport(
                                    staticImport.substring(0, lastDot),
                                    staticImport.substring(lastDot + 1));
                        }
                    }
                }
            }

            private Set<String> findOriginalImports(MethodCall method) {
                // Collect all regular and static imports used in the original method call
                return new JavaVisitor<Set<String>>() {
                    @Override
                    public @Nullable JavaType visitType(@Nullable JavaType javaType, Set<String> imports) {
                        JavaType jt = super.visitType(javaType, imports);
                        if (jt instanceof JavaType.FullyQualified) {
                            imports.add(((JavaType.FullyQualified) jt).getFullyQualifiedName());
                        }
                        return jt;
                    }

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation methodInvocation, Set<String> staticImports) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, staticImports);
                        // Check if this is a static method invocation without a select (meaning it might be statically imported)
                        JavaType.Method methodType = mi.getMethodType();
                        if (mi.getSelect() == null && methodType != null && methodType.hasFlags(Flag.Static)) {
                            staticImports.add(format("%s.%s",
                                    methodType.getDeclaringType().getFullyQualifiedName(),
                                    methodType.getName()));
                        }
                        return mi;
                    }

                    @Override
                    public J visitIdentifier(J.Identifier identifier, Set<String> staticImports) {
                        J.Identifier id = (J.Identifier) super.visitIdentifier(identifier, staticImports);
                        // Check if this is a static field reference
                        JavaType.Variable fieldType = id.getFieldType();
                        if (fieldType != null && fieldType.hasFlags(Flag.Static)) {
                            if (fieldType.getOwner() instanceof JavaType.FullyQualified) {
                                staticImports.add(format("%s.%s",
                                        ((JavaType.FullyQualified) fieldType.getOwner()).getFullyQualifiedName(),
                                        fieldType.getName()));
                            }
                        }
                        return id;
                    }
                }.reduce(method, new HashSet<>());
            }

            J applyJavaTemplate(MethodCall methodCall, Cursor cursor, Set<String> importsSet, Set<String> staticImportsSet, ExecutionContext ctx) {
                JavaType.Method methodType = requireNonNull(methodCall.getMethodType());
                String string = createTemplateString(methodCall, methodType);
                Object[] parameters = createParameters(string, methodCall);

                JavaTemplate.Builder templateBuilder = JavaTemplate.builder(string)
                        .contextSensitive()
                        .imports(importsSet.toArray(new String[0]))
                        .staticImports(staticImportsSet.toArray(new String[0]));
                if (classpathFromResources != null && !classpathFromResources.isEmpty()) {
                    templateBuilder.javaParser(JavaParser.fromJavaVersion()
                            .classpathFromResources(ctx, classpathFromResources.toArray(new String[0])));
                }
                return templateBuilder.build()
                        .apply(cursor, methodCall.getCoordinates().replace(), parameters);
            }

            private String createTemplateString(MethodCall original, JavaType.Method methodType) {
                String templateString;
                List<String> placeholders = methodType.getParameterNames();
                if (isConstructorToConstructor(original)) {
                    // Spelled as the original call does to keep its type arguments or diamond
                    TypeTree clazz = ((J.NewClass) original).getClazz();
                    String className = clazz == null ? methodType.getDeclaringType().getClassName() : clazz.printTrimmed(getCursor());
                    templateString = "new " + className + replacement.substring(4);
                } else if (original instanceof J.MethodInvocation &&
                        ((J.MethodInvocation) original).getSelect() == null &&
                        replacement.startsWith("this.")) {
                    templateString = replacement.substring(5);
                } else if (original instanceof J.MethodInvocation &&
                        ((J.MethodInvocation) original).getSelect() != null &&
                        BARE_METHOD_CALL.matcher(replacement).find()) {
                    // Original had a select but replacement is a bare method call; preserve the select
                    templateString = "#{this:any()}." + replacement;
                } else {
                    templateString = replacement;
                    placeholders = ListUtils.concat(placeholders, "this");
                }
                return substituteParameters(templateString, placeholders);
            }

            private Object[] createParameters(String templateString, MethodCall original) {
                Map<String, Expression> lookup = new HashMap<>();
                if (original instanceof J.MethodInvocation) {
                    Expression select = ((J.MethodInvocation) original).getSelect();
                    if (select != null) {
                        lookup.put("this", select);
                    }
                }
                List<String> originalParameterNames = requireNonNull(original.getMethodType()).getParameterNames();
                for (int i = 0; i < originalParameterNames.size(); i++) {
                    String originalName = originalParameterNames.get(i);
                    Expression originalValue = original.getArguments().get(i);
                    lookup.put(originalName, originalValue);
                }
                List<Object> parameters = new ArrayList<>();
                Matcher matcher = TEMPLATE_TOKEN.matcher(templateString);
                while (matcher.find()) {
                    String declared = matcher.group("declared");
                    if (declared != null && lookup.containsKey(declared)) {
                        parameters.add(lookup.get(declared));
                    }
                }
                return parameters.toArray();
            }

            private J avoidMethodSelfReferences(MethodCall original, J replacement) {
                JavaType.Method replacementMethodType = replacement instanceof MethodCall ?
                        ((MethodCall) replacement).getMethodType() : null;
                if (replacementMethodType == null) {
                    return replacement;
                }

                Cursor cursor = getCursor();
                while ((cursor = cursor.getParent()) != null) {
                    Object value = cursor.getValue();

                    JavaType.Method cursorMethodType;
                    if (value instanceof MethodCall) {
                        cursorMethodType = ((MethodCall) value).getMethodType();
                    } else if (value instanceof J.MethodDeclaration) {
                        cursorMethodType = ((J.MethodDeclaration) value).getMethodType();
                    } else {
                        continue;
                    }
                    if (TypeUtils.isOfType(replacementMethodType, cursorMethodType)) {
                        return original;
                    }
                }
                return replacement;
            }
        });
    }

    private static String substituteParameters(String template, List<String> parameterNames) {
        StringBuilder result = new StringBuilder();
        Set<String> declared = new HashSet<>();
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        int copied = 0;
        while (matcher.find()) {
            String reference = matcher.group("reference");
            if (matcher.group("declared") != null) {
                declared.add(matcher.group("declared"));
            } else if (reference != null && parameterNames.contains(reference)) {
                result.append(template, copied, matcher.start())
                        .append(declared.add(reference) ? "#{" + reference + ":any()}" : "#{" + reference + "}");
                copied = matcher.end();
            }
        }
        return result.append(template, copied, template.length()).toString();
    }
}
