package org.kaka.myreader.composite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class ClipImageBorderView extends View {
    private int horizontalPadding = 20;
    private int borderWidth = 1;
    private Paint paint;

    public ClipImageBorderView(Context context) {
        this(context, null);
    }

    public ClipImageBorderView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClipImageBorderView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        horizontalPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, horizontalPadding, getResources().getDisplayMetrics());
        borderWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderWidth, getResources().getDisplayMetrics());
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //计算矩形区域的宽度
        int width = getWidth() - 2 * horizontalPadding;
        //计算距离屏幕垂直边界 的边距
        int verticalPadding = (getHeight() - width) / 2;
        paint.setColor(Color.parseColor("#aa000000"));
        paint.setStyle(Paint.Style.FILL);
        // 绘制左边1
        canvas.drawRect(0, 0, horizontalPadding, getHeight(), paint);
        // 绘制右边2
        canvas.drawRect(getWidth() - horizontalPadding, 0, getWidth(),
                getHeight(), paint);
        // 绘制上边3
        canvas.drawRect(horizontalPadding, 0, getWidth() - horizontalPadding,
                verticalPadding, paint);
        // 绘制下边4
        canvas.drawRect(horizontalPadding, getHeight() - verticalPadding,
                getWidth() - horizontalPadding, getHeight(), paint);
        // 绘制外边框
        paint.setColor(Color.parseColor("#FFFFFF"));
        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(horizontalPadding, verticalPadding, getWidth()
                - horizontalPadding, getHeight() - verticalPadding, paint);
    }

    public void setHorizontalPadding(int horizontalPadding) {
        this.horizontalPadding = horizontalPadding;

    }
}
