/*
 * Copyright 2021 the original author or authors.
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

import com.sun.source.doctree.*;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Javadoc.Attribute.ValueKind;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public class ReloadableJava8JavadocVisitor extends DocTreeScanner<Tree, String> {
    private final Attr attr;
    private final Symbol.TypeSymbol symbol;
    private final Type enclosingClassType;
    private final ReloadableTypeMapping typeMapping;
    private final TreeScanner<J, Space> javaVisitor = new JavaVisitor();
    private final Map<Integer, Javadoc.LineBreak> lineBreaks = new HashMap<>();

    /**
     * The whitespace on the first line terminated by a newline (if any)
     */
    private String firstPrefix = "";

    private String source;
    private int cursor = 0;

    public ReloadableJava8JavadocVisitor(Context context, TreePath scope, ReloadableTypeMapping typeMapping, String source) {
        this.attr = Attr.instance(context);
        this.typeMapping = typeMapping;
        this.source = source;

        if (scope.getLeaf() instanceof JCTree.JCCompilationUnit) {
            JCTree.JCCompilationUnit cu = (JCTree.JCCompilationUnit) scope.getLeaf();
            this.enclosingClassType = cu.defs.get(0).type;
            this.symbol = cu.packge;
        } else {
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) scope.getLeaf();
            this.enclosingClassType = classDecl.type;
            this.symbol = classDecl.sym;
        }
    }

    private void init() {
        char[] sourceArr = source.toCharArray();

        StringBuilder firstPrefixBuilder = new StringBuilder();
        StringBuilder javadocContent = new StringBuilder();
        StringBuilder marginBuilder = null;
        boolean inFirstPrefix = true;

        // skip past the opening '/**'
        int i = 3;
        for (; i < sourceArr.length; i++) {
            char c = sourceArr[i];
            if (inFirstPrefix) {
                if (Character.isWhitespace(c)) {
                    firstPrefixBuilder.append(c);
                } else {
                    firstPrefix = firstPrefixBuilder.toString();
                    inFirstPrefix = false;
                }
            }

            if (c == '\n') {
                if (inFirstPrefix) {
                    firstPrefix = firstPrefixBuilder.toString();
                    inFirstPrefix = false;
                } else {
                    javadocContent.append(c);
                }
                marginBuilder = new StringBuilder();
            } else if (marginBuilder != null) {
                if (!Character.isWhitespace(c)) {
                    if (c == '*') {
                        marginBuilder.append(c);
                        lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                                marginBuilder.toString(), Markers.EMPTY));
                    } else if (c == '@') {
                        lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                                marginBuilder.toString(), Markers.EMPTY));
                        javadocContent.append(c);
                    } else {
                        cursor = 1;
                        javadocContent.append('\n').append(marginBuilder).append(c);
                    }
                    marginBuilder = null;
                } else {
                    marginBuilder.append(c);
                }
            } else if (!inFirstPrefix) {
                javadocContent.append(c);
            }
        }

        if (inFirstPrefix) {
            javadocContent.append(firstPrefixBuilder);
        }

        source = javadocContent.toString();

        if (marginBuilder != null && marginBuilder.length() > 0) {
            if (javadocContent.charAt(0) != '\n') {
                lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                        marginBuilder.toString(), Markers.EMPTY));
                source = source.substring(0, source.length() - 1); // strip trailing newline
            } else {
                lineBreaks.put(source.length(), new Javadoc.LineBreak(randomId(),
                        marginBuilder.toString(), Markers.EMPTY));
            }
        }
    }

    @Override
    public Tree visitAttribute(AttributeTree node, String fmt) {
        String name = node.getName().toString();
        cursor += name.length();
        ValueKind kind;
        List<Javadoc> value;
        switch (node.getValueKind()) {
            case EMPTY:
                kind = ValueKind.Empty;
                value = emptyList();
                break;
            case UNQUOTED:
                kind = ValueKind.Unquoted;
                sourceBefore("=");
                value = node.getValue().stream().map(v -> (Javadoc) convert(v)).collect(toList());
                break;
            case SINGLE:
                kind = ValueKind.SingleQuoted;
                sourceBefore("='");
                value = node.getValue().stream().map(v -> (Javadoc) convert(v)).collect(toList());
                sourceBefore("'");
                break;
            case DOUBLE:
            default:
                kind = ValueKind.DoubleQuoted;
                sourceBefore("=\"");
                value = node.getValue().stream().map(v -> (Javadoc) convert(v)).collect(toList());
                sourceBefore("\"");
                break;
        }

        return new Javadoc.Attribute(
                randomId(),
                fmt,
                Markers.EMPTY,
                name,
                kind,
                value
        );
    }

    @Override
    public Tree visitAuthor(AuthorTree node, String fmt) {
        String prefix = fmt + sourceBefore("@author");
        return new Javadoc.Author(randomId(), prefix, Markers.EMPTY, node.getName().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitComment(CommentTree node, String fmt) {
        String text = fmt + node.getBody();
        cursor += node.getBody().length();
        return new Javadoc.Text(randomId(), Markers.EMPTY, text, null, null);
    }

    @Override
    public Tree visitDeprecated(DeprecatedTree node, String fmt) {
        String prefix = fmt + sourceBefore("@deprecated");
        return new Javadoc.Deprecated(randomId(), prefix, Markers.EMPTY, node.getBody().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitDocComment(DocCommentTree node, String fmt) {
        init();
        List<Javadoc> body = new ArrayList<>();

        Javadoc.LineBreak leadingLineBreak = lineBreaks.remove(0);
        if (leadingLineBreak != null) {
            if (!firstPrefix.isEmpty()) {
                body.add(new Javadoc.Text(randomId(), Markers.EMPTY, firstPrefix.substring(0, firstPrefix.length() - 1), null, null));
                firstPrefix = "";
            }
            body.add(leadingLineBreak);
        }

        List<? extends DocTree> firstSentence = node.getFirstSentence();
        for (int i = 0; i < firstSentence.size(); i++) {
            DocTree docTree = firstSentence.get(i);
            String prefix = docTree instanceof DCTree.DCText && i > 0 ? "" : whitespaceBefore();
            body.add((Javadoc) scan(docTree, firstPrefix + prefix));
            firstPrefix = "";
        }

        List<? extends DocTree> restOfBody = node.getBody();
        for (DocTree docTree : restOfBody) {
            String prefix = docTree instanceof DCTree.DCText ? "" : whitespaceBefore();
            body.add((Javadoc) scan(docTree, prefix));
        }

        Javadoc.LineBreak lineBreak;

        for (DocTree blockTag : node.getBlockTags()) {
            if ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
                cursor++;
                body.add(lineBreak);
            }
            String prefix = whitespaceBefore();
            body.add((Javadoc) scan(blockTag, firstPrefix + prefix));
            firstPrefix = "";
        }

        if (lineBreaks.isEmpty()) {
            if (cursor < source.length()) {
                String trailingWhitespace = source.substring(cursor);
                if (!trailingWhitespace.isEmpty()) {
                    body.add(new Javadoc.Text(randomId(), Markers.EMPTY, trailingWhitespace, null, null));
                }
            }
        } else {
            body.addAll(lineBreaks.values());
        }

        return new Javadoc.DocComment(randomId(), Markers.EMPTY, body, "");
    }

    @Override
    public Tree visitDocRoot(DocRootTree node, String fmt) {
        String prefix = fmt + sourceBefore("{@docRoot");
        return new Javadoc.DocRoot(
                randomId(),
                prefix,
                Markers.EMPTY,
                sourceBefore("}")
        );
    }

    @Override
    public Tree visitEndElement(EndElementTree node, String fmt) {
        String prefix = fmt + sourceBefore("</");
        String name = node.getName().toString();
        cursor += name.length();
        return new Javadoc.EndElement(
                randomId(),
                prefix,
                Markers.EMPTY,
                name,
                sourceBefore(">")
        );
    }

    @Override
    public Tree visitEntity(EntityTree node, String fmt) {
        String text = fmt + sourceBefore("&") + "&" + node.getName().toString() + ";";
        cursor += node.getName().length() + 1;
        return new Javadoc.Text(randomId(), Markers.EMPTY, text, null, null);
    }

    @Override
    public Tree visitErroneous(ErroneousTree node, String fmt) {
        String body = node.getBody();
        cursor += body.length();
        return new Javadoc.Text(randomId(), Markers.EMPTY, fmt + body, null, null);
    }

    @Override
    public J.Identifier visitIdentifier(com.sun.source.doctree.IdentifierTree node, String fmt) {
        String name = node.getName().toString();
        sourceBefore(name);
        return J.Identifier.build(
                randomId(),
                Space.build(fmt, emptyList()),
                Markers.EMPTY,
                name,
                null
        );
    }

    @Override
    public Tree visitInheritDoc(InheritDocTree node, String fmt) {
        String prefix = fmt + sourceBefore("{@inheritDoc");
        return new Javadoc.InheritDoc(
                randomId(),
                prefix,
                Markers.EMPTY,
                sourceBefore("}")
        );
    }

    @Override
    public Tree visitLink(LinkTree node, String fmt) {
        String prefix = fmt + sourceBefore(node.getKind() == DocTree.Kind.LINK ? "{@link" : "{@linkplain");
        J ref = visitReference(node.getReference(), "");
        return new Javadoc.Link(
                randomId(),
                prefix,
                Markers.EMPTY,
                node.getKind() != DocTree.Kind.LINK,
                ref,
                sourceBefore("}")
        );
    }

    @Override
    public Tree visitLiteral(LiteralTree node, String fmt) {
        String prefix = fmt + sourceBefore(node.getKind() == DocTree.Kind.CODE ? "{@code" : "{@link");
        Javadoc.Text body = (Javadoc.Text) visitText(node.getBody(), "");
        body = body.withText(body.getText() + sourceBefore("}"));
        return new Javadoc.Literal(
                randomId(),
                prefix,
                Markers.EMPTY,
                node.getKind() == DocTree.Kind.CODE,
                body
        );
    }

    @Override
    public Tree visitParam(ParamTree node, String fmt) {
        String prefix = fmt + sourceBefore("@param");
        DCTree.DCParam param = (DCTree.DCParam) node;
        J typeName;
        if (param.isTypeParameter) {
            typeName = new J.TypeParameter(
                    randomId(),
                    Space.build(sourceBefore("<"), emptyList()),
                    Markers.EMPTY,
                    emptyList(),
                    visitIdentifier(node.getName(), whitespaceBefore()),
                    null
            );
            sourceBefore(">");
        } else {
            typeName = convert(node.getName());
        }

        return new Javadoc.Parameter(
                randomId(),
                prefix,
                Markers.EMPTY,
                typeName,
                param.getDescription().stream()
                        .map(desc -> (Javadoc) convert(desc))
                        .collect(toList())
        );
    }

    @Override
    public J visitReference(ReferenceTree node, String fmt) {
        DCTree.DCReference ref = (DCTree.DCReference) node;

        TypedTree tree;
        if (ref.qualifierExpression != null) {
            attr.attribType(ref.qualifierExpression, symbol);
            // ref.qualifierExpression will have a 0 position regardless
            tree = (TypedTree) javaVisitor.scan(ref.qualifierExpression, Space.build(whitespaceBefore(), emptyList()));
        } else {
            tree = J.Identifier.build(randomId(), Space.build(whitespaceBefore(), emptyList()),
                    Markers.EMPTY, "", typeMapping.type(enclosingClassType));
        }

        if (ref.memberName != null) {
            sourceBefore("#");
            if (tree.getType() instanceof JavaType.Class) {
                JavaType.Class classType = (JavaType.Class) tree.getType();
                nextMethod:
                for (JavaType.Method method : classType.getMethods()) {
                    if (method.getName().equals(ref.memberName.toString()) && method.getResolvedSignature() != null) {
                        for (JCTree param : ref.paramTypes) {
                            Type paramType = attr.attribType(param, symbol);
                            for (JavaType testParamType : method.getResolvedSignature().getParamTypes()) {
                                while (testParamType instanceof JavaType.GenericTypeVariable) {
                                    testParamType = ((JavaType.GenericTypeVariable) testParamType).getBound();
                                }

                                if (paramType instanceof Type.ClassType) {
                                    JavaType.FullyQualified fqTestParamType = TypeUtils.asFullyQualified(testParamType);
                                    if (fqTestParamType == null || !fqTestParamType.getFullyQualifiedName().equals(((Symbol.ClassSymbol) paramType.tsym)
                                            .fullname.toString())) {
                                        continue nextMethod;
                                    }
                                }
                            }
                        }

                        J.Identifier name = J.Identifier.build(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                method.getName(),
                                method
                        );

                        cursor += method.getName().length();

                        JContainer<Expression> params;
                        if (ref.paramTypes.isEmpty()) {
                            params = JContainer.build(
                                    Space.build(sourceBefore("("), emptyList()),
                                    singletonList(JRightPadded.build(new J.Empty(randomId(), Space.build(sourceBefore(")"), emptyList()), Markers.EMPTY))),
                                    Markers.EMPTY
                            );
                        } else {
                            params = JContainer.build(
                                    Space.build(sourceBefore("("), emptyList()),
                                    ref.paramTypes.stream()
                                            .map(param -> {
                                                Expression paramExpr = (Expression) javaVisitor.scan(param, Space.EMPTY);
                                                Space rightFmt = Space.format(sourceBefore(")", ","));
                                                return new JRightPadded<>(paramExpr, rightFmt, Markers.EMPTY);
                                            })
                                            .collect(toList()),
                                    Markers.EMPTY
                            );
                        }

                        tree = new J.MethodInvocation(
                                randomId(),
                                tree.getPrefix(),
                                Markers.EMPTY,
                                JRightPadded.build(tree.withPrefix(Space.EMPTY)),
                                null,
                                name,
                                params,
                                method
                        );

                        break;
                    }
                }

                for (JavaType.Variable member : classType.getMembers()) {
                    if (member.getName().equals(ref.memberName.toString())) {
                        J.Identifier name = J.Identifier.build(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                member.getName(),
                                member
                        );

                        cursor += member.getName().length();

                        tree = new J.MemberReference(
                                randomId(),
                                tree.getPrefix(),
                                Markers.EMPTY,
                                JRightPadded.build(tree.withPrefix(Space.EMPTY)),
                                JContainer.empty(),
                                JLeftPadded.build(name),
                                member,
                                member.getType()
                        );

                        break;
                    }
                }
            }
        }

        assert tree != null;
        return tree;
    }

    @Override
    public Tree visitReturn(ReturnTree node, String fmt) {
        String prefix = fmt + sourceBefore("@return");
        return new Javadoc.Return(randomId(), prefix, Markers.EMPTY, node.getDescription().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitSee(SeeTree node, String fmt) {
        String prefix = fmt + sourceBefore("@see");
        return new Javadoc.Since(randomId(), prefix, Markers.EMPTY, node.getReference().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitSerial(SerialTree node, String fmt) {
        String prefix = fmt + sourceBefore("@serial");
        return new Javadoc.Since(randomId(), prefix, Markers.EMPTY, node.getDescription().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitSerialData(SerialDataTree node, String fmt) {
        String prefix = fmt + sourceBefore("@serialData");
        return new Javadoc.Since(randomId(), prefix, Markers.EMPTY, node.getDescription().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitSerialField(SerialFieldTree node, String fmt) {
        String prefix = fmt + sourceBefore("@serialField");
        return new Javadoc.SerialField(randomId(), prefix, Markers.EMPTY,
                visitIdentifier(node.getName(), whitespaceBefore()),
                visitReference(node.getType(), whitespaceBefore()),
                node.getDescription().stream()
                        .map(desc -> (Javadoc) convert(desc))
                        .collect(toList())
        );
    }

    @Override
    public Tree visitSince(SinceTree node, String fmt) {
        String prefix = fmt + sourceBefore("@since");
        return new Javadoc.Since(randomId(), prefix, Markers.EMPTY, node.getBody().stream()
                .map(desc -> (Javadoc) convert(desc))
                .collect(toList()));
    }

    @Override
    public Tree visitStartElement(StartElementTree node, String fmt) {
        String prefix = fmt + sourceBefore("<");
        String name = node.getName().toString();
        cursor += name.length();
        return new Javadoc.StartElement(
                randomId(),
                prefix,
                Markers.EMPTY,
                name,
                node.getAttributes().stream().map(a -> (Javadoc) convert(a)).collect(toList()),
                node.isSelfClosing(),
                sourceBefore(">")
        );
    }

    @Override
    public Tree visitVersion(VersionTree node, String fmt) {
        String prefix = fmt + sourceBefore("@version");
        return new Javadoc.Version(randomId(), prefix, Markers.EMPTY, node.getBody().stream()
                .map(b -> (Javadoc) convert(b))
                .collect(toList()));
    }

    @Override
    public Tree visitText(TextTree node, String fmt) {
        Stack<Javadoc.Text> texts = new Stack<>();

        char[] textArr = node.getBody().toCharArray();
        int afterLastBreak = 0;
        for (int i = 0; i < textArr.length; i++) {
            char c = textArr[i];
            if (c == '\n') {
                texts.add(new Javadoc.Text(
                        randomId(),
                        Markers.EMPTY,
                        fmt + source.substring(cursor + afterLastBreak, cursor + i),
                        lineBreaks.remove(cursor + i + 1),
                        null
                ));
                fmt = "";
                afterLastBreak = i + 1;
            }
        }

        String last = fmt + source.substring(cursor + afterLastBreak, cursor + textArr.length);
        if (!last.isEmpty()) {
            texts.add(new Javadoc.Text(randomId(), Markers.EMPTY, last, null, null));
        }

        cursor += textArr.length;

        Javadoc.Text head = texts.pop();
        while (!texts.isEmpty()) {
            head = texts.pop().withNext(head);
        }

        return head;
    }

    @Override
    public Tree visitThrows(ThrowsTree node, String fmt) {
        boolean throwsKeyword = source.startsWith("@throws", cursor);
        sourceBefore(throwsKeyword ? "@throws" : "@exception");
        return new Javadoc.Throws(randomId(), fmt, Markers.EMPTY, throwsKeyword,
                visitReference(node.getExceptionName(), ""),
                node.getDescription().stream()
                        .map(desc -> (Javadoc) convert(desc))
                        .collect(toList())
        );
    }

    @Override
    public Tree visitUnknownBlockTag(UnknownBlockTagTree node, String fmt) {
        String prefix = fmt + sourceBefore("@" + node.getTagName());
        return new Javadoc.UnknownBlock(
                randomId(),
                prefix,
                Markers.EMPTY,
                node.getTagName(),
                node.getContent().stream()
                        .map(c -> (Javadoc) convert(c))
                        .collect(toList())
        );
    }

    @Override
    public Tree visitUnknownInlineTag(UnknownInlineTagTree node, String fmt) {
        String prefix = fmt + sourceBefore("{@" + node.getTagName());
        return new Javadoc.UnknownInline(
                randomId(),
                prefix,
                Markers.EMPTY,
                node.getTagName(),
                sourceBefore("}")
        );
    }

    @Override
    public Tree visitValue(ValueTree node, String fmt) {
        String prefix = fmt + sourceBefore("{@value");
        J ref = node.getReference() == null ? null : visitReference(node.getReference(), "");
        return new Javadoc.InlinedValue(
                randomId(),
                prefix,
                Markers.EMPTY,
                ref,
                sourceBefore("}")
        );
    }

    private String sourceBefore(String delim) {
        return sourceBefore(delim, null);
    }

    private String sourceBefore(String delim, @Nullable String orElse) {
        int endIndex = source.indexOf(delim, cursor);
        if (endIndex < 0 && orElse != null) {
            endIndex = source.indexOf(orElse, cursor);
        }
        if (endIndex < 0) {
            throw new IllegalStateException("Expected to be able to find one of [" + delim + "," + orElse + "]");
        }
        String prefix = source.substring(cursor, endIndex);
        cursor = endIndex + delim.length();
        return prefix;
    }

    private String whitespaceBefore() {
        int i = cursor;
        for (; i < source.length(); i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                break;
            }
        }
        String fmt = source.substring(cursor, i);
        cursor = i;
        return fmt;
    }

    private <J2 extends Tree> J2 convert(DocTree t) {
        String prefix = whitespaceBefore();
        @SuppressWarnings("unchecked") J2 j = (J2) scan(t, prefix);
        return j;
    }

    class JavaVisitor extends TreeScanner<J, Space> {

        @Override
        public J visitMemberSelect(MemberSelectTree node, Space fmt) {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
            Expression selected = (Expression) scan(fieldAccess.selected, Space.EMPTY);
            sourceBefore(".");
            return new J.FieldAccess(randomId(), fmt, Markers.EMPTY,
                    selected,
                    JLeftPadded.build(J.Identifier.build(randomId(),
                            Space.build(sourceBefore(fieldAccess.name.toString()), emptyList()),
                            Markers.EMPTY,
                            fieldAccess.name.toString(), null)),
                    typeMapping.type(node));
        }

        @Override
        public J visitIdentifier(IdentifierTree node, Space fmt) {
            String name = node.getName().toString();
            cursor += name.length();
            JavaType type = typeMapping.type(node);
            return J.Identifier.build(randomId(), fmt, Markers.EMPTY, name, type);
        }

        @Override
        public J visitPrimitiveType(PrimitiveTypeTree node, Space fmt) {
            JCTree.JCPrimitiveTypeTree primitiveType = (JCTree.JCPrimitiveTypeTree) node;
            String name = primitiveType.getTree().type.tsym.name.toString();
            cursor += name.length();
            return J.Identifier.build(randomId(), fmt, Markers.EMPTY, name, typeMapping.primitiveType(primitiveType.typetag));
        }
    }
}
