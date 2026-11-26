package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Property
import com.example.data.model.PropertyStatus
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormatUtils
import com.example.ui.components.PropertyCoverImage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageGreenLight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    propertyState: UiState<Property?>,
    isFavorite: Boolean,
    currentUserId: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onStartChat: (Property) -> Unit,
    onEditClick: (String) -> Unit,
    onMarkSold: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showSoldDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    when (propertyState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Scaffold { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    EmptyStateView(
                        icon = Icons.Filled.HomeWork,
                        title = "خطا در دریافت اطلاعات ملک",
                        subtitle = propertyState.message,
                        actionContent = {
                            OutlinedButton(onClick = onBack) { Text("بازگشت") }
                        },
                    )
                }
            }
        }

        is UiState.Success -> {
            val property = propertyState.data
            if (property == null) {
                Scaffold { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        EmptyStateView(
                            icon = Icons.Filled.HomeWork,
                            title = "آگهی یافت نشد",
                            subtitle = "ممکن است این ملک توسط مالک حذف شده یا به فروش رسیده باشد.",
                            actionContent = {
                                OutlinedButton(onClick = onBack) { Text("بازگشت به آگهی‌ها") }
                            },
                        )
                    }
                }
                return
            }

            val isOwner = property.ownerId == currentUserId
            val isSold = property.statusEnum() == PropertyStatus.SOLD

            if (showSoldDialog) {
                AlertDialog(
                    onDismissRequest = { showSoldDialog = false },
                    title = { Text("ثبت به عنوان فروخته‌شده") },
                    text = { Text("پس از تغییر وضعیت به «فروخته شد»، آگهی از فهرست خریداران خارج شده و دیگر قابل ویرایش نخواهد بود. ادامه می‌دهید؟") },
                    confirmButton = {
                        Button(onClick = {
                            showSoldDialog = false
                            onMarkSold(property.id)
                        }) { Text("تأیید و بستن آگهی") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSoldDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("حذف آگهی") },
                    text = { Text("آیا از حذف دائمی این آگهی مطمئن هستید؟") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                onDelete(property.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("حذف") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("انصراف") }
                    },
                )
            }

            Scaffold(
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isOwner) {
                                if (!isSold) {
                                    OutlinedButton(
                                        onClick = { onEditClick(property.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("edit_listing_button"),
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ویرایش")
                                    }
                                    Button(
                                        onClick = { showSoldDialog = true },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp)
                                            .testTag("mark_sold_button"),
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فروخته شد")
                                    }
                                } else {
                                    Button(
                                        onClick = { showDeleteDialog = true },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("delete_sold_button"),
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حذف آگهی فروخته‌شده")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onStartChat(property) },
                                    enabled = !isBusy && !isSold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("start_direct_chat_button"),
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(Icons.Filled.Chat, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isSold) "این ملک فروخته شده است" else "گفتگوی مستقیم با مالک (${property.ownerName})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                    ) {
                        PropertyCoverImage(
                            coverPreset = property.coverEnum(),
                            photoBase64 = property.photoBase64,
                            contentDescription = property.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.45f),
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = CircleShape,
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "بازگشت",
                                        tint = Color.White,
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isOwner && !isSold) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = CircleShape,
                                    ) {
                                        IconButton(
                                            onClick = { onToggleFavorite(property.id) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("detail_favorite_button"),
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = if (isFavorite) "حذف از نشان‌شده‌ها" else "نشان کردن",
                                                tint = if (isFavorite) Color(0xFFFF6B6B) else Color.White,
                                            )
                                        }
                                    }
                                }
                                if (isOwner && !isSold) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = CircleShape,
                                    ) {
                                        IconButton(
                                            onClick = { showDeleteDialog = true },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("detail_delete_button"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "حذف آگهی",
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            color = if (isSold) MaterialTheme.colorScheme.error else SageGreen,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (isSold) "فروخته شد" else "ارتباط مستقیم با مالک • بدون کمیسیون",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = property.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "${property.city}، ${property.neighborhood}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Price Highlight Card
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "قیمت کل پیشنهادی مالک",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FormatUtils.formatPriceToman(property.priceToman),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        text = FormatUtils.formatPricePerMeter(property.priceToman, property.areaSqm),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        // Key Specs Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DetailStatCard(
                                icon = Icons.Filled.SquareFoot,
                                label = "متراژ",
                                value = "${FormatUtils.toPersianDigits(property.areaSqm.toString())} متر",
                                modifier = Modifier.weight(1f),
                            )
                            DetailStatCard(
                                icon = Icons.Filled.Bed,
                                label = "اتاق خواب",
                                value = if (property.bedrooms == 0L) "بدون خواب" else "${FormatUtils.toPersianDigits(property.bedrooms.toString())} خواب",
                                modifier = Modifier.weight(1f),
                            )
                            DetailStatCard(
                                icon = Icons.Filled.CalendarToday,
                                label = "سال ساخت",
                                value = FormatUtils.toPersianDigits(property.buildYear.toString()),
                                modifier = Modifier.weight(1f),
                            )
                            DetailStatCard(
                                icon = Icons.Filled.Layers,
                                label = "طبقه",
                                value = FormatUtils.toPersianDigits(property.floor.toString()),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Amenities
                        Text(
                            text = "امکانات و وضعیت سند",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AmenityBadge(Icons.Filled.LocalParking, "پارکینگ", property.hasParking)
                            AmenityBadge(Icons.Filled.Elevator, "آسانسور", property.hasElevator)
                            AmenityBadge(Icons.Filled.Inventory2, "انباری", property.hasStorage)
                            AmenityBadge(Icons.Filled.Description, "سند تک‌برگ", property.hasTitleDeed)
                        }

                        // Description
                        if (property.description.isNotBlank()) {
                            Text(
                                text = "توضیحات مالک",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Text(
                                    text = property.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                )
                            }
                        }

                        // Owner Info Card
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = SageGreenLight.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.25f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SageGreen),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "مالک: ${property.ownerName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "تماس و هماهنگی مستقیم داخل برنامه بدون واسطه املاکی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SageGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AmenityBadge(icon: ImageVector, label: String, available: Boolean) {
    Surface(
        color = if (available) SageGreenLight.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (available) SageGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (available) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = if (available) "$label دارد" else "بدون $label",
                style = MaterialTheme.typography.labelMedium,
                color = if (available) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
