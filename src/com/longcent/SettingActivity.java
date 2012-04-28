package com.longcent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.longcent.db.Site;
import com.longcent.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设置界面
 */
public class SettingActivity extends Activity implements View.OnClickListener, AdapterView.OnItemLongClickListener {
    public static final int ADD = 0; //requestCode:添加新记录
    ListView listView;
    SimpleAdapter listAdapter;
    List<Map<String, String>> sites; //站点列表<数据库记录id,站点名称>
    String[] from = {Site.NAME};
    int[] to = {android.R.id.text1};
    Site site;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sites);

        initSiteNames(); //初始化站点列表

        listView = (ListView) findViewById(R.id.sites);
        listAdapter = new SimpleAdapter(this, sites, android.R.layout.simple_list_item_multiple_choice, from, to);
        //listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, siteNames);
        listView.setAdapter(listAdapter);

        ImageView add = Utils.setSwitchButton((ImageView) findViewById(R.id.addSite), R.drawable.add_u, R.drawable.add_d); //添加按钮
        add.setOnClickListener(this);

        ImageView trash = Utils.setSwitchButton((ImageView) findViewById(R.id.removeSite), R.drawable.trash_u, R.drawable.trash_d); //删除按钮
        trash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final List<Integer> positions = new ArrayList<Integer>(); //保存选中记录在列表中的位置
                final List<String> ids = new ArrayList<String>(); //保存选中记录数据库id
                for (int i = 0; i < sites.size(); i++) {
                    if (listView.isItemChecked(i)) {
                        positions.add(i);
                        ids.add(getSiteIdByIndex(i));
                    }
                }

                if (positions.size() > 0) { //如果没有选中记录不做任何操作
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage(getString(R.string.confirmRemoveSelectedSites)).setCancelable(false).setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for (int i = 0; i < sites.size(); i++) {
                                listView.setItemChecked(i, false); //清空选中记录
                            }
                            site.delete(ids); //从数据库中删除选中记录
                            //从站点列表和站点名称列表中删除选中记录
                            for (int i = positions.size() - 1; i >= 0; i--) {
                                int p = positions.get(i);
                                sites.remove(p);
                            }
                            listAdapter.notifyDataSetChanged(); //刷新listView
                        }
                    }).setNegativeButton(getString(R.string.no), null);
                    builder.show();
                }
            }
        });

        //长按进入编辑界面
        listView.setOnItemLongClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        Bundle record = data.getExtras();
        String name = record.getString(Site.NAME);
        String id;
        if (requestCode == ADD) {  //添加站点
            id = site.create(record) + "";
            Map<String, String> newSite = new HashMap<String, String>();
            newSite.put(Site.ID, id + "");
            newSite.put(Site.NAME, name);
            sites.add(newSite);
            listAdapter.notifyDataSetChanged();
            listView.setSelection(sites.size() - 1);
        } else { //更新站点信息
            int index = requestCode - 100;
            Map<String, String> s = sites.get(index);
            id = s.get(Site.ID);
            site.update(id, record);
            s.put(Site.NAME, name);
            sites.set(index, s);
            listAdapter.notifyDataSetChanged();
        }
    }

    protected void initSiteNames() {
        site = new Site(this);
        sites = site.fetchAll();
    }

    public void onClick(View view) {
        Intent intent = new Intent("SiteActivity");
        startActivityForResult(intent, ADD);
    }

    protected String getSiteIdByIndex(int index) {
        return sites.get(index).get(Site.ID);
    }

    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Bundle record = site.fetch(getSiteIdByIndex(i));
        Intent intent = new Intent("SiteActivity");
        intent.putExtras(record);
        startActivityForResult(intent, i + 100); //为避免与添加站点的requestCode冲突,编辑站点的requestCode为选中项在listView中的位置+100
        return true;
    }
}
