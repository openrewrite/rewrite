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
package org.openrewrite.java.cleanup;

import org.openrewrite.Incubating;
import org.openrewrite.java.*;
import org.openrewrite.java.style.HideUtilityClassConstructorStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collection;

/**
 * HideUtilityClassConstructorVisitor will perform the following operations on a Utility Class:
 * <ul>
 *     <li>Change any Public constructors to Private</li>
 *     <li>Change any Package-Private ("Default", no modifiers) to Private</li>
 *     <li>If the Implicit Default Constructor is used (as in, no explicit constructors defined), add a Private constructor</li>
 * </ul>
 * <p>
 * HideUtilityClassConstructorVisitor will NOT perform operations on a Utility Class under these circumstances:
 * <ul>
 *     <li>Will NOT change any Protected constructors to Private</li>
 *     <li>
 *         HideUtilityClassConstructorVisitor will ignore classes with a Main method signature ({@code public static void main(String[] args)}.
 *         This prevents HideUtilityClassConstructorVisitor from generating a Private constructor on classes which only
 *         serve as application entry points, though they are technically a Utility Class.
 *     </li>
 *     <li>
 *         HideUtilityClassConstructorVisitor can be configured with a list of fully-qualified "ignorable Annotations" strings.
 *         These are used with {@link org.openrewrite.java.AnnotationMatcher} to check for the presence of annotations on the class.
 *         HideUtilityClassConstructorVisitor will ignore classes which have any of the configured Annotations.
 *         This is valuable for situations such as Lombok Utility classes, which generate Private constructors in bytecode.
 *     </li>
 * </ul>
 */
@Incubating(since = "7.0.0")
public class HideUtilityClassConstructorVisitor<P> extends JavaIsoVisitor<P> {
    private final UtilityClassMatcher utilityClassMatcher;

    public HideUtilityClassConstructorVisitor(HideUtilityClassConstructorStyle style) {
        this.utilityClassMatcher = new UtilityClassMatcher(style.getIgnoreIfAnnotatedBy());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (c.getKind() != J.ClassDeclaration.Kind.Type.Interface && !c.hasModifier(J.Modifier.Type.Abstract) && utilityClassMatcher.isRefactorableUtilityClass(c)) {
            /*
             * Note, it's a deliberate choice to have these be their own respective visitors rather than putting
             * all the logic in one visitor. It's conceptually easier to distinguish what each are doing.
             * And some linters which deal with "utility classes", such as IntelliJ, Checkstyle, etc., treat
             * "change public constructor to private" and "generate a private constructor" as different toggleable flags.
             * Here we're just saying "do both", but having the logic be implemented as separate visitors does mean
             * it could be simpler to break these out into their own visitors in the future. Maybe. For now, to defer that decision
             * and keep the API surface area small, we're keeping these private.
             *
             * But, first and foremost, the main rationale is because it's hopefully conceptually easier to distinguish the steps
             * required for HideUtilityClassConstructorVisitor to work.
             */
            c = (J.ClassDeclaration) new UtilityClassWithImplicitDefaultConstructorVisitor<>().visit(c, p, getCursor().getParentOrThrow());
            c = (J.ClassDeclaration) new UtilityClassWithExposedConstructorInspectionVisitor<>(c).visit(c, p, getCursor().getParentOrThrow());
        }
        return c;
    }

