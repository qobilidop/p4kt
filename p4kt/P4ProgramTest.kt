package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ProgramTest {
  @Test
  fun programWithMultipleDeclarations() {
    val program = p4Program {
      val EthernetAddress by typedef(bit(48))

      class Ethernet_h(base: P4Expr) : HeaderRef(base) {
        val dstAddr by field(EthernetAddress)
        val etherType by field(bit(16))
      }
      header(::Ethernet_h)

      class Parsed_packet(base: P4Expr) : StructRef(base) {
        val ethernet by field(typeName("Ethernet_h"))
      }
      struct(::Parsed_packet)
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
