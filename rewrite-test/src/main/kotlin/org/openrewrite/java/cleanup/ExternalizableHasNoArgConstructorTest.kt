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
@file:Suppress("RedundantThrows", "ExternalizableWithoutPublicNoArgConstructor")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface ExternalizableHasNoArgConstructorTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = ExternalizableHasNoArgsConstructor()

    @Test
    fun hasDefaultNoArgsConstructor() = assertUnchanged(
        before = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;
            
            public class MyThing implements Externalizable {
                private String a;
                private String b;
                
                public void setA(String a) {
                    this.a = a;
                }
                public void setB(String b) {
                    this.b = b;
                }
                
                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Test
    fun hasNoArgsConstructor() = assertUnchanged(
        before = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;
            
            public class MyThing implements Externalizable {
                private String a;
                private String b;
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }
                
                public MyThing() {}
                
                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Suppress("ExternalizableWithoutPublicNoArgConstructor")
    @Test
    fun needsNoArgsConstructor() = assertChanged(
        before = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;

            public class MyThing implements Externalizable {
                private String a;
                private String b;
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """,
        after = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;

            public class MyThing implements Externalizable {
                private String a;
                private String b;
            
                public MyThing() {
                }
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Test
    fun implementsExternalizableInterface() = assertChanged(
        dependsOn = arrayOf("""
            package abc;
            import java.io.Externalizable;
            
            interface Abc extends Externalizable {
                String getLetter();
            }
        """),
        before = """
            package abc;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;
            
            public class MyThing implements Abc {
                private String a;
                private String b;
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }
                
                @Override
                public String getLetter() {
                    return a;
                }
                
                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """,
        after = """
            package abc;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;
            
            public class MyThing implements Abc {
                private String a;
                private String b;
            
                public MyThing() {
                }
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }
                
                @Override
                public String getLetter() {
                    return a;
                }
                
                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Test
    fun hasFinalFieldVar() = assertUnchanged(
        before = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;

            public class MyThing implements Externalizable {
                private final String a;
                private final String b;
                
                public MyThing(String a, String b) {
                    this.a = a;
                    this.b = b;
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Test
    fun hasInitializedFinalFieldVar() = assertChanged(
        before = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;import java.util.ArrayList;

            public class MyThing implements Externalizable {
                private final Integer limit = 10;
                private String a;
                
                public MyThing(String a) {
                    this.a = a;
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """,
        after = """
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;import java.util.ArrayList;

            public class MyThing implements Externalizable {
                private final Integer limit = 10;
                private String a;
            
                public MyThing() {
                }
                
                public MyThing(String a) {
                    this.a = a;
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )

    @Test
    fun superClassDoesNotHaveDefaultConstructor() = assertUnchanged(
        dependsOn = arrayOf("""
            package abc;
            public class SuperThing {
                private final Long l;
                public SuperThing(Long l) {
                    this.l = l;
                }
                public void doSomething() {}
            }
        """),
        before = """
            package abc;
            import java.io.Externalizable;
            import java.io.IOException;
            import java.io.ObjectInput;
            import java.io.ObjectOutput;

            public class MyThing extends SuperThing implements Externalizable {
                
                public MyThing(Long l) {
                    super(l);
                }

                @Override 
                public void writeExternal(ObjectOutput out) throws IOException {}
                
                @Override
                public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
            }
        """
    )
}
