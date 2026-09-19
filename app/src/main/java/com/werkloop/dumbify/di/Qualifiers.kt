package com.werkloop.dumbify.di

import javax.inject.Qualifier

/**
 * The process-lifetime scope the repository's hot state flow lives in. Named so
 * it cannot be confused with a ViewModel scope, which dies with its screen.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
