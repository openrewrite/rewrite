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
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

public class JavaTemplateParser {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("#{", "}", null);

    private static final String TEMPLATE_CACHE_MESSAGE_KEY = "__org.openrewrite.java.internal.template.JavaTemplateParser.cache__";

    private static final String PACKAGE_STUB = "package #{}; class $Template {}";
    private static final String PARAMETER_STUB = "abstract class $Template { abstract void $template(#{}); }";
    private static final String LAMBDA_PARAMETER_STUB = "class $Template { { Object o = (#{}) -> {}; } }";
    private static final String EXTENDS_STUB = "class $Template extends #{} {}";
    private static final String IMPLEMENTS_STUB = "class $Template implements #{} {}";
    private static final String THROWS_STUB = "abstract class $Template { abstract void $template() throws #{}; }";
    private static final String TYPE_PARAMS_STUB = "class $Template<#{}> {}";

    @Language("java")
    private static final String SUBSTITUTED_ANNOTATION = "@java.lang.annotation.Documented public @interface SubAnnotation { int value(); }";

    private final Parser.Builder parser;
    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;
    private final Set<String> imports;
    private final boolean contextSensitive;
    private final BlockStatementTemplateGenerator statementTemplateGenerator;
    private final AnnotationTemplateGenerator annotationTemplateGenerator;

    public JavaTemplateParser(boolean contextSensitive, Parser.Builder parser, Consumer<String> onAfterVariableSubstitution,
                              Consumer<String> onBeforeParseTemplate, Set<String> imports) {
        this(
                parser,
                onAfterVariableSubstitution,
                onBeforeParseTemplate,
                imports,
                contextSensitive,
                new BlockStatementTemplateGenerator(imports, contextSensitive),
                new AnnotationTemplateGenerator(imports)
        );
    }

    protected JavaTemplateParser(Parser.Builder parser, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate, Set<String> imports, boolean contextSensitive, BlockStatementTemplateGenerator statementTemplateGenerator, AnnotationTemplateGenerator annotationTemplateGenerator) {
        this.parser = parser;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.onBeforeParseTemplate = onBeforeParseTemplate;
        this.imports = imports;
        this.contextSensitive = contextSensitive;
        this.statementTemplateGenerator = statementTemplateGenerator;
        this.annotationTemplateGenerator = annotationTemplateGenerator;
    }

    public List<Statement> parseParameters(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(PARAMETER_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(cursor, stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            return m.getParameters();
        });
    }

    public J.Lambda.Parameters parseLambdaParameters(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(LAMBDA_PARAMETER_STUB, template));
        onBeforeParseTemplate.accept(stub);

