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
package org.openrewrite.java.isolated;

import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.UsesTree;
import com.sun.source.doctree.*;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.*;
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.LeadingBrace;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

public class ReloadableJava17JavadocVisitor extends DocTreeScanner<Tree, List<Javadoc>> {
    private final Attr attr;

    @Nullable
    private final Symbol.TypeSymbol symbol;

    @Nullable
    private final Type enclosingClassType;

    private final ReloadableJava17TypeMapping typeMapping;
    private final TreeScanner<J, Space> javaVisitor = new JavaVisitor();
    private final Map<Integer, Javadoc.LineBreak> lineBreaks = new HashMap<>();

    /**
     * The whitespace on the first line terminated by a newline (if any)
     */
    private String firstPrefix = "";

    private String source;
    private int cursor = 0;

    public ReloadableJava17JavadocVisitor(Context context, TreePath scope, ReloadableJava17TypeMapping typeMapping, String source, JCTree tree) {
        this.attr = Attr.instance(context);
        this.typeMapping = typeMapping;
        this.source = source;

        if (scope.getLeaf() instanceof JCTree.JCCompilationUnit) {
            this.enclosingClassType = tree.type;
            this.symbol = ((JCTree.JCClassDecl) tree).sym;
        } else {
            com.sun.source.tree.Tree classDecl = scope.getLeaf();
            if (classDecl instanceof JCTree.JCClassDecl) {
                this.enclosingClassType = ((JCTree.JCClassDecl) classDecl).type;
                this.symbol = ((JCTree.JCClassDecl) classDecl).sym;
            } else if (classDecl instanceof JCTree.JCNewClass) {
                this.enclosingClassType = ((JCTree.JCNewClass) classDecl).def.type;
                this.symbol = ((JCTree.JCNewClass) classDecl).def.sym;
            } else {
                this.enclosingClassType = null;
                this.symbol = null;
            }
        }
    }

    private void init() {
        StringBuilder firstPrefixBuilder = new StringBuilder();
        StringBuilder javadocContent = new StringBuilder();
        StringBuilder marginBuilder = null;
        boolean inFirstPrefix = true;
        boolean isPrefixAsterisk = true;

        // skip past the opening '/**'
        int i = 3;
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inFirstPrefix) {
                // '*' characters are considered a part of the margin until a non '*' is parsed.
                if (Character.isWhitespace(c) || c == '*' && isPrefixAsterisk) {
                    if (isPrefixAsterisk && i + 1 <= source.length() - 1 && source.charAt(i + 1) != '*') {
                        isPrefixAsterisk = false;
                    }
                    firstPrefixBuilder.append(c);
                } else {
                    firstPrefix = firstPrefixBuilder.toString();
                    inFirstPrefix = false;
                }
            }

            if (c == '\r') {
                continue;
            }

