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

import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    private final HideUtilityClassConstructorStyle style;

    public HideUtilityClassConstructorVisitor(HideUtilityClassConstructorStyle style) {
        this.style = style;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (UtilityClassUtilities.isRefactorableUtilityClass(c, style)) {
            /**
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
            c = maybeAutoFormat(c, (J.ClassDeclaration) new UtilityClassWithImplicitDefaultConstructorVisitor<>().visit(c, p, getCursor()), p);
            c = maybeAutoFormat(c, (J.ClassDeclaration) new UtilityClassWithExposedConstructorInspectionVisitor<>().visit(c, p, getCursor()), p);
        }
        return c;
    }

    /**
     * Adds an empty private constructor if the class has zero explicit constructors. This hides the default implicit constructor.
     */
    private static class UtilityClassWithImplicitDefaultConstructorVisitor<P> extends JavaIsoVisitor<P> {
        public UtilityClassWithImplicitDefaultConstructorVisitor() {
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
            if (UtilityClassUtilities.hasImplicitDefaultConstructor(c)) {
                c = c.withTemplate(template("private #{}() {}").build(),
                        c.getBody().getCoordinates().lastStatement(),
                        classDecl.getSimpleName()
                );
            }
            return c;
        }
    }

    /**
     * We consider a Utility Class to have an "exposed" constructor if the constructor is Public or Package-Private.
     * The constructor may be "Protected" in cases where it's desirable to subclass the Utility Class.
     */
    private static class UtilityClassWithExposedConstructorInspectionVisitor<P> extends JavaIsoVisitor<P> {
        public UtilityClassWithExposedConstructorInspectionVisitor() {
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, p);
            if (md.isConstructor() && !(md.hasModifier(J.Modifier.Type.Private) || md.hasModifier(J.Modifier.Type.Protected))) {
                // If visibility is Public, replace it with Private
                md = md.withModifiers(
                        ListUtils.map(md.getModifiers(), mod -> mod.getType() == J.Modifier.Type.Public ? mod.withType(J.Modifier.Type.Private) : mod)
                );
                // If visibility is Package-Private (no access modifier keyword), replace it with Private
                if (!md.hasModifier(J.Modifier.Type.Private)) {
                    J.Modifier mod = new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Private);
                    md = md.withModifiers(
                            ListUtils.concat(md.getModifiers(), mod)
                    );
                }
            }
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
    private static final class UtilityClassUtilities {

        private UtilityClassUtilities() {
        }

        /**
         * @param ignorableAnnotations Collection of fully-qualified Annotation name signatures, each to be used with {@link AnnotationMatcher}
         * @return true if the Class Declaration is annotated with at least one matching ignorableAnnotation
         */
        static boolean hasIgnorableAnnotation(J.ClassDeclaration c, Collection<String> ignorableAnnotations) {
            return c.getAnnotations().stream().anyMatch(
                    classAnn -> ignorableAnnotations.stream().anyMatch(
                            ignorableAnn -> new AnnotationMatcher(ignorableAnn).matches(classAnn)
                    )
            );
        }

        /**
         * @return true if this class has a {@code public static void main(String[] args)} method signature in it
         */
        static boolean hasMainMethod(J.ClassDeclaration c) {
            return c.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .filter(md -> !md.isConstructor())
                    .filter(md -> md.hasModifier(J.Modifier.Type.Public))
                    .filter(md -> md.hasModifier(J.Modifier.Type.Static))
                    .filter(md -> md.getReturnTypeExpression() != null)
                    .filter(md -> JavaType.Primitive.Void.equals(md.getReturnTypeExpression().getType()))
                    .anyMatch(md -> new MethodMatcher("* main(String)").matches(md, c));
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
            return c.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .noneMatch(J.MethodDeclaration::isConstructor);
        }

        /**
         * Convenience of determining whether a given Class Declaration is a Utility Class, and if
         * it should be refactored or not based on configuration rules such as ignorable Annotations.
         *
         * @return true if is a Utility Class, and if the class does not have ignorable Annotations, and if the class
         * does not have a "public static void main(String[] args)" method in it.
         */
        static boolean isRefactorableUtilityClass(J.ClassDeclaration c, HideUtilityClassConstructorStyle style) {
            return UtilityClassUtilities.isUtilityClass(c) &&
                    !UtilityClassUtilities.hasIgnorableAnnotation(c, style.getIgnoreIfAnnotatedBy()) &&
                    !UtilityClassUtilities.hasMainMethod(c);
        }

        static boolean isUtilityClass(J.ClassDeclaration c) {
            if (c.getImplements() != null || c.getExtends() != null) {
                return false;
            }

            List<J.MethodDeclaration> methods = c.getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast).collect(Collectors.toList());
            int staticMethodCount = UtilityClassUtilities.countStaticMethods(methods);
            if (staticMethodCount < 0) {
                return false;
            }

            List<J.VariableDeclarations> fields = c.getBody().getStatements().stream().filter(J.VariableDeclarations.class::isInstance).map(J.VariableDeclarations.class::cast).collect(Collectors.toList());
            int staticFieldCount = UtilityClassUtilities.countStaticFields(fields);
            if (staticFieldCount < 0) {
                return false;
            }

            return staticMethodCount != 0 || staticFieldCount != 0;
        }

        /**
         * @return -1 if a non-static field is found, else the count of non-private static fields.
         */
        private static int countStaticFields(Collection<J.VariableDeclarations> fields) {
            int count = 0;
            for (J.VariableDeclarations field : fields) {
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
        private static int countStaticMethods(Collection<J.MethodDeclaration> methods) {
            int count = 0;
            for (J.MethodDeclaration method : methods) {
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

/**
 * This style configuration is not intended for direct usage yet.
 * As such, this is not included in other named default styles.
 */
@Incubating(since = "7.0.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
class HideUtilityClassConstructorStyle implements JavaStyle {
    /**
     * If any of the annotation signatures are present on the utility class, the visitor will ignore operating on the class.
     * These should be {@link AnnotationMatcher}-compatible, fully-qualified annotation signature strings.
     */
    Collection<String> ignoreIfAnnotatedBy;

    static HideUtilityClassConstructorStyle hideUtilityClassConstructorStyle() {
        return new HideUtilityClassConstructorStyle(Arrays.asList(
                "@lombok.experimental.UtilityClass",
                "@lombok.Data"
        ));
    }
}
