package org.openrewrite.properties

import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class Issue1020 : PropertiesRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1020")
    @Test
    fun removalOfDoublePound() = assertUnchanged(
        recipe = ChangePropertyKey("server.port", "chassis.name", null),
        before = """
            key=**##**chassis.management.metrics.export.cloudwatch.awsAccessKey
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1020")
    @Test
    fun removalOfSlashPound() = assertUnchanged(
        recipe = ChangePropertyValue("server.tomcat.accesslog.enabled", "true", null, null),
        before = """
            boot.features=https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle**/#**boot-features-jersey
            server.tomcat.accesslog.enabled=true
        """
    )
}
