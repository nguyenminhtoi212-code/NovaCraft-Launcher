package com.aistudio.novacraft.launcher.core.mods

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.novacraft.launcher.core.utils.InstalledModItem
import com.aistudio.novacraft.launcher.core.utils.ModrinthProject
import com.aistudio.novacraft.launcher.core.utils.ModrinthSearchHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Thực thể Room lưu trữ mod / addon được người dùng thả tim (Yêu thích) để truy cập nhanh ngoại tuyến.
 */
@Entity(tableName = "favorite_mods")
data class FavoriteModEntity(
    @PrimaryKey val projectId: String,
    val projectType: String,
    val itemType: String,
    val source: String,
    val slug: String?,
    val author: String?,
    val title: String,
    val description: String?,
    val categoriesCsv: String,
    val versionsCsv: String,
    val downloads: Long,
    val iconUrl: String?,
    val dateCreated: String?,
    val dateModified: String?,
    val follows: Long,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toSearchHit(): ModrinthSearchHit {
        val cats = categoriesCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val vers = versionsCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        return ModrinthSearchHit(
            projectId = projectId,
            projectType = projectType,
            slug = slug,
            author = author,
            title = title,
            description = description,
            categories = cats,
            displayCategories = cats,
            versions = vers,
            downloads = downloads,
            iconUrl = iconUrl,
            color = null,
            dateCreated = dateCreated,
            dateModified = dateModified,
            follows = follows
        )
    }

    fun toProject(): ModrinthProject {
        val cats = categoriesCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val vers = versionsCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val knownLoaders = setOf("fabric", "forge", "neoforge", "quilt", "liteloader", "cauldron")
        return ModrinthProject(
            id = projectId,
            slug = slug ?: projectId,
            title = title,
            description = description.orEmpty(),
            body = null,
            categories = cats.filterNot { it.lowercase() in knownLoaders },
            loaders = cats.filter { it.lowercase() in knownLoaders },
            gameVersions = vers,
            downloads = downloads,
            iconUrl = iconUrl,
            gallery = emptyList()
        )
    }

    companion object {
        fun fromSearchHit(hit: ModrinthSearchHit, itemType: String): FavoriteModEntity {
            val source = when {
                hit.projectId.startsWith(CurseForgeMapper.ID_PREFIX) -> NuxModManager.SOURCE_CURSEFORGE
                else -> NuxModManager.SOURCE_MODRINTH
            }
            val cats = (hit.displayCategories ?: hit.categories).orEmpty().joinToString("|")
            val vers = hit.versions.orEmpty().joinToString("|")
            return FavoriteModEntity(
                projectId = hit.projectId,
                projectType = hit.projectType,
                itemType = itemType,
                source = source,
                slug = hit.slug,
                author = hit.author,
                title = hit.title,
                description = hit.description,
                categoriesCsv = cats,
                versionsCsv = vers,
                downloads = hit.downloads,
                iconUrl = hit.iconUrl,
                dateCreated = hit.dateCreated,
                dateModified = hit.dateModified,
                follows = hit.follows,
                savedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Bộ nhớ đệm cục bộ cho các mod đã duyệt trực tuyến, giúp chế độ Local-Only hoạt động tức thì khi mạng yếu.
 */
@Entity(tableName = "cached_mods")
data class CachedModEntity(
    @PrimaryKey val projectId: String,
    val projectType: String,
    val itemType: String,
    val source: String,
    val slug: String?,
    val author: String?,
    val title: String,
    val description: String?,
    val categoriesCsv: String,
    val versionsCsv: String,
    val downloads: Long,
    val iconUrl: String?,
    val dateCreated: String?,
    val dateModified: String?,
    val follows: Long,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toSearchHit(): ModrinthSearchHit {
        val cats = categoriesCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val vers = versionsCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        return ModrinthSearchHit(
            projectId = projectId,
            projectType = projectType,
            slug = slug,
            author = author,
            title = title,
            description = description,
            categories = cats,
            displayCategories = cats,
            versions = vers,
            downloads = downloads,
            iconUrl = iconUrl,
            color = null,
            dateCreated = dateCreated,
            dateModified = dateModified,
            follows = follows
        )
    }

    fun toProject(): ModrinthProject {
        val cats = categoriesCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val vers = versionsCsv.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val knownLoaders = setOf("fabric", "forge", "neoforge", "quilt", "liteloader", "cauldron")
        return ModrinthProject(
            id = projectId,
            slug = slug ?: projectId,
            title = title,
            description = description.orEmpty(),
            body = null,
            categories = cats.filterNot { it.lowercase() in knownLoaders },
            loaders = cats.filter { it.lowercase() in knownLoaders },
            gameVersions = vers,
            downloads = downloads,
            iconUrl = iconUrl,
            gallery = emptyList()
        )
    }

    companion object {
        fun fromSearchHit(hit: ModrinthSearchHit, itemType: String): CachedModEntity {
            val source = when {
                hit.projectId.startsWith(CurseForgeMapper.ID_PREFIX) -> NuxModManager.SOURCE_CURSEFORGE
                else -> NuxModManager.SOURCE_MODRINTH
            }
            val cats = (hit.displayCategories ?: hit.categories).orEmpty().joinToString("|")
            val vers = hit.versions.orEmpty().joinToString("|")
            return CachedModEntity(
                projectId = hit.projectId,
                projectType = hit.projectType,
                itemType = itemType,
                source = source,
                slug = hit.slug,
                author = hit.author,
                title = hit.title,
                description = hit.description,
                categoriesCsv = cats,
                versionsCsv = vers,
                downloads = hit.downloads,
                iconUrl = hit.iconUrl,
                dateCreated = hit.dateCreated,
                dateModified = hit.dateModified,
                follows = hit.follows,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Bộ nhớ đệm siêu dữ liệu mod đã cài đặt (.jar / .mrpack) giúp bỏ qua giải nén ZIP lặp lại khi khởi động.
 */
@Entity(tableName = "installed_mod_cache")
data class InstalledModCacheEntity(
    @PrimaryKey val filePath: String,
    val dirPath: String,
    val itemType: String,
    val id: String,
    val filename: String,
    val originalFilename: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toInstalledModItem(): InstalledModItem = InstalledModItem(
        id = id,
        filename = filename,
        originalFilename = originalFilename,
        name = name,
        description = description,
        version = version,
        category = category,
        isEnabled = isEnabled,
        sizeBytes = sizeBytes,
        itemType = itemType,
        filePath = filePath
    )

    companion object {
        fun fromInstalledModItem(
            item: InstalledModItem,
            dirPath: String,
            lastModified: Long
        ): InstalledModCacheEntity = InstalledModCacheEntity(
            filePath = item.filePath,
            dirPath = dirPath,
            itemType = item.itemType,
            id = item.id,
            filename = item.filename,
            originalFilename = item.originalFilename,
            name = item.name,
            description = item.description,
            version = item.version,
            category = item.category,
            isEnabled = item.isEnabled,
            sizeBytes = item.sizeBytes,
            lastModified = lastModified,
            cachedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Lịch sử tìm kiếm thông minh theo loại nội dung để người dùng chạm lại nhanh các từ khóa đã học.
 */
@Entity(tableName = "mod_search_history")
data class ModSearchHistoryEntity(
    @PrimaryKey val historyKey: String,
    val itemType: String,
    val displayQuery: String,
    val searchCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteModDao {
    @androidx.room.Query("SELECT * FROM favorite_mods ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteModEntity>>

    @androidx.room.Query("SELECT projectId FROM favorite_mods")
    fun observeFavoriteIds(): Flow<List<String>>

    @androidx.room.Query("SELECT * FROM favorite_mods WHERE itemType = :itemType")
    suspend fun getFavoritesByType(itemType: String): List<FavoriteModEntity>

    @androidx.room.Query("SELECT * FROM favorite_mods WHERE projectId = :projectId LIMIT 1")
    suspend fun getFavoriteById(projectId: String): FavoriteModEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteModEntity)

    @androidx.room.Query("DELETE FROM favorite_mods WHERE projectId = :projectId")
    suspend fun deleteById(projectId: String)

    @androidx.room.Query("SELECT EXISTS(SELECT 1 FROM favorite_mods WHERE projectId = :projectId)")
    suspend fun isFavorite(projectId: String): Boolean

    @androidx.room.Query("SELECT * FROM mod_search_history WHERE itemType = :itemType ORDER BY lastUsedAt DESC, searchCount DESC LIMIT 10")
    fun observeRecentQueries(itemType: String): Flow<List<ModSearchHistoryEntity>>

    @androidx.room.Query("SELECT * FROM mod_search_history WHERE historyKey = :key LIMIT 1")
    suspend fun getHistory(key: String): ModSearchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: ModSearchHistoryEntity)

    @androidx.room.Query("DELETE FROM mod_search_history WHERE itemType = :itemType")
    suspend fun clearHistoryForType(itemType: String)

    // Bộ nhớ đệm mod cục bộ (chế độ Local-Only)
    @androidx.room.Query("SELECT COUNT(*) FROM cached_mods")
    fun observeCachedModsCount(): Flow<Int>

    @androidx.room.Query("SELECT * FROM cached_mods WHERE itemType = :itemType ORDER BY cachedAt DESC")
    suspend fun getCachedModsByType(itemType: String): List<CachedModEntity>

    @androidx.room.Query("SELECT * FROM cached_mods WHERE projectId = :projectId LIMIT 1")
    suspend fun getCachedModById(projectId: String): CachedModEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedMods(items: List<CachedModEntity>)

    @androidx.room.Query("DELETE FROM cached_mods")
    suspend fun clearCachedMods()

    // Bộ nhớ đệm siêu dữ liệu mod đã cài đặt (tăng tốc khởi động)
    @androidx.room.Query("SELECT * FROM installed_mod_cache WHERE dirPath = :dirPath AND itemType = :itemType")
    suspend fun getInstalledCacheForDir(dirPath: String, itemType: String): List<InstalledModCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledCache(items: List<InstalledModCacheEntity>)

    @androidx.room.Query("DELETE FROM installed_mod_cache WHERE dirPath = :dirPath AND itemType = :itemType")
    suspend fun clearInstalledCacheForDir(dirPath: String, itemType: String)
}

@Database(
    entities = [
        FavoriteModEntity::class,
        ModSearchHistoryEntity::class,
        CachedModEntity::class,
        InstalledModCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NovaLauncherDatabase : RoomDatabase() {
    abstract fun favoriteModDao(): FavoriteModDao

    companion object {
        @Volatile
        private var INSTANCE: NovaLauncherDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_mods` (
                        `projectId` TEXT NOT NULL,
                        `projectType` TEXT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `slug` TEXT,
                        `author` TEXT,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `categoriesCsv` TEXT NOT NULL,
                        `versionsCsv` TEXT NOT NULL,
                        `downloads` INTEGER NOT NULL,
                        `iconUrl` TEXT,
                        `dateCreated` TEXT,
                        `dateModified` TEXT,
                        `follows` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`projectId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_mod_cache` (
                        `filePath` TEXT NOT NULL,
                        `dirPath` TEXT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `filename` TEXT NOT NULL,
                        `originalFilename` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`filePath`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NovaLauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaLauncherDatabase::class.java,
                    "novacraft_mods.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class ModFavoriteRepository(private val dao: FavoriteModDao) {
    val allFavorites: Flow<List<FavoriteModEntity>> = dao.observeAll()
    val favoriteIds: Flow<Set<String>> = dao.observeFavoriteIds().map { it.toSet() }
    val cachedModsCount: Flow<Int> = dao.observeCachedModsCount()

    fun observeRecentQueries(itemType: String): Flow<List<ModSearchHistoryEntity>> =
        dao.observeRecentQueries(itemType)

    suspend fun toggleFavorite(hit: ModrinthSearchHit, itemType: String): Boolean {
        val exists = dao.isFavorite(hit.projectId)
        return if (exists) {
            dao.deleteById(hit.projectId)
            false
        } else {
            dao.upsert(FavoriteModEntity.fromSearchHit(hit, itemType))
            true
        }
    }

    suspend fun removeFavorite(projectId: String) {
        dao.deleteById(projectId)
    }

    suspend fun recordSearch(query: String, itemType: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val key = "${itemType.lowercase()}:${trimmed.lowercase()}"
        val existing = dao.getHistory(key)
        dao.upsertHistory(
            ModSearchHistoryEntity(
                historyKey = key,
                itemType = itemType,
                displayQuery = trimmed,
                searchCount = (existing?.searchCount ?: 0) + 1,
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSearchHistory(itemType: String) {
        dao.clearHistoryForType(itemType)
    }

    suspend fun clearCachedMods() {
        dao.clearCachedMods()
    }
}

class ModFavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ModFavoriteRepository(
        NovaLauncherDatabase.getInstance(application).favoriteModDao()
    )

    val favorites: StateFlow<List<FavoriteModEntity>> = repository.allFavorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val cachedModsCount: StateFlow<Int> = repository.cachedModsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun recentQueriesFor(itemType: String): Flow<List<ModSearchHistoryEntity>> =
        repository.observeRecentQueries(itemType)

    fun toggleFavorite(hit: ModrinthSearchHit, itemType: String, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val saved = repository.toggleFavorite(hit, itemType)
            onDone?.invoke(saved)
        }
    }

    fun recordSearch(query: String, itemType: String) {
        viewModelScope.launch {
            repository.recordSearch(query, itemType)
        }
    }

    fun clearSearchHistory(itemType: String) {
        viewModelScope.launch {
            repository.clearSearchHistory(itemType)
        }
    }

    fun clearCachedMods() {
        viewModelScope.launch {
            repository.clearCachedMods()
        }
    }
}
