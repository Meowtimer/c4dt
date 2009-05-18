// $ANTLR 3.1.2 /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g 2009-05-18 00:02:45
package net.arctics.clonk.parser.map;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MapGeneratorLexer extends Lexer {
    public static final int WORD=5;
    public static final int WS=11;
    public static final int T__12=12;
    public static final int OVERLAY=6;
    public static final int T__14=14;
    public static final int LETTER=9;
    public static final int T__13=13;
    public static final int MAP=4;
    public static final int MATERIAL=8;
    public static final int INT=7;
    public static final int DIGIT=10;
    public static final int EOF=-1;

    // delegates
    // delegators

    public MapGeneratorLexer() {;} 
    public MapGeneratorLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MapGeneratorLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g"; }

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:5:7: ( '{' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:5:9: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:6:7: ( '}' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:6:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:7:7: ( ';' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:7:9: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "MAP"
    public final void mMAP() throws RecognitionException {
        try {
            int _type = MAP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:58:5: ( 'map' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:58:7: 'map'
            {
            match("map"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MAP"

    // $ANTLR start "OVERLAY"
    public final void mOVERLAY() throws RecognitionException {
        try {
            int _type = OVERLAY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:59:9: ( 'overlay' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:59:11: 'overlay'
            {
            match("overlay"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OVERLAY"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            int _type = LETTER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:60:8: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LETTER"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            int _type = DIGIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:61:7: ( '0' .. '9' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:61:9: '0' .. '9'
            {
            matchRange('0','9'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "WORD"
    public final void mWORD() throws RecognitionException {
        try {
            int _type = WORD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:62:6: ( LETTER ( LETTER | DIGIT )* )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:62:8: LETTER ( LETTER | DIGIT )*
            {
            mLETTER(); 
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:62:15: ( LETTER | DIGIT )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WORD"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:63:5: ( ( '+' | '-' )? ( DIGIT )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:63:7: ( '+' | '-' )? ( DIGIT )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:63:7: ( '+' | '-' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='+'||LA2_0=='-') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:63:18: ( DIGIT )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:63:18: DIGIT
            	    {
            	    mDIGIT(); 

            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "MATERIAL"
    public final void mMATERIAL() throws RecognitionException {
        try {
            int _type = MATERIAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:64:10: ( WORD '-' WORD )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:64:12: WORD '-' WORD
            {
            mWORD(); 
            match('-'); 
            mWORD(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MATERIAL"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:65:4: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:65:6: ( ' ' | '\\t' | '\\n' | '\\r' )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:65:6: ( ' ' | '\\t' | '\\n' | '\\r' )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( ((LA4_0>='\t' && LA4_0<='\n')||LA4_0=='\r'||LA4_0==' ') ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);

            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:8: ( T__12 | T__13 | T__14 | MAP | OVERLAY | LETTER | DIGIT | WORD | INT | MATERIAL | WS )
        int alt5=11;
        alt5 = dfa5.predict(input);
        switch (alt5) {
            case 1 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:10: T__12
                {
                mT__12(); 

                }
                break;
            case 2 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:16: T__13
                {
                mT__13(); 

                }
                break;
            case 3 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:22: T__14
                {
                mT__14(); 

                }
                break;
            case 4 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:28: MAP
                {
                mMAP(); 

                }
                break;
            case 5 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:32: OVERLAY
                {
                mOVERLAY(); 

                }
                break;
            case 6 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:40: LETTER
                {
                mLETTER(); 

                }
                break;
            case 7 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:47: DIGIT
                {
                mDIGIT(); 

                }
                break;
            case 8 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:53: WORD
                {
                mWORD(); 

                }
                break;
            case 9 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:58: INT
                {
                mINT(); 

                }
                break;
            case 10 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:62: MATERIAL
                {
                mMATERIAL(); 

                }
                break;
            case 11 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:71: WS
                {
                mWS(); 

                }
                break;

        }

    }


    protected DFA5 dfa5 = new DFA5(this);
    static final String DFA5_eotS =
        "\4\uffff\3\13\1\17\2\uffff\1\21\1\uffff\1\21\1\uffff\1\21\1\uffff"+
        "\1\23\1\uffff\1\21\1\uffff\3\21\1\30\1\uffff";
    static final String DFA5_eofS =
        "\31\uffff";
    static final String DFA5_minS =
        "\1\11\3\uffff\3\55\1\60\2\uffff\1\55\1\uffff\1\55\1\uffff\1\55\1"+
        "\uffff\1\55\1\uffff\1\55\1\uffff\4\55\1\uffff";
    static final String DFA5_maxS =
        "\1\175\3\uffff\3\172\1\71\2\uffff\1\172\1\uffff\1\172\1\uffff\1"+
        "\172\1\uffff\1\172\1\uffff\1\172\1\uffff\4\172\1\uffff";
    static final String DFA5_acceptS =
        "\1\uffff\1\1\1\2\1\3\4\uffff\1\11\1\13\1\uffff\1\6\1\uffff\1\12"+
        "\1\uffff\1\7\1\uffff\1\10\1\uffff\1\4\4\uffff\1\5";
    static final String DFA5_specialS =
        "\31\uffff}>";
    static final String[] DFA5_transitionS = {
            "\2\11\2\uffff\1\11\22\uffff\1\11\12\uffff\1\10\1\uffff\1\10"+
            "\2\uffff\12\7\1\uffff\1\3\5\uffff\32\6\4\uffff\1\6\1\uffff\14"+
            "\6\1\4\1\6\1\5\13\6\1\1\1\uffff\1\2",
            "",
            "",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\1\12"+
            "\31\14",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\25\14"+
            "\1\16\4\14",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\32\14",
            "\12\10",
            "",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\17\14"+
            "\1\20\12\14",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\32\14",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\4\14"+
            "\1\22\25\14",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\32\14",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\21\14"+
            "\1\24\10\14",
            "",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\13\14"+
            "\1\25\16\14",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\1\26"+
            "\31\14",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\30\14"+
            "\1\27\1\14",
            "\1\15\2\uffff\12\14\7\uffff\32\14\4\uffff\1\14\1\uffff\32\14",
            ""
    };

    static final short[] DFA5_eot = DFA.unpackEncodedString(DFA5_eotS);
    static final short[] DFA5_eof = DFA.unpackEncodedString(DFA5_eofS);
    static final char[] DFA5_min = DFA.unpackEncodedStringToUnsignedChars(DFA5_minS);
    static final char[] DFA5_max = DFA.unpackEncodedStringToUnsignedChars(DFA5_maxS);
    static final short[] DFA5_accept = DFA.unpackEncodedString(DFA5_acceptS);
    static final short[] DFA5_special = DFA.unpackEncodedString(DFA5_specialS);
    static final short[][] DFA5_transition;

    static {
        int numStates = DFA5_transitionS.length;
        DFA5_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA5_transition[i] = DFA.unpackEncodedString(DFA5_transitionS[i]);
        }
    }

    class DFA5 extends DFA {

        public DFA5(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 5;
            this.eot = DFA5_eot;
            this.eof = DFA5_eof;
            this.min = DFA5_min;
            this.max = DFA5_max;
            this.accept = DFA5_accept;
            this.special = DFA5_special;
            this.transition = DFA5_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__12 | T__13 | T__14 | MAP | OVERLAY | LETTER | DIGIT | WORD | INT | MATERIAL | WS );";
        }
    }
 

}