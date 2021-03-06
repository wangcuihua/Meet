package com.example.meet.fragment;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import android.renderscript.Sampler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;


import com.example.meet.R;
import com.example.meet.activity.EditTaskActivity;
import com.example.meet.activity.MainActivity;
import com.example.meet.bean.Task;
import com.example.meet.bean.TaskLab;
import com.example.meet.provider.TaskAdapter;
import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarLayout;
import com.haibin.calendarview.CalendarView;

import org.litepal.LitePal;
import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.meet.R.color.foreground_material_dark;
import static com.example.meet.R.color.item_checked;


public class TaskFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "TaskFragment";
    private View rootView;
    private MainActivity mActivity;
    private CalendarView.OnCalendarSelectListener CalendarSelectListener; //实例化接口
    private CalendarView.OnYearChangeListener YearChangeListener;
    private CalendarView mCalenderView;
    private CalendarLayout mCalendarLayout;
    private TextView mTextMonthDay;
    private TextView mTextYear;
    private TextView mTextLunar;//农历日期
    private TextView mTextCurrentDay;
    private RelativeLayout mRelativeTool;
    private int mYear;
    private RecyclerView mRecyclerView;
    private FloatingActionButton addFab;
    private List<Task> mTaskList;
    private TaskAdapter mTaskAdapter;
    private String mSelectTime;
    private ExecutorService mSingleThreadPool = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler();
    private OnFragmentInteractionListener mListener;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Map<String,Calendar> map = new HashMap<>();


    //当与activity建立联系时调用
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
        CalendarSelectListener = (CalendarView.OnCalendarSelectListener) context;
        YearChangeListener = (CalendarView.OnYearChangeListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_task, container, false);
        try {
            initCalenderView(rootView);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        addFab = rootView.findViewById(R.id.fab);
        addFab.setOnClickListener(this);

        //Calendar点击事件
        mCalenderView.setOnCalendarSelectListener(new CalendarView.OnCalendarSelectListener() {
            @Override
            public void onCalendarOutOfRange(Calendar calendar) {

            }

            @Override
            public void onCalendarSelect(Calendar calendar, boolean isClick) {
                mTextLunar.setVisibility(View.VISIBLE);
                mTextYear.setVisibility(View.VISIBLE);
                mTextMonthDay.setText(calendar.getMonth() + "月" + calendar.getDay() + "日");
                mTextLunar.setText(calendar.getLunar());
                mYear = calendar.getYear();
                String time = mYear+"-"+calendar.getMonth()+"-"+calendar.getDay() ;
                try {
                    mSelectTime = getmSelectTime(time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "mSelectTime:" + mSelectTime);
                //查找选中日期下的任务列表
                mSingleThreadPool.execute(updateUIRunnable);

            }
        });


        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();
        mSingleThreadPool.execute(updateUIRunnable);
    }


    //线程查找数据库
    private Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            mTaskList = LitePal.where("toDoTime = ?", String.valueOf(mSelectTime)).find(Task.class);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTaskAdapter = null;
                    mTaskAdapter = new TaskAdapter(mTaskList, getActivity());
                    //点击编辑、长按删除
                    mTaskAdapter.setOnItemClickListener(new TaskAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(TaskAdapter.ViewHolder vh, int position) {
                            //点击进入编辑页面
                            Task task = mTaskList.get(position);
                            Intent intent = new Intent(mActivity,EditTaskActivity.class);
                            intent.putExtra("taskId",task.getId());
                            startActivity(intent);
                        }

                        @Override
                        public void onItemLongClick(TaskAdapter.ViewHolder vh, int position) {

                        }

                        @SuppressLint("ResourceAsColor")
                        @Override
                        public void onCheckBoxClick(TaskAdapter.ViewHolder vh, int position, boolean isChecked) {
                            Log.d(TAG,"listSize"+"--- "+mTaskList.size());
                            Log.d(TAG,"onClickedPosition"+"--- "+position);
                            Task task = mTaskList.get(position);
//                            task.setFinish(isChecked);
                            TextView contentView = vh.contentView;
                            if (isChecked) {
//                                mTaskAdapter.removeTask(position);
//                                mTaskAdapter.addTask(task);
                                task.setFinish(true);
                                contentView.setPaintFlags(contentView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                contentView.setTextColor(R.color.item_checked);


                            } else {
                                task.setFinish(false);
                                contentView.setPaintFlags(contentView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                                contentView.setTextColor(Color.BLACK);
                            }
                            TaskLab.get(getActivity()).updateTask(task);
                            for(Task t : mTaskList) {
                                Log.d(TAG,"listContent"+"--- "+t.getContent());
                            }
                        }
                    });
                    mRecyclerView.setAdapter(mTaskAdapter);

                    initCalenderDate();
                }
            });
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public TaskFragment() {
        // Required empty public constructor
    }


    public static TaskFragment newInstance(String param1, String param2) {
        TaskFragment fragment = new TaskFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * @param view 初始化月历视图
     */
    private void initCalenderView(View view) throws ParseException {
        mTextMonthDay = view.findViewById(R.id.tv_month_day);
        mTextYear = view.findViewById(R.id.tv_year);
        mTextLunar = view.findViewById(R.id.tv_lunar);
        mTextCurrentDay = view.findViewById(R.id.tv_current_day);
        mRelativeTool = view.findViewById(R.id.rl_tool);
        mCalenderView = view.findViewById(R.id.calendarView);
        mCalendarLayout = view.findViewById(R.id.calendarLayout);
        mTextMonthDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCalendarLayout.isExpand()) {
                    mCalendarLayout.expand();
                    return;
                }
                mCalenderView.showYearSelectLayout(mYear);
                mTextLunar.setVisibility(View.GONE);
                mTextYear.setVisibility(View.GONE);
                mTextMonthDay.setText(String.valueOf(mYear));

            }
        });

        //点击右上角滚动到当前
        view.findViewById(R.id.fl_current).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalenderView.scrollToCurrent();
            }
        });

        mCalenderView.setOnYearChangeListener(new CalendarView.OnYearChangeListener() {
            @Override
            public void onYearChange(int year) {
                mTextMonthDay.setText(String.valueOf(year));
            }
        });

        mTextYear.setText(String.valueOf(mCalenderView.getCurYear()));
        mYear = mCalenderView.getCurYear();
        mTextMonthDay.setText(mCalenderView.getCurMonth() + "月" + mCalenderView.getCurDay() + "日");
        mTextLunar.setText("今日");
        mTextCurrentDay.setText(String.valueOf(mCalenderView.getCurDay()));

        String time = mCalenderView.getCurYear()  +"-"+ mCalenderView.getCurMonth()+"-"+ mCalenderView.getCurDay();
        mSelectTime = getmSelectTime(time);

        Log.d(TAG, "today:-" + mSelectTime);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                showAddDialog();//添加task的编辑框
                break;
            default:
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    /**
     * 点击添加按钮弹出对话框
     */
    private void showAddDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
        View addTaskDialog = layoutInflater.inflate(R.layout.add_dialog_2, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(addTaskDialog);
        builder.setTitle("添加新任务");

        final EditText addText = addTaskDialog.findViewById(R.id.add_task_text);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //添加任务
                Task task = new Task(addText.getText().toString());
                task.setCreateTime(System.currentTimeMillis());
                task.setToDoTime(mSelectTime);
                TaskLab.get(getActivity()).addTask(task);
                Log.d(TAG, "onClick: task:"+task.getToDoTime()+task.getContent());
                mSingleThreadPool.execute(updateUIRunnable);
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.create();
        builder.show();

    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftKeyWord() {
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

    }

    //将选中日期转换
    private String getmSelectTime(String calenderDate) throws ParseException {
        Date date = simpleDateFormat.parse(calenderDate);
        String selectDate = simpleDateFormat.format(date);
        return selectDate;
    }

    // //绘制含有task的日期
    private void initCalenderDate() {
        //去重后任务日期集合
        List<Task> tasks = LitePal.select("toDoTime").find(Task.class);
        List<String> dates = new ArrayList<>();
        for (Task t : tasks){
            dates.add(t.getToDoTime());
        }
        LinkedHashSet<String> set = new LinkedHashSet<String>(dates.size());
        set.addAll(dates);
        dates.clear();
        dates.addAll(set);

        for (String ss : dates) {
            String date[] = ss.split("-");
            map.put(ss.replace("-",""), getSchemeCalendar(Integer.parseInt(date[0]),Integer.parseInt(date[1]),
                    Integer.parseInt(date[2]),getProgress(ss)));
        }

        mCalenderView.setSchemeDate(map);
    }

    //计算完成进度
    private String getProgress(String time) {
        List<Task> tasks = LitePal.select("isFinish").where("toDoTime = ?",time ).find(Task.class);
        Log.d(TAG, "getProgress: "+tasks.size());
        int done = 0;
        for (Task t : tasks) {
            if (t.isFinish()){
                done++;
            }
        }
        String progress =String.valueOf(done * 100/tasks.size());
        return progress;
    }

    /**
     * @param mYear
     * @param mMonth
     * @param day
     * @param progress   事务完成的进度
     * @return
     */
    private Calendar getSchemeCalendar(int mYear,int mMonth,int day,String progress){
        Calendar calendar = new Calendar();
        calendar.setYear(mYear);
        calendar.setMonth(mMonth);
        calendar.setDay(day);
        calendar.setScheme(progress);
        return calendar;
    }
}
