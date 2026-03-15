package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ConstTest {
  @Test
  fun constDeclaration() {
    val c = P4.const_("DROP_PORT", P4.typeName("PortId"), P4.lit(4, 0xF))
    assertEquals("const PortId DROP_PORT = 4w15;", c.toP4())
  }
}
