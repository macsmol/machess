package machess;

import machess.interfaces.FEN;
import machess.interfaces.UCI;
import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void moveGenerationTestDebug() {
//        debugTestPosition(parseUciPosition("1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1 moves a7a8q"),2,0, State.GeneratorMode.TACTICAL_MOVES);
    }

    @Test
    public void moveGenerationTest() {
        // test new game from no-arg constructor
        testPosition(new State(), new long[] { 20,  400,  8902,  197281,  4865609}, State.GeneratorMode.ALL_MOVES);
        // test new game from fen string
        testAllMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new long[] { 20,  400,  8902,  197281,  4865609});
        testAllMoves("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", new long[] { 48,  2039,  97862,  4085603,  });
        testAllMoves("4k3/8/8/8/8/8/8/4K2R w K - 0 1", new long[] { 15,  66,  1197,  7059,  133987,  764643});
        testAllMoves("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1", new long[] { 16,  71,  1287,  7626,  145232,  846648});
        testAllMoves("4k2r/8/8/8/8/8/8/4K3 w k - 0 1", new long[] { 5,  75,  459,  8290,  47635,  899442});
        testAllMoves("r3k3/8/8/8/8/8/8/4K3 w q - 0 1", new long[] { 5,  80,  493,  8897,  52710,  1001523});
        testAllMoves("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", new long[] { 26,  112,  3189,  17945,  532933,  2788982});
        testAllMoves("r3k2r/8/8/8/8/8/8/4K3 w kq - 0 1", new long[] { 5,  130,  782,  22180,  118882,  3517770});
        testAllMoves("8/8/8/8/8/8/6k1/4K2R w K - 0 1", new long[] { 12,  38,  564,  2219,  37735,  185867});
        testAllMoves("8/8/8/8/8/8/1k6/R3K3 w Q - 0 1", new long[] { 15,  65,  1018,  4573,  80619,  413018});
        testAllMoves("4k2r/6K1/8/8/8/8/8/8 w k - 0 1", new long[] { 3,  32,  134,  2073,  10485,  179869});
        testAllMoves("r3k3/1K6/8/8/8/8/8/8 w q - 0 1", new long[] { 4,  49,  243,  3991,  20780,  367724});
        testAllMoves("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", new long[] { 26,  568,  13744,  314346,  7594526,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/1R2K2R w Kkq - 0 1", new long[] { 25,  567,  14095,  328965,  8153719,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/2R1K2R w Kkq - 0 1", new long[] { 25,  548,  13502,  312835,  7736373,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/R3K1R1 w Qkq - 0 1", new long[] { 25,  547,  13579,  316214,  7878456,  });
        testAllMoves("1r2k2r/8/8/8/8/8/8/R3K2R w KQk - 0 1", new long[] { 26,  583,  14252,  334705,  8198901,  });
        testAllMoves("2r1k2r/8/8/8/8/8/8/R3K2R w KQk - 0 1", new long[] { 25,  560,  13592,  317324,  7710115,  });
        testAllMoves("r3k1r1/8/8/8/8/8/8/R3K2R w KQq - 0 1", new long[] { 25,  560,  13607,  320792,  7848606,  });
        testAllMoves("4k3/8/8/8/8/8/8/4K2R b K - 0 1", new long[] { 5,  75,  459,  8290,  47635,  899442});
        testAllMoves("4k3/8/8/8/8/8/8/R3K3 b Q - 0 1", new long[] { 5,  80,  493,  8897,  52710,  1001523});
        testAllMoves("4k2r/8/8/8/8/8/8/4K3 b k - 0 1", new long[] { 15,  66,  1197,  7059,  133987,  764643});
        testAllMoves("r3k3/8/8/8/8/8/8/4K3 b q - 0 1", new long[] { 16,  71,  1287,  7626,  145232,  846648});
        testAllMoves("4k3/8/8/8/8/8/8/R3K2R b KQ - 0 1", new long[] { 5,  130,  782,  22180,  118882,  3517770});
        testAllMoves("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1", new long[] { 26,  112,  3189,  17945,  532933,  2788982});
        testAllMoves("8/8/8/8/8/8/6k1/4K2R b K - 0 1", new long[] { 3,  32,  134,  2073,  10485,  179869});
        testAllMoves("8/8/8/8/8/8/1k6/R3K3 b Q - 0 1", new long[] { 4,  49,  243,  3991,  20780,  367724});
        testAllMoves("4k2r/6K1/8/8/8/8/8/8 b k - 0 1", new long[] { 12,  38,  564,  2219,  37735,  185867});
        testAllMoves("r3k3/1K6/8/8/8/8/8/8 b q - 0 1", new long[] { 15,  65,  1018,  4573,  80619,  413018});
        testAllMoves("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", new long[] { 26,  568,  13744,  314346,  7594526,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/1R2K2R b Kkq - 0 1", new long[] { 26,  583,  14252,  334705,  8198901,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/2R1K2R b Kkq - 0 1", new long[] { 25,  560,  13592,  317324,  7710115,  });
        testAllMoves("r3k2r/8/8/8/8/8/8/R3K1R1 b Qkq - 0 1", new long[] { 25,  560,  13607,  320792,  7848606,  });
        testAllMoves("1r2k2r/8/8/8/8/8/8/R3K2R b KQk - 0 1", new long[] { 25,  567,  14095,  328965,  8153719,  });
        testAllMoves("2r1k2r/8/8/8/8/8/8/R3K2R b KQk - 0 1", new long[] { 25,  548,  13502,  312835,  7736373,  });
        testAllMoves("r3k1r1/8/8/8/8/8/8/R3K2R b KQq - 0 1", new long[] { 25,  547,  13579,  316214,  7878456,  });
        testAllMoves("8/1n4N1/2k5/8/8/5K2/1N4n1/8 w - - 0 1", new long[] { 14,  195,  2760,  38675,  570726,  8107539});
        testAllMoves("8/1k6/8/5N2/8/4n3/8/2K5 w - - 0 1", new long[] { 11,  156,  1636,  20534,  223507,  2594412});
        testAllMoves("8/8/4k3/3Nn3/3nN3/4K3/8/8 w - - 0 1", new long[] { 19,  289,  4442,  73584,  1198299,  19870403});
        testAllMoves("K7/8/2n5/1n6/8/8/8/k6N w - - 0 1", new long[] { 3,  51,  345,  5301,  38348,  588695});
        testAllMoves("k7/8/2N5/1N6/8/8/8/K6n w - - 0 1", new long[] { 17,  54,  835,  5910,  92250,  688780});
        testAllMoves("8/1n4N1/2k5/8/8/5K2/1N4n1/8 b - - 0 1", new long[] { 15,  193,  2816,  40039,  582642,  8503277});
        testAllMoves("8/1k6/8/5N2/8/4n3/8/2K5 b - - 0 1", new long[] { 16,  180,  2290,  24640,  288141,  3147566});
        testAllMoves("8/8/3K4/3Nn3/3nN3/4k3/8/8 b - - 0 1", new long[] { 4,  68,  1118,  16199,  281190,  4405103});
        testAllMoves("K7/8/2n5/1n6/8/8/8/k6N b - - 0 1", new long[] { 17,  54,  835,  5910,  92250,  688780});
        testAllMoves("k7/8/2N5/1N6/8/8/8/K6n b - - 0 1", new long[] { 3,  51,  345,  5301,  38348,  588695});
        testAllMoves("B6b/8/8/8/2K5/4k3/8/b6B w - - 0 1", new long[] { 17,  278,  4607,  76778,  1320507,  22823890});
        testAllMoves("8/8/1B6/7b/7k/8/2B1b3/7K w - - 0 1", new long[] { 21,  316,  5744,  93338,  1713368,  28861171});
        testAllMoves("k7/B7/1B6/1B6/8/8/8/K6b w - - 0 1", new long[] { 21,  144,  3242,  32955,  787524,  7881673});
        testAllMoves("K7/b7/1b6/1b6/8/8/8/k6B w - - 0 1", new long[] { 7,  143,  1416,  31787,  310862,  7382896});
        testAllMoves("B6b/8/8/8/2K5/5k2/8/b6B b - - 0 1", new long[] { 6,  106,  1829,  31151,  530585,  9250746});
        testAllMoves("8/8/1B6/7b/7k/8/2B1b3/7K b - - 0 1", new long[] { 17,  309,  5133,  93603,  1591064,  29027891});
        testAllMoves("k7/B7/1B6/1B6/8/8/8/K6b b - - 0 1", new long[] { 7,  143,  1416,  31787,  310862,  7382896});
        testAllMoves("K7/b7/1b6/1b6/8/8/8/k6B b - - 0 1", new long[] { 21,  144,  3242,  32955,  787524,  7881673});
        testAllMoves("7k/RR6/8/8/8/8/rr6/7K w - - 0 1", new long[] { 19,  275,  5300,  104342,  2161211,  44956585});
        testAllMoves("R6r/8/8/2K5/5k2/8/8/r6R w - - 0 1", new long[] { 36,  1027,  29215,  771461,  20506480,  });
        testAllMoves("7k/RR6/8/8/8/8/rr6/7K b - - 0 1", new long[] { 19,  275,  5300,  104342,  2161211,  44956585});
        testAllMoves("R6r/8/8/2K5/5k2/8/8/r6R b - - 0 1", new long[] { 36,  1027,  29227,  771368,  20521342,  });
        testAllMoves("6kq/8/8/8/8/8/8/7K w - - 0 1", new long[] { 2,  36,  143,  3637,  14893,  391507});
        testAllMoves("6KQ/8/8/8/8/8/8/7k b - - 0 1", new long[] { 2,  36,  143,  3637,  14893,  391507});
        testAllMoves("K7/8/8/3Q4/4q3/8/8/7k w - - 0 1", new long[] { 6,  35,  495,  8349,  166741,  3370175});
        testAllMoves("6qk/8/8/8/8/8/8/7K b - - 0 1", new long[] { 22,  43,  1015,  4167,  105749,  419369});
        testAllMoves("6KQ/8/8/8/8/8/8/7k b - - 0 1", new long[] { 2,  36,  143,  3637,  14893,  391507});
        testAllMoves("K7/8/8/3Q4/4q3/8/8/7k b - - 0 1", new long[] { 6,  35,  495,  8349,  166741,  3370175});
        testAllMoves("8/8/8/8/8/K7/P7/k7 w - - 0 1", new long[] { 3,  7,  43,  199,  1347,  6249});
        testAllMoves("8/8/8/8/8/7K/7P/7k w - - 0 1", new long[] { 3,  7,  43,  199,  1347,  6249});
        testAllMoves("K7/p7/k7/8/8/8/8/8 w - - 0 1", new long[] { 1,  3,  12,  80,  342,  2343});
        testAllMoves("7K/7p/7k/8/8/8/8/8 w - - 0 1", new long[] { 1,  3,  12,  80,  342,  2343});
        testAllMoves("8/2k1p3/3pP3/3P2K1/8/8/8/8 w - - 0 1", new long[] { 7,  35,  210,  1091,  7028,  34834});
        testAllMoves("8/8/8/8/8/K7/P7/k7 b - - 0 1", new long[] { 1,  3,  12,  80,  342,  2343});
        testAllMoves("8/8/8/8/8/7K/7P/7k b - - 0 1", new long[] { 1,  3,  12,  80,  342,  2343});
        testAllMoves("K7/p7/k7/8/8/8/8/8 b - - 0 1", new long[] { 3,  7,  43,  199,  1347,  6249});
        testAllMoves("7K/7p/7k/8/8/8/8/8 b - - 0 1", new long[] { 3,  7,  43,  199,  1347,  6249});
        testAllMoves("8/2k1p3/3pP3/3P2K1/8/8/8/8 b - - 0 1", new long[] { 5,  35,  182,  1091,  5408,  34822});
        testAllMoves("8/8/8/8/8/4k3/4P3/4K3 w - - 0 1", new long[] { 2,  8,  44,  282,  1814,  11848});
        testAllMoves("4k3/4p3/4K3/8/8/8/8/8 b - - 0 1", new long[] { 2,  8,  44,  282,  1814,  11848});
        testAllMoves("8/8/7k/7p/7P/7K/8/8 w - - 0 1", new long[] { 3,  9,  57,  360,  1969,  10724});
        testAllMoves("8/8/k7/p7/P7/K7/8/8 w - - 0 1", new long[] { 3,  9,  57,  360,  1969,  10724});
        testAllMoves("8/8/3k4/3p4/3P4/3K4/8/8 w - - 0 1", new long[] { 5,  25,  180,  1294,  8296,  53138});
        testAllMoves("8/3k4/3p4/8/3P4/3K4/8/8 w - - 0 1", new long[] { 8,  61,  483,  3213,  23599,  157093});
        testAllMoves("8/8/3k4/3p4/8/3P4/3K4/8 w - - 0 1", new long[] { 8,  61,  411,  3213,  21637,  158065});
        testAllMoves("k7/8/3p4/8/3P4/8/8/7K w - - 0 1", new long[] { 4,  15,  90,  534,  3450,  20960});
        testAllMoves("8/8/7k/7p/7P/7K/8/8 b - - 0 1", new long[] { 3,  9,  57,  360,  1969,  10724});
        testAllMoves("8/8/k7/p7/P7/K7/8/8 b - - 0 1", new long[] { 3,  9,  57,  360,  1969,  10724});
        testAllMoves("8/8/3k4/3p4/3P4/3K4/8/8 b - - 0 1", new long[] { 5,  25,  180,  1294,  8296,  53138});
        testAllMoves("8/3k4/3p4/8/3P4/3K4/8/8 b - - 0 1", new long[] { 8,  61,  411,  3213,  21637,  158065});
        testAllMoves("8/8/3k4/3p4/8/3P4/3K4/8 b - - 0 1", new long[] { 8,  61,  483,  3213,  23599,  157093});
        testAllMoves("k7/8/3p4/8/3P4/8/8/7K b - - 0 1", new long[] { 4,  15,  89,  537,  3309,  21104});
        testAllMoves("7k/3p4/8/8/3P4/8/8/K7 w - - 0 1", new long[] { 4,  19,  117,  720,  4661,  32191});
        testAllMoves("7k/8/8/3p4/8/8/3P4/K7 w - - 0 1", new long[] { 5,  19,  116,  716,  4786,  30980});
        testAllMoves("k7/8/8/7p/6P1/8/8/K7 w - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("k7/8/7p/8/8/6P1/8/K7 w - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/8/8/6p1/7P/8/8/K7 w - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("k7/8/6p1/8/8/7P/8/K7 w - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/8/8/3p4/4p3/8/8/7K w - - 0 1", new long[] { 3,  15,  84,  573,  3013,  22886});
        testAllMoves("k7/8/3p4/8/8/4P3/8/7K w - - 0 1", new long[] { 4,  16,  101,  637,  4271,  28662});
        testAllMoves("7k/3p4/8/8/3P4/8/8/K7 b - - 0 1", new long[] { 5,  19,  117,  720,  5014,  32167});
        testAllMoves("7k/8/8/3p4/8/8/3P4/K7 b - - 0 1", new long[] { 4,  19,  117,  712,  4658,  30749});
        testAllMoves("k7/8/8/7p/6P1/8/8/K7 b - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("k7/8/7p/8/8/6P1/8/K7 b - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/8/8/6p1/7P/8/8/K7 b - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("k7/8/6p1/8/8/7P/8/K7 b - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/8/8/3p4/4p3/8/8/7K b - - 0 1", new long[] { 5,  15,  102,  569,  4337,  22579});
        testAllMoves("k7/8/3p4/8/8/4P3/8/7K b - - 0 1", new long[] { 4,  16,  101,  637,  4271,  28662});
        testAllMoves("7k/8/8/p7/1P6/8/8/7K w - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("7k/8/p7/8/8/1P6/8/7K w - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("7k/8/8/1p6/P7/8/8/7K w - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("7k/8/1p6/8/8/P7/8/7K w - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/7p/8/8/8/8/6P1/K7 w - - 0 1", new long[] { 5,  25,  161,  1035,  7574,  55338});
        testAllMoves("k7/6p1/8/8/8/8/7P/K7 w - - 0 1", new long[] { 5,  25,  161,  1035,  7574,  55338});
        testAllMoves("3k4/3pp3/8/8/8/8/3PP3/3K4 w - - 0 1", new long[] { 7,  49,  378,  2902,  24122,  199002});
        testAllMoves("7k/8/8/p7/1P6/8/8/7K b - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("7k/8/p7/8/8/1P6/8/7K b - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("7k/8/8/1p6/P7/8/8/7K b - - 0 1", new long[] { 5,  22,  139,  877,  6112,  41874});
        testAllMoves("7k/8/1p6/8/8/P7/8/7K b - - 0 1", new long[] { 4,  16,  101,  637,  4354,  29679});
        testAllMoves("k7/7p/8/8/8/8/6P1/K7 b - - 0 1", new long[] { 5,  25,  161,  1035,  7574,  55338});
        testAllMoves("k7/6p1/8/8/8/8/7P/K7 b - - 0 1", new long[] { 5,  25,  161,  1035,  7574,  55338});
        testAllMoves("3k4/3pp3/8/8/8/8/3PP3/3K4 b - - 0 1", new long[] { 7,  49,  378,  2902,  24122,  199002});
        testAllMoves("8/Pk6/8/8/8/8/6Kp/8 w - - 0 1", new long[] { 11,  97,  887,  8048,  90606,  1030499});
        testAllMoves("n1n5/1Pk5/8/8/8/8/5Kp1/5N1N w - - 0 1", new long[] { 24,  421,  7421,  124608,  2193768,  37665329});
        testAllMoves("8/PPPk4/8/8/8/8/4Kppp/8 w - - 0 1", new long[] { 18,  270,  4699,  79355,  1533145,  28859283});
        testAllMoves("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N w - - 0 1", new long[] { 24,  496,  9483,  182838,  3605103,  71179139});
        testAllMoves("8/Pk6/8/8/8/8/6Kp/8 b - - 0 1", new long[] { 11,  97,  887,  8048,  90606,  1030499});
        testAllMoves("n1n5/1Pk5/8/8/8/8/5Kp1/5N1N b - - 0 1", new long[] { 24,  421,  7421,  124608,  2193768,  37665329});
        testAllMoves("8/PPPk4/8/8/8/8/4Kppp/8 b - - 0 1", new long[] { 18,  270,  4699,  79355,  1533145,  28859283});
        testAllMoves("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1", new long[] { 24,  496,  9483,  182838,  3605103,  71179139});
    }

    @Test
    public void tacticalMoveGenerationTest() {
        /* Initially wanted to use more cases from https://www.chessprogramming.org/Perft_Results
         but realized I couldn't: count of tactical moves is not the sum of captures and promotions
         some captures are also promotions and shouldn't be counted twice.
         I could not find perft results for tactical moves together so I used only these contrived test cases.
         */
        // simplest promotion case
        testTacticalMoves("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", new long[] { 4,  0,  100});
        testTacticalMoves("4k3/8/8/8/8/8/p7/4K3 b - - 0 1", new long[] { 4,  0,  100});

        //simple capture-promotion case
        testTacticalMoves("1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1", new long[] { 8, 0, 271});
        testTacticalMoves("4k3/8/8/8/8/8/p7/1N2K3 b - - 0 1", new long[] { 8, 0, 271});
        // newgame
        testTacticalMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new long[] { 0,  0,  34,  1576,  82719,  2812008});
    }

    private void debugTestPosition(State position, int depth, long expectedLegalMoves, State.GeneratorMode mode) {
        long actualLegalMoves = Scorer.perft(position, depth, mode);
        if (expectedLegalMoves != actualLegalMoves) {
            System.out.println("Wrong perft(" + depth + "). Expected: " + expectedLegalMoves+ ", actual: "+ actualLegalMoves +". Position: " + position);
            Scorer.perftDivide(position, depth, mode);
            fail();
        }
    }

    private void debugTestPosition(String fen, int depth, long expectedLegalMoves, State.GeneratorMode mode) {
        State position = FEN.parse(fen);
        long actualLegalMoves = Scorer.perft(position, depth, mode);
        if (expectedLegalMoves != actualLegalMoves) {
            System.out.println("Wrong perft(" + depth + "). Expected: " + expectedLegalMoves+ ", actual: "+ actualLegalMoves +". Position: " + position);
            Scorer.perftDivide(position, depth, mode);
            fail();
        }
    }

    private void testAllMoves(String fen, long[] expectedLegalMoves) {
        State position = FEN.parse(fen);
        System.out.println("Testing all moves: " + fen);
        testPosition(position, expectedLegalMoves, State.GeneratorMode.ALL_MOVES);
    }

    private void testPosition(State position, long[] expectedLegalMoves, State.GeneratorMode mode) {
        for (int i = 0; i < expectedLegalMoves.length; i++) {
            int depth = i + 1;
            long actualLegalMoves = Scorer.perft(position, depth, mode);
            if (expectedLegalMoves[i] != actualLegalMoves) {
                System.out.println("Wrong perft(" + depth + "). Expected: " + expectedLegalMoves[i] + ", actual: "+ actualLegalMoves +". Position: " + position);
                Scorer.perftDivide(position, depth, mode);
                fail();
            }
            System.out.println("Perft(" + depth + ") ok");
        }
    }

    private void testTacticalMoves(String fen, long[] expectedLegalTacticalMoves) {
        State position = FEN.parse(fen);
        System.out.println("Testing tactical moves: " + fen);
        testPosition(position, expectedLegalTacticalMoves, State.GeneratorMode.TACTICAL_MOVES);
    }

    private State parseUciPosition(String positionCommand) {
        int movesIdx = positionCommand.indexOf(UCI.MOVES);
        if (movesIdx == -1) {
            return UCI.parseState(positionCommand);
        } else {
            State state = UCI.parseState(positionCommand.substring(0, movesIdx));

            String[] moves = positionCommand
                    .substring(movesIdx + UCI.MOVES.length())
                    .trim()
                    .toLowerCase()
                    .split(" ");

            for (String move : moves) {
                state = Lan.move(state, move);
            }
            return state;
        }
    }
}