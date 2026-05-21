package com.fms.app.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import com.fms.app.data.UserSession

/**
 * AccountMasterViewModel: High-fidelity controller for Multi-Tenant Account Management.
 * Handles Customers, Vendors, and Internal accounts with strict tenant isolation.
 */
class AccountMasterViewModel : ViewModel() {
    
    // Global State for the Tenant
    val accountList = mutableStateListOf<Pair<String, Map<String, Any>>>()
    var isLoading = mutableStateOf(false)
    var isEntryStarted = mutableStateOf(false)
    var searchQuery = mutableStateOf("")

    // --- Form State (Account Details) ---
    var id = mutableStateOf("0")
    var name = mutableStateOf("")
    var alias = mutableStateOf("")
    var contactPerson = mutableStateOf("")
    var address = mutableStateOf("")
    var area = mutableStateOf("Default")
    var district = mutableStateOf("Default")
    var city = mutableStateOf("MUMBAI")
    var pincode = mutableStateOf("")
    var state = mutableStateOf("MAHARASHTRA")
    var email = mutableStateOf("")
    var fax = mutableStateOf("")
    var phone = mutableStateOf("")
    var mobile = mutableStateOf("")
    var isWhatsappSame = mutableStateOf(true)
    var whatsappNo = mutableStateOf("")
    var accountType = mutableStateOf("Customer")
    var group = mutableStateOf("Sundry Debtors")
    var reference = mutableStateOf("")

    // --- Bank Details ---
    var bankName = mutableStateOf("")
    var branchName = mutableStateOf("")
    var branchLocation = mutableStateOf("")
    var bankAccountNo = mutableStateOf("")
    var ifscCode = mutableStateOf("")
    var micrCode = mutableStateOf("")
    var swiftCode = mutableStateOf("")

    // --- ID Proofs ---
    var panNo = mutableStateOf("")
    var aadhaarNo = mutableStateOf("")
    var passportNo = mutableStateOf("")
    var gstin = mutableStateOf("")
    var voterId = mutableStateOf("")
    var drivingLicense = mutableStateOf("")
    var dob = mutableStateOf("")
    var annDate = mutableStateOf("")
    var dueDays = mutableStateOf("30")

    // --- Statutory Details ---
    var isMsmeRegistered = mutableStateOf(false)
    var msmeCategory = mutableStateOf("Micro")
    var isKyc = mutableStateOf(false)
    var tdsTcsApplicable = mutableStateOf("None")
    var assesseType = mutableStateOf("Individual")

    val accountTypes = listOf("Customer", "Vendor", "Employee", "Partner")
    val assesseTypes = listOf("Individual", "Company", "HUF", "Partnership Firm")
    val tdsOptions = listOf("None", "TDS", "TCS")

    val filteredAccountList = derivedStateOf {
        if (searchQuery.value.isEmpty()) {
            accountList
        } else {
            accountList.filter { acc ->
                val accName = acc.second["name"]?.toString() ?: ""
                val accMobile = acc.second["mobile"]?.toString() ?: ""
                val accCity = acc.second["city"]?.toString() ?: ""
                accName.contains(searchQuery.value, ignoreCase = true) ||
                accMobile.contains(searchQuery.value) ||
                accCity.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        isLoading.value = true
        FirebaseRepository.getModuleDataWithKeys("Accounts") { data ->
            accountList.clear()
            accountList.addAll(data)
            isLoading.value = false
        }
    }

    fun startNewEntry() {
        resetFields()
        isEntryStarted.value = true
    }

    fun selectAccount(accountId: String, data: Map<String, Any>) {
        id.value = accountId
        name.value = data["name"]?.toString() ?: ""
        alias.value = data["alias"]?.toString() ?: ""
        contactPerson.value = data["contactPerson"]?.toString() ?: ""
        address.value = data["address"]?.toString() ?: ""
        area.value = data["area"]?.toString() ?: "Default"
        district.value = data["district"]?.toString() ?: "Default"
        city.value = data["city"]?.toString() ?: ""
        pincode.value = data["pincode"]?.toString() ?: ""
        state.value = data["state"]?.toString() ?: ""
        email.value = data["email"]?.toString() ?: ""
        fax.value = data["fax"]?.toString() ?: ""
        phone.value = data["phone"]?.toString() ?: ""
        mobile.value = data["mobile"]?.toString() ?: ""
        whatsappNo.value = data["whatsappNo"]?.toString() ?: ""
        accountType.value = data["accountType"]?.toString() ?: "Customer"
        group.value = data["group"]?.toString() ?: ""
        
        bankName.value = data["bankName"]?.toString() ?: ""
        bankAccountNo.value = data["bankAccountNo"]?.toString() ?: ""
        gstin.value = data["gstin"]?.toString() ?: ""
        
        isEntryStarted.value = true
    }

    fun saveAccount(onSuccess: (String, String) -> Unit) {
        if (name.value.isBlank()) return

        val data = mapOf(
            "name" to name.value,
            "alias" to alias.value,
            "contactPerson" to contactPerson.value,
            "address" to address.value,
            "city" to city.value,
            "mobile" to mobile.value,
            "whatsappNo" to whatsappNo.value,
            "accountType" to accountType.value,
            "gstin" to gstin.value,
            "bankAccountNo" to bankAccountNo.value,
            "tenantId" to (UserSession.companyId ?: "")
        )

        val targetId = if (id.value == "0") null else id.value
        FirebaseRepository.saveItem("Accounts", targetId, data) { success ->
            if (success) {
                loadAccounts()
                onSuccess(id.value, name.value)
                resetFields()
            }
        }
    }

    fun deleteAccount(accountId: String) {
        FirebaseRepository.deleteItem("Accounts", accountId) { success ->
            if (success) loadAccounts()
        }
    }

    private fun resetFields() {
        id.value = "0"
        name.value = ""
        alias.value = ""
        contactPerson.value = ""
        address.value = ""
        mobile.value = ""
        whatsappNo.value = ""
        isEntryStarted.value = false
    }
}