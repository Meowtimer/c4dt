// $ANTLR 3.1.2 C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g 2009-08-26 16:30:37
package net.arctics.clonk.parser.mapcreator;

import org.antlr.runtime.*;

public class MapCreatorLexer extends Lexer {
    public static final int OVERLAY=8;
    public static final int LETTER=14;
    public static final int STATEMENTEND=4;
    public static final int OPERATOR=5;
    public static final int NUMBER=12;
    public static final int SLCOMMENT=20;
    public static final int INT=17;
    public static final int EOF=-1;
    public static final int WORD=18;
    public static final int BLOCKOPEN=9;
    public static final int NAME=7;
    public static final int UNIT=15;
    public static final int WS=19;
    public static final int BLOCKCLOSE=10;
    public static final int MAP=6;
    public static final int ASSIGN=11;
    public static final int DIGIT=16;
    public static final int MLCOMMENT=21;
    public static final int MATCOMBO=13;

    // delegates
    // delegators

    public MapCreatorLexer() {;} 
    public MapCreatorLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MapCreatorLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g"; }

    // $ANTLR start "MAP"
    public final void mMAP() throws RecognitionException {
        try {
            int _type = MAP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:193:6: ( 'map' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:193:8: 'map'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:194:10: ( 'overlay' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:194:12: 'overlay'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:196:17: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:
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

    // $ANTLR start "UNIT"
    public final void mUNIT() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:197:15: ( ( 'px' | '%' ) )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:197:17: ( 'px' | '%' )
            {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:197:17: ( 'px' | '%' )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='p') ) {
                alt1=1;
            }
            else if ( (LA1_0=='%') ) {
                alt1=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }
            switch (alt1) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:197:18: 'px'
                    {
                    match("px"); 


                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:197:23: '%'
                    {
                    match('%'); 

                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "UNIT"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:198:16: ( '0' .. '9' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:198:18: '0' .. '9'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:14: ( ( '+' | '-' )? ( DIGIT )+ ( UNIT )? )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:16: ( '+' | '-' )? ( DIGIT )+ ( UNIT )?
            {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:16: ( '+' | '-' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='+'||LA2_0=='-') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:
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

            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:27: ( DIGIT )+
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
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:27: DIGIT
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

            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:33: ( UNIT )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='%'||LA4_0=='p') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:199:33: UNIT
                    {
                    mUNIT(); 

                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "WORD"
    public final void mWORD() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:200:15: ( LETTER ( LETTER | DIGIT )* )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:200:17: LETTER ( LETTER | DIGIT )*
            {
            mLETTER(); 
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:200:24: ( LETTER | DIGIT )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( ((LA5_0>='0' && LA5_0<='9')||(LA5_0>='A' && LA5_0<='Z')||LA5_0=='_'||(LA5_0>='a' && LA5_0<='z')) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:
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
            	    break loop5;
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:202:9: ( INT )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:202:11: INT
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:203:7: ( WORD )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:203:9: WORD
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:204:10: ( WORD '-' WORD )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:204:12: WORD '-' WORD
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:205:5: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:205:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
            {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:205:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='\t' && LA6_0<='\n')||LA6_0=='\r'||LA6_0==' ') ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:
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
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:206:11: ( '//' ( . )* '\\n' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:206:13: '//' ( . )* '\\n'
            {
            match("//"); 

            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:206:18: ( . )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( (LA7_0=='\n') ) {
                    alt7=2;
                }
                else if ( ((LA7_0>='\u0000' && LA7_0<='\t')||(LA7_0>='\u000B' && LA7_0<='\uFFFF')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:206:18: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop7;
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:207:11: ( '/*' ( . )* '*/' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:207:13: '/*' ( . )* '*/'
            {
            match("/*"); 

            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:207:18: ( . )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( (LA8_0=='*') ) {
                    int LA8_1 = input.LA(2);

                    if ( (LA8_1=='/') ) {
                        alt8=2;
                    }
                    else if ( ((LA8_1>='\u0000' && LA8_1<='.')||(LA8_1>='0' && LA8_1<='\uFFFF')) ) {
                        alt8=1;
                    }


                }
                else if ( ((LA8_0>='\u0000' && LA8_0<=')')||(LA8_0>='+' && LA8_0<='\uFFFF')) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:207:18: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop8;
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:208:9: ( '=' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:208:11: '='
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:209:11: ( '{' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:209:13: '{'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:210:12: ( '}' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:210:14: '}'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:211:14: ( ';' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:211:16: ';'
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
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:212:11: ( '|' | '&' | '^' )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:
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
        // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:8: ( MAP | OVERLAY | NUMBER | NAME | MATCOMBO | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR )
        int alt9=13;
        alt9 = dfa9.predict(input);
        switch (alt9) {
            case 1 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:10: MAP
                {
                mMAP(); 

                }
                break;
            case 2 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:14: OVERLAY
                {
                mOVERLAY(); 

                }
                break;
            case 3 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:22: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 4 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:29: NAME
                {
                mNAME(); 

                }
                break;
            case 5 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:34: MATCOMBO
                {
                mMATCOMBO(); 

                }
                break;
            case 6 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:43: WS
                {
                mWS(); 

                }
                break;
            case 7 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:46: SLCOMMENT
                {
                mSLCOMMENT(); 

                }
                break;
            case 8 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:56: MLCOMMENT
                {
                mMLCOMMENT(); 

                }
                break;
            case 9 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:66: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 10 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:73: BLOCKOPEN
                {
                mBLOCKOPEN(); 

                }
                break;
            case 11 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:83: BLOCKCLOSE
                {
                mBLOCKCLOSE(); 

                }
                break;
            case 12 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:94: STATEMENTEND
                {
                mSTATEMENTEND(); 

                }
                break;
            case 13 :
                // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:1:107: OPERATOR
                {
                mOPERATOR(); 

                }
                break;

        }

    }


    protected DFA9 dfa9 = new DFA9(this);
    static final String DFA9_eotS =
        "\1\uffff\2\15\1\uffff\1\15\7\uffff\1\15\1\uffff\1\15\1\uffff\1"+
        "\15\2\uffff\1\25\1\15\1\uffff\3\15\1\32\1\uffff";
    static final String DFA9_eofS =
        "\33\uffff";
    static final String DFA9_minS =
        "\1\11\2\55\1\uffff\1\55\1\uffff\1\52\5\uffff\1\55\1\uffff\1\55"+
        "\1\uffff\1\55\2\uffff\2\55\1\uffff\4\55\1\uffff";
    static final String DFA9_maxS =
        "\1\175\2\172\1\uffff\1\172\1\uffff\1\57\5\uffff\1\172\1\uffff\1"+
        "\172\1\uffff\1\172\2\uffff\2\172\1\uffff\4\172\1\uffff";
    static final String DFA9_acceptS =
        "\3\uffff\1\3\1\uffff\1\6\1\uffff\1\11\1\12\1\13\1\14\1\15\1\uffff"+
        "\1\4\1\uffff\1\5\1\uffff\1\7\1\10\2\uffff\1\1\4\uffff\1\2";
    static final String DFA9_specialS =
        "\33\uffff}>";
    static final String[] DFA9_transitionS = {
            "\2\5\2\uffff\1\5\22\uffff\1\5\5\uffff\1\13\4\uffff\1\3\1\uffff"+
            "\1\3\1\uffff\1\6\12\3\1\uffff\1\12\1\uffff\1\7\3\uffff\32\4"+
            "\3\uffff\1\13\1\4\1\uffff\14\4\1\1\1\4\1\2\13\4\1\10\1\13\1"+
            "\11",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\1\14"+
            "\31\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\25"+
            "\16\1\20\4\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32"+
            "\16",
            "",
            "\1\22\4\uffff\1\21",
            "",
            "",
            "",
            "",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\17"+
            "\16\1\23\12\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32"+
            "\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\4\16"+
            "\1\24\25\16",
            "",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32"+
            "\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\21"+
            "\16\1\26\10\16",
            "",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\13"+
            "\16\1\27\16\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\1\30"+
            "\31\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\30"+
            "\16\1\31\1\16",
            "\1\17\2\uffff\12\16\7\uffff\32\16\4\uffff\1\16\1\uffff\32"+
            "\16",
            ""
    };

    static final short[] DFA9_eot = DFA.unpackEncodedString(DFA9_eotS);
    static final short[] DFA9_eof = DFA.unpackEncodedString(DFA9_eofS);
    static final char[] DFA9_min = DFA.unpackEncodedStringToUnsignedChars(DFA9_minS);
    static final char[] DFA9_max = DFA.unpackEncodedStringToUnsignedChars(DFA9_maxS);
    static final short[] DFA9_accept = DFA.unpackEncodedString(DFA9_acceptS);
    static final short[] DFA9_special = DFA.unpackEncodedString(DFA9_specialS);
    static final short[][] DFA9_transition;

    static {
        int numStates = DFA9_transitionS.length;
        DFA9_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA9_transition[i] = DFA.unpackEncodedString(DFA9_transitionS[i]);
        }
    }

    class DFA9 extends DFA {

        public DFA9(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 9;
            this.eot = DFA9_eot;
            this.eof = DFA9_eof;
            this.min = DFA9_min;
            this.max = DFA9_max;
            this.accept = DFA9_accept;
            this.special = DFA9_special;
            this.transition = DFA9_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( MAP | OVERLAY | NUMBER | NAME | MATCOMBO | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR );";
        }
    }
 

}