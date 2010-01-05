// $ANTLR 3.2 Sep 23, 2009 12:02:23 /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g 2010-01-05 15:41:14
package net.arctics.clonk.parser.mapcreator;

import org.antlr.runtime.*;

public class MapCreatorLexer extends Lexer {
    public static final int POINT=9;
    public static final int OVERLAY=8;
    public static final int LETTER=16;
    public static final int STATEMENTEND=4;
    public static final int OPERATOR=5;
    public static final int NUMBER=13;
    public static final int SLCOMMENT=22;
    public static final int MINUS=14;
    public static final int EOF=-1;
    public static final int WORD=19;
    public static final int BLOCKOPEN=10;
    public static final int NAME=7;
    public static final int UNIT=17;
    public static final int WS=21;
    public static final int BLOCKCLOSE=11;
    public static final int MAP=6;
    public static final int ASSIGN=12;
    public static final int PLUS=20;
    public static final int DIGIT=18;
    public static final int MLCOMMENT=23;
    public static final int MATCOMBO=15;

    // delegates
    // delegators

    public MapCreatorLexer() {;} 
    public MapCreatorLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MapCreatorLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g"; }

    // $ANTLR start "MAP"
    public final void mMAP() throws RecognitionException {
        try {
            int _type = MAP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:199:6: ( 'map' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:199:8: 'map'
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:10: ( 'overlay' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:12: 'overlay'
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

    // $ANTLR start "POINT"
    public final void mPOINT() throws RecognitionException {
        try {
            int _type = POINT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:201:8: ( 'point' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:201:10: 'point'
            {
            match("point"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "POINT"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:17: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:15: ( ( 'px' | '%' ) )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:17: ( 'px' | '%' )
            {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:17: ( 'px' | '%' )
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
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:18: 'px'
                    {
                    match("px"); 


                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:204:23: '%'
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:16: ( '0' .. '9' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:18: '0' .. '9'
            {
            matchRange('0','9'); 

            }

        }
        finally {
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "WORD"
    public final void mWORD() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:15: ( LETTER ( LETTER | DIGIT )* )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:17: LETTER ( LETTER | DIGIT )*
            {
            mLETTER(); 
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:24: ( LETTER | DIGIT )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( ((LA2_0>='0' && LA2_0<='9')||(LA2_0>='A' && LA2_0<='Z')||LA2_0=='_'||(LA2_0>='a' && LA2_0<='z')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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
            	    break loop2;
                }
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "WORD"

    // $ANTLR start "MATCOMBO"
    public final void mMATCOMBO() throws RecognitionException {
        try {
            int _type = MATCOMBO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:10: ( WORD MINUS WORD )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:12: WORD MINUS WORD
            {
            mWORD(); 
            mMINUS(); 
            mWORD(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MATCOMBO"

    // $ANTLR start "MINUS"
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:209:8: ( '-' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:209:10: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MINUS"

    // $ANTLR start "PLUS"
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:210:7: ( '+' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:210:9: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PLUS"

    // $ANTLR start "NUMBER"
    public final void mNUMBER() throws RecognitionException {
        try {
            int _type = NUMBER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:9: ( ( PLUS | MINUS )? ( DIGIT )+ ( UNIT )? )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:11: ( PLUS | MINUS )? ( DIGIT )+ ( UNIT )?
            {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:11: ( PLUS | MINUS )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0=='+'||LA3_0=='-') ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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

            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:25: ( DIGIT )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( ((LA4_0>='0' && LA4_0<='9')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:25: DIGIT
            	    {
            	    mDIGIT(); 

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

            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:31: ( UNIT )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='%'||LA5_0=='p') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:211:31: UNIT
                    {
                    mUNIT(); 

                    }
                    break;

            }


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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:212:7: ( WORD )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:212:9: WORD
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

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:213:5: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:213:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
            {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:213:7: ( ' ' | '\\t' | '\\n' | '\\r' )+
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
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:214:11: ( '//' ( . )* '\\n' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:214:13: '//' ( . )* '\\n'
            {
            match("//"); 

            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:214:18: ( . )*
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
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:214:18: .
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:215:11: ( '/*' ( . )* '*/' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:215:13: '/*' ( . )* '*/'
            {
            match("/*"); 

            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:215:18: ( . )*
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
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:215:18: .
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:216:9: ( '=' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:216:11: '='
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:217:11: ( '{' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:217:13: '{'
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:218:12: ( '}' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:218:14: '}'
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:219:14: ( ';' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:219:16: ';'
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
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:220:11: ( '|' | '&' | '^' )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:
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
        // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:8: ( MAP | OVERLAY | POINT | MATCOMBO | MINUS | PLUS | NUMBER | NAME | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR )
        int alt9=16;
        alt9 = dfa9.predict(input);
        switch (alt9) {
            case 1 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:10: MAP
                {
                mMAP(); 

                }
                break;
            case 2 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:14: OVERLAY
                {
                mOVERLAY(); 

                }
                break;
            case 3 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:22: POINT
                {
                mPOINT(); 

                }
                break;
            case 4 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:28: MATCOMBO
                {
                mMATCOMBO(); 

                }
                break;
            case 5 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:37: MINUS
                {
                mMINUS(); 

                }
                break;
            case 6 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:43: PLUS
                {
                mPLUS(); 

                }
                break;
            case 7 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:48: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 8 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:55: NAME
                {
                mNAME(); 

                }
                break;
            case 9 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:60: WS
                {
                mWS(); 

                }
                break;
            case 10 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:63: SLCOMMENT
                {
                mSLCOMMENT(); 

                }
                break;
            case 11 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:73: MLCOMMENT
                {
                mMLCOMMENT(); 

                }
                break;
            case 12 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:83: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 13 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:90: BLOCKOPEN
                {
                mBLOCKOPEN(); 

                }
                break;
            case 14 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:100: BLOCKCLOSE
                {
                mBLOCKCLOSE(); 

                }
                break;
            case 15 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:111: STATEMENTEND
                {
                mSTATEMENTEND(); 

                }
                break;
            case 16 :
                // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:1:124: OPERATOR
                {
                mOPERATOR(); 

                }
                break;

        }

    }


    protected DFA9 dfa9 = new DFA9(this);
    static final String DFA9_eotS =
        "\1\uffff\4\20\1\25\1\26\10\uffff\1\20\1\uffff\1\20\1\uffff\2\20"+
        "\4\uffff\1\34\2\20\1\uffff\3\20\1\42\1\20\1\uffff\1\44\1\uffff";
    static final String DFA9_eofS =
        "\45\uffff";
    static final String DFA9_minS =
        "\1\11\4\55\2\60\2\uffff\1\52\5\uffff\1\55\1\uffff\1\55\1\uffff\2"+
        "\55\4\uffff\3\55\1\uffff\5\55\1\uffff\1\55\1\uffff";
    static final String DFA9_maxS =
        "\1\175\4\172\2\71\2\uffff\1\57\5\uffff\1\172\1\uffff\1\172\1\uffff"+
        "\2\172\4\uffff\3\172\1\uffff\5\172\1\uffff\1\172\1\uffff";
    static final String DFA9_acceptS =
        "\7\uffff\1\7\1\11\1\uffff\1\14\1\15\1\16\1\17\1\20\1\uffff\1\10"+
        "\1\uffff\1\4\2\uffff\1\5\1\6\1\12\1\13\3\uffff\1\1\5\uffff\1\3\1"+
        "\uffff\1\2";
    static final String DFA9_specialS =
        "\45\uffff}>";
    static final String[] DFA9_transitionS = {
            "\2\10\2\uffff\1\10\22\uffff\1\10\5\uffff\1\16\4\uffff\1\6\1"+
            "\uffff\1\5\1\uffff\1\11\12\7\1\uffff\1\15\1\uffff\1\12\3\uffff"+
            "\32\4\3\uffff\1\16\1\4\1\uffff\14\4\1\1\1\4\1\2\1\3\12\4\1\13"+
            "\1\16\1\14",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\1\17"+
            "\31\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\25\21"+
            "\1\23\4\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\16\21"+
            "\1\24\13\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\32\21",
            "\12\7",
            "\12\7",
            "",
            "",
            "\1\30\4\uffff\1\27",
            "",
            "",
            "",
            "",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\17\21"+
            "\1\31\12\21",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\32\21",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\4\21"+
            "\1\32\25\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\10\21"+
            "\1\33\21\21",
            "",
            "",
            "",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\32\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\21\21"+
            "\1\35\10\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\15\21"+
            "\1\36\14\21",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\13\21"+
            "\1\37\16\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\23\21"+
            "\1\40\6\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\1\41"+
            "\31\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\32\21",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\30\21"+
            "\1\43\1\21",
            "",
            "\1\22\2\uffff\12\21\7\uffff\32\21\4\uffff\1\21\1\uffff\32\21",
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
            return "1:1: Tokens : ( MAP | OVERLAY | POINT | MATCOMBO | MINUS | PLUS | NUMBER | NAME | WS | SLCOMMENT | MLCOMMENT | ASSIGN | BLOCKOPEN | BLOCKCLOSE | STATEMENTEND | OPERATOR );";
        }
    }
 

}