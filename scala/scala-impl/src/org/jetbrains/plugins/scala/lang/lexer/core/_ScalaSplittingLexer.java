/* The following code was generated by JFlex 1.7.0 tweaked for IntelliJ platform */

package org.jetbrains.plugins.scala.lang.lexer.core;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypesEx;
import org.jetbrains.plugins.scala.lang.scaladoc.parser.ScalaDocElementTypes;
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.7.0
 * from the specification file <tt>_ScalaSplittingLexer.flex</tt>
 */
public class _ScalaSplittingLexer implements FlexLexer, ScalaTokenTypesEx, ScalaDocTokenType {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int IN_BLOCK_COMMENT = 2;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0,  0,  1, 1
  };

  /** 
   * Translates characters to character classes
   * Chosen bits are [8, 6, 7]
   * Total runtime size is 1040 bytes
   */
  public static int ZZ_CMAP(int ch) {
    return ZZ_CMAP_A[ZZ_CMAP_Y[ZZ_CMAP_Z[ch>>13]|((ch>>7)&0x3f)]|(ch&0x7f)];
  }

  /* The ZZ_CMAP_Z table has 136 entries */
  static final char ZZ_CMAP_Z[] = zzUnpackCMap(
    "\1\0\207\100");

  /* The ZZ_CMAP_Y table has 128 entries */
  static final char ZZ_CMAP_Y[] = zzUnpackCMap(
    "\1\0\177\200");

  /* The ZZ_CMAP_A table has 256 entries */
  static final char ZZ_CMAP_A[] = zzUnpackCMap(
    "\12\0\1\6\2\0\1\6\23\0\1\15\1\13\1\14\3\0\1\12\2\0\1\2\4\0\1\1\1\10\7\3\2"+
    "\4\1\16\6\0\1\11\5\4\25\0\1\5\3\0\1\17\6\4\16\0\1\7\212\0");

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\2\0\7\1\3\2\1\3\1\4\2\0\3\1\3\0"+
    "\1\5\1\6\1\7\5\0\1\10\4\0\1\1\7\0"+
    "\1\1\3\0\1\1";

  private static int [] zzUnpackAction() {
    int [] result = new int[48];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\20\0\40\0\60\0\100\0\120\0\140\0\160"+
    "\0\200\0\40\0\220\0\240\0\260\0\300\0\320\0\340"+
    "\0\360\0\u0100\0\u0110\0\u0120\0\u0130\0\200\0\40\0\40"+
    "\0\u0140\0\u0150\0\u0160\0\u0170\0\u0180\0\u0190\0\40\0\u01a0"+
    "\0\u01b0\0\u01c0\0\u01d0\0\u0120\0\u01e0\0\u01f0\0\u0200\0\u0210"+
    "\0\u0220\0\u0230\0\u0240\0\u0250\0\u0260\0\u0270\0\u0280\0\u01e0";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[48];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\3\1\4\10\3\1\5\1\6\1\7\1\3\1\10"+
    "\1\11\1\12\1\13\1\14\15\12\21\0\1\15\1\16"+
    "\15\0\5\17\1\20\1\0\3\17\1\0\5\17\5\21"+
    "\1\22\1\0\4\21\1\23\4\21\15\0\1\24\20\0"+
    "\1\25\1\0\17\26\1\3\2\0\1\27\16\0\1\30"+
    "\16\0\6\15\1\0\11\15\2\0\1\31\22\0\1\3"+
    "\4\0\1\3\5\0\3\17\1\32\2\17\1\0\1\33"+
    "\1\32\7\17\5\21\1\22\1\0\4\21\1\3\12\21"+
    "\1\0\11\21\13\0\1\34\4\0\15\24\1\35\2\24"+
    "\14\0\1\36\4\0\1\37\21\0\1\40\1\0\1\3"+
    "\2\0\1\40\1\0\1\3\10\0\2\41\1\3\2\0"+
    "\1\42\1\41\1\3\5\0\13\34\1\43\4\34\14\24"+
    "\1\44\1\35\2\24\15\0\1\45\5\0\1\17\1\0"+
    "\1\3\2\0\1\17\1\0\1\3\10\0\2\46\3\0"+
    "\2\46\11\0\2\46\3\0\1\47\1\46\6\0\13\34"+
    "\1\50\4\34\16\45\1\51\1\45\3\0\2\52\3\0"+
    "\2\52\11\0\2\52\3\0\1\53\1\52\6\0\13\34"+
    "\1\54\4\34\16\45\1\55\1\45\3\0\2\17\3\0"+
    "\2\17\11\0\2\17\3\0\1\17\1\56\21\0\1\54"+
    "\4\0\15\45\1\57\1\55\1\45\12\0\1\3\5\0"+
    "\14\45\1\60\1\45\1\51\1\45";

  private static int [] zzUnpackTrans() {
    int [] result = new int[656];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\2\0\1\11\6\1\1\11\4\1\2\0\3\1\3\0"+
    "\2\11\1\1\5\0\1\11\4\0\1\1\7\0\1\1"+
    "\3\0\1\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[48];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */

  private int braceCount = 0;
  private IElementType commentType = tBLOCK_COMMENT;

  public void resetCustom() {
    braceCount = 0;
    commentType = tBLOCK_COMMENT;
  }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public _ScalaSplittingLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    int size = 0;
    for (int i = 0, length = packed.length(); i < length; i += 2) {
      size += packed.charAt(i);
    }
    char[] map = new char[size];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < packed.length()) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }

  @Override
  public final int getTokenStart() {
    return zzStartRead;
  }

  @Override
  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  @Override
  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;

    // !!!! NOTE !!!!
    // When updating the skeleton from IntelliJ repository, do not forget to add this method call
    resetCustom();

    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  @Override
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  @Override
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() {
    if (!zzEOFDone) {
      zzEOFDone = true;
    
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/*, zzEndReadL*/);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/*, zzEndReadL*/);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + ZZ_CMAP(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        zzDoEOF();
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1: 
            { return SCALA_PLAIN_CONTENT;
            } 
            // fall through
          case 9: break;
          case 2: 
            { return(commentType);
            } 
            // fall through
          case 10: break;
          case 3: 
            { return tLINE_COMMENT;
            } 
            // fall through
          case 11: break;
          case 4: 
            { commentType = tBLOCK_COMMENT;
                            braceCount++;
                            yybegin(IN_BLOCK_COMMENT);
                            return(commentType);
            } 
            // fall through
          case 12: break;
          case 5: 
            { braceCount++;
                            return(commentType);
            } 
            // fall through
          case 13: break;
          case 6: 
            { braceCount--;
                            if (braceCount == 0) {
                              yybegin(YYINITIAL);
                            }
                            return(commentType);
            } 
            // fall through
          case 14: break;
          case 7: 
            { commentType = ScalaDocElementTypes.SCALA_DOC_COMMENT;
                            braceCount++;
                            yybegin(IN_BLOCK_COMMENT);
                            return(commentType);
            } 
            // fall through
          case 15: break;
          case 8: 
            { return tBLOCK_COMMENT;
            } 
            // fall through
          case 16: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
