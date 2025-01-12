package tech.rocksavage.chiselware.addrdecode

import tech.rocksavage.chiselware.addrdecode.AddrDecodeParams
import tech.rocksavage.traits.ModuleConfig

class AddrDecodeConfig extends ModuleConfig {
  override def getDefaultConfigs: Map[String, Any] = Map(
    "8_8_8" -> AddrDecodeParams(dataWidth = 8, addressWidth = 8, memorySizes = List.fill(8)(8)),
    "16_16_16" -> AddrDecodeParams(dataWidth = 16, addressWidth = 16, memorySizes = List.fill(16)(16)),
    "32_32_32" -> AddrDecodeParams(dataWidth = 32, addressWidth = 32, memorySizes = List.fill(32)(32)),
    "64_64_64" -> AddrDecodeParams(dataWidth = 64, addressWidth = 64, memorySizes = List.fill(64)(64))
  )
}
