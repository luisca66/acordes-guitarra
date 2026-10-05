package com.luis.acordes;

import android.text.Html;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Reads public chord versions, preserving source placement and attribution. */
public final class SongService {
    public static final String HOME="https://www.ultimate-guitar.com";
    public static final class Song {
        public final long id; public final String title,artist,url,content,author,tuning;
        Song(long id,String title,String artist,String url,String content,String author,String tuning) {
            this.id=id;this.title=title;this.artist=artist;this.url=url;this.content=content;this.author=author;this.tuning=tuning;
        }
        public String label() {return title+" · "+artist;}
    }
    public List<Song> search(String query) throws IOException,JSONException {
        String html;
        try {html=get(HOME+"/search.php?search_type=title&value="+URLEncoder.encode(query,StandardCharsets.UTF_8.name()));}
        catch(HttpStatusException e) {
            // The public search returns HTTP 404 for some queries with no matches.
            // Keep this interpretation scoped to search; a missing song is an error.
            if(e.status==404 && e.emptySearch)return Collections.emptyList();
            throw e;
        }
        JSONObject data=store(html);
        JSONArray results=data.optJSONArray("results");List<Song> songs=new ArrayList<>();
        if(results!=null)for(int i=0;i<results.length() && songs.size()<15;i++) {
            JSONObject row=results.getJSONObject(i);
            if(!row.optString("type").equals("Chords")) continue;
            String url=row.optString("tab_url");if(!safeUrl(url))continue;
            songs.add(new Song(row.getLong("id"),row.getString("song_name"),row.getString("artist_name"),url,"","",""));
        }
        return songs;
    }
    public Song load(Song selected) throws IOException,JSONException {
        if(!safeUrl(selected.url))throw new IOException("Enlace de canción no válido");
        JSONObject data=store(get(selected.url));JSONObject view=data.getJSONObject("tab_view");
        JSONObject wiki=view.optJSONObject("wiki_tab");String content=wiki==null?"":wiki.optString("content");
        if(content.trim().isEmpty())throw new IOException("Esta versión no tiene una hoja pública de letra y acordes. Elige otra versión.");
        String tuning="";JSONObject meta=view.optJSONObject("meta");if(meta!=null && meta.optJSONObject("tuning")!=null)tuning=meta.getJSONObject("tuning").optString("value");
        return new Song(selected.id,selected.title,selected.artist,selected.url,content,wiki.optString("username"),tuning);
    }
    static JSONObject store(String html) throws JSONException,IOException {
        Matcher matcher=Pattern.compile("<[^>]*\\bclass=[\"']js-store[\"'][^>]*\\bdata-content=\"([^\"]+)\"",Pattern.CASE_INSENSITIVE).matcher(html);
        if(!matcher.find())throw new IOException("La página del catálogo cambió o no está disponible. Puedes pegar una hoja de letra y acordes.");
        String json=Html.fromHtml(matcher.group(1),Html.FROM_HTML_MODE_LEGACY).toString();
        return new JSONObject(json).getJSONObject("store").getJSONObject("page").getJSONObject("data");
    }
    private static boolean safeUrl(String url) {
        try {URI uri=URI.create(url);return uri.getScheme().equals("https") && "tabs.ultimate-guitar.com".equals(uri.getHost()) && uri.getPath().startsWith("/tab/");}catch(Exception e){return false;}
    }
    private static final class HttpStatusException extends IOException {
        final int status;final boolean emptySearch;
        HttpStatusException(int status,boolean emptySearch){super(status==404?"Esta página ya no está disponible. Prueba otra búsqueda o versión de la canción.":"El catálogo no respondió ("+status+"). Intenta de nuevo.");this.status=status;this.emptySearch=emptySearch;}
    }
    static boolean emptySearchPage(String html) {
        return Pattern.compile("(?is)<title[^>]*>\\s*No results\\s*@\\s*Ultimate-Guitar\\.Com Search\\s*</title>").matcher(html).find();
    }
    private String get(String url) throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setRequestProperty("User-Agent","AcordesGuitarra/2.0");connection.setRequestProperty("Accept","text/html");
        try {
            int status=connection.getResponseCode();
            if(status==403)throw new IOException("El sitio no permite cargar esta página ahora. Puedes abrirla en el navegador o pegar tu hoja de acordes.");
            if(status==429)throw new IOException("El buscador está ocupado. Espera un momento y vuelve a intentar.");
            if(status!=200)throw new HttpStatusException(status,status==404 && emptySearchPage(readBody(connection.getErrorStream())));
            return readBody(connection.getInputStream());
        } finally {connection.disconnect();}
    }
    private String readBody(InputStream stream) throws IOException {
        if(stream==null)return "";
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>3_000_000)throw new IOException("Respuesta demasiado grande");}
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
