package com.example.delivery.data.repository

fun interface MenuAccessCodeValidator {
    fun accessIdFor(code: String): String?
}

data class MenuAccessCode(
    val code: String,
    val accessId: String,
    val enabled: Boolean = true,
)

class ConfiguredMenuAccessCodeValidator(
    private val codes: List<MenuAccessCode>,
) : MenuAccessCodeValidator {
    override fun accessIdFor(code: String): String? =
        codes.firstOrNull { it.enabled && it.code == code && it.code.isNotBlank() }?.accessId
}

object NoMenuAccessCodeValidator : MenuAccessCodeValidator {
    override fun accessIdFor(code: String): String? = null
}
