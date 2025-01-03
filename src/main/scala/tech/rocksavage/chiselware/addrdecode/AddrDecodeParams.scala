// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)

package tech.rocksavage.chiselware.addrdecode

/** Default parameter settings for the AddressDecoder
  *
  * @constructor
  *   default parameter settings
  * @param dataWidth
  *   specifies the width of the data bus
  * @param addressWidth
  *   specifies the width of the address bus
  * @param memorySizes
  *   specifies the size of each memory range,
  *   note that this is the total number of addresses needed for each space,
  *   **NOT** the number of bits, bytes, ... for each space
  * @author
  *   Warren Savage
  * @version 1.0
  *
  * @see
  *   [[http://www.rocksavage.tech]] for more information
  */
case class AddrDecodeParams(
    dataWidth: Int = 8,
    addressWidth: Int = 8,
    memorySizes: Seq[Int] = Seq(32, 32, 32, 32, 32, 32, 32, 32)
) {

  require(dataWidth >= 1, "Data Width must be greater than or equal 1")
  require(addressWidth >= 1, "Address Width must be greater than or equal 1")

}
