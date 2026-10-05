package com.luis.acordes;

import java.util.*;
import java.util.regex.*;

/** Named chords only: no key, degrees or automatic harmony. */
public final class Chords {
    private static final Map<String,int[]> TYPES=new LinkedHashMap<>();
    private static final Map<String,Music.Chord> CACHE=new HashMap<>();
    private static final int[] TUNING={40,45,50,55,59,64};
    static {
        type("",0,4,7);type("m",0,3,7);type("7",0,4,7,10);type("maj7",0,4,7,11);type("m7",0,3,7,10);
        type("dim",0,3,6);type("dim7",0,3,6,9);type("aug",0,4,8);type("sus2",0,2,7);type("sus4",0,5,7);
        type("add9",0,2,4,7);type("madd9",0,2,3,7);type("7sus4",0,5,7,10);type("7sus2",0,2,7,10);
        type("6",0,4,7,9);type("m6",0,3,7,9);type("m7b5",0,3,6,10);type("5",0,7);
        type("9",0,2,4,7,10);type("m9",0,2,3,7,10);type("maj9",0,2,4,7,11);
    }
    private static void type(String name,int... notes) {TYPES.put(name,notes);}
    public static int[] intervals(String quality) {return TYPES.get(quality).clone();}
    public static Set<String> qualities() {return Collections.unmodifiableSet(TYPES.keySet());}
    public static List<Music.Chord> parse(String text) {
        if(text==null || text.trim().isEmpty()) throw new IllegalArgumentException("Escribe los acordes, por ejemplo: C G Am F.");
        String[] tokens=text.trim().split("[\\s,;|–—−-]+");
        if(tokens.length==0) throw new IllegalArgumentException("Escribe al menos un acorde.");
        if(tokens.length>64) throw new IllegalArgumentException("Usa hasta 64 nombres de acordes.");
        LinkedHashMap<String,Music.Chord> unique=new LinkedHashMap<>();
        for(String token:tokens) {Music.Chord c=one(token);unique.putIfAbsent(c.name,c);}
        if(unique.size()>24) throw new IllegalArgumentException("Usa hasta 24 acordes diferentes.");
        return new ArrayList<>(unique.values());
    }
    public static synchronized Music.Chord one(String text) {
        String normalized=text.replace('♯','#').replace('♭','b').replace("°","dim").replace("º","dim");
        Matcher spanish=Pattern.compile("^(Do|Re|Mi|Fa|Sol|La|Si)([#b]?)([^/]*)(/.*)?$").matcher(normalized);
        if(spanish.matches() && (TYPES.containsKey(spanish.group(3)) || spanish.group(3).equals("min") || spanish.group(3).equals("M7"))) {
            int index=Arrays.asList("Do","Re","Mi","Fa","Sol","La","Si").indexOf(spanish.group(1));
            normalized=new String[]{"C","D","E","F","G","A","B"}[index]+spanish.group(2)+spanish.group(3)+(spanish.group(4)==null?"":spanish.group(4));
        }
        Matcher m=Pattern.compile("^([A-Ga-g])([#b]?)([^/]*)(?:/([A-Ga-g])([#b]?))?$").matcher(normalized);
        if(!m.matches()) throw new IllegalArgumentException("No reconozco «"+text+"». Escribe nombres como C, Sol, Am, F# o D/F#. No necesitas tonalidad ni grados.");
        String letter=m.group(1);
        int root=pitch(letter,m.group(2));String quality=m.group(3);
        if(quality.equals("min")) quality="m";if(quality.equals("M7")) quality="maj7";
        if(!TYPES.containsKey(quality)) throw new IllegalArgumentException("Aún no hay una posición para «"+text+"». Puedes usar m, 7, maj7, m7, sus2, sus4, add9, 7sus4, 6, m6, dim, dim7, aug, 9, m9, maj9, m7b5 o 5.");
        String name=letter.toUpperCase(Locale.ROOT)+m.group(2)+quality;
        Integer bass=m.group(4)==null?null:pitch(m.group(4),m.group(5));
        if(bass!=null) name+="/"+m.group(4).toUpperCase(Locale.ROOT)+m.group(5);
        if(CACHE.containsKey(name)) return CACHE.get(name);
        int[] frets;
        if(bass==null && new ArrayList<>(TYPES.keySet()).indexOf(quality)<10) frets=new Music.Chord(root,quality,"",m.group(2).equals("b")).frets;
        else if(name.equals("Cadd9")) frets=new int[]{-1,3,2,0,3,3};
        else if(name.equals("A7sus4")) frets=new int[]{-1,0,2,0,3,0};
        else if(name.equals("G/F#")) frets=new int[]{2,2,0,0,3,3};
        else frets=new Voicing(root,TYPES.get(quality),bass).find();
        Music.Chord chord=new Music.Chord(root,quality,name,frets);CACHE.put(name,chord);return chord;
    }
    private static int pitch(String letter,String accidental) {return Math.floorMod(new int[]{9,11,0,2,4,5,7}[Character.toUpperCase(letter.charAt(0))-'A']+(accidental.equals("#")?1:accidental.equals("b")?-1:0),12);}
    private static final class Voicing {
        final int required,allowed,bass;int bestScore=Integer.MAX_VALUE;int[] best;
        Voicing(int root,int[] intervals,Integer bass) {
            int mask=0;for(int n:intervals)mask|=1<<((root+n)%12);
            required=mask;allowed=bass==null?mask:mask|1<<bass;this.bass=bass==null?root:bass;
        }
        int[] find() {
            for(int start=1;start<=12;start++) visit(0,new int[6],start,0,-1);
            if(best==null) throw new IllegalArgumentException("No encontré una posición cómoda para este acorde con ese bajo. Prueba otra escritura del acorde.");
            return best;
        }
        void visit(int string,int[] frets,int start,int mask,int lowest) {
            if(string==6) {
                if((mask&required)!=required || lowest<0 || lowest%12!=bass) return;
                int muted=0,pressed=0,max=0,min=99,open=0;for(int f:frets) {if(f<0)muted++;else if(f==0)open++;else {pressed++;min=Math.min(min,f);max=Math.max(max,f);}}
                if(6-muted<2) return;
                int fingers=pressed;
                // A barre may replace several presses at the lowest fretted position.
                for(int a=0;a<6;a++) if(frets[a]==min) for(int b=a+1;b<6;b++) if(frets[b]==min) {
                    boolean clear=true;int same=0;for(int i=a;i<=b;i++) {if(frets[i]>=0 && frets[i]<min)clear=false;if(frets[i]==min)same++;}
                    if(clear) fingers=Math.min(fingers,pressed-same+1);
                }
                if(fingers>4) return;
                int score=max*8+muted*9+fingers*3+(min==99?0:max-min)*4-open*2;
                if(score<bestScore) {bestScore=score;best=frets.clone();}return;
            }
            frets[string]=-1;visit(string+1,frets,start,mask,lowest);
            for(int f=0;f<=start+3;f++) {
                if(f>0 && f<start)continue;int note=TUNING[string]+f;
                if((allowed&(1<<(note%12)))==0)continue;
                int low=lowest<0?note:Math.min(lowest,note);
                frets[string]=f;visit(string+1,frets,start,mask|1<<(note%12),low);
            }
        }
    }
}
