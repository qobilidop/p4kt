package p4kt.p4include

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {
  @Test
  fun coreErrors() {
    val output = Core.toP4()
    assertTrue(output.contains("error {"))
    assertTrue(output.contains("NoError"))
    assertTrue(output.contains("PacketTooShort"))
    assertTrue(output.contains("NoMatch"))
    assertTrue(output.contains("StackOutOfBounds"))
    assertTrue(output.contains("HeaderTooShort"))
    assertTrue(output.contains("ParserTimeout"))
    assertTrue(output.contains("ParserInvalidArgument"))
  }

  @Test
  fun corePacketIn() {
    val output = Core.toP4()
    assertTrue(output.contains("extern packet_in {"))
  }

  @Test
  fun corePacketOut() {
    val output = Core.toP4()
    assertTrue(output.contains("extern packet_out {"))
  }

  @Test
  fun coreDeclarationOrder() {
    val output = Core.toP4()
    val errorIndex = output.indexOf("error {")
    val packetInIndex = output.indexOf("extern packet_in")
    val packetOutIndex = output.indexOf("extern packet_out")
    assertTrue(errorIndex < packetInIndex, "error should come before packet_in")
    assertTrue(packetInIndex < packetOutIndex, "packet_in should come before packet_out")
  }

  @Test
  fun corePacketInUsableAsParamType() {
    assertEquals("packet_in", Core.packet_in.typeRef.name)
  }

  @Test
  fun corePacketOutUsableAsParamType() {
    assertEquals("packet_out", Core.packet_out.typeRef.name)
  }
}
