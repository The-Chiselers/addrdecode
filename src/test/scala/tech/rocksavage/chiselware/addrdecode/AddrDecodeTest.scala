package tech.rocksavage.chiselware.addrdecode

import chiseltest.ChiselScalatestTester
import chiseltest.formal.{BoundedCheck, Formal}
import org.scalatest.flatspec.AnyFlatSpec

class AddrDecodeTest
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Formal {

    "AddrDecode" should "pass" in {

        val addrWidth: Int   = 32
        val dataWidth: Int   = 32
        val sizes: List[Int] = List.fill(8)(8)

        val p = AddrDecodeParams(dataWidth, addrWidth, sizes)

        verify(new AddrDecode(p, true), Seq(BoundedCheck(10)))

    }
}
