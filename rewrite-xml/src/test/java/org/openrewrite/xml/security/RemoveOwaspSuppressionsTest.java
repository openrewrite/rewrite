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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.time.LocalDate;

import static org.openrewrite.xml.Assertions.xml;

class RemoveOwaspSuppressionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveOwaspSuppressions(null));
    }

    @DocumentExample
    @Test
    void provideExplicitDateRange() {
        rewriteRun(
          spec -> spec.recipe(new RemoveOwaspSuppressions("2023-02-01")),
          xml("""
              <?xml version="1.0" encoding="UTF-8" ?>
              <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                  <suppress until="2023-01-01">
                      <notes>
                      </notes>
                  </suppress>
              </suppressions>""",
            """
              <?xml version="1.0" encoding="UTF-8" ?>
              <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
              </suppressions>""",
            spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void removesSuppressionsAtLeastOneDayAgo() {
        LocalDate today = LocalDate.now();
        LocalDate dayBeforeYesterday = today.minusDays(2);

        // yesterday in YYYY-MM-DD format + Z to indicate UTC
        String dayBeforeYesterdayString = dayBeforeYesterday + "Z";

        rewriteRun(
                xml("""
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>"""
                                .formatted(dayBeforeYesterdayString),
                        """
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                </suppressions>""",
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void onlyRemovesSuppressionsInThePast() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);
        String tomorrowString = tomorrow.toString().substring(0, 10);
        String dayBeforeYesterdayString = dayBeforeYesterday.toString().substring(0, 10);

        rewriteRun(
                xml(("""
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""")
                                .formatted(tomorrowString, dayBeforeYesterdayString),
                        """
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>"""
                                .formatted(tomorrowString),
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void removesSuppressionsWithAndWithoutZ() {
        LocalDate today = LocalDate.now();
        LocalDate dayBeforeYesterday = today.minusDays(2);
        String dayBeforeYesterdayString = dayBeforeYesterday + "Z";
        String dayBeforeYesterdayStringNoZ = dayBeforeYesterday.toString();

        rewriteRun(
                xml(("""
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""")
                                .formatted(dayBeforeYesterdayString, dayBeforeYesterdayStringNoZ),
                        """
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                </suppressions>""",
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void ignoresMalformedDates() {
        LocalDate today = LocalDate.now();
        LocalDate dayBeforeYesterday = today.minusDays(2);
        String dayBeforeYesterdayString = dayBeforeYesterday.toString().substring(0, 10);

        rewriteRun(
                xml(("""
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="blah">
                                        <notes>
                                        </notes>
                                    </suppress>
                                    <suppress until="blahZ">
                                        <notes>
                                        </notes>
                                    </suppress>
                                    <suppress until="%s">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""")
                                .formatted(dayBeforeYesterdayString),
                        """
                                <?xml version="1.0" encoding="UTF-8" ?>
                                <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                                    <suppress until="blah">
                                        <notes>
                                        </notes>
                                    </suppress>
                                    <suppress until="blahZ">
                                        <notes>
                                        </notes>
                                    </suppress>
                                </suppressions>""",
                        spec -> spec.path("suppressions.xml"))
        );
    }

    @Test
    void provideExplicitDateRangeDoNotRemove() {
        rewriteRun(
          spec -> spec.recipe(new RemoveOwaspSuppressions("2023-01-01")),
          xml("""
              <?xml version="1.0" encoding="UTF-8" ?>
              <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                  <suppress until="2023-02-01">
                      <notes>
                      </notes>
                  </suppress>
              </suppressions>""",
            spec -> spec.path("suppressions.xml"))
        );
    }
}
