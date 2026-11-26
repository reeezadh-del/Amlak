package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CoverPreset
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.ui.components.FormatUtils
import com.example.ui.components.PropertyCoverImage
import com.example.ui.components.compressPhotoUriToBase64
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditListingScreen(
    existingProperty: Property? = null,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        propertyType: PropertyType,
        city: String,
        neighborhood: String,
        priceToman: Long,
        areaSqm: Long,
        bedrooms: Long,
        buildYear: Long,
        floor: Long,
        hasParking: Boolean,
        hasElevator: Boolean,
        hasStorage: Boolean,
        hasTitleDeed: Boolean,
        coverPreset: CoverPreset,
        photoBase64: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditing = existingProperty != null

    var title by remember(existingProperty?.id) { mutableStateOf(existingProperty?.title ?: "") }
    var city by remember(existingProperty?.id) { mutableStateOf(existingProperty?.city ?: "تهران") }
    var neighborhood by remember(existingProperty?.id) { mutableStateOf(existingProperty?.neighborhood ?: "") }
    var priceMillions by remember(existingProperty?.id) {
        mutableStateOf(
            if (existingProperty != null && existingProperty.priceToman > 0) {
                (existingProperty.priceToman / 1_000_000L).toString()
            } else ""
        )
    }
    var areaSqm by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.areaSqm?.toString() ?: "")
    }
    var bedrooms by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.bedrooms?.toString() ?: "2")
    }
    var buildYear by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.buildYear?.toString() ?: "1401")
    }
    var floor by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.floor?.toString() ?: "2")
    }
    var description by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.description ?: "")
    }
    var propertyType by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.typeEnum() ?: PropertyType.APARTMENT)
    }
    var hasParking by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.hasParking ?: true)
    }
    var hasElevator by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.hasElevator ?: true)
    }
    var hasStorage by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.hasStorage ?: true)
    }
    var hasTitleDeed by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.hasTitleDeed ?: true)
    }
    var coverPreset by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.coverEnum() ?: CoverPreset.HERO_HOME)
    }
    var photoBase64 by remember(existingProperty?.id) {
        mutableStateOf(existingProperty?.photoBase64)
    }
    var isCompressingPhoto by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingProperty?.id) {
        if (existingProperty != null) {
            title = existingProperty.title
            city = existingProperty.city
            neighborhood = existingProperty.neighborhood
            priceMillions = (existingProperty.priceToman / 1_000_000L).toString()
            areaSqm = existingProperty.areaSqm.toString()
            bedrooms = existingProperty.bedrooms.toString()
            buildYear = existingProperty.buildYear.toString()
            floor = existingProperty.floor.toString()
            description = existingProperty.description
            propertyType = existingProperty.typeEnum()
            hasParking = existingProperty.hasParking
            hasElevator = existingProperty.hasElevator
            hasStorage = existingProperty.hasStorage
            hasTitleDeed = existingProperty.hasTitleDeed
            coverPreset = existingProperty.coverEnum()
            photoBase64 = existingProperty.photoBase64
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isCompressingPhoto = true
            validationError = null
            scope.launch {
                val compressed = compressPhotoUriToBase64(context, uri)
                isCompressingPhoto = false
                if (compressed != null) {
                    photoBase64 = compressed
                    coverPreset = CoverPreset.CUSTOM_PHOTO
                } else {
                    validationError = "امکان پردازش تصویر انتخاب‌شده وجود نداشت."
                }
            }
        }
    }

    val parsedPriceToman = (priceMillions.trim().toLongOrNull() ?: 0L) * 1_000_000L
    val parsedArea = areaSqm.trim().toLongOrNull() ?: 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "ویرایش آگهی ملک" else "ثبت آگهی مستقیم مالک")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val cleanCity = city.trim()
                            val cleanNeighborhood = neighborhood.trim()
                            val cleanDesc = description.trim()
                            val priceVal = (priceMillions.trim().toLongOrNull() ?: 0L) * 1_000_000L
                            val areaVal = areaSqm.trim().toLongOrNull() ?: 0L
                            val bedroomsVal = bedrooms.trim().toLongOrNull() ?: -1L
                            val yearVal = buildYear.trim().toLongOrNull() ?: 0L
                            val floorVal = floor.trim().toLongOrNull() ?: 999L

                            val error = when {
                                cleanTitle.length !in 3..120 -> "عنوان آگهی باید بین ۳ تا ۱۲۰ کاراکتر باشد."
                                cleanCity.length !in 2..60 -> "نام شهر را وارد کنید (حداقل ۲ حرف)."
                                cleanNeighborhood.length !in 1..80 -> "نام محله را وارد کنید."
                                priceVal !in 1..1_000_000_000_000L -> "قیمت پیشنهادی به میلیون تومان را معتبر وارد کنید."
                                areaVal !in 10..100_000L -> "متراژ باید بین ۱۰ تا ۱۰۰,۰۰۰ متر باشد."
                                bedroomsVal !in 0..20L -> "تعداد خواب باید بین ۰ تا ۲۰ باشد."
                                yearVal !in 1300..1410L -> "سال ساخت باید بین ۱۳۰۰ تا ۱۴۱۰ باشد."
                                floorVal !in -2..60L -> "طبقه باید بین -۲ تا ۶۰ باشد."
                                cleanDesc.length > 1500 -> "توضیحات نباید بیشتر از ۱۵۰۰ کاراکتر باشد."
                                else -> null
                            }

                            if (error != null) {
                                validationError = error
                            } else {
                                validationError = null
                                onSubmit(
                                    cleanTitle,
                                    cleanDesc,
                                    propertyType,
                                    cleanCity,
                                    cleanNeighborhood,
                                    priceVal,
                                    areaVal,
                                    bedroomsVal,
                                    yearVal,
                                    floorVal,
                                    hasParking,
                                    hasElevator,
                                    hasStorage,
                                    hasTitleDeed,
                                    coverPreset,
                                    if (coverPreset == CoverPreset.CUSTOM_PHOTO) photoBase64 else null,
                                )
                            }
                        },
                        enabled = !isBusy && !isCompressingPhoto,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_listing_button"),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isEditing) "ذخیره تغییرات" else "انتشار مستقیم آگهی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    }
                }

                // Cover Photo Preview & Picker
                Text(
                    text = "تصویر ملک",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    PropertyCoverImage(
                        coverPreset = coverPreset,
                        photoBase64 = if (coverPreset == CoverPreset.CUSTOM_PHOTO) photoBase64 else null,
                        contentDescription = "پیش‌نمایش تصویر ملک",
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCompressingPhoto) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pick_photo_button"),
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("انتخاب عکس از گالری")
                    }

                    if (photoBase64 != null) {
                        OutlinedButton(
                            onClick = {
                                photoBase64 = null
                                coverPreset = CoverPreset.HERO_HOME
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "حذف عکس")
                        }
                    }
                }

                // Preset cover selector
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        CoverPreset.HERO_HOME,
                        CoverPreset.VILLA_NORTH,
                        CoverPreset.INTERIOR_LIVING,
                    ).forEach { preset ->
                        FilterChip(
                            selected = coverPreset == preset,
                            onClick = { coverPreset = preset },
                            label = { Text(preset.labelFa) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                // Property Type
                Text(
                    text = "نوع ملک",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PropertyType.entries.forEach { type ->
                        val selected = propertyType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable { propertyType = type },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = type.labelFa,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("عنوان آگهی *") },
                    placeholder = { Text("مثلاً: آپارتمان ۱۱۰ متری نوساز در یوسف‌آباد") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { if (it.length <= 60) city = it },
                        label = { Text("شهر *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_city"),
                    )
                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { if (it.length <= 80) neighborhood = it },
                        label = { Text("محله *") },
                        placeholder = { Text("مثلاً: سعادت‌آباد") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_neighborhood"),
                    )
                }

                OutlinedTextField(
                    value = priceMillions,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 7) priceMillions = input
                    },
                    label = { Text("قیمت پیشنهادی (میلیون تومان) *") },
                    placeholder = { Text("مثلاً: 8500 برای ۸ میلیارد و ۵۰۰ میلیون") },
                    supportingText = {
                        if (parsedPriceToman > 0) {
                            val perMeter = FormatUtils.formatPricePerMeter(parsedPriceToman, parsedArea)
                            val summary = FormatUtils.formatPriceToman(parsedPriceToman)
                            Text(if (perMeter.isNotBlank()) "$summary ($perMeter)" else summary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_price"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = areaSqm,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 5) areaSqm = it },
                        label = { Text("متراژ (متر) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_area"),
                    )
                    OutlinedTextField(
                        value = bedrooms,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) bedrooms = it },
                        label = { Text("تعداد خواب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bedrooms"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = buildYear,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) buildYear = it },
                        label = { Text("سال ساخت") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_build_year"),
                    )
                    OutlinedTextField(
                        value = floor,
                        onValueChange = {
                            if ((it.isEmpty() || it == "-" || it.toIntOrNull() != null) && it.length <= 3) {
                                floor = it
                            }
                        },
                        label = { Text("طبقه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_floor"),
                    )
                }

                Text(
                    text = "امکانات و سند",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AmenityToggleChip("پارکینگ", hasParking) { hasParking = !hasParking }
                    AmenityToggleChip("آسانسور", hasElevator) { hasElevator = !hasElevator }
                    AmenityToggleChip("انباری", hasStorage) { hasStorage = !hasStorage }
                    AmenityToggleChip("سند تک‌برگ", hasTitleDeed) { hasTitleDeed = !hasTitleDeed }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1500) description = it },
                    label = { Text("توضیحات تکمیلی برای خریدار") },
                    placeholder = { Text("نورگیری، وضعیت بازسازی، شرایط پرداخت یا زمان مناسب بازدید...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_description"),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AmenityToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = Modifier.minimumInteractiveComponentSize(),
    )
}
