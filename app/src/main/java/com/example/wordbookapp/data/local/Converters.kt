package com.example.wordbookapp.data.local

import androidx.room.TypeConverter
import com.example.wordbookapp.data.model.DeckType
import com.example.wordbookapp.data.model.WordField
import com.example.wordbookapp.data.model.WordOrder

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
}
