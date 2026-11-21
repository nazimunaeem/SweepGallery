package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SortOrder
import com.example.model.TindPhotoStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tindphoto_prefs")

data class PersistedPreferences(
    val keptPhotoIds: Set<Long> = emptySet(),
    val stats: TindPhotoStats = TindPhotoStats(),
    val includeReviewed: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val darkMode: Boolean = true,
    val dismissedUpdateTag: String = "",
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val KEPT_PHOTO_IDS = stringSetPreferencesKey("kept_photo_ids")
        val TOTAL_DELETED_COUNT = intPreferencesKey("total_deleted_count")
        val TOTAL_KEPT_COUNT = intPreferencesKey("total_kept_count")
        val TOTAL_FREED_BYTES = longPreferencesKey("total_freed_bytes")
        val INCLUDE_REVIEWED = booleanPreferencesKey("include_reviewed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DISMISSED_UPDATE_TAG = stringPreferencesKey("dismissed_update_tag")
    }

    val preferencesFlow: Flow<PersistedPreferences> = context.dataStore.data.map { prefs ->
        val keptIds = prefs[Keys.KEPT_PHOTO_IDS]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        val sortOrderName = prefs[Keys.SORT_ORDER] ?: SortOrder.NEWEST.name
        val sortOrder = SortOrder.entries.firstOrNull { it.name == sortOrderName } ?: SortOrder.NEWEST

        PersistedPreferences(
            keptPhotoIds = keptIds,
            stats = TindPhotoStats(
                totalDeletedCount = prefs[Keys.TOTAL_DELETED_COUNT] ?: 0,
                totalKeptCount = prefs[Keys.TOTAL_KEPT_COUNT] ?: 0,
                totalFreedBytes = prefs[Keys.TOTAL_FREED_BYTES] ?: 0L,
            ),
            includeReviewed = prefs[Keys.INCLUDE_REVIEWED] ?: false,
            sortOrder = sortOrder,
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            dismissedUpdateTag = prefs[Keys.DISMISSED_UPDATE_TAG] ?: "",
        )
    }

    suspend fun markPhotoKept(photoId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            val added = current.add(photoId.toString())
            prefs[Keys.KEPT_PHOTO_IDS] = current
            if (added) {
                prefs[Keys.TOTAL_KEPT_COUNT] = (prefs[Keys.TOTAL_KEPT_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun unmarkPhotoKept(photoId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            val removed = current.remove(photoId.toString())
            prefs[Keys.KEPT_PHOTO_IDS] = current
            if (removed) {
                val prev = prefs[Keys.TOTAL_KEPT_COUNT] ?: 0
                if (prev > 0) prefs[Keys.TOTAL_KEPT_COUNT] = prev - 1
            }
        }
    }

    suspend fun recordBatchDeletion(deletedCount: Int, freedBytes: Long, deletedIds: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_DELETED_COUNT] = (prefs[Keys.TOTAL_DELETED_COUNT] ?: 0) + deletedCount
            prefs[Keys.TOTAL_FREED_BYTES] = (prefs[Keys.TOTAL_FREED_BYTES] ?: 0L) + freedBytes
            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            deletedIds.forEach { current.remove(it.toString()) }
            prefs[Keys.KEPT_PHOTO_IDS] = current
        }
    }

    suspend fun setIncludeReviewed(include: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INCLUDE_REVIEWED] = include
        }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun resetKeptHistory() {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEPT_PHOTO_IDS] = emptySet()
        }
    }

    suspend fun setDismissedUpdateTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_UPDATE_TAG] = tag
        }
    }
}
