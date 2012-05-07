package com.smartvision.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * 工具类
 */
public class Utils {
    /**
     * 文本编辑框是否为空
     * @param editText
     * @return
     */
    public static boolean isEditTextValueEmpty(EditText editText) {
        return getEditTextStringValue(editText).length() == 0;
    }

    /**
     * 获得文本编辑框内容
     * @param editText
     * @return
     */
    public static String getEditTextStringValue(EditText editText) {
        return editText.getText().toString().trim();
    }

    /**
     * 转换文件大小显示格式
     * @param size
     * @return
     */
    public static String convertFileSize(long size) {
        double k = size / 1024;
        if (k < 1) {
            return size + "B";
        }
        double m = k / 1024;
        if (m > 1) {
            return String.format("%.2fMB", m);
        }
        return String.format("%.2fKB", k);
    }

    /**
     * 从文件uri获得文件
     * @param context
     * @param uri
     * @return
     */
    public static File getFileFromUri(Activity context, Uri uri) {
        String[] proj = new String[1];
        if (uri.toString().indexOf("/images/") > 0) {
            proj[0] = MediaStore.Images.Media.DATA;
        } else {
            proj[0] = MediaStore.Video.Media.DATA;
        }
        Cursor cursor = context.managedQuery(uri, proj, null, null, null);
        if (cursor == null || cursor.getCount() == 0) return null;
        int columnIndex = cursor.getColumnIndexOrThrow(proj[0]);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        return new File(path);
    }

    /**
     * 修改标签名称
     * @param tabHost
     * @param tabIndex
     * @param text
     */
    public static void changeTabTitle(TabHost tabHost, int tabIndex, String text) {
        TextView title = (TextView) tabHost.getTabWidget().getChildAt(tabIndex).findViewById(android.R.id.title);
        title.setText(text);
    }

    /**
     * 获取缩略图
     * @param contentResolver
     * @param uri
     * @param height
     * @return
     * @throws Exception
     */
    public static Bitmap getThumbnail(ContentResolver contentResolver, Uri uri, int height) throws Exception {
        Uri thumb = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, uri.getLastPathSegment());
        return MediaStore.Images.Media.getBitmap(contentResolver, thumb);
        /*
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap;
        bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options);

        options.inJustDecodeBounds = false;
        int be = (int) (options.outHeight / (float) height);
        if (be <= 0)
            be = 1;
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, options);
        return bitmap;
        */
    }

    /**
     * 获取本应用程序当前版本编码
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        int versionCode = 1;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception e) {

        }
        return versionCode;
    }

    /**
     * 获取本应用程序当前版本号
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        String versionName = "1.0";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {

        }
        return versionName;
    }

    /**
     * 设置up down按钮
     * @param imageView
     * @param upId
     * @param downId
     * @return
     */
    public static ImageView setSwitchButton(ImageView imageView, int upId, int downId) {
        final int _upId = upId;
        final int _downId = downId;
        imageView.setImageResource(upId);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ImageView _imageView = (ImageView) view;
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        _imageView.setImageResource(_downId);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_OUTSIDE:
                        _imageView.setImageResource(_upId);
                        break;
                }
                return false;
            }
        });
        return imageView;
    }

    /**
     * 获得设备信息
     * @param context
     * @return
     */
    public static TelephonyManager getDeviceInf(Context context) {
        return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Post XML
     * @param url
     * @param xml
     * @return
     * @throws Exception
     */
    public static String postXML(String url, String xml) throws Exception {
        URL u = new URL(url);
        URLConnection con = u.openConnection();
        con.setReadTimeout(10000);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "text/xml");

        PrintWriter post = new PrintWriter(new OutputStreamWriter(con
                .getOutputStream()), true);
        post.println(xml);

        InputStreamReader isr = new InputStreamReader(con.getInputStream());
        char[] resultBytes = new char[1024];
        int rcount = isr.read(resultBytes, 0, 1024);
        StringBuffer soapResponse = new StringBuffer();
        while (rcount != -1) {
            soapResponse = soapResponse.append(resultBytes, 0, rcount);
            rcount = isr.read(resultBytes, 0, 1024);
        }
        return soapResponse.toString();
    }

    /**
     * http post 数据
     * @param url
     * @param params
     */
    public static void httpPost(String url, List<NameValuePair> params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("charset", "GB2312");
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params,"GB2312"));
            HttpResponse response = httpclient.execute(httppost);
            Log.e("httpPost", response.toString());
        } catch (ClientProtocolException e) {
            Log.e("httpPost", e.getMessage());
        } catch (IOException e) {
            Log.e("httpPost", e.getMessage());
        }
    }
}
