// $ANTLR 3.1.2 /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g 2009-05-18 00:02:45

package net.arctics.clonk.parser.map;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MapGeneratorParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "MAP", "WORD", "OVERLAY", "INT", "MATERIAL", "LETTER", "DIGIT", "WS", "'{'", "'}'", "';'"
    };
    public static final int WORD=5;
    public static final int WS=11;
    public static final int T__12=12;
    public static final int OVERLAY=6;
    public static final int LETTER=9;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int MAP=4;
    public static final int MATERIAL=8;
    public static final int INT=7;
    public static final int DIGIT=10;
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


    void createMapObject(Class<? extends C4MapOverlay> cls, String name) {
    	try {
    		C4MapOverlay newOverlay = current.createOverlay(cls, name);
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
    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:1: parse : ( subobject )* ;
    public final void parse() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:7: ( ( subobject )* )
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:9: ( subobject )*
            {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:9: ( subobject )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>=MAP && LA1_0<=OVERLAY)) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:48:9: subobject
            	    {
            	    pushFollow(FOLLOW_subobject_in_parse31);
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
    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:50:1: subobject : ( MAP (name= WORD )? '{' ( subobject )* '}' ';' | OVERLAY (name= WORD )? '{' ( subobject )* '}' ';' | template= WORD (name= WORD )? '{' ( subobject )* '}' ';' | attr= WORD attrValue= INT | attr= WORD attrValue= WORD | attr= WORD attrValue= MATERIAL );
    public final void subobject() throws RecognitionException {
        Token name=null;
        Token template=null;
        Token attr=null;
        Token attrValue=null;

        try {
            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:2: ( MAP (name= WORD )? '{' ( subobject )* '}' ';' | OVERLAY (name= WORD )? '{' ( subobject )* '}' ';' | template= WORD (name= WORD )? '{' ( subobject )* '}' ';' | attr= WORD attrValue= INT | attr= WORD attrValue= WORD | attr= WORD attrValue= MATERIAL )
            int alt8=6;
            switch ( input.LA(1) ) {
            case MAP:
                {
                alt8=1;
                }
                break;
            case OVERLAY:
                {
                alt8=2;
                }
                break;
            case WORD:
                {
                switch ( input.LA(2) ) {
                case INT:
                    {
                    alt8=4;
                    }
                    break;
                case WORD:
                    {
                    int LA8_5 = input.LA(3);

                    if ( (LA8_5==EOF||(LA8_5>=MAP && LA8_5<=OVERLAY)||LA8_5==13) ) {
                        alt8=5;
                    }
                    else if ( (LA8_5==12) ) {
                        alt8=3;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 8, 5, input);

                        throw nvae;
                    }
                    }
                    break;
                case MATERIAL:
                    {
                    alt8=6;
                    }
                    break;
                case 12:
                    {
                    alt8=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 3, input);

                    throw nvae;
                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }

            switch (alt8) {
                case 1 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:4: MAP (name= WORD )? '{' ( subobject )* '}' ';'
                    {
                    match(input,MAP,FOLLOW_MAP_in_subobject41); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:12: (name= WORD )?
                    int alt2=2;
                    int LA2_0 = input.LA(1);

                    if ( (LA2_0==WORD) ) {
                        alt2=1;
                    }
                    switch (alt2) {
                        case 1 :
                            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:12: name= WORD
                            {
                            name=(Token)match(input,WORD,FOLLOW_WORD_in_subobject45); 

                            }
                            break;

                    }

                    createMapObject(C4Map.class, (name!=null?name.getText():null));
                    match(input,12,FOLLOW_12_in_subobject50); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:67: ( subobject )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( ((LA3_0>=MAP && LA3_0<=OVERLAY)) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:51:67: subobject
                    	    {
                    	    pushFollow(FOLLOW_subobject_in_subobject52);
                    	    subobject();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);

                    match(input,13,FOLLOW_13_in_subobject55); 
                    match(input,14,FOLLOW_14_in_subobject57); 

                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:52:4: OVERLAY (name= WORD )? '{' ( subobject )* '}' ';'
                    {
                    match(input,OVERLAY,FOLLOW_OVERLAY_in_subobject62); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:52:16: (name= WORD )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==WORD) ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:52:16: name= WORD
                            {
                            name=(Token)match(input,WORD,FOLLOW_WORD_in_subobject66); 

                            }
                            break;

                    }

                    createMapObject(C4MapOverlay.class, (name!=null?name.getText():null));
                    match(input,12,FOLLOW_12_in_subobject71); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:52:78: ( subobject )*
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( ((LA5_0>=MAP && LA5_0<=OVERLAY)) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:52:78: subobject
                    	    {
                    	    pushFollow(FOLLOW_subobject_in_subobject73);
                    	    subobject();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop5;
                        }
                    } while (true);

                    match(input,13,FOLLOW_13_in_subobject76); 
                    match(input,14,FOLLOW_14_in_subobject78); 

                    }
                    break;
                case 3 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:53:4: template= WORD (name= WORD )? '{' ( subobject )* '}' ';'
                    {
                    template=(Token)match(input,WORD,FOLLOW_WORD_in_subobject85); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:53:22: (name= WORD )?
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==WORD) ) {
                        alt6=1;
                    }
                    switch (alt6) {
                        case 1 :
                            // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:53:22: name= WORD
                            {
                            name=(Token)match(input,WORD,FOLLOW_WORD_in_subobject89); 

                            }
                            break;

                    }

                    createMapObject((template!=null?template.getText():null), (name!=null?name.getText():null));
                    match(input,12,FOLLOW_12_in_subobject94); 
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:53:80: ( subobject )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( ((LA7_0>=MAP && LA7_0<=OVERLAY)) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:53:80: subobject
                    	    {
                    	    pushFollow(FOLLOW_subobject_in_subobject96);
                    	    subobject();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);

                    match(input,13,FOLLOW_13_in_subobject99); 
                    match(input,14,FOLLOW_14_in_subobject101); 

                    }
                    break;
                case 4 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:54:4: attr= WORD attrValue= INT
                    {
                    attr=(Token)match(input,WORD,FOLLOW_WORD_in_subobject108); 
                    attrValue=(Token)match(input,INT,FOLLOW_INT_in_subobject112); 

                    }
                    break;
                case 5 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:55:4: attr= WORD attrValue= WORD
                    {
                    attr=(Token)match(input,WORD,FOLLOW_WORD_in_subobject119); 
                    attrValue=(Token)match(input,WORD,FOLLOW_WORD_in_subobject123); 

                    }
                    break;
                case 6 :
                    // /Users/madeen/Projects/Eclipse/Clonk/src/net/arctics/clonk/parser/map/MapGenerator.g:56:4: attr= WORD attrValue= MATERIAL
                    {
                    attr=(Token)match(input,WORD,FOLLOW_WORD_in_subobject130); 
                    attrValue=(Token)match(input,MATERIAL,FOLLOW_MATERIAL_in_subobject134); 

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


 

    public static final BitSet FOLLOW_subobject_in_parse31 = new BitSet(new long[]{0x0000000000000072L});
    public static final BitSet FOLLOW_MAP_in_subobject41 = new BitSet(new long[]{0x0000000000001020L});
    public static final BitSet FOLLOW_WORD_in_subobject45 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_subobject50 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_subobject_in_subobject52 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_13_in_subobject55 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_subobject57 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_OVERLAY_in_subobject62 = new BitSet(new long[]{0x0000000000001020L});
    public static final BitSet FOLLOW_WORD_in_subobject66 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_subobject71 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_subobject_in_subobject73 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_13_in_subobject76 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_subobject78 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WORD_in_subobject85 = new BitSet(new long[]{0x0000000000001020L});
    public static final BitSet FOLLOW_WORD_in_subobject89 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_12_in_subobject94 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_subobject_in_subobject96 = new BitSet(new long[]{0x0000000000002070L});
    public static final BitSet FOLLOW_13_in_subobject99 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_subobject101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WORD_in_subobject108 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_subobject112 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WORD_in_subobject119 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_WORD_in_subobject123 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WORD_in_subobject130 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_MATERIAL_in_subobject134 = new BitSet(new long[]{0x0000000000000002L});

}