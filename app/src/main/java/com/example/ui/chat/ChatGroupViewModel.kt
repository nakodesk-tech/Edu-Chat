package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.ChatMessage
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.GroupType
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatGroupUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    val currentProfile: UserProfile? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val selectedGroupMembers: List<GroupMember> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val activeSchools: List<School> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showGroupDetailDialog: Boolean = false,
    val showAddMemberDialog: Boolean = false,
    val eligibleUsers: List<UserProfile> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val emailSearchQuery: String = "",
    val searchedUserByEmail: UserProfile? = null,
    val emailSearchMessage: String? = null,
    val roleFilter: String? = null,
    val schoolFilter: String? = null,
    val userSearchQuery: String = "",
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
)

class ChatGroupViewModel(application: Application) : AndroidViewModel(application) {
    private val groupRepo = GroupRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(ChatGroupUiState())
    val uiState: StateFlow<ChatGroupUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserAndGroups()
    }

    fun loadCurrentUserAndGroups() {
        viewModelScope.launch {
            val session = sessionManager.getSession()
            _uiState.update {
                it.copy(
                    currentProfile = session?.profile,
                    isLoading = true,
                    errorMessage = null
                )
            }

            val groupsRes = groupRepo.getGroups()
            val schoolsRes = groupRepo.getActiveSchools()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    groups = groupsRes.getOrDefault(emptyList()),
                    activeSchools = schoolsRes.getOrDefault(emptyList()),
                    errorMessage = groupsRes.exceptionOrNull()?.message
                )
            }
        }
    }

    fun openCreateGroupDialog() {
        _uiState.update {
            it.copy(showCreateDialog = true, errorMessage = null)
        }
    }

    fun closeCreateGroupDialog() {
        _uiState.update {
            it.copy(showCreateDialog = false, errorMessage = null)
        }
    }

    fun createGroup(name: String, groupType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val res = groupRepo.createGroup(name, groupType)
            if (res.isSuccess) {
                val newGroup = res.getOrThrow()
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        showCreateDialog = false,
                        snackbarMessage = "गट यशस्वीरित्या तयार झाला! (${newGroup.name})"
                    )
                }
                loadCurrentUserAndGroups()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "गट तयार करण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    fun openGroupDetail(group: Group) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedGroup = group,
                    showGroupDetailDialog = true,
                    isDetailLoading = true,
                    errorMessage = null
                )
            }
            val detailsRes = groupRepo.getGroupDetails(group.id)
            if (detailsRes.isSuccess) {
                val (enrichedGroup, members) = detailsRes.getOrThrow()
                _uiState.update {
                    it.copy(
                        selectedGroup = enrichedGroup,
                        selectedGroupMembers = members,
                        isDetailLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        errorMessage = detailsRes.exceptionOrNull()?.message ?: "माहिती लोड करण्यात अडचण आली."
                    )
                }
            }
        }
    }

    fun closeGroupDetail() {
        _uiState.update {
            it.copy(
                showGroupDetailDialog = false,
                selectedGroup = null,
                selectedGroupMembers = emptyList(),
                errorMessage = null
            )
        }
    }

    fun openAddMemberDialog() {
        val group = _uiState.value.selectedGroup ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showAddMemberDialog = true,
                    selectedUserIds = emptySet(),
                    emailSearchQuery = "",
                    searchedUserByEmail = null,
                    emailSearchMessage = null,
                    userSearchQuery = "",
                    roleFilter = null,
                    schoolFilter = null,
                    errorMessage = null
                )
            }
            refreshEligibleUsers()
        }
    }

    fun closeAddMemberDialog() {
        _uiState.update {
            it.copy(
                showAddMemberDialog = false,
                selectedUserIds = emptySet(),
                searchedUserByEmail = null,
                emailSearchMessage = null,
                errorMessage = null
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        refreshEligibleUsers()
    }

    fun setRoleFilter(role: String?) {
        _uiState.update { it.copy(roleFilter = role) }
        refreshEligibleUsers()
    }

    fun setSchoolFilter(schoolId: String?) {
        _uiState.update { it.copy(schoolFilter = schoolId) }
        refreshEligibleUsers()
    }

    private fun refreshEligibleUsers() {
        val currentGroup = _uiState.value.selectedGroup ?: return
        val currentProfile = _uiState.value.currentProfile ?: return
        val existingMemberIds = _uiState.value.selectedGroupMembers.map { it.userId }.toSet()

        viewModelScope.launch {
            if (currentGroup.isAdministrative) {
                val res = groupRepo.searchEligibleUsersForAdminGroup(
                    query = _uiState.value.userSearchQuery,
                    roleFilter = _uiState.value.roleFilter,
                    schoolFilter = _uiState.value.schoolFilter
                )
                val allEligible = res.getOrDefault(emptyList())
                // Exclude already active members
                val notYetMembers = allEligible.filter { !existingMemberIds.contains(it.id) }
                _uiState.update { it.copy(eligibleUsers = notYetMembers) }
            } else if (currentGroup.isTeacherGroup) {
                val res = groupRepo.searchEligibleStudentsForTeacherGroup(
                    query = _uiState.value.userSearchQuery
                )
                val allEligible = res.getOrDefault(emptyList())
                val notYetMembers = allEligible.filter { !existingMemberIds.contains(it.id) }
                _uiState.update { it.copy(eligibleUsers = notYetMembers) }
            }
        }
    }

    fun toggleUserSelection(userId: String) {
        _uiState.update { state ->
            val current = state.selectedUserIds.toMutableSet()
            if (current.contains(userId)) {
                current.remove(userId)
            } else {
                current.add(userId)
            }
            state.copy(selectedUserIds = current)
        }
    }

    fun searchUserByEmail(email: String) {
        val trimmed = email.trim()
        _uiState.update { it.copy(emailSearchQuery = trimmed, emailSearchMessage = null, searchedUserByEmail = null) }
        if (trimmed.isBlank()) return

        val currentGroup = _uiState.value.selectedGroup ?: return
        val existingMemberIds = _uiState.value.selectedGroupMembers.map { it.userId }.toSet()

        viewModelScope.launch {
            if (currentGroup.isAdministrative) {
                val res = groupRepo.searchEligibleUsersForAdminGroup(query = trimmed)
                val matched = res.getOrDefault(emptyList()).firstOrNull { it.email.equals(trimmed, ignoreCase = true) }
                if (matched == null) {
                    _uiState.update {
                        it.copy(
                            searchedUserByEmail = null,
                            emailSearchMessage = "या Email चा पात्र वापरकर्ता आढळला नाही किंवा विद्यार्थी प्रशासकीय गटात जोडता येत नाही."
                        )
                    }
                } else if (existingMemberIds.contains(matched.id)) {
                    _uiState.update {
                        it.copy(
                            searchedUserByEmail = null,
                            emailSearchMessage = "हा वापरकर्ता आधीपासून या गटाचा सदस्य आहे."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(searchedUserByEmail = matched, emailSearchMessage = null)
                    }
                }
            } else if (currentGroup.isTeacherGroup) {
                val res = groupRepo.searchEligibleStudentsForTeacherGroup(query = trimmed)
                val matched = res.getOrDefault(emptyList()).firstOrNull { it.email.equals(trimmed, ignoreCase = true) }
                if (matched == null) {
                    _uiState.update {
                        it.copy(
                            searchedUserByEmail = null,
                            emailSearchMessage = "आपल्या शाळेत या Email चा विद्यार्थी आढळला नाही."
                        )
                    }
                } else if (existingMemberIds.contains(matched.id)) {
                    _uiState.update {
                        it.copy(
                            searchedUserByEmail = null,
                            emailSearchMessage = "हा विद्यार्थी आधीपासून या गटाचा सदस्य आहे."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(searchedUserByEmail = matched, emailSearchMessage = null)
                    }
                }
            }
        }
    }

    fun addSingleMember(userId: String) {
        val group = _uiState.value.selectedGroup ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val res = groupRepo.addMember(group.id, userId)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        showAddMemberDialog = false,
                        snackbarMessage = "सदस्य यशस्वीरित्या जोडला गेला!"
                    )
                }
                openGroupDetail(group)
                loadCurrentUserAndGroups()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "सदस्य जोडण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    fun addSelectedBatchMembers() {
        val group = _uiState.value.selectedGroup ?: return
        val selectedIds = _uiState.value.selectedUserIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val res = groupRepo.addMembersBatch(group.id, selectedIds)
            if (res.isSuccess) {
                val added = res.getOrThrow()
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        showAddMemberDialog = false,
                        selectedUserIds = emptySet(),
                        snackbarMessage = "${added.size} सदस्य यशस्वीरित्या जोडले गेले!"
                    )
                }
                openGroupDetail(group)
                loadCurrentUserAndGroups()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "सदस्य जोडण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    fun removeMember(targetUserId: String) {
        val group = _uiState.value.selectedGroup ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val res = groupRepo.removeMember(group.id, targetUserId)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = "सदस्य यशस्वीरित्या काढला गेला."
                    )
                }
                openGroupDetail(group)
                loadCurrentUserAndGroups()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "सदस्य काढण्यात अडचण आली."
                    )
                }
            }
        }
    }

    fun setMessageInput(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }

    fun loadMessages(groupId: String) {
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMessagesLoading = true, errorMessage = null) }
            val res = groupRepo.getGroupMessages(groupId)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isMessagesLoading = false,
                        messages = res.getOrThrow()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isMessagesLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "संदेश लोड करण्यात अडचण आली."
                    )
                }
            }
        }
    }

    fun sendMessage(groupId: String, content: String = _uiState.value.messageInput) {
        val trimmed = content.trim()
        if (groupId.isBlank() || trimmed.isBlank()) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true, errorMessage = null) }
            val res = groupRepo.sendGroupMessage(groupId, trimmed)
            if (res.isSuccess) {
                val sentMessage = res.getOrThrow()
                _uiState.update { state ->
                    val updatedList = state.messages + sentMessage
                    state.copy(
                        isSendingMessage = false,
                        messageInput = "",
                        messages = updatedList
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "संदेश पाठवण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
