package com.example.btl_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthVitalsActivity extends AppCompatActivity {

    private String patientId, appointmentId;
    private boolean doctorMode;
    private final List<DocumentSnapshot> vitals = new ArrayList<>();
    private VitalsAdapter adapter;
    private ListenerRegistration listener;
    private TextView txtHeartRate, txtBloodPressure, txtWeight, txtGlucose, txtEmpty;

    private static final String[] TYPES = {
        "Nhịp tim (bpm)", "Huyết áp tâm thu (mmHg)", "Huyết áp tâm trương (mmHg)",
        "Cân nặng (kg)", "Đường huyết (mg/dL)", "Nhiệt độ (°C)", "SpO2 (%)"
    };
    private static final String[] TYPE_KEYS = {
        "heart_rate", "bp_systolic", "bp_diastolic",
        "weight", "glucose", "temperature", "spo2"
    };
    private static final String[] ICONS = { "❤️", "💉", "💉", "⚖️", "🩸", "🌡️", "💨" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_health_vitals);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        patientId = getIntent().getStringExtra("patientId");
        appointmentId = getIntent().getStringExtra("appointmentId");
        doctorMode = patientId != null && !patientId.isEmpty() && !patientId.equals(App.uid());
        if (!doctorMode) patientId = App.uid();
        txtHeartRate = findViewById(R.id.txtHeartRate);
        txtBloodPressure = findViewById(R.id.txtBloodPressure);
        txtWeight = findViewById(R.id.txtWeight);
        txtGlucose = findViewById(R.id.txtGlucose);
        txtEmpty = findViewById(R.id.txtEmpty);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VitalsAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        loadVitals();
    }

    private void loadVitals() {
        com.google.firebase.firestore.Query query = App.db().collection("health_vitals")
                .document(patientId).collection("records");
        if (doctorMode) query = query.whereEqualTo("appointmentId", appointmentId);
        listener = query.addSnapshotListener((snap, e) -> {
                    if (e != null) { App.error(this,e); return; }
                    vitals.clear();
                    if (snap != null) for (QueryDocumentSnapshot d : snap) vitals.add(d);
                    Collections.sort(vitals, (a, b) -> {
                        Timestamp at = a.getTimestamp("recordedAt"), bt = b.getTimestamp("recordedAt");
                        if (at == null || bt == null) return 0;
                        return bt.compareTo(at);
                    });
                    updateSummary();
                    txtEmpty.setVisibility(vitals.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateSummary() {
        // Show latest value for each key type
        Map<String, String> latest = new HashMap<>();
        for (DocumentSnapshot d : vitals) {
            String key = d.getString("type");
            if (key != null && !latest.containsKey(key)) {
                latest.put(key, d.getString("value"));
            }
        }
        txtHeartRate.setText(latest.getOrDefault("heart_rate", "--"));
        String sys = latest.getOrDefault("bp_systolic", "--");
        String dia = latest.getOrDefault("bp_diastolic", "--");
        txtBloodPressure.setText(sys + "/" + dia);
        txtWeight.setText(latest.getOrDefault("weight", "--"));
        txtGlucose.setText(latest.getOrDefault("glucose", "--"));
    }

    private void showAddDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_vital, null);
        Spinner spinner = v.findViewById(R.id.spinnerType);
        EditText edtValue = v.findViewById(R.id.edtValue);
        EditText edtNote = v.findViewById(R.id.edtNote);

        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TYPES);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);

        new AlertDialog.Builder(this)
                .setTitle("Thêm chỉ số sức khoẻ")
                .setView(v)
                .setPositiveButton("Lưu", (d, w) -> {
                    String val = edtValue.getText().toString().trim();
                    String note = edtNote.getText().toString().trim();
                    if (val.isEmpty()) { App.note(this, "Nhập giá trị"); return; }
                    int pos = spinner.getSelectedItemPosition();
                    Map<String, Object> data = new HashMap<>();
                    data.put("type", TYPE_KEYS[pos]);
                    data.put("label", TYPES[pos]);
                    data.put("value", val);
                    data.put("icon", ICONS[pos]);
                    data.put("note", note);
                    data.put("recordedAt", FieldValue.serverTimestamp());
                    data.put("createdBy", App.uid());
                    if (doctorMode) data.put("appointmentId", appointmentId);
                    App.db().collection("health_vitals").document(patientId)
                            .collection("records").add(data)
                            .addOnSuccessListener(ref -> App.note(this, "Đã lưu chỉ số"))
                            .addOnFailureListener(e -> App.error(this, e));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView icon, type, value, date, note, status, del;
            Holder(View v) {
                super(v);
                icon = v.findViewById(R.id.txtIcon);
                type = v.findViewById(R.id.txtType);
                value = v.findViewById(R.id.txtValue);
                date = v.findViewById(R.id.txtDate);
                note = v.findViewById(R.id.txtNote);
                status = v.findViewById(R.id.txtStatus);
                del = v.findViewById(R.id.btnDelete);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_vital, p, false));
        }

        @Override public int getItemCount() { return vitals.size(); }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            DocumentSnapshot d = vitals.get(pos);
            String typeKey = d.getString("type");
            String label = d.getString("label");
            String ic = d.getString("icon");
            String val = d.getString("value");
            String noteStr = d.getString("note");

            h.icon.setText(ic != null ? ic : "📊");
            h.type.setText(label != null ? label : "Chỉ số");
            h.value.setText(val != null ? val : "--");
            h.date.setText(App.date(d.getTimestamp("recordedAt")));
            h.note.setText(noteStr != null && !noteStr.isEmpty() ? noteStr : "");
            h.note.setVisibility(noteStr != null && !noteStr.isEmpty() ? View.VISIBLE : View.GONE);

            // Simple range check for common vitals
            boolean warning = false;
            if ("heart_rate".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 60 || v > 100; } catch (Exception ignored) {}
            } else if ("bp_systolic".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 90 || v > 140; } catch (Exception ignored) {}
            } else if ("glucose".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 70 || v > 140; } catch (Exception ignored) {}
            }
            if (warning) {
                h.status.setText("⚠️ Chú ý");
                h.status.setTextColor(0xFFF59E0B);
                h.status.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                h.status.setText("✓ Bình thường");
                h.status.setTextColor(0xFF10B981);
                h.status.setBackgroundResource(R.drawable.bg_badge_green);
            }

            // Xóa chỉ số
            if (h.del != null) h.del.setVisibility(doctorMode ? View.GONE : View.VISIBLE);
            if (h.del != null && !doctorMode) {
                h.del.setOnClickListener(v -> new AlertDialog.Builder(HealthVitalsActivity.this)
                        .setTitle("🗑️ Xóa chỉ số")
                        .setMessage("Xóa bản ghi " + (label != null ? label : "này") + "?")
                        .setPositiveButton("Xóa", (dlg, w) -> d.getReference().delete()
                                .addOnSuccessListener(x -> App.note(HealthVitalsActivity.this, "Đã xóa chỉ số"))
                                .addOnFailureListener(e -> App.error(HealthVitalsActivity.this, e)))
                        .setNegativeButton("Hủy", null).show());
            }
        }
    }
}
