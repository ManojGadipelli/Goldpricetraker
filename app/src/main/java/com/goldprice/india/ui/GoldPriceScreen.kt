package com.goldprice.india.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldprice.india.GoldUiState
import com.goldprice.india.GoldViewModel
import com.goldprice.india.PriceUnit
import com.goldprice.india.data.City
import com.goldprice.india.data.GoldData
import com.goldprice.india.data.GoldPrice
import com.goldprice.india.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

fun formatINR(amount: Long): String = "₹${currencyFormat.format(amount)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldPriceScreen(viewModel: GoldViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val includeGst by viewModel.includeGst.collectAsState()
    val priceUnit by viewModel.priceUnit.collectAsState()

    var showCitySheet by remember { mutableStateOf(false) }

    GoldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "✦ सोना Gold Prices",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = GoldBright
                            )
                            Text(
                                "Live rates from GoodReturns.in",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.fetchPrices() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = GoldBright
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface.copy(alpha = 0.95f),
                        titleContentColor = TextPrimary
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // City Selector
                item {
                    CitySelector(
                        selectedCity = selectedCity,
                        onCityClick = { showCitySheet = true }
                    )
                }

                // Controls Row
                item {
                    ControlsRow(
                        includeGst = includeGst,
                        priceUnit = priceUnit,
                        onToggleGst = { viewModel.toggleGst() },
                        onUnitChange = { viewModel.selectUnit(it) }
                    )
                }

                // Content
                item {
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        }
                    ) { state ->
                        when (state) {
                            is GoldUiState.Loading -> LoadingSection()
                            is GoldUiState.Error -> ErrorSection(
                                message = state.message,
                                onRetry = { viewModel.fetchPrices() }
                            )
                            is GoldUiState.Success -> SuccessSection(
                                data = state.data,
                                includeGst = includeGst,
                                priceUnit = priceUnit,
                                computePrice = viewModel::computePrice
                            )
                        }
                    }
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "* Prices sourced from GoodReturns.in. GST @3%. 1 Tola = 11.664g.\nRates are indicative. Contact jeweller for exact pricing.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // City Bottom Sheet
    if (showCitySheet) {
        CityBottomSheet(
            cities = viewModel.cities,
            selectedCity = selectedCity,
            onCitySelect = {
                viewModel.selectCity(it)
                showCitySheet = false
            },
            onDismiss = { showCitySheet = false }
        )
    }
}

@Composable
fun GoldBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF1A1200), DarkBg)
                )
            )
    ) {
        content()
    }
}

@Composable
fun CitySelector(selectedCity: City, onCityClick: () -> Unit) {
    Card(
        onClick = onCityClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Selected City", fontSize = 11.sp, color = TextSecondary)
                Text(
                    selectedCity.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GoldBright
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun ControlsRow(
    includeGst: Boolean,
    priceUnit: PriceUnit,
    onToggleGst: () -> Unit,
    onUnitChange: (PriceUnit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Unit Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriceUnit.entries.forEach { unit ->
                val selected = priceUnit == unit
                FilterChip(
                    selected = selected,
                    onClick = { onUnitChange(unit) },
                    label = { Text(unit.label, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DarkSurface,
                        selectedLabelColor = GoldBright,
                        containerColor = DarkCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = GoldBright,
                        borderColor = DarkBorder
                    )
                )
            }
        }

        // GST Toggle
        Card(
            onClick = onToggleGst,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (includeGst) Color(0xFF0D2010) else DarkCard
            ),
            border = BorderStroke(
                1.dp,
                if (includeGst) GstHighlight.copy(alpha = 0.6f) else DarkBorder
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (includeGst) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (includeGst) GstHighlight else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (includeGst) "Including GST (3%)" else "Excluding GST (3%)",
                    fontWeight = FontWeight.Medium,
                    color = if (includeGst) GstHighlight else TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LoadingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { ShimmerCard() }
    }
}

@Composable
fun ShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = shimmerAlpha)
        ),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading prices...", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun ErrorSection(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF200800)),
        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(36.dp))
            Text(message, color = ErrorColor, textAlign = TextAlign.Center, fontSize = 14.sp)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GoldBright)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry", color = GoldBright)
            }
        }
    }
}

@Composable
fun SuccessSection(
    data: GoldData,
    includeGst: Boolean,
    priceUnit: PriceUnit,
    computePrice: (Long, Boolean, PriceUnit) -> Long
) {
    val timeStr = remember(data.fetchedAt) {
        SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(data.fetchedAt))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Last updated
        Text(
            "Last updated: $timeStr",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Price Cards
        data.prices.forEach { price ->
            GoldPriceCard(
                price = price,
                includeGst = includeGst,
                priceUnit = priceUnit,
                computePrice = computePrice
            )
        }

        // Summary Table
        Spacer(modifier = Modifier.height(4.dp))
        SummaryTable(data = data, computePrice = computePrice)
    }
}

