package com.example.ui

import android.app.job.JobParameters
import android.app.job.JobService

/** No network or sound: repair scheduling and request a permitted countdown restart. */
class BackgroundRecoveryJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        BackgroundServices.performMaintenance(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
