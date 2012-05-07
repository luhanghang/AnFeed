package com.smartvision;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.smartvision.utils.Utils;
import org.apache.commons.net.io.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 关于界面
 */
public class AboutActivity extends Activity {
    /**更新服务器*/
    public final static String VERSION_HOST = "http://www.4g-live.com";
    String newVersionName, versionName;
    int newVersionCode, versionCode;
    String apk;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        setContentView(textView);
        versionName = Utils.getVersionName(this);
        versionCode = Utils.getVersionCode(this);
        textView.setText(getString(R.string.app_name) + " (" + getString(R.string.app_en_name) + ")  " + getString(R.string.version) + ":" + versionName);
        new checkUpgradeTask().execute();
    }

    /**
     * 获取服务上的版本
     */
    private void getServerVersion() {
        newVersionName = versionName;
        newVersionCode = versionCode;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(VERSION_HOST + "/anFeedVersion.txt");
            String verjson = httpClient.execute(httpGet, new BasicResponseHandler());
            //String verjson = "[{\"appname\":\"anFeed\",\"apkname\":\"anFeed.apk\",\"verName\":1.0.1,\"verCode\":2}]";
            JSONArray array = new JSONArray(verjson);
            if (array.length() > 0) {
                JSONObject obj = array.getJSONObject(0);
                try {
                    newVersionCode = Integer.parseInt(obj.getString("verCode"));
                    newVersionName = obj.getString("verName");
                    apk = obj.getString("apkname");
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            Log.e("GetServerVersion", e.getMessage());
        }
    }

    /**
     * 更新
     */
    private void doUpgrade() {
        if (newVersionCode <= versionCode) return;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getString(R.string.foundNewVersion));
        stringBuffer.append(newVersionName);
        stringBuffer.append(",");
        stringBuffer.append(getString(R.string.confirmUpgrade));
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade))
                .setMessage(stringBuffer.toString())
                .setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new UpgradeTask().execute();
                                //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(VERSION_HOST + "/" + APK));
                                //startActivity(intent);
                            }
                        })
                .setNegativeButton(getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

    /**
     * 安装新版本
     */
    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), apk)), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    /**
     * 检查更新线程
     */
    private class checkUpgradeTask extends android.os.AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            doUpgrade();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getServerVersion();
            return null;
        }
    }

    /**
     * 现在更新包线程
     */
    private class UpgradeTask extends android.os.AsyncTask<Void, Integer, Void> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(AboutActivity.this);
            progressDialog.setTitle(getString(R.string.downloading));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        private void showError(String error) {
            progressDialog.dismiss();
            Toast.makeText(AboutActivity.this, getString(R.string.upgradeError) + " (" + error + ")", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            Toast.makeText(AboutActivity.this, getString(R.string.apkDownloaded), Toast.LENGTH_SHORT).show();
            installApk();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(VERSION_HOST + "/" + apk);
                HttpResponse response = client.execute(get);
                HttpEntity entity = response.getEntity();
                progressDialog.setMax((int)entity.getContentLength());
                InputStream is = entity.getContent();
                FileOutputStream fileOutputStream = null;
                if (is != null) {
                    File file = new File(Environment.getExternalStorageDirectory(), apk);
                    fileOutputStream = new FileOutputStream(file);
                    byte[] buf = new byte[1024];
                    int ch;
                    int count = 0;
                    while ((ch = is.read(buf)) != -1) {
                        fileOutputStream.write(buf, 0, ch);
                        count += ch;
                        publishProgress(count);
                    }
                }
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (Exception e) {
                showError(e.getMessage());
            }
            return null;
        }
    }
}
