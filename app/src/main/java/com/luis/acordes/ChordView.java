package com.luis.acordes;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class ChordView extends View {
    private final Music.Chord chord;
    public ChordView(Context context, Music.Chord chord) {
        super(context); this.chord=chord;
        StringBuilder description=new StringBuilder(chord.name+". Trastes desde la sexta hasta la primera cuerda: ");
        for(int f:chord.frets) description.append(f<0?"silenciada":f==0?"al aire":f).append(", ");
        setContentDescription(description.toString());
    }
    @Override protected void onMeasure(int w,int h) {
        int width=MeasureSpec.getSize(w); setMeasuredDimension(width,Math.round(width*0.87f));
    }
    @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); drawDiagram(canvas,chord,getWidth(),getHeight()); }
    public static void drawDiagram(Canvas canvas,Music.Chord chord,float width,float height) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); canvas.save(); canvas.scale(width/300f,height/260f);
        int min=99,max=0; for(int f:chord.frets) if(f>0) {min=Math.min(min,f);max=Math.max(max,f);}
        int start=max<=5?1:min; int rows=Math.max(5,max-start+1);
        float left=55,right=245,top=44,bottom=224,dx=(right-left)/5,dy=(bottom-top)/rows;
        p.setColor(Color.rgb(215,204,187)); p.setStrokeWidth(1.5f);
        for(int i=0;i<=rows;i++) canvas.drawLine(left,top+i*dy,right,top+i*dy,p);
        for(int i=0;i<6;i++) {p.setStrokeWidth(2f-i*.17f);canvas.drawLine(left+i*dx,top,left+i*dx,bottom,p);}
        p.setColor(Color.rgb(43,56,45));p.setStrokeWidth(start==1?5:2);canvas.drawLine(left,top,right,top,p);
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(17);
        String[] labels={"E","A","D","G","B","e"};
        for(int i=0;i<6;i++) {
            float x=left+i*dx;int f=chord.frets[i];
            p.setColor(Color.rgb(95,100,90));p.setStyle(Paint.Style.FILL);canvas.drawText(labels[i],x,248,p);
            if(f<0) {p.setStrokeWidth(2);canvas.drawLine(x-5,18,x+5,28,p);canvas.drawLine(x+5,18,x-5,28,p);}
            else if(f==0) {p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawCircle(x,23,6,p);p.setStyle(Paint.Style.FILL);}
            else {p.setColor(Color.rgb(168,84,50));canvas.drawCircle(x,top+(f-start+.5f)*dy,11,p);}
        }
        p.setColor(Color.rgb(95,100,90));p.setTextSize(13);p.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(start+"",left-13,top+dy*.6f,p);
        canvas.restore();
    }
}
