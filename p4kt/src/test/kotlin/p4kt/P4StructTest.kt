package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4StructTest {
  @Test
  fun parsedPacketStruct() {
    val parsedPacket =
      P4.struct("Parsed_packet") {
        field("ethernet", P4.typeName("Ethernet_h"))
        field("ip", P4.typeName("Ipv4_h"))
      }

    assertEquals(
      """
            struct Parsed_packet {
                Ethernet_h ethernet;
                Ipv4_h ip;
            }
            """
        .trimIndent(),
      parsedPacket.toP4(),
    )
  }
}
