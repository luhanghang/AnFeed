package com.longcent.utils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Ftp客户端
 */
public class FtpClient {
    public final static int FILE_EXISTS = 1;
    public final static int UPLOAD_FROM_BREAK_SUCCESS = 0;
    public final static int UPLOAD_FROM_BREAK_FAILED = 4;
    public final static int UPLOAD_NEW_FILE_SUCCESS = 0;
    public final static int UPLOAD_NEW_FILE_FAILED = 6;
    public final static int DELETE_REMOTE_FAILED = 7;

    public final static int LOGIN_OK = 0;
    public final static int CONNECT_SUCCESS = 0;

    public final static int RUNNING = 0;
    public final static int STOP = 1;
    public final static int PAUSE = 2;

    private String charset = "UTF8";
    private String from_charset = "ISO-8859-1";
    private Object object = new Object();
    private long uploadedBytes, previous_uploadedBytes;
    private FTPClient ftpClient;
    /**
     * 当前状态 RUNNING,STOP,PAUSE
     */
    private volatile int state;

    public FtpClient() {
        ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(3000);
    }

    /**
     * 连接ftp服务器
     * @param hostname
     * @param port
     * @return CONNECT_SUCCESS?-1
     */
    public int connect(String hostname, int port) {
        if (ftpClient.isConnected()) return CONNECT_SUCCESS;
        try {
            ftpClient.connect(hostname, port);
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.enterLocalPassiveMode();
            } else {
                ftpClient.disconnect();
                return -1;
            }
            //ftpClient.setControlEncoding("UTF8");
        } catch (Exception e) {
            return -1;
        }
        log("Connected");
        return CONNECT_SUCCESS;
    }

    /**
     * 登录
     * @param username
     * @param password
     * @return LOGIN_OK?-1
     */
    public int login(String username, String password) {
        try {
            log("Logging in");
            return ftpClient.login(username, password) ? LOGIN_OK : -1;
        } catch (Exception e) {
        }
        return -1;
    }

    /**
     * 转换编码格式
     * @param str
     * @return
     */
    private String convertCharset(String str) {
        try {
            return new String(str.getBytes(this.charset), this.from_charset);
        } catch (Exception e) {

        }
        return "";
    }

    /**
     * 上传
     * @param local 本地文件
     * @param remote 远程文件名
     * @return  上传结果状态
     * @throws Exception
     */
    public int upload(File local, String remote) throws Exception {
        state = RUNNING;
        int result;
        previous_uploadedBytes = 0;
        uploadedBytes = 0;
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding(this.charset);
        String remoteFileName = this.convertCharset(remote);
        if (remote.contains("/")) {
            remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
            if (!createDirecroty(remote)) {
                return -1;
            }
        }

        FTPFile[] files = ftpClient.listFiles(remoteFileName);
        if (files.length == 1) {
            long remoteSize = files[0].getSize();
            long localSize = local.length();
            if (remoteSize == localSize) {
                uploadedBytes = localSize;
                return FILE_EXISTS; //文件已存在且大小相同
            } else if (remoteSize > localSize) {
                return uploadFile(remoteFileName, local, 0);
            }

            result = uploadFile(remoteFileName, local, remoteSize);

            if (result == UPLOAD_FROM_BREAK_FAILED) {
                if (!ftpClient.deleteFile(remoteFileName)) {
                    return DELETE_REMOTE_FAILED;
                }
                result = uploadFile(remoteFileName, local, 0);
            }
        } else {
            result = uploadFile(remoteFileName, local, 0);
        }
        return result;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (Exception e) {

            }
        }
    }

    /**
     * 建立远程目录
     * @param remote
     * @return
     * @throws Exception
     */
    private boolean createDirecroty(String remote) throws Exception {
        String directory = remote.substring(0, remote.lastIndexOf("/") + 1);
        if (!directory.equalsIgnoreCase("/") && !ftpClient.changeWorkingDirectory(directory)) {
            int start;
            int end;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            while (true) {
                String subDirectory = remote.substring(start, end);
                if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                    if (ftpClient.makeDirectory(subDirectory)) {
                        ftpClient.changeWorkingDirectory(subDirectory);
                    } else {
                        return false;
                    }
                }

                start = end + 1;
                end = directory.indexOf("/", start);

                if (end <= start) {
                    break;
                }
            }
        }
        return true;
    }

    /**
     * 上传
     * @param remoteFile
     * @param localFile
     * @param remoteSize
     * @return
     * @throws Exception
     */
    private int uploadFile(String remoteFile, File localFile, long remoteSize) throws Exception {
        int status;
        long localReadBytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        OutputStream out = ftpClient.appendFileStream(remoteFile);
        if (remoteSize > 0) {
            ftpClient.setRestartOffset(remoteSize);
            raf.seek(remoteSize);
            localReadBytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        while ((c = raf.read(bytes)) != -1) {
            out.write(bytes, 0, c);
            localReadBytes += c;
            uploadedBytes = localReadBytes;
            if (state == PAUSE) { //暂停挂起
                synchronized (object) {
                    try {
                        object.wait(60 * 1000); //暂停超过1分钟停止上传，避免挂起时间长影响系统
                    } catch (Exception e) {

                    }
                }
            }
            if (state == PAUSE) { //暂停超时
                state = STOP;
            }
            if (state == STOP) break;
        }
        out.flush();
        raf.close();
        out.close();
        boolean result = ftpClient.completePendingCommand();
        if (remoteSize > 0) {
            status = result ? UPLOAD_FROM_BREAK_SUCCESS : UPLOAD_FROM_BREAK_FAILED;
        } else {
            status = result ? UPLOAD_NEW_FILE_SUCCESS : UPLOAD_NEW_FILE_FAILED;
        }
        if (state == STOP) disconnect();
        return status;
    }

    /**
     * 已上传字节数
     * @return
     */
    public long getUploadedBytes() {
        return uploadedBytes;
    }

    /**
     * 本次上传字节数
     * @return
     */
    public long getIncrement() {
        long increment = uploadedBytes - previous_uploadedBytes;
        previous_uploadedBytes = uploadedBytes;
        return increment;
    }

    private void log(String msg) {
        //Log.e("ftpmesg:", msg);
    }

    /**
     * 暂停
     */
    public void pause() {
        state = PAUSE;
    }

    /**
     * 恢复
     */
    public void resume() {
        synchronized (object) {
            state = RUNNING;
            object.notifyAll();
        }
    }

    /**
     * 停止
     */
    public void stop() {
        resume();
        state = STOP;
    }

    /**
     * 获取当前状态
     * @return
     */
    public int getState() {
        return state;
    }
}