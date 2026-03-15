package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4HeaderTest {
  @Test
  fun ethernetHeader() {
    val ethernetH =
      P4.header("Ethernet_h") {
        field("dstAddr", P4.typeName("EthernetAddress"))
        field("srcAddr", P4.typeName("EthernetAddress"))
        field("etherType", P4.bit(16))
      }

    assertEquals(
      """
            header Ethernet_h {
                EthernetAddress dstAddr;
                EthernetAddress srcAddr;
                bit<16> etherType;
            }
            """
        .trimIndent(),
      ethernetH.toP4(),
    )
  }
}
