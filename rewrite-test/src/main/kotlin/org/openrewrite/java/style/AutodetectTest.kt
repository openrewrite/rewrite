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
package org.openrewrite.java.style

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.style.NamedStyles

interface AutodetectTest {

    @Test
    fun springCloudTabsAndIndents(jp: JavaParser) {
        val cus = jp.parse(
            """
            package org.springframework.cloud.netflix.eureka;
            
            import static org.springframework.cloud.netflix.eureka.EurekaConstants.DEFAULT_PREFIX;
            
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
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))

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
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))

        assertThat(tabsAndIndents.useTabCharacter).isFalse
        assertThat(tabsAndIndents.tabSize).isEqualTo(1)
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
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))

        assertThat(tabsAndIndents.useTabCharacter).isFalse
        assertThat(tabsAndIndents.tabSize).isEqualTo(1)
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
        val tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle::class.java, listOf(styles))
        assertThat(tabsAndIndents.useTabCharacter).isTrue
        assertThat(tabsAndIndents.tabSize).isEqualTo(1)
        assertThat(tabsAndIndents.indentSize).isEqualTo(1)
    }

    @Test
    fun rewriteImportLayout(jp: JavaParser) {
        val cus = jp.parse(
            """
            import com.fasterxml.jackson.annotation.JsonCreator;
            import org.openrewrite.Tree;
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
        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(styles))

        assertThat(importLayout.layout[0]).isInstanceOf(ImportLayoutStyle.Block.AllOthers::class.java)
        assertThat(importLayout.layout[1]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[2])
            .isInstanceOf(ImportLayoutStyle.Block.ImportPackage::class.java)
            .matches { b -> !(b as ImportLayoutStyle.Block.ImportPackage).isStatic }
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).packageWildcard.toString() == "java\\..+" }

        assertThat(importLayout.layout[3]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[4]).isInstanceOf(ImportLayoutStyle.Block.AllOthers::class.java)
        assertThat(importLayout.layout[5]).isInstanceOf(ImportLayoutStyle.Block.BlankLines::class.java)

        assertThat(importLayout.layout[6])
            .isInstanceOf(ImportLayoutStyle.Block.ImportPackage::class.java)
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).isStatic }
            .matches { b -> (b as ImportLayoutStyle.Block.ImportPackage).packageWildcard.toString() == "java\\..+" }
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
        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(styles))

        assertThat(importLayout.classCountToUseStarImport).isEqualTo(6)
    }
}
