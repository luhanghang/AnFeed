package com.longcent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.longcent.db.Media;
import com.longcent.db.Site;
import com.longcent.db.Task;
import com.longcent.db.TaskSite;
import com.longcent.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务信息界面
 */
public class UploadActivity extends Activity {
    public static final int CATE = 0;
    public static final int GET_MEDIA = 1;
    public static final int TAKE_MEDIA = 2;
    public static final int TAKE_PHOTO = 0;
    public static final int TAKE_VIDEO = 1;
    public static final int CHOOSE_PHOTO = 0;
    public static final int CHOOSE_VIDEO = 1;

    EditText cate, title, keywords, location, createTime;
    Gallery gallery;
    List<Uri> medias;
    ImageAdapter imageAdapter;

    TextView mediaCounter;

    boolean isNew;
    Task task;
    TaskSite taskSite;
    Media media;
    String taskId;
    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_main);

        task = new Task(this);
        taskSite = new TaskSite(this);
        media = new Media(this);

        medias = new ArrayList<Uri>();
        mediaCounter = (TextView) findViewById(R.id.uploadPreviewCount);

        imageAdapter = new ImageAdapter(this);
        gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setAdapter(imageAdapter);
        updateGalleryIndex();
        //点击附件列表显示照片或视频
        gallery.setOnItemClickListener(new Gallery.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Uri uri = medias.get(i);
                String intentString = "ImageView";
                if (uri.toString().indexOf("image") < 0) {
                    intentString = "VideoView";
                }
                Intent intent = new Intent(intentString);
                intent.putExtra("uri", medias.get(i));
                startActivity(intent);
            }
        });

        title = (EditText) findViewById(R.id.title);
        title.setHint(R.string.required);

        keywords = (EditText) findViewById(R.id.keywords);
        location = (EditText) findViewById(R.id.location);

        createTime = (EditText) findViewById(R.id.time);
        createTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        cate = (EditText) findViewById(R.id.category);
        cate.setHint(R.string.clickToSelect);
        cate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent("Category");
                startActivityForResult(intent, CATE);
            }
        });

        setData();

        ImageView upload = Utils.setSwitchButton((ImageView) findViewById(R.id.uploadButton), R.drawable.upload_u, R.drawable.upload_d);
        upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                saveUploadInf();
            }

            /**
             * 保存任务信息
             */
            private void saveUploadInf() {
                if (checkValidation()) {
                    Site site = new Site(getApplicationContext());
                    List<String> selectedSiteIds = site.getSelectedIds();
                    if (selectedSiteIds.size() == 0) {
                        startActivity(new Intent("Signin"));
                    } else {
                        Bundle record = new Bundle();
                        record.putString(Task.TITLE, Utils.getEditTextStringValue(title));
                        record.putString(Task.KEYWORDS, Utils.getEditTextStringValue(keywords));
                        record.putString(Task.LOCATION, Utils.getEditTextStringValue(location));
                        record.putString(Task.CATEGORY, Utils.getEditTextStringValue(cate));
                        record.putString(Task.CREATED_TIME, Utils.getEditTextStringValue(createTime));

                        if (isNew) { //新建任务
                            long newTaskId = task.create(record);
                            if (newTaskId > 0) {
                                taskId = newTaskId + "";
                                linkSites(selectedSiteIds);
                                linkMedias();
                            }
                            startActivity(new Intent("Task"));
                        } else {  //修改任务
                            task.update(taskId, record);
                            linkSites(selectedSiteIds);
                            linkMedias();
                            setResult(RESULT_OK);
                        }
                        finish();
                    }
                }
            }

            /**
             * 关联站点
             */
            private void linkSites(List<String> selectedSiteIds) {
                taskSite.removeByTaskId(taskId);
                Bundle record = new Bundle();
                for (String siteId : selectedSiteIds) {
                    record.putString(TaskSite.TASK_ID, taskId + "");
                    record.putString(TaskSite.SITE_ID, siteId);
                    taskSite.create(record);
                }
            }

            /**
             * 关联附件
             */
            private void linkMedias() {
                media.removeByTaskId(taskId);
                Bundle record = new Bundle();
                for (Uri uri : medias) {
                    record.putString(Media.TASK_ID, taskId + "");
                    record.putString(Media.URI, uri.toString());
                    record.putString(Media.REMOTE_NAME, Media.genRemoteName(UploadActivity.this, taskId, uri));
                    media.create(record);
                }
            }
        });
        //从媒体库选择附件
        ImageView selectMedia = Utils.setSwitchButton((ImageView) findViewById(R.id.getPhotos), R.drawable.medias_u, R.drawable.medias_d);
        selectMedia.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //startActivityForResult(Intent.createChooser(intent, getString(R.string.selectPicture)), GET_MEDIA);
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(UploadActivity.this, R.string.sdInvalid, Toast.LENGTH_LONG).show();
                    return;
                }
                final CharSequence[] items = {getString(R.string.choosePhoto), getString(R.string.chooseVideo)};
                new AlertDialog.Builder(UploadActivity.this).setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        Intent intent;
                        switch (item) {
                            case CHOOSE_PHOTO: //相片
                                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(Intent.createChooser(intent, getString(R.string.selectPicture)), GET_MEDIA);
                                break;
                            case CHOOSE_VIDEO://视频
                                intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(Intent.createChooser(intent, getString(R.string.selectVideo)), GET_MEDIA);
                                break;
                        }
                    }
                }).show();
            }
        });
        //拍摄
        ImageView capture = Utils.setSwitchButton((ImageView) findViewById(R.id.capture), R.drawable.camera_u, R.drawable.camera_d);
        capture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(UploadActivity.this, R.string.sdInvalid, Toast.LENGTH_LONG).show();
                    return;
                }
                final CharSequence[] items = {getString(R.string.takePhoto), getString(R.string.takeVideo)};
                new AlertDialog.Builder(UploadActivity.this).setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        Intent intent;
                        switch (item) {
                            case TAKE_PHOTO: //照相
                                intent = new Intent("android.media.action.IMAGE_CAPTURE");
                                File tempImageFile = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempImageFile));
                                startActivityForResult(intent, TAKE_MEDIA);
                                break;
                            case TAKE_VIDEO://拍摄
                                intent = new Intent("android.media.action.VIDEO_CAPTURE");
                                startActivityForResult(intent, GET_MEDIA);
                                break;
                        }
                    }
                }).show();
            }
        });

        gallery.setOnItemSelectedListener(new Gallery.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateGalleryIndex();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                updateGalleryIndex();
            }
        });

        //删除当前附件
        ImageView removeFromGallery = Utils.setSwitchButton((ImageView) findViewById(R.id.removeFromGallery), R.drawable.trash_u, R.drawable.trash_d);
        removeFromGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int current = gallery.getSelectedItemPosition();
                if (current < 0) return;
                medias.remove(gallery.getSelectedItemPosition());
                imageAdapter.notifyDataSetChanged();
                updateGalleryIndex();
            }
        });
    }

    private void setData() {
        Bundle bundle = this.getIntent().getExtras();
        isNew = bundle == null;
        if (!isNew) {
            if (bundle.getBoolean("readOnly")) {
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.upload_tool_bar);
                linearLayout.setVisibility(View.INVISIBLE);
            }
            taskId = bundle.getString(Task.ID);
            bundle = task.fetch(taskId);
            title.setText(bundle.getString(Task.TITLE));
            keywords.setText(bundle.getString(Task.KEYWORDS));
            location.setText(bundle.getString(Task.LOCATION));
            createTime.setText(bundle.getString(Task.CREATED_TIME));
            cate.setText(bundle.getString(Task.CATEGORY));
            List<Map<String, String>> medisList = media.getByTaskId(taskId);
            for (Map<String, String> _media : medisList) {
                medias.add(Uri.parse(_media.get(Media.URI)));
            }
            imageAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 校验文本框
     *
     * @return
     */
    private boolean checkValidation() {
        if (Utils.isEditTextValueEmpty(title) || Utils.isEditTextValueEmpty(cate)) {
            Toast.makeText(this, R.string.needRequired, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateGalleryIndex() {
        int current = gallery.getSelectedItemPosition();
        mediaCounter.setText((current + 1) + "/" + medias.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();    //To change body of overridden methods use File | Settings | File Templates.
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CATE:
                    cate.setText(data.getExtras().getString("cate"));
                    break;
                case GET_MEDIA:
                    Uri uri = data.getData();
                    if (uri == null) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    }
                    addMedia(uri);
                    break;
                case TAKE_MEDIA:
                    SaveImageTask saveImageTask = new SaveImageTask();
                    saveImageTask.execute();
                    break;
            }
        }
    }

    /**
     * 添加附件
     *
     * @param uri
     */
    private void addMedia(Uri uri) {
        if (medias.contains(uri)) return;
        medias.add(uri);
        gallery.setSelection(medias.size());
        imageAdapter.notifyDataSetChanged();

    }

    /**
     * 照片适配器
     */
    private class ImageAdapter extends BaseAdapter {
        Context context;
        LayoutInflater inflater;

        public ImageAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return medias.size();
        }

        public Object getItem(int i) {
            return medias.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = inflater.inflate(R.layout.gallery_image_view, null);
            }
            ImageView play = (ImageView) view.findViewById(R.id.gallery_image_view_play);
            ImageView thumbView = (ImageView) view.findViewById(R.id.gallery_image_view_thumb);
            ContentResolver contentResolver = getContentResolver();
            Uri uri = (Uri) getItem(i);
            Bitmap bitmap;
            try {
                if (uri.toString().indexOf("/images/") > 0) {
                    play.setAlpha(0);
                    bitmap = Utils.getThumbnail(contentResolver, uri, gallery.getMeasuredHeight());
                } else {
                    play.setAlpha(90);
                    Uri thumb = Uri.withAppendedPath(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, uri.getLastPathSegment());
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, thumb);
                }
                thumbView.setImageBitmap(bitmap);
            } catch (Exception e) {
                thumbView.setImageResource(R.drawable.invalid_photo);
            }
            return view;
        }
    }

    /**
     * 拍照后保存
     */
    private class SaveImageTask extends android.os.AsyncTask<Void, Void, Void> {
        Uri mediaUri;
        Handler handler;

        @Override
        protected Void doInBackground(Void... voids) {
            File file = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
            try {
                String path = file.getAbsolutePath();
                String uri = MediaStore.Images.Media.insertImage(getContentResolver(), path, null, null);
                mediaUri = Uri.parse(uri);
                file.delete();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            if (progressDialog == null)
                progressDialog = ProgressDialog.show(UploadActivity.this, getString(R.string.plswait), getString(R.string.savingPhoto));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //handler.handleMessage(null);
            addMedia(mediaUri);
            progressDialog.hide();
        }
    }
}
