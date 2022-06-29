      *HEADER,COBOL,CM201M                                                            
000100 IDENTIFICATION DIVISION.                                         CM2014.2
000200 PROGRAM-ID.                                                      CM2014.2
000300     CM201M.                                                      CM2014.2
000400 AUTHOR.                                                          CM2014.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM2014.2
000600 INSTALLATION.                                                    CM2014.2
000700     GENERAL SERVICES ADMINISTRATION                              CM2014.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM2014.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 CM2014.2
001000     5203 LEESBURG PIKE  SUITE 1100                               CM2014.2
001100     FALLS CHURCH VIRGINIA 22041.                                 CM2014.2
001200                                                                  CM2014.2
001300     PHONE   (703) 756-6153                                       CM2014.2
001400                                                                  CM2014.2
001500     " HIGH       ".                                              CM2014.2
001600 DATE-WRITTEN.                                                    CM2014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM2014.2
001800     CREATION DATE     /    VALIDATION DATE                       CM2014.2
001900     "4.2 ".                                                      CM2014.2
002000 SECURITY.                                                        CM2014.2
002100     NONE.                                                        CM2014.2
002200 ENVIRONMENT DIVISION.                                            CM2014.2
002300 CONFIGURATION SECTION.                                           CM2014.2
002400 SOURCE-COMPUTER.                                                 CM2014.2
002500     XXXXX082.                                                    CM2014.2
002600 OBJECT-COMPUTER.                                                 CM2014.2
002700     XXXXX083.                                                    CM2014.2
002800 DATA DIVISION.                                                   CM2014.2
002900 WORKING-STORAGE SECTION.                                         CM2014.2
003000 77  MSG-72 PIC X(72).                                            CM2014.2
003100 01  RECOGNITION-MSG-1.                                           CM2014.2
003200     02  FILLER PIC X(32) VALUE                                   CM2014.2
003300         "RECEIPT OF MESSAGE FROM DEVICE """.                     CM2014.2
003400     02 DEVICE-NAME PIC X(12).                                    CM2014.2
003500     02  FILLER PIC X(18) VALUE                                   CM2014.2
003600         """ IS ACKNOWLEDGED.".                                   CM2014.2
003700 01  RECOGNITION-MSG-2.                                           CM2014.2
003800     02  FILLER PIC X(20) VALUE                                   CM2014.2
003900         "QUEUE INVOLVED WAS """.                                 CM2014.2
004000     02  QUEUE-INVOLVED PIC X(48).                                CM2014.2
004100     02  FILLER PIC XX VALUE """.".                               CM2014.2
004200 01  RECOGNITION-MSG-3 PIC X(41) VALUE                            CM2014.2
004300     "CM201M INVOKED BUT NO DATA WAS AVAILABLE.".                 CM2014.2
004400 COMMUNICATION SECTION.                                           CM2014.2
004500 CD  CM-INQUE-1 FOR INITIAL INPUT.                                CM2014.2
004600 01  INQUE-1-RECORD.                                              CM2014.2
004700     02  QUEUE-SET PIC X(48).                                     CM2014.2
004800     02  FILLER PIC X(14).                                        CM2014.2
004900     02  SYM-SOURCE PIC X(12).                                    CM2014.2
005000     02  IN-LENGTH PIC 9(4).                                      CM2014.2
005100     02  FILLER PIC XXX.                                          CM2014.2
005200     02  MSG-COUNT PIC 9(6).                                      CM2014.2
005300 CD  CM-OUTQUE-1 FOR OUTPUT.                                      CM2014.2
005400 01  OUTQUE-1-RECORD.                                             CM2014.2
005500     02  FILLER PIC 9999 VALUE 1.                                 CM2014.2
005600     02  OUT-LENGTH PIC 9999.                                     CM2014.2
005700     02  FILLER PIC XXX.                                          CM2014.2
005800     02  SYM-DEST PIC X(12) VALUE                                 CM2014.2
005900     XXXXX032.                                                    CM2014.2
006000 PROCEDURE    DIVISION.                                           CM2014.2
006100 SECT-CM201M-0001 SECTION.                                        CM2014.2
006200 CM201M-INIT.                                                     CM2014.2
006300     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM2014.2
006400     XXXXX033.                                                    CM2014.2
006500 TAKE-NEXT-MSG.                                                   CM2014.2
006600     RECEIVE CM-INQUE-1 MESSAGE INTO MSG-72                       CM2014.2
006700         NO DATA GO TO SOMETHING-IS-WRONG-HERE.                   CM2014.2
006800     MOVE QUEUE-SET TO QUEUE-INVOLVED.                            CM2014.2
006900     MOVE SYM-SOURCE TO DEVICE-NAME.                              CM2014.2
007000     MOVE 62 TO OUT-LENGTH.                                       CM2014.2
007100     SEND CM-OUTQUE-1 FROM RECOGNITION-MSG-1 WITH EMI.            CM2014.2
007200     MOVE 70 TO OUT-LENGTH.                                       CM2014.2
007300     SEND CM-OUTQUE-1 FROM RECOGNITION-MSG-2 WITH EMI.            CM2014.2
007400     MOVE IN-LENGTH TO OUT-LENGTH.                                CM2014.2
007500     SEND CM-OUTQUE-1 FROM MSG-72 WITH EGI.                       CM2014.2
007600     ACCEPT CM-INQUE-1 MESSAGE COUNT.                             CM2014.2
007700     IF MSG-COUNT IS EQUAL TO 0 STOP RUN                          CM2014.2
007800         ELSE GO TO TAKE-NEXT-MSG.                                CM2014.2
007900 SOMETHING-IS-WRONG-HERE.                                         CM2014.2
008000     MOVE 40 TO OUT-LENGTH.                                       CM2014.2
008100     SEND CM-OUTQUE-1 FROM RECOGNITION-MSG-3 WITH EMI.            CM2014.2
008200     MOVE QUEUE-SET TO QUEUE-INVOLVED.                            CM2014.2
008300     MOVE 70 TO OUT-LENGTH.                                       CM2014.2
008400     SEND CM-OUTQUE-1 FROM RECOGNITION-MSG-2 WITH EGI.            CM2014.2
008500     STOP RUN.                                                    CM2014.2
      *END-OF,CM201M                                                                  
