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
package org.openrewrite.java.internal.template;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class JavaTemplateParser {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("#{", "}", null);

    private static final Object templateCacheLock = new Object();

    private static final Map<String, List<? extends J>> templateCache = new LinkedHashMap<String, List<? extends J>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 1_000;
        }
    };

    private static final String PACKAGE_STUB = "package #{}; class $Template {}";
    private static final String PARAMETER_STUB = "abstract class $Template { abstract void $template(#{}); }";
    private static final String LAMBDA_PARAMETER_STUB = "class $Template { { Object o = (#{}) -> {}; } }";
    private static final String EXTENDS_STUB = "class $Template extends #{} {}";
    private static final String IMPLEMENTS_STUB = "class $Template implements #{} {}";
    private static final String THROWS_STUB = "abstract class $Template { abstract void $template() throws #{}; }";
    private static final String TYPE_PARAMS_STUB = "class $Template<#{}> {}";

    @Language("java")
    private static final String SUBSTITUTED_ANNOTATION = "@java.lang.annotation.Documented public @interface SubAnnotation { int value(); }";

    private final JavaParser.Builder<?, ?> parser;
    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;
    private final Set<String> imports;
    private final BlockStatementTemplateGenerator statementTemplateGenerator;
    private final AnnotationTemplateGenerator annotationTemplateGenerator;

    public JavaTemplateParser(JavaParser.Builder<?, ?> parser, Consumer<String> onAfterVariableSubstitution,
                              Consumer<String> onBeforeParseTemplate, Set<String> imports) {
        this.parser = parser;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.onBeforeParseTemplate = onBeforeParseTemplate;
        this.imports = imports;
        this.statementTemplateGenerator = new BlockStatementTemplateGenerator(imports);
        this.annotationTemplateGenerator = new AnnotationTemplateGenerator(imports);
    }

    public List<Statement> parseParameters(String template) {
        @Language("java") String stub = addImports(substitute(PARAMETER_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            return m.getParameters();
        });
    }

    public J.Lambda.Parameters parseLambdaParameters(String template) {
        @Language("java") String stub = addImports(substitute(LAMBDA_PARAMETER_STUB, template));
        onBeforeParseTemplate.accept(stub);

        return (J.Lambda.Parameters) cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.Block b = (J.Block) cu.getClasses().get(0).getBody().getStatements().get(0);
            J.VariableDeclarations v = (J.VariableDeclarations) b.getStatements().get(0);
            J.Lambda l = (J.Lambda) v.getVariables().get(0).getInitializer();
            assert l != null;
            return singletonList(l.getParameters());
        }).get(0);
    }

    public J parseExpression(Cursor cursor, String template, Space.Location location) {
        @Language("java") String stub = statementTemplateGenerator.template(cursor, template, location, JavaCoordinates.Mode.REPLACEMENT);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(stub);
        return statementTemplateGenerator.listTemplatedTrees(cu, Expression.class).get(0);
    }

    public TypeTree parseExtends(String template) {
        @Language("java") String stub = addImports(substitute(EXTENDS_STUB, template));
        onBeforeParseTemplate.accept(stub);

        return (TypeTree) cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            TypeTree anExtends = cu.getClasses().get(0).getExtends();
            assert anExtends != null;
            return singletonList(anExtends);
        }).get(0);
    }

    public List<TypeTree> parseImplements(String template) {
        @Language("java") String stub = addImports(substitute(IMPLEMENTS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            List<TypeTree> anImplements = cu.getClasses().get(0).getImplements();
            assert anImplements != null;
            return anImplements;
        });
    }

    public List<NameTree> parseThrows(String template) {
        @Language("java") String stub = addImports(substitute(THROWS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            List<NameTree> aThrows = m.getThrows();
            assert aThrows != null;
            return aThrows;
        });
    }

    public List<J.TypeParameter> parseTypeParameters(String template) {
        @Language("java") String stub = addImports(substitute(TYPE_PARAMS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            List<J.TypeParameter> tps = cu.getClasses().get(0).getTypeParameters();
            assert tps != null;
            return tps;
        });
    }

    public <J2 extends J> List<J2> parseBlockStatements(Cursor cursor, Class<J2> expected,
                                                        String template,
                                                        Space.Location location,
                                                        JavaCoordinates.Mode mode) {
        // TODO: The stub string includes the scoped elements of each original AST, and therefore is not a good
        //       cache key. There are virtual no cases where a stub key will result in re-use. If we can come up with
        //       a safe, reusable key, we can consider using the cache for block statements.
        @Language("java") String stub = statementTemplateGenerator.template(cursor, template, location, mode);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(stub);
        return statementTemplateGenerator.listTemplatedTrees(cu, expected);
    }

    public J.MethodInvocation parseMethod(Cursor cursor, String template, Space.Location location) {
        J.MethodInvocation method = cursor.getValue();
        String methodWithReplacedNameAndArgs;
        if (method.getSelect() == null) {
            methodWithReplacedNameAndArgs = template;
        } else {
            methodWithReplacedNameAndArgs = method.getSelect().print(cursor) + "." + template;
        }
        // TODO: The stub string includes the scoped elements of each original AST, and therefore is not a good
        //       cache key. There are virtual no cases where a stub key will result in re-use. If we can come up with
        //       a safe, reusable key, we can consider using the cache for block statements.
        @Language("java") String stub = statementTemplateGenerator.template(cursor, methodWithReplacedNameAndArgs, location, JavaCoordinates.Mode.REPLACEMENT);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(stub);
        return (J.MethodInvocation) statementTemplateGenerator
                .listTemplatedTrees(cu, Statement.class).get(0);
    }

    public J.MethodInvocation parseMethodArguments(Cursor cursor, String template, Space.Location location) {
        J.MethodInvocation method = cursor.getValue();
        String methodWithReplacementArgs = method.withArguments(Collections.emptyList()).printTrimmed(cursor.getParentOrThrow())
                .replaceAll("\\)$", template + ")");
        // TODO: The stub string includes the scoped elements of each original AST, and therefore is not a good
        //       cache key. There are virtual no cases where a stub key will result in re-use. If we can come up with
        //       a safe, reusable key, we can consider using the cache for block statements.
        @Language("java") String stub = statementTemplateGenerator.template(cursor, methodWithReplacementArgs, location, JavaCoordinates.Mode.REPLACEMENT);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(stub);
        return (J.MethodInvocation) statementTemplateGenerator
                .listTemplatedTrees(cu, Statement.class).get(0);
    }

    public List<J.Annotation> parseAnnotations(Cursor cursor, String template) {
        String cacheKey = addImports(annotationTemplateGenerator.cacheKey(cursor, template));
        return cache(cacheKey, () -> {
            @Language("java") String stub = annotationTemplateGenerator.template(cursor, template);
            onBeforeParseTemplate.accept(stub);
            JavaSourceFile cu = compileTemplate(stub);
            return annotationTemplateGenerator.listAnnotations(cu);
        });
    }

    public Expression parsePackage(String template) {
        @Language("java") String stub = substitute(PACKAGE_STUB, template);
        onBeforeParseTemplate.accept(stub);

        return (Expression) cache(stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            @SuppressWarnings("ConstantConditions") Expression expression = cu.getPackageDeclaration()
                    .getExpression();
            return singletonList(expression);
        }).get(0);
    }

    private String substitute(String stub, String template) {
        String beforeParse = placeholderHelper.replacePlaceholders(stub, k -> template);
        onAfterVariableSubstitution.accept(beforeParse);
        return beforeParse;
    }

    private String addImports(String stub) {
        if (!imports.isEmpty()) {
            StringBuilder withImports = new StringBuilder();
            for (String anImport : imports) {
                withImports.append(anImport);
            }
            withImports.append(stub);
            return withImports.toString();
        }
        return stub;
    }

    private JavaSourceFile compileTemplate(@Language("java") String stub) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        JavaParser jp = parser.clone().build();
        return (stub.contains("@SubAnnotation") ?
                jp.reset().parse(ctx, stub, SUBSTITUTED_ANNOTATION) :
                jp.reset().parse(ctx, stub)
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> List<J2> cache(String stub, Supplier<List<? extends J>> ifAbsent) {
        List<J2> js;
        synchronized (templateCacheLock) {
            Timer.Sample sample = Timer.start();
            js = (List<J2>) templateCache.get(stub);
            if (js == null) {
                js = (List<J2>) ifAbsent.get();
                templateCache.put(stub, js);
                sample.stop(Timer.builder("rewrite.template.cache").tag("result", "miss")
                        .register(Metrics.globalRegistry));
            } else {
                sample.stop(Timer.builder("rewrite.template.cache").tag("result", "hit")
                        .register(Metrics.globalRegistry));
            }
        }
        return ListUtils.map(js, j -> (J2) new RandomizeIdVisitor<Integer>().visit(j, 0));
    }

    public static void clearCache() {
        synchronized (templateCacheLock) {
            templateCache.clear();
        }
    }

}
