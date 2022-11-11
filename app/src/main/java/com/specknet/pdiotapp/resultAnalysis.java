package com.specknet.pdiotapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private HashMap<String, Object> activities = new HashMap<String, Object>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_analysis);

        calendarView = findViewById(R.id.calendar_view);
        calendarView.setController(this);

        readActivityData("2022/11/1");

        pieChart = findViewById(R.id.pieChart);
        setPieChart(pieChart);

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

        List<PieEntry> strings = new ArrayList<>();
        strings.add(new PieEntry(15f,"sitting"));
        strings.add(new PieEntry(35f,"Walking"));
        strings.add(new PieEntry(40f,"Running"));
        strings.add(new PieEntry(20f,"LyingDown"));

        PieDataSet dataSet = new PieDataSet(strings,"Label");

        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(getResources().getColor(R.color.red));
        colors.add(getResources().getColor(R.color.light_blue));
        colors.add(getResources().getColor(R.color.lime));
        colors.add(getResources().getColor(R.color.grey));
        dataSet.setColors(colors);

        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);

        String centerText = "Total time\n"+"3min";
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