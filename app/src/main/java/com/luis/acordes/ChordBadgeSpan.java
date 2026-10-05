package com.luis.acordes;

import android.graphics.*;
import android.text.style.ReplacementSpan;

/** Rounded chord badge that keeps the source's monospace character positions. */
public final class ChordBadgeSpan extends ReplacementSpan {
    @Override public int getSize(Paint paint,CharSequence text,int start,int end,Paint.FontMetricsInt metrics) {
        if(metrics!=null)paint.getFontMetricsInt(metrics);
        return (int)Math.ceil(paint.measureText(text,start,end));
    }
    @Override public void draw(Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint paint) {
        int color=paint.getColor();Paint.FontMetrics metrics=paint.getFontMetrics();float width=paint.measureText(text,start,end),pad=paint.getTextSize()*.08f;
        paint.setColor(0xff5844a0);canvas.drawRoundRect(x-pad,y+metrics.ascent-pad,x+width+pad,y+metrics.descent+pad,pad*2,pad*2,paint);
        paint.setColor(Color.WHITE);canvas.drawText(text,start,end,x,y,paint);paint.setColor(color);
    }
}