        return (J.Lambda.Parameters) cache(cursor, stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.Block b = (J.Block) cu.getClasses().get(0).getBody().getStatements().get(0);
            J.VariableDeclarations v = (J.VariableDeclarations) b.getStatements().get(0);
            J.Lambda l = (J.Lambda) v.getVariables().get(0).getInitializer();
            assert l != null;
            return singletonList(l.getParameters());
        }).get(0);
    }

    public J parseExpression(Cursor cursor, String template, Space.Location location) {
        return cacheIfContextFree(cursor, new ContextFreeCacheKey(template, Expression.class, imports),
                tmpl -> statementTemplateGenerator.template(cursor, tmpl, location, JavaCoordinates.Mode.REPLACEMENT),
                stub -> {
                    onBeforeParseTemplate.accept(stub);
                    JavaSourceFile cu = compileTemplate(stub);
                    return statementTemplateGenerator.listTemplatedTrees(cu, Expression.class);
                }).get(0);
    }

    public TypeTree parseExtends(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(EXTENDS_STUB, template));
        onBeforeParseTemplate.accept(stub);

        return (TypeTree) cache(cursor, stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            TypeTree anExtends = cu.getClasses().get(0).getExtends();
            assert anExtends != null;
            return singletonList(anExtends);
        }).get(0);
    }

    public List<TypeTree> parseImplements(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(IMPLEMENTS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(cursor, stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            List<TypeTree> anImplements = cu.getClasses().get(0).getImplements();
            assert anImplements != null;
            return anImplements;
        });
    }

    public List<NameTree> parseThrows(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(THROWS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(cursor, stub, () -> {
            JavaSourceFile cu = compileTemplate(stub);
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            List<NameTree> aThrows = m.getThrows();
            assert aThrows != null;
            return aThrows;
        });
    }

    public List<J.TypeParameter> parseTypeParameters(Cursor cursor, String template) {
        @Language("java") String stub = addImports(substitute(TYPE_PARAMS_STUB, template));
        onBeforeParseTemplate.accept(stub);
        return cache(cursor, stub, () -> {
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
        return cacheIfContextFree(cursor,
                new ContextFreeCacheKey(template, expected, imports),
                tmpl -> statementTemplateGenerator.template(cursor, tmpl, location, mode),
                stub -> {
                    onBeforeParseTemplate.accept(stub);
                    JavaSourceFile cu = compileTemplate(stub);
                    return statementTemplateGenerator.listTemplatedTrees(cu, expected);
                });
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
        return cache(cursor, cacheKey, () -> {
            @Language("java") String stub = annotationTemplateGenerator.template(cursor, template);
            onBeforeParseTemplate.accept(stub);
            JavaSourceFile cu = compileTemplate(stub);
            return annotationTemplateGenerator.listAnnotations(cu);
        });
    }

    public Expression parsePackage(Cursor cursor, String template) {
        @Language("java") String stub = substitute(PACKAGE_STUB, template);
        onBeforeParseTemplate.accept(stub);

        return (Expression) cache(cursor, stub, () -> {
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
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        Parser jp = parser.build();
        return (stub.contains("@SubAnnotation") ?
                jp.reset().parse(ctx, stub, SUBSTITUTED_ANNOTATION) :
                jp.reset().parse(ctx, stub))
                .findFirst()
                .filter(JavaSourceFile.class::isInstance) // Filters out ParseErrors
                .map(JavaSourceFile.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
    }

    /**
     * Return the result of parsing the stub.
     * Cache the LST elements parsed from stub only if the stub is context free.
     * <p>
     * For a stub to be context free nothing about its meaning can be changed by the context in which it is parsed.
     * For example, the statement `int i = 0;` is context free because it will always be parsed as a variable
     * The statement `i++;` cannot be context free because it cannot be parsed without a preceding declaration of i.
     * The statement `class A{}` is typically not context free because it
     *
     * @param cursor     indicates whether the stub is context free or not
     * @param treeMapper supplies the LST elements produced from the stub
     * @return result of parsing the stub into LST elements
     */
    private <J2 extends J> List<J2> cacheIfContextFree(Cursor cursor, ContextFreeCacheKey key,
                                                       UnaryOperator<String> stubMapper,
                                                       Function<String, List<? extends J>> treeMapper) {
        if (cursor.getParent() == null) {
            throw new IllegalArgumentException("Expecting `cursor` to have a parent element");
        }
        if (!contextSensitive) {
            return cache(cursor, key, () -> treeMapper.apply(stubMapper.apply(key.getTemplate())));
        }
        //noinspection unchecked
        return (List<J2>) treeMapper.apply(stubMapper.apply(key.getTemplate()));
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> List<J2> cache(Cursor cursor, Object key, Supplier<List<? extends J>> ifAbsent) {
        List<J2> js = null;

        Timer.Sample sample = Timer.start();
        Cursor root = cursor.getRoot();
        Map<Object, List<J2>> cache = root.getMessage(TEMPLATE_CACHE_MESSAGE_KEY);
        if (cache == null) {
            cache = new HashMap<>();
            root.putMessage(TEMPLATE_CACHE_MESSAGE_KEY, cache);
        } else {
            js = cache.get(key);
        }

        if (js == null) {
            js = (List<J2>) ifAbsent.get();
            cache.put(key, js);
            sample.stop(Timer.builder("rewrite.template.cache").tag("result", "miss")
                    .register(Metrics.globalRegistry));
        } else {
            sample.stop(Timer.builder("rewrite.template.cache").tag("result", "hit")
                    .register(Metrics.globalRegistry));
        }

        return ListUtils.map(js, j -> (J2) new RandomizeIdVisitor<Integer>().visit(j, 0));
    }

    @Value
    private static class ContextFreeCacheKey {
        String template;
        Class<? extends J> expected;
        Set<String> imports;
    }
}
