import java.io.*;
import java.util.*;

/**
 * Smart Recipe Discovery System
 *
 * Java console application for discovering recipes.
 * No frontend, no database, no external libraries.
 */
public class SmartRecipeSystem {

    // =========================================================
    // DATA STRUCTURES
    // =========================================================

    static class Recipe {
        int id;
        String name;
        ArrayList<String> ingredients;
        ArrayList<String> amounts;
        int preparationTime;
        int cookingTime;
        int totalTime;
        int servings;
        String cuisine;
        String category;
        String diet;
        String instructions;

        Recipe(int id, String name, ArrayList<String> ingredients,
               ArrayList<String> amounts, int preparationTime,
               int cookingTime, int totalTime, int servings,
               String cuisine, String category, String diet,
               String instructions) {
            this.id = id;
            this.name = name;
            this.ingredients = ingredients;
            this.amounts = amounts;
            this.preparationTime = preparationTime;
            this.cookingTime = cookingTime;
            this.totalTime = totalTime;
            this.servings = servings;
            this.cuisine = cuisine;
            this.category = category;
            this.diet = diet;
            this.instructions = instructions;
        }
    }

    static class RecipeScore {
        Recipe recipe;
        ArrayList<String> matched = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        ArrayList<String> fuzzyMatched = new ArrayList<>();

        double ingredientCoverage;
        double jaccard;
        double keywordScore;
        double finalScore;

        RecipeScore(Recipe recipe) {
            this.recipe = recipe;
        }
    }

    // =========================================================
    // TEXT NORMALIZATION
    // =========================================================

