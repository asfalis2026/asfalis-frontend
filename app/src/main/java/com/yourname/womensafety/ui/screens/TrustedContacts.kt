package com.yourname.womensafety.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.data.SecurityPolicyManager
import com.yourname.womensafety.data.network.dto.TrustedContact
import com.yourname.womensafety.ui.components.SecureScreen
import com.yourname.womensafety.ui.viewmodels.ContactsViewModel
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable

// ─── Country dial-code support ───────────────────────────────────────────────

data class CountryDialCode(val name: String, val dialCode: String, val isoCode: String)

/** Converts a 2-letter ISO code to its flag emoji (Unicode regional indicators). */
private fun isoToFlag(iso: String): String = iso.uppercase()
    .map { char -> String(Character.toChars(0x1F1E6 + (char.code - 'A'.code))) }
    .joinToString("")

val CountryDialCode.flag: String get() = isoToFlag(isoCode)

val ALL_COUNTRIES: List<CountryDialCode> = listOf(
    CountryDialCode("India", "+91", "IN"),
    CountryDialCode("United States", "+1", "US"),
    CountryDialCode("United Kingdom", "+44", "GB"),
    CountryDialCode("Canada", "+1", "CA"),
    CountryDialCode("Australia", "+61", "AU"),
    CountryDialCode("Germany", "+49", "DE"),
    CountryDialCode("France", "+33", "FR"),
    CountryDialCode("Italy", "+39", "IT"),
    CountryDialCode("Spain", "+34", "ES"),
    CountryDialCode("Netherlands", "+31", "NL"),
    CountryDialCode("Sweden", "+46", "SE"),
    CountryDialCode("Norway", "+47", "NO"),
    CountryDialCode("Denmark", "+45", "DK"),
    CountryDialCode("Finland", "+358", "FI"),
    CountryDialCode("Switzerland", "+41", "CH"),
    CountryDialCode("Austria", "+43", "AT"),
    CountryDialCode("Belgium", "+32", "BE"),
    CountryDialCode("Portugal", "+351", "PT"),
    CountryDialCode("Russia", "+7", "RU"),
    CountryDialCode("China", "+86", "CN"),
    CountryDialCode("Japan", "+81", "JP"),
    CountryDialCode("South Korea", "+82", "KR"),
    CountryDialCode("Singapore", "+65", "SG"),
    CountryDialCode("Malaysia", "+60", "MY"),
    CountryDialCode("Indonesia", "+62", "ID"),
    CountryDialCode("Thailand", "+66", "TH"),
    CountryDialCode("Philippines", "+63", "PH"),
    CountryDialCode("Vietnam", "+84", "VN"),
    CountryDialCode("Bangladesh", "+880", "BD"),
    CountryDialCode("Pakistan", "+92", "PK"),
    CountryDialCode("Sri Lanka", "+94", "LK"),
    CountryDialCode("Nepal", "+977", "NP"),
    CountryDialCode("Bhutan", "+975", "BT"),
    CountryDialCode("Maldives", "+960", "MV"),
    CountryDialCode("Myanmar", "+95", "MM"),
    CountryDialCode("UAE", "+971", "AE"),
    CountryDialCode("Saudi Arabia", "+966", "SA"),
    CountryDialCode("Qatar", "+974", "QA"),
    CountryDialCode("Kuwait", "+965", "KW"),
    CountryDialCode("Bahrain", "+973", "BH"),
    CountryDialCode("Oman", "+968", "OM"),
    CountryDialCode("Israel", "+972", "IL"),
    CountryDialCode("Turkey", "+90", "TR"),
    CountryDialCode("South Africa", "+27", "ZA"),
    CountryDialCode("Nigeria", "+234", "NG"),
    CountryDialCode("Kenya", "+254", "KE"),
    CountryDialCode("Ghana", "+233", "GH"),
    CountryDialCode("Egypt", "+20", "EG"),
    CountryDialCode("Brazil", "+55", "BR"),
    CountryDialCode("Mexico", "+52", "MX"),
    CountryDialCode("Argentina", "+54", "AR"),
    CountryDialCode("Colombia", "+57", "CO"),
    CountryDialCode("Chile", "+56", "CL"),
    CountryDialCode("Peru", "+51", "PE"),
    CountryDialCode("New Zealand", "+64", "NZ"),
    CountryDialCode("Ireland", "+353", "IE"),
    CountryDialCode("Poland", "+48", "PL"),
    CountryDialCode("Ukraine", "+380", "UA"),
    CountryDialCode("Greece", "+30", "GR"),
    CountryDialCode("Czech Republic", "+420", "CZ"),
    CountryDialCode("Hungary", "+36", "HU"),
    CountryDialCode("Romania", "+40", "RO"),
)

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrustedContactsScreen(navController: NavController) {
    val securityPolicy by SecurityPolicyManager.state.collectAsState()
    SecureScreen(
        enabled = securityPolicy.screenshotProtectionEnabled &&
            "trusted_contacts" in securityPolicy.protectedScreens
    )

    val contactsViewModel: ContactsViewModel = viewModel(
        factory = ContactsViewModel.Factory
    )
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by contactsViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by contactsViewModel.errorMessage.collectAsStateWithLifecycle()
    val pendingOtpVerification by contactsViewModel.pendingOtpVerification.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate to OTP verification screen when contact is added
    LaunchedEffect(pendingOtpVerification) {
        Log.d("TrustedContacts", "LaunchedEffect triggered: pendingOtpVerification=$pendingOtpVerification")
        pendingOtpVerification?.let { otpData ->
            Log.d("TrustedContacts", "Navigating to OTP screen: contactId=${otpData.contactId}, phone=${otpData.phone}")
            navController.navigate(
                "contact_otp_verification/${otpData.contactId}/${otpData.phone}/${otpData.name}/${otpData.expiresInSeconds}"
            )
            contactsViewModel.clearOtpVerification()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<com.yourname.womensafety.data.network.dto.TrustedContact?>(null) }
    var contactToDelete by remember { mutableStateOf<com.yourname.womensafety.data.network.dto.TrustedContact?>(null) }
    var selectedCountry by remember { mutableStateOf(ALL_COUNTRIES.first()) }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var relationInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    // Validation error messages (null = no error)
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        contactsViewModel.loadContacts()
    }

    val emergencyContacts = contacts.filter { it.isPrimary }
    val otherContacts = contacts.filter { !it.isPrimary }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
                )
            )
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.yourname.womensafety.R.drawable.background_image),
            contentDescription = null,
            alpha = 0.35f,
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        )
        // Overlay to blend the image seamlessly into the background
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color.Transparent, Color(0xFF120508)),
                    startY = 0f
                )
            )
        )
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                val isLimitReached = contacts.size >= 3
                FloatingActionButton(
                    onClick = { 
                        if (isLimitReached) {
                            android.widget.Toast.makeText(context, "Maximum 3 contacts allowed".trNonComposable(), android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            showAddDialog = true 
                        }
                    },
                    containerColor = if (isLimitReached) Color.Gray else Color(0xFFE25F71),
                    contentColor = if (isLimitReached) Color.DarkGray else Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact".tr())
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)).border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Trusted Contacts".tr(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Text(text = "Emergency alerts & live location recipients".tr(),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.05f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.History, null, tint = Color(0xFFE25F71), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${contacts.size} " + "Active Contacts".tr(), color = Color.White.copy(0.8f), fontSize = 12.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.05f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("${emergencyContacts.size} " + "Primary".tr(), color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFE25F71)))
                        Spacer(Modifier.width(6.dp))
                        Text("${otherContacts.size} " + "Secondary".tr(), color = Color.Gray, fontSize = 12.sp)
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color(0xFFE25F71), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE25F71), modifier = Modifier.size(32.dp))
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (emergencyContacts.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalPolice, null, tint = Color(0xFFE25F71), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("PRIMARY GUARDIAN".tr(), color = Color(0xFFE25F71), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.width(16.dp))
                                Box(modifier = Modifier.height(1.dp).weight(1f).background(Color(0xFFE25F71).copy(0.4f)))
                            }
                        }
                        items(emergencyContacts, key = { it.id }) { contact ->
                            ContactApiItem(
                                contact = contact,
                                isEmergency = true,
                                onEditRequest = { contactToEdit = contact },
                                onDeleteRequest = { contactToDelete = contact },
                                onSetPrimary = { contactsViewModel.setPrimaryContact(contact.id) }
                            )
                        }
                    }

                    if (otherContacts.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                                Icon(Icons.Default.Group, null, tint = Color(0xFFE25F71), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ADDITIONAL CONTACTS".tr(), color = Color(0xFFE25F71), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.width(16.dp))
                                Box(modifier = Modifier.height(1.dp).weight(1f).background(Color(0xFFE25F71).copy(0.4f)))
                            }
                        }
                        items(otherContacts, key = { it.id }) { contact ->
                            ContactApiItem(
                                contact = contact,
                                isEmergency = false,
                                onEditRequest = { contactToEdit = contact },
                                onDeleteRequest = { contactToDelete = contact },
                                onSetPrimary = { contactsViewModel.setPrimaryContact(contact.id) }
                            )
                        }
                    }

                    if (!isLoading && contacts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No contacts yet. Add your first trusted contact.".tr(), color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- Delete Contact Confirmation Dialog ---
        contactToDelete?.let { contact ->
            AlertDialog(
                onDismissRequest = { contactToDelete = null },
                containerColor = Color(0xFF160E0E),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color(0xFFE25F71).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonRemove, null, tint = Color(0xFFE25F71), modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text("Remove Contact?".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    val textTemplate = "Remove [NAME] from your trusted contacts? They will no longer receive SOS alerts.".tr()
                    val replacedText = textTemplate.replace("[NAME]", "${contact.name} (${contact.phone})")
                    Text(replacedText,
                        color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                },
                dismissButton = {
                    TextButton(onClick = { contactToDelete = null }) {
                        Text("Cancel".tr(), color = Color.White.copy(0.7f))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            contactsViewModel.deleteContact(contact.id)
                            contactToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- Edit Contact Dialog ---
        contactToEdit?.let { contact ->
            var editNameInput by remember { mutableStateOf(contact.name) }
            var editRelationInput by remember { mutableStateOf(contact.relationship ?: "") }
            var editEmailInput by remember { mutableStateOf(contact.email ?: "") }
            var editNameError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { contactToEdit = null }) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF160E0E))
                        .padding(24.dp)
                ) {
                    Text("Edit Contact".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))

                        // --- Name ---
                        OutlinedTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it; editNameError = null },
                            label = { Text("Full Name *".tr(), color = Color.Gray) },
                            isError = editNameError != null,
                            supportingText = editNameError?.let { { Text(it, color = Color(0xFFE25F71), fontSize = 11.sp) } },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        // --- Relationship ---
                        OutlinedTextField(
                            value = editRelationInput,
                            onValueChange = { editRelationInput = it },
                            label = { Text("Relationship (optional)".tr(), color = Color.Gray) },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        // --- Email (optional) ---
                        OutlinedTextField(
                            value = editEmailInput,
                            onValueChange = { editEmailInput = it },
                            label = { Text("Email (optional)".tr(), color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { contactToEdit = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                            ) { Text("Cancel".tr(), color = Color.White) }

                            Button(
                                onClick = {
                                    if (editNameInput.isBlank()) {
                                        editNameError = "Name is required"
                                    } else {
                                        contactsViewModel.updateContact(
                                            contactId    = contact.id,
                                            name         = editNameInput.trim(),
                                            phone        = contact.phone,
                                            relationship = editRelationInput.trim().ifEmpty { null },
                                            email        = editEmailInput.trim().ifEmpty { null }
                                        )
                                        contactToEdit = null
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                            ) { Text("Save".tr(), color = Color.White) }
                        }
                    }
            }
        }

        // --- Add Contact Dialog ---
        if (showAddDialog) {
            // Validates the local part only (digits, optionally starting with 0, 6–15 digits)
            val localPhoneRegex = remember { Regex("^[0-9]{6,14}$") }
            var countryExpanded by remember { mutableStateOf(false) }
            var countrySearch by remember { mutableStateOf("") }

            fun validate(): Boolean {
                var valid = true
                nameError = if (nameInput.isBlank()) {
                    valid = false; "Name is required"
                } else null
                phoneError = if (phoneInput.isBlank()) {
                    valid = false; "Phone number is required"
                } else if (!localPhoneRegex.matches(phoneInput.trim())) {
                    valid = false; "Enter digits only, e.g. 9876543210"
                } else null
                return valid
            }

            fun resetDialog() {
                showAddDialog = false
                selectedCountry = ALL_COUNTRIES.first()
                nameInput = ""; phoneInput = ""; relationInput = ""; emailInput = ""
                nameError = null; phoneError = null
                countrySearch = ""
            }

            Dialog(onDismissRequest = { resetDialog() }) {
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF160E0E))
                            .padding(24.dp)
                    ) {
                        Text("Add Contact".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(20.dp))

                        // --- Name ---
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it; nameError = null },
                            label = { Text("Full Name *".tr(), color = Color.Gray) },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it, color = Color(0xFFE25F71), fontSize = 11.sp) } },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        // --- Phone: country dropdown + local number ---
                        Text("Phone Number *".tr(), color = Color.Gray, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Country code button
                            Box {
                                OutlinedButton(
                                    onClick = { countryExpanded = true; countrySearch = "" },
                                    modifier = Modifier.height(56.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(0.05f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (phoneError != null) Color(0xFFE25F71) else Color.White.copy(0.2f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(text = "${selectedCountry.flag} ${selectedCountry.dialCode}".tr(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown, null,
                                        tint = Color.Gray, modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Country picker dropdown
                                if (countryExpanded) {
                                    Dialog(onDismissRequest = { countryExpanded = false }) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.85f)
                                                    .heightIn(max = 480.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color(0xFF160E0E))
                                                    .padding(16.dp)
                                            ) {
                                                Text("Select Country".tr(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(Modifier.height(12.dp))
                                                OutlinedTextField(
                                                    value = countrySearch,
                                                    onValueChange = { countrySearch = it },
                                                    placeholder = { Text("Search country...".tr(), color = Color.Gray) },
                                                    singleLine = true,
                                                    colors = outlinedFieldColors(),
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Search, null,
                                                            tint = Color.Gray, modifier = Modifier.size(18.dp))
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                val filtered = ALL_COUNTRIES.filter {
                                                    it.name.tr().contains(countrySearch, ignoreCase = true) ||
                                                    it.dialCode.contains(countrySearch)
                                                }
                                                LazyColumn(
                                                    state = rememberLazyListState(),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    items(filtered, key = { it.isoCode }) { country ->
                                                        val isSelected = country.isoCode == selectedCountry.isoCode
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(
                                                                    if (isSelected) Color(0xFFE25F71).copy(0.15f)
                                                                    else Color.Transparent
                                                                )
                                                                .then(Modifier.clickableNoRipple {
                                                                    selectedCountry = country
                                                                    countryExpanded = false
                                                                    phoneError = null
                                                                })
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Text(country.flag, fontSize = 22.sp)
                                                            Text(
                                                                country.name.tr(),
                                                                color = if (isSelected) Color(0xFFE25F71) else Color.White,
                                                                fontSize = 14.sp,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                country.dialCode,
                                                                color = Color.Gray,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                    }
                                }
                            }

                            // Local number field
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it.filter { c -> c.isDigit() }; phoneError = null },
                                placeholder = { Text("9876543210".tr(), color = Color.Gray.copy(0.5f), fontSize = 13.sp) },
                                isError = phoneError != null,
                                supportingText = phoneError?.let { { Text(it, color = Color(0xFFE25F71), fontSize = 11.sp) } },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = outlinedFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        // --- Relationship ---
                        OutlinedTextField(
                            value = relationInput,
                            onValueChange = { relationInput = it },
                            label = { Text("Relationship (optional)".tr(), color = Color.Gray) },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        // --- Email (optional) ---
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email (optional)".tr(), color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { resetDialog() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                            ) { Text("Cancel".tr(), color = Color.White) }

                            Button(
                                onClick = {
                                    if (validate()) {
                                        val fullPhone = selectedCountry.dialCode + phoneInput.trim()
                                        contactsViewModel.addContact(
                                            name         = nameInput.trim(),
                                            phone        = fullPhone,
                                            relationship = relationInput.trim().ifEmpty { null },
                                            email        = emailInput.trim().ifEmpty { null }
                                        )
                                        resetDialog()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                            ) { Text("Add".tr(), color = Color.White) }
                        }
                    }
            }
        }
    }
}

/** Clickable modifier without ripple — used for country list rows. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

// ─── Invite Dialog — shown after successfully adding a contact ────────────────
@Composable
fun InviteContactDialog(
    contact: TrustedContact,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var userName by remember { mutableStateOf("A user") }
    val userRepository = com.yourname.womensafety.data.AppServiceLocator.userRepository
    LaunchedEffect(Unit) {
        val res = userRepository.getProfile()
        if (res is com.yourname.womensafety.data.repository.NetworkResult.Success) {
            userName = res.data?.fullName ?: "A user"
        } else {
            val name = com.yourname.womensafety.data.local.AppCache(context).getUserName()
            if (!name.isNullOrBlank()) {
                userName = name
            }
        }
    }

    val inviteText = buildString {
        appendLine(contact.inviteMessage ?: "$userName added you as a trusted contact in Asfalis.")
        contact.whatsappJoinInfo?.let { wa ->
            appendLine()
            appendLine("To receive WhatsApp emergency alerts, save ${wa.twilioNumber} and send: \"${wa.sandboxCode}\"")
            appendLine("Or tap: ${wa.whatsappLink}")
        }
    }.trimEnd()

    Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF160E0E))
                    .padding(24.dp)
            ) {
                Text("Notify ".tr() + contact.name + "?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("Send an invite so they can receive WhatsApp emergency alerts.".tr(),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Send SMS
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${contact.phone}")
                                putExtra("sms_body", inviteText)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Send SMS".tr(), color = Color.White, fontSize = 13.sp) }

                    // Share
                    Button(
                        onClick = {
                            val shareIntent = Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, inviteText)
                                },
                                "Invite ${contact.name}"
                            )
                            context.startActivity(shareIntent)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Share".tr(), color = Color.White, fontSize = 13.sp) }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip".tr(), color = Color.Gray, fontSize = 14.sp)
                }
            }
    }
}

/** Shared color scheme for dialog OutlinedTextFields. */
@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.05f),
    focusedBorderColor = Color(0xFFE25F71), unfocusedBorderColor = Color.White.copy(0.2f),
    errorBorderColor = Color(0xFFE25F71), errorTextColor = Color.White,
    errorContainerColor = Color(0xFFE25F71).copy(0.05f)
)

@Composable
fun ContactApiItem(
    contact: TrustedContact,
    isEmergency: Boolean,
    onEditRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSetPrimary: () -> Unit
) {
    val initials = contact.name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
        
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = if (isEmergency) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71)) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isEmergency) Brush.verticalGradient(listOf(Color(0xFF280808), Color(0xFF140000)))
                    else Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A)))
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isEmergency) Color.Transparent else Color.White.copy(0.1f))
                            .border(1.dp, if (isEmergency) Color(0xFFE25F71) else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = if (isEmergency) Color(0xFFFF5555) else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isEmergency) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE25F71)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(Modifier.width(20.dp))

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Name
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    // Relationship
                    if (!contact.relationship.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = contact.relationship,
                                color = Color.White.copy(0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    // Phone
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Phone, null, tint = if (isEmergency) Color(0xFFE25F71) else Color.Gray, modifier = Modifier.size(14.dp))
                        Text(contact.phone, color = Color.Gray, fontSize = 13.sp)
                    }

                    // Email
                    if (!contact.email.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Email, null, tint = if (isEmergency) Color(0xFFE25F71) else Color.Gray, modifier = Modifier.size(14.dp))
                            Text(contact.email, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                        }
                    }

                    // Status Badge
                    val isVerified = contact.isVerified
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (isVerified) Icons.Default.CheckCircleOutline else Icons.Default.WarningAmber, 
                            null, 
                            tint = if (isVerified) Color(0xFF4CAF50) else Color(0xFFFFC107), 
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isVerified) "VERIFIED" else "PENDING",
                            color = if (isVerified) Color(0xFF4CAF50) else Color(0xFFFFC107),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onSetPrimary, modifier = Modifier.size(24.dp)) {
                    if (isEmergency) {
                        Icon(Icons.Default.Star, "Primary", tint = Color(0xFFE25F71), modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.StarBorder, "Set Primary", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call
                Row(
                    modifier = Modifier.weight(1f).clickable { 
                        try {
                            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phone}"))
                            context.startActivity(callIntent)
                        } catch (e: SecurityException) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                            context.startActivity(dialIntent)
                        }
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Phone, null, tint = if (isEmergency) Color(0xFFE25F71) else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call".tr(), color = Color.Gray, fontSize = 13.sp)
                }
                
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.05f)))
                
                // Edit
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onEditRequest).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = if (isEmergency) Color(0xFFE25F71) else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit".tr(), color = Color.Gray, fontSize = 13.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.05f)))

                // Remove
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onDeleteRequest).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = if (isEmergency) Color(0xFFE25F71) else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove".tr(), color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

