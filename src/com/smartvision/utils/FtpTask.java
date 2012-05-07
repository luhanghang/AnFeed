package com.smartvision.utils;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.smartvision.R;
import com.smartvision.db.Media;
import com.smartvision.db.Site;
import com.smartvision.db.Task;
import com.smartvision.db.TaskSite;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上传任务
 */
public class FtpTask {
    /**当前状态:未执行*/
    public static final int IDLE = 0;
    /**当前状态:等待*/
    public static final int QUEUE = 1;
    /**当前状态:正在上传*/
    public static final int RUNNING = 2;
    /**当前状态:暂停中*/
    public static final int PAUSE = 3;
    /**当前状态:已完成*/
    public static final int FINISHED = 4;

    /**消息类型:状态*/
    public static final int STATE = 0;
    /**消息类型:进度*/
    public static final int PROGRESS = 1;
    /**消息类型:文本消息*/
    public static final int MESSAGE = 2;
    /**消息类型:请等待*/
    public static final int WAIT = 3;
    /**消息类型:完成等待*/
    public static final int STOP_WAIT = 4;

    public static Handler siteHandler, mediaHandler, mainHandler;

    /**任务队列*/
    private static List<FtpTask> siteQueue = new ArrayList<FtpTask>();
    /**当前任务*/
    private static FtpTask sitesRunning;
    /**已完成任务*/
    private static Map<String, FtpTask> siteFinished = new HashMap<String, FtpTask>();

    /**任务id*/
    private String taskId;
    /**站点id*/
    private String siteId;
    /**key=任务id+站点id*/
    private String key;
    /**当前进度*/
    private int progress;
    private Activity context;
    /**当前任务站点下各附件进度*/
    private Map<String, Integer> mediaProgress = new HashMap<String, Integer>();
    /**正在上传的附件id*/
    private String uploadingMediaId;
    /**正在上传的文件字节数*/
    private long uploading_file_size;
    /**所有附件字节数*/
    private long total_size;
    /**已上传字节数*/
    private long total_uploaded;
    private FtpClient ftpClient;
    private TaskSite taskSite;
    private Bundle siteInf;

    private List<String> idList;
    private List<File> fileList;
    private List<String> remoteNameList;
    /**当前状态*/
    private volatile int state;
    /**上次状态*/
    private volatile int previous_state;

    /**开始下一个任务*/
    public static void startNext() {
        if (siteQueue.size() > 0 && sitesRunning == null) {
            FtpTask ftpTask = siteQueue.get(0);
            siteQueue.remove(ftpTask);
            sitesRunning = ftpTask;
            ftpTask.updateState(RUNNING);
            ftpTask.run();
        }
    }

    /**
     * 获取正在上传的任务
     * @return
     */
    public static FtpTask getSitesRunning() {
        return sitesRunning;
    }

    /**
     * 根据key获得任务
     * @param key
     * @return
     */
    public static FtpTask get(String key) {
        if (sitesRunning != null && sitesRunning.getKey().equals(key)) return sitesRunning;
        FtpTask ftpTask = siteFinished.get(key);
        if (ftpTask != null) return ftpTask;
        for (FtpTask task : siteQueue) {
            if (task.getKey().equals(key)) return task;
        }
        return null;
    }

    /**
     * 生成key
     * @param taskId
     * @param siteId
     * @return
     */
    public static String genKey(String taskId, String siteId) {
        return taskId + "_" + siteId;
    }

    public static String genKey(String taskId, String siteId, String mediaId) {
        return genKey(taskId, siteId) + "_" + mediaId;
    }

    /*
    获取key
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取Ftp任务对应的任务id
     * @return
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取当前状态
     * @return
     */
    public int getState() {
        return state;
    }

    public FtpTask(Activity context, String taskId, String siteId) {
        this.taskId = taskId;
        this.siteId = siteId;
        this.state = IDLE;
        this.context = context;
        key = genKey(taskId, siteId);
    }

    /**
     * 更新进度
     * @param uploadedBytes
     * @param increment
     */
    private void updateProgress(long uploadedBytes, long increment) {
        if (increment == 0) return;
        int mediaProgress = (int) (uploadedBytes * 100 / uploading_file_size); //附件进度
        total_uploaded += increment;
        int siteProgress = (int) (total_uploaded * 100 / total_size); //站点进度
        updateMediaProgress(mediaProgress);
        updateSiteProgress(siteProgress);
    }

    /*
    <NewsWebInputXml>
        <CmdType>PutClueInfo</CmdType>
        <Info>
            <NewsInfo>
                <When>2011-12-9 10:38</When>
                <Where>???123</Where>
                <What>???</What>
                <Who></Who>
                <Why>????</Why>
                <Key></Key>
            </NewsInfo>
            <OfferInfo>
                <PhoneNumber></PhoneNumber>
                <Email></Email>
                <OfferName></OfferName>
            </OfferInfo>
            <Resource>
                <FileName>Video 10.mov</FileName>
            </Resource>
        </Info>
    </NewsWebInputXml>

    return
    <NewsWebOutputXml>
        <CmdType>PutClueInfo</CmdType>
        <Result>0</Result>
        <Info/>
    </NewsWebOutputXml>
     */

