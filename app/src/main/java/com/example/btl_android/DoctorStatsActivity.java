package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DoctorStatsActivity extends AppCompatActivity {

    private TextView txtToday, txtPending, txtCompleted, txtRating, txtRatingValue, txtNoPending;
    private final List<DocumentSnapshot> pendingList = new ArrayList<>();
    private PendingAdapter adapter;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_doctor_stats);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        txtToday = findViewById(R.id.txtTodayCount);
        txtPending = findViewById(R.id.txtPendingCount);
        txtCompleted = findViewById(R.id.txtCompletedCount);
        txtRating = findViewById(R.id.txtRating);
        txtRatingValue = findViewById(R.id.txtRatingValue);
        txtNoPending = findViewById(R.id.txtNoPending);

        RecyclerView rv = findViewById(R.id.recyclerPending);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingAdapter();
        rv.setAdapter(adapter);

        // Stat card clicks
        View cardToday = findViewById(R.id.statCardToday);
        View cardPending = findViewById(R.id.statCardPending);
        View cardCompleted = findViewById(R.id.statCardCompleted);
        if (cardToday != null) cardToday.setOnClickListener(v -> {
            Intent i = new Intent(this, AppointmentsActivity.class);
            i.putExtra("filterToday", true);
            startActivity(i);
        });
        if (cardPending != null) cardPending.setOnClickListener(v -> {
            Intent i = new Intent(this, AppointmentsActivity.class);
            i.putExtra("filterStatus", "PENDING");
            startActivity(i);
        });
        if (cardCompleted != null) cardCompleted.setOnClickListener(v -> {
            Intent i = new Intent(this, AppointmentsActivity.class);
            i.putExtra("filterStatus", "COMPLETED");
            startActivity(i);
        });

        // Load doctor's name
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(d -> {
                    TextView title = findViewById(R.id.txtDoctorName);
                    title.setText("BS. " + App.str(d, "name"));
                });

        loadStats();
        loadRating();
    }

    private void loadStats() {
        listener = App.db().collection("appointments")
                .whereEqualTo("doctorId", App.uid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { App.error(this,e); return; }
                    if (snap == null) return;

                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);

                    int today = 0, pending = 0, completed = 0;
                    pendingList.clear();

                    for (QueryDocumentSnapshot d : snap) {
                        String status = d.getString("status");
                        Timestamp startAt = d.getTimestamp("startAt");

                        if ("PENDING".equals(status)) {
                            pending++;
                            pendingList.add(d);
                        }
                        if ("COMPLETED".equals(status)) completed++;
                        if (startAt != null && startAt.toDate().after(startOfDay.getTime())) today++;
                    }

                    txtToday.setText(String.valueOf(today));
                    txtPending.setText(String.valueOf(pending));
                    txtCompleted.setText(String.valueOf(completed));
                    txtNoPending.setVisibility(pendingList.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadRating() {
        App.db().collection("ratings")
                .whereEqualTo("doctorId", App.uid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        txtRating.setText("Chưa có đánh giá");
                        txtRatingValue.setText("--");
                        return;
                    }
                    double total = 0;
                    int count = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Long stars = d.getLong("stars");
                        if (stars != null) { total += stars; count++; }
                    }
                    double avg = count > 0 ? total / count : 0;
                    txtRating.setText(count + " đánh giá từ bệnh nhân");
                    txtRatingValue.setText(String.format("%.1f", avg));
                });
    }

    private void confirmAppointment(DocumentSnapshot d) {
        d.getReference().update("status", "CONFIRMED", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(x -> App.note(this, "Đã xác nhận lịch khám"))
                .addOnFailureListener(e -> App.error(this, e));
    }

    private void rejectAppointment(DocumentSnapshot d) {
        new android.app.AlertDialog.Builder(this)
                .setMessage("Từ chối lịch khám của " + d.getString("patientName") + "?")
                .setPositiveButton("Từ chối", (dlg, w) ->
                        d.getReference().update("status", "CANCELLED", "updatedAt", FieldValue.serverTimestamp())
                                .addOnSuccessListener(x -> App.note(this, "Đã từ chối"))
                                .addOnFailureListener(e -> App.error(this, e)))
                .setNegativeButton("Không", null).show();
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView patient, when, reason;
            MaterialButton btnConfirm, btnReject;
            Holder(View v) {
                super(v);
                patient = v.findViewById(R.id.txtPatient);
                when = v.findViewById(R.id.txtWhen);
                reason = v.findViewById(R.id.txtReason);
                btnConfirm = v.findViewById(R.id.btnConfirm);
                btnReject = v.findViewById(R.id.btnReject);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_pending_appt, p, false));
        }

        @Override public int getItemCount() { return pendingList.size(); }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            DocumentSnapshot d = pendingList.get(pos);
            h.patient.setText("🧑‍⚕️  " + d.getString("patientName"));
            h.when.setText("📅  " + App.date(d.getTimestamp("startAt")));
            h.reason.setText("Lý do: " + d.getString("reason"));
            h.btnConfirm.setOnClickListener(v -> confirmAppointment(d));
            h.btnReject.setOnClickListener(v -> rejectAppointment(d));
        }
    }
}
