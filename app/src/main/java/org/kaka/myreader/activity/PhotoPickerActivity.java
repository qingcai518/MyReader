package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.task.PhotoPickTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoPickerActivity extends Activity {
    private List<String> pathList = new ArrayList<>();
    private ProgressDialog dialog;
    public static final String IMAGEPATH = "ImagePath";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photopicker);

        initData();
    }

    private void initData() {
        dialog = ProgressDialog.show(this, "请稍等", "正在取得图片");
        ImageButton btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ImageButton camera = (ImageButton) findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                Uri uri = Uri.fromFile(new File(AppConstants.APP_PIC_DIR));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, 0);
            }
        });
        new PhotoPickTask(this).execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == 0) {
            setResult(0, data);
            finish();
        }
    }

    public void finishTask(final List<String> pathList) {
        this.pathList = pathList;
        if (dialog != null) {
            dialog.dismiss();
        }
        Fresco.initialize(this);
        GridView gridView = (GridView) findViewById(R.id.photos);
        gridView.setAdapter(new MyAdapter(this));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(PhotoPickerActivity.this, PhotoClipActivity.class);
                intent.putExtra(IMAGEPATH, pathList.get(position));
                startActivityForResult(intent, 0);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }

    class MyAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public MyAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return pathList.size();
        }

        @Override
        public Object getItem(int position) {
            return pathList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_photo, null);
                holder.imageView = (SimpleDraweeView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String path = pathList.get(position);

            Uri uri = Uri.parse("file://" + path);
            holder.imageView.setImageURI(uri);

            return convertView;
        }

        class ViewHolder {
            SimpleDraweeView imageView;
        }
    }
}
