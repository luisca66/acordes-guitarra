package com.luis.acordes;

import java.util.*;
import java.util.regex.*;

/** Parses chord markup without guessing lyric/chord alignment. */
public final class SongSheet {
    public static final class Piece {
        public final String text;public final boolean chord;
        Piece(String text,boolean chord){this.text=text;this.chord=chord;}
    }
    public static final class Line {
        public final List<Piece> pieces;public final boolean heading;
        Line(List<Piece> pieces,boolean heading){this.pieces=pieces;this.heading=heading;}
        public String plain(){StringBuilder b=new StringBuilder();for(Piece p:pieces)b.append(p.text);return b.toString();}
    }
    public static List<Line> parse(String raw) {
        String clean=raw.replace("\r\n","\n").replace('\r','\n').replace("[tab]","").replace("[/tab]","");
        List<Line> result=new ArrayList<>();
        for(String line:clean.split("\n",-1)) {
            List<Piece> pieces=new ArrayList<>();String trim=line.trim();boolean heading=trim.matches("\\[[^\\]]+\\]") && !trim.startsWith("[ch]");
            Matcher tags=Pattern.compile("\\[ch\\](.*?)\\[/ch\\]").matcher(line);int last=0;
            while(tags.find()) {if(tags.start()>last)pieces.add(new Piece(line.substring(last,tags.start()),false));pieces.add(new Piece(tags.group(1),true));last=tags.end();}
            if(last==0 && !heading && chordLine(line)) {
                Matcher tokens=Pattern.compile("\\S+").matcher(line);while(tokens.find()) {if(tokens.start()>last)pieces.add(new Piece(line.substring(last,tokens.start()),false));pieces.add(new Piece(tokens.group(),true));last=tokens.end();}
            }
            if(last<line.length())pieces.add(new Piece(line.substring(last),false));
            if(heading)pieces=Collections.singletonList(new Piece(trim.substring(1,trim.length()-1),false));
            result.add(new Line(pieces,heading));
        }
        return result;
    }
    private static boolean chordLine(String line) {
        if(line.trim().isEmpty())return false;
        for(String token:line.trim().split("\\s+")) {try {Chords.one(token);}catch(IllegalArgumentException e){return false;}}
        return true;
    }
    public static List<String> uniqueChords(List<Line> lines) {
        LinkedHashSet<String> names=new LinkedHashSet<>(positions(lines).keySet());for(Line l:lines)for(Piece p:l.pieces)if(p.chord)names.add(p.text);return new ArrayList<>(names);
    }
    /** Source fret strings are ordered low E to high e, exactly like our diagrams. */
    public static Map<String,Music.Chord> positions(List<Line> lines) {
        Map<String,Music.Chord> result=new LinkedHashMap<>();
        for(Line line:lines) {
            Matcher match=Pattern.compile("^\\s*([A-Ga-g][#b]?[^\\s]*)\\s+([xX0-9]{6})\\s*$").matcher(line.plain());
            if(!match.matches())continue;
            String name=match.group(1),shape=match.group(2);int[] frets=new int[6];boolean sounding=false;
            for(int i=0;i<6;i++){char c=shape.charAt(i);frets[i]=c=='x'||c=='X'?-1:c-'0';sounding|=frets[i]>=0;}
            if(!sounding)continue;
            // The source's transcription defines this shape; don't replace it with a generic voicing.
            int root=new int[]{9,11,0,2,4,5,7}[Character.toUpperCase(name.charAt(0))-'A'];
            int prefix=1;if(name.length()>1 && (name.charAt(1)=='#'||name.charAt(1)=='b')){root+=name.charAt(1)=='#'?1:-1;prefix++;}
            result.putIfAbsent(name,new Music.Chord(Math.floorMod(root,12),name.substring(prefix),name,frets));
        }
        return result;
    }
    public static boolean isPositionLine(Line line) {
        return !positions(Collections.singletonList(line)).isEmpty();
    }
}