            if (c == '\n') {
                char prev = source.charAt(i - 1);
                if (inFirstPrefix) {
                    firstPrefix = firstPrefixBuilder.toString();
                    inFirstPrefix = false;
                } else {
                    // Handle consecutive new lines.
                    if ((prev == '\n' ||
                            prev == '\r' && source.charAt(i - 2) == '\n')) {
                        String prevLineLine = prev == '\n' ? "\n" : "\r\n";
                        lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(), prevLineLine, Markers.EMPTY));
                    } else if (marginBuilder != null) { // A new line with no '*' that only contains whitespace.
                        String newLine = prev == '\r' ? "\r\n" : "\n";
                        lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(), newLine, Markers.EMPTY));
                        javadocContent.append(marginBuilder.substring(marginBuilder.indexOf("\n") + 1));
                    }
                    javadocContent.append(c);
                }
                String newLine = prev == '\r' ? "\r\n" : "\n";
                marginBuilder = new StringBuilder(newLine);
            } else if (marginBuilder != null) {
                if (!Character.isWhitespace(c)) {
                    if (c == '*') {
                        // '*' characters are considered a part of the margin until a non '*' is parsed.
                        marginBuilder.append(c);
                        if (i + 1 <= source.length() - 1 && source.charAt(i + 1) != '*') {
                            lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                                    marginBuilder.toString(), Markers.EMPTY));
                            marginBuilder = null;
                        }
                    } else {
                        if (c == '@') {
                            lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                                    marginBuilder.toString(), Markers.EMPTY));
                            javadocContent.append(c);
                        } else {
                            String newLine = marginBuilder.charAt(0) == '\r' ? "\r\n" : "\n";
                            lineBreaks.put(javadocContent.length(), new Javadoc.LineBreak(randomId(),
                                    newLine, Markers.EMPTY));
                            String margin = marginBuilder.toString();
                            javadocContent.append(margin.substring(margin.indexOf("\n") + 1)).append(c);
                        }
                        marginBuilder = null;
                    }
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
            if (javadocContent.length() > 0 && javadocContent.charAt(0) != '\n') {
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
    public Tree visitAttribute(AttributeTree node, List<Javadoc> body) {
        String name = node.getName().toString();
        cursor += name.length();
        List<Javadoc> beforeEqual;
        List<Javadoc> value;

        if (node.getValueKind() == AttributeTree.ValueKind.EMPTY) {
            beforeEqual = emptyList();
            value = emptyList();
        } else {
            beforeEqual = new ArrayList<>();
            value = new ArrayList<>();
            Javadoc.LineBreak lineBreak;
            while ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
                cursor++;
                beforeEqual.add(lineBreak);
            }
            String whitespaceBeforeEqual = whitespaceBeforeAsString();
            beforeEqual.add(new Javadoc.Text(randomId(), Markers.EMPTY, whitespaceBeforeEqual));

            sourceBefore("=");

            while ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
                cursor++;
                value.add(lineBreak);
            }

            switch (node.getValueKind()) {
                case UNQUOTED:
                    value.addAll(convertMultiline(node.getValue()));
                    break;
                case SINGLE:
                    value.addAll(sourceBefore("'"));
                    value.add(new Javadoc.Text(randomId(), Markers.EMPTY, "'"));
                    value.addAll(convertMultiline(node.getValue()));
                    value.addAll(sourceBefore("'"));
                    value.add(new Javadoc.Text(randomId(), Markers.EMPTY, "'"));
                    break;
                case DOUBLE:
                default:
                    value.addAll(sourceBefore("\""));
                    value.add(new Javadoc.Text(randomId(), Markers.EMPTY, "\""));
                    value.addAll(convertMultiline(node.getValue()));
                    value.addAll(sourceBefore("\""));
                    value.add(new Javadoc.Text(randomId(), Markers.EMPTY, "\""));
                    break;
            }
        }

        return new Javadoc.Attribute(
                randomId(),
                Markers.EMPTY,
                name,
                beforeEqual,
                value
        );
    }

    @Override
    public Tree visitAuthor(AuthorTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@author"));
        return new Javadoc.Author(randomId(), Markers.EMPTY, convertMultiline(node.getName()));
    }

    @Override
    public Tree visitComment(CommentTree node, List<Javadoc> body) {
        cursor += node.getBody().length();
        return new Javadoc.Text(randomId(), Markers.EMPTY, node.getBody());
    }

    @Override
    public Tree visitDeprecated(DeprecatedTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@deprecated"));
        return new Javadoc.Deprecated(randomId(), Markers.EMPTY, convertMultiline(node.getBody()));
    }

    @Override
    public Tree visitDocComment(DocCommentTree node, List<Javadoc> body) {
        init();

        Javadoc.LineBreak leadingLineBreak = lineBreaks.remove(0);
        if (leadingLineBreak != null) {
            if (!firstPrefix.isEmpty()) {
                body.add(new Javadoc.Text(randomId(), Markers.EMPTY, firstPrefix.substring(0, firstPrefix.length() - (firstPrefix.endsWith("\r\n") ? 2 : 1))));
                firstPrefix = "";
            }
            body.add(leadingLineBreak);
        }

        if (!firstPrefix.isEmpty()) {
            body.add(new Javadoc.Text(randomId(), Markers.EMPTY, firstPrefix));
        }

        List<? extends DocTree> fullBody = node.getFullBody();
        for (int i = 0; i < fullBody.size(); i++) {
            DocTree docTree = fullBody.get(i);
            if (!(docTree instanceof DCTree.DCText && i > 0)) {
                body.addAll(whitespaceBefore());
            }
            if (docTree instanceof DCTree.DCText) {
                body.addAll(visitText(((DCTree.DCText) docTree).getBody()));
            } else {
                body.add((Javadoc) scan(docTree, body));
            }
        }

        Javadoc.LineBreak lineBreak;

        for (DocTree blockTag : node.getBlockTags()) {
            spaceBeforeTags:
            while (true) {
                if ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
                    cursor++;
                    body.add(lineBreak);
                }

                StringBuilder whitespaceBeforeNewLine = new StringBuilder();
                for (int j = cursor; j < source.length(); j++) {
                    char ch = source.charAt(j);
                    if (ch == '\r') {
                        cursor++;
                        continue;
                    }

                    if (ch == '\n') {
                        if (whitespaceBeforeNewLine.length() > 0) {
                            body.add(new Javadoc.Text(randomId(), Markers.EMPTY, whitespaceBeforeNewLine.toString()));
                        }
                        cursor += whitespaceBeforeNewLine.length();
                        break;
                    } else if (Character.isWhitespace(ch)) {
                        whitespaceBeforeNewLine.append(ch);
                    } else {
                        if (whitespaceBeforeNewLine.length() > 0) {
                            body.add(new Javadoc.Text(randomId(), Markers.EMPTY, whitespaceBeforeNewLine.toString()));
                            cursor += whitespaceBeforeNewLine.length();
                        }
                        break spaceBeforeTags;
                    }
                }

                if (lineBreak == null) {
                    break;
                }
            }

            body.addAll(whitespaceBefore());
            body.addAll(convertMultiline(singletonList(blockTag)));
        }

        // The javadoc ends with trailing whitespace.
        if (cursor < source.length()) {
            String trailingWhitespace = source.substring(cursor);
            if (trailingWhitespace.contains("\n")) {
                // 1 or more newlines.
                String[] parts = trailingWhitespace.split("\n");
                for (String part : parts) {
                    // Add trailing whitespace for each new line.
                    if (!part.isEmpty()) {
                        body.add(new Javadoc.Text(randomId(), Markers.EMPTY, part));
                    }
                    // Add trailing linebreaks if they exist.
                    if (!lineBreaks.isEmpty()) {
                        int pos = Collections.min(lineBreaks.keySet());
                        if (lineBreaks.containsKey(pos)) {
                            body.add(lineBreaks.get(pos));
                            lineBreaks.remove(pos);
                        }
                    }
                }
            } else {
                // The Javadoc ends with trailing whitespace.
                body.add(new Javadoc.Text(randomId(), Markers.EMPTY, trailingWhitespace));
            }
        }

        if (!lineBreaks.isEmpty()) {
            lineBreaks.keySet().stream().sorted().forEach(o -> body.add(lineBreaks.get(o)));
        }

        return new Javadoc.DocComment(randomId(), Markers.EMPTY, body, "");
    }

    @Override
    public Tree visitDocRoot(DocRootTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@docRoot"));
        return new Javadoc.DocRoot(
                randomId(),
                Markers.EMPTY,
                endBrace()
        );
    }

    @Override
    public Tree visitDocType(DocTypeTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("<!doctype"));
        return new Javadoc.DocType(randomId(), Markers.EMPTY,
                ListUtils.concatAll(
                        ListUtils.concat(sourceBefore(node.getText()), new Javadoc.Text(randomId(), Markers.EMPTY, node.getText())),
                        sourceBefore(">")
                )
        );
    }

    @Override
    public Tree visitEndElement(EndElementTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("</"));
        String name = node.getName().toString();
        cursor += name.length();
        return new Javadoc.EndElement(
                randomId(),
                Markers.EMPTY,
                name,
                sourceBefore(">")
        );
    }

    @Override
    public Tree visitEntity(EntityTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("&"));
        cursor += node.getName().length() + 1;
        return new Javadoc.Text(randomId(), Markers.EMPTY, "&" + node.getName().toString() + ";");
    }

    @Override
    public Tree visitErroneous(ErroneousTree node, List<Javadoc> body) {
        return new Javadoc.Erroneous(randomId(), Markers.EMPTY, visitText(node.getBody()));
    }

    @Override
    public Tree visitHidden(HiddenTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@hidden"));
        return new Javadoc.Hidden(randomId(), Markers.EMPTY, convertMultiline(node.getBody()));
    }

    @Override
    public J.Identifier visitIdentifier(com.sun.source.doctree.IdentifierTree node, List<Javadoc> body) {
        String name = node.getName().toString();
        sourceBefore(name);
        return new J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                name,
                null,
                null
        );
    }

    @Override
    public Tree visitIndex(IndexTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@index"));
        List<Javadoc> searchTerm = ListUtils.concatAll(whitespaceBefore(), convertMultiline(singletonList(node.getSearchTerm())));
        List<Javadoc> description = convertMultiline(node.getDescription());
        List<Javadoc> paddedDescription = ListUtils.flatMap(description, (i, desc) -> {
            if (i == description.size() - 1) {
                if (desc instanceof Javadoc.Text) {
                    Javadoc.Text text = (Javadoc.Text) desc;
                    return text.withText(text.getText());
                } else {
                    return ListUtils.concat(desc, endBrace());
                }
            }
            return desc;
        });

        return new Javadoc.Index(
                randomId(),
                Markers.EMPTY,
                searchTerm,
                paddedDescription,
                endBrace()
        );
    }

    @Override
    public Tree visitInheritDoc(InheritDocTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@inheritDoc"));
        return new Javadoc.InheritDoc(
                randomId(),
                Markers.EMPTY,
                endBrace()
        );
    }

    @Override
    public Tree visitLink(LinkTree node, List<Javadoc> body) {
        body.addAll(sourceBefore(node.getKind() == DocTree.Kind.LINK ? "{@link" : "{@linkplain"));

        List<Javadoc> spaceBeforeRef = whitespaceBefore();
        Javadoc.Reference reference = null;
        J ref = visitReference(node.getReference(), body);
        //noinspection ConstantConditions
        if (ref != null) {
            reference = new Javadoc.Reference(randomId(), Markers.EMPTY, ref, lineBreaksInMultilineJReference());
        }

        List<Javadoc> label = convertMultiline(node.getLabel());

        return new Javadoc.Link(
                randomId(),
                Markers.EMPTY,
                node.getKind() != DocTree.Kind.LINK,
                spaceBeforeRef,
                null,
                reference,
                label,
                endBrace()
        );
    }

    @Override
    public Tree visitLiteral(LiteralTree node, List<Javadoc> body) {
        body.addAll(sourceBefore(node.getKind() == DocTree.Kind.CODE ? "{@code" : "{@literal"));

        List<Javadoc> description = whitespaceBefore();
        description.addAll(visitText(node.getBody().getBody()));

        return new Javadoc.Literal(
                randomId(),
                Markers.EMPTY,
                node.getKind() == DocTree.Kind.CODE,
                description,
                endBrace()
        );
    }

    @Override
    public Tree visitParam(ParamTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@param"));
        DCTree.DCParam param = (DCTree.DCParam) node;

        List<Javadoc> spaceBefore;
        J typeName;

        if (param.isTypeParameter) {
            spaceBefore = sourceBefore("<");

            String beforeName = whitespaceBeforeAsString();
            Space namePrefix = beforeName.isEmpty() ? Space.EMPTY : Space.build(beforeName, emptyList());
            typeName = new J.TypeParameter(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    visitIdentifier(node.getName(), whitespaceBefore()).withPrefix(namePrefix),
                    null
            );
            // The node will not be considered a type parameter if whitespace exists before `>`.
            sourceBefore(">");
        } else {
            spaceBefore = whitespaceBefore();
            typeName = (J) scan(node.getName(), body);
        }

        List<Javadoc> beforeReference = lineBreaksInMultilineJReference();
        Javadoc.Reference reference = new Javadoc.Reference(randomId(), Markers.EMPTY, typeName, beforeReference);
        return new Javadoc.Parameter(
                randomId(),
                Markers.EMPTY,
                spaceBefore,
                null,
                reference,
                convertMultiline(param.getDescription())
        );
    }

    @Override
    public Tree visitProvides(ProvidesTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@provides"));
        return new Javadoc.Provides(randomId(), Markers.EMPTY,
                whitespaceBefore(),
                visitReference(node.getServiceType(), body),
                convertMultiline(node.getDescription())
        );
    }

    @Override
    public J visitReference(@Nullable ReferenceTree node, List<Javadoc> body) {
        DCTree.DCReference ref = (DCTree.DCReference) node;
        if (node == null) {
            //noinspection ConstantConditions
            return null;
        }

        JavaType qualifierType;
        TypedTree qualifier;

        if (ref.qualifierExpression != null) {
            try {
                attr.attribType(ref.qualifierExpression, symbol);
            } catch (NullPointerException ignored) {
                // best effort, can result in:
                // java.lang.NullPointerException: Cannot read field "info" because "env" is null
                //   at com.sun.tools.javac.comp.Attr.attribType(Attr.java:404)
            }
        }

        if (ref.qualifierExpression != null) {
            qualifier = (TypedTree) javaVisitor.scan(ref.qualifierExpression, Space.EMPTY);
            qualifierType = qualifier.getType();
            if (ref.memberName != null) {
                cursor++; // skip #
            }
        } else {
            qualifierType = typeMapping.type(enclosingClassType);
            if (source.charAt(cursor) == '#') {
                qualifier = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "", qualifierType, null);
                cursor++;
            } else {
                qualifier = null;
            }
        }

        if (ref.memberName != null) {
            J.Identifier name = new J.Identifier(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    ref.memberName.toString(),
                    null,
                    null
            );

            cursor += ref.memberName.toString().length();

            JavaType.Method methodRefType = methodReferenceType(ref, qualifierType);
            JavaType.Variable fieldRefType = methodRefType == null ?
                    fieldReferenceType(ref, qualifierType) : null;

            if (ref.paramTypes != null) {
                JContainer<Expression> paramContainer;
                sourceBeforeAsString("(");
                if (ref.paramTypes.isEmpty()) {
                    paramContainer = JContainer.build(
                            Space.EMPTY,
                            singletonList(JRightPadded.build(new J.Empty(randomId(),
                                    Space.build(sourceBeforeAsString(")"), emptyList()), Markers.EMPTY))),
                            Markers.EMPTY
                    );
                } else {
                    List<JRightPadded<Expression>> parameters = new ArrayList<>(ref.paramTypes.size());
                    List<JCTree> paramTypes = ref.paramTypes;
                    for (int i = 0; i < paramTypes.size(); i++) {
                        JCTree param = paramTypes.get(i);
                        Expression paramExpr = (Expression) javaVisitor.scan(param, Space.build(whitespaceBeforeAsString(), emptyList()));
                        Space rightFmt = format(i == paramTypes.size() - 1 ?
                                sourceBeforeAsString(")") : sourceBeforeAsString(","));
                        parameters.add(new JRightPadded<>(paramExpr, rightFmt, Markers.EMPTY));
                    }
                    paramContainer = JContainer.build(
                            Space.EMPTY,
                            parameters,
                            Markers.EMPTY
                    );
                }

                return new J.MethodInvocation(
                        randomId(),
                        qualifier == null ? Space.EMPTY : qualifier.getPrefix(),
                        Markers.EMPTY,
                        qualifier == null ? null : JRightPadded.build(qualifier.withPrefix(Space.EMPTY)),
                        null,
                        name,
                        paramContainer,
                        methodRefType
                );
            } else {
                return new J.MemberReference(
                        randomId(),
                        qualifier == null ? Space.EMPTY : qualifier.getPrefix(),
                        Markers.EMPTY,
                        qualifier == null ? null : JRightPadded.build(qualifier.withPrefix(Space.EMPTY)),
                        JContainer.empty(),
                        JLeftPadded.build(name),
                        null,
                        methodRefType,
                        fieldRefType
                );
            }
        }

        assert qualifier != null;
        return qualifier;
    }

    @Nullable
    private JavaType.Method methodReferenceType(DCTree.DCReference ref, @Nullable JavaType type) {
        if (type instanceof JavaType.Class) {
            JavaType.Class classType = (JavaType.Class) type;

            nextMethod:
            for (JavaType.Method method : classType.getMethods()) {
                if (method.getName().equals(ref.memberName.toString())) {
                    if (ref.paramTypes != null) {
                        for (JCTree param : ref.paramTypes) {
                            for (JavaType testParamType : method.getParameterTypes()) {
                                Type paramType = attr.attribType(param, symbol);
                                if (testParamType instanceof JavaType.GenericTypeVariable) {
                                    List<JavaType> bounds = ((JavaType.GenericTypeVariable) testParamType).getBounds();
                                    if (bounds.isEmpty() && paramType.tsym != null && "java.lang.Object".equals(paramType.tsym.getQualifiedName().toString())) {
                                        return method;
                                    }
                                    for (JavaType bound : bounds) {
                                        if (paramTypeMatches(bound, paramType)) {
                                            return method;
                                        }
                                    }
                                    continue nextMethod;
                                }

                                if (paramTypeMatches(testParamType, paramType)) {
                                    continue nextMethod;
                                }
                            }
                        }
                    }

                    return method;
                }
            }
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) type;
            for (JavaType bound : generic.getBounds()) {
                JavaType.Method method = methodReferenceType(ref, bound);
                if (method != null) {
                    return method;
                }
            }
        }
        // a member reference, but not matching anything on type attribution
        return null;
    }

    private boolean paramTypeMatches(JavaType testParamType, Type paramType) {
        if (paramType instanceof Type.ClassType) {
            JavaType.FullyQualified fqTestParamType = TypeUtils.asFullyQualified(testParamType);
            return fqTestParamType == null || !fqTestParamType.getFullyQualifiedName().equals(((Symbol.ClassSymbol) paramType.tsym)
                    .fullname.toString());
        }
        return false;
    }

    @Nullable
    private JavaType.Variable fieldReferenceType(DCTree.DCReference ref, @Nullable JavaType type) {
        JavaType.Class classType = TypeUtils.asClass(type);
        if (classType == null) {
            return null;
        }

        for (JavaType.Variable member : classType.getMembers()) {
            if (member.getName().equals(ref.memberName.toString())) {
                return member;
            }
        }

        // a member reference, but not matching anything on type attribution
        return null;
    }

    @Override
    public Tree visitReturn(ReturnTree node, List<Javadoc> body) {
        List<Javadoc> before;
        Markers markers = Markers.EMPTY;
        if (source.startsWith("{", cursor)) {
            markers = markers.addIfAbsent(new LeadingBrace(Tree.randomId()));
            before = sourceBefore("{@return");
        } else {
            before = sourceBefore("@return");
        }
        body.addAll(before);
        return new Javadoc.Return(randomId(), markers, convertMultiline(node.getDescription()));
    }

    @Override
    public Tree visitSee(SeeTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@see"));

        Javadoc.Reference reference = null;

        J ref;
        List<Javadoc> spaceBeforeTree = whitespaceBefore();
        List<Javadoc> docs;
        if (node.getReference().get(0) instanceof DCTree.DCReference) {
            ref = visitReference((ReferenceTree) node.getReference().get(0), body);
            //noinspection ConstantConditions
            if (ref != null) {
                reference = new Javadoc.Reference(randomId(), Markers.EMPTY, ref, lineBreaksInMultilineJReference());
            }
            docs = convertMultiline(node.getReference().subList(1, node.getReference().size()));
        } else {
            docs = convertMultiline(node.getReference());
        }

        return new Javadoc.See(randomId(), Markers.EMPTY, spaceBeforeTree, null, reference, docs);
    }

    @Override
    public Tree visitSerial(SerialTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@serial"));
        return new Javadoc.Serial(randomId(), Markers.EMPTY, convertMultiline(node.getDescription()));
    }

    @Override
    public Tree visitSerialData(SerialDataTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@serialData"));
        return new Javadoc.SerialData(randomId(), Markers.EMPTY, convertMultiline(node.getDescription()));
    }

    @Override
    public Tree visitSerialField(SerialFieldTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@serialField"));

        return new Javadoc.SerialField(randomId(), Markers.EMPTY,
                visitIdentifier(node.getName(), whitespaceBefore()),
                visitReference(node.getType(), whitespaceBefore()),
                convertMultiline(node.getDescription())
        );
    }

    @Override
    public Tree visitSince(SinceTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@since"));
        return new Javadoc.Since(randomId(), Markers.EMPTY, convertMultiline(node.getBody()));
    }

    @Override
    public Tree visitStartElement(StartElementTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("<"));
        String name = node.getName().toString();
        cursor += name.length();
        return new Javadoc.StartElement(
                randomId(),
                Markers.EMPTY,
                name,
                convertMultiline(node.getAttributes()),
                node.isSelfClosing(),
                node.isSelfClosing() ? sourceBefore("/>") : sourceBefore(">")
        );
    }

    @Override
    public Tree visitSummary(SummaryTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@summary"));

        List<Javadoc> summary = convertMultiline(node.getSummary());

        List<Javadoc> paddedSummary = ListUtils.flatMap(summary, (i, sum) -> {
            if (i == summary.size() - 1) {
                if (sum instanceof Javadoc.Text) {
                    Javadoc.Text text = (Javadoc.Text) sum;
                    return ListUtils.concat(text.withText(text.getText()), endBrace());
                } else {
                    return ListUtils.concat(sum, endBrace());
                }
            }
            return sum;
        });

        return new Javadoc.Summary(
                randomId(),
                Markers.EMPTY,
                paddedSummary,
                endBrace()
        );
    }

    @Override
    public Tree visitVersion(VersionTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@version"));
        return new Javadoc.Version(randomId(), Markers.EMPTY, convertMultiline(node.getBody()));
    }

    @Override
    public Tree visitText(TextTree node, List<Javadoc> body) {
        throw new UnsupportedOperationException("Anywhere text can occur, we need to call the visitText override that " +
                "returns a list of Javadoc elements.");
    }

    public List<Javadoc> visitText(String node) {
        List<Javadoc> texts = new ArrayList<>();

        if (!node.isEmpty() && Character.isWhitespace(node.charAt(0)) &&
                !Character.isWhitespace(source.charAt(cursor))) {
            node = node.stripLeading();
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < node.length(); i++) {
            char c = node.charAt(i);
            cursor++;
            if (c == '\n') {
                if (text.length() > 0) {
                    texts.add(new Javadoc.Text(randomId(), Markers.EMPTY, text.toString()));
                    text = new StringBuilder();
                }

                Javadoc.LineBreak lineBreak = lineBreaks.remove(cursor);
                assert lineBreak != null;
                texts.add(lineBreak);
            } else {
                text.append(c);
            }
        }

        if (text.length() > 0) {
            texts.add(new Javadoc.Text(randomId(), Markers.EMPTY, text.toString()));
        }

        return texts;
    }

    @Override
    public Tree visitThrows(ThrowsTree node, List<Javadoc> body) {
        boolean throwsKeyword = source.startsWith("@throws", cursor);
        sourceBefore(throwsKeyword ? "@throws" : "@exception");
        List<Javadoc> spaceBeforeExceptionName = whitespaceBefore();
        return new Javadoc.Throws(
                randomId(),
                Markers.EMPTY,
                throwsKeyword,
                spaceBeforeExceptionName,
                visitReference(node.getExceptionName(), body),
                convertMultiline(node.getDescription())
        );
    }

    @Override
    public Tree visitUnknownBlockTag(UnknownBlockTagTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@" + node.getTagName()));
        return new Javadoc.UnknownBlock(
                randomId(),
                Markers.EMPTY,
                node.getTagName(),
                convertMultiline(node.getContent())
        );
    }

    @Override
    public Tree visitUnknownInlineTag(UnknownInlineTagTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@" + node.getTagName()));
        return new Javadoc.UnknownInline(
                randomId(),
                Markers.EMPTY,
                node.getTagName(),
                convertMultiline(node.getContent()),
                endBrace()
        );
    }

    @Override
    public Tree visitUses(UsesTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@uses"));
        return new Javadoc.Uses(randomId(), Markers.EMPTY,
                whitespaceBefore(),
                visitReference(node.getServiceType(), body),
                convertMultiline(node.getDescription())
        );
    }

    @Override
    public Tree visitValue(ValueTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("{@value"));
        return new Javadoc.InlinedValue(
                randomId(),
                Markers.EMPTY,
                whitespaceBefore(),
                node.getReference() == null ? null : visitReference(node.getReference(), body),
                endBrace()
        );
    }

    private String sourceBeforeAsString(String delim) {
        if (cursor >= source.length()) {
            return "";
        }

        int endIndex = source.indexOf(delim, cursor);
        if (endIndex < 0) {
            throw new IllegalStateException("Expected to be able to find " + delim);
        }
        String prefix = source.substring(cursor, endIndex);
        cursor = endIndex + delim.length();
        return prefix;
    }

    private List<Javadoc> sourceBefore(String delim) {
        if (cursor >= source.length()) {
            return emptyList();
        }

        int endIndex = source.indexOf(delim, cursor);
        if (endIndex < 0) {
            throw new IllegalStateException("Expected to be able to find " + delim);
        }

        List<Javadoc> before = whitespaceBefore();
        cursor += delim.length();
        return before;
    }

    private String whitespaceBeforeAsString() {
        if (cursor >= source.length()) {
            return "";
        }

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

    private List<Javadoc> whitespaceBefore() {
        if (cursor >= source.length()) {
            return emptyList();
        }

        List<Javadoc> whitespace = new ArrayList<>();

        Javadoc.LineBreak lineBreak;
        while ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
            cursor++;
            whitespace.add(lineBreak);
        }

        StringBuilder space = new StringBuilder();
        for (; cursor < source.length() && Character.isWhitespace(source.charAt(cursor)); cursor++) {
            char c = source.charAt(cursor);
            if (c == '\n') {
                if (space.length() > 0) {
                    whitespace.add(new Javadoc.Text(randomId(), Markers.EMPTY, space.toString()));
                }
                space = new StringBuilder();

                lineBreak = lineBreaks.remove(cursor + 1);
                assert lineBreak != null;
                whitespace.add(lineBreak);
            } else {
                space.append(c);
            }
        }

        if (space.length() > 0) {
            whitespace.add(new Javadoc.Text(randomId(), Markers.EMPTY, space.toString()));
        }

        return whitespace;
    }

    private List<Javadoc> endBrace() {
        if (cursor < source.length()) {
            int tempCursor = cursor;
            List<Javadoc> end = whitespaceBefore();
            if (cursor < source.length() && source.charAt(cursor) == '}') {
                end = ListUtils.concat(end, new Javadoc.Text(randomId(), Markers.EMPTY, "}"));
                cursor++;
                return end;
            } else {
                cursor = tempCursor;
            }
        }
        return emptyList();
    }

    private List<Javadoc> convertMultiline(List<? extends DocTree> dts) {
        List<Javadoc> js = new ArrayList<>(dts.size());
        Javadoc.LineBreak lineBreak;
        while ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
            cursor++;
            js.add(lineBreak);
        }

        for (int i = 0; i < dts.size(); i++) {
            DocTree dt = dts.get(i);
            if (i > 0 && dt instanceof DCTree.DCText) {
                // the whitespace is part of the text
                js.addAll(visitText(((DCTree.DCText) dt).getBody()));
            } else {
                while ((lineBreak = lineBreaks.remove(cursor + 1)) != null) {
                    cursor++;
                    js.add(lineBreak);
                }

                js.addAll(whitespaceBefore());
                if (dt instanceof DCTree.DCText) {
                    js.addAll(visitText(((DCTree.DCText) dt).getBody()));
                } else {
                    js.add((Javadoc) scan(dt, emptyList()));
                }
            }
        }
        return js;
    }

    /**
     * A {@link J} may contain new lines in each {@link Space} and each new line will have a corresponding
     * {@link org.openrewrite.java.tree.Javadoc.LineBreak}.
     * <p>
     * This method collects the linebreaks associated to new lines in a Space, and removes the applicable linebreaks
     * from the map.
     */
    private List<Javadoc> lineBreaksInMultilineJReference() {
        @SuppressWarnings("SimplifyStreamApiCallChains")
        List<Integer> linebreakIndexes = lineBreaks.keySet().stream()
                .filter(o -> o <= cursor)
                .collect(Collectors.toList());

        List<Javadoc> referenceLineBreaks = linebreakIndexes.stream()
                .sorted()
                .map(lineBreaks::get)
                .collect(Collectors.toList());

        for (Integer key : linebreakIndexes) {
            lineBreaks.remove(key);
        }
        return referenceLineBreaks;
    }

    class JavaVisitor extends TreeScanner<J, Space> {

        @Override
        public J visitMemberSelect(MemberSelectTree node, Space fmt) {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
            Expression selected = (Expression) scan(fieldAccess.selected, Space.EMPTY);
            sourceBefore(".");
            cursor += fieldAccess.name.toString().length();
            return new J.FieldAccess(randomId(), fmt, Markers.EMPTY,
                    selected,
                    JLeftPadded.build(new J.Identifier(randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            fieldAccess.name.toString(), null, null)),
                    typeMapping.type(node));
        }

        @Override
        public J visitIdentifier(IdentifierTree node, Space fmt) {
            String name = node.getName().toString();
            cursor += name.length();
            JavaType type = typeMapping.type(node);
            return new J.Identifier(randomId(), fmt, Markers.EMPTY, emptyList(), name, type, null);
        }

        @Override
        public J visitPrimitiveType(PrimitiveTypeTree node, Space fmt) {
            JCTree.JCPrimitiveTypeTree primitiveType = (JCTree.JCPrimitiveTypeTree) node;
            String name = primitiveType.toString();
            cursor += name.length();
            return new J.Identifier(randomId(), fmt, Markers.EMPTY, emptyList(), name, typeMapping.primitive(primitiveType.typetag), null);
        }

        @Override
        public J visitArrayType(ArrayTypeTree node, Space fmt) {
            TypeTree elemType = (TypeTree) scan(node.getType(), Space.EMPTY);

            int saveCursor = cursor;
            Space before = whitespace();
            JLeftPadded<Space> dimension;
            if (source.startsWith("[", cursor)) {
                cursor++;
                dimension = JLeftPadded.build(Space.build(sourceBeforeAsString("]"), emptyList())).withBefore(before);
            } else {
                cursor = saveCursor;
                return elemType.withPrefix(fmt);
            }

            return new J.ArrayType(
                    randomId(),
                    fmt,
                    Markers.EMPTY,
                    elemType,
                    null,
                    dimension,
                    typeMapping.type(node)
            );
        }

        private Space whitespace() {
            int nextNonWhitespace = indexOfNextNonWhitespace(cursor, source);
            if (nextNonWhitespace == cursor) {
                return EMPTY;
            }
            Space space = format(source, cursor, nextNonWhitespace);
            cursor = nextNonWhitespace;
            return space;
        }

        @Override
        public J visitParameterizedType(ParameterizedTypeTree node, Space fmt) {
            NameTree id = (NameTree) javaVisitor.scan(node.getType(), Space.EMPTY);
            List<JRightPadded<Expression>> expressions = new ArrayList<>(node.getTypeArguments().size());
            cursor += 1; // skip '<', JavaDocVisitor does not interpret List <Integer> as Parameterized.
            int argsSize = node.getTypeArguments().size();
            for (int i = 0; i < argsSize; i++) {
                Space space = Space.build(whitespaceBeforeAsString(), emptyList());
                JRightPadded<Expression> expression = JRightPadded.build((Expression) javaVisitor.scan(node.getTypeArguments().get(i), space));
                Space after;
                if (i == argsSize - 1) {
                    after = Space.build(sourceBeforeAsString(">"), emptyList());
                } else {
                    after = Space.build(sourceBeforeAsString(","), emptyList());
                }
                expression = expression.withAfter(after);
                expressions.add(expression);
            }
            return new J.ParameterizedType(randomId(), fmt, Markers.EMPTY, id, JContainer.build(expressions), typeMapping.type(node));
        }
    }
}
