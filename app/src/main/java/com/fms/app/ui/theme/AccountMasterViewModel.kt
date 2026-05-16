package com.fms.app.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class AccountMasterViewModel : ViewModel() {
    val accountList = mutableStateListOf<Pair<String, Map<String, Any?>>>()

    var isEntryStarted = mutableStateOf(false)

    // Selection/Filter state for specific module context (e.g., Inward)
    var accountTypeFilter = mutableStateOf<String?>(null)

    // --- Tab 1: Account detail ---
    var id = mutableStateOf("0")
    var name = mutableStateOf("")
    var alias = mutableStateOf("")
    var contactPerson = mutableStateOf("")
    var address = mutableStateOf("")
    var area = mutableStateOf("")
    var district = mutableStateOf("")
    var city = mutableStateOf("MUMBAI")
    var pincode = mutableStateOf("")
    var state = mutableStateOf("MAHARASHTRA")
    var country = mutableStateOf("INDIA")
    var email = mutableStateOf("")
    var fax = mutableStateOf("")
    var phone = mutableStateOf("")
    var mobile = mutableStateOf("")
    var whatsappNo = mutableStateOf("")
    var isWhatsappSame = mutableStateOf(false)
    var group = mutableStateOf("")
    var reference = mutableStateOf("")
    
    // --- Tab 2: Supplier/Account details ---
    var gstRegistrationType = mutableStateOf("Registered")
    var defaultCurrency = mutableStateOf("INR")
    var supplierFrequency = mutableStateOf("Monthly")

    // --- Tab 3: Bank details ---
    var bankName = mutableStateOf("")
    var branchName = mutableStateOf("")
    var branchLocation = mutableStateOf("")
    var bankAccountNo = mutableStateOf("")
    var ifscCode = mutableStateOf("")
    var micrCode = mutableStateOf("")
    var swiftCode = mutableStateOf("")

    // --- Tab 4: ID proofs ---
    var panNo = mutableStateOf("")
    var aadhaarNo = mutableStateOf("")
    var passportNo = mutableStateOf("")
    var gstin = mutableStateOf("")
    var voterId = mutableStateOf("")
    var drivingLicense = mutableStateOf("")
    var dob = mutableStateOf("")
    var annDate = mutableStateOf("")
    var dueDays = mutableStateOf("0")

    // --- Tab 5: Statutory details ---
    var isMsmeRegistered = mutableStateOf(false)
    var msmeCategory = mutableStateOf("")
    var isKyc = mutableStateOf(false)
    var tdsTcsApplicable = mutableStateOf("N/A")
    var assesseType = mutableStateOf("Registered")

    var accountType = mutableStateOf("Customer")
    var searchQuery = mutableStateOf("")

    val accountTypes = listOf("Supplier(vendor)", "Customer", "Income", "Expense", "Others")
    val assesseTypes = listOf("Registered", "Unregistered", "Composite", "Exempt")
    val tdsOptions = listOf("N/A", "TDS", "TCS")

    val filteredAccountList = derivedStateOf {
        accountList.filter { account ->
            val type = account.second["accountType"]?.toString() ?: ""
            // Flexible matching for "Supplier" or "Supplier(vendor)"
            val matchesFilter = accountTypeFilter.value == null || 
                type.contains(accountTypeFilter.value!!, ignoreCase = true) ||
                (accountTypeFilter.value == "Supplier" && type.contains("Vendor", ignoreCase = true))
            
            if (!matchesFilter) return@filter false
            
            if (searchQuery.value.isEmpty()) return@filter true
            
            val accName = account.second["name"]?.toString() ?: ""
            val accMobile = account.second["mobile"]?.toString() ?: ""
            val accCity = account.second["city"]?.toString() ?: ""
            val accAlias = account.second["alias"]?.toString() ?: ""
            val accContact = account.second["contactPerson"]?.toString() ?: ""
            val accGstin = account.second["gstin"]?.toString() ?: ""
            
            accName.contains(searchQuery.value, ignoreCase = true) ||
                    accMobile.contains(searchQuery.value, ignoreCase = true) ||
                    accCity.contains(searchQuery.value, ignoreCase = true) ||
                    accAlias.contains(searchQuery.value, ignoreCase = true) ||
                    accContact.contains(searchQuery.value, ignoreCase = true) ||
                    accGstin.contains(searchQuery.value, ignoreCase = true)
        }
    }

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        FirebaseRepository.getModuleData("Accounts") { data ->
            accountList.clear()
            accountList.addAll(data)
        }
    }

    fun startNewEntry() {
        resetFields()
        // If we have a filter (like "Supplier"), set it as the default type for the new entry
        accountTypeFilter.value?.let { filter ->
            if (filter.contains("Supplier", ignoreCase = true)) {
                accountType.value = "Supplier(vendor)"
            } else if (filter.contains("Customer", ignoreCase = true)) {
                accountType.value = "Customer"
            }
        }
        isEntryStarted.value = true
    }

    fun selectAccount(accountId: String, data: Map<String, Any?>) {
        id.value = accountId
        name.value = data["name"]?.toString() ?: ""
        alias.value = data["alias"]?.toString() ?: ""
        accountType.value = data["accountType"]?.toString() ?: "Customer"
        contactPerson.value = data["contactPerson"]?.toString() ?: ""
        address.value = data["address"]?.toString() ?: ""
        area.value = data["area"]?.toString() ?: ""
        district.value = data["district"]?.toString() ?: ""
        city.value = data["city"]?.toString() ?: "MUMBAI"
        pincode.value = data["pincode"]?.toString() ?: ""
        state.value = data["state"]?.toString() ?: "MAHARASHTRA"
        country.value = data["country"]?.toString() ?: "INDIA"
        email.value = data["email"]?.toString() ?: ""
        fax.value = data["fax"]?.toString() ?: ""
        phone.value = data["phone"]?.toString() ?: ""
        mobile.value = data["mobile"]?.toString() ?: ""
        whatsappNo.value = data["whatsappNo"]?.toString() ?: ""
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
        dueDays.value = data["dueDays"]?.toString() ?: "0"
        isMsmeRegistered.value = data["isMsmeRegistered"] as? Boolean ?: false
        msmeCategory.value = data["msmeCategory"]?.toString() ?: ""
        isKyc.value = data["isKyc"] as? Boolean ?: false
        tdsTcsApplicable.value = data["tdsTcsApplicable"]?.toString() ?: "N/A"
        assesseType.value = data["assesseType"]?.toString() ?: "Registered"
        
        isEntryStarted.value = true
    }

    fun saveAccount(onSuccess: (String, String) -> Unit) {
        if (name.value.isEmpty()) return

        val accountName = name.value
        val data = mapOf(
            "name" to name.value,
            "alias" to alias.value,
            "accountType" to accountType.value,
            "contactPerson" to contactPerson.value,
            "address" to address.value,
            "area" to area.value,
            "district" to district.value,
            "city" to city.value,
            "pincode" to pincode.value,
            "state" to state.value,
            "country" to country.value,
            "email" to email.value,
            "fax" to fax.value,
            "phone" to phone.value,
            "mobile" to mobile.value,
            "whatsappNo" to whatsappNo.value,
            "group" to group.value,
            "reference" to reference.value,
            "bankName" to bankName.value,
            "branchName" to branchName.value,
            "branchLocation" to branchLocation.value,
            "bankAccountNo" to bankAccountNo.value,
            "ifscCode" to ifscCode.value,
            "micrCode" to micrCode.value,
            "swiftCode" to swiftCode.value,
            "panNo" to panNo.value.uppercase(),
            "aadhaarNo" to aadhaarNo.value,
            "passportNo" to passportNo.value,
            "gstin" to gstin.value.uppercase(),
            "voterId" to voterId.value,
            "drivingLicense" to drivingLicense.value,
            "dob" to dob.value,
            "annDate" to annDate.value,
            "dueDays" to dueDays.value,
            "isMsmeRegistered" to isMsmeRegistered.value,
            "msmeCategory" to msmeCategory.value,
            "isKyc" to isKyc.value,
            "tdsTcsApplicable" to tdsTcsApplicable.value,
            "assesseType" to assesseType.value
        )

        val accountId = if (id.value == "0") null else id.value
        val savedId = FirebaseRepository.saveItem("Accounts", accountId, data)
        
        onSuccess(savedId, accountName)
        resetFields()
    }

    fun resetFields() {
        id.value = "0"
        name.value = ""
        alias.value = ""
        contactPerson.value = ""
        address.value = ""
        area.value = ""; district.value = ""
        city.value = "MUMBAI"; pincode.value = ""; state.value = "MAHARASHTRA"; country.value = "INDIA"
        email.value = ""; fax.value = ""; phone.value = ""; mobile.value = ""; whatsappNo.value = ""
        isWhatsappSame.value = false
        group.value = ""; reference.value = ""
        
        bankName.value = ""; branchName.value = ""; branchLocation.value = ""
        bankAccountNo.value = ""; ifscCode.value = ""; micrCode.value = ""; swiftCode.value = ""
        panNo.value = ""; aadhaarNo.value = ""; passportNo.value = ""; gstin.value = ""
        voterId.value = ""; drivingLicense.value = ""; dob.value = ""; annDate.value = ""
        dueDays.value = "0"
        isMsmeRegistered.value = false; msmeCategory.value = ""
        isKyc.value = false; tdsTcsApplicable.value = "N/A"
        assesseType.value = "Registered"
        accountType.value = "Customer"
        isEntryStarted.value = false
    }

    fun deleteAccount(id: String) {
        FirebaseRepository.deleteItem("Accounts", id)
    }
}
