import com.luis.acordes.*;
import java.util.*;
/** Generates the web catalog from the same voicings as the Android app. */
public class ExportChords {
 public static void main(String[] args) {
  String[] roots={"C","C#","Db","D","D#","Eb","E","F","F#","Gb","G","G#","Ab","A","A#","Bb","B"};
  Map<String,int[]> shapes=new TreeMap<>();
  for(String r:roots) for(String q:Chords.qualities()) shapes.put(r+q,Chords.one(r+q).frets);
  for(String r:roots) for(String b:roots) shapes.put(r+"/"+b,Chords.one(r+"/"+b).frets);
  shapes.put("G/F#",Chords.one("G/F#").frets);
  StringJoiner entries=new StringJoiner(",\n","{\n","\n}");
  shapes.forEach((name,frets)->entries.add("\""+name+"\":"+Arrays.toString(frets)));
  System.out.print(entries);
 }
}
