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
