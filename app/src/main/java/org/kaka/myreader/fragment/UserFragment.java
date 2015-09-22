package org.kaka.myreader.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.activity.AccoutActivity;
import org.kaka.myreader.activity.LoginActivity;
import org.kaka.myreader.common.AppConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserFragment extends Fragment {
    private List<Map<String, Object>> coinlistData = new ArrayList<>();
    private List<Map<String, Object>> synListData = new ArrayList<>();
    private String[] coins = new String[]{"金币充值", "充值记录", "消费记录"};
    private String[] syns = new String[]{"我的上传", "我的收藏", "我的同步"};
    private ImageView imageView;
    private TextView userNameView;
    private ImageView sexImageView;
    private TextView coinView;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        initView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        imageView = (ImageView) view.findViewById(R.id.userImage);
        userNameView = (TextView) view.findViewById(R.id.userName);
        sexImageView = (ImageView) view.findViewById(R.id.sexImage);
        coinView = (TextView) view.findViewById(R.id.coin);

        ListView coinListView = (ListView) view.findViewById(R.id.coinList);
        BaseAdapter adapter1 = new MyBaseAdapter(coinlistData);
        coinListView.setAdapter(adapter1);

        ListView synListView = (ListView) view.findViewById(R.id.synList);
        BaseAdapter adapter2 = new MyBaseAdapter(synListData);
        synListView.setAdapter(adapter2);
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            initView();
        }
    }

    private void initView() {
        preferences = getActivity().getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        boolean hasLogin = preferences.getBoolean(AppConstants.PREF_KEY_LOGIN, false);

        if (!hasLogin) {
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    login();
                }
            });

            userNameView.setText("未登录, 点击登录");
            userNameView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            userNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    login();
                }
            });
            sexImageView.setImageDrawable(null);
            coinView.setText("");

        } else {
            String userName = preferences.getString(AppConstants.PREF_KEY_USERNAME, "");
            String point = preferences.getString(AppConstants.PREF_KEY_POINT, "0");
            String sex = preferences.getString(AppConstants.PREF_KEY_SEX, "");
            String imgStr = preferences.getString(AppConstants.PREF_KEY_USER_IMAGE, "");
            byte[] imageData = Base64.decode(imgStr, Base64.DEFAULT);
            if (imageData.length != 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                imageView.setImageBitmap(bitmap);
            }

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), AccoutActivity.class);
                    getActivity().startActivityForResult(intent, 1);
                    getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            });

            userNameView.setText(userName);
            userNameView.getPaint().setFlags(Paint.LINEAR_TEXT_FLAG);
            if ("female".equals(sex)) {
                sexImageView.setImageResource(R.drawable.female);
            } else {
                sexImageView.setImageResource(R.drawable.male);
            }
            coinView.setText(point + "金币");
        }
    }

    private void login() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, 0);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void initData() {
        for (String coin : coins) {
            Map<String, Object> map = new HashMap<>();
            map.put("itemName", coin);
            map.put("itemBtn", BitmapFactory.decodeResource(getResources(), R.drawable.ic_play));
            coinlistData.add(map);
        }

        for (String syn : syns) {
            Map<String, Object> map = new HashMap<>();
            map.put("itemName", syn);
            map.put("itemBtn", BitmapFactory.decodeResource(getResources(), R.drawable.ic_play));
            synListData.add(map);
        }
    }

    class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

    class MyBaseAdapter extends BaseAdapter {
        private List<Map<String, Object>> list;

        public MyBaseAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setList(List<Map<String, Object>> list) {
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_user, null);
                holder = new ViewHolder();
                holder.textView = (TextView) view.findViewById(R.id.itemName);
                holder.imageView = (ImageView) view.findViewById(R.id.itemBtn);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> map = list.get(position);
            holder.textView.setText((String) map.get("itemName"));
            if (holder.imageView != null) {
                holder.imageView.setImageBitmap((Bitmap) map.get("itemBtn"));
            }
            return view;
        }
    }
}
