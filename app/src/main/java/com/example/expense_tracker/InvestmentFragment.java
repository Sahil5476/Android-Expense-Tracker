package com.example.expense_tracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class InvestmentFragment extends Fragment {

    // 1. Declare Variables
    private TextInputEditText etBuyPrice, etQuantity, etDate, etNote;
    private AutoCompleteTextView etAssetType;
    private TextView tvTotalCalc;
    private Button btnSave;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_investment, container, false);

        // 2. Initialize Calendar
        calendar = Calendar.getInstance();

        // 3. Link Views
        etAssetType = view.findViewById(R.id.etAssetType);
        etBuyPrice = view.findViewById(R.id.etBuyPrice);
        etQuantity = view.findViewById(R.id.etQuantity);
        etDate = view.findViewById(R.id.etInvestDate);
        etNote = view.findViewById(R.id.etInvestNote);
        tvTotalCalc = view.findViewById(R.id.tvTotalCalc);
        btnSave = view.findViewById(R.id.btnSaveInvestment);

        // 4. Setup Asset Dropdown
        String[] assets = {"Stocks", "Mutual Funds", "Gold", "Crypto", "Real Estate", "Bonds", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, assets);
        etAssetType.setAdapter(adapter);

        // 5. Setup Date Picker
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);
        etDate.setClickable(true);

        // 6. Real-time Calculation (Price * Qty)
        TextWatcher calculatorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateTotal(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etBuyPrice.addTextChangedListener(calculatorWatcher);
        etQuantity.addTextChangedListener(calculatorWatcher);

        // 7. Save Button
        btnSave.setOnClickListener(v -> saveInvestmentLocally());

        return view;
    }

    private void calculateTotal() {
        try {
            String priceStr = etBuyPrice.getText().toString();
            String qtyStr = etQuantity.getText().toString();

            if (!priceStr.isEmpty() && !qtyStr.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                double qty = Double.parseDouble(qtyStr);
                double total = price * qty;
                tvTotalCalc.setText(String.format("Total: ₹ %.2f", total));
            } else {
                tvTotalCalc.setText("Total: ₹ 0.00");
            }
        } catch (NumberFormatException e) {
            tvTotalCalc.setText("Error");
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                    etDate.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void saveInvestmentLocally() {
        String assetType = etAssetType.getText().toString().trim();
        String priceStr = etBuyPrice.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        // --- VALIDATION ---
        if (assetType.isEmpty()) { etAssetType.setError("Required"); return; }
        if (priceStr.isEmpty()) { etBuyPrice.setError("Required"); return; }
        if (qtyStr.isEmpty()) { etQuantity.setError("Required"); return; }
        if (date.isEmpty()) { etDate.setError("Required"); return; }

        if (!isValidDate(date)) {
            etDate.setError("Invalid Date! Use dd/mm/yyyy");
            return;
        }

        try {
            // CALCULATE FINAL AMOUNT
            double price = Double.parseDouble(priceStr);
            double qty = Double.parseDouble(qtyStr);
            double totalAmount = price * qty;

            // --- SAVE TO LOCAL DATABASE ---
            Transaction t = new Transaction();
            t.type = "Investment";
            t.amount = totalAmount; // This is the total value saved to wallet
            t.category = assetType;
            t.date = date;
            t.note = note;
            t.timestamp = System.currentTimeMillis();

            // Save Investment Specific Details
            t.investPricePerUnit = price;
            t.investQuantity = qty;

            // Insert into Room Database
            AppDatabase.getDatabase(getContext()).transactionDao().insertTransaction(t);

            Toast.makeText(getActivity(), "Investment Added to Wallet!", Toast.LENGTH_SHORT).show();
            clearFields();

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Invalid numbers entered", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        etBuyPrice.setText("");
        etQuantity.setText("");
        etDate.setText("");
        etNote.setText("");
        etAssetType.setText(""); // Optional: clear selection
        tvTotalCalc.setText("Total: ₹ 0.00");
    }

    // Helper Date Validator
    private boolean isValidDate(String date) {
        if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) return false;
        String[] parts = date.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        if (year < 2000 || year > 2100) return false;
        if (month < 1 || month > 12) return false;
        int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) daysInMonth[2] = 29;
        return day >= 1 && day <= daysInMonth[month];
    }
}