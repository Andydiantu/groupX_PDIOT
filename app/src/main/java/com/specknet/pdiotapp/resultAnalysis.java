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
import java.util.List;

public class resultAnalysis extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_analysis);

        pieChart = findViewById(R.id.pieChart);
        setPieChart(pieChart);
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

//
//    /**
//     * Call when the activity start.
//     */
//    @Override
//    protected void onStart() {
//        Log.d(TAG, "Activity onStart");
//        super.onStart();
//    }

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