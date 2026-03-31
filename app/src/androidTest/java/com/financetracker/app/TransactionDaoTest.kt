package com.financetracker.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financetracker.app.data.db.FinanceDatabase
import com.financetracker.app.data.db.dao.CategoryDao
import com.financetracker.app.data.db.dao.TransactionDao
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: FinanceDatabase
    private lateinit var txDao: TransactionDao
    private lateinit var catDao: CategoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        txDao  = db.transactionDao()
        catDao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleTx(
        amount: Double = 100.0,
        merchant: String = "Test Merchant",
        category: String = "Food",
        isCredit: Boolean = false,
        dateMs: Long = System.currentTimeMillis(),
        dedupHash: String = ""
    ) = TransactionEntity(
        amount = amount, merchant = merchant, category = category,
        date = dateMs, source = "Manual", rawText = "", isCredit = isCredit, dedupHash = dedupHash
    )

    @Test
    fun insertAndRetrieve() = runTest {
        val tx = sampleTx()
        txDao.insert(tx)
        val all = txDao.getAllPaged()
        // Just verify it doesn't throw; pager smoke test
        assertThat(all).isNotNull()
    }

    @Test
    fun getRecent_returnsLatestFive() = runTest {
        repeat(8) { i ->
            txDao.insert(sampleTx(amount = (i + 1) * 100.0, dateMs = (1000L * (i + 1))))
        }
        val recent = txDao.getRecent().first()
        assertThat(recent.size).isAtMost(5)
    }

    @Test
    fun getByDateRange() = runTest {
        txDao.insert(sampleTx(dateMs = 1_000L))
        txDao.insert(sampleTx(dateMs = 5_000L))
        txDao.insert(sampleTx(dateMs = 10_000L))

        val inRange = txDao.getByDateRange(startMs = 2_000L, endMs = 9_000L).first()
        assertThat(inRange.size).isEqualTo(1)
        assertThat(inRange[0].date).isEqualTo(5_000L)
    }

    @Test
    fun getTotalExpenses() = runTest {
        txDao.insert(sampleTx(amount = 200.0, isCredit = false, dateMs = 5_000L))
        txDao.insert(sampleTx(amount = 100.0, isCredit = true,  dateMs = 5_000L))

        val total = txDao.getTotalExpenses(0L, 10_000L).first()
        assertThat(total).isWithin(0.01).of(200.0)
    }

    @Test
    fun getTotalIncome() = runTest {
        txDao.insert(sampleTx(amount = 50000.0, isCredit = true,  dateMs = 5_000L))
        txDao.insert(sampleTx(amount = 1000.0,  isCredit = false, dateMs = 5_000L))

        val total = txDao.getTotalIncome(0L, 10_000L).first()
        assertThat(total).isWithin(0.01).of(50000.0)
    }

    @Test
    fun getSpendByCategory() = runTest {
        txDao.insert(sampleTx(amount = 300.0, category = "Food",      isCredit = false, dateMs = 5_000L))
        txDao.insert(sampleTx(amount = 200.0, category = "Food",      isCredit = false, dateMs = 5_000L))
        txDao.insert(sampleTx(amount = 400.0, category = "Transport", isCredit = false, dateMs = 5_000L))

        val spends = txDao.getSpendByCategory(0L, 10_000L).first()
        val food = spends.find { it.category == "Food" }
        assertThat(food?.total).isWithin(0.01).of(500.0)
    }

    @Test
    fun deduplication_preventsInsertOfDuplicateHash() = runTest {
        val hash = "abc123"
        txDao.insert(sampleTx(dedupHash = hash))
        txDao.insert(sampleTx(amount = 999.0, dedupHash = hash)) // IGNORE strategy

        val hashes = txDao.getAllDedupHashes()
        assertThat(hashes.count { it == hash }).isEqualTo(1)
    }

    @Test
    fun updateTransaction_persistsChanges() = runTest {
        val id = txDao.insert(sampleTx(category = "Other"))
        val tx = txDao.getById(id)!!
        txDao.update(tx.copy(category = "Food"))

        val updated = txDao.getById(id)
        assertThat(updated?.category).isEqualTo("Food")
    }

    @Test
    fun deleteTransaction() = runTest {
        val id = txDao.insert(sampleTx())
        val tx = txDao.getById(id)!!
        txDao.delete(tx)
        assertThat(txDao.getById(id)).isNull()
    }

    @Test
    fun categoryDao_insertAndGetAll() = runTest {
        catDao.insert(CategoryEntity(name = "TestCat", icon = "🧪", colorHex = "#FFFFFF"))
        val all = catDao.getAll().first()
        assertThat(all.any { it.name == "TestCat" }).isTrue()
    }

    @Test
    fun categoryDao_getByName() = runTest {
        catDao.insert(CategoryEntity(name = "Unique", icon = "⭐", colorHex = "#FF0000"))
        val found = catDao.getByName("Unique")
        assertThat(found).isNotNull()
        assertThat(found!!.icon).isEqualTo("⭐")
    }
}
