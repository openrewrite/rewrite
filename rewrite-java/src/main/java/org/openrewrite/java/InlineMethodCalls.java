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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class InlineMethodCalls extends Recipe {

    private static final String INLINE_ME = "InlineMe";

    @Override
    public String getDisplayName() {
        return "Inline methods annotated with `@InlineMe`";
    }

    @Override
    public String getDescription() {
        return "Apply inlinings as defined by Error Prone's [`@InlineMe` annotation](https://errorprone.info/docs/inlineme), " +
                "or compatible annotations. Uses the template and method arguments to replace method calls. " +
                "Supports both methods invocations and constructor calls, with optional new imports.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // XXX Preconditions can not yet pick up the `@InlineMe` annotation on methods used
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                InlineMeValues values = findInlineMeValues(mi.getMethodType());
                if (values == null) {
                    return mi;
                }
                Template template = values.template(mi);
                if (template == null) {
                    return mi;
                }
                removeAndAddImports(method, values.getImports(), values.getStaticImports());
                J replacement = JavaTemplate.builder(template.getString())
                        .contextSensitive()
                        .imports(values.getImports().toArray(new String[0]))
                        .staticImports(values.getStaticImports().toArray(new String[0]))
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(updateCursor(mi), mi.getCoordinates().replace(), template.getParameters());
                return avoidMethodSelfReferences(mi, replacement);
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                InlineMeValues values = findInlineMeValues(nc.getConstructorType());
                if (values == null) {
                    return nc;
                }
                Template template = values.template(nc);
                if (template == null) {
                    return nc;
                }
                removeAndAddImports(newClass, values.getImports(), values.getStaticImports());
                J replacement = JavaTemplate.builder(template.getString())
                        .contextSensitive()
                        .imports(values.getImports().toArray(new String[0]))
                        .staticImports(values.getStaticImports().toArray(new String[0]))
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(updateCursor(nc), nc.getCoordinates().replace(), template.getParameters());
                return avoidMethodSelfReferences(nc, replacement);
            }

            private @Nullable InlineMeValues findInlineMeValues(JavaType.@Nullable Method methodType) {
                if (methodType == null) {
                    return null;
                }
                List<String> parameterNames = methodType.getParameterNames();
                if (!parameterNames.isEmpty() && "arg0".equals(parameterNames.get(0))) {
                    return null; // We need `-parameters` before we're able to substitute parameters in the template
                }

                List<JavaType.FullyQualified> annotations = methodType.getAnnotations();
                for (JavaType.FullyQualified annotation : annotations) {
                    if (INLINE_ME.equals(annotation.getClassName())) {
                        return InlineMeValues.parse((JavaType.Annotation) annotation);
                    }
                }
                return null;
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
                    public @Nullable JavaType visitType(@Nullable JavaType javaType, Set<String> strings) {
                        JavaType jt = super.visitType(javaType, strings);
                        if (jt instanceof JavaType.FullyQualified) {
                            strings.add(((JavaType.FullyQualified) jt).getFullyQualifiedName());
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
        };
    }

    @Value
    private static class InlineMeValues {
        private static final Pattern TEMPLATE_IDENTIFIER = Pattern.compile("#\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*):any\\(.*?\\)}");

        @Getter(AccessLevel.NONE)
        String replacement;

        Set<String> imports;
        Set<String> staticImports;

        static InlineMeValues parse(JavaType.Annotation annotation) {
            Map<String, Object> collect = annotation.getValues().stream().collect(toMap(
                    e -> ((JavaType.Method) e.getElement()).getName(),
                    JavaType.Annotation.ElementValue::getValue
            ));
            // Parse imports and static imports from the annotation values
            return new InlineMeValues(
                    (String) collect.get("replacement"),
                    parseImports(collect.get("imports")),
                    parseImports(collect.get("staticImports")));
        }

        private static Set<String> parseImports(@Nullable Object importsValue) {
            if (importsValue instanceof List) {
                return ((List<?>) importsValue).stream()
                        .map(Object::toString)
                        .collect(toSet());
            }
            return emptySet();
        }

        @Nullable
        Template template(MethodCall original) {
            JavaType.Method methodType = original.getMethodType();
            if (methodType == null) {
                return null;
            }
            String templateString = createTemplateString(original, replacement, methodType);
            List<Object> parameters = createParameters(templateString, original);
            return new Template(templateString, parameters.toArray(new Object[0]));
        }

        private static String createTemplateString(MethodCall original, String replacement, JavaType.Method methodType) {
            String templateString;
            if (original instanceof J.NewClass && replacement.startsWith("this(")) {
                // For constructor-to-constructor replacement, replace "this" with "new ClassName"
                templateString = "new " + methodType.getDeclaringType().getClassName() + replacement.substring(4);
            } else if (original instanceof J.MethodInvocation &&
                    ((J.MethodInvocation) original).getSelect() == null &&
                    replacement.startsWith("this.")) {
                templateString = replacement.substring(5);
            } else {
                templateString = replacement.replaceAll("\\bthis\\b", "#{this:any()}");
            }
            List<String> originalParameterNames = methodType.getParameterNames();
            for (String parameterName : originalParameterNames) {
                // Replace parameter names with their values in the templateString
                templateString = templateString
                        .replaceFirst(format("\\b%s\\b", parameterName), format("#{%s:any()}", parameterName))
                        .replaceAll(format("(?<!\\{)\\b%s\\b", parameterName), format("#{%s}", parameterName));
            }
            return templateString;
        }

        private static List<Object> createParameters(String templateString, MethodCall original) {
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
            Matcher matcher = TEMPLATE_IDENTIFIER.matcher(templateString);
            while (matcher.find()) {
                Expression o = lookup.get(matcher.group(1));
                if (o != null) {
                    parameters.add(o);
                }
            }
            return parameters;
        }
    }

    @Value
    private static class Template {
        String string;
        Object[] parameters;
    }
}
