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
package org.openrewrite.java.style;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.style.LineWrapSetting.DoNotWrap;
import static org.openrewrite.style.LineWrapSetting.WrapAlways;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored", "PointlessBooleanExpression"})
class AutodetectTest implements RewriteTest {

    private static JavaParser jp() {
        return JavaParser.fromJavaVersion().build();
    }

    @Test
    void continuationIndent() {
        var cus = jp().parse(
          """
            class Test {
            	boolean eq(){
            		return (1 == 1 &&
            				2 == 2 &&
            				3 == 3);
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Test
    void continuationIndentFromTextBlock() {
        var cus = jp().parse(
          """
            public interface CoffeeRepository {

                static final String QUERY = ""\"
                    SELECT * FROM coffee
                    WHERE size = :size
                    AND price <= :maxPrice
                    ORDER BY price DESC
                    ""\";
                        
                List<String> findByNameContainingIgnoreCase(String name);
                
                List<String> findBySizeAndPriceGreaterThan(String size, int price);
                
                List<Coffee> findAffordableCoffeesBySize(String size,
                                                          int maxPrice);
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }

    @Test
    void continuationIndentFromTextBlockStartingOnNewLine() {
        var cus = jp().parse(
          """
            public interface CoffeeRepository {

                static final String QUERY =
                    ""\"
                        SELECT * FROM coffee
                        WHERE size = :size
                        AND price <= :maxPrice
                        ORDER BY price DESC
                        ""\";
                        
                List<String> findByNameContainingIgnoreCase(String name);
                
                List<String> findBySizeAndPriceGreaterThan(String size, int price);
                
                List<Coffee> findAffordableCoffeesBySize(String size,
                                                          int maxPrice);
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3552")
    @Test
    void continuationIndentFromParameters() {
        var cus = jp().parse(
          """
            class Test {
               void foo(String s1,
                    String s2,
                    String s3) {
               }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3550")
    @Test
    void alignParametersWhenMultiple() {
        var cus = jp().parse(
          """
            class Test {
            	void foo(String s1,
            	         String s2,
            	         String s3) {
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var wrap = detector.build().getStyle(WrappingAndBracesStyle.class);

        assertThat(wrap.getMethodDeclarationParameters().getAlignWhenMultiline()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1221")
    @Test
    void springDemoApp() {
        var cus = jp().parse(
          """
            package com.kmccarpenter.demospring;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            @SpringBootApplication
            public class DemoSpringApplication {

            	public static void main(String[] args) {
            		SpringApplication.run(DemoSpringApplication.class, args);
            	}

            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();

        var tabsAndIndents = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Test
    void springCloudTabsAndIndents() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @SuppressWarnings("InfiniteRecursion")
    @Test
    void spinnakerTabsAndIndents() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }

    @Test
    void rewriteTabsAndIndents() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Test
    void defaultTabIndentSizeToOne() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void tabsDeeperThanBlockDepth() {
        var cus = jp().parse(
          """
            public class Test {
            \tpublic int method(boolean b) {
            \t\tif (b)
            \t\t\t    return 1;
            \t\treturn 0;
            \t}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4() {
        var cus = jp().parse(
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
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    // TabSize 3 is atypical but not unheard of
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize3() {
        var cus = jp().parse(
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
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(3);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(3);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4AndUseTabIsFalse() {
        var cus = jp().parse(
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
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void inconsistentIndents() {
        var cus = jp().parse(
          """
            package org.openrewrite.before;

            import java.util.ArrayList;
            import java.util.List;

            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.print("Hello");
                        System.out.println(" world");
                }
            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4WithSomeErrors() {
        var cus = jp().parse(
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
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Test
    void rewriteImportLayout() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var importLayout = detector.build().getStyle(ImportLayoutStyle.class);


        assertThat(importLayout.getLayout().getFirst()).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);
        assertThat(importLayout.getLayout().get(1)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> "org\\.openrewrite\\.internal\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(3)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(4))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> "javax\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(5))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> "java\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(6)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(7))
          .isInstanceOf(ImportLayoutStyle.Block.AllOthers.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).isStatic());
    }

    @Test
    void staticImports() {
        var cus = jp().parse(
          """
            package com.example;

            import static com.example.Assertions.java;

            import static java.util.Collections.singletonList;
            import static org.assertj.core.api.Assertions.assertThat;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            import java.util.List;
            """,
          """
            package com.example;

            import static com.example.Assertions.java;

            import static java.util.Collections.singletonList;
            import static org.assertj.core.api.Assertions.assertThat;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            import java.util.List;
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var importLayout = detector.build().getStyle(ImportLayoutStyle.class);

        assertThat(importLayout.getLayout().getFirst())
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> "com\\.example\\..+".equals(((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString()));

        assertThat(importLayout.getLayout().get(1)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.AllOthers.class)
          .matches(b -> ((ImportLayoutStyle.Block.AllOthers) b).isStatic());

        assertThat(importLayout.getLayout().get(3)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(4))
          .isInstanceOf(ImportLayoutStyle.Block.AllOthers.class)
          .matches(b -> !((ImportLayoutStyle.Block.AllOthers) b).isStatic());
    }

    @Test
    void detectStarImport() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var importLayout = detector.build().getStyle(ImportLayoutStyle.class);

        assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(6);
    }

    @Test
    void detectImportCounts() {
        var cus = jp().parse(
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
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var importLayout = detector.build().getStyle(ImportLayoutStyle.class);

        assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(2147483647);
        assertThat(importLayout.getNameCountToUseStarImport()).isEqualTo(2147483647);
    }

    @Test
    void staticStarImportCountsFoldedMembersNotVariablesOfThatType() {
        var cus = jp().parse(
          """
            package org.openrewrite.test;

            public class Outer {
                public enum Measure {
                    One, Two, Three, Four, Five, Six;

                    public int label;
                }
            }
            """,
          """
            package org.openrewrite.test;

            import java.util.List;

            import static org.openrewrite.test.Outer.Measure.*;

            class OuterTest {
                List<Object> used() {
                    return List.of(One, Two, Three, Four, Five);
                }

                int parameterized(Outer.Measure measure) {
                    return measure.label;
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var importLayout = detector.build().getStyle(ImportLayoutStyle.class);

        assertThat(importLayout.getNameCountToUseStarImport()).isEqualTo(5);
    }

    @Test
    void detectMethodArgs() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a("a" ,"b" ,"c" ,"d");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Test
    void detectMethodArgAfterComma() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a("a", "b");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @SuppressWarnings({"StatementWithEmptyBody", "RedundantOperationOnEmptyContainer"})
    @Test
    void detectColonInForEachLoop() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    for (int i : new int[]{}) {}
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeColonInForEach()).isTrue();
    }

    @Test
    void detectAfterTypeCast() {
        var cus = jp().parse(
          """
            class T {
                {
                    String s = (String) getString();
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getAfterTypeCast()).isTrue();
    }

    @Test
    void afterTypeCastDefaultsToSpaceWithoutCasts() {
        var cus = jp().parse(
          """
            class T {
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getAfterTypeCast()).isTrue();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void detectBeforeForSemicolon() {
        var cus = jp().parse(
          """
            class T {
                void m() {
                    for (int i = 0; i < x; i++) {}
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeForSemicolon()).isFalse();
        assertThat(spacesStyle.getOther().getAfterForSemicolon()).isTrue();
    }

    @Test
    void detectMethodArgsNoArgs() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a();
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    void detectMethodArgsNoSpaceForComma() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a("a","b","c");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Test
    void detectMethodArgsSpaceForComma() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a("a" , "b" , "c");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    void detectAfterCommaInNewArray() {
        var cus = jp().parse(
          """
            class T {
                static {
                    int[] i = new int[]{1, 2, 3, 4};
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    @Test
    void detectAfterCommaShouldIgnoreFirstElement() {
        var cus = jp().parse(
          """
            class T {
                static {
                    int[] i0 = new int[]{1, 2};
                    int[] i1 = new int[]{2, 3};
                    int[] i2 = new int[]{3, 4};
                    int[] i3 = new int[]{4,5};
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    @Test
    void detectAfterCommaBasedOnLambdas() {
        var cus = jp().parse(
          """
            import java.util.function.BiConsumer;

            class T {
                static {
                    int[] i0 = new int[]{1,2};
                    int[] i1 = new int[]{2,3};

                    BiConsumer<?, ?> c0 = (a, b) -> {};
                    BiConsumer<?, ?> c1 = (a, b) -> {};
                    BiConsumer<?, ?> c2 = (a, b) -> {};
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2098")
    @Test
    void detectNoSpacesWithinMethodCall() {
        var cus = jp().parse(
          """
            class Test {
                void a(String a, String b, String c) {
                }
                void i() {
                    a("a","b","c");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getWithin().getEmptyMethodCallParentheses()).isFalse();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2098")
    @Test
    void detectSpacesWithinMethodCall() {
        var cus = jp().parse(
          """
            class Test {
                void i() {
                    a( "a","b","c" );
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var spacesStyle = detector.build().getStyle(SpacesStyle.class);

        assertThat(spacesStyle.getWithin().getMethodCallParentheses()).isTrue();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void detectElseWithNoNewLine() {
        var cus = jp().parse(
          """
            class Test {
                void method(int n) {
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
        var wrappingAndBracesStyle = detector.build().getStyle(WrappingAndBracesStyle.class);

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isFalse();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void detectElseOnNewLine() {
        var cus = jp().parse(
          """
            class Test {
                void method(int n) {
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
        var wrappingAndBracesStyle = detector.build().getStyle(WrappingAndBracesStyle.class);

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isTrue();
    }

    @Test
    void detectCrlfLineFormat() {
        @SuppressWarnings("TextBlockMigration") var cus = jp().parse(
          "class Test {\r\n" +
          "    // some comment\r\n" +
          "    public void test() {\n" +
          "        System.out.println();\n" +
          "    }\r\n" +
          "}\r\n"
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var lineFormatStyle = detector.build().getStyle(GeneralFormatStyle.class);

        assertThat(lineFormatStyle.isUseCRLFNewLines()).isTrue();
    }

    @Test
    void detectLfLineFormat() {
        @SuppressWarnings("TextBlockMigration") var cus = jp().parse(
          "class Test {\n" +
          "    // some comment\r\n" +
          "    public void test() {\n" +
          "        System.out.println();\n" +
          "    }\n" +
          "}\r\n"
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var lineFormatStyle = detector.build().getStyle(GeneralFormatStyle.class);

        assertThat(lineFormatStyle.isUseCRLFNewLines()).isFalse();
    }

    @Test
    void mostCommonIndentTakesPrecedence() {
        var cus = jp().parse(
          """
            package com.test;

            public class Foo {
               private static final int underIndented;
                     int overIndented;
                  public void setOrder(int order) {
                        this.order = order;
                        System.out.print("One two-space indent shouldn't override predominant 4-space indent");
                        Object o = new Object() {
                              void fooBar() {
                                    System.out.print("fooBar");
                              }
                        };
                  }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(6);
        assertThat(tabsAndIndents.getIndentSize())
          .as("While there are outlier 3 and 9 space indents, the most prevalent indentation is 6")
          .isEqualTo(6);
        assertThat(tabsAndIndents.getContinuationIndent())
          .as("With no actual continuation indents to go off of, assume IntelliJ IDEA default of 2x the normal indent")
          .isEqualTo(12);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "RedundantStreamOptionalCall"})
    @Test
    void continuationIndents() {
        var cus = jp().parse(
          """
            import java.util.stream.Stream;

            class Continuations {
                public void cont() {
                    Stream.of("foo",
                                            "continuation")
                                .map(it ->
                                            Stream.of(it)
                                                        .map(it2 ->
                                                                    Stream.of(it2)
                                                                                .map(it3 ->
                                                                                            it3))
                                                        .flatMap(it4 ->
                                                                    it4));
                    int higherContIndent = 1 +
                                                    2;
                    int lowerContIndent = 1 +
                            2;
                    int sum = 1 +
                                (2 +
                                3) +
                                Stream.of(
                                            2 + 4,
                                            4
                                ).reduce(0,
                                            Integer::sum);
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(12);
    }

    @Nested
    class ContinuationIndentForAnnotations {
        @Issue("https://github.com/openrewrite/rewrite/issues/3568")
        @Test
        void ignoreSpaceBetweenAnnotations() {
            var cus = jp().parse(
              """
                class Test {
                    @SafeVarargs
                    @Deprecated
                    @SuppressWarnings({"mistakes"})
                    boolean count(String... strings) {
                        return strings.length;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .as("With no actual continuation indents to go off of, assume IntelliJ IDEA default of 2x the normal indent")
              .isEqualTo(8);
        }

        @Test
        void includeAnnotationAsAnnotationArg() {
            var cus = jp().parse(
              """
                @interface Foo{}
                @interface Foos{
                    Foo[] value();
                }

                class Test {
                    @Foos(
                       @Foo)
                    boolean count(String... strings) {
                        return strings.length;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .isEqualTo(3);
        }

        @Test
        void includeAnnotationArgArray() {
            var cus = jp().parse(
              """
                @interface Foo{}
                @interface Foos{
                    Foo[] value();
                }

                class Test {
                    @Foos(
                       {@Foo})
                    boolean count(String... strings) {
                        return strings.length;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .isEqualTo(3);
        }

        @ExpectedToFail("existing visitor does not super-visit newArray trees")
        @Test
        void includeAnnotationArgArrayElements() {
            var cus = jp().parse(
              """
                @interface Foo{}
                @interface Foos{
                    Foo[] value();
                }

                class Test {
                    @Foos({
                       @Foo})
                    boolean count(String... strings) {
                        return strings.length;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var tabsAndIndents = detector.build().getStyle(TabsAndIndentsStyle.class);

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .isEqualTo(3);
        }
    }

    @Nested
    class AnnotationWrapping {
        @Test
        void classLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                @Foo
                @Foo
                class Test {
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getClassAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void methodLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    @Foo
                    @Foo
                    void method() {
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getMethodAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void fieldLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    @Foo
                    @Foo
                    private int field;
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getFieldAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void paramLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    void method(@Foo @Foo int param) {
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getParameterAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void localVariableLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    void method() {
                        @Foo @Foo int localVar;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getLocalVariableAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void enumFieldLevelAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                enum MyEnum {
                    @Foo @Foo VALUE
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getEnumFieldAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void recordFieldAnnotations() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                record someRecord(@Foo @Foo String name) {
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getParameterAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void classLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                @Foo @Foo class Test {
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getClassAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void methodLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    @Foo @Foo void method() {
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getMethodAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void fieldLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    @Foo @Foo private int field;
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getFieldAnnotations().getWrap()).isEqualTo(DoNotWrap);
        }

        @Test
        void paramLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    void method(
                      @Foo
                      @Foo
                      int param) {
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getParameterAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void localVariableLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                class Test {
                    void method() {
                        @Foo
                        @Foo
                        int localVar;
                    }
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getLocalVariableAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void enumFieldLevelOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                enum MyEnum {
                    @Foo
                    @Foo
                    VALUE
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getEnumFieldAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

        @Test
        void recordFieldOpposingDefault() {
            var cus = jp().parse(
              """
                import java.lang.annotation.Repeatable;

                @Repeatable(Foo.Foos.class)
                @interface Foo {
                    @interface Foos {
                        Foo[] value();
                    }
                }

                record someRecord(
                  @Foo
                  @Foo
                  String name) {
                }
                """
            );

            var detector = Autodetect.detector();
            cus.forEach(detector::sample);
            var wrapsAndBraces = detector.build().getStyle(WrappingAndBracesStyle.class);
            assertThat(wrapsAndBraces.getParameterAnnotations().getWrap()).isEqualTo(WrapAlways);
        }

    }

    @Nested
    class Merge {
        // Each file disagrees with the others somewhere, so the merged result has to reproduce the vote
        private final List<SourceFile> sources = jp().parse(
          """
            import com.fasterxml.jackson.annotation.JsonCreator;

            import org.openrewrite.internal.StringUtils;
            import org.openrewrite.internal.ListUtils;

            import javax.net.ssl.SSLContext;
            import java.util.List;
            import java.util.Map;

            import static java.util.Collections.emptyList;

            class A {
            	List<String> a(Object o) {
            		if(o == null) {
            			return emptyList();
            		}
            		return (List<String>) o;
            	}
            }
            """,
          """
            import static java.util.Collections.emptyMap;

            import javax.net.ssl.SSLContext;
            import java.io.File;
            import java.util.Map;

            class B {
                Map<String, String> b(int n) {
                    if (n > 0) {
                        return emptyMap();
                    }
                    for(int i = 0;i < n;i++) {
                        foo (i);
                    }
                    return null;
                }
            }
            """,
          """
            import java.io.File;
            import java.util.List;
            import java.util.Set;

            class C {
              @Deprecated
              void c(String s) {
                if(s == null) {
                  s = (String)s;
                }
                else {
                  bar(s , s);
                }
              }
            }
            """,
          "class D {\r\n    void d() {\r\n        try {\r\n        } catch(Exception e) {\r\n        }\r\n    }\r\n}\r\n",
          """
            class E {
                @Deprecated int e;
            }
            """
        ).toList();

        @Test
        void mergedDetectorsMatchOneDetectorThatSampledEverything() {
            var all = detector(sources);
            var first = detector(sources.subList(0, 2));
            var second = detector(sources.subList(2, 3));
            var third = detector(sources.subList(3, 5));

            assertSameStyles(Autodetect.detector().merge(first).merge(second).merge(third), all);
            assertSameStyles(detector(sources.subList(3, 5)).merge(detector(sources.subList(2, 3))).merge(detector(sources.subList(0, 2))), all);
        }

        @Test
        void importLayoutDoesNotDependOnSampleOrder() {
            // Block sequences of the same length, so neither file is the obvious choice
            var tied = jp().parse(
              """
                import java.util.List;

                import org.openrewrite.internal.StringUtils;

                class X {
                }
                """,
              """
                import org.openrewrite.internal.StringUtils;

                import java.util.List;

                class Y {
                }
                """
            ).toList();
            var reversed = new ArrayList<>(tied);
            Collections.reverse(reversed);

            assertThat(detector(reversed).build().getStyle(ImportLayoutStyle.class))
              .usingRecursiveComparison()
              .withEqualsForType(Merge::samePattern, Pattern.class)
              .isEqualTo(detector(tied).build().getStyle(ImportLayoutStyle.class));
        }

        @Test
        void buildIsRepeatable() {
            var detector = detector(sources);
            assertThat(detector.build().getStyles())
              .usingRecursiveComparison()
              .withEqualsForType(Merge::samePattern, Pattern.class)
              .isEqualTo(detector.build().getStyles());
        }

        @Test
        void serializedDetectorsRoundTrip() throws Exception {
            var all = detector(sources);
            for (ObjectMapper mapper : List.of(new ObjectMapper().findAndRegisterModules(), ObjectMappers.propertyBasedMapper(null))) {
                var first = mapper.readValue(mapper.writeValueAsBytes(detector(sources.subList(0, 3))), Autodetect.Detector.class);
                var second = mapper.readValue(mapper.writeValueAsBytes(detector(sources.subList(3, 5))), Autodetect.Detector.class);
                assertSameStyles(first.merge(second), all);
            }
        }

        @Test
        void packageNamesAreSerializedOncePerDetector() throws Exception {
            // java.util is imported by three of the files
            var json = new ObjectMapper().findAndRegisterModules().writeValueAsString(detector(sources));
            assertThat(json.split("\"java\\.util\"", -1)).hasSize(2);
        }

        private Autodetect.Detector detector(List<SourceFile> sources) {
            var detector = Autodetect.detector();
            sources.forEach(detector::sample);
            return detector;
        }

        private void assertSameStyles(Autodetect.Detector actual, Autodetect.Detector expected) {
            assertThat(actual.build().getStyles())
              .usingRecursiveComparison()
              .withEqualsForType(Merge::samePattern, Pattern.class)
              .isEqualTo(expected.build().getStyles());
        }

        private static boolean samePattern(Pattern a, Pattern b) {
            return a.pattern().equals(b.pattern());
        }
    }
}
