package com.specknet.pdiotapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
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
import com.specknet.pdiotapp.bluetooth.ConnectingActivity;
import com.specknet.pdiotapp.live.LiveDataActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.nlmartian.silkcal.DatePickerController;
import me.nlmartian.silkcal.DayPickerView;
import me.nlmartian.silkcal.SimpleMonthAdapter;

public class resultAnalysis extends AppCompatActivity implements DatePickerController {
    private final String TAG = this.getClass().getName();
    private PieChart pieChart;
    private DayPickerView calendarView;
    private Button history_live, history_record, history_history, history_connect;

    private HashMap<String, Object> activities = new HashMap<String, Object>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_analysis);

        calendarView = findViewById(R.id.calendar_view);
        calendarView.setController(this);

        readActivityData("09-11-2022");

        Log.d(TAG,"read database");

        activities.put("sitting", 300);
        activities.put("running", 150);
        activities.put("lyingDown", 120);
        activities.put("walking", 30);

        pieChart = findViewById(R.id.pieChart);
        setPieChart(pieChart);

        setupMenuBar();

        }

    @Override
    public int getMaxYear() {
        return 0;
    }

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {

    }

    @Override
    public void onDateRangeSelected(SimpleMonthAdapter.SelectedDays<SimpleMonthAdapter.CalendarDay> selectedDays) {

    }

    private void setPieChart(PieChart pieChart) {
        int totalTime = 0;
        List<PieEntry> strings = new ArrayList<>();

        for (Map.Entry<String, Object> entry : activities.entrySet()) {
            totalTime += (Integer) entry.getValue();
        }

        for (Map.Entry<String, Object> entry : activities.entrySet()) {
//            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            float time = 100*(Integer)entry.getValue() / totalTime ;
            strings.add(new PieEntry(time, entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(strings,"");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        String centerText = "Total time\n"+totalTime+" s";
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(18f);

        PieData pieData = new PieData(dataSet);
        pieData.setDrawValues(true);

        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(12f);

        pieChart.setData(pieData);
        pieChart.invalidate();

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
                        for (Map.Entry<String, Object> entry : document.getData().entrySet()) {
                            activities.put(entry.getKey(), entry.getValue());
                        }
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