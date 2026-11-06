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
package org.openrewrite.coldfusion;

import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.marker.InferredType;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the parser knows of a repository's types, for a {@link TypeInference}: the components and templates of the
 * repository and its dependencies and how CFML resolves a name to one, CFML's own values and built-in functions, and
 * the JDK's classes. It is the parser's index of the repository, which the parser types each file from.
 */
public interface TypeContext {

    /**
     * The component a name gives from the file at {@code caller}, as CFML looks for it, or null when no one component
     * matches it. A null caller names it from the repository's root.
     */
    JavaType.@Nullable FullyQualified resolve(String name, @Nullable Path caller);

    /**
     * The function a component declares by a name, in any case, or one of its supertypes, the templates it includes or
     * its interfaces does.
     */
    JavaType.@Nullable Method method(JavaType.@Nullable FullyQualified type, String name);

    /**
     * The variable a component or one of its supertypes has by a name, in any case: a property, or what its functions
     * assign to {@code variables.x} or {@code this.x}.
     */
    JavaType.@Nullable Variable variable(JavaType.@Nullable FullyQualified type, String name);

    /**
     * Whether a type is a component or a template, of the repository or of a dependency.
     */
    boolean isComponent(@Nullable JavaType type);

    /**
     * Whether a function's component assigns it to other objects, as MockBox assigns {@code $} to each mock.
     */
    boolean isAssignedToOtherObjects(JavaType.Method function);

    /**
     * Whether a type is a component or a template of a dependency rather than of the repository.
     */
    boolean isDependency(@Nullable JavaType type);

    /**
     * Every component of the repository and of its dependencies. Templates, which no variable holds, are not among them.
     */
    List<JavaType.FullyQualified> getComponents();

    /**
     * The templates a component or a template includes, whose functions are its own.
     */
    List<JavaType.FullyQualified> includedTemplates(JavaType.FullyQualified type);

    /**
     * The component a variable of the {@code application}, {@code session}, {@code server} or {@code request} scope
     * holds everywhere, as {@code application.userService}, when a component assigns it one.
     */
    JavaType.@Nullable FullyQualified sharedScopeVariableType(String dottedName);

    /**
     * Whether a template is a view or a layout of a ColdBox application or module, which runs in ColdBox's renderer
     * with ColdBox's request context as {@code event}.
     */
    boolean isColdBoxView(Path sourcePath);

    /**
     * CFML's array, {@code cfml.Array}.
     */
    JavaType.FullyQualified getArrayType();

    /**
     * CFML's struct, {@code cfml.Struct}.
     */
    JavaType.FullyQualified getStructType();

    /**
     * CFML's query, {@code cfml.Query}.
     */
    JavaType.FullyQualified getQueryType();

    /**
     * Whether a type is one of CFML's values that has member functions: an array, a struct, a query or a string.
     */
    boolean isValueType(@Nullable JavaType type);

    /**
     * The built-in function a member function of one of CFML's values aliases, as {@code arrayAppend} for
     * {@code items.append(x)} on an array, or null.
     */
    JavaType.@Nullable Method memberFunction(@Nullable JavaType receiver, String name);

    /**
     * A {@code java.*} or {@code javax.*} class of the JDK, as Java types it, or null.
     */
    JavaType.@Nullable FullyQualified javaClass(String className);

    /**
     * Whether a type is one of the JDK's classes that {@link #javaClass} gives.
     */
    boolean isJava(@Nullable JavaType type);

    /**
     * The method a Java class or one of its supertypes declares by a name, in any case, that takes as many arguments as
     * a call passes. CFML's {@code init} on a class is its constructor.
     */
    JavaType.@Nullable Method javaMethod(JavaType.FullyQualified type, String name, int arguments);

    /**
     * A name given a type, as the parser types one: a dotted name has the type on itself and on its last name.
     */
    <T extends J> T withType(T tree, JavaType type);

    /**
     * Whether a variable's initializer only declares it, as the {@code ""} or the {@code 0} does that old CFML declares
     * each variable with before it assigns the variable its value.
     */
    boolean isPlaceholder(@Nullable Expression initializer);

    /**
     * The key of each call in a tree, by the call's id. Each parse of a file gives a call the same key.
     */
    Map<UUID, CallKey> callKeys(Cf.CompilationUnit cu);

    /**
     * Gives a function of the repository the return type an inference found, when it has none. The parser types the
     * calls to the function with it as with a declared type, so give it only a type the inference is sure of. The
     * parser marks nothing it types this way. The inference marks it in {@link TypeInference#attribute}, as
     * {@link InferredType} says. Returns whether the function took it, which a dependency's function, shared by every
     * parse that maps the dependency, never does.
     *
     * @throws IllegalStateException outside {@link TypeInference#solve}, since every tree shares the function's type
     */
    boolean setReturnType(JavaType.Method function, JavaType returnType);

    /**
     * Gives a parameter of a function of the repository the component an inference found every call passes it, when the
     * parameter has no type. Only a component counts, since CFML converts strings, numbers and booleans as a call
     * passes them. Returns whether the parameter took it, which a dependency's function never does.
     *
     * @throws IllegalStateException outside {@link TypeInference#solve}, since every tree shares the function's type
     */
    boolean setParameterType(JavaType.Method function, int parameter, JavaType component);
}
