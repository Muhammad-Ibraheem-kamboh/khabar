package com.letdigitalfly.ibamail.domain.model

data class ProgramGroup(
    val name: String,
    val programs: List<ProgramItem>
)

data class ProgramItem(
    val id: String,
    val name: String,
    val level: String,
    val aliases: List<String>
)
