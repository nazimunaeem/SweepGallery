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
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tindphoto_prefs")

data class PersistedPreferences(
    val keptPhotoIds: Set<Long> = emptySet(),
    val favoritePhotoIds: Set<Long> = emptySet(),
    val trashQueueIds: List<Long> = emptyList(),
    val stats: TindPhotoStats = TindPhotoStats(),
    val includeReviewed: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val darkMode: Boolean = true,
    val useSystemTrash: Boolean = false,
    val autoCheckUpdates: Boolean = true,
    val dismissedUpdateTag: String = "",
    val dismissedPopupTag: String = "",
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val KEPT_PHOTO_IDS = stringSetPreferencesKey("kept_photo_ids")
        val FAVORITE_PHOTO_IDS = stringSetPreferencesKey("favorite_photo_ids")
        val TRASH_QUEUE_IDS_CSV = stringPreferencesKey("trash_queue_ids_csv")
        val TOTAL_DELETED_COUNT = intPreferencesKey("total_deleted_count")
        val TOTAL_KEPT_COUNT = intPreferencesKey("total_kept_count")
        val TOTAL_FREED_BYTES = longPreferencesKey("total_freed_bytes")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_CLEANUP_EPOCH_DAY = longPreferencesKey("last_cleanup_epoch_day")
        val WEEKLY_FREED_BYTES = longPreferencesKey("weekly_freed_bytes")
        val WEEKLY_ANCHOR_EPOCH_DAY = longPreferencesKey("weekly_anchor_epoch_day")
        val INCLUDE_REVIEWED = booleanPreferencesKey("include_reviewed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val USE_SYSTEM_TRASH = booleanPreferencesKey("use_system_trash")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val DISMISSED_UPDATE_TAG = stringPreferencesKey("dismissed_update_tag")
        val DISMISSED_POPUP_TAG = stringPreferencesKey("dismissed_update_popup_tag")
    }

    val preferencesFlow: Flow<PersistedPreferences> = context.dataStore.data.map { prefs ->
        val keptIds = prefs[Keys.KEPT_PHOTO_IDS]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        val favoriteIds = prefs[Keys.FAVORITE_PHOTO_IDS]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        val trashIds = (prefs[Keys.TRASH_QUEUE_IDS_CSV] ?: "")
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()

        val sortOrderName = prefs[Keys.SORT_ORDER] ?: SortOrder.NEWEST.name
        val sortOrder = SortOrder.entries.firstOrNull { it.name == sortOrderName } ?: SortOrder.NEWEST

        val todayEpochDay = currentEpochDay()
        val lastCleanupDay = prefs[Keys.LAST_CLEANUP_EPOCH_DAY] ?: -1L
        val rawStreak = prefs[Keys.STREAK_DAYS] ?: 0
        val activeStreak = if (lastCleanupDay >= 0L && (todayEpochDay - lastCleanupDay) <= 1L) {
            rawStreak
        } else {
            0
        }

        val weeklyAnchor = prefs[Keys.WEEKLY_ANCHOR_EPOCH_DAY] ?: todayEpochDay
        val weeklyFreed = if ((todayEpochDay - weeklyAnchor) < 7L) {
            prefs[Keys.WEEKLY_FREED_BYTES] ?: 0L
        } else {
            0L
        }

        PersistedPreferences(
            keptPhotoIds = keptIds,
            favoritePhotoIds = favoriteIds,
            trashQueueIds = trashIds,
            stats = TindPhotoStats(
                totalDeletedCount = prefs[Keys.TOTAL_DELETED_COUNT] ?: 0,
                totalKeptCount = prefs[Keys.TOTAL_KEPT_COUNT] ?: 0,
                totalFreedBytes = prefs[Keys.TOTAL_FREED_BYTES] ?: 0L,
                currentStreakDays = activeStreak,
                weeklyFreedBytes = weeklyFreed,
            ),
            includeReviewed = prefs[Keys.INCLUDE_REVIEWED] ?: false,
            sortOrder = sortOrder,
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            useSystemTrash = prefs[Keys.USE_SYSTEM_TRASH] ?: false,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            dismissedUpdateTag = prefs[Keys.DISMISSED_UPDATE_TAG] ?: "",
            dismissedPopupTag = prefs[Keys.DISMISSED_POPUP_TAG] ?: "",
        )
    }

    private fun currentEpochDay(): Long {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    }

    private fun updateStreakInPrefs(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val today = currentEpochDay()
        val lastDay = prefs[Keys.LAST_CLEANUP_EPOCH_DAY] ?: -1L
        val currentStreak = prefs[Keys.STREAK_DAYS] ?: 0
        val nextStreak = when {
            lastDay == today -> currentStreak.coerceAtLeast(1)
            lastDay == today - 1L -> currentStreak + 1
            else -> 1
        }
        prefs[Keys.STREAK_DAYS] = nextStreak
        prefs[Keys.LAST_CLEANUP_EPOCH_DAY] = today
    }

    suspend fun saveTrashQueueIds(ids: List<Long>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRASH_QUEUE_IDS_CSV] = ids.distinct().joinToString(",")
        }
    }

    suspend fun markPhotoKept(photoId: Long, isFavorite: Boolean = false) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            val added = current.add(photoId.toString())
            prefs[Keys.KEPT_PHOTO_IDS] = current
            if (added) {
                prefs[Keys.TOTAL_KEPT_COUNT] = (prefs[Keys.TOTAL_KEPT_COUNT] ?: 0) + 1
            }
            if (isFavorite) {
                val favs = prefs[Keys.FAVORITE_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
                favs.add(photoId.toString())
                prefs[Keys.FAVORITE_PHOTO_IDS] = favs
            }
            updateStreakInPrefs(prefs)
        }
    }

    suspend fun unmarkPhotoKept(photoId: Long, removeFavorite: Boolean = false) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            val removed = current.remove(photoId.toString())
            prefs[Keys.KEPT_PHOTO_IDS] = current
            if (removed) {
                val prev = prefs[Keys.TOTAL_KEPT_COUNT] ?: 0
                if (prev > 0) prefs[Keys.TOTAL_KEPT_COUNT] = prev - 1
            }
            if (removeFavorite) {
                val favs = prefs[Keys.FAVORITE_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
                favs.remove(photoId.toString())
                prefs[Keys.FAVORITE_PHOTO_IDS] = favs
            }
        }
    }

    suspend fun recordTrashSwipeStreak() {
        context.dataStore.edit { prefs ->
            updateStreakInPrefs(prefs)
        }
    }

    suspend fun recordBatchDeletion(deletedCount: Int, freedBytes: Long, deletedIds: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_DELETED_COUNT] = (prefs[Keys.TOTAL_DELETED_COUNT] ?: 0) + deletedCount
            prefs[Keys.TOTAL_FREED_BYTES] = (prefs[Keys.TOTAL_FREED_BYTES] ?: 0L) + freedBytes

            val today = currentEpochDay()
            val anchor = prefs[Keys.WEEKLY_ANCHOR_EPOCH_DAY] ?: today
            val prevWeekly = if ((today - anchor) < 7L) (prefs[Keys.WEEKLY_FREED_BYTES] ?: 0L) else 0L
            prefs[Keys.WEEKLY_ANCHOR_EPOCH_DAY] = if ((today - anchor) < 7L) anchor else today
            prefs[Keys.WEEKLY_FREED_BYTES] = prevWeekly + freedBytes

            val current = prefs[Keys.KEPT_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            deletedIds.forEach { current.remove(it.toString()) }
            prefs[Keys.KEPT_PHOTO_IDS] = current

            val favs = prefs[Keys.FAVORITE_PHOTO_IDS]?.toMutableSet() ?: mutableSetOf()
            deletedIds.forEach { favs.remove(it.toString()) }
            prefs[Keys.FAVORITE_PHOTO_IDS] = favs

            val existingTrash = (prefs[Keys.TRASH_QUEUE_IDS_CSV] ?: "")
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
                .filterNot { it in deletedIds }
            prefs[Keys.TRASH_QUEUE_IDS_CSV] = existingTrash.joinToString(",")

            updateStreakInPrefs(prefs)
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

    suspend fun setUseSystemTrash(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_SYSTEM_TRASH] = enabled
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CHECK_UPDATES] = enabled
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

    suspend fun setDismissedPopupTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_POPUP_TAG] = tag
        }
    }
}