    /**
     * Adds an empty private constructor if the class has zero explicit constructors. This hides the default implicit constructor.
     */
    private static class UtilityClassWithImplicitDefaultConstructorVisitor<P> extends JavaIsoVisitor<P> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            if (UtilityClassMatcher.hasImplicitDefaultConstructor(classDecl) &&
                    !J.ClassDeclaration.Kind.Type.Enum.equals(classDecl.getKind())) {
                classDecl = classDecl.withTemplate(JavaTemplate.builder(this::getCursor, "private #{}() {}").build(),
                        classDecl.getBody().getCoordinates().lastStatement(),
                        classDecl.getSimpleName()
                );
            }
            return classDecl;
        }
    }

    /**
     * We consider a Utility Class to have an "exposed" constructor if the constructor is Public or Package-Private.
     * The constructor may be "Protected" in cases where it's desirable to subclass the Utility Class.
     */
    private static class UtilityClassWithExposedConstructorInspectionVisitor<P> extends JavaIsoVisitor<P> {
        private final J.ClassDeclaration utilityClass;

        public UtilityClassWithExposedConstructorInspectionVisitor(J.ClassDeclaration utilityClass) {
            this.utilityClass = utilityClass;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            return classDecl == utilityClass ? super.visitClassDeclaration(classDecl, p) : classDecl;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, p);
            if (md.getMethodType() == null || !md.isConstructor()
                    || (md.hasModifier(J.Modifier.Type.Private) || md.hasModifier(J.Modifier.Type.Protected) || md.getMethodType().getDeclaringType().getKind().equals(JavaType.Class.Kind.Enum))) {
                return md;
            }

            ChangeMethodAccessLevelVisitor<P> changeMethodAccessLevelVisitor = new ChangeMethodAccessLevelVisitor<>(
                    new MethodMatcher(method),
                    J.Modifier.Type.Private
            );
            md = (J.MethodDeclaration) changeMethodAccessLevelVisitor.visit(md, p, getCursor().getParentOrThrow());
            assert md != null;
            return md;
        }
    }

    /**
     * Utility class for identifying utility classes.
     * <p>
     * A Class is considered a Utility Class if it meets these rules:
     *
     * <ul>
     *     <li>A utility class contains only static methods and fields in its API</li>
     *     <li>A utility class does not have any public constructors</li>
     *     <li>A utility class does not have any {@code implements} or {@code extends} keywords.</li>
     * </ul>
     *
     * <p>
     * What's an actual example of a utility class, you ask? Well, here's one.
     * <p>
     * We are keeping this class private for the moment in order to keep the API surface area small. However,
     * there are other cleanup rules which could benefit from these, such as "make utility class final", etc.
     * Until then, however, we'll keep this private and unexposed.
     */
    private static final class UtilityClassMatcher {
        private final Collection<AnnotationMatcher> ignorableAnnotations;

        private UtilityClassMatcher(Collection<String> ignorableAnnotations) {
            this.ignorableAnnotations = new ArrayList<>(ignorableAnnotations.size());
            for (String ignorableAnnotation : ignorableAnnotations) {
                this.ignorableAnnotations.add(new AnnotationMatcher(ignorableAnnotation));
            }
        }

        boolean hasIgnorableAnnotation(J.ClassDeclaration c) {
            for (J.Annotation classAnn : c.getAllAnnotations()) {
                for (AnnotationMatcher ignorableAnn : ignorableAnnotations) {
                    if (ignorableAnn.matches(classAnn)) {
                        return true;
                    }
                }
            }
            return false;
        }

        static boolean hasMainMethod(J.ClassDeclaration c) {
            if (c.getType() == null) {
                return false;
            }
            for (Statement statement : c.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) statement;
                    if (!md.isConstructor() &&
                            md.hasModifier(J.Modifier.Type.Public) &&
                            md.hasModifier(J.Modifier.Type.Static) &&
                            md.getReturnTypeExpression() != null &&
                            JavaType.Primitive.Void.equals(md.getReturnTypeExpression().getType()) &&

                            // note that the matcher for "main(String)" will match on "main(String[]) as expected.
                            new MethodMatcher(c.getType().getFullyQualifiedName() + " main(String\\[\\])")
                                    .matches(md, c)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * If a Class Declaration has zero constructors, it uses the Implicit Default Constructor.
         * <p>
         * Keep in mind, a Class Declaration can have Explicit Default Constructors if the Constructor is declared
         * without any Access Modifier such as Public, Private, or Protected.
         *
         * @return true if there are zero explicit constructors, meaning the Class has an implicit default constructor.
         */
        static boolean hasImplicitDefaultConstructor(J.ClassDeclaration c) {
            for (Statement statement : c.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration) {
                    J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) statement;
                    if (methodDeclaration.isConstructor()) {
                        return false;
                    }
                }
            }
            return true;
        }

        boolean isRefactorableUtilityClass(J.ClassDeclaration c) {
            return UtilityClassMatcher.isUtilityClass(c) &&
                    !hasIgnorableAnnotation(c) &&
                    !UtilityClassMatcher.hasMainMethod(c);
        }

        static boolean isUtilityClass(J.ClassDeclaration c) {
            if (c.getImplements() != null || c.getExtends() != null) {
                return false;
            }

            int staticMethodCount = countStaticMethods(c);
            if (staticMethodCount < 0) {
                return false;
            }

            int staticFieldCount = countStaticFields(c);
            if (staticFieldCount < 0) {
                return false;
            }

            return staticMethodCount != 0 || staticFieldCount != 0;
        }

        /**
         * @return -1 if a non-static field is found, else the count of non-private static fields.
         */
        private static int countStaticFields(J.ClassDeclaration classDeclaration) {
            int count = 0;

            for (Statement statement : classDeclaration.getBody().getStatements()) {
                if (!(statement instanceof J.VariableDeclarations)) {
                    continue;
                }
                J.VariableDeclarations field = (J.VariableDeclarations) statement;
                if (!field.hasModifier(J.Modifier.Type.Static)) {
                    return -1;
                }
                if (field.hasModifier(J.Modifier.Type.Private)) {
                    continue;
                }
                count++;
            }

            return count;
        }

        /**
         * @return -1 if a non-static method is found, else the count of non-private static methods.
         */
        private static int countStaticMethods(J.ClassDeclaration classDeclaration) {
            int count = 0;
            for (Statement statement : classDeclaration.getBody().getStatements()) {
                if (!(statement instanceof J.MethodDeclaration)) {
                    continue;
                }

                J.MethodDeclaration method = (J.MethodDeclaration) statement;
                if (method.isConstructor()) {
                    continue;
                }
                if (!method.hasModifier(J.Modifier.Type.Static)) {
                    return -1;
                }
                if (method.hasModifier(J.Modifier.Type.Private)) {
                    continue;
                }
                count++;
            }

            return count;
        }
    }
}
