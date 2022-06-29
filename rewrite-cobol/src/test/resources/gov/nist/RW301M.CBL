      *HEADER,COBOL,RW301M                                                            
000100 IDENTIFICATION DIVISION.                                         RW3014.2
000200 PROGRAM-ID.                                                      RW3014.2
000300     RW301M.                                                      RW3014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      RW3014.2
000500*FEATURES THAT ARE USED IN REPORT WRITING.                        RW3014.2
000600 ENVIRONMENT DIVISION.                                            RW3014.2
000700 CONFIGURATION SECTION.                                           RW3014.2
000800 SOURCE-COMPUTER.                                                 RW3014.2
000900     XXXXX082.                                                    RW3014.2
001000 OBJECT-COMPUTER.                                                 RW3014.2
001100     XXXXX083.                                                    RW3014.2
001200 INPUT-OUTPUT SECTION.                                            RW3014.2
001300 FILE-CONTROL.                                                    RW3014.2
001400     SELECT TFIL ASSIGN                                           RW3014.2
001500     XXXXX001                                                     RW3014.2
001600         ORGANIZATION IS SEQUENTIAL                               RW3014.2
001700         ACCESS MODE IS SEQUENTIAL.                               RW3014.2
001800                                                                  RW3014.2
001900     SELECT TFIL2 ASSIGN                                          RW3014.2
002000     XXXXX002                                                     RW3014.2
002100     ORGANIZATION IS SEQUENTIAL                                   RW3014.2
002200     ACCESS MODE IS SEQUENTIAL.                                   RW3014.2
002300                                                                  RW3014.2
002400                                                                  RW3014.2
002500 I-O-CONTROL.                                                     RW3014.2
002600     MULTIPLE FILE TAPE CONTAINS TFIL2.                           RW3014.2
002700                                                                  RW3014.2
002800 DATA DIVISION.                                                   RW3014.2
002900 FILE SECTION.                                                    RW3014.2
003000 FD TFIL                                                          RW3014.2
003100     LABEL RECORDS STANDARD                                       RW3014.2
003200     VALUE OF                                                     RW3014.2
003300     XXXXX074                                                     RW3014.2
003400     IS                                                           RW3014.2
003500     XXXXX075.                                                    RW3014.2
003600                                                                  RW3014.2
003700                                                                  RW3014.2
003800 01 FREC.                                                         RW3014.2
003900     03 RKEY PIC 9(8).                                            RW3014.2
004000                                                                  RW3014.2
004100 FD TFIL2                                                         RW3014.2
004200        REPORT IS RFIL2.                                          RW3014.2
004300*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
004400                                                                  RW3014.2
004500 WORKING-STORAGE SECTION.                                         RW3014.2
004600                                                                  RW3014.2
004700 01 VARIABLES.                                                    RW3014.2
004800     03 VKEY PIC 9(8) VALUE ZERO.                                 RW3014.2
004900                                                                  RW3014.2
005000 REPORT SECTION.                                                  RW3014.2
005100*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
005200 RD  RFIL2.                                                       RW3014.2
005300*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
005400 01  RREC                                                         RW3014.2
005500             TYPE IS DETAIL.                                      RW3014.2
005600*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
005700     02  PIC 9(8)                                                 RW3014.2
005800             SOURCE IS RKEY                                       RW3014.2
005900*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
006000             COLUMN NUMBER IS 1                                   RW3014.2
006100*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
006200             LINE NUMBER IS PLUS 1.                               RW3014.2
006300*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
006400                                                                  RW3014.2
006500 PROCEDURE DIVISION.                                              RW3014.2
006600                                                                  RW3014.2
006700 RW301M-CONTROL.                                                  RW3014.2
006800     OPEN INPUT TFIL.                                             RW3014.2
006900     READ TFIL                                                    RW3014.2
007000         AT END                                                   RW3014.2
007100             GO TO RW301M-END.                                    RW3014.2
007200     OPEN OUTPUT TFIL2.                                           RW3014.2
007300     INITIATE RFIL2.                                              RW3014.2
007400*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
007500     GENERATE RREC.                                               RW3014.2
007600*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
007700     TERMINATE RFIL2.                                             RW3014.2
007800*Message expected for above statement: NON-CONFORMING STANDARD    RW3014.2
007900 RW301M-END.                                                      RW3014.2
008000     CLOSE TFIL, TFIL2.                                           RW3014.2
008100     STOP RUN.                                                    RW3014.2
008200                                                                  RW3014.2
008300                                                                  RW3014.2
008400*TOTAL NUMBER OF FLAGS EXPECTED = 10                              RW3014.2
      *END-OF,RW301M                                                                  
