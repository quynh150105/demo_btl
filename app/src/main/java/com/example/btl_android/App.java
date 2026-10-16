package com.example.btl_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public final class App {
    private App() { }
    public static boolean configured(Activity a) {
        if (FirebaseApp.getApps(a).isEmpty()) {
            new AlertDialog.Builder(a)
                .setTitle("Chưa cấu hình Firebase")
                .setMessage("Tải google-services.json từ Firebase Console, đặt tại app/google-services.json và Sync Gradle. Xem README.md.")
                .setPositiveButton("Đã hiểu", null).show();
            return false;
        }
        return true;
    }
    public static void applyInsets(Activity a) {
        ViewGroup container=a.findViewById(android.R.id.content);
        if (container==null || container.getChildCount()==0) return;
        View root=container.getChildAt(0);
        final int l=root.getPaddingLeft(),t=root.getPaddingTop(),r=root.getPaddingRight(),b=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,windowInsets)->{
            Insets bars=windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(l+bars.left,t+bars.top,r+bars.right,b+bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static FirebaseUser user() { return FirebaseAuth.getInstance().getCurrentUser(); }
    public static String uid() { FirebaseUser u = user(); return u == null ? "" : u.getUid(); }
    public static String value(EditText e) { return e.getText().toString().trim(); }
    public static String str(DocumentSnapshot d,String key) { String v = d.getString(key); return v == null ? "" : v; }
    public static String str(QueryDocumentSnapshot d,String key) { String v = d.getString(key); return v == null ? "" : v; }
    public static String date(Timestamp t) {
        return t == null ? "Chưa có ngày" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(t.toDate());
    }
    public static void note(Context c,String s) { Toast.makeText(c,s,Toast.LENGTH_LONG).show(); }
    public static void error(Context c,Exception e) {
        String m = e == null ? "Không rõ nguyên nhân" : e.getLocalizedMessage();
        note(c,"Lỗi: "+m);
    }
    public static void logout(Activity a) {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(a, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(i);
    }
    public static void requireLogin(Activity a) {
        if (user() == null) { a.startActivity(new Intent(a, AuthActivity.class)); a.finish(); }
    }
}
