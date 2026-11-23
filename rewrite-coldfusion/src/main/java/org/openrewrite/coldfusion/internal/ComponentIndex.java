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
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.*;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * The components of a repository, by the paths of their files. A name that one file gives a component, as in
 * {@code extends="models.Base"} or {@code new User()}, resolves to a file the way CFML looks for one: in the
 * directory of the file naming it, then from the repository's root, then as the one file whose path ends with the
 * name. A name that no file or more than one file matches stays unresolved. The components share their types,
 * whose supertypes and methods come from the files declaring them.
 */
public final class ComponentIndex {
    private final Map<String, Component> byPath = new HashMap<>();
    private final Map<String, List<Component>> byFileName = new HashMap<>();
    private final Map<JavaType.FullyQualified, Component> byType = new IdentityHashMap<>();
    private final Map<JavaType.FullyQualified, JavaType.Method> constructors = new IdentityHashMap<>();

    /**
     * The components that mappings reach, by their logical paths, which a name reaches when no component of the
     * repository matches it.
     */
    @Nullable
    private final ComponentIndex dependencies;

    /**
     * The mappings each {@code Application.cfc} declares, by its directory, for the files under it.
     */
    private final Map<String, Map<String, String>> applications = new HashMap<>();

    /**
     * The mappings ColdBox modules declare for their own directories, which every file reaches.
     */
    private final Map<String, String> modules = new HashMap<>();

    /**
     * The directories of the ColdBox applications and modules, whose files ColdBox runs, as
     * {@link #isColdBoxApplication} finds them.
     */
    private final Set<String> coldBoxApplications = new HashSet<>();

    /**
     * The repository's own name in lower case, which its files may begin a name with, as a library deployed into a web
     * root under its name names its own components: {@code jsonutil.JSONUtil} in the {@code jsonutil} repository.
     */
    @Nullable
    private String repository;

    public ComponentIndex() {
        this(null);
    }

    public void repository(String name) {
        this.repository = name.toLowerCase(Locale.ROOT);
    }

    public ComponentIndex(@Nullable ComponentIndex dependencies) {
        this.dependencies = dependencies;
    }

    private static final class Component {
        final String path;
        final JavaType.Class type;

        @Nullable
        final String superclass;

        final List<String> interfaces;
        final List<Function> functions;

        /**
         * The names of the component's variables and the types they name, or null for none, from its properties and
         * from what its functions assign them.
         */
        final Map<String, @Nullable String> variables;

        /**
         * The names of the properties WireBox injects, and what each one's {@code inject} attribute says.
         */
        final Map<String, String> injections;

        final List<Property> properties;

        /**
         * Whether the component generates accessors for its properties, as {@code accessors="true"} and an ORM
         * entity's {@code persistent="true"} make it do.
         */
        final boolean accessors;

        /**
         * The templates it includes by a literal path, {@code <cfinclude template="helpers.cfm">}, whose functions
         * become its own.
         */
        final List<String> includes;

        /**
         * The names of the functions it assigns to other objects in lower case, as MockBox assigns {@code $} to each
         * mock with {@code obj.$ = variables.$}. Such a function runs as the other object's, so its {@code this} is
         * that object.
         */
        final Set<String> assignedToOthers;

        /**
         * Whether it sets {@code coldbox} to a struct, as ColdBox's configuration does.
         */
        final boolean configuresColdBox;

        Component(String path, JavaType.Class type, @Nullable String superclass, List<String> interfaces,
                  Declarations declared, boolean accessors) {
            this.path = path;
            this.type = type;
            this.superclass = superclass;
            this.interfaces = interfaces;
            this.functions = declared.functions;
            this.variables = declared.variables;
            this.injections = declared.injections;
            this.properties = declared.properties;
            this.accessors = accessors;
            this.includes = declared.includes;
            this.assignedToOthers = declared.assignedToOthers;
            this.configuresColdBox = declared.configuresColdBox;
        }
    }

    /**
     * What a component's body or a template declares, as {@link #collect} finds it.
     */
    private static final class Declarations {
        final List<Function> functions = new ArrayList<>();
        final Map<String, @Nullable String> variables = new LinkedHashMap<>();
        final Map<String, String> injections = new LinkedHashMap<>();
        final List<Property> properties = new ArrayList<>();
        final List<String> includes = new ArrayList<>();
        final Set<String> assignedToOthers = new HashSet<>();

        /**
         * Whether it sets {@code coldbox} to a struct, as ColdBox's configuration does.
         */
        boolean configuresColdBox;
    }

    /**
     * The templates, which are types as components are, by their paths in lower case with their extensions, as an
     * include names one.
     */
    private final Map<String, Component> templates = new HashMap<>();

    /**
     * The templates each component or template includes, whose functions are its own.
     */
    private final Map<JavaType.FullyQualified, List<JavaType.FullyQualified>> mixins = new IdentityHashMap<>();

    private static final class Function {
        final String name;

        @Nullable
        final String returnType;

        final List<String> parameterNames;
        final List<@Nullable String> parameterTypes;

        /**
         * What each of the function's returns returns, as {@link #returns(J.MethodDeclaration)} chains it, or null when
         * one of them is something a chain cannot say.
         */
        @Nullable
        final List<List<String>> returns;

        Function(String name, @Nullable String returnType, List<String> parameterNames, List<@Nullable String> parameterTypes,
                 @Nullable List<List<String>> returns) {
            this.name = name;
            this.returnType = returnType;
            this.parameterNames = parameterNames;
            this.parameterTypes = parameterTypes;
            this.returns = returns;
        }
    }

    /**
     * A property, with what its attributes say about the accessors the component generates for it.
     */
    private static final class Property {
        final String name;

        @Nullable
        final String type;

        final boolean getter;
        final boolean setter;

        /**
         * The ORM relationship it maps, as {@code one-to-many}, or null.
         */
        @Nullable
        final String relationship;

        /**
         * The component an ORM relationship relates to, from {@code cfc}.
         */
        @Nullable
        final String related;

        final String singular;

