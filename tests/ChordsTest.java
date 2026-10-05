import com.luis.acordes.*;
import java.util.*;

public final class ChordsTest {
    static int checks=0;
    static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    static void validate(Music.Chord chord,Integer bass){int[] tuning={40,45,50,55,59,64};Set<Integer> notes=new HashSet<>(),expected=new HashSet<>();int low=999;
        for(int interval:Chords.intervals(chord.quality))expected.add((chord.root+interval)%12);if(bass!=null)expected.add(bass);
        for(int i=0;i<6;i++)if(chord.frets[i]>=0){int note=tuning[i]+chord.frets[i];notes.add(note%12);low=Math.min(low,note);}
        Set<Integer> required=new HashSet<>(expected);if(Arrays.asList("7","maj7","m7").contains(chord.quality))required.remove((chord.root+7)%12);
        check(expected.containsAll(notes)&&notes.containsAll(required),chord.name+" invalid notes "+notes+" vs "+expected);
        if(bass!=null)check(low%12==bass,chord.name+" wrong bass "+low%12);
    }
    public static void main(String[] args){String[] roots={"C","C#","D","Eb","E","F","F#","G","Ab","A","Bb","B"};
        for(String root:roots)for(String quality:Chords.qualities())validate(Chords.one(root+quality),null);
        for(int root=0;root<12;root++)for(int bass=0;bass<12;bass++)validate(Chords.one(roots[root]+"/"+roots[bass]),bass);
        check(Chords.parse("C G Am F C G").size()==4,"unique names");
        check(Chords.parse("Do Sol Lam Fa").stream().map(c->c.name).toList().equals(Arrays.asList("C","G","Am","F")),"Spanish names");
        check(Chords.one("Bbmaj7").name.equals("Bbmaj7"),"flat spelling");
        check(Chords.one("C#").name.equals("C#"),"sharp spelling");
        for(String bad:new String[]{"I V vi IV","1 5 6 4","","---","H","C/banana","Cfoobar"}){boolean rejected=false;try{Chords.parse(bad);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Should reject "+bad);}
        java.util.List<SongSheet.Line> lines=SongSheet.parse("[Intro]\n[tab]  [ch]D[/ch]   [ch]Am[/ch]\n  original lyric spacing\n[/tab]\n[Verse 1]\nG     Bm\nA lyric, not a chord line\n");
        check(lines.get(0).heading,"section heading");check(lines.get(1).plain().equals("  D   Am"),"preserved alignment");check(lines.get(2).plain().equals("  original lyric spacing"),"preserved lyrics");
        check(SongSheet.uniqueChords(lines).equals(Arrays.asList("D","Am","G","Bm")),"tagged and plain chord extraction");
        check(lines.get(6).pieces.stream().noneMatch(p->p.chord),"lyrics must not become chords");
        java.util.List<SongSheet.Line> sourceLines=SongSheet.parse("[ch]G[/ch] 320033\nD xx0323\nEm 022000\nC 332010\nCadd9 x32030\nB7 x21202\n\n[Intro]\n[ch]G[/ch] [ch]D[/ch]\nOriginal sample words");
        Map<String,Music.Chord> positions=SongSheet.positions(sourceLines);
        check(positions.size()==6,"all six source fret definitions");
        check(Arrays.equals(positions.get("G").frets,new int[]{3,2,0,0,3,3}),"source G shape must override generic G");
        check(Arrays.equals(positions.get("D").frets,new int[]{-1,-1,0,3,2,3}),"source D shape preserved exactly");
        check(Arrays.equals(positions.get("Cadd9").frets,new int[]{-1,3,2,0,3,0}),"source Cadd9 shape preserved exactly");
        check(SongSheet.isPositionLine(sourceLines.get(0)),"tagged legend recognized");
        check(!SongSheet.isPositionLine(sourceLines.get(9)),"lyric line retained");
        check(SongSheet.uniqueChords(sourceLines).size()==6,"legend chord names included without duplicates");
        check(SongSheet.positions(SongSheet.parse("G 32003\nG xxxxxx\nA banana\nA 1234567")).isEmpty(),"invalid shapes must stay text");
        System.out.println("OK: "+checks+" checks for 252 chord types/roots, 144 slash chords, named input and lyric alignment.");
    }
}
