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
package org.openrewrite.groovy;

import groovy.lang.GroovySystem;
import groovy.transform.Canonical;
import groovy.transform.Field;
import groovy.transform.Generated;
import groovy.transform.Immutable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.groovy.internal.Delimiter;
import org.openrewrite.groovy.marker.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isWhitespace;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.groovy.internal.Delimiter.*;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * See the <a href="https://groovy-lang.org/syntax.html">language syntax reference</a>.
 */
public class GroovyParserVisitor {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final int[] sourceLineNumberOffsets;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final GroovyTypeMapping typeMapping;

    private int cursor = 0;

    /** Maps a trait class name to its synthetic Groovy-generated {@code $Trait$Helper} class. */
    private final Map<String, ClassNode> traitHelpers = new HashMap<>();
    /** Maps a trait class name to its synthetic Groovy-generated {@code $Trait$FieldHelper} class. */
    private final Map<String, ClassNode> traitFieldHelpers = new HashMap<>();

    private static final Pattern MULTILINE_COMMENT_REGEX = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");

    @SuppressWarnings("RegExpSimplifiable")
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    /**
     * Elements within GString expressions which omit curly braces have column positions which are incorrect.
     * The column positions act like there *is* a curly brace.
     */
    private int columnOffset;

    @Nullable
    private static Boolean olderThanGroovy3;

    @Nullable
    private static Boolean groovy4OrLater;

