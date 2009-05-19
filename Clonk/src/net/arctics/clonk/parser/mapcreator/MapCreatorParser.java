// $ANTLR 3.1.2 C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g 2009-05-19 08:35:22

package net.arctics.clonk.parser.map;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MapCreatorParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "STATEMENTEND", "OPERATOR", "MAP", "NAME", "OVERLAY", "BLOCKOPEN", "BLOCKCLOSE", "ASSIGN", "NUMBER", "MATCOMBO", "LETTER", "DIGIT", "INT", "WORD", "WS", "SLCOMMENT", "MLCOMMENT"
    };
    public static final int OVERLAY=8;
    public static final int STATEMENTEND=4;
    public static final int LETTER=14;
    public static final int OPERATOR=5;
    public static final int NUMBER=12;
    public static final int SLCOMMENT=19;
    public static final int INT=16;
    public static final int EOF=-1;
    public static final int WORD=17;
    public static final int BLOCKOPEN=9;
    public static final int NAME=7;
    public static final int WS=18;
    public static final int BLOCKCLOSE=10;
    public static final int MAP=6;
    public static final int ASSIGN=11;
    public static final int DIGIT=15;
    public static final int MLCOMMENT=20;
    public static final int MATCOMBO=13;

    // delegates
    // delegators


        public MapCreatorParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public MapCreatorParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return MapCreatorParser.tokenNames; }
    public String getGrammarFileName() { return "C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g"; }


    C4MapCreator mapCreator;
    C4MapOverlay current;
    C4MapOverlay lastOverlay;

    public MapCreatorParser(C4MapCreator mapCreator, TokenStream input) {
    	this(input);
    	this.mapCreator = mapCreator;
    	this.current = mapCreator;
    }

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

    private void moveLevelUp() {
    	lastOverlay = current;
    	current = (C4MapOverlay) current.getParentDeclaration();
    }

    private void assignOperator(String t) {
    	C4MapOverlay.Operator op = C4MapOverlay.Operator.valueOf(t.charAt(0));
    	lastOverlay.setOperator(op);
    }




    // $ANTLR start "parse"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:59:1: parse : ( statement )* ;
    public final void parse() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:59:7: ( ( statement )* )
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:59:9: ( statement )*
            {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:59:9: ( statement )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>=MAP && LA1_0<=OVERLAY)) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:59:9: statement
            	    {
            	    pushFollow(FOLLOW_statement_in_parse31);
            	    statement();

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


    // $ANTLR start "statement"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:61:1: statement : composition STATEMENTEND ;
    public final void statement() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:62:2: ( composition STATEMENTEND )
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:62:4: composition STATEMENTEND
            {
            lastOverlay = null;
            pushFollow(FOLLOW_composition_in_statement43);
            composition();

            state._fsp--;

            match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_statement45); 

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
    // $ANTLR end "statement"


    // $ANTLR start "composition"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:64:1: composition : subobject (op= OPERATOR composition )? ;
    public final void composition() throws RecognitionException {
        Token op=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:65:2: ( subobject (op= OPERATOR composition )? )
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:65:4: subobject (op= OPERATOR composition )?
            {
            pushFollow(FOLLOW_subobject_in_composition54);
            subobject();

            state._fsp--;

            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:65:14: (op= OPERATOR composition )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==OPERATOR) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:65:15: op= OPERATOR composition
                    {
                    op=(Token)match(input,OPERATOR,FOLLOW_OPERATOR_in_composition59); 
                    assignOperator((op!=null?op.getText():null));
                    pushFollow(FOLLOW_composition_in_composition63);
                    composition();

                    state._fsp--;


                    }
                    break;

            }


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
    // $ANTLR end "composition"


    // $ANTLR start "subobject"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:67:1: subobject : ( MAP (name= NAME )? block | OVERLAY (name= NAME )? block | template= NAME (name= NAME )? block );
    public final void subobject() throws RecognitionException {
        Token name=null;
        Token template=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:68:2: ( MAP (name= NAME )? block | OVERLAY (name= NAME )? block | template= NAME (name= NAME )? block )
            int alt6=3;
            switch ( input.LA(1) ) {
            case MAP:
                {
                alt6=1;
                }
                break;
            case OVERLAY:
                {
                alt6=2;
                }
                break;
            case NAME:
                {
                alt6=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;
            }

            switch (alt6) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:68:4: MAP (name= NAME )? block
                    {
                    match(input,MAP,FOLLOW_MAP_in_subobject74); 
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:68:12: (name= NAME )?
                    int alt3=2;
                    int LA3_0 = input.LA(1);

                    if ( (LA3_0==NAME) ) {
                        alt3=1;
                    }
                    switch (alt3) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:68:12: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject78); 

                            }
                            break;

                    }

                    createMapObject(C4Map.class, (name!=null?name.getText():null));
                    pushFollow(FOLLOW_block_in_subobject83);
                    block();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:69:4: OVERLAY (name= NAME )? block
                    {
                    match(input,OVERLAY,FOLLOW_OVERLAY_in_subobject88); 
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:69:16: (name= NAME )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==NAME) ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:69:16: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject92); 

                            }
                            break;

                    }

                    createMapObject(C4MapOverlay.class, (name!=null?name.getText():null));
                    pushFollow(FOLLOW_block_in_subobject97);
                    block();

                    state._fsp--;


                    }
                    break;
                case 3 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:70:4: template= NAME (name= NAME )? block
                    {
                    template=(Token)match(input,NAME,FOLLOW_NAME_in_subobject104); 
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:70:22: (name= NAME )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==NAME) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:70:22: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject108); 

                            }
                            break;

                    }

                    createMapObject((template!=null?template.getText():null), (name!=null?name.getText():null));
                    pushFollow(FOLLOW_block_in_subobject113);
                    block();

                    state._fsp--;


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


    // $ANTLR start "block"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:72:1: block : BLOCKOPEN ( statementorattrib )* BLOCKCLOSE ;
    public final void block() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:72:7: ( BLOCKOPEN ( statementorattrib )* BLOCKCLOSE )
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:72:9: BLOCKOPEN ( statementorattrib )* BLOCKCLOSE
            {
            match(input,BLOCKOPEN,FOLLOW_BLOCKOPEN_in_block121); 
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:72:19: ( statementorattrib )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>=MAP && LA7_0<=OVERLAY)) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:72:19: statementorattrib
            	    {
            	    pushFollow(FOLLOW_statementorattrib_in_block123);
            	    statementorattrib();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);

            match(input,BLOCKCLOSE,FOLLOW_BLOCKCLOSE_in_block126); 
            moveLevelUp();

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
    // $ANTLR end "block"


    // $ANTLR start "statementorattrib"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:74:1: statementorattrib : ( attribute | statement );
    public final void statementorattrib() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:75:2: ( attribute | statement )
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==NAME) ) {
                int LA8_1 = input.LA(2);

                if ( (LA8_1==ASSIGN) ) {
                    alt8=1;
                }
                else if ( (LA8_1==NAME||LA8_1==BLOCKOPEN) ) {
                    alt8=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA8_0==MAP||LA8_0==OVERLAY) ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:75:4: attribute
                    {
                    pushFollow(FOLLOW_attribute_in_statementorattrib137);
                    attribute();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:75:14: statement
                    {
                    pushFollow(FOLLOW_statement_in_statementorattrib139);
                    statement();

                    state._fsp--;


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
    // $ANTLR end "statementorattrib"


    // $ANTLR start "attribute"
    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:77:1: attribute : (attr= NAME ASSIGN attrValue= NAME STATEMENTEND | attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND | attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND );
    public final void attribute() throws RecognitionException {
        Token attr=null;
        Token attrValue=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:78:2: (attr= NAME ASSIGN attrValue= NAME STATEMENTEND | attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND | attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND )
            int alt9=3;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==NAME) ) {
                int LA9_1 = input.LA(2);

                if ( (LA9_1==ASSIGN) ) {
                    switch ( input.LA(3) ) {
                    case NAME:
                        {
                        alt9=1;
                        }
                        break;
                    case NUMBER:
                        {
                        alt9=2;
                        }
                        break;
                    case MATCOMBO:
                        {
                        alt9=3;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("", 9, 2, input);

                        throw nvae;
                    }

                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:78:4: attr= NAME ASSIGN attrValue= NAME STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute150); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute152); 
                    attrValue=(Token)match(input,NAME,FOLLOW_NAME_in_attribute156); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute158); 
                    setVal((attr!=null?attr.getText():null), (attrValue!=null?attrValue.getText():null));

                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:79:4: attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute167); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute169); 
                    attrValue=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_attribute173); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute175); 
                    setVal((attr!=null?attr.getText():null), (attrValue!=null?attrValue.getText():null));

                    }
                    break;
                case 3 :
                    // C:\\Users\\Madeen\\Projects\\Eclipse\\eclipse.c4dt\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:80:4: attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute184); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute186); 
                    attrValue=(Token)match(input,MATCOMBO,FOLLOW_MATCOMBO_in_attribute190); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute192); 
                    setVal((attr!=null?attr.getText():null), (attrValue!=null?attrValue.getText():null));

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
    // $ANTLR end "attribute"

    // Delegated rules


 

    public static final BitSet FOLLOW_statement_in_parse31 = new BitSet(new long[]{0x00000000000001C2L});
    public static final BitSet FOLLOW_composition_in_statement43 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_statement45 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_subobject_in_composition54 = new BitSet(new long[]{0x0000000000000022L});
    public static final BitSet FOLLOW_OPERATOR_in_composition59 = new BitSet(new long[]{0x00000000000001C0L});
    public static final BitSet FOLLOW_composition_in_composition63 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MAP_in_subobject74 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject78 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_block_in_subobject83 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_OVERLAY_in_subobject88 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject92 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_block_in_subobject97 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_subobject104 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject108 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_block_in_subobject113 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BLOCKOPEN_in_block121 = new BitSet(new long[]{0x00000000000005C0L});
    public static final BitSet FOLLOW_statementorattrib_in_block123 = new BitSet(new long[]{0x00000000000005C0L});
    public static final BitSet FOLLOW_BLOCKCLOSE_in_block126 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_statementorattrib137 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_statement_in_statementorattrib139 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute150 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute152 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_NAME_in_attribute156 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute158 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute167 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute169 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_NUMBER_in_attribute173 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute175 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute184 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute186 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_MATCOMBO_in_attribute190 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute192 = new BitSet(new long[]{0x0000000000000002L});

}