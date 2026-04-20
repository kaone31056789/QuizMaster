# How QuizMaster Works — A Complete Developer's Guide

This document explains every system inside **QuizMaster** in detail. Whether you're a developer maintaining the codebase, a student studying the architecture, or a teammate preparing to present this project — this guide covers everything.

---

## Table of Contents

1. [Overview](#-overview)
2. [Architecture — MVC Pattern](#-architecture--mvc-pattern)
3. [Application Flow (Step by Step)](#-application-flow)
4. [Lifeline System](#-lifeline-system)
5. [AI Question Generation System](#-ai-question-generation-system)
6. [Topic Diversity Engine](#-topic-diversity-engine)
7. [Difficulty System](#-difficulty-system)
8. [Memory System (Anti-Repeat)](#-memory-system-anti-repeat)
9. [Scoring & Points Engine](#-scoring--points-engine)
10. [Wrong Answer Tracking & Review](#-wrong-answer-tracking--review)
11. [Animation System](#-animation-system)
12. [Data Persistence](#-data-persistence)
13. [Theme System](#-theme-system)
14. [Dependencies & Build](#-dependencies--build)

---

## 🔎 Overview

QuizMaster is a JavaFX 17 desktop application that generates trivia quiz questions using the **Ollama Cloud API**. It uses prompt engineering to generate questions that match specific difficulty levels, stores a persistent memory of previously asked questions to prevent repeats across sessions, and features an arcade-style scoring system with combos, trophies, and lifetime stats.

**Key Technical Highlights:**
- Parallel HTTP requests to generate all questions simultaneously
- 3-strategy JSON parser that handles malformed AI responses
- Persistent memory system (survives app restarts)
- Real-time background daemon pre-fetching more questions during gameplay
- Model fallback chain for API reliability

---

## 🏗️ Architecture — MVC Pattern

The codebase is organized into 4 layers following strict **Model-View-Controller** separation:

### Model Layer (`com.quiz.model`)

Contains **pure data classes** with no UI logic:

| Class | Purpose |
|-------|---------|
| `Difficulty.java` | Java Enum: `EASY`, `MEDIUM`, `HARD`, `RANDOM` |
| `Question.java` | Holds question text, 4 options, correct answer index, difficulty |
| `QuizSession.java` | Manages game state: current question, score, points, streak, wrong answers |

**QuizSession** is the "brain" of an active game. It:
- Maintains the question list and current index
- Calculates points with formula: `base + timeBonus + streakBonus`
- Tracks wrong answers with the user's selected option for post-game review
- Supports dynamic question injection from the background daemon

### View Layer (`com.quiz.ui`)

Each View constructs a JavaFX `VBox` with styled components. Views **never** make game decisions — they call the Controller.

| View | Screen | Key Features |
|------|--------|-------------|
| `SplashView` | Startup | 2.5s choreographed animation (bounce logo, dot fade) |
| `SetupView` | First-run | API key input, model selection, connection test |
| `StartView` | Main Menu | Difficulty selection, AI toggle, theme switch, lifetime stats |
| `SettingsView` | Settings | API key management, model config, theme preferences |
| `QuizView` | Gameplay | 15s timer, option buttons, streak display, floating points |
| `ResultView` | Results | Trophy, score %, points, stats, **wrong answer review** |

### Controller Layer (`com.quiz.controller`)

**`AppController.java`** is the single traffic director:
- Manages the global JavaFX `Stage` (window)
- `switchRoot()` dynamically swaps the scene root between views
- Orchestrates the AI quiz flow: loading screen → parallel generation → gameplay → results
- Starts the background daemon for infinite question pre-fetching
- Applies theme stylesheet swaps (`styles.css` + optional `dark-mode.css`)

### Utility Layer (`com.quiz.util`)

| Utility | Purpose |
|---------|---------|
| `AIQuestionGenerator.java` | Ollama Cloud API client, prompt engineering, memory system, JSON parsing |
| `ConfigManager.java` | Read/write `~/.quizmaster/config.properties` |
| `AnimationHelper.java` | Reusable JavaFX transition factories |
| `DataLoader.java` | Loads built-in `questions.json` as fallback |

---

## 🌊 Application Flow

Here is the complete flow from app launch to quiz completion:

### 1. Launch (`Main.java`)

```
Main.main() → launch() → start(Stage)
  → AppController.setPrimaryStage(stage)
  → AppController.showInitialScreen()
    → SplashView (2.5s animation)
```

### 2. Post-Splash Routing

```
ConfigManager.isFirstRun()?
  ├── YES → SetupView (enter API key, test connection)
  └── NO  → StartView (main menu)
```

### 3. Starting a Quiz

The user selects difficulty + question count (5/10/15/20) on the StartView.

**With AI enabled:**
```
AppController.startQuizWithAI(difficulty, apiKey, model, questionCount)
  → Show loading screen ("Generating questions...")
  → New background Thread:
      1. Create AIQuestionGenerator
      2. Set StatusCallback for live UI updates
      3. generateQuestions(difficulty, questionCount)
         ← fires (questionCount + 2) requests in parallel
         ← each request gets a unique topic category
      4. Deduplicates results, returns exactly questionCount
      5. On success: create QuizSession → show QuizView
```

**Without AI (offline mode):**
```
AppController.startQuiz(difficulty, questionCount)
  → DataLoader.getQuestionsByDifficulty(difficulty)
  → Shuffle + limit to questionCount
  → Create QuizSession → show QuizView
```

### 4. Gameplay Loop (`QuizView`)

```
loadNextQuestion()
  → session.getNextQuestion()
  → Display question text + 4 option buttons
  → Show lifeline bar (🎯 50:50, ⏩ Skip, ⏰ +10s)
  → Start 15-second timer
  → Wait for user action:
      ├── Click option → handleAnswer(selectedIndex)
      │   ├── Correct: flash green, award points, show combo
      │   └── Wrong: flash red, shake screen, mark wrong
      ├── Use lifeline → execute lifeline effect
      └── Timer expires → handleTimeout()
          └── Show correct answer, mark wrong (selectedIndex = -1)
  → 900ms pause → loadNextQuestion()
```

### 5. Results (`ResultView`)

```
session has no more questions
  → AppController.showResultScreen(session)
  → Display: trophy, percentage, points, stats
  → Save to ConfigManager (lifetime points, games played, best score)
  → If any wrong answers:
      → Show scrollable "Wrong Answers Review" section
      → For each wrong answer: question text, your choice (❌), correct answer (✅)
```

---

## 🎯 Lifeline System

**File:** `QuizView.java`

Three lifelines are available per quiz, displayed as colored pill buttons between the progress bar and the question card.

### Available Lifelines

| Lifeline | Button Color | Effect | Implementation |
|----------|-------------|--------|----------------|
| **🎯 50:50** | Purple | Removes 2 wrong options | Collects wrong indices, shuffles, hides first 2 via `FadeTransition` to 25% opacity + disable |
| **⏩ Skip** | Blue | Skip without penalty | Sets `answered = true`, stops timer, calls `loadNextQuestion()` after 600ms |
| **⏰ +10s** | Green | Adds 10 seconds | `timeSeconds += 10`, removes urgency CSS class, updates timer label |

### State Management

```java
private boolean fiftyFiftyUsed = false;   // Tracks if 50:50 was used
private boolean skipUsed = false;          // Tracks if Skip was used  
private boolean extraTimeUsed = false;     // Tracks if +10s was used
```

- Each boolean is checked before executing the lifeline
- Once used, the button is greyed out (`-fx-background-color: #3a3a5c; -fx-opacity: 0.5`)
- A floating "\ud83c\udfaf 50:50 Used!" label animates upward and fades out
- Lifelines reset when a new quiz starts (new `QuizView` instance)

### Visual States

| State | Style |
|-------|-------|
| **Available** | Vibrant color + glow shadow + hover scale effect |
| **Used** | Grey (`#3a3a5c`) + 50% opacity + disabled |

---

## 🤖 AI Question Generation System

**File:** `AIQuestionGenerator.java`

### How It Works

1. **Parallel Request Firing**: When `generateQuestions(difficulty, questionCount)` is called, the system:
   - Requests `count + 2` questions (over-provisions for parse failures)
   - Creates a thread pool with `Executors.newFixedThreadPool(count + 2)`
   - Fires all HTTP requests **simultaneously** to the primary model
   - **Each request gets a unique topic category** (science, history, geography, etc.)
   - Each request generates exactly 1 question with a unique seed
   - Results are deduplicated after collection

2. **API Call Structure**:
   ```
   POST https://ollama.com/api/generate
   Authorization: Bearer <API_KEY>
   Content-Type: application/json

   {
     "model": "gemma3:4b-cloud",
     "prompt": "<difficulty-specific prompt>",
     "stream": false,
     "format": "json",
     "options": {
       "num_predict": 200,
       "temperature": 0.9
     }
   }
   ```

3. **Response Parsing** (3-Strategy Robust Parser):
   - **Strategy 1**: Parse entire response text as JSON (array or object)
   - **Strategy 2**: Find `[...]` brackets in text and extract
   - **Strategy 3**: Find `{...}` brackets in text and wrap in array
   - Each strategy silently falls through to the next on failure

4. **Option Prefix Cleanup**: The parser strips AI-generated prefixes like `"A. "`, `"B) "`, `"1. "` using regex to prevent double-prefixing in the UI (which adds its own `A.` labels).

### Model Fallback Chain

```
Primary: gemma3:4b-cloud (4B params, proven reliable)
    ↓ (if < 5 questions obtained)
Fallback 1: ministral-3:3b-cloud (3B params, small/fast)
    ↓
Fallback 2: gemini-3-flash-preview:cloud (built for speed)
    ↓
Fallback 3: qwen3.5:cloud (general purpose)
    ↓
Fallback 4: gemma4:31b-cloud (large, last resort)
```

### Timeouts & Limits

| Setting | Value | Rationale |
|---------|-------|-----------|
| Connect timeout | 5 seconds | Fast fail if Ollama is down |
| Read timeout | 30 seconds | Single question should complete in <30s |
| Future timeout | 30 seconds | Kill any hung thread |
| `num_predict` | 200 tokens | 1 question needs ~150 tokens max |
| `temperature` | 0.9 | High creativity/variety |

---

## 🎲 Topic Diversity Engine

**Problem Solved:** When firing 7 parallel requests with the same prompt, the AI would generate duplicate/similar questions (e.g., 3 questions about "largest desert in the world").

### How It Works

Each parallel request is assigned a **mandatory topic category** from a shuffled pool of 9:

| # | Category | Sub-topics |
|---|----------|-----------|
| 1 | Science | physics, chemistry, biology, astronomy, medicine |
| 2 | History | civilizations, wars, historical figures, inventions |
| 3 | Geography | countries, cities, rivers, mountains, oceans |
| 4 | Nature | animals, plants, ecosystems, weather, geology |
| 5 | Culture | art, music, literature, movies, languages |
| 6 | Sports | Olympic records, athletes, rules, tournaments |
| 7 | Technology | computers, internet, space exploration, engineering |
| 8 | Food | cuisine, ingredients, cooking traditions, agriculture |
| 9 | Human Body | anatomy, diseases, nutrition, psychology |

The prompt includes: `"MANDATORY TOPIC: Your question MUST be about SCIENCE (physics, chemistry, biology, astronomy, or medicine). Do NOT ask about any other topic category."`

This guarantees Q1 (science) and Q2 (history) **cannot overlap**.

### Post-Collection Deduplication

After all parallel results come in, a keyword-overlap algorithm runs:

```java
// Extract 4+ character words from question text
Set<String> words = extractKeywords(questionText);

// If 50%+ of keywords match an already-selected question → remove it
if ((double) overlap / smallerSet >= 0.5) { isDuplicate = true; }
```

This catches edge cases where the AI ignores the topic constraint.

### Unique Seeds

Each parallel thread gets a unique seed: `nanoTime + questionSeed * 999999L` — preventing identical random states.

---

## 🎯 Difficulty System

The AI receives **detailed, explicit prompt instructions** per difficulty level. This is critical because simply saying "easy" or "hard" in the prompt results in the AI generating random-difficulty questions.

### Prompt Engineering Per Level

**EASY:**
```
Difficulty: EASY
Rules for EASY questions:
- The answer should be something a 10-year-old student would know
- Topics: basic geography (capitals, continents), simple science (planets, animals),
  famous landmarks, colors, basic math facts
- The correct answer should be obvious to most people
- Wrong options should be clearly wrong
- Example: 'What planet is closest to the Sun?' (Mercury)
```

**MEDIUM:**
```
Difficulty: MEDIUM
Rules for MEDIUM questions:
- Requires general knowledge that an educated adult would know
- Topics: historical events with dates, scientific concepts, world geography details,
  literature, technology
- The correct answer should NOT be immediately obvious — requires thinking
- Wrong options should be plausible and tricky
- Example: 'In what year did the Berlin Wall fall?' (1989)
```

**HARD:**
```
Difficulty: HARD
Rules for HARD questions:
- Only specialists, trivia experts, or well-read people would know this
- Topics: obscure historical details, advanced science, niche geography,
  rare cultural facts, technical terminology
- All 4 options must seem equally plausible — designed to trick
- Example: 'What is the smallest country in Africa by area?' (Seychelles)
```

**RANDOM:** Each individual question randomly selects one of EASY/MEDIUM/HARD using `System.nanoTime() % 3`, creating a mixed-difficulty experience.

The prompt also includes: `"IMPORTANT: Strictly follow the difficulty rules above. Do NOT make easy questions for hard mode or vice versa."`

---

## 🧠 Memory System (Anti-Repeat)

**Goal:** Never show the same (or similar) question twice, even across app restarts.

### How It Works

1. **In-Memory Storage**: A `HashSet<String>` (case-insensitive) provides O(1) duplicate detection. A parallel `ArrayList<String>` maintains insertion order for prompt injection.

2. **On Question Generated**:
   ```java
   addToMemory(questionText)
     → Convert to lowercase → check HashSet
     → If new: add to both HashSet and ArrayList
     → If ArrayList > 80: remove oldest entry from both
     → Persist to disk: ConfigManager.setAIMemory(joined string)
   ```

3. **On App Startup**:
   ```java
   static {
     String saved = ConfigManager.getAIMemory();
     // Split by "|||" delimiter → populate HashSet + ArrayList
   }
   ```

4. **Prompt Injection**: The last 8 question snippets (first 35 chars) are injected into every AI prompt:
   ```
   Avoid these previously asked topics: What planet is closest to the Sun?; In what year did the Berlin Wall fa; ...
   ```

5. **Duplicate Detection During Generation**: If a newly generated question matches one in memory, the generator retries once with a new seed before giving up.

### Storage Format

In `~/.quizmaster/config.properties`:
```properties
ai.memory=What planet is closest to the Sun?|||In what year did the Berlin Wall fall?|||What element has atomic number 79?
```

---

## 🏆 Scoring & Points Engine

### Points Formula

When a correct answer is given:

```
earned = base + timeBonus + streakBonus
```

| Component | Formula | Range |
|-----------|---------|-------|
| **Base** | EASY=100, MEDIUM=200, HARD=300, RANDOM=150 | 100-300 |
| **Time Bonus** | `timeRemaining × 3.33` | 0-50 |
| **Streak Bonus** | `min(streak × 10, 50)` | 0-50 |
| **Total per question** | | 100-400 |

### Streak System

- Consecutive correct answers increment the streak counter
- Wrong answer or timeout resets streak to 0
- At streak ≥ 2: shows "🔥 3 streak!" text + combo graphic overlay
- Best streak in a session is tracked for the result screen

### Trophies

| Accuracy | Trophy | Label |
|----------|--------|-------|
| 100% | 🏆 | Champion |
| ≥ 80% | 🥇 | Gold |
| ≥ 60% | 🥈 | Silver |
| ≥ 40% | 🥉 | Bronze |
| < 40% | ⭐ | Participant |

### Lifetime Stats

Saved persistently across all sessions:
- **Lifetime Points**: Sum of all points ever earned
- **Games Played**: Total quiz sessions completed
- **Best Score**: Highest single-game points

---

## ❌ Wrong Answer Tracking & Review

### Recording

`QuizSession` stores wrong answers using an inner class:

```java
public static class WrongAnswer {
    public final Question question;     // The full question object
    public final int selectedIndex;     // User's chosen option (0-3, or -1 for timeout)
}
```

- On wrong answer click: `session.markWrong(selectedIndex)` — stores question + user's choice
- On timeout: `session.markWrong()` — stores question with `selectedIndex = -1`

### Display (`ResultView`)

After the main results card, if there are any wrong answers:

```
❌ Wrong Answers Review
Learn from your mistakes:

┌──────────────────────────────┐
│ #1                           │
│ What planet is closest to    │
│ the Sun?                     │
│                              │
│ Your answer:                 │
│ ❌ B. Venus                  │  ← red background
│                              │
│ Correct answer:              │
│ ✅ A. Mercury                │  ← green background
└──────────────────────────────┘
```

For timeouts, it shows: `"⏱ Time's up (no answer)"`

---

## 🎨 Animation System

**File:** `AnimationHelper.java`

All animations are centralized in reusable static methods:

| Method | Effect | Used By |
|--------|--------|---------|
| `applyFadeIn(node, ms)` | Fade from transparent to visible | Question cards |
| `applySlideUp(node, ms)` | Rise from below + fade in | Cards, splash elements |
| `applyBounceIn(node, ms)` | Scale from 0.3→1.1→1.0 + fade | Trophies, points |
| `applyHoverEffect(node)` | Scale 1.0→1.03 on mouse hover | All buttons |
| `applyRevealFlash(node, count)` | Flash opacity for suspense | Answer reveal |
| `applyScreenShake(node)` | Horizontal shake ±15px | Wrong answers |
| `spawnComboGraphic(parent, streak)` | "🔥 3x COMBO!" overlay | Streaks ≥ 2 |
| `applyStaggeredFadeIn(nodes, delay)` | Sequential fade for lists | Option buttons |
| `applyPulse(node)` | Infinite subtle scale pulse | Timer urgency |

### Implementation Pattern

All animations use JavaFX's built-in transition classes:
- `FadeTransition` for opacity
- `ScaleTransition` for size
- `TranslateTransition` for position
- `ParallelTransition` to combine multiple
- `SequentialTransition` for chained animations
- `Interpolator.EASE_OUT` for professional easing

---

## 💾 Data Persistence

**File:** `ConfigManager.java`

Uses a standard Java `Properties` file at `~/.quizmaster/config.properties`.

### Stored Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `ollama.apikey` | String | `""` | Ollama Cloud API bearer token |
| `ollama.model` | String | `"gemma3:4b-cloud"` | Selected model (display only — primary model is hardcoded) |
| `app.theme` | String | `"LIGHT"` | `"LIGHT"` or `"DARK"` |
| `setup.done` | Boolean | `false` | Whether first-run setup completed |
| `stats.lifetime_points` | int | `0` | All-time points earned |
| `stats.games_played` | int | `0` | Total completed games |
| `stats.best_score` | int | `0` | Highest single-game score |
| `ai.memory` | String | `""` | `|||`-delimited list of past question texts |

### File Location
- **Windows**: `C:\Users\<name>\.quizmaster\config.properties`
- **macOS/Linux**: `~/.quizmaster/config.properties`

All `set*()` methods call `save()` immediately, ensuring data is never lost.

---

## 🎨 Theme System

Two CSS files provide the visual themes:

| File | Theme | Style |
|------|-------|-------|
| `styles.css` | Light | Glassmorphic cards, soft gradients, dark text |
| `dark-mode.css` | Dark | Neon Cyberpunk, deep navy/purple, glowing accents |

### How Theme Switching Works

```java
AppController.applyTheme()
  → scene.getStylesheets().clear()
  → Always load: styles.css (base styles)
  → If ConfigManager.getTheme() == "DARK":
      → Also load: dark-mode.css (overrides)
```

The dark mode CSS overrides specific classes like `.root`, `.card`, `.primary-button` etc. with darker colors and neon accents.

---

## ⚙️ Dependencies & Build

### Maven Dependencies (`pom.xml`)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `javafx-controls` | 17.0.6 | JavaFX UI components |
| `javafx-fxml` | 17.0.6 | FXML support |
| `javafx-media` | 17.0.6 | Media playback (optional) |
| `gson` | 2.10.1 | JSON parsing for API responses and `questions.json` |

### Build & Run

```bash
# Compile
.\apache-maven-3.9.6\bin\mvn compile

# Run
.\apache-maven-3.9.6\bin\mvn javafx:run

# Clean build
.\apache-maven-3.9.6\bin\mvn clean compile
```

The `javafx-maven-plugin` (v0.0.8) handles the JavaFX module path automatically, with `com.quiz.Main` as the entry point.

---

## 📌 Key Design Decisions

1. **Why parallel generation?** — Sequential API calls took 2+ minutes for 5 questions. Parallel cuts it to ~20 seconds.

2. **Why over-provision by 2?** — AI models occasionally return malformed JSON (~15% failure rate). Requesting 7 and returning 5 avoids needing slow fallback models.

3. **Why hardcode the primary model?** — `gemma3:4b-cloud` is the only model confirmed 100% available. Letting users pick models caused constant 404 errors.

4. **Why strip option prefixes?** — The AI often generates options like `"A. Mercury"` even when told not to. The UI adds its own `"A. "` prefix, causing `"A. A. Mercury"`. Regex stripping fixes this.

5. **Why use Properties instead of a database?** — A full database is overkill for <10 config values. Java Properties is zero-dependency, human-readable, and built into the JDK.

6. **Why a HashSet for memory?** — O(1) lookup vs O(n) list scanning. With 80 stored questions, the difference is negligible, but it's good practice.
