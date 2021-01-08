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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Java11ParserVisitor extends TreePathScanner<J, Space> {
    private static final Logger logger = LoggerFactory.getLogger(Java11ParserVisitor.class);

    private final Path sourcePath;
    private final String source;
    private final boolean relaxedClassTypeMatching;
    private final Collection<JavaStyle> styles;
    private final Map<String, JavaType.Class> sharedClassTypes;

    private EndPosTable endPosTable;
    private int cursor = 0;

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public Java11ParserVisitor(Path sourcePath, String source, boolean relaxedClassTypeMatching, Collection<JavaStyle> styles,
                               Map<String, JavaType.Class> sharedClassTypes) {
        this.sourcePath = sourcePath;
        this.source = source;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;
        this.sharedClassTypes = sharedClassTypes;
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

            args = JContainer.build(argsPrefix, expressions);
        } else {
            String remaining = source.substring(cursor, endPos(node));

            // TODO: technically, if there is code like this, we have a bug, but seems exceedingly unlikely:
            // @MyAnnotation /* Comment () that contains parentheses */ ()

            if (remaining.contains("(") && remaining.contains(")")) {
                args = JContainer.build(
                        sourceBefore("("),
                        singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY))
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
        return new J.Assign(randomId(), fmt, Markers.EMPTY,
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
        Space stat = null;

        if ((((JCBlock) node).flags & (long) Flags.STATIC) != 0L) {
            skip("static");
            stat = sourceBefore("{");
        } else {
            skip("{");
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

        J.Ident label = node.getLabel() == null ? null : J.Ident.build(randomId(),
                sourceBefore(node.getLabel().toString()), Markers.EMPTY,
                skip(node.getLabel().toString()), null);

        return new J.Break(randomId(), fmt, Markers.EMPTY, label);
    }

    @Override
    public J visitCase(CaseTree node, Space fmt) {
        Expression pattern;
        if (node.getExpression() == null) {
            pattern = J.Ident.build(randomId(), Space.EMPTY, Markers.EMPTY, skip("default"), null);
        } else {
            skip("case");
            pattern = convertOrNull(node.getExpression());
        }
        return new J.Case(randomId(), fmt, Markers.EMPTY,
                pattern,
                JContainer.build(sourceBefore(":"), convertStatements(node.getStatements())));
    }

    @Override
    public J visitCatch(CatchTree node, Space fmt) {
        skip("catch");

        Space paramPrefix = sourceBefore("(");
        J.VariableDecls paramDecl = convert(node.getParameter());
        paramDecl = paramDecl.withVars(Space.formatLastSuffix(paramDecl.getVars(), sourceBefore(")")));

        J.Parentheses<J.VariableDecls> param = new J.Parentheses<>(randomId(), paramPrefix,
                Markers.EMPTY, padRight(paramDecl, EMPTY));

        return new J.Try.Catch(randomId(), fmt, Markers.EMPTY, param, convert(node.getBlock()));
    }

    @Override
    public J visitClass(ClassTree node, Space fmt) {
        List<J.Annotation> annotations = convertAll(node.getModifiers().getAnnotations());
        List<J.Modifier> modifiers = sortedFlags(node.getModifiers());

        JLeftPadded<J.ClassDecl.Kind> kind;
        if (hasFlag(node.getModifiers(), Flags.ENUM)) {
            kind = padLeft(sourceBefore("enum"), J.ClassDecl.Kind.Enum);
        } else if (hasFlag(node.getModifiers(), Flags.ANNOTATION)) {
            // note that annotations ALSO have the INTERFACE flag
            kind = padLeft(sourceBefore("@interface"), J.ClassDecl.Kind.Annotation);
        } else if (hasFlag(node.getModifiers(), Flags.INTERFACE)) {
            kind = padLeft(sourceBefore("interface"), J.ClassDecl.Kind.Interface);
        } else {
            kind = padLeft(sourceBefore("class"), J.ClassDecl.Kind.Class);
        }

        J.Ident name = J.Ident.build(randomId(), sourceBefore(node.getSimpleName().toString()),
                Markers.EMPTY, ((JCClassDecl) node).getSimpleName().toString(), type(node));

        JContainer<J.TypeParameter> typeParams = node.getTypeParameters().isEmpty() ? null : JContainer.build(
                sourceBefore("<"),
                convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")));

        JLeftPadded<TypeTree> extendings = node.getExtendsClause() == null ? null :
                padLeft(sourceBefore("extends"), convertOrNull(node.getExtendsClause()));

        JContainer<TypeTree> implementings = null;
        if (node.getImplementsClause() != null && !node.getImplementsClause().isEmpty()) {
            Space implementsPrefix = sourceBefore(kind.getElem() == J.ClassDecl.Kind.Interface ?
                    "extends" : "implements");

            implementings = JContainer.build(implementsPrefix, convertAll(node.getImplementsClause(), commaDelim, noDelim));
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

            enumSet = padRight(new J.EnumValueSet(randomId(), EMPTY, Markers.EMPTY, enumValues, semicolonPresent.get()), EMPTY);
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

        J.Block body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, null, members, sourceBefore("}"));

        return new J.ClassDecl(randomId(), fmt, Markers.EMPTY, annotations, modifiers, kind, name, typeParams, extendings, implementings, body, (JavaType.Class) type(node));
    }

    @Override
    public J visitCompilationUnit(CompilationUnitTree node, Space fmt) {
        logger.debug("Building AST for: " + sourcePath);

        JCCompilationUnit cu = (JCCompilationUnit) node;
        fmt = format(source.substring(0, cu.getStartPosition()));
        cursor(cu.getStartPosition());

        endPosTable = cu.endPositions;

        J.Package packageDecl = null;
        if (cu.getPackageName() != null) {
            Space packagePrefix = sourceBefore("package");
            packageDecl = new J.Package(randomId(), packagePrefix, Markers.EMPTY,
                    convert(cu.getPackageName()));
        }

        return new J.CompilationUnit(
                randomId(),
                fmt,
                Markers.EMPTY,
                sourcePath,
                packageDecl == null ? null : padRight(packageDecl, sourceBefore(";")),
                convertAll(node.getImports(), this::statementDelim, this::statementDelim),
                convertAll(node.getTypeDecls().stream().filter(JCClassDecl.class::isInstance).collect(toList())),
                format(source.substring(cursor)),
                styles
        );
    }

    @Override
    public J visitCompoundAssignment(CompoundAssignmentTree node, Space fmt) {
        Expression left = convert(((JCAssignOp) node).lhs);

        Space opPrefix = whitespace();
        J.AssignOp.Type op;
        switch (((JCAssignOp) node).getTag()) {
            case PLUS_ASG:
                skip("+=");
                op = J.AssignOp.Type.Addition;
                break;
            case MINUS_ASG:
                skip("-=");
                op = J.AssignOp.Type.Subtraction;
                break;
            case DIV_ASG:
                skip("/=");
                op = J.AssignOp.Type.Division;
                break;
            case MUL_ASG:
                skip("*=");
                op = J.AssignOp.Type.Multiplication;
                break;
            case MOD_ASG:
                skip("%=");
                op = J.AssignOp.Type.Modulo;
                break;
            case BITAND_ASG:
                skip("&=");
                op = J.AssignOp.Type.BitAnd;
                break;
            case BITOR_ASG:
                skip("|=");
                op = J.AssignOp.Type.BitOr;
                break;
            case BITXOR_ASG:
                skip("^=");
                op = J.AssignOp.Type.BitXor;
                break;
            case SL_ASG:
                skip("<<=");
                op = J.AssignOp.Type.LeftShift;
                break;
            case SR_ASG:
                skip(">>=");
                op = J.AssignOp.Type.RightShift;
                break;
            case USR_ASG:
                skip(">>>=");
                op = J.AssignOp.Type.UnsignedRightShift;
                break;
            default:
                throw new IllegalArgumentException("Unexpected compound assignment tag " + ((JCAssignOp) node).getTag());
        }

        return new J.AssignOp(randomId(), fmt, Markers.EMPTY, left,
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
                label == null ? null : J.Ident.build(randomId(), sourceBefore(label.toString()),
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
        skip(node.getName().toString());
        J.Ident name = J.Ident.build(randomId(), EMPTY, Markers.EMPTY, node.getName().toString(), type(node));

        J.NewClass initializer = null;
        if (source.charAt(endPos(node) - 1) == ')' || source.charAt(endPos(node) - 1) == '}') {
            initializer = convert(node.getInitializer());
        }

        return new J.EnumValue(randomId(), fmt, Markers.EMPTY, name, initializer);
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
        cursor += node.getName().toString().length();
        return J.Ident.build(randomId(), fmt, Markers.EMPTY, node.getName().toString(), type(node));
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
                node.isStatic() ? sourceBefore("static") : null,
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
                padRight(J.Ident.build(randomId(), EMPTY, Markers.EMPTY, node.getLabel().toString(), null), sourceBefore(":")),
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
        JavaType.Primitive type = primitive(((JCTree.JCLiteral) node).typetag);
        return new J.Literal(randomId(), fmt, Markers.EMPTY, value,
                source.substring(((JCLiteral) node).getStartPosition(), endPos(node)), type);
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

        return new J.MemberReference(randomId(),
                fmt,
                Markers.EMPTY,
                convert(ref.expr),
                convertTypeParameters(node.getTypeArguments()),
                padLeft(sourceBefore("::"), J.Ident.build(randomId(),
                        sourceBefore(referenceName),
                        Markers.EMPTY,
                        referenceName,
                        null)),
                type(node));
    }

    @Override
    public J visitMemberSelect(MemberSelectTree node, Space fmt) {
        JCFieldAccess fieldAccess = (JCFieldAccess) node;
        return new J.FieldAccess(randomId(), fmt, Markers.EMPTY,
                convert(fieldAccess.selected),
                padLeft(sourceBefore("."), J.Ident.build(randomId(),
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
                    t -> sourceBefore(">")));
        }

        J.Ident name;
        if (jcSelect instanceof JCFieldAccess) {
            String selectName = ((JCFieldAccess) jcSelect).name.toString();
            name = J.Ident.build(randomId(), sourceBefore(selectName), Markers.EMPTY, selectName, null);
        } else {
            name = convert(jcSelect);
        }

        JContainer<Expression> args = JContainer.build(sourceBefore("("), node.getArguments().isEmpty() ?
                singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)) :
                convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")));

        Symbol genericSymbol = (jcSelect instanceof JCFieldAccess) ? ((JCFieldAccess) jcSelect).sym : ((JCIdent) jcSelect).sym;

        return new J.MethodInvocation(randomId(), fmt, Markers.EMPTY, select, typeParams, name, args,
                methodType(jcSelect.type, genericSymbol, name.getSimpleName()));
    }

    @Override
    public J visitMethod(MethodTree node, Space fmt) {
        JCMethodDecl jcMethod = (JCMethodDecl) node;

        List<J.Annotation> annotations = convertAll(node.getModifiers().getAnnotations());
        List<J.Modifier> modifiers = sortedFlags(node.getModifiers());

        // see https://docs.oracle.com/javase/tutorial/java/generics/methods.html
        JContainer<J.TypeParameter> typeParams = node.getTypeParameters().isEmpty() ? null :
                JContainer.build(sourceBefore("<"),
                        convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")));

        TypeTree returnType = convertOrNull(node.getReturnType());

        Symbol.MethodSymbol nodeSym = jcMethod.sym;

        J.Ident name;
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
            name = J.Ident.build(randomId(), sourceBefore(owner), Markers.EMPTY, owner, null);
        } else {
            name = J.Ident.build(randomId(), sourceBefore(node.getName().toString()), Markers.EMPTY,
                    node.getName().toString(), null);
        }

        Space paramFmt = sourceBefore("(");
        JContainer<Statement> params = !node.getParameters().isEmpty() ?
                JContainer.build(paramFmt, convertAll(node.getParameters(), commaDelim, t -> sourceBefore(")"))) :
                JContainer.build(paramFmt, singletonList(padRight(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY), EMPTY)));

        JContainer<NameTree> throwss = node.getThrows().isEmpty() ? null :
                JContainer.build(sourceBefore("throws"), convertAll(node.getThrows(), commaDelim, noDelim));

        J.Block body = convertOrNull(node.getBody());

        JLeftPadded<Expression> defaultValue = node.getDefaultValue() == null ? null :
                padLeft(sourceBefore("default"), convert(node.getDefaultValue()));

        return new J.MethodDecl(randomId(), fmt, Markers.EMPTY, annotations, modifiers, typeParams,
                returnType, name, params, throwss, body, defaultValue,
                methodType(jcMethod.type, jcMethod.sym, name.getSimpleName()));
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
                        convertAll(node.getInitializers(), commaDelim, t -> sourceBefore("}")));

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
                            convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")));
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

            body = new J.Block(randomId(), bodyPrefix, Markers.EMPTY, null,
                    convertAll(members, noDelim, noDelim), sourceBefore("}"));
        }

        return new J.NewClass(randomId(), fmt, Markers.EMPTY, encl, whitespaceBeforeNew,
                clazz, args, body, type(((JCNewClass) node).type));
    }

    @Override
    public J visitParameterizedType(ParameterizedTypeTree node, Space fmt) {
        return new J.ParameterizedType(randomId(), fmt, Markers.EMPTY, convert(node.getType()), convertTypeParameters(node.getTypeArguments()));
    }

    @Override
    public J visitParenthesized(ParenthesizedTree node, Space fmt) {
        skip("(");
        return new J.Parentheses<Expression>(randomId(), fmt, Markers.EMPTY,
                convert(node.getExpression(), t -> sourceBefore(")")));
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
                new J.Block(randomId(), sourceBefore("{"), Markers.EMPTY, null,
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
                J.VariableDecls resourceVar = convert(resource);
                boolean semicolonPresent = true;
                if (i == node.getResources().size() - 1) {
                    semicolonPresent = positionOfNext(";", ')') > 0;
                }

                Space resourcePrefix = resourceVar.getPrefix();
                resourceVar = resourceVar.withPrefix(EMPTY); // moved to the containing Try.Resource

                if (semicolonPresent) {
                    resourceVar = resourceVar.withVars(Space.formatLastSuffix(resourceVar.getVars(), sourceBefore(";")));
                }

                resourceList.add(padRight(new J.Try.Resource(randomId(), resourcePrefix, Markers.EMPTY,
                        resourceVar.withPrefix(EMPTY), semicolonPresent), sourceBefore(")")));
            }

            resources = JContainer.build(before, resourceList);
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
                new J.Parentheses<>(randomId(),
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
                        convertAll(node.getBounds(), t -> sourceBefore("&"), noDelim));

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
                expr = J.Ident.build(randomId(), EMPTY, Markers.EMPTY, part, null);
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
                        padLeft(namePrefix, J.Ident.build(randomId(), identFmt, Markers.EMPTY, part.trim(), null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.Class.build(fullName) :
                                null
                );
            }
        }

        //noinspection unchecked
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

    private J.VariableDecls visitVariables(List<VariableTree> nodes, Space fmt) {
        JCTree.JCVariableDecl node = (JCVariableDecl) nodes.get(0);
        List<J.Annotation> annotations = convertAll(node.getModifiers().annotations);

        JCExpression vartype = node.vartype;

        List<J.Modifier> modifiers;
        if (node.getModifiers().pos >= 0) {
            modifiers = sortedFlags(node.getModifiers());
        } else {
            modifiers = emptyList(); // these are implicit modifiers, like "final" on try-with-resources variable declarations
        }

        TypeTree typeExpr;
        if (vartype == null || endPos(vartype) < 0 || vartype instanceof JCErroneous) {
            typeExpr = null; // this is a lambda parameter with an inferred type expression
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

        String vartypeString = typeExpr == null ? "" : source.substring(vartype.getStartPosition(), endPos(vartype));
        Matcher varargMatcher = Pattern.compile("(\\s*)\\.{3}").matcher(vartypeString);
        Space varargs = null;
        if (varargMatcher.find()) {
            Matcher matcher = Pattern.compile("\\G(\\s*)\\.{3}").matcher(source);
            if (matcher.find(cursor)) {
                cursor(matcher.end());
            }
            varargs = format(varargMatcher.group(1));
        }

        List<JRightPadded<J.VariableDecls.NamedVar>> vars = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            VariableTree n = nodes.get(i);

            Space namedVarPrefix = sourceBefore(n.getName().toString());
            JCVariableDecl vd = (JCVariableDecl) n;

            J.Ident name = J.Ident.build(randomId(), EMPTY, Markers.EMPTY, n.getName().toString(), type(node));
            List<JLeftPadded<Space>> dimensionsAfterName = dimensions.get();

            vars.add(
                    padRight(
                            new J.VariableDecls.NamedVar(randomId(), namedVarPrefix, Markers.EMPTY,
                                    name,
                                    dimensionsAfterName,
                                    vd.init != null ? padLeft(sourceBefore("="), convertOrNull(vd.init)) : null,
                                    type(n)
                            ),
                            i == nodes.size() - 1 ? EMPTY : sourceBefore(",")
                    )
            );
        }

        return new J.VariableDecls(randomId(), fmt, Markers.EMPTY, annotations, modifiers, typeExpr, varargs, beforeDimensions, vars);
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
            logger.error("Failed to convert " + t.getClass().getSimpleName() + " for the following cursor stack:");
            logCurrentPathAsError();
            throw ex;
        }
    }

    private <J2 extends J> JRightPadded<J2> convert(Tree t, Function<Tree, Space> suffix) {
        J2 j = convert(t);
        JRightPadded<J2> rightPadded = j == null ? null : new JRightPadded<>(j, suffix.apply(t));
        cursor(max(endPos(t), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
        return rightPadded;
    }

    private void logCurrentPathAsError() {
        logger.error("--- BEGIN PATH ---");

        List<Tree> paths = stream(getCurrentPath().spliterator(), false).collect(toList());
        for (int i = paths.size(); i-- > 0; ) {
            JCTree tree = (JCTree) paths.get(i);
            if (tree instanceof JCCompilationUnit) {
                logger.error("JCCompilationUnit(sourceFile = " + ((JCCompilationUnit) tree).sourcefile.getName() + ")");
            } else if (tree instanceof JCClassDecl) {
                logger.error("JCClassDecl(name = " + ((JCClassDecl) tree).name + ", line = " + lineNumber(tree) + ")");
            } else if (tree instanceof JCVariableDecl) {
                logger.error("JCVariableDecl(name = " + ((JCVariableDecl) tree).name + ", line = " + lineNumber(tree) + ")");
            } else {
                logger.error(tree.getClass().getSimpleName() + "(line = " + lineNumber(tree) + ")");
            }
        }

        logger.error("--- END PATH ---");
    }

    private long lineNumber(Tree tree) {
        return source.substring(0, ((JCTree) tree).getStartPosition()).chars().filter(c -> c == '\n').count() + 1;
    }

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

    private JContainer<Expression> convertTypeParameters(List<? extends Tree> typeArguments) {
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

        return JContainer.build(typeArgPrefix, params);
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
                J.VariableDecls vars = visitVariables((List<VariableTree>) treeGroup, format(prefix));
                JRightPadded<Statement> paddedVars = padRight(vars, semiDelim.apply(last));
                cursor(max(endPos(last), cursor));
                converted.add(paddedVars);
            }
        }

        return converted;
    }

    /**
     * --------------
     * Type conversion
     * --------------
     */

    private final Map<Long, Flag> flagMasks = Map.of(
            1L, Flag.Public,
            1L << 1, Flag.Private,
            1L << 2, Flag.Protected,
            1L << 3, Flag.Static,
            1L << 4, Flag.Final,
            1L << 5, Flag.Synchronized,
            1L << 6, Flag.Volatile,
            1L << 7, Flag.Transient,
            1L << 10, Flag.Abstract
    );

    private Set<Flag> filteredFlags(Symbol sym) {
        Set<Flag> set = new HashSet<>();
        for (Map.Entry<Long, Flag> mask : flagMasks.entrySet()) {
            if ((sym.flags() & mask.getKey()) != 0L) {
                Flag value = mask.getValue();
                set.add(value);
            }
        }
        return set;
    }

    @Nullable
    private JavaType.Method methodType(Type selectType, @Nullable Symbol symbol, String methodName) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol genericSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        JavaType.Method type = null;
        if (genericSymbol != null && selectType != null) {
            Function<com.sun.tools.javac.code.Type, JavaType.Method.Signature> signature = t -> {
                if (t instanceof MethodType) {
                    MethodType mt = (MethodType) t;

                    List<JavaType> paramTypes = new ArrayList<>();
                    for (Type argtype : mt.argtypes) {
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
            if (genericSymbol.type instanceof com.sun.tools.javac.code.Type.ForAll) {
                genericSignature = signature.apply(((com.sun.tools.javac.code.Type.ForAll) genericSymbol.type).qtype);
            } else {
                genericSignature = signature.apply(genericSymbol.type);
            }

            List<String> paramNames = new ArrayList<>();
            for (Symbol.VarSymbol p : genericSymbol.params()) {
                String s = p.name.toString();
                paramNames.add(s);
            }

            return JavaType.Method.build(
                    TypeUtils.asClass(type(genericSymbol.owner)),
                    methodName,
                    genericSignature,
                    signature.apply(selectType),
                    paramNames,
                    filteredFlags(genericSymbol)
            );
        }

        return null;
    }

    @Nullable
    private JavaType type(@Nullable Symbol symbol) {
        if (symbol instanceof Symbol.ClassSymbol || symbol instanceof Symbol.TypeVariableSymbol) {
            return type(symbol.type);
        } else if (symbol instanceof Symbol.VarSymbol) {
            return new JavaType.GenericTypeVariable(symbol.name.toString(), null);
        }
        return null;
    }

    @Nullable
    private JavaType type(@Nullable Type type) {
        return type(type, emptyList());
    }

    @Nullable
    private JavaType type(@Nullable Type type, List<Symbol> stack) {
        return type(type, stack, false);
    }

    @Nullable
    private JavaType type(@Nullable Type type, List<Symbol> stack, boolean shallow) {
        if (type instanceof ClassType) {
            if (type instanceof Type.ErrorType) {
                return null;
            }

            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;

            if (stack.contains(sym))
                return new JavaType.Cyclic(sym.className());
            else {
                if (shallow) {
                    return new JavaType.ShallowClass(sym.className());
                } else {
                    JavaType.Class flyweight = sharedClassTypes.get(sym.className());
                    if (flyweight != null) {
                        return flyweight;
                    }

                    List<Symbol> stackWithSym = new ArrayList<>(stack);
                    stackWithSym.add(sym);

                    List<JavaType.Var> fields;
                    if (sym.members_field == null) {
                        fields = emptyList();
                    } else {
                        fields = new ArrayList<>();
                        for (Symbol elem : sym.members_field.getSymbols()) {
                            if (elem instanceof Symbol.VarSymbol) {
                                fields.add(new JavaType.Var(
                                        elem.name.toString(),
                                        type(elem.type, stackWithSym),
                                        filteredFlags(elem)
                                ));
                            }
                        }
                    }

                    ClassType classType = (ClassType) type;
                    ClassType symType = (ClassType) sym.type;

                    List<JavaType> typeParameters;
                    if (classType.typarams_field == null) {
                        typeParameters = emptyList();
                    } else {
                        typeParameters = new ArrayList<>();
                        for (Type tParam : classType.typarams_field) {
                            JavaType javaType = type(tParam, stackWithSym, true);
                            if (javaType != null) {
                                typeParameters.add(javaType);
                            }
                        }
                    }

                    List<JavaType> interfaces;
                    if (symType.interfaces_field == null) {
                        interfaces = emptyList();
                    } else {
                        interfaces = new ArrayList<>();
                        for (Type iParam : symType.interfaces_field) {
                            JavaType javaType = type(iParam, stackWithSym, false);
                            if (javaType != null) {
                                interfaces.add(javaType);
                            }
                        }
                    }

                    JavaType.Class clazz = JavaType.Class.build(
                            sym.className(),
                            fields,
                            typeParameters,
                            interfaces,
                            null,
                            TypeUtils.asClass(type(classType.supertype_field, stackWithSym)),
                            relaxedClassTypeMatching);

                    sharedClassTypes.put(sym.className(), clazz);

                    return clazz;
                }
            }
        } else if (type instanceof TypeVar) {
            return new JavaType.GenericTypeVariable(type.tsym.name.toString(),
                    TypeUtils.asClass(type(type.getUpperBound(), stack)));
        } else if (type instanceof JCPrimitiveType) {
            return primitive(type.getTag());
        } else if (type instanceof ArrayType) {
            return new JavaType.Array(type(((ArrayType) type).elemtype, stack));
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
        assert tree != null;
        return new JRightPadded<>(tree, right);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        assert tree != null;
        return new JLeftPadded<>(left, tree);
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

    @Nullable
    private String skip(@Nullable String token) {
        if (token == null)
            return null;
        if (source.startsWith(token, cursor))
            cursor += token.length();
        return token;
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
     * Modifiers in the order they appear in the source, which is not necessarily the same as the order in
     * which they appear in the OpenJDK AST
     */
    private List<J.Modifier> sortedFlags(ModifiersTree modifiers) {
        if (modifiers.getFlags().isEmpty()) {
            return emptyList();
        }

        ArrayList<Modifier> sortedModifiers = new ArrayList<>();

        boolean inComment = false;
        boolean inMultilineComment = false;
        final AtomicReference<String> word = new AtomicReference<>("");
        for (int i = cursor; i < source.length(); i++) {
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
                            break;
                        } else {
                            sortedModifiers.add(matching);
                            word.set("");
                        }
                    }
                } else {
                    word.getAndUpdate(w -> w + c);
                }
            }
        }

        List<J.Modifier> mappedModifiers = new ArrayList<>();
        for (Modifier mod : sortedModifiers) {
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
            mappedModifiers.add(new J.Modifier(randomId(), modFormat, Markers.EMPTY, type));
        }

        return mappedModifiers;
    }
}
