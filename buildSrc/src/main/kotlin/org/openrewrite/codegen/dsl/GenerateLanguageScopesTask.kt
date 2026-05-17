/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.codegen.dsl

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.Serializable
import java.util.jar.JarFile
import javax.inject.Inject

/**
 * Generates one `<Lang>Scope.kt` per language descriptor under
 * [outputDir]. Each generated class extends [org.openrewrite.dsl.scopes.LanguageScope]
 * and exposes one `visitX(action: (Node) -> Node)` function per public `visitX`
 * declared on the corresponding `<Lang>Visitor.class` (and its ancestors up to
 * but not including `TreeVisitor`). The class's `build()` returns an anonymous
 * `<Lang>Visitor<ExecutionContext>` whose overrides dispatch through the
 * registered actions map.
 *
 * Implementation: ASM reads visitor `.class` files from [visitorClasspath]
 * (typically `rewrite-kotlin`'s compileClasspath + its own compileJava output).
 * No reflection / classloading — so missing transitive deps on the codegen
 * classpath don't fail introspection.
 */
@CacheableTask
abstract class GenerateLanguageScopesTask @Inject constructor() : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val visitorClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val languages: ListProperty<LanguageDescriptor>

    @TaskAction
    fun generate() {
        val classIndex = ClassIndex.build(visitorClasspath.files)
        val outRoot = outputDir.get().asFile.also { it.mkdirs() }
        // Generated scopes live under `org.openrewrite.dsl.scopes`.
        val pkgDir = outRoot.resolve("org/openrewrite/dsl/scopes").also { it.mkdirs() }
        // Clean stale scope files so a removed language descriptor doesn't
        // leave an orphan file.
        pkgDir.listFiles()?.forEach { f -> if (f.name.endsWith("Scope.kt")) f.delete() }
        languages.get().forEach { lang ->
            val scope = buildScope(lang, classIndex)
            pkgDir.resolve("${lang.scopeName}.kt").writeText(scope)
        }
    }

    private fun buildScope(lang: LanguageDescriptor, classIndex: ClassIndex): String {
        val visitorInternalName = lang.visitorFqn.replace('.', '/')
        val visitMethods = collectVisitMethods(visitorInternalName, classIndex)
        return renderScope(lang, visitMethods)
    }

    /**
     * Walk the visitor class hierarchy from [startInternalName] upward,
     * collecting every public `visitX` method with shape `(NodeType, P)`
     * returning a Tree subtype. Stops at `TreeVisitor` (its bookkeeping
     * methods aren't part of the per-node visit surface).
     */
    private fun collectVisitMethods(startInternalName: String, classIndex: ClassIndex): List<VisitMethod> {
        val collected = LinkedHashMap<String, VisitMethod>()
        var current: String? = startInternalName
        while (current != null && current != "org/openrewrite/TreeVisitor" && current != "java/lang/Object") {
            val node = classIndex.read(current) ?: break
            node.methods.filter { it.isCandidateVisitMethod() }.forEach { m ->
                val parsed = parseVisitMethod(m) ?: return@forEach
                // Dedup by method NAME only, not name+descriptor. Subclasses
                // sometimes overload `visitX` with a language-specific node
                // type alongside the inherited generic node form (e.g.
                // `CSharpVisitor.visitCompilationUnit(Cs.CompilationUnit, P)`
                // alongside the inherited `(J.CompilationUnit, P)`). Exposing
                // both in the DSL would produce a JVM-signature clash on
                // `visitCompilationUnit(Function1)`. Subclass declarations win
                // because we walk the hierarchy from subclass upward.
                collected.putIfAbsent(parsed.methodName, parsed)
            }
            current = node.superName
        }
        return collected.values.toList()
    }

    private fun MethodNode.isCandidateVisitMethod(): Boolean {
        // public, non-static, non-bridge, non-synthetic, name starts with `visit`
        // and isn't one of the bookkeeping methods.
        if ((access and Opcodes.ACC_PUBLIC) == 0) return false
        if ((access and Opcodes.ACC_STATIC) != 0) return false
        if ((access and (Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC)) != 0) return false
        if (!name.startsWith("visit") || name.length == 5) return false
        // Skip explicitly bookkeeping methods: visit(Tree, P), visitNonNull(Tree, P),
        // visitMarker, visitMarkers, visitContainer, visitRightPadded, visitLeftPadded,
        // visitContainer*, visitTreeNode (rare), etc. The IR-level helpers are
        // shape (Tree?, P) -> Tree? or (Container, P) -> Container — they don't
        // match our (NodeType, P) -> NodeType template anyway.
        if (name in BOOKKEEPING_METHODS) return false
        // Skip methods that DECLARE their own type parameters (e.g.
        // `<J2 extends J> visitParentheses(Parentheses<J2>, P)`). Their generic
        // signature starts with `<`. Bytecode erases the type args, so we'd
        // emit a Kotlin signature that doesn't satisfy the parent's parameter-
        // ized override. Methods that only reference the class-level `P` type
        // var have a signature but it doesn't start with `<` — those are fine.
        if (signature?.startsWith("<") == true) return false
        return true
    }

    private fun parseVisitMethod(m: MethodNode): VisitMethod? {
        // Descriptor of the form (LNodeType;LP;)LReturn; — exactly 2 reference params.
        val (params, retType) = splitDescriptor(m.desc) ?: return null
        if (params.size != 2) return null
        val nodeInternal = params[0].asReferenceTypeInternal() ?: return null
        val pInternal = params[1].asReferenceTypeInternal()
        // Skip if second param isn't a generic `P` (which on JVM resolves to java/lang/Object)
        // or any tree-or-ctx — we want the canonical (Node, P) shape.
        if (pInternal != "java/lang/Object" && pInternal != "org/openrewrite/ExecutionContext") return null
        // Returns a node — must be a reference type.
        val retInternal = retType.asReferenceTypeInternal() ?: return null
        // Skip if return type is Container/RightPadded/LeftPadded — those are
        // structural carriers, not visitable nodes per se.
        if (retInternal in STRUCTURAL_RETURN_TYPES) return null
        return VisitMethod(
            methodName = m.name,
            descriptor = m.desc,
            nodeInternalName = nodeInternal,
            returnInternalName = retInternal,
        )
    }

    private fun splitDescriptor(desc: String): Pair<List<String>, String>? {
        if (!desc.startsWith("(")) return null
        val close = desc.indexOf(')')
        if (close < 0) return null
        val paramsRaw = desc.substring(1, close)
        val ret = desc.substring(close + 1)
        val params = mutableListOf<String>()
        var i = 0
        while (i < paramsRaw.length) {
            val (token, consumed) = readDescriptorToken(paramsRaw, i) ?: return null
            params.add(token)
            i += consumed
        }
        return params to ret
    }

    private fun readDescriptorToken(s: String, start: Int): Pair<String, Int>? {
        if (start >= s.length) return null
        var i = start
        while (i < s.length && s[i] == '[') i++  // arrays
        return when (s[i]) {
            'L' -> {
                val end = s.indexOf(';', i)
                if (end < 0) null else s.substring(start, end + 1) to (end + 1 - start)
            }
            in setOf('B', 'C', 'D', 'F', 'I', 'J', 'S', 'V', 'Z') -> s.substring(start, i + 1) to (i + 1 - start)
            else -> null
        }
    }

    private fun String.asReferenceTypeInternal(): String? {
        if (!startsWith("L") || !endsWith(";")) return null
        return substring(1, length - 1)
    }

    private fun renderScope(lang: LanguageDescriptor, methods: List<VisitMethod>): String {
        val visitorFqn = lang.visitorFqn
        val sb = StringBuilder()
        sb.appendLine("""/*
 * Copyright 2026 the original author or authors.
 * Generated by the rewrite-kotlin `generateLanguageScopes` Gradle task — do not edit.
 */
package org.openrewrite.dsl.scopes

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.RecipeDsl

/**
 * Receiver scope for ${'`'}${lang.factoryName} { }${'`'} blocks inside ${'`'}edit { }${'`'},
 * ${'`'}scan { }${'`'}, and ${'`'}generate { }${'`'}. Exposes one ${'`'}visitX(action: <Visitor>.(Node) -> Node)${'`'}
 * function per public ${'`'}visitX${'`'} declared on ${'`'}$visitorFqn${'`'} and its
 * supertypes (up to but not including ${'`'}TreeVisitor${'`'}). Each action is a
 * receiver-style lambda whose ${'`'}this${'`'} is the running visitor, so authors can
 * read ${'`'}cursor${'`'} (and any other visitor state) inline.
 *
 * Generated by the rewrite-kotlin ${'`'}generateLanguageScopes${'`'} Gradle task —
 * regenerate by running ${'`'}:rewrite-kotlin:generateLanguageScopes${'`'} after
 * a visitor's ${'`'}visitX${'`'} surface changes.
 */
@RecipeDsl
public class ${lang.scopeName} internal constructor() : LanguageScope() {""")
        sb.appendLine()
        methods.forEach { m ->
            val nodeFqn = m.nodeInternalName.replace('/', '.').replace('$', '.')
            val key = (m.methodName + ":" + m.descriptor).replace("$", "\\$")
            sb.appendLine("    @Suppress(\"UNCHECKED_CAST\")")
            sb.appendLine("    public fun ${m.methodName}(action: $visitorFqn<ExecutionContext>.($nodeFqn) -> $nodeFqn) {")
            sb.appendLine("        actions[\"$key\"] = action as ((Any, Any) -> Any)")
            sb.appendLine("    }")
            sb.appendLine()
        }
        sb.appendLine("    @Suppress(\"UNCHECKED_CAST\")")
        sb.appendLine("    override fun build(): TreeVisitor<*, ExecutionContext> = object : $visitorFqn<ExecutionContext>() {")
        methods.forEach { m ->
            val nodeFqn = m.nodeInternalName.replace('/', '.').replace('$', '.')
            val retFqn = m.returnInternalName.replace('/', '.').replace('$', '.')
            val key = (m.methodName + ":" + m.descriptor).replace("$", "\\$")
            // The return type of the underlying visitor may be a wider J (e.g.
            // JavaVisitor.visitMethodInvocation returns J, action returns
            // J.MethodInvocation). Cast accordingly.
            //
            // Action shape is a receiver-style function type
            // `<Visitor>.(Node) -> Node`, which on the JVM compiles to
            // `Function2<Visitor, Node, Node>`. We pass `this` as the receiver
            // so authors can read `cursor` (and any other visitor state)
            // directly inside the lambda.
            sb.appendLine("        override fun ${m.methodName}(node: $nodeFqn, ctx: ExecutionContext): $retFqn {")
            sb.appendLine("            val action = actions[\"$key\"] as? ((Any, Any) -> Any)")
            sb.appendLine("            val transformed = if (action != null) action(this, node) as $nodeFqn else node")
            sb.appendLine("            return super.${m.methodName}(transformed, ctx) as $retFqn")
            sb.appendLine("        }")
            sb.appendLine()
        }
        sb.appendLine("    }")
        sb.appendLine("}")
        return sb.toString()
    }

    private companion object {
        val BOOKKEEPING_METHODS = setOf(
            "visitMarker", "visitMarkers", "visitContainer", "visitRightPadded",
            "visitLeftPadded", "visitNonNull", "visitTreeWithCursor",
            // Type-attribution helpers that aren't author surface
            "visitType", "visitJavaSourceFile",
        )
        val STRUCTURAL_RETURN_TYPES = setOf(
            "org/openrewrite/java/tree/JContainer",
            "org/openrewrite/java/tree/JRightPadded",
            "org/openrewrite/java/tree/JLeftPadded",
            "org/openrewrite/marker/Markers",
            "org/openrewrite/marker/Marker",
        )
    }
}

