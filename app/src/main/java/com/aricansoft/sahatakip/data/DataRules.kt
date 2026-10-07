package com.aricansoft.sahatakip.data

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

private val trLocale=Locale.forLanguageTag("tr-TR")

internal fun normalizeWorkItemName(value:String):String=
    value.trim().lowercase(trLocale)

internal fun validateTargetDate(value:String?):String?{
    val clean=value?.trim()?.ifBlank{null} ?: return null
    try{
        LocalDate.parse(clean)
    }catch(_:DateTimeParseException){
        throw IllegalArgumentException("Hedef tarih geçerli bir YYYY-AA-GG tarihi olmalı.")
    }
    return clean
}

internal fun isValidTargetDate(value:String):Boolean=
    value.isBlank() || runCatching{LocalDate.parse(value.trim())}.isSuccess
