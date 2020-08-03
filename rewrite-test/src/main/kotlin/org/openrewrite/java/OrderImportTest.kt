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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.whenParsedBy

interface OrderImportTest {
    @Test
    fun orderImports(jp: JavaParser) {
        """
            import static java.util.stream.Collectors.joining;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import org.openrewrite.java.tree.JavaType;
            import org.openrewrite.java.tree.TypeUtils;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import org.openrewrite.java.tree.JavaType;
                    import org.openrewrite.java.tree.TypeUtils;
                    
                    import java.util.ArrayList;
                    import java.util.Objects;
                    import java.util.Set;
                    import java.util.regex.Pattern;
                    
                    import static java.util.stream.Collectors.joining;
                    
                    class A {}
                """.trimIndent())
    }

    @Test
    fun blankLineThenEmptyBlockThenNonEmptyBlock(jp: JavaParser) {
        """
            import java.util.ArrayList;
            import java.util.Objects;

            import org.openrewrite.java.tree.JavaType;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import org.openrewrite.java.tree.JavaType;
                    
                    import java.util.ArrayList;
                    import java.util.Objects;
                    
                    class A {}
                """)
    }

    @Test
    fun foldIntoStar(jp: JavaParser) {
        """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            import java.util.Set;
            import java.util.Map;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import java.util.*;
                    import java.util.regex.Pattern;
                    
                    class A {}
                """)
    }

    @Test
    fun blankLinesNotFollowedByBlockArentAdded(jp: JavaParser) {
        """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isUnchanged()
    }

    @Test
    fun foldIntoExistingStar(jp: JavaParser) {
        """
            import java.util.*;
            import java.util.ArrayList;
            import java.util.regex.Pattern;
            import java.util.Objects;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import java.util.*;
                    import java.util.regex.Pattern;
                    
                    class A {}
                """)
    }

    @Test
    fun idempotence(jp: JavaParser) {
        """
            import java.util.*;
            import java.util.regex.Pattern;

            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isUnchanged()
    }

    @Test
    fun unfoldStar(jp: JavaParser) {
        """
            import java.util.*;
            
            class A {
                List list;
                List list2;
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports())
                .isRefactoredTo("""
                    import java.util.List;
                    
                    class A {
                        List list;
                        List list2;
                    }
                """)
    }

    @Test
    fun removeUnused(jp: JavaParser) {
        """
            import java.util.*;
            
            class A {
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports())
                .isRefactoredTo("""
                    class A {
                    }
                """)
    }

    @Test
    fun unfoldStaticStar(jp: JavaParser) {
        """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {
                List list = emptyList();
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports())
                .isRefactoredTo("""
                    import java.util.List;
                    
                    import static java.util.Collections.emptyList;
                    
                    class A {
                        List list = emptyList();
                    }
                """)
    }

    @Test
    fun packagePatternEscapesDots(jp: JavaParser) {
        """
            import javax.annotation.Nonnull;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isUnchanged()
    }

    @Test
    fun twoImportsFollowedByStar(jp: JavaParser) {
        """
            import java.io.IOException;
            import java.io.UncheckedIOException;
            import java.nio.files.*;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isUnchanged()
    }

    @Test
    fun springCloudFormat(jp: JavaParser) {
        """
            import java.io.ByteArrayOutputStream;
            import java.nio.charset.StandardCharsets;
            import java.util.Collections;
            import java.util.zip.GZIPOutputStream;
            
            import javax.servlet.ReadListener;
            import javax.servlet.ServletInputStream;
            import javax.servlet.ServletOutputStream;
            
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;
            
            import org.springframework.core.io.buffer.DataBuffer;
            import org.springframework.core.io.buffer.DataBufferFactory;
            import org.springframework.http.HttpHeaders;
            import org.springframework.util.MultiValueMap;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.server.ServerWebExchange;
            
            import static java.util.Arrays.stream;
            import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply {
                    setLayout(OrderImports.Layout.builder(999, 999)
                            .importPackage("java.*")
                            .blankLine()
                            .importPackage("javax.*")
                            .blankLine()
                            .importAllOthers()
                            .blankLine()
                            .importPackage("org.springframework.*")
                            .blankLine()
                            .importStaticAllOthers()
                            .build())

                    setRemoveUnused(false)
                })
                .isUnchanged()
    }

    @Test
    fun importSorting(jp: JavaParser) {
        """
            import r.core.Flux;
            import s.core.Flux;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.apache.commons.logging.Log;
            import reactor.core.publisher.Mono;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import com.fasterxml.jackson.databind.ObjectMapper;
                    import org.apache.commons.logging.Log;
                    import r.core.Flux;
                    import reactor.core.publisher.Mono;
                    import s.core.Flux;
                    
                    class A {}
                """)
    }

    @Test
    fun foldGroupOfStaticImportsThatAppearLast(jp: JavaParser) {
        """
            import static java.util.stream.Collectors.toList;
            import static java.util.stream.Collectors.toMap;
            import static java.util.stream.Collectors.toSet;
            
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(OrderImports().apply { setRemoveUnused(false) })
                .isRefactoredTo("""
                    import static java.util.stream.Collectors.*;
                    
                    class A {}
                """)
    }
}
