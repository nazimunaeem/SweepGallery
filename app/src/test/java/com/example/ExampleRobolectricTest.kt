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
import com.example.model.AlbumSummary
import com.example.model.MonthSummary
import com.example.model.PhotoItem
import com.example.ui.components.PhotoInfoBottomSheet
import com.example.ui.screens.StudioStatsScreen
import com.example.ui.theme.TindPhotoTheme
import com.example.ui.viewmodel.MONTH_ALL
import com.example.ui.viewmodel.TindPhotoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  fun `studio screen switches between month and album square grids`() {
    val sampleState = TindPhotoUiState(
      hasPermission = true,
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
          )
        }
      }
    }

    composeTestRule.waitForIdle()
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
  fun `photo info bottom sheet shows directory show in files and metadata`() {
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
    composeTestRule.onNodeWithText("JPEG").assertIsDisplayed()
    composeTestRule.onNodeWithText("4032 × 3024 (12.2 MP)").assertIsDisplayed()
  }

  @Test
  fun `swipe deck renders populated sample photos cleanly without crash`() {
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
        uri = Uri.parse("content://media/external/images/media/2"),
        displayName = "IMG_Alpine_Morning.jpg",
        sizeBytes = 900_000L,
        dateTakenMillis = 1_763_611_700_000L,
        dateAddedSeconds = 1_763_611_700L,
        width = 1080,
        height = 1440,
        albumName = "SweepGallerySamples",
        mimeType = "image/jpeg",
        directoryPath = "Pictures/SweepGallerySamples",
      ),
    )

    val state = TindPhotoUiState(
      hasPermission = true,
      allPhotosCount = samplePhotos.size,
      deckPhotos = samplePhotos,
      months = listOf(
        MonthSummary(MONTH_ALL, "All Months", 2, 2, 2_100_000L, false),
        MonthSummary("2026-11", "Nov 2026", 2, 2, 2_100_000L, false),
      ),
      albums = listOf(
        AlbumSummary("All Photos", 2, 2_100_000L),
        AlbumSummary("SweepGallerySamples", 2, 2_100_000L),
      ),
    )

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.setContent {
        TindPhotoTheme {
          com.example.ui.screens.SwipeDeckScreen(
            state = state,
            onSwipe = { _, _ -> },
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
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("keep_action_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("trash_action_button").assertIsDisplayed()
  }

  @Test
  fun `sample photo seeder handles MediaStore failure cleanly without crash`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val seeder = com.example.data.SamplePhotoSeeder(context)
    val result = seeder.seedSamplePhotosResult()
    assertNotNull(result)
  }

  @Test
  fun `update available popup modal displays version notes and actions`() {
    var downloadClicked = false
    var dismissed = false
    val release = com.example.data.GitHubReleaseInfo(
      tagName = "v1.2.0",
      title = "SweepGallery v1.2.0",
      notes = "Faster swipes and Studio square grids.",
      htmlUrl = "https://github.com/example/SweepGallery/releases/tag/v1.2.0",
      apkDownloadUrl = "https://github.com/example/SweepGallery/releases/download/v1.2.0/app.apk",
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
}
