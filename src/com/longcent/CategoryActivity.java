package com.longcent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/**
 * 分类界面
 */
public class CategoryActivity extends Activity {
    ListView listView;
    String[] items;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = this.getResources().getStringArray(R.array.cateArray);
        listView = new ListView(this);

        listView.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, items));
        setContentView(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int i, long l) {
                Intent data = new Intent();
                data.putExtra("cate", items[i]);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
}
