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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class AddOrUpdateAnnotationAttributeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
          //language=java
          """
            package org.example;
            public enum FooEnum { ONE, TWO, THREE, FOUR, JUNK }
            """,
          //language=java
          """
            package org.example;
            public class Const {
                public class X {
                    public class Y {
                        public static final String FIRST_CONST = "a";
                        public static final String SECOND_CONST = "b";
                        public static final String THIRD_CONST = "c";
                        public static final String FOURTH_CONST = "d";
                    }
                }
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdsWrapper { FooDefaultString[] value(); }
            @Repeatable(FdsWrapper.class)
            public @interface FooDefaultString {
                String value() default "";
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdsaWrapper { FooDefaultStringArray[] value(); }
            @Repeatable(FdsaWrapper.class)
            public @interface FooDefaultStringArray {
                String[] value() default {};
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdcWrapper { FooDefaultClass[] value(); }
            @Repeatable(FdcWrapper.class)
            public @interface FooDefaultClass {
                Class<? extends Number> value() default Number.class;
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdcaWrapper { FooDefaultClassArray[] value(); }
            @Repeatable(FdcaWrapper.class)
            public @interface FooDefaultClassArray {
                Class<? extends Number>[] value() default {};
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdeWrapper { FooDefaultEnum[] value(); }
            @Repeatable(FdeWrapper.class)
            public @interface FooDefaultEnum {
                FooEnum value() default FooEnum.JUNK;
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdeaWrapper { FooDefaultEnumArray[] value(); }
            @Repeatable(FdeaWrapper.class)
            public @interface FooDefaultEnumArray {
                FooEnum[] value() default {};
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdlWrapper { FooDefaultLong[] value(); }
            @Repeatable(FdlWrapper.class)
            public @interface FooDefaultLong {
                long value() default 0L;
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdlaWrapper { FooDefaultLongArray[] value(); }
            @Repeatable(FdlaWrapper.class)
            public @interface FooDefaultLongArray {
                long[] value() default {};
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdbWrapper { FooDefaultBoolean[] value(); }
            @Repeatable(FdbWrapper.class)
            public @interface FooDefaultBoolean {
                boolean value() default false;
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """,
          //language=java
          """
            package org.example;
            import java.lang.annotation.Repeatable;
            @interface FdbaWrapper { FooDefaultBooleanArray[] value(); }
            @Repeatable(FdbaWrapper.class)
            public @interface FooDefaultBooleanArray {
                boolean[] value() default {};
                String str() default "";
                String[] strArr() default {};
                Class<? extends Number> cla() default Number.class;
                Class<? extends Number>[] claArr() default {};
                FooEnum enu() default FooEnum.JUNK;
                FooEnum[] enuArr() default {};
                long lon() default 0L;
                long[] lonArr() default {};
                boolean boo() default false;
                boolean[] booArr() default {};
            }
            """
        ));
    }

    @Nested
    class UsingImplicitAttributeName {
        @Nested
        class UsingNullAttributeValue {
            @Nested
            class WithLiteralTypeAttribute {
                @Test
                void literalAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString(str = "a")
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString(Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = "b")
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "a", str = "z")
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST, str = "z")
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString(Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = "b")
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST)
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString
                          @FooDefaultString
                          @FooDefaultString
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "a", str = "z1")
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST, str = "z2")
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(str = "z1")
                          @FooDefaultString(str = "z2")
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_asConst_usingProvidedOldAttributeValue_ofConstRef_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, "Const.X.Y.FIRST_CONST", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(Const.X.Y.FIRST_CONST)
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST)
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST, str = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST, str = Const.X.Y.FIRST_CONST)
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(str = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST, str = Const.X.Y.FIRST_CONST)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, "a", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString("b")
                          @FooDefaultString(value = "a")
                          @FooDefaultString(value = "b")
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString("b")
                          @FooDefaultString
                          @FooDefaultString("b")
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "a", str = "a")
                          @FooDefaultString(value = "b", str = "a")
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString(str = "a")
                          @FooDefaultString(value = "b", str = "a")
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithLiteralArrayTypeAttribute {
                @Test
                void literalArrayAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray
                          @FooDefaultStringArray()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(strArr = "a")
                          @FooDefaultStringArray(strArr = {"b"})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalArrayAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray("a")
                          @FooDefaultStringArray(value = "b")
                          @FooDefaultStringArray({})
                          @FooDefaultStringArray({"c"})
                          @FooDefaultStringArray(value = {})
                          @FooDefaultStringArray(value = {"d"})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(value = "a", strArr = {"z"})
                          @FooDefaultStringArray(value = {}, strArr = {"y"})
                          @FooDefaultStringArray(value = {"d"}, strArr = {"x"})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalArrayAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray("a")
                          @FooDefaultStringArray(Const.X.Y.FIRST_CONST)
                          @FooDefaultStringArray(value = "b")
                          @FooDefaultStringArray(Const.X.Y.FIRST_CONST)
                          @FooDefaultStringArray({})
                          @FooDefaultStringArray({"c"})
                          @FooDefaultStringArray({Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = {})
                          @FooDefaultStringArray(value = {"d"})
                          @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST})
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(value = "b", strArr = {"z1"})
                          @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST, strArr = {"z2"})
                          @FooDefaultStringArray(value = {}, strArr = {"y1"})
                          @FooDefaultStringArray(value = {"d"}, strArr = {"y2"})
                          @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST}, strArr = {"y3"})
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(strArr = {"z1"})
                          @FooDefaultStringArray(strArr = {"z2"})
                          @FooDefaultStringArray(strArr = {"y1"})
                          @FooDefaultStringArray(strArr = {"y2"})
                          @FooDefaultStringArray(strArr = {"y3"})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalArrayAttribute_existing_asConst_usingProvidedOldAttributeValue_ofConstRef_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, null, "Const.X.Y.FIRST_CONST", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(Const.X.Y.FIRST_CONST)
                          @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                          @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST)
                          @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST)
                          @FooDefaultStringArray({Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                          @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray
                          @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                          @FooDefaultStringArray
                          @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                          @FooDefaultStringArray
                          @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.THIRD_CONST})
                          @FooDefaultStringArray
                          @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.THIRD_CONST})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST, strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(strArr = {Const.X.Y.FIRST_CONST})
                          @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalArrayAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, null, "a", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray("a")
                          @FooDefaultStringArray("b")
                          @FooDefaultStringArray(value = "a")
                          @FooDefaultStringArray(value = "b")
                          @FooDefaultStringArray({})
                          @FooDefaultStringArray({"a"})
                          @FooDefaultStringArray({"b", "a", "c"})
                          @FooDefaultStringArray(value = {})
                          @FooDefaultStringArray(value = {"a"})
                          @FooDefaultStringArray(value = {"b", "a", "c"})
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray
                          @FooDefaultStringArray("b")
                          @FooDefaultStringArray
                          @FooDefaultStringArray("b")
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray({"b", "c"})
                          @FooDefaultStringArray
                          @FooDefaultStringArray
                          @FooDefaultStringArray({"b", "c"})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(value = "a", strArr = {"a"})
                          @FooDefaultStringArray(value = "b", strArr = {"a"})
                          @FooDefaultStringArray(value = {}, strArr = {"a"})
                          @FooDefaultStringArray(value = {"a"}, strArr = {"a"})
                          @FooDefaultStringArray(value = {"b", "a", "c"}, strArr = {"a"})
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultStringArray;
                          @FooDefaultStringArray(strArr = {"a"})
                          @FooDefaultStringArray(value = "b", strArr = {"a"})
                          @FooDefaultStringArray(strArr = {"a"})
                          @FooDefaultStringArray(strArr = {"a"})
                          @FooDefaultStringArray(value = {"b", "c"}, strArr = {"a"})
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithClassTypeAttribute {
                @Test
                void classAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass
                          @FooDefaultClass()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(cla = Integer.class)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(Integer.class)
                          @FooDefaultClass(Long.class)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(value = Integer.class, cla = Long.class)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(Integer.class)
                          @FooDefaultClass(Long.class)
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass
                          @FooDefaultClass
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(value = Long.class, cla = Byte.class)
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(cla = Byte.class)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, null, "Integer.class", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(Integer.class)
                          @FooDefaultClass(Long.class)
                          @FooDefaultClass(value = Integer.class)
                          @FooDefaultClass(value = Long.class)
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass
                          @FooDefaultClass(Long.class)
                          @FooDefaultClass
                          @FooDefaultClass(Long.class)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(value = Integer.class, cla = Integer.class)
                          @FooDefaultClass(value = Long.class, cla = Integer.class)
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultClass;
                          @FooDefaultClass(cla = Integer.class)
                          @FooDefaultClass(value = Long.class, cla = Integer.class)
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithClassArrayTypeAttribute {
                @Test
                void classArrayAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClassArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray
                          @FooDefaultClassArray()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(claArr = Integer.class)
                          @FooDefaultClassArray(claArr = {Long.class})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classArrayAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClassArray", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(Integer.class)
                          @FooDefaultClassArray(value = Long.class)
                          @FooDefaultClassArray({})
                          @FooDefaultClassArray({Short.class})
                          @FooDefaultClassArray(value = {})
                          @FooDefaultClassArray(value = {Byte.class})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(value = Long.class, claArr = {Byte.class})
                          @FooDefaultClassArray(value = {}, claArr = {Integer.class})
                          @FooDefaultClassArray(value = {Long.class}, claArr = {Integer.class})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classArrayAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClassArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(Integer.class)
                          @FooDefaultClassArray(value = Long.class)
                          @FooDefaultClassArray({})
                          @FooDefaultClassArray({Short.class})
                          @FooDefaultClassArray(value = {})
                          @FooDefaultClassArray(value = {Byte.class})
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(value = Long.class, claArr = {Byte.class})
                          @FooDefaultClassArray(value = {}, claArr = {Float.class})
                          @FooDefaultClassArray(value = {Long.class}, claArr = {Integer.class})
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(claArr = {Byte.class})
                          @FooDefaultClassArray(claArr = {Float.class})
                          @FooDefaultClassArray(claArr = {Integer.class})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void classArrayAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClassArray", null, null, "Integer.class", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(Integer.class)
                          @FooDefaultClassArray(Long.class)
                          @FooDefaultClassArray(value = Integer.class)
                          @FooDefaultClassArray(value = Long.class)
                          @FooDefaultClassArray({})
                          @FooDefaultClassArray({Integer.class})
                          @FooDefaultClassArray({Long.class, Integer.class, Short.class})
                          @FooDefaultClassArray(value = {})
                          @FooDefaultClassArray(value = {Integer.class})
                          @FooDefaultClassArray(value = {Long.class, Integer.class, Short.class})
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray
                          @FooDefaultClassArray(Long.class)
                          @FooDefaultClassArray
                          @FooDefaultClassArray(Long.class)
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray({Long.class, Short.class})
                          @FooDefaultClassArray
                          @FooDefaultClassArray
                          @FooDefaultClassArray({Long.class, Short.class})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(value = Integer.class, claArr = {Integer.class})
                          @FooDefaultClassArray(value = Long.class, claArr = {Integer.class})
                          @FooDefaultClassArray(value = {}, claArr = {Integer.class})
                          @FooDefaultClassArray(value = {Integer.class}, claArr = {Integer.class})
                          @FooDefaultClassArray(value = {Long.class, Integer.class, Short.class}, claArr = {Integer.class})
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultClassArray;
                          @FooDefaultClassArray(claArr = {Integer.class})
                          @FooDefaultClassArray(value = Long.class, claArr = {Integer.class})
                          @FooDefaultClassArray(claArr = {Integer.class})
                          @FooDefaultClassArray(claArr = {Integer.class})
                          @FooDefaultClassArray(value = {Long.class, Short.class}, claArr = {Integer.class})
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithEnumTypeAttribute {
                @Test
                void enumAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnum", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          @FooDefaultEnum
                          @FooDefaultEnum()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(enu = FooEnum.ONE)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnum", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(FooEnum.ONE)
                          @FooDefaultEnum(FooEnum.TWO)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(value = FooEnum.ONE, enu = FooEnum.TWO)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnum", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(FooEnum.ONE)
                          @FooDefaultEnum(FooEnum.TWO)
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum
                          @FooDefaultEnum
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(value = FooEnum.ONE, enu = FooEnum.TWO)
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(enu = FooEnum.TWO)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnum", null, null, "FooEnum.ONE", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(FooEnum.ONE)
                          @FooDefaultEnum(FooEnum.TWO)
                          @FooDefaultEnum(value = FooEnum.ONE)
                          @FooDefaultEnum(value = FooEnum.TWO)
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum
                          @FooDefaultEnum(FooEnum.TWO)
                          @FooDefaultEnum
                          @FooDefaultEnum(FooEnum.TWO)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(value = FooEnum.ONE, enu = FooEnum.ONE)
                          @FooDefaultEnum(value = FooEnum.TWO, enu = FooEnum.ONE)
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultEnum;
                          import org.example.FooEnum;
                          @FooDefaultEnum(enu = FooEnum.ONE)
                          @FooDefaultEnum(value = FooEnum.TWO, enu = FooEnum.ONE)
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithEnumArrayTypeAttribute {
                @Test
                void enumArrayAttribute_absent_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnumArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray()
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(enuArr = FooEnum.ONE)
                          @FooDefaultEnumArray(enuArr = {FooEnum.TWO})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumArrayAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnumArray", null, null, null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(FooEnum.ONE)
                          @FooDefaultEnumArray(value = FooEnum.TWO)
                          @FooDefaultEnumArray({})
                          @FooDefaultEnumArray({FooEnum.THREE})
                          @FooDefaultEnumArray(value = {})
                          @FooDefaultEnumArray(value = {FooEnum.FOUR})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(value = FooEnum.ONE, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {}, enuArr = {FooEnum.TWO})
                          @FooDefaultEnumArray(value = {FooEnum.THREE}, enuArr = {FooEnum.THREE})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumArrayAttribute_existing_usingNullOldAttributeValue_removesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnumArray", null, null, null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(FooEnum.ONE)
                          @FooDefaultEnumArray(value = FooEnum.TWO)
                          @FooDefaultEnumArray({})
                          @FooDefaultEnumArray({FooEnum.THREE})
                          @FooDefaultEnumArray(value = {})
                          @FooDefaultEnumArray(value = {FooEnum.FOUR})
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(value = FooEnum.ONE, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {}, enuArr = {FooEnum.TWO})
                          @FooDefaultEnumArray(value = {FooEnum.THREE}, enuArr = {FooEnum.THREE})
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(enuArr = {FooEnum.TWO})
                          @FooDefaultEnumArray(enuArr = {FooEnum.THREE})
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void enumArrayAttribute_existing_usingProvidedOldAttributeValue_removesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultEnumArray", null, null, "FooEnum.ONE", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(FooEnum.ONE)
                          @FooDefaultEnumArray(FooEnum.TWO)
                          @FooDefaultEnumArray(value = FooEnum.ONE)
                          @FooDefaultEnumArray(value = FooEnum.TWO)
                          @FooDefaultEnumArray({})
                          @FooDefaultEnumArray({FooEnum.ONE})
                          @FooDefaultEnumArray({FooEnum.TWO, FooEnum.ONE, FooEnum.THREE})
                          @FooDefaultEnumArray(value = {})
                          @FooDefaultEnumArray(value = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {FooEnum.TWO, FooEnum.ONE, FooEnum.THREE})
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray(FooEnum.TWO)
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray(FooEnum.TWO)
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray({FooEnum.TWO, FooEnum.THREE})
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray
                          @FooDefaultEnumArray({FooEnum.TWO, FooEnum.THREE})
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(value = FooEnum.ONE, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = FooEnum.TWO, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {}, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {FooEnum.ONE}, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {FooEnum.TWO, FooEnum.ONE, FooEnum.THREE}, enuArr = {FooEnum.ONE})
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultEnumArray;
                          import org.example.FooEnum;
                          @FooDefaultEnumArray(enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = FooEnum.TWO, enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(enuArr = {FooEnum.ONE})
                          @FooDefaultEnumArray(value = {FooEnum.TWO, FooEnum.THREE}, enuArr = {FooEnum.ONE})
                          public class B {}
                          """
                      )
                    );
                }
            }

            // TODO: long and boolean versions?
        }

        @Nested
        class UsingLiteralAttributeValue {
            @Nested
            class WithLiteralTypeAttribute {
                @Test
                void literalAttribute_absent_usingNullOldAttributeValue_addsSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "a", null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString()
                          @FooDefaultString(str = "b")
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString("a")
                          @FooDefaultString(value = "a", str = "b")
                          public class A {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_absent_usingLiteralOldAttributeValue_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "a", "b", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString
                          @FooDefaultString()
                          @FooDefaultString(str = "b")
                          public class A {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingAddOnly_doesNothing() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "a", null, true, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("b")
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString(value = "b")
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST)
                          public class A {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingNullOldAttributeValue_updatesSafely() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "a", null, null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("b")
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString(value = "c")
                          @FooDefaultString(value = Const.X.Y.THIRD_CONST)
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString("a")
                          @FooDefaultString("a")
                          @FooDefaultString("a")
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "b", str = "b")
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST, str = Const.X.Y.SECOND_CONST)
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "a", str = "b")
                          @FooDefaultString(value = "a", str = Const.X.Y.SECOND_CONST)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_asConst_usingProvidedOldAttributeValue_ofConstRef_updatesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "b", "Const.X.Y.FIRST_CONST", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(Const.X.Y.FIRST_CONST)
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST)
                          public class A {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString("b")
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          @FooDefaultString("b")
                          @FooDefaultString(Const.X.Y.SECOND_CONST)
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = Const.X.Y.FIRST_CONST, str = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST, str = Const.X.Y.FIRST_CONST)
                          public class B {}
                          """,
                        """
                          import org.example.Const;
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "b", str = Const.X.Y.FIRST_CONST)
                          @FooDefaultString(value = Const.X.Y.SECOND_CONST, str = Const.X.Y.FIRST_CONST)
                          public class B {}
                          """
                      )
                    );
                }

                @Test
                void literalAttribute_existing_usingProvidedOldAttributeValue_updatesSafelyOnlyMatched() {
                    rewriteRun(
                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "b", "a", null, null)),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString("a")
                          @FooDefaultString("c")
                          @FooDefaultString(value = "a")
                          @FooDefaultString(value = "c")
                          public class A {}
                          """,
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString("b")
                          @FooDefaultString("c")
                          @FooDefaultString("b")
                          @FooDefaultString("c")
                          public class A {}
                          """
                      ),
                      //language=java
                      java(
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "a", str = "a")
                          @FooDefaultString(value = "c", str = "a")
                          public class B {}
                          """,
                        """
                          import org.example.FooDefaultString;
                          @FooDefaultString(value = "b", str = "a")
                          @FooDefaultString(value = "c", str = "a")
                          public class B {}
                          """
                      )
                    );
                }
            }

            @Nested
            class WithLiteralArrayTypeAttribute {
                @Nested
                class UsingSingularValue {
                    @Test
                    void literalArrayAttribute_absent_usingNullOldAttributeValue_addsSafely() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "a", null, null, null)),
                          //language=java
                          java(
                            """
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray
                              @FooDefaultStringArray()
                              @FooDefaultStringArray(strArr = {"b"})
                              public class A {}
                              """,
                            """
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray(value = "a", strArr = {"b"})
                              public class A {}
                              """
                          )
                        );
                    }

                    @Test
                    void literalArrayAttribute_absent_usingLiteralOldAttributeValue_doesNothing() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "a", "b", null, null)),
                          //language=java
                          java(
                            """
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray
                              @FooDefaultStringArray()
                              @FooDefaultStringArray(strArr = {"b"})
                              public class A {}
                              """
                          )
                        );
                    }

                    @Test
                    void literalArrayAttribute_existing_usingAddOnly_doesNothing() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "a", null, true, null)),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray(value = "b")
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray({"b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"b"})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST})
                              public class A {}
                              """
                          )
                        );
                    }

                    // TODO: revisit
                    @Test
                    void literalArrayAttribute_existing_usingNullOldAttributeValue_updatesSafely() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "a", null, null, null)),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray(value = "c")
                              @FooDefaultStringArray(value = Const.X.Y.THIRD_CONST)
                              @FooDefaultStringArray({"b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST})
                              // TODO: The two below are curious simplifications
                              @FooDefaultStringArray(value = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.THIRD_CONST})
                              public class A {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray({"a"})
                              @FooDefaultStringArray({"a"})
                              // TODO: The two below are curious simplifications
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              public class A {}
                              """
                          ),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = "b", strArr = {"b"})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"c"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.THIRD_CONST})
                              public class B {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = "a", strArr = {"b"})
                              @FooDefaultStringArray(value = "a", strArr = {Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"a"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {"a"}, strArr = {Const.X.Y.THIRD_CONST})
                              public class B {}
                              """
                          )
                        );
                    }

                    @Test
                    void literalArrayAttribute_existing_usingNullOldAttributeValue_andAppendArray_appendsSafely() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "a", null, null, true)),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray(value = "c")
                              @FooDefaultStringArray(value = Const.X.Y.THIRD_CONST)
                              @FooDefaultStringArray({"b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.THIRD_CONST})
                              // below already contain the value to append
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray(value = "a")
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST})
                              public class A {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray({"b", "a"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "a"})
                              @FooDefaultStringArray({"c", "a"})
                              @FooDefaultStringArray({Const.X.Y.THIRD_CONST, "a"})
                              @FooDefaultStringArray({"b", "a"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "a"})
                              @FooDefaultStringArray({"c", "a"})
                              @FooDefaultStringArray({Const.X.Y.THIRD_CONST, "a"})
                              // below already contain the value to append
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray("a")
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST})
                              public class A {}
                              """
                          ),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = "b", strArr = {"b"})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"c"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.THIRD_CONST})
                              // below already contain the value to append
                              @FooDefaultStringArray(value = "a", strArr = {"b"})
                              @FooDefaultStringArray(value = {"a"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST}, strArr = {"d"})
                              public class B {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = {"b", "a"}, strArr = {"b"})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "a"}, strArr = {Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {"c", "a"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.THIRD_CONST, "a"}, strArr = {Const.X.Y.THIRD_CONST})
                              // below already contain the value to append
                              @FooDefaultStringArray(value = "a", strArr = {"b"})
                              @FooDefaultStringArray(value = {"a"}, strArr = {"c"})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "a", Const.X.Y.THIRD_CONST}, strArr = {"d"})
                              public class B {}
                              """
                          )
                        );
                    }

                    // TODO: revisit
                    @Test
                    void literalArrayAttribute_existing_asConst_usingProvidedOldAttributeValue_ofConstRef_updatesSafelyOnlyMatched() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b", "Const.X.Y.FIRST_CONST", null, null)),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(Const.X.Y.FIRST_CONST)
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST)
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray({})
                              // TODO: Try to make below consistent
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                              @FooDefaultStringArray(value = {})
                              @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                              public class A {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray
                              // TODO: Try to make below consistent
                              @FooDefaultStringArray({"b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "b", Const.X.Y.THIRD_CONST})
                              @FooDefaultStringArray
                              @FooDefaultStringArray("b")
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, "b", Const.X.Y.THIRD_CONST})
                              public class A {}
                              """
                          ),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              public class B {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = "b", strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {"b"}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "b", Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              public class B {}
                              """
                          )
                        );
                    }

                    // TODO: revisit
                    @Test
                    void literalArrayAttribute_existing_asConst_usingProvidedOldAttributeValue_ofConstRef_andAppendArray_appendsSafelyOnlyForMatched() {
                        rewriteRun(
                          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b", "Const.X.Y.FIRST_CONST", null, true)),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(Const.X.Y.FIRST_CONST)
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST)
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray({})
                              // TODO: Try to make below consistent
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                              @FooDefaultStringArray(value = {})
                              @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST})
                              // below already contain the value to append
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b", Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST, "b", Const.X.Y.SECOND_CONST})
                              public class A {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b"})
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b"})
                              @FooDefaultStringArray(Const.X.Y.SECOND_CONST)
                              @FooDefaultStringArray
                              // TODO: Try to make below consistent
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST, "b"})
                              @FooDefaultStringArray
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b"})
                              @FooDefaultStringArray({Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST, "b"})
                              // below already contain the value to append
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b", Const.X.Y.SECOND_CONST})
                              @FooDefaultStringArray({Const.X.Y.FIRST_CONST, "b", Const.X.Y.SECOND_CONST})
                              public class A {}
                              """
                          ),
                          //language=java
                          java(
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = Const.X.Y.FIRST_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.FIRST_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, Const.X.Y.FIRST_CONST, Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              public class B {}
                              """,
                            """
                              import org.example.Const;
                              import org.example.FooDefaultStringArray;
                              @FooDefaultStringArray(value = "b", strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = Const.X.Y.SECOND_CONST, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {"b"}, strArr = {Const.X.Y.FIRST_CONST})
                              @FooDefaultStringArray(value = {Const.X.Y.SECOND_CONST, "b", Const.X.Y.THIRD_CONST}, strArr = {Const.X.Y.FIRST_CONST})
                              public class B {}
                              """
                          )
                        );
                    }
                }





