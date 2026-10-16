package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AppointmentsActivity extends AppCompatActivity {
    private final List<DocumentSnapshot> allAppointments = new ArrayList<>();
    private final List<DocumentSnapshot> filtered = new ArrayList<>();
    private AppointmentAdapter adapter;
    private ListenerRegistration subscription;
    private String role = "";
    private boolean notices = false;
    private boolean filterToday = false;
    private String currentFilter = "ALL";
    private TextView empty;

    // Filter tab IDs and labels
    private static final int[] TAB_IDS = {
        R.id.tabAll, R.id.tabPending, R.id.tabConfirmed, R.id.tabCompleted, R.id.tabCancelled
    };
    private static final String[] TAB_FILTERS = {
        "ALL", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_appointments);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        notices = getIntent().getBooleanExtra("notices", false);
        filterToday = getIntent().getBooleanExtra("filterToday", false);
        String filterStatus = getIntent().getStringExtra("filterStatus");
        if (filterStatus != null && !filterStatus.isEmpty()) {
            currentFilter = filterStatus;
        }

        if (notices) {
            ((TextView) findViewById(R.id.txtScreenTitle)).setText("Thông báo trạng thái lịch khám");
        } else if (filterToday) {
            ((TextView) findViewById(R.id.txtScreenTitle)).setText("Lịch khám hôm nay");
        }

        empty = findViewById(R.id.txtEmpty);

        // Setup filter tabs (hidden for notices mode)
        View tabRow = findViewById(R.id.filterTabRow);
        if (tabRow != null) {
            if (notices) {
                tabRow.setVisibility(View.GONE);
            } else {
                tabRow.setVisibility(View.VISIBLE);
                for (int i = 0; i < TAB_IDS.length; i++) {
                    final String filter = TAB_FILTERS[i];
                    TextView tab = findViewById(TAB_IDS[i]);
                    if (tab != null) {
                        tab.setOnClickListener(v -> {
                            currentFilter = filter;
                            updateTabHighlight();
                            applyFilter();
                        });
                    }
                }
                // Pre-select "Hôm nay" filter when coming from doctor dash
                if (filterToday) {
                    // Will be handled in applyFilter by date
                }
                updateTabHighlight();
            }
        }

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter();
        rv.setAdapter(adapter);

        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(profile -> {
            role = App.str(profile, "role");
            if (!role.equals("doctor") && !role.equals("patient")) { App.logout(this); return; }
            String filter = role.equals("doctor") ? "doctorId" : "patientId";
            subscription = App.db().collection("appointments").whereEqualTo(filter, App.uid())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) { empty.setText("Lỗi tải lịch: " + e.getLocalizedMessage()); return; }
                        allAppointments.clear();
                        if (snapshot != null) for (QueryDocumentSnapshot d : snapshot) allAppointments.add(d);
                        Collections.sort(allAppointments, (a, c) -> {
                            Timestamp at = a.getTimestamp("startAt"), ct = c.getTimestamp("startAt");
                            if (at == null || ct == null) return 0;
                            return ct.compareTo(at);
                        });
                        applyFilter();
                    });
        }).addOnFailureListener(e -> App.error(this, e));
    }

    private void updateTabHighlight() {
        for (int i = 0; i < TAB_IDS.length; i++) {
            TextView tab = findViewById(TAB_IDS[i]);
            if (tab == null) continue;
            if (TAB_FILTERS[i].equals(currentFilter)) {
                tab.setTextColor(0xFFFFFFFF);
                tab.setBackgroundResource(R.drawable.bg_icon_circle_blue);
            } else {
                tab.setTextColor(0xFF6B7280);
                tab.setBackgroundResource(0);
            }
        }
    }

    private void applyFilter() {
        filtered.clear();
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        for (DocumentSnapshot d : allAppointments) {
            String st = d.getString("status");
            if (st == null) st = "";

            // Filter by status tab
            if (!"ALL".equals(currentFilter) && !currentFilter.equals(st)) continue;

            // If filterToday: only show appointments for today
            if (filterToday) {
                Timestamp ts = d.getTimestamp("startAt");
                if (ts == null || ts.toDate().before(startOfDay.getTime())) continue;
            }

            filtered.add(d);
        }

        if (filtered.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(allAppointments.isEmpty() ? "Chưa có lịch khám" : "Không có lịch nào phù hợp");
        } else {
            empty.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() { if (subscription != null) subscription.remove(); super.onDestroy(); }

    private void changeStatus(DocumentSnapshot d, String next) {
        DocumentReference ref = d.getReference();
        String current = App.str(d, "status");
        if (next.equals("CONFIRMED")) {
            ref.update("status", "CONFIRMED", "updatedAt", FieldValue.serverTimestamp())
                    .addOnSuccessListener(x -> App.note(this, "Đã xác nhận lịch khám"))
                    .addOnFailureListener(e -> App.error(this, e));
            return;
        }
        if (next.equals("COMPLETED")) {
            ref.update("status", "COMPLETED", "updatedAt", FieldValue.serverTimestamp())
                    .addOnSuccessListener(x -> App.note(this, "Đã đánh dấu hoàn thành"))
                    .addOnFailureListener(e -> App.error(this, e));
            return;
        }
        if (!next.equals("CANCELLED") || (!current.equals("PENDING") && !current.equals("CONFIRMED"))) return;
        WriteBatch batch = App.db().batch();
        batch.update(ref, "status", "CANCELLED", "updatedAt", FieldValue.serverTimestamp());
        String slotId = App.str(d, "slotId");
        if (!slotId.isEmpty()) batch.delete(App.db().collection("slots").document(slotId));
        batch.commit().addOnFailureListener(e -> App.error(this, e));
    }

    private String status(String s) {
        switch (s) {
            case "PENDING":   return "⏳ Chờ xác nhận";
            case "CONFIRMED": return "✅ Đã xác nhận";
            case "CANCELLED": return "❌ Đã hủy";
            case "COMPLETED": return "🏥 Đã khám";
            default: return s;
        }
    }

    private int statusColor(String s) {
        switch (s) {
            case "PENDING":   return 0xFFF59E0B;
            case "CONFIRMED": return 0xFF10B981;
            case "CANCELLED": return 0xFFEF4444;
            case "COMPLETED": return 0xFF0D6EFD;
            default: return 0xFF6B7280;
        }
    }

    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView title, info, statusTv;
            Button primary, secondary;
            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.txtTitle);
                info = v.findViewById(R.id.txtInfo);
                statusTv = v.findViewById(R.id.txtStatus);
                primary = v.findViewById(R.id.btnPrimary);
                secondary = v.findViewById(R.id.btnSecondary);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_appointment, p, false));
        }

        @Override public int getItemCount() { return filtered.size(); }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int p) {
            DocumentSnapshot d = filtered.get(p);
            String st = App.str(d, "status");
            h.title.setText(role.equals("doctor") ? "🧑‍⚕️  " + App.str(d, "patientName") : "👨‍⚕️  BS. " + App.str(d, "doctorName"));
            h.info.setText("🏥  " + App.str(d, "specialty") + "\n📅  " + App.date(d.getTimestamp("startAt")) + "\n📝  " + App.str(d, "reason"));
            h.statusTv.setText(status(st));
            h.statusTv.setTextColor(statusColor(st));
            h.primary.setVisibility(View.GONE);
            h.secondary.setVisibility(View.GONE);
            h.primary.setOnClickListener(null);
            h.secondary.setOnClickListener(null);

            if (notices) return;

            if (role.equals("doctor")) {
                if (st.equals("PENDING")) {
                    // Xác nhận
                    h.primary.setVisibility(View.VISIBLE);
                    h.primary.setText("✅ Xác nhận");
                    h.primary.setOnClickListener(v -> changeStatus(d, "CONFIRMED"));
                    // Từ chối
                    h.secondary.setVisibility(View.VISIBLE);
                    h.secondary.setText("❌ Từ chối");
                    h.secondary.setOnClickListener(v -> new android.app.AlertDialog.Builder(AppointmentsActivity.this)
                            .setTitle("Từ chối lịch khám")
                            .setMessage("Từ chối lịch khám của " + App.str(d, "patientName") + "?")
                            .setPositiveButton("Từ chối", (dg, w) -> changeStatus(d, "CANCELLED"))
                            .setNegativeButton("Không", null).show());
                } else if (st.equals("CONFIRMED")) {
                    // Nhập bệnh án
                    h.primary.setVisibility(View.VISIBLE);
                    h.primary.setText("📋 Nhập bệnh án");
                    h.primary.setOnClickListener(v -> openRecord(d));
                    // Đánh dấu hoàn thành
                    h.secondary.setVisibility(View.VISIBLE);
                    h.secondary.setText("🏥 Hoàn thành");
                    h.secondary.setOnClickListener(v -> new android.app.AlertDialog.Builder(AppointmentsActivity.this)
                            .setMessage("Đánh dấu lịch khám đã hoàn thành?")
                            .setPositiveButton("Hoàn thành", (dg, w) -> changeStatus(d, "COMPLETED"))
                            .setNegativeButton("Hủy", null).show());
                } else if (st.equals("COMPLETED")) {
                    h.primary.setVisibility(View.VISIBLE);
                    h.primary.setText("📋 Sửa bệnh án");
                    h.primary.setOnClickListener(v -> openRecord(d));
                }
            } else {
                // PATIENT
                if (st.equals("PENDING") || st.equals("CONFIRMED")) {
                    h.secondary.setVisibility(View.VISIBLE);
                    h.secondary.setText("🚫 Hủy lịch");
                    h.secondary.setOnClickListener(v -> new android.app.AlertDialog.Builder(AppointmentsActivity.this)
                            .setTitle("Hủy lịch khám")
                            .setMessage("Bạn muốn hủy lịch khám này?")
                            .setPositiveButton("Hủy lịch", (dg, w) -> changeStatus(d, "CANCELLED"))
                            .setNegativeButton("Quay lại", null).show());
                }
                if (st.equals("COMPLETED")) {
                    h.primary.setVisibility(View.VISIBLE);
                    h.primary.setText("📄 Xem bệnh án");
                    h.primary.setOnClickListener(v -> startActivity(new Intent(AppointmentsActivity.this, RecordsActivity.class)));
                    Boolean rated = d.getBoolean("rated");
                    if (!Boolean.TRUE.equals(rated)) {
                        h.secondary.setVisibility(View.VISIBLE);
                        h.secondary.setText("⭐ Đánh giá");
                        h.secondary.setOnClickListener(v -> {
                            Intent ri = new Intent(AppointmentsActivity.this, RatingActivity.class);
                            ri.putExtra("appointmentId", d.getId());
                            ri.putExtra("doctorId", App.str(d, "doctorId"));
                            ri.putExtra("doctorName", App.str(d, "doctorName"));
                            ri.putExtra("specialty", App.str(d, "specialty"));
                            ri.putExtra("visitDate", App.date(d.getTimestamp("startAt")));
                            startActivity(ri);
                        });
                    }
                }
            }
        }

        private void openRecord(DocumentSnapshot d) {
            Intent i = new Intent(AppointmentsActivity.this, RecordEditorActivity.class);
            i.putExtra("appointmentId", d.getId());
            startActivity(i);
        }
    }
}
