package org.openrewrite.gradle;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Optional;

import static java.util.Comparator.comparing;

public class AddDelegatesToGradleApi extends Recipe {
    private static final JavaType.ShallowClass CLOSURE_TYPE = JavaType.ShallowClass.build("groovy.lang.Closure");
    private static final JavaType.ShallowClass ACTION_TYPE = JavaType.ShallowClass.build("org.gradle.api.Action");
    private static final JavaType.ShallowClass DELEGATES_TO_TYPE = JavaType.ShallowClass.build("groovy.lang.DelegatesTo");

    @Override
    public String getDisplayName() {
        return "Add `@DelegatesTo` to the Gradle API";
    }

    @Override
    public String getDescription() {
        return "The Gradle API has methods which accept `groovy.lang.Closure`. " +
                "Typically, there is an overload which accepts an `org.gradle.api.Action`." +
                "This recipe takes the type declared as the receiver of the `Action` overload and adds an appropriate " +
                "`@groovy.lang.DelegatesTo` annotation to the `Closure` overload.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("groovy.lang.Closure");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, context);
                md = md.withParameters(ListUtils.map(md.getParameters(), it -> {
                    if (!(it instanceof J.VariableDeclarations)) {
                        return it;
                    }
                    J.VariableDeclarations param = (J.VariableDeclarations) it;
                    if (!(TypeUtils.isOfType(CLOSURE_TYPE, param.getType()) && FindAnnotations.find(param, "@groovy.lang.DelegatesTo").isEmpty())) {
                        return param;
                    }
                    if (method.getMethodType() == null || !(method.getMethodType().getDeclaringType() instanceof JavaType.Class)) {
                        return param;
                    }
                    JavaType.Class declaringClass = (JavaType.Class) method.getMethodType().getDeclaringType();
                    //noinspection OptionalGetWithoutIsPresent
                    Optional<JavaType> maybeDelegateType = declaringClass.getMethods().stream()
                            .filter(m -> m.getName().equals(method.getSimpleName())
                                    && m.getParameterTypes().stream()
                                    .anyMatch(mp -> TypeUtils.isOfType(ACTION_TYPE, mp)
                                            && mp instanceof JavaType.Parameterized))
                            .map(m -> (JavaType.Parameterized) m.getParameterTypes().stream()
                                    .filter(mp -> TypeUtils.isOfType(ACTION_TYPE, mp))
                                    .findFirst().get())
                            .filter(m -> m.getTypeParameters().size() == 1)
                            .map(m -> m.getTypeParameters().get(0))
                            .findAny();
                    if (!maybeDelegateType.isPresent()) {
                        return param;
                    }
                    JavaType delegateType = unwrapGenericTypeVariable(maybeDelegateType.get());
                    String simpleName;
                    String fullyQualifiedName;
                    if(delegateType instanceof JavaType.FullyQualified) {
                        fullyQualifiedName = ((JavaType.FullyQualified) delegateType).getFullyQualifiedName();
                        simpleName = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
                    } else {
                        return param;
                    }
                    param = param.withTemplate(
                            JavaTemplate.builder(this::getCursor, "@DelegatesTo(#{}.class)")
                                    .imports(fullyQualifiedName, DELEGATES_TO_TYPE.getFullyQualifiedName())
                                    .javaParser(() -> JavaParser.fromJavaVersion()
                                            .classpath("groovy")
                                            .build())
                                    .build(),
                            param.getCoordinates().addAnnotation(comparing(a -> 0)),
                            simpleName);
                    maybeAddImport(DELEGATES_TO_TYPE);
                    return param;
                }));
                return md;
            }
        };
    }

    private static JavaType unwrapGenericTypeVariable(JavaType type) {
        if(type instanceof JavaType.GenericTypeVariable) {
            return unwrapGenericTypeVariable(((JavaType.GenericTypeVariable) type).getBounds().get(0));
        }
        return type;
    }
}
