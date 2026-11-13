/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Types a file's trees from the repository's components: its component and the methods it declares, the types it
 * names, and the calls and {@code new} expressions that resolve to a built-in function or to a component's method.
 */
public final class TypeAttribution extends ColdFusionIsoVisitor<Integer> {
    private final ComponentIndex index;
    private final String path;

    private final JavaType.@Nullable Class component;

    /**
     * The parameters and local variables of the functions being visited, innermost first. The outermost is the
     * file's own, for the variables a template or a component's pseudo-constructor assigns outside any function.
     */
    private final Deque<Scope> scopes = new ArrayDeque<>();

    /**
     * A scope's variables by their names in lower case, for those whose type is a component, a Java class or a CFML
     * value, and the names a placeholder declared, which their first assignment types.
     */
    private static final class Scope {
        final Map<String, JavaType> types = new HashMap<>();
        final Set<String> placeholders = new HashSet<>();
    }

    /**
     * ColdBox's request context, which its handlers, interceptors and views have as {@code event}, when ColdBox
     * resolves and this file is one of those.
     */
    private final JavaType.@Nullable Class requestContext;

    /**
     * ColdBox's renderer, whose methods a view or a layout calls unqualified, when this file is one.
     */
    private final JavaType.@Nullable Class renderer;

    /**
     * Whether the file is a template, whose type is its own and whose functions are its methods.
     */
    private final boolean template;

    private TypeAttribution(ComponentIndex index, Path path) {
        this.index = index;
        this.path = path.toString().replace('\\', '/');
        this.component = index.type(path);
        scopes.push(new Scope());
        this.template = !this.path.toLowerCase(Locale.ROOT).endsWith(".cfc");
        boolean view = isColdBoxView(index, this.path);
        boolean coldbox = view || !template && component != null && descendsFromColdBox(component);
        this.renderer = view ? index.resolve("coldbox.system.web.Renderer", "") : null;
        this.requestContext = coldbox ? index.resolve("coldbox.system.web.context.RequestContext", "") : null;
        if (view && requestContext != null) {
            scopes.getLast().types.put("event", requestContext);
        }
    }

    /**
     * Whether a template is a view or a layout of a ColdBox application or module, which runs in ColdBox's renderer
     * with its request context as {@code event}.
     */
    static boolean isColdBoxView(ComponentIndex index, String path) {
        String lower = "/" + path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return !lower.endsWith(".cfc") && (lower.contains("/views/") || lower.contains("/layouts/")) &&
               index.isColdBoxApplication(path);
    }

    private static boolean descendsFromColdBox(JavaType.FullyQualified type) {
        Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (JavaType.FullyQualified t = type.getSupertype(); t != null && seen.add(t); t = t.getSupertype()) {
            if (t.getFullyQualifiedName().toLowerCase(Locale.ROOT).startsWith("coldbox.system.")) {
                return true;
            }
        }
        return false;
    }

    public static Cf.CompilationUnit attribute(Cf.CompilationUnit cu, ComponentIndex index) {
        return (Cf.CompilationUnit) new TypeAttribution(index, cu.getSourcePath()).visitNonNull(cu, 0);
    }

    @Override
    public Cf.CompilationUnit visitCompilationUnit(Cf.CompilationUnit compilationUnit, Integer p) {
        return super.visitCompilationUnit(compilationUnit, p).withType(component);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (component != null && c.getType() != null &&
            c.getType().getFullyQualifiedName().equals(component.getFullyQualifiedName())) {
            c = c.withType(component).withName(c.getName().withType(component));
        }
        if (c.getExtends() != null) {
            c = c.withExtends(typed(c.getExtends()));
        }
        if (c.getImplements() != null) {
            c = c.withImplements(ListUtils.map(c.getImplements(), this::typed));
        }
        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
        Scope scope = new Scope();
        for (Statement parameter : method.getParameters()) {
            if (parameter instanceof J.VariableDeclarations) {
                J.VariableDeclarations declaration = (J.VariableDeclarations) parameter;
                String name = declaration.getVariables().get(0).getSimpleName();
                JavaType type = index.type(TypeNames.text(declaration.getTypeExpression()), path);
                if (requestContext != null && type instanceof JavaType.Unknown && "event".equalsIgnoreCase(name)) {
                    // a ColdBox handler's or interceptor's event is the request context
                    type = requestContext;
                }
                declare(scope, name, type);
            }
        }
        scopes.push(scope);
        J.MethodDeclaration m;
        try {
            m = super.visitMethodDeclaration(method, p);
        } finally {
            scopes.pop();
        }
        m = m.withReturnTypeExpression(typed(m.getReturnTypeExpression()));
        if (component != null && isComponentMethod()) {
            for (JavaType.Method declared : component.getMethods()) {
                if (declared.getName().equalsIgnoreCase(m.getSimpleName())) {
                    return m.withMethodType(declared).withName(m.getName().withType(declared));
                }
            }
        }
        return m;
    }

