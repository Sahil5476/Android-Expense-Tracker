package com.example.expense_tracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    private TabLayout tabLayout;
    private LineChart lineChart;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> allTransactions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutStats);
        lineChart = view.findViewById(R.id.lineChart);
        recyclerView = view.findViewById(R.id.recyclerStats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load data initially
        loadDataAndFilter("Expense");

        // Listen for Tab Changes
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String type = "Expense";
                if (tab.getPosition() == 1) type = "Investment";
                else if (tab.getPosition() == 2) type = "Liability"; // Or "Loan" depending on your DB string

                loadDataAndFilter(type);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadDataAndFilter(String filterType) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Get All Data
            allTransactions = AppDatabase.getDatabase(getContext()).transactionDao().getAllTransactions();

            // 2. Filter List based on Tab (Expense/Investment/Loan)
            List<Transaction> filteredList = new ArrayList<>();
            for (Transaction t : allTransactions) {
                if (t.type.equalsIgnoreCase(filterType)) {
                    filteredList.add(t);
                }
                // Handle "Asset" as Loan(Cr) if you want them in the Loan tab
                if (filterType.equals("Liability") && t.type.equals("Asset")) {
                    filteredList.add(t);
                }
            }

            // 3. Prepare Graph Data (Group by Month)
            updateGraph(filteredList, filterType);

            // 4. Update List (Main Thread)
            getActivity().runOnUiThread(() -> {
                // Pass 'null' for the delete listener so nothing happens on click
                adapter = new TransactionAdapter(getContext(), filteredList, null);

                // IMPORTANT: Enable Read-Only mode
                adapter.setReadOnly(true);

                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void updateGraph(List<Transaction> transactions, String label) {
        // Map to store Month -> Total Amount
        // TreeMap keeps keys sorted (Jan, Feb, Mar...)
        Map<String, Float> monthMap = new TreeMap<>();
        List<String> xLabels = new ArrayList<>();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // 1. Aggregate Data
        for (Transaction t : transactions) {
            // Convert timestamp to Date to get Month
            cal.setTimeInMillis(t.timestamp);
            String monthName = monthFormat.format(cal.getTime());

            // Add amount to existing month total
            float currentTotal = monthMap.getOrDefault(monthName, 0f);
            monthMap.put(monthName, currentTotal + (float) t.amount);

            if (!xLabels.contains(monthName)) {
                xLabels.add(monthName);
            }
        }

        // 2. Convert to Entry List for Chart
        List<Entry> entries = new ArrayList<>();
        int index = 0;
        for (String month : xLabels) {
            if (monthMap.containsKey(month)) {
                entries.add(new Entry(index, monthMap.get(month)));
            }
            index++;
        }

        // 3. Setup Chart UI (Main Thread)
        getActivity().runOnUiThread(() -> {
            if (entries.isEmpty()) {
                lineChart.clear(); // Clear if empty
                return;
            }

            LineDataSet dataSet = new LineDataSet(entries, label);
            dataSet.setColor(Color.parseColor("#6200EE"));
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(Color.parseColor("#6200EE"));
            dataSet.setCircleRadius(4f);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Curved lines
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#D1C4E9")); // Light purple fill

            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);

            // X-Axis Formatting (Show Month Names)
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);

            // General Chart Styling
            lineChart.getDescription().setEnabled(false);
            lineChart.getAxisRight().setEnabled(false);
            lineChart.animateY(1000); // Animation
            lineChart.invalidate(); // Refresh
        });
    }
}