        Property(String name, @Nullable String type, boolean getter, boolean setter, @Nullable String relationship,
                 @Nullable String related, String singular) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
            this.relationship = relationship;
            this.related = related;
            this.singular = singular;
        }
    }

    /**
     * The variables of the {@code application}, {@code session}, {@code server} and {@code request} scopes that a
     * component assigns a component to, by their dotted names in lower case, with the component's name and the file
     * naming it.
     */
    private final Map<String, String[]> scopeAssignments = new HashMap<>();

    private final Map<String, JavaType.Class> scopeVariables = new HashMap<>();

    private static final Set<String> SHARED_SCOPES = new HashSet<>(Arrays.asList("application", "session", "server", "request"));

    /**
     * Adds the component a parsed file declares, or the template it is.
     */
    public void add(Path path, Cf.CompilationUnit cu) {
        String file = file(path);
        J.ClassDeclaration classDecl = declaredClass(cu);
        if (classDecl == null || classDecl.getType() == null) {
            if (!file.toLowerCase(Locale.ROOT).endsWith(".cfc")) {
                addTemplate(file, cu);
            }
            return;
        }
        JavaType.Class type = new JavaType.Class(null, Flag.Public.getBitMask(), classDecl.getType().getFullyQualifiedName(),
                classDecl.getType().getKind(), null, null, null, null, null, null, null);
        List<String> interfaces = new ArrayList<>();
        if (classDecl.getImplements() != null) {
            for (TypeTree implemented : classDecl.getImplements()) {
                String name = TypeNames.text(implemented);
                if (name != null) {
                    interfaces.add(name);
                }
            }
        }
        Declarations declared = new Declarations();
        collect(classDecl.getBody(), file, declared);
        boolean accessors = Boolean.TRUE.equals(flag(classDecl.getLeadingAnnotations(), "accessors")) ||
                            Boolean.TRUE.equals(flag(classDecl.getLeadingAnnotations(), "persistent"));
        Component component = new Component(file, type, TypeNames.text(classDecl.getExtends()), interfaces, declared,
                accessors);
        String key = key(file);
        byPath.put(key, component);
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        byFileName.computeIfAbsent(fileName, k -> new ArrayList<>()).add(component);
        byType.put(type, component);

        String directory = parent(key);
        if ("application".equals(fileName)) {
            applications.put(directory, DeclaredMappings.application(classDecl, file));
        } else if ("moduleconfig".equals(fileName)) {
            coldBoxApplications.add(directory);
            String module = DeclaredMappings.module(classDecl);
            if (module != null) {
                modules.putIfAbsent(module, directory);
            }
        }
    }

    /**
     * A template, which is a type named for its path as a component is, whose functions are its methods.
     */
    private void addTemplate(String file, Cf.CompilationUnit cu) {
        int extension = file.lastIndexOf('.');
        String name = (extension > file.lastIndexOf('/') ? file.substring(0, extension) : file).replace('/', '.');
        JavaType.Class type = new JavaType.Class(null, Flag.Public.getBitMask(), name, JavaType.FullyQualified.Kind.Class,
                null, null, null, null, null, null, null);
        Declarations declared = new Declarations();
        collect(cu, file, declared);
        // a template's variables are its file's, which attribution types in the order the file assigns them
        declared.variables.clear();
        declared.injections.clear();
        declared.properties.clear();
        Component template = new Component(file, type, null, emptyList(), declared, false);
        String key = file.toLowerCase(Locale.ROOT);
        templates.put(key, template);
        byType.put(type, template);
        // an older ColdBox application reads its configuration from XML in config/coldbox.xml.cfm, or runs from an
        // index.cfm that includes the framework's own
        String directory = parent(key);
        if (isConfigDirectory(directory) && "coldbox.xml.cfm".equals(key.substring(key.lastIndexOf('/') + 1))) {
            coldBoxApplications.add(parent(directory));
        }
        for (String include : declared.includes) {
            if (include.replace('\\', '/').toLowerCase(Locale.ROOT).endsWith("coldbox/system/coldbox.cfm")) {
                coldBoxApplications.add(directory);
            }
        }
    }

    private static String parent(String path) {
        return path.lastIndexOf('/') < 0 ? "" : path.substring(0, path.lastIndexOf('/'));
    }

    private static boolean isConfigDirectory(String directory) {
        return "config".equals(directory) || directory.endsWith("/config");
    }

    /**
     * What a component's body or a template declares outside its functions: its functions, its properties and the
     * variables it assigns components to, the shared scopes' variables it assigns, and the templates it includes. In
     * its functions too: the functions it assigns to other objects, and whether it configures ColdBox.
     */
    private void collect(J tree, String file, Declarations declared) {
        List<Function> functions = declared.functions;
        Map<String, @Nullable String> variables = declared.variables;
        Map<String, String> injections = declared.injections;
        List<Property> properties = declared.properties;
        List<String> includes = declared.includes;
        new ColdFusionIsoVisitor<Integer>() {
            @Override
            public Cf.Tag visitTag(Cf.Tag tag, Integer depth) {
                if (tag.isNamed("cfinclude")) {
                    Cf.TagAttribute template = tag.getAttribute("template");
                    if (template != null && template.getValue() instanceof J.Literal &&
                        ((J.Literal) template.getValue()).getValue() instanceof String) {
                        includes.add((String) ((J.Literal) template.getValue()).getValue());
                    }
                }
                return super.visitTag(tag, depth);
            }

            @Override
            public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, Integer depth) {
                if ("include".equalsIgnoreCase(scriptTag.getName().getSimpleName()) &&
                    scriptTag.getExpression() instanceof J.Literal &&
                    ((J.Literal) scriptTag.getExpression()).getValue() instanceof String) {
                    includes.add((String) ((J.Literal) scriptTag.getExpression()).getValue());
                }
                return super.visitScriptTag(scriptTag, depth);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer depth) {
                // a function declared in a function is not the component's
                if (depth == 0) {
                    List<String> parameterNames = new ArrayList<>();
                    List<@Nullable String> parameterTypes = new ArrayList<>();
                    for (Statement parameter : method.getParameters()) {
                        if (parameter instanceof J.VariableDeclarations) {
                            J.VariableDeclarations declaration = (J.VariableDeclarations) parameter;
                            parameterNames.add(declaration.getVariables().get(0).getSimpleName());
                            parameterTypes.add(TypeNames.text(declaration.getTypeExpression()));
                        }
                    }
                    functions.add(new Function(method.getSimpleName(), TypeNames.text(method.getReturnTypeExpression()),
                            parameterNames, parameterTypes, returns(method)));
                }
                return super.visitMethodDeclaration(method, depth + 1);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer depth) {
                String typeName = TypeNames.text(multiVariable.getTypeExpression());
                String injection = injection(multiVariable);
                if (depth == 0 && isProperty(multiVariable)) {
                    for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                        String name = variable.getSimpleName().toLowerCase(Locale.ROOT);
                        if (injection != null && typeName == null) {
                            injections.putIfAbsent(name, injection);
                        } else if (!variables.containsKey(name)) {
                            // a property with no type is still the component's, as its accessors are
                            variables.put(name, typeName);
                        }
                        List<J.Annotation> attributes = multiVariable.getLeadingAnnotations();
                        String relationship = attributeText(attributes, "fieldtype");
                        String singular = attributeText(attributes, "singularname");
                        properties.add(new Property(variable.getSimpleName(), typeName,
                                !Boolean.FALSE.equals(flag(attributes, "getter")), !Boolean.FALSE.equals(flag(attributes, "setter")),
                                relationship == null || "column".equalsIgnoreCase(relationship) || "id".equalsIgnoreCase(relationship) ?
                                        null : relationship.toLowerCase(Locale.ROOT),
                                attributeText(attributes, "cfc"), singular == null ? variable.getSimpleName() : singular));
                    }
                }
                return super.visitVariableDeclarations(multiVariable, depth);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Integer depth) {
                Expression variable = assignment.getVariable();
                String name = componentVariable(variable);
                String typeName = constructed(assignment.getAssignment());
                if (name != null && typeName != null) {
                    variables.putIfAbsent(name.toLowerCase(Locale.ROOT), typeName);
                }
                String shared = sharedScopeVariable(variable);
                if (shared != null && typeName != null) {
                    scopeAssignments.putIfAbsent(shared, new String[]{typeName, file});
                }
                String function = componentVariable(assignment.getAssignment());
                if (function != null && variable instanceof J.FieldAccess && !isOwnScope(((J.FieldAccess) variable).getTarget())) {
                    declared.assignedToOthers.add(function.toLowerCase(Locale.ROOT));
                }
                if (("coldbox".equalsIgnoreCase(TypeNames.text(variable)) || "coldbox".equalsIgnoreCase(name)) &&
                    isStruct(assignment.getAssignment())) {
                    declared.configuresColdBox = true;
                }
                return super.visitAssignment(assignment, depth);
            }
        }.visit(tree, 0);
    }

    /**
     * Whether an object is the component's own scope or the function's local one, where assigning a function does not
     * give it to another object.
     */
    private static boolean isOwnScope(Expression target) {
        String name = target instanceof J.Identifier ? ((J.Identifier) target).getSimpleName() : null;
        return "variables".equalsIgnoreCase(name) || "this".equalsIgnoreCase(name) || "local".equalsIgnoreCase(name);
    }

    private static boolean isStruct(Expression value) {
        if (value instanceof J.MethodInvocation) {
            J.MethodInvocation call = (J.MethodInvocation) value;
            return call.getSelect() == null && "structNew".equalsIgnoreCase(call.getSimpleName());
        }
        return value instanceof Cf.StructLiteral;
    }

    /**
     * Whether a declaration is a component's property, {@code property name="x"} or {@code <cfproperty name="x">}.
     */
    private static boolean isProperty(J.VariableDeclarations declaration) {
        for (J.Modifier modifier : declaration.getModifiers()) {
            if ("property".equalsIgnoreCase(modifier.getKeyword())) {
                return true;
            }
        }
        return declaration.getMarkers().findFirst(TagSyntax.class).map(tag -> "cfproperty".equalsIgnoreCase(tag.getName()))
                .orElse(false);
    }

    /**
     * Resolves each component's supertypes and variables, then types its methods, once every component is added. The
     * methods come second so that one returning a variable a supertype declares can take the variable's type.
     */
    public void link() {
        // before any supertype is set, since a handler's virtual one depends on whether ColdBox runs its application
        for (Component component : byPath.values()) {
            String key = key(component.path);
            String directory = parent(key);
            if (isConfigDirectory(directory) &&
                ("coldbox".equals(key.substring(key.lastIndexOf('/') + 1)) || configuresColdBox(component))) {
                coldBoxApplications.add(parent(directory));
            }
        }
        List<Component> all = new ArrayList<>(byPath.values());
        all.addAll(templates.values());
        for (Component component : all) {
            boolean template = !component.path.toLowerCase(Locale.ROOT).endsWith(".cfc");
            JavaType.Class supertype = component.superclass != null ? resolve(component.superclass, component.path) :
                    template ? null : virtualSupertype(component.path);
            List<JavaType.FullyQualified> included = new ArrayList<>();
            for (String include : component.includes) {
                Component target = included(include, component.path);
                if (target != null && target != component) {
                    included.add(target.type);
                }
            }
            if (!included.isEmpty()) {
                mixins.put(component.type, included);
            }
            List<JavaType.FullyQualified> interfaces = new ArrayList<>();
            for (String name : component.interfaces) {
                JavaType.Class resolved = resolve(name, component.path);
                if (resolved != null) {
                    interfaces.add(resolved);
                }
            }
            List<JavaType.Variable> members = new ArrayList<>(component.variables.size() + component.injections.size());
            for (Map.Entry<String, @Nullable String> variable : component.variables.entrySet()) {
                members.add(new JavaType.Variable(null, 0, variable.getKey(), component.type,
                        type(variable.getValue(), component.path), null));
            }
            for (Map.Entry<String, String> injection : component.injections.entrySet()) {
                JavaType.Class injected = injected(injection.getValue(), injection.getKey(), component.path);
                members.add(new JavaType.Variable(null, 0, injection.getKey(), component.type,
                        injected == null ? JavaType.Unknown.getInstance() : injected, null));
            }
            component.type.unsafeSet(null, supertype == component.type ? null : supertype, null, null, interfaces,
                    members, null);
        }
        // the functions that declare no return type, whose type is what their returns agree on
        List<Object[]> undeclared = new ArrayList<>();
        for (Component component : all) {
            List<JavaType.Method> methods = new ArrayList<>(component.functions.size());
            Set<String> declared = new HashSet<>();
            for (Function function : component.functions) {
                List<JavaType> parameterTypes = new ArrayList<>(function.parameterTypes.size());
                for (String parameterType : function.parameterTypes) {
                    parameterTypes.add(type(parameterType, component.path));
                }
                JavaType returnType = type(function.returnType, component.path);
                JavaType.Method method = new JavaType.Method(null, Flag.Public.getBitMask(), component.type, function.name,
                        returnType, function.parameterNames, parameterTypes, null, null, null, null);
                if (function.returns != null &&
                    (returnType instanceof JavaType.Unknown || "component".equalsIgnoreCase(function.returnType))) {
                    undeclared.add(new Object[]{method, component, function});
                }
                methods.add(method);
                declared.add(function.name.toLowerCase(Locale.ROOT));
            }
            if (component.accessors) {
                for (Property property : component.properties) {
                    accessors(component, property, declared, methods);
                }
            }
            component.type.unsafeSet(null, component.type.getSupertype(), null, null, component.type.getInterfaces(),
                    component.type.getMembers(), methods);
        }
        // A return type can depend on one that another function's inference gives, so infer until none changes. An
        // inferred type never changes, since every return it rests on was known, so each round but the last types
        // another function. Stopping sooner would leave what gets typed to the order of the functions.
        boolean changed = true;
        for (int round = 0; changed && round <= undeclared.size(); round++) {
            changed = false;
            for (Object[] function : undeclared) {
                JavaType.Method method = (JavaType.Method) function[0];
                JavaType inferred = inferred((Component) function[1], (Function) function[2], method);
                if (inferred != null && inferred != method.getReturnType()) {
                    method.unsafeSet(method.getDeclaringType(), inferred, method.getParameterTypes(), null, null);
                    changed = true;
                }
            }
        }
        for (Map.Entry<String, String[]> assignment : scopeAssignments.entrySet()) {
            JavaType.Class assigned = resolve(assignment.getValue()[0], assignment.getValue()[1]);
            if (assigned != null) {
                scopeVariables.put(assignment.getKey(), assigned);
            }
        }
    }

    /**
     * The accessors a component generates for a property, as Lucee and Adobe ColdFusion do, unless the component
     * declares a function by the same name: {@code getName()}, {@code setName(name)}, which returns the component,
     * and for an ORM relationship {@code hasName()}, and {@code addName(item)} and {@code removeName(item)} by its
     * {@code singularname} when it relates many.
     */
    private void accessors(Component component, Property property, Set<String> declared, List<JavaType.Method> methods) {
        String name = capitalize(property.name);
        JavaType related = property.related == null ? JavaType.Unknown.getInstance() : type(property.related, component.path);
        boolean many = property.relationship != null && property.relationship.endsWith("-to-many");
        JavaType type = property.relationship == null ? type(property.type, component.path) :
                many ? JavaType.Unknown.getInstance() : related;
        if (property.getter) {
            generate(component, "get" + name, type, emptyList(), emptyList(), declared, methods);
        }
        if (property.setter) {
            generate(component, "set" + name, component.type, singletonList(property.name), singletonList(type), declared, methods);
        }
        if (property.relationship != null) {
            generate(component, "has" + name, JavaType.Primitive.Boolean, emptyList(), emptyList(), declared, methods);
            if (many) {
                String singular = capitalize(property.singular);
                generate(component, "add" + singular, JavaType.Primitive.Void, singletonList(property.singular),
                        singletonList(related), declared, methods);
                generate(component, "remove" + singular, JavaType.Primitive.Void, singletonList(property.singular),
                        singletonList(related), declared, methods);
            }
        }
    }

    private static void generate(Component component, String name, JavaType returnType, List<String> parameterNames,
                                 List<JavaType> parameterTypes, Set<String> declared, List<JavaType.Method> methods) {
        if (declared.add(name.toLowerCase(Locale.ROOT))) {
            methods.add(new JavaType.Method(null, Flag.Public.getBitMask(), component.type, name, returnType,
                    parameterNames, parameterTypes, null, null, null, null));
        }
    }

    private static String capitalize(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * What each of a function's returns returns, outside the functions declared in it, as a chain: what it starts from,
     * then each method called on that. A chain starts from {@code this}, {@code new:X} for a component it makes,
     * {@code var:x} for one of its component's variables, {@code param:i} for a parameter, {@code call:f} for a function
     * of the component, {@code builtin:f} for a built-in function, or {@code type:array} for a literal or a query's
     * result. A step is {@code name/arguments}. A local variable returns the chain it was assigned, when every
     * assignment agrees, and a query tag assigns the variable it names. Null when a return is something no chain says,
     * or there are none.
     */
    private static @Nullable List<List<String>> returns(J.MethodDeclaration method) {
        if (method.getBody() == null) {
            return null;
        }
        Map<String, Integer> parameters = new HashMap<>();
        for (Statement parameter : method.getParameters()) {
            if (parameter instanceof J.VariableDeclarations) {
                parameters.put(((J.VariableDeclarations) parameter).getVariables().get(0).getSimpleName().toLowerCase(Locale.ROOT),
                        parameters.size());
            }
        }
        Map<String, @Nullable List<String>> locals = new HashMap<>();
        Set<String> placeholders = new HashSet<>();
        List<List<String>> returned = new ArrayList<>();
        boolean[] unknown = {false};
        new ColdFusionIsoVisitor<Integer>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration nested, Integer p) {
                return nested;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
                return lambda;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                String key = variable.getSimpleName().toLowerCase(Locale.ROOT);
                if (isPlaceholder(variable.getInitializer()) && !locals.containsKey(key)) {
                    // var result = "" returns a string, unless the function assigns result something else
                    locals.put(key, chain(Objects.requireNonNull(variable.getInitializer()), parameters, locals));
                    placeholders.add(key);
                } else {
                    local(variable.getSimpleName(), variable.getInitializer());
                }
                return super.visitVariable(variable, p);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Integer p) {
                Expression variable = assignment.getVariable();
                if (variable instanceof J.Identifier) {
                    local(((J.Identifier) variable).getSimpleName(), assignment.getAssignment());
                } else if (variable instanceof J.FieldAccess && "local".equalsIgnoreCase(TypeNames.text(((J.FieldAccess) variable).getTarget()))) {
                    local(((J.FieldAccess) variable).getSimpleName(), assignment.getAssignment());
                }
                return super.visitAssignment(assignment, p);
            }

            private void local(String name, @Nullable Expression value) {
                local(name, value == null ? null : chain(value, parameters, locals));
            }

            private void local(String name, @Nullable List<String> chain) {
                String key = name.toLowerCase(Locale.ROOT);
                if (placeholders.remove(key) || !locals.containsKey(key)) {
                    locals.put(key, chain);
                } else if (!Objects.equals(locals.get(key), chain)) {
                    locals.put(key, null);
                }
            }

            /**
             * A query tag, which assigns its result to the variable it names.
             */
            private void queried(J tag) {
                String name = Queries.variable(tag);
                if (name != null && name.regionMatches(true, 0, "local.", 0, 6)) {
                    name = name.substring(6);
                }
                if (name != null && name.indexOf('.') < 0) {
                    JavaType result = Queries.result(tag);
                    local(name, result == null ? null : singletonList(valueRoot(result)));
                }
            }

            @Override
            public J.Return visitReturn(J.Return aReturn, Integer p) {
                returned(aReturn.getExpression());
                return aReturn;
            }

            @Override
            public Cf.Tag visitTag(Cf.Tag tag, Integer p) {
                if (tag.isNamed("cfreturn")) {
                    returned(tag.getExpression() instanceof Expression ? (Expression) tag.getExpression() : null);
                }
                queried(tag);
                return super.visitTag(tag, p);
            }

            @Override
            public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, Integer p) {
                queried(scriptTag);
                return super.visitScriptTag(scriptTag, p);
            }

            @Override
            public Cf.TagCall visitTagCall(Cf.TagCall tagCall, Integer p) {
                queried(tagCall);
                return super.visitTagCall(tagCall, p);
            }

            private void returned(@Nullable Expression value) {
                if (value == null) {
                    return;
                }
                List<String> chain = chain(value, parameters, locals);
                if (chain == null) {
                    unknown[0] = true;
                } else {
                    returned.add(chain);
                }
            }
        }.visit(method.getBody(), 0);
        return unknown[0] || returned.isEmpty() ? null : returned;
    }

    /**
     * Whether a declaration's initializer only declares its variable, as the empty string or the zero does that old
     * CFML gives each variable it declares at the top of a function, before it assigns the variable its value.
     */
    public static boolean isPlaceholder(@Nullable Expression initializer) {
        if (!(initializer instanceof J.Literal)) {
            return false;
        }
        Object value = ((J.Literal) initializer).getValue();
        return "".equals(value) || value instanceof Number && ((Number) value).doubleValue() == 0;
    }

    /**
     * The root of a chain for one of CFML's values, as {@code type:query} for {@code cfml.Query}.
     */
    private static String valueRoot(JavaType value) {
        return "type:" + ((JavaType.FullyQualified) value).getClassName().toLowerCase(Locale.ROOT);
    }

    /**
     * The chain an expression is, as {@link #returns(J.MethodDeclaration)} describes it, or null.
     */
    private static @Nullable List<String> chain(Expression expression, Map<String, Integer> parameters,
                                               Map<String, @Nullable List<String>> locals) {
        if (expression instanceof J.Parentheses && ((J.Parentheses<?>) expression).getTree() instanceof Expression) {
            return chain((Expression) ((J.Parentheses<?>) expression).getTree(), parameters, locals);
        }
        String constructed = constructed(expression);
        if (constructed != null) {
            return singletonList("new:" + constructed);
        }
        if (expression instanceof J.Identifier) {
            String name = ((J.Identifier) expression).getSimpleName().toLowerCase(Locale.ROOT);
            if ("this".equals(name)) {
                return singletonList("this");
            }
            if (parameters.containsKey(name)) {
                return singletonList("param:" + parameters.get(name));
            }
            return locals.containsKey(name) ? locals.get(name) : singletonList("var:" + name);
        }
        String variable = componentVariable(expression);
        if (variable != null) {
            return singletonList("var:" + variable.toLowerCase(Locale.ROOT));
        }
        if (expression instanceof J.FieldAccess && "arguments".equalsIgnoreCase(TypeNames.text(((J.FieldAccess) expression).getTarget()))) {
            Integer index = parameters.get(((J.FieldAccess) expression).getSimpleName().toLowerCase(Locale.ROOT));
            return index == null ? null : singletonList("param:" + index);
        }
        if (expression instanceof J.FieldAccess && "local".equalsIgnoreCase(TypeNames.text(((J.FieldAccess) expression).getTarget()))) {
            return locals.get(((J.FieldAccess) expression).getSimpleName().toLowerCase(Locale.ROOT));
        }
        if (expression instanceof J.NewArray) {
            return singletonList("type:array");
        }
        if (expression instanceof Cf.StructLiteral) {
            return singletonList("type:struct");
        }
        if (expression instanceof J.Literal && ((J.Literal) expression).getValue() instanceof String ||
            expression instanceof Cf.StringTemplate ||
            expression instanceof Cf.Binary && ((Cf.Binary) expression).getOperator() == Cf.Binary.Type.Concat) {
            return singletonList("type:string");
        }
        if (expression instanceof J.MethodInvocation) {
            J.MethodInvocation call = (J.MethodInvocation) expression;
            List<Expression> arguments = call.getArguments();
            int count = arguments.size() == 1 && arguments.get(0) instanceof J.Empty ? 0 : arguments.size();
            if (call.getSelect() == null) {
                JavaType queried = Queries.result(call);
                if (queried != null) {
                    return singletonList(valueRoot(queried));
                }
                return singletonList((BuiltIns.function(call.getSimpleName()) != null ? "builtin:" : "call:") +
                                     call.getSimpleName().toLowerCase(Locale.ROOT));
            }
            List<String> receiver = chain(call.getSelect(), parameters, locals);
            if (receiver == null) {
                return null;
            }
            List<String> chain = new ArrayList<>(receiver);
            chain.add(call.getSimpleName().toLowerCase(Locale.ROOT) + "/" + count);
            return chain;
        }
        return null;
    }

    /**
     * The template an include names from the file at {@code caller}: a path from the caller's directory, as
     * {@code helpers/udfs.cfm}, or from the web root, as {@code /includes/udfs.cfm}, which a mapping, the application's
     * root, the repository's root or the repository's own name reaches. Null for a template no file is.
     */
    private @Nullable Component included(String include, String caller) {
        String path = include.trim().replace('\\', '/');
        if (path.isEmpty() || path.indexOf('#') >= 0) {
            return null;
        }
        String callerKey = caller.toLowerCase(Locale.ROOT);
        String directory = callerKey.lastIndexOf('/') < 0 ? "" : callerKey.substring(0, callerKey.lastIndexOf('/'));
        if (!path.startsWith("/")) {
            String relative = DeclaredMappings.directory(directory.isEmpty() ? path : directory + "/" + path);
            return relative == null ? null : template(relative);
        }
        String relative = DeclaredMappings.directory(path);
        if (relative == null) {
            return null;
        }
        String application = application(directory);
        List<Map<String, String>> mappings = new ArrayList<>();
        if (application != null) {
            mappings.add(applications.get(application));
        }
        mappings.add(modules);
        if (dependencies != null) {
            mappings.add(dependencies.modules);
        }
        for (Map<String, String> declared : mappings) {
            for (int end = relative.lastIndexOf('/'); end > 0; end = relative.lastIndexOf('/', end - 1)) {
                String target = declared.get(relative.substring(0, end));
                if (target != null) {
                    Component found = template(join(target, relative.substring(end + 1)));
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        Component found = application == null || application.isEmpty() ? null : template(join(application, relative));
        if (found == null) {
            found = template(relative);
        }
        if (found == null && repository != null && relative.startsWith(repository + "/")) {
            found = template(relative.substring(repository.length() + 1));
        }
        return found;
    }

    private @Nullable Component template(String key) {
        Component found = templates.get(key);
        return found != null || dependencies == null ? found : dependencies.templates.get(key);
    }

    /**
     * What ColdBox makes a component that extends nothing extend, by its virtual inheritance: a handler in a
     * {@code handlers} directory is an {@code EventHandler}, and an interceptor in an {@code interceptors} directory an
     * {@code Interceptor}. Null outside a ColdBox application, when ColdBox does not resolve, or for ColdBox's own
     * components.
     */
    private JavaType.@Nullable Class virtualSupertype(String path) {
        String key = key(path);
        if (key.startsWith("coldbox/system/") || !isColdBoxApplication(path)) {
            return null;
        }
        List<String> directories = Arrays.asList(key.split("/"));
        directories = directories.subList(0, directories.size() - 1);
        return directories.contains("handlers") ? resolve("coldbox.system.EventHandler", "") :
                directories.contains("interceptors") ? resolve("coldbox.system.Interceptor", "") : null;
    }

    /**
     * Whether a component sets {@code coldbox} to a struct, or one of its supertypes does, as the configuration of an
     * application of a framework built on ColdBox extends the framework's.
     */
    private boolean configuresColdBox(Component component) {
        Set<Component> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Component c = component; c != null && seen.add(c); ) {
            if (c.configuresColdBox) {
                return true;
            }
            JavaType.Class supertype = c.superclass == null ? null : resolve(c.superclass, c.path);
            c = supertype == null ? null : component(supertype);
        }
        return false;
    }

    /**
     * Whether ColdBox runs the file at a path: whether a directory above it holds a ColdBox module's
     * {@code ModuleConfig.cfc}, ColdBox's configuration in its {@code config} directory, or a template that includes
     * ColdBox's {@code coldbox.cfm}, as an older application's {@code index.cfm} does. The configuration is a
     * {@code ColdBox.cfc}, a {@code coldbox.xml.cfm}, or a component that sets {@code coldbox} to a struct or extends one
     * that does. Where ColdBox only resolves, as it may in an application of another framework, its conventions do not
     * hold.
     */
    boolean isColdBoxApplication(String path) {
        String key = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String directory = parent(key); ; directory = parent(directory)) {
            if (coldBoxApplications.contains(directory)) {
                return true;
            }
            if (directory.isEmpty()) {
                return false;
            }
        }
    }

    /**
     * The type a function returns when its declaration says nothing, when what each of its returns evaluates to
     * agrees, or null.
     */
    private @Nullable JavaType inferred(Component component, Function function, JavaType.Method method) {
        if (function.returns == null) {
            return null;
        }
        JavaType agreed = null;
        for (List<String> chain : function.returns) {
            JavaType type = evaluate(component, method, chain);
            if (type == null || agreed != null && agreed != type) {
                return null;
            }
            agreed = type;
        }
        return agreed;
    }

    /**
     * What a chain evaluates to with the types known so far, or null for a type that is not a component, a Java
     * class or one of CFML's values.
     */
    private @Nullable JavaType evaluate(Component component, JavaType.Method method, List<String> chain) {
        String root = chain.get(0);
        JavaType type;
        if ("this".equals(root)) {
            // a function the component assigns to other objects returns the object it is called on, which has no name here
            type = component.assignedToOthers.contains(method.getName().toLowerCase(Locale.ROOT)) ? null : component.type;
        } else if (root.startsWith("new:")) {
            type = resolve(root.substring(4), component.path);
        } else if (root.startsWith("var:")) {
            JavaType.Variable variable = variable(component.type, root.substring(4));
            type = variable == null ? null : variable.getType();
        } else if (root.startsWith("param:")) {
            int index = Integer.parseInt(root.substring(6));
            type = index < method.getParameterTypes().size() ? method.getParameterTypes().get(index) : null;
        } else if (root.startsWith("call:")) {
            JavaType.Method called = method(component.type, root.substring(5));
            type = called == null ? null : called.getReturnType();
        } else if (root.startsWith("builtin:")) {
            JavaType.Method builtIn = BuiltIns.function(root.substring(8));
            type = builtIn == null ? null : builtIn.getReturnType();
        } else {
            type = BuiltIns.javaTypeOrNull(root.substring(5));
        }
        for (int i = 1; i < chain.size() && type != null; i++) {
            String step = chain.get(i);
            int slash = step.lastIndexOf('/');
            String name = step.substring(0, slash);
            JavaType.Method called = isComponent(type) ? method((JavaType.FullyQualified) type, name) :
                    JavaClasses.isJava(type) ? JavaClasses.method((JavaType.FullyQualified) type, name, Integer.parseInt(step.substring(slash + 1))) :
                    BuiltIns.member(type, name);
            type = called == null ? null : called.getReturnType();
        }
        return isComponent(type) || JavaClasses.isJava(type) || BuiltIns.isValueType(type) ? type : null;
    }

    /**
     * What a declaration's attribute says is true or false, {@code accessors="true"} or a bare {@code accessors}, or
     * null when the declaration does not have it.
     */
    private static @Nullable Boolean flag(List<J.Annotation> attributes, String name) {
        for (J.Annotation attribute : attributes) {
            if (name.equalsIgnoreCase(attribute.getSimpleName())) {
                List<Expression> arguments = attribute.getArguments();
                if (arguments == null || arguments.isEmpty()) {
                    return true;
                }
                Expression value = arguments.get(0);
                String text = value instanceof J.Literal ? String.valueOf(((J.Literal) value).getValue()) : TypeNames.text(value);
                return text == null ? null : "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
            }
        }
        return null;
    }

    private static @Nullable String attributeText(List<J.Annotation> attributes, String name) {
        for (J.Annotation attribute : attributes) {
            if (name.equalsIgnoreCase(attribute.getSimpleName())) {
                List<Expression> arguments = attribute.getArguments();
                if (arguments == null || arguments.isEmpty()) {
                    return null;
                }
                Expression value = arguments.get(0);
                if (value instanceof J.Literal) {
                    Object literal = ((J.Literal) value).getValue();
                    return literal instanceof String ? (String) literal : null;
                }
                return TypeNames.text(value);
            }
        }
        return null;
    }

    /**
     * The dotted name of an {@code application}, {@code session}, {@code server} or {@code request} variable an
     * assignment sets, in lower case, or null.
     */
    static @Nullable String sharedScopeVariable(Expression variable) {
        if (!(variable instanceof J.FieldAccess)) {
            return null;
        }
        String name = TypeNames.text(variable);
        if (name == null) {
            return null;
        }
        int dot = name.indexOf('.');
        return dot > 0 && SHARED_SCOPES.contains(name.substring(0, dot).toLowerCase(Locale.ROOT)) ?
                name.toLowerCase(Locale.ROOT) : null;
    }

    static boolean isSharedScope(String name) {
        return SHARED_SCOPES.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * The component a shared scope's variable holds, {@code application.userService}, when a component assigns it
     * one.
     */
    JavaType.@Nullable Class sharedScopeVariableType(String dottedName) {
        JavaType.Class type = scopeVariables.get(dottedName.toLowerCase(Locale.ROOT));
        return type != null || dependencies == null ? type : dependencies.sharedScopeVariableType(dottedName);
    }

    /**
     * The name of the component's variable an assignment sets: {@code variables.x}, {@code this.x}.
     */
    static @Nullable String componentVariable(Expression variable) {
        if (variable instanceof J.FieldAccess && ((J.FieldAccess) variable).getTarget() instanceof J.Identifier) {
            String scope = ((J.Identifier) ((J.FieldAccess) variable).getTarget()).getSimpleName();
            if ("variables".equalsIgnoreCase(scope) || "this".equalsIgnoreCase(scope)) {
                return ((J.FieldAccess) variable).getSimpleName();
            }
        }
        return null;
    }

    /**
     * The name of the component an expression makes: {@code new User()} or {@code createObject("component", "User")}.
     */
    static @Nullable String constructed(Expression expression) {
        if (expression instanceof J.NewClass) {
            return TypeNames.text(((J.NewClass) expression).getClazz());
        }
        if (expression instanceof J.MethodInvocation) {
            J.MethodInvocation call = (J.MethodInvocation) expression;
            // a component's init returns the component, by CFML's convention
            if ("init".equalsIgnoreCase(call.getSimpleName()) && call.getSelect() != null) {
                return constructed(call.getSelect());
            }
            List<Expression> arguments = call.getArguments();
            if (call.getSelect() == null && "createObject".equalsIgnoreCase(call.getSimpleName()) && arguments.size() >= 2 &&
                arguments.get(0) instanceof J.Literal && "component".equalsIgnoreCase(String.valueOf(((J.Literal) arguments.get(0)).getValue())) &&
                arguments.get(1) instanceof J.Literal && ((J.Literal) arguments.get(1)).getValue() instanceof String) {
                return (String) ((J.Literal) arguments.get(1)).getValue();
            }
        }
        return null;
    }

    /**
     * A variable a component or one of its supertypes has by a name, in any case.
     */
    static JavaType.@Nullable Variable variable(JavaType.@Nullable FullyQualified type, String name) {
        Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (JavaType.FullyQualified t = type; t != null && seen.add(t); t = t.getSupertype()) {
            for (JavaType.Variable member : t.getMembers()) {
                if (member.getName().equalsIgnoreCase(name)) {
                    return member;
                }
            }
        }
        return null;
    }

    /**
     * The first name in a component's chain of supertypes that does not resolve, as {@code testbox.system.BaseSpec},
     * or null when every one resolves. It is what a mapping would be missing.
     */
    public @Nullable String unresolvedSupertype(JavaType.@Nullable FullyQualified type) {
        Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (JavaType.FullyQualified t = type; t != null && seen.add(t); t = t.getSupertype()) {
            Component component = component(t);
            if (component == null) {
                return null;
            }
            if (component.superclass != null && t.getSupertype() == null) {
                return component.superclass;
            }
        }
        return null;
    }

    private @Nullable Component component(JavaType.FullyQualified type) {
        Component component = byType.get(type);
        return component != null || dependencies == null ? component : dependencies.component(type);
    }

    /**
     * The type of the component a file declares, or null when it declares none.
     */
    JavaType.@Nullable Class type(Path path) {
        String file = file(path);
        Component component = file.toLowerCase(Locale.ROOT).endsWith(".cfc") ? byPath.get(key(file)) :
                templates.get(file.toLowerCase(Locale.ROOT));
        return component == null ? null : component.type;
    }

    boolean isComponent(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified &&
               (byType.containsKey(type) || dependencies != null && dependencies.isComponent(type));
    }

    /**
     * The component a name gives from the file at {@code caller}, or null when no one file matches it. It looks where
     * CFML looks: in the caller's directory, through the mappings the caller's application and modules declare, from
     * the application's root and from the repository's. A dependency's component is at the path the mapping that
     * reaches it gives. Last, it takes the one component of the repository whose path ends with the name.
     */
    JavaType.@Nullable Class resolve(String name, String caller) {
        if (name.isEmpty() || name.startsWith(".") || name.endsWith(".") || name.contains("..")) {
            return null;
        }
        String relative = name.replace('.', '/').toLowerCase(Locale.ROOT);
        String callerKey = key(caller);
        String directory = callerKey.lastIndexOf('/') < 0 ? "" : callerKey.substring(0, callerKey.lastIndexOf('/'));
        Component found = directory.isEmpty() ? null : find(join(directory, relative));
        if (found == null) {
            String application = application(directory);
            found = mapped(relative, application == null ? null : applications.get(application));
            if (found == null) {
                found = mapped(relative, modules);
            }
            if (found == null && dependencies != null) {
                found = mapped(relative, dependencies.modules);
            }
            if (found == null && repository != null && relative.startsWith(repository + "/")) {
                found = byPath.get(relative.substring(repository.length() + 1));
            }
            if (found == null && application != null && !application.isEmpty()) {
                found = find(join(application, relative));
            }
        }
        if (found == null) {
            found = find(relative);
        }
        if (found == null) {
            for (Component component : byFileName.getOrDefault(relative.substring(relative.lastIndexOf('/') + 1), emptyList())) {
                if (key(component.path).endsWith("/" + relative)) {
                    if (found != null) {
                        return null;
                    }
                    found = component;
                }
            }
        }
        return found == null ? null : found.type;
    }

    /**
     * The component at a path, in the repository or else in a dependency.
     */
    private @Nullable Component find(String key) {
        Component found = byPath.get(key);
        return found != null || dependencies == null ? found : dependencies.byPath.get(key);
    }

    /**
     * The component a name reaches through the longest logical path it starts with that a mapping declares.
     */
    private @Nullable Component mapped(String relative, @Nullable Map<String, String> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        for (int end = relative.lastIndexOf('/'); end > 0; end = relative.lastIndexOf('/', end - 1)) {
            String target = mappings.get(relative.substring(0, end));
            if (target != null) {
                Component found = find(join(target, relative.substring(end + 1)));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * The directory of the {@code Application.cfc} nearest above a directory, whose application its files are in, or
     * null when none is.
     */
    private @Nullable String application(String directory) {
        for (String d = directory; ; d = d.lastIndexOf('/') < 0 ? "" : d.substring(0, d.lastIndexOf('/'))) {
            if (applications.containsKey(d)) {
                return d;
            }
            if (d.isEmpty()) {
                return null;
            }
        }
    }

    private static String join(String directory, String relative) {
        return directory.isEmpty() ? relative : directory + "/" + relative;
    }

    /**
     * The type a declaration names from the file at {@code caller}: CFML's own, a component's, or an unknown type.
     */
    JavaType type(@Nullable String name, String caller) {
        if (name == null) {
            return JavaType.Unknown.getInstance();
        }
        JavaType type = BuiltIns.javaTypeOrNull(name);
        if (type == null && !BuiltIns.isCfmlType(name)) {
            type = resolve(name, caller);
        }
        return type == null ? JavaType.Unknown.getInstance() : type;
    }

    /**
     * The method a component or one of its supertypes declares by a name, in any case.
     */
    JavaType.@Nullable Method method(JavaType.@Nullable FullyQualified type, String name) {
        Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<JavaType.FullyQualified> interfaces = new ArrayList<>();
        for (JavaType.FullyQualified t = type; t != null && seen.add(t); t = t.getSupertype()) {
            JavaType.Method found = declared(t, name);
            if (found == null) {
                found = mixedIn(t, name, seen);
            }
            if (found != null) {
                return found;
            }
            interfaces.addAll(t.getInterfaces());
        }
        // an interface declares what a component implementing it has, as a value of the interface's type does
        for (int i = 0; i < interfaces.size(); i++) {
            JavaType.FullyQualified anInterface = interfaces.get(i);
            if (seen.add(anInterface)) {
                for (JavaType.Method method : anInterface.getMethods()) {
                    if (method.getName().equalsIgnoreCase(name)) {
                        return method;
                    }
                }
                interfaces.addAll(anInterface.getInterfaces());
            }
        }
        return null;
    }

    private static JavaType.@Nullable Method declared(JavaType.FullyQualified type, String name) {
        for (JavaType.Method method : type.getMethods()) {
            if (method.getName().equalsIgnoreCase(name)) {
                return method;
            }
        }
        return null;
    }

    /**
     * The method a template a type includes declares by a name, or one that template includes, as CFML makes the
     * functions an include declares the includer's.
     */
    private JavaType.@Nullable Method mixedIn(JavaType.FullyQualified type, String name, Set<JavaType.FullyQualified> seen) {
        for (JavaType.FullyQualified mixin : mixins(type)) {
            if (seen.add(mixin)) {
                JavaType.Method found = declared(mixin, name);
                if (found == null) {
                    found = mixedIn(mixin, name, seen);
                }
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    List<JavaType.FullyQualified> mixins(JavaType.FullyQualified type) {
        List<JavaType.FullyQualified> own = mixins.get(type);
        return own != null ? own : dependencies == null ? emptyList() : dependencies.mixins(type);
    }

    /**
     * Whether a function's component assigns it to other objects, as MockBox assigns {@code $} to each mock.
     */
    boolean isAssignedToOtherObjects(JavaType.Method function) {
        Component component = component(function.getDeclaringType());
        return component != null && component.assignedToOthers.contains(function.getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Whether a type is a component or a template of a dependency rather than of the repository.
     */
    boolean isDependency(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified && !byType.containsKey(type) && isComponent(type);
    }

    /**
     * Every component of the repository and of its dependencies, without the templates.
     */
    List<JavaType.FullyQualified> components() {
        List<JavaType.FullyQualified> components = new ArrayList<>();
        for (ComponentIndex index = this; index != null; index = index.dependencies) {
            for (Component component : index.byPath.values()) {
                components.add(component.type);
            }
        }
        return components;
    }

    /**
     * Gives a function of the repository a return type where it has none. A dependency's functions are shared by every
     * parse that maps it, so they keep theirs.
     */
    boolean setReturnType(JavaType.Method function, JavaType returnType) {
        if (!isOwnFunction(function) || !(function.getReturnType() instanceof JavaType.Unknown) ||
            returnType instanceof JavaType.Unknown) {
            return false;
        }
        function.unsafeSet(function.getDeclaringType(), returnType, function.getParameterTypes(), null, null);
        return true;
    }

    /**
     * Gives a parameter of a function of the repository a component's type where it has none.
     */
    boolean setParameterType(JavaType.Method function, int parameter, JavaType component) {
        List<JavaType> parameterTypes = new ArrayList<>(function.getParameterTypes());
        Component given = component instanceof JavaType.FullyQualified ? component((JavaType.FullyQualified) component) : null;
        if (!isOwnFunction(function) || parameter < 0 || parameter >= parameterTypes.size() ||
            !(parameterTypes.get(parameter) instanceof JavaType.Unknown) || given == null ||
            !given.path.toLowerCase(Locale.ROOT).endsWith(".cfc")) {
            return false;
        }
        parameterTypes.set(parameter, component);
        function.unsafeSet(function.getDeclaringType(), function.getReturnType(), parameterTypes, null, null);
        if ("init".equalsIgnoreCase(function.getName())) {
            // what new calls takes init's parameters, and a component inherits its supertype's init
            constructors.clear();
        }
        return true;
    }

    private boolean isOwnFunction(JavaType.Method function) {
        Component component = byType.get(function.getDeclaringType());
        if (component != null) {
            for (JavaType.Method method : component.type.getMethods()) {
                if (method == function) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * What a property's {@code inject} attribute says WireBox injects, or null when it has none. A bare
     * {@code inject} says the property's own name.
     */
    static @Nullable String injection(J.VariableDeclarations property) {
        for (J.Annotation annotation : property.getLeadingAnnotations()) {
            if ("inject".equalsIgnoreCase(annotation.getSimpleName())) {
                List<Expression> arguments = annotation.getArguments();
                if (arguments == null || arguments.isEmpty()) {
                    return "";
                }
                Expression value = arguments.get(0);
                if (value instanceof J.Literal) {
                    Object literal = ((J.Literal) value).getValue();
                    return literal instanceof String ? (String) literal : null;
                }
                return TypeNames.text(value);
            }
        }
        return null;
    }

    /**
     * The component WireBox injects, by its convention that an ID names a component: {@code inject="UserService"} or
     * {@code inject="model:UserService"} is the one component named {@code UserService}, preferring one in a
     * {@code models} directory, and {@code UserService@cbauth} the one in the {@code cbauth} module. A dotted ID is a
     * component's path. The {@code coldbox}, {@code wirebox}, {@code cachebox} and {@code logbox} IDs are those
     * frameworks' own objects, and the DSL's other IDs whose object has one type are that type, as
     * {@code coldbox:flash} is ColdBox's flash scope, when ColdBox is in the repository or a mapping.
     */
    JavaType.@Nullable Class injected(String injection, String property, String caller) {
        String id = injection.trim();
        int colon = id.indexOf(':');
        if (colon >= 0) {
            String namespace = id.substring(0, colon).toLowerCase(Locale.ROOT);
            String rest = id.substring(colon + 1);
            if (!"model".equals(namespace) && !"id".equals(namespace)) {
                return framework(namespace, rest);
            }
            int next = rest.indexOf(':');
            id = next < 0 ? rest : rest.substring(0, next);
        } else if (DSL_NAMESPACES.contains(id.toLowerCase(Locale.ROOT))) {
            return framework(id.toLowerCase(Locale.ROOT), "");
        }
        if (id.isEmpty() || "model".equalsIgnoreCase(id) || "id".equalsIgnoreCase(id)) {
            id = property;
        }
        if (FRAMEWORK_IDS.contains(id.toLowerCase(Locale.ROOT))) {
            return framework(id.toLowerCase(Locale.ROOT), "");
        }
        String module = null;
        int at = id.indexOf('@');
        if (at >= 0) {
            module = id.substring(at + 1).toLowerCase(Locale.ROOT);
            id = id.substring(0, at);
        }
        if (id.indexOf('.') >= 0) {
            return resolve(id, caller);
        }
        return named(id, module);
    }

    /**
     * The namespaces of WireBox's injection DSL whose objects have one type in every application. WireBox takes an ID
     * that is one of them alone as the DSL, as {@code inject="executor"}.
     */
    private static final Set<String> DSL_NAMESPACES = new HashSet<>(Arrays.asList("coldbox", "box", "wirebox",
            "cachebox", "logbox", "executor", "provider"));

    /**
     * ColdBox's own objects, which a property's name gives when its {@code inject} says nothing.
     */
    private static final Set<String> FRAMEWORK_IDS = new HashSet<>(Arrays.asList("coldbox", "wirebox", "cachebox",
            "logbox"));

    /**
     * The object an injection DSL gives, as ColdBox's own DSL builders make it, or null for one whose type an
     * application configures.
     */
    private JavaType.@Nullable Class framework(String namespace, String rest) {
        String what = rest.toLowerCase(Locale.ROOT);
        String fullyQualifiedName;
        switch (namespace) {
            case "coldbox":
            case "box":
                fullyQualifiedName = coldBoxObject(what);
                break;
            case "wirebox":
                fullyQualifiedName = what.isEmpty() ? "coldbox.system.ioc.Injector" :
                        "populator".equals(what) ? "coldbox.system.core.dynamic.ObjectPopulator" : null;
                break;
            case "cachebox":
                // an application configures each cache's provider, and every provider implements ICacheProvider
                fullyQualifiedName = what.isEmpty() ? "coldbox.system.cache.CacheFactory" :
                        what.indexOf(':') < 0 ? "coldbox.system.cache.providers.ICacheProvider" : null;
                break;
            case "logbox":
                fullyQualifiedName = what.isEmpty() ? "coldbox.system.logging.LogBox" :
                        what.startsWith("logger") || what.startsWith("root") ? "coldbox.system.logging.Logger" : null;
                break;
            case "executor":
                fullyQualifiedName = what.indexOf(':') < 0 ? "coldbox.system.async.executors.Executor" : null;
                break;
            case "provider":
                fullyQualifiedName = "coldbox.system.ioc.Provider";
                break;
            default:
                fullyQualifiedName = null;
        }
        return fullyQualifiedName == null ? null : resolve(fullyQualifiedName, "");
    }

    private static @Nullable String coldBoxObject(String what) {
        switch (what) {
            case "":
                return "coldbox.system.web.Controller";
            case "flash":
                // each flash scope ColdBox can be configured with extends AbstractFlashScope
                return "coldbox.system.web.flash.AbstractFlashScope";
            case "requestservice":
                return "coldbox.system.web.services.RequestService";
            case "interceptorservice":
                return "coldbox.system.web.services.InterceptorService";
            default:
                return null;
        }
    }

    /**
     * The one component a file name names, in the module's directory when there is a module, else preferring one in a
     * {@code models} directory. A module may be a mapping's, since ColdBox modules install as packages, but an ID
     * with no module is the application's own.
     */
    private JavaType.@Nullable Class named(String name, @Nullable String module) {
        List<Component> candidates = new ArrayList<>(byFileName.getOrDefault(name.toLowerCase(Locale.ROOT), emptyList()));
        if (module != null && dependencies != null) {
            candidates.addAll(dependencies.byFileName.getOrDefault(name.toLowerCase(Locale.ROOT), emptyList()));
        }
        if (module != null) {
            candidates.removeIf(c -> !("/" + key(c.path)).contains("/" + module + "/"));
        } else if (candidates.size() > 1) {
            List<Component> models = new ArrayList<>(candidates);
            models.removeIf(c -> !("/" + key(c.path)).contains("/models/"));
            if (!models.isEmpty()) {
                candidates = models;
            }
        }
        return candidates.size() == 1 ? candidates.get(0).type : null;
    }

    /**
     * What {@code new} calls on a component, which takes the parameters of its {@code init} function.
     */
    JavaType.Method constructor(JavaType.FullyQualified type) {
        return constructors.computeIfAbsent(type, t -> {
            JavaType.Method init = method(t, "init");
            return new JavaType.Method(null, Flag.Public.getBitMask(), t, "<constructor>", t,
                    init == null ? emptyList() : init.getParameterNames(),
                    init == null ? emptyList() : init.getParameterTypes(), null, null, null, null);
        });
    }

    /**
     * The class a component file declares: the first at its root, or in a {@code <cfscript>} at its root.
     */
    static J.@Nullable ClassDeclaration declaredClass(Cf.CompilationUnit cu) {
        for (Statement statement : cu.getStatements()) {
            if (statement instanceof J.ClassDeclaration) {
                return (J.ClassDeclaration) statement;
            }
            if (statement instanceof Cf.Tag && ((Cf.Tag) statement).getBody() != null) {
                for (Statement inner : Objects.requireNonNull(((Cf.Tag) statement).getBody()).getStatements()) {
                    if (inner instanceof J.ClassDeclaration) {
                        return (J.ClassDeclaration) inner;
                    }
                }
            }
        }
        return null;
    }

    private static String file(Path path) {
        return path.toString().replace('\\', '/');
    }

    /**
     * A file's path in lower case, without its extension, since CFML names components without regard to case.
     */
    private static String key(String file) {
        int extension = file.lastIndexOf('.');
        String withoutExtension = extension > file.lastIndexOf('/') ? file.substring(0, extension) : file;
        return withoutExtension.toLowerCase(Locale.ROOT);
    }
}
