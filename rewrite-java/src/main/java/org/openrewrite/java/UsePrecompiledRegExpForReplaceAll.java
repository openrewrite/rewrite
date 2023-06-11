package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

public class UsePrecompiledRegExpForReplaceAll extends Recipe {

    public static final String REG_EXP_KEY = "regExp";

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + ".";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceVisitor();
    }

    public static class ReplaceVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String PATTERN_VAR = "openRewriteReplaceAllPatternVar";
        JavaTemplate matcherTemplate = JavaTemplate.builder(this::getCursor,
                        "#{any()}.matcher( #{any(java.lang.CharSequence)}).replaceAll( #{any(java.lang.String)})")
                .imports("java.util.regex.Pattern")
                .build();
        JavaTemplate compilePatternTemplate = JavaTemplate.builder(this::getCursor,
                        "private static final java.util.regex.Pattern "
                                + PATTERN_VAR + "= Pattern.compile(#{});")
                .imports("java.util.regex.Pattern")
                .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl0, ExecutionContext executionContext) {
            J.ClassDeclaration classDecl = super.visitClassDeclaration(classDecl0, executionContext);
            Object regExp = getCursor().pollNearestMessage(REG_EXP_KEY);
            return regExp == null ? classDecl :
                    classDecl.withBody(classDecl.getBody().withTemplate(compilePatternTemplate,
                            classDecl.getBody().getCoordinates().firstStatement(), regExp));
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
            JavaType.Method methodType = invocation.getMethodType();
            if (methodType == null
                    || !"java.lang.String".equals(methodType.getDeclaringType().getFullyQualifiedName())
                    || !"replaceAll".equals(invocation.getName().getSimpleName())
            ) {
                return invocation;
            }
            Object varIdentifier = new J.Identifier(
                    randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    PATTERN_VAR,
                    JavaType.buildType(Pattern.class.getName()),
                    null
            );
            getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, REG_EXP_KEY, invocation.getArguments().get(0));
            return invocation.withTemplate(matcherTemplate, invocation.getCoordinates().replace(), varIdentifier,
                    invocation.getSelect(), invocation.getArguments().get(1));
        }
    }
}
