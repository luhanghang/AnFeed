package com.smartvision;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * 帮助界面
 */
public class HelpActivity extends Activity {
    String[] items;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = this.getResources().getStringArray(R.array.helpTopic);
        ListView listView = new ListView(this);
        setContentView(listView);
        listView.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent("HelpItem");
                intent.putExtra("index", i);
                startActivity(intent);
            }
        });
    }
}
