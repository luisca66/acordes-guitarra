package com.luis.acordes;

import java.util.*;
import java.util.regex.*;

/** Pure music logic. Frets are ordered from low E to high e; -1 means muted. */
public final class Music {
    public static final String[] KEYS = {"C · Do", "C# · Do♯", "D · Re", "Eb · Mi♭", "E · Mi", "F · Fa", "F# · Fa♯", "G · Sol", "Ab · La♭", "A · La", "Bb · Si♭", "B · Si"};
    private static final String[] SHARP = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    private static final String[] FLAT = {"C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B"};
    private static final Map<String,int[]> OPEN = new HashMap<>();
    static {
        put("C", -1,3,2,0,1,0); put("D",-1,-1,0,2,3,2); put("E",0,2,2,1,0,0);
        put("G",3,2,0,0,0,3); put("A",-1,0,2,2,2,0); put("Am",-1,0,2,2,1,0);
        put("Dm",-1,-1,0,2,3,1); put("Em",0,2,2,0,0,0);
        put("C7",-1,3,2,3,1,0); put("D7",-1,-1,0,2,1,2); put("E7",0,2,0,1,0,0);
        put("G7",3,2,0,0,0,1); put("A7",-1,0,2,0,2,0); put("B7",-1,2,1,2,0,2);
        put("Cmaj7",-1,3,2,0,0,0); put("Dmaj7",-1,-1,0,2,2,2); put("Emaj7",0,2,1,1,0,0);
        put("Am7",-1,0,2,0,1,0); put("Dm7",-1,-1,0,2,1,1); put("Em7",0,2,0,0,0,0);
        put("Dsus2",-1,-1,0,2,3,0); put("Dsus4",-1,-1,0,2,3,3);
        put("F",1,3,3,2,1,1); put("Bm",-1,2,4,4,3,2);
    }
    private static void put(String n,int... f) { OPEN.put(n,f); }
    public static final class Chord {
        public final int root; public final String quality, name, degree; public final int[] frets;
        Chord(int root,String quality,String name,int[] frets) {
            this.root=root;this.quality=quality;this.name=name;this.degree="";this.frets=frets.clone();
        }
        public Chord(int root,String quality,String degree,boolean flats) {
            this.root=Math.floorMod(root,12); this.quality=quality; this.degree=degree;
            name=(flats?FLAT:SHARP)[this.root]+quality;
            int[] open=OPEN.get(SHARP[this.root]+quality);
            frets=open!=null?open.clone():movable(this.root,quality);
        }
    }
    public static List<Chord> parse(int key,boolean minor,String progression) {
        if(progression==null || progression.trim().isEmpty()) throw new IllegalArgumentException("Escribe una progresión, por ejemplo: I V vi IV.");
        String normalized=progression.replace('♯','#').replace('♭','b').replace('°','º');
        String[] tokens=normalized.trim().split("[\\s,;|–—−-]+");
        if(tokens.length==0) throw new IllegalArgumentException("Escribe al menos un acorde o grado.");
        if(tokens.length>24) throw new IllegalArgumentException("Usa hasta 24 acordes por progresión.");
        List<Chord> result=new ArrayList<>();
        int[] scale=minor?new int[]{0,2,3,5,7,8,10}:new int[]{0,2,4,5,7,9,11};
        String[] qualities=minor?new String[]{"m","dim","","m","m","",""}:new String[]{"","m","m","","","m","dim"};
        boolean flats=key==3 || key==5 || key==8 || key==10 || (minor && (key==0 || key==2 || key==7));
        for(String token:tokens) {
            Matcher direct=Pattern.compile("^([A-Ga-g])([#b]?)(maj7|m7|dim7|dim|aug|sus2|sus4|m|7)?$").matcher(token);
            if(direct.matches()) {
                int root=new int[]{9,11,0,2,4,5,7}[Character.toUpperCase(direct.group(1).charAt(0))-'A'];
                root+=direct.group(2).equals("#")?1:direct.group(2).equals("b")?-1:0;
                result.add(new Chord(root,direct.group(3)==null?"":direct.group(3),token,direct.group(2).equals("b") || flats));
                continue;
            }
            Matcher roman=Pattern.compile("^([#b]?)(VII|VI|IV|III|II|V|I|vii|vi|iv|iii|ii|v|i|[1-7])(maj7|m7|dim7|dim|aug|sus2|sus4|m|7|º)?$").matcher(token);
            if(!roman.matches()) throw new IllegalArgumentException("No reconozco «"+token+"». Usa I V vi IV, 1 5 6 4 o C G Am F. Se admiten m, 7, maj7, m7, dim, dim7, aug, sus2 y sus4.");
            String numeral=roman.group(2); int degree;
            if(Character.isDigit(numeral.charAt(0))) degree=Integer.parseInt(numeral)-1;
            else degree=Arrays.asList("I","II","III","IV","V","VI","VII").indexOf(numeral.toUpperCase(Locale.ROOT));
            String q=roman.group(3);
            if(q==null) q=Character.isDigit(numeral.charAt(0))?qualities[degree]:numeral.equals(numeral.toLowerCase(Locale.ROOT))?(qualities[degree].equals("dim")?"dim":"m"):"";
            else if(q.equals("º")) q="dim";
            else if(q.equals("7") && !Character.isDigit(numeral.charAt(0)) && numeral.equals(numeral.toLowerCase(Locale.ROOT))) q="m7";
            int accidental=roman.group(1).equals("#")?1:roman.group(1).equals("b")?-1:0;
            result.add(new Chord(key+scale[degree]+accidental,q,token,flats || accidental<0));
        }
        return result;
    }
    private static int[] movable(int root,String q) {
        int r=Math.floorMod(root-9,12);
        int e=Math.floorMod(root-4,12);
        // Prefer a lower E-shape position when it is closer to the nut.
        if(e<r && !q.equals("dim") && !q.equals("dim7") && !q.equals("aug")) {
            int[] shape;
            switch(q) {
                case "m": shape=new int[]{0,2,2,0,0,0};break;
                case "7": shape=new int[]{0,2,0,1,0,0};break;
                case "maj7": shape=new int[]{0,2,1,1,0,0};break;
                case "m7": shape=new int[]{0,2,0,0,0,0};break;
                case "sus2": shape=new int[]{0,2,4,4,0,0};break;
                case "sus4": shape=new int[]{0,2,2,2,0,0};break;
                default: shape=new int[]{0,2,2,1,0,0};
            }
            for(int i=0;i<6;i++) shape[i]+=e;
            return shape;
        }
        if(q.equals("dim7") && r==0) r=12;
        int[] offsets;
        switch(q) {
            case "m": offsets=new int[]{-1,0,2,2,1,0}; break;
            case "7": offsets=new int[]{-1,0,2,0,2,0}; break;
            case "maj7": offsets=new int[]{-1,0,2,1,2,0}; break;
            case "m7": offsets=new int[]{-1,0,2,0,1,0}; break;
            case "dim": offsets=new int[]{-1,0,1,2,1,-1}; break;
            case "dim7": offsets=new int[]{-1,0,1,-1,1,-1}; break;
            case "aug": offsets=new int[]{-1,0,3,2,2,-1}; break;
            case "sus2": offsets=new int[]{-1,0,2,2,0,0}; break;
            case "sus4": offsets=new int[]{-1,0,2,2,3,0}; break;
            default: offsets=new int[]{-1,0,2,2,2,0};
        }
        int[] frets=new int[6];
        for(int i=0;i<6;i++) frets[i]=(i==0 || (i==5 && (q.equals("dim") || q.equals("dim7") || q.equals("aug"))))?-1:r+offsets[i];
        return frets;
    }
}
