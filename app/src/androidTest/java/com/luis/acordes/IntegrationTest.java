package com.luis.acordes;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Small device smoke test for the live public source and Android HTML decoding. */
public final class IntegrationTest extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        SongService service=new SongService();
        if(!SongService.emptySearchPage("<title>No results @ Ultimate-Guitar.Com Search</title>"))throw new AssertionError("No-results page recognition");
        if(SongService.emptySearchPage("<title>404 Not Found</title>"))throw new AssertionError("Generic 404 must remain a source error");
        if(!service.search("loving spoons").isEmpty())throw new AssertionError("Regression query should produce an empty result, not a network error");
        List<SongService.Song> found=service.search("rayando el sol");
        if(found.isEmpty())throw new AssertionError("No public search results");
        SongService.Song selected=found.stream().filter(s->s.artist.toLowerCase(Locale.ROOT).contains("man")).findFirst().orElse(found.get(0));
        SongService.Song full=service.load(selected);List<SongSheet.Line> lines=SongSheet.parse(full.content);List<String> names=SongSheet.uniqueChords(lines);
        if(names.isEmpty() || lines.stream().noneMatch(l->l.pieces.stream().anyMatch(p->!p.chord && p.text.trim().length()>12)))throw new AssertionError("Missing chords or lyrics");
        for(String name:names)Chords.one(name);
        // Original sample text, so the UI screenshot contains no downloaded lyrics.
        getTargetContext().getSharedPreferences("MainActivity",0).edit()
            .putString("sheetTitle","Ejemplo de lectura")
            .putString("sheet","G 320033\nD xx0323\nEm 022000\nC 332010\nCadd9 x32030\nB7 x21202\n\n[Intro]\n[ch]D[/ch]  [ch]Am[/ch]  [ch]G[/ch]  [ch]Bm[/ch]\n\n[Verso 1]\n[ch]D[/ch]        [ch]Am[/ch]          [ch]G[/ch]\nHoy suena la guitarra\n[ch]G[/ch]                   [ch]Bm[/ch]\ny vuelvo a empezar\n")
            .putString("sheetUrl","").putString("sheetAuthor","").putString("sheetTuning","").apply();
        Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        runOnMainSync(()->{
            if(findText(activity.getWindow().getDecorView(),"Ejemplo de lectura")==null)throw new AssertionError("Missing sheet title");
            TextView lyrics=findContaining(activity.getWindow().getDecorView(),"Hoy suena la guitarra");
            if(lyrics==null || !(lyrics.getText() instanceof android.text.Spanned))throw new AssertionError("Missing interactive lyric sheet");
            android.text.style.ClickableSpan[] spans=((android.text.Spanned)lyrics.getText()).getSpans(0,lyrics.length(),android.text.style.ClickableSpan.class);
            if(spans.length!=9)throw new AssertionError("Expected 9 clickable chords, got "+spans.length);
            if(lyrics.getText().toString().contains("320033"))throw new AssertionError("Source fret text was not converted to a diagram");
            if(findText(activity.getWindow().getDecorView(),"POSICIONES VISUALES")==null)throw new AssertionError("Missing visible diagrams before lyrics");
            if(countDiagrams(activity.getWindow().getDecorView())!=6)throw new AssertionError("Only the six source legend entries should become visible diagrams");
            findText(activity.getWindow().getDecorView(),"Mis acordes").performClick();
            if(findText(activity.getWindow().getDecorView(),"4 posiciones de acordes")==null)throw new AssertionError("Manual chord mode");
            findText(activity.getWindow().getDecorView(),"Canciones").performClick();
        });
        waitForIdleSync();
        runOnMainSync(()->{
            ScrollView scroll=findScroll(activity.getWindow().getDecorView());
            View section=findText(activity.getWindow().getDecorView(),"POSICIONES VISUALES");
            android.graphics.Rect rect=new android.graphics.Rect();section.getDrawingRect(rect);scroll.offsetDescendantRectToMyCoords(section,rect);scroll.scrollTo(0,rect.top-dp(activity,16));
            View decor=activity.getWindow().getDecorView();Bitmap screenshot=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);decor.draw(new Canvas(screenshot));
            try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),"lectura-test.png"))){screenshot.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new RuntimeException(e);}finally{screenshot.recycle();}
        });
        result.putString("result","PASS: search + public song + Android entity decoding + lyric/chord parsing + UI modes + 9 clickable chord spans; "+names.size()+" song chord diagrams. Source: "+full.url);
        finish(-1,result);
    }catch(Throwable e){result.putString("result","FAIL: "+e);finish(0,result);}}
    private TextView findText(View v,String exact){if(v instanceof TextView && ((TextView)v).getText().toString().equals(exact))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=findText(((ViewGroup)v).getChildAt(i),exact);if(found!=null)return found;}return null;}
    private TextView findContaining(View v,String fragment){if(v instanceof TextView && ((TextView)v).getText().toString().contains(fragment))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=findContaining(((ViewGroup)v).getChildAt(i),fragment);if(found!=null)return found;}return null;}
    private int countDiagrams(View v){int count=v instanceof ChordView?1:0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)count+=countDiagrams(((ViewGroup)v).getChildAt(i));return count;}
    private ScrollView findScroll(View v){if(v instanceof ScrollView)return (ScrollView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ScrollView found=findScroll(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}return null;}
    private int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
}
