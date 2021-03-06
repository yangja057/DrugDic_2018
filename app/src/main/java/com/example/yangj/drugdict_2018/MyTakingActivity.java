package com.example.yangj.drugdict_2018;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;

public class MyTakingActivity extends AppCompatActivity {

    static final int START_GALLERY_CHOOSER = 1000;
    static final int START_CAMERA = 1001;
    private static final int CLOUD_VISION = 2000;
    private static final int ADD_PRESCRIPTION = 2001;

    FloatingActionButton drugFAB;
    FloatingActionButton prescriptionFAB;
    Handler mHandler;
    FirebaseHandler_MyTaking handler;

    ArrayList<ProductInfo> mDrugs;

    private ListView mDrugList;
    private TakingDrugListAdapter adapter;

    LoadingDialog dialog;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_taking);

        setupToolBar();

        drugFAB = (FloatingActionButton) findViewById(R.id.drugFAB);
        prescriptionFAB = (FloatingActionButton) findViewById(R.id.prescriptionFAB);
        mDrugList = (ListView) findViewById(R.id.lvTakingDrugList);
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == FirebaseHandler_MyTaking.GET_COMPLETE){
                    mDrugs = handler.getList();
                    adapter = new TakingDrugListAdapter(getApplicationContext(), R.layout.taking_drug_layout, mDrugs);
                    mDrugList.setAdapter(adapter);
                } else if(msg.what >= 9900){
                    //병용금기 표시
                    Log.v("test", "SEARCH_COMPLETE MESSAGE receive");
                    String interaction = (String)msg.obj;
                    Toast.makeText(MyTakingActivity.this, interaction, Toast.LENGTH_SHORT).show();
                }
            }
        };
        handler = new FirebaseHandler_MyTaking(LoginActivity.uid);
        handler.setHandler(mHandler);
        handler.getAllDrug();

        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(mDrugList,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    handler.deleteDrug(((ProductInfo)adapter.getItem(position)).getName());
                                    mDrugs.remove(position);
                                }
                                adapter = new TakingDrugListAdapter(getApplicationContext(), R.layout.drug_list_row, mDrugs);
                                listView.setAdapter(adapter);
                            }
                        });
        mDrugList.setOnTouchListener(touchListener);
        mDrugList.setOnScrollListener(touchListener.makeScrollListener());

        dialog = new LoadingDialog(this);

        mDrugList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), DrugDetailActivity.class);
                intent.putExtra("drug", mDrugs.get(position));
                intent.putExtra("where", "MyTaking");
                startActivity(intent);
            }
        });
    }

    public void setupToolBar(){
        getSupportActionBar().setTitle("백약사전");
    }

    public void onDrugFAB(View view) {
        Intent intent = new Intent(this, SearchDrugActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onPrescriptionFAB(View view) {
        Intent intent = new Intent(getApplicationContext(), CloudVisionActivity.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(MyTakingActivity.this);

        builder.setMessage(R.string.dialog_select_prompt);
        builder.setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                intent.putExtra("startMode", START_GALLERY_CHOOSER);
                startActivityForResult(intent, CLOUD_VISION);
            }
        });
        builder.setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                intent.putExtra("startMode", START_CAMERA);
                startActivityForResult(intent, CLOUD_VISION);
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(resultCode == RESULT_OK){
            switch (requestCode) {
                case CLOUD_VISION:
                    Log.d("MyTakingActivity", "finish cloud vision");

                    ArrayList<ProductInfo> list;
                    list = (ArrayList) data.getSerializableExtra("detectDrugs");

                    Intent intent = new Intent(getApplicationContext(), AddPrescriptionActivity.class);
                    intent.putExtra("detectDrugs", list);
                    intent.putExtra("imageUri", data.getStringExtra("imageUri"));
                    startActivityForResult(intent, ADD_PRESCRIPTION);

                    break;
                case ADD_PRESCRIPTION:
                    ArrayList<ProductInfo> new_drug = (ArrayList) data.getSerializableExtra("useDrugs");

                    for(int i=0;i<mDrugs.size();i++){
                        for(int j=0;j<new_drug.size();j++){
                            Log.v("test", "MyTakingActivty searchInteraction");
                            Intent intent2 = new Intent(this, DrugDatabaseActivity.class);
                            intent2.putExtra("drug1", mDrugs.get(i).getName());
                            intent2.putExtra("drug2", new_drug.get(j).getName());
                            startActivityForResult(intent2, 1);
                        }
                    }

                    // firebase에 올려주는 작업
                    for(int i=0;i<new_drug.size();i++) {
                        handler.addDrug(new_drug.get(i));
                        mDrugs.add(new_drug.get(i));
                    }
                    adapter = new TakingDrugListAdapter(getApplicationContext(), R.layout.drug_list_row, mDrugs);
                    mDrugList.setAdapter(adapter);
                    break;
            }
        } else if(resultCode == SearchDrugActivity.PUT_IN_BUCKET){
            dialog.show();
            ArrayList<ProductInfo> new_drug = (ArrayList<ProductInfo>) data.getSerializableExtra("addDrug");

            for(int i=0;i<mDrugs.size();i++){
                for(int j=0;j<new_drug.size();j++){
                    Log.v("test", "MyTakingActivty searchInteraction");
                    Intent intent = new Intent(this, DrugDatabaseActivity.class);
                    intent.putExtra("drug1", mDrugs.get(i).getName());
                    intent.putExtra("drug2", new_drug.get(j).getName());
                    startActivityForResult(intent, 1);
                }
            }

            for(int i=0;i<new_drug.size();i++) {
                handler.addDrug(new_drug.get(i));
                mDrugs.add(new_drug.get(i));
            }
            adapter = new TakingDrugListAdapter(getApplicationContext(), R.layout.drug_list_row, mDrugs);
            mDrugList.setAdapter(adapter);
            dialog.dismiss();

        } else if(resultCode == DrugDatabaseActivity.INTERACTION_SEARCH){
            String interaction = data.getStringExtra("interaction");
            String drug1 = data.getStringExtra("drug1");
            String drug2 = data.getStringExtra("drug2");
            if(!interaction.equals("")){
                showPrecautionDialog(interaction, drug1, drug2);
            }
        }
    }

    public void showPrecautionDialog(String interaction, String drug1, String drug2){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.precaution_dialog_title);
        builder.setMessage("[" + drug1 + "]" + "과(와) " + "[" + drug2 + "]" + "을(를) 함께 복용할 시 " +
                "[" + interaction + "]" + " 부작용 발생");
        builder.setPositiveButton(R.string.precaution_dialog_positive_btn,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }
}
