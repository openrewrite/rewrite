      *HEADER,COBOL,SQ302M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ3024.2
000200 PROGRAM-ID.                                                      SQ3024.2
000300     SQ302M.                                                      SQ3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      SQ3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN MINIMUM SUBSET                SQ3024.2
000600*SEQUENTIAL INPUT-OUTPUT.                                         SQ3024.2
000700 ENVIRONMENT DIVISION.                                            SQ3024.2
000800 CONFIGURATION SECTION.                                           SQ3024.2
000900 SOURCE-COMPUTER.                                                 SQ3024.2
001000     XXXXX082.                                                    SQ3024.2
001100 OBJECT-COMPUTER.                                                 SQ3024.2
001200     XXXXX083.                                                    SQ3024.2
001300 INPUT-OUTPUT SECTION.                                            SQ3024.2
001400 FILE-CONTROL.                                                    SQ3024.2
001500     SELECT TFIL ASSIGN                                           SQ3024.2
001600     XXXXX014                                                     SQ3024.2
001700     ORGANIZATION IS SEQUENTIAL                                   SQ3024.2
001800     ACCESS MODE IS SEQUENTIAL.                                   SQ3024.2
001900                                                                  SQ3024.2
002000     SELECT SQ-FRR ASSIGN                                         SQ3024.2
002100     XXXXX013                                                     SQ3024.2
002200         ORGANIZATION IS SEQUENTIAL.                              SQ3024.2
002300                                                                  SQ3024.2
002400     SELECT RR-FS1 ASSIGN                                         SQ3024.2
002500     XXXXX014                                                     SQ3024.2
002600         ORGANIZATION IS SEQUENTIAL.                              SQ3024.2
002700                                                                  SQ3024.2
002800 I-O-CONTROL.                                                     SQ3024.2
002900     XXXXX053.                                                    SQ3024.2
003000*Message expected for above statement: OBSOLETE                   SQ3024.2
003100                                                                  SQ3024.2
003200 DATA DIVISION.                                                   SQ3024.2
003300 FILE SECTION.                                                    SQ3024.2
003400 FD TFIL                                                          SQ3024.2
003500     LABEL RECORDS STANDARD                                       SQ3024.2
003600*Message expected for above statement: OBSOLETE                   SQ3024.2
003700     VALUE OF                                                     SQ3024.2
003800     XXXXX074                                                     SQ3024.2
003900     IS                                                           SQ3024.2
004000     XXXXX075                                                     SQ3024.2
004100*Message expected for above statement: OBSOLETE                   SQ3024.2
004200     DATA RECORDS ARE FREC.                                       SQ3024.2
004300*Message expected for above statement: OBSOLETE                   SQ3024.2
004400                                                                  SQ3024.2
004500 01 FREC.                                                         SQ3024.2
004600     03 RKEY PIC 9(8).                                            SQ3024.2
004700                                                                  SQ3024.2
004800 FD SQ-FRR.                                                       SQ3024.2
004900 01 SREC.                                                         SQ3024.2
005000    03  SKEY    PIC X(8).                                         SQ3024.2
005100                                                                  SQ3024.2
005200 FD RR-FS1.                                                       SQ3024.2
005300 01 RREC.                                                         SQ3024.2
005400    03  FKEY    PIC X(8).                                         SQ3024.2
005500                                                                  SQ3024.2
005600 WORKING-STORAGE SECTION.                                         SQ3024.2
005700                                                                  SQ3024.2
005800 01 VARIABLES.                                                    SQ3024.2
005900     03 VKEY PIC 9(8) VALUE ZERO.                                 SQ3024.2
006000     03 DKEY PIC 9(8) VALUE ZERO.                                 SQ3024.2
006100                                                                  SQ3024.2
006200 PROCEDURE DIVISION.                                              SQ3024.2
006300                                                                  SQ3024.2
006400 SQ302M-CONTROL.                                                  SQ3024.2
006500     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         SQ3024.2
006600     STOP RUN.                                                    SQ3024.2
006700                                                                  SQ3024.2
006800*TOTAL NUMBER OF FLAGS EXPECTED = 4.                              SQ3024.2
      *END-OF,SQ302M                                                                  
