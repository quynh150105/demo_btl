package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatingActivity extends AppCompatActivity {

    private int selectedStars = 0;
    private final List<String> selectedTags = new ArrayList<>();
    private String appointmentId, doctorId, doctorName, specialty, visitDate;
    private TextView[] stars = new TextView[5];
    private static final String[] STAR_LABELS = {
            "", "Rất tệ 😞", "Không hài lòng 😕", "Bình thường 😐",
            "Hài lòng 😊", "Tuyệt vời! 🤩"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null)
            return;
        setContentView(R.layout.activity_rating);
        App.applyInsets(this);

        // Read extras passed from AppointmentsActivity
        Intent intent = getIntent();
        appointmentId = intent.getStringExtra("appointmentId");
        doctorId = intent.getStringExtra("doctorId");
        doctorName = intent.getStringExtra("doctorName");
        specialty = intent.getStringExtra("specialty");
        visitDate = intent.getStringExtra("visitDate");

        // Bind views
        ((TextView) findViewById(R.id.txtDoctorName)).setText("BS. " + (doctorName != null ? doctorName : ""));
        ((TextView) findViewById(R.id.txtSpecialty)).setText(specialty != null ? specialty : "");
        ((TextView) findViewById(R.id.txtVisitDate)).setText("Ngày khám: " + (visitDate != null ? visitDate : ""));

        // Stars
        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> selectStars(rating));
        }

        // Tag chips – toggle selection
        setupChip(R.id.chipFriendly, "Thân thiện");
        setupChip(R.id.chipProfessional, "Chuyên nghiệp");
        setupChip(R.id.chipOnTime, "Đúng giờ");
        setupChip(R.id.chipCareful, "Tận tâm");
        setupChip(R.id.chipExplain, "Giải thích rõ");

        // Submit
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitRating());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void selectStars(int count) {
        selectedStars = count;
        for (int i = 0; i < 5; i++) {
            stars[i].setText(i < count ? "★" : "☆");
        }
        TextView label = findViewById(R.id.txtRatingLabel);
        label.setText(count > 0 ? STAR_LABELS[count] : "Chạm vào sao để đánh giá");
        label.setTextColor(count > 0 ? 0xFFF59E0B : 0xFF9CA3AF);
    }

    private void setupChip(int viewId, String tag) {
        TextView chip = findViewById(viewId);
        chip.setOnClickListener(v -> {
            if (selectedTags.contains(tag)) {
                selectedTags.remove(tag);
                chip.setBackgroundResource(R.drawable.bg_section);
                chip.setTextColor(0xFF6B7280);
            } else {
                selectedTags.add(tag);
                chip.setBackgroundResource(R.drawable.bg_badge_green);
                chip.setTextColor(0xFF10B981);
            }
        });
    }

    private void submitRating() {
        if (selectedStars == 0) {
            App.note(this, "Hãy chọn số sao đánh giá");
            return;
        }
        String comment = "";
        com.google.android.material.textfield.TextInputEditText edt = findViewById(R.id.edtComment);
        if (edt.getText() != null)
            comment = edt.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("appointmentId", appointmentId);
        data.put("doctorId", doctorId);
        data.put("doctorName", doctorName);
        data.put("patientId", App.uid());
        data.put("stars", (long) selectedStars);
        data.put("tags", selectedTags);
        data.put("comment", comment);
        data.put("ratedAt", FieldValue.serverTimestamp());

        // Save to ratings collection, keyed by appointmentId (one rating per
        // appointment)
        App.db().collection("ratings").document(appointmentId).set(data)
                .addOnSuccessListener(x -> {
                    ((TextView) findViewById(R.id.txtMessage)).setText("✓ Cảm ơn bạn đã đánh giá!");
                    App.note(this, "Đánh giá đã được gửi!");
                    // Mark appointment as rated
                    App.db().collection("appointments").document(appointmentId)
                            .update("rated", true);
                    // Delay close
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(this::finish, 1500);
                })
                .addOnFailureListener(e -> App.error(this, e));
    }
}
