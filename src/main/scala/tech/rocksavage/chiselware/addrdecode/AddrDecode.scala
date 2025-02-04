// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.addrdecode

import chisel3._
import chisel3.util.log2Ceil

/** An address decoder that can be used to decode addresses into a set of ranges
  *
  * @constructor
  *   Create a new address decoder
  * @param params
  *   BaseParams object including dataWidth and addressWidth
  * @param formal
  *   A boolean value to enable formal verification
  * @author
  *   Warren Savage
  */
class AddrDecode(
    params: AddrDecodeParams,
    formal: Boolean = false
) extends Module {

    // ###################
    // Default Constructor
    // ###################

    val lengthSel: Int = params.memorySizes.length

    // ###################
    // Parameter checking & calculation
    // ###################
    val totalMemorySize: Int = params.memorySizes.sum
    val numBitsNeeded        = log2Ceil(totalMemorySize)
    val mask                = (1 << numBitsNeeded) - 1

    var ranges: List[(Int, Int)] = List()
    for (i <- 0 until lengthSel) {
        if (i == 0) {
            ranges = ranges :+ (0, params.memorySizes(i) - 1)
        } else {
            ranges = ranges :+ (
              ranges(i - 1)._2 + 1,
              ranges(i - 1)._2 + params.memorySizes(i)
            )
        }
    }

    require(ranges.nonEmpty, "At least one range must be provided")

    val io = IO(new Bundle {
        val addr = Input(UInt(params.addressWidth.W))
//        val addrOffset = Input(UInt(params.addressWidth.W))
        val en       = Input(Bool())
        val selInput = Input(Bool())

        val sel       = Output(Vec(lengthSel, Bool()))
        val addrOut   = Output(UInt(params.addressWidth.W))
        val errorCode = Output(AddrDecodeError())
        val errorAddr = Output(UInt(params.addressWidth.W))
    })

    val addrMasked = Wire(UInt(params.addressWidth.W))
    addrMasked := io.addr & mask.U

    /** in this section, we take the results from the above functions and assign
      * them to the output ports
      */
    private val isErr = Wire(Bool())
    require(
      totalMemorySize <= math.pow(2, params.addressWidth),
      "Address space is not large enough to hold all ranges"
    )
//    private val addr             = addrMasked - io.addrOffset
    private val addr = addrMasked
    private val en   = io.en

    def this() = {
        this(AddrDecodeParams(), false)
    }

    /** Returns the number of memory addresses used by the module
      *
      * @return
      *   The width of the memory
      */
    def memWidth(): Int = totalMemorySize

    /** Returns a vector of booleans representing the selected range
      *
      * @param addrRanges
      *   A sequence of tuples representing the start and end of each range
      * @param inputAddr
      *   The address to be decoded
      * @return
      *   A vector of booleans representing the selected range
      */
    def getSelect(
        addrRanges: List[(Int, Int)],
        inputAddr: UInt
    ): Vec[Bool] = {
        // declare sel
        val selOut = Wire(Vec(lengthSel, Bool()))
        // check if input_addr is in range
        var index: Int = 0
        while (index < lengthSel) {
            when(
              inputAddr >= addrRanges(
                index
              )._1.U && inputAddr <= addrRanges(
                index
              )._2.U
            ) {
                selOut(index) := true.B
            }.otherwise {
                selOut(index) := false.B
            }
            index += 1
        }
        return selOut
    }

    // ##########
    // Main logic
    // ##########

    /** Returns the address output
      *
      * @param addrRanges
      *   A sequence of tuples representing the start and end of each range
      * @param inputAddr
      *   The address to be decoded
      * @return
      *   The address output
      */

    def getAddrOut(
        addrRanges: List[(Int, Int)],
        inputAddr: UInt
    ): UInt = {
        val addrOut = Wire(UInt(params.addressWidth.W))
        addrOut := 0.U

        for ((startAddr, endAddr) <- addrRanges) {
            when(
              inputAddr >= startAddr.U && inputAddr <= endAddr.U
            ) {
                addrOut := (inputAddr - startAddr.U)
            }
        }
        return addrOut
    }

    /** Returns a boolean value indicating if the address is in error
      *
      * @param rangeAddr
      *   A sequence of tuples representing the start and end of each range
      * @param inputAddr
      *   The address to be decoded
      * @return
      *   A boolean value indicating if the address is in error
      */
    def addrIsError(
        rangeAddr: List[(Int, Int)],
        inputAddr: UInt
    ): Bool = {
        val isErr = Wire(Bool())

        val minAddr: Int = rangeAddr.head._1
        val maxAddr: Int = rangeAddr.last._2

        isErr := false.B
        when(
          inputAddr < minAddr.U || inputAddr > maxAddr.U
        ) {
            isErr := true.B
        }
        return isErr
    }

    /** Returns the error address
      *
      * @param addrRanges
      *   A sequence of tuples representing the start and end of each range
      * @param inputAddr
      *   The address to be decoded
      * @param offsetAddr
      *   The offset address
      * @return
      *   The error address
      */

    def getErrorAddress(
        addrRanges: List[(Int, Int)],
        rawInputAddr: UInt,
        addrMasked: UInt
    ): UInt = {
        val errorAddr = Wire(UInt(params.addressWidth.W))
        errorAddr := 0.U

        val minAddr: Int = addrRanges.head._1
        val maxAddr: Int = addrRanges.last._2

        when(
          addrMasked < minAddr.U || addrMasked > maxAddr.U
        ) {
            errorAddr := rawInputAddr
        }

        errorAddr
    }

    io.sel       := VecInit(Seq.fill(lengthSel)(false.B))
    io.addrOut   := 0.U
    io.errorCode := AddrDecodeError.None
    io.errorAddr := 0.U

    isErr := 0.U

    when(en && io.selInput) {
        isErr := addrIsError(ranges, addr)
        when(isErr) {
            io.errorCode := AddrDecodeError.AddressOutOfRange
        }.otherwise {
            io.errorCode := AddrDecodeError.None
        }

        io.sel       := getSelect(ranges, addr)
        io.addrOut   := getAddrOut(ranges, addr)
        io.errorAddr := getErrorAddress(ranges, io.addr, addrMasked)
    }

    // ###################
    // Formal verification
    // ###################
    /** The assertions being made here are:
      *   - If the address is in range:
      *     - Exactly one of the sel vector is high
      *     - The address is decoded correctly from the relative start of the
      *       internal block
      *       - offset = 10, addr = 20, start_addr = 10, addr_out = 10
      *     - The error code is set to None
      *     - The error address is set to 0
      *   - If the address is out of range:
      *     - The sel vector is all low
      *     - The address output is 0
      *     - The error code is set to AddressOutOfRange
      *     - The error address is set to the input address
      */
    if (formal) {
        when(en && io.selInput) {
            // ranges to
            for ((startAddr, endAddr) <- ranges) {
                when(
                  addr >= startAddr.U && addr <= endAddr.U
                ) {
                    assert(
                      io.sel(ranges.indexOf((startAddr, endAddr))),
                      "Invalid addr decoding"
                    )
                    assert(
                      io.addrOut === addr - startAddr.U,
                      "Invalid addr output"
                    )
                    assert(
                      io.errorCode === AddrDecodeError.None,
                      "Invalid error code"
                    )
                    assert(io.errorAddr === 0.U, "Invalid error address")
                }
            }
            val minAddr: Int = ranges.head._1
            val maxAddr: Int = ranges.last._2
            when(
              addr < minAddr.U || addr > maxAddr.U
            ) {
                // assert sel are all low
                assert(!io.sel.contains(true.B), "Invalid addr decoding")
                assert(io.addrOut === 0.U, "Invalid addr output")
                assert(
                  io.errorCode === AddrDecodeError.AddressOutOfRange,
                  "Invalid error code"
                )
                assert(io.errorAddr === io.addr, "Invalid error address")
            }

        }
    }

    assert(ranges.nonEmpty, "At least one range must be provided")
}
