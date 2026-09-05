package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OnSecondaryGreenContainer
import com.example.ui.theme.SecondaryGreen
import com.example.ui.theme.SecondaryGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// -------------------------------------------------------------------------------------
// TILE 3: Create School Dialog
// -------------------------------------------------------------------------------------
@Composable
fun CreateSchoolDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, mobile: String?, email: String?, address: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_create_school"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "नवीन शाळा नोंदणी (School Registration)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "नवीन शाळेची नोंदणी करा. शाळा UDISE कोड अद्वितीय असणे आवश्यक आहे.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // 1. School Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("शाळेचे नाव (School Name) *") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_name")
                )

                // 2. School UDISE Code (stored in 'code')
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("शाळा UDISE कोड (UDISE Code) *") },
                    leadingIcon = { Icon(Icons.Default.Domain, contentDescription = null) },
                    placeholder = { Text("e.g. 27251401501") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_udise_code")
                        .testTag("input_school_code")
                )

                // 3. Mobile Number
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile Number)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    placeholder = { Text("e.g. 9822012345") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_mobile")
                )

                // 4. E-Mail ID
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ई-मेल आयडी (E-Mail ID)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    placeholder = { Text("e.g. school@educhat.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_email")
                )

                // 5. Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पत्ता (Address)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_address")
                )

                // 6. Active Status Indicator
                Surface(
                    color = SecondaryGreenContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnSecondaryGreenContainer, modifier = Modifier.size(16.dp))
                        Text(
                            text = "नोंदणीनंतर शाळा डीफॉल्टनुसार 'सक्रिय (Active)' राहील.",
                            style = MaterialTheme.typography.bodySmall.copy(color = OnSecondaryGreenContainer)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                name,
                                code,
                                mobile.ifBlank { null },
                                email.ifBlank { null },
                                address.ifBlank { null }
                            )
                        },
                        enabled = !isLoading && name.isNotBlank() && code.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        modifier = Modifier.testTag("btn_submit_school")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("शाळा नोंदवा (Register)")
                        }
                    }
                }
            }
        }
    }
}
