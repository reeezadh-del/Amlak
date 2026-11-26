package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Property
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormatUtils
import com.example.ui.components.PropertyCard
import com.example.ui.theme.PersianTeal
import com.example.ui.theme.PersianTealDark
import com.example.ui.viewmodel.PropertyFilterType
import com.example.ui.viewmodel.PropertySortOption

@Composable
fun ExploreScreen(
    propertiesState: UiState<List<Property>>,
    searchQuery: String,
    selectedType: PropertyFilterType,
    sortOption: PropertySortOption,
    favoriteIds: Set<String>,
    currentUserId: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeSelected: (PropertyFilterType) -> Unit,
    onSortOptionSelected: (PropertySortOption) -> Unit,
    onPropertyClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 700.dp) 2 else 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Editorial direct-trade banner
            item(span = { GridItemSpan(columns) }) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PersianTeal, PersianTealDark)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Handshake,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(28.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "معامله مستقیم با مالک واقعی",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تمام آگهی‌ها مستقیماً توسط مالکان ثبت شده‌اند؛ بدون واسطه و کمیسیون املاک گفتگو کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                    }
                }
            }

            // Search bar + Sort button
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("explore_search_input"),
                        placeholder = { Text("جستجو بر اساس شهر، محله یا عنوان...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "جستجو")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )

                    Box {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .height(54.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("sort_button")
                                .clickable { showSortMenu = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = "مرتب‌سازی",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = sortOption.labelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            PropertySortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.labelFa) },
                                    onClick = {
                                        onSortOptionSelected(option)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Property Type filter chips
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PropertyFilterType.entries.forEach { filterType ->
                        val selected = selectedType == filterType
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterTypeSelected(filterType) },
                            label = { Text(filterType.labelFa) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_chip_${filterType.name}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }

            when (propertiesState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(span = { GridItemSpan(columns) }) {
                        EmptyStateView(
                            icon = Icons.Filled.HomeWork,
                            title = "خطا در بارگذاری آگهی‌ها",
                            subtitle = propertiesState.message,
                        )
                    }
                }

                is UiState.Success -> {
                    val items = propertiesState.data
                    if (items.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyStateView(
                                icon = Icons.Filled.HomeWork,
                                title = if (searchQuery.isNotBlank() || selectedType != PropertyFilterType.ALL) {
                                    "ملکی با این مشخصات پیدا نشد"
                                } else {
                                    "هنوز آگهی‌ای ثبت نشده است"
                                },
                                subtitle = if (searchQuery.isNotBlank() || selectedType != PropertyFilterType.ALL) {
                                    "فیلتر یا عبارت جستجو را تغییر دهید تا خانه‌های بیشتری نمایش داده شوند."
                                } else {
                                    "اولین مالک باشید و خانه خود را بدون واسطه برای خریداران واقعی ثبت کنید."
                                },
                                actionContent = {
                                    Button(
                                        onClick = onCreateClick,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("empty_create_listing_btn"),
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ثبت رایگان آگهی خانه")
                                    }
                                },
                            )
                        }
                    } else {
                        item(span = { GridItemSpan(columns) }) {
                            Text(
                                text = "${FormatUtils.toPersianDigits(items.size.toString())} ملک فعال مستقیم از مالک",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(items, key = { it.id }) { property ->
                            PropertyCard(
                                property = property,
                                isFavorite = property.id in favoriteIds,
                                isOwnListing = property.ownerId == currentUserId,
                                onClick = { onPropertyClick(property.id) },
                                onToggleFavorite = { onToggleFavorite(property.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
