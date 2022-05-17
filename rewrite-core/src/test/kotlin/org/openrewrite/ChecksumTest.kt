package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URL

class ChecksumTest {

    @Disabled
    @Test
    fun checksum() {
        val checksum = Checksum("SHA-256", URL("https://services.gradle.org/distributions/gradle-7.4.2-wrapper.jar.sha256")
            .readBytes())
        assertThat(checksum.hexValue).isEqualTo("575098db54a998ff1c6770b352c3b16766c09848bee7555dab09afc34e8cf590")
    }
}
