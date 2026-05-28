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
        isWhatsappSame.value = data["isWhatsappSame"] as? Boolean ?: true
        whatsappNo.value = data["whatsappNo"]?.toString() ?: ""
        accountType.value = data["accountType"]?.toString() ?: "Customer"
        group.value = data["group"]?.toString() ?: ""
        reference.value = data["reference"]?.toString() ?: ""
        
        bankName.value = data["bankName"]?.toString() ?: ""
        branchName.value = data["branchName"]?.toString() ?: ""
        branchLocation.value = data["branchLocation"]?.toString() ?: ""
        bankAccountNo.value = data["bankAccountNo"]?.toString() ?: ""
        ifscCode.value = data["ifscCode"]?.toString() ?: ""
        micrCode.value = data["micrCode"]?.toString() ?: ""
        swiftCode.value = data["swiftCode"]?.toString() ?: ""
        
        panNo.value = data["panNo"]?.toString() ?: ""
        aadhaarNo.value = data["aadhaarNo"]?.toString() ?: ""
        passportNo.value = data["passportNo"]?.toString() ?: ""
        gstin.value = data["gstin"]?.toString() ?: ""
        voterId.value = data["voterId"]?.toString() ?: ""
        drivingLicense.value = data["drivingLicense"]?.toString() ?: ""
        dob.value = data["dob"]?.toString() ?: ""
        annDate.value = data["annDate"]?.toString() ?: ""
        dueDays.value = data["dueDays"]?.toString() ?: "30"
        
        isMsmeRegistered.value = data["isMsmeRegistered"] as? Boolean ?: false
        msmeCategory.value = data["msmeCategory"]?.toString() ?: "Micro"
        isKyc.value = data["isKyc"] as? Boolean ?: false
        tdsTcsApplicable.value = data["tdsTcsApplicable"]?.toString() ?: "None"
        assesseType.value = data["assesseType"]?.toString() ?: "Individual"
        
        isEntryStarted.value = true
    }

    fun saveAccount(onSuccess: (String, String) -> Unit) {
        if (name.value.isBlank()) return

        val data = mutableMapOf<String, Any>(
            "name" to name.value,
            "alias" to alias.value,
            "contactPerson" to contactPerson.value,
            "address" to address.value,
            "area" to area.value,
            "district" to district.value,
            "city" to city.value,
            "pincode" to pincode.value,
            "state" to state.value,
            "email" to email.value,
            "fax" to fax.value,
            "phone" to phone.value,
            "mobile" to mobile.value,
            "isWhatsappSame" to isWhatsappSame.value,
            "whatsappNo" to whatsappNo.value,
            "accountType" to accountType.value,
            "group" to group.value,
            "reference" to reference.value,
            
            "bankName" to bankName.value,
            "branchName" to branchName.value,
            "branchLocation" to branchLocation.value,
            "bankAccountNo" to bankAccountNo.value,
            "ifscCode" to ifscCode.value,
            "micrCode" to micrCode.value,
            "swiftCode" to swiftCode.value,
            
            "panNo" to panNo.value,
            "aadhaarNo" to aadhaarNo.value,
            "passportNo" to passportNo.value,
            "gstin" to gstin.value,
            "voterId" to voterId.value,
            "drivingLicense" to drivingLicense.value,
            "dob" to dob.value,
            "annDate" to annDate.value,
            "dueDays" to dueDays.value,
            
            "isMsmeRegistered" to isMsmeRegistered.value,
            "msmeCategory" to msmeCategory.value,
            "isKyc" to isKyc.value,
            "tdsTcsApplicable" to tdsTcsApplicable.value,
            "assesseType" to assesseType.value,
            
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
        area.value = "Default"
        district.value = "Default"
        city.value = "MUMBAI"
        pincode.value = ""
        state.value = "MAHARASHTRA"
        email.value = ""
        fax.value = ""
        phone.value = ""
        mobile.value = ""
        isWhatsappSame.value = true
        whatsappNo.value = ""
        accountType.value = "Customer"
        group.value = "Sundry Debtors"
        reference.value = ""
        
        bankName.value = ""
        branchName.value = ""
        branchLocation.value = ""
        bankAccountNo.value = ""
        ifscCode.value = ""
        micrCode.value = ""
        swiftCode.value = ""
        
        panNo.value = ""
        aadhaarNo.value = ""
        passportNo.value = ""
        gstin.value = ""
        voterId.value = ""
        drivingLicense.value = ""
        dob.value = ""
        annDate.value = ""
        dueDays.value = "30"
        
        isMsmeRegistered.value = false
        msmeCategory.value = "Micro"
        isKyc.value = false
        tdsTcsApplicable.value = "None"
        assesseType.value = "Individual"
        
        isEntryStarted.value = false
    }
}
