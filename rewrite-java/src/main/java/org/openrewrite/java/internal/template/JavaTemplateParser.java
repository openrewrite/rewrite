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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RandomizeIdVisitor;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class JavaTemplateParser {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("#{", "}", null);

    private static final String TEMPLATE_CACHE_MESSAGE_KEY = "__org.openrewrite.java.internal.template.JavaTemplateParser.cache__";

    @Language("java")
    private static final String SUBSTITUTED_ANNOTATION = "@java.lang.annotation.Documented public @interface SubAnnotation { int value(); }";

    private final Parser.Builder parser;
    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;
    private final Set<String> imports;
    private final boolean contextSensitive;
    private final BlockStatementTemplateGenerator statementTemplateGenerator;
    private final AnnotationTemplateGenerator annotationTemplateGenerator;
    private final TemplateStubs templateStubs;

    public JavaTemplateParser(boolean contextSensitive, Parser.Builder parser, Consumer<String> onAfterVariableSubstitution,
                              Consumer<String> onBeforeParseTemplate, Set<String> imports, String bindType) {
        this(
                parser,
                onAfterVariableSubstitution,
                onBeforeParseTemplate,
                imports,
                contextSensitive,
                new BlockStatementTemplateGenerator(imports, contextSensitive, bindType),
                new AnnotationTemplateGenerator(imports)
        );
    }

    protected JavaTemplateParser(Parser.Builder parser, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate, Set<String> imports, boolean contextSensitive, BlockStatementTemplateGenerator statementTemplateGenerator, AnnotationTemplateGenerator annotationTemplateGenerator) {
        this(parser, onAfterVariableSubstitution, onBeforeParseTemplate, imports, contextSensitive,
                statementTemplateGenerator, annotationTemplateGenerator, new JavaTemplateStubs());
    }

    protected JavaTemplateParser(Parser.Builder parser, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate, Set<String> imports, boolean contextSensitive, BlockStatementTemplateGenerator statementTemplateGenerator, AnnotationTemplateGenerator annotationTemplateGenerator, TemplateStubs templateStubs) {
        this.parser = parser;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.onBeforeParseTemplate = onBeforeParseTemplate;
        this.imports = imports;
        this.contextSensitive = contextSensitive;
        this.statementTemplateGenerator = statementTemplateGenerator;
        this.annotationTemplateGenerator = annotationTemplateGenerator;
        this.templateStubs = templateStubs;
    }

    /**
     * Wrap {@code template} in the language's stub for this coordinate, compile it, and read the templated
     * elements back out. The stub and the extraction that pairs with it both come from {@link TemplateStubs},
     * so a language that needs a differently shaped wrapper gets the matching extraction for free.
     */
    private <T extends J> List<T> parseStub(Cursor cursor, TemplateStubs.Stub<T> stub, String template) {
        String substituted = substitute(stub.getCode(), template);
        switch (stub.getImports()) {
            case TEMPLATE:
                substituted = addImports(substituted);
                break;
            case TEMPLATE_AND_ENCLOSING:
                substituted = addImports(cursor, substituted);
                break;
            case NONE:
                break;
        }
        String finalStub = substituted;
        onBeforeParseTemplate.accept(finalStub);
        return cache(cursor, finalStub, () -> stub.getExtract().apply(compileTemplate(cursor, finalStub)));
    }

    public List<Statement> parseParameters(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.parameters(), template);
    }

    public J.Lambda.Parameters parseLambdaParameters(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.lambdaParameters(), template).get(0);
    }

    public J parseExpression(Cursor cursor, String template, Collection<JavaType.GenericTypeVariable> typeVariables, Space.Location location) {
        List<J> result = cacheIfContextFree(cursor, new ContextFreeCacheKey(template, typeVariables.stream().map(TypeUtils::toGenericTypeString).sorted().collect(toList()), Expression.class, imports, statementTemplateGenerator.getBindType(), parser.clone()),
                tmpl -> statementTemplateGenerator.template(cursor, tmpl, typeVariables, location, JavaCoordinates.Mode.REPLACEMENT),
                stub -> {
                    onBeforeParseTemplate.accept(stub);
                    JavaSourceFile cu = compileTemplate(cursor, stub);
                    return statementTemplateGenerator.listTemplatedTrees(cu, Expression.class);
                });
        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Failed to parse expression template. The generated stub may contain types that cannot be expressed " +
                    "as valid Java source code. Use JavaTemplate.Builder#doBeforeParseTemplate() to inspect the generated code. Template:\n" + template);
        }
        return result.get(0);
    }

    public TypeTree parseExtends(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.anExtends(), template).get(0);
    }

    public List<TypeTree> parseImplements(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.anImplements(), template);
    }

    public List<NameTree> parseThrows(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.checkedExceptions(), template);
    }

    public List<J.TypeParameter> parseTypeParameters(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.typeParameters(), template);
    }

    public <J2 extends J> List<J2> parseBlockStatements(Cursor cursor, Class<J2> expected,
                                                        String template,
                                                        Collection<JavaType.GenericTypeVariable> typeVariables,
                                                        Space.Location location,
                                                        JavaCoordinates.Mode mode) {
        return cacheIfContextFree(cursor,
                new ContextFreeCacheKey(template, typeVariables.stream().map(TypeUtils::toGenericTypeString).sorted().collect(toList()), expected, imports, statementTemplateGenerator.getBindType(), parser.clone()),
                tmpl -> statementTemplateGenerator.template(cursor, tmpl, typeVariables, location, mode),
                stub -> {
                    onBeforeParseTemplate.accept(stub);
                    JavaSourceFile cu = compileTemplate(cursor, stub);
                    return statementTemplateGenerator.listTemplatedTrees(cu, expected);
                });
    }

    public J.MethodInvocation parseMethod(Cursor cursor, String template, Collection<JavaType.GenericTypeVariable> typeVariables, Space.Location location) {
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
        @Language("java") String stub = statementTemplateGenerator.template(cursor, methodWithReplacedNameAndArgs, typeVariables, location, JavaCoordinates.Mode.REPLACEMENT);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(cursor, stub);
        return (J.MethodInvocation) statementTemplateGenerator
                .listTemplatedTrees(cu, Statement.class).get(0);
    }

    public J.MethodInvocation parseMethodArguments(Cursor cursor, String template, Collection<JavaType.GenericTypeVariable> typeVariables, Space.Location location) {
        J.MethodInvocation method = cursor.getValue();
        String methodWithReplacementArgs = method.withArguments(emptyList()).printTrimmed(cursor.getParentOrThrow())
                .replaceAll("\\)$", template + (isStatement(cursor) ? ");" : ")"));
        // TODO: The stub string includes the scoped elements of each original AST, and therefore is not a good
        //       cache key. There are virtual no cases where a stub key will result in re-use. If we can come up with
        //       a safe, reusable key, we can consider using the cache for block statements.
        @Language("java") String stub = statementTemplateGenerator.template(cursor, methodWithReplacementArgs, typeVariables, location, JavaCoordinates.Mode.REPLACEMENT);
        onBeforeParseTemplate.accept(stub);
        JavaSourceFile cu = compileTemplate(cursor, stub);
        return (J.MethodInvocation) statementTemplateGenerator
                .listTemplatedTrees(cu, Statement.class).get(0);
    }

    private boolean isStatement(Cursor cursor) {
        if (!(cursor.getValue() instanceof Statement)) {
            return false;
        } else if (cursor.getValue() instanceof Expression) {
            J parent = cursor.getParentTreeCursor().getValue();
            return parent instanceof J.Block ||
                   parent instanceof J.If && ((J.If) parent).getThenPart() == cursor.getValue() ||
                   parent instanceof J.If.Else && ((J.If.Else) parent).getBody() == cursor.getValue() ||
                   parent instanceof Loop && ((Loop) parent).getBody() == cursor.getValue();
        }
        return false;
    }

    public List<J.Annotation> parseAnnotations(Cursor cursor, String template) {
        String cacheKey = addImports(annotationTemplateGenerator.cacheKey(cursor, template));
        return cache(cursor, cacheKey, () -> {
            @Language("java") String stub = annotationTemplateGenerator.template(cursor, template);
            onBeforeParseTemplate.accept(stub);
            JavaSourceFile cu = compileTemplate(cursor, stub);
            return annotationTemplateGenerator.listAnnotations(cu);
        });
    }

    public Expression parsePackage(Cursor cursor, String template) {
        return parseStub(cursor, templateStubs.packageDeclaration(), template).get(0);
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

    /**
     * Context-sensitive templates see the enclosing source file's imports in addition to the template's own,
     * so type references relying on those imports resolve when the stub is compiled.
     */
    private String addImports(Cursor cursor, String stub) {
        if (contextSensitive) {
            JavaSourceFile sourceFile = cursor.firstEnclosing(JavaSourceFile.class);
            if (sourceFile != null && !sourceFile.getImports().isEmpty()) {
                StringBuilder withImports = new StringBuilder();
                for (J.Import anImport : sourceFile.getImports()) {
                    withImports.append(anImport.withPrefix(Space.EMPTY).printTrimmed(cursor)).append(";\n");
                }
                return withImports.append(addImports(stub)).toString();
            }
        }
        return addImports(stub);
    }

    private JavaSourceFile compileTemplate(Cursor cursor, @Language("java") String stub) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        Parser jp = configuredParser(cursor).build();
        return getJavaSourceFile(stub, jp, ctx)
                // In some specific and rare cases, the parser fails to parse what is a valid program. This has been
                // investigated for several days to no avail, so the workaround is to retry parsing, which is known to
                // address the issue.
                // Context:
                // - https://github.com/openrewrite/rewrite-spring/pull/757
                // - also a thread in private Moderne slack mentioning this PR
                // - https://github.com/openrewrite/rewrite/pull/5801 which is a unit test that reproduces the issue
                // TLDR: I suspect either a bug in Java Compiler, or some fault in how we call its internals.
                .orElseGet(() -> getJavaSourceFile(stub, jp, ctx)
                        .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java:\n" + stub)));
    }

    /**
     * The parser builder to compile a stub with, given the source file the template is being applied to.
     * Languages whose builder does not extend {@link JavaParser.Builder} override this to hand the
     * enclosing type factory to their own builder type.
     */
    protected Parser.Builder configuredParser(Cursor cursor) {
        JavaTypeFactory typeFactory = enclosingTypeFactory(cursor);
        if (parser instanceof JavaParser.Builder && typeFactory != null) {
            ((JavaParser.Builder<?, ?>) parser).typeFactory(typeFactory);
        }
        return parser;
    }

    /**
     * Resolve the {@link JavaTypeFactory} that should back snippet parsing for templates
     * applied inside this cursor's enclosing source file. The factory is carried on the
     * file's {@link JavaSourceSet} marker when the source file's parser had one attached;
     * returns {@code null} otherwise and callers fall back to a fresh factory.
     */
    protected static @Nullable JavaTypeFactory enclosingTypeFactory(Cursor cursor) {
        return cursor.firstEnclosingOrThrow(SourceFile.class)
                .getMarkers().findFirst(JavaSourceSet.class)
                .map(JavaSourceSet::getTypeFactory)
                .orElse(null);
    }

    private static Optional<JavaSourceFile> getJavaSourceFile(@Language("java") String stub, Parser jp, ExecutionContext ctx) {
        return (stub.contains("@SubAnnotation") ?
                jp.reset().parse(ctx, stub, SUBSTITUTED_ANNOTATION) :
                jp.reset().parse(ctx, stub))
                .findFirst()
                .filter(JavaSourceFile.class::isInstance) // Filters out ParseErrors
                .map(JavaSourceFile.class::cast);
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
        List<String> typeVariables;
        Class<? extends J> expected;
        Set<String> imports;

        /**
         * Two templates identical but for their bind type generate different stubs and attribute differently,
         * so omitting this silently serves the first one's tree to the second.
         */
        String bindType;

        /**
         * The cache is scoped to a source file, not to a language, and a Kotlin file can legitimately have both
         * a {@code JavaTemplate} and a {@code KotlinTemplate} applied to it. Their stubs are different source in
         * different languages, so without this a template of one kind can be served the other's tree — a
         * {@code JavaTemplate} silently succeeding on text that is not valid Java.
         * <p>
         * Carries the builder itself, so parsers of the same language configured with different classpaths are
         * distinguished too. A defensive clone is stored: the live builder mutates on first
         * {@code build()} when it folds artifact names into the resolved classpath, which would otherwise
         * change the hash of a key already in the map.
         */
        Parser.Builder parser;
    }
}