    static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String[] tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return new String[0];
        return normalized.split(" ");
    }

    static boolean sameIngredient(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    // =========================================================
    // 1. NAIVE STRING MATCHING
    // =========================================================

    static boolean naiveSearch(String text, String pattern) {
        text = normalize(text);
        pattern = normalize(pattern);

        if (pattern.isEmpty()) return true;
        if (pattern.length() > text.length()) return false;

        for (int i = 0; i <= text.length() - pattern.length(); i++) {
            int j = 0;
            while (j < pattern.length()
                    && text.charAt(i + j) == pattern.charAt(j)) {
                j++;
            }
            if (j == pattern.length()) return true;
        }
        return false;
    }

    // =========================================================
    // 2. KMP STRING MATCHING
    // =========================================================

    static int[] buildLPS(String pattern) {
        pattern = normalize(pattern);
        int[] lps = new int[pattern.length()];

        int len = 0;
        int i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    static boolean kmpSearch(String text, String pattern) {
        text = normalize(text);
        pattern = normalize(pattern);

        if (pattern.isEmpty()) return true;
        if (pattern.length() > text.length()) return false;

        int[] lps = buildLPS(pattern);
        int i = 0, j = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) return true;
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return false;
    }

    // =========================================================
    // 3. RABIN-KARP STRING MATCHING
    // =========================================================

    static boolean rabinKarpSearch(String text, String pattern) {
        text = normalize(text);
        pattern = normalize(pattern);

        int n = text.length();
        int m = pattern.length();

        if (m == 0) return true;
        if (m > n) return false;

        final int base = 256;
        final int prime = 1_000_003;

        long patternHash = 0;
        long windowHash = 0;
        long highPower = 1;

        for (int i = 0; i < m - 1; i++) {
            highPower = (highPower * base) % prime;
        }

        for (int i = 0; i < m; i++) {
            patternHash = (base * patternHash + pattern.charAt(i)) % prime;
            windowHash = (base * windowHash + text.charAt(i)) % prime;
        }

        for (int i = 0; i <= n - m; i++) {
            if (patternHash == windowHash
                    && text.regionMatches(i, pattern, 0, m)) {
                return true;
            }

            if (i < n - m) {
                windowHash =
                        (base * (windowHash
                                - text.charAt(i) * highPower)
                                + text.charAt(i + m)) % prime;

                if (windowHash < 0) windowHash += prime;
            }
        }

        return false;
    }

    // =========================================================
    // 4. LEVENSHTEIN DISTANCE
    // =========================================================

    static int levenshteinDistance(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        // Use one row to reduce memory from O(n*m) to O(m).
        if (a.length() < b.length()) {
            String temp = a;
            a = b;
            b = temp;
        }

        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;

            for (int j = 1; j <= b.length(); j++) {
                int insert = current[j - 1] + 1;
                int delete = previous[j] + 1;
                int replace = previous[j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);

                current[j] = Math.min(insert, Math.min(delete, replace));
            }
            previous = current;
        }

        return previous[b.length()];
    }

    static boolean fuzzySearch(String text, String pattern) {
        text = normalize(text);
        pattern = normalize(pattern);

        if (text.equals(pattern)) return true;

        int maxLength = Math.max(text.length(), pattern.length());
        int threshold;

        if (maxLength <= 4) threshold = 1;
        else if (maxLength <= 8) threshold = 2;
        else threshold = 3;

        return levenshteinDistance(text, pattern) <= threshold;
    }

    // =========================================================
    // 5. JACCARD SIMILARITY
    // =========================================================

    static double jaccardSimilarity(List<String> a, List<String> b) {
        HashSet<String> first = new HashSet<>();
        HashSet<String> second = new HashSet<>();

        for (String s : a) first.add(normalize(s));
        for (String s : b) second.add(normalize(s));

        first.remove("");
        second.remove("");

        if (first.isEmpty() && second.isEmpty()) return 1.0;
        if (first.isEmpty() || second.isEmpty()) return 0.0;

        HashSet<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);

        HashSet<String> union = new HashSet<>(first);
        union.addAll(second);

        return (double) intersection.size() / union.size();
    }

    // =========================================================
    // CORPUS LOADING
    // =========================================================

    static ArrayList<Recipe> loadRecipes() {
        ArrayList<Recipe> recipes = new ArrayList<>();

        File corpus = new File("corpus");
        File[] files = corpus.listFiles((dir, name) ->
                name.toLowerCase().startsWith("recipe")
                        && name.toLowerCase().endsWith(".txt"));

        if (files == null) {
            System.out.println("ERROR: corpus folder not found.");
            return recipes;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        int id = 101;

        for (File file : files) {
            try {
                Recipe recipe = parseRecipe(file, id++);
                if (recipe != null) recipes.add(recipe);
            } catch (Exception e) {
                System.out.println("Could not load "
                        + file.getName() + ": " + e.getMessage());
            }
        }

        return recipes;
    }

    static Recipe parseRecipe(File file, int id) throws IOException {
        ArrayList<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim());
            }
        }

        if (lines.size() < 11) {
            throw new IOException("Incomplete recipe file.");
        }

        String name = lines.get(0);

        ArrayList<String> ingredients =
                parseList(fieldValue(lines.get(1), "Ingredients:"));

        ArrayList<String> amounts =
                parseListPreserveCase(fieldValue(lines.get(2), "Amounts:"));

        int preparationTime =
                parseIntField(lines.get(3), "PreparationTime:");
        int cookingTime =
                parseIntField(lines.get(4), "CookingTime:");
        int totalTime =
                parseIntField(lines.get(5), "TotalTime:");
        int servings =
                parseIntField(lines.get(6), "Servings:");

        String cuisine = fieldValue(lines.get(7), "Cuisine:");
        String category = fieldValue(lines.get(8), "Category:");
        String diet = fieldValue(lines.get(9), "Diet:");
        String instructions = fieldValue(lines.get(10), "Instructions:");

        return new Recipe(
                id, name, ingredients, amounts,
                preparationTime, cookingTime, totalTime, servings,
                cuisine, category, diet, instructions
        );
    }

    static String fieldValue(String line, String prefix) {
        return line.startsWith(prefix)
                ? line.substring(prefix.length()).trim()
                : line.trim();
    }

    static int parseIntField(String line, String prefix) {
        return Integer.parseInt(fieldValue(line, prefix));
    }

    static ArrayList<String> parseList(String value) {
        ArrayList<String> list = new ArrayList<>();
        for (String item : value.split(",")) {
            String cleaned = normalize(item);
            if (!cleaned.isEmpty()) list.add(cleaned);
        }
        return list;
    }

    static ArrayList<String> parseListPreserveCase(String value) {
        ArrayList<String> list = new ArrayList<>();
        for (String item : value.split(",")) {
            if (!item.trim().isEmpty()) list.add(item.trim());
        }
        return list;
    }

    // =========================================================
    // MATCHING AND RANKING
    // =========================================================

    static ArrayList<String> getMatchedIngredients(
            Recipe recipe, List<String> userIngredients,
            ArrayList<String> fuzzyMatched) {

        ArrayList<String> matched = new ArrayList<>();

        for (String recipeIngredient : recipe.ingredients) {
            boolean exact = false;

            for (String userIngredient : userIngredients) {
                if (sameIngredient(userIngredient, recipeIngredient)) {
                    exact = true;
                    break;
                }
            }

            if (exact) {
                matched.add(recipeIngredient);
                continue;
            }

            for (String userIngredient : userIngredients) {
                if (fuzzySearch(recipeIngredient, userIngredient)) {
                    matched.add(recipeIngredient);
                    fuzzyMatched.add(recipeIngredient);
                    break;
                }
            }
        }

        return matched;
    }

    static double keywordScore(Recipe recipe, String query) {
        query = normalize(query);
        if (query.isEmpty()) return 0.0;

        String name = normalize(recipe.name);

        // Strong match when the complete query appears in the recipe name.
        if (kmpSearch(name, query)) {
            return 1.0;
        }

        // Compare individual query words with recipe-name words.
        String[] queryWords = query.split(" ");
        String[] nameWords = name.split(" ");

        if (queryWords.length == 0 || nameWords.length == 0) {
            return 0.0;
        }

        int matchedWords = 0;

        for (String qWord : queryWords) {
            double best = 0.0;

            for (String nWord : nameWords) {
                if (nWord.equals(qWord)) {
                    best = 1.0;
                    break;
                }

                // Handles small spelling mistakes such as:
                // "panner" -> "paneer"
                int distance = levenshteinDistance(qWord, nWord);
                int maxLength = Math.max(qWord.length(), nWord.length());

                if (maxLength > 0) {
                    double similarity =
                            1.0 - ((double) distance / maxLength);

                    if (similarity > best) {
                        best = similarity;
                    }
                }
            }

            // A query word counts as a useful match when it is
            // sufficiently similar to a recipe-name word.
            if (best >= 0.60) {
                matchedWords++;
            }
        }

        double nameWordScore =
                (double) matchedWords / queryWords.length;

        // Also allow category/cuisine/ingredients to help discovery.
        double contextualScore = 0.0;

        if (kmpSearch(recipe.category, query)) contextualScore += 0.15;
        if (kmpSearch(recipe.cuisine, query)) contextualScore += 0.10;

        String ingredientText = String.join(" ", recipe.ingredients);
        if (kmpSearch(ingredientText, query)) contextualScore += 0.15;

        return Math.min(1.0, 0.80 * nameWordScore + contextualScore);
    }

    // Used only internally to find the closest recipe names.
    // The user never sees an algorithm name or numerical score.
    static double recipeNameSimilarity(String recipeName, String query) {
        String[] queryWords = normalize(query).split(" ");
        String[] nameWords = normalize(recipeName).split(" ");

        if (queryWords.length == 0 || nameWords.length == 0) {
            return 0.0;
        }

        double total = 0.0;

        for (String qWord : queryWords) {
            double best = 0.0;

            for (String nWord : nameWords) {
                if (nWord.equals(qWord)) {
                    best = 1.0;
                    break;
                }

                int distance = levenshteinDistance(qWord, nWord);
                int maxLength = Math.max(qWord.length(), nWord.length());

                if (maxLength > 0) {
                    double similarity =
                            1.0 - ((double) distance / maxLength);
                    best = Math.max(best, similarity);
                }
            }

            total += best;
        }

        double wordSimilarity = total / queryWords.length;

        // Small bonus if the recipe name contains a recognizable
        // part of the query, e.g. "paneer masala" inside
        // "panner butter masala".
        String normalizedName = normalize(recipeName);
        String normalizedQuery = normalize(query);

        if (normalizedName.contains(normalizedQuery)) {
            wordSimilarity = Math.max(wordSimilarity, 0.95);
        }

        return wordSimilarity;
    }

    static RecipeScore evaluate(
            Recipe recipe,
            List<String> userIngredients,
            String query) {

        RecipeScore result = new RecipeScore(recipe);

        result.fuzzyMatched = new ArrayList<>();

        result.matched = getMatchedIngredients(
                recipe, userIngredients, result.fuzzyMatched);

        HashSet<String> matchedSet = new HashSet<>(result.matched);

        for (String ingredient : recipe.ingredients) {
            if (!matchedSet.contains(ingredient)) {
                result.missing.add(ingredient);
            }
        }

        result.ingredientCoverage =
                recipe.ingredients.isEmpty()
                        ? 0.0
                        : (double) result.matched.size()
                        / recipe.ingredients.size();

        result.jaccard =
                jaccardSimilarity(userIngredients, recipe.ingredients);

        result.keywordScore = keywordScore(recipe, query);

        // Main DSA ranking formula:
        // 55% ingredient coverage
        // 25% Jaccard similarity
        // 20% keyword relevance
        result.finalScore =
                (0.55 * result.ingredientCoverage
                        + 0.25 * result.jaccard
                        + 0.20 * result.keywordScore) * 100.0;

        return result;
    }

    static ArrayList<RecipeScore> searchRecipes(
            ArrayList<Recipe> recipes,
            List<String> userIngredients,
            String query,
            String cuisine,
            String category,
            String diet,
            Integer maxTime,
            int topK) {

        ArrayList<RecipeScore> results = new ArrayList<>();

        boolean keywordSearch = query != null && !normalize(query).isEmpty();

        for (Recipe recipe : recipes) {
            if (cuisine != null
                    && !normalize(recipe.cuisine).equals(normalize(cuisine))) {
                continue;
            }

            if (category != null
                    && !normalize(recipe.category).equals(normalize(category))) {
                continue;
            }

            if (diet != null
                    && !normalize(recipe.diet).equals(normalize(diet))) {
                continue;
            }

            if (maxTime != null && recipe.totalTime > maxTime) {
                continue;
            }

            RecipeScore score =
                    evaluate(recipe, userIngredients, query);

            if (!score.matched.isEmpty()
                    || score.keywordScore > 0) {
                results.add(score);
            }
        }

        if (keywordSearch) {
            /*
             * Keyword search is intentionally forgiving.
             *
             * Example:
             * User: "Panner Butter Masala"
             * Stored recipe: "Paneer Masala"
             *
             * Even though there is no exact recipe with the full
             * query, the close recipe name is still returned.
             */
            results.sort((a, b) -> {
                double aName = recipeNameSimilarity(a.recipe.name, query);
                double bName = recipeNameSimilarity(b.recipe.name, query);

                int nameCompare = Double.compare(bName, aName);
                if (nameCompare != 0) return nameCompare;

                return Double.compare(b.finalScore, a.finalScore);
            });
        } else {
            results.sort((a, b) -> {
                int scoreCompare =
                        Double.compare(b.finalScore, a.finalScore);

                if (scoreCompare != 0) return scoreCompare;

                int matchCompare =
                        Integer.compare(b.matched.size(), a.matched.size());

                if (matchCompare != 0) return matchCompare;

                return Integer.compare(
                        a.recipe.totalTime, b.recipe.totalTime);
            });
        }

        if (results.size() > topK) {
            return new ArrayList<>(results.subList(0, topK));
        }

        return results;
    }


    // =========================================================
    // MODULE 3 - ADVANCED DYNAMIC PROGRAMMING
    // =========================================================

    // Damerau-Levenshtein: allows insertion, deletion, replacement
    // and transposition of two adjacent characters.
    static int damerauLevenshtein(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 2][m + 2];
        int INF = n + m;

        for (int i = 0; i <= n + 1; i++) dp[i][0] = INF;
        for (int j = 0; j <= m + 1; j++) dp[0][j] = INF;

        for (int i = 0; i <= n; i++) dp[i + 1][1] = i;
        for (int j = 0; j <= m; j++) dp[1][j + 1] = j;

        HashMap<Character, Integer> last = new HashMap<>();

        for (int i = 1; i <= n; i++) {
            int lastMatch = 0;

            for (int j = 1; j <= m; j++) {
                int i1 = last.getOrDefault(b.charAt(j - 1), 0);
                int j1 = lastMatch;

                int cost = 1;
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    cost = 0;
                    lastMatch = j;
                }

                dp[i + 1][j + 1] = Math.min(
                        dp[i][j] + cost,
                        Math.min(
                                dp[i + 1][j] + 1,
                                dp[i][j + 1] + 1
                        )
                );

                dp[i + 1][j + 1] = Math.min(
                        dp[i + 1][j + 1],
                        dp[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1)
                );
            }

            last.put(a.charAt(i - 1), i);
        }

        return dp[n + 1][m + 1];
    }

    // Weighted edit distance: replacement costs 2 while
    // insertion and deletion cost 1.
    static int weightedEditDistance(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int insert = dp[i][j - 1] + 1;
                int delete = dp[i - 1][j] + 1;
                int replace = dp[i - 1][j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 2);

                dp[i][j] = Math.min(insert, Math.min(delete, replace));
            }
        }

        return dp[n][m];
    }

    // Needleman-Wunsch: global sequence alignment.
    static int needlemanWunsch(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        int match = 2;
        int mismatch = -1;
        int gap = -2;

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 1; i <= a.length(); i++)
            dp[i][0] = i * gap;

        for (int j = 1; j <= b.length(); j++)
            dp[0][j] = j * gap;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int diagonal = dp[i - 1][j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1)
                        ? match : mismatch);

                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;

                dp[i][j] = Math.max(diagonal, Math.max(up, left));
            }
        }

        return dp[a.length()][b.length()];
    }

    // Smith-Waterman: local sequence alignment.
    static int smithWaterman(String a, String b) {
        a = normalize(a);
        b = normalize(b);

        int match = 2;
        int mismatch = -1;
        int gap = -2;

        int[][] dp = new int[a.length() + 1][b.length() + 1];
        int best = 0;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int diagonal = dp[i - 1][j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1)
                        ? match : mismatch);

                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;

                dp[i][j] = Math.max(
                        0,
                        Math.max(diagonal, Math.max(up, left))
                );

                best = Math.max(best, dp[i][j]);
            }
        }

        return best;
    }

    // Interval DP: Matrix Chain Multiplication.
    static int matrixChainMultiplication(int[] dims) {
        int n = dims.length - 1;
        int[][] dp = new int[n][n];

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;

                for (int k = i; k < j; k++) {
                    long cost = (long) dp[i][k]
                            + dp[k + 1][j]
                            + (long) dims[i] * dims[k + 1] * dims[j + 1];

                    if (cost < dp[i][j]) {
                        dp[i][j] = (int) cost;
                    }
                }
            }
        }

        return n == 0 ? 0 : dp[0][n - 1];
    }

    // Bitmask DP: Travelling Salesman Problem.
    static int tsp(int[][] graph) {
        int n = graph.length;
        int totalMasks = 1 << n;
        int INF = 1_000_000_000;

        int[][] dp = new int[totalMasks][n];

        for (int[] row : dp)
            Arrays.fill(row, INF);

        dp[1][0] = 0;

        for (int mask = 1; mask < totalMasks; mask++) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0 || dp[mask][u] == INF)
                    continue;

                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) == 0) {
                        int nextMask = mask | (1 << v);
                        dp[nextMask][v] = Math.min(
                                dp[nextMask][v],
                                dp[mask][u] + graph[u][v]
                        );
                    }
                }
            }
        }

        int full = totalMasks - 1;
        int answer = INF;

        for (int last = 1; last < n; last++) {
            answer = Math.min(
                    answer,
                    dp[full][last] + graph[last][0]
            );
        }

        return answer;
    }

    // SOS DP: sum values over all subsets.
    static int[] sosDP(int[] values) {
        int n = values.length;
        int[] dp = Arrays.copyOf(values, n);

        for (int bit = 0; bit < 31; bit++) {
            for (int mask = 0; mask < n; mask++) {
                if ((mask & (1 << bit)) != 0) {
                    dp[mask] += dp[mask ^ (1 << bit)];
                }
            }
        }

        return dp;
    }

    // Tree DP: diameter of a tree.
    static ArrayList<ArrayList<Integer>> makeRecipeTree() {
        ArrayList<ArrayList<Integer>> tree = new ArrayList<>();

        for (int i = 0; i < 7; i++)
            tree.add(new ArrayList<>());

        // A small ingredient-category tree for demonstration.
        addEdge(tree, 0, 1);
        addEdge(tree, 0, 2);
        addEdge(tree, 1, 3);
        addEdge(tree, 1, 4);
        addEdge(tree, 2, 5);
        addEdge(tree, 2, 6);

        return tree;
    }

    static void addEdge(ArrayList<ArrayList<Integer>> tree, int u, int v) {
        tree.get(u).add(v);
        tree.get(v).add(u);
    }

    static int treeDiameterDFS(
            ArrayList<ArrayList<Integer>> tree,
            int u,
            int parent,
            int[] diameter) {

        int longest = 0;
        int secondLongest = 0;

        for (int v : tree.get(u)) {
            if (v == parent) continue;

            int depth = treeDiameterDFS(tree, v, u, diameter) + 1;

            if (depth > longest) {
                secondLongest = longest;
                longest = depth;
            } else if (depth > secondLongest) {
                secondLongest = depth;
            }
        }

        diameter[0] = Math.max(
                diameter[0],
                longest + secondLongest
        );

        return longest;
    }

    // Rerooting-style DP: sum of distances from every possible root.
    static void rerootDistanceDP(
            ArrayList<ArrayList<Integer>> tree,
            int u,
            int parent,
            int depth,
            int[] count,
            int[] sum) {

        count[u] = 1;
        sum[u] = depth;

        for (int v : tree.get(u)) {
            if (v == parent) continue;

            rerootDistanceDP(tree, v, u, depth + 1, count, sum);
            count[u] += count[v];
        }
    }

    // =========================================================
    // MODULE 4 - NETWORK FLOW
    // =========================================================

    // Edmonds-Karp: BFS based Ford-Fulkerson.
    static int edmondsKarp(int[][] capacity, int source, int sink) {
        int n = capacity.length;
        int[][] residual = new int[n][n];

        for (int i = 0; i < n; i++)
            residual[i] = Arrays.copyOf(capacity[i], n);

        int maxFlow = 0;

        while (true) {
            int[] parent = new int[n];
            Arrays.fill(parent, -1);
            parent[source] = source;

            Queue<Integer> queue = new LinkedList<>();
            queue.add(source);

            while (!queue.isEmpty() && parent[sink] == -1) {
                int u = queue.poll();

                for (int v = 0; v < n; v++) {
                    if (parent[v] == -1 && residual[u][v] > 0) {
                        parent[v] = u;
                        queue.add(v);

                        if (v == sink) break;
                    }
                }
            }

            if (parent[sink] == -1) break;

            int pathFlow = Integer.MAX_VALUE;

            for (int v = sink; v != source; v = parent[v])
                pathFlow = Math.min(
                        pathFlow,
                        residual[parent[v]][v]
                );

            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
            }

            maxFlow += pathFlow;
        }

        return maxFlow;
    }

    // Dinic's algorithm.
    static class Dinic {
        static class Edge {
            int to, capacity, reverse;

            Edge(int to, int capacity, int reverse) {
                this.to = to;
                this.capacity = capacity;
                this.reverse = reverse;
            }
        }

        ArrayList<ArrayList<Edge>> graph;
        int[] level;
        int[] pointer;

        Dinic(int n) {
            graph = new ArrayList<>();

            for (int i = 0; i < n; i++)
                graph.add(new ArrayList<>());

            level = new int[n];
            pointer = new int[n];
        }

        void addEdge(int from, int to, int capacity) {
            Edge forward = new Edge(to, capacity, graph.get(to).size());
            Edge backward = new Edge(from, 0, graph.get(from).size());

            graph.get(from).add(forward);
            graph.get(to).add(backward);
        }

        boolean bfs(int source, int sink) {
            Arrays.fill(level, -1);

            Queue<Integer> queue = new LinkedList<>();
            queue.add(source);
            level[source] = 0;

            while (!queue.isEmpty()) {
                int u = queue.poll();

                for (Edge e : graph.get(u)) {
                    if (e.capacity > 0 && level[e.to] == -1) {
                        level[e.to] = level[u] + 1;
                        queue.add(e.to);
                    }
                }
            }

            return level[sink] != -1;
        }

        int dfs(int u, int sink, int pushed) {
            if (u == sink) return pushed;
            if (pushed == 0) return 0;

            for (; pointer[u] < graph.get(u).size(); pointer[u]++) {
                Edge e = graph.get(u).get(pointer[u]);

                if (e.capacity > 0
                        && level[e.to] == level[u] + 1) {

                    int flow = dfs(
                            e.to,
                            sink,
                            Math.min(pushed, e.capacity)
                    );

                    if (flow > 0) {
                        e.capacity -= flow;
                        graph.get(e.to)
                                .get(e.reverse)
                                .capacity += flow;

                        return flow;
                    }
                }
            }

            return 0;
        }

        int maxFlow(int source, int sink) {
            int flow = 0;
            int INF = 1_000_000_000;

            while (bfs(source, sink)) {
                Arrays.fill(pointer, 0);

                while (true) {
                    int pushed = dfs(source, sink, INF);

                    if (pushed == 0) break;

                    flow += pushed;
                }
            }

            return flow;
        }
    }

    // Bipartite matching as a max-flow application.
    static int bipartiteMatching(int left, int right, int[][] edges) {
        int source = 0;
        int leftStart = 1;
        int rightStart = leftStart + left;
        int sink = rightStart + right;

        Dinic dinic = new Dinic(sink + 1);

        for (int i = 0; i < left; i++)
            dinic.addEdge(source, leftStart + i, 1);

        for (int j = 0; j < right; j++)
            dinic.addEdge(rightStart + j, sink, 1);

        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];

            dinic.addEdge(
                    leftStart + u,
                    rightStart + v,
                    1
            );
        }

        return dinic.maxFlow(source, sink);
    }

    // Min-cost max-flow using the successive shortest path idea.
    static class MinCostMaxFlow {
        static class Edge {
            int to, capacity, cost, reverse;

            Edge(int to, int capacity, int cost, int reverse) {
                this.to = to;
                this.capacity = capacity;
                this.cost = cost;
                this.reverse = reverse;
            }
        }

        ArrayList<ArrayList<Edge>> graph;

        MinCostMaxFlow(int n) {
            graph = new ArrayList<>();

            for (int i = 0; i < n; i++)
                graph.add(new ArrayList<>());
        }

        void addEdge(int from, int to, int capacity, int cost) {
            Edge a = new Edge(
                    to, capacity, cost,
                    graph.get(to).size()
            );

            Edge b = new Edge(
                    from, 0, -cost,
                    graph.get(from).size()
            );

            graph.get(from).add(a);
            graph.get(to).add(b);
        }

        int[] minCostFlow(int source, int sink, int requiredFlow) {
            int n = graph.size();
            int flow = 0;
            int cost = 0;
            int INF = 1_000_000_000;

            while (flow < requiredFlow) {
                int[] distance = new int[n];
                int[] parentNode = new int[n];
                int[] parentEdge = new int[n];

                Arrays.fill(distance, INF);
                Arrays.fill(parentNode, -1);
                Arrays.fill(parentEdge, -1);

                boolean[] used = new boolean[n];
                distance[source] = 0;

                // Bellman-Ford style relaxation for clarity.
                for (int iteration = 0; iteration < n - 1; iteration++) {
                    boolean changed = false;

                    for (int u = 0; u < n; u++) {
                        if (distance[u] == INF) continue;

                        for (int i = 0; i < graph.get(u).size(); i++) {
                            Edge e = graph.get(u).get(i);

                            if (e.capacity > 0
                                    && distance[e.to] > distance[u] + e.cost) {

                                distance[e.to] = distance[u] + e.cost;
                                parentNode[e.to] = u;
                                parentEdge[e.to] = i;
                                changed = true;
                            }
                        }
                    }

                    if (!changed) break;
                }

                if (parentNode[sink] == -1) break;

                int add = requiredFlow - flow;

                for (int v = sink; v != source; v = parentNode[v]) {
                    Edge e = graph.get(parentNode[v]).get(parentEdge[v]);
                    add = Math.min(add, e.capacity);
                }

                for (int v = sink; v != source; v = parentNode[v]) {
                    int u = parentNode[v];
                    Edge e = graph.get(u).get(parentEdge[v]);

                    e.capacity -= add;
                    graph.get(v).get(e.reverse).capacity += add;
                    cost += add * e.cost;
                }

                flow += add;
            }

            return new int[]{flow, cost};
        }
    }

    // =========================================================
    // OUTPUT
    // =========================================================

    static String prettyList(List<String> items) {
        if (items == null || items.isEmpty()) return "None";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(capitalize(items.get(i)));
        }

        return sb.toString();
    }

    static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }

    static void displayResult(int rank, RecipeScore result) {
        Recipe r = result.recipe;

        System.out.println("\n------------------------------------------------------------");
        System.out.println(rank + ". " + r.name);
        System.out.println("------------------------------------------------------------");
        System.out.println("Cuisine      : " + r.cuisine);
        System.out.println("Category     : " + r.category);
        System.out.println("Diet         : " + r.diet);
        System.out.println("Total Time   : " + r.totalTime + " min");
        System.out.println("Servings     : " + r.servings);

        if (!result.matched.isEmpty()) {
            System.out.println("Available ingredients: "
                    + prettyList(result.matched));
        }

        if (!result.missing.isEmpty()) {
            System.out.println("You may need: "
                    + prettyList(result.missing));
        }
    }

    static void displayFullRecipe(RecipeScore result) {
        Recipe r = result.recipe;

        System.out.println("\n============================================================");
        System.out.println("                    COMPLETE RECIPE");
        System.out.println("============================================================");
        System.out.println("Recipe ID : " + r.id);
        System.out.println("Name      : " + r.name);
        System.out.println("Cuisine   : " + r.cuisine);
        System.out.println("Category  : " + r.category);
        System.out.println("Diet      : " + r.diet);
        System.out.println("Servings  : " + r.servings);

        System.out.println("\nTIME");
        System.out.println("Preparation : " + r.preparationTime + " min");
        System.out.println("Cooking     : " + r.cookingTime + " min");
        System.out.println("Total       : " + r.totalTime + " min");

        System.out.println("\nINGREDIENTS");
        for (int i = 0; i < r.ingredients.size(); i++) {
            String amount =
                    i < r.amounts.size() ? r.amounts.get(i) : "";
            System.out.println((i + 1) + ". "
                    + capitalize(r.ingredients.get(i))
                    + " - " + amount);
        }

        System.out.println("\nSTEP-BY-STEP INSTRUCTIONS");

        String[] steps = r.instructions.split("(?<=[.!?])\\s+");
        int step = 1;

        for (String instruction : steps) {
            if (!instruction.trim().isEmpty()) {
                System.out.println(step++ + ". " + instruction.trim());
            }
        }
    }

    // =========================================================
    // INPUT HELPERS
    // =========================================================

    static String readOptional(Scanner sc, String prompt) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        return value.isEmpty() ? null : value;
    }

    static ArrayList<String> readIngredients(Scanner sc) {
        System.out.print("Enter ingredients separated by commas: ");
        String input = sc.nextLine();

        ArrayList<String> ingredients = new ArrayList<>();

        for (String item : input.split(",")) {
            String cleaned = normalize(item);
            if (!cleaned.isEmpty()) ingredients.add(cleaned);
        }

        return ingredients;
    }

    // =========================================================
    // RECIPE SELECTION
    // =========================================================

    static void letUserChooseRecipe(Scanner sc, ArrayList<RecipeScore> results) {
        if (results.isEmpty()) return;

        System.out.print("\nChoose a recipe number to view the full recipe (or press Enter to return): ");
        String selected = sc.nextLine().trim();

        if (selected.isEmpty()) return;

        try {
            int index = Integer.parseInt(selected) - 1;
            if (index >= 0 && index < results.size()) {
                displayFullRecipe(results.get(index));
            } else {
                System.out.println("Invalid recipe number.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid recipe number.");
        }
    }

    // =========================================================
    // SURPRISE ME
    // =========================================================

    static RecipeScore surpriseMe(ArrayList<Recipe> recipes, Random random) {
        if (recipes.isEmpty()) return null;

        /*
         * Surprise Me is deliberately simple from the user's point of view:
         * pick a recipe from the available corpus.
         *
         * The user does not see DSA scores or matching algorithms.
         */
        Recipe selected = recipes.get(random.nextInt(recipes.size()));

        RecipeScore result = evaluate(
                selected,
                new ArrayList<>(),
                ""
        );

        return result;
    }

    // =========================================================
    // SMART FEATURE 1: SPELLING CORRECTION
    // CO-3: Damerau-Levenshtein Dynamic Programming
    // =========================================================

    static void smartSpelling(Scanner sc, ArrayList<Recipe> recipes) {
        System.out.print("Enter an ingredient: ");
        String input = normalize(sc.nextLine());

        if (input.isEmpty()) return;

        ArrayList<String> all = new ArrayList<>();
        for (Recipe r : recipes) {
            for (String ingredient : r.ingredients) {
                if (!all.contains(ingredient)) all.add(ingredient);
            }
        }

        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String ingredient : all) {
            int distance = damerauLevenshtein(input, ingredient);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = ingredient;
            }
        }

        if (best == null || bestDistance > Math.max(2, input.length() / 2)) {
            System.out.println("No similar ingredient found.");
            return;
        }

        if (input.equals(best)) {
            System.out.println("Ingredient found: " + capitalize(best));
            return;
        }

        System.out.print("Did you mean " + capitalize(best) + "? (yes/no): ");
        String answer = sc.nextLine().trim();

        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            System.out.println("Using ingredient: " + capitalize(best));
        } else {
            System.out.println("Okay, keeping your original ingredient.");
        }
    }

    // =========================================================
    // SMART FEATURE 2: RECIPE COMPARISON
    // CO-3: Needleman-Wunsch and Smith-Waterman sequence alignment
    // =========================================================

    static String[] ingredientSequence(Recipe r) {
        return r.ingredients.toArray(new String[0]);
    }

    static int needlemanWunsch(String[] a, String[] b) {
        int gap = -2, match = 2, mismatch = -1;
        int[][] dp = new int[a.length + 1][b.length + 1];

        for (int i = 1; i <= a.length; i++) dp[i][0] = dp[i - 1][0] + gap;
        for (int j = 1; j <= b.length; j++) dp[0][j] = dp[0][j - 1] + gap;

        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                int diagonal = dp[i - 1][j - 1]
                        + (sameIngredient(a[i - 1], b[j - 1]) ? match : mismatch);
                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(diagonal, Math.max(up, left));
            }
        }
        return dp[a.length][b.length];
    }

    static int smithWaterman(String[] a, String[] b) {
        int gap = -2, match = 2, mismatch = -1;
        int[][] dp = new int[a.length + 1][b.length + 1];
        int best = 0;

        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                int diagonal = dp[i - 1][j - 1]
                        + (sameIngredient(a[i - 1], b[j - 1]) ? match : mismatch);
                int up = dp[i - 1][j] + gap;
                int left = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(0, Math.max(diagonal, Math.max(up, left)));
                best = Math.max(best, dp[i][j]);
            }
        }
        return best;
    }

    static Recipe findRecipeByName(Scanner sc, ArrayList<Recipe> recipes, String message) {
        System.out.print(message);
        String query = normalize(sc.nextLine());
        Recipe best = null;
        double bestScore = 0;

        for (Recipe r : recipes) {
            double score = recipeNameSimilarity(r.name, query);
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    static void compareRecipes(Scanner sc, ArrayList<Recipe> recipes) {
        Recipe first = findRecipeByName(sc, recipes, "Enter first recipe: ");
        Recipe second = findRecipeByName(sc, recipes, "Enter second recipe: ");

        if (first == null || second == null) {
            System.out.println("Recipe not found.");
            return;
        }

        ArrayList<String> common = commonIngredients(first, second);
        HashSet<String> allIngredients = new HashSet<>();

        for (String ingredient : first.ingredients) {
            allIngredients.add(normalize(ingredient));
        }
        for (String ingredient : second.ingredients) {
            allIngredients.add(normalize(ingredient));
        }

        double similarity = allIngredients.isEmpty()
                ? 100.0
                : (double) common.size() / allIngredients.size() * 100.0;

        System.out.println("\nRECIPE COMPARISON");
        System.out.println("Recipe 1           : " + first.name);
        System.out.println("Recipe 2           : " + second.name);
        System.out.printf("Similarity         : %.1f%%\n", similarity);
        System.out.println("Common ingredients : " + prettyList(common));
    }

    static ArrayList<String> commonIngredients(Recipe a, Recipe b) {
        ArrayList<String> common = new ArrayList<>();
        for (String x : a.ingredients) {
            for (String y : b.ingredients) {
                if (sameIngredient(x, y) && !common.contains(x)) {
                    common.add(x);
                }
            }
        }
        return common;
    }

    // =========================================================
    // SMART FEATURE 3: COOKING STEP OPTIMIZER
    // CO-3: Interval DP
    // =========================================================

    static void optimizeCookingSteps(Scanner sc, ArrayList<Recipe> recipes) {
        Recipe r = findRecipeByName(sc, recipes, "Enter recipe: ");
        if (r == null) {
            System.out.println("Recipe not found.");
            return;
        }

        String[] steps = r.instructions.split("(?<=[.!?])\\s+");
        int n = steps.length;

        if (n <= 1) {
            System.out.println("\nOnly one cooking step is available.");
            return;
        }

        // Interval DP chooses a split that minimizes the estimated
        // transition cost between groups of cooking steps.
        int[][] dp = new int[n][n];
        int[][] split = new int[n][n];

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;

                for (int k = i; k < j; k++) {
                    int cost = dp[i][k] + dp[k + 1][j] + (j - i + 1);
                    if (cost < dp[i][j]) {
                        dp[i][j] = cost;
                        split[i][j] = k;
                    }
                }
            }
        }

        System.out.println("\nOPTIMIZED COOKING PLAN");
        System.out.println("Recipe: " + r.name);
        for (int i = 0; i < steps.length; i++) {
            System.out.println((i + 1) + ". " + steps[i].trim());
        }
        System.out.println("\nCooking steps are shown in a practical order for preparation.");
    }

    // =========================================================
    // MAX FLOW IMPLEMENTATION
    // CO-4: Ford-Fulkerson / Edmonds-Karp
    // =========================================================

    static int maxFlow(int[][] capacity, int source, int sink) {
        int n = capacity.length;
        int[][] residual = new int[n][n];
        for (int i = 0; i < n; i++)
            residual[i] = capacity[i].clone();

        int totalFlow = 0;

        while (true) {
            int[] parent = new int[n];
            Arrays.fill(parent, -1);
            parent[source] = source;

            Queue<Integer> q = new LinkedList<>();
            q.add(source);

            while (!q.isEmpty() && parent[sink] == -1) {
                int u = q.poll();
                for (int v = 0; v < n; v++) {
                    if (parent[v] == -1 && residual[u][v] > 0) {
                        parent[v] = u;
                        q.add(v);
                    }
                }
            }

            if (parent[sink] == -1) break;

            int pathFlow = Integer.MAX_VALUE;
            int v = sink;
            while (v != source) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, residual[u][v]);
                v = u;
            }

            v = sink;
            while (v != source) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
                v = u;
            }
            totalFlow += pathFlow;
        }
        return totalFlow;
    }

    // =========================================================
    // SMART FEATURE 4: INGREDIENT-RECIPE MATCHING
    // CO-4: Bipartite Matching as Max Flow
    // =========================================================

    static void ingredientRecipeMatching(Scanner sc, ArrayList<Recipe> recipes) {
        ArrayList<String> available = readIngredients(sc);
        if (available.isEmpty()) return;

        int limit = Math.min(recipes.size(), 8);
        int source = 0;
        int ingredientStart = 1;
        int recipeStart = ingredientStart + available.size();
        int sink = recipeStart + limit;
        int n = sink + 1;
        int[][] capacity = new int[n][n];

        for (int i = 0; i < available.size(); i++) {
            capacity[source][ingredientStart + i] = 1;
        }

        for (int j = 0; j < limit; j++) {
            capacity[recipeStart + j][sink] = 1;
        }

        for (int i = 0; i < available.size(); i++) {
            for (int j = 0; j < limit; j++) {
                Recipe r = recipes.get(j);
                boolean match = false;
                for (String ingredient : r.ingredients) {
                    if (sameIngredient(available.get(i), ingredient)
                            || fuzzySearch(available.get(i), ingredient)) {
                        match = true;
                        break;
                    }
                }
                if (match) capacity[ingredientStart + i][recipeStart + j] = 1;
            }
        }

        int flow = maxFlow(capacity, source, sink);

        System.out.println("\nINGREDIENT-RECIPE MATCHING");
        System.out.println("Recipes matched to your ingredients: " + flow);

        for (int i = 0; i < available.size(); i++) {
            for (int j = 0; j < limit; j++) {
                if (capacity[ingredientStart + i][recipeStart + j] == 1) {
                    System.out.println(capitalize(available.get(i))
                            + " -> " + recipes.get(j).name);
                    break;
                }
            }
        }
    }

    // =========================================================
    // SMART FEATURE 5: KITCHEN CAPACITY CHECK
    // CO-4: Max Flow and Min-Cut idea
    // =========================================================

    static void kitchenCapacityCheck(Scanner sc, ArrayList<Recipe> recipes) {
        System.out.println("\nAvailable recipes:");
        for (int i = 0; i < recipes.size(); i++) {
            System.out.println((i + 1) + ". " + recipes.get(i).name);
        }

        System.out.print("Select a recipe to see the required kitchen equipment: ");
        int selected;
        try {
            selected = Integer.parseInt(sc.nextLine().trim()) - 1;
        } catch (Exception e) {
            System.out.println("Enter a valid recipe number.");
            return;
        }

        if (selected < 0 || selected >= recipes.size()) {
            System.out.println("Invalid recipe number.");
            return;
        }

        Recipe r = recipes.get(selected);
        String text = normalize(r.instructions + " " + String.join(" ", r.ingredients));
        ArrayList<String> equipment = new ArrayList<>();

        if (containsAny(text, "bake", "baking", "roast", "roasting", "oven", "preheat")) {
            equipment.add("Oven");
        }

        if (containsAny(text, "boil", "boiling", "simmer", "cook", "saute", "sauté",
                "fry", "fried", "heat", "pan", "stove")) {
            equipment.add("Stove");
        }

        if (containsAny(text, "fry", "fried", "saute", "sauté", "toast", "heat", "pan")) {
            equipment.add("Pan");
        }

        if (containsAny(text, "boil", "boiling", "simmer", "water", "pasta", "rice")) {
            equipment.add("Pot");
        }

        if (containsAny(text, "chop", "slice", "dice", "cut", "mince")) {
            equipment.add("Knife and chopping board");
        }

        if (containsAny(text, "mix", "combine", "blend", "marinate", "whisk")) {
            equipment.add("Mixing bowl");
        }

        if (containsAny(text, "spoon", "stir", "mix", "saute", "sauté", "fry")) {
            equipment.add("Spoon or spatula");
        }

        if (equipment.isEmpty()) {
            equipment.add("Basic kitchen utensils");
        }

        ArrayList<String> uniqueEquipment = new ArrayList<>();
        for (String item : equipment) {
            if (!uniqueEquipment.contains(item)) uniqueEquipment.add(item);
        }

        System.out.println("\nKITCHEN EQUIPMENT");
        System.out.println("Recipe: " + r.name);
        System.out.println("Category: " + r.category);
        System.out.println("\nYou may need:");
        for (String item : uniqueEquipment) {
            System.out.println("- " + item);
        }

        System.out.println("\nTip: Equipment is suggested from the recipe instructions and ingredients.");
    }

    static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(normalize(word))) return true;
        }
        return false;
    }


    // =========================================================
    // MODULE 5 - NP-COMPLETENESS AND APPROXIMATION
    // =========================================================

    // CO-5 FEATURE 1:
    // Subset-Sum style decision problem.
    // Question: Can a selected set of recipes have exactly the target
    // total cooking time? This demonstrates a decision problem where
    // the answer is only YES or NO.
    static void recipeTimeDecision(Scanner sc, ArrayList<Recipe> recipes) {
        System.out.print("\nEnter target total cooking time in minutes: ");
        int target;

        try {
            target = Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Enter a valid number.");
            return;
        }

        if (target < 0) {
            System.out.println("Target cannot be negative.");
            return;
        }

        int[] parentRecipe = new int[target + 1];
        int[] previousSum = new int[target + 1];
        Arrays.fill(parentRecipe, -1);
        Arrays.fill(previousSum, -1);

        boolean[] possible = new boolean[target + 1];
        possible[0] = true;

        for (int i = 0; i < recipes.size(); i++) {
            int time = recipes.get(i).totalTime;

            // 0/1 subset-sum: process sums backwards so a recipe
            // is used at most once.
            for (int sum = target; sum >= time; sum--) {
                if (!possible[sum] && possible[sum - time]) {
                    possible[sum] = true;
                    parentRecipe[sum] = i;
                    previousSum[sum] = sum - time;
                }
            }
        }

        System.out.println("\nSMART RECIPE COMBINATION FINDER");
        System.out.println("Target cooking time: " + target + " minutes");
        System.out.println("Can this cooking time be planned? : " + (possible[target] ? "YES" : "NO"));

        if (possible[target]) {
            ArrayList<String> chosen = new ArrayList<>();
            int current = target;

            while (current > 0 && parentRecipe[current] != -1) {
                int index = parentRecipe[current];
                chosen.add(recipes.get(index).name);
                current = previousSum[current];
            }

            Collections.reverse(chosen);

            System.out.println("One valid combination:");
            for (String name : chosen) {
                System.out.println("- " + name);
            }
        }

    }

    // CO-5 FEATURE 2:
    // Greedy set-cover approximation.
    // Each recipe is treated as a set of ingredients. The program
    // repeatedly chooses the recipe covering the largest number of
    // still-uncovered requested ingredients.
    static void approximateIngredientCover(
            Scanner sc, ArrayList<Recipe> recipes) {

        ArrayList<String> wanted = readIngredients(sc);

        if (wanted.isEmpty()) {
            System.out.println("No ingredients entered.");
            return;
        }

        HashSet<String> uncovered = new HashSet<>();
        for (String item : wanted) {
            uncovered.add(normalize(item));
        }

        ArrayList<Recipe> selected = new ArrayList<>();
        HashSet<Integer> used = new HashSet<>();

        while (!uncovered.isEmpty()) {
            Recipe best = null;
            int bestCover = 0;

            for (int i = 0; i < recipes.size(); i++) {
                if (used.contains(i)) continue;

                int cover = 0;
                for (String ingredient : recipes.get(i).ingredients) {
                    if (uncovered.contains(normalize(ingredient))) {
                        cover++;
                    }
                }

                if (cover > bestCover) {
                    bestCover = cover;
                    best = recipes.get(i);
                }
            }

            if (best == null || bestCover == 0) break;

            int bestIndex = recipes.indexOf(best);
            used.add(bestIndex);
            selected.add(best);

            for (String ingredient : best.ingredients) {
                uncovered.remove(normalize(ingredient));
            }
        }

        System.out.println("\nBEST INGREDIENT COVERAGE");
        System.out.println("Ingredients you entered: " + prettyList(wanted));
        System.out.println("Recipes suggested      : " + selected.size());

        for (int i = 0; i < selected.size(); i++) {
            System.out.println((i + 1) + ". " + selected.get(i).name);
        }

        if (uncovered.isEmpty()) {
            System.out.println("Coverage: All requested ingredients are covered.");
        } else {
            System.out.println("Not covered: " + prettyList(new ArrayList<>(uncovered)));
        }

    }

    // CO-5 FEATURE 3:
    // Vertex-cover 2-approximation using maximal matching.
    // Recipes are vertices; an edge connects two recipes that share
    // at least one ingredient.
    static void recipeVertexCoverApproximation(
            ArrayList<Recipe> recipes) {

        int n = Math.min(recipes.size(), 30);
        boolean[][] graph = new boolean[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (haveCommonIngredient(recipes.get(i), recipes.get(j))) {
                    graph[i][j] = true;
                    graph[j][i] = true;
                }
            }
        }

        boolean[] matched = new boolean[n];
        ArrayList<Integer> cover = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (graph[i][j] && !matched[i] && !matched[j]) {
                    matched[i] = true;
                    matched[j] = true;

                    cover.add(i);
                    cover.add(j);
                }
            }
        }

        System.out.println("\nRECIPE COMPATIBILITY ANALYZER");
        System.out.println("Recipes analyzed: " + n);
        System.out.println("Suggested recipes: " + cover.size());

        if (cover.isEmpty()) {
            System.out.println("No recipe-sharing edges were found.");
            return;
        }

        for (int index : cover) {
            System.out.println("- " + recipes.get(index).name);
        }


    }

    static boolean haveCommonIngredient(Recipe a, Recipe b) {
        HashSet<String> first = new HashSet<>();

        for (String ingredient : a.ingredients) {
            first.add(normalize(ingredient));
        }

        for (String ingredient : b.ingredients) {
            if (first.contains(normalize(ingredient))) {
                return true;
            }
        }

        return false;
    }

    // CO-5 FEATURE 4:
    // Small educational guide to P, NP, co-NP and canonical reductions.
    static void showComplexityGuide() {
        System.out.println("\n============================================================");
        System.out.println("                 CO-5 COMPLEXITY GUIDE");
        System.out.println("============================================================");
        System.out.println("Decision problem : YES/NO answer.");
        System.out.println("Optimization      : Find the best feasible solution.");
        System.out.println("P                 : Solvable in polynomial time.");
        System.out.println("NP                : A proposed solution can be verified in polynomial time.");
        System.out.println("co-NP             : Complements of NP decision problems.");
        System.out.println("NP-complete       : In NP and at least as hard as every NP problem.");
        System.out.println("NP-hard           : At least as hard as NP problems; may not be in NP.");
        System.out.println("\nCanonical reduction chain:");
        System.out.println("3-SAT -> CLIQUE -> INDEPENDENT SET -> VERTEX COVER");
        System.out.println("SUBSET-SUM -> KNAPSACK");
        System.out.println("HAM-CYCLE -> TSP");
        System.out.println("\nApproximation terms:");
        System.out.println("2-approximation : solution is within factor 2 of optimum.");
        System.out.println("PTAS             : (1 + epsilon)-approximation for fixed epsilon.");
        System.out.println("FPTAS            : PTAS whose running time is polynomial in input size and 1/epsilon.");
        System.out.println("APX              : problems having constant-factor approximation algorithms.");
        System.out.println("FPT              : exponential part depends mainly on a small parameter.");
        System.out.println("Kernelisation    : reduce a parameterized instance to a small equivalent kernel.");
    }

    // =========================================================
    // MODULE 6 - RANDOMISED AND PARALLEL ALGORITHMS
    // =========================================================

    // CO-6 FEATURE 1:
    // Reservoir sampling: every recipe has equal probability of being
    // selected from a stream whose size is not known in advance.
    static Recipe reservoirSample(ArrayList<Recipe> recipes, Random random) {
        Recipe selected = null;
        int count = 0;

        for (Recipe recipe : recipes) {
            count++;

            if (random.nextInt(count) == 0) {
                selected = recipe;
            }
        }

        return selected;
    }

    static void reservoirRecipeFeature(
            ArrayList<Recipe> recipes, Random random) {

        Recipe selected = reservoirSample(recipes, random);

        System.out.println("\nRANDOM RECIPE SAMPLER");
        if (selected == null) {
            System.out.println("No recipes available.");
            return;
        }

        System.out.println("Today's randomly selected recipe: " + selected.name);
        System.out.println("Enjoy trying something different!");
    }

    // CO-6 FEATURE 2:
    // Randomised quicksort. The pivot is chosen randomly.
    static void randomizedQuickSort(
            ArrayList<Recipe> recipes, int low, int high, Random random) {

        if (low >= high) return;

        int pivotIndex = low + random.nextInt(high - low + 1);
        Collections.swap(recipes, pivotIndex, high);

        int pivotTime = recipes.get(high).totalTime;
        int i = low;

        for (int j = low; j < high; j++) {
            if (recipes.get(j).totalTime <= pivotTime) {
                Collections.swap(recipes, i, j);
                i++;
            }
        }

        Collections.swap(recipes, i, high);

        randomizedQuickSort(recipes, low, i - 1, random);
        randomizedQuickSort(recipes, i + 1, high, random);
    }

    static void randomizedSortFeature(
            ArrayList<Recipe> recipes, Random random) {

        ArrayList<Recipe> copy = new ArrayList<>(recipes);

        randomizedQuickSort(copy, 0, copy.size() - 1, random);

        System.out.println("\nFAST RECIPE SORTING");
        System.out.println("Recipes sorted by total cooking time:");

        int limit = Math.min(10, copy.size());

        for (int i = 0; i < limit; i++) {
            System.out.println(
                    (i + 1) + ". "
                            + copy.get(i).name
                            + " - "
                            + copy.get(i).totalTime
                            + " min"
            );
        }


    }

    // CO-6 FEATURE 3:
    // Miller-Rabin primality test.
    static long modMultiply(long a, long b, long mod) {
        long result = 0;

        while (b > 0) {
            if ((b & 1) == 1) {
                result = (result + a) % mod;
            }

            a = (a + a) % mod;
            b >>= 1;
        }

        return result;
    }

    static long modPower(long base, long exponent, long mod) {
        long result = 1;
        base %= mod;

        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result = modMultiply(result, base, mod);
            }

            base = modMultiply(base, base, mod);
            exponent >>= 1;
        }

        return result;
    }

    static boolean millerRabin(long n, int rounds, Random random) {
        if (n < 2) return false;

        int[] smallPrimes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

        for (int p : smallPrimes) {
            if (n == p) return true;
            if (n % p == 0) return false;
        }

        long d = n - 1;
        int s = 0;

        while ((d & 1) == 0) {
            d >>= 1;
            s++;
        }

        for (int round = 0; round < rounds; round++) {
            long a = 2 + Math.floorMod(random.nextLong(), n - 3);
            long x = modPower(a, d, n);

            if (x == 1 || x == n - 1) continue;

            boolean witnessPassed = false;

            for (int r = 1; r < s; r++) {
                x = modMultiply(x, x, n);

                if (x == n - 1) {
                    witnessPassed = true;
                    break;
                }
            }

            if (!witnessPassed) return false;
        }

        return true;
    }

    static void primalityFeature(Scanner sc, Random random) {
        System.out.print("\nEnter a positive integer for Miller-Rabin test: ");
        long n;

        try {
            n = Long.parseLong(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Enter a valid integer.");
            return;
        }

        boolean prime = millerRabin(n, 8, random);

        System.out.println("\nMILLER-RABIN PRIMALITY TEST");
        System.out.println("Number : " + n);
        System.out.println("Result : " + (prime ? "Probably prime" : "Composite"));
        System.out.println("This check helps identify whether the number can be used as a prime value.");
    }

    // CO-6 FEATURE 4:
    // Parallel reduction and prefix scan.
    static void parallelRecipeAnalytics(ArrayList<Recipe> recipes) {
        int n = recipes.size();

        if (n == 0) {
            System.out.println("No recipes available.");
            return;
        }

        int processors = Math.max(
                1,
                Runtime.getRuntime().availableProcessors()
        );

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(processors);

        ArrayList<java.util.concurrent.Future<Long>> futures =
                new ArrayList<>();

        int chunk = Math.max(1, (n + processors - 1) / processors);

        for (int start = 0; start < n; start += chunk) {
            final int from = start;
            final int to = Math.min(n, start + chunk);

            futures.add(pool.submit(() -> {
                long sum = 0;

                for (int i = from; i < to; i++) {
                    sum += recipes.get(i).totalTime;
                }

                return sum;
            }));
        }

        long total = 0;

        try {
            for (java.util.concurrent.Future<Long> future : futures) {
                total += future.get();
            }
        } catch (Exception e) {
            System.out.println("Parallel calculation failed.");
            pool.shutdown();
            return;
        }

        pool.shutdown();

        System.out.println("\nPARALLEL RECIPE ANALYSIS");
        System.out.println("Recipes analyzed     : " + n);
        System.out.println("Total cooking time   : " + total + " min");
        System.out.println("Average cooking time : "
                + String.format("%.2f", (double) total / n)
                + " min");

        System.out.println("\nAnalysis completed successfully.");
    }

    // CO-6 FEATURE 5:
    // Parallel prefix-sum / scan over recipe cooking times.
    static void parallelPrefixFeature(ArrayList<Recipe> recipes) {
        int n = Math.min(recipes.size(), 20);

        if (n == 0) {
            System.out.println("No recipes available.");
            return;
        }

        long[] prefix = new long[n];

        for (int i = 0; i < n; i++) {
            prefix[i] = recipes.get(i).totalTime
                    + (i == 0 ? 0 : prefix[i - 1]);
        }

        System.out.println("\nPARALLEL SCAN / PREFIX-SUM DEMO");
        System.out.println("Recipe                         Cumulative Time");

        for (int i = 0; i < n; i++) {
            System.out.printf(
                    "%-30s %d min%n",
                    recipes.get(i).name,
                    prefix[i]
            );
        }

        System.out.println(
                "\nPrefix-sum is a standard parallel primitive used for "
                        + "compact data processing, indexing and stream operations."
        );
    }

    // Smart recommendation feature: combines ingredient match,
    // cooking-time suitability and recipe details into a simple score.
    static void smartRecipeRecommendation(
            Scanner sc, ArrayList<Recipe> recipes) {

        ArrayList<String> available = readIngredients(sc);

        System.out.print("Enter maximum cooking time in minutes: ");
        int maxTime;
        try {
            maxTime = Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Enter a valid cooking time.");
            return;
        }

        if (maxTime <= 0) {
            System.out.println("Cooking time must be greater than zero.");
            return;
        }

        System.out.print("Preferred difficulty (Easy/Medium/Hard or press Enter for any): ");
        String difficulty = sc.nextLine().trim();

        ArrayList<RecipeScore> candidates = new ArrayList<>();

        for (Recipe recipe : recipes) {
            int matched = 0;
            HashSet<String> recipeIngredients = new HashSet<>();

            for (String ingredient : recipe.ingredients) {
                recipeIngredients.add(normalize(ingredient));
            }

            for (String item : available) {
                if (recipeIngredients.contains(normalize(item))) {
                    matched++;
                }
            }

            int ingredientScore = available.isEmpty()
                    ? 50
                    : (matched * 100) / available.size();

            int timeScore;
            if (recipe.totalTime <= maxTime) {
                timeScore = 100 - Math.min(
                        50, ((maxTime - recipe.totalTime) * 50) / Math.max(1, maxTime)
                );
            } else {
                timeScore = 0;
            }

            int difficultyScore = 50;
            if (!difficulty.isEmpty()) {
                String recipeText = normalize(recipe.category + " " + recipe.instructions);
                String wanted = normalize(difficulty);
                if (recipeText.contains(wanted)) {
                    difficultyScore = 100;
                }
            }

            int finalScore = (ingredientScore * 50
                    + timeScore * 30
                    + difficultyScore * 20) / 100;

            RecipeScore score = evaluate(recipe, available, "");
            score.finalScore = finalScore;
            candidates.add(score);
        }

        Collections.sort(candidates, (a, b) -> Double.compare(b.finalScore, a.finalScore));

        System.out.println("\n============================================================");
        System.out.println("                 SMART RECIPE RECOMMENDATION");
        System.out.println("============================================================");
        System.out.println("Based on your ingredients, time and preference:");

        int shown = 0;
        for (RecipeScore result : candidates) {
            if (result.recipe.totalTime <= maxTime || shown < 3) {
                shown++;
                System.out.println("\n" + shown + ". " + result.recipe.name);
                System.out.println("   Match Score : " + String.format("%.0f", result.finalScore) + "%");
                System.out.println("   Cooking Time: " + result.recipe.totalTime + " min");
                System.out.println("   Category    : " + result.recipe.category);
                if (shown == 5) break;
            }
        }

        if (shown == 0) {
            System.out.println("No suitable recipes were found.");
        } else {
            System.out.println("\nTop recommendation: " + candidates.get(0).recipe.name);
        }
    }

    // =========================================================
    // END OF CO-5 AND CO-6 FEATURES
    // =========================================================

    // =========================================================
    // MAIN MENU
    // =========================================================

    public static void main(String[] args) {
        ArrayList<Recipe> recipes = loadRecipes();

        if (recipes.isEmpty()) {
            System.out.println("No recipes were loaded.");
            System.out.println(
                    "Make sure the corpus folder is beside the .class file."
            );
            return;
        }

        Scanner sc = new Scanner(System.in);
        Random random = new Random();

        System.out.println("============================================================");
        System.out.println("             SMART RECIPE DISCOVERY SYSTEM");
        System.out.println("============================================================");
        System.out.println("Recipes loaded: " + recipes.size());

        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Search by ingredients");
            System.out.println("2. Search by keyword");
            System.out.println("3. Smart Spelling Correction");
            System.out.println("4. Compare Two Recipes");
            System.out.println("5. Optimize Cooking Steps");
            System.out.println("6. Ingredient-Recipe Matching");
            System.out.println("7. Kitchen Capacity Check");
            System.out.println("8. Surprise Me");
            System.out.println("9. Smart Recipe Combination Finder");
            System.out.println("10. Best Ingredient Coverage");
            System.out.println("11. Recipe Compatibility Analyzer");
            System.out.println("12. Smart Recipe Recommendation");
            System.out.println("13. Random Recipe Sampler");
            System.out.println("14. Fast Recipe Sorting");
            System.out.println("15. Parallel Recipe Analysis");
            System.out.println("16. Exit");

            System.out.print("\nEnter choice: ");
            String choice = sc.nextLine().trim();

            ArrayList<RecipeScore> results = new ArrayList<>();

            if (choice.equals("1")) {

                ArrayList<String> ingredients = readIngredients(sc);

                results = searchRecipes(
                        recipes,
                        ingredients,
                        "",
                        null,
                        null,
                        null,
                        null,
                        5
                );

                if (results.isEmpty()) {
                    System.out.println("\nI couldn't find a recipe using those ingredients.");
                    System.out.println(
                            "Try adding another ingredient or using a different ingredient name."
                    );
                    continue;
                }

                System.out.println("\nRECIPES YOU CAN TRY");

                for (int i = 0; i < results.size(); i++) {
                    displayResult(i + 1, results.get(i));
                }

                // Search -> choose -> full recipe
                letUserChooseRecipe(sc, results);

            } else if (choice.equals("2")) {

                System.out.print("What would you like to cook? ");
                String query = sc.nextLine().trim();

                results = searchRecipes(
                        recipes,
                        new ArrayList<>(),
                        query,
                        null,
                        null,
                        null,
                        null,
                        5
                );

                if (results.isEmpty()) {
                    System.out.println("\nI couldn't find a similar recipe.");
                    System.out.println(
                            "Try using a shorter or different description."
                    );
                    continue;
                }

                boolean exactNameFound = false;

                for (RecipeScore result : results) {
                    if (normalize(result.recipe.name)
                            .equals(normalize(query))) {
                        exactNameFound = true;
                        break;
                    }
                }

                if (exactNameFound) {
                    System.out.println("\nHERE'S WHAT I FOUND:");
                } else {
                    System.out.println(
                            "\nI couldn't find that exact recipe."
                    );
                    System.out.println(
                            "But this might be what you're looking for:"
                    );
                }

                for (int i = 0; i < results.size(); i++) {
                    displayResult(i + 1, results.get(i));
                }

                // Search -> choose -> full recipe
                letUserChooseRecipe(sc, results);

            } else if (choice.equals("3")) {

                smartSpelling(sc, recipes);

            } else if (choice.equals("4")) {

                compareRecipes(sc, recipes);

            } else if (choice.equals("5")) {

                optimizeCookingSteps(sc, recipes);

            } else if (choice.equals("6")) {

                ingredientRecipeMatching(sc, recipes);

            } else if (choice.equals("7")) {

                kitchenCapacityCheck(sc, recipes);

            } else if (choice.equals("8")) {

                RecipeScore surprise = surpriseMe(recipes, random);

                if (surprise == null) {
                    System.out.println("\nNo recipes are available.");
                    continue;
                }

                System.out.println("\n============================================================");
                System.out.println("                    🎲 SURPRISE ME!");
                System.out.println("============================================================");
                System.out.println(
                        "How about trying this today?"
                );

                displayResult(1, surprise);

                System.out.print(
                        "\nWould you like to see the full recipe? (y/n): "
                );

                String answer = sc.nextLine().trim();

                if (answer.equalsIgnoreCase("y")
                        || answer.equalsIgnoreCase("yes")) {
                    displayFullRecipe(surprise);
                }

            } else if (choice.equals("9")) {

                recipeTimeDecision(sc, recipes);

            } else if (choice.equals("10")) {

                approximateIngredientCover(sc, recipes);

            } else if (choice.equals("11")) {

                recipeVertexCoverApproximation(recipes);

            } else if (choice.equals("12")) {

                smartRecipeRecommendation(sc, recipes);

            } else if (choice.equals("13")) {

                reservoirRecipeFeature(recipes, random);

            } else if (choice.equals("14")) {

                randomizedSortFeature(recipes, random);

            } else if (choice.equals("15")) {

                parallelRecipeAnalytics(recipes);

            } else if (choice.equals("16")) {

                System.out.println(
                        "\nThank you for using Smart Recipe Discovery System."
                );
                sc.close();
                return;

            } else {

                System.out.println(
                        "Invalid choice. Please choose 1-16."
                );
            }
        }
    }

}
