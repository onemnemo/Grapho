package com.onemnemo.grapho.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoSheet
import com.onemnemo.grapho.ui.components.ModalLayer
import com.onemnemo.grapho.ui.components.SheetFrame
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val SHEET_WIDTH = 1000.dp
private val SHEET_HEIGHT = 680.dp
private val NAV_WIDTH = 240.dp
private val PAGE_MAX_WIDTH = 680.dp

/**
 * Settings over the library or a note. From 600 dp wide it is a centred sheet with the pages in a list on the
 * left. Narrower, it fills the window: the list first, then one page with Back. Back closes a pushed view, then
 * the page on a narrow window, then Settings. [onCustomiseWheel] is set only while a note is open behind it.
 */
@Composable
fun SettingsOverlay(
    settings: AppSettings,
    onClose: () -> Unit,
    onLibraryChanged: () -> Unit,
    onCustomiseWheel: (() -> Unit)?,
) {
    val stacked = settingsStacked(LocalConfiguration.current.screenWidthDp.toFloat())
    var place by remember { mutableStateOf(SettingsPlace.start(stacked)) }
    val back = place.back(stacked)
    val pages = PageContext(settings, onLibraryChanged, onCustomiseWheel) { place = it }
    if (stacked) {
        ModalLayer(onDismiss = onClose, dismissOnScrim = false) { _, _ ->
            Surface(Modifier.fillMaxSize(), color = Grapho.colors.surfaceOverlay) {
                BackHandler(enabled = back != null) { back?.let { place = it } }
                Column(Modifier.fillMaxSize()) {
                    StackedBar(place, onBack = back?.let { next -> { place = next } }, onClose = onClose)
                    if (place.section == null) {
                        SettingsNav(null, stacked = true, onSelect = { place = SettingsPlace(it) })
                    } else {
                        Page(place, stacked = true, pages)
                    }
                }
            }
        }
    } else {
        GraphoSheet(
            title = stringResource(R.string.settings_title),
            onDismiss = onClose,
            frame = SheetFrame(SHEET_WIDTH, SHEET_HEIGHT),
        ) {
            BackHandler(enabled = place.pushed) { back?.let { place = it } }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SettingsNav(
                    selected = place.section ?: settingsGroups.first().first(),
                    stacked = false,
                    onSelect = { place = SettingsPlace(it) },
                    modifier = Modifier.width(NAV_WIDTH).fillMaxHeight(),
                )
                Page(place, stacked = false, pages, Modifier.weight(1f))
            }
        }
    }
}

/** What the pages need from the overlay: the settings to change and how to leave a page for a pushed view. */
private class PageContext(
    val settings: AppSettings,
    val onLibraryChanged: () -> Unit,
    val onCustomiseWheel: (() -> Unit)?,
    val goTo: (SettingsPlace) -> Unit,
)

@Composable
private fun StackedBar(place: SettingsPlace, onBack: (() -> Unit)?, onClose: () -> Unit) {
    val title = when {
        place.licence != null -> place.licence.name
        place.barTools -> stringResource(R.string.settings_bar_tools)
        place.section != null -> stringResource(place.section.label)
        else -> stringResource(R.string.settings_title)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(Grapho.metrics.sizeSheetHeader)
            .padding(start = if (onBack == null) GraphoSpace.space2xl else GraphoSpace.spaceSm)
            .padding(end = GraphoSpace.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) GraphoIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), onBack)
        Text(
            title,
            style = Grapho.type.title,
            color = Grapho.colors.textDefault,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        GraphoIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose)
    }
}

/** The open page, or the licence text pushed from About. Wide windows put the page's name above it. */
@Composable
private fun Page(place: SettingsPlace, stacked: Boolean, pages: PageContext, modifier: Modifier = Modifier) {
    val section = place.section ?: settingsGroups.first().first()
    key(place) {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (stacked) GraphoSpace.spaceLg else GraphoSpace.space3xl,
                    vertical = if (stacked) GraphoSpace.spaceSm else GraphoSpace.spaceLg,
                ),
        ) {
            Column(Modifier.widthIn(max = PAGE_MAX_WIDTH)) {
                val licence = place.licence
                if (licence != null) {
                    if (!stacked) PageTitle(licence.name, onBack = { pages.goTo(SettingsPlace(section)) })
                    LicenceText(licence)
                } else if (place.barTools) {
                    if (!stacked) {
                        PageTitle(stringResource(R.string.settings_bar_tools), onBack = {
                            pages.goTo(SettingsPlace(section))
                        })
                    }
                    BarToolsPage(pages.settings)
                } else {
                    if (!stacked) PageTitle(stringResource(section.label), onBack = null)
                    SectionPage(section, pages)
                }
            }
        }
    }
}

@Composable
private fun PageTitle(text: String, onBack: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = GraphoSpace.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) GraphoIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), onBack)
        Text(text, style = Grapho.type.title, color = Grapho.colors.textDefault, maxLines = 1)
    }
}

@Composable
private fun SectionPage(section: SettingsSection, pages: PageContext) {
    when (section) {
        SettingsSection.General -> GeneralPage(pages.settings) {
            pages.goTo(SettingsPlace(SettingsSection.General, barTools = true))
        }

        SettingsSection.Appearance -> AppearancePage(pages.settings)

        SettingsSection.Language -> LanguagePage()

        SettingsSection.Pen -> PenPage(pages.settings, pages.onCustomiseWheel)

        SettingsSection.Gestures -> GesturesPage(pages.settings)

        SettingsSection.Library -> LibraryPage(pages.onLibraryChanged)

        SettingsSection.About -> AboutPage { pages.goTo(SettingsPlace(SettingsSection.About, it)) }
    }
}
