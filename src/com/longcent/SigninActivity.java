package com.longcent;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.longcent.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录界面
 */
public class SigninActivity extends SettingActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        initSiteNames();

        listView = (ListView) findViewById(R.id.select_site);
        listAdapter = new SimpleAdapter(this, sites, android.R.layout.simple_list_item_checked, from, to);
        //listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, siteNames);
        listView.setAdapter(listAdapter);
        listView.setOnItemLongClickListener(this);

        setSelectedSite();

        ImageView add = Utils.setSwitchButton((ImageView) findViewById(R.id.addNewSite), R.drawable.add_u, R.drawable.add_d);
        add.setOnClickListener(this);

        ImageView done = Utils.setSwitchButton((ImageView) findViewById(R.id.select_site_done), R.drawable.check_u, R.drawable.check_d);
        //选取完成
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                List<String> ids = new ArrayList<String>();
                for (int i = 0; i < sites.size(); i++) {
                    if (listView.isItemChecked(i)) {
                        ids.add(getSiteIdByIndex(i));
                    }
                }
                site.setSelected(ids);
                finish();
            }
        });
    }

    /**
     * 获得已选择站点
     */
    private void setSelectedSite() {
        List<String> ids = site.getSelectedIds();
        for (int i = 0; i < sites.size(); i++) {
            if (ids.contains(getSiteIdByIndex(i))) {
                listView.setItemChecked(i, true);
            }
        }
    }
}
