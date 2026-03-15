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
        val ipv4_dest by param(typeName("IPv4Address"))
        val port by param(typeName("PortId"))
        assign(P4Expr.Ref("nextHop"), ipv4_dest)
        assign(
          P4Expr.Ref("headers").dot("ip").dot("ttl"),
          P4Expr.Ref("headers").dot("ip").dot("ttl") - lit(1),
        )
        assign(P4Expr.Ref("outCtrl").dot("outputPort"), port)
      }

    assertEquals(
      """
            action Set_nhop(IPv4Address ipv4_dest, PortId port) {
                nextHop = ipv4_dest;
                headers.ip.ttl = (headers.ip.ttl - 1);
                outCtrl.outputPort = port;
            }
      """
        .trimIndent(),
      a.toP4(),
    )
  }
}
