package com.example.video_downloder.di

import android.app.Application
import androidx.room.Room
import com.example.video_downloder.data.local.database.AppDatabase
import com.example.video_downloder.data.repository.NoteRepositoryImpl
import com.example.video_downloder.data.repository.StatusRepositoryImpl
import com.example.video_downloder.domain.repository.NoteRepository
import com.example.video_downloder.domain.repository.StatusRepository
import com.example.video_downloder.domain.repository.VideoRepository
import com.example.video_downloder.data.repository.VideoRepositoryImpl
import com.example.video_downloder.data.repository.MediaRepositoryImpl
import com.example.video_downloder.domain.repository.MediaRepository
import com.example.video_downloder.data.remote.api.VideoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.content.Context
import java.util.concurrent.TimeUnit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.video_downloder.data.repository.LockedVideoRepositoryImpl
import com.example.video_downloder.domain.repository.LockedVideoRepository
import com.example.video_downloder.data.repository.HiddenVideoRepositoryImpl
import com.example.video_downloder.domain.repository.HiddenVideoRepository

import com.example.video_downloder.data.repository.RecentChatRepositoryImpl
import com.example.video_downloder.domain.repository.RecentChatRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRecentChatRepository(db: AppDatabase): RecentChatRepository {
        return RecentChatRepositoryImpl(db.recentChatDao)
    }

    @Provides
    @Singleton
    fun provideDownloadDao(db: AppDatabase): com.example.video_downloder.data.local.dao.DownloadDao {
        return db.downloadDao
    }

    @Provides
    @Singleton
    fun provideNoteDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(db: AppDatabase): NoteRepository {
        return NoteRepositoryImpl(db.noteDao)
    }

    @Provides
    @Singleton
    fun provideLockedVideoRepository(db: AppDatabase): LockedVideoRepository {
        return LockedVideoRepositoryImpl(db.lockedVideoDao)
    }

    @Provides
    @Singleton
    fun provideHiddenVideoRepository(db: AppDatabase): HiddenVideoRepository {
        return HiddenVideoRepositoryImpl(db.hiddenVideoDao)
    }

    @Provides
    @Singleton
    fun provideStatusRepository(context: Application): StatusRepository {
        return StatusRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoApi(client: OkHttpClient): VideoApi {
        return Retrofit.Builder()
            .baseUrl("http://143.244.139.153:11500/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VideoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVideoRepository(api: VideoApi, app: Application): VideoRepository {
        return VideoRepositoryImpl(api, app)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(app: Application): MediaRepository {
        return MediaRepositoryImpl(app)
    }

    @Provides
    @Singleton
    fun provideLanguagePreferences(app: Application): com.example.video_downloder.data.local.LanguagePreferences {
        return com.example.video_downloder.data.local.LanguagePreferences(app)
    }

    @Provides
    @Singleton
    fun provideSettingsPreferences(app: Application): com.example.video_downloder.data.local.SettingsPreferences {
        return com.example.video_downloder.data.local.SettingsPreferences(app)
    }
}
