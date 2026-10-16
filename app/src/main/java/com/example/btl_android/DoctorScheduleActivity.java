package com.example.btl_android;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoctorScheduleActivity extends AppCompatActivity {

    private final List<DocumentSnapshot> slots = new ArrayList<>();
    private SlotAdapter adapter;
    private ListenerRegistration listener;
    private TextView txtEmpty, txtDayLabel;
    private int selectedDay = 2; // Monday = 2 (Calendar.MONDAY)

    private static final int[] DAY_IDS = {
        R.id.dayMon, R.id.dayTue, R.id.dayWed,
        R.id.dayThu, R.id.dayFri, R.id.daySat, R.id.daySun
    };
    private static final int[] DAY_CODES = { 2, 3, 4, 5, 6, 7, 1 }; // Calendar constants
    private static final String[] DAY_NAMES = {
        "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_doctor_schedule);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        txtEmpty = findViewById(R.id.txtEmpty);
        txtDayLabel = findViewById(R.id.txtDayLabel);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddSlotDialog());

        // Set up day buttons
        for (int i = 0; i < DAY_IDS.length; i++) {
            final int index = i;
            findViewById(DAY_IDS[i]).setOnClickListener(v -> selectDay(index));
        }
        selectDay(0); // Start on Monday
    }

    private void selectDay(int index) {
        selectedDay = DAY_CODES[index];
        txtDayLabel.setText("Khung giờ – " + DAY_NAMES[index]);

        // Visual highlight
        for (int i = 0; i < DAY_IDS.length; i++) {
            TextView tv = findViewById(DAY_IDS[i]);
            if (i == index) {
                tv.setTextColor(0xFFFFFFFF);
                tv.setBackgroundResource(R.drawable.bg_icon_circle_blue);
            } else {
                tv.setTextColor(0xFF6B7280);
                tv.setBackgroundResource(R.drawable.bg_section);
            }
        }
        loadSlots();
    }

    private void loadSlots() {
        if (listener != null) listener.remove();
        listener = App.db().collection("doctor_schedules")
                .document(App.uid())
                .collection("slots")
                .whereEqualTo("dayOfWeek", selectedDay)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { App.error(this,e); return; }
                    slots.clear();
                    if (snap != null) for (QueryDocumentSnapshot d : snap) slots.add(d);
                    // Sort by startHour
                    slots.sort((a, b) -> {
                        Long ah = a.getLong("startHour"), bh = b.getLong("startHour");
                        Long am = a.getLong("startMin"), bm = b.getLong("startMin");
                        if (ah == null || bh == null) return 0;
                        int cmp = ah.compareTo(bh);
                        if (cmp != 0) return cmp;
                        if (am == null || bm == null) return 0;
                        return am.compareTo(bm);
                    });
                    txtEmpty.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddSlotDialog() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (picker, hour, minute) -> {
            // Snap minute to 0 or 30
            int snapped = minute < 15 ? 0 : (minute < 45 ? 30 : 0);
            int snappedHour = (minute >= 45) ? hour + 1 : hour;
            String label = String.format(Locale.getDefault(), "%02d:%02d", snappedHour, snapped);

            // Check duplicate
            for (DocumentSnapshot d : slots) {
                Long h = d.getLong("startHour"), m = d.getLong("startMin");
                if (h != null && m != null && h == snappedHour && m == snapped) {
                    App.note(this, "Khung giờ " + label + " đã tồn tại");
                    return;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("dayOfWeek", selectedDay);
            data.put("startHour", (long) snappedHour);
            data.put("startMin", (long) snapped);
            data.put("label", label);
            data.put("isBooked", false);
            data.put("createdAt", FieldValue.serverTimestamp());

            App.db().collection("doctor_schedules").document(App.uid())
                    .collection("slots").add(data)
                    .addOnSuccessListener(ref -> App.note(this, "Đã thêm khung giờ " + label))
                    .addOnFailureListener(e -> App.error(this, e));

        }, now.get(Calendar.HOUR_OF_DAY), 0, true).show();
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private void showCopyDayDialog(DocumentSnapshot d, String label) {
        Long startHour = d.getLong("startHour");
        Long startMin = d.getLong("startMin");
        if (startHour == null || startMin == null) return;

        new AlertDialog.Builder(this)
                .setTitle("📋 Sao chép giờ " + label + " sang ngày")
                .setItems(DAY_NAMES, (dlg, which) -> {
                    int targetDay = DAY_CODES[which];
                    // Check duplicate
                    final long fHour = startHour, fMin = startMin;
                    App.db().collection("doctor_schedules").document(App.uid())
                            .collection("slots")
                            .whereEqualTo("dayOfWeek", targetDay)
                            .get()
                            .addOnSuccessListener(snap -> {
                                for (com.google.firebase.firestore.QueryDocumentSnapshot s : snap) {
                                    Long h = s.getLong("startHour"), m = s.getLong("startMin");
                                    if (h != null && m != null && h == fHour && m == fMin) {
                                        App.note(this, DAY_NAMES[which] + " đã có giờ " + label);
                                        return;
                                    }
                                }
                                Map<String, Object> data = new HashMap<>();
                                data.put("dayOfWeek", targetDay);
                                data.put("startHour", fHour);
                                data.put("startMin", fMin);
                                data.put("label", label);
                                data.put("isBooked", false);
                                data.put("createdAt", FieldValue.serverTimestamp());
                                App.db().collection("doctor_schedules").document(App.uid())
                                        .collection("slots").add(data)
                                        .addOnSuccessListener(ref -> App.note(this, "Đã sao chép sang " + DAY_NAMES[which]))
                                        .addOnFailureListener(e -> App.error(this, e));
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView time, status, del, copy;
            Holder(View v) {
                super(v);
                time = v.findViewById(R.id.txtTime);
                status = v.findViewById(R.id.txtStatus);
                del = v.findViewById(R.id.btnDelete);
                copy = v.findViewById(R.id.btnCopy);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_schedule_slot, p, false));
        }

        @Override public int getItemCount() { return slots.size(); }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            DocumentSnapshot d = slots.get(pos);
            String label = d.getString("label");
            Boolean booked = d.getBoolean("isBooked");
            boolean isBooked = Boolean.TRUE.equals(booked);

            h.time.setText("🕐  " + (label != null ? label : "--"));

            // Copy button: always visible to copy slot to another day
            if (h.copy != null) {
                h.copy.setOnClickListener(v -> showCopyDayDialog(d, label));
            }

            if (isBooked) {
                h.status.setText("🔒 Đã đặt");
                h.status.setTextColor(0xFFF59E0B);
                h.status.setBackgroundResource(R.drawable.bg_badge_amber);
                h.del.setVisibility(View.INVISIBLE);
            } else {
                h.status.setText("✓ Còn trống");
                h.status.setTextColor(0xFF10B981);
                h.status.setBackgroundResource(R.drawable.bg_badge_green);
                h.del.setVisibility(View.VISIBLE);
                h.del.setOnClickListener(v ->
                    new AlertDialog.Builder(DoctorScheduleActivity.this)
                        .setMessage("Xóa khung giờ " + label + "?")
                        .setPositiveButton("Xóa", (dlg, w) -> d.getReference().delete()
                            .addOnSuccessListener(x -> App.note(DoctorScheduleActivity.this, "Đã xóa")))
                        .setNegativeButton("Hủy", null).show()
                );
            }
        }
    }
}
