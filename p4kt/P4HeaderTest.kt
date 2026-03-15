package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4HeaderTest {
  @Test
  fun ethernetHeader() {
    val ethernetH =
      p4Header("Ethernet_h") {
        field("dstAddr", typeName("EthernetAddress"))
        field("srcAddr", typeName("EthernetAddress"))
        field("etherType", bit(16))
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
