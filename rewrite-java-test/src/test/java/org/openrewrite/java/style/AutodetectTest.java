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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3552")
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3550")
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getMethodDeclarationParameters().getAlignWhenMultiple()).isTrue();
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

        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
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
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().get(0)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);
        assertThat(importLayout.getLayout().get(1)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("org\\.openrewrite\\.internal\\..+"));

        assertThat(importLayout.getLayout().get(3)).isInstanceOf(ImportLayoutStyle.Block.BlankLines.class);

        assertThat(importLayout.getLayout().get(4))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("javax\\..+"));

        assertThat(importLayout.getLayout().get(5))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> !((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("java\\..+"));

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
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().get(0))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).isStatic())
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("com\\.example\\..+"));

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
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(2147483647);
        assertThat(importLayout.getNameCountToUseStarImport()).isEqualTo(2147483647);
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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var lineFormatStyle = NamedStyles.merge(GeneralFormatStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var lineFormatStyle = NamedStyles.merge(GeneralFormatStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

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
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(12);
    }

    @Nested
    class ContinuationIndentForAnnotations {
        @Test
        @Issue("https://github.com/openrewrite/rewrite/issues/3568")
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
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

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
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

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
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .isEqualTo(3);
        }

        @Test
        @ExpectedToFail("existing visitor does not super-visit newArray trees")
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
            var styles = detector.build();
            var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
            assertThat(tabsAndIndents.getContinuationIndent())
              .isEqualTo(3);
        }
    }
}
