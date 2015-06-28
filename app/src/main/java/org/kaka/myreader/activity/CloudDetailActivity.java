package org.kaka.myreader.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.kaka.myreader.R;
import org.kaka.myreader.common.AppConstants;
import org.kaka.myreader.fragment.BookmarkFragment;
import org.kaka.myreader.fragment.ChapterFragment;
import org.kaka.myreader.fragment.CloudFragment;
import org.kaka.myreader.task.FileDownloadTask;
import org.w3c.dom.Text;

import java.util.List;
import java.util.Vector;

public class CloudDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_detail);

        init();
    }

    private void init() {
        Bundle bundle = getIntent().getBundleExtra("bundle");

        String id = bundle.getString(CloudFragment.KEY_ID);
        String path = bundle.getString(CloudFragment.KEY_PATH);

        ImageView image = (ImageView) findViewById(R.id.image);
        image.setImageBitmap((Bitmap) bundle.getParcelable(CloudFragment.KEY_IMAGE));

        TextView bookNameView = (TextView) findViewById(R.id.name);
        bookNameView.setText(bundle.getString(CloudFragment.KEY_NAME));

        TextView authorView = (TextView) findViewById(R.id.author);
        authorView.setText("作者 : " + bundle.getString(CloudFragment.KEY_AUTHOR));


        TextView sizeView = (TextView) findViewById(R.id.size);
        // TODO
        sizeView.setText("大小 : 1.5M (TXT)");

        RatingBar score = (RatingBar) findViewById(R.id.score);
        // TODO
        score.setRating(4.5f);

        TextView detailView = (TextView) findViewById(R.id.detail);
        detailView.append("内容简介:\n");
        detailView.append(bundle.getString(CloudFragment.KEY_DETAIL));

        ImageButton backBtn = (ImageButton) findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudDetailActivity.this.finish();
            }
        });

        Button downloadBtn = (Button) findViewById(R.id.download);
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }
}
