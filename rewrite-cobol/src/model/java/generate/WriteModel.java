package generate;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class WriteModel extends Recipe {
    final List<J.ClassDeclaration> modelClasses;

    @Override
    public String getDisplayName() {
        return "Write the AST model";
    }

    @Override
    public String getDescription() {
        return "Expand the model into an AST with Lombok annotations, Padding classes, etc.";
    }

    Supplier<JavaParser> parser = () -> JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build();

    JavaVisitor<ExecutionContext> writeModelClass = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate valueModel = JavaTemplate.builder(this::getCursor, "" +
                "@Value " +
                "@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true) " +
                "@With").javaParser(parser).build();

        final JavaTemplate paddedModel = JavaTemplate.builder(this::getCursor, "" +
                "@ToString " +
                "@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE) " +
                "@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true) " +
                "@RequiredArgsConstructor " +
                "@AllArgsConstructor(access = AccessLevel.PRIVATE)").javaParser(parser).build();

        final JavaTemplate idField = JavaTemplate.builder(this::getCursor, "#{}@EqualsAndHashCode.Include UUID id;").javaParser(parser).build();
        final JavaTemplate prefixField = JavaTemplate.builder(this::getCursor, "#{}Space prefix;").javaParser(parser).build();
        final JavaTemplate markersField = JavaTemplate.builder(this::getCursor, "#{}Markers markers;").javaParser(parser).build();
        final JavaTemplate paddingField = JavaTemplate.builder(this::getCursor, "@Nullable @NonFinal transient WeakReference<Padding> padding;").javaParser(parser).build();

        final JavaTemplate getPadding = JavaTemplate.builder(this::getCursor, "" +
                        "public Padding getPadding() {" +
                        "    Padding p;" +
                        "    if (this.padding == null) {" +
                        "        p = new Padding(this);" +
                        "        this.padding = new WeakReference<>(p);" +
                        "    } else {" +
                        "        p = this.padding.get();" +
                        "        if (p == null || p.t != this) {" +
                        "            p = new Padding(this);" +
                        "            this.padding = new WeakReference<>(p);" +
                        "        }" +
                        "    }" +
                        "    return p;" +
                        "}")
                .build();

        final JavaTemplate paddingClass = JavaTemplate.builder(this::getCursor, "" +
                        "@RequiredArgsConstructor " +
                        "public static class Padding {" +
                        "    private final #{} t;" +
                        "}")
                .build();

        final JavaTemplate acceptMethod = JavaTemplate.builder(this::getCursor, "" +
                "@Override public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {" +
                "  return v.visit#{}(this, p);" +
                "}").javaParser(parser).build();

        /**
         * The accessors in the model class that skips the padding and return the contained element.
         */
        final JavaTemplate unwrappedPaddedGetterWither = JavaTemplate.builder(this::getCursor, "" +
                "public #{} get#{}() {" +
                "    return #{}.getElement();" +
                "}" +
                "public #{} with#{}(#{} #{}) {\n" +
                "    //noinspection ConstantConditions\n" +
                "    return getPadding().with#{}(Cobol#{}Padded.withElement(this.#{}, #{}));" +
                "}").javaParser(parser).build();

        final JavaTemplate nullableUnwrappedPaddedGetterWither = JavaTemplate.builder(this::getCursor, "" +
                "@Nullable " +
                "public #{} get#{}() {" +
                "    return #{} == null ? null : #{}.getElement();" +
                "} " +
                "public #{} with#{}(@Nullable #{} #{}) {" +
                "    if (#{} == null) {" +
                "        return this.#{} == null ? this : new #{}(#{});" +
                "    }" +
                "    return getPadding().with#{}(Cobol#{}Padded.withElement(this.#{}, #{}));" +
                "}").javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            boolean padded = c.getBody().getStatements().stream().anyMatch(this::isPadded);
            String fieldAnnotations = padded ? " @With @Getter " : "";

            c = c.withTemplate(markersField, c.getBody().getCoordinates().firstStatement(), fieldAnnotations);
            c = c.withTemplate(prefixField, c.getBody().getCoordinates().firstStatement(), fieldAnnotations);
            c = c.withTemplate(idField, c.getBody().getCoordinates().firstStatement(), fieldAnnotations);
            c = c.withTemplate(acceptMethod, c.getBody().getCoordinates().lastStatement(), classDecl.getSimpleName());

            if (padded) {
                c = c.withTemplate(paddingField, c.getBody().getCoordinates().firstStatement());
                c = c.withTemplate(getPadding, c.getBody().getCoordinates().lastStatement());
                c = c.withTemplate(paddedModel, c.getCoordinates().replaceAnnotations());
            } else {
                c = c.withTemplate(valueModel, c.getCoordinates().replaceAnnotations());
            }

            for (Statement statement : c.getBody().getStatements()) {
                if (statement instanceof J.VariableDeclarations) {
                    J.VariableDeclarations varDec = (J.VariableDeclarations) statement;
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(varDec.getType());

                    JavaType.FullyQualified elementType = null;
                    if (varDec.getTypeExpression() instanceof J.ParameterizedType) {
                        J.ParameterizedType typeExpression = (J.ParameterizedType) varDec.getTypeExpression();
                        if (typeExpression.getTypeParameters() != null) {
                            elementType = TypeUtils.asFullyQualified(typeExpression.getTypeParameters().get(0).getType());
                        }
                    }

                    if (fqn != null && elementType != null) {
                        switch (fqn.getClassName()) {
                            case "CobolContainer":
                                c = writeContainerGetterWithers(c, varDec, elementType);
                                break;
                            case "CobolLeftPadded":
                                c = writePaddedGetterWithers(c, varDec, elementType, "Left");
                                break;
                            case "CobolRightPadded":
                                c = writePaddedGetterWithers(c, varDec, elementType, "Right");
                                break;
                        }
                    }
                }
            }

            if(padded) {
                c = c.withTemplate(paddingClass, c.getBody().getCoordinates().lastStatement(), c.getSimpleName());
            }

            return c;
        }

        private J.ClassDeclaration writeContainerGetterWithers(J.ClassDeclaration c, J.VariableDeclarations statement, JavaType.FullyQualified elementType) {
            return c;
        }

        private J.ClassDeclaration writePaddedGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType,
                                                            String leftOrRight) {
            boolean nullable = !FindAnnotations.find(varDec, "@org.openrewrite.internal.lang.Nullable").isEmpty();
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = c.getSimpleName();

            if (nullable) {
                StringJoiner newModelArguments = new StringJoiner(", ");
                for (Statement statement : c.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        newModelArguments.add(statement == varDec ? "null" : ((J.VariableDeclarations) statement).getVariables()
                                .get(0).getSimpleName());
                    }
                }
                c = c.withTemplate(nullableUnwrappedPaddedGetterWither, c.getBody().getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name, name, modelTypeName, capitalizedName,
                        elementTypeName, name, name, name, modelTypeName, newModelArguments.toString(),
                        capitalizedName, leftOrRight, name, name);
            } else {
                c = c.withTemplate(unwrappedPaddedGetterWither, c.getBody().getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name,
                        modelTypeName, capitalizedName, elementTypeName, name,
                        capitalizedName, leftOrRight, name, name);
            }

            return c;
        }

        boolean isPadded(Statement statement) {
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(((J.VariableDeclarations) statement).getType());
            assert type != null;
            return type.getClassName().contains("Padded") || type.getClassName().equals("CobolContainer");
        }
    };

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (!(parent instanceof J.ClassDeclaration) || !((J.ClassDeclaration) parent).getSimpleName().equals("Cobol")) {
                    return block;
                }

                J.Block b = block.withStatements(ListUtils.map(block.getStatements(), s -> s instanceof J.ClassDeclaration &&
                        !(((J.ClassDeclaration) s).getSimpleName().equals("CompilationUnit")) ? null : s));

                b = b.withStatements(ListUtils.concatAll(b.getStatements(), ListUtils.map(modelClasses,
                        mc -> (J.ClassDeclaration) writeModelClass.visitNonNull(mc, ctx, getCursor().getParentOrThrow()))));

                return b;
            }
        };
    }
}
