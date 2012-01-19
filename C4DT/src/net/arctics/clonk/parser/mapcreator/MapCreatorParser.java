// $ANTLR 3.2 Sep 23, 2009 12:02:23 /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g 2010-02-22 05:35:05

package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.resource.c4group.C4GroupItem;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.io.InputStreamReader;


import org.antlr.runtime.*;

public class MapCreatorParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "STATEMENTEND", "OPERATOR", "MAP", "NAME", "OVERLAY", "POINT", "BLOCKOPEN", "BLOCKCLOSE", "ASSIGN", "NUMBER", "MINUS", "MATCOMBO", "LETTER", "UNIT", "DIGIT", "WORD", "PLUS", "WS", "SLCOMMENT", "MLCOMMENT"
    };
    public static final int OVERLAY=8;
    public static final int POINT=9;
    public static final int STATEMENTEND=4;
    public static final int LETTER=16;
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


        public MapCreatorParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public MapCreatorParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    @Override
	public String[] getTokenNames() { return MapCreatorParser.tokenNames; }
    @Override
	public String getGrammarFileName() { return "/Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g"; }


    MapCreator mapCreator;
    MapOverlayBase current;
    MapOverlayBase lastOverlay;

    Token valueLo, valueHi;
    private boolean createMarkers;

    public MapCreatorParser(MapCreator mapCreator, TokenStream input) {
    	this(input);
    	this.mapCreator = mapCreator;
    	createMarkers = mapCreator.resource() == null || C4GroupItem.getGroupItemBackingResource(mapCreator.resource()) == null;
    	this.current = mapCreator;
    }

    public MapCreatorParser(MapCreator mapCreator) {
    	this (mapCreator, getTokenStream(mapCreator));
    }

    private static TokenStream getTokenStream(MapCreator mapCreator) {
    	CharStream charStream;
    	try {
    		charStream = new ANTLRReaderStream(new InputStreamReader(((IFile)mapCreator.resource()).getContents()));
    		MapCreatorLexer lexer = new MapCreatorLexer(charStream);
    		CommonTokenStream tokenStream = new CommonTokenStream();
    		tokenStream.setTokenSource(lexer);
    		return tokenStream;
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	} catch (CoreException e) {
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

    private void setCurrentOverlay(MapOverlayBase overlay, Token typeToken, Token nameToken) {
    	current = overlay;
    	current.setLocation(new SourceLocation(startPos(typeToken), endPos(nameToken, typeToken)));
    }

    private void createMapObject(Token typeToken, Token nameToken) {
    	try {
    		if (current instanceof MapOverlay) {
    			MapOverlayBase newOverlay = ((MapOverlay) current).createOverlay(typeToken.getText(), nameToken!=null?nameToken.getText():null);
    			if (newOverlay == null)
    				errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(typeToken), endPos(typeToken), typeToken.getText());
    			else
    				setCurrentOverlay(newOverlay, typeToken, nameToken);
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private void setVal(Token nameToken, Token valueTokenLo, Token valueTokenHi) {
    	try {
    		if (valueTokenLo == null)
    			errorWithCode(ParserErrorCode.ExpressionExpected, endPos(nameToken), endPos(nameToken)+1);
    		else
    			current.setAttribute(nameToken.getText(), valueTokenLo.getText(), valueTokenHi != null ? valueTokenHi.getText() : null);
    	} catch (NoSuchFieldException e) {
    		errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(nameToken), endPos(nameToken), nameToken.getText());
    	} catch (Exception e) {
    		errorWithCode(ParserErrorCode.InvalidExpression, startPos(valueTokenLo), endPos(valueTokenLo), nameToken.getText());
    	}
    }

    private void moveLevelUp() {
    	lastOverlay = current;
    	if (current != null)
    		current = (MapOverlay) current.getParentDeclaration();
    }

    private void assignOperator(String t) {
    	MapOverlay.Operator op = MapOverlay.Operator.valueOf(t.charAt(0));
    	if (lastOverlay instanceof MapOverlay)
    	((MapOverlay)lastOverlay).setOperator(op);
    }

    private IMarker createMarker(int start, int end, String message, int severity) {
    	if (!createMarkers || mapCreator.resource() == null) return null;
    	try {
    		IMarker marker = mapCreator.resource().createMarker(IMarker.PROBLEM);
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
    		if (mapCreator.resource() != null)
    			mapCreator.resource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:181:1: start : ( statement )* ;
    public final void start() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:181:7: ( ( statement )* )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:181:9: ( statement )*
            {
            deleteMarkers();
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:181:28: ( statement )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>=MAP && LA1_0<=POINT)) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:181:28: statement
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:183:1: statement : composition STATEMENTEND ;
    public final void statement() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:184:2: ( composition STATEMENTEND )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:184:4: composition STATEMENTEND
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:187:1: composition : subobject (op= OPERATOR composition )? ;
    public final void composition() throws RecognitionException {
        Token op=null;

        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:188:2: ( subobject (op= OPERATOR composition )? )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:188:4: subobject (op= OPERATOR composition )?
            {
            pushFollow(FOLLOW_subobject_in_composition57);
            subobject();

            state._fsp--;

            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:188:14: (op= OPERATOR composition )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==OPERATOR) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:188:15: op= OPERATOR composition
                    {
                    op=(Token)match(input,OPERATOR,FOLLOW_OPERATOR_in_composition62); 
                    assignOperator((op!=null?op.getText():null));
                    pushFollow(FOLLOW_composition_in_composition66);
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:190:1: subobject : (type= MAP (name= NAME )? optionalblock | type= OVERLAY (name= NAME )? optionalblock | type= POINT (name= NAME )? optionalblock | template= NAME (name= NAME )? optionalblock );
    public final void subobject() throws RecognitionException {
        Token type=null;
        Token name=null;
        Token template=null;

        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:191:2: (type= MAP (name= NAME )? optionalblock | type= OVERLAY (name= NAME )? optionalblock | type= POINT (name= NAME )? optionalblock | template= NAME (name= NAME )? optionalblock )
            int alt7=4;
            switch ( input.LA(1) ) {
            case MAP:
                {
                alt7=1;
                }
                break;
            case OVERLAY:
                {
                alt7=2;
                }
                break;
            case POINT:
                {
                alt7=3;
                }
                break;
            case NAME:
                {
                alt7=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;
            }

            switch (alt7) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:191:4: type= MAP (name= NAME )? optionalblock
                    {
                    type=(Token)match(input,MAP,FOLLOW_MAP_in_subobject79); 
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:191:17: (name= NAME )?
                    int alt3=2;
                    int LA3_0 = input.LA(1);

                    if ( (LA3_0==NAME) ) {
                        alt3=1;
                    }
                    switch (alt3) {
                        case 1 :
                            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:191:17: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject83); 

                            }
                            break;

                    }

                    createMapObject(type, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject88);
                    optionalblock();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:192:4: type= OVERLAY (name= NAME )? optionalblock
                    {
                    type=(Token)match(input,OVERLAY,FOLLOW_OVERLAY_in_subobject95); 
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:192:21: (name= NAME )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==NAME) ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:192:21: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject99); 

                            }
                            break;

                    }

                    createMapObject(type, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject104);
                    optionalblock();

                    state._fsp--;


                    }
                    break;
                case 3 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:193:4: type= POINT (name= NAME )? optionalblock
                    {
                    type=(Token)match(input,POINT,FOLLOW_POINT_in_subobject111); 
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:193:19: (name= NAME )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==NAME) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:193:19: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject115); 

                            }
                            break;

                    }

                    createMapObject(type, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject120);
                    optionalblock();

                    state._fsp--;


                    }
                    break;
                case 4 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:194:4: template= NAME (name= NAME )? optionalblock
                    {
                    template=(Token)match(input,NAME,FOLLOW_NAME_in_subobject127); 
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:194:22: (name= NAME )?
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==NAME) ) {
                        alt6=1;
                    }
                    switch (alt6) {
                        case 1 :
                            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:194:22: name= NAME
                            {
                            name=(Token)match(input,NAME,FOLLOW_NAME_in_subobject131); 

                            }
                            break;

                    }

                    createMapObject(template, name);
                    pushFollow(FOLLOW_optionalblock_in_subobject136);
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:196:1: optionalblock : ( block )? ;
    public final void optionalblock() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:2: ( ( block )? )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:4: ( block )?
            {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:4: ( block )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==BLOCKOPEN) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:197:4: block
                    {
                    pushFollow(FOLLOW_block_in_optionalblock145);
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:1: block : open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE ;
    public final void block() throws RecognitionException {
        Token open=null;
        Token close=null;

        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:7: (open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:9: open= BLOCKOPEN ( statementorattrib )* close= BLOCKCLOSE
            {
            open=(Token)match(input,BLOCKOPEN,FOLLOW_BLOCKOPEN_in_block161); 
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:24: ( statementorattrib )*
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( ((LA9_0>=MAP && LA9_0<=POINT)) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:200:24: statementorattrib
            	    {
            	    pushFollow(FOLLOW_statementorattrib_in_block163);
            	    statementorattrib();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop9;
                }
            } while (true);

            close=(Token)match(input,BLOCKCLOSE,FOLLOW_BLOCKCLOSE_in_block168); 
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:202:1: statementorattrib : ( attribute | statement );
    public final void statementorattrib() throws RecognitionException {
        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:2: ( attribute | statement )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==NAME) ) {
                int LA10_1 = input.LA(2);

                if ( (LA10_1==ASSIGN) ) {
                    alt10=1;
                }
                else if ( ((LA10_1>=STATEMENTEND && LA10_1<=OPERATOR)||LA10_1==NAME||LA10_1==BLOCKOPEN) ) {
                    alt10=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 10, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA10_0==MAP||(LA10_0>=OVERLAY && LA10_0<=POINT)) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:4: attribute
                    {
                    pushFollow(FOLLOW_attribute_in_statementorattrib179);
                    attribute();

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:203:14: statement
                    {
                    pushFollow(FOLLOW_statement_in_statementorattrib181);
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
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:205:1: attribute : attr= NAME ASSIGN value STATEMENTEND ;
    public final void attribute() throws RecognitionException {
        Token attr=null;

        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:2: (attr= NAME ASSIGN value STATEMENTEND )
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:206:4: attr= NAME ASSIGN value STATEMENTEND
            {
            valueLo=valueHi=null;
            attr=(Token)match(input,NAME,FOLLOW_NAME_in_attribute194); 
            match(input,ASSIGN,FOLLOW_ASSIGN_in_attribute196); 
            pushFollow(FOLLOW_value_in_attribute198);
            value();

            state._fsp--;

            match(input,STATEMENTEND,FOLLOW_STATEMENTEND_in_attribute200); 
            setVal(attr, valueLo, valueHi);

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


    // $ANTLR start "value"
    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:1: value : (valName= NAME | (valLo= NUMBER ( MINUS valHi= NUMBER )? ) | valMat= MATCOMBO );
    public final void value() throws RecognitionException {
        Token valName=null;
        Token valLo=null;
        Token valHi=null;
        Token valMat=null;

        try {
            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:7: (valName= NAME | (valLo= NUMBER ( MINUS valHi= NUMBER )? ) | valMat= MATCOMBO )
            int alt12=3;
            switch ( input.LA(1) ) {
            case NAME:
                {
                alt12=1;
                }
                break;
            case NUMBER:
                {
                alt12=2;
                }
                break;
            case MATCOMBO:
                {
                alt12=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;
            }

            switch (alt12) {
                case 1 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:9: valName= NAME
                    {
                    valName=(Token)match(input,NAME,FOLLOW_NAME_in_value212); 
                    valueLo=valName;

                    }
                    break;
                case 2 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:43: (valLo= NUMBER ( MINUS valHi= NUMBER )? )
                    {
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:43: (valLo= NUMBER ( MINUS valHi= NUMBER )? )
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:44: valLo= NUMBER ( MINUS valHi= NUMBER )?
                    {
                    valLo=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_value221); 
                    valueLo=valLo;
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:74: ( MINUS valHi= NUMBER )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0==MINUS) ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:75: MINUS valHi= NUMBER
                            {
                            match(input,MINUS,FOLLOW_MINUS_in_value226); 
                            valHi=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_value230); 
                            valueHi=valHi;

                            }
                            break;

                    }


                    }


                    }
                    break;
                case 3 :
                    // /Users/madeen/Projects/Clonk/C4DT/C4DT/src/net/arctics/clonk/parser/mapcreator/MapCreator.g:208:118: valMat= MATCOMBO
                    {
                    valMat=(Token)match(input,MATCOMBO,FOLLOW_MATCOMBO_in_value243); 
                    valueLo=valMat;

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
    // $ANTLR end "value"

    // Delegated rules


 

    public static final BitSet FOLLOW_statement_in_start33 = new BitSet(new long[]{0x00000000000003C2L});
    public static final BitSet FOLLOW_composition_in_statement45 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_statement47 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_subobject_in_composition57 = new BitSet(new long[]{0x0000000000000022L});
    public static final BitSet FOLLOW_OPERATOR_in_composition62 = new BitSet(new long[]{0x00000000000003C0L});
    public static final BitSet FOLLOW_composition_in_composition66 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MAP_in_subobject79 = new BitSet(new long[]{0x0000000000000480L});
    public static final BitSet FOLLOW_NAME_in_subobject83 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_optionalblock_in_subobject88 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_OVERLAY_in_subobject95 = new BitSet(new long[]{0x0000000000000480L});
    public static final BitSet FOLLOW_NAME_in_subobject99 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_optionalblock_in_subobject104 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POINT_in_subobject111 = new BitSet(new long[]{0x0000000000000480L});
    public static final BitSet FOLLOW_NAME_in_subobject115 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_optionalblock_in_subobject120 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_subobject127 = new BitSet(new long[]{0x0000000000000480L});
    public static final BitSet FOLLOW_NAME_in_subobject131 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_optionalblock_in_subobject136 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_block_in_optionalblock145 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BLOCKOPEN_in_block161 = new BitSet(new long[]{0x0000000000000BC0L});
    public static final BitSet FOLLOW_statementorattrib_in_block163 = new BitSet(new long[]{0x0000000000000BC0L});
    public static final BitSet FOLLOW_BLOCKCLOSE_in_block168 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_statementorattrib179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_statement_in_statementorattrib181 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_attribute194 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_ASSIGN_in_attribute196 = new BitSet(new long[]{0x000000000000A080L});
    public static final BitSet FOLLOW_value_in_attribute198 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_STATEMENTEND_in_attribute200 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_value212 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_value221 = new BitSet(new long[]{0x0000000000004002L});
    public static final BitSet FOLLOW_MINUS_in_value226 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_NUMBER_in_value230 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MATCOMBO_in_value243 = new BitSet(new long[]{0x0000000000000002L});

}