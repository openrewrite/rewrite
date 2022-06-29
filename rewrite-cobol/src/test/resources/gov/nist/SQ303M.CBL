      *HEADER,COBOL,SQ303M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ3034.2
000200 PROGRAM-ID.                                                      SQ3034.2
000300     SQ303M.                                                      SQ3034.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      SQ3034.2
000500*OBSOLETE FEATURES THAT ARE USED IN HIGH SUBSET SEQUENTIAL        SQ3034.2
000600*INPUT-OUTPUT.                                                    SQ3034.2
000700 ENVIRONMENT DIVISION.                                            SQ3034.2
000800 CONFIGURATION SECTION.                                           SQ3034.2
000900 SOURCE-COMPUTER.                                                 SQ3034.2
001000     XXXXX082.                                                    SQ3034.2
001100 OBJECT-COMPUTER.                                                 SQ3034.2
001200     XXXXX083.                                                    SQ3034.2
001300 INPUT-OUTPUT SECTION.                                            SQ3034.2
001400 FILE-CONTROL.                                                    SQ3034.2
001500     SELECT TFIL ASSIGN                                           SQ3034.2
001600     XXXXX014                                                     SQ3034.2
001700         ORGANIZATION IS SEQUENTIAL                               SQ3034.2
001800         ACCESS MODE IS SEQUENTIAL.                               SQ3034.2
001900                                                                  SQ3034.2
002000     SELECT TFIL2 ASSIGN                                          SQ3034.2
002100     XXXXX008                                                     SQ3034.2
002200         ORGANIZATION IS SEQUENTIAL                               SQ3034.2
002300         ACCESS MODE IS SEQUENTIAL.                               SQ3034.2
002400                                                                  SQ3034.2
002500 I-O-CONTROL.                                                     SQ3034.2
002600     MULTIPLE FILE TAPE CONTAINS TFIL2.                           SQ3034.2
002700*Message expected for above statement: OBSOLETE                   SQ3034.2
002800                                                                  SQ3034.2
002900 DATA DIVISION.                                                   SQ3034.2
003000 FILE SECTION.                                                    SQ3034.2
003100 FD TFIL.                                                         SQ3034.2
003200 01 FREC.                                                         SQ3034.2
003300     03 RKEY PIC 9(8).                                            SQ3034.2
003400                                                                  SQ3034.2
003500 FD TFIL2.                                                        SQ3034.2
003600 01 FREC2.                                                        SQ3034.2
003700     03 RKEY2 PIC 9(8).                                           SQ3034.2
003800                                                                  SQ3034.2
003900 PROCEDURE DIVISION.                                              SQ3034.2
004000                                                                  SQ3034.2
004100 SQ303M-CONTROL.                                                  SQ3034.2
004200     OPEN INPUT TFIL REVERSED.                                    SQ3034.2
004300*Message expected for above statement: OBSOLETE                   SQ3034.2
004400                                                                  SQ3034.2
004500     CLOSE TFIL.                                                  SQ3034.2
004600     STOP RUN.                                                    SQ3034.2
004700                                                                  SQ3034.2
004800                                                                  SQ3034.2
004900*TOTAL NUMBER OF FLAGS EXPECTED = 2.                              SQ3034.2
      *END-OF,SQ303M                                                                  
