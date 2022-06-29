      *HEADER,COBOL,IX302M                                                            
000100 IDENTIFICATION DIVISION.                                         IX3024.2
000200 PROGRAM-ID.                                                      IX3024.2
000300     IX302M.                                                      IX3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      IX3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN INTERMEDIATE SUBSET INDEXED   IX3024.2
000600*INPUT-OUTPUT.                                                    IX3024.2
000700 ENVIRONMENT DIVISION.                                            IX3024.2
000800 CONFIGURATION SECTION.                                           IX3024.2
000900 SOURCE-COMPUTER.                                                 IX3024.2
001000     XXXXX082.                                                    IX3024.2
001100 OBJECT-COMPUTER.                                                 IX3024.2
001200     XXXXX083.                                                    IX3024.2
001300 INPUT-OUTPUT SECTION.                                            IX3024.2
001400 FILE-CONTROL.                                                    IX3024.2
001500     SELECT TFIL ASSIGN                                           IX3024.2
001600     XXXXX024                                                     IX3024.2
001700         ORGANIZATION IS INDEXED                                  IX3024.2
001800         ACCESS MODE IS SEQUENTIAL                                IX3024.2
001900         RECORD KEY IS RKEY.                                      IX3024.2
002000                                                                  IX3024.2
002100     SELECT SQ-FRR ASSIGN                                         IX3024.2
002200     XXXXX013.                                                    IX3024.2
002300                                                                  IX3024.2
002400                                                                  IX3024.2
002500     SELECT RR-FS1 ASSIGN                                         IX3024.2
002600     XXXXX024                                                     IX3024.2
002700         ORGANIZATION IS INDEXED                                  IX3024.2
002800         RECORD KEY IS FKEY.                                      IX3024.2
002900 I-O-CONTROL.                                                     IX3024.2
003000     XXXXX053.                                                    IX3024.2
003100*Message expected for above statement: OBSOLETE                   IX3024.2
003200                                                                  IX3024.2
003300 DATA DIVISION.                                                   IX3024.2
003400 FILE SECTION.                                                    IX3024.2
003500 FD TFIL                                                          IX3024.2
003600     LABEL RECORDS STANDARD                                       IX3024.2
003700*Message expected for above statement: OBSOLETE                   IX3024.2
003800                                                                  IX3024.2
003900     VALUE OF                                                     IX3024.2
004000     XXXXX074                                                     IX3024.2
004100     IS                                                           IX3024.2
004200     XXXXX075                                                     IX3024.2
004300*Message expected for above statement: OBSOLETE                   IX3024.2
004400                                                                  IX3024.2
004500     DATA RECORDS ARE FREC.                                       IX3024.2
004600*Message expected for above statement: OBSOLETE                   IX3024.2
004700                                                                  IX3024.2
004800 01 FREC.                                                         IX3024.2
004900     03 RKEY PIC X(8).                                            IX3024.2
005000                                                                  IX3024.2
005100 FD SQ-FRR.                                                       IX3024.2
005200    01 SREC.                                                      IX3024.2
005300     03 SKEY PIC X(8).                                            IX3024.2
005400                                                                  IX3024.2
005500                                                                  IX3024.2
005600 FD RR-FS1.                                                       IX3024.2
005700    01 RREC.                                                      IX3024.2
005800     03 FKEY PIC X(8).                                            IX3024.2
005900                                                                  IX3024.2
006000 WORKING-STORAGE SECTION.                                         IX3024.2
006100 01 VARIABLES.                                                    IX3024.2
006200     03 VKEY PIC 9(8) VALUE ZERO.                                 IX3024.2
006300     03 DKEY PIC 9(8) VALUE ZERO.                                 IX3024.2
006400                                                                  IX3024.2
006500 PROCEDURE DIVISION.                                              IX3024.2
006600                                                                  IX3024.2
006700 IX302M-CONTROL.                                                  IX3024.2
006800     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         IX3024.2
006900     STOP RUN.                                                    IX3024.2
007000                                                                  IX3024.2
007100*TOTAL NUMBER OF FLAGS EXPECTED = 4.                              IX3024.2
      *END-OF,IX302M                                                                  
