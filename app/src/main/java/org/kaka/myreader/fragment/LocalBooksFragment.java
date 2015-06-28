package org.kaka.myreader.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;

import org.kaka.myreader.R;
import org.kaka.myreader.activity.FileSearchActivity;
import org.kaka.myreader.activity.MainActivity;
import org.kaka.myreader.activity.ReaderActivity;
import org.kaka.myreader.common.AppUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LocalBooksFragment extends Fragment {
    private List<Map<String, Object>> listData;
    private GridView gridView;
    private PopupWindow addBookWindow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listData = AppUtility.getData(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bookself, container, false);
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
                    setAdapter(listData);
                } else {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Map<String, Object> data : listData) {
                        if (((String) data.get("name")).contains(s)) {
                            list.add(data);
                        }
                    }
                    setAdapter(list);
                }
                return true;
            }
        });

        gridView = (GridView) baseView.findViewById(R.id.bookSelf);
        setAdapter(listData);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openBook(position);
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

    private void setAdapter(List<Map<String, Object>> list) {
        SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), list, R.layout.bookitem_local,
                new String[]{"image", "name"}, new int[]{R.id.image, R.id.title});

        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if ((view instanceof ImageView) && (data instanceof Bitmap)) {
                    ImageView imageView = (ImageView) view;
                    Bitmap bmp = (Bitmap) data;
                    imageView.setImageBitmap(bmp);
                    return true;
                }
                return false;
            }
        });

        gridView.setAdapter(adapter);
    }

    public void update(List<Map<String, Object>> listData) {
        this.listData = listData;
        setAdapter(listData);
    }

    private void openBook(int position) {
        if (position == listData.size() - 1) {
            showPopupWindow();
            return;
        }
        Map<String, Object> map = listData.get(position);
        int id = (Integer) map.get("id");
        String path = (String) map.get("path");
        String name = (String) map.get("name");

        Intent intent = new Intent(getActivity(), ReaderActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("path", path);
        intent.putExtra("name", name);
        startActivity(intent);
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
                startActivity(intent);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_localbooks, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {

            case R.id.read:
                openBook(info.position);
                return true;
            case R.id.delete:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
