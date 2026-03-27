package com.mistbottle.jpnwordtrainer.data.local

import androidx.room.TypeConverter
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.TestStatus
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
    fun testStatusFromString(value: String): TestStatus = TestStatus.valueOf(value)

    @TypeConverter
    fun testStatusToString(value: TestStatus): String = value.name
}
