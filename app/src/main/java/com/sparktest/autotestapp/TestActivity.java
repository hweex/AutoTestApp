package com.sparktest.autotestapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sparktest.autotestapp.framework.Test;
import com.sparktest.autotestapp.framework.TestCase;
import com.sparktest.autotestapp.framework.TestRunner;
import com.sparktest.autotestapp.framework.TestState;
import com.sparktest.autotestapp.framework.TestSuite;
import com.sparktest.autotesteapp.R;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

public class TestActivity extends Activity {

    public ListView mListView;

    public Handler mHandler;
    public List<TestSuite> mSuites;
    private static final int FINISH = 1;

    @Inject
    public TestRunner mRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        mListView = findViewById(R.id.testcaseListView);
        Handler.Callback callback = (msg) -> {
            if (msg.what == FINISH) update();
            return true;
        };
        mHandler = new Handler(callback);

        ObjectGraph objectGraph = ObjectGraph.create(new TestModule(this));
        objectGraph.inject(this);
        Log.d("TestActivity", mRunner.getClass().getName());
        ((AppTestRunner) mRunner).setInjector(objectGraph);

        mSuites = new ArrayList<>();

        TestCaseAdapter adapter = new TestCaseAdapter(this, mSuites);
        mListView.setAdapter(adapter);

        android.media.AudioManager am = (android.media.AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //requestPermissions();
        new Handler(Looper.myLooper()).postDelayed(() -> requestPermissions(), 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public void requestPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int permissionAudio = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionCamera != PackageManager.PERMISSION_GRANTED
                || permissionAudio != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }


    public void update() {
        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    public void runTest(View v) {
        int pos = mListView.getPositionForView(v);
        Test test = mSuites.get(pos);
        if (test instanceof TestSuite) {
            ViewGroup parent = (ViewGroup) v.getParent();
            ViewGroup parent_parent = (ViewGroup) parent.getParent();
            int index = parent_parent.indexOfChild(parent);
            mRunner.run((TestCase) ((TestSuite) test).get(index));
        } else {
            mRunner.run((TestCase) test);
        }

        this.update();
        moveToSuite(v);
    }

    public void moveToSuite(View v) {
        int pos = mListView.getPositionForView(v);
        mListView.smoothScrollToPositionFromTop(pos, 0, 500);
    }

    private class TestCaseAdapter extends ArrayAdapter<TestSuite> {

        public TestCaseAdapter(Context context, List<TestSuite> objects) {
            super(context, R.layout.listview_testsuite, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TestSuite testSuite = getItem(position);

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_testsuite, null);
            String desc = String.format("%d", position);
            convertView.setContentDescription(desc);
            desc = String.format("%d. %s", position + 1, testSuite.getDescription());
            ((TextView) convertView.findViewById(R.id.textView)).setText(desc);
            LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.subListView);
            for (TestCase t : testSuite.cases()) {
                View child = getLayoutInflater().inflate(R.layout.listview_testcase, null);
                desc = String.format("%d", testSuite.cases().indexOf(t));
                child.setContentDescription(desc);
                ViewHolder holder = ViewHolder.createInstance(child);
                ViewHolder.updateViewHolder(holder, t);
                layout.addView(child);
            }
            return convertView;
        }
    }

    static class ViewHolder {
        Button button;
        TextView textView;
        ProgressBar progress;
        View indicator;

        static ViewHolder createInstance(View root) {
            ViewHolder holder = new ViewHolder();

            holder.button = (Button) root.findViewById(R.id.run);
            holder.textView = (TextView) root.findViewById(R.id.textView);
            holder.progress = (ProgressBar) root.findViewById(R.id.progressBar);
            holder.indicator = root.findViewById(R.id.indicator);

            return holder;
        }

        static void updateViewHolder(ViewHolder holder, TestCase testCase) {
            holder.textView.setText(testCase.getDescription());
            holder.textView.setContentDescription(testCase.getTestClass().getName());

            TestState state = testCase.getState();
            switch (state) {
                case NotRun:
                    holder.progress.setVisibility(View.INVISIBLE);
                    holder.button.setText("RUN");
                    holder.indicator.setBackgroundResource(android.R.drawable.presence_away);
                    break;
                case Running:
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.button.setEnabled(false);
                    holder.button.setText("RUNNING");
                    holder.indicator.setBackgroundResource(android.R.drawable.presence_online);
                    break;
                case Success:
                    holder.progress.setVisibility(View.INVISIBLE);
                    holder.button.setEnabled(true);
                    holder.button.setTextColor(Color.parseColor("#008800"));
                    holder.button.setText("PASSED");
                    holder.indicator.setBackgroundResource(android.R.drawable.presence_online);
                    break;
                case Failed:
                    holder.progress.setVisibility(View.INVISIBLE);
                    holder.button.setEnabled(true);
                    holder.button.setText("FAILED");
                    holder.button.setTextColor(Color.parseColor("#880000"));
                    holder.indicator.setBackgroundResource(android.R.drawable.presence_busy);
                    break;
            }
        }
    }
}
