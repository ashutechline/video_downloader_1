package com.example.video_downloder.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.video_downloder.presentation.ui.components.NoteItem
import com.example.video_downloder.presentation.viewmodel.NoteViewModel
import com.example.video_downloder.presentation.ui.components.CommonTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "My Notes",
                onBackClick = { /* Handle back if needed, but currently no back nav in MainActivity for this */ }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.notes) { note ->
                NoteItem(
                    note = note,
                    onDeleteClick = { /* No-op */ }
                )
            }
        }
    }
}
