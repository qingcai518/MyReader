package org.kaka.myreader.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.dlayer.dao.BookmarkInfoDao;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.entities.BookmarkInfoEntity;

import java.util.List;

public class BookmarkFragment extends ListFragment {
    private List<BookmarkInfoEntity> list;
    private BaseAdapter adapter;
    DaoFactory factory;
    BookmarkInfoDao dao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getData();

        adapter = new BaseAdapter() {
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

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) LayoutInflater.from(BookmarkFragment.this.getActivity()).inflate(
                        R.layout.bookmarkitem, null);
                BookmarkInfoEntity entity = list.get(position);
                textView.setText(entity.getCaptureName() + "  " + entity.getProgress());

                return textView;
            }
        };
        setListAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        registerForContextMenu(getListView());
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_bookmark, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {

            case R.id.open:
                backToReader(info.position);
                return true;
            case R.id.delete:
                BookmarkInfoEntity entity = list.get(info.position);
                dao.delete(entity.getId(), entity.getOffset());
                list.remove(info.position);
                adapter.notifyDataSetChanged();
                return true;
            case R.id.deleteAll:
                dao.deleteAll(list.get(info.position).getId());
                list.clear();
                adapter.notifyDataSetChanged();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        backToReader(position);
    }

    @Override
    public void onDestroy() {
        if (factory != null) {
            factory.closeDB();
        }

        super.onDestroy();
    }

    private void backToReader(int position) {
        Intent intent = new Intent();
        int currentBookmark = list.get(position).getOffset();
        intent.putExtra("currentOffset", currentBookmark);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void getData() {
        int id = getActivity().getIntent().getIntExtra("id", 0);
        factory = new DaoFactory(getActivity());
        dao = factory.getBookmarkInfoDao();
        list = dao.selectById(id);
        Log.i("here", list.size() + "");
    }
}