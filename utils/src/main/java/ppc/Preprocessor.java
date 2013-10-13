package ppc;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class Preprocessor {

    static class CommentsCase {
        final String ext;
        final String prefix;
        final String suffix;

        CommentsCase(String ext, String prefix, String suffix) {
            this.ext = ext;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        CommentsCase(String ext, String prefix) {
            this.ext = ext;
            this.prefix = prefix;
            this.suffix = "";
        }
    }

    static CommentsCase[] CCASES = new CommentsCase[]{
            new CommentsCase("*", "#"),
            new CommentsCase("java", "/*", "*/"),
            new CommentsCase("java", "//"),
            new CommentsCase("xml", "<!--", "-->")
    };

    static String[] OPS = new String[]{
            "#\\s*(?<op>if|elif|ifdef|ifndef)\\s+(?<expr>.*?)",
            "#\\s*(?<op>else|endif)",
            "#\\s*(?<op>define)\\s+(?<var>[^\\s]*?)(\\s+(?<val>.+?))?",
            "#\\s*(?<op>undef)\\s+(?<var>[^\\s]*?)"
    };


    final CommentsCase[] activeCcases;

    final Pattern[] linePatterns;

    public Preprocessor(String ext, Reader reader, Writer writer) {
        if (ext == null) {
            ext = "*";
        }
        while (ext.charAt(0) == '.') {
            ext = ext.substring(1);
        }
        List<CommentsCase> selected = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            for (CommentsCase cc : CCASES) {
                if (ext.equals(cc.ext)) {
                    selected.add(cc);
                }
            }
            if (selected.isEmpty()) {
                ext = "*";
            } else {
                break;
            }
        }
        activeCcases = selected.toArray(new CommentsCase[selected.size()]);
        linePatterns = null; //todo
    }

    public void preprocess() {


    }


    public static void main(String[] args) {
        //System.out.println(OPS[0].matcher("#if ").matches());
        StringWriter sw = new StringWriter();
        StringReader sr = new StringReader(" //#if AAAA \n" +
                                           "test\n" +
                                           "#endif\n");
        Preprocessor p = new Preprocessor(".xml", sr, sw);
        p.preprocess();
        System.out.println("out=" + sw);

    }

}
