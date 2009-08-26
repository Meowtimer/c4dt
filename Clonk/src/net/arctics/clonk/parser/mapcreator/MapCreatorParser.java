// $ANTLR 3.1.2 C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g 2009-08-26 14:50:32

package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;


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
    public String getGrammarFileName() { return "C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g"; }


    C4MapCreator mapCreator;
    C4MapOverlay current;
    C4MapOverlay lastOverlay;

    public MapCreatorParser(C4MapCreator mapCreator, TokenStream input) {
    	this(input);
    	this.mapCreator = mapCreator;
    	this.current = mapCreator;
    }

    public MapCreatorParser(C4MapCreator mapCreator) {
    	this (mapCreator, getTokenStream(mapCreator));
    }

    private static TokenStream getTokenStream(C4MapCreator mapCreator) {
    	CharStream charStream;
    	try {
    		charStream = new ANTLRFileStream(mapCreator.getResource().getLocation().toOSString());
    		MapCreatorLexer lexer = new MapCreatorLexer(charStream);
    		CommonTokenStream tokenStream = new CommonTokenStream();
    		tokenStream.setTokenSource(lexer);
    		return tokenStream;
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	}
    }

    private static int startPos(Token t) {
    	return ((CommonToken)t).getStartIndex();
    }

    private static int endPos(Token t, Token fallback) {
    	return ((CommonToken)(t!=null?t:fallback)).getStopIndex()+1;
    }

    private static int endPos(Token t) {
    	return endPos(t, null);
    }

    private void setCurrentOverlay(C4MapOverlay overlay, Token typeToken, Token nameToken) {
    	current = overlay;
    	current.setLocation(new SourceLocation(startPos(typeToken), endPos(nameToken, typeToken)));
    }

    private void createMapObject(Token typeToken, Token nameToken) {
    	try {
    		if (current == null)
    			return;	
    		C4MapOverlay newOverlay = current.createOverlay(typeToken.getText(), nameToken!=null?nameToken.getText():null);
    		if (newOverlay == null)
    			errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(typeToken), endPos(typeToken), typeToken.getText());
    		else
    			setCurrentOverlay(newOverlay, typeToken, nameToken);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private void setVal(Token nameToken, Token valueToken) {
    	try {
    		current.setAttribute(nameToken.getText(), valueToken.getText());
    	} catch (NoSuchFieldException e) {
    		errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(nameToken), endPos(nameToken), nameToken.getText());
    	} catch (Exception e) {
    		errorWithCode(ParserErrorCode.InvalidExpression, startPos(valueToken), endPos(valueToken), nameToken.getText());
    	}
    }

    private void moveLevelUp() {
    	lastOverlay = current;
    	if (current != null)
    		current = (C4MapOverlay) current.getParentDeclaration();
    }

    private void assignOperator(String t) {
    	C4MapOverlay.Operator op = C4MapOverlay.Operator.valueOf(t.charAt(0));
    	lastOverlay.setOperator(op);
    }

    private IMarker createMarker(int start, int end, String message, int severity) {
    	if (mapCreator.getResource() == null) return null;
    	try {
    		IMarker marker = mapCreator.getResource().createMarker(IMarker.PROBLEM);
    		marker.setAttribute(IMarker.SEVERITY, severity);
    		marker.setAttribute(IMarker.TRANSIENT, false);
    		marker.setAttribute(IMarker.MESSAGE, message);
    		marker.setAttribute(IMarker.CHAR_START, start);
    		marker.setAttribute(IMarker.CHAR_END, end);
    		return marker;
    	} catch (CoreException e) {
    		e.printStackTrace();
    	}
    	return null;
    }

    private IMarker createErrorMarker(int start, int end, String message) {
    	return createMarker(start, end, message, IMarker.SEVERITY_ERROR);
    }

    /*
    private IMarker createWarningMarker(int start, int end, String message) {
    	return createMarker(start, end, message, IMarker.SEVERITY_WARNING);
    }
    */

    private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
    	String problem = code.getErrorString(args);
    	createErrorMarker(errorStart, errorEnd, problem);    	
    }

    private void deleteMarkers() {
    	try {
    		if (mapCreator.getResource() != null)
    			mapCreator.getResource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
    	} catch (CoreException e) {
    		e.printStackTrace();
    	}
    }

    @Override
    public void reportError(RecognitionException error) {
    	if (error.token.getText() != null)	
    		errorWithCode(ParserErrorCode.UnexpectedToken, startPos(error.token), endPos(error.token), error.token.getText());
    	super.reportError(error);
    }

    private void setBody(Token blockOpen, Token blockClose) {
    	if (current != null)
    		current.setBody(new SourceLocation(startPos(blockOpen), endPos(blockClose)));
    }

    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
    	// do nothing
    }

    public void parse() {
    	try {
    		start();
    	} catch (RecognitionException e) {
    		e.printStackTrace();
    	}
    }





    // $ANTLR start "start"
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:167:1: start : ( statement )* ;
    public final void start() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:167:7: ( ( statement )* )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:167:9: ( statement )*
            {
            deleteMarkers();
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:167:28: ( statement )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>=MAP && LA1_0<=OVERLAY)) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:167:28: statement
            	    {
            	    pushFollow(FOLLOW_statement_in_start33);
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
    // $ANTLR end "start"


    // $ANTLR start "statement"
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:169:1: statement : composition STATEMENTEND ;
    public final void statement() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:170:2: ( composition STATEMENTEND )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:170:4: composition STATEMENTEND
            {
            lastOverlay = null;
            pushFollow(FOLLOW_composition_in_statement45);
            composition();

            state._fsp--;

            match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_statement47); 

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
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:172:1: composition : subobject (op= OPERATOR composition )? ;
    public final void composition() throws RecognitionException {
        Token op=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:173:2: ( subobject (op= OPERATOR composition )? )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:173:4: subobject (op= OPERATOR composition )?
            {
            pushFollow(FOLLOW_subobject_in_composition56);
            subobject();

            state._fsp--;

            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:173:14: (op= OPERATOR composition )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==OPERATOR) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:173:15: op= OPERATOR composition
                    {
                    op=(Token)match(input,OPERATOR,FOLLOW_OPERATOR_in_composition61); 
                    assignOperator((op!=null?op.getText():null));
                    pushFollow(FOLLOW_composition_in_composition65);
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
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:175:1: subobject : (type= MAP (name= NAME )? optionalblock | type= OVERLAY (name= NAME )? optionalblock | template= NAME (name= NAME )? optionalblock );
    public final void subobject() throws RecognitionException {
        Token type=null;
        Token name=null;
        Token template=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:176:2: (type= MAP (name= NAME )? optionalblock | type= OVERLAY (name= NAME )? optionalblock | template= NAME (name= NAME )? optionalblock )
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
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:176:4: type= MAP (name= NAME )? optionalblock
                    {
                    type=(Token)match(input,MAP,FOLLOW_MAP_in_subobject78); 
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:176:17: (name= NAME )?
                    int alt3=2;
                    int LA3_0 = input.LA(1);

                    if ( (LA3_0==NAME) ) {
                        alt3=1;
                    }
                    switch (alt3) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:176:17: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject82); 

                            }
                            break;

                    }

                    createMapObject(type, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject87);
                    optionalblock();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:177:4: type= OVERLAY (name= NAME )? optionalblock
                    {
                    type=(Token)match(input,OVERLAY,FOLLOW_OVERLAY_in_subobject94); 
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:177:21: (name= NAME )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==NAME) ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:177:21: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject98); 

                            }
                            break;

                    }

                    createMapObject(type, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject103);
                    optionalblock();

                    state._fsp--;


                    }
                    break;
                case 3 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:178:4: template= NAME (name= NAME )? optionalblock
                    {
                    template=(Token)match(input,NAME,FOLLOW_NAME_in_subobject110); 
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:178:22: (name= NAME )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==NAME) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:178:22: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject114); 

                            }
                            break;

                    }

                    createMapObject(template, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject119);
                    optionalblock();

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


    // $ANTLR start "optionalblock"
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:180:1: optionalblock : ( block )? ;
    public final void optionalblock() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:181:2: ( ( block )? )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:181:4: ( block )?
            {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:181:4: ( block )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==BLOCKOPEN) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:181:4: block
                    {
                    pushFollow(FOLLOW_block_in_optionalblock128);
                    block();

                    state._fsp--;


                    }
                    break;

            }

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
    // $ANTLR end "optionalblock"


    // $ANTLR start "block"
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:183:1: block : open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE ;
    public final void block() throws RecognitionException {
        Token open=null;
        Token close=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:183:7: (open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE )
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:183:9: open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE
            {
            open=(Token)match(input,BLOCKOPEN,FOLLOW_BLOCKOPEN_in_block142); 
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:183:24: ( statementorattrib )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>=MAP && LA8_0<=OVERLAY)) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:183:24: statementorattrib
            	    {
            	    pushFollow(FOLLOW_statementorattrib_in_block144);
            	    statementorattrib();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);

            close=(Token)match(input,BLOCKCLOSE,FOLLOW_BLOCKCLOSE_in_block149); 
            setBody(open, close);

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
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:185:1: statementorattrib : ( attribute | statement );
    public final void statementorattrib() throws RecognitionException {
        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:186:2: ( attribute | statement )
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==NAME) ) {
                int LA9_1 = input.LA(2);

                if ( (LA9_1==ASSIGN) ) {
                    alt9=1;
                }
                else if ( ((LA9_1>=STATEMENTEND && LA9_1<=OPERATOR)||LA9_1==NAME||LA9_1==BLOCKOPEN) ) {
                    alt9=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA9_0==MAP||LA9_0==OVERLAY) ) {
                alt9=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:186:4: attribute
                    {
                    pushFollow(FOLLOW_attribute_in_statementorattrib160);
                    attribute();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:186:14: statement
                    {
                    pushFollow(FOLLOW_statement_in_statementorattrib162);
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
    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:188:1: attribute : (attr= NAME ASSIGN attrValue= NAME STATEMENTEND | attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND | attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND );
    public final void attribute() throws RecognitionException {
        Token attr=null;
        Token attrValue=null;

        try {
            // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:189:2: (attr= NAME ASSIGN attrValue= NAME STATEMENTEND | attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND | attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND )
            int alt10=3;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==NAME) ) {
                int LA10_1 = input.LA(2);

                if ( (LA10_1==ASSIGN) ) {
                    switch ( input.LA(3) ) {
                    case NAME:
                        {
                        alt10=1;
                        }
                        break;
                    case NUMBER:
                        {
                        alt10=2;
                        }
                        break;
                    case MATCOMBO:
                        {
                        alt10=3;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("", 10, 2, input);

                        throw nvae;
                    }

                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 10, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:189:4: attr= NAME ASSIGN attrValue= NAME STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute173); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute175); 
                    attrValue=(Token)match(input,NAME,FOLLOW_NAME_in_attribute179); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute181); 
                    setVal(attr, attrValue);

                    }
                    break;
                case 2 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:190:4: attr= NAME ASSIGN attrValue= NUMBER STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute190); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute192); 
                    attrValue=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_attribute196); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute198); 
                    setVal(attr, attrValue);

                    }
                    break;
                case 3 :
                    // C:\\Users\\Madeen\\Projects\\Clonk\\C4DT\\Clonk\\src\\net\\arctics\\clonk\\parser\\mapcreator\\MapCreator.g:191:4: attr= NAME ASSIGN attrValue= MATCOMBO STATEMENTEND
                    {
                    attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute207); 
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute209); 
                    attrValue=(Token)match(input,MATCOMBO,FOLLOW_MATCOMBO_in_attribute213); 
                    match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute215); 
                    setVal(attr, attrValue);

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


 

    public static final BitSet FOLLOW_statement_in_start33 = new BitSet(new long[]{0x00000000000001C2L});
    public static final BitSet FOLLOW_composition_in_statement45 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_statement47 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_subobject_in_composition56 = new BitSet(new long[]{0x0000000000000022L});
    public static final BitSet FOLLOW_OPERATOR_in_composition61 = new BitSet(new long[]{0x00000000000001C0L});
    public static final BitSet FOLLOW_composition_in_composition65 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MAP_in_subobject78 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject82 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_optionalblock_in_subobject87 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_OVERLAY_in_subobject94 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject98 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_optionalblock_in_subobject103 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_subobject110 = new BitSet(new long[]{0x0000000000000280L});
    public static final BitSet FOLLOW_NAME_in_subobject114 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_optionalblock_in_subobject119 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_block_in_optionalblock128 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BLOCKOPEN_in_block142 = new BitSet(new long[]{0x00000000000005C0L});
    public static final BitSet FOLLOW_statementorattrib_in_block144 = new BitSet(new long[]{0x00000000000005C0L});
    public static final BitSet FOLLOW_BLOCKCLOSE_in_block149 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_statementorattrib160 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_statement_in_statementorattrib162 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute173 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute175 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_NAME_in_attribute179 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute181 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute190 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute192 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_NUMBER_in_attribute196 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute198 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute207 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute209 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_MATCOMBO_in_attribute213 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute215 = new BitSet(new long[]{0x0000000000000002L});

}