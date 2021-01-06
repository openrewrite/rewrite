package org.openrewrite.java

import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.DebugOnly

class Java11EvalDebugTests

@DebugOnly
@ExtendWith(JavaParserResolver::class)
class Java11JavaTemplateTest : Java11Test, JavaTemplateTest
