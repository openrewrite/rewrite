      *HEADER,COBOL,RL302M                                                            
000100 IDENTIFICATION DIVISION.                                         RL3024.2
000200 PROGRAM-ID.                                                      RL3024.2
000300     RL302M.                                                      RL3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      RL3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN INTERMEDIATE SUBSET           RL3024.2
000600*RELATIVE INPUT-OUTPUT.                                           RL3024.2
000700*INPUT-OUTPUT.                                                    RL3024.2
000800 ENVIRONMENT DIVISION.                                            RL3024.2
000900 CONFIGURATION SECTION.                                           RL3024.2
001000 SOURCE-COMPUTER.                                                 RL3024.2
001100     XXXXX082.                                                    RL3024.2
001200 OBJECT-COMPUTER.                                                 RL3024.2
001300     XXXXX083.                                                    RL3024.2
001400 INPUT-OUTPUT SECTION.                                            RL3024.2
001500 FILE-CONTROL.                                                    RL3024.2
001600     SELECT TFIL ASSIGN                                           RL3024.2
001700     XXXXX021                                                     RL3024.2
001800         ORGANIZATION IS RELATIVE                                 RL3024.2
001900         ACCESS MODE IS RANDOM                                    RL3024.2
002000         RELATIVE KEY IS RKEY.                                    RL3024.2
002100                                                                  RL3024.2
002200     SELECT SQ-FRR ASSIGN                                         RL3024.2
002300     XXXXX013                                                     RL3024.2
002400         ORGANIZATION IS SEQUENTIAL.                              RL3024.2
002500                                                                  RL3024.2
002600     SELECT RR-FS1 ASSIGN                                         RL3024.2
002700     XXXXX021                                                     RL3024.2
002800         ORGANIZATION IS RELATIVE.                                RL3024.2
002900 I-O-CONTROL.                                                     RL3024.2
003000     XXXXX053.                                                    RL3024.2
003100*Message expected for above statement: OBSOLETE                   RL3024.2
003200                                                                  RL3024.2
003300 DATA DIVISION.                                                   RL3024.2
003400 FILE SECTION.                                                    RL3024.2
003500 FD TFIL                                                          RL3024.2
003600     LABEL RECORDS STANDARD                                       RL3024.2
003700*Message expected for above statement: OBSOLETE                   RL3024.2
003800     VALUE OF                                                     RL3024.2
003900     XXXXX074                                                     RL3024.2
004000     IS                                                           RL3024.2
004100     XXXXX075                                                     RL3024.2
004200*Message expected for above statement: OBSOLETE                   RL3024.2
004300     DATA RECORDS ARE FREC.                                       RL3024.2
004400*Message expected for above statement: OBSOLETE                   RL3024.2
004500                                                                  RL3024.2
004600 01 FREC.                                                         RL3024.2
004700     03 GUBBINS PIC 9(8).                                         RL3024.2
004800                                                                  RL3024.2
004900 FD SQ-FRR.                                                       RL3024.2
005000 01 SREC.                                                         RL3024.2
005100    03  SKEY    PIC X(8).                                         RL3024.2
005200                                                                  RL3024.2
005300 FD RR-FS1.                                                       RL3024.2
005400 01 RREC.                                                         RL3024.2
005500    03  FKEY    PIC X(8).                                         RL3024.2
005600                                                                  RL3024.2
005700 WORKING-STORAGE SECTION.                                         RL3024.2
005800                                                                  RL3024.2
005900 01 VARIABLES.                                                    RL3024.2
006000     03 RKEY PIC 9(8) VALUE ZERO.                                 RL3024.2
006100     03 VKEY PIC 9(8) VALUE ZERO.                                 RL3024.2
006200     03 DKEY PIC 9(8) VALUE ZERO.                                 RL3024.2
006300                                                                  RL3024.2
006400 PROCEDURE DIVISION.                                              RL3024.2
006500                                                                  RL3024.2
006600 RL302M-CONTROL.                                                  RL3024.2
006700     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         RL3024.2
006800     STOP RUN.                                                    RL3024.2
006900                                                                  RL3024.2
007000*TOTAL NUMBER OF FLAGS EXPECTED = 4.                              RL3024.2
      *END-OF,RL302M                                                                  
