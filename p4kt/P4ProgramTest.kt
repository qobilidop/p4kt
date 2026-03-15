package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program = p4Program {
      typedef("EthernetAddress", bit(48))
      header("Ethernet_h") {
        field("dstAddr", typeName("EthernetAddress"))
        field("etherType", bit(16))
      }
      struct("Parsed_packet") { field("ethernet", typeName("Ethernet_h")) }
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
