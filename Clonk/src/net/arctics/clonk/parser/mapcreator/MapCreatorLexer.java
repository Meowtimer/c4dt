// $ANTLR 3.1.2 /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g 2009-07-23 17:28:17
package net.arctics.clonk.parser.mapcreator;

import org.antlr.runtime.*;

public class MapCreatorLexer extends Lexer {
    public static final int MAP=6;
    public static final int BLOCKCLOSE=10;
    public static final int LETTER=14;
    public static final int BLOCKOPEN=9;
    public static final int NUMBER=12;
    public static final int WS=18;
    public static final int OVERLAY=8;
    public static final int MLCOMMENT=20;
    public static final int SLCOMMENT=19;
    public static final int WORD=17;
    public static final int STATEMENTEND=4;
    public static final int INT=16;
    public static final int EOF=-1;
    public static final int ASSIGN=11;
    public static final int MATCOMBO=13;
    public static final int DIGIT=15;
    public static final int OPERATOR=5;
    public static final int NAME=7;

    // delegates
    // delegators

    public MapCreatorLexer() {;} 
    public MapCreatorLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MapCreatorLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g"; }

    // $ANTLR start "MAP"
    public final void mMAP() throws RecognitionException {
        try {
            int _type = MAP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:193:6: ( 'map' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:193:8: 'map'
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
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:194:10: ( 'overlay' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:194:12: 'overlay'
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
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:196:17: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "LETTER"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:16: ( '0' .. '9' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:18: '0' .. '9'
            {
            matchRange('0','9'); 

            }

        }
        finally {
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:198:15: ( ( '+' | '-' )? ( DIGIT )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:198:17: ( '+' | '-' )? ( DIGIT )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:198:17: ( '+' | '-' )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='+'||LA1_0=='-') ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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

            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:198:28: ( DIGIT )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( ((LA2_0>='0' && LA2_0<='9')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:198:28: DIGIT
            	    {
            	    mDIGIT(); 

            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "WORD"
    public final void mWORD() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:199:15: ( LETTER ( LETTER | DIGIT )* )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:199:17: LETTER ( LETTER | DIGIT )*
            {
            mLETTER(); 
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:199:24: ( LETTER | DIGIT )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='0' && LA3_0<='9')||(LA3_0>='A' && LA3_0<='Z')||LA3_0=='_'||(LA3_0>='a' && LA3_0<='z')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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
            	    break loop3;
                }
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "WORD"

    // $ANTLR start "NUMBER"
    public final void mNUMBER() throws RecognitionException {
        try {
            int _type = NUMBER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:201:9: ( INT )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:201:11: INT
            {
            mINT(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NUMBER"

    // $ANTLR start "NAME"
    public final void mNAME() throws RecognitionException {
        try {
            int _type = NAME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:202:7: ( WORD )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:202:9: WORD
            {
            mWORD(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NAME"

    // $ANTLR start "MATCOMBO"
    public final void mMATCOMBO() throws RecognitionException {
        try {
            int _type = MATCOMBO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:10: ( WORD '-' WORD )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:12: WORD '-' WORD
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
    // $ANTLR end "MATCOMBO"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:5: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
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
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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

    // $ANTLR start "SLCOMMENT"
    public final void mSLCOMMENT() throws RecognitionException {
        try {
            int _type = SLCOMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:11: ( '//' ( . )* '\\n' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:13: '//' ( . )* '\\n'
            {
            match("//"); 

            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:18: ( . )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0=='\n') ) {
                    alt5=2;
                }
                else if ( ((LA5_0>='\u0000' && LA5_0<='\t')||(LA5_0>='\u000B' && LA5_0<='\uFFFF')) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:18: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);

            match('\n'); 
            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SLCOMMENT"

    // $ANTLR start "MLCOMMENT"
    public final void mMLCOMMENT() throws RecognitionException {
        try {
            int _type = MLCOMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:11: ( '/*' ( . )* '*/' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:13: '/*' ( . )* '*/'
            {
            match("/*"); 

            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:18: ( . )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0=='*') ) {
                    int LA6_1 = input.LA(2);

                    if ( (LA6_1=='/') ) {
                        alt6=2;
                    }
                    else if ( ((LA6_1>='\u0000' && LA6_1<='.')||(LA6_1>='0' && LA6_1<='\uFFFF')) ) {
                        alt6=1;
                    }


                }
                else if ( ((LA6_0>='\u0000' && LA6_0<=')')||(LA6_0>='+' && LA6_0<='\uFFFF')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:18: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match("*/"); 

            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MLCOMMENT"

    // $ANTLR start "ASSIGN"
    public final void mASSIGN() throws RecognitionException {
        try {
            int _type = ASSIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:207:9: ( '=' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:207:11: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ASSIGN"

    // $ANTLR start "BLOCKOPEN"
    public final void mBLOCKOPEN() throws RecognitionException {
        try {
            int _type = BLOCKOPEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:11: ( '{' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:13: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLOCKOPEN"

    // $ANTLR start "BLOCKCLOSE"
    public final void mBLOCKCLOSE() throws RecognitionException {
        try {
            int _type = BLOCKCLOSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:209:12: ( '}' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:209:14: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLOCKCLOSE"

    // $ANTLR start "STATEMENTEND"
    public final void mSTATEMENTEND() throws RecognitionException {
        try {
            int _type = STATEMENTEND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:210:14: ( ';' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:210:16: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STATEMENTEND"

    // $ANTLR start "OPERATOR"
    public final void mOPERATOR() throws RecognitionException {
        try {
            int _type = OPERATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:11: ( '|' | '&' | '^' )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
            {
            if ( input.LA(1)=='&'||input.LA(1)=='^'||input.LA(1)=='|' ) {
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
    // $ANTLR end "OPERATOR"

    public void mTokens() throws RecognitionException {
        // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:8: ( MAP | OVERLAY | NUMBER | NAME | MATCOMBO | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR )
        int alt7=13;
        alt7 = dfa7.predict(input);
        switch (alt7) {
            case 1 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:10: MAP
                {
                mMAP(); 

                }
                break;
            case 2 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:14: OVERLAY
                {
                mOVERLAY(); 

                }
                break;
            case 3 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:22: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 4 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:29: NAME
                {
                mNAME(); 

                }
                break;
            case 5 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:34: MATCOMBO
                {
                mMATCOMBO(); 

                }
                break;
            case 6 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:43: WS
                {
                mWS(); 

                }
                break;
            case 7 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:46: SLCOMMENT
                {
                mSLCOMMENT(); 

                }
                break;
            case 8 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:56: MLCOMMENT
                {
                mMLCOMMENT(); 

                }
                break;
            case 9 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:66: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 10 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:73: BLOCKOPEN
                {
                mBLOCKOPEN(); 

                }
                break;
            case 11 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:83: BLOCKCLOSE
                {
                mBLOCKCLOSE(); 

                }
                break;
            case 12 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:94: STATEMENTEND
                {
                mSTATEMENTEND(); 

                }
                break;
            case 13 :
                // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:107: OPERATOR
                {
                mOPERATOR(); 

                }
                break;

        }

    }


    protected DFA7 dfa7 = new DFA7(this);
    static final String DFA7_eotS =
        "\1\uffff\2\15\1\uffff\1\15\7\uffff\1\15\1\uffff\1\15\1\uffff\1\15"+
        "\2\uffff\1\25\1\15\1\uffff\3\15\1\32\1\uffff";
    static final String DFA7_eofS =
        "\33\uffff";
    static final String DFA7_minS =
        "\1\11\2\55\1\uffff\1\55\1\uffff\1\52\5\uffff\1\55\1\uffff\1\55\1"+
        "\uffff\1\55\2\uffff\2\55\1\uffff\4\55\1\uffff";
    static final String DFA7_maxS =
        "\1\175\2\172\1\uffff\1\172\1\uffff\1\57\5\uffff\1\172\1\uffff\1"+
        "\172\1\uffff\1\172\2\uffff\2\172\1\uffff\4\172\1\uffff";
    static final String DFA7_acceptS =
        "\3\uffff\1\3\1\uffff\1\6\1\uffff\1\11\1\12\1\13\1\14\1\15\1\uffff"+
        "\1\4\1\uffff\1\5\1\uffff\1\7\1\10\2\uffff\1\1\4\uffff\1\2";
    static final String DFA7_specialS =
        "\33\uffff}>";
    static final String[] DFA7_transitionS = {
            "\2\5\2\uffff\1\5\22\uffff\1\5\5\uffff\1\13\4\uffff\1\3\1\uffff"+
            "\1\3\1\uffff\1\6\12\3\1\uffff\1\12\1\uffff\1\7\3\uffff\32\4"+
            "\3\uffff\1\13\1\4\1\uffff\14\4\1\1\1\4\1\2\13\4\1\10\1\13\1"+
            "\11",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\1\14"+
            "\31\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\25\16"+
            "\1\20\4\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32\16",
            "",
            "\1\22\4\uffff\1\21",
            "",
            "",
            "",
            "",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\17\16"+
            "\1\23\12\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\4\16"+
            "\1\24\25\16",
            "",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\21\16"+
            "\1\26\10\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\13\16"+
            "\1\27\16\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\1\30"+
            "\31\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\30\16"+
            "\1\31\1\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32\16",
            ""
    };

    static final short[] DFA7_eot = DFA.unpackEncodedString(DFA7_eotS);
    static final short[] DFA7_eof = DFA.unpackEncodedString(DFA7_eofS);
    static final char[] DFA7_min = DFA.unpackEncodedStringToUnsignedChars(DFA7_minS);
    static final char[] DFA7_max = DFA.unpackEncodedStringToUnsignedChars(DFA7_maxS);
    static final short[] DFA7_accept = DFA.unpackEncodedString(DFA7_acceptS);
    static final short[] DFA7_special = DFA.unpackEncodedString(DFA7_specialS);
    static final short[][] DFA7_transition;

    static {
        int numStates = DFA7_transitionS.length;
        DFA7_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA7_transition[i] = DFA.unpackEncodedString(DFA7_transitionS[i]);
        }
    }

    class DFA7 extends DFA {

        public DFA7(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 7;
            this.eot = DFA7_eot;
            this.eof = DFA7_eof;
            this.min = DFA7_min;
            this.max = DFA7_max;
            this.accept = DFA7_accept;
            this.special = DFA7_special;
            this.transition = DFA7_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( MAP | OVERLAY | NUMBER | NAME | MATCOMBO | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR );";
        }
    }
 

}