//                @Test
//                void literalAttribute_existing_usingProvidedOldAttributeValue_updatesSafelyOnlyMatched() {
//                    rewriteRun(
//                      spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "b", "a", null, null)),
//                      //language=java
//                      java(
//                        """
//                          import org.example.FooDefaultString;
//                          @FooDefaultString("a")
//                          @FooDefaultString("c")
//                          @FooDefaultString(value = "a")
//                          @FooDefaultString(value = "c")
//                          public class A {}
//                          """,
//                        """
//                          import org.example.FooDefaultString;
//                          @FooDefaultString("b")
//                          @FooDefaultString("c")
//                          @FooDefaultString("b")
//                          @FooDefaultString("c")
//                          public class A {}
//                          """
//                      ),
//                      //language=java
//                      java(
//                        """
//                          import org.example.FooDefaultString;
//                          @FooDefaultString(value = "a", a = "a")
//                          @FooDefaultString(value = "c", a = "a")
//                          public class B {}
//                          """,
//                        """
//                          import org.example.FooDefaultString;
//                          @FooDefaultString(value = "b", a = "a")
//                          @FooDefaultString(value = "c", a = "a")
//                          public class B {}
//                          """
//                      )
//                    );
//                }
            }

            // TODO: nested `WithLiteralArrayTypeAttribute`
            // TODO: nested `WithClassTypeAttribute`
            // TODO: nested `WithClassArrayTypeAttribute`
        }

        // TODO: Check on `addOnly` and `appendArray` effects
        // TODO: Pull other checks from below up, such as not getting confused by fields of the same name during qualified constant checks
    }

    // TODO: all the above, but for explicit attribute

    @DocumentExample
    @Test
    void addValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo("hello")
              public class A {
              }
              """
          )
        );
    }

    @Test
    void literalToListFromSingleValueUsingAppendArray() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", "strArr", "xyz", null, null, true)),
          //language=java
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = "abc")
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"abc", "xyz"})
              public class A {}
              """
          )
        );
    }

    @Test
    void addValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, "Integer.class", null, null, null)),
          java(
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass
              public class A {}
              """,
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass(Integer.class)
              public class A {}
              """
          )
        );
    }

    @Test
    void addValueAttributeFullyQualifiedClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, "java.math.BigDecimal.class", null, null, null)),
          java(
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass
              public class A {}
              """,
            """
              import org.example.FooDefaultClass;
              
              import java.math.BigDecimal;

              @FooDefaultClass(BigDecimal.class)
              public class A {}
              """
          )
        );
    }

    @Test
    void updateValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "hello", null, null, null)),
          java(
            """
              import org.example.FooDefaultString;

              @FooDefaultString("goodbye")
              public class A {}
              """,
            """
              import org.example.FooDefaultString;

              @FooDefaultString("hello")
              public class A {}
              """
          )
        );
    }

    @Test
    void updateValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, "Integer.class", null, null, null)),
          java(
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass(Long.class)
              public class A {}
              """,
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass(Integer.class)
              public class A {}
              """
          )
        );
    }

    @Test
    void removeValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, null, null, null, null)),
          java(
            """
              import org.example.FooDefaultString;

              @FooDefaultString("goodbye")
              public class A {}
              """,
            """
              import org.example.FooDefaultString;

              @FooDefaultString
              public class A {}
              """
          )
        );
    }

    @Test
    void removeValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, null, null, null, null)),
          java(
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass(Long.class)
              public class A {}
              """,
            """
              import org.example.FooDefaultClass;

              @FooDefaultClass
              public class A {}
              """
          )
        );
    }

    @Test
    void removeExplicitAttributeNameWhenRemovingValue() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", "str", null, null, null, null)),
          java(
            """
              import org.example.FooDefaultString;
              
              @FooDefaultString(value = "newTest1", str = "newTest2")
              public class A {}
              """,
            """
              import org.example.FooDefaultString;
              
              @FooDefaultString("newTest1")
              public class A {}
              """
          )
        );
    }

    @Test
    void addNamedAttribute() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(timeout = 500)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(timeout = 1)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(timeout = 500)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", null, null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(timeout = 1)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveExistingAttributes() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
                  String foo() default "";
              }
              """
          ),

          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(foo = "")
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(timeout = 500, foo = "")
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitValueToExplicitValue() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test("foo")
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(other = 1, value = "foo")
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitValueToExplicitValueClass() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  Class<? extends Number> value();
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(Integer.class)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;

              class SomeTest {

                  @Test(other = 1, value = Integer.class)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void dontChangeWhenSetToAddOnly() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, true, false)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.junit.Test;

              class SomeTest {
                  @Test(other = 0)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void changeWhenSetTargetsNonUsedMethod() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "value", "1", null, true, false)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  int value() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
                  @Test(other = 0)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
                  @Test(value = 1, other = 0)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = {"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"newTest"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInAnnotationValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            null,
            "newTest",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo({"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo({"newTest"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInAnnotationExplicitValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            null,
            "newTest",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(value = {"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo("newTest")
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInputMoreThanOneInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = {"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInputMoreThanOneInAnnotationLiteralAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = "oldTest")
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"oldTest", "newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInputMoreThanOneInAnnotationLiteralValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            null,
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo("oldTest")
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo({"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInputMoreThanOneInAnnotationValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            null,
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo({"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo({"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttributeWithAppendTrue() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttributeEmptyBraces() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo()
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void removeArrayInputInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            null,
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addOtherAttributeInArrayAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "string",
            "test",
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
                  String string() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(string = "test", array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addOtherAttributeInArrayValueAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "string",
            "test",
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
                  String string() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo({"newTest1", "newTest2"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(string = "test", value = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void dontChangeWhenAttributeDoesNotMatchAtAll() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo({"a"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendSingleValueToExistingArrayAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo(array = {"a"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(array = {"a", "b"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendSingleValueToExistingArrayValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            null,
            "b",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] value() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;

              @Foo({"a"})
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo({"a", "b"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", "strArr", "b,c", null, false, true)),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.FooDefaultStringArray",
            null,
            "b,c",
            null,
            false,
            true
          )),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayAttributeWithOverlap() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray","strArr", "b,c", null, false, true)),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a", "b"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayValueAttributeWithOverlap() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.FooDefaultStringArray",
            null,
            "b,c",
            null,
            false,
            true
          )),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a", "b"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    // literal array attrType, explicit non-default attrName, string literal array attrVal, null oldAttrVal, false addOnly (does not matter)
    @Test
    void appendMultipleValuesToExistingArrayAttributeNonSet() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", "strArr", "b,c", null, false, true)),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a", "b"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray(strArr = {"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    // literal array attrType, implicit "value" attrName, string literal array attrVal, null oldAttrVal, false addOnly (does not matter), true appendArray
    @Test
    void appendMultipleValuesToExistingArrayValueAttributeNonSet() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b,c", null, false, true)),
          java(
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a", "b"})
              public class A {}
              """,
            """
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"a", "b", "c"})
              public class A {}
              """
          )
        );
    }

    // literal attrType, explicit "value" attrName, string literal attrVal, null oldAttrVal, false addOnly (does not matter)
    @Test
    void updateConstantWithValue() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", "value", "hello", null, false, null)),
          java(
            """
              import org.example.Const;
              import org.example.FooDefaultString;

              @FooDefaultString(value = Const.X.Y.FIRST_CONST)
              public class A {}
              """,
            """
              import org.example.Const;
              import org.example.FooDefaultString;

              @FooDefaultString("hello")
              public class A {}
              """
          )
        );
    }

    @Disabled("There is no way to determine right now to determine whether the `attributeValue` is meant as constant or as String value")
    @Test
    void updateConstantWithConstant() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", "value", "Const.A.B.BYE", null, false, null)),
          java(
            """
              package org.example;

              public class Const {
                  public class A {
                      public class B {
                          public static final String HI = "hi";
                          public static final String BYE = "bye";
                      }
                  }
              }
              """
          ),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              import org.example.Const;

              @Foo(value = Const.A.B.HI)
              public class A {
              }
              """,
            """
              import org.example.Foo;
              import org.example.Const;

              @Foo(value = Const.A.B.HI2)
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addAttributeToNestedAnnotationArray() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Bar",
            "attribute",
            "",
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Bar[] array() default {};
              }
              """
          ),
          java(
            """
              package org.example;
              public @interface Bar {
                  String attribute() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              import org.example.Bar;

              @Foo(array = { @Bar() })
              public class A {
              }
              """,
            """
              import org.example.Foo;
              import org.example.Bar;

              @Foo(array = { @Bar(attribute = "") })
              public class A {
              }
              """
          )
        );
    }

    @Nested
    class OnMatch {
        @Test
        void matchValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "hello", "goodbye", null, null)),
              java(
                """
                  import org.example.FooDefaultString;

                  @FooDefaultString("goodbye")
                  public class A {}
                  """,
                """
                  import org.example.FooDefaultString;

                  @FooDefaultString("hello")
                  public class A {}
                  """
              )
            );
        }

        @Test
        void matchConstant() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", null, "a", "Const.X.Y.FIRST_CONST", false, null)),
              java(
                """
                  import org.example.Const;
                  import org.example.FooDefaultString;
    
                  @FooDefaultString(Const.X.Y.FIRST_CONST)
                  public class A {}
                  """,
                """
                  import org.example.Const;
                  import org.example.FooDefaultString;
    
                  @FooDefaultString("a")
                  public class A {}
                  """
              )
            );
        }

        @Disabled("We can't support this right now, as there is no reference to the actual string literal of the constant")
        @Test
        void matchConstantLiteral() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", "hi", false, null)),
              java(
                """
                  package org.example;
                  
                  public class Const {
                      public class A {
                          public class B {
                              public static final String HI = "hi";
                          }
                      }
                  }
                  """
              ),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String value() default "";
                  }
                  """
              ),
              java(
                """
                  import org.example.Foo;
                  import org.example.Const;
    
                  @Foo(Const.A.B.HI)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  import org.example.Const;
    
                  @Foo("hello")
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchEnumValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Values.TWO", "Values.ONE", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Values value() default "";
                  }
                  public enum Values {ONE, TWO}
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;

                  @Foo(Values.ONE)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;

                  @Foo(Values.TWO)
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void replaceValueArrayWhenSingleValueMatchesImplicitArray() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Values.TWO,Values.THREE", "Values.ONE", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Values[] value() default "";
                  }
                  public enum Values {ONE, TWO,THREE}
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(Values.ONE)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo({Values.TWO, Values.THREE})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
         void addExplicitValueToImplicitArrayWhenAddingNewAttribute() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", "name", "hello", null, null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String name() default "";
                      Values[] value() default "";
                  }
                  public enum Values {ONE, TWO}
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(Values.TWO)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(name = "hello", value = Values.TWO)
                  public class A {
                  }
                  """
              )
            );
        }


        @Test
        void matchValueInArray() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "hello",
                "hi",
                null,
                false)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String[] value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo({"welcome", "hi", "goodbye"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo({"welcome", "hello", "goodbye"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchValueInArrayWhenAppending() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "hello,cheerio",
                "hi",
                null,
                true)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String[] value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;

                  @Foo({"welcome", "hi", "goodbye"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;

                  @Foo({"welcome", "hello", "cheerio", "goodbye"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void noMatchValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", "hi", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;

                  @Foo("goodbye")
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchClass() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Integer.class", "Long.class", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Class<? extends Number> value();
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;

                  @Foo(Long.class)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;

                  @Foo(Integer.class)
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void nomatchClass() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultClass", null, "Integer.class", "Double.class", null, null)),
              java(
                """
                  import org.example.FooDefaultClass;

                  @FooDefaultClass(Long.class)
                  public class A {}
                  """
              )
            );
        }
    }

    @Nested
    class AsValueAttribute {

        @Language("java")
        private static final String FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE = """
          package org.example;
          public @interface Foo {
              String[] value() default {};
          }
          """;

        @Test
        void implicitWithNullAttributeName() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b", null, false, true)),
              java(
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a"})
                  public class A {}
                  """,
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a", "b"})
                  public class A {}
                  """
              )
            );
        }

        @Test
        void implicitWithAttributeNameValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b", null, false, true)),
              java(
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a"})
                  public class A {}
                  """,
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a", "b"})
                  public class A {}
                  """
              )
            );
        }

        @Test
        void explicitWithNullAttributeName() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "b", null, false, true)),
              java(
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray(value = {"a"})
                  public class A {}
                  """,
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a", "b"})
                  public class A {}
                  """
              )
            );
        }

        @Test
        void explicitWithAttributeNameValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", "value", "b", null, false, true)),
              java(
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray(value = {"a"})
                  public class A {}
                  """,
                """
                  import org.example.FooDefaultStringArray;

                  @FooDefaultStringArray({"a", "b"})
                  public class A {}
                  """
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5526")
    @Test
    void fieldAccessArgumentDefaultAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultStringArray", null, "hello", null, false, false)),
          java(
            """
              package org.example;
              public interface Bar {
                  String BAR = "bar";
              }
              """
          ),
          java(
            """
              import org.example.Bar;
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({Bar.BAR})
              public class A {}
              """,
            """
              import org.example.Bar;
              import org.example.FooDefaultStringArray;

              @FooDefaultStringArray({"hello"})
              public class A {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5526")
    @Test
    void fieldAccessArgumentNamedAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo", "foo", "hello", null, false, false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] foo() default {};
              }
              """
          ),
          java(
            """
              package org.example;
              public interface Bar {
                  String BAR = "bar";
              }
              """
          ),
          java(
            """
              import org.example.Bar;
              import org.example.Foo;

              @Foo(foo = {Bar.BAR})
              public class A {}
              """,
            """
              import org.example.Bar;
              import org.example.Foo;

              @Foo(foo = {"hello"})
              public class A {}
              """
          )
        );
    }

    @Test
    void doNotMisMatchWhenUsingFieldReferenceOnNamedAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.FooDefaultString", "str", "newValue", "oldValue", null, null)),
          java(
            """
              import org.example.FooDefaultString;

              @FooDefaultString(str = A.OTHER_VALUE)
              public class A {
                  public static final String OTHER_VALUE = "otherValue";
              }
              """
          )
        );
    }

    @Test
    void doNotMisMatchOnMissingNamedAttributeWhenOldAttributeValueShouldMatch() {
        rewriteRun(
          spec -> spec.recipe(
            new AddOrUpdateAnnotationAttribute(
              "org.example.Foo",
              "name",
              "newValue",
              "oldValue",
              null,
              null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String name() default "";
                  String value() default "";
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.example.Foo;

              @Foo(value = "oldValue")
              public class A {}
              """
          )
        );
    }

    @Test
    void doNotAddOrRemoveExplicitParameterNameValueWhenOldAttributeValueDoesNotMatchAndAttributeNameIsNotValue() {
        rewriteRun(
          spec -> spec.recipe(
            new AddOrUpdateAnnotationAttribute(
              "org.example.Foo",
              "name",
              "newValue",
              "oldValue",
              null,
              null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String name() default "";
                  String value() default "";
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.example.Foo;

              @Foo("oldValue")
              public class A {}

              @Foo(value = "oldExplicitValue")
              public class B {}
              """
          )
        );
    }


    @Test
    void doNotAddOrRemoveExplicitParameterNameValueWhenOldAttributeValueDoesMatchAndAttributeNameIsValue() {
        rewriteRun(
          spec -> spec.recipe(
            new AddOrUpdateAnnotationAttribute(
              "org.example.Foo",
              "value",
              "newValue",
              "oldValue",
              null,
              null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String name() default "";
                  String value() default "";
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.example.Foo;

              @Foo("newValue")
              public class A {}

              @Foo(value = "newValue")
              public class B {}
              """
          )
        );
    }

    @Test
    void doAddExplicitParameterNameValueWhenOldAttributeValueIsNullAndAttributeNameIsNotValue() {
        rewriteRun(
          spec -> spec.recipe(
            new AddOrUpdateAnnotationAttribute(
              "org.example.Foo",
              "name",
              "aName",
              null,
              null,
              null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String name() default "";
                  String value() default "";
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.example.Foo;

              @Foo("someValue")
              public class A {}

              @Foo(value = "someValue")
              public class B {}
              """,
            """
              import org.example.Foo;

              @Foo(name = "aName", value = "someValue")
              public class A {}

              @Foo(name = "aName", value = "someValue")
              public class B {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5918")
    @Test
    void attributeWithShallowType() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeType("org.example.Bar", "org.example.Foo", true),
            new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "required",
            "true",
            null,
            false,
            false)
          ),
          java(
            """
              package org.example;
              public @interface Bar {
              }
              """
          ),
          java(
            """
              package org.example;
              public @interface Foo {
                  boolean required();
              }
              """
          ),
          java(
            """
              import org.example.Bar;

              @Bar
              public class A {
              }
              """,
            """
              import org.example.Foo;

              @Foo(required = true)
              public class A {
              }
              """
          )
        );
    }

}
