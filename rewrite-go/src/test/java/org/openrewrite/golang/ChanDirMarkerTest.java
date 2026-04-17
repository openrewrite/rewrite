/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang;

import org.junit.jupiter.api.Test;
import org.openrewrite.golang.tree.ChanDirMarker;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.go;

class ChanDirMarkerTest implements RewriteTest {

    @Test
    void sendChannelWithSpaceBeforeArrow() {
        rewriteRun(
                go(
                        """
                                package main

                                func test() {
                                    ch := make(chan <- int)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            // Verify that ChanDirMarker is present and captures the space
                            var channelType = findChannelType(cu);
                            assertThat(channelType).isNotNull();

                            var marker = channelType.getMarkers()
                                    .findFirst(ChanDirMarker.class);
                            assertThat(marker).isPresent()
                                    .hasValueSatisfying(m -> {
                                        assertThat(m.getBefore().getWhitespace()).contains(" ");
                                    });
                        })
                )
        );
    }

    @Test
    void receiveChannelWithSpaceBeforeArrow() {
        rewriteRun(
                go(
                        """
                                package main

                                func test() {
                                    ch := make(<- chan int)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            var channelType = findChannelType(cu);
                            assertThat(channelType).isNotNull();

                            // For receive channels, the space is typically in the prefix
                            // but may also be in ChanDirMarker depending on implementation
                            var marker = channelType.getMarkers()
                                    .findFirst(ChanDirMarker.class);
                            // Just verify the marker can be found if present
                            assertThat(marker).isPresent();
                        })
                )
        );
    }

    @Test
    void channelWithCommentBeforeArrow() {
        rewriteRun(
                go(
                        """
                                package main

                                func test() {
                                    ch := make(chan /* send */ <- int)
                                }
                                """
                )
        );
    }

    @Test
    void biDirectionalChannelNoArrow() {
        rewriteRun(
                go(
                        """
                                package main

                                func test() {
                                    ch := make(chan int)
                                }
                                """,
                        spec -> spec.afterRecipe(cu -> {
                            var channelType = findChannelType(cu);
                            assertThat(channelType).isNotNull();

                            var marker = channelType.getMarkers()
                                    .findFirst(ChanDirMarker.class);
                            // Bi-directional channels may not have a ChanDirMarker
                            assertThat(marker).isEmpty();
                        })
                )
        );
    }

    private Go.Channel findChannelType(Go.CompilationUnit cu) {
        final Go.Channel[] result = {null};
        new org.openrewrite.golang.GolangVisitor<Integer>() {
            @Override
            public org.openrewrite.java.tree.J visitChannel(Go.Channel channel, Integer p) {
                if (result[0] == null) {
                    result[0] = channel;
                }
                return super.visitChannel(channel, p);
            }
        }.visit(cu, 0);
        return result[0];
    }
}
