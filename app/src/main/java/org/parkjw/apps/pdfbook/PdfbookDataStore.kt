package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.pdfbookDataStore by preferencesDataStore("pdfbook")
