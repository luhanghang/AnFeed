package com.longcent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.longcent.MainMenuActivity;
import com.longcent.R;

/**
 * 主菜单适配器
 */
public class MenuAdapter extends BaseAdapter {
    private LayoutInflater inflater;

    private Integer[] icons = {
                    R.drawable.menu_upload,
                    R.drawable.taskman,
                    R.drawable.menu_setting,
                    R.drawable.menu_signin,
                    R.drawable.menu_help,
                    R.drawable.menu_about
            };

    private String[] labels;

    public MenuAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        labels = context.getResources().getStringArray(R.array.mainMenuItems);;
    }

    public int getCount() {
        return icons.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View view, ViewGroup parent) {
        if (null == view) {
            view = inflater.inflate(R.layout.menuitem, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.itemIcon);
            TextView textView = (TextView) view.findViewById(R.id.itemLabel);

            imageView.setImageResource(icons[position]);
            textView.setText(labels[position]);
        }
        return view;
    }
}
