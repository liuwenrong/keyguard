package com.example.zhangzhenqiu.ydlock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

//该类主要用来管理在锁屏界面上显示时间(包括12小时制下显示上午和下午)和日期。
public class StatusViewManager {

    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";

    private TextView mDateView;
    private TextView mTimeView;
    public TextView mArtistView;
    public TextView mMusicView;

    private String mDateFormat;
    private String mFormat;

    private static Activity mActivity;
    private static View mView;
    private AmPm mAmPm;
    private Calendar mCalendar;
    public ContentObserver mFormatChangeObserver;
    public BroadcastReceiver mIntentReceiver;

    private final Handler mHandler = new Handler();

    private static Context mContext;


    public StatusViewManager(Activity activity, Context context) {
        mContext = context;
        mActivity = activity;
        initViews();
        refreshDate();
    }

    public StatusViewManager(View view, Context context) {
        mContext = context;
        mView = view;
        initViews();
        refreshDate();
    }

    private View findViewById(int id) {
        if (mActivity != null) {
            return mActivity.findViewById(id);
        } else if (mView != null) {
            return mView.findViewById(id);
        }
        return null;
    }

    private void refreshDate() {
        if (mDateView != null) {
            //锁屏界面显示日期
            mDateView.setText(DateFormat.format(mDateFormat, new Date()));
        }
    }

    private String getString(int id) {

        return mContext.getString(id);

    }

    class AmPm {
        private TextView mAmPmTextView;
        private String mAmString, mPmString;

        AmPm(Typeface tf) {
            mAmPmTextView = (TextView) findViewById(R.id.am_pm);
            if (mAmPmTextView != null && tf != null) {
                //设置显示的上午、下午字体风格
                mAmPmTextView.setTypeface(tf);
            }

            //获取显示上午、下午的字符串数组
            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
        }

        void setShowAmPm(boolean show) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        void setIsMorning(boolean isMorning) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setText(isMorning ? mAmString : mPmString);
            }
        }
    }

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<StatusViewManager> mStatusViewManager;
        //private Context mContext;

        public TimeChangedReceiver(StatusViewManager status) {
            mStatusViewManager = new WeakReference(status);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Post a runnable to avoid blocking the broadcast.
            final boolean timezoneChanged =
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            final StatusViewManager status = mStatusViewManager.get();
            if (status != null) {
                status.mHandler.post(new Runnable() {
                    public void run() {
                        if (timezoneChanged) {
                            status.mCalendar = Calendar.getInstance();
                        }
                        status.updateTime();
                    }
                });
            } else {

                mContext.unregisterReceiver(this);

            }
        }
    }


    /*监听URI为Settings.System.CONTENT_URI的数据变化，即12小时制还是24小时制
     * 的变化(一般来自用户在设置里对时间显示的设置)
     */
    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<StatusViewManager> mStatusViewManager;

        //private Context mContext;
        public FormatChangeObserver(StatusViewManager status) {
            super(new Handler());
            //创建保存在弱应用中的StatusViewManager对象
            mStatusViewManager = new WeakReference(status);
        }

        @Override
        public void onChange(boolean selfChange) {
            StatusViewManager mStatusManager = mStatusViewManager.get();
            if (mStatusManager != null) {
                mStatusManager.setDateFormat();
                mStatusManager.updateTime();
            } else {
                mContext.getContentResolver().unregisterContentObserver(this);
            }
        }
    }

    //更新时间
    private void updateTime() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        CharSequence newTime = DateFormat.format(mFormat, mCalendar);
        mTimeView.setText(newTime);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == Calendar.ERA);
    }

    //设置时间显示格式，如果时间显示为12小时制，则显示上午、下午
    private void setDateFormat() {
        mFormat = android.text.format.DateFormat.is24HourFormat(mContext)
                ? M24 : M12;
        mAmPm.setShowAmPm(mFormat.equals(M12));
    }


    public void registerComponent() {
        // TODO Auto-generated method stub
        Log.d("MainActivity", "registerComponent()");

        if (mIntentReceiver == null) {
            mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }

        if (mFormatChangeObserver == null) {
            mFormatChangeObserver = new FormatChangeObserver(this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        updateTime();
    }

    public void unregisterComponent() {
        // TODO Auto-generated method stub
        Log.d("MainActivity", "unregisterComponent()");
        if (mIntentReceiver != null) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        if (mFormatChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(
                    mFormatChangeObserver);
        }
        mFormatChangeObserver = null;
        mIntentReceiver = null;
    }


    public void initViews() {
        // TODO Auto-generated method stub
        mDateView = (TextView) findViewById(R.id.date);
        mDateFormat = getString(R.string.month_day_year);
        mTimeView = (TextView) findViewById(R.id.time);

    	/*创建AmPm对象，参数为设置的字体风格(如可设为Typeface.DEFAULT_BOLD粗体)，
    	 * 此处参数为空，默认情况。
    	 */
        mAmPm = new AmPm(null);
        //获取mCalendar对象
        mCalendar = Calendar.getInstance();

        setDateFormat();
        //注册监听
        registerComponent();
    }

}
