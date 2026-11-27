package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.model.GoalSortOption
import com.example.piggybank.model.GoalStatusFilter
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.sampleSavingsGoals
import com.example.piggybank.ui.components.BorderlessEmptyState
import com.example.piggybank.ui.components.rememberRemainingLazyListHeight
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.DarkTeal
import com.example.piggybank.ui.theme.ExpenseRed
import com.example.piggybank.ui.theme.ExpenseTint
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.OutlineGray
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary
import com.example.piggybank.util.toRupiah
import com.example.piggybank.viewmodel.GoalUiState
import com.example.piggybank.viewmodel.GoalViewModel

@Composable
fun GoalListScreen(
    viewModel: GoalViewModel,
    onGoalClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalListContent(
        uiState = uiState,
        onStatusFilterSelected = viewModel::setStatusFilter,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSortOptionSelected = viewModel::setSortOption,
        onGoalClick = onGoalClick,
        onRefresh = viewModel::fetchGoals,
        modifier = modifier
    )
}

@Composable
fun GoalListContent(
    uiState: GoalUiState,
    onStatusFilterSelected: (GoalStatusFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelected: (GoalSortOption) -> Unit,
    onGoalClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val emptyAnchorIndex = 2
    val emptyAreaHeight = rememberRemainingLazyListHeight(
        listState = listState,
        anchorItemIndex = emptyAnchorIndex,
        itemSpacing = 12.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (uiState.displayedGoals.isEmpty()) 0.dp else 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Target Celengan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wujudkan target finansialmu bertahap",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            item {
                GoalStatusFilterRow(
                    selectedFilter = uiState.selectedStatusFilter,
                    onFilterSelected = onStatusFilterSelected
                )
            }

            item {
                GoalSearchAndSortBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    selectedSortOption = uiState.selectedSortOption,
                    onSortOptionSelected = onSortOptionSelected
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                }
            } else if (uiState.displayedGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = emptyAreaHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyGoalsState(
                            hasAnyGoals = uiState.goals.isNotEmpty(),
                            searchQuery = uiState.searchQuery,
                            selectedStatusFilter = uiState.selectedStatusFilter,
                            onClearSearch = { onSearchQueryChange("") }
                        )
                    }
                }
            } else {
                items(
                    items = uiState.displayedGoals,
                    key = { it.id }
                ) { goal ->
                    SavingsGoalItemCard(
                        goal = goal,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStatusFilterRow(
    selectedFilter: GoalStatusFilter,
    onFilterSelected: (GoalStatusFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalStatusFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                onClick = { onFilterSelected(filter) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = CircleShape,
                color = if (isSelected) PrimaryTeal else CardSurface,
                border = if (isSelected) null else BorderStroke(1.dp, OutlineGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalSearchAndSortBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSortOption: GoalSortOption,
    onSortOptionSelected: (GoalSortOption) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Cari nama target...",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryTeal
            ),
            modifier = Modifier.weight(1f)
        )

        Box {
            Surface(
                onClick = { sortMenuExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = CardSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (sortMenuExpanded) PrimaryTeal else CardBorder
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Urutkan target",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                containerColor = CardSurface
            ) {
                GoalSortOption.entries.forEach { option ->
                    val isSelected = option == selectedSortOption
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) PrimaryTeal else TextPrimary
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            onSortOptionSelected(option)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalItemCard(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val isOverdue = goal.isOverdue()
    val cardBorderColor = if (isOverdue) ExpenseRed.copy(alpha = 0.45f) else CardBorder
    val deadlineColor = if (isOverdue) ExpenseRed else TextSecondary
    val progressAccentColor = if (goal.isCompleted) IncomeGreen else PrimaryTeal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = goal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isOverdue) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseTint
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Melewati tenggat",
                                tint = ExpenseRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Lewat tenggat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = progressAccentColor
                ) {
                    Text(
                        text = "${goal.progressPercentage}%",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressAccentColor,
                trackColor = CardBorder,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.currentAmount.toRupiah()} / ${goal.targetAmount.toRupiah()}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (goal.deadline.isNotBlank()) "Target: ${goal.deadline}" else "Tanpa target",
                    fontSize = 12.sp,
                    fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                    color = deadlineColor
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalsState(
    hasAnyGoals: Boolean,
    searchQuery: String,
    selectedStatusFilter: GoalStatusFilter,
    onClearSearch: () -> Unit
) {
    val trimmedQuery = searchQuery.trim()
    val isSearching = hasAnyGoals && trimmedQuery.isNotEmpty()
    val emptyMessage = when {
        !hasAnyGoals -> "Belum ada target celengan. Tekan tombol + untuk memulai!"
        trimmedQuery.isNotEmpty() -> "Target celengan \"$trimmedQuery\" tidak ditemukan."
        else -> "Belum ada target celengan pada kategori \"${selectedStatusFilter.label}\"."
    }

    BorderlessEmptyState(
        icon = if (trimmedQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Savings,
        message = emptyMessage,
        actionLabel = if (isSearching) "Hapus Pencarian" else null,
        onActionClick = if (isSearching) onClearSearch else null,
        messageColor = if (isSearching) TextPrimary else TextSecondary
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GoalListScreenPreview() {
    val samples = sampleSavingsGoals()
    PiggyBankTheme {
        GoalListContent(
            uiState = GoalUiState(
                goals = samples,
                displayedGoals = samples,
                totalActiveGoals = samples.size,
                totalSavedAllGoals = samples.sumOf { it.currentAmount },
                isLoading = false
            ),
            onStatusFilterSelected = {},
            onSearchQueryChange = {},
            onSortOptionSelected = {},
            onGoalClick = {},
            onRefresh = {}
        )
    }
}
