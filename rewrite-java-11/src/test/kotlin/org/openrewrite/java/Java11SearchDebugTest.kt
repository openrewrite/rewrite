package org.openrewrite.java

import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.DebugOnly
import org.openrewrite.java.search.SemanticallyEqualTest

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11SemanticallyEqualTest: Java11Test(), SemanticallyEqualTest
