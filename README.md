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

```scala
class Timer(val timerParams: TimerParams) extends Module {
  val dataWidth = timerParams.dataWidth
  val addressWidth = timerParams.addressWidth

  val io = IO(new Bundle {
    val apb = new ApbBundle(ApbParams(dataWidth, addressWidth))
    val timerOutput = new TimerOutputBundle(timerParams)
    val interrupt = new TimerInterruptBundle
  })

  // Define memory sizes for the timer registers
  val memorySizes = List(1, 32, 32, 32, 32, 1, 2) // Example sizes for en, prescaler, maxCount, etc.

  // Create AddrDecodeParams
  val addrDecodeParams = AddrDecodeParams(
    dataWidth = dataWidth,
    addressWidth = addressWidth,
    memorySizes = memorySizes
  )

  // Instantiate the AddrDecode module
  val addrDecode = Module(new AddrDecode(addrDecodeParams))
  addrDecode.io.addr := io.apb.PADDR
  addrDecode.io.addrOffset := 0.U
  addrDecode.io.en := true.B
  addrDecode.io.selInput := true.B

  // Instantiate the APB interface
  val apbInterface = Module(new ApbInterface(ApbParams(dataWidth, addressWidth)))
  apbInterface.io.apb <> io.apb

  // Connect the address decoder to the APB interface
  apbInterface.io.mem.addr := addrDecode.io.addrOut
  apbInterface.io.mem.wdata := io.apb.PWDATA
  apbInterface.io.mem.read := !io.apb.PWRITE
  apbInterface.io.mem.write := io.apb.PWRITE

  // Handle writes to the timer registers
  when(apbInterface.io.mem.write) {
    for (i <0 until memorySizes.sum) {
      when(addrDecode.io.sel(i)) {
        // Write to the appropriate register
      }
    }
  }

  // Handle reads from the timer registers
  when(apbInterface.io.mem.read) {
    apbInterface.io.mem.rdata := 0.U
    for (i <0 until memorySizes.sum) {
      when(addrDecode.io.sel(i)) {
        // Read from the appropriate register
      }
    }
  }

  // Handle APB error conditions
  when(addrDecode.io.errorCode === AddrDecodeError.AddressOutOfRange) {
    apbInterface.io.apb.PSLVERR := true.B
  }.otherwise {
    apbInterface.io.apb.PSLVERR := false.B
  }

  // Instantiate the timer logic
  val timerInner = Module(new TimerInner(timerParams))
  // Connect timer inputs and outputs
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
