package org.stepic.droid.services

import android.app.DownloadManager
import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.Handler
import com.squareup.otto.Bus
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.MainApplication
import org.stepic.droid.events.steps.StepRemovedEvent
import org.stepic.droid.model.*
import org.stepic.droid.model.Unit
import org.stepic.droid.preferences.UserPreferences
import org.stepic.droid.store.IStoreStateManager
import org.stepic.droid.store.operations.DatabaseFacade
import org.stepic.droid.store.operations.Table
import org.stepic.droid.util.AppConstants
import org.stepic.droid.util.resolvers.IVideoResolver
import org.stepic.droid.web.IApi
import java.io.File
import java.util.*
import javax.inject.Inject

class DeleteService : IntentService("delete_service") {
    @Inject
    lateinit var mSystemDownloadManager: DownloadManager
    @Inject
    lateinit var mUserPrefs: UserPreferences
    @Inject
    lateinit var mBus: Bus
    @Inject
    lateinit var mResolver: IVideoResolver
    @Inject
    lateinit var mApi: IApi
    @Inject
    lateinit var mDb: DatabaseFacade
    @Inject
    lateinit var mStoreStateManager: IStoreStateManager
    @Inject
    lateinit var analytic : Analytic

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MainApplication.component().inject(this)
        super.onStartCommand(intent, flags, startId)
        return Service.START_REDELIVER_INTENT
    }


    override fun onHandleIntent(intent: Intent) {
        val type = intent.getSerializableExtra(AppConstants.KEY_LOAD_TYPE) as? LoadService.LoadTypeKey
        try {
            when (type) {
                LoadService.LoadTypeKey.Course -> {
                    val course = intent.getSerializableExtra(AppConstants.KEY_COURSE_BUNDLE) as? Course
                    val tableType = intent.getSerializableExtra(AppConstants.KEY_TABLE_TYPE) as? Table
                    removeFromDisk(course)
                }
                LoadService.LoadTypeKey.Section -> {
                    val section = intent.getSerializableExtra(AppConstants.KEY_SECTION_BUNDLE) as? Section
                    removeFromDisk(section)
                }
                LoadService.LoadTypeKey.UnitLesson -> {
                    val unit = intent.getSerializableExtra(AppConstants.KEY_UNIT_BUNDLE) as? Unit
                    val lesson = intent.getSerializableExtra(AppConstants.KEY_LESSON_BUNDLE) as? Lesson
                    removeFromDisk(lesson)
                }
                LoadService.LoadTypeKey.Step -> {
                    val step = intent.getSerializableExtra(AppConstants.KEY_STEP_BUNDLE) as? Step
                    removeFromDisk(step)
                }
            }
        } catch (ex: NullPointerException) {
            //possibly user click clear cache;
            //            throw ex;
            analytic.reportError(Analytic.Error.DELETE_SERVICE_ERROR, ex)
            mDb.dropDatabase()
        }

    }

    private fun removeFromDisk(step: Step?) {
        step?.block?.video?.let {
            val path = mDb.getPathToVideoIfExist(it)
            var file = File(path)
            if (file.exists()) {
                file.delete()
            }

            //delete png thumbnail
            file = File(path + AppConstants.THUMBNAIL_POSTFIX_EXTENSION)
            if (file.exists()) {
                file.delete()
            }

            mDb.deleteVideo(it)
        }
        step?.let {
            step.is_cached = false
            step.is_loading = false
            mDb.updateOnlyCachedLoadingStep(step)
//            mDb.deleteStep(step) // remove steps FIXME: MAYBE NOT DELETE STEP?
            mStoreStateManager.updateStepAfterDeleting(step)
            val mainHandler = Handler(MainApplication.getAppContext().mainLooper)
            mainHandler.post { mBus.post(StepRemovedEvent(step.id)) }
        }
    }

    private fun removeFromDisk(lesson: Lesson?) {
        lesson?.let {
            val steps = mDb.getStepsOfLesson(lesson.id)
            for (step in steps) {
                removeFromDisk(step)
            }
        }
    }

    private fun removeFromDisk(section: Section?) {
        section?.let {
            val units = mDb.getAllUnitsOfSection(section.id)
            val lessons = ArrayList<Lesson>()
            for (unit in units) {
                val lesson = mDb.getLessonOfUnit(unit)
                if (lesson != null) {
                    lessons.add(lesson)
                }
            }

            for (lesson in lessons) {
                removeFromDisk(lesson)
            }
        }
    }

    private fun removeFromDisk(course: Course?) {
        course?.let {
            val sections = mDb.getAllSectionsOfCourse(course)
            for (section in sections) {
                removeFromDisk(section)
            }
        }
    }
}