    @SuppressWarnings("unused")
    public GroovyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source, JavaTypeCache typeCache, ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        AtomicInteger counter = new AtomicInteger(1);
        AtomicInteger offsetCursor = new AtomicInteger(0);
        this.sourceLineNumberOffsets = Arrays.stream(this.source.split("\n")).mapToInt(it -> {
            int saveCursor = offsetCursor.get();
            offsetCursor.set(saveCursor + it.length() + 1); // + 1 for the `\n` char
            return saveCursor;
        }).toArray();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
        this.typeMapping = new GroovyTypeMapping(typeCache);
    }

    private static int groovyMajorVersion() {
        String groovyVersionText = GroovySystem.getVersion();
        return Integer.parseInt(groovyVersionText.substring(0, groovyVersionText.indexOf('.')));
    }

    private static boolean isOlderThanGroovy3() {
        if (olderThanGroovy3 == null) {
            olderThanGroovy3 = groovyMajorVersion() < 3;
        }
        return olderThanGroovy3;
    }

    private static boolean isGroovy4OrLater() {
        if (groovy4OrLater == null) {
            groovy4OrLater = groovyMajorVersion() >= 4;
        }
        return groovy4OrLater;
    }

    public G.CompilationUnit visit(SourceUnit unit, ModuleNode ast) throws GroovyParsingException {
        NavigableMap<LineColumn, ASTNode> sortedByPosition = new TreeMap<>();
        for (org.codehaus.groovy.ast.stmt.Statement s : ast.getStatementBlock().getStatements()) {
            if (!isSynthetic(s)) {
                sortedByPosition.put(pos(s), s);
            }
        }
        String shebang = null;
        if (source.startsWith("#!")) {
            int i = 0;
            while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                i++;
            }
            shebang = source.substring(0, i);
            cursor += i;
        }
        Space prefix = EMPTY;
        JRightPadded<J.Package> pkg = null;
        if (ast.getPackage() != null) {
            prefix = whitespace();
            skip("package");
            pkg = maybeSemicolon(new J.Package(randomId(), EMPTY, Markers.EMPTY, typeTree(null), emptyList()));
        }

        for (ImportNode anImport : ast.getImports()) {
            sortedByPosition.put(pos(anImport), anImport);
        }
        for (ImportNode anImport : ast.getStarImports()) {
            sortedByPosition.put(pos(anImport), anImport);
        }
        for (ImportNode anImport : ast.getStaticImports().values()) {
            sortedByPosition.put(pos(anImport), anImport);
        }
        for (ImportNode anImport : getStaticStarImports(ast)) {
            sortedByPosition.put(pos(anImport), anImport);
        }

        for (ClassNode aClass : ast.getClasses()) {
            // skip over the synthetic script class
            if (!aClass.getName().equals(ast.getMainClassName()) || !aClass.getName().endsWith("doesntmatter")) {
                // synthetic helper classes Groovy generates for traits hold the bodies of trait methods/fields;
                // record them by their owning trait so we can merge bodies back when visiting the trait
                String name = aClass.getName();
                int helperIdx = name.indexOf("$Trait$Helper");
                if (helperIdx > 0) {
                    traitHelpers.put(name.substring(0, helperIdx), aClass);
                    continue;
                }
                int fieldHelperIdx = name.indexOf("$Trait$FieldHelper");
                if (fieldHelperIdx > 0) {
                    traitFieldHelpers.put(name.substring(0, fieldHelperIdx), aClass);
                    continue;
                }
                sortedByPosition.put(pos(aClass), aClass);
            }
        }

        for (MethodNode method : ast.getMethods()) {
            sortedByPosition.put(pos(method), method);
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(sortedByPosition.size());
        for (Map.Entry<LineColumn, ASTNode> entry : sortedByPosition.entrySet()) {
            if (entry.getKey().getLine() == -1) {
                // default import
                continue;
            }

            try {
                if (entry.getValue() instanceof InnerClassNode) {
                    // Inner classes will be visited as part of visiting their containing class
                    continue;
                }
                JRightPadded<Statement> statement = convertTopLevelStatement(unit, entry.getValue());
                if (statements.isEmpty() && pkg == null && statement.getElement() instanceof J.Import) {
                    prefix = statement.getElement().getPrefix();
                    statement = statement.withElement(statement.getElement().withPrefix(EMPTY));
                }
                statements.add(statement);
            } catch (Throwable t) {
                if (t instanceof StringIndexOutOfBoundsException) {
                    throw new GroovyParsingException("Failed to parse " + sourcePath + ", cursor position likely inaccurate.", t);
                }
                throw new GroovyParsingException(
                        "Failed to parse " + sourcePath + " at cursor position " + cursor +
                                ". The surrounding characters in the original source are:\n" +
                                source.substring(Math.max(0, cursor - 250), cursor) + "~cursor~>" +
                                source.substring(cursor, Math.min(source.length(), cursor + 250)), t);
            }
        }

        return new G.CompilationUnit(
                randomId(),
                shebang,
                prefix,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                pkg,
                statements,
                format(source, cursor, source.length())
        );
    }

    @RequiredArgsConstructor
    private class RewriteGroovyClassVisitor extends ClassCodeVisitorSupport {
        @Getter
        private final SourceUnit sourceUnit;

        private final Queue<Object> queue = new LinkedList<>();

        @Override
        public void visitClass(ClassNode clazz) {
            Space fmt = whitespace();
            List<J.Annotation> leadingAnnotations = visitAndGetAnnotations(clazz, this);
            List<J.Modifier> modifiers = getModifiers();

            Space kindPrefix = whitespace();
            J.ClassDeclaration.Kind.Type kindType;
            Markers kindMarkers = Markers.EMPTY;
            if (sourceStartsWith("class")) {
                kindType = J.ClassDeclaration.Kind.Type.Class;
                skip("class");
            } else if (sourceStartsWith("trait")) {
                kindType = J.ClassDeclaration.Kind.Type.Interface;
                kindMarkers = kindMarkers.addIfAbsent(new Trait(randomId()));
                skip("trait");
            } else if (clazz.isAnnotationDefinition()) {
                kindType = J.ClassDeclaration.Kind.Type.Annotation;
                skip("@interface");
            } else if (clazz.isInterface()) {
                kindType = J.ClassDeclaration.Kind.Type.Interface;
                skip("interface");
            } else if (clazz.isEnum()) {
                kindType = J.ClassDeclaration.Kind.Type.Enum;
                skip("enum");
            } else if (isGroovy4OrLater() && clazz.isRecord()) {
                kindType = J.ClassDeclaration.Kind.Type.Record;
                skip("record");
            } else {
                throw new IllegalStateException("Unexpected class type: " + name());
            }
            J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(randomId(), kindPrefix, kindMarkers, emptyList(), kindType);
            J.Identifier name = new J.Identifier(randomId(), whitespace(), Markers.EMPTY, emptyList(), name(), typeMapping.type(clazz), null);
            JContainer<J.TypeParameter> typeParameterContainer = null;
            if (clazz.isUsingGenerics() && clazz.getGenericsTypes() != null) {
                typeParameterContainer = visitTypeParameters(clazz.getGenericsTypes());
            }

            JContainer<Statement> primaryConstructor = null;
            if (kindType == J.ClassDeclaration.Kind.Type.Record) {
                Space pcPrefix = sourceBefore("(");
                List<RecordComponentNode> components = clazz.getRecordComponents();
                if (components == null || components.isEmpty()) {
                    primaryConstructor = JContainer.build(pcPrefix,
                            singletonList(JRightPadded.build((Statement) new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY))),
                            Markers.EMPTY);
                } else {
                    List<JRightPadded<Statement>> componentDecls = new ArrayList<>(components.size());
                    for (int i = 0; i < components.size(); i++) {
                        RecordComponentNode rc = components.get(i);
                        Space compPrefix = whitespace();
                        TypeTree typeExpr = visitTypeTree(rc.getType());
                        J.Identifier compName = new J.Identifier(randomId(), sourceBefore(rc.getName()), Markers.EMPTY,
                                emptyList(), rc.getName(), typeMapping.type(rc.getType()), null);
                        J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                                randomId(), compName.getPrefix(), Markers.EMPTY,
                                compName.withPrefix(EMPTY), emptyList(), null, typeMapping.variableType(compName.getSimpleName(), rc.getType()));
                        J.VariableDeclarations varDecl = new J.VariableDeclarations(randomId(), compPrefix, Markers.EMPTY,
                                emptyList(), emptyList(), typeExpr, null, emptyList(), singletonList(JRightPadded.build(namedVar)));
                        componentDecls.add(JRightPadded.build((Statement) varDecl)
                                .withAfter(i == components.size() - 1 ? sourceBefore(")") : sourceBefore(",")));
                    }
                    primaryConstructor = JContainer.build(pcPrefix, componentDecls, Markers.EMPTY);
                }
            }

            JLeftPadded<TypeTree> extendings = null;
            if (kindType == J.ClassDeclaration.Kind.Type.Class) {
                if (sourceStartsWith("extends")) {
                    extendings = padLeft(sourceBefore("extends"), visitTypeTree(clazz.getSuperClass()));
                }
            }

            JContainer<TypeTree> implementings = null;
            Markers implementingsMarkers = Markers.EMPTY;
            if (clazz.getInterfaces().length > 0) {
                Space implPrefix;
                boolean isTrait = kindMarkers.findFirst(Trait.class).isPresent();
                // Traits can have either "extends" (extending another trait) or "implements" (implementing a regular interface)
                boolean traitImplements = false;
                if (isTrait) {
                    int saveCursor = cursor;
                    whitespace();
                    traitImplements = source.startsWith("implements", cursor);
                    cursor = saveCursor;
                    implPrefix = traitImplements ? sourceBefore("implements") : sourceBefore("extends");
                } else if (kindType == J.ClassDeclaration.Kind.Type.Interface || kindType == J.ClassDeclaration.Kind.Type.Annotation) {
                    implPrefix = sourceBefore("extends");
                } else {
                    implPrefix = sourceBefore("implements");
                }
                if (traitImplements) {
                    implementingsMarkers = implementingsMarkers.addIfAbsent(new TraitImplementsKeyword(randomId()));
                }
                List<JRightPadded<TypeTree>> implTypes = new ArrayList<>(clazz.getInterfaces().length);
                ClassNode[] interfaces = clazz.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    ClassNode anInterface = interfaces[i];
                    // Any annotation @interface is listed as extending java.lang.annotation.Annotation, although it doesn't appear in source
                    if (kindType == J.ClassDeclaration.Kind.Type.Annotation && Annotation.class.getName().equals(anInterface.getName())) {
                        continue;
                    }
                    implTypes.add(JRightPadded.build(visitTypeTree(anInterface))
                            .withAfter(i == interfaces.length - 1 ? EMPTY : sourceBefore(",")));
                }
                // Can be empty for an annotation @interface which only implements Annotation
                if (!implTypes.isEmpty()) {
                    implementings = JContainer.build(implPrefix, implTypes, implementingsMarkers);
                }
            }

            JContainer<TypeTree> permitting = null;
            if (isGroovy4OrLater() && clazz.isSealed() && clazz.getPermittedSubclasses() != null && !clazz.getPermittedSubclasses().isEmpty()) {
                Space permitsPrefix = sourceBefore("permits");
                List<ClassNode> permitted = clazz.getPermittedSubclasses();
                List<JRightPadded<TypeTree>> permitTypes = new ArrayList<>(permitted.size());
                for (int i = 0; i < permitted.size(); i++) {
                    permitTypes.add(JRightPadded.build(visitTypeTree(permitted.get(i)))
                            .withAfter(i == permitted.size() - 1 ? EMPTY : sourceBefore(",")));
                }
                permitting = JContainer.build(permitsPrefix, permitTypes, Markers.EMPTY);
            }

            queue.add(new J.ClassDeclaration(randomId(), fmt, Markers.EMPTY,
                    leadingAnnotations,
                    modifiers,
                    kind,
                    name,
                    typeParameterContainer,
                    primaryConstructor,
                    extendings,
                    implementings,
                    permitting,
                    visitClassBlock(clazz),
                    TypeUtils.asFullyQualified(typeMapping.type(clazz))));
        }

        /**
         * Find the helper method on a trait's {@code $Trait$Helper} class that corresponds to {@code traitMethod}.
         * The helper version has the trait instance prepended as a synthetic {@code $self} parameter.
         */
        private @Nullable MethodNode findTraitHelperMethod(ClassNode helper, MethodNode traitMethod) {
            for (MethodNode candidate : helper.getMethods(traitMethod.getName())) {
                Parameter[] params = candidate.getParameters();
                if (params.length == traitMethod.getParameters().length + 1 &&
                        ("$self".equals(params[0].getName()) || "$static$self".equals(params[0].getName()))) {
                    return candidate;
                }
            }
            return null;
        }

        /**
         * Recover trait field declarations whose source positions were lost by Groovy's trait AST transformation.
         * The transformation strips fields from the trait class and exposes them only as abstract accessors on a
         * {@code $Trait$FieldHelper} interface. Walk the helper for accessor methods to learn each field's name,
         * type, and modifiers, scan the trait body source for the matching declaration, then synthesize a
         * {@link FieldNode} with valid source positions and add it to {@code sortedByPosition} so the existing
         * field-printing path picks it up.
         */
        private void recoverTraitFields(ClassNode clazz, ClassNode fieldHelper, NavigableMap<LineColumn, ASTNode> sortedByPosition, int blockBodyStart) {
            String traitName = clazz.getNameWithoutPackage();
            String accessorPrefix = traitName + "__";
            // fieldName -> getter (carries return type + modifiers)
            Map<String, MethodNode> getters = new LinkedHashMap<>();
            for (MethodNode m : fieldHelper.getMethods()) {
                String name = m.getName();
                if (name.startsWith(accessorPrefix) && name.endsWith("$get")) {
                    getters.put(name.substring(accessorPrefix.length(), name.length() - 4), m);
                }
            }
            // Backing fields on the helper carry the original type and initial expression even when their own
            // positions are -1. Static-only fields don't get $get/$set accessors, so the backing fields are also
            // the only place to discover their names.
            Map<String, FieldNode> backingFields = new LinkedHashMap<>();
            for (FieldNode bf : fieldHelper.getFields()) {
                int idx = bf.getName().indexOf(accessorPrefix);
                if (idx >= 0) {
                    String fieldName = bf.getName().substring(idx + accessorPrefix.length());
                    FieldNode existing = backingFields.get(fieldName);
                    if (existing == null || (existing.getInitialExpression() == null && bf.getInitialExpression() != null)) {
                        backingFields.put(fieldName, bf);
                    }
                }
            }
            // Trait field initializers are stored on the trait helper as setter calls inside $init$ / $static$init$:
            //     ((TraitName$Trait$FieldHelper) $self).TraitName__field$set((castedType) <init expr>)
            // Walk those bodies to recover the original initial expression for each field.
            Map<String, org.codehaus.groovy.ast.expr.Expression> initialExpressions = new HashMap<>();
            ClassNode traitHelperClass = traitHelpers.get(clazz.getName());
            if (traitHelperClass != null) {
                for (MethodNode m : traitHelperClass.getMethods()) {
                    if (("$init$".equals(m.getName()) || "$static$init$".equals(m.getName())) && m.getCode() instanceof BlockStatement) {
                        for (org.codehaus.groovy.ast.stmt.Statement s : ((BlockStatement) m.getCode()).getStatements()) {
                            if (!(s instanceof org.codehaus.groovy.ast.stmt.ExpressionStatement)) {
                                continue;
                            }
                            org.codehaus.groovy.ast.expr.Expression e = ((org.codehaus.groovy.ast.stmt.ExpressionStatement) s).getExpression();
                            if (!(e instanceof MethodCallExpression)) {
                                continue;
                            }
                            MethodCallExpression mce = (MethodCallExpression) e;
                            String methodName = mce.getMethodAsString();
                            if (methodName == null || !(mce.getArguments() instanceof org.codehaus.groovy.ast.expr.TupleExpression)) {
                                continue;
                            }
                            List<org.codehaus.groovy.ast.expr.Expression> args = ((org.codehaus.groovy.ast.expr.TupleExpression) mce.getArguments()).getExpressions();
                            String fieldName = null;
                            org.codehaus.groovy.ast.expr.Expression initArg = null;
                            if (methodName.startsWith(accessorPrefix) && methodName.endsWith("$set") && !args.isEmpty()) {
                                // Direct setter call: ((FieldHelper) $self).TraitName__field$set(<initExpr>)
                                fieldName = methodName.substring(accessorPrefix.length(), methodName.length() - 4);
                                initArg = args.get(0);
                            } else if ("invokeStaticMethod".equals(methodName) && args.size() >= 3 &&
                                    args.get(1) instanceof ConstantExpression &&
                                    ((ConstantExpression) args.get(1)).getValue() instanceof String) {
                                // Reflective static setter: InvokerHelper.invokeStaticMethod($static$self, "TraitName__field$set", <initExpr>)
                                String setterName = (String) ((ConstantExpression) args.get(1)).getValue();
                                if (setterName.startsWith(accessorPrefix) && setterName.endsWith("$set")) {
                                    fieldName = setterName.substring(accessorPrefix.length(), setterName.length() - 4);
                                    initArg = args.get(2);
                                }
                            }
                            if (fieldName != null && initArg != null) {
                                if (initArg instanceof CastExpression) {
                                    initArg = ((CastExpression) initArg).getExpression();
                                }
                                initialExpressions.put(fieldName, initArg);
                            }
                        }
                    }
                }
            }
            Set<String> fieldNames = new LinkedHashSet<>(getters.keySet());
            fieldNames.addAll(backingFields.keySet());
            if (fieldNames.isEmpty()) {
                return;
            }
            Map<String, Integer> declarationStarts = scanTraitBodyForFieldDeclarations(blockBodyStart, fieldNames);
            for (Map.Entry<String, Integer> e : declarationStarts.entrySet()) {
                String fieldName = e.getKey();
                int declStart = e.getValue();
                MethodNode getter = getters.get(fieldName);
                FieldNode backing = backingFields.get(fieldName);
                int modifiers;
                ClassNode fieldType;
                if (getter != null) {
                    modifiers = getter.getModifiers() & ~java.lang.reflect.Modifier.ABSTRACT;
                    fieldType = getter.getReturnType();
                } else {
                    // Static-only fields lack accessors; reconstruct modifiers from the backing field.
                    modifiers = backing.getModifiers() & ~java.lang.reflect.Modifier.PRIVATE & ~java.lang.reflect.Modifier.FINAL;
                    fieldType = backing.getOriginType();
                }
                org.codehaus.groovy.ast.expr.Expression initExpr = initialExpressions.get(fieldName);
                if (initExpr == null && backing != null) {
                    initExpr = backing.getInitialExpression();
                }
                FieldNode synth = new FieldNode(
                        fieldName,
                        modifiers,
                        fieldType,
                        clazz,
                        initExpr
                );
                int line = lineOf(declStart);
                int column = declStart - sourceLineNumberOffsets[line - 1] + 1;
                synth.setLineNumber(line);
                synth.setColumnNumber(column);
                synth.setLastLineNumber(line);
                synth.setLastColumnNumber(column + fieldName.length());
                sortedByPosition.put(pos(synth), synth);
            }
        }

        /**
         * Scan from {@code start} (just past the trait body's opening {@code {}) to the matching {@code }},
         * skipping nested braces, strings, comments, and regex/slashy literals. Return the start position of
         * each top-level declaration whose name appears in {@code fieldNames}.
         */
        private Map<String, Integer> scanTraitBodyForFieldDeclarations(int start, Set<String> fieldNames) {
            Map<String, Integer> result = new LinkedHashMap<>();
            int len = source.length();
            int depth = 1;
            // Tracks where the next top-level statement *could* start (after a separator, just past whitespace).
            int statementStart = start;
            int i = start;
            while (i < len && depth > 0) {
                char c = source.charAt(i);
                if (depth == 1) {
                    Delimiter d = getDelimiter(null, i);
                    if (d != null && d != CLOSURE && d != ARRAY) {
                        // Skip past string/comment/regex
                        i += d.open.length();
                        while (i < len && !source.startsWith(d.close, i)) {
                            i++;
                        }
                        i += d.close.length();
                        continue;
                    }
                }
                if (c == '{') {
                    depth++;
                    i++;
                    continue;
                }
                if (c == '}') {
                    depth--;
                    i++;
                    if (depth == 1) {
                        statementStart = i;
                    }
                    continue;
                }
                if (depth > 1) {
                    i++;
                    continue;
                }
                if (c == ';' || c == '\n') {
                    i++;
                    statementStart = i;
                    continue;
                }
                if (Character.isJavaIdentifierStart(c)) {
                    int idStart = i;
                    while (i < len && Character.isJavaIdentifierPart(source.charAt(i))) {
                        i++;
                    }
                    String id = source.substring(idStart, i);
                    if (fieldNames.contains(id) && !result.containsKey(id)) {
                        // Verify this looks like a declaration: after the identifier, the next non-whitespace
                        // is one of `=`, `;`, `\n`, `}` (i.e., ends a simple declaration), not `(` (method),
                        // `.` (chained access), or operators that would indicate a reference.
                        int j = i;
                        while (j < len && (source.charAt(j) == ' ' || source.charAt(j) == '\t')) {
                            j++;
                        }
                        if (j < len) {
                            char next = source.charAt(j);
                            if (next == '=' || next == '\n' || next == ';' || next == '}' || next == '\r') {
                                // Find first non-whitespace from statementStart - that's the declaration start
                                int declStart = statementStart;
                                while (declStart < idStart && Character.isWhitespace(source.charAt(declStart))) {
                                    declStart++;
                                }
                                if (declStart <= idStart) {
                                    result.put(id, declStart);
                                }
                            }
                        }
                    }
                    continue;
                }
                i++;
            }
            return result;
        }

        private int lineOf(int position) {
            int line = Arrays.binarySearch(sourceLineNumberOffsets, position);
            return line >= 0 ? line + 1 : -line - 1;
        }

        J.Block visitClassBlock(ClassNode clazz) {
            NavigableMap<LineColumn, ASTNode> sortedByPosition = new TreeMap<>();
            List<FieldNode> enumConstants = new ArrayList<>();
            // Groovy's trait AST transformation strips method bodies from the trait and moves them to a synthetic
            // $Trait$Helper class, leaving the trait class with only abstract method signatures. Substitute each
            // trait method with its helper counterpart so we can recover the original body when visiting it.
            ClassNode helperForTrait = traitHelpers.get(clazz.getName());
            for (MethodNode method : clazz.getMethods()) {
                // Most synthetic methods do not appear in source code and should be skipped entirely.
                if (method.isSynthetic()) {
                    // Static initializer blocks show up as a synthetic method with name <clinit>
                    // The part that actually appears in source code is a block statement inside the method declaration
                    if ("<clinit>".equals(method.getName()) && method.getCode() instanceof BlockStatement && ((BlockStatement) method.getCode()).getStatements().size() == 1) {
                        org.codehaus.groovy.ast.stmt.Statement statement = ((BlockStatement) method.getCode()).getStatements().get(0);
                        sortedByPosition.put(pos(statement), statement);
                    }
                } else if (method.getAnnotations(new ClassNode(Generated.class)).isEmpty() && appearsInSource(method)) {
                    MethodNode toAdd = method;
                    if (helperForTrait != null) {
                        MethodNode helperMethod = findTraitHelperMethod(helperForTrait, method);
                        if (helperMethod != null) {
                            toAdd = helperMethod;
                        }
                    }
                    sortedByPosition.put(pos(toAdd), toAdd);
                }
            }
            // For traits, the AST transformation may move static methods entirely to the $Trait$Helper class,
            // leaving the trait class with no source-positioned counterpart. Pick those up from the helper.
            if (helperForTrait != null) {
                for (MethodNode helperMethod : helperForTrait.getMethods()) {
                    if (helperMethod.isSynthetic() || !appearsInSource(helperMethod)) {
                        continue;
                    }
                    Parameter[] params = helperMethod.getParameters();
                    if (params.length == 0 || !"$static$self".equals(params[0].getName())) {
                        continue;
                    }
                    sortedByPosition.put(pos(helperMethod), helperMethod);
                }
            }
            for (org.codehaus.groovy.ast.stmt.Statement objectInitializer : clazz.getObjectInitializerStatements()) {
                if (!(objectInitializer instanceof BlockStatement)) {
                    continue;
                }
                // A class initializer BlockStatement will be wrapped in an otherwise empty BlockStatement with the same source positions
                // No idea why, except speculation that it is for consistency with static intiializers, but we can skip the wrapper and just visit its contents
                BlockStatement s = (BlockStatement) objectInitializer;
                if (s.getStatements().size() == 1 && pos(s).equals(pos(s.getStatements().get(0)))) {
                    s = (BlockStatement) s.getStatements().get(0);
                }
                sortedByPosition.put(pos(s), s);
            }
            /*
              In certain circumstances the same AST node may appear in multiple places.
              class A {
                  def a = new Object() {
                      // this anonymous class is both part of the initializing expression for the variable "a"
                      // And appears in the list of inner classes of "A"
                  }
              }
              So keep track of inner classes that are part of field initializers so that they don't get parsed twice
             */
            Set<InnerClassNode> fieldInitializers = new HashSet<>();
            Set<String> recordComponentNames = new HashSet<>();
            if (isGroovy4OrLater() && clazz.getRecordComponents() != null) {
                for (RecordComponentNode rc : clazz.getRecordComponents()) {
                    recordComponentNames.add(rc.getName());
                }
            }
            for (FieldNode field : clazz.getFields()) {
                if (!appearsInSource(field)) {
                    continue;
                }
                if (field.isEnum()) {
                    enumConstants.add(field);
                    continue;
                }
                // Record component backing fields are synthetic but have source positions; skip them
                if (field.isSynthetic() && recordComponentNames.contains(field.getName())) {
                    continue;
                }
                if (field.getInitialExpression() instanceof ConstructorCallExpression) {
                    ConstructorCallExpression cce = (ConstructorCallExpression) field.getInitialExpression();
                    if (cce.isUsingAnonymousInnerClass() && cce.getType() instanceof InnerClassNode) {
                        fieldInitializers.add((InnerClassNode) cce.getType());
                    }
                }
                sortedByPosition.put(pos(field), field);
            }
            for (ConstructorNode ctor : clazz.getDeclaredConstructors()) {
                if (!appearsInSource(ctor)) {
                    continue;
                }
                sortedByPosition.put(pos(ctor), ctor);
            }
            Iterator<InnerClassNode> innerClassIterator = clazz.getInnerClasses();
            while (innerClassIterator.hasNext()) {
                InnerClassNode icn = innerClassIterator.next();
                if (icn.isSynthetic() || fieldInitializers.contains(icn) || icn.getName().contains("$Trait$") || !appearsInSource(icn)) {
                    continue;
                }
                sortedByPosition.put(pos(icn), icn);
            }

            Space blockPrefix = sourceBefore("{");

            // The trait AST transformation strips fields entirely from the trait class and synthesizes them on the
            // $Trait$FieldHelper interface with no source positions. Recover them by reading source between the
            // trait's `{` and matching `}` for declarations matching the field names exposed by the helper getters.
            ClassNode fieldHelperForTrait = traitFieldHelpers.get(clazz.getName());
            if (fieldHelperForTrait != null) {
                recoverTraitFields(clazz, fieldHelperForTrait, sortedByPosition, cursor);
            }

            List<JRightPadded<Statement>> statements = new ArrayList<>();
            if (!enumConstants.isEmpty()) {
                enumConstants.sort(Comparator.comparing(GroovyParserVisitor::pos));

                List<JRightPadded<J.EnumValue>> enumValues = new ArrayList<>();
                for (int i = 0; i < enumConstants.size(); i++) {
                    J.EnumValue enumValue = visitEnumField(enumConstants.get(i));
                    JRightPadded<J.EnumValue> paddedEnumValue = JRightPadded.build(enumValue).withAfter(whitespace());
                    if (sourceStartsWith(",")) {
                        skip(",");
                        if (i == enumConstants.size() - 1) {
                            paddedEnumValue = paddedEnumValue.withMarkers(Markers.build(singleton(new TrailingComma(randomId(), whitespace()))));
                        }
                    }
                    enumValues.add(paddedEnumValue);
                }

                J.EnumValueSet enumValueSet = new J.EnumValueSet(randomId(), EMPTY, Markers.EMPTY, enumValues, sourceStartsWith(";"));
                if (enumValueSet.isTerminatedWithSemicolon()) {
                    skip(";");
                }
                statements.add(JRightPadded.build(enumValueSet));
            }

            return new J.Block(randomId(), blockPrefix, Markers.EMPTY,
                    JRightPadded.build(false),
                    ListUtils.concatAll(statements, sortedByPosition.values().stream()
                            // anonymous classes will be visited as part of visiting the ConstructorCallExpression
                            .filter(ast -> !(ast instanceof InnerClassNode && ((InnerClassNode) ast).isAnonymous()))
                            .map(ast -> {
                                if (ast instanceof FieldNode) {
                                    visitField((FieldNode) ast);
                                } else if (ast instanceof MethodNode) {
                                    visitMethod((MethodNode) ast);
                                } else if (ast instanceof ClassNode) {
                                    visitClass((ClassNode) ast);
                                } else if (ast instanceof BlockStatement) {
                                    visitBlockStatement((BlockStatement) ast);
                                }
                                Statement stat = pollQueue();
                                return maybeSemicolon(stat);
                            })
                            .collect(toList())),
                    sourceBefore("}"));
        }

        @Override
        public void visitBlockStatement(BlockStatement statement) {
            queue.add(new RewriteGroovyVisitor(statement, this)
                    .doVisit(statement));
        }

        @Override
        public void visitField(FieldNode field) {
            if (field.isEnum()) {
                // Enum constants are handled separately in visitClassBlock, thus should be skipped here
                return;
            }
            visitVariableField(field);
        }

        private J.EnumValue visitEnumField(FieldNode field) {
            Space prefix = whitespace();

            List<J.Annotation> annotations = visitAndGetAnnotations(field, this);

            Space namePrefix = whitespace();
            String enumName = skip(field.getName());

            J.Identifier name = new J.Identifier(randomId(), namePrefix, Markers.EMPTY, emptyList(), enumName, typeMapping.type(field.getType()), typeMapping.variableType(field));

            // Groovy 4 represents enum constants with anonymous class bodies by appending a
            // ClassExpression as the last element of the ListExpression initializer
            InnerClassNode anonymousClassNode = null;
            if (field.getInitialExpression() instanceof ListExpression) {
                ListExpression listExpr = (ListExpression) field.getInitialExpression();
                List<org.codehaus.groovy.ast.expr.Expression> allExprs = listExpr.getExpressions();
                if (!allExprs.isEmpty() && allExprs.get(allExprs.size() - 1) instanceof ClassExpression) {
                    ClassExpression classExpr = (ClassExpression) allExprs.get(allExprs.size() - 1);
                    if (classExpr.getType() instanceof InnerClassNode && ((InnerClassNode) classExpr.getType()).isAnonymous()) {
                        anonymousClassNode = (InnerClassNode) classExpr.getType();
                    }
                }
            }

            J.NewClass initializer = null;
            if (sourceStartsWith("(")) {
                Space prefixNewClass = whitespace();
                skip("(");
                RewriteGroovyVisitor visitor = new RewriteGroovyVisitor(field, this);
                ListExpression arguments = (ListExpression) field.getInitialExpression();
                // Filter out the trailing ClassExpression that represents the anonymous class body
                List<org.codehaus.groovy.ast.expr.Expression> realArgs = anonymousClassNode != null ?
                        arguments.getExpressions().subList(0, arguments.getExpressions().size() - 1) :
                        arguments.getExpressions();
                List<JRightPadded<Expression>> list;
                if (realArgs.isEmpty()) {
                    list = singletonList(JRightPadded.build((Expression) new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY)));
                } else {
                    list = visitor.convertAll(realArgs, n -> sourceBefore(","), n -> whitespace(), n -> {
                        if (n == realArgs.get(realArgs.size() - 1) && source.charAt(cursor) == ',') {
                            cursor++;
                            return Markers.build(singleton(new TrailingComma(randomId(), whitespace())));
                        }
                        return Markers.EMPTY;
                    });
                    skip(")");
                }

                MethodNode ctor = null;
                for (ConstructorNode constructor : field.getOwner().getDeclaredConstructors()) {
                    if (constructor.getParameters().length == realArgs.size()) {
                        ctor = constructor;
                    }
                }

                J.Block body = anonymousClassNode != null ? visitClassBlock(anonymousClassNode) : null;
                initializer = new J.NewClass(randomId(), prefixNewClass, Markers.EMPTY, null, EMPTY, null, JContainer.build(list), body, typeMapping.methodType(ctor));
            } else if (anonymousClassNode != null) {
                // Enum constant with anonymous body but no constructor args, e.g. `A2 { ... }`
                MethodNode ctor = null;
                for (ConstructorNode constructor : field.getOwner().getDeclaredConstructors()) {
                    if (constructor.getParameters().length == 0) {
                        ctor = constructor;
                    }
                }
                JContainer<Expression> args = JContainer.<Expression>empty()
                        .withMarkers(Markers.build(singleton(new OmitParentheses(randomId()))));
                J.Block body = visitClassBlock(anonymousClassNode);
                initializer = new J.NewClass(randomId(), EMPTY, Markers.EMPTY, null, EMPTY, null, args, body, typeMapping.methodType(ctor));
            }

            return new J.EnumValue(randomId(), prefix, Markers.EMPTY, annotations, name, initializer);
        }

        private void visitVariableField(FieldNode field) {
            RewriteGroovyVisitor visitor = new RewriteGroovyVisitor(field, this);

            // Groovy emits a separate FieldNode per variable in a multi-variable declaration (e.g. `final String a, b`);
            // continuation fields have no modifiers or type keyword in the source, so skip past just the comma.
            Optional<MultiVariable> multiVariable = visitor.maybeMultiVariable();
            List<J.Annotation> annotations = multiVariable.isPresent() ? emptyList() : visitAndGetAnnotations(field, this);
            List<J.Modifier> modifiers = multiVariable.isPresent() ? emptyList() : getModifiers();
            TypeTree typeExpr;
            if (field.isDynamicTyped()) {
                typeExpr = null;
            } else if (multiVariable.isPresent()) {
                typeExpr = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), "", typeMapping.type(field.getOriginType()), null);
            } else {
                typeExpr = visitTypeTree(field.getOriginType());
            }

            J.Identifier name = new J.Identifier(randomId(), sourceBefore(field.getName()), Markers.EMPTY,
                    emptyList(), field.getName(), typeMapping.type(field.getOriginType()), typeMapping.variableType(field));

            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    name.getPrefix(),
                    Markers.EMPTY,
                    name.withPrefix(EMPTY),
                    emptyList(),
                    null,
                    typeMapping.variableType(field)
            );

            if (field.getInitialExpression() != null) {
                Space beforeAssign = sourceBefore("=");
                Expression initializer = visitor.doVisit(field.getInitialExpression());
                namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    multiVariable.<Markers>map(mv -> Markers.build(singleton(mv))).orElse(Markers.EMPTY),
                    annotations,
                    modifiers,
                    typeExpr,
                    null,
                    singletonList(JRightPadded.build(namedVariable))
            );

            queue.add(variableDeclarations);
        }

        @Override
        protected void visitAnnotation(AnnotationNode annotation) {
            GroovyParserVisitor.this.visitAnnotation(annotation, this);
        }

        @Override
        public void visitMethod(MethodNode method) {
            Space fmt = whitespace();

            List<J.Annotation> annotations = visitAndGetAnnotations(method, this);
            List<J.Modifier> modifiers = getModifiers();
            boolean isConstructorOfEnum = false;
            boolean isConstructorOfInnerNonStaticClass = false;
            J.TypeParameters typeParameters = null;
            if (method.getGenericsTypes() != null) {
                Space prefix = sourceBefore("<");
                GenericsType[] genericsTypes = method.getGenericsTypes();
                List<JRightPadded<J.TypeParameter>> typeParametersList = new ArrayList<>(genericsTypes.length);
                for (int i = 0; i < genericsTypes.length; i++) {
                    typeParametersList.add(JRightPadded.build(visitTypeParameter(genericsTypes[i]))
                            .withAfter(i < genericsTypes.length - 1 ? sourceBefore(",") : sourceBefore(">")));
                }
                typeParameters = new J.TypeParameters(randomId(), prefix, Markers.EMPTY, emptyList(), typeParametersList);
            }
            TypeTree returnType = method instanceof ConstructorNode || method.isDynamicReturnType() ? null : visitTypeTree(method.getReturnType());

            Space namePrefix = whitespace();
            String methodName;
            if (method instanceof ConstructorNode) {
                // To support special constructors well, the groovy compiler adds extra parameters and statements to the constructor under the hood.
                // In our LST, we don't need this internal logic.
                if (method.getDeclaringClass().isEnum()) {
                    /*
                    For enums, there are two extra parameters and wraps the block in a super call:
                    enum A {                                enum A {
                      A1                                      A1
                      A(String s) {           =>             A(String __str, int __int, String s) {
                        println "ss"          =>                super() { println "ss" }
                      }                                       }
                    }                                       }
                    */
                    isConstructorOfEnum = true;
                } else {
                    /*
                    For Java syntax for non-static inner classes, there's an extra parameter with a reference to its parent class and two statements (ConstructorCallExpression and BlockStatement):
                    class A {                               class A {
                      class B {                               class B {
                        String s                                String s
                        B(String s) {           =>              B(A $p$, String s) {
                                                =>                new Object().this$0 = $p$
                          this.s = s            =>                this.s = s
                        }                                       }
                      }                                       }
                    }
                    See also: https://groovy-lang.org/differences.html#_creating_instances_of_non_static_inner_classes
                    */
                    isConstructorOfInnerNonStaticClass = method.getDeclaringClass() instanceof InnerClassNode && (method.getDeclaringClass().getModifiers() & Modifier.STATIC) == 0;
                }
                methodName = method.getDeclaringClass().getNameWithoutPackage().replaceFirst(".*\\$", "");
            } else if (source.startsWith(method.getName(), cursor)) {
                methodName = method.getName();
            } else {
                // Method name might be in quotes
                String delim = source.charAt(cursor) + "";
                methodName = sourceSubstring(cursor, delim) + delim;
            }
            cursor += methodName.length();
            J.Identifier name = new J.Identifier(randomId(),
                    namePrefix,
                    Markers.EMPTY,
                    emptyList(),
                    methodName,
                    null, null);

            RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor(method, this);

            // Parameter has no visit implementation, so we've got to do this by hand
            Space beforeParen = sourceBefore("(");
            List<JRightPadded<Statement>> params = new ArrayList<>(method.getParameters().length);
            Parameter[] unparsedParams = method.getParameters();
            // For trait methods (which are stored on the synthetic $Trait$Helper class), skip the
            // synthetic first parameter ($self / $static$self) the trait transformation prepends.
            boolean isTraitHelperMethod = method.getDeclaringClass() != null &&
                    method.getDeclaringClass().getName().contains("$Trait$Helper") &&
                    unparsedParams.length > 0 &&
                    ("$self".equals(unparsedParams[0].getName()) || "$static$self".equals(unparsedParams[0].getName()));
            int skipParams = isConstructorOfEnum ? 2 : isConstructorOfInnerNonStaticClass ? 1 : isTraitHelperMethod ? 1 : 0;
            for (int i = skipParams; i < unparsedParams.length; i++) {
                Parameter param = unparsedParams[i];

                List<J.Annotation> paramAnnotations = visitAndGetAnnotations(param, this);
                List<J.Modifier> paramModifiers = getModifiers();
                TypeTree paramType;
                if (param.isDynamicTyped()) {
                    if (sourceStartsWith("java.lang.Object")) {
                        paramType = new J.Identifier(randomId(), whitespace(), Markers.EMPTY, emptyList(), skip(name()), JavaType.ShallowClass.build("java.lang.Object"), null);
                    } else {
                        paramType = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), "", JavaType.ShallowClass.build("java.lang.Object"), null);
                    }
                } else {
                    paramType = visitTypeTree(param.getType());
                }

                Space varargs = null;
                if (paramType instanceof J.ArrayType && sourceStartsWith("...")) {
                    int varargStart = indexOfNextNonWhitespace(cursor, source);
                    varargs = format(source, cursor, varargStart);
                    cursor = varargStart + 3;
                }

                JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                        new J.VariableDeclarations.NamedVariable(randomId(), EMPTY, Markers.EMPTY,
                                new J.Identifier(randomId(), whitespace(), Markers.EMPTY, emptyList(), param.getName(), null, null),
                                emptyList(), null, null)
                );
                skip(param.getName());

                org.codehaus.groovy.ast.expr.Expression defaultValue = param.getInitialExpression();
                if (defaultValue != null) {
                    paramName = paramName.withElement(paramName.getElement().getPadding()
                            .withInitializer(new JLeftPadded<>(
                                    sourceBefore("="),
                                    new RewriteGroovyVisitor(defaultValue, this).doVisit(defaultValue),
                                    Markers.EMPTY)));
                }
                Space rightPad = sourceBefore(i == unparsedParams.length - 1 ? ")" : ",");

                params.add(JRightPadded.build((Statement) new J.VariableDeclarations(randomId(), EMPTY,
                        Markers.EMPTY, paramAnnotations, paramModifiers, paramType,
                        varargs,
                        singletonList(paramName))).withAfter(rightPad));
            }

            if (unparsedParams.length == skipParams) {
                params.add(JRightPadded.build(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY)));
            }

            JContainer<NameTree> throws_ = method.getExceptions().length == 0 ? null : JContainer.build(
                    sourceBefore("throws"),
                    bodyVisitor.visitRightPadded(method.getExceptions(), null),
                    Markers.EMPTY
            );

            J.Block body = null;
            if (method.getCode() != null) {
                if (isConstructorOfInnerNonStaticClass) {
                    // Groovy injects a synthetic super() call at index 0 (when the user didn't write one)
                    // and a synthetic `this.this$0 = $p$` BlockStatement at index 1. Keep an explicit
                    // user-written super() call (line number > 0) and skip both synthetic statements.
                    List<org.codehaus.groovy.ast.stmt.Statement> all = ((BlockStatement) method.getCode()).getStatements();
                    List<org.codehaus.groovy.ast.stmt.Statement> visible = new ArrayList<>(all.size());
                    if (!all.isEmpty() && all.get(0).getLineNumber() > 0) {
                        visible.add(all.get(0));
                    }
                    if (all.size() > 2) {
                        visible.addAll(all.subList(2, all.size()));
                    }
                    body = bodyVisitor.doVisit(
                            new BlockStatement(visible, ((BlockStatement) method.getCode()).getVariableScope())
                    );
                } else if (isConstructorOfEnum && ((BlockStatement) method.getCode()).getStatements().size() > 1) {
                    org.codehaus.groovy.ast.stmt.Statement node = ((BlockStatement) method.getCode()).getStatements().get(1);
                    if (node instanceof BlockStatement) {
                        body = bodyVisitor.doVisit(node);
                    } else {
                        body = bodyVisitor.doVisit(method.getCode());
                    }
                } else {
                    if (annotations.stream().anyMatch(a -> TypeUtils.isOfClassType(a.getAnnotationType().getType(), "groovy.transform.Synchronized"))) {
                        body = bodyVisitor.doVisit(((SynchronizedStatement) method.getCode()).getCode());
                    } else if (annotations.stream().anyMatch(a -> TypeUtils.isOfClassType(a.getAnnotationType().getType(), "groovy.test.NotYetImplemented") ||
                            TypeUtils.isOfClassType(a.getAnnotationType().getType(), "groovy.transform.NotYetImplemented"))) {
                        // The @NotYetImplemented AST transformation wraps the original method body in a TryCatchStatement
                        // followed by a ThrowStatement; unwrap it so source positions align with the original body
                        org.codehaus.groovy.ast.stmt.Statement code = method.getCode();
                        if (code instanceof BlockStatement && !((BlockStatement) code).getStatements().isEmpty() &&
                                ((BlockStatement) code).getStatements().get(0) instanceof TryCatchStatement) {
                            body = bodyVisitor.doVisit(((TryCatchStatement) ((BlockStatement) code).getStatements().get(0)).getTryStatement());
                        } else {
                            body = bodyVisitor.doVisit(method.getCode());
                        }
                    } else {
                        body = bodyVisitor.doVisit(method.getCode());
                    }
                }
            }

            queue.add(new J.MethodDeclaration(
                    randomId(), fmt,
                    Markers.EMPTY,
                    annotations,
                    modifiers,
                    typeParameters,
                    returnType,
                    new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                    JContainer.build(beforeParen, params, Markers.EMPTY),
                    throws_,
                    body,
                    null,
                    typeMapping.methodType(method)
            ));
        }

        @SuppressWarnings({"ConstantConditions", "unchecked"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private class RewriteGroovyVisitor extends CodeVisitorSupport {
        private Cursor nodeCursor;
        private final Queue<Object> queue = new LinkedList<>();
        private final RewriteGroovyClassVisitor classVisitor;

        public RewriteGroovyVisitor(ASTNode root, RewriteGroovyClassVisitor classVisitor) {
            this.nodeCursor = new Cursor(null, root);
            this.classVisitor = classVisitor;
        }

        private <T> T doVisit(ASTNode node) {
            nodeCursor = new Cursor(nodeCursor, node);
            if (node instanceof AnnotationConstantExpression) {
                // AnnotationConstantExpression.visit() walks the inner annotation's members itself,
                // which would re-visit them at the wrong cursor position. Dispatch directly instead.
                queue.add(visitAnnotation((AnnotationNode) ((AnnotationConstantExpression) node).getValue(), classVisitor));
            } else {
                node.visit(this);
            }
            nodeCursor = nodeCursor.getParentOrThrow();
            return pollQueue();
        }

        private <T> List<JRightPadded<T>> visitRightPadded(ASTNode[] nodes, @Nullable String afterLast) {
            List<JRightPadded<T>> ts = new ArrayList<>(nodes.length);
            for (int i = 0; i < nodes.length; i++) {
                ASTNode node = nodes[i];
                @SuppressWarnings("unchecked")
                JRightPadded<T> converted = JRightPadded.build(node instanceof ClassNode ? (T) visitTypeTree((ClassNode) node) : doVisit(node));
                if (i == nodes.length - 1) {
                    converted = converted.withAfter(whitespace());
                    if (',' == source.charAt(cursor)) {
                        // In Groovy trailing "," are allowed
                        skip(",");
                        converted = converted.withMarkers(Markers.EMPTY.add(new TrailingComma(randomId(), whitespace())));
                    }
                    ts.add(converted);
                    skip(afterLast);
                } else {
                    ts.add(converted.withAfter(sourceBefore(",")));
                }
            }
            return ts;
        }

        private Expression insideParentheses(ASTNode node, Function<Space, Expression> parenthesizedTree) {
            Integer insideParenthesesLevel = getInsideParenthesesLevel(node);
            if (insideParenthesesLevel != null) {
                Stack<Space> openingParens = new Stack<>();
                for (int i = 0; i < insideParenthesesLevel; i++) {
                    openingParens.push(sourceBefore("("));
                }
                Expression parenthesized = parenthesizedTree.apply(whitespace());
                for (int i = 0; i < insideParenthesesLevel; i++) {
                    parenthesized = new J.Parentheses<>(randomId(), openingParens.pop(), Markers.EMPTY,
                            padRight(parenthesized, sourceBefore(")")));
                }
                return parenthesized;
            }
            return parenthesizedTree.apply(whitespace());
        }

        private Statement labeled(org.codehaus.groovy.ast.stmt.Statement statement, Supplier<Statement> labeledTree) {
            List<J.Label> labels = null;
            if (statement.getStatementLabels() != null && !statement.getStatementLabels().isEmpty()) {
                labels = new ArrayList<>(statement.getStatementLabels().size());
                // Labels appear in statement.getStatementLabels() in reverse order of their appearance in source code
                // Could iterate over those in reverse order, but feels safer to just take the count and go off source code alone
                for (int i = 0; i < statement.getStatementLabels().size(); i++) {
                    labels.add(new J.Label(randomId(), whitespace(), Markers.EMPTY, JRightPadded.build(
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), name(), null, null)).withAfter(sourceBefore(":")),
                            new J.Empty(randomId(), EMPTY, Markers.EMPTY)));
                }
            }
            Statement s = labeledTree.get();
            if (labels != null) {
                //noinspection ConstantConditions
                return condenseLabels(labels, s);
            }
            return s;
        }

        @Override
        public void visitArrayExpression(ArrayExpression expression) {
            Space prefix = whitespace();
            skip("new");
            TypeTree typeTree = visitTypeTree(expression.getElementType());
            List<J.ArrayDimension> dimensions = buildNewArrayDimensions(expression);
            JContainer<Expression> initializer = buildNewArrayInitializer(expression);
            queue.add(new J.NewArray(randomId(), prefix, Markers.EMPTY, typeTree, dimensions, initializer, typeMapping.type(expression.getElementType())));
        }

        private JContainer<Expression> buildNewArrayInitializer(ArrayExpression expression) {
            if (expression.getSizeExpression() != null) {
                return null;
            }

            Space fmt = sourceBefore("{");

            List<JRightPadded<Expression>> expressions;
            if (expression.getExpressions().isEmpty()) {
                expressions = singletonList(padRight(new J.Empty(randomId(), sourceBefore("}"), Markers.EMPTY), EMPTY));
            } else {
                expressions = convertAll(expression.getExpressions(), n -> sourceBefore(","), n -> whitespace(), n -> {
                    if (n == expression.getExpression(expression.getExpressions().size() - 1) && source.charAt(cursor) == ',') {
                        cursor++;
                        return Markers.build(singleton(new TrailingComma(randomId(), whitespace())));
                    }
                    return Markers.EMPTY;
                });
                skip("}");
            }

            return JContainer.build(fmt, expressions, Markers.EMPTY);
        }

        private List<J.ArrayDimension> buildNewArrayDimensions(ArrayExpression expression) {
            List<J.ArrayDimension> dimensions = new ArrayList<>();
            for (int i = 0; expression.getSizeExpression() != null && i < expression.getSizeExpression().size(); i++) {
                dimensions.add(new J.ArrayDimension(randomId(), sourceBefore("["), Markers.EMPTY, padRight(doVisit(expression.getSizeExpression().get(i)), sourceBefore("]"))));
            }
            while (true) {
                int beginBracket = indexOfNextNonWhitespace(cursor, source);
                if (beginBracket >= source.length() || source.charAt(beginBracket) != '[') {
                    break;
                }

                int endBracket = indexOfNextNonWhitespace(beginBracket + 1, source);
                dimensions.add(new J.ArrayDimension(randomId(), format(source, cursor, beginBracket), Markers.EMPTY, padRight(new J.Empty(randomId(), format(source, beginBracket + 1, endBracket), Markers.EMPTY), EMPTY)));
                cursor = endBracket + 1;
            }
            return dimensions;
        }

        @Override
        public void visitArgumentlistExpression(ArgumentListExpression expression) {
            List<JRightPadded<Expression>> args = new ArrayList<>(expression.getExpressions().size());

            int saveCursor = cursor;
            Space beforeOpenParen = whitespace();

            boolean hasParentheses = true;
            if (source.charAt(cursor) == '(') {
                skip("(");
            } else {
                hasParentheses = false;
                beforeOpenParen = EMPTY;
                cursor = saveCursor;
            }

            List<org.codehaus.groovy.ast.expr.Expression> unparsedArgs = expression.getExpressions().stream()
                    .filter(GroovyParserVisitor.this::appearsInSource)
                    .collect(toList());
            // If the first parameter to a function is a Map, then groovy allows "named parameters" style invocations, see:
            //     https://docs.groovy-lang.org/latest/html/documentation/#_named_parameters_2
            // When named parameters are in use they may appear before, after, or intermixed with any positional arguments
            if (unparsedArgs.size() > 1 && unparsedArgs.get(0) instanceof MapExpression &&
                    (unparsedArgs.get(0).getLastLineNumber() > unparsedArgs.get(1).getLastLineNumber() ||
                            (unparsedArgs.get(0).getLastLineNumber() == unparsedArgs.get(1).getLastLineNumber() &&
                                    unparsedArgs.get(0).getLastColumnNumber() > unparsedArgs.get(1).getLastColumnNumber()))) {

                // Figure out the source-code ordering of the expressions
                MapExpression namedArgExpressions = (MapExpression) unparsedArgs.get(0);
                unparsedArgs =
                        ListUtils.concatAll(unparsedArgs.subList(1, unparsedArgs.size()), namedArgExpressions.getMapEntryExpressions()).stream()
                                .sorted(Comparator.comparing(ASTNode::getLastLineNumber).thenComparing(ASTNode::getLastColumnNumber))
                                .collect(toList());
            } else if (!unparsedArgs.isEmpty() && unparsedArgs.get(0) instanceof MapExpression) {
                // The map literal may or may not be wrapped in "[]"
                // If it is wrapped in "[]" then this isn't a named arguments situation, and we should not lift the parameters out of the enclosing MapExpression
                saveCursor = cursor;
                whitespace();
                if ('[' != source.charAt(cursor)) {
                    // Bring named parameters out of their containing MapExpression so that they can be parsed correctly
                    MapExpression namedArgExpressions = (MapExpression) unparsedArgs.get(0);
                    unparsedArgs =
                            Stream.concat(
                                            namedArgExpressions.getMapEntryExpressions().stream(),
                                            unparsedArgs.subList(1, unparsedArgs.size()).stream())
                                    .collect(toList());
                }
                cursor = saveCursor;
            }

            if (unparsedArgs.isEmpty()) {
                args.add(JRightPadded.build((Expression) new J.Empty(randomId(), whitespace(), Markers.EMPTY))
                        .withAfter(hasParentheses ? sourceBefore(")") : EMPTY));
            } else {
                boolean lastArgumentsAreAllClosures = endsWithClosures(expression.getExpressions());
                for (int i = 0; i < unparsedArgs.size(); i++) {
                    org.codehaus.groovy.ast.expr.Expression rawArg = unparsedArgs.get(i);
                    Expression exp = doVisit(rawArg);
                    if (!hasParentheses) {
                        exp = exp.withMarkers(exp.getMarkers().add(new OmitParentheses(randomId())));
                    }

                    Space after = EMPTY;
                    Space trailingCommaSuffix = null;
                    if (i == unparsedArgs.size() - 1) {
                        if (hasParentheses) {
                            saveCursor = cursor;
                            after = whitespace();
                            if (source.charAt(cursor) == ',') {
                                skip(",");
                                trailingCommaSuffix = sourceBefore(")");
                            } else {
                                cursor = saveCursor;
                                after = sourceBefore(")");
                            }
                        }
                    } else if (!(exp instanceof J.Lambda && lastArgumentsAreAllClosures && !hasParentheses)) {
                        after = whitespace();
                        saveCursor = cursor;
                        if (source.charAt(cursor) == ',') {
                            // we might have a trailing comma
                            skip(",");
                            trailingCommaSuffix = whitespace();
                        }
                        if (source.charAt(cursor) == ')') {
                            // next argument(s), if they exists, are trailing closures and will have an OmitParentheses marker
                            hasParentheses = false;
                        } else if (trailingCommaSuffix != null) {
                            // we don't have a trailing comma, just a regular comma
                            trailingCommaSuffix = null;
                            cursor = saveCursor;
                        }
                        cursor++;
                    }

                  args.add(newRightPadded(exp, after, trailingCommaSuffix));
                }
            }

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
        }

        public boolean endsWithClosures(List<org.codehaus.groovy.ast.expr.Expression> list) {
            if (!(list.get(list.size() - 1) instanceof ClosureExpression)) {
                return false;
            }

            boolean foundNonClosure = false;
            for (int i = list.size() - 2; i >= 0; i--) {
                if (list.get(i) instanceof ClosureExpression) {
                    if (foundNonClosure) {
                        return false;
                    }
                } else {
                    foundNonClosure = true;
                }
            }

            return true;
        }

        @Override
        public void visitClassExpression(ClassExpression clazz) {
            ClassNode type = clazz.getType();
            if (type.isArray()) {
                queue.add(arrayType(type));
                return;
            }
            Space prefix = whitespace();
            String name = type.getNameWithoutPackage().replace('$', '.');
            if (!source.startsWith(name, cursor)) {
                name = type.getUnresolvedName().replace('$', '.');
            }
            skip(name);
            if (type.isUsingGenerics()) {
                GenericsType[] generics = type.getGenericsTypes();
                if (generics != null && generics.length > 0) {
                    J.Identifier ident = new J.Identifier(randomId(),
                            EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            name,
                            typeMapping.type(type), null);
                    J.ParameterizedType parameterizedType = new J.ParameterizedType(randomId(), prefix, Markers.EMPTY, ident, visitTypeParameterizations(generics), typeMapping.type(type));
                    if (sourceStartsWith(".class")) {
                        Space beforeDot = sourceBefore(".");
                        Space classPrefix = whitespace();
                        skip("class");
                        J.Identifier classIdent = new J.Identifier(randomId(), classPrefix, Markers.EMPTY, emptyList(), "class", null, null);
                        queue.add(new J.FieldAccess(randomId(), EMPTY, Markers.EMPTY, parameterizedType, padLeft(beforeDot, classIdent), typeMapping.type(type)));
                        return;
                    }
                    queue.add(parameterizedType);
                    return;
                }
            }
            if (sourceStartsWith(".class")) {
                int afterClass = indexOfNextNonWhitespace(cursor, source) + ".class".length();
                if (afterClass >= source.length() || !Character.isJavaIdentifierPart(source.charAt(afterClass))) {
                    String classSuffix = source.substring(cursor, indexOfNextNonWhitespace(cursor, source)) + ".class";
                    name += classSuffix;
                    skip(classSuffix);
                }
            }
            queue.add(TypeTree.build(name)
                    .withType(typeMapping.type(type))
                    .withPrefix(prefix));
        }

        @Override
        public void visitAssertStatement(AssertStatement statement) {
            queue.add(labeled(statement, () -> {
                Space prefix = whitespace();
                skip("assert");
                Expression condition = doVisit(statement.getBooleanExpression());
                JLeftPadded<Expression> message = null;
                if (!(statement.getMessageExpression() instanceof ConstantExpression) || !((ConstantExpression) statement.getMessageExpression()).isNullExpression()) {
                    Space messagePrefix = whitespace();
                    skip(":");
                    message = padLeft(messagePrefix, doVisit(statement.getMessageExpression()));
                }
                return new J.Assert(randomId(), prefix, Markers.EMPTY, condition, message);
            }));
        }

        @Override
        public void visitBinaryExpression(BinaryExpression binary) {
            queue.add(insideParentheses(binary, fmt -> {
                Expression left = doVisit(binary.getLeftExpression());
                Space opPrefix = whitespace();
                boolean assignment = false;
                boolean instanceOf = false;
                J.AssignmentOperation.Type assignOp = null;
                J.Binary.Type binaryOp = null;
                G.Binary.Type gBinaryOp = null;

                switch (binary.getOperation().getText()) {
                    case "+":
                        binaryOp = J.Binary.Type.Addition;
                        break;
                    case "&&":
                        binaryOp = J.Binary.Type.And;
                        break;
                    case "&":
                        binaryOp = J.Binary.Type.BitAnd;
                        break;
                    case "|":
                        binaryOp = J.Binary.Type.BitOr;
                        break;
                    case "^":
                        binaryOp = J.Binary.Type.BitXor;
                        break;
                    case "/":
                        binaryOp = J.Binary.Type.Division;
                        break;
                    case "==":
                        binaryOp = J.Binary.Type.Equal;
                        break;
                    case ">":
                        binaryOp = J.Binary.Type.GreaterThan;
                        break;
                    case ">=":
                        binaryOp = J.Binary.Type.GreaterThanOrEqual;
                        break;
                    case "<<":
                        binaryOp = J.Binary.Type.LeftShift;
                        break;
                    case "<":
                        binaryOp = J.Binary.Type.LessThan;
                        break;
                    case "<=":
                        binaryOp = J.Binary.Type.LessThanOrEqual;
                        break;
                    case "%":
                        binaryOp = J.Binary.Type.Modulo;
                        break;
                    case "*":
                        binaryOp = J.Binary.Type.Multiplication;
                        break;
                    case "!=":
                        binaryOp = J.Binary.Type.NotEqual;
                        break;
                    case "||":
                        binaryOp = J.Binary.Type.Or;
                        break;
                    case ">>":
                        binaryOp = J.Binary.Type.RightShift;
                        break;
                    case "-":
                        binaryOp = J.Binary.Type.Subtraction;
                        break;
                    case ">>>":
                        binaryOp = J.Binary.Type.UnsignedRightShift;
                        break;
                    case "instanceof":
                        instanceOf = true;
                        break;
                    case "=":
                        assignment = true;
                        break;
                    case "+=":
                        assignOp = J.AssignmentOperation.Type.Addition;
                        break;
                    case "-=":
                        assignOp = J.AssignmentOperation.Type.Subtraction;
                        break;
                    case "&=":
                        assignOp = J.AssignmentOperation.Type.BitAnd;
                        break;
                    case "|=":
                        assignOp = J.AssignmentOperation.Type.BitOr;
                        break;
                    case "^=":
                        assignOp = J.AssignmentOperation.Type.BitXor;
                        break;
                    case "/=":
                        assignOp = J.AssignmentOperation.Type.Division;
                        break;
                    case "<<=":
                        assignOp = J.AssignmentOperation.Type.LeftShift;
                        break;
                    case "%=":
                        assignOp = J.AssignmentOperation.Type.Modulo;
                        break;
                    case "*=":
                        assignOp = J.AssignmentOperation.Type.Multiplication;
                        break;
                    case ">>=":
                        assignOp = J.AssignmentOperation.Type.RightShift;
                        break;
                    case ">>>=":
                        assignOp = J.AssignmentOperation.Type.UnsignedRightShift;
                        break;
                    case "=~":
                        gBinaryOp = G.Binary.Type.Find;
                        break;
                    case "==~":
                        gBinaryOp = G.Binary.Type.Match;
                        break;
                    case "[":
                        gBinaryOp = G.Binary.Type.Access;
                        break;
                    case "in":
                        gBinaryOp = G.Binary.Type.In;
                        break;
                    case "!in":
                        gBinaryOp = G.Binary.Type.NotIn;
                        break;
                    case "<=>":
                        gBinaryOp = G.Binary.Type.Spaceship;
                        break;
                    case "?=":
                        gBinaryOp = G.Binary.Type.ElvisAssignment;
                        break;
                    case "**":
                        gBinaryOp = G.Binary.Type.Power;
                        break;
                    case "**=":
                        gBinaryOp = G.Binary.Type.PowerAssignment;
                        break;
                    case "===":
                        gBinaryOp = G.Binary.Type.IdentityEquals;
                        break;
                    case "!==":
                        gBinaryOp = G.Binary.Type.IdentityNotEquals;
                        break;
                }

                cursor += binary.getOperation().getText().length();
                Expression right = doVisit(binary.getRightExpression());

                if (assignment) {
                    return new J.Assignment(randomId(), fmt, Markers.EMPTY,
                            left, JLeftPadded.build(right).withBefore(opPrefix),
                            typeMapping.type(binary.getType()));
                } else if (instanceOf) {
                    return new J.InstanceOf(randomId(), fmt, Markers.EMPTY,
                            JRightPadded.build(left).withAfter(opPrefix), right, null,
                            typeMapping.type(binary.getType()));
                } else if (assignOp != null) {
                    return new J.AssignmentOperation(randomId(), fmt, Markers.EMPTY,
                            left, JLeftPadded.build(assignOp).withBefore(opPrefix),
                            right, typeMapping.type(binary.getType()));
                } else if (binaryOp != null) {
                    return new J.Binary(randomId(), fmt, Markers.EMPTY,
                            left, JLeftPadded.build(binaryOp).withBefore(opPrefix),
                            right, typeMapping.type(binary.getType()));
                } else if (gBinaryOp != null) {
                    Space after = EMPTY;
                    if (gBinaryOp == G.Binary.Type.Access) {
                        after = sourceBefore("]");
                    }
                    return new G.Binary(randomId(), fmt, Markers.EMPTY,
                            left, JLeftPadded.build(gBinaryOp).withBefore(opPrefix),
                            right, after, typeMapping.type(binary.getType()));
                }
                throw new IllegalStateException("Unknown binary expression " + binary.getClass().getSimpleName());
            }));
        }

        @Override
        public void visitBlockStatement(BlockStatement block) {
            Space fmt = EMPTY;
            Space staticInitPadding = EMPTY;
            boolean isStaticInit = sourceStartsWith("static");
            Object parent = nodeCursor.getParentOrThrow().getValue();
            boolean withinClosure = !(parent instanceof LambdaExpression) && parent instanceof ClosureExpression ||
                    (parent instanceof ExpressionStatement &&
                            !(((ExpressionStatement) parent).getExpression() instanceof LambdaExpression) &&
                            ((ExpressionStatement) parent).getExpression() instanceof ClosureExpression
                    );
            if (isStaticInit) {
                fmt = sourceBefore("static");
                staticInitPadding = whitespace();
                skip("{");
            } else if (!withinClosure) {
                fmt = sourceBefore("{");
            }
            List<JRightPadded<Statement>> statements = new ArrayList<>(block.getStatements().size());
            List<org.codehaus.groovy.ast.stmt.Statement> blockStatements = block.getStatements();
            for (int i = 0; i < blockStatements.size(); i++) {
                ASTNode statement = blockStatements.get(i);
                J expr = doVisit(statement);
                if (i == blockStatements.size() - 1 && (expr instanceof Expression)) {
                    if (parent instanceof ClosureExpression || (parent instanceof MethodNode &&
                            JavaType.Primitive.Void != typeMapping.type(((MethodNode) parent).getReturnType()))) {
                        expr = new J.Return(randomId(), expr.getPrefix(), Markers.EMPTY,
                                expr.withPrefix(EMPTY));
                        expr = expr.withMarkers(expr.getMarkers().add(new ImplicitReturn(randomId())));
                    }
                }

                JRightPadded<Statement> stat = JRightPadded.build((Statement) expr);
                int saveCursor = cursor;
                Space beforeSemicolon = whitespace();
                if (source.charAt(cursor) == ';') {
                    stat = stat
                            .withMarkers(stat.getMarkers().add(new Semicolon(randomId())))
                            .withAfter(beforeSemicolon);
                    skip(";");
                } else {
                    cursor = saveCursor;
                }

                statements.add(stat);
            }
            queue.add(new J.Block(randomId(), fmt, Markers.EMPTY, new JRightPadded<>(isStaticInit, staticInitPadding, Markers.EMPTY), statements, whitespace()));
            if (!withinClosure) {
                skip("}");
            }
        }

        @Override
        public void visitCatchStatement(CatchStatement node) {
            Space prefix = sourceBefore("catch");
            Space parenPrefix = sourceBefore("(");
            List<J.Modifier> modifiers = getModifiers();

            Parameter param = node.getVariable();
            TypeTree paramType;
            Space paramPrefix = whitespace();
            // Groovy allows catch variables to omit their type, shorthand for being of type java.lang.Exception
            // Can't use isSynthetic() here because groovy doesn't record the line number on the Parameter
            if (Exception.class.getName().equals(param.getType().getName()) &&
                    !source.startsWith("Exception", cursor) &&
                    !source.startsWith("java.lang.Exception", cursor)) {
                paramType = new J.Identifier(randomId(), paramPrefix, Markers.EMPTY, emptyList(), "",
                        JavaType.ShallowClass.build(Exception.class.getName()), null);
            } else {
                paramType = visitTypeTree(param.getOriginType()).withPrefix(paramPrefix);
            }

            JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                    new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), param.getName(), null, null),
                            emptyList(), null, null)
            );
            cursor += param.getName().length();
            Space rightPad = whitespace();
            skip(")");

            Space varDeclPrefix = paramType.getPrefix();
            TypeTree varDeclTypeExpression = paramType.withPrefix(EMPTY);
            if (!modifiers.isEmpty()) {
                varDeclPrefix = modifiers.get(0).getPrefix();
                varDeclTypeExpression = paramType;
                modifiers = ListUtils.mapFirst(modifiers, it -> it.withPrefix(EMPTY));
            }

            JRightPadded<J.VariableDeclarations> variable = JRightPadded.build(new J.VariableDeclarations(randomId(), varDeclPrefix,
                    Markers.EMPTY, emptyList(), modifiers, varDeclTypeExpression,
                    null,
                    singletonList(paramName))
            ).withAfter(rightPad);
            J.ControlParentheses<J.VariableDeclarations> catchControl = new J.ControlParentheses<>(randomId(), parenPrefix, Markers.EMPTY, variable);
            queue.add(new J.Try.Catch(randomId(), prefix, Markers.EMPTY, catchControl, doVisit(node.getCode())));
        }

        private void visitMultiCatchStatement(List<CatchStatement> catchStatements) {
            Space prefix = sourceBefore("catch");
            Space parenPrefix = sourceBefore("(");
            List<J.Modifier> modifiers = getModifiers();

            // Build J.MultiCatch from the exception types of each catch statement
            List<JRightPadded<NameTree>> alternatives = new ArrayList<>(catchStatements.size());
            for (int i = 0; i < catchStatements.size(); i++) {
                Space typePrefix = whitespace();
                TypeTree alt = visitTypeTree(catchStatements.get(i).getVariable().getOriginType()).withPrefix(typePrefix);
                alternatives.add(i < catchStatements.size() - 1 ?
                        padRight(alt, sourceBefore("|")) :
                        JRightPadded.build(alt));
            }
            TypeTree paramType = new J.MultiCatch(randomId(), EMPTY, Markers.EMPTY, alternatives);

            // All catch statements in a multi-catch share the same variable name
            Parameter param = catchStatements.get(0).getVariable();
            JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                    new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), param.getName(), null, null),
                            emptyList(), null, null)
            );
            cursor += param.getName().length();
            Space rightPad = whitespace();
            skip(")");

            Space varDeclPrefix = alternatives.get(0).getElement().getPrefix();
            TypeTree varDeclTypeExpression = paramType;
            // Strip prefix from first alternative since it moves to varDecl
            alternatives.set(0, alternatives.get(0).withElement(alternatives.get(0).getElement().withPrefix(EMPTY)));
            if (!modifiers.isEmpty()) {
                varDeclPrefix = modifiers.get(0).getPrefix();
                modifiers = ListUtils.mapFirst(modifiers, it -> it.withPrefix(EMPTY));
            }

            JRightPadded<J.VariableDeclarations> variable = JRightPadded.build(new J.VariableDeclarations(randomId(), varDeclPrefix,
                    Markers.EMPTY, emptyList(), modifiers, varDeclTypeExpression,
                    null,
                    singletonList(paramName))
            ).withAfter(rightPad);
            J.ControlParentheses<J.VariableDeclarations> catchControl = new J.ControlParentheses<>(randomId(), parenPrefix, Markers.EMPTY, variable);
            // Use the code block from the first catch statement (they all share the same block)
            queue.add(new J.Try.Catch(randomId(), prefix, Markers.EMPTY, catchControl, doVisit(catchStatements.get(0).getCode())));
        }

        @Override
        public void visitBreakStatement(BreakStatement statement) {
            queue.add(new J.Break(randomId(),
                    sourceBefore("break"),
                    Markers.EMPTY,
                    (statement.getLabel() == null) ?
                            null :
                            new J.Identifier(randomId(),
                                    sourceBefore(statement.getLabel()),
                                    Markers.EMPTY, emptyList(), statement.getLabel(), null, null))
            );
        }

        @Override
        public void visitCaseStatement(CaseStatement statement) {
            queue.add(new J.Case(randomId(),
                    sourceBefore("case"),
                    Markers.EMPTY,
                    J.Case.Type.Statement,
                    null,
                    JContainer.build(singletonList(JRightPadded.build(doVisit(statement.getExpression())))),
                    null,
                    null,
                    statement.getCode() instanceof EmptyStatement ?
                            JContainer.build(sourceBefore(":"), convertStatements(emptyList()), Markers.EMPTY) :
                            JContainer.build(sourceBefore(":"), convertStatements(((BlockStatement) statement.getCode()).getStatements()), Markers.EMPTY),
                    null)
            );
        }

        private J.Case visitDefaultCaseStatement(BlockStatement statement) {
            return new J.Case(randomId(),
                    sourceBefore("default"),
                    Markers.EMPTY,
                    J.Case.Type.Statement,
                    null,
                    JContainer.build(singletonList(JRightPadded.build(new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), skip("default"), null, null)))),
                    null,
                    null,
                    JContainer.build(sourceBefore(":"), convertStatements(statement.getStatements()), Markers.EMPTY),
                    null
            );
        }

        @Override
        public void visitCastExpression(CastExpression cast) {
            queue.add(insideParentheses(cast, prefix -> {
                // Java-style cast "(type)object" vs groovy-style cast "object as type".
                // Can't detect by looking at cursor character because the expression
                // itself may start with '(' (e.g. "(foo as Bar).name as Set").
                if (!cast.isCoerce()) {
                    skip("(");
                    return new J.TypeCast(randomId(), prefix, Markers.EMPTY,
                            new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY,
                                    new JRightPadded<>(visitTypeTree(cast.getType()), sourceBefore(")"), Markers.EMPTY)
                            ),
                            doVisit(cast.getExpression()));
                } else {
                    Expression expr = doVisit(cast.getExpression());
                    Space asPrefix = sourceBefore("as");

                    return new J.TypeCast(randomId(), prefix, new Markers(randomId(), singletonList(new AsStyleTypeCast(randomId()))),
                            new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY,
                                    new JRightPadded<>(visitTypeTree(cast.getType()), asPrefix, Markers.EMPTY)),
                            expr);
                }
            }));
        }

        @Override
        public void visitClosureExpression(ClosureExpression expression) {
            Space prefix = whitespace();
            LambdaStyle ls = new LambdaStyle(randomId(), expression instanceof LambdaExpression, true);
            boolean parenthesized = false;
            if (source.charAt(cursor) == '(') {
                parenthesized = true;
                skip("(");
            } else if (source.charAt(cursor) == '{') {
                skip("{");
            }
            JavaType closureType = typeMapping.type(staticType(expression));
            List<JRightPadded<J>> paramExprs;
            if (expression.getParameters() != null && expression.getParameters().length > 0) {
                paramExprs = new ArrayList<>(expression.getParameters().length);
                Parameter[] parameters = expression.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    JavaType type = typeMapping.type(staticType(p));
                    Space varDeclPrefix = whitespace();
                    TypeTree paramType = p.isDynamicTyped() ? null : visitTypeTree(p.getType());
                    JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                            new J.VariableDeclarations.NamedVariable(randomId(), sourceBefore(p.getName()), Markers.EMPTY,
                                    new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), p.getName(), type, null),
                                    emptyList(), null,
                                    typeMapping.variableType(p.getName(), staticType(p)))
                    );
                    org.codehaus.groovy.ast.expr.Expression defaultValue = p.getInitialExpression();
                    if (defaultValue != null) {
                        paramName = paramName.withElement(paramName.getElement().getPadding()
                                .withInitializer(new JLeftPadded<>(
                                        sourceBefore("="),
                                        doVisit(defaultValue),
                                        Markers.EMPTY)));
                    }
                    J expr = new J.VariableDeclarations(randomId(), varDeclPrefix, Markers.EMPTY,
                            emptyList(), emptyList(), paramType,
                            null,
                            singletonList(paramName));
                    JRightPadded<J> param = JRightPadded.build(expr);
                    if (i != parameters.length - 1) {
                        param = param.withAfter(sourceBefore(","));
                    } else {
                        param = param.withAfter(whitespace());
                    }
                    paramExprs.add(param);
                }
            } else {
                Space argPrefix = EMPTY;
                if (parenthesized) {
                    argPrefix = whitespace();
                }
                paramExprs = singletonList(JRightPadded.build(new J.Empty(randomId(), argPrefix, Markers.EMPTY)));
            }
            if (parenthesized) {
                cursor += 1;
            }
            J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, parenthesized, paramExprs);
            int saveCursor = cursor;
            Space arrowPrefix = whitespace();
            if (source.startsWith("->", cursor)) {
                skip("->");
            } else {
                ls = ls.withArrow(false);
                cursor = saveCursor;
                arrowPrefix = EMPTY;
            }
            J body = doVisit(expression.getCode());
            queue.add(new J.Lambda(randomId(), prefix, Markers.build(singleton(ls)), params,
                    arrowPrefix,
                    body,
                    closureType));
            if (cursor < source.length() && source.charAt(cursor) == '}') {
                skip("}");
            }
        }

        @Override
        public void visitClosureListExpression(ClosureListExpression closureListExpression) {
            List<org.codehaus.groovy.ast.expr.Expression> expressions = closureListExpression.getExpressions();
            List<JRightPadded<Object>> results = new ArrayList<>(closureListExpression.getExpressions().size());
            for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
                results.add(JRightPadded.build(doVisit(expressions.get(i))).withAfter(whitespace()));
                if (i < expressionsSize - 1) {
                    cursor++; // "," or ";" (a for-loop uses a ClosureListExpression)
                }
            }
            queue.add(results);
        }

        @Override
        public void visitConstantExpression(ConstantExpression expression) {
            // The groovy compiler can add or remove annotations for AST transformations.
            // Because @groovy.transform.Field is transformed to a ConstantExpression, we need to restore the original DeclarationExpression
            if (sourceStartsWith("@" + Field.class.getSimpleName()) || sourceStartsWith("@" + Field.class.getCanonicalName())) {
                visitDeclarationExpression(transformBackToDeclarationExpression(expression));
                return;
            }

            queue.add(insideParentheses(expression, fmt -> {
                JavaType.Primitive jType;
                // The unaryPlus is not included in the expression and must be handled through the source.
                String text = expression.getText();
                Object value = expression.getValue();
                ClassNode type = expression.getType();
                if (type == ClassHelper.BigDecimal_TYPE) {
                    // TODO: Proper support for BigDecimal literals
                    jType = JavaType.Primitive.Double;
                    value = ((BigDecimal) value).doubleValue();
                } else if (type == ClassHelper.boolean_TYPE) {
                    jType = JavaType.Primitive.Boolean;
                } else if (type == ClassHelper.byte_TYPE) {
                    jType = JavaType.Primitive.Byte;
                } else if (type == ClassHelper.char_TYPE) {
                    jType = JavaType.Primitive.Char;
                } else if (type == ClassHelper.double_TYPE || Double.class.getName().equals(type.getName())) {
                    jType = JavaType.Primitive.Double;
                } else if (type == ClassHelper.float_TYPE || Float.class.getName().equals(type.getName())) {
                    jType = JavaType.Primitive.Float;
                } else if (type == ClassHelper.int_TYPE || Integer.class.getName().equals(type.getName())) {
                    jType = JavaType.Primitive.Int;
                } else if (type == ClassHelper.long_TYPE || Long.class.getName().equals(type.getName())) {
                    jType = JavaType.Primitive.Long;
                } else if (type == ClassHelper.short_TYPE || Short.class.getName().equals(type.getName())) {
                    jType = JavaType.Primitive.Short;
                } else if (type == ClassHelper.STRING_TYPE) {
                    jType = JavaType.Primitive.String;
                    // an attribute selector is modeled as a String ConstantExpression
                    if (source.startsWith("@" + value, cursor)) {
                        value = "@" + value;
                        text = "@" + text;
                    } else {
                        Delimiter delimiter = getDelimiter(expression, cursor);
                        if (delimiter != null) {
                            // Get the string literal from the source, so escaping of newlines and the like works out of the box
                            value = sourceSubstring(cursor + delimiter.open.length(), delimiter.close);
                            text = delimiter.open + value + delimiter.close;
                        }
                    }
                } else if (expression.isNullExpression()) {
                    if (source.startsWith("null", cursor)) {
                        text = "null";
                    } else {
                        text = "";
                    }
                    jType = JavaType.Primitive.Null;
                } else {
                    throw new IllegalStateException("Unexpected constant type: " + type);
                }

                // Get the string literal from the source, as numeric literals may have a unary operator, underscores, dots and can be followed by "L", "f", or "d"
                if (jType == JavaType.Primitive.Int || jType == JavaType.Primitive.Long || jType == JavaType.Primitive.Float || jType == JavaType.Primitive.Double) {
                    int i = cursor;
                    if (source.charAt(cursor) == '-' || source.charAt(cursor) == '+') {
                        i = indexOfNextNonWhitespace(cursor + 1, source);
                    }
                    for (; i < source.length(); i++) {
                        char c = source.charAt(i);
                        if (!(isJavaIdentifierPart(c) || (c == '.' && source.length() > (i + 1) && Character.isDigit(source.charAt(i + 1))))) {
                            break;
                        }
                    }
                    text = source.substring(cursor, i);
                }

                skip(text);
                return new J.Literal(randomId(), fmt, Markers.EMPTY, value, text, null, jType);
            }));
        }

        @Override
        public void visitConstructorCallExpression(ConstructorCallExpression ctor) {
            queue.add(insideParentheses(ctor, fmt -> {
                if (ctor.getType() == ClassNode.SUPER || ctor.getType() == ClassNode.THIS) {
                    MethodNode methodNode = (MethodNode) ctor.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                    return new J.MethodInvocation(
                            randomId(),
                            fmt,
                            Markers.EMPTY,
                            null,
                            null,
                            new J.Identifier(randomId(),
                                    EMPTY,
                                    Markers.EMPTY,
                                    emptyList(),
                                    skip(ctor.getType() == ClassNode.SUPER ? "super" : "this"),
                                    null,
                                    null
                            ),
                            doVisit(ctor.getArguments()),
                            typeMapping.methodType(methodNode)
                    );
                }

                skip("new");
                TypeTree clazz = visitTypeTree(ctor.getType(), isInferred(ctor));
                JContainer<Expression> args = doVisit(ctor.getArguments());
                J.Block body = null;
                if (ctor.isUsingAnonymousInnerClass() && ctor.getType() instanceof InnerClassNode) {
                    body = classVisitor.visitClassBlock(ctor.getType());
                }
                MethodNode methodNode = (MethodNode) ctor.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                return new J.NewClass(randomId(), fmt, Markers.EMPTY, null, EMPTY,
                        clazz, args, body, typeMapping.methodType(methodNode));
            }));
        }

        @Override
        public void visitContinueStatement(ContinueStatement statement) {
            queue.add(new J.Continue(randomId(),
                    sourceBefore("continue"),
                    Markers.EMPTY,
                    (statement.getLabel() == null) ?
                            null :
                            new J.Identifier(randomId(),
                                    sourceBefore(statement.getLabel()),
                                    Markers.EMPTY, emptyList(), statement.getLabel(), null, null))
            );
        }

        @Override
        public void visitNotExpression(NotExpression expression) {
            queue.add(insideParentheses(expression, fmt -> {
                skip("!");
                JLeftPadded<J.Unary.Type> op = padLeft(EMPTY, J.Unary.Type.Not);
                // Groovy 3+ does not set _INSIDE_PARENTHESES_LEVEL on PropertyExpression / MethodCallExpression,
                // so when `!` directly wraps such an expression in parentheses, detect the parentheses from the source.
                org.codehaus.groovy.ast.expr.Expression operand = expression.getExpression();
                int savedCursor = cursor;
                Space beforeParen = whitespace();
                Expression expr;
                if (cursor < source.length() && source.charAt(cursor) == '(' && getInsideParenthesesLevel(operand) == null) {
                    skip("(");
                    Expression inner = doVisit(operand);
                    expr = new J.Parentheses<>(randomId(), beforeParen, Markers.EMPTY,
                            JRightPadded.build((J) inner).withAfter(sourceBefore(")")));
                } else {
                    cursor = savedCursor;
                    expr = doVisit(operand);
                }
                return new J.Unary(randomId(), fmt, Markers.EMPTY, op, expr, typeMapping.type(expression.getType()));
            }));
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            Space prefix = whitespace();
            List<J.Annotation> leadingAnnotations = visitAndGetAnnotations(expression, classVisitor);
            Optional<MultiVariable> multiVariable = maybeMultiVariable();
            List<J.Modifier> modifiers = getModifiers();

            if (expression.isMultipleAssignmentDeclaration()) {
                // def (a, b, c) = expr
                TupleExpression tuple = expression.getTupleExpression();
                List<org.codehaus.groovy.ast.expr.Expression> tupleExpressions = tuple.getExpressions();

                VariableExpression firstVar = (VariableExpression) tupleExpressions.get(0);
                TypeTree typeExpr = visitVariableExpressionType(firstVar);

                Space beforeOpenParen = sourceBefore("(");

                List<JRightPadded<J.VariableDeclarations>> tupleVars = new ArrayList<>(tupleExpressions.size());
                for (int i = 0; i < tupleExpressions.size(); i++) {
                    VariableExpression varExpr = (VariableExpression) tupleExpressions.get(i);
                    TypeTree innerType = visitVariableExpressionType(varExpr);
                    J.Identifier name = doVisit(varExpr);
                    J.VariableDeclarations.NamedVariable nv = new J.VariableDeclarations.NamedVariable(
                            randomId(),
                            name.getPrefix(),
                            Markers.EMPTY,
                            name.withPrefix(EMPTY),
                            emptyList(),
                            null,
                            typeMapping.variableType(name.getSimpleName(), innerType.getType()));
                    J.VariableDeclarations innerDecl = new J.VariableDeclarations(
                            randomId(), EMPTY, Markers.EMPTY,
                            emptyList(), emptyList(),
                            innerType, null,
                            singletonList(JRightPadded.build(nv)));
                    Space after = i < tupleExpressions.size() - 1 ? sourceBefore(",") : sourceBefore(")");
                    tupleVars.add(JRightPadded.<J.VariableDeclarations>build(innerDecl).withAfter(after));
                }

                G.TupleExpression tupleDeclarator = new G.TupleExpression(
                        randomId(), EMPTY, Markers.EMPTY,
                        JContainer.build(beforeOpenParen, tupleVars, Markers.EMPTY),
                        null);

                J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                        randomId(), EMPTY, Markers.EMPTY,
                        tupleDeclarator, emptyList(), null, null);

                if (!(expression.getRightExpression() instanceof EmptyExpression)) {
                    Space beforeAssign = sourceBefore("=");
                    Expression initializer = doVisit(expression.getRightExpression());
                    namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
                }

                J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                        randomId(), prefix, Markers.EMPTY, leadingAnnotations, modifiers,
                        typeExpr, null, singletonList(JRightPadded.build(namedVariable)));
                queue.add(variableDeclarations);
                return;
            }

            TypeTree typeExpr = visitVariableExpressionType(expression.getVariableExpression());

            J.VariableDeclarations.NamedVariable namedVariable;
            if (expression.isMultipleAssignmentDeclaration()) {
                // def (a, b) = [1, 2]
                throw new UnsupportedOperationException("Parsing multiple assignment (e.g.: def (a, b) = [1, 2]) is not implemented");
            } else {
                J.Identifier name = doVisit(expression.getVariableExpression());
                namedVariable = new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        name.getPrefix(),
                        Markers.EMPTY,
                        name.withPrefix(EMPTY),
                        emptyList(),
                        null,
                        typeMapping.variableType(name.getSimpleName(), typeExpr.getType())
                );
            }

            if (!(expression.getRightExpression() instanceof EmptyExpression)) {
                Space beforeAssign = sourceBefore("=");
                Expression initializer = doVisit(expression.getRightExpression());
                namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    leadingAnnotations,
                    modifiers,
                    typeExpr,
                    null,
                    singletonList(JRightPadded.build(namedVariable))
            );
            if (multiVariable.isPresent()) {
                variableDeclarations = variableDeclarations.withMarkers(variableDeclarations.getMarkers().add(multiVariable.get()));
            }

            queue.add(variableDeclarations);
        }

        private Optional<MultiVariable> maybeMultiVariable() {
            int saveCursor = cursor;
            Space commaPrefix = whitespace();
            if (source.startsWith(",", cursor)) {
                skip(",");
                return Optional.of(new MultiVariable(randomId(), commaPrefix));
            }
            cursor = saveCursor;
            return Optional.empty();
        }

        @Override
        public void visitEmptyExpression(EmptyExpression expression) {
            queue.add(new J.Empty(randomId(), EMPTY, Markers.EMPTY));
        }

        @Override
        public void visitExpressionStatement(ExpressionStatement statement) {
            queue.add(labeled(statement, () -> {
                super.visitExpressionStatement(statement);
                Object e = queue.poll();
                if (e instanceof Statement) {
                    return (Statement) e;
                }
                return new G.ExpressionStatement(randomId(), (Expression) e);
            }));
        }

        Statement condenseLabels(List<J.Label> labels, Statement s) {
            if (labels.isEmpty()) {
                return s;
            }
            return labels.get(0).withStatement(condenseLabels(labels.subList(1, labels.size()), s));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void visitForLoop(ForStatement forLoop) {
            queue.add(labeled(forLoop, () -> {
                Space prefix = sourceBefore("for");
                Space controlFmt = sourceBefore("(");
                if (forLoop.getCollectionExpression() instanceof ClosureListExpression) {
                    List<JRightPadded<?>> controls = doVisit(forLoop.getCollectionExpression());
                    // There will always be exactly three elements in a for loop's ClosureListExpression
                    List<JRightPadded<Statement>> init = controls.get(0).getElement() instanceof List ?
                            (List<JRightPadded<Statement>>) controls.get(0).getElement() :
                            singletonList((JRightPadded<Statement>) controls.get(0));

                    JRightPadded<Expression> condition = (JRightPadded<Expression>) controls.get(1);

                    List<JRightPadded<Statement>> update = controls.get(2).getElement() instanceof List ?
                            (List<JRightPadded<Statement>>) controls.get(2).getElement() :
                            singletonList((JRightPadded<Statement>) controls.get(2));
                    skip(")");

                    return new J.ForLoop(randomId(), prefix, Markers.EMPTY,
                            new J.ForLoop.Control(randomId(), controlFmt,
                                    Markers.EMPTY, init, condition, update),
                            JRightPadded.build(doVisit(forLoop.getLoopBlock())));
                } else {
                    Parameter param = forLoop.getVariable();
                    Space paramFmt = whitespace();
                    List<J.Modifier> modifiers = getModifiers();
                    TypeTree paramType = param.getOriginType().getColumnNumber() >= 0 ? visitTypeTree(param.getOriginType()) : null;
                    JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                            new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                                    new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), param.getName(), null, null),
                                    emptyList(), null, null)
                    );
                    cursor += param.getName().length();
                    Space rightPad = whitespace();
                    Markers forEachMarkers = Markers.EMPTY;
                    if (source.charAt(cursor) == ':') {
                        skip(":");
                    } else {
                        skip("in");
                        forEachMarkers = forEachMarkers.add(new InStyleForEachLoop(randomId()));
                    }

                    JRightPadded<Statement> variable = JRightPadded.<Statement>build(
                            new J.VariableDeclarations(randomId(), paramFmt, Markers.EMPTY, emptyList(), modifiers, paramType, null, singletonList(paramName))
                    ).withAfter(rightPad);

                    JRightPadded<Expression> iterable = JRightPadded.build((Expression) doVisit(forLoop.getCollectionExpression()))
                            .withAfter(sourceBefore(")"));

                    return new J.ForEachLoop(randomId(), prefix, forEachMarkers,
                            new J.ForEachLoop.Control(randomId(), controlFmt, Markers.EMPTY, variable, iterable),
                            JRightPadded.build(doVisit(forLoop.getLoopBlock())));
                }
            }));
        }

        @Override
        public void visitIfElse(IfStatement ifElse) {
            Space fmt = sourceBefore("if");
            J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                    JRightPadded.build((Expression) doVisit(ifElse.getBooleanExpression().getExpression())).withAfter(sourceBefore(")")));
            JRightPadded<Statement> then = maybeSemicolon(doVisit(ifElse.getIfBlock()));
            J.If.Else else_ = ifElse.getElseBlock() instanceof EmptyStatement ? null :
                    new J.If.Else(randomId(), sourceBefore("else"), Markers.EMPTY,
                            maybeSemicolon(doVisit(ifElse.getElseBlock())));
            queue.add(new J.If(randomId(), fmt, Markers.EMPTY, ifCondition, then, else_));
        }

        @Override
        public void visitGStringExpression(GStringExpression gstring) {
            Space fmt = whitespace();
            Delimiter delimiter = getDelimiter(gstring, cursor);
            skip(delimiter.open);

            NavigableMap<LineColumn, org.codehaus.groovy.ast.expr.Expression> sortedByPosition = new TreeMap<>();
            for (ConstantExpression e : gstring.getStrings()) {
                // There will always be constant expressions before and after any values
                // No need to represent these empty strings
                if (!e.getText().isEmpty()) {
                    sortedByPosition.put(pos(e), e);
                }
            }
            for (org.codehaus.groovy.ast.expr.Expression e : gstring.getValues()) {
                sortedByPosition.put(pos(e), e);
            }
            List<org.codehaus.groovy.ast.expr.Expression> rawExprs = new ArrayList<>(sortedByPosition.values());
            boolean hasInterpolation = !gstring.getValues().isEmpty();
            List<J> strings = new ArrayList<>(rawExprs.size());
            for (int i = 0; i < rawExprs.size(); i++) {
                org.codehaus.groovy.ast.expr.Expression e = rawExprs.get(i);
                if (source.charAt(cursor) == '$') {
                    skip("$");
                    boolean inCurlies = source.charAt(cursor) == '{';
                    if (inCurlies) {
                        skip("{");
                    } else {
                        columnOffset--;
                    }
                    strings.add(new G.GString.Value(randomId(), Markers.EMPTY, doVisit(e), inCurlies ? sourceBefore("}") : EMPTY, inCurlies));
                    if (!inCurlies) {
                        columnOffset++;
                    }
                } else if (e instanceof ConstantExpression) {
                    // Get the string literal from the source, so escaping of newlines and the like works out of the box
                    String value = hasInterpolation ?
                            readConstantSegmentBeforeNextInterpolation(delimiter) :
                            sourceSubstring(cursor, delimiter.close);
                    strings.add(new J.Literal(randomId(), EMPTY, Markers.EMPTY, value, value, null, JavaType.Primitive.String));
                    skip(value);
                } else {
                    // Everything should be handled already by the other two code paths, but just in case
                    strings.add(doVisit(e));
                }
            }

            queue.add(new G.GString(randomId(), fmt, Markers.EMPTY, delimiter.open, strings, typeMapping.type(gstring.getType())));
            skip(delimiter.close);
        }

        @Override
        public void visitListExpression(ListExpression list) {
            queue.add(insideParentheses(list, fmt -> {
                skip("[");
                if (list.getExpressions().isEmpty()) {
                    return new G.ListLiteral(randomId(), fmt, Markers.EMPTY,
                            JContainer.build(singletonList(new JRightPadded<>(new J.Empty(randomId(), EMPTY, Markers.EMPTY), sourceBefore("]"), Markers.EMPTY))),
                            typeMapping.type(list.getType()));
                } else {
                    return new G.ListLiteral(randomId(), fmt, Markers.EMPTY,
                            JContainer.build(visitRightPadded(list.getExpressions().toArray(new ASTNode[0]), "]")),
                            typeMapping.type(list.getType()));
                }
            }));
        }

        @Override
        public void visitMapEntryExpression(MapEntryExpression expression) {
            Space prefix = whitespace();
            Expression key;
            int saveCursor = cursor;
            Space beforeOpenParen = whitespace();
            Integer keyParenLevel = getInsideParenthesesLevel(expression.getKeyExpression());
            if ((keyParenLevel == null || keyParenLevel == 0) &&
                    cursor < source.length() && source.charAt(cursor) == '(') {
                skip("(");
                Expression inner = doVisit(expression.getKeyExpression());
                key = new J.Parentheses<>(randomId(), beforeOpenParen, Markers.EMPTY,
                        JRightPadded.build((J) inner).withAfter(sourceBefore(")")));
            } else {
                cursor = saveCursor;
                key = doVisit(expression.getKeyExpression());
            }
            G.MapEntry mapEntry = new G.MapEntry(randomId(), prefix, Markers.EMPTY,
                    JRightPadded.build(key).withAfter(sourceBefore(":")),
                    doVisit(expression.getValueExpression()),
                    null
            );
            queue.add(mapEntry);
        }

        @Override
        public void visitMapExpression(MapExpression map) {
            queue.add(insideParentheses(map, fmt -> {
                skip("[");
                JContainer<G.MapEntry> entries;
                if (map.getMapEntryExpressions().isEmpty()) {
                    entries = JContainer.build(singletonList(JRightPadded.build(
                            new G.MapEntry(randomId(), whitespace(), Markers.EMPTY,
                                    JRightPadded.build(new J.Empty(randomId(), sourceBefore(":"), Markers.EMPTY)),
                                    new J.Empty(randomId(), sourceBefore("]"), Markers.EMPTY), null))));
                } else {
                    entries = JContainer.build(visitRightPadded(map.getMapEntryExpressions().toArray(new ASTNode[0]), "]"));
                }
                return new G.MapLiteral(randomId(), fmt, Markers.EMPTY, entries, typeMapping.type(map.getType()));
            }));
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            // Groovy parses control-flow constructs inside a GString interpolation
            // (e.g. ${if (cond) { a } else { b }}) as a synthetic implicit call() on
            // a ClosureExpression that wraps the statement. The "call" method name
            // does not appear in the source, so unwrap and visit the inner statement.
            if (call.isImplicitThis() &&
                    call.getObjectExpression() instanceof ClosureExpression &&
                    "call".equals(call.getMethodAsString()) &&
                    call.getArguments() instanceof ArgumentListExpression &&
                    ((ArgumentListExpression) call.getArguments()).getExpressions().isEmpty()) {
                ClosureExpression closure = (ClosureExpression) call.getObjectExpression();
                if (closure.getCode() instanceof BlockStatement) {
                    BlockStatement body = (BlockStatement) closure.getCode();
                    if (body.getStatements().size() == 1 && body.getStatements().get(0) instanceof IfStatement) {
                        body.getStatements().get(0).visit(this);
                        return;
                    }
                }
            }
            // Trait field reads inside method bodies are rewritten by the trait transform into synthetic accessor
            // calls like `$self.TraitName__field$get()`. Only the field name appears in source, so emit the bare
            // identifier here rather than walking the synthetic call.
            String callMethodName = call.getMethodAsString();
            if (callMethodName != null && callMethodName.endsWith("$get") &&
                    call.getObjectExpression().getLineNumber() < 0) {
                int sep = callMethodName.indexOf("__");
                if (sep > 0) {
                    String fieldName = callMethodName.substring(sep + 2, callMethodName.length() - 4);
                    Space prefix = whitespace();
                    if (source.startsWith(fieldName, cursor)) {
                        skip(fieldName);
                        queue.add(new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), fieldName, typeMapping.type(staticType(call)), null));
                        return;
                    }
                }
            }
            queue.add(insideParentheses(call, fmt -> {
                ImplicitDot implicitDot = null;
                JRightPadded<Expression> select = null;
                // The trait AST transformation rewrites implicit-this calls inside trait method bodies to use a
                // synthetic $self variable expression with no source position; treat that as implicit-this so we
                // don't print "$self" where the user wrote nothing.
                boolean syntheticSelf = !call.isImplicitThis() && call.getObjectExpression().getLineNumber() < 0;
                if (!call.isImplicitThis() && !syntheticSelf) {
                    Expression selectExpr = doVisit(call.getObjectExpression());
                    int saveCursor = cursor;
                    Space afterSelect = whitespace();
                    if (source.charAt(cursor) == '.' || source.charAt(cursor) == '?' || source.charAt(cursor) == '*') {
                        cursor = saveCursor;
                        afterSelect = sourceBefore(call.isSpreadSafe() ? "*." : call.isSafe() ? "?." : ".");
                    } else {
                        implicitDot = new ImplicitDot(randomId());
                    }
                    select = JRightPadded.build(selectExpr).withAfter(afterSelect);
                }
                JContainer<Expression> typeParameters = call.getGenericsTypes() != null ? visitTypeParameterizations(call.getGenericsTypes()) : null;
                // Closure invocations that are written as closure.call() and closure() are parsed into identical MethodCallExpression
                // closure() has implicitThis set to false
                // So the "select" that was just parsed _may_ have actually been the method name
                J.Identifier name;

                String methodName = call.getMethodAsString();
                // Check for escaped method name, often used in tests (def 'description'() {}) or to avoid clashing with Groovy keywords
                if (source.charAt(cursor) == '"' || source.charAt(cursor) == '\'') {
                    // TODO: Methods with string interpolation are parsed as just one method name instead of multiple LST elements
                    String delim = source.charAt(cursor) + "";
                    methodName = sourceSubstring(cursor, delim) + delim;
                }

                Space prefix = whitespace();
                boolean implicitCall = (methodName != null && cursor < source.length() &&
                        source.charAt(cursor) == '(' && (cursor + methodName.length() > source.length() ||
                        !methodName.equals(source.substring(cursor, cursor + methodName.length())))
                );
                Space spaceBeforeArgs = null;
                if (implicitCall) {
                    // This is an implicit call() method - create identifier but it doesn't get printed
                    name = new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), "", null, null);
                } else {
                    if (methodName.equals(source.substring(cursor, cursor + methodName.length()))) {
                        skip(methodName);
                        name = new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), methodName, null, null);
                    } else if (select != null && select.getElement() instanceof J.Identifier) {
                        name = (J.Identifier) select.getElement();
                        // Closure-style command expression like `x foo(c)` parses as `x.call(foo(c))`.
                        // The whitespace captured as select's right-padding is the space between
                        // the closure name and its argument list, which has no parens.
                        spaceBeforeArgs = select.getAfter();
                        select = null;
                    } else {
                        throw new IllegalArgumentException("Unable to parse method call");
                    }
                }

                if (call.isSpreadSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new StarDot(randomId())));
                }
                if (call.isSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
                }
                if (implicitDot != null) {
                    name = name.withMarkers(name.getMarkers().add(implicitDot));
                }
                // Method invocations may have type information that can enrich the type information of its parameters
                Markers markers = Markers.EMPTY;
                if (call.getArguments() instanceof ArgumentListExpression) {
                    ArgumentListExpression args = (ArgumentListExpression) call.getArguments();
                    if (call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE) != null) {
                        for (org.codehaus.groovy.ast.expr.Expression arg : args.getExpressions()) {
                            if (!(arg instanceof ClosureExpression)) {
                                continue;
                            }
                            ClosureExpression cl = (ClosureExpression) arg;
                            ClassNode actualParamTypeRaw = call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            for (Parameter p : cl.getParameters()) {
                                if (p.isDynamicTyped()) {
                                    p.setType(actualParamTypeRaw);
                                    p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                                }
                            }
                            for (Map.Entry<String, Variable> declaredVariable : cl.getVariableScope().getDeclaredVariables().entrySet()) {
                                if (declaredVariable.getValue() instanceof Parameter && declaredVariable.getValue().isDynamicTyped()) {
                                    Parameter p = (Parameter) declaredVariable.getValue();
                                    p.setType(actualParamTypeRaw);
                                    p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                                }
                            }
                        }
                    }
                    markers = handlesCaseWhereEmptyParensAheadOfClosure(args, markers);
                }
                JContainer<Expression> args = doVisit(call.getArguments());
                if (spaceBeforeArgs != null && !spaceBeforeArgs.getWhitespace().isEmpty()) {
                    args = args.withBefore(spaceBeforeArgs);
                }

                MethodNode methodNode = (MethodNode) call.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                JavaType.Method methodType = null;
                if (methodNode == null && call.getObjectExpression() instanceof VariableExpression &&
                        ((VariableExpression) call.getObjectExpression()).getAccessedVariable() != null) {
                    // Groovy doesn't know what kind of object this method is being invoked on
                    // But if this invocation is inside a Closure we may have already enriched its parameters with types from the static type checker
                    // Use any such type information to attempt to find a matching method
                    ClassNode parameterType = staticType(((VariableExpression) call.getObjectExpression()).getAccessedVariable());
                    if (args.getElements().size() == 1 && args.getElements().get(0) instanceof J.Empty) {
                        methodType = typeMapping.methodType(parameterType.getMethod(name.getSimpleName(), new Parameter[]{}));
                    } else if (call.getArguments() instanceof ArgumentListExpression) {
                        List<org.codehaus.groovy.ast.expr.Expression> rawArgs = ((ArgumentListExpression) call.getArguments()).getExpressions();
                    /*
                      Look through the methods returning the closest match on a best-effort basis
                      Factors which can result in a less accurate match, or no match, include:
                          * The type of each parameter may or may not be known to us
                          * Usage of Groovy's "named parameters" syntactic sugaring throwing off argument count and order
                    */
                        methodLoop:
                        for (MethodNode candidateMethod : parameterType.getAllDeclaredMethods()) {
                            if (!name.getSimpleName().equals(candidateMethod.getName())) {
                                continue;
                            }
                            if (rawArgs.size() != candidateMethod.getParameters().length) {
                                continue;
                            }
                            // Better than nothing
                            methodType = typeMapping.methodType(candidateMethod);

                            // If all parameter types agree then we have found an exact match
                            for (int i = 0; i < candidateMethod.getParameters().length; i++) {
                                JavaType param = typeMapping.type(staticType(candidateMethod.getParameters()[i]));
                                JavaType arg = typeMapping.type(staticType(rawArgs.get(i)));
                                if (!TypeUtils.isAssignableTo(param, arg)) {
                                    continue methodLoop;
                                }
                            }
                            break;
                        }
                    }
                } else {
                    methodType = typeMapping.methodType(methodNode);
                }
                return new J.MethodInvocation(randomId(), fmt, markers, select, typeParameters, name, args, methodType);
            }));
        }

        @Override
        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
            Space fmt = whitespace();

            MethodNode methodNode = (MethodNode) call.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
            JavaType.Method methodType = typeMapping.methodType(methodNode);
            J.Identifier name = new J.Identifier(randomId(), sourceBefore(call.getMethodAsString()), Markers.EMPTY,
                    emptyList(), call.getMethodAsString(), methodType, null);

            // Method invocations may have type information that can enrich the type information of its parameters
            Markers markers = Markers.EMPTY;
            if (call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression args = (ArgumentListExpression) call.getArguments();
                if (call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE) != null) {
                    for (org.codehaus.groovy.ast.expr.Expression arg : args.getExpressions()) {
                        if (!(arg instanceof ClosureExpression)) {
                            continue;
                        }
                        ClosureExpression cl = (ClosureExpression) arg;
                        ClassNode actualParamTypeRaw = call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                        for (Parameter p : cl.getParameters()) {
                            if (p.isDynamicTyped()) {
                                p.setType(actualParamTypeRaw);
                                p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            }
                        }
                        for (Map.Entry<String, Variable> declaredVariable : cl.getVariableScope().getDeclaredVariables().entrySet()) {
                            if (declaredVariable.getValue() instanceof Parameter && declaredVariable.getValue().isDynamicTyped()) {
                                Parameter p = (Parameter) declaredVariable.getValue();
                                p.setType(actualParamTypeRaw);
                                p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            }
                        }
                    }
                }
                markers = handlesCaseWhereEmptyParensAheadOfClosure(args, markers);
            }
            JContainer<Expression> args = doVisit(call.getArguments());

            queue.add(new J.MethodInvocation(randomId(), fmt, markers, null, null, name, args, methodType));
        }

        @Override
        public void visitMethodPointerExpression(MethodPointerExpression ref) {
            boolean isMethodRef = ref instanceof MethodReferenceExpression;
            String name = ref.getMethodName().getText();
            queue.add(new J.MemberReference(randomId(),
                    whitespace(),
                    isMethodRef ? Markers.EMPTY : Markers.build(singleton(new MethodPointer(randomId()))),
                    padRight(doVisit(ref.getExpression()), sourceBefore(isMethodRef ? "::" : ".&")),
                    null, // not supported by Groovy
                    padLeft(whitespace(), new J.Identifier(randomId(),
                            sourceBefore(name),
                            Markers.EMPTY,
                            emptyList(),
                            name,
                            null, null)),
                    typeMapping.type(ref.getType()),
                    null, // not enough information in the AST
                    null  // not enough information in the AST
            ));
        }

        @Override
        public void visitAttributeExpression(AttributeExpression attr) {
            queue.add(insideParentheses(attr, fmt -> {
                Expression target = doVisit(attr.getObjectExpression());
                Space beforeDot = attr.isSafe() ? sourceBefore("?.") : sourceBefore(attr.isSpreadSafe() ? "*." : ".");
                J name = doVisit(attr.getProperty());
                if (name instanceof J.Literal) {
                    String nameStr = ((J.Literal) name).getValueSource();
                    assert nameStr != null;
                    name = new J.Identifier(randomId(), name.getPrefix(), Markers.EMPTY, emptyList(), nameStr, null, null);
                }
                if (attr.isSpreadSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new StarDot(randomId())));
                }
                if (attr.isSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
                }
                return new J.FieldAccess(randomId(), fmt, Markers.EMPTY, target, padLeft(beforeDot, (J.Identifier) name), null);
            }));
        }

        @Override
        public void visitPropertyExpression(PropertyExpression prop) {
            // The trait AST transformation rewrites field accesses inside trait method bodies to use a synthetic
            // $self variable expression with no source position; collapse such property accesses to the property
            // name only since "$self." does not appear in source.
            if (prop.getObjectExpression().getLineNumber() < 0 && prop.getProperty() instanceof ConstantExpression) {
                ConstantExpression nameExpr = (ConstantExpression) prop.getProperty();
                Object value = nameExpr.getValue();
                if (value instanceof String) {
                    Space namePrefix = whitespace();
                    String simpleName = (String) value;
                    skip(simpleName);
                    queue.add(new J.Identifier(randomId(), namePrefix, Markers.EMPTY, emptyList(), simpleName, typeMapping.type(staticType(prop)), null));
                    return;
                }
            }
            queue.add(insideParentheses(prop, fmt -> {
                Expression target = doVisit(prop.getObjectExpression());
                Space beforeDot = prop.isSpreadSafe() ? sourceBefore("*.") : sourceBefore(prop.isSafe() ? "?." : ".");
                J name = doVisit(prop.getProperty());
                if (name instanceof J.Literal) {
                    J.Literal nameLiteral = ((J.Literal) name);
                    name = new J.Identifier(randomId(), name.getPrefix(), Markers.EMPTY, emptyList(), nameLiteral.getValueSource(), nameLiteral.getType(), null);
                }
                if (prop.isSpreadSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new StarDot(randomId())));
                } else if (prop.isSafe()) {
                    name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
                }
                return new J.FieldAccess(randomId(), fmt, Markers.EMPTY, target, padLeft(beforeDot, (J.Identifier) name), null);
            }));
        }

        @Override
        public void visitRangeExpression(RangeExpression range) {
            queue.add(insideParentheses(range, fmt -> new G.Range(randomId(), fmt, Markers.EMPTY,
                    doVisit(range.getFrom()),
                    JLeftPadded.build(range.isInclusive()).withBefore(sourceBefore(range.isInclusive() ? ".." : "..<")),
                    doVisit(range.getTo()))));
        }

        @Override
        public void visitReturnStatement(ReturnStatement return_) {
            Space fmt = sourceBefore("return");
            if (return_.getExpression() instanceof ConstantExpression && isSynthetic(return_.getExpression()) &&
                    (((ConstantExpression) return_.getExpression()).getValue() == null)) {
                queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, null));
            } else {
                queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, doVisit(return_.getExpression())));
            }
        }

        @Override
        public void visitShortTernaryExpression(ElvisOperatorExpression ternary) {
            queue.add(insideParentheses(ternary, fmt -> {
                Expression trueExpr = doVisit(ternary.getBooleanExpression());
                J.Ternary elvis = new J.Ternary(randomId(), fmt, Markers.EMPTY,
                        trueExpr,
                        padLeft(sourceBefore("?"), trueExpr),
                        padLeft(sourceBefore(":"), doVisit(ternary.getFalseExpression())),
                        typeMapping.type(staticType(ternary)));
                return elvis.withMarkers(elvis.getMarkers().add(new Elvis(randomId())));
            }));
        }

        @Override
        public void visitSwitch(SwitchStatement statement) {
            queue.add(new J.Switch(
                    randomId(),
                    sourceBefore("switch"),
                    Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) doVisit(statement.getExpression())).withAfter(sourceBefore(")"))),
                    new J.Block(
                            randomId(), sourceBefore("{"), Markers.EMPTY,
                            JRightPadded.build(false),
                            ListUtils.concat(
                                    convertAll(statement.getCaseStatements(), t -> EMPTY, t -> EMPTY),
                                    statement.getDefaultStatement().isEmpty() ? null : JRightPadded.build(visitDefaultCaseStatement((BlockStatement) statement.getDefaultStatement()))
                            ),
                            sourceBefore("}"))));
        }

        @Override
        public void visitSynchronizedStatement(SynchronizedStatement statement) {
            Space fmt = sourceBefore("synchronized");
            queue.add(new J.Synchronized(randomId(), fmt, Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) doVisit(statement.getExpression())).withAfter(sourceBefore(")"))),
                    doVisit(statement.getCode())));
        }

        @Override
        public void visitTernaryExpression(TernaryExpression ternary) {
            queue.add(insideParentheses(ternary, fmt -> new J.Ternary(randomId(), fmt, Markers.EMPTY,
                    doVisit(ternary.getBooleanExpression()),
                    padLeft(sourceBefore("?"), doVisit(ternary.getTrueExpression())),
                    padLeft(sourceBefore(":"), doVisit(ternary.getFalseExpression())),
                    typeMapping.type(ternary.getType()))));
        }

        @Override
        public void visitThrowStatement(ThrowStatement statement) {
            Space fmt = sourceBefore("throw");
            queue.add(new J.Throw(randomId(), fmt, Markers.EMPTY, doVisit(statement.getExpression())));
        }

        // the current understanding is that TupleExpression only exist as method invocation arguments.
        // this is the reason behind the simplifying assumption that there is one expression, and it is
        // a NamedArgumentListExpression.
        @Override
        public void visitTupleExpression(TupleExpression tuple) {
            int saveCursor = cursor;
            Space beforeOpenParen = whitespace();

            OmitParentheses omitParentheses = null;
            if (source.charAt(cursor) == '(') {
                skip("(");
            } else {
                omitParentheses = new OmitParentheses(randomId());
                beforeOpenParen = EMPTY;
                cursor = saveCursor;
            }

            List<JRightPadded<Expression>> args = new ArrayList<>(tuple.getExpressions().size());
            for (org.codehaus.groovy.ast.expr.Expression expression : tuple.getExpressions()) {
                NamedArgumentListExpression namedArgList = (NamedArgumentListExpression) expression;
                List<MapEntryExpression> mapEntryExpressions = namedArgList.getMapEntryExpressions();
                for (int i = 0; i < mapEntryExpressions.size(); i++) {
                    Expression arg = doVisit(mapEntryExpressions.get(i));
                    if (omitParentheses != null) {
                        arg = arg.withMarkers(arg.getMarkers().add(omitParentheses));
                    }

                    Space after = EMPTY;
                    Space trailingCommaSuffix = null;
                    if (i == mapEntryExpressions.size() - 1) {
                        if (omitParentheses == null) {
                            saveCursor = cursor;
                            after = whitespace();
                            if (source.charAt(cursor) == ',') {
                                skip(",");
                                trailingCommaSuffix = sourceBefore(")");
                            } else {
                                cursor = saveCursor;
                                after = sourceBefore(")");
                            }
                        }
                    } else {
                        after = whitespace();
                        if (source.charAt(cursor) == ')') {
                            // the next argument will have an OmitParentheses marker
                            omitParentheses = new OmitParentheses(randomId());
                        }
                        cursor++;
                    }

                  args.add(newRightPadded(arg, after, trailingCommaSuffix));
                }
            }

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
        }

        @Override
        public void visitTryCatchFinally(TryCatchStatement node) {
            Space prefix = sourceBefore("try");

            // Groovy 4 desugars try-with-resources at parse time (getResourceStatements() is always empty).
            // Detect from source: if "(" follows "try", parse resources from source text.
            JContainer<J.Try.Resource> resources = null;
            boolean hasTryWithResources = source.charAt(indexOfNextNonWhitespace(cursor, source)) == '(';
            if (hasTryWithResources) {
                Space beforeParen = sourceBefore("(");
                List<JRightPadded<J.Try.Resource>> resourceList = new ArrayList<>();

                // Groovy 4 desugars each resource into a nested structure:
                //   BlockStatement -> BlockStatement -> [resourceDecl, sentinel, innerTryCatch]
                // Multiple resources nest recursively: innerTryCatch.tryStmt wraps the next resource.
                org.codehaus.groovy.ast.stmt.Statement current = node.getTryStatement();
                while (isDesugaredResourceBlock(current)) {
                    BlockStatement innerBlock = (BlockStatement) ((BlockStatement) current).getStatements().get(0);
                    ExpressionStatement resourceStmt = (ExpressionStatement) innerBlock.getStatements().get(0);
                    J resourceVar = doVisit(resourceStmt.getExpression());
                    Space resourcePrefix = resourceVar.getPrefix();
                    resourceVar = resourceVar.withPrefix(EMPTY);

                    TryCatchStatement innerTry = (TryCatchStatement) innerBlock.getStatements().get(innerBlock.getStatements().size() - 1);
                    boolean hasMoreResources = isDesugaredResourceBlock(innerTry.getTryStatement());

                    int nextNonWs = indexOfNextNonWhitespace(cursor, source);
                    boolean semicolonPresent = nextNonWs < source.length() && source.charAt(nextNonWs) == ';';
                    if (semicolonPresent && resourceVar instanceof J.VariableDeclarations) {
                        J.VariableDeclarations resourceVarDecl = (J.VariableDeclarations) resourceVar;
                        resourceVar = resourceVarDecl.getPadding().withVariables(Space.formatLastSuffix(resourceVarDecl
                                .getPadding().getVariables(), sourceBefore(";")));
                    }

                    J.Try.Resource tryResource = new J.Try.Resource(randomId(), resourcePrefix, Markers.EMPTY,
                            resourceVar.withPrefix(EMPTY), semicolonPresent);
                    skip(";");

                    if (hasMoreResources) {
                        resourceList.add(padRight(tryResource, EMPTY));
                        current = innerTry.getTryStatement();
                    } else {
                        resourceList.add(padRight(tryResource, sourceBefore(")")));
                        break;
                    }
                }
                resources = JContainer.build(beforeParen, resourceList, Markers.EMPTY);
            }

            // When try-with-resources, find the actual body by walking down the nested structure
            // to the innermost TryCatchStatement's tryStmt.
            J.Block body;
            if (hasTryWithResources) {
                org.codehaus.groovy.ast.stmt.Statement current = node.getTryStatement();
                TryCatchStatement innerTry = null;
                while (isDesugaredResourceBlock(current)) {
                    BlockStatement innerBlock = (BlockStatement) ((BlockStatement) current).getStatements().get(0);
                    innerTry = (TryCatchStatement) innerBlock.getStatements().get(innerBlock.getStatements().size() - 1);
                    if (isDesugaredResourceBlock(innerTry.getTryStatement())) {
                        current = innerTry.getTryStatement();
                    } else {
                        break;
                    }
                }
                body = doVisit(innerTry != null ? innerTry.getTryStatement() : node.getTryStatement());
            } else {
                body = doVisit(node.getTryStatement());
            }

            // Handle catches, merging multi-catch statements.
            // Groovy 4 splits catch(A | B e) into separate CatchStatements at the same source position.
            List<J.Try.Catch> catches;
            if (node.getCatchStatements().isEmpty()) {
                catches = emptyList();
            } else {
                catches = new ArrayList<>(node.getCatchStatements().size());
                List<CatchStatement> catchStatements = node.getCatchStatements();
                for (int i = 0; i < catchStatements.size(); ) {
                    CatchStatement catchStatement = catchStatements.get(i);
                    // Detect multi-catch: consecutive catches at the same line/col
                    int j = i + 1;
                    while (j < catchStatements.size() &&
                            catchStatements.get(j).getLineNumber() == catchStatement.getLineNumber() &&
                            catchStatements.get(j).getColumnNumber() == catchStatement.getColumnNumber()) {
                        j++;
                    }
                    if (j > i + 1) {
                        // Multi-catch: parse a single catch with J.MultiCatch type
                        visitMultiCatchStatement(catchStatements.subList(i, j));
                    } else {
                        visitCatchStatement(catchStatement);
                    }
                    catches.add((J.Try.Catch) queue.poll());
                    i = j;
                }
            }

            // Strangely, groovy parses the finally's block as a BlockStatement which contains another BlockStatement
            // The true contents of the block are within the first statement of this apparently pointless enclosing BlockStatement
            JLeftPadded<J.Block> finally_ = !(node.getFinallyStatement() instanceof BlockStatement) ? null :
                    padLeft(sourceBefore("finally"), doVisit(((BlockStatement) node.getFinallyStatement()).getStatements().get(0)));

            //noinspection ConstantConditions
            queue.add(new J.Try(randomId(), prefix, Markers.EMPTY, resources, body, catches, finally_));
        }

        @Override
        public void visitPostfixExpression(PostfixExpression unary) {
            Space fmt = whitespace();
            Expression expression = doVisit(unary.getExpression());

            Space operatorPrefix = whitespace();
            String typeToken = unary.getOperation().getText();
            cursor += typeToken.length();

            J.Unary.Type operator = null;
            switch (typeToken) {
                case "++":
                    operator = J.Unary.Type.PostIncrement;
                    break;
                case "--":
                    operator = J.Unary.Type.PostDecrement;
                    break;
            }
            assert operator != null;

            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY,
                    JLeftPadded.build(operator).withBefore(operatorPrefix),
                    expression, null));
        }

        @Override
        public void visitPrefixExpression(PrefixExpression unary) {
            Space fmt = whitespace();
            String typeToken = unary.getOperation().getText();
            skip(typeToken);

            J.Unary.Type operator = null;
            switch (typeToken) {
                case "++":
                    operator = J.Unary.Type.PreIncrement;
                    break;
                case "--":
                    operator = J.Unary.Type.PreDecrement;
                    break;
                case "~":
                    operator = J.Unary.Type.Complement;
                    break;
            }

            assert operator != null;
            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY,
                    JLeftPadded.build(operator),
                    doVisit(unary.getExpression()),
                    null));
        }

        @Override
        public void visitSpreadExpression(SpreadExpression spreadExpression) {
            Space fmt = whitespace();
            skip("*");
            queue.add(new G.Unary(randomId(), fmt, Markers.EMPTY, JLeftPadded.build(G.Unary.Type.Spread), doVisit(spreadExpression.getExpression()), null));
        }

        public TypeTree visitVariableExpressionType(@Nullable VariableExpression expression) {
            if (expression == null) {
                return null;
            }
            if (!expression.isDynamicTyped() && expression.getOriginType().isArray()) {
                return visitTypeTree(expression.getOriginType());
            }

            JavaType type = typeMapping.type(staticType(((org.codehaus.groovy.ast.expr.Expression) expression)));
            Space prefix = whitespace();
            String typeName = "";

            if (!expression.isDynamicTyped() && source.startsWith(expression.getOriginType().getUnresolvedName(), cursor)) {
                if (cursor + expression.getOriginType().getUnresolvedName().length() < source.length() &&
                        !isJavaIdentifierPart(source.charAt(cursor + expression.getOriginType().getUnresolvedName().length()))) {
                    typeName = expression.getOriginType().getUnresolvedName();
                    skip(typeName);
                }
            }
            J.Identifier ident = new J.Identifier(randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    typeName,
                    type, null);
            if (expression.getOriginType().getGenericsTypes() != null && !expression.getOriginType().isGenericsPlaceHolder() && sourceStartsWith("<")) {
                return new J.ParameterizedType(randomId(), prefix, Markers.EMPTY, ident, visitTypeParameterizations(
                        staticType((org.codehaus.groovy.ast.expr.Expression) expression).getGenericsTypes()), type);
            }
            return ident.withPrefix(prefix);
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            queue.add(insideParentheses(expression, fmt -> {
                JavaType type;
                if (expression.isDynamicTyped() && expression.getAccessedVariable() != null && expression.getAccessedVariable().getType() != expression.getOriginType()) {
                    type = typeMapping.type(staticType(expression.getAccessedVariable()));
                } else {
                    type = typeMapping.type(staticType((org.codehaus.groovy.ast.expr.Expression) expression));
                }

                return new J.Identifier(randomId(),
                        fmt.withWhitespace(fmt.getWhitespace() + sourceBefore(expression.getName()).getWhitespace()),
                        Markers.EMPTY,
                        emptyList(),
                        expression.getName(),
                        type, null);
            }));
        }

        @Override
        public void visitWhileLoop(WhileStatement loop) {
            Space fmt = sourceBefore("while");
            queue.add(new J.WhileLoop(randomId(), fmt, Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) doVisit(loop.getBooleanExpression().getExpression()))
                                    .withAfter(sourceBefore(")"))),
                    JRightPadded.build(doVisit(loop.getLoopBlock()))
            ));
        }

        private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends ASTNode> nodes,
                                                                 Function<ASTNode, Space> innerSuffix,
                                                                 Function<ASTNode, Space> suffix,
                                                                 Function<ASTNode, Markers> markers) {
            if (nodes.isEmpty()) {
                return emptyList();
            }
            List<JRightPadded<J2>> converted = new ArrayList<>(nodes.size());
            for (int i = 0; i < nodes.size(); i++) {
                converted.add(convert(nodes.get(i), i == nodes.size() - 1 ? suffix : innerSuffix, markers));
            }
            return converted;
        }

        private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends ASTNode> nodes,
                                                                 Function<ASTNode, Space> innerSuffix,
                                                                 Function<ASTNode, Space> suffix) {
            return convertAll(nodes, innerSuffix, suffix, n -> Markers.EMPTY);
        }

        private <J2 extends J> JRightPadded<J2> convert(ASTNode node, Function<ASTNode, Space> suffix) {
            return convert(node, suffix, n -> Markers.EMPTY);
        }

        private <J2 extends J> JRightPadded<J2> convert(ASTNode node, Function<ASTNode, Space> suffix, Function<ASTNode, Markers> markers) {
            J2 j = doVisit(node);
            return padRight(j, suffix.apply(node), markers.apply(node));
        }

        private List<JRightPadded<Statement>> convertStatements(List<? extends ASTNode> nodes) {
            if (nodes.isEmpty()) {
                return emptyList();
            }

            List<JRightPadded<Statement>> converted = new ArrayList<>(nodes.size());
            for (ASTNode node : nodes) {
                Statement statement = doVisit(node);
                converted.add(maybeSemicolon(statement));
            }

            return converted;
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private static JRightPadded<Expression> newRightPadded(
        Expression exp,
        Space after,
        @Nullable Space trailingCommaSuffix
    ) {
        JRightPadded<Expression> arg = JRightPadded.build(exp).withAfter(after);
        if (trailingCommaSuffix != null) {
            arg = arg.withMarkers(exp.getMarkers().add(new TrailingComma(randomId(), trailingCommaSuffix)));
        }
        return arg;
    }

  // handle the obscure case where there are empty parens ahead of a closure
    private Markers handlesCaseWhereEmptyParensAheadOfClosure(ArgumentListExpression args, Markers markers) {
        if (args.getExpressions().size() == 1 && args.getExpressions().get(0) instanceof ClosureExpression) {
            int saveCursor = cursor;
            Space argPrefix = whitespace();
            if (source.charAt(cursor) == '(') {
                skip("(");
                Space infix = whitespace();
                if (source.charAt(cursor) == ')') {
                    skip(")");
                    markers = markers.add(new EmptyArgumentListPrecedesArgument(randomId(), argPrefix, infix));
                } else {
                    cursor = saveCursor;
                }
            } else {
                cursor = saveCursor;
            }
        }
        return markers;
    }

    private JRightPadded<Statement> convertTopLevelStatement(SourceUnit unit, ASTNode node) {
        if (node instanceof ClassNode) {
            ClassNode classNode = (ClassNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitClass(classNode);
            return JRightPadded.build(classVisitor.pollQueue());
        } else if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitMethod(methodNode);
            return JRightPadded.build(classVisitor.pollQueue());
        } else if (node instanceof ImportNode) {
            ImportNode importNode = (ImportNode) node;
            Space importPrefix = sourceBefore("import");
            JLeftPadded<Boolean> statik = importNode.isStatic() ? padLeft(sourceBefore("static"), true) : padLeft(EMPTY, false);
            Space space = whitespace();
            J.FieldAccess qualid = TypeTree.build(name()).withPrefix(space);
            JLeftPadded<J.Identifier> alias = null;
            if (sourceStartsWith("as", "\n", " ")) {
                alias = padLeft(sourceBefore("as"), new J.Identifier(randomId(), whitespace(), Markers.EMPTY, emptyList(), name(), null, null));
            }
            return maybeSemicolon(new J.Import(randomId(), importPrefix, Markers.EMPTY, statik, qualid, alias));
        }

        RewriteGroovyVisitor groovyVisitor = new RewriteGroovyVisitor(node, new RewriteGroovyClassVisitor(unit));
        node.visit(groovyVisitor);
        return maybeSemicolon(groovyVisitor.pollQueue());
    }

    // The groovy compiler discards these annotations in favour of other transform annotations,
    // so they must be parsed by hand when found in source.
    private static final Class<?>[] DISCARDED_TRANSFORM_ANNOTATIONS = {Canonical.class, Immutable.class, groovy.transform.Synchronized.class};

    public List<J.Annotation> visitAndGetAnnotations(AnnotatedNode node, RewriteGroovyClassVisitor classVisitor) {
        if (node.getAnnotations().isEmpty()) {
            return emptyList();
        }

        List<J.Annotation> paramAnnotations = new ArrayList<>(node.getAnnotations().size());
        for (AnnotationNode annotationNode : node.getAnnotations()) {
            for (Class<?> discarded : DISCARDED_TRANSFORM_ANNOTATIONS) {
                if (sourceStartsWith("@" + discarded.getSimpleName()) || sourceStartsWith("@" + discarded.getCanonicalName())) {
                    paramAnnotations.add(visitAnnotation(new AnnotationNode(new ClassNode(discarded)), classVisitor));
                }
            }

            if (appearsInSource(annotationNode)) {
                paramAnnotations.add(visitAnnotation(annotationNode, classVisitor));
            }
        }
        return paramAnnotations;
    }

    public J.Annotation visitAnnotation(AnnotationNode annotation, RewriteGroovyClassVisitor classVisitor) {
        RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor(annotation, classVisitor);
        String lastArgKey = annotation.getMembers().keySet().stream().reduce("", (k1, k2) -> k2);
        Space prefix = sourceBefore("@");
        NameTree annotationType = visitTypeTree(annotation.getClassNode());
        JContainer<Expression> arguments = null;
        // AST transforms like @Immutable can attach synthetic members to other annotations
        // (e.g. @ToString) that don't appear in source — only parse arguments if "(" is actually next.
        if (!annotation.getMembers().isEmpty() && sourceStartsWith("(")) {
            arguments = JContainer.build(
                    sourceBefore("("),
                    annotation.getMembers().entrySet().stream()
                            // Non-value implicit properties should not be represented in our LST.
                            .filter(it -> sourceStartsWith(it.getKey()) || "value".equals(it.getKey()))
                            .map(arg -> {
                                boolean isImplicitValue = "value".equals(arg.getKey()) && !sourceStartsWith("value");
                                Space argPrefix = isImplicitValue ? whitespace() : sourceBefore(arg.getKey());
                                Space isSign = isImplicitValue ? null : sourceBefore("=");
                                Expression expression;
                                if (arg.getValue() instanceof AnnotationConstantExpression) {
                                    expression = visitAnnotation((AnnotationNode) ((AnnotationConstantExpression) arg.getValue()).getValue(), classVisitor);
                                } else {
                                    expression = bodyVisitor.doVisit(arg.getValue());
                                }
                                Expression element = isImplicitValue ? expression.withPrefix(argPrefix) :
                                        (new J.Assignment(randomId(), argPrefix, Markers.EMPTY,
                                                new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), arg.getKey(), null, null),
                                                padLeft(isSign, expression), null));
                                return JRightPadded.build(element)
                                        .withAfter(arg.getKey().equals(lastArgKey) ? sourceBefore(")") : sourceBefore(","));
                            })
                            .collect(toList()),
                    Markers.EMPTY
            );
            // Rare scenario where annotation does only have non-value implicit properties
            if (arguments.getElements().isEmpty()) {
                arguments = null;
            }
        } else if (sourceStartsWith("(")) {
            // Annotation with empty arguments like @Foo()
            arguments = JContainer.build(sourceBefore("("),
                    singletonList(JRightPadded.build(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY))),
                    Markers.EMPTY);
        }

        return new J.Annotation(randomId(), prefix, Markers.EMPTY, annotationType, arguments);
    }

    private static LineColumn pos(ASTNode node) {
        return new LineColumn(node.getLineNumber(), node.getColumnNumber());
    }

    private static boolean isSynthetic(ASTNode node) {
        return node.getLineNumber() == -1;
    }

    @Value
    private static class LineColumn implements Comparable<LineColumn> {
        int line;
        int column;

        @Override
        public int compareTo(@NonNull LineColumn lc) {
            return line != lc.line ? line - lc.line : column - lc.column;
        }
    }

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return padRight(tree, right, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, Space right, Markers markers) {
        return new JRightPadded<>(tree, right, markers);
    }

    /**
     * Groovy 4 desugars each try-with-resource into:
     * BlockStatement -> BlockStatement -> [resourceDecl, sentinel, innerTryCatchStatement]
     */
    private static boolean isDesugaredResourceBlock(org.codehaus.groovy.ast.stmt.Statement stmt) {
        if (!(stmt instanceof BlockStatement)) return false;
        List<org.codehaus.groovy.ast.stmt.Statement> stmts = ((BlockStatement) stmt).getStatements();
        if (stmts.isEmpty() || !(stmts.get(0) instanceof BlockStatement)) return false;
        List<org.codehaus.groovy.ast.stmt.Statement> inner = ((BlockStatement) stmts.get(0)).getStatements();
        return inner.size() >= 3 && inner.get(inner.size() - 1) instanceof TryCatchStatement;
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim) {
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
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = !inMultiLineComment;
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
                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    /**
     * Get all whitespace characters of the source file between the cursor and the first non-whitespace character.
     * The cursor will be moved before first non-whitespace character.
     */
    private Space whitespace() {
        int endIndex = indexOfNextNonWhitespace(cursor, source);
        Space space = format(source, cursor, endIndex);
        cursor = endIndex;
        return space;
    }

    /**
     * Move the cursor after the token.
     */
    private @Nullable String skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (source.startsWith(token, cursor)) {
            cursor += token.length();
        }
        return token;
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends TypeTree & Expression> T typeTree(@Nullable ClassNode classNode) {
        return typeTree(classNode, false);
    }

    private <T extends TypeTree & Expression> T typeTree(@Nullable ClassNode classNode, boolean inferredType) {
        if (classNode != null && classNode.isArray()) {
            //noinspection unchecked
            return (T) arrayType(classNode);
        }
        Space prefix = whitespace();
        String maybeFullyQualified = name();
        String[] parts = maybeFullyQualified.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), part, typeMapping.type(classNode), null);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                Space identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : EMPTY;

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

        assert expr != null;
        if (classNode != null) {
            boolean isAnonymousClassWithGenericSuper = classNode instanceof InnerClassNode &&
                    classNode.getUnresolvedSuperClass() != null &&
                    classNode.getUnresolvedSuperClass().isUsingGenerics() &&
                    !classNode.getUnresolvedSuperClass().isGenericsPlaceHolder() &&
                    classNode.getGenericsTypes() == null;
            if ((isAnonymousClassWithGenericSuper || (classNode.isUsingGenerics() && !classNode.isGenericsPlaceHolder())) && sourceStartsWith("<")) {
                JContainer<Expression> typeParameters = inferredType ?
                        JContainer.build(sourceBefore("<"), singletonList(padRight(new J.Empty(randomId(), EMPTY, Markers.EMPTY), sourceBefore(">"))), Markers.EMPTY) :
                        visitTypeParameterizations(isAnonymousClassWithGenericSuper ? classNode.getUnresolvedSuperClass().getGenericsTypes() : classNode.getGenericsTypes());
                expr = new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, (NameTree) expr, typeParameters, typeMapping.type(classNode));
            }
        }
        return expr.withPrefix(prefix);
    }

    private TypeTree arrayType(ClassNode classNode) {
        ClassNode typeTree = classNode.getComponentType();
        int count = 1;
        while (typeTree.isArray()) {
            count++;
            typeTree = typeTree.getComponentType();
        }
        Space prefix = whitespace();
        TypeTree elemType = typeTree(typeTree);
        JLeftPadded<Space> dimension = sourceStartsWith("...") ? null : padLeft(sourceBefore("["), sourceBefore("]"));
        return new J.ArrayType(randomId(), prefix, Markers.EMPTY,
                count == 1 ? elemType : mapDimensions(elemType, classNode.getComponentType()),
                null,
                dimension,
                typeMapping.type(classNode));
    }

    private TypeTree mapDimensions(TypeTree baseType, ClassNode classNode) {
        if (classNode.isArray()) {
            int saveCursor = cursor;
            Space prefix = whitespace();
            if (cursor < source.length() && source.charAt(cursor) == '[') {
                JLeftPadded<Space> dimension = padLeft(sourceBefore("["), sourceBefore("]"));
                return new J.ArrayType(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        mapDimensions(baseType, classNode.getComponentType()),
                        null,
                        dimension,
                        typeMapping.type(classNode)
                );
            }
            cursor = saveCursor;
        }
        return baseType;
    }

    /**
     * Get all characters of the source file between the cursor and the given delimiter.
     * The cursor will be moved past the delimiter.
     */
    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim);
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

    /**
     * Tests if the source beginning at the current cursor starts with the specified delimiter.
     * Whitespace characters are excluded, the cursor will not be moved.
     */
    private boolean sourceStartsWith(String delimiter) {
        return source.startsWith(delimiter, indexOfNextNonWhitespace(cursor, source));
    }

    private boolean sourceStartsWith(String delimiter, String... optionalSuffixes) {
        int whitespaceIndex = indexOfNextNonWhitespace(cursor, source);
        boolean startsWith = source.startsWith(delimiter, whitespaceIndex);
        if (startsWith) {
            for (String suffix : optionalSuffixes) {
                if (source.startsWith(suffix, whitespaceIndex + delimiter.length())) {
                    return true;
                }
            }
            return optionalSuffixes.length == 0;
        }
        return false;
    }


    /**
     * Determines if the character at the specified {@code index} in the {@code source} is escaped.
     * A character is considered escaped if it is preceded by an odd number of consecutive backslashes.
     *
     * @param index The index in the string to check for escaping.
     *              This must be a valid index within the range of {@code source}.
     * @return {@code true} if the character at the given index is escaped, {@code false} otherwise.
     * @throws StringIndexOutOfBoundsException If the {@code index} is not within the valid range of the source string.
     */
    private boolean isEscaped(int index) {
        int backslashCount = 0;
        while (index > 0 && source.charAt(index - 1) == '\\') {
            backslashCount++;
            index--;
        }
        return backslashCount % 2 != 0;
    }

    /**
     * Determines if a $ character in a GString is escaped based on the delimiter type.
     * For slashy strings, $$ escapes a dollar sign and $ followed by the closing delimiter is treated as literal.
     * For other string types, backslash escaping is used.
     */
    private boolean isEscaped(int index, Delimiter delimiter) {
        if (index < 0) {
            return false;
        }

        // Slashy-type strings use different escaping: $$ for literal $ and $ before closing delimiter
        if (delimiter.isSlashyStringDelimiter() || delimiter.isDollarSlashyStringDelimiter()) {
            if (index + 1 < source.length()) {
                if (source.charAt(index + 1) == '$') {
                    return true; // $$ escapes a dollar sign
                }
                // For slashy strings (not dollar-slashy), $ before closing delimiter is also literal
                return delimiter.isSlashyStringDelimiter() && source.startsWith(delimiter.close, index + 1);
            }
            return false;
        }

        // Regular strings use backslash escaping
        return isEscaped(index);
    }

    /**
     * Returns a string that is a part of this source. The substring begins at the specified beginIndex and extends until delimiter.
     * The cursor will not be moved.
     */
    private String sourceSubstring(int beginIndex, String untilDelim) {
        int fromIndex = Math.max(beginIndex, cursor + untilDelim.length());
        int endIndex = source.indexOf(untilDelim, fromIndex);
        if (endIndex < 0) {
            throw new IllegalArgumentException(
                "Couldn't find delimiter: " + untilDelim + " with fromIndex: " + fromIndex
            );
        }
        // don't stop if the last char is escaped.
        // Fixed a potential infinite loop by correctly handling escaped delimiters in the source string.
        while (isEscaped(endIndex)) {
            endIndex = source.indexOf(untilDelim, endIndex + 1);
        }
        if (endIndex < 0) {
            throw new IllegalArgumentException(
                "Couldn't find unescaped delimiter: " + untilDelim + " with fromIndex: " + fromIndex
            );
        }
        return source.substring(beginIndex, endIndex);
    }

    /**
     * Reads a constant (literal) segment of a GString, starting at the current {@code cursor} and
     * ending just before either the next interpolation sign ({@code $}) or the closing delimiter.
     * Unlike {@link #sourceSubstring(int, String)} this does not scan the entire remaining source
     * for the closing delimiter, which is unreliable for multi-line strings that may contain
     * embedded quote sequences.
     */
    private String readConstantSegmentBeforeNextInterpolation(Delimiter delimiter) {
        int indexNextSign = source.indexOf("$", cursor);
        while (indexNextSign != -1 && isEscaped(indexNextSign, delimiter)) {
            indexNextSign = source.indexOf("$", indexNextSign + 1);
        }
        int indexCloseDelim = source.indexOf(delimiter.close, cursor);
        while (indexCloseDelim != -1 && isEscaped(indexCloseDelim)) {
            indexCloseDelim = source.indexOf(delimiter.close, indexCloseDelim + 1);
        }
        int endIndex;
        if (indexNextSign == -1) {
            endIndex = indexCloseDelim;
        } else if (indexCloseDelim == -1) {
            endIndex = indexNextSign;
        } else {
            endIndex = Math.min(indexNextSign, indexCloseDelim);
        }
        if (endIndex < 0) {
            throw new IllegalArgumentException(
                "Couldn't find end of GString constant segment starting at cursor: " + cursor
            );
        }
        return source.substring(cursor, endIndex);
    }

    private @Nullable Integer getInsideParenthesesLevel(ASTNode node) {
        Object rawIpl = node.getNodeMetaData("_INSIDE_PARENTHESES_LEVEL");
        if (rawIpl instanceof AtomicInteger) {
            // On Java 11 and newer _INSIDE_PARENTHESES_LEVEL is an AtomicInteger
            return ((AtomicInteger) rawIpl).get();
        } else if (rawIpl instanceof Integer) {
            // On Java 8 _INSIDE_PARENTHESES_LEVEL is a regular Integer
            return (Integer) rawIpl;
        }

        if (isOlderThanGroovy3()) {
            if (node instanceof ConstantExpression) {
                ConstantExpression expr = (ConstantExpression) node;
                return determineParenthesisLevel(expr, expr.getLineNumber(), expr.getLastLineNumber(), expr.getColumnNumber(), expr.getLastColumnNumber());
            } else if (node instanceof ConstructorCallExpression) {
                ConstructorCallExpression expr = (ConstructorCallExpression) node;
                return determineParenthesisLevel(expr, expr.getArguments().getLineNumber(), expr.getLineNumber(), expr.getArguments().getColumnNumber(), expr.getColumnNumber()) - 1;
            } else if (node instanceof BinaryExpression) {
                BinaryExpression expr = (BinaryExpression) node;
                return determineParenthesisLevel(expr, expr.getLeftExpression().getLineNumber(), expr.getLineNumber(), expr.getLeftExpression().getColumnNumber(), expr.getColumnNumber());
            }
        } else {
            if (node instanceof MethodCallExpression) {
                // Only for groovy 3+, because lower versions do always return `-1` for objectExpression.lineNumber / objectExpression.columnNumber
                MethodCallExpression expr = (MethodCallExpression) node;
                // The trait AST transformation rewrites implicit-this calls inside trait method bodies to use a synthetic
                // $self variable expression that has no source position; without a valid object position there's nothing
                // to scan, so skip the parenthesis-level computation.
                if (expr.getObjectExpression().getLineNumber() < 0) {
                    return null;
                }
                return determineParenthesisLevel(expr, expr.getObjectExpression().getLineNumber(), expr.getLineNumber(), expr.getObjectExpression().getColumnNumber(), expr.getColumnNumber());
            } else if (node instanceof PropertyExpression && source.charAt(indexOfNextNonWhitespace(cursor, source)) == '(') {
                // Groovy doesn't set _INSIDE_PARENTHESES_LEVEL on parenthesized PropertyExpressions like `(a.b)` but does
                // extend the column range to cover the wrapping parens. Only apply when the cursor is positioned at `(` to
                // avoid double-counting in cases where a caller already consumed the wrapping parens (e.g. map entry keys).
                PropertyExpression expr = (PropertyExpression) node;
                return determineParenthesisLevel(expr, expr.getObjectExpression().getLineNumber(), expr.getLineNumber(), expr.getObjectExpression().getColumnNumber(), expr.getColumnNumber());
            }
        }

        return null;
    }

    /**
     * @param node             the node to determine the parenthesis level of
     * @param childLineNumber  the beginning line number of the first sub node
     * @param parentLineNumber the beginning line number of the parent node
     * @param childColumn      the column on the {@code childLineNumber} line where the sub node starts
     * @param parentColumn     the column on the {@code parentLineNumber} line where the parent node starts
     * @return the level of parenthesis parsed from the source
     */
    private int determineParenthesisLevel(ASTNode node, int childLineNumber, int parentLineNumber, int childColumn, int parentColumn) {
        // Map the coordinates
        int startingLineNumber;
        int startingColumn;
        int endingLineNumber;
        int endingColumn;
        if (childLineNumber == parentLineNumber) {
            startingLineNumber = childLineNumber;
            endingLineNumber = childLineNumber;
            startingColumn = Math.min(childColumn, parentColumn);
            endingColumn = Math.max(childColumn, parentColumn);
        } else if (childLineNumber > parentLineNumber) {
            startingLineNumber = parentLineNumber;
            endingLineNumber = childLineNumber;
            startingColumn = parentColumn;
            endingColumn = childColumn;
        } else {
            startingLineNumber = childLineNumber;
            endingLineNumber = parentLineNumber;
            startingColumn = childColumn;
            endingColumn = parentColumn;
        }

        // line numbers and columns are 1-based
        int start = sourceLineNumberOffsets[startingLineNumber - 1] + startingColumn - 1;
        int end = sourceLineNumberOffsets[endingLineNumber - 1] + endingColumn - 1;

        // Determine level of parentheses by going through the source, don't count parentheses in comments, string literals and regexes
        int count = 0;
        Delimiter delimiter = null;
        for (int i = start; i < end; i++) {
            if (delimiter == null) {
                delimiter = getDelimiter(node, i);
                if (delimiter == null) {
                    if (source.charAt(i) == '(') {
                        count++;
                    } else if (source.charAt(i) == ')' && !(node instanceof ConstantExpression)) {
                        count--;
                    }
                } else {
                    i += delimiter.open.length() - 1; // skip the next chars for the rest of the delimiter
                }
            } else if (source.startsWith(delimiter.close, i)) {
                i += delimiter.close.length() - 1; // skip the next chars for the rest of the delimiter
                delimiter = null;
            }
        }
        return Math.max(count, 0);
    }

    /**
     * Grabs a {@link Delimiter} from source if cursor is right in front of a delimiter.
     * Whitespace characters are NOT excluded, the cursor will not be moved.
     */
    private @Nullable Delimiter getDelimiter(@Nullable ASTNode node, int cursor) {
        boolean isPatternOperator = source.startsWith("~", cursor);
        int c = cursor;
        if (isPatternOperator) {
            c = cursor + 1;
        }

        if (source.startsWith("$/", c)) {
            return isPatternOperator ? PATTERN_DOLLAR_SLASHY_STRING : DOLLAR_SLASHY_STRING;
        } else if (source.startsWith("\"\"\"", c)) {
            return isPatternOperator ? PATTERN_TRIPLE_DOUBLE_QUOTE_STRING : TRIPLE_DOUBLE_QUOTE_STRING;
        } else if (source.startsWith("'''", c)) {
            return isPatternOperator ? PATTERN_TRIPLE_SINGLE_QUOTE_STRING : TRIPLE_SINGLE_QUOTE_STRING;
        } else if (source.startsWith("//", c)) {
            return SINGLE_LINE_COMMENT;
        } else if (source.startsWith("/*", c)) {
            return MULTILINE_COMMENT;
        } else if (source.startsWith("/", c) && validateIsDelimiter(node, c)) {
            return isPatternOperator ? PATTERN_SLASHY_STRING : SLASHY_STRING;
        } else if (source.startsWith("\"", c)) {
            return isPatternOperator ? PATTERN_DOUBLE_QUOTE_STRING : DOUBLE_QUOTE_STRING;
        } else if (source.startsWith("'", c)) {
            return isPatternOperator ? PATTERN_SINGLE_QUOTE_STRING : SINGLE_QUOTE_STRING;
        } else if (source.startsWith("[", c)) {
            return ARRAY;
        } else if (source.startsWith("{", c)) {
            return CLOSURE;
        }

        return null;
    }

    private boolean validateIsDelimiter(@Nullable ASTNode node, int c) {
        if (node == null) {
            return true;
        }
        FindBinaryOperationVisitor visitor = new FindBinaryOperationVisitor(source.substring(c, c + 1), c, sourceLineNumberOffsets);
        node.visit(visitor);
        return !visitor.isFound();
    }

    private TypeTree visitTypeTree(ClassNode classNode) {
        return visitTypeTree(classNode, false);
    }

    private TypeTree visitTypeTree(ClassNode classNode, boolean inferredType) {
        JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(classNode.getUnresolvedName());
        if (primitiveType != null) {
            return new J.Primitive(randomId(), sourceBefore(classNode.getUnresolvedName()), Markers.EMPTY, primitiveType);
        }
        return typeTree(classNode, inferredType);
    }

    private List<J.Modifier> getModifiers() {
        List<J.Modifier> modifiers = new ArrayList<>();
        Set<String> possibleModifiers = new LinkedHashSet<>(modifierNameToType.keySet());
        String currentModifier = possibleModifiers.stream().filter(this::sourceStartsWith).findFirst().orElse(null);
        while (currentModifier != null) {
            possibleModifiers.remove(currentModifier);
            modifiers.add(new J.Modifier(randomId(), whitespace(), Markers.EMPTY, currentModifier, modifierNameToType.get(currentModifier), emptyList()));
            skip(currentModifier);
            currentModifier = possibleModifiers.stream()
                    // Try to avoid confusing a variable name with an incidentally similar modifier keyword like `def defaultPublicStaticFinal = 0`
                    .filter(modifierName -> sourceStartsWith(modifierName, "\n", " ", ")"))
                    .findFirst()
                    .orElse(null);
        }
        return modifiers;
    }

    private <G2 extends J> JRightPadded<G2> maybeSemicolon(G2 g) {
        int saveCursor = cursor;
        Space beforeSemi = whitespace();
        Semicolon semicolon = null;
        if (cursor < source.length() && source.charAt(cursor) == ';') {
            semicolon = new Semicolon(randomId());
            cursor++;
        } else {
            beforeSemi = EMPTY;
            cursor = saveCursor;
        }

        JRightPadded<G2> paddedG = JRightPadded.build(g).withAfter(beforeSemi);
        if (semicolon != null) {
            paddedG = paddedG.withMarkers(paddedG.getMarkers().add(semicolon));
        }

        return paddedG;
    }

    private String name() {
        int i = cursor;
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            boolean isVarargs = source.length() > (i + 2) && c == '.' && source.charAt(i + 1) == '.' && source.charAt(i + 2) == '.';
            if (!(isJavaIdentifierPart(c) || c == '.' || c == '*') || isVarargs) {
                break;
            }
        }
        String result = source.substring(cursor, i);
        cursor += i - cursor;
        return result;
    }

    /*
       Visit the value filled into a parameterized type, such as in a variable declaration.
       Not to be confused with the declaration of a type parameter on a class or method.

       Can contain a J.Identifier, as is the case in a typical variable declaration:
           List<String>
           Map<T, Y>
       Can contain a J.Blank, as in a diamond operator:
           List<String> s = new ArrayList< >()
       Can contain a J.FieldAccess, as in a variable declaration with fully qualified type parameterization:
           List<java.lang.String>
     */
    private JContainer<Expression> visitTypeParameterizations(GenericsType @Nullable [] genericsTypes) {
        Space prefix = sourceBefore("<");
        List<JRightPadded<Expression>> parameters;

        //noinspection ConstantConditions
        if (genericsTypes == null) {
            // Groovy compiler does not always bother to record type parameter info in places it does not care about
            Space paramPrefix = whitespace();
            if (source.charAt(cursor) == '>') {
                parameters = singletonList(JRightPadded.build(new J.Empty(randomId(), paramPrefix, Markers.EMPTY)));
            } else {
                parameters = new ArrayList<>();
                while (true) {
                    Expression param = typeTree(null).withPrefix(paramPrefix);
                    Space suffix = whitespace();
                    parameters.add(JRightPadded.build(param).withAfter(suffix));
                    if (source.charAt(cursor) == '>') {
                        skip(">");
                        break;
                    }
                    cursor++;
                    paramPrefix = whitespace();
                }
            }
        } else {
            parameters = new ArrayList<>(genericsTypes.length);
            for (int i = 0; i < genericsTypes.length; i++) {
                parameters.add(JRightPadded.build(visitTypeParameterization(genericsTypes[i]))
                        .withAfter(i < genericsTypes.length - 1 ? sourceBefore(",") : sourceBefore(">")));
            }
        }

        return JContainer.build(prefix, parameters, Markers.EMPTY);
    }

    private Expression visitTypeParameterization(GenericsType genericsType) {
        int saveCursor = cursor;
        Space prefix = whitespace();
        if (source.charAt(cursor) == '?') {
            skip("?");
            JLeftPadded<J.Wildcard.Bound> bound = null;
            NameTree boundedType = null;
            if (genericsType.getUpperBounds() != null) {
                bound = padLeft(sourceBefore("extends"), J.Wildcard.Bound.Extends);
                boundedType = visitTypeTree(genericsType.getUpperBounds()[0]);
            } else if (genericsType.getLowerBound() != null) {
                bound = padLeft(sourceBefore("super"), J.Wildcard.Bound.Super);
                boundedType = visitTypeTree(genericsType.getLowerBound());
            }
            return new J.Wildcard(randomId(), prefix, Markers.EMPTY, bound, boundedType);
        } else if (source.charAt(cursor) == '>') {
            skip(">");
            return new J.Empty(randomId(), prefix, Markers.EMPTY);
        }
        cursor = saveCursor;
        return typeTree(genericsType.getType())
                .withType(typeMapping.type(genericsType));
    }

    /*
      Visit the declaration of a type parameter as part of a class declaration or method declaration
     */
    private JContainer<J.TypeParameter> visitTypeParameters(GenericsType[] genericsTypes) {
        Space prefix = sourceBefore("<");
        List<JRightPadded<J.TypeParameter>> typeParameters = new ArrayList<>(genericsTypes.length);
        for (int i = 0; i < genericsTypes.length; i++) {
            typeParameters.add(JRightPadded.build(visitTypeParameter(genericsTypes[i]))
                    .withAfter(i < genericsTypes.length - 1 ? sourceBefore(",") : sourceBefore(">")));
        }
        return JContainer.build(prefix, typeParameters, Markers.EMPTY);
    }

    private J.TypeParameter visitTypeParameter(GenericsType genericType) {
        Space prefix = whitespace();
        Expression name = typeTree(null).withType(typeMapping.type(genericType));
        JContainer<TypeTree> bounds = null;
        if (genericType.getUpperBounds() != null) {
            Space boundsPrefix = sourceBefore("extends");
            ClassNode[] upperBounds = genericType.getUpperBounds();
            List<JRightPadded<TypeTree>> convertedBounds = new ArrayList<>(upperBounds.length);
            for (int i = 0; i < upperBounds.length; i++) {
                convertedBounds.add(JRightPadded.build(visitTypeTree(upperBounds[i]))
                        .withAfter(i < upperBounds.length - 1 ? sourceBefore("&") : EMPTY));
            }
            bounds = JContainer.build(boundsPrefix, convertedBounds, Markers.EMPTY);
        } else if (genericType.getLowerBound() != null) {
            Space boundsPrefix = sourceBefore("super");
            ClassNode lowerBound = genericType.getLowerBound();
            List<JRightPadded<TypeTree>> convertedBounds = new ArrayList<>(1);
            convertedBounds.add(JRightPadded.build(visitTypeTree(lowerBound))
                    .withAfter(EMPTY));
            bounds = JContainer.build(boundsPrefix, convertedBounds, Markers.EMPTY);
        }
        return new J.TypeParameter(randomId(), prefix, Markers.EMPTY, emptyList(), emptyList(), name, bounds);
    }

    /**
     * Sometimes the groovy compiler inserts phantom elements into argument lists and class bodies,
     * presumably to pass type information around. Other times the groovy compiler adds extra transform annotations.
     * These elements do not appear in source code and should not be represented in our LST.
     *
     * @param node possible phantom node
     * @return true if the node reports that it does have a position within the source code
     */
    private boolean appearsInSource(ASTNode node) {
        if (node instanceof AnnotationNode) {
            String name = ((AnnotationNode) node).getClassNode().getUnresolvedName();
            String[] parts = name.split("\\.");
            return sourceStartsWith("@" + name) || sourceStartsWith("@" + parts[parts.length - 1]);
        }
        if (node instanceof ConstructorNode && ((ConstructorNode) node).getDeclaringClass().isEnum()) {
            return ((ConstructorNode) node).getAnnotations(new ClassNode(Generated.class)).isEmpty();
        }

        return node.getColumnNumber() >= 0 && node.getLineNumber() >= 0 && node.getLastColumnNumber() >= 0 && node.getLastLineNumber() >= 0;
    }

    /**
     * Duplicate imports do work out of the box for import, star-import and static-import.
     * For static-star-import, this does work though.
     * The groovy compiler does only memoize the last duplicate import instead of all, so retrieve all static star imports by hand.
     */
    private List<ImportNode> getStaticStarImports(ModuleNode ast) {
        List<ImportNode> completeStaticStarImports = new ArrayList<>();
        Map<String, ImportNode> staticStarImports = ast.getStaticStarImports();
        if (!staticStarImports.isEmpty()) {
            // Take source code until last static star import for performance reasons
            int lastLineNumber = -1;
            for (ImportNode anImport : ast.getStaticStarImports().values()) {
                lastLineNumber = Math.max(lastLineNumber, anImport.getLastLineNumber());
            }
            String importSource = sourceLineNumberOffsets.length <= lastLineNumber ? source : source.substring(0, sourceLineNumberOffsets[lastLineNumber]);

            importSource = eraseComments(importSource);
            // Create a node for each `import static`, don't parse comments
            int offset = 0;
            int lineNo = 1;
            while (offset < importSource.length()) {
                int importIndex = importSource.indexOf("import", offset);
                if (importIndex == -1) break;
                lineNo += StringUtils.countOccurrences(importSource.substring(offset, importIndex), "\n");

                int maybeStaticIndex = indexOfNextNonWhitespace(importIndex + 6, importSource);
                if (!importSource.startsWith("static", maybeStaticIndex)) {
                    offset = importIndex + 6;
                    continue;
                }

                int packageBegin = indexOfNextNonWhitespace(maybeStaticIndex + 6, importSource);
                int packageEnd = packageBegin;
                while (packageEnd < importSource.length() && (isJavaIdentifierPart(importSource.charAt(packageEnd)) || importSource.charAt(packageEnd) == '.')) {
                    packageEnd++;
                }

                if (packageEnd < importSource.length() && importSource.charAt(packageEnd) == '*') {
                    ImportNode node = new ImportNode(staticStarImports.get(importSource.substring(packageBegin, packageEnd - 1)).getType());
                    node.setLineNumber(lineNo);
                    node.setColumnNumber(importIndex + 1);
                    completeStaticStarImports.add(node);
                }

                lineNo += StringUtils.countOccurrences(importSource.substring(importIndex, packageEnd), "\n");
                offset = packageEnd;
            }
        }
        return completeStaticStarImports;
    }

    // VisibleForTesting
    static String eraseComments(String importSource) {
        Matcher matcher = MULTILINE_COMMENT_REGEX.matcher(importSource);
        StringBuffer sb = new StringBuffer(); // appendReplacement requires Java 9+ for StringBuilder
        while (matcher.find()) {
            String match = matcher.group();
            String replacement = match.replaceAll("[^\\n]", " ");
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        String multiLineRemoved = sb.toString();
        return Arrays.stream(multiLineRemoved.split("\n", -1)).map(x -> x.split(SINGLE_LINE_COMMENT.open)[0])
                .collect(joining("\n"));
    }

    private DeclarationExpression transformBackToDeclarationExpression(ConstantExpression expression) {
        // We don't use `expression` but the raw source
        String str = source.substring(cursor);
        int equalsIndex = str.indexOf("=");

        int end = equalsIndex - 1;
        while (end >= 0 && isWhitespace(str.charAt(end))) {
            end--;
        }
        int start = end;
        while (start >= 0 && !isWhitespace(str.charAt(start))) {
            start--;
        }

        int startX = indexOfNextNonWhitespace(equalsIndex + 1, str);
        int endX = startX;
        Delimiter delim = getDelimiter(expression, endX);
        if (delim != null) {
            endX = str.indexOf(delim.close, endX + delim.open.length()) + 1;
        } else {
            while (endX < str.length() && (isJavaIdentifierPart(str.charAt(endX)) || str.charAt(endX) == ',' || str.charAt(endX) == '(' || str.charAt(endX) == ')')) {
                endX++;
            }
        }

        VariableExpression left = new VariableExpression(str.substring(start + 1, end + 1));
        Token operation = new Token(Types.EQUAL, "=", -1, -1);
        // Notice this give wrong type information if a non-variable is used, but at least we can parse the `right` side of the @Field declaration
        ConstantExpression right = new ConstantExpression(str.substring(startX, endX));
        DeclarationExpression declarationExpression = new DeclarationExpression(left, operation, right);
        declarationExpression.addAnnotations(singletonList(new AnnotationNode(new ClassNode(Field.class))));

        return declarationExpression;
    }

    /**
     * Static type checking for groovy is an add-on that places the type information it discovers into expression metadata.
     *
     * @param expression which may have more accurate type information in its metadata
     * @return most accurate available type of the supplied node
     */
    private static ClassNode staticType(org.codehaus.groovy.ast.expr.Expression expression) {
        ClassNode inferred = (ClassNode) expression.getNodeMetaData().get(StaticTypesMarker.INFERRED_TYPE);
        if (inferred == null) {
            return expression.getType();
        } else {
            return inferred;
        }
    }

    private static ClassNode staticType(Variable variable) {
        if (variable instanceof Parameter) {
            return staticType((Parameter) variable);
        } else if (variable instanceof org.codehaus.groovy.ast.expr.Expression) {
            return staticType((org.codehaus.groovy.ast.expr.Expression) variable);
        }
        return variable.getType();
    }

    private static ClassNode staticType(Parameter parameter) {
        ClassNode inferred = (ClassNode) parameter.getNodeMetaData().get(StaticTypesMarker.INFERRED_TYPE);
        if (inferred == null) {
            return parameter.getType();
        } else {
            return inferred;
        }
    }

    private boolean isInferred(ConstructorCallExpression ctor) {
        if (ctor.getNodeMetaData().containsKey(StaticTypesMarker.INFERRED_TYPE)) {
            return true;
        }

        ClassNode innerClass = ctor.getType();
        if (!(innerClass instanceof InnerClassNode)) {
            return innerClass.getGenericsTypes() != null && innerClass.getGenericsTypes().length == 0;
        }

        ClassNode superClass = innerClass.getUnresolvedSuperClass();
        GenericsType[] generics = superClass != null ? superClass.getGenericsTypes() : null;
        return generics != null && generics.length == 0;
    }

    private static final Map<String, J.Modifier.Type> modifierNameToType;

    static {
        modifierNameToType = new LinkedHashMap<>();
        modifierNameToType.put("def", J.Modifier.Type.LanguageExtension);
        modifierNameToType.put("var", J.Modifier.Type.LanguageExtension);
        modifierNameToType.put("public", J.Modifier.Type.Public);
        modifierNameToType.put("protected", J.Modifier.Type.Protected);
        modifierNameToType.put("private", J.Modifier.Type.Private);
        modifierNameToType.put("abstract", J.Modifier.Type.Abstract);
        modifierNameToType.put("static", J.Modifier.Type.Static);
        modifierNameToType.put("final", J.Modifier.Type.Final);
        modifierNameToType.put("volatile", J.Modifier.Type.Volatile);
        modifierNameToType.put("synchronized", J.Modifier.Type.Synchronized);
        modifierNameToType.put("transient", J.Modifier.Type.Transient);
        modifierNameToType.put("native", J.Modifier.Type.Native);
        modifierNameToType.put("sealed", J.Modifier.Type.Sealed);
        modifierNameToType.put("non-sealed", J.Modifier.Type.NonSealed);
        modifierNameToType.put("default", J.Modifier.Type.Default);
        modifierNameToType.put("strictfp", J.Modifier.Type.Strictfp);
    }
}
