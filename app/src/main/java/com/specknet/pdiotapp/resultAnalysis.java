package com.specknet.pdiotapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateLongClickListener;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.specknet.pdiotapp.bluetooth.ConnectingActivity;
import com.specknet.pdiotapp.live.LiveDataActivity;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class resultAnalysis extends AppCompatActivity implements OnDateSelectedListener, OnMonthChangedListener, OnDateLongClickListener {
    private final String TAG = this.getClass().getName();
    private PieChart pieChart;
    private MaterialCalendarView calendarView;
    private Button history_live, history_record, history_history, history_connect;

    private HashMap<String, Object> activities = new HashMap<String, Object>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private DocumentSnapshot data ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_analysis);

        calendarView = findViewById(R.id.calendar_view);
        calendarView.setOnDateChangedListener(this);
        calendarView.setOnDateLongClickListener(this);
        calendarView.setOnMonthChangedListener(this);

        calendarView.setCurrentDate(CalendarDay.today());
        calendarView.setDateSelected(CalendarDay.today(), true);
        pieChart = findViewById(R.id.pieChart);


        readActivityData(FORMATTER.format(CalendarDay.today().getDate()));

        Log.d(TAG, "ACTIVIT: " + String.valueOf(activities));


        Log.d(TAG, "read database");

        Log.d(TAG, "actin up: " + activities);

        setupMenuBar();

    }

    @Override
    public void onDateSelected(
            @NonNull MaterialCalendarView calendarView,
            @NonNull CalendarDay date,
            boolean selected) {
            Log.d(TAG, FORMATTER.format(date.getDate()));
            activities.clear();

            readActivityData(FORMATTER.format(date.getDate()));
            setPieChart(pieChart);
    }

    @Override
    public void onDateLongClick(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date) {
        final String text = String.format("%s is available", FORMATTER.format(date.getDate()));
//        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        //noinspection ConstantConditions
        Log.d(TAG, "month changed");
    }

    private void setPieChart(PieChart pieChart) {
        int totalTime = 0;
        List<PieEntry> strings = new ArrayList<>();

        for (Map.Entry<String, Object> entry : activities.entrySet()) {
            totalTime +=  ((Long) entry.getValue()).intValue();
        }

        for (Map.Entry<String, Object> entry : activities.entrySet()) {
//            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            float time = 100*((Long) entry.getValue()).intValue() / totalTime ;
            strings.add(new PieEntry(time, entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(strings,"");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.pie_1));
        colors.add(getResources().getColor(R.color.pie_2));
        colors.add(getResources().getColor(R.color.pie_3));
        colors.add(getResources().getColor(R.color.pie_4));
        colors.add(getResources().getColor(R.color.pie_5));
        colors.add(getResources().getColor(R.color.pie_6));
        colors.add(getResources().getColor(R.color.pie_7));
        dataSet.setColors(colors);

        String centerText = "Total time\n"+totalTime+"s";
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(18f);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);

        PieData pieData = new PieData(dataSet);
        pieData.setDrawValues(true);

        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(12f);

        pieChart.setData(pieData);

        pieChart.animateY(1400, Easing.EaseInOutQuad);
        Legend l = pieChart.getLegend();
        l.setFormSize(10f);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setWordWrapEnabled(true);
        l.setXEntrySpace(10f);

        pieChart.invalidate();

        if(activities.isEmpty()){
            Toast.makeText(getApplicationContext(),"No data today",Toast.LENGTH_SHORT).show();
        }

    }

    //TODO
    private void readActivityData(String date){
        //use date to search the activities info and store in the variable 'activities'
        //current date is "2022/11/1"
        //in activities, data structure should be (<String>, <Integer>), e.g.('sitting', 300) means sit for 300 seconds
        //in activities, ('total', xxx) is compulsory
        //example:
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("Data").document(date);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        activities.putAll(document.getData());
                        Log.d(TAG, "act len: " + activities.size());
                        setPieChart(pieChart);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });



    }

    private void setupMenuBar() {
        history_live=findViewById(R.id.history_live_button);
        history_history=findViewById(R.id.history_history_button);
        history_record=findViewById(R.id.history_record_button);
        history_connect=findViewById(R.id.history_ble_button);
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");
        history_live.setTypeface(iconfont);
        history_history.setTypeface(iconfont);
        history_record.setTypeface(iconfont);
        history_connect.setTypeface(iconfont);

        history_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(resultAnalysis.this, ConnectingActivity.class);
                startActivity(intent);
            }
        });
        history_live.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(resultAnalysis.this, LiveDataActivity.class);
                startActivity(intent);
            }
        });
        history_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(resultAnalysis.this, RecordingActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Call when the activity restart.
     */
    @Override
    protected void onRestart() {
        Log.d(TAG, "Activity onRestart");
        super.onRestart();
    }

    /**
     * Call when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        Log.d(TAG, "Activity onDestroy");
        super.onDestroy();
    }






}