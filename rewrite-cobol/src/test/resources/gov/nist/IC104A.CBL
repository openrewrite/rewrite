      *HEADER,COBOL,IC103A,SUBRTN,IC104A                                              
000100 IDENTIFICATION DIVISION.                                         IC1044.2
000200 PROGRAM-ID.                                                      IC1044.2
000300     IC104A.                                                      IC1044.2
000400****************************************************************  IC1044.2
000500*                                                              *  IC1044.2
000600*    VALIDATION FOR:-                                          *  IC1044.2
000700*                                                              *  IC1044.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1044.2
000900*                                                              *  IC1044.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1044.2
001100*                                                              *  IC1044.2
001200****************************************************************  IC1044.2
001300*                                                              *  IC1044.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1044.2
001500*                                                              *  IC1044.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1044.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1044.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1044.2
001900*                                                              *  IC1044.2
002000****************************************************************  IC1044.2
002100*        THE SUBPROGRAM IC104 HAS THREE OPERANDS IN THE           IC1044.2
002200*    USING PHRASE OF THE PROCEDURE DIVISION HEADER.  TWO          IC1044.2
002300*    OPERANDS ARE 01 GROUP ITEMS AND THE THIRD OPERAND IS         IC1044.2
002400*    AN ELEMENTARY 77 ITEM.  THE DATA DESCRIPTIONS OF THESE       IC1044.2
002500*    OPERANDS IN THE LINKAGE SECTION ARE NOT THE SAME AS THE      IC1044.2
002600*    DATA DESCRIPTIONS IN THE WORKING-STORAGE SECTION OF THE      IC1044.2
002700*    CALLING PROGRAM, BUT AN EQUAL NUMBER OF CHARACTER            IC1044.2
002800*    POSITIONS ARE DEFINED.  THE CALLING PROGRAM IS IC103.        IC1044.2
002900 ENVIRONMENT DIVISION.                                            IC1044.2
003000 CONFIGURATION SECTION.                                           IC1044.2
003100 SOURCE-COMPUTER.                                                 IC1044.2
003200     XXXXX082.                                                    IC1044.2
003300 OBJECT-COMPUTER.                                                 IC1044.2
003400     XXXXX083.                                                    IC1044.2
003500 INPUT-OUTPUT SECTION.                                            IC1044.2
003600 FILE-CONTROL.                                                    IC1044.2
003700     SELECT PRINT-FILE ASSIGN TO                                  IC1044.2
003800     XXXXX055.                                                    IC1044.2
003900 DATA DIVISION.                                                   IC1044.2
004000 FILE SECTION.                                                    IC1044.2
004100 FD  PRINT-FILE.                                                  IC1044.2
004200 01  PRINT-REC PICTURE X(120).                                    IC1044.2
004300 01  DUMMY-RECORD PICTURE X(120).                                 IC1044.2
004400 WORKING-STORAGE SECTION.                                         IC1044.2
004500 01  CONSTANT-VALUES.                                             IC1044.2
004600     02  AN-CONSTANT PIC X(5) VALUE "IC104".                      IC1044.2
004700     02  NUM-CONSTANT PIC 99V9999 VALUE 0.7654.                   IC1044.2
004800 LINKAGE SECTION.                                                 IC1044.2
004900 01  GRP-01.                                                      IC1044.2
005000     02  AN-FIELD PICTURE X(5).                                   IC1044.2
005100     02  NUM-DISPLAY PIC 99.                                      IC1044.2
005200     02  GRP-LEVEL.                                               IC1044.2
005300         03  A-FIELD PICTURE A(3).                                IC1044.2
005400 77  ELEM-01 PIC  V9(4) COMPUTATIONAL.                            IC1044.2
005500 01  GRP-02.                                                      IC1044.2
005600     02  GRP-03.                                                  IC1044.2
005700         03  NUM-ITEM PICTURE S99.                                IC1044.2
005800         03  EDITED-FIELD  PIC XXBX0X.                            IC1044.2
005900 PROCEDURE DIVISION USING GRP-01 ELEM-01 GRP-02.                  IC1044.2
006000 SECT-IC104-0001 SECTION.                                         IC1044.2
006100*        THIS SECTION SETS THE PARAMETER FIELDS REFERRED TO       IC1044.2
006200*    IN THE USING PHRASE AND DEFINED IN THE LINKAGE SECTION.      IC1044.2
006300 CALL-TEST-06.                                                    IC1044.2
006400     MOVE AN-CONSTANT TO AN-FIELD.                                IC1044.2
006500     ADD 25 TO NUM-DISPLAY.                                       IC1044.2
006600     MOVE "YES" TO A-FIELD.                                       IC1044.2
006700     MOVE NUM-CONSTANT TO ELEM-01.                                IC1044.2
006800     MOVE NUM-DISPLAY TO NUM-ITEM.                                IC1044.2
006900     MOVE "ABCD" TO EDITED-FIELD.                                 IC1044.2
007000 CALL-EXIT-06.                                                    IC1044.2
007100     EXIT PROGRAM.                                                IC1044.2
      *END-OF,IC104A                                                                  
