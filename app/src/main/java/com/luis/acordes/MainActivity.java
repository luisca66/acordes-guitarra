package com.luis.acordes;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int INK=0xff29382e,MUTED=0xff6c7265,PAPER=0xfff7f3eb,RUST=0xffa85432,DARK=0xff121212,PURPLE=0xff5844a0;
    private EditText input,query;private TextView error,status,summary,songTitle,source;
    private LinearLayout results,songResults,songPanel,manualPanel,readerPanel;private Button share,songTab,manualTab;
    private java.util.List<Music.Chord> chords=Collections.emptyList();
    private Map<String,Music.Chord> sourcePositions=Collections.emptyMap();
    private String renderedTitle="Mis acordes",renderedInput="",sheetText="",sheetTitle="",sheetUrl="",sheetAuthor="",sheetTuning="";
    private final ExecutorService network=Executors.newFixedThreadPool(2);
    private final SongService songs=new SongService();private int request=0;private boolean manual=false;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(PAPER);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(24),dp(20),dp(28));scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});setContentView(scroll);
        page.addView(text("ACORDES DE GUITARRA",12,RUST,true));TextView title=text("Busca. Lee.\nToca.",34,INK,true);title.setPadding(0,dp(10),0,dp(8));page.addView(title);
        page.addView(text("Encuentra una canción o escribe tus acordes. Toca un acorde para ver su posición.",15,MUTED,false));
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(0,dp(18),0,dp(14));
        songTab=button("Canciones",INK,Color.WHITE);manualTab=button("Mis acordes",0xffe9e2d6,INK);tabs.addView(songTab,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams tabParam=new LinearLayout.LayoutParams(0,dp(50),1);tabParam.leftMargin=dp(8);tabs.addView(manualTab,tabParam);page.addView(tabs);
        songPanel=panel();page.addView(songPanel);songPanel.addView(text("BUSCAR UNA CANCIÓN",12,MUTED,true));
        query=field("Título y, si quieres, artista");query.setContentDescription("Título o artista de la canción");query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);songPanel.addView(query,new LinearLayout.LayoutParams(-1,dp(54)));
        Button search=button("Buscar canción",RUST,Color.WHITE);songPanel.addView(search,spaceParams(50,10));search.setOnClickListener(v->searchSongs());query.setOnEditorActionListener((v,a,e)->{if(a==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){searchSongs();return true;}return false;});
        status=text("Elige una versión para ver la letra y sus acordes.",13,MUTED,false);status.setPadding(0,dp(12),0,dp(8));status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);songPanel.addView(status);
        songResults=new LinearLayout(this);songResults.setOrientation(LinearLayout.VERTICAL);songPanel.addView(songResults);
        Button paste=button("Pegar letra con acordes",0xffeee7d9,INK);songPanel.addView(paste,spaceParams(48,8));paste.setOnClickListener(v->pasteSheet());
        manualPanel=panel();page.addView(manualPanel);manualPanel.addView(text("ESCRIBE LOS ACORDES",12,MUTED,true));
        input=field("C G Am F");input.setTextSize(21);input.setContentDescription("Nombres de acordes");manualPanel.addView(input,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView help=text("También: Do Sol Lam Fa · Dm7 G7 Cmaj7\nSin tonalidad ni grados. Los acordes repetidos aparecen una sola vez.",13,MUTED,false);help.setPadding(0,dp(10),0,dp(10));manualPanel.addView(help);
        Button generate=button("Ver posiciones",RUST,Color.WHITE);manualPanel.addView(generate,spaceParams(50,0));generate.setOnClickListener(v->{generateManual();hideKeyboard(input);});
        readerPanel=panel();readerPanel.setBackground(background(DARK,16));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(16);page.addView(readerPanel,rp);
        songTitle=text("",22,Color.WHITE,true);readerPanel.addView(songTitle);
        source=text("",12,0xffafa5d1,false);source.setPadding(0,dp(8),0,dp(12));readerPanel.addView(source);source.setOnClickListener(v->openSource());
        error=text("",14,0xffad342a,false);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);error.setPadding(0,dp(12),0,0);page.addView(error);
        summary=text("",18,INK,true);summary.setPadding(0,dp(18),0,dp(8));summary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(summary);
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);page.addView(results);
        share=button("Compartir posiciones como imagen",INK,Color.WHITE);page.addView(share,spaceParams(54,16));share.setOnClickListener(v->shareImage());
        TextView footer=text("E A D G B e · Afinación estándar\n× no tocar   ○ al aire   ● presionar el traste\nLa sexta cuerda está a la izquierda. Algunas posiciones requieren cejilla.\n\nLas versiones de canciones pueden variar. La búsqueda necesita internet y depende de las páginas públicas de Ultimate Guitar.",12,MUTED,false);footer.setPadding(0,dp(20),0,0);page.addView(footer);
        songTab.setOnClickListener(v->setMode(false));manualTab.setOnClickListener(v->setMode(true));
        SharedPreferences prefs=getPreferences(MODE_PRIVATE);query.setText(state!=null?state.getString("query",""):prefs.getString("query",""));input.setText(state!=null?state.getString("input","C G Am F"):prefs.getString("namedInput","C G Am F"));
        sheetText=prefs.getString("sheet","");sheetTitle=prefs.getString("sheetTitle","");sheetUrl=prefs.getString("sheetUrl","");sheetAuthor=prefs.getString("sheetAuthor","");sheetTuning=prefs.getString("sheetTuning","");
        if(!sheetText.isEmpty())renderSheet();else {readerPanel.setVisibility(View.GONE);share.setVisibility(View.GONE);}
        setMode(state!=null && state.getBoolean("manual",false));
    }
    private void setMode(boolean value) {
        manual=value;songTab.setBackground(background(value?0xffe9e2d6:INK,12));songTab.setTextColor(value?INK:Color.WHITE);manualTab.setBackground(background(value?INK:0xffe9e2d6,12));manualTab.setTextColor(value?Color.WHITE:INK);request++;status.setText("Elige una versión para ver la letra y sus acordes.");songPanel.setVisibility(value?View.GONE:View.VISIBLE);manualPanel.setVisibility(value?View.VISIBLE:View.GONE);
        if(value){readerPanel.setVisibility(View.GONE);generateManual();}else if(!sheetText.isEmpty())renderSheet();else {readerPanel.setVisibility(View.GONE);results.removeAllViews();summary.setText("");error.setText("");chords=Collections.emptyList();share.setVisibility(View.GONE);}
    }
    private void generateManual() {
        try {chords=Chords.parse(input.getText().toString());renderedTitle="Mis acordes";renderedInput=input.getText().toString();error.setText("");renderCards();getPreferences(MODE_PRIVATE).edit().putString("namedInput",renderedInput).apply();}
        catch(IllegalArgumentException e){error.setText(e.getMessage());chords=Collections.emptyList();renderCards();}
    }
    private void searchSongs() {
        String q=query.getText().toString().trim();if(q.length()<2){status.setText("Escribe al menos dos letras del título.");return;}
        int token=++request;songResults.removeAllViews();status.setText("Buscando canciones…");hideKeyboard(query);getPreferences(MODE_PRIVATE).edit().putString("query",q).apply();
        network.execute(()->{
            try {java.util.List<SongService.Song> found=songs.search(q);runOnUiThread(()->{if(!current(token))return;status.setText(found.isEmpty()?"No encontré canciones con «"+q+"». Prueba una parte del título o agrega el artista.":"Elige canción, artista y versión:");
                if(found.isEmpty() && q.lastIndexOf(' ')>0){String shorter=q.substring(0,q.lastIndexOf(' ')).trim();if(shorter.length()>=2){Button retry=button("Buscar «"+shorter+"»",0xffeee7d9,INK);songResults.addView(retry,spaceParams(50,8));retry.setOnClickListener(v->{query.setText(shorter);searchSongs();});}}
                int version=0;for(SongService.Song song:found){Button b=button(song.label()+" · v"+(++version),0xffeee7d9,INK);b.setMinHeight(dp(54));songResults.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->loadSong(song));}
            });}catch(Exception e){runOnUiThread(()->{if(current(token)){status.setText(networkMessage(e));Button web=button("Abrir búsqueda en el navegador",0xffeee7d9,INK);songResults.addView(web);web.setOnClickListener(v->openUrl(SongService.HOME+"/search.php?search_type=title&value="+Uri.encode(q)));}});}
        });
    }
    private void loadSong(SongService.Song selected) {
        int token=++request;status.setText("Cargando letra y acordes…");
        network.execute(()->{try {SongService.Song full=songs.load(selected);runOnUiThread(()->{if(!current(token))return;sheetTitle=full.label();sheetText=full.content;sheetUrl=full.url;sheetAuthor=full.author;sheetTuning=full.tuning;saveSheet();songResults.removeAllViews();status.setText("Canción lista. Toca cualquier acorde para ver su posición.");renderSheet();});}
            catch(Exception e){runOnUiThread(()->{if(current(token)){status.setText(networkMessage(e));Button web=button("Abrir esta versión en el navegador",0xffeee7d9,INK);songResults.addView(web);web.setOnClickListener(v->openUrl(selected.url));}});}});
    }
    private boolean current(int token){return token==request && !isFinishing() && !isDestroyed();}
    private String networkMessage(Exception e){return e instanceof java.net.SocketTimeoutException?"La búsqueda tardó demasiado. Intenta de nuevo.":e instanceof java.net.UnknownHostException?"No hay conexión con el catálogo. Puedes escribir tus acordes o pegar una hoja.":e instanceof org.json.JSONException?"Cambió el formato de la página. Puedes abrirla en el navegador o pegar tu hoja.":e.getMessage()==null?"No se pudo cargar la canción. Intenta de nuevo.":e.getMessage();}
    private void renderSheet() {
        readerPanel.setVisibility(View.VISIBLE);while(readerPanel.getChildCount()>2)readerPanel.removeViewAt(2);songTitle.setText(sheetTitle);
        source.setText(sheetUrl.isEmpty()?"Tu hoja · Toca un acorde para ver su posición":"Ultimate Guitar · "+(sheetAuthor.isEmpty()?"versión pública":sheetAuthor)+"\nAbrir la versión original ↗");
        java.util.List<SongSheet.Line> lines=SongSheet.parse(sheetText);sourcePositions=SongSheet.positions(lines);SpannableStringBuilder body=new SpannableStringBuilder();
        for(SongSheet.Line line:lines){if(SongSheet.isPositionLine(line) || (body.length()==0 && line.plain().trim().isEmpty()))continue;int start=body.length();for(SongSheet.Piece piece:line.pieces){int a=body.length();body.append(piece.text);int b=body.length();if(piece.chord){body.setSpan(new ChordBadgeSpan(),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);body.setSpan(new ClickableSpan(){@Override public void onClick(View v){showChord(piece.text);}@Override public void updateDrawState(TextPaint p){p.setColor(Color.WHITE);p.setUnderlineText(false);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}},a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}}
            if(line.heading){body.setSpan(new ForegroundColorSpan(0xffa6b7c9),start,body.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);body.setSpan(new StyleSpan(Typeface.BOLD),start,body.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}body.append('\n');}
        TextView lyrics=text("",15,0xfff2f0f7,false);lyrics.setTypeface(Typeface.MONOSPACE);lyrics.setText(body);lyrics.setMovementMethod(LinkMovementMethod.getInstance());lyrics.setLineSpacing(dp(7),1);lyrics.setPadding(0,dp(8),dp(10),dp(10));lyrics.setContentDescription("Letra y acordes de "+sheetTitle);lyrics.setHorizontallyScrolling(true);
        LinkedHashMap<String,Music.Chord> available=new LinkedHashMap<>();java.util.List<String> missing=new ArrayList<>();for(String name:SongSheet.uniqueChords(lines)){try{Music.Chord c=sourcePositions.containsKey(name)?sourcePositions.get(name):Chords.one(name);available.putIfAbsent(c.name,c);}catch(IllegalArgumentException e){missing.add(name);}}
        boolean standard=sheetTuning.isEmpty() || sheetTuning.equalsIgnoreCase("E A D G B E");
        if(!standard){available.clear();error.setText("La fuente indica afinación "+sheetTuning+". Estos diagramas solo admiten afinación estándar; se conserva la hoja original.");}
        else error.setText(missing.isEmpty()?"":"Se conserva la hoja completa. Aún no hay diagrama para: "+String.join(", ",missing));
        chords=standard?new ArrayList<>(sourcePositions.isEmpty()?available.values():sourcePositions.values()):Collections.emptyList();renderedTitle=sheetTitle;renderedInput=String.join(" ",sourcePositions.isEmpty()?available.keySet():sourcePositions.keySet());
        if(!chords.isEmpty()) {
            TextView visualTitle=text("POSICIONES VISUALES",12,0xffafa5d1,true);visualTitle.setPadding(0,dp(8),0,dp(8));readerPanel.addView(visualTitle);
            readerPanel.addView(text("× no tocar · ○ al aire · ● presionar\nToca un diagrama para ampliarlo.",12,0xffc6c0d2,false));
            readerPanel.addView(diagramGrid(chords));
        }
        HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setFillViewport(true);horizontal.addView(lyrics,new HorizontalScrollView.LayoutParams(-2,-2));readerPanel.addView(horizontal,new LinearLayout.LayoutParams(-1,-2));
        renderCards();
    }
    private void renderCards(){results.removeAllViews();summary.setText(!manual || chords.isEmpty()?"":chords.size()+" posiciones de acordes");share.setVisibility(chords.isEmpty()?View.GONE:View.VISIBLE);
        if(manual)results.addView(diagramGrid(chords));
    }
    private LinearLayout diagramGrid(java.util.List<Music.Chord> diagrams) {
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<diagrams.size();i+=2) {
            LinearLayout row=new LinearLayout(this);grid.addView(row,new LinearLayout.LayoutParams(-1,-2));
            for(int j=i;j<Math.min(i+2,diagrams.size());j++) {
                Music.Chord c=diagrams.get(j);LinearLayout card=panel();card.setPadding(dp(6),dp(10),dp(6),dp(8));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(j==i?0:dp(4),dp(10),j==i?dp(4):0,0);row.addView(card,p);
                TextView name=text(c.name,22,INK,true);name.setGravity(Gravity.CENTER);card.addView(name);
                card.addView(new ChordView(this,c),new LinearLayout.LayoutParams(-1,-2));
                if(!manual && sourcePositions.containsKey(c.name)){TextView origin=text("Digitación de esta versión",10,MUTED,false);origin.setGravity(Gravity.CENTER);card.addView(origin);}
                card.setOnClickListener(v->showChord(c.name));
            }
            if(i+1==diagrams.size())row.addView(new View(this),new LinearLayout.LayoutParams(0,0,1));
        }
        return grid;
    }
    private void showChord(String name){try {
        if(!manual && !sheetTuning.isEmpty() && !sheetTuning.equalsIgnoreCase("E A D G B E"))throw new IllegalArgumentException("La fuente usa otra afinación: "+sheetTuning+".");
        Music.Chord chord=!manual && sourcePositions.containsKey(name)?sourcePositions.get(name):Chords.one(name);LinearLayout box=panel();box.addView(new ChordView(this,chord),new LinearLayout.LayoutParams(-1,-2));box.addView(text("E A D G B e · Afinación estándar\n× no tocar · ○ al aire\nEl número lateral indica el traste.",13,MUTED,false));new AlertDialog.Builder(this).setTitle(chord.name).setView(box).setPositiveButton("Listo",null).show();}catch(IllegalArgumentException e){new AlertDialog.Builder(this).setTitle(name).setMessage(e.getMessage()).setPositiveButton("Listo",null).show();}}
    private void pasteSheet(){LinearLayout box=panel();box.addView(text("Pega una hoja con acordes encima de la letra. Se conservan los espacios y las secciones como [Verso].",14,MUTED,false));EditText content=new EditText(this);content.setMinLines(8);content.setMaxLines(12);content.setGravity(Gravity.TOP);content.setTypeface(Typeface.MONOSPACE);content.setHint("[Verso]\nC         G\nTu letra aquí");content.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);box.addView(content);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Letra y acordes").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Abrir hoja",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{String raw=content.getText().toString();if(raw.trim().isEmpty()){content.setError("Pega tu hoja de acordes");return;}if(raw.length()>60_000){content.setError("Usa hasta 60 000 caracteres");return;}request++;sheetText=raw;sheetTitle="Mi hoja de acordes";sheetUrl="";sheetAuthor="";sheetTuning="";saveSheet();renderSheet();dialog.dismiss();}));dialog.show();}
    private void saveSheet(){getPreferences(MODE_PRIVATE).edit().putString("sheet",sheetText).putString("sheetTitle",sheetTitle).putString("sheetUrl",sheetUrl).putString("sheetAuthor",sheetAuthor).putString("sheetTuning",sheetTuning).apply();}
    private void openSource(){if(!sheetUrl.isEmpty())openUrl(sheetUrl);}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"No hay un navegador disponible",Toast.LENGTH_SHORT).show();}}
    private void hideKeyboard(View v){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}
    private void shareImage(){if(chords.isEmpty())return;if(chords.size()>24){Toast.makeText(this,"Puedes compartir hasta 24 posiciones a la vez.",Toast.LENGTH_LONG).show();return;}try{int rows=(chords.size()+1)/2,w=1000,h=160+rows*440+80;Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(PAPER);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(INK);p.setTextSize(32);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));canvas.drawText(TextUtils.ellipsize(renderedTitle,new TextPaint(p),920,TextUtils.TruncateAt.END).toString(),40,58,p);p.setTextSize(21);p.setTypeface(Typeface.DEFAULT);canvas.drawText("Afinación: E A D G B e · × no tocar · ○ al aire",40,102,p);
        for(int i=0;i<chords.size();i++){Music.Chord c=chords.get(i);float x=30+(i%2)*490,y=145+(i/2)*440;p.setColor(Color.WHITE);canvas.drawRoundRect(x,y,x+460,y+420,22,22,p);p.setColor(INK);p.setTextSize(32);canvas.drawText(c.name,x+24,y+44,p);canvas.save();canvas.translate(x+35,y+60);ChordView.drawDiagram(canvas,c,390,338);canvas.restore();}p.setColor(MUTED);p.setTextSize(20);canvas.drawText("Acordes de guitarra · Los números laterales indican el traste",40,h-30,p);File directory=new File(getCacheDir(),"shared");if(!directory.exists() && !directory.mkdirs())throw new IOException();File[] old=directory.listFiles();if(old!=null)for(File f:old)if(System.currentTimeMillis()-f.lastModified()>7L*24*60*60*1000)f.delete();File file=new File(directory,"acordes-"+System.currentTimeMillis()+".png");try(FileOutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();}finally{bitmap.recycle();}Uri uri=Uri.parse("content://com.luis.acordes.images/"+file.getName());Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("image/png");intent.putExtra(Intent.EXTRA_STREAM,uri);intent.putExtra(Intent.EXTRA_TEXT,renderedTitle+" · "+renderedInput+(manual || sheetUrl.isEmpty()?"":"\n"+sheetUrl));intent.setClipData(ClipData.newRawUri("Posiciones",uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(intent,"Compartir posiciones"));
        }catch(IOException|ActivityNotFoundException e){Toast.makeText(this,"No se pudo compartir la imagen. Intenta de nuevo.",Toast.LENGTH_LONG).show();}}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("input",input.getText().toString());state.putString("query",query.getText().toString());state.putBoolean("manual",manual);}
    @Override protected void onDestroy(){request++;network.shutdownNow();super.onDestroy();}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(INK);e.setTextSize(17);e.setHint(hint);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(background(0xfff4efe5,10));e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);return e;}
    private LinearLayout.LayoutParams spaceParams(int height,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(height));p.topMargin=dp(top);return p;}
    private LinearLayout panel(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(16),dp(16),dp(16));b.setBackground(background(Color.WHITE,16));return b;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    private Button button(String title,int color,int foreground){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(foreground);b.setBackground(background(color,12));return b;}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
