package org.kaka.myreader.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;

public class AccoutActivity extends Activity {
    private String[] infoNames = new String[]{"头像", "昵称", "性别", "金币"};
    private String[] settings = new String[]{"退出登录"};
    private String[] infoContents = new String[3];
    private Bitmap bitmap;
    private SharedPreferences preferences;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        ImageButton backBtn = (ImageButton) findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(0);
                finish();
            }
        });

        preferences = getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        initData();

        ListView list1 = (ListView) findViewById(R.id.infos);
        adapter = new MyAdapter(this);
        list1.setAdapter(adapter);
        list1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    Intent intent = new Intent(AccoutActivity.this, PhotoPickerActivity.class);
                    startActivityForResult(intent, 0);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            }
        });

        ListView list2 = (ListView) findViewById(R.id.settings);
        MySettingAdapter adapter2 = new MySettingAdapter(this);
        list2.setAdapter(adapter2);
        list2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(AccoutActivity.this).setTitle("退出登录").setMessage("退出登录").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(AppConstants.PREF_KEY_LOGIN, false);
                        editor.apply();
                        AccoutActivity.this.finish();
                    }
                }).setNegativeButton("No", null).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == 0) {
            String imgStr = preferences.getString(AppConstants.PREF_KEY_USER_IMAGE, "");
            byte[] imageData = Base64.decode(imgStr, Base64.DEFAULT);
            if (imageData.length != 0) {
                bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void initData() {
        infoContents[0] = preferences.getString(AppConstants.PREF_KEY_USERNAME, "");
        String sex = preferences.getString(AppConstants.PREF_KEY_SEX, "");
        if ("male".equals(sex)) {
            infoContents[1] = "男";
        } else {
            infoContents[1] = "女";
        }
        infoContents[2] = preferences.getString(AppConstants.PREF_KEY_POINT, "");

        String imgStr = preferences.getString(AppConstants.PREF_KEY_USER_IMAGE, "");
        byte[] data = Base64.decode(imgStr, Base64.DEFAULT);
        if (data.length != 0) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user);
        }
    }

    class MySettingAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public MySettingAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int position) {
            return settings[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_setting, null);
                holder = new ViewHolder();
                holder.textView = (TextView) convertView.findViewById(R.id.itemName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.textView.setText(settings[0]);

            return convertView;
        }

        class ViewHolder {
            TextView textView;
        }
    }

    class MyAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private final int TYPE_ONE = 0, TYPE_TWO = 1, TYPE_COUNT = 2;

        public MyAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_ONE;
            } else {
                return TYPE_TWO;
            }
        }

        @Override
        public Object getItem(int position) {
            return infoNames[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder1 holder1 = null;
            ViewHolder2 holder2 = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case TYPE_ONE:
                        holder1 = new ViewHolder1();
                        convertView = inflater.inflate(R.layout.item_account_one, null);
                        holder1.titles = (TextView) convertView.findViewById(R.id.infoName);
                        holder1.contents = (ImageView) convertView.findViewById(R.id.infoContent);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(300, 300);
                        params.addRule(RelativeLayout.ALIGN_PARENT_END);
                        params.topMargin = 30;
                        params.bottomMargin = 30;
                        params.rightMargin = 30;
                        holder1.contents.setLayoutParams(params);
                        convertView.setTag(holder1);
                        break;
                    case TYPE_TWO:
                        holder2 = new ViewHolder2();
                        convertView = inflater.inflate(R.layout.item_account_two, null);
                        holder2.titles = (TextView) convertView.findViewById(R.id.infoName);
                        holder2.contents = (TextView) convertView.findViewById(R.id.infoContent);
                        convertView.setTag(holder2);
                        break;
                }
            } else {
                switch (type) {
                    case TYPE_ONE:
                        holder1 = (ViewHolder1) convertView.getTag();
                        break;
                    case TYPE_TWO:
                        holder2 = (ViewHolder2) convertView.getTag();
                        break;
                }
            }

            switch (type) {
                case TYPE_ONE:
                    holder1.titles.setText(infoNames[0]);
                    holder1.contents.setImageBitmap(bitmap);
                    break;
                case TYPE_TWO:
                    holder2.titles.setText(infoNames[position]);
                    holder2.contents.setText(infoContents[position - 1]);
                    break;
            }
            return convertView;
        }

        class ViewHolder1 {
            TextView titles;
            ImageView contents;
        }

        class ViewHolder2 {
            TextView titles;
            TextView contents;
        }
    }
}
