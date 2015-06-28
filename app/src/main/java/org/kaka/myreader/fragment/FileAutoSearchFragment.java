package org.kaka.myreader.fragment;

import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.KeyEvent;
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
import org.kaka.myreader.dlayer.dao.DaoFactory;
import org.kaka.myreader.dlayer.dao.MyBookDao;
import org.kaka.myreader.dlayer.entities.MyBookEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FileAutoSearchFragment extends ListFragment {
    private List<ItemHolder> listData = new ArrayList<>();
    private Set<File> files;
    private int count = 0;
    private MyBookDao myBookDao;
    private File currentDir;
    private Button btnImport;
    private TextView pathView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ItemHolder itemHolder = listData.get(position);
        File file = itemHolder.getFile();
        boolean imported = itemHolder.isImported();

        if (file.isDirectory()) {
            listData.clear();
            currentDir = file;
            setAdapter();
        } else if (!imported) {
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
        View view = inflater.inflate(R.layout.fragment_manual, container, false);
        pathView = (TextView) view.findViewById(R.id.path);
        pathView.setClickable(true);
        pathView.setFocusable(true);
        pathView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDir = Environment.getExternalStorageDirectory();
                setAdapter();
            }
        });

        files = new HashSet<>();

        DaoFactory factory = new DaoFactory(getActivity());
        myBookDao = factory.getMyBookDao();
        btnImport = (Button) getActivity().findViewById(R.id.importbook);
        currentDir = Environment.getExternalStorageDirectory();
        setAdapter();
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (files == null || files.size() == 0) {
                    return;
                }

                for (File file : files) {
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
                        files.remove(file);
                        count--;
                    } catch (Exception e) {
                        Log.e("FileSearchActivity", e.getMessage());
                    }
                }
                btnImport.setText("导入(" + count + ")");

                setAdapter();
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                String path = currentDir.getCanonicalPath();
                if (path.equals(AppConstants.SDCARD_DIR)) {
                    getActivity().setResult(FragmentActivity.RESULT_OK);
                    getActivity().finish();
                    return false;
                }

                currentDir = new File(path.substring(0, path.lastIndexOf("/")));
                setAdapter();
            }
        } catch (Exception e) {
            Log.e("", e.getMessage());
        }
        return false;
    }

    private void setAdapter() {
        getData(currentDir);
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
                        R.layout.importitem, null);
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

                if (file.isFile()) {
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
                } else {
                    TextView textView = new TextView(getActivity());
                    textView.setText(">");
                    textView.setId(R.id.fileSelection);
                    textView.setLayoutParams(getParams());
                    layout.addView(textView);
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

    private void getData(File dir) {
        try {
            setPathInfo(dir.getCanonicalPath());
            listData.clear();
            List<String> importedFiles = new ArrayList<>();
            if (myBookDao != null) {
                importedFiles = myBookDao.getFilePaths(AppConstants.ID_PREFIX_IMPORT);
            }
            List<ItemHolder> dirList = new ArrayList<>();
            for (File file : dir.listFiles()) {
                ItemHolder holder = new ItemHolder();
                String path = file.getCanonicalPath();
                if (AppConstants.APP_DIR.equals(path)) {
                    continue;
                }

                if (file.isFile()) {
                    String fileName = file.getName();
                    if (!fileName.endsWith(".pdf") && !fileName.endsWith(".txt") && !fileName.endsWith(".epub")) {
                        continue;
                    }
                    String ext = fileName.substring(file.getName().lastIndexOf(".") + 1);
                    holder.setFileName(fileName);
                    double size = (double) file.getTotalSpace() / 1024;
                    holder.setFileDetail("类型:" + ext + "  大小:" + AppConstants.DECIMAL_FORMAT.format(size) + "KB");
                    holder.setFileLogo(getResources().getDrawable(R.drawable.ic_action_document));
                    if (importedFiles.contains(path)) {
                        holder.setImported(true);
                    } else {
                        holder.setImported(false);
                    }

                    holder.setFile(file);
                    listData.add(holder);
                } else {
                    holder.setFileName(file.getName());
                    holder.setFileDetail(getFileCount(file) + "个文件");
                    holder.setFileLogo(getResources().getDrawable(R.drawable.ic_action_folder_closed));
                    holder.setImported(false);
                    holder.setFile(file);
                    dirList.add(holder);
                }
            }
            listData.addAll(dirList);
        } catch (Exception e) {
            Log.e("", e.getMessage());
        }
    }

    private void setPathInfo(String path) {
        if (!path.startsWith(AppConstants.SDCARD_DIR) || path.equals(AppConstants.SDCARD_DIR)) {
            SpannableString text = new SpannableString(path);
            text.setSpan(new URLSpan("#"), 0, path.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pathView.setText(text);
            return;
        }

        SpannableString text = new SpannableString(AppConstants.SDCARD_DIR);
        text.setSpan(new URLSpan("#"), 0, AppConstants.SDCARD_DIR.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        pathView.setText(text);

        String extPath = path.substring(AppConstants.SDCARD_DIR.length());
        pathView.append(extPath);
    }

    private int getFileCount(File dir) {
        int count = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                count++;
            }
        }

        return count;
    }

    public class ItemHolder {
        private File file;
        private String fileName;
        private String fileDetail;
        private Drawable fileLogo;
        private boolean imported;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileDetail() {
            return fileDetail;
        }

        public void setFileDetail(String fileDetail) {
            this.fileDetail = fileDetail;
        }

        public Drawable getFileLogo() {
            return fileLogo;
        }

        public void setFileLogo(Drawable fileLogo) {
            this.fileLogo = fileLogo;
        }

        public boolean isImported() {
            return imported;
        }

        public void setImported(boolean imported) {
            this.imported = imported;
        }
    }
}