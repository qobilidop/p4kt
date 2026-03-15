package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program = p4Program {
      val EthernetAddress by typedef(bit(48))
      val Ethernet_h by header {
        field("dstAddr", EthernetAddress)
        field("etherType", bit(16))
      }
      @Suppress("UnusedPrivateProperty")
      val Parsed_packet by struct { field("ethernet", Ethernet_h) }
    }

    assertEquals(
      """
            typedef bit<48> EthernetAddress;

            header Ethernet_h {
                EthernetAddress dstAddr;
                bit<16> etherType;
            }

            struct Parsed_packet {
                Ethernet_h ethernet;
            }
            """
        .trimIndent(),
      program.toP4(),
    )
  }
}
