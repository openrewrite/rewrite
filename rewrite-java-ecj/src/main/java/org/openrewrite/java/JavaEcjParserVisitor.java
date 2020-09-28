package org.openrewrite.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Formatting.format;
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

        private J tree;
        private int pos = 0;

        @Nullable
        private <T extends J> T visit(ASTNode node) {
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
                scopes.push(n.scope);
                visit(n, n.scope);
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

        @Nullable
        private <T extends J> List<T> visitAll(@Nullable ASTNode[] nodes) {
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
                mappedPkg = new J.Package(randomId(),
                        identOrFieldAccess(compilationUnitDeclaration.currentPackage.tokens),
                        EMPTY);
            }

            tree = new J.CompilationUnit(
                    randomId(),
                    new String(compilationUnitDeclaration.getFileName()),
                    emptyList(),
                    mappedPkg,
                    visitAll(compilationUnitDeclaration.imports),
                    visitAll(compilationUnitDeclaration.types),
                    EMPTY,
                    emptyList()
            );

            return false;
        }

        @Override
        public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
            diagram(importRef);

            String prefix = sourceBefore(importRef.declarationSourceStart);

            tree = new J.Import(randomId(),
                    (J.FieldAccess) identOrFieldAccess(importRef.tokens),
                    importRef.isStatic(),
                    format(prefix));

            pos = importRef.declarationSourceEnd;
            tree = tree.withSuffix(sourceBefore(";"));

            return false;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
            String prefix = sourceBefore(typeDeclaration.declarationSourceStart);

            Snip kindSnip = snip(typeDeclaration.restrictedIdentifierStart, typeDeclaration.sourceStart);
            J.ClassDecl.Kind kind;
            switch (kindSnip.source) {
                case "class":
                    kind = new J.ClassDecl.Kind.Class(randomId(), kindSnip.formatting);
                    break;
                case "interface":
                    kind = new J.ClassDecl.Kind.Interface(randomId(), kindSnip.formatting);
                    break;
                case "@interface":
                    kind = new J.ClassDecl.Kind.Annotation(randomId(), kindSnip.formatting);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected class kind '" + kindSnip.source + "'");
            }
            pos = typeDeclaration.sourceStart;

            diagram(typeDeclaration);

            J.Ident name = J.Ident.build(randomId(), new String(typeDeclaration.name), null,
                    format(kind.getSuffix()));
            skip(name.getSimpleName());

            List<J.Annotation> annotations = visitAll(typeDeclaration.annotations);
            List<J.Modifier> modifiers = snip(typeDeclaration.modifiersSourceStart, typeDeclaration.restrictedIdentifierStart)
                    .collectTokens(J.Modifier::buildModifier);
            J.TypeParameters typeParameters = typeDeclaration.typeParameters == null ? null : new J.TypeParameters(randomId(),
                    visitAll(typeDeclaration.typeParameters), EMPTY);
            J.ClassDecl.Extends extendings = visit(typeDeclaration.superclass);

            J.ClassDecl.Implements implementings = null;
            if (typeDeclaration.superInterfaces != null) {
                String implementsPrefix = sourceBefore("implements");

                List<TypeTree> superInterfaces = visitAll(typeDeclaration.superInterfaces);
                assert superInterfaces != null;

                for (int i = 0; i < superInterfaces.size(); i++) {
                    TypeTree si = superInterfaces.get(i);
                    si = si.withPrefix(sourceBefore(si.printTrimmed()));
                    if (i < superInterfaces.size() - 1) {
                        si = si.withSuffix(sourceBefore(","));
                    }
                    superInterfaces.set(i, si);
                }

                implementings = new J.ClassDecl.Implements(randomId(),
                        superInterfaces, format(implementsPrefix));
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

            J.Block<J> block = new J.Block<>(
                    randomId(),
                    null,
                    visitAll(blockStatements),
                    format(blockPrefix),
                    new J.Block.End(randomId(), format(sourceBefore("}")))
            );

            visitAll(blockStatements);

            tree = new J.ClassDecl(
                    randomId(),
                    annotations,
                    modifiers,
                    kind.withSuffix(""),
                    name,
                    typeParameters,
                    extendings,
                    implementings,
                    block,
                    TypeUtils.asClass(type(typeDeclaration.binding)),
                    format(prefix)
            );

            pos = typeDeclaration.declarationSourceEnd;

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
            tree = TreeBuilder.buildName(new String(singleTypeReference.token))
                    .withType(type(singleTypeReference.resolvedType))
                    .withPrefix(prefix);
        }

        @Nullable
        private JavaType type(Binding typeBinding) {
            if (typeBinding instanceof MissingTypeBinding || typeBinding instanceof ProblemBinding) {
                return null;
            } else if (typeBinding instanceof BinaryTypeBinding) {
                BinaryTypeBinding t = (BinaryTypeBinding) typeBinding;
                return JavaType.Class.build(
                        stream(t.compoundName).map(String::new).collect(joining(".")),
                        stream(t.fields()).map(this::type)
                                .map(JavaType.Var.class::cast)
                                .collect(toList()),
                        stream(t.typeVariables()).map(this::type).collect(toList()),
                        stream(t.superInterfaces()).map(this::type).collect(toList()),
                        stream(t.methods())
                                .filter(MethodBinding::isConstructor)
                                .map(this::type)
                                .map(JavaType.Method.class::cast)
                                .collect(toList()),
                        TypeUtils.asClass(type(t.superclass())),
                        false
                );
            } else if (typeBinding instanceof SourceTypeBinding) {
                SourceTypeBinding t = (SourceTypeBinding) typeBinding;
                return JavaType.Class.build(
                        stream(t.compoundName).map(String::new).collect(joining(".")),
                        stream(t.fields()).map(this::type)
                                .map(JavaType.Var.class::cast)
                                .collect(toList()),
                        stream(t.typeVariables()).map(this::type).collect(toList()),
                        stream(t.superInterfaces()).map(this::type).collect(toList()),
                        stream(t.methods())
                                .filter(MethodBinding::isConstructor)
                                .map(this::type)
                                .map(JavaType.Method.class::cast)
                                .collect(toList()),
                        TypeUtils.asClass(type(t.superclass())),
                        false
                );
            } else if (typeBinding instanceof TypeVariableBinding) {
                TypeVariableBinding t = (TypeVariableBinding) typeBinding;
                return new JavaType.GenericTypeVariable(
                        stream(t.compoundName).map(String::new).collect(joining(".")),
                        TypeUtils.asClass(type(t.firstBound))
                );
            } else if (typeBinding instanceof ReferenceBinding) {
                return JavaType.Class.build(
                        stream(((ReferenceBinding) typeBinding).compoundName)
                                .map(String::new)
                                .collect(joining("."))
                );
            } else if (typeBinding instanceof VariableBinding) {
                VariableBinding t = (VariableBinding) typeBinding;
                return new JavaType.Var(new String(t.name), type(t.type), t instanceof FieldBinding ?
                        flags(((FieldBinding) t).getAccessFlags()) : emptySet());
            } else if (typeBinding instanceof MethodBinding) {
                MethodBinding t = (MethodBinding) typeBinding;

                return JavaType.Method.build(
                        TypeUtils.asClass(type(t.receiver)),
                        new String(t.selector),
                        new JavaType.Method.Signature(
                                type(t.genericMethod().returnType),
                                stream(t.genericMethod().parameters).map(this::type).collect(toList())
                        ),
                        new JavaType.Method.Signature(
                                type(t.returnType),
                                stream(t.parameters).map(this::type).collect(toList())
                        ),
                        stream(t.parameterNames).map(String::new).collect(toList()),
                        flags(t.getAccessFlags())
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
                            min.set(Math.min(min.get(), pos));
                            max.set(Math.max(max.get(), pos));
                            logger.info("{}: {}", f.getName(), pos);
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

        private Expression identOrFieldAccess(char[][] names) {
            if (names.length == 0) {
                return null;
            }

            Expression fa = J.Ident.build(randomId(), new String(names[0]), null, EMPTY);
            for (int i = 1; i < names.length; i++) {
                fa = new J.FieldAccess(randomId(), fa,
                        J.Ident.build(randomId(), new String(names[i]), null, EMPTY),
                        null, EMPTY);
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
            Formatting formatting = Formatting.EMPTY;

            Snip(String source) {
                this.source = source.trim();

                StringBuilder format = new StringBuilder();
                char[] chars = source.toCharArray();
                for (char c : chars) {
                    if (!Character.isWhitespace(c)) {
                        formatting = formatting.withPrefix(format.toString());
                        break;
                    }
                    format.append(c);
                }

                format = new StringBuilder();
                for (int i = chars.length - 1; i >= 0; i--) {
                    char c = chars[i];
                    if (!Character.isWhitespace(c)) {
                        formatting = formatting.withSuffix(format.reverse().toString());
                        break;
                    }
                    format.append(c);
                }
            }

            <T> T map(BiFunction<String, Formatting, T> map) {
                return map.apply(source, formatting);
            }

            Stream<Snip> tokenStream() {
                return stream(source.split("((?<=\\s)|(?=\\s+))")).map(Snip::new);
            }

            <T> List<T> collectTokens(BiFunction<String, Formatting, T> map) {
                return tokenStream()
                        .map(snip -> map.apply(snip.source, snip.formatting))
                        .collect(toList());
            }
        }
    }
}
