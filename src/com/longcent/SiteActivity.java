package com.longcent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.longcent.db.Site;
import com.longcent.utils.FtpClient;
import com.longcent.utils.FtpTask;
import com.longcent.utils.Utils;

/**
 * 站点详细信息界面
 */
public class SiteActivity extends Activity {
    ProgressDialog progressDialog;
    Handler handler;
    EditText[] editTexts = new EditText[8];
    /**数据库列*/
    String[] dbFields = {
            Site.NAME,
            Site.HOST,
            Site.PORT,
            Site.ACCOUNT,
            Site.PASSWD,
            Site.ACCOUNT1,
            Site.PASSWD1,
            Site.ADDR
    };
    /**对应的文本框控件*/
    int[] editTextIds = {
            R.id.site_name,
            R.id.host,
            R.id.port,
            R.id.account,
            R.id.password,
            R.id.account1,
            R.id.password1,
            R.id.address
    };
    /**
     * 必选项
     */
    boolean[] editTextRequired = {
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false
    };

    Button signIn, save;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.site);

        for (int i = 0; i < editTexts.length; i++) {
            editTexts[i] = (EditText) findViewById(editTextIds[i]);
            if (editTextRequired[i]) {
                editTexts[i].setHint(R.string.required);
            }
        }

        signIn = (Button) findViewById(R.id.signin);
        //登录
        signIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (checkValidations()) {
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(SiteActivity.this);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            String siteName = Utils.getEditTextStringValue(editTexts[0]);
                            String host = Utils.getEditTextStringValue(editTexts[1]);
                            String port = Utils.getEditTextStringValue(editTexts[2]);
                            String account = Utils.getEditTextStringValue(editTexts[3]);
                            String passwd = Utils.getEditTextStringValue(editTexts[4]);

                            FtpClient ftpClient = new FtpClient();
                            showWait(String.format(getString(R.string.connecting), siteName));
                            boolean success = ftpClient.connect(host, Integer.parseInt(port)) == FtpClient.CONNECT_SUCCESS;

                            if (!success) {
                                hideWait();
                                sendMessage(String.format(getString(R.string.connectError), siteName));
                                return;
                            }

                            showWait(String.format(getString(R.string.signning_in), siteName));
                            success = ftpClient.login(account, passwd) == FtpClient.LOGIN_OK;
                            hideWait();
                            if (!success) {
                                sendMessage(String.format(getString(R.string.incorrectUser), siteName));
                                return;
                            }

                            sendMessage(getString(R.string.loginSuccess));
                            handler.sendEmptyMessage(0);
                        }
                    }).start();
                }
            }
        });

        save = (Button) findViewById(R.id.save);
        //保存
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (checkValidations())
                    save();
            }
        });

        setData();

        initHandler();
    }

    private void save() {
        Intent data = new Intent();
        for (int i = 0; i < dbFields.length; i++) {
            data.putExtra(dbFields[i], Utils.getEditTextStringValue(editTexts[i]));
        }
        setResult(RESULT_OK, data);
        this.finish();
    }

    private void setData() {
        Bundle record = this.getIntent().getExtras();
        if (record != null) {
            for (int i = 0; i < editTexts.length; i++) {
                editTexts[i].setText(record.getString(dbFields[i]));
            }
        }
    }

    /**
     * 验证文本框
     * @return
     */
    private boolean checkValidations() {
        for (int i = 0; i < editTexts.length; i++) {
            if (editTextRequired[i] && Utils.isEditTextValueEmpty(editTexts[i])) {
                Toast.makeText(this, R.string.needRequired, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        int p = Integer.parseInt(Utils.getEditTextStringValue(editTexts[2]));
        if (p <= 0 || p > 65535) {
            Toast.makeText(this, R.string.portError, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showWait(String text) {
        Message message = new Message();
        message.obj = text;
        message.what = FtpTask.WAIT;
        handler.sendMessage(message);
    }

    private void hideWait() {
        Message message = new Message();
        message.what = FtpTask.STOP_WAIT;
        handler.sendMessage(message);
    }

    private void sendMessage(String msg) {
        Message message = new Message();
        message.obj = msg;
        message.what = FtpTask.MESSAGE;
        handler.sendMessage(message);
    }

    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case FtpTask.WAIT:
                        progressDialog.setMessage(msg.obj.toString());
                        progressDialog.show();
                        break;
                    case FtpTask.STOP_WAIT:
                        progressDialog.dismiss();
                        break;
                    case FtpTask.MESSAGE:
                        Toast.makeText(SiteActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                        break;
                    default:
                        progressDialog.dismiss();
                        progressDialog = null;
                        save();
                }
            }
        };
    }
}
