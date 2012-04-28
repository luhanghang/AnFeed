package com.longcent;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.longcent.utils.FtpTask;

/**
 * 站点列表项   <br/>
 * 包括一个进度条<br/>
 * 站点名称<br/>
 * 进度百分比<br/>
 * 删除按钮<br/>
 * 开始暂停按钮<br/>
 */
public class SiteViewHolder {
    public ProgressBar bar;
    public TextView siteName, percent;
    public ImageView playPause;
    public ImageView removeButton;
    public FtpTask ftpTask;
    public View view;

    public static void setProgress(SiteViewHolder viewHolder, int progress) {
        if (progress < 100) {
            viewHolder.bar.setProgress(progress);
            viewHolder.percent.setText(progress + "%");
        }
    }

    public static void initSiteViewHolder(Activity context, SiteViewHolder siteViewHolder) {
        setProgress(siteViewHolder, 0);
        if (siteViewHolder.removeButton != null)
            siteViewHolder.removeButton.setVisibility(View.VISIBLE);
        siteViewHolder.playPause.setVisibility(View.VISIBLE);

        if (siteViewHolder.ftpTask == null) {
            siteViewHolder.playPause.setImageResource(R.drawable.btn_play);
            return;
        }

        switch (siteViewHolder.ftpTask.getState()) {
            case FtpTask.QUEUE: //等待：显示等待，按钮为暂停
                siteViewHolder.percent.setText(context.getString(R.string.waiting));
                siteViewHolder.playPause.setImageResource(R.drawable.btn_pause);
                break;
            case FtpTask.PAUSE://暂停：显示当前进度，百分比位置显示为暂停，按钮为开始
                setProgress(siteViewHolder, siteViewHolder.ftpTask.getProgress());
                siteViewHolder.percent.setText(context.getString(R.string.pause));
                siteViewHolder.playPause.setImageResource(R.drawable.btn_play);
                break;
            case FtpTask.RUNNING://正在上传：按钮为暂停 ，不允许删除
                siteViewHolder.playPause.setImageResource(R.drawable.btn_pause);
                if (siteViewHolder.removeButton != null)
                    siteViewHolder.removeButton.setVisibility(View.INVISIBLE);
                break;
            case FtpTask.FINISHED: //完成：显示完成，不允许删除，按钮显示为开始可以重新上传
                siteViewHolder.bar.setProgress(100);
                siteViewHolder.percent.setText(context.getString(R.string.done));
                if (siteViewHolder.removeButton != null)
                    siteViewHolder.removeButton.setVisibility(View.INVISIBLE);
                siteViewHolder.playPause.setImageResource(R.drawable.btn_play);
                //siteViewHolder.playPause.setVisibility(View.INVISIBLE);
                break;
            default:
                siteViewHolder.playPause.setImageResource(R.drawable.btn_play);
        }
    }
}
