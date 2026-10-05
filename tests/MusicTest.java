import com.luis.acordes.Music;
import java.util.*;

public final class MusicTest {
    private static int checks=0;
    private static void check(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
    private static void names(int key,boolean minor,String input,String expected) {
        StringJoiner names=new StringJoiner(" ");for(Music.Chord c:Music.parse(key,minor,input)) names.add(c.name);
        check(names.toString().equals(expected),input+" => "+names+" (expected "+expected+")");
    }
    public static void main(String[] args) {
        names(0,false,"I V vi IV","C G Am F");names(2,false,"1 5 6 4","D A Bm G");
        names(9,true,"1 4 5 6 7","Am Dm Em F G");names(9,true,"i iv V7","Am Dm E7");
        names(0,false,"ii7 V7 Imaj7","Dm7 G7 Cmaj7");names(0,false,"viiº","Bdim");
        names(0,false,"bVII IV I","Bb F C");names(0,false,"C–G–Am–F","C G Am F");
        names(0,false,"Bb F# C#m7 Dsus4","Bb F# C#m7 Dsus4");
        String[] qualities={"","m","7","maj7","m7","dim","dim7","aug","sus2","sus4"};
        int[][] intervals={{0,4,7},{0,3,7},{0,4,7,10},{0,4,7,11},{0,3,7,10},{0,3,6},{0,3,6,9},{0,4,8},{0,2,7},{0,5,7}};
        int[] tuning={4,9,2,7,11,4};
        for(int root=0;root<12;root++) for(int q=0;q<qualities.length;q++) {
            Music.Chord c=new Music.Chord(root,qualities[q],"",false);Set<Integer> notes=new HashSet<>();
            for(int i=0;i<6;i++) {int fret=c.frets[i];check(fret>=-1 && fret<=24,c.name+" invalid fret "+fret);if(fret>=0)notes.add(Math.floorMod(tuning[i]+fret-root,12));}
            Set<Integer> expected=new HashSet<>();for(int n:intervals[q])expected.add(n);
            // An open C7 conventionally omits the fifth; root, third and seventh define it.
            Set<Integer> required=new HashSet<>(expected);
            if(qualities[q].equals("7") || qualities[q].equals("maj7") || qualities[q].equals("m7")) required.remove(7);
            check(expected.containsAll(notes) && notes.containsAll(required),c.name+" wrong notes "+notes+" vs "+expected);
        }
        for(String bad:new String[]{"","---","H","VIII","C/foo","I banana IV","0","8","C##"}) {
            boolean rejected=false;try {Music.parse(0,false,bad);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Accepted invalid input: "+bad);
        }
        check(Music.parse(0,false,"I ".repeat(24)).size()==24,"24 chord boundary");
        boolean rejected=false;try {Music.parse(0,false,"I ".repeat(25));}catch(IllegalArgumentException e){rejected=true;}check(rejected,"25 chord limit");
        System.out.println("OK: "+checks+" checks, all 120 chord shapes have valid notes and all defining tones.");
    }
}
