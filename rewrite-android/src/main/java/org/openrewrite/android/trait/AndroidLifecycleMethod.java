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
package org.openrewrite.android.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Identifies Android lifecycle method overrides on
 * {@link ComponentFamily#ACTIVITY Activity}, {@link ComponentFamily#FRAGMENT Fragment},
 * {@link ComponentFamily#SERVICE Service},
 * {@link ComponentFamily#BROADCAST_RECEIVER BroadcastReceiver},
 * {@link ComponentFamily#WORKER Worker} subclasses, and {@code @Composable}
 * functions ({@link ComponentFamily#COMPOSE COMPOSE}).
 * <p>
 * Cross-language via the K&rarr;J composition unwrap pattern: in Kotlin the
 * declaration is a {@link K.MethodDeclaration} that wraps a
 * {@link J.MethodDeclaration}. The matcher accepts either and the
 * {@link #getMethodDeclaration()} accessor unwraps so trait consumers can write
 * single-language code that works for both Java and Kotlin sources.
 */
@Incubating(since = "8.65.0")
@Value
public class AndroidLifecycleMethod implements Trait<Statement> {
    Cursor cursor;
    ComponentFamily componentFamily;

    /**
     * Returns the inner {@link J.MethodDeclaration} whether the tree at the cursor
     * is a {@code J.MethodDeclaration} or a Kotlin {@code K.MethodDeclaration}
     * wrapping one. Trait consumers should always call this rather than
     * {@code (J.MethodDeclaration) getCursor().getValue()}.
     */
    public J.MethodDeclaration getMethodDeclaration() {
        Object v = cursor.getValue();
        return v instanceof K.MethodDeclaration ?
                ((K.MethodDeclaration) v).getMethodDeclaration() :
                (J.MethodDeclaration) v;
    }

    public String getMethodName() {
        return getMethodDeclaration().getSimpleName();
    }

    /**
     * Coarse family of the enclosing Android component. Useful for routing
     * recipes that want to rewrite (e.g.) only Activity {@code onCreate} but
     * not Fragment {@code onCreate}.
     */
    public enum ComponentFamily {
        ACTIVITY,
        FRAGMENT,
        SERVICE,
        BROADCAST_RECEIVER,
        WORKER,
        COMPOSE
    }

    /**
     * Lifecycle method name registry keyed by the component class' fully-qualified
     * name. Insertion-ordered so we always probe the most specific entries first
     * (e.g., {@code androidx.fragment.app.Fragment} before
     * {@code android.app.Activity}). Extend by editing the static initializer.
     */
    static final Map<String, Set<String>> LIFECYCLE_METHODS_BY_COMPONENT;
    static final Map<String, ComponentFamily> FAMILY_BY_COMPONENT;

    static {
        LIFECYCLE_METHODS_BY_COMPONENT = new LinkedHashMap<>();
        FAMILY_BY_COMPONENT = new HashMap<>();

        addComponent("androidx.fragment.app.Fragment", ComponentFamily.FRAGMENT, Arrays.asList(
                "onAttach", "onCreate", "onCreateView", "onViewCreated", "onStart",
                "onResume", "onPause", "onStop", "onDestroyView", "onDestroy", "onDetach"));
        addComponent("android.app.Fragment", ComponentFamily.FRAGMENT, Arrays.asList(
                "onAttach", "onCreate", "onCreateView", "onViewCreated", "onStart",
                "onResume", "onPause", "onStop", "onDestroyView", "onDestroy", "onDetach"));
        addComponent("android.app.Activity", ComponentFamily.ACTIVITY, Arrays.asList(
                "onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy",
                "onRestart", "onSaveInstanceState", "onRestoreInstanceState"));
        addComponent("android.app.Service", ComponentFamily.SERVICE, Arrays.asList(
                "onCreate", "onStartCommand", "onBind", "onUnbind", "onDestroy"));
        addComponent("android.content.BroadcastReceiver", ComponentFamily.BROADCAST_RECEIVER,
                Collections.singletonList("onReceive"));
        addComponent("androidx.work.Worker", ComponentFamily.WORKER,
                Collections.singletonList("doWork"));
        addComponent("androidx.work.ListenableWorker", ComponentFamily.WORKER,
                Arrays.asList("startWork", "onStopped"));
    }

    private static void addComponent(String fqn, ComponentFamily family, Iterable<String> methods) {
        Set<String> set = new HashSet<>();
        for (String m : methods) {
            set.add(m);
        }
        LIFECYCLE_METHODS_BY_COMPONENT.put(fqn, set);
        FAMILY_BY_COMPONENT.put(fqn, family);
    }

    private static final String COMPOSABLE_ANNOTATION_FQN = "androidx.compose.runtime.Composable";

    public static class Matcher extends SimpleTraitMatcher<AndroidLifecycleMethod> {

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<AndroidLifecycleMethod, P> visitor) {
            // Both Java and Kotlin source converge on J.MethodDeclaration during traversal:
            // for Kotlin, a K.MethodDeclaration wraps a J.MethodDeclaration and the standard
            // KotlinVisitor descends into the inner J during accept. Triggering on
            // J.MethodDeclaration therefore covers both languages without double-firing,
            // and lets recipes attach markers to the J node the printer actually renders.
            // The trait's getMethodDeclaration() accessor handles either kind of cursor
            // when the trait is obtained via test()/get()/higher() directly.
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, P p) {
                    AndroidLifecycleMethod m = test(getCursor());
                    return m != null ?
                            (J) visitor.visit(m, p) :
                            super.visitMethodDeclaration(method, p);
                }
            };
        }

        @Override
        protected @Nullable AndroidLifecycleMethod test(Cursor cursor) {
            Object v = cursor.getValue();
            J.MethodDeclaration md;
            if (v instanceof K.MethodDeclaration) {
                md = ((K.MethodDeclaration) v).getMethodDeclaration();
            } else if (v instanceof J.MethodDeclaration) {
                md = (J.MethodDeclaration) v;
            } else {
                return null;
            }
            ComponentFamily family = detectFamily(md, cursor);
            if (family == null) {
                return null;
            }
            return new AndroidLifecycleMethod(cursor, family);
        }

        private static @Nullable ComponentFamily detectFamily(J.MethodDeclaration md, Cursor cursor) {
            // Composable functions are identified by the annotation, not by name or enclosing class.
            if (md.getLeadingAnnotations() != null) {
                for (J.Annotation ann : md.getLeadingAnnotations()) {
                    if (TypeUtils.isOfClassType(ann.getType(), COMPOSABLE_ANNOTATION_FQN)) {
                        return ComponentFamily.COMPOSE;
                    }
                }
            }

            String name = md.getSimpleName();
            J.ClassDeclaration enclosing = cursor.firstEnclosing(J.ClassDeclaration.class);
            if (enclosing == null || enclosing.getType() == null) {
                return null;
            }
            for (Map.Entry<String, Set<String>> entry : LIFECYCLE_METHODS_BY_COMPONENT.entrySet()) {
                String componentFqn = entry.getKey();
                if (entry.getValue().contains(name) &&
                        TypeUtils.isAssignableTo(componentFqn, enclosing.getType())) {
                    return FAMILY_BY_COMPONENT.get(componentFqn);
                }
            }
            return null;
        }
    }
}
