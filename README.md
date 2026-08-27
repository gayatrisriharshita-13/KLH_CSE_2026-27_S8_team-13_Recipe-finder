# 🍽️ Smart Recipe Discovery System

## 📌 Overview

The **Smart Recipe Discovery System** is a Java-based application designed to make recipe discovery easier and more flexible.

Instead of requiring users to search only for an exact dish name, the system allows them to search using the ingredients they already have or by entering a recipe name or keyword. The system then identifies relevant recipes and provides suitable suggestions.

The project uses **Data Structures and Algorithms** for efficient searching and similarity-based recipe discovery.

---

## ✨ Features

### 🥕 Search by Ingredients

Users can enter the ingredients available at home and find recipes that can be prepared using them.

**Example:**

```text
Input:
paneer, tomato, onion

Output:
Paneer Masala
Paneer Curry
...
```
SYSTEM ARCHITECTURE:

START
  ↓
Load Recipe Dataset
  ↓
Display Menu
  ↓
┌─────────────────────────────┐
│ 1. Search by Ingredients     │
│ 2. Search by Keyword         │
│ 3. Surprise Me               │
│ 4. Exit                      │
└─────────────────────────────┘
  ↓
Process User Request
  ↓
Find Relevant Recipes
  ↓
Display Recipe Suggestions
  ↓
User Selects a Recipe
  ↓
Display Full Recipe
  ↓
Return to Menu / Exit
