      *HEADER,COBOL,RW302M                                                            
000100 IDENTIFICATION DIVISION.                                         RW3024.2
000200 PROGRAM-ID.                                                      RW3024.2
000300     RW302M.                                                      RW3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      RW3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN REPORT                        RW3024.2
000600*WRITING.                                                         RW3024.2
000700 ENVIRONMENT DIVISION.                                            RW3024.2
000800 CONFIGURATION SECTION.                                           RW3024.2
000900 SOURCE-COMPUTER.                                                 RW3024.2
001000     XXXXX082.                                                    RW3024.2
001100 OBJECT-COMPUTER.                                                 RW3024.2
001200     XXXXX083.                                                    RW3024.2
001300 INPUT-OUTPUT SECTION.                                            RW3024.2
001400 FILE-CONTROL.                                                    RW3024.2
001500     SELECT TFIL ASSIGN                                           RW3024.2
001600     XXXXX001                                                     RW3024.2
001700         ORGANIZATION IS SEQUENTIAL                               RW3024.2
001800         ACCESS MODE IS SEQUENTIAL.                               RW3024.2
001900                                                                  RW3024.2
002000     SELECT TFIL2 ASSIGN                                          RW3024.2
002100     XXXXX002                                                     RW3024.2
002200     ORGANIZATION IS SEQUENTIAL                                   RW3024.2
002300     ACCESS MODE IS SEQUENTIAL.                                   RW3024.2
002400                                                                  RW3024.2
002500                                                                  RW3024.2
002600 I-O-CONTROL.                                                     RW3024.2
002700     MULTIPLE FILE TAPE CONTAINS TFIL.                            RW3024.2
002800*Message expected for above statement: OBSOLETE                   RW3024.2
002900                                                                  RW3024.2
003000                                                                  RW3024.2
003100 DATA DIVISION.                                                   RW3024.2
003200 FILE SECTION.                                                    RW3024.2
003300 FD TFIL                                                          RW3024.2
003400     LABEL RECORDS STANDARD                                       RW3024.2
003500*Message expected for above statement: OBSOLETE                   RW3024.2
003600     VALUE OF                                                     RW3024.2
003700     XXXXX074                                                     RW3024.2
003800     IS                                                           RW3024.2
003900     XXXXX075.                                                    RW3024.2
004000*Message expected for above statement: OBSOLETE                   RW3024.2
004100                                                                  RW3024.2
004200                                                                  RW3024.2
004300 01 FREC.                                                         RW3024.2
004400     03 RKEY PIC 9(8).                                            RW3024.2
004500                                                                  RW3024.2
004600 FD TFIL2                                                         RW3024.2
004700        REPORT IS RFIL2.                                          RW3024.2
004800                                                                  RW3024.2
004900 WORKING-STORAGE SECTION.                                         RW3024.2
005000                                                                  RW3024.2
005100 01 VARIABLES.                                                    RW3024.2
005200     03 VKEY PIC 9(8) VALUE ZERO.                                 RW3024.2
005300                                                                  RW3024.2
005400 REPORT SECTION.                                                  RW3024.2
005500 RD  RFIL2.                                                       RW3024.2
005600 01  RREC                                                         RW3024.2
005700             TYPE IS DETAIL.                                      RW3024.2
005800     02  PIC 9(8)                                                 RW3024.2
005900             SOURCE IS RKEY                                       RW3024.2
006000             LINE NUMBER IS PLUS 1                                RW3024.2
006100             COLUMN NUMBER IS 1.                                  RW3024.2
006200                                                                  RW3024.2
006300 PROCEDURE DIVISION.                                              RW3024.2
006400                                                                  RW3024.2
006500 RW302M-CONTROL.                                                  RW3024.2
006600     OPEN INPUT TFIL.                                             RW3024.2
006700     READ TFIL                                                    RW3024.2
006800         AT END                                                   RW3024.2
006900             GO TO RW302M-END.                                    RW3024.2
007000     OPEN OUTPUT TFIL2.                                           RW3024.2
007100     INITIATE RFIL2.                                              RW3024.2
007200     GENERATE RREC.                                               RW3024.2
007300     TERMINATE RFIL2.                                             RW3024.2
007400 RW302M-END.                                                      RW3024.2
007500     CLOSE TFIL, TFIL2.                                           RW3024.2
007600     STOP RUN.                                                    RW3024.2
007700                                                                  RW3024.2
007800                                                                  RW3024.2
007900*TOTAL NUMBER OF FLAGS EXPECTED = 3.                              RW3024.2
      *END-OF,RW302M                                                                  
