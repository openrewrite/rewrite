import com.github.jk1.license.LicenseReportExtension
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.util.Calendar

plugins {
    id("com.github.hierynomus.license")
    id("com.github.jk1.dependency-license-report")
}

configure<LicenseReportExtension> {
    renderers = arrayOf(com.github.jk1.license.render.CsvReportRenderer())
}

configure<LicenseExtension> {
    ext.set("year", Calendar.getInstance().get(Calendar.YEAR))
    skipExistingHeaders = true
    excludePatterns.addAll(listOf("**/*.tokens", "**/*.config", "**/*.interp", "**/*.txt"))
    header = project.rootProject.file("gradle/licenseHeader.txt")
    mapping(mapOf("kt" to "SLASHSTAR_STYLE", "java" to "SLASHSTAR_STYLE"))
    strictCheck = true
}
