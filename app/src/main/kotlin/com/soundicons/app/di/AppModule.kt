package com.soundicons.app.di

import android.content.Context
import androidx.room.Room
import com.soundicons.app.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SoundIconDatabase =
        Room.databaseBuilder(context, SoundIconDatabase::class.java, "sound_icons.db")
            .addMigrations(
                SoundIconDatabase.MIGRATION_3_4,
                SoundIconDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigrationFrom(1, 2)
            .build()

    @Provides @Singleton
    fun provideSoundIconDao(db: SoundIconDatabase): SoundIconDao = db.soundIconDao()

    @Provides @Singleton
    fun provideWidgetMappingDao(db: SoundIconDatabase): WidgetMappingDao = db.widgetMappingDao()

    @Provides @Singleton
    fun provideCategoryDao(db: SoundIconDatabase): CategoryDao = db.categoryDao()
}
