/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml.security;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;


class IsOwaspSuppressionsFileTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IsOwaspSuppressionsFile());
    }

    @Test
    void doesntAffectFilesWithoutXmlns() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(0),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions>
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("suppressions.xml")
                )
        );
    }

    @Test
    void doesntAffectFilesWithWrongXmlns() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(0),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="http://foo.bar/literally-anything-else.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("suppressions.xml")
                )
        );
    }

    @Test
    void addsMarkerToFilesWithCorrectXmlns() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <!--~~(Found it)~~>--><suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>
                        """,
                        spec -> spec.path("suppressions.xml")
                )
        );
    }

    @Test
    void worksEvenWithoutOnePointThree() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <!--~~(Found it)~~>--><suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>
                        """,
                        spec -> spec.path("suppressions.xml")
                )
        );
    }

    @Test
    void noChangesIfNoSuppressionsFile() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(0),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("soppressata.xml")
                )
        );
    }

    @Test
    void changesIfSuppressionsFile() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <!--~~(Found it)~~>--><suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("suppressions.xml")
                )
        );
    }

    @Test
    void doesntChangeIfNotAtRoot() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(0),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("not/root/suppressions.xml")
                )
        );
    }

    @Test
    void onlyChangesRoot() {
        rewriteRun(
                spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("not/root/suppressions.xml")
                ),
                xml("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <!--~~(Found it)~~>--><suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.2.4.xsd">
                            <suppress>
                                <notes>
                                </notes>
                            </suppress>
                        </suppressions>""",
                        spec -> spec.path("suppressions.xml")
                )
        );
    }
}
