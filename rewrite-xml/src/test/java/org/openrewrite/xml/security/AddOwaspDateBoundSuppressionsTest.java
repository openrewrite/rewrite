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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class AddOwaspDateBoundSuppressionsTest implements RewriteTest {

    @DocumentExample
    @Test
    void addsUntilIfNotPresent() {
        rewriteRun(
                spec -> spec.recipe(new AddOwaspDateBoundSuppressions("2020-01-01")),
                xml("""
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress>
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""", """
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="2020-01-01Z">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""",
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void defaultIs30DaysInFuture() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        String thirtyDaysFromNowString = thirtyDaysFromNow + "Z";
        rewriteRun(
                spec -> spec.recipe(new AddOwaspDateBoundSuppressions(null)),
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
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>"""
                                .formatted(thirtyDaysFromNowString),
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void defaultIs30DaysInFutureWithEmptyString() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        String thirtyDaysFromNowString = thirtyDaysFromNow + "Z";
        rewriteRun(
                spec -> spec.recipe(new AddOwaspDateBoundSuppressions("")),
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
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>"""
                                .formatted(thirtyDaysFromNowString),
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @CsvSource({"abcd,false",
            "2022,false",
            "2022-01-01,true"})
    @ParameterizedTest
    void valid(String untilDate, boolean valid) {
        assertThat(new AddOwaspDateBoundSuppressions(untilDate).validate().isValid()).isEqualTo(valid);
    }
}
