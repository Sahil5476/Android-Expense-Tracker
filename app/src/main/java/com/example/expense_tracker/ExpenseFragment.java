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

public class ExpenseFragment extends Fragment {

    // 1. Declare Variables
    private TextInputEditText etAmount, etDate, etNote;
    private AutoCompleteTextView etCategory;
    private TextView tvExpenseTotal; // The Summary Text
    private Button btnSave;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        // 2. Initialize Calendar
        calendar = Calendar.getInstance();

        // 3. Link Views
        etAmount = view.findViewById(R.id.etExpenseAmount);
        etCategory = view.findViewById(R.id.etExpenseCategory);
        etDate = view.findViewById(R.id.etExpenseDate);
        etNote = view.findViewById(R.id.etExpenseNote);
        tvExpenseTotal = view.findViewById(R.id.tvExpenseTotal);
        btnSave = view.findViewById(R.id.btnSaveExpense);

        // 4. Setup Dropdown
        String[] categories = {"Food", "Transport", "Rent", "Groceries", "Entertainment", "Health", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);

        // 5. Setup Date Picker
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);
        etDate.setClickable(true);

        // 6. Real-time Summary Update (Shows amount in Summary Card as you type)
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    tvExpenseTotal.setText("₹ " + s.toString());
                } else {
                    tvExpenseTotal.setText("₹ 0.00");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 7. Save Button Logic
        btnSave.setOnClickListener(v -> saveExpenseLocally());

        return view;
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

    private void saveExpenseLocally() {
        String amountStr = etAmount.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        // --- VALIDATION ---
        if (amountStr.isEmpty()) { etAmount.setError("Required"); return; }
        if (category.isEmpty()) { etCategory.setError("Required"); return; }
        if (date.isEmpty()) { etDate.setError("Required"); return; }

        try {
            double amount = Double.parseDouble(amountStr);

            // --- SAVE TO LOCAL DATABASE ---
            Transaction t = new Transaction();
            t.type = "Expense";
            t.amount = amount;
            t.category = category;
            t.date = date;
            t.note = note;
            t.timestamp = System.currentTimeMillis();

            // Insert into Room Database
            AppDatabase.getDatabase(getContext()).transactionDao().insertTransaction(t);

            Toast.makeText(getActivity(), "Expense Added to Wallet!", Toast.LENGTH_SHORT).show();
            clearFields();

        } catch (NumberFormatException e) {
            etAmount.setError("Invalid Amount");
        }
    }

    private void clearFields() {
        etAmount.setText("");
        etCategory.setText(""); // Optional: Reset category
        etDate.setText("");
        etNote.setText("");
        tvExpenseTotal.setText("₹ 0.00");
    }
}