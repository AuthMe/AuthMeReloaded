package fr.xephi.authme.util;

import net.ricecode.similarity.LevenshteinDistanceStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

public class StringUtils {

    public static double getDifference(String first, String second) {
        if(first == null || second == null)
            return 1.0;

        StringSimilarityService service = new StringSimilarityServiceImpl(new LevenshteinDistanceStrategy());

        double score = service.score(first, second);

        return Math.abs(score - 1.0);
    }
}
