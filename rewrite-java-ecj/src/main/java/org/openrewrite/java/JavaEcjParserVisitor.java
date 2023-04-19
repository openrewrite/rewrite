/*
 * Copyright 2023 the original author or authors.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

class JavaEcjParserVisitor {

    private static final Logger logger = LogManager.getLogger(JavaEcjParserVisitor.class);

    private final Visitor visitor = new Visitor();

    private final String source;

    JavaEcjParserVisitor(CompilationUnit source) {
        this.source = new String(source.contents);
    }

    public J.CompilationUnit visit(CompilationUnitDeclaration declaration) {
        return visitor.visit(declaration);
    }

    private class Visitor extends ASTVisitor {
        private final Stack<Scope> scopes = new Stack<>();

        private Object tree;
        private int pos = 0;

        private <T> T visit(ASTNode node) {
            tree = null;

            if (node instanceof CompilationUnitDeclaration) {
                CompilationUnitDeclaration n = (CompilationUnitDeclaration) node;
                scopes.push(n.scope);
                visit(n, n.scope);
                scopes.pop();
            } else if (node instanceof ImportReference) {
                ImportReference n = (ImportReference) node;
                visit(n, (CompilationUnitScope) scopes.peek());
            } else if (node instanceof TypeDeclaration) {
                TypeDeclaration n = (TypeDeclaration) node;
                Scope localScope = scopes.peek();
                scopes.push(n.scope);
                if (localScope instanceof CompilationUnitScope) {
                    visit(n, (CompilationUnitScope) localScope);
                } else if (localScope instanceof ClassScope) {
                    visit(n, (ClassScope) localScope);
                } else if (localScope instanceof BlockScope) {
                    visit(n, (BlockScope) localScope);
                } else {
                    throw new IllegalStateException("Invalid scope for TypeDeclaration: " + localScope.getClass().getName());
                }
                scopes.pop();
            } else if (node instanceof SingleTypeReference) {
                SingleTypeReference n = (SingleTypeReference) node;
                Scope scope = scopes.peek();
                if (scope instanceof BlockScope) {
                    visit(n, (BlockScope) scope);
                } else if (scope instanceof ClassScope) {
                    visit(n, (ClassScope) scope);
                }
            }

            //noinspection unchecked
            return (T) tree;
        }

        private <T> List<T> visitAll(ASTNode[] nodes) {
            if (nodes == null) {
                return null;
            }

            //noinspection unchecked
            return stream(nodes).map(this::visit)
                    .map(j -> (T) j)
                    .collect(toList());
        }

        @Override
        public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration,
                             CompilationUnitScope scope) {
            J.Package mappedPkg = null;

            ImportReference pkg = compilationUnitDeclaration.currentPackage;
            if (pkg != null) {
                Snip pkgSnip = snip(pkg.declarationSourceStart, pkg.sourceStart);
                mappedPkg = new J.Package(randomId(),
                        pkgSnip.prefix,
                        Markers.EMPTY,
                        identOrFieldAccess(pkg.tokens, pkgSnip.suffix),
                        emptyList());
            }

            tree = new J.CompilationUnit(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Paths.get(new String(compilationUnitDeclaration.getFileName())),
                    null,
                    null,
                    false,
                    null,
                    mappedPkg == null ? null : JRightPadded.build(mappedPkg),
                    visitAll(compilationUnitDeclaration.imports),
                    visitAll(compilationUnitDeclaration.types).stream().map(J.ClassDeclaration.class::cast).collect(toList()),
                    Space.format(sourceBefore(source.length()))
            );

//            tree = new J.CompilationUnit(
//                    randomId(),
//                    Space.EMPTY,
//                    Markers.EMPTY,
//                    new String(compilationUnitDeclaration.getFileName()),
//                    emptyList(),
//                    JRightPadded.build(mappedPkg),
//                    visitAll(compilationUnitDeclaration.imports),
//                    visitAll(compilationUnitDeclaration.types),
//                    emptyList(),
//                    Space.EMPTY
//            );

            return false;
        }

        @Override
        public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
            diagram(importRef);

            String prefix = sourceBefore(importRef.declarationSourceStart);

            Snip importKeyWordSnippet = snip(importRef.declarationSourceStart, importRef.sourceStart);
            J.Import imp = new J.Import(randomId(), Space.format(prefix), Markers.EMPTY, JLeftPadded.build(importRef.isStatic()), (J.FieldAccess) identOrFieldAccess(importRef.tokens, importKeyWordSnippet.suffix), null);

            pos = importRef.sourceEnd + 1;
            tree = JRightPadded.build(imp).withAfter(Space.format(sourceBefore(importRef.declarationSourceEnd)));

            pos = importRef.declarationSourceEnd + 1;
            return false;
        }

        private int findEndingPositionOfLastIdentifier(TypeDeclaration typeDeclaration) {
            int start = typeDeclaration.declarationSourceStart;
            int end = start;
            for (int i = 0; i < typeDeclaration.modifiers; i++) {
                for (; Character.isWhitespace(source.charAt(end)) && end < typeDeclaration.sourceStart; end++) {}
                for (; Character.isJavaIdentifierPart(source.charAt(end)) && end < typeDeclaration.sourceStart; end++) {}
            }
            return end;
        }

        private J.ClassDeclaration visitTypeDeclaration(TypeDeclaration typeDeclaration) {
            String prefix = sourceBefore(typeDeclaration.declarationSourceStart);

            int endOfIdentifiers = findEndingPositionOfLastIdentifier(typeDeclaration);

            List<J.Modifier> modifiers = snip(typeDeclaration.declarationSourceStart, endOfIdentifiers)
                    //TODO: handling for annotations over modifiers?
                    .collectTokens((ident, p) -> new J.Modifier(randomId(), p, Markers.EMPTY, J.Modifier.Type.valueOf(StringUtils.capitalize(ident)), Collections.emptyList()));

            Snip kindSnip = snip(endOfIdentifiers, typeDeclaration.sourceStart);
            J.ClassDeclaration.Kind kind;
            switch (kindSnip.source) {
                case "class":
                    kind = new J.ClassDeclaration.Kind(randomId(), kindSnip.prefix, Markers.EMPTY, Collections.emptyList(), J.ClassDeclaration.Kind.Type.Class);
                    break;
                case "interface":
                    kind = new J.ClassDeclaration.Kind(randomId(), kindSnip.prefix, Markers.EMPTY, Collections.emptyList(), J.ClassDeclaration.Kind.Type.Interface);
                    break;
                case "@interface":
                    kind = new J.ClassDeclaration.Kind(randomId(), kindSnip.prefix, Markers.EMPTY, Collections.emptyList(), J.ClassDeclaration.Kind.Type.Annotation);
                    break;
                case "enum":
                    kind = new J.ClassDeclaration.Kind(randomId(), kindSnip.prefix, Markers.EMPTY, Collections.emptyList(), J.ClassDeclaration.Kind.Type.Enum);
                    break;
                case "record":
                    kind = new J.ClassDeclaration.Kind(randomId(), kindSnip.prefix, Markers.EMPTY, Collections.emptyList(), J.ClassDeclaration.Kind.Type.Record);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected class kind '" + kindSnip.source + "'");
            }
            pos = endOfIdentifiers + 1;

            diagram(typeDeclaration);

            // TODO: specify type for the identifier!
            J.Identifier name = new J.Identifier(randomId(), kindSnip.suffix, Markers.EMPTY, new String(typeDeclaration.name), null, null);
            skip(name.getSimpleName());

            List<J.Annotation> annotations = visitAll(typeDeclaration.annotations);
            //TODO: white space prefix for type parameters
            JContainer<J.TypeParameter> typeParameters = typeDeclaration.typeParameters == null ? null : JContainer.build(Space.EMPTY, visitAll(typeDeclaration.typeParameters), Markers.EMPTY);
            JLeftPadded<TypeTree> extendings = null;
            if (typeDeclaration.superclass != null) {
                String extendsPrefix = sourceBefore("extends");
                extendings = JLeftPadded.build((TypeTree)visit(typeDeclaration.superclass)).withBefore(Space.format(extendsPrefix));
            }

            JContainer<TypeTree> implementings = null;
            if (typeDeclaration.superInterfaces != null) {
                String implementsPrefix = sourceBefore("implements");

                List<TypeTree> superInterfaces = visitAll(typeDeclaration.superInterfaces);
                List<JRightPadded<TypeTree>> rightPaddedInterfaces = new ArrayList<>();

                for (int i = 0; i < superInterfaces.size(); i++) {
                    TypeTree si = superInterfaces.get(i);
                    JRightPadded<TypeTree> rightPaddedInterface = JRightPadded.build(si);
                    if (i < superInterfaces.size() - 1) {
                        rightPaddedInterface = rightPaddedInterface.withAfter(Space.format(sourceBefore(",")));
                    }
                    rightPaddedInterfaces.add(rightPaddedInterface);
                }

                implementings = JContainer.build(Space.format(implementsPrefix), rightPaddedInterfaces, Markers.EMPTY);
            }

            String blockPrefix = sourceBefore("{");

            ASTNode[] blockStatements = Stream.concat(
                    typeDeclaration.fields == null ? Stream.empty() : stream(typeDeclaration.fields),
                    Stream.concat(
                            typeDeclaration.methods == null ? Stream.empty() : stream(typeDeclaration.methods)
                                    .filter(m -> !m.isDefaultConstructor()),
                            typeDeclaration.memberTypes == null ? Stream.empty() : stream(typeDeclaration.memberTypes)
                    )
            ).sorted(Comparator.comparing(node -> node.sourceStart)).toArray(ASTNode[]::new);

            J.Block block = new J.Block(randomId(), Space.format(blockPrefix), Markers.EMPTY, JRightPadded.build(false), visitAll(blockStatements), Space.format(sourceBefore("}")));

            pos = typeDeclaration.declarationSourceEnd + 1;

            J.ClassDeclaration classDecl = new J.ClassDeclaration(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    annotations,
                    modifiers,
                    kind,
                    name,
                    typeParameters,
                    null, // TODO: primary constructor
                    extendings,
                    implementings,
                    null, // TODO: permitting?
                    block,
                    TypeUtils.asClass(type(typeDeclaration.binding))
            );

            return classDecl;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
            J.ClassDeclaration classDecl = visitTypeDeclaration(typeDeclaration);
            tree = classDecl;
            return false;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
            Object classDecl = visitTypeDeclaration(typeDeclaration);
            tree = JRightPadded.build(classDecl);
            return false;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
            Object classDecl = visitTypeDeclaration(typeDeclaration);
            tree = JRightPadded.build(classDecl);
            return false;
        }

        public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
            visit(singleTypeReference);
            return false;
        }

        public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
            visit(singleTypeReference);
            return false;
        }

        private void visit(SingleTypeReference singleTypeReference) {
            if (singleTypeReference.annotations != null) {
                // TODO implement me
            }

            String prefix = sourceBefore(singleTypeReference.sourceStart);


            tree = TypeTree.build(prefix + new String(singleTypeReference.token)).withType(type(singleTypeReference.resolvedType));
//            tree = TreeBuilder.buildName(new String(singleTypeReference.token))
//                    .withType()
//                    .withPrefix(prefix);
            pos = singleTypeReference.sourceEnd + 1;
        }

        private JavaType type(Binding typeBinding) {
            if (typeBinding instanceof MissingTypeBinding || typeBinding instanceof ProblemBinding) {
                return null;
            } else if (typeBinding instanceof BinaryTypeBinding) {
                BinaryTypeBinding t = (BinaryTypeBinding) typeBinding;
                // TODO: annotations? constructors?
                return JavaType.ShallowClass.build(stream(t.compoundName).map(String::new).collect(joining(".")))
                        .withMembers(stream(t.fields()).map(this::type)
                                .map(JavaType.Variable.class::cast)
                                .collect(toList()))
                        .withTypeParameters(stream(t.typeVariables()).map(this::type).collect(toList()))
                        .withInterfaces(stream(t.superInterfaces()).map(this::type).map(JavaType.FullyQualified.class::cast).collect(toList()))
                        .withMethods(stream(t.methods())
                                .filter(MethodBinding::isConstructor)
                                .map(this::type)
                                .map(JavaType.Method.class::cast)
                                .collect(toList()))
                        .withSupertype(TypeUtils.asClass(type(t.superclass())))
                        .withFlags(Flag.bitMapToFlags(((BinaryTypeBinding) typeBinding).modifiers));
            } else if (typeBinding instanceof SourceTypeBinding) {
                SourceTypeBinding t = (SourceTypeBinding) typeBinding;
                // TODO: annotations? constructors
                return JavaType.ShallowClass.build(stream(t.compoundName).map(String::new).collect(joining(".")))
                        .withMembers(stream(t.fields()).map(this::type)
                                .map(JavaType.Variable.class::cast)
                                .collect(toList()))
                        .withTypeParameters(stream(t.typeVariables()).map(this::type).collect(toList()))
                        .withInterfaces(stream(t.superInterfaces()).map(this::type).map(JavaType.FullyQualified.class::cast).collect(toList()))
                        .withMethods(stream(t.methods())
                                .filter(MethodBinding::isConstructor)
                                .map(this::type)
                                .map(JavaType.Method.class::cast)
                                .collect(toList()))
                        .withSupertype(TypeUtils.asClass(type(t.superclass())))
                        .withFlags(Flag.bitMapToFlags(((SourceTypeBinding) typeBinding).modifiers));
            } else if (typeBinding instanceof TypeVariableBinding) {
                TypeVariableBinding t = (TypeVariableBinding) typeBinding;
                // TODO: isn't there more boundary types in the list? Variance is hardcoded at the moment
                JavaType.Class boundType = TypeUtils.asClass(type(t.firstBound));
                //TODO: no support for COVARINAT ( extends Something) and CONTRAVARIANT (super Something)
                return new JavaType.GenericTypeVariable(
                        null,
                        stream(t.compoundName).map(String::new).collect(joining(".")),
                        JavaType.GenericTypeVariable.Variance.INVARIANT,
                        boundType == null ? Collections.emptyList() : Collections.singletonList(boundType)
                );
            } else if (typeBinding instanceof ReferenceBinding) {
                return JavaType.ShallowClass.build(stream(((ReferenceBinding) typeBinding).compoundName)
                        .map(String::new)
                        .collect(joining(".")));
//                return JavaType.Class.build(
//                        stream(((ReferenceBinding) typeBinding).compoundName)
//                                .map(String::new)
//                                .collect(joining("."))
//                );
            } else if (typeBinding instanceof VariableBinding) {
                VariableBinding t = (VariableBinding) typeBinding;
                List<JavaType.FullyQualified> annotations = stream(t.getAnnotations()).map(an -> (JavaType.FullyQualified) type(an.getAnnotationType())).collect(toList());
                return new JavaType.Variable(null, t instanceof FieldBinding ? ((FieldBinding) t).getAccessFlags() : 0, new String(t.name), t instanceof FieldBinding ? type(((FieldBinding) t).declaringClass) : null, type(t.type), annotations);
            } else if (typeBinding instanceof MethodBinding) {
                MethodBinding t = (MethodBinding) typeBinding;

                return new JavaType.Method(
                        null,
                        t.getAccessFlags(),
                        TypeUtils.asClass(type(t.receiver)),
                        new String(t.selector),
                        type(t.genericMethod().returnType),
                        stream(t.parameterNames).map(String::new).collect(toList()),
                        stream(t.genericMethod().parameters).map(this::type).collect(toList()),
                        stream(t.thrownExceptions).map(this::type).map(TypeUtils::asFullyQualified).collect(toList()),
                        stream(t.getAnnotations()).map(an -> (JavaType.FullyQualified) type(an.getAnnotationType())).collect(toList()),
                        null //TODO: default value
                );
            }

            return null;
        }

        private Set<Flag> flags(int accessFlags) {
            Set<Flag> flags = new HashSet<>();
            if ((accessFlags & ClassFileConstants.AccPrivate) != 0) {
                flags.add(Flag.Private);
            }
            if ((accessFlags & ClassFileConstants.AccProtected) != 0) {
                flags.add(Flag.Protected);
            }
            if ((accessFlags & ClassFileConstants.AccPublic) != 0) {
                flags.add(Flag.Public);
            }
            if ((accessFlags & ClassFileConstants.AccAbstract) != 0) {
                flags.add(Flag.Abstract);
            }
            if ((accessFlags & ClassFileConstants.AccFinal) != 0) {
                flags.add(Flag.Final);
            }
            if ((accessFlags & ClassFileConstants.AccStatic) != 0) {
                flags.add(Flag.Static);
            }
            if ((accessFlags & ClassFileConstants.AccSynchronized) != 0) {
                flags.add(Flag.Synchronized);
            }
            if ((accessFlags & ClassFileConstants.AccTransient) != 0) {
                flags.add(Flag.Transient);
            }
            if ((accessFlags & ClassFileConstants.AccVolatile) != 0) {
                flags.add(Flag.Volatile);
            }

            return flags;
        }

        /**
         * A simple debugging utility for understanding where ECJ is putting start/end position indices.
         */
        private void diagram(ASTNode node) {
            logger.info("---------------");
            logger.info("current position: {}", pos);

            AtomicInteger min = new AtomicInteger(Integer.MAX_VALUE);
            AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);

            stream(node.getClass().getDeclaredFields())
                    .filter(f -> f.getName().contains("Start") || f.getName().contains("End"))
                    .sorted(Comparator.comparing(Field::getName))
                    .sorted(Comparator.comparing(f -> {
                        try {
                            return (int) f.get(node);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .forEachOrdered(f -> {
                        try {
                            int pos = (int) f.get(node);
                            if (pos >= 0) {
                                min.set(Math.min(min.get(), pos));
                                max.set(Math.max(max.get(), pos));
                                logger.info("{}: {}", f.getName(), pos);
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });

            String nodeSource = source.substring(min.get(), max.get());

            StringBuilder positions = new StringBuilder();
            StringBuilder chars = new StringBuilder();

            int i = min.get();
            for (char c : nodeSource.toCharArray()) {
                chars.append(c);
                if (i >= 10_000) {
                    chars.append("    |");
                } else if (i >= 1_000) {
                    chars.append("   |");
                } else if (i >= 100) {
                    chars.append("  |");
                } else if (i >= 10) {
                    chars.append(" |");
                } else {
                    chars.append("|");
                }
                positions.append(i++).append('|');
            }

            logger.info(positions.toString());
            logger.info(chars.toString());
        }

        private Expression identOrFieldAccess(char[][] names, Space prefix) {
            if (names.length == 0) {
                return null;
            }

            // TODO: prefix?
            Expression fa = new J.Identifier(randomId(), prefix, Markers.EMPTY, new String(names[0]), null, null);
            for (int i = 1; i < names.length; i++) {
                //TODO: type, prefix?
                fa = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, fa,
                        JLeftPadded.build(new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, new String(names[i]), null, null)),
                        null);
            }
            return fa;
        }

        private String sourceBefore(String delimiter) {
            int endIndex = source.indexOf(delimiter, pos);
            String snip = source.substring(pos, endIndex);
            pos = endIndex + delimiter.length();
            return snip;
        }

        private void skip(String delimiter) {
            int endIndex = source.indexOf(delimiter, pos);
            pos = endIndex + delimiter.length();
        }

        private String sourceBefore(int declarationSourceStart) {
            String snip = source.substring(pos, declarationSourceStart);
            pos = declarationSourceStart;
            return snip;
        }

        private Snip snip(int start, int end) {
            return new Snip(source.substring(start, end));
        }

        private class Snip {
            String source;
            Space prefix = Space.EMPTY;
            Space suffix = Space.EMPTY;

            Snip(String source) {
                this.source = source.trim();

                StringBuilder format = new StringBuilder();
                char[] chars = source.toCharArray();
                for (char c : chars) {
                    if (!Character.isWhitespace(c)) {
                        prefix = prefix.withWhitespace(format.toString());
                        break;
                    }
                    format.append(c);
                }

                format = new StringBuilder();
                for (int i = chars.length - 1; i >= 0; i--) {
                    char c = chars[i];
                    if (!Character.isWhitespace(c)) {
                        suffix = suffix.withWhitespace(format.reverse().toString());
                        break;
                    }
                    format.append(c);
                }
            }

//            <T> T map(BiFunction<String, Formatting, T> map) {
//                return map.apply(source, formatting);
//            }

            Stream<Snip> tokenStream() {
                return stream(source.split("((?<=\\s)|(?=\\s+))")).map(Snip::new);
            }

            <T> List<T> collectTokens(BiFunction<String, Space, T> map) {
                return tokenStream()
                        .filter(snip -> !snip.source.isEmpty() || !snip.prefix.isEmpty() || !snip.suffix.isEmpty())
                        .map(snip -> map.apply(snip.source, snip.prefix))
                        .collect(toList());
            }
        }
    }
}