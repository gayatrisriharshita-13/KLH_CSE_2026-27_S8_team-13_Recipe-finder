// Smart Recipe Discovery System
import java.io.*;
import java.util.*;

/**
 * Smart Recipe Discovery System
 *
 * Java-only DSA implementation.
 * No frontend, no database, no external libraries.
 *
 * DSA used:
 * 1. Naive String Matching
 * 2. KMP String Matching
 * 3. Rabin-Karp String Matching
 * 4. Levenshtein Distance
 * 5. Jaccard Similarity
 * 6. Multi-factor recipe ranking
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
            System.out.println("3. Surprise Me");
            System.out.println("4. Exit");

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

            } else if (choice.equals("4")) {

                System.out.println(
                        "\nThank you for using Smart Recipe Discovery System."
                );
                sc.close();
                return;

            } else {

                System.out.println(
                        "Invalid choice. Please choose 1-4."
                );
            }
        }
    }

}
