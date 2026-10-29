package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** A heading and the settings under it. Headings are heavier than setting names, so a page reads in groups. */
@Composable
internal fun SettingsGroup(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
        Text(
            stringResource(title),
            style = Grapho.type.heading,
            color = Grapho.colors.textDefault,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceXs),
        )
        content()
    }
}

/** The room between groups on a page. */
@Composable
internal fun SettingsPage(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.space2xl)) { content() }
}
