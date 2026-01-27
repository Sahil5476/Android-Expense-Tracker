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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StatsFragment extends Fragment {

    // UI Components
    private TabLayout tabLayout;
    private MaterialButtonToggleGroup toggleGroupTime;
    private LineChart lineChart;
    private RecyclerView recyclerStats;
    private TransactionAdapter adapter;

    // Helpers
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Current State Variables
    private String currentCategory = "Expense"; // Default Category
    private String currentTimeFilter = "MONTH"; // Default Time Filter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        // 1. Initialize Views
        tabLayout = view.findViewById(R.id.tabLayoutStats);
        toggleGroupTime = view.findViewById(R.id.toggleGroupTime);
        lineChart = view.findViewById(R.id.lineChart);
        recyclerStats = view.findViewById(R.id.recyclerStats);

        // 2. Setup RecyclerView
        recyclerStats.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize empty adapter first
        adapter = new TransactionAdapter(getContext(), new ArrayList<>(), null);
        adapter.setReadOnly(true); // Hide delete buttons in stats
        recyclerStats.setAdapter(adapter);

        // 3. Listener: Category Tabs (Expense, Investment, Loan)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentCategory = "Expense"; break;
                    case 1: currentCategory = "Investment"; break;
                    case 2: currentCategory = "Loan"; break;
                }
                loadData(); // Reload data with new category + existing time filter
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 4. Listener: Time Filters (Weekly, Monthly, Yearly)
        toggleGroupTime.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeek) currentTimeFilter = "WEEK";
                else if (checkedId == R.id.btnMonth) currentTimeFilter = "MONTH";
                else if (checkedId == R.id.btnYear) currentTimeFilter = "YEAR";

                loadData(); // Reload data with new time filter + existing category
            }
        });

        // 5. Initial Data Load
        loadData();

        return view;
    }

    // --- MAIN DATA LOADING LOGIC ---
    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // A. Fetch All Transactions
            List<Transaction> allTransactions = AppDatabase.getDatabase(getContext()).transactionDao().getAllTransactions();

            // B. Apply Time Filter First
            List<Transaction> dateFiltered = filterByDate(allTransactions, currentTimeFilter);

            // C. Apply Category Filter Second
            List<Transaction> finalFiltered = filterByCategory(dateFiltered, currentCategory);

            // D. Prepare Chart Data (Simple Index-based plotting)
            List<Entry> chartEntries = new ArrayList<>();
            for (int i = 0; i < finalFiltered.size(); i++) {
                float val = (float) finalFiltered.get(i).amount;
                chartEntries.add(new Entry(i, val));
            }

            // E. Update UI on Main Thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Update List
                    adapter = new TransactionAdapter(getContext(), finalFiltered, null);
                    adapter.setReadOnly(true);
                    recyclerStats.setAdapter(adapter);

                    // Update Chart
                    updateChart(chartEntries);
                });
            }
        });
    }

    // --- Helper: Update Chart Styling ---
    private void updateChart(List<Entry> entries) {
        if (entries.isEmpty()) {
            lineChart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, currentCategory + " Trend");

        // Dynamic Coloring based on Category
        int color;
        if (currentCategory.equals("Expense")) color = Color.parseColor("#E53935"); // Red
        else if (currentCategory.equals("Investment")) color = Color.parseColor("#1976D2"); // Blue
        else color = Color.parseColor("#43A047"); // Green (Loan)

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false); // Hide numbers on dots for cleaner look
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth Curves
        dataSet.setDrawFilled(true); // Fill area under line
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(50); // Slight transparency

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Graph Styling
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.animateY(800); // Animate chart
        lineChart.invalidate();  // Refresh view
    }

    // --- Helper: Filter Logic for Date ---
    private List<Transaction> filterByDate(List<Transaction> list, String type) {
        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Calendar transactionCal = Calendar.getInstance();

        // Reset cal to current time
        cal.setTime(new Date());

        // Subtract time based on filter
        if (type.equals("WEEK")) cal.add(Calendar.DAY_OF_YEAR, -7);
        else if (type.equals("MONTH")) cal.add(Calendar.MONTH, -1);
        else if (type.equals("YEAR")) cal.add(Calendar.YEAR, -1);

        for (Transaction t : list) {
            try {
                Date d = sdf.parse(t.date);
                if (d != null) {
                    transactionCal.setTime(d);
                    // Include if transaction date is AFTER the cutoff date
                    if (transactionCal.after(cal)) {
                        result.add(t);
                    }
                }
            } catch (ParseException e) { e.printStackTrace(); }
        }
        return result;
    }

    // --- Helper: Filter Logic for Category ---
    private List<Transaction> filterByCategory(List<Transaction> list, String category) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : list) {
            if (t.type == null) continue;

            if (category.equals("Expense") && t.type.equals("Expense")) {
                result.add(t);
            } else if (category.equals("Investment") && t.type.equals("Investment")) {
                result.add(t);
            } else if (category.equals("Loan")) {
                // "Loan" tab shows both Liabilities (Borrowed) and Assets (Lent)
                if (t.type.equals("Liability") || t.type.equals("Asset")) {
                    result.add(t);
                }
            }
        }
        return result;
    }
}