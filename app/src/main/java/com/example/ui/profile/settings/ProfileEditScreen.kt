package com.example.ui.profile.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.models.User
import com.example.ui.auth.AuthViewModel
import com.example.ui.admin.EditSectionCard
import com.example.ui.admin.ImagePickerSection
import kotlinx.coroutines.launch
import android.net.Uri
import java.util.UUID
import com.example.data.repository.AuraRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(navController: NavController, authViewModel: AuthViewModel) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val user = currentUser ?: User(
        id = "guest_user",
        name = "Guest Student",
        email = "guest@auralearning.com",
        role = "guest"
    )

    // Form states
    var name by remember { mutableStateOf(user.name) }
    var mobileNumber by remember { mutableStateOf(user.mobileNumber) }
    var photoUrl by remember { mutableStateOf(user.photoUrl) }
    var bannerUrl by remember { mutableStateOf(user.bannerUrl) }
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedBannerUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var repository = remember { com.example.data.repository.AuraRepository() }
    
    // Academic details
    var schoolName by remember { mutableStateOf(user.schoolName) }
    var className by remember { mutableStateOf(user.className) }
    var section by remember { mutableStateOf(user.section) }
    var rollNumber by remember { mutableStateOf(user.rollNumber) }
    var admissionNumber by remember { mutableStateOf(user.admissionNumber) }
    var board by remember { mutableStateOf(user.board) }
    var medium by remember { mutableStateOf(user.medium) }
    var academicSession by remember { mutableStateOf(user.academicSession) }

    // Personal details
    var gender by remember { mutableStateOf(user.gender) }
    var dob by remember { mutableStateOf(user.dob) }
    var ageStr by remember { mutableStateOf(if (user.age > 0) user.age.toString() else "") }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup) }
    var nationality by remember { mutableStateOf(user.nationality) }
    var category by remember { mutableStateOf(user.category) }

    // Contact / Parent details
    var parentName by remember { mutableStateOf(user.parentName) }
    var parentMobileNumber by remember { mutableStateOf(user.parentMobileNumber) }

    // Address
    var country by remember { mutableStateOf(user.country) }
    var state by remember { mutableStateOf(user.state) }
    var district by remember { mutableStateOf(user.district) }
    var city by remember { mutableStateOf(user.city) }
    var pinCode by remember { mutableStateOf(user.pinCode) }

    // UI Status states
    var isSaving by remember { mutableStateOf(false) }
    
    // Errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var parentMobileError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }

    fun validateForm(): Boolean {
        var isValid = true
        
        // Name validation
        if (name.trim().isEmpty()) {
            nameError = "Full Name cannot be empty"
            isValid = false
        } else if (name.length > 50) {
            nameError = "Full Name must be under 50 characters"
            isValid = false
        } else {
            nameError = null
        }

        // Mobile validation (must be 10 digits if not empty)
        if (mobileNumber.isNotEmpty() && !mobileNumber.all { it.isDigit() }) {
            mobileError = "Mobile number must contain only digits"
            isValid = false
        } else if (mobileNumber.isNotEmpty() && mobileNumber.length < 10) {
            mobileError = "Mobile number must be at least 10 digits"
            isValid = false
        } else {
            mobileError = null
        }

        // Parent Mobile validation
        if (parentMobileNumber.isNotEmpty() && !parentMobileNumber.all { it.isDigit() }) {
            parentMobileError = "Parent mobile number must contain only digits"
            isValid = false
        } else if (parentMobileNumber.isNotEmpty() && parentMobileNumber.length < 10) {
            parentMobileError = "Parent mobile number must be at least 10 digits"
            isValid = false
        } else {
            parentMobileError = null
        }

        // Pin code validation (must be 6 digits if not empty)
        if (pinCode.isNotEmpty() && (!pinCode.all { it.isDigit() } || pinCode.length != 6)) {
            pinError = "PIN Code must be a 6-digit number"
            isValid = false
        } else {
            pinError = null
        }

        // Age validation
        val ageVal = ageStr.toIntOrNull()
        if (ageStr.isNotEmpty() && (ageVal == null || ageVal <= 0 || ageVal > 120)) {
            ageError = "Please enter a valid age (1-120)"
            isValid = false
        } else {
            ageError = null
        }

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Guide Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Student Account Manager",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Modify your student information below. These details sync instantly with the Aura student records database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 1: PROFILE ASSETS (Photos)
                EditSectionCard(title = "Profile Assets", icon = Icons.Filled.CameraAlt) {
                    ImagePickerSection(
                        title = "Profile Photo",
                        selectedImageUri = selectedPhotoUri,
                        onImageSelected = { selectedPhotoUri = it },
                        existingImageUrl = photoUrl
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ImagePickerSection(
                        title = "Profile Banner",
                        selectedImageUri = selectedBannerUri,
                        onImageSelected = { selectedBannerUri = it },
                        existingImageUrl = bannerUrl
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: ACADEMIC DETAILS
                EditSectionCard(title = "Academic Credentials", icon = Icons.Filled.School) {
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("School / College Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = className,
                            onValueChange = { className = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Class") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { section = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Section") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = rollNumber,
                            onValueChange = { rollNumber = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Roll Number") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = admissionNumber,
                            onValueChange = { admissionNumber = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Admission Number") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = board,
                            onValueChange = { board = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Board") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = medium,
                            onValueChange = { medium = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Medium") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = academicSession,
                        onValueChange = { academicSession = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Academic Session") },
                        placeholder = { Text("e.g. 2026-2027") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: PERSONAL DETAILS
                EditSectionCard(title = "Personal Profiles", icon = Icons.Filled.Person) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name *") },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Gender") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("DOB (DD-MM-YYYY)") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = ageError != null,
                            supportingText = { ageError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) } }
                        )
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Blood Group") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = nationality,
                            onValueChange = { nationality = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nationality") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Category (Optional)") },
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 4: CONTACT & GUARDIAN DETAILS
                EditSectionCard(title = "Contact & Guardians", icon = Icons.Filled.ContactPhone) {
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Student Mobile") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = mobileError != null,
                        supportingText = { mobileError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Parent / Guardian Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.FamilyRestroom, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = parentMobileNumber,
                        onValueChange = { parentMobileNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Parent Mobile") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = parentMobileError != null,
                        supportingText = { parentMobileError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Filled.FamilyRestroom, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 5: ADDRESS
                EditSectionCard(title = "Permanent Address", icon = Icons.Filled.Place) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Country") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("State") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("District") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("City") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("PIN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = { pinError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // SAVE & CANCEL BUTTONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Button(
                        onClick = {
                            if (validateForm()) {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        var finalPhotoUrl = photoUrl
                                        var finalBannerUrl = bannerUrl
                                        
                                        if (selectedPhotoUri != null) {
                                            val bytes = com.example.utils.StorageUtils.compressImage(context, selectedPhotoUri!!)
                                            if (bytes != null) {
                                                val fileName = "avatar_${user.id}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
                                                val uploadedUrl = repository.uploadImage(bytes, fileName, "avatars")
                                                if (uploadedUrl.isNotEmpty()) finalPhotoUrl = uploadedUrl
                                            }
                                        }
                                        
                                        if (selectedBannerUri != null) {
                                            val bytes = com.example.utils.StorageUtils.compressImage(context, selectedBannerUri!!)
                                            if (bytes != null) {
                                                val fileName = "banner_${user.id}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
                                                val uploadedUrl = repository.uploadImage(bytes, fileName, "banners")
                                                if (uploadedUrl.isNotEmpty()) finalBannerUrl = uploadedUrl
                                            }
                                        }

                                        val ageVal = ageStr.toIntOrNull() ?: 0
                                        val updatedUser = user.copy(
                                            name = name.trim(),
                                            mobileNumber = mobileNumber.trim(),
                                            photoUrl = finalPhotoUrl,
                                            bannerUrl = finalBannerUrl,
                                            schoolName = schoolName.trim(),
                                            className = className.trim(),
                                            section = section.trim(),
                                            rollNumber = rollNumber.trim(),
                                            admissionNumber = admissionNumber.trim(),
                                            board = board.trim(),
                                            medium = medium.trim(),
                                            academicSession = academicSession.trim(),
                                            gender = gender.trim(),
                                            dob = dob.trim(),
                                            age = ageVal,
                                            bloodGroup = bloodGroup.trim(),
                                            nationality = nationality.trim(),
                                            category = category.trim(),
                                            parentName = parentName.trim(),
                                            parentMobileNumber = parentMobileNumber.trim(),
                                            country = country.trim(),
                                            state = state.trim(),
                                            district = district.trim(),
                                            city = city.trim(),
                                            pinCode = pinCode.trim()
                                        )
                                        
                                        authViewModel.updateUserProfile(
                                            updatedUser = updatedUser,
                                            onSuccess = {
                                                isSaving = false
                                                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            },
                                            onError = { error ->
                                                isSaving = false
                                                Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isSaving = false
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please fix the validation errors", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Save Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Global Full screen block input saving spinner
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Syncing changes with Supabase...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}
