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
package org.openrewrite.java;


import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * Maps the compiler internal AST to the the Rewrite {@link J} AST.
 * <p>
 * This visitor is not thread safe, as it maintains a {@link #cursor} and {@link #endPosTable}
 * for each compilation unit visited.
 */
public class Java11ParserVisitor extends TreePathScanner<J, Space> {
    private final static int SURR_FIRST = 0xD800;
    private final static int SURR_LAST = 0xDFFF;

    public static final int KIND_BITMASK_INTERFACE = 1 << 9;
    public static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    public static final int KIND_BITMASK_ENUM = 1 << 14;

    private final Path sourcePath;
    private final String source;
    private final boolean relaxedClassTypeMatching;
    private final Collection<NamedStyles> styles;
    private final Map<String, JavaType.Class> sharedClassTypes;
    private final ExecutionContext ctx;

    @SuppressWarnings("NotNullFieldNotInitialized")
    private EndPosTable endPosTable;

    private int cursor = 0;

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public Java11ParserVisitor(Path sourcePath, String source,
                               boolean relaxedClassTypeMatching,
                               Collection<NamedStyles> styles,
                               Map<String, JavaType.Class> sharedClassTypes,
                               ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.source = source;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;
        this.sharedClassTypes = sharedClassTypes;
        this.ctx = ctx;
    }

    @Override
    public J visitAnnotation(AnnotationTree node, Space fmt) {
        skip("@");
        NameTree name = convert(node.getAnnotationType());

        JContainer<Expression> args = null;
        if (node.getArguments().size() > 0) {
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
            String remaining = source.substring(cursor, endPos(node));

            // TODO: technically, if there is code like this, we have a bug, but seems exceedingly unlikely:
            // @MyAnnotation /* Comment () that contains parentheses */ ()

            if (remaining.contains("(") && remaining.contains(")")) {
                args = JContainer.build(
                        sourceBefore("("),
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)),
                        Markers.EMPTY
                );
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
                type(node)
        );
    }

    @Override
    public J visitArrayType(ArrayTypeTree node, Space fmt) {
        Tree typeIdent = node.getType();
        int dimCount = 1;

        while (typeIdent instanceof ArrayTypeTree) {
            dimCount++;
            typeIdent = ((ArrayTypeTree) typeIdent).getType();
        }

        TypeTree elemType = convert(typeIdent);

        List<JRightPadded<Space>> dimensions = new ArrayList<>();
        for (int n = 0; n < dimCount; n++) {
            dimensions.add(padRight(sourceBefore("["), sourceBefore("]")));
        }

        return new J.ArrayType(
                randomId(),
                fmt,
                Markers.EMPTY,
                elemType,
                dimensions
        );
    }

    @Override
    public J visitAssert(AssertTree node, Space fmt) {
        skip("assert");
        return new J.Assert(randomId(), fmt, Markers.EMPTY, convert(((JCAssert) node).cond));
    }

    @Override
    public J visitAssignment(AssignmentTree node, Space fmt) {
        return new J.Assignment(randomId(), fmt, Markers.EMPTY,
                convert(node.getVariable()),
                padLeft(sourceBefore("="), convert(node.getExpression())),
                type(node));
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
                convert(node.getRightOperand()), type(node));
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
        List<StatementTree> statementTrees = new ArrayList<>();
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

        J.Identifier label = node.getLabel() == null ? null : J.Identifier.build(randomId(),
                sourceBefore(node.getLabel().toString()), Markers.EMPTY,
                skip(node.getLabel().toString()), null);

        return new J.Break(randomId(), fmt, Markers.EMPTY, label);
    }

    @Override
    public J visitCase(CaseTree node, Space fmt) {
        Expression pattern;
        if (node.getExpression() == null) {
            pattern = J.Identifier.build(randomId(), Space.EMPTY, Markers.EMPTY, skip("default"), null);
        } else {
            skip("case");
            pattern = convertOrNull(node.getExpression());
        }
        return new J.Case(randomId(), fmt, Markers.EMPTY,
                pattern,
                JContainer.build(sourceBefore(":"), convertStatements(node.getStatements()), Markers.EMPTY));
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
        Map<Integer, JCAnnotation> annotationPosTable = new HashMap<>();
        for (AnnotationTree annotationNode : node.getModifiers().getAnnotations()) {
            JCAnnotation annotation = (JCAnnotation) annotationNode;
            annotationPosTable.put(annotation.pos, annotation);
        }

        Java11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

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

        J.Identifier name = J.Identifier.build(randomId(), sourceBefore(node.getSimpleName().toString()),
                Markers.EMPTY, ((JCClassDecl) node).getSimpleName().toString(), type(node));

        JContainer<J.TypeParameter> typeParams = node.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")),
                Markers.EMPTY);

        JLeftPadded<TypeTree> extendings = node.getExtendsClause() == null ? null :
                padLeft(sourceBefore("extends"), convertOrNull(node.getExtendsClause()));

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
        List<Tree> jcEnums = new ArrayList<>();
        for (Tree tree : node.getMembers()) {
            if (tree instanceof JCVariableDecl) {
                if (hasFlag(((JCVariableDecl) tree).getModifiers(), Flags.ENUM)) {
                    jcEnums.add(tree);
                }
            }
        }

        JRightPadded<Statement> enumSet = null;
        if (!jcEnums.isEmpty()) {
            AtomicBoolean semicolonPresent = new AtomicBoolean(false);

            List<JRightPadded<J.EnumValue>> enumValues = convertAll(jcEnums, commaDelim, t -> {
                // this semicolon is required when there are non-value members, but can still
                // be present when there are not
                semicolonPresent.set(positionOfNext(";", '}') > 0);
                return semicolonPresent.get() ? sourceBefore(";", '}') : EMPTY;
            });

            enumSet = padRight(
                    new J.EnumValueSet(
                            randomId(),
                            enumValues.get(0).getElement().getPrefix(),
                            Markers.EMPTY,
                            ListUtils.map(enumValues, (i, ev) -> i == 0 ? ev.withElement(ev.getElement().withPrefix(EMPTY)) : ev),
                            semicolonPresent.get()
                    ),
                    EMPTY
            );
        }

        List<Tree> membersMultiVariablesSeparated = new ArrayList<>();
        for (Tree m : node.getMembers()) {
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

        J.Block body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                members, sourceBefore("}"));

        return new J.ClassDeclaration(randomId(), fmt, Markers.EMPTY, modifierResults.getLeadingAnnotations(), modifierResults.getModifiers(), kind, name, typeParams, extendings, implementings, body, (JavaType.FullyQualified) type(node));
    }

    @Override
    public J visitCompilationUnit(CompilationUnitTree node, Space fmt) {

        JCCompilationUnit cu = (JCCompilationUnit) node;
        fmt = format(source.substring(0, cu.getStartPosition()));
        cursor(cu.getStartPosition());

        endPosTable = cu.endPositions;

        Map<Integer, JCAnnotation> annotationPosTable = new HashMap<>();
        for (AnnotationTree annotationNode : node.getPackageAnnotations()) {
            JCAnnotation annotation = (JCAnnotation) annotationNode;
            annotationPosTable.put(annotation.pos, annotation);
        }
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
                padLeft(opPrefix, op), convert(((JCAssignOp) node).rhs), type(node));
    }

    @Override
    public J visitConditionalExpression(ConditionalExpressionTree node, Space fmt) {
        return new J.Ternary(randomId(), fmt, Markers.EMPTY,
                convert(node.getCondition()),
                padLeft(sourceBefore("?"), convert(node.getTrueExpression())),
                padLeft(sourceBefore(":"), convert(node.getFalseExpression())),
                type(node));
    }

    @Override
    public J visitContinue(ContinueTree node, Space fmt) {
        skip("continue");
        Name label = node.getLabel();
        return new J.Continue(randomId(), fmt, Markers.EMPTY,
                label == null ? null : J.Identifier.build(randomId(), sourceBefore(label.toString()),
                        Markers.EMPTY, label.toString(), null));
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

        J.Identifier name = J.Identifier.build(randomId(), nameSpace, Markers.EMPTY, node.getName().toString(), type(node));

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

        JRightPadded<Statement> init = convertStatements(node.getInitializer())
                .stream()
                .findAny()
                .orElseGet(() -> padRight(new J.Empty(randomId(), sourceBefore(";"), Markers.EMPTY), EMPTY));

        JRightPadded<Expression> condition = convertOrNull(node.getCondition(), semiDelim);
        if (condition == null) {
            condition = padRight(new J.Empty(randomId(), sourceBefore(";"), Markers.EMPTY), EMPTY);
        }

        List<JRightPadded<Statement>> update;
        if (node.getUpdate().isEmpty()) {
            update = singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY));
        } else {
            update = new ArrayList<>();
            List<? extends ExpressionStatementTree> nodeUpdate = node.getUpdate();
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
        JavaType type = type(node);

        // we don't map all the possible symbol types here, because in many cases they aren't necessary.
        // for method invocations, the J.MethodInvocation will have type attribution, so having the JavaType.Method on the
        // method invocation select is not needed. for fields, this symbol is the only way to determine an identifier represents
        // a field.
        JavaType fieldType = null;
        if (ident.sym instanceof Symbol.VarSymbol) {
            // currently only the first 16 bits are meaningful
            fieldType = JavaType.Variable.build(name, type(ident.sym.owner.type), (int) ident.sym.flags_field & 0xFFFF);
        }

        return J.Identifier.build(randomId(), fmt, Markers.EMPTY, name, type, fieldType);
    }

    @Override
    public J visitIf(IfTree node, Space fmt) {
        skip("if");
        return new J.If(randomId(), fmt, Markers.EMPTY,
                convert(node.getCondition()),
                convert(node.getThenStatement(), this::statementDelim),
                node.getElseStatement() instanceof JCTree.JCStatement ?
                        new J.If.Else(randomId(), sourceBefore("else"), Markers.EMPTY, convert(node.getElseStatement(), this::statementDelim)) :
                        null);
    }

    @Override
    public J visitImport(ImportTree node, Space fmt) {
        skip("import");
        return new J.Import(randomId(), fmt, Markers.EMPTY,
                new JLeftPadded<>(node.isStatic() ? sourceBefore("static") : EMPTY,
                        node.isStatic(), Markers.EMPTY),
                convert(node.getQualifiedIdentifier()));
    }

    @Override
    public J visitInstanceOf(InstanceOfTree node, Space fmt) {
        return new J.InstanceOf(randomId(), fmt, Markers.EMPTY,
                convert(node.getExpression(), t -> sourceBefore("instanceof")),
                convert(node.getType()),
                type(node));
    }

    @Override
    public J visitLabeledStatement(LabeledStatementTree node, Space fmt) {
        skip(node.getLabel().toString());
        return new J.Label(randomId(), fmt, Markers.EMPTY,
                padRight(J.Identifier.build(randomId(), EMPTY, Markers.EMPTY, node.getLabel().toString(), null), sourceBefore(":")),
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
        if (node.getBody() instanceof JCTree.JCBlock) {
            Space prefix = sourceBefore("{");
            cursor--;
            body = convert(node.getBody());
            body = body.withPrefix(prefix);
        } else {
            body = convert(node.getBody());
        }

        return new J.Lambda(randomId(), fmt, Markers.EMPTY, params, arrow, body, type(node));
    }

    @Override
    public J visitLiteral(LiteralTree node, Space fmt) {
        cursor(endPos(node));
        Object value = node.getValue();
        String valueSource = source.substring(((JCLiteral) node).getStartPosition(), endPos(node));
        JavaType.Primitive type = primitive(((JCTree.JCLiteral) node).typetag);

        if (value instanceof Character) {
            char c = (Character) value;
            if (c >= SURR_FIRST && c <= SURR_LAST) {
                return new J.Literal(randomId(), fmt, Markers.EMPTY, null, "''",
                        singletonList(new J.Literal.UnicodeEscape(1,
                                valueSource.substring(3, valueSource.length() - 1))), type);
            }
        } else if (JavaType.Primitive.String.equals(type)) {
            StringBuilder valueSourceWithoutSurrogates = new StringBuilder();
            List<J.Literal.UnicodeEscape> unicodeEscapes = null;

            int i = 0;
            char[] valueSourceArr = valueSource.toCharArray();
            for (int j = 0; j < valueSourceArr.length; j++) {
                char c = valueSourceArr[j];
                if (c == '\\' && j < valueSourceArr.length - 1) {
                    if (valueSourceArr[j + 1] == 'u' && j < valueSource.length() - 5) {
                        String codePoint = valueSource.substring(j + 2, j + 6);
                        int codePointNumeric = Integer.parseInt(codePoint, 16);
                        if (codePointNumeric >= SURR_FIRST && codePointNumeric <= SURR_LAST) {
                            if (unicodeEscapes == null) {
                                unicodeEscapes = new ArrayList<>();
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

        JavaType referenceType = null;
        if (ref.sym instanceof Symbol.MethodSymbol) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) ref.sym;
            referenceType = methodType(methodSymbol.owner.type, methodSymbol, referenceName);
        }

        return new J.MemberReference(randomId(),
                fmt,
                Markers.EMPTY,
                padRight(convert(ref.expr), sourceBefore("::")),
                convertTypeParameters(node.getTypeArguments()),
                padLeft(whitespace(), J.Identifier.build(randomId(),
                        sourceBefore(referenceName),
                        Markers.EMPTY,
                        referenceName,
                        null)),
                type(node),
                referenceType);
    }

    @Override
    public J visitMemberSelect(MemberSelectTree node, Space fmt) {
        JCFieldAccess fieldAccess = (JCFieldAccess) node;
        return new J.FieldAccess(randomId(), fmt, Markers.EMPTY,
                convert(fieldAccess.selected),
                padLeft(sourceBefore("."), J.Identifier.build(randomId(),
                        sourceBefore(fieldAccess.name.toString()), Markers.EMPTY,
                        fieldAccess.name.toString(), null)),
                type(node));
    }

    @Override
    public J visitMethodInvocation(MethodInvocationTree node, Space fmt) {
        JCExpression jcSelect = ((JCTree.JCMethodInvocation) node).getMethodSelect();

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
            name = J.Identifier.build(randomId(), sourceBefore(selectName), Markers.EMPTY, selectName, null);
        } else {
            name = convert(jcSelect);
        }

        JContainer<Expression> args = JContainer.build(sourceBefore("("), node.getArguments().isEmpty() ?
                singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")), Markers.EMPTY);

        Symbol genericSymbol = (jcSelect instanceof JCFieldAccess) ? ((JCFieldAccess) jcSelect).sym : ((JCIdent) jcSelect).sym;

        return new J.MethodInvocation(randomId(), fmt, Markers.EMPTY, select, typeParams, name, args,
                methodType(jcSelect.type, genericSymbol, name.getSimpleName()));
    }

    @Override
    public J visitMethod(MethodTree node, Space fmt) {
        JCMethodDecl jcMethod = (JCMethodDecl) node;

        Map<Integer, JCAnnotation> annotationPosTable = new HashMap<>();
        for (AnnotationTree annotationNode : node.getModifiers().getAnnotations()) {
            JCAnnotation annotation = (JCAnnotation) annotationNode;
            annotationPosTable.put(annotation.pos, annotation);
        }
        Java11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

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
        TypeTree returnType = convertOrNull(node.getReturnType());
        if (returnType != null && !returnTypeAnnotations.isEmpty()) {
            returnType = new J.AnnotatedType(randomId(), Space.EMPTY, Markers.EMPTY,
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
            name = new J.MethodDeclaration.IdentifierWithAnnotations(J.Identifier.build(randomId(), sourceBefore(owner), Markers.EMPTY, owner, null), returnType == null ? returnTypeAnnotations : Collections.emptyList());
        } else {
            name = new J.MethodDeclaration.IdentifierWithAnnotations(J.Identifier.build(randomId(), sourceBefore(node.getName().toString()), Markers.EMPTY,
                    node.getName().toString(), null), returnType == null ? returnTypeAnnotations : Collections.emptyList());
        }

        Space paramFmt = sourceBefore("(");
        JContainer<Statement> params = !node.getParameters().isEmpty() ?
                JContainer.build(paramFmt, convertAll(node.getParameters(), commaDelim, t -> sourceBefore(")")),
                        Markers.EMPTY) :
                JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"),
                        Markers.EMPTY), EMPTY)), Markers.EMPTY);

        JContainer<NameTree> throwss = node.getThrows().isEmpty() ? null :
                JContainer.build(sourceBefore("throws"), convertAll(node.getThrows(), commaDelim, noDelim),
                        Markers.EMPTY);

        J.Block body = convertOrNull(node.getBody());

        JLeftPadded<Expression> defaultValue = node.getDefaultValue() == null ? null :
                padLeft(sourceBefore("default"), convert(node.getDefaultValue()));

        return new J.MethodDeclaration(randomId(), fmt, Markers.EMPTY,
                modifierResults.getLeadingAnnotations(),
                modifierResults.getModifiers(), typeParams,
                returnType, name, params, throwss, body, defaultValue,
                methodType(jcMethod.type, jcMethod.sym, name.getIdentifier().getSimpleName()));
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
            typeExpr = convertOrNull(elementType);
        } else {
            typeExpr = convertOrNull(jcVarType);
        }


        List<J.ArrayDimension> dimensions = new ArrayList<>();
        List<? extends ExpressionTree> nodeDimensions = node.getDimensions();
        for (ExpressionTree dim : nodeDimensions) {
            dimensions.add(new J.ArrayDimension(
                    randomId(),
                    sourceBefore("["),
                    Markers.EMPTY,
                    convert(dim, t -> sourceBefore("]"))));
        }

        Matcher matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)]").matcher(source);
        while (matcher.find(cursor)) {
            cursor(matcher.end());
            dimensions.add(new J.ArrayDimension(
                    randomId(),
                    format(matcher.group(1)),
                    Markers.EMPTY,
                    padRight(new J.Empty(randomId(), format(matcher.group(2)), Markers.EMPTY), EMPTY)));
        }

        JContainer<Expression> initializer = node.getInitializers() == null ? null :
                JContainer.build(sourceBefore("{"), node.getInitializers().isEmpty() ?
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore("}"), Markers.EMPTY), EMPTY)) :
                        convertAll(node.getInitializers(), commaDelim, t -> sourceBefore("}")), Markers.EMPTY);

        return new J.NewArray(randomId(), fmt, Markers.EMPTY, typeExpr, dimensions,
                initializer, type(node));
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
        TypeTree clazz = endPos(node.getIdentifier()) >= 0 ? convertOrNull(node.getIdentifier()) : null;

        JContainer<Expression> args = null;
        if (positionOfNext("(", '{') > -1) {
            args = JContainer.build(sourceBefore("("),
                    node.getArguments().isEmpty() ?
                            singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                            convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")), Markers.EMPTY);
        }

        J.Block body = null;
        if (node.getClassBody() != null) {
            Space bodyPrefix = sourceBefore("{");

            // we don't care about the compiler-inserted default constructor,
            // since it will never be subject to refactoring
            List<Tree> members = new ArrayList<>();
            for (Tree m : node.getClassBody().getMembers()) {
                if (!(m instanceof JCMethodDecl) || (((JCMethodDecl) m).getModifiers().flags & Flags.GENERATEDCONSTR) == 0L) {
                    members.add(m);
                }
            }

            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, new JRightPadded<>(false, EMPTY, Markers.EMPTY),
                    convertAll(members, noDelim, noDelim), sourceBefore("}"));
        }

        JCNewClass jcNewClass = (JCNewClass) node;
        JavaType.Method constructorType = methodType(jcNewClass.constructorType, jcNewClass.constructor, "<constructor>");

        return new J.NewClass(randomId(), fmt, Markers.EMPTY, encl, whitespaceBeforeNew,
                clazz, args, body, constructorType,
                type(jcNewClass.type));
    }

    @Override
    public J visitParameterizedType(ParameterizedTypeTree node, Space fmt) {
        return new J.ParameterizedType(randomId(), fmt, Markers.EMPTY, convert(node.getType()), convertTypeParameters(node.getTypeArguments()));
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
        return new J.Return(randomId(), fmt, Markers.EMPTY, convertOrNull(node.getExpression()));
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
            List<JRightPadded<J.Try.Resource>> resourceList = new ArrayList<>();

            for (int i = 0; i < node.getResources().size(); i++) {
                Tree resource = node.getResources().get(i);
                J.VariableDeclarations resourceVar = convert(resource);
                boolean semicolonPresent = true;
                if (i == node.getResources().size() - 1) {
                    semicolonPresent = positionOfNext(";", ')') > 0;
                }

                Space resourcePrefix = resourceVar.getPrefix();
                resourceVar = resourceVar.withPrefix(EMPTY); // moved to the containing Try.Resource

                if (semicolonPresent) {
                    resourceVar = resourceVar.getPadding().withVariables(Space.formatLastSuffix(resourceVar
                            .getPadding().getVariables(), sourceBefore(";")));
                }

                J.Try.Resource tryResource = new J.Try.Resource(randomId(), resourcePrefix, Markers.EMPTY,
                        resourceVar.withPrefix(EMPTY), semicolonPresent);
                resourceList.add(padRight(tryResource, i == node.getResources().size() - 1 ?
                        sourceBefore(")") : EMPTY));
            }

            resources = JContainer.build(before, resourceList, Markers.EMPTY);
        }

        J.Block block = convert(node.getBlock());
        List<J.Try.Catch> catches = convertAll(node.getCatches());

        JLeftPadded<J.Block> finallyy = node.getFinallyBlock() == null ? null :
                padLeft(sourceBefore("finally"), convert(node.getFinallyBlock()));

        return new J.Try(randomId(), fmt, Markers.EMPTY, resources, block, catches, finallyy);
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
        return new J.AnnotatedType(randomId(), fmt, Markers.EMPTY, convertAll(node.getAnnotations()),
                convert(node.getUnderlyingType()));
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

        return new J.TypeParameter(randomId(), fmt, Markers.EMPTY, annotations, name, bounds);
    }

    private <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = J.Identifier.build(randomId(), EMPTY, Markers.EMPTY, part, null);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                Space identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Space.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                //noinspection ResultOfMethodCallIgnored
                whitespaceSuffix.matches();
                Space namePrefix = i == parts.length - 1 ? Space.EMPTY : format(whitespaceSuffix.group(1));

                expr = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        expr,
                        padLeft(namePrefix, J.Identifier.build(randomId(), identFmt, Markers.EMPTY, part.trim(), null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.Class.build(fullName) :
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

        return new J.Unary(randomId(), fmt, Markers.EMPTY, op, expr, type(node));
    }

    @Override
    public J visitVariable(VariableTree node, Space fmt) {
        return hasFlag(node.getModifiers(), Flags.ENUM) ?
                visitEnumVariable(node, fmt) :
                visitVariables(singletonList(node), fmt); // method arguments cannot be multi-declarations
    }

    private J.VariableDeclarations visitVariables(List<VariableTree> nodes, Space fmt) {
        JCTree.JCVariableDecl node = (JCVariableDecl) nodes.get(0);

        JCExpression vartype = node.vartype;

        Map<Integer, JCAnnotation> annotationPosTable = new HashMap<>();
        for (JCAnnotation annotationNode : node.getModifiers().getAnnotations()) {
            annotationPosTable.put(annotationNode.pos, annotationNode);
        }
        Java11ModifierResults modifierResults = sortedModifiersAndAnnotations(node.getModifiers(), annotationPosTable);

        List<J.Annotation> typeExprAnnotations = collectAnnotations(annotationPosTable);

        TypeTree typeExpr;
        if (vartype == null || vartype instanceof JCErroneous) {
            typeExpr = null;
        } else if (endPos(vartype) < 0) {
            if (skipIfPresent("var")) {
                typeExpr = new J.VarType(randomId(), Space.EMPTY, Markers.EMPTY, type(vartype));
            } else {
                typeExpr = null; // this is a lambda parameter with an inferred type expression
            }
        } else if (vartype instanceof JCArrayTypeTree) {
            // we'll capture the array dimensions in a bit, just convert the element type
            JCExpression elementType = ((JCArrayTypeTree) vartype).elemtype;
            while (elementType instanceof JCArrayTypeTree) {
                elementType = ((JCArrayTypeTree) elementType).elemtype;
            }
            typeExpr = convert(elementType);
        } else {
            typeExpr = convert(vartype);
        }

        if (typeExpr != null && !typeExprAnnotations.isEmpty()) {
            typeExpr = new J.AnnotatedType(randomId(), Space.EMPTY, Markers.EMPTY, typeExprAnnotations, typeExpr);
        }

        Supplier<List<JLeftPadded<Space>>> dimensions = () -> {
            Matcher matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)]").matcher(source);
            List<JLeftPadded<Space>> dims = new ArrayList<>();
            while (matcher.find(cursor)) {
                cursor(matcher.end());
                dims.add(padLeft(format(matcher.group(1)), format(matcher.group(2))));
            }
            return dims;
        };

        List<JLeftPadded<Space>> beforeDimensions = dimensions.get();

        Space varargs = null;
        if (!(typeExpr instanceof J.VarType)) {
            String vartypeString = typeExpr == null ? "" : source.substring(vartype.getStartPosition(), endPos(vartype));
            Matcher varargMatcher = Pattern.compile("(\\s*)\\.{3}").matcher(vartypeString);
            if (varargMatcher.find()) {
                Matcher matcher = Pattern.compile("\\G(\\s*)\\.{3}").matcher(source);
                if (matcher.find(cursor)) {
                    cursor(matcher.end());
                }
                varargs = format(varargMatcher.group(1));
            }
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            JCVariableDecl n = (JCVariableDecl) nodes.get(i);

            Space namedVarPrefix = sourceBefore(n.getName().toString());

            J.Identifier name = J.Identifier.build(randomId(), EMPTY, Markers.EMPTY, n.getName().toString(), type(node));
            List<JLeftPadded<Space>> dimensionsAfterName = dimensions.get();

            vars.add(
                    padRight(
                            new J.VariableDeclarations.NamedVariable(randomId(), namedVarPrefix, Markers.EMPTY,
                                    name,
                                    dimensionsAfterName,
                                    n.init != null ? padLeft(sourceBefore("="), convertOrNull(n.init)) : null,
                                    type(n)
                            ),
                            i == nodes.size() - 1 ? EMPTY : sourceBefore(",")
                    )
            );
        }

        return new J.VariableDeclarations(randomId(), fmt, Markers.EMPTY, modifierResults.getLeadingAnnotations(), modifierResults.getModifiers(), typeExpr, varargs, beforeDimensions, vars);
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

        return new J.Wildcard(randomId(), fmt, Markers.EMPTY, bound, convertOrNull(wildcard.inner));
    }

    /**
     * --------------
     * Conversion utilities
     * --------------
     */

    private <J2 extends J> J2 convert(Tree t) {
        try {
            String prefix = source.substring(cursor, max(((JCTree) t).getStartPosition(), cursor));
            cursor += prefix.length();
            @SuppressWarnings("unchecked") J2 j = (J2) scan(t, format(prefix));
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

    private <J2 extends J> JRightPadded<J2> convert(Tree t, Function<Tree, Space> suffix) {
        J2 j = convert(t);
        @SuppressWarnings("ConstantConditions") JRightPadded<J2> rightPadded = j == null ? null :
                new JRightPadded<>(j, suffix.apply(t), Markers.EMPTY);
        cursor(max(endPos(t), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
        return rightPadded;
    }

    private long lineNumber(Tree tree) {
        return source.substring(0, ((JCTree) tree).getStartPosition()).chars().filter(c -> c == '\n').count() + 1;
    }

    @Nullable
    private <T extends J> T convertOrNull(@Nullable Tree t) {
        return t == null ? null : convert(t);
    }

    @Nullable
    private <J2 extends J> JRightPadded<J2> convertOrNull(@Nullable Tree t, Function<Tree, Space> suffix) {
        return t == null ? null : convert(t, suffix);
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
        List<JRightPadded<J2>> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            converted.add(convert(trees.get(i), i == trees.size() - 1 ? suffix : innerSuffix));
        }
        return converted;
    }

    @Nullable
    private JContainer<Expression> convertTypeParameters(@Nullable List<? extends Tree> typeArguments) {
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
                t instanceof JCMethodInvocation ||
                t instanceof JCNewClass ||
                t instanceof JCReturn ||
                t instanceof JCThrow ||
                t instanceof JCUnary ||
                t instanceof JCExpressionStatement ||
                t instanceof JCVariableDecl) {
            return sourceBefore(";");
        }

        if (t instanceof JCLabeledStatement) {
            return statementDelim(((JCLabeledStatement) t).getStatement());
        }

        if (t instanceof JCMethodDecl) {
            JCMethodDecl m = (JCMethodDecl) t;
            return sourceBefore(m.body == null || m.defaultValue != null ? ";" : "");
        }

        return EMPTY;
    }

    @SuppressWarnings("unchecked")
    private List<JRightPadded<Statement>> convertStatements(@Nullable List<? extends Tree> trees) {
        if (trees == null)
            return emptyList();

        Map<Integer, List<Tree>> treesGroupedByStartPosition = new LinkedHashMap<>();
        for (Tree t : trees) {
            treesGroupedByStartPosition.computeIfAbsent(((JCTree) t).getStartPosition(), k -> new ArrayList<>()).add(t);
        }

        List<JRightPadded<Statement>> converted = new ArrayList<>();
        for (List<? extends Tree> treeGroup : treesGroupedByStartPosition.values()) {
            if (treeGroup.size() == 1) {
                converted.add(convert(treeGroup.get(0), this::statementDelim));
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

    @Nullable
    private JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol, String methodName) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        if (methodSymbol != null && selectType != null) {
            Function<com.sun.tools.javac.code.Type, JavaType.Method.Signature> signature = t -> {
                if (t instanceof MethodType) {
                    MethodType mt = (MethodType) t;

                    List<JavaType> paramTypes = new ArrayList<>();
                    for (com.sun.tools.javac.code.Type argtype : mt.argtypes) {
                        if (argtype != null) {
                            JavaType javaType = type(argtype);
                            paramTypes.add(javaType);
                        }
                    }

                    return new JavaType.Method.Signature(type(mt.restype), paramTypes);
                }
                return null;
            };

            JavaType.Method.Signature genericSignature;
            if (methodSymbol.type instanceof com.sun.tools.javac.code.Type.ForAll) {
                genericSignature = signature.apply(((com.sun.tools.javac.code.Type.ForAll) methodSymbol.type).qtype);
            } else {
                genericSignature = signature.apply(methodSymbol.type);
            }

            List<String> paramNames = new ArrayList<>();
            for (Symbol.VarSymbol p : methodSymbol.params()) {
                String s = p.name.toString();
                paramNames.add(s);
            }

            List<JavaType.FullyQualified> exceptionTypes = new ArrayList<>();
            if (selectType instanceof MethodType) {
                for (com.sun.tools.javac.code.Type exceptionType : ((MethodType) selectType).thrown) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(exceptionType));
                    if (javaType == null) {
                        //If the type cannot be resolved to a class (it might not be on the classpath or it might have
                        //been mapped to cyclic, build the class.
                        if (exceptionType instanceof ClassType) {
                            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) exceptionType.tsym;
                            javaType = JavaType.Class.build(sym.className());
                        }
                    }
                    if (javaType != null) {
                        //If the exception type is not resolved, it is not added to the list of exceptions.
                        exceptionTypes.add(javaType);
                    }
                }
            }

            JavaType.FullyQualified declaringType = null;
            if (methodSymbol.owner instanceof Symbol.ClassSymbol || methodSymbol.owner instanceof Symbol.TypeVariableSymbol) {
                declaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type));
            } else if (methodSymbol.owner instanceof Symbol.VarSymbol) {
                declaringType = new JavaType.GenericTypeVariable(methodSymbol.owner.name.toString(), null);
            }

            if (declaringType == null) {
                return null;
            }

            return JavaType.Method.build(
                    // currently only the first 16 bits are meaningful
                    (int) methodSymbol.flags_field & 0xFFFF,
                    declaringType,
                    methodName,
                    genericSignature,
                    signature.apply(selectType),
                    paramNames,
                    Collections.unmodifiableList(exceptionTypes)
            );
        }

        return null;
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        return type(type, emptyList());
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type, List<Symbol> stack) {
        if (type instanceof ClassType) {
            if (type instanceof com.sun.tools.javac.code.Type.ErrorType) {
                return null;
            }

            ClassType classType = (ClassType) type;
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            ClassType symType = (ClassType) sym.type;
            if (!sym.isCompleted()) {
                try {
                    sym.complete();
                } catch (Symbol.CompletionFailure e) {
                    //During attribution, we will likely encounter symbols that have been parsed but are not
                    // on the parser's classpath. Calling complete on the symbol will result in an exception
                    // being thrown. We eat the exception
                }
            }

            if (stack.contains(sym))
                return new JavaType.Cyclic(sym.className());
            else {
                JavaType.Class clazz = sharedClassTypes.get(sym.className());
                List<Symbol> stackWithSym = new ArrayList<>(stack);
                stackWithSym.add(sym);
                if (clazz == null) {

                    List<JavaType.Variable> fields;
                    if (sym.members_field == null) {
                        fields = emptyList();
                    } else {
                        fields = new ArrayList<>();
                        for (Symbol elem : sym.members_field.getSymbols()) {
                            if (elem instanceof Symbol.VarSymbol) {
                                fields.add(JavaType.Variable.build(
                                        elem.name.toString(),
                                        type(elem.type, stackWithSym),
                                        // currently only the first 16 bits are meaningful
                                        (int) elem.flags_field & 0xFFFF
                                ));
                            }
                        }
                    }

                    List<JavaType.FullyQualified> interfaces;
                    if (symType.interfaces_field == null) {
                        interfaces = emptyList();
                    } else {
                        interfaces = new ArrayList<>(symType.interfaces_field.length());
                        for (com.sun.tools.javac.code.Type iParam : symType.interfaces_field) {
                            JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam, stackWithSym));
                            if (javaType != null) {
                                interfaces.add(javaType);
                            }
                        }
                    }
                    JavaType.Class.Kind kind;
                    if ((sym.flags_field & KIND_BITMASK_ENUM) != 0) {
                        kind = JavaType.Class.Kind.Enum;
                    } else if ((sym.flags_field & KIND_BITMASK_ANNOTATION) != 0) {
                        kind = JavaType.Class.Kind.Annotation;
                    } else if ((sym.flags_field & KIND_BITMASK_INTERFACE) != 0) {
                        kind = JavaType.Class.Kind.Interface;
                    } else {
                        kind = JavaType.Class.Kind.Class;
                    }

                    JavaType.FullyQualified owner = null;
                    if (sym.owner instanceof Symbol.ClassSymbol) {
                        owner = TypeUtils.asFullyQualified(type(sym.owner.type, stackWithSym));
                    }
                    clazz = JavaType.Class.build(
                            //Currently only the first 16 bits are meaninful
                            (int) sym.flags_field & 0xFFFF,
                            sym.className(),
                            kind,
                            fields,
                            interfaces,
                            null,
                            TypeUtils.asFullyQualified(type(classType.supertype_field != null ? classType.supertype_field : symType.supertype_field, stackWithSym)),
                            owner,
                            relaxedClassTypeMatching);
                    sharedClassTypes.put(clazz.getFullyQualifiedName(), clazz);
                }

                List<JavaType> typeParameters;
                if (classType.typarams_field == null) {
                    typeParameters = emptyList();
                } else {
                    typeParameters = new ArrayList<>(classType.typarams_field.length());
                    for (com.sun.tools.javac.code.Type tParam : classType.typarams_field) {
                        JavaType javaType = type(tParam, stackWithSym);
                        if (javaType != null) {
                            typeParameters.add(javaType);
                        }
                    }
                }

                if (!typeParameters.isEmpty()) {
                    return JavaType.Parameterized.build(clazz, typeParameters);
                } else {
                    return clazz;
                }
            }
        } else if (type instanceof TypeVar) {
            return new JavaType.GenericTypeVariable(type.tsym.name.toString(),
                    TypeUtils.asFullyQualified(type(type.getUpperBound(), stack)));
        } else if (type instanceof JCPrimitiveType) {
            return primitive(type.getTag());
        } else if (type instanceof JCVoidType) {
            return JavaType.Primitive.Void;
        } else if (type instanceof ArrayType) {
            return new JavaType.Array(type(((ArrayType) type).elemtype, stack));
        } else if (type instanceof WildcardType) {

            // TODO: For now we are just mapping wildcards into their bound types and we are not accounting for the
            //       "bound kind"
            // <?>                --> java.lang.Object
            // <? extends Number> --> Number
            // <? super Number>   --> Number
            // <? super T>        --> GenericTypeVariable

            WildcardType wildcard = (WildcardType) type;
            if (wildcard.kind == BoundKind.UNBOUND) {
                return JavaType.Class.OBJECT;
            } else {
                return type(wildcard.type, stack);
            }
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        } else {
            return null;
        }
    }

    @Nullable
    private JavaType type(Tree t) {
        return type(((JCTree) t).type);
    }

    private JavaType.Primitive primitive(TypeTag tag) {
        switch (tag) {
            case BOOLEAN:
                return JavaType.Primitive.Boolean;
            case BYTE:
                return JavaType.Primitive.Byte;
            case CHAR:
                return JavaType.Primitive.Char;
            case DOUBLE:
                return JavaType.Primitive.Double;
            case FLOAT:
                return JavaType.Primitive.Float;
            case INT:
                return JavaType.Primitive.Int;
            case LONG:
                return JavaType.Primitive.Long;
            case SHORT:
                return JavaType.Primitive.Short;
            case VOID:
                return JavaType.Primitive.Void;
            case NONE:
                return JavaType.Primitive.None;
            case CLASS:
                return JavaType.Primitive.String;
            case BOT:
                return JavaType.Primitive.Null;
            default:
                throw new IllegalArgumentException("Unknown type tag " + tag);
        }
    }

    /**
     * --------------
     * Other convenience utilities
     * --------------
     */

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

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
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
            if (inSingleLineComment && source.charAt(delimIndex) == '\n') {
                inSingleLineComment = false;
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
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
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length(); delimIndex++) {
            if (inSingleLineComment && (source.charAt(delimIndex) == '\n' || source.charAt(delimIndex) == '\r')) {
                inSingleLineComment = false;
            } else {
                if (source.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            continue;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            continue;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex++;
                            continue;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (!Character.isWhitespace(source.substring(delimIndex, delimIndex + 1).charAt(0))) {
                        break; // found it!
                    }
                }
            }
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length();
        return format(prefix);
    }

    private String skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (source.startsWith(token, cursor))
            cursor += token.length();
        return token;
    }

    /**
     * Advances the cursor if the current cursor position starts with the given token.
     *
     * @param token Token to skip
     * @return true if the token is found, otherwise false.
     */
    private boolean skipIfPresent(String token) {
        if (source.startsWith(token, cursor)) {
            cursor += token.length();
            return true;
        }
        return false;
    }

    // Only exists as a function to make it easier to debug unexpected cursor shifts
    private void cursor(int n) {
        cursor = n;
    }

    private boolean hasFlag(ModifiersTree modifiers, long flag) {
        return (((JCModifiers) modifiers).flags & flag) != 0L;
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
                .collect(Collectors.toMap(Field::getName, field -> {
                    try {
                        return (Long) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }));

        List<String> all = new ArrayList<>();
        for (Map.Entry<String, Long> flagNameAndCode : allFlags.entrySet()) {
            if ((flagNameAndCode.getValue() & flags) != 0L) {
                all.add(flagNameAndCode.getKey());
            }
        }
        return all;
    }

    /**
     * Leading Annotations and modifiers in the order they appear in the source, which is not necessarily the same as the order in
     * which they appear in the OpenJDK AST
     */
    private Java11ModifierResults sortedModifiersAndAnnotations(ModifiersTree modifiers, Map<Integer, JCAnnotation> annotationPosTable) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> sortedModifiers = new ArrayList<>();
        List<J.Annotation> currentAnnotations = new ArrayList<>();
        boolean afterFirstModifier = false;
        boolean inComment = false;
        boolean inMultilineComment = false;
        final AtomicReference<String> word = new AtomicReference<>("");
        int afterLastModifierPosition = cursor;
        int lastAnnotationPosition = cursor;
        for (int i = cursor; i < source.length(); i++) {
            if (annotationPosTable.containsKey(i)) {
                J.Annotation annotation = convert(annotationPosTable.get(i));
                if (afterFirstModifier) {
                    currentAnnotations.add(annotation);
                } else {
                    leadingAnnotations.add(annotation);
                }
                i = cursor;
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
            } else if (inComment && c == '\n' || c == '\r') {
                inComment = false;
            } else if (!inMultilineComment && !inComment) {
                if (Character.isWhitespace(c)) {
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
        return new Java11ModifierResults(leadingAnnotations, sortedModifiers);
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
        return new J.Modifier(randomId(), modFormat, Markers.EMPTY, type, annotations);
    }

    private List<J.Annotation> collectAnnotations(Map<Integer, JCAnnotation> annotationPosTable) {
        int maxAnnotationPosition = annotationPosTable.keySet().stream().mapToInt(i -> i).max().orElse(0);
        List<J.Annotation> annotations = new ArrayList<>();
        boolean inComment = false;
        boolean inMultilineComment = false;
        for (int i = cursor; i <= maxAnnotationPosition; i++) {
            if (annotationPosTable.containsKey(i)) {
                annotations.add(convert(annotationPosTable.get(i)));
                i = cursor;
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
            } else if (inComment && c == '\n' || c == '\r') {
                inComment = false;
            } else if (!inMultilineComment && !inComment) {
                if (!Character.isWhitespace(c)) {
                    break;
                }
            }
        }
        return annotations;
    }
}
