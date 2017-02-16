package com.weihuoya.bboo;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by zhangwei1 on 2016/5/3.
 */
public class GridDividerDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private boolean mDrawVertical;
    private boolean mDrawHorizontal;

    public GridDividerDecoration(Drawable divider, boolean vertical, boolean horizontal) {
        mDivider = divider;
        mDrawVertical = vertical;
        mDrawHorizontal = horizontal;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left, right, top, bottom;
        RecyclerView.LayoutParams params;
        View child;

        for (int i = 0; i < parent.getChildCount(); i++) {
            child = parent.getChildAt(i);
            params = (RecyclerView.LayoutParams) child.getLayoutParams();

            if(mDrawVertical) {
                // Vertical
                left = child.getLeft() - params.leftMargin;
                right = child.getRight() + params.rightMargin;
                top = child.getBottom() + params.bottomMargin;
                bottom = top + mDivider.getIntrinsicHeight() - 1;
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }

            if(mDrawHorizontal) {
                // Horizontal
                left = child.getRight() + params.rightMargin;
                right = left + mDivider.getIntrinsicWidth() - 1;
                top = child.getTop() - params.topMargin;
                bottom = child.getBottom() + params.bottomMargin;
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}
