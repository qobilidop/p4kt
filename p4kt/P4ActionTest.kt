package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ActionTest {
  @Test
  fun emptyAction() {
    val a = p4Action("NoAction") {}
    assertEquals(
      """
            action NoAction() {
            }
      """
        .trimIndent(),
      a.toP4(),
    )
  }

  @Test
  fun actionWithDirectionlessParams() {
    val a =
      p4Action("Set_nhop") {
        val ipv4_dest by param(P4.typeName("IPv4Address"))
        val port by param(P4.typeName("PortId"))
        assign(P4.ref("nextHop"), ipv4_dest)
        assign(
          P4Expr.FieldAccess(P4Expr.FieldAccess(P4.ref("headers"), "ip"), "ttl"),
          P4Expr.FieldAccess(P4Expr.FieldAccess(P4.ref("headers"), "ip"), "ttl") - P4.lit(1),
        )
        assign(P4Expr.FieldAccess(P4.ref("outCtrl"), "outputPort"), port)
      }

    assertEquals(
      """
            action Set_nhop(IPv4Address ipv4_dest, PortId port) {
                nextHop = ipv4_dest;
                headers.ip.ttl = headers.ip.ttl - 1;
                outCtrl.outputPort = port;
            }
      """
        .trimIndent(),
      a.toP4(),
    )
  }
}
