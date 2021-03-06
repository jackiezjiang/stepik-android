package org.stepic.droid.ui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import org.jetbrains.annotations.Nullable;
import org.stepic.droid.R;
import org.stepic.droid.base.MainApplication;
import org.stepic.droid.configuration.IConfig;
import org.stepic.droid.core.IShell;
import org.stepic.droid.model.Course;
import org.stepic.droid.store.CleanManager;
import org.stepic.droid.store.IDownloadManager;
import org.stepic.droid.store.operations.DatabaseFacade;
import org.stepic.droid.store.operations.Table;
import org.stepic.droid.util.HtmlHelper;
import org.stepic.droid.util.StepicLogicHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyCoursesAdapter extends ArrayAdapter<Course> {

    @Inject
    IShell mShell;

    @Inject
    IConfig mConfig;

    @Inject
    DatabaseFacade mDatabase;

    @Inject
    IDownloadManager mDownloadManager;

    @Inject
    CleanManager mCleaner;

    private Drawable coursePlaceholder;

    @Nullable
    private final Table type;
    private LayoutInflater mInflater;

    public MyCoursesAdapter(Fragment fragment, List<Course> courses, @Nullable Table type) {
        super(fragment.getActivity(), 0, courses);
        this.type = type;
        mInflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        MainApplication.component().inject(this);
        coursePlaceholder = ContextCompat.getDrawable(fragment.getContext(), R.drawable.ic_course_placeholder);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Course course = getItem(position);

        View view = convertView;
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolderItem viewHolderItem = null;
        if (view == null) {
            view = mInflater.inflate(R.layout.course_item, null);
            viewHolderItem = new ViewHolderItem(view);
            view.setTag(viewHolderItem);
        } else {
            viewHolderItem = (ViewHolderItem) convertView.getTag();
        }
        viewHolderItem.courseName.setText(course.getTitle());
        viewHolderItem.courseSummary.setText(HtmlHelper.fromHtml(course.getSummary()));

        Glide
                .with(getContext())
                .load(StepicLogicHelper.getPathForCourseOrEmpty(course, mConfig))
                .placeholder(coursePlaceholder)
                .centerCrop()
                .into(viewHolderItem.imageViewTarget);
        return view;
    }

    static class ViewHolderItem {

        @BindView(R.id.course_name)
        TextView courseName;

        @BindView(R.id.course_info)
        TextView courseSummary;

        @BindView(R.id.course_icon)
        ImageView courseIcon;

        @BindView(R.id.course_date_interval)
        TextView courseDateInterval;

        @BindView(R.id.cv)
        View cardView;

        GlideDrawableImageViewTarget imageViewTarget;

        public ViewHolderItem(View view) {
            ButterKnife.bind(this, view);
            imageViewTarget = new GlideDrawableImageViewTarget(courseIcon);
        }
    }
}
