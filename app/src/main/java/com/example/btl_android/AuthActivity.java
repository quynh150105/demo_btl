package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {
    private EditText email, password, name;
    private Button btnLogin,btnRegister;
    private TextView message;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_auth); App.applyInsets(this);
        email = findViewById(R.id.edtEmail);
        password = findViewById(R.id.edtPassword);
        name = findViewById(R.id.edtName);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        message = findViewById(R.id.txtMessage);
        if (!App.configured(this)) { btnLogin.setEnabled(false); btnRegister.setEnabled(false); return; }
        if (App.user() != null) { openByRole(); }
        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
    }
    private boolean validate(boolean signup) {
        String e=App.value(email), p=App.value(password);
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) { email.setError("Email không hợp lệ"); return false; }
        if (p.length()<6) { password.setError("Mật khẩu cần ít nhất 6 ký tự"); return false; }
        if (signup && App.value(name).length()<2) { name.setError("Nhập đầy đủ họ tên"); return false; }
        return true;
    }
    private void busy(boolean busy) { btnLogin.setEnabled(!busy); btnRegister.setEnabled(!busy); }
    private void login() {
        if (!validate(false)) return;
        busy(true); message.setText("Đang đăng nhập...");
        FirebaseAuth.getInstance().signInWithEmailAndPassword(App.value(email),App.value(password))
            .addOnSuccessListener(r -> openByRole())
            .addOnFailureListener(e -> { busy(false); message.setText("Đăng nhập thất bại: "+e.getLocalizedMessage()); });
    }
    private void register() {
        if (!validate(true)) return;
        busy(true); message.setText("Đang tạo tài khoản...");
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(App.value(email),App.value(password))
            .addOnSuccessListener(r -> {
                Map<String,Object> data = new HashMap<>();
                data.put("role", "patient"); // Do NOT allow client to create doctor accounts.
                data.put("email",App.value(email));
                data.put("name",App.value(name));
                data.put("phone",""); data.put("dob",""); data.put("gender","");
                data.put("address",""); data.put("medicalHistory","");
                data.put("createdAt",FieldValue.serverTimestamp());
                App.db().collection("users").document(App.uid()).set(data)
                    .addOnSuccessListener(v -> { busy(false); openByRole(); })
                    .addOnFailureListener(e -> {
                        busy(false); message.setText("Đã tạo tài khoản Auth nhưng chưa lưu hồ sơ: "+e.getLocalizedMessage()+". Liên hệ quản trị viên để bổ sung hồ sơ.");
                        FirebaseAuth.getInstance().signOut();
                    });
            }).addOnFailureListener(e -> { busy(false); message.setText("Lỗi đăng ký: "+e.getLocalizedMessage()); });
    }
    private void openByRole() {
        if (App.user()==null) return;
        App.db().collection("users").document(App.uid()).get()
            .addOnSuccessListener(d -> {
                String role=App.str(d,"role");
                if (!d.exists() || (!role.equals("patient") && !role.equals("doctor"))) {
                    FirebaseAuth.getInstance().signOut();
                    message.setText("Tài khoản chưa được cấp hồ sơ/vai trò hợp lệ trong Firestore.");
                    busy(false); return;
                }
                Intent i=new Intent(this,DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }).addOnFailureListener(e -> { busy(false); App.error(this,e); });
    }
}
