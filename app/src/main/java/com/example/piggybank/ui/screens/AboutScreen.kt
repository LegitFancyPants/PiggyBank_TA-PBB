package com.example.piggybank.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.ui.theme.BackgroundCanvas
import com.example.piggybank.ui.theme.CardBorder
import com.example.piggybank.ui.theme.CardSurface
import com.example.piggybank.ui.theme.IncomeGreen
import com.example.piggybank.ui.theme.MintSurfaceTint
import com.example.piggybank.ui.theme.PiggyBankTheme
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.SoftMint
import com.example.piggybank.ui.theme.TextPrimary
import com.example.piggybank.ui.theme.TextSecondary

// =========================================================================
// BIODATA PRAKTIKAN
// =========================================================================
private const val DEVELOPER_NAME = "Nugroho Ghathfaan Rajendra"
private const val DEVELOPER_NIM = "21120124130048"
private const val DEVELOPER_GROUP = "Kelompok 09 - Praktikum PPB 2026"

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Profil & Tentang",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Informasi akun lokal & pengembang aplikasi",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Card 1 — Local User Profile Card (Single-User Mode)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SoftMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Avatar Pengguna PiggyBank",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengguna PiggyBank",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MintSurfaceTint,
                            border = BorderStroke(1.dp, SoftMint)
                        ) {
                            Text(
                                text = "Mode Lokal • Tanpa Login",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryTeal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = CardBorder
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mata Uang Utama",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IDR (Rupiah)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Penyimpanan",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cloud REST API",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }

        // Card 2 — About Application & Developer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Tentang Aplikasi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PiggyBank v1.0.0",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aplikasi tracking arus kas harian dan manajemen target tabungan pribadi berbasis Android Native (Jetpack Compose) dan RESTful API.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = CardBorder
                )

                Text(
                    text = "Dikembangkan Oleh",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AboutMetadataRow(
                        label = "Nama",
                        value = DEVELOPER_NAME
                    )
                    AboutMetadataRow(
                        label = "NIM",
                        value = DEVELOPER_NIM
                    )
                    AboutMetadataRow(
                        label = "Kelompok",
                        value = DEVELOPER_GROUP
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Tech Stack",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Kotlin, Jetpack Compose, Express.js, Supabase",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutMetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AboutScreenPreview() {
    PiggyBankTheme {
        AboutScreen()
    }
}
