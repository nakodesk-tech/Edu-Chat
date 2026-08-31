package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudentManagementTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeApi: FakeSupabaseDatabaseEngine

    private val puneSchoolId = "s0000000-0001-4000-8000-000000000001"

    private lateinit var teacherSession: AuthSession

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeSupabaseDatabaseEngine()
        SupabaseClient.testApiOverride = fakeApi
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        authRepository = AuthRepository(context, sessionManager, fakeApi)
        studentRepository = StudentRepository(context, sessionManager, fakeApi)

        // Teacher session setup
        val teacherProfile = UserProfile(
            id = "t-pune-001",
            fullName = "Sachin Patil Sir",
            email = "teacher.pune@educhat.edu",
            mobile = "9822011223",
            role = "teacher",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(teacherProfile.email!!, "password123", teacherProfile)

        teacherSession = AuthSession(
            accessToken = teacherProfile.id,
            refreshToken = "mock_refresh",
            profile = teacherProfile
        )
        sessionManager.saveSession(teacherSession)
    }

    @After
    fun tearDown() {
        SupabaseClient.reset()
    }

    @Test
    fun testStudentRegistrationSuccess() = runBlocking {
        val regResult = studentRepository.registerStudent(
            fullName = "Ananya Joshi (अनन्या जोशी)",
            email = "ananya.joshi@educhat.edu",
            password = "password123",
            mobile = "9822123456",
            standard = "इयत्ता १० वी अ (Class 10-A)",
            schoolId = puneSchoolId
        )

        assertTrue("Student registration should succeed", regResult.isSuccess)
        val created = regResult.getOrNull()
        assertNotNull(created)
        assertEquals("Ananya Joshi (अनन्या जोशी)", created?.fullName)
        assertEquals("ananya.joshi@educhat.edu", created?.email)
        assertEquals("student", created?.role)
        assertEquals("इयत्ता १० वी अ (Class 10-A)", created?.standard)
        assertEquals(puneSchoolId, created?.schoolId)
        assertTrue(created?.isActive == true)

        // Verify newly registered student can log in
        val loginResult = authRepository.login("ananya.joshi@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Newly registered student should be able to log in", loginResult is AuthResult.Success)
    }

    @Test
    fun testStudentRegistrationValidation_BlankName() = runBlocking {
        val result = studentRepository.registerStudent(
            fullName = "   ",
            email = "blank.name@educhat.edu",
            password = "password123",
            mobile = null,
            standard = "इयत्ता ९ वी",
            schoolId = puneSchoolId
        )
        assertTrue("Registration with blank name should fail", result.isFailure)
    }

    @Test
    fun testStudentRegistrationValidation_InvalidEmail() = runBlocking {
        val result = studentRepository.registerStudent(
            fullName = "Test Student",
            email = "invalid-email-format",
            password = "password123",
            mobile = null,
            standard = "इयत्ता ९ वी",
            schoolId = puneSchoolId
        )
        assertTrue("Registration with invalid email should fail", result.isFailure)
    }

    @Test
    fun testStudentRegistrationValidation_ShortPassword() = runBlocking {
        val result = studentRepository.registerStudent(
            fullName = "Test Student",
            email = "valid@educhat.edu",
            password = "123", // Short password
            mobile = null,
            standard = "इयत्ता ९ वी",
            schoolId = puneSchoolId
        )
        assertTrue("Registration with short password should fail", result.isFailure)
    }

    @Test
    fun testStudentRegistrationValidation_DuplicateEmail() = runBlocking {
        // student@educhat.edu is already seeded
        val result = studentRepository.registerStudent(
            fullName = "Duplicate Student",
            email = "student@educhat.edu",
            password = "password123",
            mobile = null,
            standard = "इयत्ता ९ वी",
            schoolId = puneSchoolId
        )
        assertTrue("Registration with duplicate email should fail", result.isFailure)
    }

    @Test
    fun testStudentRegistrationValidation_BlankAcademicYear() = runBlocking {
        val result = studentRepository.registerStudent(
            fullName = "Test Student",
            email = "valid.student@educhat.edu",
            password = "password123",
            mobile = null,
            standard = "इयत्ता ९ वी",
            schoolId = puneSchoolId,
            academicYear = "   "
        )
        assertTrue("Registration with blank academic year should fail", result.isFailure)
    }

    @Test
    fun testSchoolAdminCreateStudentRequest_RpcPayloadContract() {
        val request = com.example.data.model.SchoolAdminCreateStudentRequest(
            email = "student@test.com",
            password = "password123",
            fullName = "Student Name",
            mobile = "9822112233",
            standard = "10th",
            section = "A",
            academicYear = "2026-27"
        )
        assertEquals("student@test.com", request.email)
        assertEquals("password123", request.password)
        assertEquals("Student Name", request.fullName)
        assertEquals("9822112233", request.mobile)
        assertEquals("10th", request.standard)
        assertEquals("A", request.section)
        assertEquals("2026-27", request.academicYear)

        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(com.example.data.model.SchoolAdminCreateStudentRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"p_email\":\"student@test.com\""))
        assertTrue(json.contains("\"p_password\":\"password123\""))
        assertTrue(json.contains("\"p_full_name\":\"Student Name\""))
        assertTrue(json.contains("\"p_mobile\":\"9822112233\""))
        assertTrue(json.contains("\"p_standard\":\"10th\""))
        assertTrue(json.contains("\"p_section\":\"A\""))
        assertTrue(json.contains("\"p_academic_year\":\"2026-27\""))
    }

    @Test
    fun testSchoolAdminUpdateStudentRequest_RpcPayloadContract() {
        val request = com.example.data.model.SchoolAdminUpdateStudentRequest(
            studentId = "test-uuid-123",
            fullName = "Updated Student",
            mobile = "9822112233",
            standard = "10th",
            section = "B",
            isActive = true
        )
        assertEquals("test-uuid-123", request.studentId)
        assertEquals("Updated Student", request.fullName)
        assertEquals("9822112233", request.mobile)
        assertEquals("10th", request.standard)
        assertEquals("B", request.section)
        assertTrue(request.isActive)

        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(com.example.data.model.SchoolAdminUpdateStudentRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"p_student_id\":\"test-uuid-123\""))
        assertTrue(json.contains("\"p_full_name\":\"Updated Student\""))
        assertTrue(json.contains("\"p_mobile\":\"9822112233\""))
        assertTrue(json.contains("\"p_standard\":\"10th\""))
        assertTrue(json.contains("\"p_section\":\"B\""))
        assertTrue(json.contains("\"p_is_active\":true"))
    }

    @Test
    fun testStudentStandardUtils_AcademicYears() {
        val years = com.example.ui.students.StudentStandardUtils.ACADEMIC_YEARS
        assertTrue(years.contains("2026-27"))
        assertTrue(years.contains("2027-28"))
        assertEquals("2026-27", com.example.ui.students.StudentStandardUtils.DEFAULT_ACADEMIC_YEAR)
    }

    @Test
    fun testGetStudentsList() = runBlocking {
        val listResult = studentRepository.getStudents(puneSchoolId)
        assertTrue("Get students should succeed", listResult.isSuccess)
        val students = listResult.getOrDefault(emptyList())
        assertFalse("Students list should not be empty", students.isEmpty())
        assertTrue("All entries should be students", students.all { it.role.equals("student", ignoreCase = true) })
    }

    @Test
    fun testUpdateStudentProfile() = runBlocking {
        val students = studentRepository.getStudents(puneSchoolId).getOrDefault(emptyList())
        val target = students.first()

        val updateResult = studentRepository.updateStudent(
            studentId = target.id,
            fullName = "Updated Name (सुधारित नाव)",
            mobile = "9822998877",
            standard = "इयत्ता १० वी ब (Class 10-B)",
            schoolId = target.schoolId,
            isActive = true
        )

        assertTrue("Update student should succeed", updateResult.isSuccess)
        val updated = updateResult.getOrNull()
        assertEquals("Updated Name (सुधारित नाव)", updated?.fullName)
        assertEquals("9822998877", updated?.mobile)
        assertEquals("इयत्ता १० वी ब (Class 10-B)", updated?.standard)
    }

    @Test
    fun testToggleStudentStatus_DeactivateAndReactivate() = runBlocking {
        val students = studentRepository.getStudents(puneSchoolId).getOrDefault(emptyList())
        val target = students.first { it.isActive }

        // Deactivate
        val deactResult = studentRepository.toggleStudentStatus(target.id, false)
        assertTrue("Deactivate should succeed", deactResult.isSuccess)
        assertFalse(deactResult.getOrNull()!!.isActive)

        // Verify deactivated student cannot log in
        val loginAttempt = authRepository.login(target.email!!, "password123", UserRole.STUDENT)
        assertTrue("Deactivated student login should be rejected with Error", loginAttempt is AuthResult.Error)

        // Reactivate
        val reactResult = studentRepository.toggleStudentStatus(target.id, true)
        assertTrue("Reactivation should succeed", reactResult.isSuccess)
        assertTrue(reactResult.getOrNull()!!.isActive)

        // Login should now work
        val loginSuccess = authRepository.login(target.email!!, "password123", UserRole.STUDENT)
        assertTrue("Reactivated student should be able to log in", loginSuccess is AuthResult.Success)
    }

    @Test
    fun testDeleteStudent() = runBlocking {
        // Create student to delete
        val created = studentRepository.registerStudent(
            fullName = "Temporary Student",
            email = "temp.student@educhat.edu",
            password = "password123",
            mobile = null,
            standard = "इयत्ता ५ वी",
            schoolId = puneSchoolId
        ).getOrNull()!!

        val deleteResult = studentRepository.deleteStudent(created.id)
        assertTrue("Delete student should succeed", deleteResult.isSuccess)

        // Verify deleted from list
        val currentStudents = studentRepository.getStudents(puneSchoolId).getOrDefault(emptyList())
        assertFalse("Deleted student should not exist in list", currentStudents.any { it.id == created.id })
    }

    @Test
    fun testStudentManagementViewModel_SingleArgConstructorInstantiation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        // Verify ViewModel can be instantiated with single Application parameter (AndroidViewModelFactory contract)
        val constructor = com.example.ui.students.StudentManagementViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull("StudentManagementViewModel must have a single Application constructor", constructor)
        val viewModel = constructor.newInstance(app)
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testSchoolAdminCanAccessAndManageStudents() = runBlocking {
        val puneAdminProfile = UserProfile(
            id = "sa-pune-001",
            fullName = "Pune School Principal",
            email = "principal.pune@educhat.edu",
            mobile = "9800000001",
            role = "school_admin",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(puneAdminProfile.email!!, "password123", puneAdminProfile)

        // School Admin logs in
        val loginResult = authRepository.login("principal.pune@educhat.edu", "password123", UserRole.SCHOOL_ADMIN)
        assertTrue("School Admin login should succeed", loginResult is AuthResult.Success)

        // School Admin registers a student
        val regResult = studentRepository.registerStudent(
            fullName = "Rohan Shinde",
            email = "rohan.shinde@educhat.edu",
            password = "password123",
            mobile = "9822334455",
            standard = "इयत्ता ९ वी ब (Class 9-B)",
            schoolId = puneSchoolId
        )
        assertTrue("School Admin can register student", regResult.isSuccess)

        // School Admin retrieves students list for their school
        val studentsResult = studentRepository.getStudents(puneSchoolId)
        assertTrue(studentsResult.isSuccess)
        val students = studentsResult.getOrDefault(emptyList())
        assertTrue(students.any { it.email == "rohan.shinde@educhat.edu" })
    }

    @Test
    fun testStudentStandardUtils_FormattingAndParsing() {
        val formatted = com.example.ui.students.StudentStandardUtils.formatStoredStandard("10th", "A")
        assertEquals("10th - A", formatted)

        val parsedStd1 = com.example.ui.students.StudentStandardUtils.parseStandard("10th - A")
        val parsedSec1 = com.example.ui.students.StudentStandardUtils.parseSection("10th - A")
        assertEquals("10th", parsedStd1)
        assertEquals("A", parsedSec1)

        val parsedStd2 = com.example.ui.students.StudentStandardUtils.parseStandard("इयत्ता १० वी अ (Class 10-A)")
        val parsedSec2 = com.example.ui.students.StudentStandardUtils.parseSection("इयत्ता १० वी अ (Class 10-A)")
        assertEquals("10th", parsedStd2)
        assertEquals("A", parsedSec2)

        val parsedStd3 = com.example.ui.students.StudentStandardUtils.parseStandard("1st")
        val parsedSec3 = com.example.ui.students.StudentStandardUtils.parseSection("1st")
        assertEquals("1st", parsedStd3)
        assertNull(parsedSec3)

        val parsedStd4 = com.example.ui.students.StudentStandardUtils.parseStandard("7th - B")
        val parsedSec4 = com.example.ui.students.StudentStandardUtils.parseSection("7th - B")
        assertEquals("7th", parsedStd4)
        assertEquals("B", parsedSec4)
    }

    @Test
    fun testFilterStandardsOrder_AllAnd1stTo10th() {
        val standards = com.example.ui.students.StudentStandardUtils.FILTER_STANDARDS
        assertEquals("सर्व", standards[0])
        assertEquals("1st", standards[1])
        assertEquals("2nd", standards[2])
        assertEquals("3rd", standards[3])
        assertEquals("4th", standards[4])
        assertEquals("5th", standards[5])
        assertEquals("6th", standards[6])
        assertEquals("7th", standards[7])
        assertEquals("8th", standards[8])
        assertEquals("9th", standards[9])
        assertEquals("10th", standards[10])
    }
}
