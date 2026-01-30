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
package org.openrewrite.kotlin.replace;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.KotlinVisitor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces deprecated Kotlin method calls based on {@code @Deprecated(replaceWith=ReplaceWith(...))} annotations.
 * <p>
 * This recipe takes a method pattern to match and a replacement expression that follows the Kotlin
 * {@code ReplaceWith} annotation format.
 */
@Incubating(since = "8.43.0")
@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceKotlinMethod extends Recipe {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\b");
    private static final Pattern TEMPLATE_PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
    private static final Collection<String> KOTLIN_KEYWORDS = new HashSet<>(Arrays.asList(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
            "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
            "val", "var", "when", "while"));

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "arrow.core.MapKt mapOrAccumulate(kotlin.Function2)")
    String methodPattern;

    @Option(displayName = "Replacement",
            description = "The replacement expression from `@Deprecated(replaceWith=ReplaceWith(...))`. " +
                          "Parameter names from the original method can be used directly.",
            example = "mapValuesOrAccumulate(transform)")
    String replacement;

    @Option(displayName = "Imports",
            description = "List of imports to add when the replacement is made.",
            required = false,
            example = "[\"arrow.core.Either\"]")
    @Nullable
    List<String> imports;

    @Option(displayName = "Classpath from resources",
            description = "List of classpath resource names for parsing the replacement template.",
            required = false,
            example = "[\"arrow-core-2\"]")
    @Nullable
    List<String> classpathFromResources;

    String displayName = "Replace Kotlin method";
    String description = "Replaces Kotlin method calls based on `@Deprecated(replaceWith=ReplaceWith(...))` annotations.";
    Set<String> tags = new HashSet<>(Arrays.asList("kotlin", "deprecated"));

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, true);
        return Preconditions.check(new UsesMethod<>(methodPattern, true), new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                MethodCall mc = (MethodCall) super.visitNewClass(newClass, ctx);
                if (matcher.matches(mc)) {
                    return replaceMethod(mc, ctx);
                }
                return mc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                MethodCall mc = (MethodCall) super.visitMethodInvocation(method, ctx);
                if (matcher.matches(mc)) {
                    return replaceMethod(mc, ctx);
                }
                return mc;
            }

            private J replaceMethod(MethodCall method, ExecutionContext ctx) {
                JavaType.Method methodType = method.getMethodType();
                if (methodType == null) {
                    return method;
                }

                // Add imports if specified
                if (imports != null) {
                    for (String imp : imports) {
                        int lastDot = imp.lastIndexOf('.');
                        if (lastDot > 0) {
                            maybeAddImport(imp.substring(0, lastDot), imp.substring(lastDot + 1), false);
                        }
                    }
                }

                // Build and apply the template
                TemplateConversion conversion = convertToTemplate(method, methodType);
                KotlinTemplate.Builder templateBuilder = KotlinTemplate.builder(conversion.templateString);
                if (imports != null) {
                    templateBuilder.imports(imports.toArray(new String[0]));
                }
                if (classpathFromResources != null && !classpathFromResources.isEmpty()) {
                    templateBuilder.parser(KotlinParser.builder()
                            .classpathFromResources(ctx, classpathFromResources.toArray(new String[0])));
                }

                J result = templateBuilder.build()
                        .apply(getCursor(), method.getCoordinates().replace(), conversion.parameters.toArray());

                return result.withPrefix(method.getPrefix());
            }

            private TemplateConversion convertToTemplate(MethodCall method, JavaType.Method methodType) {
                String templateString = replacement;
                List<Object> parameters = new ArrayList<>();
                Map<String, Expression> parameterLookup = new HashMap<>();

                // Map 'this' to the select expression (receiver)
                Expression select = method instanceof J.MethodInvocation ? ((J.MethodInvocation) method).getSelect() : null;
                if (select != null) {
                    parameterLookup.put("this", select);
                }

                // Map parameter names to their argument expressions
                List<String> parameterNames = methodType.getParameterNames();
                List<Expression> arguments = method.getArguments();
                // For extension functions, the method type includes the receiver as the first
                // parameter, but the arguments don't include it (it's the select/receiver).
                // Detect this by checking if there are more parameter names than arguments.
                int paramOffset = parameterNames.size() > arguments.size() && select != null ?
                        parameterNames.size() - arguments.size() : 0;
                for (int i = 0; i < arguments.size() && i + paramOffset < parameterNames.size(); i++) {
                    parameterLookup.put(parameterNames.get(i + paramOffset), arguments.get(i));
                }

                // Also support positional references like p0, p1, etc.
                for (int i = 0; i < arguments.size(); i++) {
                    parameterLookup.put("p" + i, arguments.get(i));
                }

                // Determine if this is an instance method call that needs a receiver.
                // The replacement needs a receiver prepended if:
                // - The original call has a receiver (select)
                // - The replacement doesn't explicitly use 'this' (handled separately)
                // - The replacement isn't a static/constructor call (starts with uppercase)
                boolean needsReceiver = select != null &&
                                        !replacement.matches(".*\\bthis\\b.*") &&
                                        !isLikelyStaticReplacement(replacement);

                // Convert the replacement expression to a template
                // Replace 'this.' prefix with receiver placeholder
                if (templateString.startsWith("this.")) {
                    if (select != null) {
                        templateString = "#{any()}." + templateString.substring(5);
                        parameters.add(select);
                    } else {
                        // No select, just remove 'this.'
                        templateString = templateString.substring(5);
                    }
                } else if (needsReceiver) {
                    // Prepend the receiver for instance method calls
                    templateString = "#{any()}." + templateString;
                    parameters.add(select);
                }

                // Now replace 'this' references that appear elsewhere
                if (templateString.contains("this") && select != null) {
                    int thisCount = StringUtils.countOccurrences(templateString, "this");
                    templateString = templateString.replaceAll("\\bthis\\b", "#{any()}");
                    for (int i = 0; i < thisCount; i++) {
                        parameters.add(select);
                    }
                }

                // Find all identifiers in the template and replace with placeholders
                Set<String> processedParams = new HashSet<>();
                StringBuilder result = new StringBuilder();
                Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(templateString);
                int lastEnd = 0;

                while (identifierMatcher.find()) {
                    String identifier = identifierMatcher.group(1);

                    // Skip if already a placeholder or a keyword
                    if ("any".equals(identifier) ||
                            KOTLIN_KEYWORDS.contains(identifier) ||
                            processedParams.contains(identifier)) {
                        continue;
                    }

                    Expression expr = parameterLookup.get(identifier);
                    if (expr != null && !processedParams.contains(identifier)) {
                        // This identifier is a parameter reference
                        result.append(templateString, lastEnd, identifierMatcher.start());
                        result.append("#{any()}");
                        parameters.add(expr);
                        processedParams.add(identifier);
                        lastEnd = identifierMatcher.end();
                    }
                }
                result.append(templateString.substring(lastEnd));
                templateString = result.toString();

                return new TemplateConversion(templateString, parameters);
            }

            private boolean isLikelyStaticReplacement(String replacement) {
                // Check if the replacement looks like a constructor or static call
                // e.g., "EmptySerializersModule()" or "SomeClass.method()"
                return !replacement.isEmpty() && Character.isUpperCase(replacement.charAt(0));
            }
        });
    }

    @Value
    private static class TemplateConversion {
        String templateString;
        List<Object> parameters;
    }
}
