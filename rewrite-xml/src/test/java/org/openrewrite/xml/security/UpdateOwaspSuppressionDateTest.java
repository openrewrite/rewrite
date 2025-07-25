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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class UpdateOwaspSuppressionDateTest implements RewriteTest {

    @DocumentExample
    @Test
    void updatesUntilIfCveExists() {
        rewriteRun(
                spec -> spec.recipe(new UpdateOwaspSuppressionDate(Collections.singletonList("CVE-2022-1234"), "2020-02-01")),
                xml("""
                    <?xml version="1.0" encoding="UTF-8" ?>
                    <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-5678</cve>
                            <cve>CVE-2022-1234</cve>
                        </suppress>
                    </suppressions>""", """
                    <?xml version="1.0" encoding="UTF-8" ?>
                    <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                        <suppress until="2020-02-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-5678</cve>
                            <cve>CVE-2022-1234</cve>
                        </suppress>
                    </suppressions>""",
                spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void noUpdateIfCveDoesNotExist() {
        rewriteRun(
                spec -> spec.recipe(new UpdateOwaspSuppressionDate(Collections.singletonList("CVE-2022-5678"), "2020-02-01")),
                xml("""
                    <?xml version="1.0" encoding="UTF-8" ?>
                    <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-1234</cve>
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
                spec -> spec.recipe(new UpdateOwaspSuppressionDate(List.of("CVE-2022-1234","CVE-2022-5678"), null)),
                xml("""
                    <?xml version="1.0" encoding="UTF-8" ?>
                    <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-1234</cve>
                        </suppress>
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-5678</cve>
                        </suppress>
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-00011</cve>
                        </suppress>
                    </suppressions>""", """
                    <?xml version="1.0" encoding="UTF-8" ?>
                    <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                        <suppress until="%s">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-1234</cve>
                        </suppress>
                        <suppress until="%s">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-5678</cve>
                        </suppress>
                        <suppress until="2020-01-01Z">
                            <notes>
                            </notes>
                            <cve>CVE-2019-10321</cve>
                            <cve>CVE-2022-00011</cve>
                        </suppress>
                    </suppressions>""".formatted(thirtyDaysFromNowString, thirtyDaysFromNowString),
                spec -> spec.path("suppressions.xml"))
        );
    }

    @CsvSource({"abcd,false",
            "2022,false",
            "2022-01-01,true"})
    @ParameterizedTest
    void valid(String untilDate, boolean valid) {
        assertThat(new UpdateOwaspSuppressionDate(Collections.singletonList(""), untilDate).validate().isValid()).isEqualTo(valid);
    }
}
