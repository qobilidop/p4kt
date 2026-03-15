package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4TypedefTest {
  @Test
  fun typedefBitType() {
    val ethernetAddress = P4.typedef("EthernetAddress", P4.bit(48))

    assertEquals("typedef bit<48> EthernetAddress;", ethernetAddress.toP4())
  }

  @Test
  fun typedefNamedType() {
    val addr = P4.typedef("Addr", P4.typeName("EthernetAddress"))

    assertEquals("typedef EthernetAddress Addr;", addr.toP4())
  }
}
