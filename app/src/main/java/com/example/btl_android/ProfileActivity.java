package com.example.btl_android;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    EditText name,phone,dob,gender,address,history;
    Button save;
    TextView message;
    String role="";
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); App.requireLogin(this); if(App.user()==null)return;
        setContentView(R.layout.activity_profile); App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        name=findViewById(R.id.edtName);phone=findViewById(R.id.edtPhone); dob=findViewById(R.id.edtDob);
        gender=findViewById(R.id.edtGender);address=findViewById(R.id.edtAddress);
        history=findViewById(R.id.edtMedicalHistory);save=findViewById(R.id.btnSaveProfile);
        message=findViewById(R.id.txtMessage);
        save.setEnabled(false);
        ((TextView)findViewById(R.id.txtEmail)).setText("Email: "+App.user().getEmail());
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(d -> {
            if(!d.exists()){message.setText("Không có hồ sơ người dùng");return;}
            role=App.str(d,"role");
            ((TextView)findViewById(R.id.txtRole)).setText("Vai trò: "+role);
            name.setText(App.str(d,"name"));phone.setText(App.str(d,"phone"));
            dob.setText(App.str(d,"dob"));gender.setText(App.str(d,"gender"));
            address.setText(App.str(d,"address"));history.setText(App.str(d,"medicalHistory"));
            save.setEnabled(true);
        }).addOnFailureListener(e->App.error(this,e));
        save.setOnClickListener(v->saveProfile());
    }
    private void saveProfile(){
        if(App.value(name).length()<2){name.setError("Họ tên quá ngắn");return;}
        if(!App.value(phone).isEmpty()&&!App.value(phone).matches("^0[0-9]{9}$")){phone.setError("SĐT phải gồm 10 số bắt đầu bằng 0");return;}
        Map<String,Object> changes=new HashMap<>();
        changes.put("name",App.value(name)); changes.put("phone",App.value(phone));
        changes.put("dob",App.value(dob));changes.put("gender",App.value(gender));
        changes.put("address",App.value(address));changes.put("medicalHistory",App.value(history));
        // No client can change their role by updating profile.
        save.setEnabled(false);
        App.db().collection("users").document(App.uid()).update(changes)
                .addOnSuccessListener(x->{save.setEnabled(true);message.setText("Đã lưu hồ sơ");})
                .addOnFailureListener(e->{save.setEnabled(true);App.error(this,e);});
    }
}
