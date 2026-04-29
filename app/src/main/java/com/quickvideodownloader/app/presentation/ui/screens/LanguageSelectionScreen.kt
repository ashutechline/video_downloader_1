package com.quickvideodownloader.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quickvideodownloader.app.R
import com.quickvideodownloader.app.domain.model.LanguageItem
import com.quickvideodownloader.app.presentation.viewmodel.LanguageViewModel
import com.quickvideodownloader.app.presentation.ui.components.CommonTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onContinue: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryColor = Color(0xFFE65100) // Deep orange

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.language_selection_title),
                showBackButton = false
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.saveLanguage(onContinue) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text(
                            text = stringResource(R.string.continue_btn),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                placeholder = { Text(stringResource(R.string.search_language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    containerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.filteredLanguages) { language ->
                    LanguageCard(
                        language = language,
                        isSelected = uiState.selectedLanguageCode == language.code,
                        primaryColor = primaryColor,
                        onClick = { viewModel.selectLanguage(language.code) }
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageCard(
    language: LanguageItem,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = Color.White
    val borderColor = if (isSelected) primaryColor else Color.Transparent
    val elevation = if (isSelected) 4.dp else 0.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = language.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF212529) else Color(0xFF495057)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${language.nativeName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFadb5bd)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.dp, Color(0xFFdee2e6), RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
