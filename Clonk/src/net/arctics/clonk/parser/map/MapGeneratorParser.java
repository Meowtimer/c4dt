// $ANTLR 3.1.2 /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g 2009-05-17 20:36:49

package net.arctics.clonk.parser.map;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MapGeneratorParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "WORD", "ATTRVALUE", "LETTER", "DIGIT", "INT", "WS", "'{'", "'}'", "';'", "'='"
    };
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


        public MapGeneratorParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public MapGeneratorParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return MapGeneratorParser.tokenNames; }
    public String getGrammarFileName() { return "/Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g"; }


    C4MapCreator mapCreator;
    C4MapOverlay current;

    void createMapObject(String type, String name) {
    	try {
    		C4MapOverlay newOverlay = current.createOverlay(type, name);
    		current = newOverlay;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private void setVal(String name, String value) {
    	try {
    		current.setAttribute(name, value);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    public MapGeneratorParser(C4MapCreator mapCreator, TokenStream input) {
    	this(input);
    	this.mapCreator = mapCreator;
    	this.current = mapCreator;
    }




    // $ANTLR start "parse"
    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:38:1: parse : ( subobject )* ;
    public final void parse() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:38:8: ( ( subobject )* )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:38:10: ( subobject )*
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:38:10: ( subobject )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==WORD) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:38:10: subobject
            	    {
            	    pushFollow(FOLLOW_subobject_in_parse32);
            	    subobject();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "parse"


    // $ANTLR start "subobject"
    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:40:1: subobject : (type= WORD (name= WORD )? '{' ( subobject )* '}' ';' | attr= WORD '=' attrvalue= ATTRVALUE ';' );
    public final void subobject() throws RecognitionException {
        Token type=null;
        Token name=null;
        Token attr=null;
        Token attrvalue=null;

        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:3: (type= WORD (name= WORD )? '{' ( subobject )* '}' ';' | attr= WORD '=' attrvalue= ATTRVALUE ';' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==WORD) ) {
                int LA4_1 = input.LA(2);

                if ( (LA4_1==13) ) {
                    alt4=2;
                }
                else if ( (LA4_1==WORD||LA4_1==10) ) {
                    alt4=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:5: type= WORD (name= WORD )? '{' ( subobject )* '}' ';'
                    {
                    type=(Token)match(input,WORD,FOLLOW_WORD_in_subobject45); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:15: (name= WORD )?
                    int alt2=2;
                    int LA2_0 = input.LA(1);

                    if ( (LA2_0==WORD) ) {
                        alt2=1;
                    }
                    switch (alt2) {
                        case 1 :
                            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:16: name= WORD
                            {
                            name=(Token)match(input,WORD,FOLLOW_WORD_in_subobject50); 

                            }
                            break;

                    }

                    createMapObject((type!=null?type.getText():null), (name!=null?name.getText():null));
                    match(input,10,FOLLOW_10_in_subobject56); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:75: ( subobject )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( (LA3_0==WORD) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:41:75: subobject
                    	    {
                    	    pushFollow(FOLLOW_subobject_in_subobject58);
                    	    subobject();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);

                    match(input,11,FOLLOW_11_in_subobject61); 
                    match(input,12,FOLLOW_12_in_subobject63); 

                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:42:5: attr= WORD '=' attrvalue= ATTRVALUE ';'
                    {
                    attr=(Token)match(input,WORD,FOLLOW_WORD_in_subobject71); 
                    match(input,13,FOLLOW_13_in_subobject73); 
                    attrvalue=(Token)match(input,ATTRVALUE,FOLLOW_ATTRVALUE_in_subobject77); 
                    match(input,12,FOLLOW_12_in_subobject79); 
                    setVal((attr!=null?attr.getText():null), (attrvalue!=null?attrvalue.getText():null));

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "subobject"

    // Delegated rules


 

    public static final BitSet FOLLOW_subobject_in_parse32 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_WORD_in_subobject45 = new BitSet(new long[]{0x0000000000000410L});
    public static final BitSet FOLLOW_WORD_in_subobject50 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_subobject56 = new BitSet(new long[]{0x0000000000000810L});
    public static final BitSet FOLLOW_subobject_in_subobject58 = new BitSet(new long[]{0x0000000000000810L});
    public static final BitSet FOLLOW_11_in_subobject61 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_subobject63 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WORD_in_subobject71 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_13_in_subobject73 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ATTRVALUE_in_subobject77 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_subobject79 = new BitSet(new long[]{0x0000000000000002L});

}