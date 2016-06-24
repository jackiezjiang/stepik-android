package org.stepic.droid.view.abstraction;

import org.stepic.droid.events.joining_course.FailJoinEvent;
import org.stepic.droid.events.joining_course.SuccessJoinEvent;

public interface CourseJoinView {
    void showProgress();
    void setEnabledJoinButton (boolean isEnabled);
    void onFailJoin(FailJoinEvent e);
    void onSuccessJoin(SuccessJoinEvent e);
}
