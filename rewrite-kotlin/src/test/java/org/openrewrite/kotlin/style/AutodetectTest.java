/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.kotlin.style;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"All", "RedundantVisibilityModifier"})
class AutodetectTest implements RewriteTest {
    private static KotlinParser kp() {
        return KotlinParser.builder().build();
    }

    @Test
    void continuationIndent() {
        var cus = kp().parse(
          """
            class Test {
            	fun eq(): Boolean {
            		return (1 == 1 &&
            				2 == 2 &&
            				3 == 3)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3552")
    @Test
    void continuationIndentFromParameters() {
        var cus = kp().parse(
          """
            class Test {
               fun foo(s1: String,
                    s2: String,
                    s3: String) {
               }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Test
    void continuationIndentFromChainedCalls() {
        var cus = kp().parse(
          """
            class Test {
                val s = ""
                     .uppercase()
                     .lowercase()
                     .toString()
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3550")
    @Test
    void alignParametersWhenMultiple() {
        var cus = kp().parse(
          """
            class Test {
            	fun foo(s1: String,
            	        s2: String,
            	        s3: String) {
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getFunctionDeclarationParameters().getAlignWhenMultiple()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1221")
    @Test
    void springDemoApp() {
        //language=kotlin
        var cus = kp().parse(
          """
            package com.kmccarpenter.demospring

            import java.util.Collections

            class DemoSpringApplication {
                companion object {
                    @JvmStatic
            		fun main(args: Array<String>) {
            			System.out.print("Hello world")
            		}
                }
            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();

        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Test
    void springCloudTabsAndIndents() {
        //language=kotlin
        var cus = kp().parse(
          """
            package org.springframework.cloud.netflix.eureka

            import org.springframework.cloud.netflix.eureka.*

            @SuppressWarnings("ALL")
            class EurekaClientConfigBean {
            	companion object {
            	private const val MINUTES = 60
            	}

            	override fun setOrder(order: Int) {
            	var a = order
            	}

            	override fun equals(o: Any?): Boolean {
            		val that = o as EurekaClientConfigBean
            		return (1 == 1 &&
            				2 == 2 &&
            				3 == 3)
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @SuppressWarnings("InfiniteRecursion")
    @Test
    void spinnakerTabsAndIndents() {
        var cus = kp().parse(
          """
            package com.netflix.kayenta.orca.controllers

            import java.lang.RuntimeException

            @SuppressWarnings("ALL")
            class AdminController {
              var publisher: String = ""

              @Suppress
              fun method(publisher: String) {
                this.publisher = publisher;
              }

              @Suppress(
                  "X")
              fun setInstanceEnabled(enabledWrapper : Map<String, Boolean>) {
                val enabled = enabledWrapper.get("enabled")

                if (enabled == null) {
                  throw RuntimeException("The field 'enabled' must be set.", null);
                }

                setInstanceEnabled(enabledWrapper);
              }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }

    @Test
    void rewriteTabsAndIndents() {
        var cus = kp().parse(
          """
            class Autodetect {
                @Override
                fun visitIdentifier(ident: Int, ctx: String): String {
                    var i = visitIdentifier(ident, ctx)

                    if (true
                            && true) {
                        i = visitIdentifier(ident, ctx)
                                .visitIdentifier(ident, ctx)
                    }

                    return i
                }

            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Test
    void defaultTabIndentSizeToOne() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
            	val publisher: String = "A"
            	fun method() {
            		var value = 0;
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4() {
        var cus = kp().parse(
          """
            /**
             *
             */
            class Test {
            	private val publisher: String = "1"
            	public fun method() {
            		var value = 0
                	var value1 = 1
            	    var value2 = 2
            		{
            	        var value3 = 2
                	    var value4 = 4
            		}
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    // TabSize 3 is atypical but not unheard of
    @Disabled
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize3() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
            	private final ApplicationEventPublisher publisher;
            	public void method() {
            		int value = 0;
            	   int value1 = 1;
            	   int value2 = 2;
            		{
            	      int value3 = 2;
            	      int value4 = 4;
            		}
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(org.openrewrite.java.style.TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(3);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(3);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4AndUseTabIsFalse() {
        var cus = kp().parse(
          """
            /**
             *
             */
            class Test {
            	private val publisher: String = "A"
                public fun method() {
            	    val value = 0;
            	    val value1 = 1;
            	    val value2 = 2;
            	    {
                	    val value3 = 2;
                    	val value4 = 4;
            	    }
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void inconsistentIndents() {
        var cus = kp().parse(
          """
            package org.openrewrite.before;

            import java.util.ArrayList;
            import java.util.List;

            public class HelloWorld {
                public fun main() {
                    System.out.print("Hello");
                        System.out.println(" world");
                }
            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4WithSomeErrors() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
                private val publisher: String = "A"
                public fun method() {
            	     var value = 0
            	   var value1 = 1
            	    var value2 = 2
            	    {
                	     var value3 = 2
                   	var value4 = 4
            	    }
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void defaultKotlinImportLayout() {
        var cus = kp().parse(
          """
            import org.a.A
            import org.a.B

            import java.util.Map
            import java.util.Set

            import javax.lang.model.type.ArrayType

            import kotlin.math.PI
            import kotlin.math.sqrt
            import kotlin.random.Random

            import kotlin.collections.Map as KMap
            import kotlin.collections.Set as KSet

            class Test {
                var a : Map<Int, Int>? = null
                var b : Set<Int>? = null
                var c = PI;
                var d = sqrt(1.0)
                var e = Random(1)
                var f : KMap<Int, String> = mapOf(1 to "one", 2 to "two", 3 to "three")
                var g : KSet<Int> = setOf(1, 2, 3)
                var h : ArrayType? = null
                var i : A? = null
                var j : B? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout()).hasSize(5);

        assertThat(importLayout.getLayout().getFirst()).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "java\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "javax\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "kotlin\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);
    }

    @Test
    void customizedKotlinImportLayout() {
        var cus = kp().parse(
          """
            import kotlin.collections.Map as KMap
            import kotlin.collections.Set as KSet

            import kotlin.math.PI
            import kotlin.math.sqrt
            import kotlin.random.Random

            import javax.lang.model.type.ArrayType

            import java.util.Map
            import java.util.Set

            import org.a.A
            import org.a.B

            class Test {
                var a : Map<Int, Int>? = null
                var b : Set<Int>? = null
                var c = PI;
                var d = sqrt(1.0)
                var e = Random(1)
                var f : KMap<Int, String> = mapOf(1 to "one", 2 to "two", 3 to "three")
                var g : KSet<Int> = setOf(1, 2, 3)
                var h : ArrayType? = null
                var i : A? = null
                var j : B? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout()).hasSize(5);

        assertThat(importLayout.getLayout().getFirst()).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "kotlin\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "javax\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "java\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);
    }

    @Test
    void partialImportLayout() {
        var cus = kp().parse(
          """
            import java.util.Map
            import java.util.Set
            import java.util.stream.Collectors

            import kotlin.math.PI
            import kotlin.math.cos
            import kotlin.math.sin
            import kotlin.math.sqrt
            import kotlin.random.Random
            import kotlin.collections.List
            import kotlin.text.toCharArray
            import kotlin.time.Duration
            import kotlin.sequences.Sequence

            class Test {
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().getFirst())
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "java\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "kotlin\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(2)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> "javax\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);
    }

    @Disabled
    @Test
    void detectStarImport() {
        var cus = kp().parse(
          """
            import java.util.*;

            class Test {
                val l: List<Integer>? = null
                val s: Set<Integer>? = null
                val m: Map<Integer, Integer>? = null
                val c: Collection<Integer>? = null
                val lhm: LinkedHashMap<Integer, Integer>? = null
                val integer: HashSet<Integer>? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getTopLevelSymbolsToUseStarImport()).isEqualTo(6);
        // assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(6);
    }

    @Test
    void detectImportCounts() {
        var cus = kp().parse(
          """
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Set;

                import javax.persistence.Entity;
                import javax.persistence.FetchType;
                import javax.persistence.JoinColumn;
                import javax.persistence.JoinTable;
                import javax.persistence.ManyToMany;
                import javax.persistence.Table;
                import javax.xml.bind.annotation.XmlElement;

                public class Test {
                    var l : List<Integer>? = null
                    var s : Set<Integer>? = null
                    var m : Map<Integer, Integer>? = null
                    var c : Collection<Integer>? = null
                    var lhm : LinkedHashMap<Integer, Integer>? = null
                    var hs : HashSet<Integer>? = null
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getTopLevelSymbolsToUseStarImport()).isEqualTo(5);
        assertThat(importLayout.getJavaStaticsAndEnumsToUseStarImport()).isEqualTo(3);
    }

    @Test
    void detectMethodArgs() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a" ,"b" ,"c" ,"d");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Test
    void detectMethodArgAfterComma() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a", "b");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    void detectMethodArgsNoArgs() {
        var cus = kp().parse(
          """
            class Test {
                fun i() {
                    a();
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    void detectMethodArgsNoSpaceForComma() {
        var cus = kp().parse(
          """
            class Test {
                fun i() {
                    a("a","b","c");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Test
    void detectMethodArgsSpaceForComma() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a" , "b" , "c");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    void detectAfterCommaInNewArray() {
        var cus = kp().parse(
          """
            class T {
                companion object {
                    val i = intArrayOf(1, 2, 3, 4)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    @Test
    void detectAfterCommaShouldIgnoreFirstElement() {
        var cus = kp().parse(
          """
            class T {
                companion object {
                    val i0 = intArrayOf(1, 2)
                    val i1 = intArrayOf(2, 3)
                    val i2 = intArrayOf(3, 4)
                    val i3 = intArrayOf(4, 5)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    @Test
    void detectAfterCommaBasedOnLambdas() {
        var cus = kp().parse(
          """
            import java.util.function.BiConsumer

            class T {
                companion object {
                    init {
                        val i0 = intArrayOf(1, 2)
                        val i1 = intArrayOf(2, 3)

                        val c0: BiConsumer<*, *> = BiConsumer { a, b -> }
                        val c1: BiConsumer<*, *> = BiConsumer { a, b -> }
                        val c2: BiConsumer<*, *> = BiConsumer { a, b -> }
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void detectElseWithNoNewLine() {
        var cus = kp().parse(
          """
            class Test {
                fun method(n: Int) {
                    if (n == 0) {
                    } else if (n == 1) {
                    } else {
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isFalse();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void detectElseOnNewLine() {
        var cus = kp().parse(
          """
                class Test {
                    fun method(n: Int) {
                        if (n == 0) {
                        }
                        else if (n == 1) {
                        }
                        else {
                        }
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isTrue();
    }

    @Disabled
    @Test
    void mostCommonIndentTakesPrecedence() {
        var cus = kp().parse(
          """
            package com.test;

            public class Foo {
               private val underIndented: Int = 1
                     var order: Int = 2
                  fun setOrder(order: Int) {
                        this.order = order
                        print("One two-space indent shouldn't override predominant 4-space indent")
                        val o = object {
                              fun fooBar() {
                                    print("fooBar");
                              }
                        };
                  }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(6);
        assertThat(tabsAndIndents.getIndentSize())
          .as("While there are outlier 3 and 9 space indents, the most prevalent indentation is 6")
          .isEqualTo(6);
        assertThat(tabsAndIndents.getContinuationIndent())
          .as("With no actual continuation indents to go off of, assume IntelliJ default of 2x the normal indent")
          .isEqualTo(12);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "RedundantStreamOptionalCall"})
    @Test
    void continuationIndents() {
        var cus = kp().parse(
          """
            import java.util.stream.Stream;

            class Continuations {
                fun cont() {
                    Stream.of("foo",
                                            "continuation")
                                .map{it ->
                                            Stream.of(it)
                                                        .map{it2 ->
                                                                    Stream.of(it2)
                                                                                .map{it3 ->
                                                                                            it3}}
                                                        .flatMap{it4 ->
                                                                    it4}}
                    val higherContIndent = 1 +
                                                    2
                    val lowerContIndent = 1 +
                            2
                    val sum = 1 +
                                (2 +
                                3) +
                                Stream.of(
                                            2 + 4,
                                            4
                                ).reduce(0){ acc, value -> acc + value}
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(12);
    }

    @Test
    void continuationIndentFromFieldAccess() {
        var cus = kp().parse(
          """
            class Node(val next: Node?, val name: String)
            class Test {
                val node = Node(null, "foo")
                val res = node
                     .next
                     ?.next
                     ?.next
                     ?.name
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Nested
    class ContinuationIndentForAnnotations {

        @Issue("https://github.com/openrewrite/rewrite/issues/3568")
        @Test
        void ignoreSpaceBetweenAnnotations() {
            var cus = kp().parse(
              """
                class Test {
                    @SafeVarargs
                    @Deprecated("")
                    @Suppress("more", "mistakes")
                    fun count(vararg strings: String) {
                        return strings.length
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .as("With no actual continuation indents to go off of, assume IntelliJ IDEA default of 2x the normal indent")
              .isEqualTo(8);
        }

        @Test
        void includeAnnotationAsAnnotationArg() {
            var cus = kp().parse(
              """
                annotation class Foo
                annotation class Foos(val value: Array<Foo>)

                class Test {
                    @Foos(
                       value = [Foo()])
                    fun count(vararg strings: String) {
                        return strings.length
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(3);
        }
    }

    @Test
    void useTrailingCommaDetection() {
        var cus = kp().parse(
          """
            class A(
                val a: Boolean,
                val b: Boolean,
            ) {}

            fun method(
                arg1: String,
                arg2: String,
            ) {
            }

            val x = method(
                "foo",
                "bar"
            )

            val y = method(
                "x",
                if (true) "foo" else "bar",
            )
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var otherStyle = NamedStyles.merge(OtherStyle.class, singletonList(styles));
        assertThat(otherStyle.getUseTrailingComma()).isTrue();
    }

    @Test
    void unuseTrailingCommaDetection() {
        var cus = kp().parse(
          """
            class A(
                val a: Boolean,
                val b: Boolean
            ) {}

            fun method(
                arg1: String,
                arg2: String,
            ) {
            }

            val x = method(
                "foo",
                "bar"
            )

            val y = method(
                "x",
                if (true) "foo" else "bar"
            )
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var otherStyle = NamedStyles.merge(OtherStyle.class, singletonList(styles));
        assertThat(otherStyle.getUseTrailingComma()).isFalse();
    }

    @Test
    void unqualifiedFunctionImport() {
        var cus = kp().parse(
          """
            fun a() = 1
            """,
          """
            package b
            import a
            fun b() = a()
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        assertThat(styles).isNotNull();
    }

    @Test
    void fourSpaceIndentDetection() {
        // This test verifies that a file with 4-space indentation is correctly detected.
        var cus = kp().parse(
          """
            class MyService {
                private fun validateInput(value: Int) {
                    logger.debug { "validating input" }
                    if (value <= 0) {
                        throw IllegalArgumentException("error")
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void fourSpaceWithLambdaAndSingleLineIf() {
        // Test the pattern:
        // - A function with 4-space indentation
        // - Contains a lambda (logger.debug { ... })
        // - Contains a single-line if statement
        var cus = kp().parse(
          """
            class MyService {
                private fun validateInput(value: Int) {
                    logger.debug { "validating input" }
                    if (value <= 0) throw IllegalArgumentException("error")
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void mixedIndentProjectPrefersLargerSample() {
        // Test that when a project has files with different indentation styles,
        // the autodetect picks the most common one.
        // This can cause issues when a file uses a minority indentation style.
        var cus = kp().parse(
          // File 1: 2-space indentation (many statements)
          """
            class TwoSpaceFile1 {
              fun method1() {
                val a = 1
                val b = 2
                val c = 3
                val d = 4
              }
              fun method2() {
                val e = 5
                val f = 6
              }
            }
            """,
          // File 2: 2-space indentation (many statements)
          """
            class TwoSpaceFile2 {
              fun method1() {
                val g = 7
                val h = 8
                val i = 9
              }
              fun method2() {
                val j = 10
                val k = 11
                val l = 12
              }
            }
            """,
          // File 3: 4-space indentation (fewer statements - minority)
          """
            class FourSpaceFile {
                private fun validateInput(value: Int) {
                    if (value <= 0) {
                        throw Exception("error")
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        // The autodetect aggregates all files and picks the most common indentation.
        // With more 2-space statements than 4-space statements, it picks 2 spaces.
        // This demonstrates the potential issue - a file with 4-space indentation
        // in a project that mostly uses 2-space will get reformatted to 2-space.
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        // We expect this to be 2 because that's the dominant style in the project
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
    }
}
