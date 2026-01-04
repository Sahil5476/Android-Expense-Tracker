package com.example.expense_tracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LoanAssetFragment extends Fragment {

    // 1. Declare UI Variables
    private TextInputEditText etBorrower, etAmount, etRate, etStartDate, etEndDate;
    private TextView tvTotalInterest, tvFinalAmount;
    private Button btnSave;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loan_asset, container, false);

        // 2. Initialize Calendar
        calendar = Calendar.getInstance();

        // 3. Link Java Variables to XML IDs
        etBorrower = view.findViewById(R.id.etBorrowerName);
        etAmount = view.findViewById(R.id.etAssetAmount);
        etRate = view.findViewById(R.id.etAssetInterestRate);
        etStartDate = view.findViewById(R.id.etAssetStartDate);
        etEndDate = view.findViewById(R.id.etAssetEndDate);

        // Summary Card Views
        tvTotalInterest = view.findViewById(R.id.tvAssetTotalInterest);
        tvFinalAmount = view.findViewById(R.id.tvAssetFinalAmount);

        btnSave = view.findViewById(R.id.btnSaveAsset);

        // 4. Setup Date Pickers
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // 5. Add TextWatchers for Real-Time Calculation
        TextWatcher calcWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateAsset(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etAmount.addTextChangedListener(calcWatcher);
        etRate.addTextChangedListener(calcWatcher);

        // 6. Save Button Logic
        btnSave.setOnClickListener(v -> saveAssetLocally());

        return view;
    }

    // Helper function to show Calendar Popup
    private void showDatePicker(TextInputEditText targetField) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                (view, year, month, dayOfMonth) -> {
                    // Format: dd/mm/yyyy
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    targetField.setText(date);
                    // Recalculate whenever a date changes
                    calculateAsset();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // --- REAL-TIME CALCULATION LOGIC ---
    private void calculateAsset() {
        String amountStr = etAmount.getText().toString().trim();
        String rateStr = etRate.getText().toString().trim();
        String dateStartStr = etStartDate.getText().toString().trim();
        String dateEndStr = etEndDate.getText().toString().trim();

        // Only calculate if all fields have data
        if (amountStr.isEmpty() || rateStr.isEmpty() || dateStartStr.isEmpty() || dateEndStr.isEmpty()) {
            tvTotalInterest.setText("₹ 0.00");
            tvFinalAmount.setText("₹ 0.00");
            return;
        }

        try {
            double principal = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);

            // Date Calculation
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date1 = sdf.parse(dateStartStr);
            Date date2 = sdf.parse(dateEndStr);

            if (date2 != null && date1 != null) {
                long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                if (diffInDays == 0) diffInDays = 1;

                // Interest Formula: (P * R * T) / 100
                double interest = (principal * rate * diffInDays) / (365 * 100);
                double total = principal + interest;

                // Update UI
                tvTotalInterest.setText(String.format("₹ %.2f", interest));
                tvFinalAmount.setText(String.format("₹ %.2f", total));
            }
        } catch (Exception e) {
            // Ignore errors while typing
        }
    }

    private void saveAssetLocally() {
        String borrower = etBorrower.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String rateStr = etRate.getText().toString().trim();
        String startDateStr = etStartDate.getText().toString().trim();
        String endDateStr = etEndDate.getText().toString().trim();

        // --- VALIDATION ---
        if (borrower.isEmpty()) { etBorrower.setError("Required"); return; }
        if (amountStr.isEmpty()) { etAmount.setError("Required"); return; }
        if (rateStr.isEmpty()) { etRate.setError("Required"); return; }
        if (startDateStr.isEmpty()) { etStartDate.setError("Required"); return; }
        if (endDateStr.isEmpty()) { etEndDate.setError("Required"); return; }

        try {
            double principal = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);

            // Re-calculate for saving
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dateStart = sdf.parse(startDateStr);
            Date dateEnd = sdf.parse(endDateStr);

            if (dateEnd.before(dateStart)) {
                etEndDate.setError("End date cannot be before start date!");
                return;
            }

            long diffInMillies = Math.abs(dateEnd.getTime() - dateStart.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            if (diffInDays == 0) diffInDays = 1;

            double interestAmount = (principal * rate * diffInDays) / (365 * 100);
            double totalReceivable = principal + interestAmount;

            // --- SAVE TO LOCAL DATABASE ---
            Transaction t = new Transaction();
            t.type = "Asset";
            t.amount = totalReceivable; // Value to Wallet
            t.category = "Lent";        // Hardcoded category
            t.date = endDateStr;        // Due Date
            t.note = "Lent to " + borrower;
            t.timestamp = System.currentTimeMillis();

            // Save Specific Loan Details
            t.loanPerson = borrower;
            t.loanPrincipal = principal;
            t.loanInterestRate = rate;
            t.loanStartDate = startDateStr;
            t.loanEndDate = endDateStr;

            // Insert into Room Database
            AppDatabase.getDatabase(getContext()).transactionDao().insertTransaction(t);

            Toast.makeText(getActivity(), "Asset Added! Receivable: ₹" + String.format("%.2f", totalReceivable), Toast.LENGTH_LONG).show();
            clearFields();

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error saving data", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void clearFields() {
        etBorrower.setText("");
        etAmount.setText("");
        etRate.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        tvTotalInterest.setText("₹ 0.00");
        tvFinalAmount.setText("₹ 0.00");
    }
}