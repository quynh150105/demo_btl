package com.example.btl_android;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;
import java.util.Map;
import java.util.HashMap;

public class RecordEditorActivity extends AppCompatActivity {
    private String appointmentId;
    private DocumentSnapshot appointment;
    private boolean hasRecord=false,hasPlan=false;
    private EditText diagnosis,prescription,notes,medicine,method,times,purpose,guide;
    private Button save;
    private TextView message;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); App.requireLogin(this); if(App.user()==null)return;
        setContentView(R.layout.activity_record_editor); App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        appointmentId=getIntent().getStringExtra("appointmentId");
        diagnosis=findViewById(R.id.edtDiagnosis); prescription=findViewById(R.id.edtPrescription);
        notes=findViewById(R.id.edtNotes);medicine=findViewById(R.id.edtMedicine);
        method=findViewById(R.id.edtMethod);times=findViewById(R.id.edtTimes);
        purpose=findViewById(R.id.edtPurpose);guide=findViewById(R.id.edtGuide);
        message=findViewById(R.id.txtMessage);save=findViewById(R.id.btnSaveRecord);
        save.setEnabled(false);
        if(appointmentId==null||appointmentId.isEmpty()){message.setText("Thiếu mã lịch khám");return;}
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(user->{
            if(!"doctor".equals(App.str(user,"role"))){message.setText("Chỉ bác sĩ được chỉnh bệnh án");return;}
            loadAppointment();
        }).addOnFailureListener(e->App.error(this,e));
        save.setOnClickListener(v->save());
    }
    private void loadAppointment(){
        App.db().collection("appointments").document(appointmentId).get()
                .addOnSuccessListener(d->{
                    if(!d.exists()||!App.uid().equals(App.str(d,"doctorId"))){message.setText("Bạn không được phân công lịch khám này");return;}
                    String st=App.str(d,"status");
                    if(!st.equals("CONFIRMED")&&!st.equals("COMPLETED")){message.setText("Cần xác nhận lịch trước khi lập bệnh án");return;}
                    appointment=d;
                    ((TextView)findViewById(R.id.txtAppointment)).setText("Bệnh nhân: "+App.str(d,"patientName")+" • "+App.date(d.getTimestamp("startAt")));
                    App.db().collection("medical_records").document(appointmentId).get()
                            .addOnSuccessListener(r->{
                                hasRecord=r.exists();
                                if(r.exists()){
                                    diagnosis.setText(App.str(r,"diagnosis"));prescription.setText(App.str(r,"prescription"));notes.setText(App.str(r,"notes"));
                                }
                                App.db().collection("treatment_plans").document(appointmentId).get()
                                        .addOnSuccessListener(t->{
                                            hasPlan=t.exists();
                                            if(t.exists()){
                                                medicine.setText(App.str(t,"medicine"));method.setText(App.str(t,"method"));
                                                Long count=t.getLong("timesPerDay"); times.setText(count==null?"":String.valueOf(count));
                                                purpose.setText(App.str(t,"purpose"));guide.setText(App.str(t,"guide"));
                                            }
                                            save.setEnabled(true);message.setText("");
                                        }).addOnFailureListener(e->message.setText("Lỗi đọc phác đồ: "+e.getLocalizedMessage()));
                            }).addOnFailureListener(e->message.setText("Lỗi đọc bệnh án: "+e.getLocalizedMessage()));
                }).addOnFailureListener(e->message.setText("Lỗi đọc lịch khám: "+e.getLocalizedMessage()));
    }
    private void save(){
        if(appointment==null)return;
        if(App.value(diagnosis).length()<3){diagnosis.setError("Chẩn đoán quá ngắn");return;}
        if(App.value(medicine).isEmpty() || App.value(method).isEmpty() || App.value(times).isEmpty()){
            App.note(this,"Nhập tên thuốc, cách dùng, số lần/ngày");return;
        }
        int count;
        try { count=Integer.parseInt(App.value(times)); }
        catch(NumberFormatException e){times.setError("Số lần không hợp lệ");return;}
        if(count<1||count>12){times.setError("Số lần phải từ 1 đến 12");return;}
        save.setEnabled(false);message.setText("Đang lưu dữ liệu...");
        DocumentReference record=App.db().collection("medical_records").document(appointmentId);
        DocumentReference plan=App.db().collection("treatment_plans").document(appointmentId);
        DocumentReference appt=App.db().collection("appointments").document(appointmentId);
        Map<String,Object> r=new HashMap<>();
        r.put("appointmentId",appointmentId);r.put("patientId",App.str(appointment,"patientId"));r.put("doctorId",App.uid());
        r.put("patientName",App.str(appointment,"patientName"));r.put("doctorName",App.str(appointment,"doctorName"));
        r.put("visitAt",appointment.getTimestamp("startAt"));r.put("diagnosis",App.value(diagnosis));
        r.put("prescription",App.value(prescription));r.put("notes",App.value(notes));
        r.put("updatedAt",FieldValue.serverTimestamp());
        if(!hasRecord)r.put("createdAt",FieldValue.serverTimestamp());
        Map<String,Object> t=new HashMap<>();
        t.put("appointmentId",appointmentId); t.put("patientId",App.str(appointment,"patientId")); t.put("doctorId",App.uid());
        t.put("medicine",App.value(medicine)); t.put("method",App.value(method)); t.put("timesPerDay",count);
        t.put("purpose",App.value(purpose));t.put("guide",App.value(guide));
        t.put("updatedAt",FieldValue.serverTimestamp());
        if(!hasPlan)t.put("createdAt",FieldValue.serverTimestamp());
        // Save record, treatment and completed status together (never leave a half-record).
        WriteBatch batch=App.db().batch();
        batch.set(record,r,SetOptions.merge());batch.set(plan,t,SetOptions.merge());
        if("CONFIRMED".equals(App.str(appointment,"status")))
            batch.update(appt,"status","COMPLETED","updatedAt",FieldValue.serverTimestamp());
        batch.commit().addOnSuccessListener(x->{App.note(this,"Đã lưu bệnh án và phác đồ");finish();})
                .addOnFailureListener(e->{save.setEnabled(true);message.setText("Lưu thất bại: "+e.getLocalizedMessage());});
    }
}
