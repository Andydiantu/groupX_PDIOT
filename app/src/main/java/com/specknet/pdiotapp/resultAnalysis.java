package com.specknet.pdiotapp;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import me.nlmartian.silkcal.DatePickerController;
import me.nlmartian.silkcal.DayPickerView;
import me.nlmartian.silkcal.SimpleMonthAdapter;

public class resultAnalysis extends AppCompatActivity implements DatePickerController {
    private final String TAG = this.getClass().getName();
    private PieChart pieChart;
    private DayPickerView calendarView;

    private HashMap<String, Integer> activities = new HashMap<String, Integer>();

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
        activities.put("total", 600);
        activities.put("sitting", 300);
        activities.put("walking", 30);
        activities.put("running", 150);
        activities.put("lyingDown", 120);

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