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
package org.openrewrite.java.isolated;


import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Context;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParsingException;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * Maps the compiler internal AST to the Rewrite {@link J} AST.
 * <p>
 * This visitor is not thread safe, as it maintains a {@link #cursor} and {@link #endPosTable}
 * for each compilation unit visited.
 */
public class ReloadableJava11ParserVisitor extends TreePathScanner<J, Space> {
    private final static int SURR_FIRST = 0xD800;
    private final static int SURR_LAST = 0xDFFF;

    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final Collection<NamedStyles> styles;
    private final ExecutionContext ctx;
    private final Context context;
    private final ReloadableJava11TypeMapping typeMapping;

    @SuppressWarnings("NotNullFieldNotInitialized")
    private EndPosTable endPosTable;

    @SuppressWarnings("NotNullFieldNotInitialized")
    private DocCommentTable docCommentTable;

    private int cursor = 0;

    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public ReloadableJava11ParserVisitor(Path sourcePath,
                                         @Nullable FileAttributes fileAttributes,
                                         EncodingDetectingInputStream source,
                                         Collection<NamedStyles> styles,
                                         JavaTypeCache typeCache,
                                         ExecutionContext ctx,
                                         Context context) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
        this.styles = styles;
        this.ctx = ctx;
        this.context = context;
        this.typeMapping = new ReloadableJava11TypeMapping(typeCache);
    }

    @Override
    public J visitAnnotation(AnnotationTree node, Space fmt) {
        skip("@");
        NameTree name = convert(node.getAnnotationType());

        JContainer<Expression> args = null;
        if (!node.getArguments().isEmpty()) {
            Space argsPrefix = sourceBefore("(");
            List<JRightPadded<Expression>> expressions;
            if (node.getArguments().size() == 1) {
                ExpressionTree arg = node.getArguments().get(0);
                if (arg instanceof JCAssign) {
                    if (endPos(arg) < 0) {
                        expressions = singletonList(convert(((JCAssign) arg).rhs, t -> sourceBefore(")")));
                    } else {
                        expressions = singletonList(convert(arg, t -> sourceBefore(")")));
                    }
                } else {
                    expressions = singletonList(convert(arg, t -> sourceBefore(")")));
                }
            } else {
                expressions = convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")"));
            }

            args = JContainer.build(argsPrefix, expressions, Markers.EMPTY);
        } else {
            int saveCursor = cursor;
            Space prefix = whitespace();
            if (source.charAt(cursor) == '(') {
                skip("(");
                args = JContainer.build(
                        prefix,
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)),
                        Markers.EMPTY
                );
            } else {
                cursor = saveCursor;
            }
        }

        return new J.Annotation(randomId(), fmt, Markers.EMPTY, name, args);
    }

    @Override
    public J visitArrayAccess(ArrayAccessTree node, Space fmt) {
        return new J.ArrayAccess(
                randomId(),
                fmt,
                Markers.EMPTY,
                convert(node.getExpression()),
                new J.ArrayDimension(randomId(), sourceBefore("["), Markers.EMPTY,
                        convert(node.getIndex(), t -> sourceBefore("]"))),
                typeMapping.type(node)
        );
    }

    @Override
    public J visitArrayType(ArrayTypeTree node, Space fmt) {
        return arrayTypeTree(node, new HashMap<>()).withPrefix(fmt);
    }

    @Override
    public J visitAssert(AssertTree node, Space fmt) {
        skip("assert");
        JCAssert jcAssert = (JCAssert) node;
        return new J.Assert(randomId(), fmt, Markers.EMPTY,
                convert(jcAssert.cond),
                jcAssert.detail == null ? null : padLeft(sourceBefore(":"), convert(jcAssert.detail)));
    }

    @Override
    public J visitAssignment(AssignmentTree node, Space fmt) {
        return new J.Assignment(randomId(), fmt, Markers.EMPTY,
                convert(node.getVariable()),
                padLeft(sourceBefore("="), convert(node.getExpression())),
                typeMapping.type(node));
    }

    @Override
    public J visitErroneous(ErroneousTree node, Space fmt) {
        String erroneousNode = source.substring(((JCTree) node).getStartPosition(), ((JCTree) node).getEndPosition(endPosTable));
        return new J.Erroneous(
                randomId(),
                fmt,
                Markers.EMPTY,
                erroneousNode);
    }

    @Override
    public J visitBinary(BinaryTree node, Space fmt) {
        Expression left = convert(node.getLeftOperand());

        Space opPrefix = whitespace();
        J.Binary.Type op;
        switch (((JCBinary) node).getTag()) {
            case PLUS:
                skip("+");
                op = J.Binary.Type.Addition;
                break;
            case MINUS:
                skip("-");
                op = J.Binary.Type.Subtraction;
                break;
            case DIV:
                skip("/");
                op = J.Binary.Type.Division;
                break;
            case MUL:
                skip("*");
                op = J.Binary.Type.Multiplication;
                break;
            case MOD:
                skip("%");
                op = J.Binary.Type.Modulo;
                break;
            case AND:
                skip("&&");
                op = J.Binary.Type.And;
                break;
            case OR:
                skip("||");
                op = J.Binary.Type.Or;
                break;
            case BITAND:
                skip("&");
                op = J.Binary.Type.BitAnd;
                break;
            case BITOR:
                skip("|");
                op = J.Binary.Type.BitOr;
                break;
            case BITXOR:
                skip("^");
                op = J.Binary.Type.BitXor;
                break;
            case SL:
                skip("<<");
                op = J.Binary.Type.LeftShift;
                break;
            case SR:
                skip(">>");
                op = J.Binary.Type.RightShift;
                break;
            case USR:
                skip(">>>");
                op = J.Binary.Type.UnsignedRightShift;
                break;
            case LT:
                skip("<");
                op = J.Binary.Type.LessThan;
                break;
            case GT:
                skip(">");
                op = J.Binary.Type.GreaterThan;
                break;
            case LE:
                skip("<=");
                op = J.Binary.Type.LessThanOrEqual;
                break;
            case GE:
                skip(">=");
                op = J.Binary.Type.GreaterThanOrEqual;
                break;
            case EQ:
                skip("==");
                op = J.Binary.Type.Equal;
                break;
            case NE:
                skip("!=");
                op = J.Binary.Type.NotEqual;
                break;
            default:
                throw new IllegalArgumentException("Unexpected binary tag " + ((JCBinary) node).getTag());
        }

        return new J.Binary(randomId(), fmt, Markers.EMPTY, left, padLeft(opPrefix, op),
                convert(node.getRightOperand()), typeMapping.type(node));
    }

    @Override
    public J visitBlock(BlockTree node, Space fmt) {
        JRightPadded<Boolean> stat;

        if ((((JCBlock) node).flags & (long) Flags.STATIC) != 0L) {
            skip("static");
            stat = new JRightPadded<>(true, sourceBefore("{"), Markers.EMPTY);
        } else {
            skip("{");
            stat = new JRightPadded<>(false, EMPTY, Markers.EMPTY);
        }

        // filter out synthetic super() invocations and the like
        List<StatementTree> statementTrees = new ArrayList<>(node.getStatements().size());
        for (StatementTree s : node.getStatements()) {
            if (endPos(s) > 0) {
                statementTrees.add(s);
            }
        }

        return new J.Block(randomId(), fmt, Markers.EMPTY,
                stat,
                convertStatements(statementTrees),
                sourceBefore("}"));
    }

    @Override
    public J visitBreak(BreakTree node, Space fmt) {
        skip("break");

        J.Identifier label = node.getLabel() == null ? null : new J.Identifier(randomId(),
                sourceBefore(node.getLabel().toString()), Markers.EMPTY,
                emptyList(), node.getLabel().toString(), null, null);

        return new J.Break(randomId(), fmt, Markers.EMPTY, label);
    }

    @Override
    public J visitCase(CaseTree node, Space fmt) {
        return new J.Case(randomId(), fmt, Markers.EMPTY,
                J.Case.Type.Statement,
                null,
                JContainer.build(
                        node.getExpression() == null ? EMPTY : sourceBefore("case"),
                        singletonList(node.getExpression() == null ?
                                JRightPadded.build(new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), skip("default"), null, null)) :
                                JRightPadded.build(convert(node.getExpression()))
                        ),
                        Markers.EMPTY
                ),
                null,
                null,
                JContainer.build(sourceBefore(":"), convertStatements(node.getStatements()), Markers.EMPTY),
                null
        );
    }

    @Override
    public J visitCatch(CatchTree node, Space fmt) {
        skip("catch");

        Space paramPrefix = sourceBefore("(");
        J.VariableDeclarations paramDecl = convert(node.getParameter());

        J.ControlParentheses<J.VariableDeclarations> param = new J.ControlParentheses<>(randomId(), paramPrefix,
                Markers.EMPTY, padRight(paramDecl, sourceBefore(")")));

        return new J.Try.Catch(randomId(), fmt, Markers.EMPTY, param, convert(node.getBlock()));
    }

    @Override
    public J visitClass(ClassTree node, Space fmt) {
        Map<Integer, JCAnnotation> annotationPosTable = mapAnnotations(node.getModifiers().getAnnotations(),
                new HashMap<>(node.getModifiers().getAnnotations().size()));
        ReloadableJava11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

        List<J.Annotation> kindAnnotations = collectAnnotations(annotationPosTable);

        J.ClassDeclaration.Kind kind;
        if (hasFlag(node.getModifiers(), Flags.ENUM)) {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("enum"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Enum);
        } else if (hasFlag(node.getModifiers(), Flags.ANNOTATION)) {
            // note that annotations ALSO have the INTERFACE flag
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("@interface"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Annotation);
        } else if (hasFlag(node.getModifiers(), Flags.INTERFACE)) {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("interface"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Interface);
        } else {
            kind = new J.ClassDeclaration.Kind(randomId(), sourceBefore("class"), Markers.EMPTY, kindAnnotations, J.ClassDeclaration.Kind.Type.Class);
        }

        J.Identifier name = new J.Identifier(randomId(), sourceBefore(node.getSimpleName().toString()),
                Markers.EMPTY, emptyList(), ((JCClassDecl) node).getSimpleName().toString(), typeMapping.type(node), null);

        JContainer<J.TypeParameter> typeParams = node.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")),
                Markers.EMPTY);

        JLeftPadded<TypeTree> extendings = node.getExtendsClause() == null ? null :
                padLeft(sourceBefore("extends"), convert(node.getExtendsClause()));

        JContainer<TypeTree> implementings = null;
        if (node.getImplementsClause() != null && !node.getImplementsClause().isEmpty()) {
            Space implementsPrefix = sourceBefore(kind.getType() == J.ClassDeclaration.Kind.Type.Interface ?
                    "extends" : "implements");

            implementings = JContainer.build(
                    implementsPrefix,
                    convertAll(node.getImplementsClause(), commaDelim, noDelim),
                    Markers.EMPTY
            );
        }

        Space bodyPrefix = sourceBefore("{");

        // enum values are required by the grammar to occur before any ordinary field, constructor, or method members
        List<Tree> jcEnums = new ArrayList<>(node.getMembers().size());
        for (Tree tree : node.getMembers()) {
            if (tree instanceof JCVariableDecl) {
                if (hasFlag(((JCVariableDecl) tree).getModifiers(), Flags.ENUM)) {
                    jcEnums.add(tree);
                }
            }
        }

        JRightPadded<Statement> enumSet = null;
        if (!jcEnums.isEmpty()) {
            Tree lastConstant = jcEnums.get(jcEnums.size() - 1);
            List<JRightPadded<J.EnumValue>> enumValues = convertAll(jcEnums, commaDelim, t -> {
                if (t != lastConstant) {
                    return whitespace();
                }
                int savedCursor = cursor;
                Space suffix = whitespace();
                if (source.charAt(cursor) == ',' || source.charAt(cursor) == ';') {
                    return suffix;
                }
                // Whitespace should be assigned to prefix of next statement or `J.Block#end`
                cursor = savedCursor;
                return EMPTY;
            }, t -> {
                if (t == lastConstant && skip(",") != null) {
                    int savedCursor = cursor;
                    Space suffix = whitespace();
                    if (source.charAt(cursor) == ';') {
                        return Markers.build(singletonList(new TrailingComma(randomId(), suffix)));
                    }
                    // Whitespace should be assigned to prefix of next statement or `J.Block#end`
                    cursor = savedCursor;
                    return Markers.build(singletonList(new TrailingComma(randomId(), EMPTY)));
                }
                return Markers.EMPTY;
            });

            enumSet = padRight(
                    new J.EnumValueSet(
                            randomId(),
                            EMPTY,
                            Markers.EMPTY,
                            enumValues,
                            skip(";") != null
                    ),
                    EMPTY
            );
        } else if (kind.getType() == J.ClassDeclaration.Kind.Type.Enum) {
            int nextSemicolonPosition = positionOfNext(";", null);
            int nextClosingBracePosition = positionOfNext("}", null);
            if (nextSemicolonPosition >= 0 && nextSemicolonPosition < nextClosingBracePosition) {
                enumSet = padRight(new J.EnumValueSet(randomId(), sourceBefore(";"), Markers.EMPTY, emptyList(), true), EMPTY);
            }
        }

        List<Tree> membersMultiVariablesSeparated = new ArrayList<>(node.getMembers().size());
        for (Tree m : node.getMembers()) {
            // skip lombok generated trees
            if (isLombokGenerated(m)) {
                continue;
            }

            // we don't care about the compiler-inserted default constructor,
            // since it will never be subject to refactoring
            if (m instanceof JCMethodDecl && hasFlag(((JCMethodDecl) m).getModifiers(), Flags.GENERATEDCONSTR)) {
                continue;
            }
            if (m instanceof JCVariableDecl && hasFlag(((JCVariableDecl) m).getModifiers(), Flags.ENUM)) {
                continue;
            }
            membersMultiVariablesSeparated.add(m);
        }

        List<JRightPadded<Statement>> members = new ArrayList<>();
        if (enumSet != null) {
            members.add(enumSet);
        }
        members.addAll(convertStatements(membersMultiVariablesSeparated));
        addPossibleEmptyStatementsBeforeClosingBrace(members);

        J.Block body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                members, sourceBefore("}"));

        return new J.ClassDeclaration(randomId(), fmt, Markers.EMPTY, modifierResults.getLeadingAnnotations(), modifierResults.getModifiers(), kind, name, typeParams,
                null, extendings, implementings, null, body, (JavaType.FullyQualified) typeMapping.type(node));
    }

    @Override
    public J visitCompilationUnit(CompilationUnitTree node, Space fmt) {
        JCCompilationUnit cu = (JCCompilationUnit) node;

        if (node.getTypeDecls().isEmpty() || cu.getPackageName() != null || !node.getImports().isEmpty()) {
            // if the package and imports are empty, allow the formatting to apply to the first class declaration.
            // in this way, javadoc comments are interpreted as javadocs on that class declaration.
            fmt = format(source.substring(0, cu.getStartPosition()));
            cursor(cu.getStartPosition());
        }

        endPosTable = cu.endPositions;
        docCommentTable = cu.docComments;

        Map<Integer, JCAnnotation> annotationPosTable = mapAnnotations(node.getPackageAnnotations(),
                new HashMap<>(node.getPackageAnnotations().size()));
        List<J.Annotation> packageAnnotations = collectAnnotations(annotationPosTable);

        J.Package packageDecl = null;
        if (cu.getPackageName() != null) {
            Space packagePrefix = sourceBefore("package");
            packageDecl = new J.Package(randomId(), packagePrefix, Markers.EMPTY,
                    convert(cu.getPackageName()), packageAnnotations);
        }
        return new J.CompilationUnit(
                randomId(),
                fmt,
                Markers.build(styles),
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                packageDecl == null ? null : padRight(packageDecl, sourceBefore(";")),
                convertAll(node.getImports(), this::statementDelim, this::statementDelim),
                convertAll(node.getTypeDecls().stream().filter(JCClassDecl.class::isInstance).collect(toList())),
                format(source.substring(cursor))
        );
    }

    @Override
    public J visitCompoundAssignment(CompoundAssignmentTree node, Space fmt) {
        Expression left = convert(((JCAssignOp) node).lhs);

        Space opPrefix = whitespace();
        J.AssignmentOperation.Type op;
        switch (((JCAssignOp) node).getTag()) {
            case PLUS_ASG:
                skip("+=");
                op = J.AssignmentOperation.Type.Addition;
                break;
            case MINUS_ASG:
                skip("-=");
                op = J.AssignmentOperation.Type.Subtraction;
                break;
            case DIV_ASG:
                skip("/=");
                op = J.AssignmentOperation.Type.Division;
                break;
            case MUL_ASG:
                skip("*=");
                op = J.AssignmentOperation.Type.Multiplication;
                break;
            case MOD_ASG:
                skip("%=");
                op = J.AssignmentOperation.Type.Modulo;
                break;
            case BITAND_ASG:
                skip("&=");
                op = J.AssignmentOperation.Type.BitAnd;
                break;
            case BITOR_ASG:
                skip("|=");
                op = J.AssignmentOperation.Type.BitOr;
                break;
            case BITXOR_ASG:
                skip("^=");
                op = J.AssignmentOperation.Type.BitXor;
                break;
            case SL_ASG:
                skip("<<=");
                op = J.AssignmentOperation.Type.LeftShift;
                break;
            case SR_ASG:
                skip(">>=");
                op = J.AssignmentOperation.Type.RightShift;
                break;
            case USR_ASG:
                skip(">>>=");
                op = J.AssignmentOperation.Type.UnsignedRightShift;
                break;
            default:
                throw new IllegalArgumentException("Unexpected compound assignment tag " + ((JCAssignOp) node).getTag());
        }

        return new J.AssignmentOperation(randomId(), fmt, Markers.EMPTY, left,
                padLeft(opPrefix, op), convert(((JCAssignOp) node).rhs), typeMapping.type(node));
    }

    @Override
    public J visitConditionalExpression(ConditionalExpressionTree node, Space fmt) {
        return new J.Ternary(randomId(), fmt, Markers.EMPTY,
                convert(node.getCondition()),
                padLeft(sourceBefore("?"), convert(node.getTrueExpression())),
                padLeft(sourceBefore(":"), convert(node.getFalseExpression())),
                typeMapping.type(node));
    }

    @Override
    public J visitContinue(ContinueTree node, Space fmt) {
        skip("continue");
        Name label = node.getLabel();
        return new J.Continue(randomId(), fmt, Markers.EMPTY,
                label == null ? null : new J.Identifier(randomId(), sourceBefore(label.toString()),
                        Markers.EMPTY, emptyList(), label.toString(), null, null));
    }

    @Override
    public J visitDoWhileLoop(DoWhileLoopTree node, Space fmt) {
        skip("do");
        return new J.DoWhileLoop(randomId(), fmt, Markers.EMPTY,
                convert(node.getStatement(), this::statementDelim),
                padLeft(sourceBefore("while"), convert(node.getCondition())));
    }

    @Override
    public J visitEmptyStatement(EmptyStatementTree node, Space fmt) {
        return new J.Empty(randomId(), fmt, Markers.EMPTY);
    }

    @Override
    public J visitEnhancedForLoop(EnhancedForLoopTree node, Space fmt) {
        skip("for");
        return new J.ForEachLoop(randomId(), fmt, Markers.EMPTY,
                new J.ForEachLoop.Control(randomId(), sourceBefore("("), Markers.EMPTY,
                        convert(node.getVariable(), t -> sourceBefore(":")),
                        convert(node.getExpression(), t -> sourceBefore(")"))),
                convert(node.getStatement(), this::statementDelim));
    }

    private J visitEnumVariable(VariableTree node, Space fmt) {
        List<J.Annotation> annotations = emptyList();
        Space nameSpace = EMPTY;

        if (!node.getModifiers().getAnnotations().isEmpty()) {
            annotations = convertAll(node.getModifiers().getAnnotations());
            nameSpace = sourceBefore(node.getName().toString());
        } else {
            skip(node.getName().toString());
        }

        J.Identifier name = new J.Identifier(randomId(), nameSpace, Markers.EMPTY, emptyList(), node.getName().toString(), typeMapping.type(node), null);

        J.NewClass initializer = null;
        if (source.charAt(endPos(node) - 1) == ')' || source.charAt(endPos(node) - 1) == '}') {
            initializer = convert(node.getInitializer());
        }

        return new J.EnumValue(randomId(), fmt, Markers.EMPTY, annotations, name, initializer);
    }

    @Override
    public J visitForLoop(ForLoopTree node, Space fmt) {
        skip("for");
        Space ctrlPrefix = sourceBefore("(");

        List<JRightPadded<Statement>> init = node.getInitializer().isEmpty() ?
                singletonList(padRight(new J.Empty(randomId(), sourceBefore(";"), Markers.EMPTY), EMPTY)) :
                convertStatements(node.getInitializer(), t ->
                        positionOfNext(",", ';') == -1 ?
                                semiDelim.apply(t) :
                                commaDelim.apply(t)
                );

        JRightPadded<Expression> condition = convert(node.getCondition(), semiDelim);
        if (condition == null) {
            condition = padRight(new J.Empty(randomId(), sourceBefore(";"), Markers.EMPTY), EMPTY);
        }

        List<JRightPadded<Statement>> update;
        if (node.getUpdate().isEmpty()) {
            update = singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY));
        } else {
            List<? extends ExpressionStatementTree> nodeUpdate = node.getUpdate();
            update = new ArrayList<>(nodeUpdate.size());
            for (int i = 0; i < nodeUpdate.size(); i++) {
                ExpressionStatementTree tree = nodeUpdate.get(i);
                update.add(convert(tree, i == nodeUpdate.size() - 1 ? t -> sourceBefore(")") : commaDelim));
            }
        }

        return new J.ForLoop(randomId(), fmt, Markers.EMPTY,
                new J.ForLoop.Control(randomId(), ctrlPrefix, Markers.EMPTY, init, condition, update),
                convert(node.getStatement(), this::statementDelim));
    }

    @Override
    public J visitIdentifier(IdentifierTree node, Space fmt) {
        String name = node.getName().toString();
        cursor += name.length();

        JCIdent ident = (JCIdent) node;
        // no `JavaType.Method` attribution for `super()` and `this()`
        JavaType type = ident.sym != null && ident.sym.isConstructor() ? null : typeMapping.type(ident);
        return new J.Identifier(randomId(), fmt, Markers.EMPTY, emptyList(), name, type, typeMapping.variableType(ident.sym));
    }

    @Override
    public J visitIf(IfTree node, Space fmt) {
        skip("if");
        return new J.If(randomId(), fmt, Markers.EMPTY,
                convert(node.getCondition()),
                convert(node.getThenStatement(), this::statementDelim),
                node.getElseStatement() instanceof JCStatement ?
                        new J.If.Else(randomId(), sourceBefore("else"), Markers.EMPTY, convert(node.getElseStatement(), this::statementDelim)) :
                        null);
    }

    @Override
    public J visitImport(ImportTree node, Space fmt) {
        skip("import");
        return new J.Import(randomId(), fmt, Markers.EMPTY,
                new JLeftPadded<>(node.isStatic() ? sourceBefore("static") : EMPTY,
                        node.isStatic(), Markers.EMPTY),
                convert(node.getQualifiedIdentifier()),
                null);
    }

    @Override
    public J visitInstanceOf(InstanceOfTree node, Space fmt) {
        return new J.InstanceOf(randomId(), fmt, Markers.EMPTY,
                convert(node.getExpression(), t -> sourceBefore("instanceof")),
                convert(node.getType()),
                null,
                typeMapping.type(node));
    }

    @Override
    public J visitIntersectionType(IntersectionTypeTree node, Space fmt) {
        JContainer<TypeTree> bounds = node.getBounds().isEmpty() ? null :
                JContainer.build(EMPTY,
                        convertAll(node.getBounds(), t -> sourceBefore("&"), noDelim), Markers.EMPTY);
        return new J.IntersectionType(randomId(), fmt, Markers.EMPTY, bounds);
    }

    @Override
    public J visitLabeledStatement(LabeledStatementTree node, Space fmt) {
        skip(node.getLabel().toString());
        return new J.Label(randomId(), fmt, Markers.EMPTY,
                padRight(new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), node.getLabel().toString(), null, null), sourceBefore(":")),
                convert(node.getStatement()));
    }

    @Override
    public J visitLambdaExpression(LambdaExpressionTree node, Space fmt) {
        boolean parenthesized = source.charAt(cursor) == '(';
        skip("(");

        List<JRightPadded<J>> paramList;
        if (parenthesized && node.getParameters().isEmpty()) {
            paramList = singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY));
        } else {
            paramList = convertAll(node.getParameters(), commaDelim, t -> parenthesized ? sourceBefore(")") : EMPTY);
        }

        J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, parenthesized, paramList);
        Space arrow = sourceBefore("->");

        J body;
        if (node.getBody() instanceof JCBlock) {
            Space prefix = sourceBefore("{");
            cursor--;
            body = convert(node.getBody());
            body = body.withPrefix(prefix);
        } else {
            body = convert(node.getBody());
        }

        return new J.Lambda(randomId(), fmt, Markers.EMPTY, params, arrow, body, typeMapping.type(node));
    }

    @Override
    public J visitLiteral(LiteralTree node, Space fmt) {
        cursor(endPos(node));
        Object value = node.getValue();
        String valueSource = source.substring(((JCLiteral) node).getStartPosition(), endPos(node));
        JavaType.Primitive type = typeMapping.primitive(((JCLiteral) node).typetag);

        if (value instanceof Character) {
            char c = (Character) value;
            if (c >= SURR_FIRST && c <= SURR_LAST) {
                return new J.Literal(randomId(), fmt, Markers.EMPTY, null, "''",
                        singletonList(new J.Literal.UnicodeEscape(1,
                                valueSource.substring(3, valueSource.length() - 1))), type);
            }
        } else if (JavaType.Primitive.String == type) {
            StringBuilder valueSourceWithoutSurrogates = new StringBuilder();
            List<J.Literal.UnicodeEscape> unicodeEscapes = null;

            int i = 0;
            char[] valueSourceArr = valueSource.toCharArray();
            for (int j = 0; j < valueSourceArr.length; j++) {
                char c = valueSourceArr[j];
                if (c == '\\' && j < valueSourceArr.length - 1 && (j == 0 || valueSourceArr[j - 1] != '\\')) {
                    if (valueSourceArr[j + 1] == 'u' && j < valueSource.length() - 5) {
                        String codePoint = valueSource.substring(j + 2, j + 6);
                        int codePointNumeric = Integer.parseInt(codePoint, 16);
                        if (codePointNumeric >= SURR_FIRST && codePointNumeric <= SURR_LAST) {
                            if (unicodeEscapes == null) {
                                unicodeEscapes = new ArrayList<>(1);
                            }
                            unicodeEscapes.add(new J.Literal.UnicodeEscape(i, codePoint));
                            j += 5;
                            continue;
                        }
                    }
                }
                valueSourceWithoutSurrogates.append(c);
                i++;
            }

            return new J.Literal(randomId(), fmt, Markers.EMPTY,
                    unicodeEscapes == null ? value : valueSourceWithoutSurrogates.toString(),
                    valueSourceWithoutSurrogates.toString(), unicodeEscapes, type);
        }

        return new J.Literal(randomId(), fmt, Markers.EMPTY, value, valueSource, null, type);
    }

    @Override
    public J visitMemberReference(MemberReferenceTree node, Space fmt) {
        JCMemberReference ref = (JCMemberReference) node;

        String referenceName;
        switch (ref.getMode()) {
            case NEW:
                referenceName = "new";
                break;
            case INVOKE:
            default:
                referenceName = node.getName().toString();
                break;
        }

        JavaType.Method methodReferenceType = null;
        if (ref.sym instanceof Symbol.MethodSymbol) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) ref.sym;
            methodReferenceType = typeMapping.methodInvocationType(methodSymbol.type, methodSymbol);
        }

        JavaType.Variable fieldReferenceType = null;
        if (ref.sym instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) ref.sym;
            fieldReferenceType = typeMapping.variableType(varSymbol);
        }

        return new J.MemberReference(randomId(),
                fmt,
                Markers.EMPTY,
                padRight(convert(ref.expr), sourceBefore("::")),
                convertTypeParameters(node.getTypeArguments()),
                padLeft(whitespace(), new J.Identifier(randomId(),
                        sourceBefore(referenceName),
                        Markers.EMPTY,
                        emptyList(),
                        referenceName,
                        null, null)),
                typeMapping.type(node),
                methodReferenceType,
                fieldReferenceType
        );
    }

    @Override
    public J visitMemberSelect(MemberSelectTree node, Space fmt) {
        JCFieldAccess fieldAccess = (JCFieldAccess) node;
        JavaType type = typeMapping.type(node);
        return new J.FieldAccess(randomId(), fmt, Markers.EMPTY,
                convert(fieldAccess.selected),
                padLeft(sourceBefore("."), new J.Identifier(randomId(),
                        sourceBefore(fieldAccess.name.toString()), Markers.EMPTY,
                        emptyList(), fieldAccess.name.toString(), type, typeMapping.variableType(fieldAccess.sym))),
                type);
    }

    @Override
    public J visitMethodInvocation(MethodInvocationTree node, Space fmt) {
        JCExpression jcSelect = ((JCMethodInvocation) node).getMethodSelect();

        JRightPadded<Expression> select = null;
        if (jcSelect instanceof JCFieldAccess) {
            select = convert(((JCFieldAccess) jcSelect).selected, t -> sourceBefore("."));
        } else if (!(jcSelect instanceof JCIdent)) {
            throw new IllegalStateException("Unexpected method select type " + jcSelect.getClass().getSimpleName());
        }

        // generic type parameters can only exist on qualified targets
        JContainer<Expression> typeParams = null;
        if (!node.getTypeArguments().isEmpty()) {
            typeParams = JContainer.build(sourceBefore("<"), convertAll(node.getTypeArguments(), commaDelim,
                    t -> sourceBefore(">")), Markers.EMPTY);
        }

        J.Identifier name;
        if (jcSelect instanceof JCFieldAccess) {
            String selectName = ((JCFieldAccess) jcSelect).name.toString();
            name = new J.Identifier(randomId(), sourceBefore(selectName), Markers.EMPTY, emptyList(), selectName, null, null);
        } else {
            name = convert(jcSelect);
        }

        JContainer<Expression> args = JContainer.build(sourceBefore("("), node.getArguments().isEmpty() ?
                singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")), Markers.EMPTY);

        Symbol methodSymbol = (jcSelect instanceof JCFieldAccess) ? ((JCFieldAccess) jcSelect).sym :
                ((JCIdent) jcSelect).sym;

        return new J.MethodInvocation(randomId(), fmt, Markers.EMPTY, select, typeParams, name, args,
                typeMapping.methodInvocationType(jcSelect.type, methodSymbol));
    }

    @Override
    public J visitMethod(MethodTree node, Space fmt) {
        JCMethodDecl jcMethod = (JCMethodDecl) node;

        Map<Integer, JCAnnotation> annotationPosTable = mapAnnotations(node.getModifiers().getAnnotations(),
                new HashMap<>(node.getModifiers().getAnnotations().size()));
        ReloadableJava11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

        J.TypeParameters typeParams;
        if (node.getTypeParameters().isEmpty()) {
            typeParams = null;
        } else {
            List<J.Annotation> typeParamsAnnotations = collectAnnotations(annotationPosTable);

            // see https://docs.oracle.com/javase/tutorial/java/generics/methods.html
            typeParams = new J.TypeParameters(randomId(), sourceBefore("<"), Markers.EMPTY,
                    typeParamsAnnotations,
                    convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")));
        }

        List<J.Annotation> returnTypeAnnotations = collectAnnotations(annotationPosTable);
        TypeTree returnType = convert(node.getReturnType());
        if (returnType != null && !returnTypeAnnotations.isEmpty()) {
            returnType = new J.AnnotatedType(randomId(), EMPTY, Markers.EMPTY,
                    returnTypeAnnotations, returnType);
        }

        Symbol.MethodSymbol nodeSym = jcMethod.sym;

        J.MethodDeclaration.IdentifierWithAnnotations name;
        if ("<init>".equals(node.getName().toString())) {
            String owner = null;
            if (nodeSym == null) {
                for (Tree tree : getCurrentPath()) {
                    if (tree instanceof JCClassDecl) {
                        owner = ((JCClassDecl) tree).getSimpleName().toString();
                        break;
                    }
                }

                if (owner == null) {
                    throw new IllegalStateException("Should have been able to locate an owner");
                }
            } else {
                owner = jcMethod.sym.owner.name.toString();
            }
            name = new J.MethodDeclaration.IdentifierWithAnnotations(new J.Identifier(randomId(), sourceBefore(owner),
                    Markers.EMPTY, emptyList(), owner, null, null), returnType == null ? returnTypeAnnotations : emptyList());
        } else {
            name = new J.MethodDeclaration.IdentifierWithAnnotations(new J.Identifier(randomId(), sourceBefore(node.getName().toString(), null), Markers.EMPTY,
                    emptyList(), node.getName().toString(), null, null), returnType == null ? returnTypeAnnotations : emptyList());
        }

        Space paramFmt = sourceBefore("(");
        JContainer<Statement> params = !node.getParameters().isEmpty() ?
                JContainer.build(paramFmt, convertAll(node.getParameters(), commaDelim, t -> sourceBefore(")")),
                        Markers.EMPTY) :
                JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"),
                        Markers.EMPTY), EMPTY)), Markers.EMPTY);

        JContainer<NameTree> throws_ = node.getThrows().isEmpty() ? null :
                JContainer.build(sourceBefore("throws"), convertAll(node.getThrows(), commaDelim, noDelim),
                        Markers.EMPTY);

        J.Block body = convert(node.getBody());

        JLeftPadded<Expression> defaultValue = node.getDefaultValue() == null ? null :
                padLeft(sourceBefore("default"), convert(node.getDefaultValue()));

        return new J.MethodDeclaration(randomId(), fmt, Markers.EMPTY,
                modifierResults.getLeadingAnnotations(),
                modifierResults.getModifiers(), typeParams,
                returnType, name, params, throws_, body, defaultValue,
                typeMapping.methodDeclarationType(jcMethod.sym, null));
    }

    @Override
    public J visitNewArray(NewArrayTree node, Space fmt) {
        skip("new");

        JCExpression jcVarType = ((JCNewArray) node).elemtype;
        TypeTree typeExpr;
        if (jcVarType instanceof JCArrayTypeTree) {
            // we'll capture the array dimensions in a bit, just convert the element type
            JCExpression elementType = ((JCArrayTypeTree) jcVarType).elemtype;
            while (elementType instanceof JCArrayTypeTree) {
                elementType = ((JCArrayTypeTree) elementType).elemtype;
            }
            typeExpr = convert(elementType);
        } else {
            typeExpr = convert(jcVarType);
        }

        List<? extends ExpressionTree> nodeDimensions = node.getDimensions();
        List<J.ArrayDimension> dimensions = new ArrayList<>(nodeDimensions.size());
        for (ExpressionTree dim : nodeDimensions) {
            dimensions.add(new J.ArrayDimension(
                    randomId(),
                    sourceBefore("["),
                    Markers.EMPTY,
                    convert(dim, t -> sourceBefore("]"))));
        }

        while (true) {
            int beginBracket = indexOfNextNonWhitespace(cursor, source);
            if (source.charAt(beginBracket) == '[') {
                int endBracket = indexOfNextNonWhitespace(beginBracket + 1, source);
                dimensions.add(new J.ArrayDimension(
                        randomId(),
                        format(source.substring(cursor, beginBracket)),
                        Markers.EMPTY,
                        padRight(new J.Empty(randomId(), format(source.substring(beginBracket + 1, endBracket)), Markers.EMPTY), EMPTY)));
                cursor = endBracket + 1;
            } else {
                break;
            }
        }

        JContainer<Expression> initializer = node.getInitializers() == null ? null :
                JContainer.build(sourceBefore("{"), node.getInitializers().isEmpty() ?
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore("}"), Markers.EMPTY), EMPTY)) :
                        convertAll(node.getInitializers(), commaDelim, t -> whitespace(), t -> {
                            if (t == node.getInitializers().get(node.getInitializers().size() - 1) && source.charAt(cursor) == ',') {
                                cursor++;
                                return Markers.build(singletonList(new TrailingComma(randomId(), whitespace())));
                            }
                            return Markers.EMPTY;
                        }), Markers.EMPTY);
        skip("}");

        return new J.NewArray(randomId(), fmt, Markers.EMPTY, typeExpr, dimensions,
                initializer, typeMapping.type(node));
    }

    @Override
    public J visitNewClass(NewClassTree node, Space fmt) {
        JRightPadded<Expression> encl = node.getEnclosingExpression() == null ? null :
                convert(node.getEnclosingExpression(), t -> sourceBefore("."));

        Space whitespaceBeforeNew = EMPTY;

        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (!(parent instanceof JCVariableDecl && ((((JCVariableDecl) parent).mods.flags & Flags.ENUM) != 0))) {
            whitespaceBeforeNew = sourceBefore("new");
        }

        // for enum definitions with anonymous class initializers, endPos of node identifier will be -1
        TypeTree clazz = endPos(node.getIdentifier()) >= 0 ? convert(node.getIdentifier()) : null;

        JContainer<Expression> args;
        if (positionOfNext("(", '{') > -1) {
            args = JContainer.build(sourceBefore("("),
                    node.getArguments().isEmpty() ?
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                            convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")), Markers.EMPTY);
        } else {
            args = JContainer.<Expression>empty()
                    .withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
        }

        J.Block body = null;
        if (node.getClassBody() != null) {
            Space bodyPrefix = sourceBefore("{");

            // we don't care about the compiler-inserted default constructor,
            // since it will never be subject to refactoring
            List<Tree> members = new ArrayList<>(node.getClassBody().getMembers().size());
            for (Tree m : node.getClassBody().getMembers()) {
                if (!(m instanceof JCMethodDecl) || (((JCMethodDecl) m).getModifiers().flags & Flags.GENERATEDCONSTR) == 0L) {
                    members.add(m);
                }
            }

            List<JRightPadded<Statement>> converted = convertStatements(members);
            addPossibleEmptyStatementsBeforeClosingBrace(converted);

            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    converted, sourceBefore("}"));
        }

        JCNewClass jcNewClass = (JCNewClass) node;
        JavaType.Method constructorType = typeMapping.methodInvocationType(jcNewClass.constructorType, jcNewClass.constructor);
        if (constructorType != null && jcNewClass.clazz.type.isParameterized() && node.getClassBody() == null) {
            constructorType = constructorType.withReturnType(typeMapping.type(jcNewClass.clazz.type));
        }

        return new J.NewClass(randomId(), fmt, Markers.EMPTY, encl, whitespaceBeforeNew,
                clazz, args, body, constructorType);
    }

    @Override
    public J visitParameterizedType(ParameterizedTypeTree node, Space fmt) {
        return new J.ParameterizedType(randomId(), fmt, Markers.EMPTY, convert(node.getType()), convertTypeParameters(node.getTypeArguments()), typeMapping.type(node));
    }

    @Override
    public J visitParenthesized(ParenthesizedTree node, Space fmt) {
        skip("(");
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        switch (parent.getKind()) {
            case CATCH:
            case DO_WHILE_LOOP:
            case IF:
            case SWITCH:
            case SYNCHRONIZED:
            case TYPE_CAST:
            case WHILE_LOOP:
                return new J.ControlParentheses<Expression>(randomId(), fmt, Markers.EMPTY,
                        convert(node.getExpression(), t -> sourceBefore(")")));
            default:
                return new J.Parentheses<Expression>(randomId(), fmt, Markers.EMPTY,
                        convert(node.getExpression(), t -> sourceBefore(")")));
        }
    }

    @Override
    public J visitPrimitiveType(PrimitiveTypeTree node, Space fmt) {
        cursor(endPos(node));

        JavaType.Primitive primitiveType;
        switch (node.getPrimitiveTypeKind()) {
            case BOOLEAN:
                primitiveType = JavaType.Primitive.Boolean;
                break;
            case BYTE:
                primitiveType = JavaType.Primitive.Byte;
                break;
            case CHAR:
                primitiveType = JavaType.Primitive.Char;
                break;
            case DOUBLE:
                primitiveType = JavaType.Primitive.Double;
                break;
            case FLOAT:
                primitiveType = JavaType.Primitive.Float;
                break;
            case INT:
                primitiveType = JavaType.Primitive.Int;
                break;
            case LONG:
                primitiveType = JavaType.Primitive.Long;
                break;
            case SHORT:
                primitiveType = JavaType.Primitive.Short;
                break;
            case VOID:
                primitiveType = JavaType.Primitive.Void;
                break;
            default:
                throw new IllegalArgumentException("Unknown primitive type " + node.getPrimitiveTypeKind());
        }

        return new J.Primitive(randomId(), fmt, Markers.EMPTY, primitiveType);
    }

    @Override
    public J visitReturn(ReturnTree node, Space fmt) {
        skip("return");
        Expression expression = convert(node.getExpression());
        return new J.Return(randomId(), fmt, Markers.EMPTY, expression);
    }

    @Override
    public J visitSwitch(SwitchTree node, Space fmt) {
        skip("switch");
        return new J.Switch(randomId(), fmt, Markers.EMPTY,
                convert(node.getExpression()),
                new J.Block(randomId(), sourceBefore("{"), Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                        convertAll(node.getCases(), noDelim, noDelim), sourceBefore("}")));
    }

    @Override
    public J visitSynchronized(SynchronizedTree node, Space fmt) {
        skip("synchronized");
        return new J.Synchronized(randomId(), fmt, Markers.EMPTY, convert(node.getExpression()),
                convert(node.getBlock()));
    }

    @Override
    public J visitThrow(ThrowTree node, Space fmt) {
        skip("throw");
        return new J.Throw(randomId(), fmt, Markers.EMPTY, convert(node.getExpression()));
    }

    @Override
    public J visitTry(TryTree node, Space fmt) {
        skip("try");
        JContainer<J.Try.Resource> resources;
        if (node.getResources().isEmpty()) {
            resources = null;
        } else {
            Space before = sourceBefore("(");
            List<JRightPadded<J.Try.Resource>> resourceList = new ArrayList<>(node.getResources().size());
            for (int i = 0; i < node.getResources().size(); i++) {
                Tree resource = node.getResources().get(i);
                J resourceVar = convert(resource);
                boolean semicolonPresent = true;
                if (i == node.getResources().size() - 1) {
                    semicolonPresent = positionOfNext(";", ')') > 0;
                }

                Space resourcePrefix = resourceVar.getPrefix();
                resourceVar = resourceVar.withPrefix(EMPTY); // moved to the containing Try.Resource

                if (semicolonPresent && resourceVar instanceof J.VariableDeclarations) {
                    J.VariableDeclarations resourceVarDecl = (J.VariableDeclarations) resourceVar;
                    resourceVar = resourceVarDecl.getPadding().withVariables(Space.formatLastSuffix(resourceVarDecl
                            .getPadding().getVariables(), sourceBefore(";")));
                }

                J.Try.Resource tryResource = new J.Try.Resource(randomId(), resourcePrefix, Markers.EMPTY,
                        resourceVar.withPrefix(EMPTY), semicolonPresent);

                // Starting in Java 9, you can have an identifier in the try with resource, if an Identifier
                // is parsed, the cursor is advance to the trailing semicolon. We do not want to pick this up in the
                // prefix of the next resource (or the right padding of identifier (if it is the last resource)
                skip(";");

                resourceList.add(padRight(tryResource, i == node.getResources().size() - 1 ?
                        sourceBefore(")") : EMPTY));
            }

            resources = JContainer.build(before, resourceList, Markers.EMPTY);
        }

        J.Block block = convert(node.getBlock());
        List<J.Try.Catch> catches = convertAll(node.getCatches());

        JLeftPadded<J.Block> finally_ = node.getFinallyBlock() == null ? null :
                padLeft(sourceBefore("finally"), convert(node.getFinallyBlock()));

        return new J.Try(randomId(), fmt, Markers.EMPTY, resources, block, catches, finally_);
    }

    @Override
    public J visitTypeCast(TypeCastTree node, Space fmt) {
        return new J.TypeCast(randomId(), fmt, Markers.EMPTY,
                new J.ControlParentheses<>(randomId(),
                        sourceBefore("("), Markers.EMPTY,
                        convert(node.getType(), t -> sourceBefore(")"))),
                convert(node.getExpression()));
    }

    @Override
    public J visitAnnotatedType(AnnotatedTypeTree node, Space fmt) {
        Map<Integer, JCAnnotation> annotationPosTable = mapAnnotations(node.getAnnotations(),
                new HashMap<>(node.getAnnotations().size()));
        Tree underlying = node.getUnderlyingType();
        if (underlying instanceof JCArrayTypeTree) {
            Tree element = ((JCArrayTypeTree) underlying).getType();
            if (element instanceof JCAnnotatedType) {
                annotationPosTable.putAll(mapAnnotations(((JCAnnotatedType) element).getAnnotations(), new HashMap<>()));
            }
        }
        List<J.Annotation> leadingAnnotations = leadingAnnotations(annotationPosTable);
        if (!annotationPosTable.isEmpty()) {
            if (node.getUnderlyingType() instanceof JCFieldAccess) {
                return new J.AnnotatedType(randomId(), fmt, Markers.EMPTY, leadingAnnotations, annotatedTypeTree(node.getUnderlyingType(), annotationPosTable));
            } else if (node.getUnderlyingType() instanceof JCArrayTypeTree) {
                return new J.AnnotatedType(randomId(), fmt, Markers.EMPTY, leadingAnnotations, arrayTypeTree(node, annotationPosTable));
            }
        }
        return new J.AnnotatedType(randomId(), fmt, Markers.EMPTY, leadingAnnotations, convert(node.getUnderlyingType()));
    }

    private Map<Integer, JCAnnotation> mapAnnotations(List<? extends AnnotationTree> annotations, Map<Integer, JCAnnotation> annotationPosTable) {
        for (AnnotationTree annotationNode : annotations) {
            JCAnnotation annotation = (JCAnnotation) annotationNode;
            annotationPosTable.put(annotation.pos, annotation);
        }
        return annotationPosTable;
    }

    private List<J.Annotation> leadingAnnotations(Map<Integer, JCAnnotation> annotationPosTable) {
        List<J.Annotation> annotations = new ArrayList<>(annotationPosTable.size());
        int saveCursor = cursor;
        whitespace();
        while (annotationPosTable.containsKey(cursor)) {
            JCAnnotation jcAnnotation = annotationPosTable.get(cursor);
            annotationPosTable.remove(cursor);
            cursor = saveCursor;
            J.Annotation ann = convert(jcAnnotation);
            annotations.add(ann);
            saveCursor = cursor;
            whitespace();
        }
        cursor = saveCursor;
        return annotations.isEmpty() ? emptyList() : annotations;
    }

    private TypeTree annotatedTypeTree(Tree node, Map<Integer, JCAnnotation> annotationPosTable) {
        if (node instanceof JCFieldAccess) {
            Space prefix = whitespace();
            JCFieldAccess fieldAccess = (JCFieldAccess) node;
            JavaType type = typeMapping.type(node);
            Expression select = (Expression) annotatedTypeTree(fieldAccess.selected, annotationPosTable);
            Space dotPrefix = sourceBefore(".");
            List<J.Annotation> annotations = leadingAnnotations(annotationPosTable);
            return new J.FieldAccess(randomId(), prefix, Markers.EMPTY,
                    select,
                    padLeft(dotPrefix, new J.Identifier(randomId(),
                            sourceBefore(fieldAccess.name.toString()), Markers.EMPTY,
                            annotations, fieldAccess.name.toString(), type, typeMapping.variableType(fieldAccess.sym))),
                    type
            );
        }
        return convert(node);
    }

    private TypeTree arrayTypeTree(Tree tree, Map<Integer, JCAnnotation> annotationPosTable) {
        Tree typeIdent = tree;
        int count = 0;
        JCArrayTypeTree arrayTypeTree = null;
        while (typeIdent instanceof JCAnnotatedType || typeIdent instanceof JCArrayTypeTree) {
            if (typeIdent instanceof JCAnnotatedType) {
                typeIdent = ((JCAnnotatedType) typeIdent).getUnderlyingType();
            }
            if (typeIdent instanceof JCArrayTypeTree) {
                if (count == 0) {
                    arrayTypeTree = (JCArrayTypeTree) typeIdent;
                }
                count++;
                typeIdent = ((JCArrayTypeTree) typeIdent).getType();
            }
        }

        Space prefix = whitespace();
        TypeTree elemType = convert(typeIdent);
        List<J.Annotation> annotations = leadingAnnotations(annotationPosTable);

        // Check if this is varargs (...) or regular array brackets ([])
        Markers markers = Markers.EMPTY;
        JLeftPadded<Space> dimension;
        int nextNonWhitespace = indexOfNextNonWhitespace(cursor, source);
        if (source.startsWith("...", nextNonWhitespace)) {
            // Varargs syntax
            markers = markers.addIfAbsent(new org.openrewrite.java.marker.Varargs(randomId()));
            dimension = padLeft(sourceBefore("..."), EMPTY);
        } else {
            // Regular array brackets
            dimension = padLeft(sourceBefore("["), sourceBefore("]"));
        }

        assert arrayTypeTree != null;
        return new J.ArrayType(randomId(), prefix, markers,
                count == 1 ? elemType : mapDimensions(elemType, arrayTypeTree.getType(), annotationPosTable),
                annotations,
                dimension,
                typeMapping.type(tree));
    }

    private TypeTree mapDimensions(TypeTree baseType, Tree tree, Map<Integer, JCAnnotation> annotationPosTable) {
        Tree typeIdent = tree;
        if (typeIdent instanceof JCAnnotatedType) {
            mapAnnotations(((JCAnnotatedType) typeIdent).getAnnotations(), annotationPosTable);
            typeIdent = ((JCAnnotatedType) typeIdent).getUnderlyingType();
        }

        if (typeIdent instanceof JCArrayTypeTree) {
            List<J.Annotation> annotations = leadingAnnotations(annotationPosTable);

            // Check if this is varargs (...) or regular array brackets ([])
            Markers markers = Markers.EMPTY;
            JLeftPadded<Space> dimension;
            int nextNonWhitespace = indexOfNextNonWhitespace(cursor, source);
            if (source.startsWith("...", nextNonWhitespace)) {
                // Varargs syntax
                markers = markers.addIfAbsent(new org.openrewrite.java.marker.Varargs(randomId()));
                dimension = padLeft(sourceBefore("..."), EMPTY);
            } else if (source.startsWith("[", nextNonWhitespace)) {
                // Regular array brackets
                dimension = padLeft(sourceBefore("["), sourceBefore("]"));
            } else {
                // No dimension found
                return baseType;
            }

            return new J.ArrayType(
                    randomId(),
                    EMPTY,
                    markers,
                    mapDimensions(baseType, ((JCArrayTypeTree) typeIdent).elemtype, annotationPosTable),
                    annotations,
                    dimension,
                    typeMapping.type(tree)
            );
        }
        return baseType;
    }

    @Override
    public J visitTypeParameter(TypeParameterTree node, Space fmt) {
        List<J.Annotation> annotations = convertAll(node.getAnnotations());

        Expression name = buildName(node.getName().toString())
                .withPrefix(sourceBefore(node.getName().toString()));

        // see https://docs.oracle.com/javase/tutorial/java/generics/bounded.html
        JContainer<TypeTree> bounds = node.getBounds().isEmpty() ? null :
                JContainer.build(sourceBefore("extends"),
                        convertAll(node.getBounds(), t -> sourceBefore("&"), noDelim), Markers.EMPTY);

        return new J.TypeParameter(randomId(), fmt, Markers.EMPTY, annotations, emptyList(), name, bounds);
    }

    private <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), part, null, null);
            } else {
                fullName += "." + part;

                int endOfPrefix = indexOfNextNonWhitespace(0, part);
                Space identFmt = endOfPrefix > 0 ? format(part, 0, endOfPrefix) : EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                //noinspection ResultOfMethodCallIgnored
                whitespaceSuffix.matches();
                Space namePrefix = i == parts.length - 1 ? EMPTY : format(whitespaceSuffix.group(1));

                expr = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        expr,
                        padLeft(namePrefix, new J.Identifier(randomId(), identFmt, Markers.EMPTY, emptyList(), part.trim(), null, null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.ShallowClass.build(fullName) :
                                null
                );
            }
        }

        //noinspection unchecked,ConstantConditions
        return (T) expr;
    }

    @Override
    public J visitUnionType(UnionTypeTree node, Space fmt) {
        return new J.MultiCatch(randomId(), fmt, Markers.EMPTY,
                convertAll(node.getTypeAlternatives(), t -> sourceBefore("|"), noDelim));
    }

    @Override
    public J visitUnary(UnaryTree node, Space fmt) {
        JCUnary unary = (JCUnary) node;
        Tag tag = unary.getTag();
        JLeftPadded<J.Unary.Type> op;
        Expression expr;

        switch (tag) {
            case POS:
                skip("+");
                op = padLeft(EMPTY, J.Unary.Type.Positive);
                expr = convert(unary.arg);
                break;
            case NEG:
                skip("-");
                op = padLeft(EMPTY, J.Unary.Type.Negative);
                expr = convert(unary.arg);
                break;
            case PREDEC:
                skip("--");
                op = padLeft(EMPTY, J.Unary.Type.PreDecrement);
                expr = convert(unary.arg);
                break;
            case PREINC:
                skip("++");
                op = padLeft(EMPTY, J.Unary.Type.PreIncrement);
                expr = convert(unary.arg);
                break;
            case POSTDEC:
                expr = convert(unary.arg);
                op = padLeft(sourceBefore("--"), J.Unary.Type.PostDecrement);
                break;
            case POSTINC:
                expr = convert(unary.arg);
                op = padLeft(sourceBefore("++"), J.Unary.Type.PostIncrement);
                break;
            case COMPL:
                skip("~");
                op = padLeft(EMPTY, J.Unary.Type.Complement);
                expr = convert(unary.arg);
                break;
            case NOT:
                skip("!");
                op = padLeft(EMPTY, J.Unary.Type.Not);
                expr = convert(unary.arg);
                break;
            default:
                throw new IllegalArgumentException("Unexpected unary tag " + tag);
        }

        return new J.Unary(randomId(), fmt, Markers.EMPTY, op, expr, typeMapping.type(node));
    }

    @Override
    public J visitVariable(VariableTree node, Space fmt) {
        JCVariableDecl jcVariableDecl = (JCVariableDecl) node;
        if ("<error>".equals(jcVariableDecl.getName().toString())) {
            int startPos = jcVariableDecl.getStartPosition();
            int endPos = jcVariableDecl.getEndPosition(endPosTable);

            if (startPos == endPos) {
                endPos = startPos + 1; // For cases where the error node is a single character like "/"
            }
            String erroneousNode = source.substring(startPos, endPos);
            return new J.Erroneous(
                    randomId(),
                    fmt,
                    Markers.EMPTY,
                    erroneousNode
            );
        }
        return hasFlag(node.getModifiers(), Flags.ENUM) ?
                visitEnumVariable(node, fmt) :
                visitVariables(singletonList(node), fmt); // method arguments cannot be multi-declarations
    }

    private J.VariableDeclarations visitVariables(List<VariableTree> nodes, Space fmt) {
        JCVariableDecl node = (JCVariableDecl) nodes.get(0);

        JCExpression vartype = node.vartype;

        Map<Integer, JCAnnotation> annotationPosTable = mapAnnotations(node.getModifiers().getAnnotations(),
                new HashMap<>(node.getModifiers().getAnnotations().size()));
        ReloadableJava11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

        List<J.Annotation> typeExprAnnotations = collectAnnotations(annotationPosTable);

        TypeTree typeExpr;
        if (vartype == null) {
            typeExpr = null;
        } else if (endPos(vartype) < 0) {
            if ((node.sym.flags() & Flags.PARAMETER) > 0) {
                // this is a lambda parameter with an inferred type expression
                typeExpr = null;
            } else {
                Space space = whitespace();
                boolean lombokVal = source.startsWith("val", cursor);
                cursor += 3; // skip `val` or `var`
                typeExpr = new J.Identifier(randomId(),
                        space,
                        Markers.build(singletonList(JavaVarKeyword.build())),
                        emptyList(),
                        lombokVal ? "val" : "var",
                        typeMapping.type(vartype),
                        null);
            }
        } else if (vartype instanceof JCArrayTypeTree) {
            JCExpression elementType = vartype;
            while (elementType instanceof JCArrayTypeTree || elementType instanceof JCAnnotatedType) {
                if (elementType instanceof JCAnnotatedType) {
                    elementType = ((JCAnnotatedType) elementType).underlyingType;
                }
                if (elementType instanceof JCArrayTypeTree) {
                    elementType = ((JCArrayTypeTree) elementType).elemtype;
                }
            }
            int idx = indexOfNextNonWhitespace(elementType.getEndPosition(endPosTable), source);
            typeExpr = idx != -1 && (source.charAt(idx) == '[' || source.charAt(idx) == '@' || source.startsWith("...", idx)) ? convert(vartype) :
                    // we'll capture the array dimensions in a bit, just convert the element type
                    convert(elementType);
        } else {
            typeExpr = convert(vartype);
        }

        if (typeExpr == null) {
            // `node.declaredUsingVar()` was only added around 11.0.15
            int nextTokenIdx = indexOfNextNonWhitespace(cursor, source);
            boolean nextTokenStartsWithVar = source.startsWith("var", nextTokenIdx);
            if (nextTokenStartsWithVar &&
                node.getStartPosition() <= nextTokenIdx &&
                nextTokenIdx + 3 < node.getPreferredPosition()
            ) {
                typeExpr = new J.Identifier(randomId(), sourceBefore("var"),
                    Markers.build(singletonList(JavaVarKeyword.build())), emptyList(), "var",
                    typeMapping.type(vartype), null);
            }
        }

        if (typeExpr != null && !typeExprAnnotations.isEmpty()) {
            Space prefix = typeExprAnnotations.get(0).getPrefix();
            typeExpr = new J.AnnotatedType(randomId(), prefix, Markers.EMPTY, ListUtils.mapFirst(typeExprAnnotations, a -> a.withPrefix(EMPTY)), typeExpr);
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            JCVariableDecl n = (JCVariableDecl) nodes.get(i);

            Space namedVarPrefix = sourceBefore(n.getName().toString());

            JavaType.Variable type = typeMapping.variableType(n.sym);
            J.Identifier name = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), n.getName().toString(),
                    type != null ? type.getType() : null,
                    type);
            List<JLeftPadded<Space>> dimensionsAfterName = arrayDimensions();

            vars.add(
                    padRight(
                            new J.VariableDeclarations.NamedVariable(randomId(), namedVarPrefix, Markers.EMPTY,
                                    name,
                                    dimensionsAfterName,
                                    n.init != null ? padLeft(sourceBefore("="), convert(n.init)) : null,
                                    type
                            ),
                            i == nodes.size() - 1 ? EMPTY : sourceBefore(",")
                    )
            );
        }

        return new J.VariableDeclarations(randomId(), fmt, Markers.EMPTY, modifierResults.getLeadingAnnotations(), modifierResults.getModifiers(), typeExpr, null, vars);
    }

    private List<JLeftPadded<Space>> arrayDimensions() {
        List<JLeftPadded<Space>> dims = null;
        while (true) {
            int beginBracket = indexOfNextNonWhitespace(cursor, source);
            if (source.charAt(beginBracket) == '[') {
                int endBracket = indexOfNextNonWhitespace(beginBracket + 1, source);
                if (dims == null) {
                    dims = new ArrayList<>(2);
                }
                dims.add(padLeft(format(source, cursor, beginBracket),
                        format(source, beginBracket + 1, endBracket)));
                cursor = endBracket + 1;
            } else {
                break;
            }
        }
        return dims == null ? emptyList() : dims;
    }

    @Override
    public J visitWhileLoop(WhileLoopTree node, Space fmt) {
        skip("while");
        return new J.WhileLoop(randomId(), fmt, Markers.EMPTY,
                convert(node.getCondition()),
                convert(node.getStatement(), this::statementDelim));
    }

    @Override
    public J visitWildcard(WildcardTree node, Space fmt) {
        skip("?");

        JCWildcard wildcard = (JCWildcard) node;

        JLeftPadded<J.Wildcard.Bound> bound;
        switch (wildcard.kind.kind) {
            case EXTENDS:
                bound = padLeft(sourceBefore("extends"), J.Wildcard.Bound.Extends);
                break;
            case SUPER:
                bound = padLeft(sourceBefore("super"), J.Wildcard.Bound.Super);
                break;
            case UNBOUND:
            default:
                bound = null;
        }

        return new J.Wildcard(randomId(), fmt, Markers.EMPTY, bound, convert(wildcard.inner));
    }

    /**
     * --------------
     * Conversion utilities
     * --------------
     */
    private <J2 extends J> @Nullable J2 convert(@Nullable Tree t) {
        if (t == null) {
            return null;
        }
        try {
            // The spacing of initialized enums such as `ONE   (1)` is handled in the `visitNewClass` method, so set it explicitly to “” here.
            String prefix = isEnum(t) ? "" : source.substring(cursor, indexOfNextNonWhitespace(cursor, source));
            cursor += prefix.length();
            @SuppressWarnings("unchecked") J2 j = (J2) scan(t, formatWithCommentTree(prefix, (JCTree) t, docCommentTable.getCommentTree((JCTree) t)));
            return j;
        } catch (Throwable ex) {
            // this SHOULD never happen, but is here simply as a diagnostic measure in the event of unexpected exceptions
            StringBuilder message = new StringBuilder("Failed to convert for the following cursor stack:");
            message.append("--- BEGIN PATH ---\n");

            List<Tree> paths = stream(getCurrentPath().spliterator(), false).collect(toList());
            for (int i = paths.size(); i-- > 0; ) {
                JCTree tree = (JCTree) paths.get(i);
                if (tree instanceof JCCompilationUnit) {
                    message.append("JCCompilationUnit(sourceFile = ").append(((JCCompilationUnit) tree).sourcefile.getName()).append(")\n");
                } else if (tree instanceof JCClassDecl) {
                    message.append("JCClassDecl(name = ").append(((JCClassDecl) tree).name).append(", line = ").append(lineNumber(tree)).append(")\n");
                } else if (tree instanceof JCVariableDecl) {
                    message.append("JCVariableDecl(name = ").append(((JCVariableDecl) tree).name).append(", line = ").append(lineNumber(tree)).append(")\n");
                } else {
                    message.append(tree.getClass().getSimpleName()).append("(line = ").append(lineNumber(tree)).append(")\n");
                }
            }

            message.append("--- END PATH ---\n");

            ctx.getOnError().accept(new JavaParsingException(message.toString(), ex));
            throw ex;
        }
    }

    private boolean isEnum(Tree t) {
        if (t instanceof JCNewClass) {
            JCNewClass newClass = (JCNewClass) t;
            return newClass.type != null && newClass.type.tsym != null && hasFlag(newClass.type.tsym.flags(), Flags.ENUM);
        }
        return false;
    }

    private <J2 extends @Nullable J> @Nullable JRightPadded<J2> convert(@Nullable Tree t, Function<Tree, Space> suffix) {
        return convert(t, suffix, j -> Markers.EMPTY);
    }

    private <J2 extends @Nullable J> @Nullable JRightPadded<J2> convert(@Nullable Tree t, Function<Tree, Space> suffix, Function<Tree, Markers> markers) {
        if (t == null) {
            return null;
        }
        J2 j = convert(t);
        @SuppressWarnings("ConstantConditions") JRightPadded<J2> rightPadded = j == null ? null :
                new JRightPadded<>(j, suffix.apply(t), markers.apply(t));
        int idx = findFirstNonWhitespaceChar(rightPadded.getAfter().getWhitespace());
        if (idx >= 0) {
            rightPadded = (JRightPadded<J2>) JRightPadded.build(getErroneous(List.of(rightPadded)));
        }
        // Cursor hasn't been updated but points at the end of erroneous node already
        // This means that error node start position == end position
        // Therefore ensure that cursor has moved to the end of erroneous node but adding its length to the cursor
        // Example `/pet` results in 2 erroneous nodes: `/` and `pet`. The `/` node would have start and end position the
        // same from the JC compiler.
        if (endPos(t) == cursor && rightPadded.getElement() instanceof J.Erroneous) {
            cursor += ((J.Erroneous) rightPadded.getElement()).getText().length();
        } else {
            cursor(max(endPos(t), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
        }
        return rightPadded;
    }

    private <J2 extends J> J.Erroneous getErroneous(List<JRightPadded<J2>> converted) {
        PrintOutputCapture<Integer> p = new PrintOutputCapture<>(0);
        new JavaPrinter<Integer>().visitContainer(JContainer.build(EMPTY, converted, Markers.EMPTY), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p);
        return new J.Erroneous(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                p.getOut()
        );
    }

    private static int findFirstNonWhitespaceChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private long lineNumber(Tree tree) {
        return source.substring(0, ((JCTree) tree).getStartPosition()).chars().filter(c -> c == '\n').count() + 1;
    }

    private <J2 extends J> List<J2> convertAll(List<? extends Tree> trees) {
        List<J2> converted = new ArrayList<>(trees.size());
        for (Tree tree : trees) {
            converted.add(convert(tree));
        }
        return converted;
    }

    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends Tree> trees,
                                                             Function<Tree, Space> innerSuffix,
                                                             Function<Tree, Space> suffix) {
        return convertAll(trees, innerSuffix, suffix, t -> Markers.EMPTY);
    }

    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends Tree> trees,
                                                             Function<Tree, Space> innerSuffix,
                                                             Function<Tree, Space> suffix,
                                                             Function<Tree, Markers> markers) {
        int size = trees.size();
        if (size == 0) {
            return emptyList();
        }
        List<JRightPadded<J2>> converted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            converted.add(convert(trees.get(i), i == size - 1 ? suffix : innerSuffix, markers));
        }
        return converted;
    }

    private @Nullable JContainer<Expression> convertTypeParameters(@Nullable List<? extends Tree> typeArguments) {
        if (typeArguments == null) {
            return null;
        }

        Space typeArgPrefix = sourceBefore("<");
        List<JRightPadded<Expression>> params;
        if (typeArguments.isEmpty()) {
            // raw type, see http://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html
            // adding space before > as a suffix to be consistent with space before > for non-empty lists of type args
            params = singletonList(padRight(new J.Empty(randomId(), sourceBefore(">"), Markers.EMPTY), EMPTY));
        } else {
            params = convertAll(typeArguments, commaDelim, t -> sourceBefore(">"));
        }

        return JContainer.build(typeArgPrefix, params, Markers.EMPTY);
    }

    private Space statementDelim(@Nullable Tree t) {
        if (t instanceof JCAssert ||
            t instanceof JCAssign ||
            t instanceof JCAssignOp ||
            t instanceof JCBreak ||
            t instanceof JCContinue ||
            t instanceof JCDoWhileLoop ||
            t instanceof JCImport ||
            t instanceof JCMethodInvocation ||
            t instanceof JCNewClass ||
            t instanceof JCReturn ||
            t instanceof JCThrow ||
            t instanceof JCUnary) {
            return sourceBefore(";");
        }

        if (t instanceof JCLabeledStatement) {
            return statementDelim(((JCLabeledStatement) t).getStatement());
        }

        if (t instanceof JCExpressionStatement) {
            ExpressionTree expTree = ((ExpressionStatementTree) t).getExpression();
            if (expTree instanceof ErroneousTree) {
                return Space.build(source.substring(((JCTree) expTree).getEndPosition(endPosTable), ((JCTree) t).getEndPosition(endPosTable)), emptyList());
            } else {
                return sourceBefore(";");
            }
        }

        if (t instanceof JCVariableDecl) {
            JCVariableDecl varTree = (JCVariableDecl) t;
            if ("<error>".contentEquals(varTree.getName())) {
                int start = varTree.vartype.getEndPosition(endPosTable);
                int end = varTree.getEndPosition(endPosTable);
                String whitespace = source.substring(start, end);
                if (whitespace.contains("\n")) {
                    return EMPTY;
                } else {
                    return Space.build(source.substring(start, end), emptyList());
                }
            }
            return sourceBefore(";");
        }

        if (t instanceof JCMethodDecl) {
            JCMethodDecl m = (JCMethodDecl) t;
            if (m.body == null || m.defaultValue != null) {
                return sourceBefore(";");
            } else {
                return sourceBefore("");
            }
        }

        return EMPTY;
    }

    private List<JRightPadded<Statement>> convertStatements(@Nullable List<? extends Tree> trees) {
        return convertStatements(trees, this::statementDelim);
    }

    @SuppressWarnings("unchecked")
    private List<JRightPadded<Statement>> convertStatements(@Nullable List<? extends Tree> trees,
                                                            Function<Tree, Space> suffix) {
        if (trees == null || trees.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, List<Tree>> treesGroupedByStartPosition = new LinkedHashMap<>();
        for (Tree t : trees) {
            if (!(t instanceof JCVariableDecl) && isLombokGenerated(t)) {
                continue;
            }
            treesGroupedByStartPosition.computeIfAbsent(((JCTree) t).getStartPosition(), k -> new ArrayList<>(1)).add(t);
        }

        List<JRightPadded<Statement>> converted = new ArrayList<>(treesGroupedByStartPosition.size());
        for (List<? extends Tree> treeGroup : treesGroupedByStartPosition.values()) {
            if (treeGroup.size() == 1) {
                Tree t = treeGroup.get(0);
                int startPosition = ((JCTree) t).getStartPosition();
                if (cursor > startPosition)
                    continue;
                if (!(t instanceof JCSkip)) {
                    while (cursor < startPosition) {
                        int nonWhitespaceIndex = indexOfNextNonWhitespace(cursor, source);
                        int semicolonIndex = source.charAt(nonWhitespaceIndex) == ';' ? nonWhitespaceIndex : -1;
                        if (semicolonIndex > -1) {
                            Space prefix = format(source, cursor, semicolonIndex);
                            converted.add(new JRightPadded<>(new J.Empty(randomId(), prefix, Markers.EMPTY), EMPTY, Markers.EMPTY));
                            cursor = semicolonIndex + 1;
                        } else {
                            break;
                        }
                    }
                }
                converted.add(convert(treeGroup.get(0), suffix));
            } else {
                // multi-variable declarations are split into independent overlapping JCVariableDecl's by the OpenJDK AST
                String prefix = source.substring(cursor, max(((JCTree) treeGroup.get(0)).getStartPosition(), cursor));
                cursor += prefix.length();

                Tree last = treeGroup.get(treeGroup.size() - 1);

                @SuppressWarnings("unchecked")
                J.VariableDeclarations vars = visitVariables((List<VariableTree>) treeGroup, format(prefix));
                JRightPadded<Statement> paddedVars = padRight(vars, semiDelim.apply(last));
                cursor(max(endPos(last), cursor));
                converted.add(paddedVars);
            }
        }

        return converted;
    }

    /**
     * --------------
     * Other convenience utilities
     * --------------
     */

    private boolean isLombokGenerated(Tree t) {
        if (!hasLombokGeneratedSymbol(t)) {
            return false;
        }
        // If the lombok annotation is actually present in the source, the user wrote it themselves
        if (t instanceof JCAnnotation) {
            int pos = ((JCAnnotation) t).pos;
            return !(pos >= 0 && pos < source.length() &&
                    (source.startsWith("@Generated", pos) || source.startsWith("@lombok.Generated", pos)));
        }
        List<JCAnnotation> annotations;
        if (t instanceof JCMethodDecl) {
            annotations = ((JCMethodDecl) t).getModifiers().getAnnotations();
        } else if (t instanceof JCClassDecl) {
            annotations = ((JCClassDecl) t).getModifiers().getAnnotations();
        } else if (t instanceof JCVariableDecl) {
            annotations = ((JCVariableDecl) t).getModifiers().getAnnotations();
        } else {
            return true;
        }
        for (JCAnnotation ann : annotations) {
            if (hasLombokGeneratedSymbol(ann)) {
                int pos = ann.pos;
                if (pos >= 0 && pos < source.length() &&
                        (source.startsWith("@Generated", pos) || source.startsWith("@lombok.Generated", pos))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasLombokGeneratedSymbol(Tree t) {
        Tree tree = (t instanceof JCAnnotation) ? ((JCAnnotation) t).getAnnotationType() : t;

        Symbol sym = extractSymbol(tree);
        if (sym == null) {
            return false;
        }

        return isLombokAnnotationType(sym.getQualifiedName().toString()) ||
                sym.getDeclarationAttributes().stream()
                        .map(a -> a.type.toString())
                        .anyMatch(ReloadableJava11ParserVisitor::isLombokAnnotationType);
    }

    private static boolean isLombokAnnotationType(String name) {
        return "lombok.val".equals(name) ||
                "lombok.var".equals(name) ||
                "lombok.Generated".equals(name);
    }

    private static @Nullable Symbol extractSymbol(Tree tree) {
        if (tree instanceof JCIdent) {
            return ((JCIdent) tree).sym;
        } else if (tree instanceof JCTree.JCMethodDecl) {
            return ((JCTree.JCMethodDecl) tree).sym;
        } else if (tree instanceof JCTree.JCClassDecl) {
            return ((JCTree.JCClassDecl) tree).sym;
        } else if (tree instanceof JCTree.JCVariableDecl) {
            return ((JCTree.JCVariableDecl) tree).sym;
        }
        return null;
    }

    private int endPos(Tree t) {
        return ((JCTree) t).getEndPosition(endPosTable);
    }

    private Space sourceBefore(String untilDelim) {
        return sourceBefore(untilDelim, null);
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim, @Nullable Character stop) {
        int delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        if (delimIndex == cursor) {
            cursor += untilDelim.length();
            return EMPTY;
        }

        Space space = format(source, cursor, delimIndex);
        cursor = delimIndex + untilDelim.length(); // advance past the delimiter
        return space;
    }

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    char c1 = source.charAt(delimIndex);
                    char c2 = source.charAt(delimIndex + 1);
                    switch (c1) {
                        case '/':
                            switch (c2) {
                                case '/':
                                    inSingleLineComment = !inMultiLineComment;
                                    delimIndex++;
                                    break;
                                case '*':
                                    inMultiLineComment = true;
                                    delimIndex++;
                                    break;
                            }
                            break;
                        case '*':
                            if (c2 == '/') {
                                inMultiLineComment = false;
                                delimIndex++;
                                continue;
                            }
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (stop != null && source.charAt(delimIndex) == stop)
                        return -1; // reached stop word before finding the delimiter

                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private final Function<Tree, Space> semiDelim = ignored -> sourceBefore(";");
    private final Function<Tree, Space> commaDelim = ignored -> sourceBefore(",");
    private final Function<Tree, Space> noDelim = ignored -> EMPTY;

    private Space whitespace() {
        int nextNonWhitespace = indexOfNextNonWhitespace(cursor, source);
        if (nextNonWhitespace == cursor) {
            return EMPTY;
        }
        Space space = format(source, cursor, nextNonWhitespace);
        cursor = nextNonWhitespace;
        return space;
    }

    private @Nullable String skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (source.startsWith(token, cursor)) {
            cursor += token.length();
            return token;
        }
        return null;
    }

    // Only exists as a function to make it easier to debug unexpected cursor shifts
    private void cursor(int n) {
        cursor = n;
    }

    private boolean hasFlag(ModifiersTree modifiers, long flag) {
        return hasFlag(((JCModifiers) modifiers).flags, flag);
    }

    private boolean hasFlag(long flags, long flag) {
        return (flags & flag) != 0L;
    }

    @SuppressWarnings("unused")
    // Used for debugging
    private List<String> listFlags(long flags) {
        Map<String, Long> allFlags = Arrays.stream(Flags.class.getDeclaredFields())
                .filter(field -> {
                    field.setAccessible(true);
                    try {
                        // FIXME instanceof probably not right here...
                        return field.get(null) instanceof Long &&
                               field.getName().matches("[A-Z_]+");
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(toMap(Field::getName, field -> {
                    try {
                        return (Long) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }));

        List<String> all = new ArrayList<>(allFlags.size());
        for (Map.Entry<String, Long> flagNameAndCode : allFlags.entrySet()) {
            if ((flagNameAndCode.getValue() & flags) != 0L) {
                all.add(flagNameAndCode.getKey());
            }
        }
        return all;
    }

    /**
     * Leading annotations and modifiers in the order they appear in the source, which is not necessarily the same as the order in
     * which they appear in the OpenJDK AST
     */
    private ReloadableJava11ModifierResults sortedModifiersAndAnnotations(ModifiersTree modifiers, Map<Integer, JCAnnotation> annotationPosTable) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> sortedModifiers = new ArrayList<>();
        List<J.Annotation> currentAnnotations = new ArrayList<>();
        boolean afterFirstModifier = false;
        boolean inComment = false;
        boolean inMultilineComment = false;
        final AtomicReference<String> word = new AtomicReference<>("");
        int afterLastModifierPosition = cursor;
        int lastAnnotationPosition = cursor;
        boolean noSpace = false;

        for (int i = cursor; i < source.length(); i++) {
            if (annotationPosTable.containsKey(i)) {
                JCAnnotation jcAnnotation = annotationPosTable.get(i);
                if (isLombokGenerated(jcAnnotation)) {
                    continue;
                }
                J.Annotation annotation = convert(jcAnnotation);
                if (afterFirstModifier) {
                    currentAnnotations.add(annotation);
                } else {
                    leadingAnnotations.add(annotation);
                }
                i = cursor - 1;
                lastAnnotationPosition = cursor;
                continue;
            }
            char c = source.charAt(i);
            if (c == '/' && source.length() > i + 1) {
                char next = source.charAt(i + 1);
                if (next == '*') {
                    inMultilineComment = true;
                } else if (next == '/') {
                    inComment = true;
                }
            }

            if (inMultilineComment && c == '/' && source.charAt(i - 1) == '*') {
                inMultilineComment = false;
            } else if (inComment && (c == '\n' || c == '\r')) {
                inComment = false;
            } else if (!inMultilineComment && !inComment) {
                // Check: char is whitespace OR next char is an `@` (which is an annotation preceded by modifier/annotation without space)
                if (Character.isWhitespace(c) || (noSpace = (i + 1 < source.length() && source.charAt(i + 1) == '@'))) {
                    if (noSpace) {
                        word.getAndUpdate(w -> w + c);
                        noSpace = false;
                    }
                    if (!word.get().isEmpty()) {
                        Modifier matching = null;
                        for (Modifier modifier : modifiers.getFlags()) {
                            if (modifier.name().toLowerCase().equals(word.get())) {
                                matching = modifier;
                                break;
                            }
                        }

                        if (matching == null) {
                            this.cursor = afterLastModifierPosition;
                            break;
                        } else {
                            sortedModifiers.add(mapModifier(matching, currentAnnotations));
                            afterFirstModifier = true;
                            currentAnnotations = new ArrayList<>();
                            word.set("");
                            afterLastModifierPosition = cursor;
                            i = cursor - 1;
                        }
                    }
                } else {
                    word.getAndUpdate(w -> w + c);
                }
            }
        }
        if (sortedModifiers.isEmpty()) {
            cursor = lastAnnotationPosition;
        }
        return new ReloadableJava11ModifierResults(
                leadingAnnotations.isEmpty() ? emptyList() : leadingAnnotations,
                sortedModifiers.isEmpty() ? emptyList() : sortedModifiers
        );
    }

    private J.Modifier mapModifier(Modifier mod, List<J.Annotation> annotations) {
        Space modFormat = whitespace();
        cursor += mod.name().length();
        J.Modifier.Type type;
        switch (mod) {
            case DEFAULT:
                type = J.Modifier.Type.Default;
                break;
            case PUBLIC:
                type = J.Modifier.Type.Public;
                break;
            case PROTECTED:
                type = J.Modifier.Type.Protected;
                break;
            case PRIVATE:
                type = J.Modifier.Type.Private;
                break;
            case ABSTRACT:
                type = J.Modifier.Type.Abstract;
                break;
            case STATIC:
                type = J.Modifier.Type.Static;
                break;
            case FINAL:
                type = J.Modifier.Type.Final;
                break;
            case NATIVE:
                type = J.Modifier.Type.Native;
                break;
            case STRICTFP:
                type = J.Modifier.Type.Strictfp;
                break;
            case SYNCHRONIZED:
                type = J.Modifier.Type.Synchronized;
                break;
            case TRANSIENT:
                type = J.Modifier.Type.Transient;
                break;
            case VOLATILE:
                type = J.Modifier.Type.Volatile;
                break;
            default:
                throw new IllegalArgumentException("Unexpected modifier " + mod);
        }
        return new J.Modifier(randomId(), modFormat, Markers.EMPTY, null, type, annotations);
    }

    private List<J.Annotation> collectAnnotations(Map<Integer, JCAnnotation> annotationPosTable) {
        int maxAnnotationPosition = 0;
        for (Integer pos : annotationPosTable.keySet()) {
            if (pos > maxAnnotationPosition) {
                maxAnnotationPosition = pos;
            }
        }

        List<J.Annotation> annotations = new ArrayList<>();
        boolean inComment = false;
        boolean inMultilineComment = false;
        for (int i = cursor; i <= maxAnnotationPosition && i < source.length(); i++) {
            if (annotationPosTable.containsKey(i)) {
                JCAnnotation jcAnnotation = annotationPosTable.get(i);
                if (isLombokGenerated(jcAnnotation)) {
                    continue;
                }
                annotations.add(convert(jcAnnotation));
                // Adjusting the index by subtracting 1 to account for the case where annotations are not separated by a space
                i = cursor - 1;
                continue;
            }
            char c = source.charAt(i);
            if (c == '/' && source.length() > i + 1) {
                char next = source.charAt(i + 1);
                if (next == '*') {
                    inMultilineComment = true;
                } else if (next == '/') {
                    inComment = true;
                }
            }

            if (inMultilineComment && c == '/' && i > 0 && source.charAt(i - 1) == '*') {
                inMultilineComment = false;
            } else if (inComment && (c == '\n' || c == '\r')) {
                inComment = false;
            } else if (!inMultilineComment && !inComment) {
                if (!Character.isWhitespace(c)) {
                    break;
                }
            }
        }
        return annotations;
    }

    Space formatWithCommentTree(String prefix, JCTree tree, DCTree.@Nullable DCDocComment commentTree) {
        Space fmt = format(prefix);
        if (commentTree != null) {
            List<Comment> comments = fmt.getComments();
            int i;
            for (i = comments.size() - 1; i >= 0; i--) {
                Comment comment = comments.get(i);
                if (comment.isMultiline() && ((TextComment) comment).getText().startsWith("*")) {
                    break;
                }
            }

            AtomicReference<Javadoc.DocComment> javadoc = new AtomicReference<>();
            for (int j = 0; j < comments.size(); j++) {
                if (i == j) {
                    javadoc.set((Javadoc.DocComment) new ReloadableJava11JavadocVisitor(
                            context,
                            getCurrentPath(),
                            typeMapping,
                            "/*" + ((TextComment) comments.get(j)).getText(),
                            tree
                    ).scan(commentTree, new ArrayList<>(1)));
                    break;
                }
            }

            int javadocIndex = i;
            return fmt.withComments(ListUtils.map(fmt.getComments(), (j, c) ->
                    j == javadocIndex ? javadoc.get().withSuffix(c.getSuffix()) : c));
        }

        return fmt;
    }

    private void addPossibleEmptyStatementsBeforeClosingBrace(List<JRightPadded<Statement>> converted) {
        while (true) {
            int closingBracePosition = positionOfNext("}", null);
            int semicolonPosition = positionOfNext(";", null);

            if (semicolonPosition > -1 && semicolonPosition < closingBracePosition) {
                converted.add(new JRightPadded<>(new J.Empty(randomId(), sourceBefore(";"), Markers.EMPTY), EMPTY, Markers.EMPTY));
                cursor = semicolonPosition + 1;
            } else {
                break;
            }
        }
    }
}