/**
 * Self-contained descriptor of a language scope to generate. Mirrors what the
 * plan §Phase 3 calls `RecipeIrLanguageDescriptors` but lives at build time
 * (the IR-time copy in `RecipeIrLanguageDescriptors.kt` lands in Phase 3).
 *
 * @param factoryName lowercase factory name used in the DSL (e.g. `kotlin`)
 * @param scopeName scope class simple name (e.g. `KotlinScope`)
 * @param visitorFqn fully qualified `<Lang>Visitor` class on the compile classpath
 */
data class LanguageDescriptor(
    val factoryName: String,
    val scopeName: String,
    val visitorFqn: String,
) : Serializable

private class ClassIndex(private val byInternalName: Map<String, ByteArray>) {
    fun read(internalName: String): ClassNode? {
        val bytes = byInternalName[internalName] ?: return null
        val reader = ClassReader(bytes)
        val node = ClassNode()
        reader.accept(node, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        return node
    }

    companion object {
        fun build(files: Set<File>): ClassIndex {
            val map = HashMap<String, ByteArray>()
            for (file in files) {
                when {
                    file.isDirectory -> indexDirectory(file, map)
                    file.isFile && file.name.endsWith(".jar") -> indexJar(file, map)
                    file.isFile && file.name.endsWith(".class") -> indexClassFile(file, map)
                }
            }
            return ClassIndex(map)
        }

        private fun indexDirectory(dir: File, into: MutableMap<String, ByteArray>) {
            dir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { f ->
                val rel = f.relativeTo(dir).invariantSeparatorsPath
                val internalName = rel.removeSuffix(".class")
                into.putIfAbsent(internalName, f.readBytes())
            }
        }

        private fun indexJar(jar: File, into: MutableMap<String, ByteArray>) {
            JarFile(jar).use { jf ->
                jf.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }.forEach { e ->
                    val internalName = e.name.removeSuffix(".class")
                    if (internalName !in into) {
                        jf.getInputStream(e).use { into[internalName] = it.readBytes() }
                    }
                }
            }
        }

        private fun indexClassFile(file: File, into: MutableMap<String, ByteArray>) {
            // Standalone .class files (rare); index by what their internal name claims.
            val bytes = file.readBytes()
            val reader = ClassReader(bytes)
            val node = ClassNode().also { reader.accept(it, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES) }
            into.putIfAbsent(node.name, bytes)
        }
    }
}

private data class VisitMethod(
    val methodName: String,
    val descriptor: String,
    val nodeInternalName: String,
    val returnInternalName: String,
)
