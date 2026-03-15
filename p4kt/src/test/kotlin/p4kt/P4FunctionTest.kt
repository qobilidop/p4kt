package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4FunctionTest {
  @Test
  fun identityFunction() {
    val id =
      P4.function("id", P4.bit(8)) {
        val x by param(P4.bit(8), P4.IN)
        return_(x)
      }

    assertEquals(
      """
            function bit<8> id(in bit<8> x) {
                return x;
            }
            """
        .trimIndent(),
      id.toP4(),
    )
  }
}
