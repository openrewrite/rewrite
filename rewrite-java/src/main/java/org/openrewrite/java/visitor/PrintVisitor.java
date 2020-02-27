/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Tree;

import java.util.Collection;

import static org.openrewrite.java.tree.J.Modifier.*;
import static java.util.stream.StreamSupport.stream;

public class PrintVisitor extends AstVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String reduce(String r1, String r2) {
        return r1 + r2;
    }

    private String visit(Collection<? extends Tree> nodes, String suffixBetween) {
        return visit(nodes, suffixBetween, "");
    }

    private String visit(Collection<? extends Tree> nodes, String suffixBetween, String suffixEnd) {
        var acc = "";
        Tree[] array = nodes.toArray(Tree[]::new);
        for (int i = 0; i < array.length; i++) {
            Tree node = array[i];
            acc = reduce(acc, visit(node) + (i == array.length - 1 ? suffixEnd : suffixBetween));
        }
        return acc;
    }

    private String visitStatements(Collection<? extends Tree> statements) {
        return statements.stream()
                .map(this::fmtStatement)
                .reduce("", this::reduce);
    }

    private String fmtStatement(Tree statement) {
        String semicolonOrEmpty = (statement instanceof Statement && ((Statement) statement).isSemicolonTerminated()) ||
                (statement instanceof J.MethodDecl && ((J.MethodDecl) statement).isAbstract()) ? ";" : "";
        return visit(statement) + semicolonOrEmpty;
    }

    private String fmt(@Nullable Tree tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getFormatting().getPrefix() + code + tree.getFormatting().getSuffix();
    }

    private String visitModifiers(Iterable<J.Modifier> modifiers) {
        return stream(modifiers.spliterator(), false)
                .map(mod -> {
                    String keyword = "";
                    if (mod instanceof Public) {
                        keyword = "public";
                    } else if (mod instanceof Protected) {
                        keyword = "protected";
                    } else if (mod instanceof Private) {
                        keyword = "private";
                    } else if (mod instanceof Abstract) {
                        keyword = "abstract";
                    } else if (mod instanceof Native) {
                        keyword = "native";
                    } else if (mod instanceof Static) {
                        keyword = "static";
                    } else if (mod instanceof Strictfp) {
                        keyword = "strictfp";
                    } else if (mod instanceof Final) {
                        keyword = "final";
                    } else if (mod instanceof Transient) {
                        keyword = "transient";
                    } else if (mod instanceof Volatile) {
                        keyword = "volatile";
                    } else if (mod instanceof Default) {
                        keyword = "default";
                    } else if (mod instanceof Modifier.Synchronized) {
                        keyword = "synchronized";
                    }
                    return fmt(mod, keyword);
                })
                .reduce("", this::reduce);
    }

    private String visitDims(Collection<J.VariableDecls.Dimension> dims) {
        return dims.stream().map(d -> fmt(d, "[" + visit(d.getWhitespace()) + "]"))
                .reduce("", this::reduce);
    }

    @Override
    public String visitAnnotation(J.Annotation annotation) {
        var args = annotation.getArgs() == null ? "" :
                fmt(annotation.getArgs(), "(" + visit(annotation.getArgs().getArgs(), ",") + ")");
        return fmt(annotation, "@" + visit(annotation.getAnnotationType()) + args);
    }

    @Override
    public String visitArrayAccess(J.ArrayAccess arrayAccess) {
        var dimension = fmt(arrayAccess.getDimension(), "[" + visit(arrayAccess.getDimension().getIndex()) + "]");
        return fmt(arrayAccess, visit(arrayAccess.getIndexed()) + dimension);
    }

    @Override
    public String visitArrayType(J.ArrayType arrayType) {
        var dimension = arrayType.getDimensions().stream()
                .map(d -> fmt(d, "[" + visit(d.getInner()) + "]"))
                .reduce("", this::reduce);
        return fmt(arrayType, visit(arrayType.getElementType()) + dimension);
    }

    @Override
    public String visitAssert(J.Assert azzert) {
        return fmt(azzert, "assert" + visit(azzert.getCondition()));
    }

    @Override
    public String visitAssign(J.Assign assign) {
        return fmt(assign, visit(assign.getVariable()) + "=" + visit(assign.getAssignment()));
    }

    @Override
    public String visitAssignOp(J.AssignOp assignOp) {
        String keyword = "";
        if (assignOp.getOperator() instanceof J.AssignOp.Operator.Addition) {
            keyword = "+=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.Subtraction) {
            keyword = "-=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.Multiplication) {
            keyword = "*=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.Division) {
            keyword = "/=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.Modulo) {
            keyword = "%=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.BitAnd) {
            keyword = "&=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.BitOr) {
            keyword = "|=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.BitXor) {
            keyword = "^=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.LeftShift) {
            keyword = "<<=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.RightShift) {
            keyword = ">>=";
        } else if (assignOp.getOperator() instanceof J.AssignOp.Operator.UnsignedRightShift) {
            keyword = ">>>=";
        }

        return fmt(assignOp, visit(assignOp.getVariable()) + fmt(assignOp.getOperator(), keyword) + visit(assignOp.getAssignment()));
    }

    @Override
    public String visitBinary(J.Binary binary) {
        String keyword = "";
        if (binary.getOperator() instanceof J.Binary.Operator.Addition) {
            keyword = "+";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Subtraction) {
            keyword = "-";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Multiplication) {
            keyword = "*";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Division) {
            keyword = "/";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Modulo) {
            keyword = "%";
        } else if (binary.getOperator() instanceof J.Binary.Operator.LessThan) {
            keyword = "<";
        } else if (binary.getOperator() instanceof J.Binary.Operator.GreaterThan) {
            keyword = ">";
        } else if (binary.getOperator() instanceof J.Binary.Operator.LessThanOrEqual) {
            keyword = "<=";
        } else if (binary.getOperator() instanceof J.Binary.Operator.GreaterThanOrEqual) {
            keyword = ">=";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Equal) {
            keyword = "==";
        } else if (binary.getOperator() instanceof J.Binary.Operator.NotEqual) {
            keyword = "!=";
        } else if (binary.getOperator() instanceof J.Binary.Operator.BitAnd) {
            keyword = "&";
        } else if (binary.getOperator() instanceof J.Binary.Operator.BitOr) {
            keyword = "|";
        } else if (binary.getOperator() instanceof J.Binary.Operator.BitXor) {
            keyword = "^";
        } else if (binary.getOperator() instanceof J.Binary.Operator.LeftShift) {
            keyword = "<<";
        } else if (binary.getOperator() instanceof J.Binary.Operator.RightShift) {
            keyword = ">>";
        } else if (binary.getOperator() instanceof J.Binary.Operator.UnsignedRightShift) {
            keyword = ">>>";
        } else if (binary.getOperator() instanceof J.Binary.Operator.Or) {
            keyword = "||";
        } else if (binary.getOperator() instanceof J.Binary.Operator.And) {
            keyword = "&&";
        }

        return fmt(binary, visit(binary.getLeft()) + fmt(binary.getOperator(), keyword) + visit(binary.getRight()));
    }

    @Override
    public String visitBlock(J.Block<Tree> block) {
        return fmt(block, fmt(block.getStatic(), "static") + "{" + visitStatements(block.getStatements()) + block.getEndOfBlockSuffix() + "}");
    }

    @Override
    public String visitBreak(J.Break breakStatement) {
        return fmt(breakStatement, "break" + visit(breakStatement.getLabel()));
    }

    @Override
    public String visitCase(J.Case caze) {
        return fmt(caze, visit(caze.getPattern()) + ":" + visitStatements(caze.getStatements()));
    }

    @Override
    public String visitCatch(Try.Catch catzh) {
        return fmt(catzh, "catch" + visit(catzh.getParam()) + visit(catzh.getBody()));
    }

    @Override
    public String visitClassDecl(J.ClassDecl classDecl) {
        var modifiers = visitModifiers(classDecl.getModifiers());

        var kind = "";
        if (classDecl.getKind() instanceof ClassDecl.Kind.Class) {
            kind = "class";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Enum) {
            kind = "enum";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Interface) {
            kind = "interface";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Annotation) {
            kind = "@interface";
        }

        return fmt(classDecl, visit(classDecl.getAnnotations()) +
                modifiers + fmt(classDecl.getKind(), kind) + visit(classDecl.getName()) +
                visit(classDecl.getTypeParameters()) + visit(classDecl.getExtends()) + visit(classDecl.getImplements(), ",") +
                visit(classDecl.getBody()));
    }

    @Override
    public String visitCompilationUnit(CompilationUnit cu) {
        return fmt(cu, (cu.getPackageDecl() == null ? "" : visit(cu.getPackageDecl()) + ";") +
                visit(cu.getImports(), ";", ";") +
                visit(cu.getClasses()));
    }

    @Override
    public String visitContinue(Continue continueStatement) {
        return fmt(continueStatement, "continue" + visit(continueStatement.getLabel()));
    }

    @Override
    public String visitDoWhileLoop(DoWhileLoop doWhileLoop) {
        return fmt(doWhileLoop, "do" + fmtStatement(doWhileLoop.getBody()) + fmt(doWhileLoop.getWhileCondition(), "while") + visit(doWhileLoop.getWhileCondition().getCondition()));
    }

    @Override
    public String visitEmpty(Empty empty) {
        return fmt(empty, "");
    }

    @Override
    public String visitEnumValue(EnumValue enoom) {
        var initializer = enoom.getInitializer() == null ? "" :
                fmt(enoom.getInitializer(), "(" + visit(enoom.getInitializer().getArgs(), ",") + ")");
        return fmt(enoom, visit(enoom.getName()) + initializer);
    }

    @Override
    public String visitEnumValueSet(EnumValueSet enums) {
        return fmt(enums, visit(enums.getEnums(), ",") +
                (enums.isTerminatedWithSemicolon() ? ";" : ""));
    }

    @Override
    public String visitFieldAccess(FieldAccess fieldAccess) {
        return fmt(fieldAccess, visit(fieldAccess.getTarget()) + "." + visit(fieldAccess.getName()));
    }

    public String visitFinally(Try.Finally finallie) {
        return fmt(finallie, "finally" + visit(finallie.getBody()));
    }

    @Override
    public String visitForLoop(ForLoop forLoop) {
        ForLoop.Control ctrl = forLoop.getControl();
        var expr = fmt(ctrl, "(" + visit(ctrl.getInit()) + ";" + visit(ctrl.getCondition()) + ";" + visit(ctrl.getUpdate(), ",", "") + ")");
        return fmt(forLoop, "for" + expr + fmtStatement(forLoop.getBody()));
    }

    @Override
    public String visitForEachLoop(ForEachLoop forEachLoop) {
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        var expr = fmt(ctrl, "(" + visit(ctrl.getVariable()) + ":" + visit(ctrl.getIterable()) + ")");
        return fmt(forEachLoop, "for" + expr + fmtStatement(forEachLoop.getBody()));
    }

    @Override
    public String visitIdentifier(Ident ident) {
        return fmt(ident, ident.getSimpleName());
    }

    @Override
    public String visitIf(If iff) {
        var elsePart = iff.getElsePart() == null ? "" :
                fmt(iff.getElsePart(), "else" + fmtStatement(iff.getElsePart().getStatement()));
        return fmt(iff, "if" + visit(iff.getIfCondition()) + fmtStatement(iff.getThenPart()) + elsePart);
    }

    @Override
    public String visitImport(Import impoort) {
        return fmt(impoort, "import" + (impoort.isStatic() ? " static" : "") + visit(impoort.getQualid()));
    }

    @Override
    public String visitInstanceOf(InstanceOf instanceOf) {
        return fmt(instanceOf, visit(instanceOf.getExpr()) + "instanceof" + visit(instanceOf.getClazz()));
    }

    @Override
    public String visitLabel(Label label) {
        return fmt(label, visit(label.getLabel()) + ":" + visit(label.getStatement()));
    }

    @Override
    public String visitLambda(Lambda lambda) {
        var params = visit(lambda.getParamSet().getParams(), ",");
        var paramSet = fmt(lambda.getParamSet(), lambda.getParamSet().isParenthesized() ? "(" + params + ")" : params);
        return fmt(lambda, paramSet + fmt(lambda.getArrow(), "->") + visit(lambda.getBody()));
    }

    @Override
    public String visitLiteral(Literal literal) {
        return fmt(literal, literal.getValueSource());
    }

    @Override
    public String visitMemberReference(MemberReference memberRef) {
        return fmt(memberRef, visit(memberRef.getContaining()) + "::" +
                visit(memberRef.getTypeParameters()) + visit(memberRef.getReference()));
    }

    @Override
    public String visitMethod(MethodDecl method) {
        var modifiers = visitModifiers(method.getModifiers());
        var params = fmt(method.getParams(), "(" + visit(method.getParams().getParams(), ",")) + ")";
        var defaultValue = method.getDefaultValue() == null ? "" :
                fmt(method.getDefaultValue(), "default" + visit(method.getDefaultValue().getValue()));
        var thrown = method.getThrows() == null ? "" :
                fmt(method.getThrows(), "throws" + visit(method.getThrows().getExceptions(), ","));

        return fmt(method, visit(method.getAnnotations()) + modifiers + visit(method.getTypeParameters()) +
                visit(method.getReturnTypeExpr()) + visit(method.getName()) + params +
                thrown + visit(method.getBody()) + defaultValue);
    }

    @Override
    public String visitMethodInvocation(MethodInvocation method) {
        var args = fmt(method.getArgs(), "(" + visit(method.getArgs().getArgs(), ",") + ")");
        var typeParams = method.getTypeParameters() == null ? "" :
                fmt(method.getTypeParameters(), "<" + visit(method.getTypeParameters().getParams(), ",") + ">");
        var selectSeparator = method.getSelect() == null ? "" : ".";
        return fmt(method, visit(method.getSelect()) + selectSeparator + typeParams + visit(method.getName()) + args);
    }

    @Override
    public String visitMultiCatch(MultiCatch multiCatch) {
        return fmt(multiCatch, visit(multiCatch.getAlternatives(), "|"));
    }

    @Override
    public String visitMultiVariable(VariableDecls multiVariable) {
        var modifiers = visitModifiers(multiVariable.getModifiers());
        var varargs = multiVariable.getVarargs() == null ? "" :
                fmt(multiVariable.getVarargs(), "...");

        return fmt(multiVariable, visit(multiVariable.getAnnotations()) + modifiers +
                visit(multiVariable.getTypeExpr()) + visitDims(multiVariable.getDimensionsBeforeName()) +
                varargs + visit(multiVariable.getVars(), ","));
    }

    @Override
    public String visitNewArray(NewArray newArray) {
        var typeExpr = newArray.getTypeExpr() == null ? "" :
                "new" + visit(newArray.getTypeExpr());
        var dimensions = newArray.getDimensions().stream()
                .map(d -> fmt(d, "[" + visit(d.getSize()) + "]"))
                .reduce("", this::reduce);
        var init = newArray.getInitializer() == null ? "" :
                fmt(newArray.getInitializer(), "{" + visit(newArray.getInitializer().getElements(), ",") + "}");

        return fmt(newArray, typeExpr + dimensions + init);
    }

    @Override
    public String visitNewClass(NewClass newClass) {
        var args = fmt(newClass.getArgs(), "(" + visit(newClass.getArgs().getArgs(), ",") + ")");
        return fmt(newClass, "new" + visit(newClass.getClazz()) + args + visit(newClass.getBody()));
    }

    @Override
    public String visitPackage(J.Package pkg) {
        return fmt(pkg, "package" + visit(pkg.getExpr()));
    }

    @Override
    public String visitParameterizedType(ParameterizedType type) {
        return fmt(type, visit(type.getClazz()) + visit(type.getTypeParameters()));
    }

    @Override
    public String visitPrimitive(Primitive primitive) {
        String keyword;
        switch (primitive.getType()) {
            case Boolean:
                keyword = "boolean";
                break;
            case Byte:
                keyword = "byte";
                break;
            case Char:
                keyword = "char";
                break;
            case Double:
                keyword = "double";
                break;
            case Float:
                keyword = "float";
                break;
            case Int:
                keyword = "int";
                break;
            case Long:
                keyword = "long";
                break;
            case Short:
                keyword = "short";
                break;
            case Void:
                keyword = "void";
                break;
            case String:
                keyword = "String";
                break;
            case Wildcard:
                keyword = "*";
                break;
            case None:
                throw new IllegalStateException("Unable to print None primitive");
            case Null:
                throw new IllegalStateException("Unable to print Null primitive");
            default:
                throw new IllegalStateException("Unable to print non-primitive type");
        }

        return fmt(primitive, keyword);
    }

    @Override
    public <T extends Tree> String visitParentheses(Parentheses<T> parens) {
        return fmt(parens, "(" + visit(parens.getTree()) + ")");
    }

    @Override
    public String visitReturn(Return retrn) {
        return fmt(retrn, "return" + visit(retrn.getExpr()));
    }

    @Override
    public String visitSwitch(Switch switzh) {
        return fmt(switzh, "switch" + visit(switzh.getSelector()) + visit(switzh.getCases()));
    }

    @Override
    public String visitSynchronized(J.Synchronized synch) {
        return fmt(synch, "synchronized" + visit(synch.getLock()) + visit(synch.getBody()));
    }

    @Override
    public String visitTernary(Ternary ternary) {
        return fmt(ternary, visit(ternary.getCondition()) + "?" + visit(ternary.getTruePart()) + ":" +
                visit(ternary.getFalsePart()));
    }

    @Override
    public String visitThrow(Throw thrown) {
        return fmt(thrown, "throw" + visit(thrown.getException()));
    }

    @Override
    public String visitTry(Try tryable) {
        var resources = tryable.getResources() == null ? "" :
                fmt(tryable.getResources(), "(" + visit(tryable.getResources().getDecls(), ";") + ")");

        return fmt(tryable, "try" + resources + visit(tryable.getBody()) + visit(tryable.getCatches()) + visit(tryable.getFinally()));
    }

    @Override
    public String visitTypeCast(TypeCast typeCast) {
        return fmt(typeCast, visit(typeCast.getClazz()) + visit(typeCast.getExpr()));
    }

    @Override
    public String visitTypeParameters(TypeParameters typeParams) {
        return fmt(typeParams, "<" + visit(typeParams.getParams(), ",", "") + ">");
    }

    @Override
    public String visitTypeParameter(TypeParameter typeParam) {
        var bounds = typeParam.getBounds() == null ? "" :
                fmt(typeParam.getBounds(), "extends" + visit(typeParam.getBounds().getTypes(), "&"));
        return fmt(typeParam, visit(typeParam.getAnnotations(), "") + visit(typeParam.getName()) + bounds);
    }

    @Override
    public String visitUnary(Unary unary) {
        String code = "";
        if (unary.getOperator() instanceof J.Unary.Operator.PreIncrement) {
            code = "++" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.PreDecrement) {
            code = "--" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.PostIncrement) {
            code = visit(unary.getExpr()) + fmt(unary.getOperator(), "++");
        } else if (unary.getOperator() instanceof J.Unary.Operator.PostDecrement) {
            code = visit(unary.getExpr()) + fmt(unary.getOperator(), "--");
        } else if (unary.getOperator() instanceof J.Unary.Operator.Positive) {
            code = "+" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Negative) {
            code = "-" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Complement) {
            code = "~" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Not) {
            code = "!" + visit(unary.getExpr());
        }

        return fmt(unary, code);
    }

    @Override
    public String visitUnparsedSource(UnparsedSource unparsed) {
        return fmt(unparsed, unparsed.getSource());
    }

    @Override
    public String visitVariable(VariableDecls.NamedVar variable) {
        var init = variable.getInitializer() == null ? "" :
                "=" + visit(variable.getInitializer());
        return fmt(variable, visit(variable.getName()) + visitDims(variable.getDimensionsAfterName()) + init);
    }

    @Override
    public String visitWhileLoop(WhileLoop whileLoop) {
        return fmt(whileLoop, "while" + visit(whileLoop.getCondition()) + fmtStatement(whileLoop.getBody()));
    }

    @Override
    public String visitWildcard(Wildcard wildcard) {
        var bound = "";
        if (wildcard.getBound() instanceof Wildcard.Bound.Extends) {
            bound = fmt(wildcard.getBound(), "extends");
        } else if (wildcard.getBound() instanceof Wildcard.Bound.Super) {
            bound = fmt(wildcard.getBound(), "super");
        }
        return fmt(wildcard, "?" + bound + visit(wildcard.getBoundedType()));
    }
}
