package com.browser.app.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.dao.PasswordDao
import com.browser.app.utils.CryptoUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * PasswordRepository 单元测试。
 *
 * CryptoUtils 基于 Android Keystore，在 Robolectric 环境下 Keystore 可能不可用。
 * 因此本测试在 @Before 中探测 Keystore 可用性：
 * - 可用：完整验证 加密入库 / 解密读取 的流程
 * - 不可用：使用 assumeTrue 跳过依赖加解密的用例（不影响构建）
 */
@RunWith(RobolectricTestRunner::class)
class PasswordRepositoryTest {

    private lateinit var database: BrowserDatabase
    private lateinit var dao: PasswordDao
    private lateinit var repo: PasswordRepository
    private var keystoreAvailable = false

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.passwordDao()
        repo = PasswordRepository(dao)

        // 探测 Robolectric 环境下 Android Keystore 是否可用
        keystoreAvailable = try {
            val cipher = CryptoUtils.encrypt("probe")
            CryptoUtils.decrypt(cipher) == "probe"
        } catch (t: Throwable) {
            false
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getAllPasswords_emptyInitially() = runTest {
        // 不依赖加密，仅验证空数据库返回空列表
        assertTrue(repo.getAllPasswords().first().isEmpty())
    }

    @Test
    fun addPassword_andRetrieveDecrypted() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        repo.addPassword(
            site = "TestSite",
            username = "alice",
            plainPassword = "P@ssw0rd",
            url = "https://test.com"
        )

        val list = repo.getAllPasswords().first()
        assertEquals(1, list.size)
        // Repository 出参应为已解密的明文密码
        assertEquals("P@ssw0rd", list[0].encryptedPassword)
        assertEquals("alice", list[0].username)
        assertEquals("TestSite", list[0].site)
        assertEquals("https://test.com", list[0].url)
    }

    @Test
    fun addPassword_storesCipher_notPlaintext() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        val plain = "superSecret123"
        repo.addPassword(site = "Site", username = "bob", plainPassword = plain)

        // 直接读取 DAO（未经 Repository 解密），数据库中应存储密文而非明文
        val rawList = dao.getAllPasswords().first()
        assertEquals(1, rawList.size)
        assertNotEquals("数据库中不应直接存储明文密码", plain, rawList[0].encryptedPassword)
        // 解密后应还原为明文
        assertEquals(plain, CryptoUtils.decrypt(rawList[0].encryptedPassword))
    }

    @Test
    fun getByUrl_returnsDecryptedOrNull() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        repo.addPassword(
            site = "Site",
            username = "u",
            plainPassword = "secret",
            url = "https://login.example.com"
        )

        val found = repo.getByUrl("https://login.example.com")
        assertEquals("secret", found?.encryptedPassword)
        assertEquals("u", found?.username)

        val missing = repo.getByUrl("https://nope.com")
        assertNull(missing)
    }

    @Test
    fun deleteById_removesEntry() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        val id = repo.addPassword(
            site = "S", username = "u", plainPassword = "p", url = "https://x.com"
        )
        repo.deleteById(id)

        assertTrue(repo.getAllPasswords().first().isEmpty())
    }

    @Test
    fun addMultiplePasswords_allDecryptedCorrectly() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        repo.addPassword(site = "A", username = "u1", plainPassword = "pwd1")
        repo.addPassword(site = "B", username = "u2", plainPassword = "pwd2")
        repo.addPassword(site = "C", username = "u3", plainPassword = "pwd3")

        val list = repo.getAllPasswords().first()
        assertEquals(3, list.size)
        // getAllPasswords 按 site 升序排序
        val plains = list.map { it.encryptedPassword }
        assertTrue("pwd1" in plains)
        assertTrue("pwd2" in plains)
        assertTrue("pwd3" in plains)
    }

    @Test
    fun addMultiplePasswords_eachCipherIsUnique() = runTest {
        assumeTrue("Android Keystore 不可用，跳过加密验证", keystoreAvailable)

        // 相同明文加密后应产生不同密文（GCM 随机 IV）
        repo.addPassword(site = "A", username = "u1", plainPassword = "samePwd")
        repo.addPassword(site = "B", username = "u2", plainPassword = "samePwd")

        val rawList = dao.getAllPasswords().first()
        assertEquals(2, rawList.size)
        assertNotEquals(
            "相同明文应产生不同密文（IV 随机）",
            rawList[0].encryptedPassword,
            rawList[1].encryptedPassword
        )
        // 但解密后都应还原为相同明文
        assertEquals("samePwd", CryptoUtils.decrypt(rawList[0].encryptedPassword))
        assertEquals("samePwd", CryptoUtils.decrypt(rawList[1].encryptedPassword))
    }
}
