// $ANTLR 3.1.2 /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g 2009-05-17 20:36:50
package net.arctics.clonk.parser.map;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MapGeneratorLexer extends Lexer {
    public static final int WORD=4;
    public static final int WS=9;
    public static final int ATTRVALUE=5;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int LETTER=6;
    public static final int T__13=13;
    public static final int T__10=10;
    public static final int INT=8;
    public static final int DIGIT=7;
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

    // $ANTLR start "T__10"
    public final void mT__10() throws RecognitionException {
        try {
            int _type = T__10;
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
    // $ANTLR end "T__10"

    // $ANTLR start "T__11"
    public final void mT__11() throws RecognitionException {
        try {
            int _type = T__11;
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
    // $ANTLR end "T__11"

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
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
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:8:7: ( '=' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:8:9: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            int _type = LETTER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:44:9: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
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
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:45:8: ( '0' .. '9' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:45:10: '0' .. '9'
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
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:46:7: ( LETTER ( LETTER | DIGIT )* )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:46:9: LETTER ( LETTER | DIGIT )*
            {
            mLETTER(); 
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:46:16: ( LETTER | DIGIT )*
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
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:47:6: ( ( '+' | '-' )? ( DIGIT )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:47:8: ( '+' | '-' )? ( DIGIT )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:47:8: ( '+' | '-' )?
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

            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:47:19: ( DIGIT )+
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
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:47:19: DIGIT
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

    // $ANTLR start "ATTRVALUE"
    public final void mATTRVALUE() throws RecognitionException {
        try {
            int _type = ATTRVALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:11: ( INT | WORD | ( WORD '-' WORD ) )
            int alt4=3;
            alt4 = dfa4.predict(input);
            switch (alt4) {
                case 1 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:13: INT
                    {
                    mINT(); 

                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:17: WORD
                    {
                    mWORD(); 

                    }
                    break;
                case 3 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:22: ( WORD '-' WORD )
                    {
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:22: ( WORD '-' WORD )
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:23: WORD '-' WORD
                    {
                    mWORD(); 
                    match('-'); 
                    mWORD(); 

                    }


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ATTRVALUE"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:49:5: ( ( ' ' | '\\t' )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:49:7: ( ' ' | '\\t' )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:49:7: ( ' ' | '\\t' )+
            int cnt5=0;
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0=='\t'||LA5_0==' ') ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:
            	    {
            	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt5 >= 1 ) break loop5;
                        EarlyExitException eee =
                            new EarlyExitException(5, input);
                        throw eee;
                }
                cnt5++;
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
        // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:8: ( T__10 | T__11 | T__12 | T__13 | LETTER | DIGIT | WORD | INT | ATTRVALUE | WS )
        int alt6=10;
        alt6 = dfa6.predict(input);
        switch (alt6) {
            case 1 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:10: T__10
                {
                mT__10(); 

                }
                break;
            case 2 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:16: T__11
                {
                mT__11(); 

                }
                break;
            case 3 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:22: T__12
                {
                mT__12(); 

                }
                break;
            case 4 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:28: T__13
                {
                mT__13(); 

                }
                break;
            case 5 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:34: LETTER
                {
                mLETTER(); 

                }
                break;
            case 6 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:41: DIGIT
                {
                mDIGIT(); 

                }
                break;
            case 7 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:47: WORD
                {
                mWORD(); 

                }
                break;
            case 8 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:52: INT
                {
                mINT(); 

                }
                break;
            case 9 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:56: ATTRVALUE
                {
                mATTRVALUE(); 

                }
                break;
            case 10 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:1:66: WS
                {
                mWS(); 

                }
                break;

        }

    }


    protected DFA4 dfa4 = new DFA4(this);
    protected DFA6 dfa6 = new DFA6(this);
    static final String DFA4_eotS =
        "\2\uffff\1\3\1\uffff\1\3\1\uffff";
    static final String DFA4_eofS =
        "\6\uffff";
    static final String DFA4_minS =
        "\1\53\1\uffff\1\55\1\uffff\1\55\1\uffff";
    static final String DFA4_maxS =
        "\1\172\1\uffff\1\172\1\uffff\1\172\1\uffff";
    static final String DFA4_acceptS =
        "\1\uffff\1\1\1\uffff\1\2\1\uffff\1\3";
    static final String DFA4_specialS =
        "\6\uffff}>";
    static final String[] DFA4_transitionS = {
            "\1\1\1\uffff\1\1\2\uffff\12\1\7\uffff\32\2\4\uffff\1\2\1\uffff"+
            "\32\2",
            "",
            "\1\5\2\uffff\12\4\7\uffff\32\4\4\uffff\1\4\1\uffff\32\4",
            "",
            "\1\5\2\uffff\12\4\7\uffff\32\4\4\uffff\1\4\1\uffff\32\4",
            ""
    };

    static final short[] DFA4_eot = DFA.unpackEncodedString(DFA4_eotS);
    static final short[] DFA4_eof = DFA.unpackEncodedString(DFA4_eofS);
    static final char[] DFA4_min = DFA.unpackEncodedStringToUnsignedChars(DFA4_minS);
    static final char[] DFA4_max = DFA.unpackEncodedStringToUnsignedChars(DFA4_maxS);
    static final short[] DFA4_accept = DFA.unpackEncodedString(DFA4_acceptS);
    static final short[] DFA4_special = DFA.unpackEncodedString(DFA4_specialS);
    static final short[][] DFA4_transition;

    static {
        int numStates = DFA4_transitionS.length;
        DFA4_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA4_transition[i] = DFA.unpackEncodedString(DFA4_transitionS[i]);
        }
    }

    class DFA4 extends DFA {

        public DFA4(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 4;
            this.eot = DFA4_eot;
            this.eof = DFA4_eof;
            this.min = DFA4_min;
            this.max = DFA4_max;
            this.accept = DFA4_accept;
            this.special = DFA4_special;
            this.transition = DFA4_transition;
        }
        public String getDescription() {
            return "48:1: ATTRVALUE : ( INT | WORD | ( WORD '-' WORD ) );";
        }
    }
    static final String DFA6_eotS =
        "\5\uffff\1\11\1\14\3\uffff\1\16\2\uffff\1\17\2\uffff";
    static final String DFA6_eofS =
        "\20\uffff";
    static final String DFA6_minS =
        "\1\11\4\uffff\1\55\2\60\2\uffff\1\55\2\uffff\1\60\2\uffff";
    static final String DFA6_maxS =
        "\1\175\4\uffff\1\172\2\71\2\uffff\1\172\2\uffff\1\71\2\uffff";
    static final String DFA6_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\3\uffff\1\12\1\5\1\uffff\1\11\1\6\1\uffff"+
        "\1\7\1\10";
    static final String DFA6_specialS =
        "\20\uffff}>";
    static final String[] DFA6_transitionS = {
            "\1\10\26\uffff\1\10\12\uffff\1\7\1\uffff\1\7\2\uffff\12\6\1"+
            "\uffff\1\3\1\uffff\1\4\3\uffff\32\5\4\uffff\1\5\1\uffff\32\5"+
            "\1\1\1\uffff\1\2",
            "",
            "",
            "",
            "",
            "\1\13\2\uffff\12\12\7\uffff\32\12\4\uffff\1\12\1\uffff\32\12",
            "\12\15",
            "\12\15",
            "",
            "",
            "\1\13\2\uffff\12\12\7\uffff\32\12\4\uffff\1\12\1\uffff\32\12",
            "",
            "",
            "\12\15",
            "",
            ""
    };

    static final short[] DFA6_eot = DFA.unpackEncodedString(DFA6_eotS);
    static final short[] DFA6_eof = DFA.unpackEncodedString(DFA6_eofS);
    static final char[] DFA6_min = DFA.unpackEncodedStringToUnsignedChars(DFA6_minS);
    static final char[] DFA6_max = DFA.unpackEncodedStringToUnsignedChars(DFA6_maxS);
    static final short[] DFA6_accept = DFA.unpackEncodedString(DFA6_acceptS);
    static final short[] DFA6_special = DFA.unpackEncodedString(DFA6_specialS);
    static final short[][] DFA6_transition;

    static {
        int numStates = DFA6_transitionS.length;
        DFA6_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA6_transition[i] = DFA.unpackEncodedString(DFA6_transitionS[i]);
        }
    }

    class DFA6 extends DFA {

        public DFA6(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 6;
            this.eot = DFA6_eot;
            this.eof = DFA6_eof;
            this.min = DFA6_min;
            this.max = DFA6_max;
            this.accept = DFA6_accept;
            this.special = DFA6_special;
            this.transition = DFA6_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__10 | T__11 | T__12 | T__13 | LETTER | DIGIT | WORD | INT | ATTRVALUE | WS );";
        }
    }
 

}