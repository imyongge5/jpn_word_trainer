package com.mistbottle.jpnwordtrainer.data.local

import androidx.room.TypeConverter
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.SessionStatus
import com.mistbottle.jpnwordtrainer.data.model.WordField
import com.mistbottle.jpnwordtrainer.data.model.WordOrder

class Converters {
    @TypeConverter
    fun deckTypeFromString(value: String): DeckType = DeckType.valueOf(value)

    @TypeConverter
    fun deckTypeToString(value: DeckType): String = value.name

    @TypeConverter
    fun wordOrderFromString(value: String): WordOrder = WordOrder.valueOf(value)

    @TypeConverter
    fun wordOrderToString(value: WordOrder): String = value.name

    @TypeConverter
    fun wordFieldFromString(value: String): WordField = WordField.valueOf(value)

    @TypeConverter
    fun wordFieldToString(value: WordField): String = value.name

    @TypeConverter
    fun sessionStatusFromString(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun sessionStatusToString(value: SessionStatus): String = value.name
}
