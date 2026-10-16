package com.example.btl_android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordsActivity extends AppCompatActivity {
    private String role="";
    private final List<DocumentSnapshot> records=new ArrayList<>();
    private ListenerRegistration listener;
    private RecordsAdapter adapter;
    private TextView empty;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);App.requireLogin(this);if(App.user()==null)return;
        setContentView(R.layout.activity_records); App.applyInsets(this);
        // Quay về màn hình trước, giữ nguyên trạng thái đăng nhập Firebase.
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        empty=findViewById(R.id.txtEmpty);
        RecyclerView recycler=findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter=new RecordsAdapter(); recycler.setAdapter(adapter);
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(d->{
            role=App.str(d,"role");
            if(!role.equals("doctor")&&!role.equals("patient")){App.logout(this);return;}
            String filter=role.equals("doctor")?"doctorId":"patientId";
            listener=App.db().collection("medical_records").whereEqualTo(filter,App.uid()).addSnapshotListener((snap,e)->{
                if(e!=null){empty.setText("Lỗi tải bệnh án: "+e.getLocalizedMessage());return;}
                records.clear(); if(snap!=null)for(QueryDocumentSnapshot r:snap)records.add(r);
                Collections.sort(records,(a,c)->{
                    Timestamp at=a.getTimestamp("visitAt"),ct=c.getTimestamp("visitAt");
                    if(at==null||ct==null)return 0;return ct.compareTo(at);
                });
                empty.setText(records.isEmpty()?"Chưa có bệnh án":"Số bệnh án: "+records.size());
                adapter.notifyDataSetChanged();
            });
        });
    }
    @Override protected void onDestroy(){if(listener!=null)listener.remove();super.onDestroy();}
    private void showRecord(DocumentSnapshot d){
        App.db().collection("treatment_plans").document(d.getId()).get()
                .addOnSuccessListener(t->{
                    String detail="Bệnh nhân: "+App.str(d,"patientName")+"\nBác sĩ: "+App.str(d,"doctorName")
                            +"\nNgày khám: "+App.date(d.getTimestamp("visitAt"))
                            +"\n\nCHẨN ĐOÁN\n"+App.str(d,"diagnosis")
                            +"\n\nĐƠN THUỐC\n"+App.str(d,"prescription")
                            +"\n\nGHI CHÚ\n"+App.str(d,"notes")
                            +"\n\nPHÁC ĐỒ ĐIỀU TRỊ\n";
                    if(t.exists()) detail+="Tên thuốc: "+App.str(t,"medicine")+"\nCách dùng: "+App.str(t,"method")
                            +"\nSố lần/ngày: "+t.getLong("timesPerDay")+"\nMục đích: "+App.str(t,"purpose")
                            +"\nHướng dẫn: "+App.str(t,"guide");
                    else detail+="Chưa có phác đồ";
                    AlertDialog.Builder alert=new AlertDialog.Builder(this).setTitle("Chi tiết bệnh án").setMessage(detail).setNegativeButton("Đóng",null);
                    if(role.equals("doctor"))alert.setPositiveButton("Chỉnh sửa",(dia,w)->{
                        Intent i=new Intent(this,RecordEditorActivity.class);i.putExtra("appointmentId",d.getId());startActivity(i);
                    });
                    alert.show();
                }).addOnFailureListener(e->App.error(this,e));
    }
    private class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.Holder>{
        class Holder extends RecyclerView.ViewHolder {
            TextView title,info;Button btn;
            Holder(View v){super(v);title=v.findViewById(R.id.txtTitle);info=v.findViewById(R.id.txtInfo);btn=v.findViewById(R.id.btnDetail);}
        }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_record,p,false));}
        @Override public int getItemCount(){return records.size();}
        @Override public void onBindViewHolder(@NonNull Holder h,int p){
            DocumentSnapshot d=records.get(p);
            h.title.setText(role.equals("doctor")?"Bệnh nhân: "+App.str(d,"patientName"):"Bác sĩ: "+App.str(d,"doctorName"));
            h.info.setText("Ngày khám: "+App.date(d.getTimestamp("visitAt"))+"\nChẩn đoán: "+App.str(d,"diagnosis"));
            h.btn.setOnClickListener(v->showRecord(d));
        }
    }
}
