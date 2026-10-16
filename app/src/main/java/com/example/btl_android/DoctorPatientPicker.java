package com.example.btl_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Choose a patient with an existing booking assigned to the logged-in doctor. */
public final class DoctorPatientPicker {
    private DoctorPatientPicker() {}
    public static void open(Activity activity, Class<?> target) {
        App.db().collection("appointments").whereEqualTo("doctorId", App.uid()).get()
            .addOnSuccessListener(snapshot -> {
                Map<String, QueryDocumentSnapshot> choices = new LinkedHashMap<>();
                for (QueryDocumentSnapshot d : snapshot) {
                    String id = App.str(d,"patientId");
                    String status = App.str(d,"status");
                    if (!id.isEmpty() && ("CONFIRMED".equals(status) || "COMPLETED".equals(status))) {
                        choices.put(id,d);
                    }
                }
                if (choices.isEmpty()) {
                    App.note(activity,"Chưa có bệnh nhân có lịch khám đã xác nhận");return;
                }
                ArrayList<QueryDocumentSnapshot> documents = new ArrayList<>(choices.values());
                String[] labels = new String[documents.size()];
                for (int i=0;i<documents.size();i++) labels[i] = App.str(documents.get(i),"patientName");
                new AlertDialog.Builder(activity).setTitle("Chọn bệnh nhân")
                    .setItems(labels, (dialog,position) -> {
                        QueryDocumentSnapshot d = documents.get(position);
                        Intent intent = new Intent(activity,target);
                        intent.putExtra("patientId",App.str(d,"patientId"));
                        intent.putExtra("appointmentId",d.getId());
                        intent.putExtra("patientName",App.str(d,"patientName"));
                        activity.startActivity(intent);
                    }).setNegativeButton("Hủy",null).show();
            }).addOnFailureListener(e -> App.error(activity,e));
    }
}
