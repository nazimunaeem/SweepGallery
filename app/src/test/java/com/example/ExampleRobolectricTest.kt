package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.BurstDetector
import com.example.model.AlbumSummary
import com.example.model.BurstGroup
import com.example.model.MediaKind
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.model.SmartCategory
import com.example.model.SmartCategorySummary
import com.example.model.SwipeDirection
import com.example.model.TindPhotoStats
import com.example.ui.components.BurstCompareSheet
import com.example.ui.components.DeckJumpSheet
import com.example.ui.components.PhotoInfoBottomSheet
import com.example.ui.screens.StudioStatsScreen
import com.example.ui.screens.SwipeDeckScreen
import com.example.ui.screens.TrashBinScreen
import com.example.ui.theme.TindPhotoTheme
import com.example.ui.viewmodel.MONTH_ALL
import com.example.ui.viewmodel.TindPhotoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string and render launcher drawables cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SweepGallery", appName)

    listOf(
      R.mipmap.ic_launcher,
      R.mipmap.ic_launcher_round,
      R.drawable.ic_launcher_foreground,
      R.drawable.ic_launcher_background,
    ).forEach { resId ->
      val drawable = ContextCompat.getDrawable(context, resId)
      assertNotNull("Drawable $resId should inflate", drawable)
      val bmp = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      drawable!!.setBounds(0, 0, 108, 108)
      drawable.draw(canvas)
    }
  }

  @Test
  fun `burst detector groups rapid shots and detects screenshots`() {
    val baseTime = 1_763_711_700_000L
    val photos = listOf(
      PhotoItem(
        id = 10L,
        uri = Uri.parse("content://media/external/images/media/10"),
        displayName = "IMG_1.jpg",
        sizeBytes = 2_000_000L,
        dateTakenMillis = baseTime,
        dateAddedSeconds = baseTime / 1000L,
        width = 4000,
        height = 3000,
        albumName = "Camera",
      ),
      PhotoItem(
        id = 11L,
        uri = Uri.parse("content://media/external/images/media/11"),
        displayName = "IMG_2.jpg",
        sizeBytes = 2_400_000L,
        dateTakenMillis = baseTime + 2_000L,
        dateAddedSeconds = (baseTime + 2_000L) / 1000L,
        width = 4000,
        height = 3000,
        albumName = "Camera",
      ),
      PhotoItem(
        id = 12L,
        uri = Uri.parse("content://media/external/images/media/12"),
        displayName = "Screenshot_2026.png",
        sizeBytes = 400_000L,
        dateTakenMillis = baseTime + 500_000L,
        dateAddedSeconds = (baseTime + 500_000L) / 1000L,
        width = 1080,
        height = 2400,
        albumName = "Screenshots",
        directoryPath = "Pictures/Screenshots",
      ),
    )

    val groups = BurstDetector.detectBurstGroups(photos)
    assertEquals(1, groups.size)
    assertEquals(2, groups.first().photos.size)
    assertTrue(photos[2].isScreenshot)
  }

  @Test
  fun `launches main activity permission gate cleanly without crash`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("grant_permission_button").assertIsDisplayed()
  }

  @Test
  fun `launches with permission and opens all tabs cleanly`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)
    composeTestRule.activityRule.scenario.recreate()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Sweep").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_trash").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_tab_stats").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("developer_credit_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Md. Nazim Uddin").assertIsDisplayed()
    composeTestRule.onNodeWithTag("support_developer_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("Buy me a coffee").assertIsDisplayed()
  }

  @Test
  fun `studio screen displays streak smart categories system trash toggle and square grids`() {
    val sampleState = TindPhotoUiState(
      hasPermission = true,
      stats = TindPhotoStats(
        totalDeletedCount = 12,
        totalKeptCount = 30,
        totalFreedBytes = 420_000_000L,
        currentStreakDays = 4,
        weeklyFreedBytes = 120_000_000L,
      ),
      smartCategories = listOf(
        SmartCategorySummary(SmartCategory.ALL, 10, 25_000_000L),
        SmartCategorySummary(SmartCategory.BURSTS, 4, 8_000_000L),
        SmartCategorySummary(SmartCategory.VIDEOS, 2, 12_000_000L),
        SmartCategorySummary(SmartCategory.LARGE_FILES, 1, 15_000_000L),
        SmartCategorySummary(SmartCategory.SCREENSHOTS, 3, 2_000_000L),
        SmartCategorySummary(SmartCategory.STARRED, 2, 4_000_000L),
      ),
      months = listOf(
        MonthSummary(MONTH_ALL, "All Months", 10, 8, 25_000_000L, false),
        MonthSummary("2026-11", "Nov 2026", 6, 4, 15_000_000L, false),
        MonthSummary("2026-10", "Oct 2026", 4, 0, 10_000_000L, true),
      ),
      albums = listOf(
        AlbumSummary("All Photos", 10, 25_000_000L, remainingCount = 8, isCompleted = false),
        AlbumSummary("Camera", 7, 18_000_000L, remainingCount = 7, isCompleted = false),
        AlbumSummary("Screenshots", 3, 7_000_000L, remainingCount = 0, isCompleted = true),
      ),
    )

    var selectedMonth = ""
    var selectedAlbum = ""
    var selectedCategory: SmartCategory? = null
    var systemTrashToggled = false

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          StudioStatsScreen(
            state = sampleState,
            onSelectAlbumAndSwipe = { selectedAlbum = it },
            onSelectMonthAndSwipe = { selectedMonth = it },
            onSetSortOrder = {},
            onSetIncludeReviewed = {},
            onSetDarkMode = {},
            onResetKeptHistory = {},
            onSeedSamples = {},
            onCheckForUpdates = {},
            onOpenUpdateDialog = {},
            onSelectSmartCategoryAndSwipe = { selectedCategory = it },
            onSetUseSystemTrash = { systemTrashToggled = it },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("streak_milestone_card"))
    composeTestRule.onNodeWithText("4-Day Cleanup Streak").assertIsDisplayed()

    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("smart_category_card_VIDEOS"))
    composeTestRule.onNodeWithTag("smart_category_card_VIDEOS").performClick()
    assertEquals(SmartCategory.VIDEOS, selectedCategory)

    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("system_trash_switch"))
    composeTestRule.onNodeWithTag("system_trash_switch").performClick()
    assertTrue(systemTrashToggled)

    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("github_releases_link"))
    composeTestRule.onNodeWithTag("github_releases_link").assertIsDisplayed()

    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("studio_tab_month"))
    composeTestRule.onNodeWithTag("studio_tab_month").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("stats_month_row_2026-10"))
    composeTestRule.onNodeWithText("4 photos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stats_month_row_2026-11").performClick()
    assertEquals("2026-11", selectedMonth)

    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("studio_tab_album"))
    composeTestRule.onNodeWithTag("studio_tab_album").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_lazy_column")
      .performScrollToNode(hasTestTag("album_row_Screenshots"))
    composeTestRule.onNodeWithText("3 photos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("album_row_Camera").performClick()
    assertEquals("Camera", selectedAlbum)
  }

  @Test
  fun `photo info bottom sheet shows directory show in files share and metadata`() {
    val samplePhoto = PhotoItem(
      id = 42L,
      uri = Uri.parse("content://media/external/images/media/42"),
      displayName = "IMG_20261121_075500.jpg",
      sizeBytes = 3_500_000L,
      dateTakenMillis = 1_763_711_700_000L,
      dateAddedSeconds = 1_763_711_700L,
      width = 4032,
      height = 3024,
      albumName = "Camera",
      mimeType = "image/jpeg",
      directoryPath = "DCIM/Camera",
    )

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          PhotoInfoBottomSheet(
            photo = samplePhoto,
            onDismiss = {},
            onOpenLightbox = {},
            onKeep = {},
            onTrash = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("inspector_directory_value").assertIsDisplayed()
    composeTestRule.onNodeWithText("DCIM/Camera").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inspector_show_in_files_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inspector_share_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("JPEG").assertIsDisplayed()
    composeTestRule.onNodeWithText("4032 × 3024 (12.2 MP)").assertIsDisplayed()
  }

  @Test
  fun `swipe deck renders populated photos videos favorite button and jump pill`() {
    val samplePhotos = listOf(
      PhotoItem(
        id = 1L,
        uri = Uri.parse("content://media/external/images/media/1"),
        displayName = "IMG_Sunset_Coast.jpg",
        sizeBytes = 1_200_000L,
        dateTakenMillis = 1_763_711_700_000L,
        dateAddedSeconds = 1_763_711_700L,
        width = 1080,
        height = 1440,
        albumName = "SweepGallerySamples",
        mimeType = "image/jpeg",
        directoryPath = "Pictures/SweepGallerySamples",
      ),
      PhotoItem(
        id = 2L,
        uri = Uri.parse("content://media/external/video/media/2"),
        displayName = "VID_Coast.mp4",
        sizeBytes = 15_000_000L,
        dateTakenMillis = 1_763_611_700_000L,
        dateAddedSeconds = 1_763_611_700L,
        width = 1920,
        height = 1080,
        albumName = "Camera",
        mimeType = "video/mp4",
        directoryPath = "DCIM/Camera",
        mediaKind = MediaKind.VIDEO,
        durationMillis = 18_000L,
      ),
    )

    val state = TindPhotoUiState(
      hasPermission = true,
      isLimitedAccess = true,
      allPhotosCount = samplePhotos.size,
      deckPhotos = samplePhotos,
      months = listOf(
        MonthSummary(MONTH_ALL, "All Months", 2, 2, 16_200_000L, false),
        MonthSummary("2026-11", "Nov 2026", 2, 2, 16_200_000L, false),
      ),
      albums = listOf(
        AlbumSummary("All Photos", 2, 16_200_000L),
        AlbumSummary("Camera", 1, 15_000_000L),
      ),
    )

    var lastDirection: SwipeDirection? = null
    var jumpOpened = false

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          SwipeDeckScreen(
            state = state,
            onSwipe = { _, dir -> lastDirection = dir },
            onUndo = {},
            onInspectPhoto = {},
            onOpenLightbox = {},
            onSelectAlbum = {},
            onSelectMonth = {},
            onSelectSortOrder = {},
            onRefresh = {},
            onSeedSamples = {},
            onIncludeReviewed = {},
            onOpenTrashTab = {},
            onOpenJumpSheet = { jumpOpened = true },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("limited_access_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("deck_jump_button").performClick()
    assertTrue(jumpOpened)
    composeTestRule.onNodeWithTag("favorite_action_button").performClick()
    assertEquals(SwipeDirection.UP_FAVORITE, lastDirection)
    composeTestRule.onNodeWithTag("keep_action_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("trash_action_button").assertIsDisplayed()
  }

  @Test
  fun `trash screen supports multi select restore and system trash label`() {
    val queued = listOf(
      PhotoItem(
        id = 101L,
        uri = Uri.parse("content://media/external/images/media/101"),
        displayName = "IMG_A.jpg",
        sizeBytes = 1_000_000L,
        dateTakenMillis = 1_763_711_700_000L,
        dateAddedSeconds = 1_763_711_700L,
        width = 1080,
        height = 1080,
        albumName = "Camera",
      ),
      PhotoItem(
        id = 102L,
        uri = Uri.parse("content://media/external/images/media/102"),
        displayName = "IMG_B.jpg",
        sizeBytes = 2_000_000L,
        dateTakenMillis = 1_763_711_600_000L,
        dateAddedSeconds = 1_763_711_600L,
        width = 1080,
        height = 1080,
        albumName = "Camera",
      ),
    )

    var restoredBatch: Set<Long> = emptySet()

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          TrashBinScreen(
            state = TindPhotoUiState(
              hasPermission = true,
              trashQueue = queued,
              trashTotalBytes = 3_000_000L,
              useSystemTrash = true,
            ),
            onRestorePhoto = {},
            onRestoreAll = {},
            onInspectPhoto = {},
            onDeleteAllConfirmed = {},
            onGoToSwipe = {},
            onRestoreSelected = { restoredBatch = it },
            onDeleteSelected = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Move to System Trash (2.9 MB)").assertIsDisplayed()
    composeTestRule.onNodeWithTag("trash_select_toggle").performClick()
    composeTestRule.onNodeWithTag("trash_tile_101").performClick()
    composeTestRule.onNodeWithTag("restore_selected_button").performClick()
    assertEquals(setOf(101L), restoredBatch)
  }

  @Test
  fun `burst compare sheet and deck jump sheet invoke selection callbacks`() {
    val p1 = PhotoItem(
      id = 201L,
      uri = Uri.parse("content://media/external/images/media/201"),
      displayName = "BURST_1.jpg",
      sizeBytes = 1_500_000L,
      dateTakenMillis = 1_763_711_700_000L,
      dateAddedSeconds = 1_763_711_700L,
      width = 3000,
      height = 4000,
      albumName = "Camera",
    )
    val p2 = PhotoItem(
      id = 202L,
      uri = Uri.parse("content://media/external/images/media/202"),
      displayName = "BURST_2.jpg",
      sizeBytes = 2_500_000L,
      dateTakenMillis = 1_763_711_701_000L,
      dateAddedSeconds = 1_763_711_701L,
      width = 4000,
      height = 4000,
      albumName = "Camera",
    )
    val group = BurstGroup("burst_201", listOf(p1, p2), 4_000_000L)
    var keptId = -1L
    var jumpedId = -1L

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          BurstCompareSheet(
            burstGroup = group,
            onDismiss = {},
            onKeepBestAndTrashRest = { _, keep -> keptId = keep.id },
            onOpenLightbox = {},
          )
          DeckJumpSheet(
            visible = true,
            deckPhotos = listOf(p1, p2),
            favoriteIds = setOf(p2.id),
            onDismiss = {},
            onSelectPhoto = { jumpedId = it.id },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("jump_item_202").performClick()
    assertEquals(202L, jumpedId)
    composeTestRule.onNodeWithTag("burst_keep_selected_button").performClick()
    assertEquals(202L, keptId)
  }

  @Test
  fun `sample photo seeder handles MediaStore failure cleanly without crash`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val seeder = com.example.data.SamplePhotoSeeder(context)
    val result = seeder.seedSamplePhotosResult()
    assertNotNull(result)
  }

  @Test
  fun `update available popup modal displays version notes progress and actions`() {
    var downloadClicked = false
    var dismissed = false
    val release = com.example.data.GitHubReleaseInfo(
      tagName = "v1.2.0",
      title = "SweepGallery v1.2.0",
      notes = "Faster swipes and Studio square grids.",
      htmlUrl = "https://github.com/nazimunaeem/SweepGallery/releases/tag/v1.2.0",
      apkDownloadUrl = "https://github.com/nazimunaeem/SweepGallery/releases/download/v1.2.0/app.apk",
      apkFileName = "SweepGallery-v1.2.0.apk",
      apkSizeBytes = 12_500_000L,
    )

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          com.example.ui.UpdateAvailableDialog(
            currentVersionName = "1.1.0",
            release = release,
            onDownload = { downloadClicked = true },
            onDismiss = { dismissed = true },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("update_available_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Update Available").assertIsDisplayed()
    composeTestRule.onNodeWithText("v1.1.0 → v1.2.0").assertIsDisplayed()
    composeTestRule.onNodeWithText("Faster swipes and Studio square grids.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_github_release_button").performClick()
    assertEquals(true, downloadClicked)
    composeTestRule.onNodeWithTag("update_dialog_later_button").performClick()
    assertEquals(true, dismissed)
  }

  @Test
  fun `in app updater validates apk header and shows download progress ui`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val apkFile = com.example.data.InAppUpdater.targetApkFile(context, "v1.2.0")
    apkFile.parentFile?.mkdirs()
    val bytes = ByteArray(8192) { 0x11 }
    bytes[0] = 0x50
    bytes[1] = 0x4B
    apkFile.writeBytes(bytes)
    assertTrue(com.example.data.InAppUpdater.isValidApkFile(apkFile))
    val intent = com.example.data.InAppUpdater.buildInstallApkIntent(context, apkFile)
    assertEquals("application/vnd.android.package-archive", intent.type)
    apkFile.delete()

    val release = com.example.data.GitHubReleaseInfo(
      tagName = "v1.2.0",
      title = "SweepGallery v1.2.0",
      notes = "Auto-update support",
      htmlUrl = "https://github.com/nazimunaeem/SweepGallery/releases/tag/v1.2.0",
      apkDownloadUrl = "https://github.com/nazimunaeem/SweepGallery/releases/download/v1.2.0/app.apk",
      apkSizeBytes = 10_000_000L,
    )

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          com.example.ui.UpdateAvailableDialog(
            currentVersionName = "1.1.0",
            release = release,
            downloadState = com.example.ui.viewmodel.UpdateDownloadState.Downloading(
              tagName = "v1.2.0",
              bytesDownloaded = 5_000_000L,
              totalBytes = 10_000_000L,
            ),
            onDownload = {},
            onDismiss = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("update_download_progress").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel_update_download_button").assertIsDisplayed()
  }
}
