package p4kt.p4include

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class V1modelTest {
  @Test
  fun v1modelPortIdTypedef() {
    val output = V1model.toP4()
    assertTrue(output.contains("typedef bit<9> PortId_t;"))
  }

  @Test
  fun v1modelStandardMetadata() {
    val output = V1model.toP4()
    assertTrue(output.contains("struct standard_metadata_t {"))
    assertTrue(output.contains("PortId_t ingress_port;"))
    assertTrue(output.contains("PortId_t egress_spec;"))
    assertTrue(output.contains("error parser_error;"))
    assertTrue(output.contains("bit<3> priority;"))
  }

  @Test
  fun v1modelActionProfile() {
    val output = V1model.toP4()
    assertTrue(output.contains("extern action_profile {"))
    assertTrue(output.contains("action_profile();"))
  }

  @Test
  fun v1modelActionSelector() {
    val output = V1model.toP4()
    assertTrue(output.contains("extern action_selector {"))
    assertTrue(output.contains("action_selector();"))
  }

  @Test
  fun v1modelPortIdUsableAsType() {
    assertEquals("PortId_t", V1model.PortId_t.typeRef.name)
  }

  @Test
  fun v1modelStandardMetadataUsableAsStructRef() {
    val s = V1model.standard_metadata_t(p4kt.P4Expr.Ref("sm"))
    assertTrue(s.fields.isNotEmpty())
  }
}