    /**
     * 上传元数据
     */
    private void uploadMetaInf() {
        Task task = new Task(context);
        Bundle taskInf = task.fetch(taskId);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<NewsWebInputXml><CmdType>PutClueInfo</CmdType><Info><NewsInfo>");
        stringBuffer.append("<When>").append(taskInf.getString(Task.CREATE)).append("</When>");
        stringBuffer.append("<Where>").append(taskInf.getString(Task.LOCATION)).append("</Where>");
        stringBuffer.append("<What>").append(taskInf.getString(Task.TITLE)).append("</What>");
        stringBuffer.append("<Who>").append(taskInf.getString(siteInf.getString(Site.ACCOUNT1))).append("</Who>");
        stringBuffer.append("<Why>").append(taskInf.getString(Task.CATEGORY)).append("</Why>");
        stringBuffer.append("<Key>").append(taskInf.getString(Task.KEYWORDS)).append("</Key>");
        stringBuffer.append("</NewsInfo><OfferInfo><PhoneNumber>").append(Utils.getDeviceInf(context).getLine1Number()).append("</PhoneNumber><Email></Email><OfferName></OfferName></OfferInfo>");
        stringBuffer.append("<Resource>");
        if (remoteNameList != null) {
            for (String remoteName : remoteNameList) {
                stringBuffer.append("<FileName>").append(remoteName).append("</FileName>");
            }
        }
        stringBuffer.append("</Resource>");
        stringBuffer.append("</Info>");
        stringBuffer.append("</NewsWebInputXml>");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        try {
            String iso = new String(stringBuffer.toString().getBytes("UTF-8"),"ISO-8859-1");
            nameValuePairs.add(new BasicNameValuePair("strXml", new String(iso.getBytes("ISO-8859-1"),"GB2312")));
            Utils.httpPost(siteInf.getString(Site.ADDR), nameValuePairs);
        } catch (Exception e) {

        }
    }

    /**
     * 当前任务完成
     */
    private void finish() {
        uploadMetaInf();
        clear();
        taskSite.setFinish(taskId, siteId, true);
        updateState(FINISHED);

        if (taskSite.isTaskFinished(taskId)) {
            Task task = new Task(context);
            task.setFinish(taskId, true);
            clearFinishedTaskSites(taskId);
            mainHandler.sendEmptyMessage(0);
        } else {
            siteFinished.put(key, this);
        }
    }

    /**
     * 从已完成任务列表中删除任务id为taskId的ftp任务
     * @param taskId
     */
    private void clearFinishedTaskSites(String taskId) {
        List<String> keys = new ArrayList<String>();
        for (String key : siteFinished.keySet()) {
            if (key.startsWith(taskId + "_")) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            siteFinished.remove(key);
        }
    }

    /**
     * 重置ftp任务
     */
    private void clear() {
        sitesRunning = null;
        idList = null;
        fileList = null;
        updateState(IDLE);
        startNext();
    }

    /**
     * 获取当前进度
     * @return
     */
    public int getProgress() {
        return progress;
    }

    /**
     * 发送消息
     * @param type
     * @param arg1
     */
    private void sendMessage(int type, int arg1) {
        Message message = new Message();
        message.arg1 = arg1;
        message.what = type;
        message.obj = key;
        dispatchMessage(message);
    }

    /**
     * 给siteHanlder发送消息
     * @param message
     */
    private void dispatchMessage(Message message) {
        if (siteHandler != null) {
            siteHandler.sendMessage(message);
        }
    }

    private void sendMessage(int type, String msg) {
        Message message = new Message();
        message.what = type;
        message.obj = msg;
        dispatchMessage(message);
    }

    /**
     * 更新当前状态
     * @param state
     */
    private void updateState(int state) {
        this.previous_state = this.state;
        this.state = state;
        sendMessage(STATE, state);
    }

    /**
     * 更新站点进度
     * @param progress
     */
    private void updateSiteProgress(int progress) {
        this.progress = progress;
        sendMessage(PROGRESS, progress);
    }

    /**
     * 更新附件进度
     * @param progress
     */
    private void updateMediaProgress(int progress) {
        mediaProgress.put(uploadingMediaId, progress);
        if (mediaHandler == null) return;
        Message message = new Message();
        message.arg1 = progress;
        message.obj = genKey(taskId, siteId, uploadingMediaId);
        mediaHandler.sendMessage(message);
    }

