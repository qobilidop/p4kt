package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ConstTest {
  @Test
  fun constDeclaration() {
    val c = p4Const("DROP_PORT", typeName("PortId"), lit(4, 0xF))
    assertEquals("const PortId DROP_PORT = 4w15;", c.toP4())
  }
}
