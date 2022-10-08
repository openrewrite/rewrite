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
@file:Suppress("JUnitMalformedDeclaration")

package org.openrewrite.java.style

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.TabsAndIndentsStyle.MethodDeclarationParameters
import org.openrewrite.style.GeneralFormatStyle
import org.openrewrite.style.NamedStyles
import java.util.*

interface AutodetectTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1221")
    @Test
    fun springDemoApp(jp: JavaParser) {
        val cus = jp.parse("""
            package com.kmccarpenter.demospring;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class DemoSpringApplication {

            	public static void main(String[] args) {
            		SpringApplication.run(DemoSpringApplication.class, args);
            	}

            }
        """.trimIndent())
        val styles = Autodetect.detect(cus)
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))!!
        assertThat(tabsAndIndents.useTabCharacter).isTrue
    }

    @Test
    fun springCloudTabsAndIndents(jp: JavaParser) {
        val cus = jp.parse(
            """
            package org.springframework.cloud.netflix.eureka;
            
            import static org.springframework.cloud.netflix.eureka.EurekaConstants.DEFAULT_PREFIX;
            
            @SuppressWarnings("ALL")
            @ConfigurationProperties(EurekaClientConfigBean.PREFIX)
            public class EurekaClientConfigBean implements EurekaClientConfig, Ordered {
            	private static final int MINUTES = 60;
            
            	public void setOrder(int order) {
            		this.order = order;
            	}
            
            	@Override
            	public boolean equals(Object o) {
            		EurekaClientConfigBean that = (EurekaClientConfigBean) o;
            		return Objects.equals(propertyResolver, that.propertyResolver) && enabled == that.enabled
            				&& Objects.equals(transport, that.transport);
            	}
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))!!

        assertThat(tabsAndIndents.useTabCharacter).isTrue
        assertThat(tabsAndIndents.tabSize).isEqualTo(1)
        assertThat(tabsAndIndents.indentSize).isEqualTo(1)
        assertThat(tabsAndIndents.continuationIndent).isEqualTo(2)
    }

    @Test
    fun spinnakerTabsAndIndents(jp: JavaParser) {
        val cus = jp.parse(
            """
            package com.netflix.kayenta.orca.controllers;
            
            @SuppressWarnings("ALL")
            @RestController
            public class AdminController {
              private final ApplicationEventPublisher publisher;
            
              @Autowired
              public AdminController(ApplicationEventPublisher publisher) {
                this.publisher = publisher;
              }
            
              @RequestMapping(
                  method = RequestMethod.POST)
              void setInstanceEnabled(@RequestBody Map<String, Boolean> enabledWrapper) {
                Boolean enabled = enabledWrapper.get("enabled");
            
                if (enabled == null) {
                  throw new ValidationException("The field 'enabled' must be set.", null);
                }
            
                setInstanceEnabled(enabled);
              }
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))!!

        assertThat(tabsAndIndents.useTabCharacter).isFalse
        assertThat(tabsAndIndents.tabSize).isEqualTo(2)
        assertThat(tabsAndIndents.indentSize).isEqualTo(2)
        assertThat(tabsAndIndents.continuationIndent).isEqualTo(4)
    }

    @Test
    fun rewriteTabsAndIndents(jp: JavaParser) {
        val cus = jp.parse(
            """
            public class Autodetect extends NamedStyles {
                @Override
                public J.Identifier visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                    J.Identifier i = super.visitIdentifier(ident, ctx);
             
                    if (TypeUtils.isOfClassType(i.getType(), oldPackageName)
                            && i.getSimpleName().equals(oldPackageType.getClassName())) {
                        i = i.withName((newPackageType).getClassName())
                                .withType(newPackageType);
                    }
                    
                    return i;
                }
            
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))!!

        assertThat(tabsAndIndents.useTabCharacter).isFalse
        assertThat(tabsAndIndents.tabSize).isEqualTo(4)
        assertThat(tabsAndIndents.indentSize).isEqualTo(4)
        assertThat(tabsAndIndents.continuationIndent).isEqualTo(8)
    }

    @Test
    fun defaultTabIndentSizeToOne(jp: JavaParser) {
        val cus = jp.parse(
            """
            /**
             * 
             */
            public class Test {
            	private final ApplicationEventPublisher publisher;
            	public void method() {
            		int value = 0;
            	}
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))!!
        assertThat(tabsAndIndents.useTabCharacter).isTrue
        assertThat(tabsAndIndents.tabSize).isEqualTo(1)
        assertThat(tabsAndIndents.indentSize).isEqualTo(1)
    }

    @Test
    fun rewriteImportLayout(jp: JavaParser) {
        val cus = jp.parse(
            """
            import com.fasterxml.jackson.annotation.JsonCreator;
            
            import org.openrewrite.internal.StringUtils;
            import org.openrewrite.internal.ListUtils;
            import org.openrewrite.internal.lang.Nullable;
            
            import java.util.*;
            import java.util.stream.Collectors;
            
            import static java.util.Collections.*;
            import static java.util.function.Function.identity;
            
            public class Test {
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(styles))!!

        assertThat(importLayout.layout[0]).isInstanceOf(ImportLayoutStyle.Block.AllOthers::class.java)

        assertThat(importLayout.layout[1]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[2])
            .isInstanceOf(ImportLayoutStyle.Block.ImportPackage::class.java)
            .matches { b -> !(b as ImportLayoutStyle.Block.ImportPackage).isStatic }
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).packageWildcard.toString() == "org\\.openrewrite\\.internal\\..+" }

        assertThat(importLayout.layout[3]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[4])
            .isInstanceOf(ImportLayoutStyle.Block.ImportPackage::class.java)
            .matches { b -> !(b as ImportLayoutStyle.Block.ImportPackage).isStatic }
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).packageWildcard.toString() == "javax\\..+" }

        assertThat(importLayout.layout[5])
            .isInstanceOf(ImportLayoutStyle.Block.ImportPackage::class.java)
            .matches { b -> !(b as ImportLayoutStyle.Block.ImportPackage).isStatic }
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).packageWildcard.toString() == "java\\..+" }

        assertThat(importLayout.layout[6]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[7])
            .isInstanceOf(ImportLayoutStyle.Block.AllOthers::class.java)
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).isStatic }
    }

    @Test
    fun detectStarImport(jp: JavaParser) {
        val cus = jp.parse(
            """
            import java.util.*;
            
            public class Test {
                List<Integer> l;
                Set<Integer> s;
                Map<Integer, Integer> m;
                Collection<Integer> c;
                LinkedHashMap<Integer, Integer> lhm;
                HashSet<Integer> integer;
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(styles))!!

        assertThat(importLayout.classCountToUseStarImport).isEqualTo(6)
    }

    @Test
    fun detectImportCounts(jp: JavaParser) {
        val cus = jp.parse(
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
                List<Integer> l;
                Set<Integer> s;
                Map<Integer, Integer> m;
                Collection<Integer> c;
                LinkedHashMap<Integer, Integer> lhm;
                HashSet<Integer> integer;
            }
        """.trimIndent()
        )

        val styles = Autodetect.detect(cus)
        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(styles))!!

        assertThat(importLayout.classCountToUseStarImport).isEqualTo(2147483647)
        assertThat(importLayout.nameCountToUseStarImport).isEqualTo(2147483647)
    }

    @Test
    fun detectMethodArgs(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        a("a" ,"b" ,"c" ,"d");
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isTrue
        assertThat(spacesStyle.other.afterComma).isFalse
    }

    @Test
    fun detectMethodArgsAfterComma(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        a("a", "b");
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isFalse
        assertThat(spacesStyle.other.afterComma).isTrue
    }

    @Suppress("StatementWithEmptyBody", "RedundantOperationOnEmptyContainer")
    @Test
    fun detectColonInForEachLoop(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        for (int i : new int[]{}) {}
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeColonInForEach).isTrue
    }

    @Test
    fun detectAfterTypeCast(jp: JavaParser) {
        val cu = jp.parse(
            """
                class T {
                    {
                        String s = (String) getString();
                    }
                }
            """
        )
        val styles = Autodetect.detect(cu)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.afterTypeCast).isTrue
    }
    @Suppress("StatementWithEmptyBody")
    @Test
    fun detectBeforeForSemicolon(jp: JavaParser) {
        val cu = jp.parse(
            """
            class T {
                void m() {
                    for (int i = 0; i < x; i++) {}
                }
            }
            """)
        val styles = Autodetect.detect(cu)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeForSemicolon).isFalse
        assertThat(spacesStyle.other.afterForSemicolon).isTrue
    }
    @Test
    fun detectMethodArgsNoArgs(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        a();
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isFalse
        assertThat(spacesStyle.other.afterComma).isTrue
    }

    @Test
    fun detectMethodArgsNoSpaceForComma(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        a("a","b","c");
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isFalse
        assertThat(spacesStyle.other.afterComma).isFalse
    }

    @Test
    fun detectMethodArgsSpaceForComma(jp: JavaParser) {
        val cus = jp.parse(
            """
                class Test {
                    void i() {
                        a("a" , "b" , "c");
                    }
                }
            """.trimIndent()
        )
        val styles = Autodetect.detect(cus)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isTrue
        assertThat(spacesStyle.other.afterComma).isTrue
    }

    @Test
    fun detectAfterCommaInNewArray(jp: JavaParser) {
        val cu = jp.parse(
            """
                class T {
                    static {
                        new int[]{1,2,3,4};
                    }
                }
            """
        )
        val styles = Autodetect.detect(cu)
        val spacesStyle = NamedStyles.merge(SpacesStyle::class.java, listOf(styles))!!

        assertThat(spacesStyle.other.beforeComma).isFalse
        assertThat(spacesStyle.other.afterComma).isTrue
    }

    @Test
    fun detectClrfLineFormat(jp: JavaParser) {
        val cus = jp.parse(
            "class Test {\r\n" +
                    "    // some comment\r\n" +
                    "    public void test() {\n" +
                    "        System.out.println();\n" +
                    "    }\r\n" +
                    "}\r\n"
        )

        val styles = Autodetect.detect(cus)
        val lineFormatStyle = NamedStyles.merge(GeneralFormatStyle::class.java, listOf(styles))

        assertThat(lineFormatStyle!!.isUseCRLFNewLines).isTrue
    }

    @Test
    fun detectLfLineFormat(jp: JavaParser) {
        val cus = jp.parse(
            "class Test {\n" +
                    "    // some comment\r\n" +
                    "    public void test() {\n" +
                    "        System.out.println();\n" +
                    "    }\n" +
                    "}\r\n"
        )

        val styles = Autodetect.detect(cus)
        val lineFormatStyle = NamedStyles.merge(GeneralFormatStyle::class.java, listOf(styles))

        assertThat(lineFormatStyle!!.isUseCRLFNewLines).isFalse
    }
}