    /**
     * 删除ftp任务
     */
    public void delete() {
        if (state == QUEUE) {
            siteQueue.remove(this);
            return;
        }
        if (ftpClient != null) {
            ftpClient.stop();
            clear();
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        updateState(PAUSE);
        if (ftpClient != null) {
            ftpClient.pause();
        }
    }

    /**
     * 恢复
     */
    public void resume() {
        updateState(this.previous_state);
        if (ftpClient != null) {
            if (ftpClient.getState() == FtpClient.PAUSE) {
                ftpClient.resume(); //如果已暂停就恢复
            } else { //如果已停止就重新开始
                run();
            }
        }
    }

    /**
     * 加入任务队列
     */
    public void addToQueue() {
        if (siteFinished.containsKey(key)) {
            siteFinished.remove(key);
        }
        state = IDLE;
        siteQueue.add(this);
        updateState(QUEUE);
        startNext();
    }

    /**
     * 获取附件进度
     * @param mediaId
     * @return
     */
    public int getMediaProgress(String mediaId) {
        Integer progress = mediaProgress.get(mediaId);
        if (progress == null) return 0;
        return progress;
    }

    /**
     * 设置完成状态
     */
    public void setFinished() {
        state = FINISHED;
        siteFinished.put(key, this);
    }

    /**
     * 开始上传
     */
    private void run() {
        new LoginThread().start();
    }

    /**
     * 登录线程
     */
    private class LoginThread extends Thread {
        public void run() {
            if (siteInf == null) {
                Site site = new Site(context);
                siteInf = site.fetch(siteId);
            }

            if (ftpClient == null)
                ftpClient = new FtpClient();

            String siteName = siteInf.getString(Site.NAME);

            sendMessage(WAIT, String.format(context.getString(R.string.connecting), siteName));
            //Connect error
            if (ftpClient.connect(siteInf.getString(Site.HOST), Integer.parseInt(siteInf.getString(Site.PORT))) != FtpClient.CONNECT_SUCCESS) {
                sendMessage(STOP_WAIT, null);
                sendMessage(MESSAGE, String.format(context.getString(R.string.connectError), siteName));
                clear();
                return;
            }
            //Login error
            sendMessage(WAIT, String.format(context.getString(R.string.signning_in), siteName));
            if (ftpClient.login(siteInf.getString(Site.ACCOUNT), siteInf.getString(Site.PASSWD)) != FtpClient.LOGIN_OK) {
                ftpClient.disconnect();
                sendMessage(STOP_WAIT, null);
                sendMessage(MESSAGE, String.format(context.getString(R.string.incorrectUser), siteName));
                clear();
                return;
            }

            sendMessage(STOP_WAIT, null);

            total_size = 0;
            Media media = new Media(context);
            List<Map<String, String>> medias = media.getByTaskId(taskId);
            idList = new ArrayList<String>();
            fileList = new ArrayList<File>();
            remoteNameList = new ArrayList<String>();
            for (int i = 0; i < medias.size(); i++) {
                Map<String, String> item = medias.get(i);
                String mediaId = item.get(Media.ID);
                Uri uri = Uri.parse(item.get(Media.URI));
                File file = Utils.getFileFromUri(context, uri);
                if (file == null) {
                    continue;
                }
                total_size += file.length();
                idList.add(mediaId);
                fileList.add(file);
                remoteNameList.add(item.get(Media.REMOTE_NAME));
            }

            new UpLoadThread().start();
            new UpdateProgressThread().start();
        }
    }

    /**
     * 上传线程
     */
    private class UpLoadThread extends Thread {
        public void run() {
            if (taskSite == null) {
                taskSite = new TaskSite(context);
            }
            taskSite.setFinish(taskId, siteId, false);
            total_uploaded = 0;

            boolean isErrorOccurred = false;
            for (int i = 0; idList != null && i < idList.size(); i++) {
                try {
                    File file = fileList.get(i);
                    uploadingMediaId = idList.get(i);
                    uploading_file_size = file.length();
                    ftpClient.upload(file, remoteNameList.get(i));
                    if (ftpClient.getState() == FtpClient.STOP) {
                        break;
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    isErrorOccurred = true;
                    sendMessage(MESSAGE, "Error:" + e.getLocalizedMessage());
                }
            }

            ftpClient.disconnect();
            if (ftpClient.getState() == FtpClient.STOP) {
                return;
            }

            if (idList == null || isErrorOccurred) {
                clear();
            } else {
                finish();
            }
        }
    }

    /**
     * 更新进度线程
     */
    private class UpdateProgressThread extends Thread {
        public void run() {
            while (true) {
                if (state == FINISHED || state == IDLE) {
                    break;
                }
                try {
                    if (state == RUNNING) {
                        updateProgress(ftpClient.getUploadedBytes(), ftpClient.getIncrement());
                    }
                    Thread.sleep(10);
                } catch (Exception e) {

                }
            }
        }
    }
}