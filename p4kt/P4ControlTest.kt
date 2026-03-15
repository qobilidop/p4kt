package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ControlTest {
  @Test
  fun methodCallStatement() {
    val stmt = P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList())
    assertEquals("ipv4_match.apply();", stmt.toP4())
  }

  @Test
  fun tableRendering() {
    val table =
      P4Table(
        name = "ipv4_match",
        keys =
          listOf(P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), MatchKind.LPM)),
        actions = listOf("Drop_action", "Set_nhop"),
        size = 1024,
        defaultAction = "Drop_action",
        isDefaultActionConst = false,
      )

    assertEquals(
      """
          table ipv4_match {
              key = { headers.dstAddr : lpm; }
              actions = {
                  Drop_action;
                  Set_nhop;
              }
              size = 1024;
              default_action = Drop_action;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun tableWithConstDefaultAction() {
    val table =
      P4Table(
        name = "check_ttl",
        keys =
          listOf(P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "ttl"), MatchKind.EXACT)),
        actions = listOf("Send_to_cpu", "NoAction"),
        defaultAction = "NoAction",
        isDefaultActionConst = true,
      )

    assertEquals(
      """
          table check_ttl {
              key = { headers.ttl : exact; }
              actions = {
                  Send_to_cpu;
                  NoAction;
              }
              const default_action = NoAction;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun controlRendering() {
    val ctrl =
      P4Control(
        name = "MyCtrl",
        params = listOf(P4Param("outCtrl", P4Type.Named("OutControl"), Direction.OUT)),
        declarations =
          listOf(
            P4Action(
              "Drop",
              emptyList(),
              listOf(
                P4Statement.Assign(
                  P4Expr.FieldAccess(P4Expr.Ref("outCtrl"), "outputPort"),
                  P4Expr.Ref("DROP_PORT"),
                )
              ),
            )
          ),
        body = listOf(P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList())),
      )

    assertEquals(
      """
          control MyCtrl(out OutControl outCtrl) {
              action Drop() {
                  outCtrl.outputPort = DROP_PORT;
              }
              apply {
                  ipv4_match.apply();
              }
          }
      """
        .trimIndent(),
      ctrl.toP4(),
    )
  }
}