    /**
     * Whether the method visited is the component's own, in its body rather than in another function, or the
     * template's own, outside any function.
     */
    private boolean isComponentMethod() {
        for (Cursor c = getCursor().getParentTreeCursor(); !(c.getValue() instanceof Cf.CompilationUnit); c = c.getParentTreeCursor()) {
            Object value = c.getValue();
            if (value instanceof J.MethodDeclaration || value instanceof J.Lambda) {
                return false;
            }
            if (value instanceof J.ClassDeclaration) {
                JavaType.FullyQualified type = ((J.ClassDeclaration) value).getType();
                return component != null && type != null && type.getFullyQualifiedName().equals(component.getFullyQualifiedName());
            }
        }
        return template;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
        scopes.push(new Scope());
        try {
            return super.visitLambda(lambda, p);
        } finally {
            scopes.pop();
        }
    }

    /**
     * A variable, whose type is its initializer's when a function declares it with {@code var}.
     */
    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);
        Scope scope = scopes.peek();
        if (scope != null && !v.getModifiers().isEmpty() && "var".equalsIgnoreCase(v.getModifiers().get(0).getKeyword())) {
            for (J.VariableDeclarations.NamedVariable variable : v.getVariables()) {
                Expression initializer = variable.getInitializer();
                if (initializer != null) {
                    declare(scope, variable.getSimpleName(), initializer.getType(), ComponentIndex.isPlaceholder(initializer));
                }
            }
        }
        return v.withTypeExpression(typed(v.getTypeExpression()));
    }

    /**
     * An assignment to a local variable, {@code user = new User()} or {@code local.user = new User()}, or to a key of a
     * struct, {@code prc.user = userService.get(id)}, which gives the variable or the key the type of what it assigns.
     */
    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, Integer p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        Scope scope = scopes.peek();
        Expression variable = a.getVariable();
        JavaType assigned = a.getAssignment().getType();
        if (scope != null && variable instanceof J.Identifier) {
            declare(scope, ((J.Identifier) variable).getSimpleName(), assigned);
        } else if (scope != null && variable instanceof J.FieldAccess) {
            Expression target = ((J.FieldAccess) variable).getTarget();
            String root = target instanceof J.Identifier ? ((J.Identifier) target).getSimpleName() : null;
            if ("local".equalsIgnoreCase(root)) {
                declare(scope, ((J.FieldAccess) variable).getSimpleName(), assigned);
            } else if ("variables".equalsIgnoreCase(root)) {
                // a template's variables scope is the file's own
                declare(scopes.getLast(), ((J.FieldAccess) variable).getSimpleName(), assigned);
            } else if (ComponentIndex.sharedScopeVariable(variable) == null) {
                String key = TypeNames.text(variable);
                if (key != null) {
                    declare(scope, key, assigned);
                }
            }
        }
        return a;
    }

    @Override
    public Cf.Tag visitTag(Cf.Tag tag, Integer p) {
        Cf.Tag t = super.visitTag(tag, p);
        queried(t);
        return t;
    }

    @Override
    public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, Integer p) {
        Cf.ScriptTag s = super.visitScriptTag(scriptTag, p);
        queried(s);
        return s;
    }

    @Override
    public Cf.TagCall visitTagCall(Cf.TagCall tagCall, Integer p) {
        Cf.TagCall t = super.visitTagCall(tagCall, p);
        queried(t);
        return t;
    }

    /**
     * A query tag, which assigns its result to the variable it names as an assignment to that name would.
     */
    private void queried(J tag) {
        String name = Queries.variable(tag);
        Scope scope = scopes.peek();
        if (name == null || scope == null || !DOTTED_NAME.matcher(name).matches()) {
            return;
        }
        JavaType result = Queries.result(tag);
        int dot = name.indexOf('.');
        String root = dot < 0 ? "" : name.substring(0, dot);
        String rest = name.substring(dot + 1);
        if (dot < 0) {
            declare(scope, name, result);
        } else if ("local".equalsIgnoreCase(root) && rest.indexOf('.') < 0) {
            declare(scope, rest, result);
        } else if ("variables".equalsIgnoreCase(root) && rest.indexOf('.') < 0) {
            declare(scopes.getLast(), rest, result);
        } else if (!ComponentIndex.isSharedScope(root)) {
            declare(scope, name, result);
        }
    }

    private static final Pattern DOTTED_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{Nd}_$]*(\\.[\\p{L}_$][\\p{L}\\p{Nd}_$]*)*");

    private void declare(Scope scope, String name, @Nullable JavaType type) {
        declare(scope, name, type, false);
    }

    /**
     * Gives a variable the type a function assigns it, as long as every assignment agrees. One that assigns another
     * type, or a value whose type is not known, leaves the variable with no type from then on. A placeholder, the
     * {@code var user = ""} that old CFML declares a variable with, types the variable only until its first assignment.
     */
    private void declare(Scope scope, String name, @Nullable JavaType type, boolean placeholder) {
        String key = name.toLowerCase(Locale.ROOT);
        boolean typed = index.isComponent(type) || JavaClasses.isJava(type) || BuiltIns.isValueType(type);
        if (placeholder && !scope.types.containsKey(key)) {
            if (typed) {
                scope.types.put(key, Objects.requireNonNull(type));
            }
            scope.placeholders.add(key);
        } else if (scope.placeholders.remove(key)) {
            scope.types.put(key, typed ? Objects.requireNonNull(type) : JavaType.Unknown.getInstance());
        } else if (typed) {
            scope.types.merge(key, Objects.requireNonNull(type), (was, now) -> was == now ? was : JavaType.Unknown.getInstance());
        } else if (scope.types.containsKey(key)) {
            scope.types.put(key, JavaType.Unknown.getInstance());
        }
    }

    @Override
    public J.NewArray visitNewArray(J.NewArray newArray, Integer p) {
        J.NewArray n = super.visitNewArray(newArray, p);
        return n.getType() == null && n.getTypeExpression() == null ? n.withType(BuiltIns.ARRAY) : n;
    }

    @Override
    public Cf.StructLiteral visitStructLiteral(Cf.StructLiteral structLiteral, Integer p) {
        Cf.StructLiteral s = super.visitStructLiteral(structLiteral, p);
        return s.getType() == null ? s.withType(BuiltIns.STRUCT) : s;
    }

    /**
     * The component a variable holds, from where a function declares it or the component's own variables.
     */
    private @Nullable JavaType variableType(Expression expression) {
        if (expression instanceof J.Identifier) {
            String name = ((J.Identifier) expression).getSimpleName();
            JavaType local = local(name);
            return local != null ? local : componentVariable(name);
        }
        if (expression instanceof J.FieldAccess && ((J.FieldAccess) expression).getTarget() instanceof J.Identifier) {
            String scope = ((J.Identifier) ((J.FieldAccess) expression).getTarget()).getSimpleName().toLowerCase(Locale.ROOT);
            String name = ((J.FieldAccess) expression).getSimpleName();
            if ("arguments".equals(scope) || "local".equals(scope)) {
                return local(name);
            }
            if ("variables".equals(scope) || "this".equals(scope)) {
                JavaType variable = componentVariable(name);
                return variable != null ? variable : known(scopes.getLast().types.get(name.toLowerCase(Locale.ROOT)));
            }
        }
        // a variable of the application, session, server or request scope that a component assigns a component to
        String shared = expression instanceof J.FieldAccess ? ComponentIndex.sharedScopeVariable(expression) : null;
        if (shared != null) {
            return index.sharedScopeVariableType(shared);
        }
        // a key of a struct the function assigns, prc.user
        String key = expression instanceof J.FieldAccess ? TypeNames.text(expression) : null;
        return key == null ? null : local(key);
    }

    private @Nullable JavaType local(String name) {
        for (Scope scope : scopes) {
            JavaType type = scope.types.get(name.toLowerCase(Locale.ROOT));
            if (type != null) {
                return known(type);
            }
        }
        return null;
    }

    private static @Nullable JavaType known(@Nullable JavaType type) {
        return type instanceof JavaType.Unknown ? null : type;
    }

    private @Nullable JavaType componentVariable(String name) {
        JavaType.Variable variable = ComponentIndex.variable(component, name);
        return variable == null || !index.isComponent(variable.getType()) ? null : variable.getType();
    }

    /**
     * A call to a built-in function, or to a method of the component, of one of its supertypes, or of a component a
     * {@code new} expression makes. Built-in functions come first, since a component's function cannot replace one.
     */
    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        if (m.getMethodType() != null) {
            return m;
        }
        Expression select = m.getSelect();
        String name = m.getSimpleName();
        JavaType.Method type = null;
        if (select == null) {
            type = BuiltIns.function(name);
            JavaType result = type == null ? null : Queries.result(m);
            if (type != null && "createObject".equalsIgnoreCase(name)) {
                type = createObject(type, m);
            } else if (result != null) {
                type = returning(type, result);
            } else if (type == null) {
                type = index.method(component, name);
                if (type == null && renderer != null) {
                    // a ColdBox view or layout runs in the renderer, whose methods it calls unqualified
                    type = index.method(renderer, name);
                }
            }
        } else if (select instanceof J.Identifier) {
            String scope = ((J.Identifier) select).getSimpleName();
            if ("this".equalsIgnoreCase(scope) || "variables".equalsIgnoreCase(scope)) {
                type = index.method(component, name);
            } else if ("super".equalsIgnoreCase(scope) && component != null) {
                type = index.method(component.getSupertype(), name);
            }
        }
        if (type == null && select != null) {
            JavaType receiver = select.getType();
            if (receiver == null) {
                receiver = variableType(select);
                if (receiver != null) {
                    m = m.withSelect(TypeNames.withType(select, receiver));
                }
            }
            List<Expression> arguments = m.getArguments();
            int count = arguments.size() == 1 && arguments.get(0) instanceof J.Empty ? 0 : arguments.size();
            if (index.isComponent(receiver)) {
                type = index.method((JavaType.FullyQualified) receiver, name);
            } else if (JavaClasses.isJava(receiver)) {
                type = JavaClasses.method((JavaType.FullyQualified) receiver, name, count);
            } else {
                // a member function on one of CFML's values is the built-in function it aliases
                type = BuiltIns.member(receiver, name);
                JavaType.FullyQualified string = JavaClasses.type("java.lang.String");
                if (type == null && receiver == JavaType.Primitive.String && string != null) {
                    // a string is a java.lang.String, whose methods CFML calls as well
                    type = JavaClasses.method(string, name, count);
                }
            }
        }
        if (type != null && ("getInstance".equalsIgnoreCase(name) || "getModel".equalsIgnoreCase(name)) &&
            type.getDeclaringType().getFullyQualifiedName().toLowerCase(Locale.ROOT).startsWith("coldbox.system.")) {
            type = instance(type, m);
        }
        // the name first, since withName renames a method type to the name as the call spells it
        return type == null ? m : m.withName(m.getName().withType(type)).withMethodType(type);
    }

    /**
     * {@code createObject("component", "models.User")}, which returns the component it names, or
     * {@code createObject("java", "java.io.File")}, which returns the Java class.
     */
    private JavaType.Method createObject(JavaType.Method type, J.MethodInvocation call) {
        List<Expression> arguments = call.getArguments();
        if (arguments.size() >= 2 && arguments.get(0) instanceof J.Literal && arguments.get(1) instanceof J.Literal &&
            "java".equalsIgnoreCase(String.valueOf(((J.Literal) arguments.get(0)).getValue()))) {
            JavaType.FullyQualified javaClass = JavaClasses.type(String.valueOf(((J.Literal) arguments.get(1)).getValue()));
            return javaClass == null ? type : returning(type, javaClass);
        }
        String name = ComponentIndex.constructed(call);
        JavaType.Class created = name == null ? null : index.resolve(name, path);
        return created == null ? type : returning(type, created);
    }

    /**
     * ColdBox's {@code getInstance("UserService")}, which returns what WireBox maps the ID to.
     */
    private JavaType.Method instance(JavaType.Method type, J.MethodInvocation call) {
        List<Expression> arguments = call.getArguments();
        Expression id = arguments.isEmpty() || arguments.get(0) instanceof Cf.KeyValue ? null : arguments.get(0);
        for (Expression argument : arguments) {
            if (argument instanceof Cf.KeyValue && "name".equalsIgnoreCase(TypeNames.text(((Cf.KeyValue) argument).getKey()))) {
                id = ((Cf.KeyValue) argument).getValue();
            }
        }
        String name = id instanceof J.Literal && ((J.Literal) id).getValue() instanceof String ?
                (String) ((J.Literal) id).getValue() : null;
        JavaType.Class instance = name == null || name.isEmpty() ? null : index.injected(name, name, path);
        return instance == null ? type : returning(type, instance);
    }

    private static JavaType.Method returning(JavaType.Method type, JavaType returnType) {
        return new JavaType.Method(null, type.getFlagsBitMap(), type.getDeclaringType(), type.getName(), returnType,
                type.getParameterNames(), type.getParameterTypes(), type.getThrownExceptions(), type.getAnnotations(),
                type.getDefaultValue(), type.getDeclaredFormalTypeNames());
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
        J.NewClass n = super.visitNewClass(newClass, p);
        TypeTree clazz = n.getClazz();
        String name = TypeNames.text(clazz);
        JavaType.Class type = name == null ? null : index.resolve(name, path);
        if (clazz == null || type == null) {
            return n;
        }
        return n.withClazz(TypeNames.withType(clazz, type)).withConstructorType(index.constructor(type));
    }

    private <T extends TypeTree> @Nullable T typed(@Nullable T tree) {
        if (tree == null || tree.getType() != null && !(tree.getType() instanceof JavaType.Unknown)) {
            return tree;
        }
        String name = TypeNames.text(tree);
        if (name == null) {
            return tree;
        }
        JavaType type = index.type(name, path);
        return type instanceof JavaType.Unknown ? tree : TypeNames.withType(tree, type);
    }
}
