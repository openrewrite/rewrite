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
package org.openrewrite.kotlin.recipe.internal

import org.jetbrains.kotlin.name.FqName

// Central map of the recipe DSL's language scope factories to their target
// language details. Mirrors the build-time `LanguageDescriptor` list in
// `rewrite-kotlin/build.gradle.kts`: adding a language is one entry here plus
// one Gradle classpath line.
//
// Consumed by:
//   - `RecipeFirDslCheckers` to recognise which calls are "language scopes"
//     for the nesting / phase-level rules.
//   - `RecipeIrGenerationExtension`'s Java-vs-Kotlin classifier to identify
//     explicit `kotlin { … }` / `java { … }` overrides.
internal data class LanguageDescriptor(
    /** DSL factory name on `RecipeBuilder` / `LanguageHost`, e.g. `"kotlin"`. */
    val factoryName: String,
    /** Generated/handwritten scope class FQN, e.g. `"org.openrewrite.dsl.scopes.JavaScope"`. */
    val scopeFqn: FqName,
    /** Visitor class FQN the scope targets, e.g. `"org.openrewrite.java.JavaVisitor"`. */
    val visitorFqn: FqName,
    /**
     * LST root-package prefix. Any tree node whose FQN starts with this prefix
     * is "owned" by this language for the LST-structural classifier — e.g. a
     * `J.MethodInvocation` reference promotes nothing (the default JavaVisitor
     * already walks it), but a `K.Property` reference promotes the recipe to a
     * KotlinVisitor.
     */
    val treeRootPackage: String,
)

internal object RecipeIrLanguageDescriptors {

    val LANGUAGES: List<LanguageDescriptor> = listOf(
        LanguageDescriptor(
            factoryName = "kotlin",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.KotlinScope"),
            visitorFqn = FqName("org.openrewrite.kotlin.KotlinVisitor"),
            treeRootPackage = "org.openrewrite.kotlin.tree.K",
        ),
        LanguageDescriptor(
            factoryName = "java",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.JavaScope"),
            visitorFqn = FqName("org.openrewrite.java.JavaVisitor"),
            treeRootPackage = "org.openrewrite.java.tree.J",
        ),
        LanguageDescriptor(
            factoryName = "yaml",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.YamlScope"),
            visitorFqn = FqName("org.openrewrite.yaml.YamlVisitor"),
            treeRootPackage = "org.openrewrite.yaml.tree.Yaml",
        ),
        LanguageDescriptor(
            factoryName = "xml",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.XmlScope"),
            visitorFqn = FqName("org.openrewrite.xml.XmlVisitor"),
            treeRootPackage = "org.openrewrite.xml.tree.Xml",
        ),
        LanguageDescriptor(
            factoryName = "maven",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.MavenScope"),
            visitorFqn = FqName("org.openrewrite.maven.MavenVisitor"),
            treeRootPackage = "org.openrewrite.maven.tree",
        ),
        LanguageDescriptor(
            factoryName = "json",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.JsonScope"),
            visitorFqn = FqName("org.openrewrite.json.JsonVisitor"),
            treeRootPackage = "org.openrewrite.json.tree.Json",
        ),
        LanguageDescriptor(
            factoryName = "properties",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.PropertiesScope"),
            visitorFqn = FqName("org.openrewrite.properties.PropertiesVisitor"),
            treeRootPackage = "org.openrewrite.properties.tree.Properties",
        ),
        LanguageDescriptor(
            factoryName = "toml",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.TomlScope"),
            visitorFqn = FqName("org.openrewrite.toml.TomlVisitor"),
            treeRootPackage = "org.openrewrite.toml.tree.Toml",
        ),
        LanguageDescriptor(
            factoryName = "hcl",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.HclScope"),
            visitorFqn = FqName("org.openrewrite.hcl.HclVisitor"),
            treeRootPackage = "org.openrewrite.hcl.tree.Hcl",
        ),
        LanguageDescriptor(
            factoryName = "python",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.PythonScope"),
            visitorFqn = FqName("org.openrewrite.python.PythonVisitor"),
            treeRootPackage = "org.openrewrite.python.tree.Py",
        ),
        LanguageDescriptor(
            factoryName = "csharp",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.CSharpScope"),
            visitorFqn = FqName("org.openrewrite.csharp.CSharpVisitor"),
            treeRootPackage = "org.openrewrite.csharp.tree.Cs",
        ),
        LanguageDescriptor(
            factoryName = "scala",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.ScalaScope"),
            visitorFqn = FqName("org.openrewrite.scala.ScalaVisitor"),
            treeRootPackage = "org.openrewrite.scala.tree.Scala",
        ),
        LanguageDescriptor(
            factoryName = "javascript",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.JavaScriptScope"),
            visitorFqn = FqName("org.openrewrite.javascript.JavaScriptVisitor"),
            treeRootPackage = "org.openrewrite.javascript.tree.JS",
        ),
        LanguageDescriptor(
            factoryName = "groovy",
            scopeFqn = FqName("org.openrewrite.dsl.scopes.GroovyScope"),
            visitorFqn = FqName("org.openrewrite.groovy.GroovyVisitor"),
            treeRootPackage = "org.openrewrite.groovy.tree.G",
        ),
    )

    /** Factory names → descriptors. Used by the FIR checker. */
    val byFactoryName: Map<String, LanguageDescriptor> =
        LANGUAGES.associateBy { it.factoryName }

    /** Scope class FQNs → descriptors. Used by the FIR checker's call-site recognition. */
    val byScopeFqn: Map<FqName, LanguageDescriptor> =
        LANGUAGES.associateBy { it.scopeFqn }

    /** Factory FQNs (e.g. `org.openrewrite.LanguageHost.kotlin`) → descriptors. */
    val byFactoryFqn: Map<String, LanguageDescriptor> by lazy {
        // Multiple receiver classes expose the same factory names — they all
        // live on `LanguageHost` (an abstract base of `EditScope` /
        // `EditScopeWithAcc` / `ScanScope` / `GenerateScope` / `GenerateScopeWithAcc`).
        // The IR pass / checker resolve callable IDs against any of these
        // receivers, so the map is keyed by every plausible owner FQN.
        val receivers = listOf(
            "org.openrewrite.LanguageHost",
            "org.openrewrite.EditScope",
            "org.openrewrite.EditScopeWithAcc",
            "org.openrewrite.ScanScope",
            "org.openrewrite.GenerateScope",
            "org.openrewrite.GenerateScopeWithAcc",
        )
        buildMap {
            for (lang in LANGUAGES) {
                for (recv in receivers) {
                    put("$recv.${lang.factoryName}", lang)
                }
            }
        }
    }

    /**
     * True if [typeFqn] is a tree node owned by the Kotlin language module.
     * Used by the LST-structural classifier to decide JavaVisitor vs
     * KotlinVisitor: any `K.*` reference in the before/after templates of a
     * `rewrite { } to { }` clause promotes the generated visitor to Kotlin.
     */
    fun isKotlinSpecificTreeNode(typeFqn: String): Boolean =
        typeFqn.startsWith("org.openrewrite.kotlin.tree.K.") ||
            typeFqn == "org.openrewrite.kotlin.tree.K"
}
