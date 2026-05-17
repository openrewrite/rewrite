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

/**
 * Android-specific {@link org.openrewrite.trait.Trait Traits} for Java and Kotlin
 * source. Each trait wraps a {@link org.openrewrite.Cursor} pointing at a single
 * LST node and exposes Android-relevant semantics (resource references,
 * lifecycle overrides, intent actions, view bindings, permission checks). Each
 * trait ships a nested {@code Matcher extends SimpleTraitMatcher<XxxTrait>}; use
 * {@code matcher.asVisitor(t -&gt; ...)} to plug it into a
 * {@link org.openrewrite.Recipe}.
 *
 * <p>There is intentionally <em>no</em> {@code AndroidVisitor} base class.
 * Android recipes routinely span Groovy/Kotlin Gradle scripts, Java/Kotlin source,
 * and XML resources; a single-language inheritance hierarchy is the wrong shape.
 * Traits are the composable alternative.</p>
 *
 * <h2>Cross-language via composition unwrap</h2>
 *
 * <p>Most traits in this package match a {@code J.*} node and therefore work for
 * Java <em>and</em> Kotlin sources unchanged. The exception is declaration-level
 * shapes: in Kotlin, a method declaration is a {@code K.MethodDeclaration} that
 * composes a {@code J.MethodDeclaration} via the
 * {@link org.openrewrite.kotlin.tree.K.MethodDeclaration#getMethodDeclaration()
 * methodDeclaration} field.
 * {@link org.openrewrite.android.trait.AndroidLifecycleMethod} demonstrates the
 * canonical unwrap pattern (see its source).</p>
 *
 * <h2>Trait + ScanningRecipe composition pattern</h2>
 *
 * <p>Traits answer "does this syntax node match a shape?" — a purely local
 * question. Anything that requires <em>cross-file</em> knowledge (does the
 * referenced resource exist? Is this {@code <activity>} declared in
 * {@code AndroidManifest.xml}?) is the job of a
 * {@link org.openrewrite.ScanningRecipe}: the scan phase walks every file in the
 * run and populates an accumulator; the visit phase composes a trait matcher
 * with an accumulator lookup.</p>
 *
 * <p>Worked example — flag {@code R.string.<name>} references whose entry is
 * missing from any scanned {@code res/values*&#47;strings.xml}:</p>
 *
 * <pre>{@code
 * public class FindMissingStringResources extends ScanningRecipe<Set<String>> {
 *
 *     @Override public Set<String> getInitialValue(ExecutionContext ctx) {
 *         return new HashSet<>();
 *     }
 *
 *     @Override public TreeVisitor<?, ExecutionContext> getScanner(Set<String> known) {
 *         return new XmlIsoVisitor<ExecutionContext>() {
 *             @Override public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
 *                 if ("string".equals(tag.getName())) {
 *                     tag.getAttributes().stream()
 *                         .filter(a -> "name".equals(a.getKeyAsString()))
 *                         .findFirst()
 *                         .ifPresent(a -> known.add(a.getValueAsString()));
 *                 }
 *                 return super.visitTag(tag, ctx);
 *             }
 *         };
 *     }
 *
 *     @Override public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> known) {
 *         return new AndroidResourceReference.Matcher().asVisitor((ref, ctx) -> {
 *             if ("string".equals(ref.getResourceType()) && !known.contains(ref.getResourceName())) {
 *                 return SearchResult.found(ref.getTree(), "missing string resource: " + ref.getResourceName());
 *             }
 *             return ref.getTree();
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>Notice the division of labor: the trait owns the syntactic question
 * ("is this an {@code R.string.X} reference?"), the accumulator owns the
 * cross-file invariant ("is X defined somewhere in the project's resources?"),
 * and the recipe glues them together with a single
 * {@link org.openrewrite.trait.SimpleTraitMatcher#asVisitor visitor lambda}.</p>
 */
@NullMarked
package org.openrewrite.android.trait;

import org.jspecify.annotations.NullMarked;
