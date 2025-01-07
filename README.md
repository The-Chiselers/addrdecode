# AddrDecode Module

## Overview

The `AddrDecode` module is a hardware address decoder designed to simplify the process of decoding memory addresses into specific ranges. It is particularly useful in systems where memory-mapped I/O or multiple memory regions need to be managed efficiently. The module is implemented in Chisel, a hardware design language, and provides a flexible and configurable way to handle address decoding.

## Features

- **Configurable Address Ranges**: Define multiple memory ranges with customizable sizes.
- **Error Handling**: Detect and handle out-of-range addresses with error codes.
- **Formal Verification Support**: Enable formal verification to ensure correctness of address decoding logic.
- **Integration with APB**: Seamlessly integrate with APB (Advanced Peripheral Bus) interfaces for memory-mapped I/O.

## Usage

### Defining Address Ranges

To define address ranges, create an instance of `AddrDecodeParams` with the desired data width, address width, and memory sizes. The `memorySizes` parameter specifies the size of each memory range.

```scala
val addrDecodeParams = AddrDecodeParams(
  dataWidth = 32,
  addressWidth = 32,
  memorySizes = List(1024, 2048, 4096) // Define three memory ranges
)
```

### Instantiating the AddrDecode Module

Instantiate the `AddrDecode` module with the defined parameters. The module will automatically calculate the address ranges and handle address decoding.

```scala
val addrDecode = Module(new AddrDecode(addrDecodeParams))
```

### Connecting Inputs and Outputs

Connect the address, address offset, enable signal, and select input to the `AddrDecode` module. The module will output the selected range, decoded address, error code, and error address.

```scala
addrDecode.io.addr := io.apb.PADDR
addrDecode.io.addrOffset := 0.U
addrDecode.io.en := true.B
addrDecode.io.selInput := true.B
```

### Handling Errors

The `AddrDecode` module provides an error code and error address output to handle out-of-range addresses. Use these outputs to manage error conditions in your design.

```scala
when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
  // Handle out-of-range address error
}
```

### Formal Verification

Enable formal verification by setting the `formal` parameter to `true` when instantiating the `AddrDecode` module. This will add assertions to verify the correctness of the address decoding logic.

```scala
val addrDecode = Module(new AddrDecode(addrDecodeParams, formal = true))
```

## Example

The following example demonstrates how to use the `AddrDecode` module in a timer design with an APB interface.

As a side note, AddressDecode does not program registers, it just gives the address of what should be programmed. The actual programming and initialization is left to the user, this is shown below

```scala
// (c) 2024 Rocksavage Technology, Inc.
// This code is licensed under the Apache Software License 2.0 (see LICENSE.MD)
package tech.rocksavage.chiselware.timer

import chisel3._
import chisel3.util._
import tech.rocksavage.chiselware.apb.{ApbBundle, ApbParams}
import tech.rocksavage.chiselware.addrdecode.{AddrDecode, AddrDecodeError, AddrDecodeParams}
import tech.rocksavage.chiselware.timer.bundle.{TimerBundle, TimerInterruptBundle, TimerInterruptEnum, TimerOutputBundle}
import tech.rocksavage.chiselware.timer.param.TimerParams
import tech.rocksavage.chiselware.addressable.RegisterMap
import tech.rocksavage.chiselware.timer.TimerInner

class Timer(val timerParams: TimerParams) extends Module {
  // Default Constructor
  def this() = this(TimerParams())
  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  // Input/Output bundle for the Timer module
  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
  })

  // Create a RegisterMap to manage the addressable registers
  val registerMap = new RegisterMap(dataWidth, addressWidth)

  // Now define your registers without the macro
  val en: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(en, "en")

  val prescaler: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(prescaler, "prescaler")

  val maxCount: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(maxCount, "maxCount")

  val pwmCeiling: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(pwmCeiling, "pwmCeiling")

  val setCountValue: UInt = RegInit(0.U(timerParams.countWidth.W))
  registerMap.createAddressableRegister(setCountValue, "setCountValue")

  val setCount: Bool = RegInit(false.B)
  registerMap.createAddressableRegister(setCount, "setCount")

  // Generate AddrDecode
  val addrDecodeParams = registerMap.getAddrDecodeParams
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  io.apb.PREADY := (io.apb.PENABLE && io.apb.PSEL)
  io.apb.PSLVERR := addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange

  io.apb.PRDATA := 0.U
  // Control Register Read/Write
  when(io.apb.PSEL && io.apb.PENABLE) {
    when(io.apb.PWRITE) {
      for (reg <- registerMap.getRegisters) {
        when(addrDecode.io.sel(reg.id)) {
          reg.writeCallback(addrDecode.io.addrOffset, io.apb.PWDATA)
        }
      }
    }.otherwise {
      for (reg <- registerMap.getRegisters) {
        when(addrDecode.io.sel(reg.id)) {
          io.apb.PRDATA := reg.readCallback(addrDecode.io.addrOffset)
        }
      }
    }
  }

  // Instantiate the TimerInner module
  val timerInner = Module(new TimerInner(timerParams))
  timerInner.io.timerInputBundle.en := en // Add this line
  timerInner.io.timerInputBundle.setCount := setCount
  timerInner.io.timerInputBundle.prescaler := prescaler
  timerInner.io.timerInputBundle.maxCount := maxCount
  timerInner.io.timerInputBundle.pwmCeiling := pwmCeiling
  timerInner.io.timerInputBundle.setCountValue := setCountValue
  // Connect the TimerInner outputs to the top-level outputs
  io.timerOutput <> timerInner.io.timerOutputBundle
  // Handle interrupts
  io.interrupt.interrupt := TimerInterruptEnum.None
  when(timerInner.io.timerOutputBundle.maxReached) {
    io.interrupt.interrupt := TimerInterruptEnum.MaxReached
  }
}
```

## Conclusion

The `AddrDecode` module is a powerful tool for managing address decoding in Chisel-based hardware designs. It simplifies the process of defining and handling multiple memory ranges, provides robust error handling, and supports formal verification for ensuring correctness. The module integrates seamlessly with APB interfaces, making it an essential component for memory-mapped I/O operations in complex systems.
