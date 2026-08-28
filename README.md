# Smart Recipe Discovery System

## Final user menu

1. Search by ingredients
2. Search by keyword
3. Surprise Me
4. Exit

## User experience

### 1. Search by ingredients
The user enters ingredients available at home. The system recommends recipes using those ingredients.

### 2. Search by keyword
The user can enter a dish name or description. Search is forgiving of small spelling differences and extra words.

Example:

`Panner Butter Masala`

can suggest the stored recipe:

`Paneer Masala`

instead of simply returning "Not Found".

### 3. Surprise Me
The system randomly recommends a recipe from the available recipe corpus. The user can choose to view the complete recipe.

### 4. Exit
Closes the program.

## Important implementation detail

The DSA algorithms are used internally. Users are not shown:
- algorithm names
- match percentages
- Jaccard scores
- edit distances
- internal ranking calculations

## Technology

- Java only
- Plain-text recipe corpus (`.txt`)
- No frontend
- No database
- No external libraries

## Run

```bash
javac SmartRecipeSystem.java
java SmartRecipeSystem
```
