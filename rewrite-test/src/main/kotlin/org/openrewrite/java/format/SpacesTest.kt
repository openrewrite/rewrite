/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.style.NamedStyles

interface SpacesTest : RecipeTest {

    override val recipe: Recipe?
        get() = Spaces()

    val dependsOn: Array<String>
        get() = arrayOf(
            """
                    class MyResource implements AutoCloseable {
                        public void close() {
                        }
                    }
                """
        )

    val testCode: String
        get() = """
                    @SuppressWarnings({"ALL"})
                    public class A {
                        void bar() {}
                        void foo(int arg) {
                            Runnable r = () -> {};
                            Runnable r1 = this::bar;
                            if (true) {
                                foo(1);
                            } else {
                                foo(2);
                            }
                            int j = 0;
                            for (int i = 0; i < 10 || j > 0; i++) {
                                j += i;
                            }
                            int[] arr = new int[]{1, 3, 5, 6, 7, 87, 1213, 2};
                            for (int e : arr) {
                                j += e;
                            }
                            int[] arr2 = new int[]{};
                            int elem = arr[j];
                            int x;
                            while (j < 1000 && x > 0) {
                                j = j + 1;
                            }
                            do {
                                j = j + 1;
                            } while (j < 2000);
                            switch (j) {
                                case 1:
                                default:
                            }
                            try (MyResource res1 = new MyResource(); MyResource res2 = null) {
                            } catch (Exception e) {
                            } finally {
                            }
                            Object o = new Object();
                            synchronized (o) {
                            }
                            if (x == 0) {
                            }
                            if (j != 0) {
                            }
                            if (x <= 0) {
                            }
                            if (j >= 0) {
                            }
                            x = x << 2;
                            x = x >> 2;
                            x = x >>> 2;
                            x = x | 2;
                            x = x & 2;
                            x = x ^ 2;
                            x = x + 1;
                            x = x - 1;
                            x = x * 2;
                            x = x / 2;
                            x = x % 2;
                            boolean b;
                            b = !b;
                            x = -x;
                            x = +x;
                            x++;
                            ++x;
                            x--;
                            --x;
                            x += (x + 1);
                        }
                    }
                    
                    @SuppressWarnings({})
                    public interface I {}
                    
                    public class C {}
                """

    @Test
    fun beforeParens(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(NamedStyles("test", listOf(IntelliJ.spaces().apply {
                withBeforeParentheses(beforeParentheses.apply {
                    withMethodDeclaration(true)
                    withMethodCall(true)
                    withIfParentheses(false)
                    withForParentheses(false)
                    withWhileParentheses(false)
                    withSwitchParentheses(false)
                    withTryParentheses(false)
                    withCatchParentheses(false)
                    withSynchronizedParentheses(false)
                    withAnnotationParameters(true)
                })
                
                withAroundOperators(aroundOperators.apply {
                    withAssignment(false)
                    withLogical(false)
                    withEquality(false)
                    withRelational(false)
                    withBitwise(false)
                    withAdditive(false)
                    withMultiplicative(false)
                    withShift(false)
                    withUnary(true)
                    withLambdaArrow(false)
                    withMethodReferenceDoubleColon(true)
                })

                withBeforeLeftBrace(beforeLeftBrace.apply {
                    withClassLeftBrace(false)
                    withMethodLeftBrace(false)
                    withIfLeftBrace(false)
                    withElseLeftBrace(false)
                    withForLeftBrace(false)
                    withWhileLeftBrace(false)
                    withDoLeftBrace(false)
                    withSwitchLeftBrace(false)
                    withTryLeftBrace(false)
                    withCatchLeftBrace(false)
                    withFinallyLeftBrace(false)
                    withSynchronizedLeftBrace(false)
                    withArrayInitializerLeftBrace(true)
                    withAnnotationArrayInitializerLeftBrace(true)
                })

                withBeforeKeywords(beforeKeywords.apply {
                    withElseKeyword(false)
                    withWhileKeyword(false)
                    withCatchKeyword(false)
                    withFinallyKeyword(false)
                })

                withWithin(within.apply {
                    withCodeBraces(true)
                    withBrackets(true)
                    withArrayInitializerBraces(true)
                    withEmptyArrayInitializerBraces(true)
                    withGroupingParentheses(true)
                })
            })))).build(),
            dependsOn = dependsOn,
            before = testCode,
            after = /* THE HORROR */ """
                @SuppressWarnings ( { "ALL" })
                public class A{
                    void bar (){}
                    void foo (int arg){
                        Runnable r=()->{};
                        Runnable r1=this :: bar;
                        if(true){
                            foo (1);
                        }else{
                            foo (2);
                        }
                        int j=0;
                        for(int i=0; i<10||j>0; i ++){
                            j+=i;
                        }
                        int[] arr=new int[] { 1, 3, 5, 6, 7, 87, 1213, 2 };
                        for(int e : arr){
                            j+=e;
                        }
                        int[] arr2=new int[] { };
                        int elem=arr[ j ];
                        int x;
                        while(j<1000&&x>0){
                            j=j+1;
                        }
                        do{
                            j=j+1;
                        }while(j<2000);
                        switch(j){
                            case 1:
                            default:
                        }
                        try(MyResource res1 = new MyResource(); MyResource res2 = null){
                        }catch(Exception e){
                        }finally{
                        }
                        Object o=new Object();
                        synchronized(o){
                        }
                        if(x==0){
                        }
                        if(j!=0){
                        }
                        if(x<=0){
                        }
                        if(j>=0){
                        }
                        x=x<<2;
                        x=x>>2;
                        x=x>>>2;
                        x=x|2;
                        x=x&2;
                        x=x^2;
                        x=x+1;
                        x=x-1;
                        x=x*2;
                        x=x/2;
                        x=x%2;
                        boolean b;
                        b=! b;
                        x=- x;
                        x=+ x;
                        x ++;
                        ++ x;
                        x --;
                        -- x;
                        x+=( x+1 );
                    }
                }
 
                @SuppressWarnings ( { })
                public interface I{ }
                
                public class C{ }
            """
        )

        @Test
        fun unchanged(jp: JavaParser.Builder<*, *>) = assertUnchanged(
            jp.styles(listOf(NamedStyles("testspaces", listOf(IntelliJ.spaces())))).build(),
            dependsOn = dependsOn,
            before = testCode
        )
}
