package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.fragment.CloudFragment;
import org.kaka.myreader.task.FileDownloadTask;
import org.kaka.myreader.task.LoadCommitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudDetailActivity extends FragmentActivity {

    List<Map<String, Object>> listData = new ArrayList<>();
    private ProgressDialog dialog;
    private Bundle bundle;
    public final static String KEY_IMAGE = "image";
    public final static String KEY_NAME = "name";
    public final static String KEY_STAR = "star";
    public final static String KEY_TIME = "time";
    public final static String KEY_COMMIT = "commit";
    public final static String KEY_BOOK_ID = "id";
    private String id;
    private Map<String, Object> currentMap = new HashMap<>();
    private BroadcastReceiver receiver;
    private Button downloadBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_detail);

        bundle = getIntent().getBundleExtra("bundle");
        id = bundle.getString(CloudFragment.KEY_ID);

        dialog = ProgressDialog.show(this, "请稍等", "正在加载详细信息");
        new LoadCommitTask(this).execute(id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    public void finishConnection(List<Map<String, Object>> listData) {
        this.listData = listData;
        if (dialog != null) {
            dialog.dismiss();
        }
        init();
    }

    public void finishDownload() {
        init();
    }

    private void init() {
        ImageView image = (ImageView) findViewById(R.id.image);
        String imageStr = bundle.getString(CloudFragment.KEY_IMAGE);
        byte[] data = Base64.decode(imageStr, Base64.DEFAULT);
        Bitmap bitmap = null;
        if (data.length != 0) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
//        Bitmap bitmap = bundle.getParcelable(CloudFragment.KEY_IMAGE);
        image.setImageBitmap(bitmap);

        TextView bookNameView = (TextView) findViewById(R.id.name);
        final String bookName = bundle.getString(CloudFragment.KEY_NAME);
        bookNameView.setText(bookName);

        TextView authorView = (TextView) findViewById(R.id.author);
        String author = bundle.getString(CloudFragment.KEY_AUTHOR);
        authorView.setText("作者 : " + author);

        TextView sizeView = (TextView) findViewById(R.id.size);
        final String path = bundle.getString(CloudFragment.KEY_PATH);
        String type;
        if (path == null || !path.contains(".")) {
            type = "未知";
        } else {
            type = path.substring(path.lastIndexOf(".") + 1).toUpperCase();
        }
        sizeView.setText("大小 : " + bundle.getString(CloudFragment.KEY_SIZE) + " (" + type + ")");

        RatingBar score = (RatingBar) findViewById(R.id.score);
        score.setRating(bundle.getFloat(CloudFragment.KEY_SCORE));

        TextView detailView = (TextView) findViewById(R.id.detail);
        detailView.setText("内容简介:\n");
        String detail = bundle.getString(CloudFragment.KEY_DETAIL, "");
        detailView.append(detail);

        ImageButton backBtn = (ImageButton) findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudDetailActivity.this.finish();
            }
        });

        downloadBtn = (Button) findViewById(R.id.download);
        downloadBtn.setClickable(true);
        DaoFactory factory = new DaoFactory(this);
        MyBookDao bookDao = factory.getMyBookDao();
        boolean exist = bookDao.exist(bookName);
        if (exist) {
            downloadBtn.setText("进入阅读");
            downloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (path == null) {
                        return;
                    }
                    String fileName = path;
                    int begin = path.lastIndexOf("/");
                    if (begin >= 0) {
                        fileName = path.substring(begin);
                    }

                    String filePath = AppConstants.APP_DOWNLOAD_DIR + fileName;

                    Intent intent;
                    if (path.toLowerCase().endsWith(".txt")) {
                        intent = new Intent(CloudDetailActivity.this, ReaderTxtActivity.class);
                    } else {
                        intent = new Intent(CloudDetailActivity.this, ReaderEpubActivity.class);
                    }
                    intent.putExtra("id", id);
                    intent.putExtra("path", filePath);
                    intent.putExtra("name", bookName);
                    startActivityForResult(intent, 0);

                    CloudDetailActivity.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            });
        } else {
            downloadBtn.setText("点击下载");
            downloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(CloudDetailActivity.this, "开始下载..", Toast.LENGTH_SHORT).show();
                    FileDownloadTask task = new FileDownloadTask(CloudDetailActivity.this);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentMap);
                    downloadBtn.setText("开始下载");
                    downloadBtn.setClickable(false);
                }
            });
        }

        TextView addCommit = (TextView) findViewById(R.id.addCommit);
        addCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
                boolean isLogin = preferences.getBoolean(AppConstants.PREF_KEY_LOGIN, false);
                if (!isLogin) {
                    Intent intent = new Intent(CloudDetailActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                } else {
                    Intent intent = new Intent(CloudDetailActivity.this, AddCommitActivity.class);
                    intent.putExtra(KEY_BOOK_ID, id);
                    startActivityForResult(intent, 0);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            }
        });

        currentMap.put(CloudFragment.KEY_ID, id);
        currentMap.put(CloudFragment.KEY_PATH, path);
        currentMap.put(CloudFragment.KEY_AUTHOR, author);
        currentMap.put(CloudFragment.KEY_DETAIL, detail);
        currentMap.put(CloudFragment.KEY_NAME, bookName);
        currentMap.put(CloudFragment.KEY_IMAGE, bitmap);

        ListView commitList = (ListView) findViewById(R.id.commit);
        MyAdapter adapter = new MyAdapter(this);
        commitList.setAdapter(adapter);

        addBroadcast();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            dialog = ProgressDialog.show(this, "请稍等", "正在加载详细信息");
            new LoadCommitTask(this).execute(id);
        }
    }

    private void addBroadcast() {
        if (receiver != null) {
            return;
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String myId = intent.getStringExtra("id");
                if (id.equals(myId)) {
                    String downloadSize = intent.getStringExtra("downloadSize");
                    String totalSize = intent.getStringExtra("totalSize");
                    if (downloadSize == null || totalSize == null) {
                        return;
                    }
                    int status = (int) (Double.parseDouble(downloadSize) * 100 / Double.parseDouble(totalSize));
                    downloadBtn.setText("下载中 (" + status + "%)");
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(AppConstants.BROADCAST_DOWNLOAD));
    }

    class MyAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        public MyAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
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
                convertView = inflater.inflate(R.layout.item_commit, null);
                holder.userImage = (ImageView) convertView.findViewById(R.id.userImage);
                holder.userName = (TextView) convertView.findViewById(R.id.userName);
                holder.userStar = (RatingBar) convertView.findViewById(R.id.userStar);
                holder.userCommit = (TextView) convertView.findViewById(R.id.userCommit);
                holder.userDate = (TextView) convertView.findViewById(R.id.userDate);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Map<String, Object> map = listData.get(position);
            holder.userImage.setImageBitmap((Bitmap) map.get(KEY_IMAGE));

            holder.userName.setText((String) map.get(KEY_NAME));
            double star = (Double) map.get(KEY_STAR);
            holder.userStar.setRating((float) star);
            holder.userCommit.setText((String) map.get(KEY_COMMIT));
            holder.userDate.setText((String) map.get(KEY_TIME));

            return convertView;
        }

        class ViewHolder {
            ImageView userImage;
            TextView userName;
            RatingBar userStar;
            TextView userCommit;
            TextView userDate;
        }
    }
}