data class KaratStyle(
    val color: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val label: String,
)

fun karatStyle(karat: String) = when (karat) {
    "24K" -> KaratStyle(GoldBright, Color(0xFF2A1E00), Color(0xFF0A0800), "Purest Gold")
    "22K" -> KaratStyle(GoldMid, Color(0xFF221A00), Color(0xFF0A0800), "Jewellery Gold")
    "18K" -> KaratStyle(Gold18K, Color(0xFF1C1400), Color(0xFF0A0800), "Mixed Alloy")
    else  -> KaratStyle(TextPrimary, DarkSurface, DarkBg, "")
}

@Composable
fun GoldPriceCard(
    price: GoldPrice,
    includeGst: Boolean,
    priceUnit: PriceUnit,
    computePrice: (Long, Boolean, PriceUnit) -> Long
) {
    val style = karatStyle(price.karat)
    val displayPrice = computePrice(price.pricePerGram, includeGst, priceUnit)
    val basePrice = (price.pricePerGram * priceUnit.multiplier).toLong()
    val gstAmount = (basePrice * 0.03).toLong()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = style.gradientStart
        ),
        border = BorderStroke(1.dp, style.color.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(style.gradientStart, style.gradientEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Karat Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(style.color.copy(alpha = 0.15f))
                            .border(1.dp, style.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            price.karat,
                            color = style.color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            style.label,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            "Purity: ${price.purity}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Price
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatINR(displayPrice),
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            priceUnit.label + if (includeGst) " + GST" else "",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // GST Breakdown
                if (includeGst) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = style.color.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GstBreakdownItem("Base Price", formatINR(basePrice), TextSecondary)
                        GstBreakdownItem("GST (3%)", formatINR(gstAmount), TextSecondary)
                        GstBreakdownItem("Total", formatINR(displayPrice), GstHighlight)
                    }
                }

                // Quick compare row (if not showing GST)
                if (!includeGst) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "With GST: ${formatINR((displayPrice * 1.03).toLong())}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GstBreakdownItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontSize = 10.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun SummaryTable(
    data: GoldData,
    computePrice: (Long, Boolean, PriceUnit) -> Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(12.dp)
            ) {
                Text(
                    "📊  COMPLETE PRICE TABLE",
                    color = GoldBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.08.sp
                )
            }

            // Column headers
            SummaryRow(
                col1 = "Karat",
                col2 = "Per Gram",
                col3 = "Per 10g",
                col4 = "+GST/g",
                headerColor = TextSecondary,
                isHeader = true
            )

            data.prices.forEachIndexed { index, price ->
                if (index > 0) Divider(color = DarkBorder.copy(alpha = 0.5f))
                val style = karatStyle(price.karat)
                SummaryRow(
                    col1 = price.karat,
                    col2 = formatINR(price.pricePerGram),
                    col3 = formatINR(price.pricePerTenGram),
                    col4 = formatINR((price.pricePerGram * 1.03).toLong()),
                    karatColor = style.color,
                    bgColor = if (index % 2 == 0) DarkBg.copy(alpha = 0.5f) else Color.Transparent
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    col1: String,
    col2: String,
    col3: String,
    col4: String,
    headerColor: Color = TextPrimary,
    karatColor: Color? = null,
    isHeader: Boolean = false,
    bgColor: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = if (isHeader) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            col1,
            modifier = Modifier.weight(0.7f),
            color = karatColor ?: headerColor,
            fontWeight = if (isHeader) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = if (isHeader) 11.sp else 14.sp
        )
        Text(col2, modifier = Modifier.weight(1f), color = if (isHeader) headerColor else TextPrimary, fontSize = if (isHeader) 11.sp else 13.sp, textAlign = TextAlign.End)
        Text(col3, modifier = Modifier.weight(1f), color = if (isHeader) headerColor else TextPrimary, fontSize = if (isHeader) 11.sp else 13.sp, textAlign = TextAlign.End)
        Text(col4, modifier = Modifier.weight(1f), color = if (isHeader) headerColor else GoldLight, fontSize = if (isHeader) 11.sp else 13.sp, textAlign = TextAlign.End, fontWeight = if (!isHeader) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityBottomSheet(
    cities: List<City>,
    selectedCity: City,
    onCitySelect: (City) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Select City",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GoldBright,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            Divider(color = DarkBorder)
            cities.forEach { city ->
                val isSelected = city.slug == selectedCity.slug
                ListItem(
                    headlineContent = {
                        Text(
                            city.name,
                            color = if (isSelected) GoldBright else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) GoldBright else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected) DarkCard else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onCitySelect(city) }
                )
                Divider(color = DarkBorder.copy(alpha = 0.3f))
            }
        }
    }
}
