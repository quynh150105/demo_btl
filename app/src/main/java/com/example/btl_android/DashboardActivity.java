package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private ListenerRegistration statListener;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_dashboard);
        App.applyInsets(this);

        // Shared buttons always visible
        findViewById(R.id.btnProfile).setOnClickListener(
                v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(
                v -> App.logout(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (App.user() == null) return;
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { App.note(this, "Không tìm thấy hồ sơ"); App.logout(this); return; }
                    String role = App.str(d, "role");
                    String name = App.str(d, "name");

                    // Update header
                    ((TextView) findViewById(R.id.txtWelcome)).setText(
                            "Xin chào, " + (name.isEmpty() ? "bạn" : name.split(" ")[name.split(" ").length - 1]) + " 👋");
                    ((TextView) findViewById(R.id.txtRole)).setText(
                            role.equals("doctor") ? "👨‍⚕️ Bác sĩ" : "🧑 Bệnh nhân");

                    if (!role.equals("patient") && !role.equals("doctor")) {
                        App.logout(this);
                        return;
                    }

                    View sectionPatient = findViewById(R.id.sectionPatient);
                    View sectionDoctor = findViewById(R.id.sectionDoctor);

                    if (role.equals("patient")) {
                        sectionPatient.setVisibility(View.VISIBLE);
                        sectionDoctor.setVisibility(View.GONE);
                        wirePatientButtons();
                        loadPatientStats();
                    } else {
                        sectionPatient.setVisibility(View.GONE);
                        sectionDoctor.setVisibility(View.VISIBLE);
                        wireDoctorButtons();
                        loadDoctorStats();
                    }
                })
                .addOnFailureListener(e -> App.error(this, e));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (statListener != null) { statListener.remove(); statListener = null; }
    }

    // ============================================================
    //  PATIENT
    // ============================================================
    private void wirePatientButtons() {
        // Stat cards → navigate
        safeClick(R.id.statCardAppointments, AppointmentsActivity.class);
        safeClick(R.id.statCardMedication, MedicationActivity.class);
        safeClick(R.id.statCardHealth, HealthVitalsActivity.class);

        // Service cards
        safeClick(R.id.btnBook, BookingActivity.class);
        safeClick(R.id.btnAppointments, AppointmentsActivity.class);
        safeClick(R.id.btnHealthVitals, HealthVitalsActivity.class);
        safeClick(R.id.btnMedication, MedicationActivity.class);
        safeClick(R.id.btnRecords, RecordsActivity.class);

        // Notifications
        View btnNotices = findViewById(R.id.btnNotices);
        if (btnNotices != null) {
            btnNotices.setOnClickListener(v -> {
                Intent i = new Intent(this, AppointmentsActivity.class);
                i.putExtra("notices", true);
                startActivity(i);
            });
        }
    }

    /** Realtime stats cho bệnh nhân: đếm lịch khám, thuốc, chỉ số */
    private void loadPatientStats() {
        TextView tvAppt = findViewById(R.id.statCountAppointments);
        TextView tvMed  = findViewById(R.id.statCountMedication);
        TextView tvHlth = findViewById(R.id.statCountHealth);
        if (tvAppt == null) return;

        // Lịch khám active (PENDING + CONFIRMED)
        statListener = App.db().collection("appointments")
                .whereEqualTo("patientId", App.uid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    int active = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        String st = d.getString("status");
                        if ("PENDING".equals(st) || "CONFIRMED".equals(st)) active++;
                    }
                    tvAppt.setText(String.valueOf(active));
                });

        // Thuốc: đếm tổng
        App.db().collection("medications").document(App.uid()).collection("list")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    tvMed.setText(String.valueOf(snap.size()));
                });

        // Chỉ số sức khoẻ: đếm tổng records
        App.db().collection("health_vitals").document(App.uid()).collection("records")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    tvHlth.setText(String.valueOf(snap.size()));
                });
    }

    // ============================================================
    //  DOCTOR
    // ============================================================
    private void wireDoctorButtons() {
        // Stat cards
        safeClick(R.id.drStatCardPatients, AppointmentsActivity.class);
        View cardToday = findViewById(R.id.drStatCardToday);
        if (cardToday != null) cardToday.setOnClickListener(v -> {
            Intent i = new Intent(this, AppointmentsActivity.class);
            i.putExtra("filterToday", true);
            startActivity(i);
        });
        safeClick(R.id.drStatCardRating, DoctorStatsActivity.class);

        // Tool cards – use unique IDs for doctor section to avoid clash with patient section
        safeClick(R.id.btnDrAppointments, AppointmentsActivity.class);
        findViewById(R.id.btnDrVitals).setOnClickListener(v -> DoctorPatientPicker.open(this, HealthVitalsActivity.class));
        findViewById(R.id.btnDrMedication).setOnClickListener(v -> DoctorPatientPicker.open(this, MedicationActivity.class));
        safeClick(R.id.btnDrRecords, RecordsActivity.class);
        // Make the entire doctor service card clickable (not just its small action button).
        int[] doctorButtons = {R.id.btnDrAppointments, R.id.btnDoctorStats,
                R.id.btnDoctorSchedule, R.id.btnDrRecords};
        for (int id : doctorButtons) {
            View button = findViewById(id);
            if (button != null && button.getParent() instanceof View) {
                View row = (View) button.getParent();
                row.setOnClickListener(v -> button.performClick());
            }
        }
        safeClick(R.id.btnDoctorStats, DoctorStatsActivity.class);
        safeClick(R.id.btnDoctorSchedule, DoctorScheduleActivity.class);
    }

    /** Realtime stats cho bác sĩ: bệnh nhân hôm nay, lịch chờ, đánh giá */
    private void loadDoctorStats() {
        TextView tvPatients = findViewById(R.id.drStatCountPatients);
        TextView tvToday    = findViewById(R.id.drStatCountToday);
        TextView tvRating   = findViewById(R.id.drStatCountRating);
        if (tvPatients == null) return;

        // Lịch hẹn
        statListener = App.db().collection("appointments")
                .whereEqualTo("doctorId", App.uid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);
                    startOfDay.set(Calendar.MILLISECOND, 0);

                    int todayCount = 0, uniquePatients = 0;
                    java.util.Set<String> patientIds = new java.util.HashSet<>();
                    for (QueryDocumentSnapshot d : snap) {
                        String pid = d.getString("patientId");
                        if (pid != null) patientIds.add(pid);
                        Timestamp ts = d.getTimestamp("startAt");
                        if (ts != null && ts.toDate().after(startOfDay.getTime())) todayCount++;
                    }
                    tvPatients.setText(String.valueOf(patientIds.size()));
                    tvToday.setText(String.valueOf(todayCount));
                });

        // Đánh giá trung bình
        App.db().collection("ratings").whereEqualTo("doctorId", App.uid()).get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) { tvRating.setText("--"); return; }
                    double total = 0; int count = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Long stars = d.getLong("stars");
                        if (stars != null) { total += stars; count++; }
                    }
                    if (count > 0) tvRating.setText(String.format(Locale.US, "%.1f★", total / count));
                    else tvRating.setText("--");
                });
    }

    /** Null-safe click listener for activity launch. */
    private void safeClick(int viewId, Class<?> activityClass) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, activityClass)));
    }
}
