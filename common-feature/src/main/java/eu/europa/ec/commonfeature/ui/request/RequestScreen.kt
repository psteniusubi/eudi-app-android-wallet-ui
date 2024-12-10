/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.RelyingPartyData
import eu.europa.ec.uilogic.component.content.ContentHeader
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    navController: NavController,
    viewModel: RequestViewModel,
) {
    val state = viewModel.viewState.value

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        navigatableAction = ScreenNavigateAction.NONE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.SecondaryButtonPressed) },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                stickyBottomModifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    //.background(Color.Blue)
                    ,
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(
                        config = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            enabled = !state.isLoading && state.allowShare,
                            onClick = { viewModel.setEvent(Event.PrimaryButtonPressed) }
                        )
                    )
                )
            ) {
                Text(text = stringResource(R.string.request_sticky_button_text))
            }
        },
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {

                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute)
                    }

                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is Effect.Navigation.PopTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = false
                        )
                    }
                }
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState
        )

        if (isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false
                        )
                    )
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.DoWork)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top
    ) {
        // Screen Header.
        ContentHeader(
            modifier = Modifier.fillMaxWidth(),
            config = state.headerConfig,
        )

        // Screen Main Content.
        /*Request(
            modifier = Modifier.weight(1f).background(Color.Red),
            items = state.items,
            noData = state.noItems,
            isShowingFullUserInfo = state.isShowingFullUserInfo,
            onEventSend = onEventSend,
            listState = rememberLazyListState(),
            contentPadding = paddingValues
        )*/

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.items) { requestDocumentsUi ->
                requestDocumentsUi.documentsUi.forEach { documentUiItem ->
                    WrapExpandableListItem(
                        data = documentUiItem.item.expandableListItem,
                        isExpanded = false, // Track expand state if needed
                        onExpandedChange = { /* Handle expand state */ },
                        onExpandedItemClick = { listItemData ->
                            onEventSend(documentUiItem.item.event ?: return@WrapExpandableListItem)
                        },
                    )
                }
            }
        }

        /*WrapExpandableListItem(
            data = ExpandableListItemData(
                collapsed = ListItemData(
                    mainText = "Digital ID", //TODO
                    supportingText = stringResource(R.string.request_collapsed_supporting_text),
                )
            )
        )*/

        /*WrapListItems(
            modifier = Modifier.weight(1f)*//*.background(Color.Red)*//*,
            items = state.items.flatMap {
                it.documentsUi.map {
                    it.documentDetailsUiItem
                }
            },
            mainTextVerticalPadding = 12,
        )*/
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}

@Composable
private fun SheetContent(
    sheetContent: RequestBottomSheetContent,
    onEventSent: (event: Event) -> Unit
) {
    when (sheetContent) {
        RequestBottomSheetContent.BADGE -> {
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(id = R.string.request_bottom_sheet_badge_title),
                    message = stringResource(id = R.string.request_bottom_sheet_badge_subtitle),
                    positiveButtonText = stringResource(id = R.string.request_bottom_sheet_badge_primary_button_text),
                ),
                onPositiveClick = { onEventSent(Event.BottomSheet.Badge.PrimaryButtonPressed) },
            )
        }

        RequestBottomSheetContent.SUBTITLE -> {
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(id = R.string.request_bottom_sheet_subtitle_title),
                    message = stringResource(id = R.string.request_bottom_sheet_subtitle_subtitle),
                    positiveButtonText = stringResource(id = R.string.request_bottom_sheet_subtitle_primary_button_text),
                ),
                onPositiveClick = { onEventSent(Event.BottomSheet.Subtitle.PrimaryButtonPressed) },
            )
        }

        RequestBottomSheetContent.CANCEL -> {
            DialogBottomSheet(
                textData = BottomSheetTextData(
                    title = stringResource(id = R.string.request_bottom_sheet_cancel_title),
                    message = stringResource(id = R.string.request_bottom_sheet_cancel_subtitle),
                    positiveButtonText = stringResource(id = R.string.request_bottom_sheet_cancel_primary_button_text),
                    negativeButtonText = stringResource(id = R.string.request_bottom_sheet_cancel_secondary_button_text),
                ),
                onPositiveClick = { onEventSent(Event.BottomSheet.Cancel.PrimaryButtonPressed) },
                onNegativeClick = { onEventSent(Event.BottomSheet.Cancel.SecondaryButtonPressed) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                headerConfig = ContentHeaderConfig(
                    description = stringResource(R.string.request_header_description),
                    mainText = stringResource(R.string.request_header_main_text),
                    relyingPartyData = RelyingPartyData(
                        isVerified = true,
                        name = "Relying Party",
                        description = "requests the following"
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState()
        )
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentCancelPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.CANCEL,
            onEventSent = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentSubtitlePreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = RequestBottomSheetContent.SUBTITLE,
            onEventSent = {}
        )
    }
}