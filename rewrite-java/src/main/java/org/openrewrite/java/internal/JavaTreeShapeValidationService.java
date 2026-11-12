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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.tree.TypedTree;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

public class JavaTreeShapeValidationService implements TreeShapeValidationService {
    private static final Pattern PARSER_VERSION = Pattern.compile("\\.Java(\\d+)Parser\\b");

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof J.Identifier) {
            return "the name `" + ((J.Identifier) tree).getSimpleName() + "`";
        } else if (tree instanceof J.Literal) {
            J.Literal literal = (J.Literal) tree;
            Object value = literal.getValue();
            JavaType.Primitive type = literal.getType();
            return (value == null ? "the value null" :
                    "the " + value.getClass().getSimpleName() + " value " + quoted(value)) +
                   " of type " + (type == null ? "null" : type.getKeyword());
        } else if (tree instanceof J.Modifier) {
            J.Modifier modifier = (J.Modifier) tree;
            return "the modifier " + (modifier.getType() == J.Modifier.Type.LanguageExtension ?
                    modifier.getKeyword() : modifier.getType());
        } else if (tree instanceof J.Primitive) {
            return "the type " + ((J.Primitive) tree).getType().getKeyword();
        } else if (tree instanceof J.Binary) {
            return "the operator " + ((J.Binary) tree).getOperator();
        } else if (tree instanceof J.Unary) {
            return "the operator " + ((J.Unary) tree).getOperator();
        } else if (tree instanceof J.AssignmentOperation) {
            return "the operator " + ((J.AssignmentOperation) tree).getOperator();
        }
        return null;
    }

    private static String quoted(Object value) {
        String printed = String.valueOf(value).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
        if (printed.length() > 40) {
            printed = printed.substring(0, 40) + "...";
        }
        return value instanceof Character ? "'" + printed + "'" : value instanceof String ? '"' + printed + '"' :
                printed;
    }

    @Override
    public boolean skip(Tree tree) {
        // the parser adds empty elements where recipes often leave none, and documentation comments aren't code
        return tree instanceof J.Empty || tree instanceof Javadoc;
    }

    @Override
    public boolean reads(Parser.Builder parser, SourceFile sourceFile) {
        // a test can mark its sources with a newer Java than the JDK parser it parses them with
        Matcher parserVersion = PARSER_VERSION.matcher(parser.getClass().getName());
        JavaVersion version = sourceFile.getMarkers().findFirst(JavaVersion.class).orElse(null);
        return version == null || !parserVersion.find() ||
               version.getMajorVersion() <= Integer.parseInt(parserVersion.group(1));
    }

    @Override
    public Parser.Builder untypedParser(Parser.Builder parser) {
        if (!(parser instanceof JavaParser.Builder)) {
            return parser;
        }
        // the copy's types are incomplete, so they stay out of a type factory the test's builder holds
        return ((JavaParser.Builder<?, ?>) parser.clone())
                .classpath(emptyList())
                .dependsOn(emptyList())
                .typeAttribution(false)
                .typeFactory(null);
    }

    @Override
    public Parser.Builder typedParser(Parser.Builder parser, List<SourceFile> sources) {
        if (!(parser instanceof JavaParser.Builder)) {
            return parser;
        }
        JavaParser.Builder<?, ?> java = (JavaParser.Builder<?, ?>) parser;
        // The test's cache holds its classes as they were before the recipe ran, such as with a method it renamed, so
        // those are typed afresh. Writing to a cache of its own keeps the types of the reparse out of the test's cache.
        return java.clone().typeFactory(new DefaultJavaTypeFactory(
                new HashMapJavaTypeCache(new WithoutClassesOf(sources, java.getTypeCache()))));
    }

    @Override
    public Parser.Builder typedParser(Parser.Builder parser) {
        if (!(parser instanceof JavaParser.Builder)) {
            return parser;
        }
        // a cache of its own, of the kind a builder gives its parsers, which reads the JDK classes test parsers share
        JavaTypeCache cache = HashMapJavaTypeCache.forParser(
                JdkClassSharingTypeFactory.cacheForParser(new JavaTypeCache(), parser.getClass(), false), false);
        JavaTypeFactory factory = JdkClassSharingTypeFactory.forParser(cache);
        return ((JavaParser.Builder<?, ?>) parser.clone())
                .typeFactory(factory == null ? new DefaultJavaTypeFactory(cache) : factory);
    }

    /**
     * The types of a cache, but for those whose signature names a class that one of the sources declares.
     */
    private static class WithoutClassesOf extends JavaTypeCache {
        private final JavaTypeCache cache;
        private final @Nullable Pattern classes;

        WithoutClassesOf(List<SourceFile> sources, JavaTypeCache cache) {
            this.cache = cache;
            Set<String> names = new TreeSet<>();
            for (SourceFile source : sources) {
                if (source instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) source;
                    String prefix = cu.getPackageDeclaration() == null ? "" :
                            cu.getPackageDeclaration().getPackageName() + ".";
                    for (J.ClassDeclaration classDecl : cu.getClasses()) {
                        names.add(prefix + classDecl.getSimpleName());
                    }
                }
            }
            // the signature of a nested, local or anonymous class, such as a.A$1, starts with its top-level class's name
            classes = names.isEmpty() ? null : Pattern.compile("(?<![\\w$.])(?:" +
                    names.stream().map(Pattern::quote).collect(joining("|")) + ")(?!\\w)");
        }

        @Override
        public <T> @Nullable T get(String signature) {
            return classes != null && classes.matcher(signature).find() ? null : cache.get(signature);
        }
    }

    @Override
    public List<String> typeDifferences(Tree tree, Tree parsed) {
        Map<String, @Nullable JavaType> parsedTypes = types(parsed);
        List<String> differences = new ArrayList<>();
        types(tree).forEach((of, type) -> {
            JavaType parsedType = parsedTypes.get(of);
            String written = written(type);
            String parsedWritten = written(parsedType);
            // A type the parser can't resolve has nothing to compare with. One the recipe leaves out is
            // TypeValidation's to report.
            if (written != null && parsedWritten != null && !same(type, parsedType)) {
                differences.add("the " + of + " " + written + unwritten(type, parsedType) +
                                " where the parser gives " + parsedWritten + unwritten(parsedType, type));
            }
        });
        return differences;
    }

    private static Map<String, @Nullable JavaType> types(Tree tree) {
        Map<String, @Nullable JavaType> types = new LinkedHashMap<>();
        if (tree instanceof J.VariableDeclarations.NamedVariable) {
            // its type is its variable's
            types.put("variable", ((J.VariableDeclarations.NamedVariable) tree).getVariableType());
            return types;
        }
        if (tree instanceof TypedTree) {
            types.put("type", ((TypedTree) tree).getType());
        }
        if (tree instanceof J.Identifier) {
            types.put("variable", ((J.Identifier) tree).getFieldType());
        } else if (tree instanceof J.MethodInvocation) {
            types.put("method", ((J.MethodInvocation) tree).getMethodType());
        } else if (tree instanceof J.MemberReference) {
            types.put("method", ((J.MemberReference) tree).getMethodType());
            types.put("variable", ((J.MemberReference) tree).getVariableType());
        } else if (tree instanceof J.MethodDeclaration) {
            types.put("method", ((J.MethodDeclaration) tree).getMethodType());
        } else if (tree instanceof J.NewClass) {
            types.put("constructor", ((J.NewClass) tree).getConstructorType());
        }
        return types;
    }

    private static boolean same(@Nullable JavaType type, @Nullable JavaType parsed) {
        if (type instanceof JavaType.Variable && parsed instanceof JavaType.Variable) {
            // A variable's owner isn't compared, as a recipe that changes a method's type leaves its parameters and
            // locals owned by the method's old type.
            JavaType.Variable variable = (JavaType.Variable) type;
            JavaType.Variable parsedVariable = (JavaType.Variable) parsed;
            return variable.getName().equals(parsedVariable.getName()) &&
                   TypeUtils.isOfType(variable.getType(), parsedVariable.getType());
        }
        if (type instanceof JavaType.Method && parsed instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            JavaType.Method parsedMethod = (JavaType.Method) parsed;
            return TypeUtils.isOfType(method.withThrownExceptions(emptyList()),
                    parsedMethod.withThrownExceptions(emptyList())) &&
                   sameThrown(method.getThrownExceptions(), parsedMethod.getThrownExceptions());
        }
        return TypeUtils.isOfType(type, parsed);
    }

    /**
     * An exception that either side doesn't resolve isn't compared, so it doesn't keep the rest of the method from
     * being compared.
     */
    private static boolean sameThrown(List<JavaType> thrown, List<JavaType> parsedThrown) {
        if (thrown.size() != parsedThrown.size()) {
            return false;
        }
        for (int i = 0; i < thrown.size(); i++) {
            if (written(thrown.get(i)) != null && written(parsedThrown.get(i)) != null &&
                !TypeUtils.isOfType(thrown.get(i), parsedThrown.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The flags and thrown exceptions of {@code type}, which {@link #written} leaves out, where they differ from those
     * of {@code other}.
     */
    private static String unwritten(@Nullable JavaType type, @Nullable JavaType other) {
        if (!(type instanceof JavaType.Method) || !(other instanceof JavaType.Method)) {
            return "";
        }
        JavaType.Method method = (JavaType.Method) type;
        JavaType.Method otherMethod = (JavaType.Method) other;
        StringBuilder unwritten = new StringBuilder();
        if (method.getFlagsBitMap() != otherMethod.getFlagsBitMap()) {
            unwritten.append(" with the flags ").append(method.getFlags());
        }
        if (!sameThrown(method.getThrownExceptions(), otherMethod.getThrownExceptions())) {
            StringJoiner thrown = new StringJoiner(", ", " throwing ", "").setEmptyValue(" throwing nothing");
            for (JavaType exception : method.getThrownExceptions()) {
                String written = written(exception);
                thrown.add(written == null ? "{undefined}" : written);
            }
            unwritten.append(thrown);
        }
        return unwritten.toString();
    }

    /**
     * {@code type} as text, without a variable's owner or a method's flags and thrown exceptions, and with a {@code .}
     * before a nested class's name. A type that isn't resolved is null.
     */
    private static @Nullable String written(@Nullable JavaType type) {
        DefaultJavaTypeSignatureBuilder signatures = new DefaultJavaTypeSignatureBuilder();
        String written;
        if (type instanceof JavaType.Variable) {
            JavaType.Variable variable = (JavaType.Variable) type;
            written = signatures.signature(variable.getType()) + " " + variable.getName();
        } else if (type instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            StringJoiner parameters = new StringJoiner(", ", "(", ")");
            for (JavaType parameter : method.getParameterTypes()) {
                parameters.add(signatures.signature(parameter));
            }
            written = signatures.signature(method.getReturnType()) + " " +
                      signatures.signature(method.getDeclaringType()) + "." + method.getName() + parameters;
        } else {
            written = signatures.signature(type);
        }
        return written.contains("{undefined}") ? null : written.replace('$', '.');
    }

    @Override
    public void collectCompilationErrors(ExecutionContext parseCtx) {
        parseCtx.putMessage(JavaParser.COMPILATION_ERRORS, new ArrayList<Diagnostic<? extends JavaFileObject>>());
    }

    @Override
    public Map<Path, Map<String, List<String>>> compilationErrors(ExecutionContext parseCtx) {
        Collection<Diagnostic<? extends JavaFileObject>> errors = parseCtx.getMessage(JavaParser.COMPILATION_ERRORS);
        Map<Path, Map<String, List<String>>> byPath = new LinkedHashMap<>();
        if (errors == null) {
            return byPath;
        }
        Map<JavaFileObject, String[]> lines = new IdentityHashMap<>();
        Set<String> reported = new HashSet<>();
        for (Diagnostic<? extends JavaFileObject> error : errors) {
            JavaFileObject source = error.getSource();
            // an error that javac places in no source isn't about the code the sources hold
            if (source != null) {
                String[] ofSource = lines.computeIfAbsent(source, JavaTreeShapeValidationService::lines);
                long number = error.getLineNumber();
                Path path = Paths.get(source.getName());
                String message = error.getMessage(Locale.ENGLISH).trim().replaceAll("\\s+", " ");
                // javac reports some errors more than once, such as an import of a missing package
                if (reported.add(path + ":" + error.getPosition() + ":" + message)) {
                    byPath.computeIfAbsent(path, p -> new LinkedHashMap<>())
                            .computeIfAbsent(message, m -> new ArrayList<>())
                            .add(number >= 1 && number <= ofSource.length ? ofSource[(int) number - 1].trim() : "");
                }
            }
        }
        return byPath;
    }

    private static String[] lines(JavaFileObject source) {
        try {
            return source.getCharContent(true).toString().split("\\R", -1);
        } catch (IOException e) {
            return new String[0];
        }
    }

    @Override
    public @Nullable String syntaxError(SourceFile sourceFile) {
        return new TreeVisitor<Tree, AtomicReference<@Nullable String>>() {
            @Override
            public Tree preVisit(Tree t, AtomicReference<@Nullable String> text) {
                if (text.get() != null) {
                    stopAfterPreVisit();
                } else if (t instanceof J.Erroneous) {
                    text.set(((J.Erroneous) t).getText());
                    stopAfterPreVisit();
                }
                return t;
            }
        }.reduce(sourceFile, new AtomicReference<>()).get();
    }

    @Override
    public boolean equivalent(Tree tree, Tree parsed) {
        // a type annotation can sit on a declaration, on its type or on a part of the type, and recipes place it
        // where the parser doesn't
        return tree instanceof J && parsed instanceof J && (hasAnnotations(tree) || hasAnnotations(parsed)) &&
               kindsWithoutAnnotations(tree).equals(kindsWithoutAnnotations(parsed));
    }

    private static boolean hasAnnotations(Tree tree) {
        return new TreeVisitor<Tree, AtomicBoolean>() {
            @Override
            public Tree preVisit(Tree t, AtomicBoolean f) {
                if (t instanceof J.Annotation || t instanceof J.AnnotatedType) {
                    f.set(true);
                    stopAfterPreVisit();
                }
                return t;
            }
        }.reduce(tree, new AtomicBoolean()).get();
    }

    /**
     * The kinds of node in a tree in the order they are visited, without annotations and what holds them.
     */
    private static List<Class<?>> kindsWithoutAnnotations(Tree tree) {
        return new TreeVisitor<Tree, List<Class<?>>>() {
            @Override
            public Tree preVisit(Tree t, List<Class<?>> k) {
                if (t instanceof J.Annotation || t instanceof J.Empty || t instanceof Javadoc) {
                    stopAfterPreVisit();
                } else if (!(t instanceof J.AnnotatedType)) {
                    k.add(t.getClass());
                }
                return t;
            }
        }.reduce(tree, new ArrayList<>());
    }
}
