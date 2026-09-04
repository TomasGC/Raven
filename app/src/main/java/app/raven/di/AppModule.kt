package app.raven.di

import android.content.Context
import app.raven.core.PuzzleModule
import app.raven.placeholder.PlaceholderModule
import app.raven.util.hasOverlayPermission
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideHasOverlayPermission(@ApplicationContext context: Context): () -> Boolean =
        { hasOverlayPermission(context) }

    @Provides
    fun providePuzzleModules(): List<PuzzleModule> = listOf(PlaceholderModule)
}
