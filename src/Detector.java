
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class Detector {
    static Collection<Character> punctuationMarks= Arrays.asList('.', '?','!',':'); // BURAYA : EKLEDIM.
    static ArrayList<String> abbreviations = new ArrayList<String>(Arrays.asList("mr", "av","dr","mrs","dk","sn","apt","bk","doç","hz",
            "ing","korg","mah","min","max","müh","no","nu","ör","sok","tc","yy","yrd","yd","sb","vb","tel","tic","prof","ltd","gön","doð","çvþ",
            "ii","iii","iv","vi","vii","viii","ix",
            "A","B","C","Ç","D","E","F","G","H","I","Ý","J","K","L","M","N","O","Ö","P","R","S","Þ","T","U","Ü","V","Y","Z","W","Q","X","s"));

    // letters are added to for the name abbreviations.
    static HashMap<String, Boolean> ruleList = new HashMap<String, Boolean>();
    public static final Charset charset = Charset.forName("UTF-8");

    public List<String> detect(String inputPath, String outputPath) throws IOException {
        List<Integer> sentencePositions = new ArrayList();
        initRuleList();
        File file = new File(inputPath);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.read();
        raf.read(); // ignore first two characters.
        int r;
        while ((r = raf.read()) != -1) {
            char ch = (char) r;
            String trioFormat="";
            if(punctuationMarks.contains(ch)){
                int puncPos = (int) raf.getFilePointer();
                //Get the previous letter of punctuation mark
                String previousWordReverse = ""; // the word before punctuation mark in reverse order.
                raf.seek(puncPos-2);
                int backtraceWordPos = (int) raf.getFilePointer();
                char lastCharOfprevious = (char)  raf.read();
                previousWordReverse += lastCharOfprevious;

                int tempCount=1;
                while(lastCharOfprevious == ' '){ // 3'lÃ¼ kÄ±saltmalar iÃ§in noktayÄ± da ekledim.
                    raf.seek(puncPos-2-tempCount); // go backward till find a character
                    tempCount++;
                    backtraceWordPos = (int) raf.getFilePointer(); // initially it is the position of the last character of the previous word.
                    lastCharOfprevious = (char)  raf.read();
                    previousWordReverse += lastCharOfprevious;
                }
                raf.seek(backtraceWordPos-1);
                char previous = (char)  raf.read();
                previousWordReverse += previous;
                backtraceWordPos--;

                while (previous !=' ' && previous != '.' && backtraceWordPos !=0){ // trying to find the empty character before the word so that i get the world and check the first character of it.
                    // 3'lÃ¼ kÄ±saltmalar iÃ§in noktayÄ± da ekledim.
                    raf.seek(backtraceWordPos-1);
                    previous = (char)  raf.read();
                    previousWordReverse += previous;
                    backtraceWordPos--;
                }
                if(backtraceWordPos!=0)
                    previous = (char)  raf.read(); // the character after empty character.
                String previousWord = new StringBuffer(previousWordReverse).reverse().toString().substring(1);
                //String previousWord = new StringBuffer(previousWordReverse).reverse().toString().trim(); // This is the word before punctuation mark.
                boolean isAbbreviation;
                if(previousWord.length()==1)
                    isAbbreviation = abbreviations.contains(previousWord); // i need to check if the previous word is in the abbreviation list or not.
                else
                    isAbbreviation = abbreviations.contains(previousWord.toLowerCase()); // i need to check if the previous word is in the abbreviation list or not.
                ///p               System.out.println(previousWord + " " +isAbbreviation); /// printline

                if(isUpperCase(previous)){
                    trioFormat+='U';
                }
                else if(isLowerCase(previous)){
                    trioFormat+='L';
                }
                else if(isDigit(previous)){
                    trioFormat+='#';
                }

                ///p                System.out.print(previous); /// printline


                //-----------
                raf.seek(puncPos-1);
                char current = (char)  raf.read();
                trioFormat+='.';
                ///p             System.out.print(current); /// printline

                //Get the next letter of punctuation mark
                char next = (char)  raf.read();
                if(next=='.'){ // two consecutive dots. "..." is coming.
                    ///p                    System.out.print(next); /// printline
                    next = (char)  raf.read(); // it is supposed to be another dot(.)
                    puncPos = (int) raf.getFilePointer();
                    ///p                    System.out.print(next); /// printline
                    next = (char)  raf.read(); // the character after "..."
                }
                while(next==' '){ // go till find a character but empty character.
                    next = (char)  raf.read();
                }
                if(isUpperCase(next)){
                    trioFormat+='U';
                }
                else if(isLowerCase(next)){
                    trioFormat+='L';
                }
                else if(isDigit(next)){
                    trioFormat+='#';
                }
                else if(next == '-' || next == ',' || next == '(' || next == ')' || next == '/' || next == '\'' || next == '"'){
                    trioFormat+=next;
                }
                ///p System.out.println(next); /// printline

                raf.seek(puncPos);
                ///p System.out.println(trioFormat); /// printline

                boolean isSentence = ruleList.get(trioFormat);
                ///p System.out.println("Is sentence? " + (isSentence && !isAbbreviation) ); /// printline
                if(isSentence&!isAbbreviation)
                    sentencePositions.add(puncPos); // there is end-of-sentence at this index of the file.
                ///p System.out.println("---"); /// printline
                trioFormat="";
            }

        }
        ///p System.out.println(sentencePositions); /// printline
        Integer startPos= 0;
        Integer endPos= 0;
        List<String> sentences = new ArrayList<String>(); // this list goes to design panel.
        for (Integer pos : sentencePositions){
            raf.seek(startPos);
            endPos=pos;
            byte[] arr = new byte[(int) endPos-startPos];  // create an array equal to the length of sentence
            raf.readFully(arr);// read the file
            String sentence = new String(arr).trim();    // create a new string based on arr
            if(sentence.charAt(0)==' ') // to get rid of the empty char at the beginning of the sentence.
                sentence = sentence.substring(1);
            // Write to the file.
            String str = new String(sentence.getBytes("UTF-8"),charset);

            //BURASI SUMMARIZATION SYSTEM'DA AÃIK KALACAK**************************
            FileOutputStream fo = new FileOutputStream(outputPath,true);
            fo.write(str.getBytes(charset));
            fo.write('\n');
            fo.close();
            sentences.add(str);
            ///p System.out.println(str); /// printline
            startPos=pos;
        }
        raf.close();
        return sentences;
    }

    public static void initRuleList(){
        ruleList.put("L.U",true);
        ruleList.put("L.#",true);
        ruleList.put("L.'",true); // we added this.
        ruleList.put("U.'",true); // we added this.
        ruleList.put("L.\"",true); // we added this.
        ruleList.put("U.\"",true); // we added this.
        ruleList.put("L.(",true); // we added this.
        ruleList.put("U.(",true); // we added this.
        ruleList.put("L.)",true); // we added this.
        ruleList.put("U.)",true); // we added this.
        ruleList.put("L.-",true); // we added this.
        ruleList.put("U.-",true); // we added this.
        ruleList.put("L./",true); // we added this.
        ruleList.put("U./",true); // we added this.
        ruleList.put("U.U",true); // we added this. Abbreviation listesinden sonra tekrar doÄru yaptÄ±m bunu.

        ruleList.put("U.",true);
        ruleList.put("L.",true);
        ruleList.put("U.#",false); // yeni ekleme
        ruleList.put("U.,",false); // yeni ekleme
        ruleList.put("L.,",false); // yeni ekleme
        ruleList.put("#.,",false); // yeni ekleme
        ruleList.put("U.L",false);
        ruleList.put("L.L",false);
        ruleList.put(".,",false);
        ruleList.put("#.L",false);
        ruleList.put("#.'",false);
        ruleList.put("#.\"",false);
        ruleList.put("#.)",false);
        ruleList.put("#.(",false);
        ruleList.put("#.-",false);
        ruleList.put("#.U",false);
        ruleList.put("#.#",false);

    }

    static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
    static boolean isLowerCase(char ch) {
        return (ch >= 'a' && ch <= 'z') || ch == 'ý' || ch == 'þ' || ch == 'ð' || ch == 'ü' || ch == 'ç' || ch == 'ö'|| ch==254 || ch==253 || ch==240; // þ,i,ð sýrasýyla.
    }

    static boolean isUpperCase(char ch) {
        return (ch >= 'A' && ch <= 'Z') || ch == 'Ý' || ch == 'Þ' || ch == 'Ð' || ch == 'Ü' || ch=='Ç' || ch=='Ö' || ch==222 || ch==221 || ch==208;  //Ü+, Ç+, Ö+ Diðerleri için HTML code kullanýnca oldu. Þ,Ý,Ð
    }
}