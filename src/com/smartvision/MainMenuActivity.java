package com.smartvision;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import com.smartvision.adapters.MenuAdapter;

/**
 *主菜单界面
 */
public class MainMenuActivity extends Activity {
    private GridView mGrid;
    private String[] menuItemActivities;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        menuItemActivities = this.getResources().getStringArray(R.array.mainMenuActivities);
        mGrid = (GridView) findViewById(R.id.gridview);
        mGrid.setAdapter(new MenuAdapter(this));

        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(menuItemActivities[position]);
                startActivity(intent);
            }
        });
    }
}
