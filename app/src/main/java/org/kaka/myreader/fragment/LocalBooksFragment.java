package org.kaka.myreader.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.activity.DownloadStatusActivity;
import org.kaka.myreader.activity.FileSearchActivity;
import org.kaka.myreader.activity.MainActivity;
import org.kaka.myreader.activity.ReaderActivity;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.BookmarkInfoDao;
import org.kaka.myreader.dlayer.dao.CaptureInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LocalBooksFragment extends Fragment {
    private final static String TAG = "LocalBooksFragment";
    private List<Map<String, Object>> listData;
    private PopupWindow addBookWindow;
    private MyBaseAdapter adapter;
    private int currentOrder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookself, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View baseView = getView();
        if (baseView == null) {
            return;
        }
        final SearchView searchView = (SearchView) baseView.findViewById(R.id.search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (TextUtils.isEmpty(s)) {
                    adapter.setList(listData);
                } else {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Map<String, Object> data : listData) {
                        if (((String) data.get("name")).contains(s)) {
                            list.add(data);
                        }
                    }
                    adapter.setList(list);
                }
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        final ImageView imageView = (ImageView) baseView.findViewById(R.id.operation);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(imageView);
            }
        });

        GridView gridView = (GridView) baseView.findViewById(R.id.bookSelf);
        listData = AppUtility.getData(getActivity(), currentOrder);
        adapter = new MyBaseAdapter(listData);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openBook(position);
            }
        });
        gridView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
                if (position == listData.size() - 1) {
                    return;
                }
                MenuInflater inflater = LocalBooksFragment.this.getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_localbooks, menu);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (addBookWindow != null && addBookWindow.isShowing()) {
            addBookWindow.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == FragmentActivity.RESULT_OK) {
            update();
        }
    }

    public void update() {
        listData = AppUtility.getData(getActivity(), currentOrder);
        adapter.setList(listData);
        adapter.notifyDataSetChanged();
    }

    private void init() {
        SharedPreferences preferences = getActivity().getSharedPreferences(AppConstants.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        currentOrder = preferences.getInt(AppConstants.PREF_KEY_READ_ORDER, AppConstants.ORDER_READTIME);
    }

    private void openBook(int position) {
        if (position == listData.size() - 1) {
            showPopupWindow();
            return;
        }
        Map<String, Object> map = listData.get(position);
        String id = (String) map.get("id");
        String path = (String) map.get("path");
        String name = (String) map.get("name");

        Intent intent = new Intent(getActivity(), ReaderActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("path", path);
        intent.putExtra("name", name);
        startActivityForResult(intent, 0);
        getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void showPopupWindow() {
        Point size = new Point();
        Display display = ((WindowManager) getActivity().getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay();
        display.getSize(size);
        int width = size.x;
        addBookWindow = new PopupWindow(getActivity());
        View popupView = getActivity().getLayoutInflater().inflate(R.layout.setting_addbook, null);
        addBookWindow.setContentView(popupView);
        addBookWindow.setOutsideTouchable(true);
        addBookWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addBookWindow.setWindowLayoutMode(width, WindowManager.LayoutParams.WRAP_CONTENT);
        addBookWindow.setWidth(width);
        addBookWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        int height = AppUtility.getVirtualButtonHeight(getActivity(), display, size.y);
        addBookWindow.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.BOTTOM, 0, height);
        Button btnDownload = (Button) popupView.findViewById(R.id.download);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTabHost tabHost = ((MainActivity) getActivity()).getTabHost();
                tabHost.setCurrentTab(1);
                addBookWindow.dismiss();
            }
        });
        Button btnLocal = (Button) popupView.findViewById(R.id.local);
        btnLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBookWindow.dismiss();
                Intent intent = new Intent(getActivity(), FileSearchActivity.class);
                startActivityForResult(intent, 1);
                getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        Button btnWifi = (Button) popupView.findViewById(R.id.wifi);
        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_operation, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.sort_read:
                        currentOrder = AppConstants.ORDER_READTIME;
                        update();
                        return true;
                    case R.id.sort_download:
                        currentOrder = AppConstants.ORDER_DOWNLOAD;
                        update();
                        menuItem.setChecked(true);
                        return true;
                    case R.id.sort_name:
                        currentOrder = AppConstants.ORDER_BOOKNAME;
                        update();
                        menuItem.setChecked(true);
                        return true;
                    case R.id.sort_author:
                        currentOrder = AppConstants.ORDER_AUTHOR;
                        update();
                        menuItem.setChecked(true);
                        return true;
                    case R.id.downloadStatus:
                        Intent intent = new Intent(getActivity(), DownloadStatusActivity.class);
                        startActivity(intent);
                        getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {

            case R.id.read:
                openBook(info.position);
                return true;
            case R.id.delete:
                Map<String, Object> map = listData.get(info.position);
                String id = (String) map.get("id");
                delete(id);

                update();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void delete(String id) {
        DaoFactory factory = new DaoFactory(getActivity());
        try {

            MyBookDao dao = factory.getMyBookDao();
            dao.deleteBook(id);
            BookmarkInfoDao bookmarkInfoDao = factory.getBookmarkInfoDao();
            bookmarkInfoDao.deleteAll(id);
            CaptureInfoDao captureInfoDao = factory.getCaptureInfoDao();
            captureInfoDao.delete(id);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            factory.closeDB();
        }
    }

    class ViewHolder {
        ImageView imageView;
        TextView textView;
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
                view = inflater.inflate(R.layout.item_local, null);
                holder = new ViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                holder.textView = (TextView) view.findViewById(R.id.title);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> map = list.get(position);
            holder.imageView.setImageBitmap((Bitmap) map.get("image"));
            holder.textView.setText((String) map.get("name"));
            return view;
        }
    }
}
