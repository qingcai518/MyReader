package org.kaka.myreader.fragment;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.common.AppUtility;
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FileAutoSearchFragment extends ListFragment {
    private List<ItemHolder> listData = new ArrayList<>();
    private Set<File> files;
    private int count = 0;
    private MyBookDao myBookDao;
    private Button btnImport;
    private ProgressDialog dialog;
    private MyHandler handler = new MyHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ItemHolder itemHolder = listData.get(position);
        boolean imported = itemHolder.isImported();

        if (!imported) {
            View subView = view.findViewById(R.id.fileSelection);
            if (!(subView instanceof CheckBox)) {
                return;
            }
            CheckBox checkBox = (CheckBox) subView;
            checkBox.setChecked(!checkBox.isChecked());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auto, container, false);

        files = new HashSet<>();

        DaoFactory factory = new DaoFactory(getActivity());
        myBookDao = factory.getMyBookDao();
        btnImport = (Button) getActivity().findViewById(R.id.importbook);
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (files == null || files.size() == 0) {
                    return;
                }

                Iterator<File> iterator = files.iterator();
                while (iterator.hasNext()) {
                    File file = iterator.next();
                    try {
                        MyBookEntity entity = new MyBookEntity();
                        entity.setAuthor("匿名");
                        entity.setCurrentOffset(0);
                        entity.setDetail("");
                        entity.setId(AppConstants.ID_PREFIX_IMPORT + UUID.randomUUID().toString());

                        entity.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.default_txt));
                        entity.setName(file.getName());
                        entity.setPath(file.getCanonicalPath());
                        myBookDao.addBook(entity);
                        iterator.remove();
                        for (ItemHolder holder : listData) {
                            if (holder.getFile().equals(file)) {
                                holder.setImported(true);
                                break;
                            }
                        }
                        count--;
                    } catch (Exception e) {
                        Log.e("FileSearchActivity", e.getMessage());
                    }
                }
                btnImport.setText("导入(" + count + ")");
                getData();
            }
        });
        getData();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void getData() {
        if (listData.size() > 0) {
            setAdapter();
            return;
        }
        dialog = ProgressDialog.show(getActivity(), "请稍后", "智能查找中...", true);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                List<String> importedFiles = new ArrayList<>();
                if (myBookDao != null) {
                    importedFiles = myBookDao.getFilePaths(AppConstants.ID_PREFIX_IMPORT);
                }
                long startTime = System.currentTimeMillis();
                searchFile(Environment.getExternalStorageDirectory(), importedFiles);
                long endTime = System.currentTimeMillis();
                Log.i("time = ", (endTime - startTime) + "ms");
                handler.sendEmptyMessage(0);
            }
        });
        thread.start();
    }

    private void searchFile(File dir, List<String> importedFiles) {
        for (File file : dir.listFiles()) {
            if (file.getAbsolutePath().equals(AppConstants.APP_DIR)) {
                continue;
            }
            if (file.isFile()
                    && (file.getName().endsWith(".txt")
                    || file.getName().endsWith(".pdf")
                    || file.getName().endsWith(".epub"))) {
                try {
                    ItemHolder holder = new ItemHolder();
                    String path = file.getCanonicalPath();
                    String fileName = file.getName();
                    String ext = fileName.substring(file.getName().lastIndexOf(".") + 1);
                    holder.setFileName(fileName);
                    double size = AppUtility.getFileSize(file);
                    holder.setFileDetail("类型:" + ext + "  大小:" + AppConstants.DECIMAL_FORMAT.format(size) + "KB");
                    holder.setFileLogo(getResources().getDrawable(R.drawable.ic_action_document));
                    if (importedFiles.contains(path)) {
                        holder.setImported(true);
                    } else {
                        holder.setImported(false);
                    }

                    holder.setFile(file);
                    listData.add(holder);
                } catch (Exception e) {
                    Log.e("", e.getMessage());
                }
            } else if (file.isDirectory()) {
                searchFile(file, importedFiles);
            }
        }
    }

    private void setAdapter() {
        BaseAdapter adapter = new BaseAdapter() {
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
                RelativeLayout layout = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.item_import, null);
                ItemHolder holder = listData.get(position);
                if (holder == null) {
                    return null;
                }

                final File file = holder.getFile();
                boolean imported = holder.isImported();
                Drawable fileLogo = holder.getFileLogo();
                String fileName = holder.getFileName();
                String fileDetail = holder.getFileDetail();

                ImageView fileLogoView = (ImageView) layout.findViewById(R.id.fileLogo);
                TextView fileNameView = (TextView) layout.findViewById(R.id.fileName);
                TextView fileDetailView = (TextView) layout.findViewById(R.id.fileDetail);

                fileLogoView.setImageDrawable(fileLogo);
                fileNameView.setText(fileName);
                fileDetailView.setText(fileDetail);

                if (imported) {
                    TextView textView = new TextView(getActivity());
                    textView.setText("已导入");
                    textView.setId(R.id.fileSelection);
                    textView.setLayoutParams(getParams());
                    layout.addView(textView);
                } else {
                    CheckBox checkBox = new CheckBox(getActivity());
                    checkBox.setId(R.id.fileSelection);
                    checkBox.setFocusable(false);
                    checkBox.setLayoutParams(getParams());
                    if (files.contains(file)) {
                        checkBox.setChecked(true);
                    } else {
                        checkBox.setChecked(false);
                    }
                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                files.add(file);
                                count++;
                            } else {
                                files.remove(file);
                                count--;
                            }
                            btnImport.setText("导入(" + count + ")");
                        }
                    });
                    layout.addView(checkBox);
                }

                return layout;
            }

            private RelativeLayout.LayoutParams getParams() {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                return params;
            }
        };
        setListAdapter(adapter);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<FileAutoSearchFragment> myWeakReference;

        public MyHandler(FileAutoSearchFragment fragment) {
            myWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            FileAutoSearchFragment fragment = myWeakReference.get();
            if (fragment == null) {
                return;
            }
            int code = msg.what;
            switch (code) {
                case 0:
                    fragment.setAdapter();
                    fragment.dialog.dismiss();
                    break;
            }
        }
    }
}