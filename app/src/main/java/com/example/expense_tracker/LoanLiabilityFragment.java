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
import java.util.concurrent.Executors; // IMPORTED FOR BACKGROUND THREAD
import java.util.concurrent.TimeUnit;

public class LoanLiabilityFragment extends Fragment {

    // 1. Declare Variables
    private TextInputEditText etLender, etAmount, etInterest, etBorrowDate, etPaymentDate;
    private TextView tvTotalInterest, tvFinalAmount;
    private Button btnSave;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loan_liability, container, false);

        // 2. Initialize Calendar
        calendar = Calendar.getInstance();

        // 3. Link Views
        etLender = view.findViewById(R.id.etLenderName);
        etAmount = view.findViewById(R.id.etPrincipalAmount);
        etInterest = view.findViewById(R.id.etInterestRate);
        etBorrowDate = view.findViewById(R.id.etBorrowDate);
        etPaymentDate = view.findViewById(R.id.etPaymentDate);

        // Summary Card Views
        tvTotalInterest = view.findViewById(R.id.tvTotalInterest);
        tvFinalAmount = view.findViewById(R.id.tvFinalAmount);

        btnSave = view.findViewById(R.id.btnSaveLiability);

        // 4. Setup Date Pickers
        etBorrowDate.setOnClickListener(v -> showDatePicker(etBorrowDate));
        etPaymentDate.setOnClickListener(v -> showDatePicker(etPaymentDate));

        // 5. Add TextWatchers for Real-Time Calculation
        TextWatcher calcWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateLiability(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etAmount.addTextChangedListener(calcWatcher);
        etInterest.addTextChangedListener(calcWatcher);

        // 6. Save Button Logic
        btnSave.setOnClickListener(v -> saveLoanLiabilityLocally());

        return view;
    }

    // --- FIX 1: SAFETY CHECK FOR WINDOW LEAK CRASH ---
    private void showDatePicker(TextInputEditText targetField) {
        // Check if Activity is still alive before showing dialog
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    targetField.setText(date);
                    calculateLiability();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // --- REAL-TIME CALCULATION LOGIC ---
    private void calculateLiability() {
        String amountStr = etAmount.getText().toString().trim();
        String rateStr = etInterest.getText().toString().trim();
        String dateStartStr = etBorrowDate.getText().toString().trim();
        String dateEndStr = etPaymentDate.getText().toString().trim();

        if (amountStr.isEmpty() || rateStr.isEmpty() || dateStartStr.isEmpty() || dateEndStr.isEmpty()) {
            tvTotalInterest.setText("₹ 0.00");
            tvFinalAmount.setText("₹ 0.00");
            return;
        }

        try {
            double principal = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date1 = sdf.parse(dateStartStr);
            Date date2 = sdf.parse(dateEndStr);

            if (date2 != null && date1 != null) {
                long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                if (diffInDays == 0) diffInDays = 1;

                double interest = (principal * rate * diffInDays) / (365 * 100);
                double total = principal + interest;

                tvTotalInterest.setText(String.format("₹ %.2f", interest));
                tvFinalAmount.setText(String.format("₹ %.2f", total));
            }

        } catch (Exception e) {
            // Ignore parse errors while typing
        }
    }

    // --- FIX 2: RUN DATABASE SAVE IN BACKGROUND (PREVENTS ANR FREEZE) ---
    private void saveLoanLiabilityLocally() {
        String lender = etLender.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String rateStr = etInterest.getText().toString().trim();
        String borrowDateStr = etBorrowDate.getText().toString().trim();
        String paymentDateStr = etPaymentDate.getText().toString().trim();

        if (lender.isEmpty()) { etLender.setError("Required"); return; }
        if (amountStr.isEmpty()) { etAmount.setError("Required"); return; }
        if (rateStr.isEmpty()) { etInterest.setError("Required"); return; }
        if (borrowDateStr.isEmpty()) { etBorrowDate.setError("Required"); return; }
        if (paymentDateStr.isEmpty()) { etPaymentDate.setError("Required"); return; }

        try {
            double principal = Double.parseDouble(amountStr);
            double rate = Double.parseDouble(rateStr);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date1 = sdf.parse(borrowDateStr);
            Date date2 = sdf.parse(paymentDateStr);

            if (date2.before(date1)) {
                etPaymentDate.setError("Payment date cannot be before borrow date!");
                return;
            }

            long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            if (diffInDays == 0) diffInDays = 1;

            double interestAmount = (principal * rate * diffInDays) / (365 * 100);
            double totalRepayment = principal + interestAmount;

            // Prepare Transaction Object
            Transaction t = new Transaction();
            t.type = "Liability";
            t.amount = totalRepayment;
            t.category = "Debt";
            t.date = paymentDateStr;
            t.note = "Borrowed from " + lender;
            t.timestamp = System.currentTimeMillis();

            t.loanPerson = lender;
            t.loanPrincipal = principal;
            t.loanInterestRate = rate;
            t.loanStartDate = borrowDateStr;
            t.loanEndDate = paymentDateStr;

            // --- DATABASE OPERATION IN BACKGROUND THREAD ---
            Executors.newSingleThreadExecutor().execute(() -> {
                // 1. Insert into Database
                AppDatabase.getDatabase(getContext()).transactionDao().insertTransaction(t);

                // 2. Update UI on Main Thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Debt Added", Toast.LENGTH_SHORT).show();
                        clearFields();
                        // Close the parent Activity to return to Dashboard
                        getActivity().finish();
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error saving data", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        etLender.setText("");
        etAmount.setText("");
        etInterest.setText("");
        etBorrowDate.setText("");
        etPaymentDate.setText("");
        tvTotalInterest.setText("₹ 0.00");
        tvFinalAmount.setText("₹ 0.00");
    }
}