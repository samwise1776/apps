# Nova Local Java AI — Project Memory

## Purpose

Nova is a completely local Java desktop assistant. It uses no Ollama, cloud API, network service, API key, or third-party dependency.

The application is intentionally educational: its neural classifier, dictionary, completion engine, offline question-answering rules, mathematical parser, and Swing interface are readable Java source code.

## Important requirements

- Keep the application offline.
- Do not add Ollama or another external AI service.
- Do not add third-party dependencies.
- Compile with the Java standard library.
- Do not fabricate unknown facts. Give a useful direct response while identifying limits of locally stored knowledge.
- Every non-empty question should receive a direct response.

## Source files

### `App.java`

The main application and Swing interface.

- Starts the desktop window on Swing's Event Dispatch Thread.
- Uses a dark visual theme.
- Displays user and Nova message bubbles.
- Contains the original `Brain` neural classifier.
- Supports command-line testing:

```bash
java -cp helper App --self-test
java -cp helper App --ask "What is (12 + 8) * 3?"
```

### `OfflineAssistant.java`

The general question-answering coordinator.

Its current capabilities include:

- Arithmetic with `+`, `-`, `*`, `/`, `%`, `^`, and parentheses
- What, who, when, where, why, and how questions
- Yes/no questions and decisions
- Comparisons
- Definitions
- General step-by-step guidance
- Local date and time
- Remembering the user's name during the running session
- Built-in programming, AI, computing, science, history, and geography facts
- Original neural classifier fallback for conversational intents
- Direct, non-fabricated responses for subjects absent from local knowledge

The built-in expression evaluator is `MathParser`. It is dependency-free and rejects division by zero and malformed expressions.

### `Dictionary.java`

A token vocabulary and word-list manager.

- Loads the first available operating-system English dictionary.
- On the current machine, `/usr/share/dict/words` contains about 104,334 entries.
- Adds common modern slang and abbreviations.
- Assigns stable token IDs.
- Encodes and decodes text.
- Creates binary, count, and frequency vectors.
- Tracks word frequency.
- Saves and loads vocabulary files.
- Supports learning new words dynamically.

### `Unfinished.java`

Completes partial words from `Dictionary`.

- Completes a single prefix.
- Completes the final partial word in a sentence.
- Preserves initial capitalization.
- Returns ranked alternative suggestions.
- Prioritizes common everyday words and slang.

Examples:

```text
prog         -> program
that is buss -> that is bussin
Please hel   -> Please help
```

### `Matrix.java`

Provides matrix-related educational utilities used by the helper project.

## Neural classifier

The `Brain` class in `App.java` is a small real trained classifier. It is not a large language model.

Architecture:

- Bag-of-words input vectors
- One output neuron per intent
- Softmax probabilities
- Cross-entropy loss
- Gradient descent training
- Deterministic random seed

Supported trained intents:

1. Greeting
2. How are you
3. Thanks
4. Goodbye
5. Help
6. Name
7. Yes
8. No
9. Coding
10. AI

`Brain.runSelfTest()` verifies all ten intents and unknown-word rejection.

## Conversation memory

Nova currently remembers the user's name while the application process remains open:

```text
User: My name is Ray
Nova: Nice to meet you, Ray...
User: What is my name?
Nova: Your name is Ray.
```

Memories are persisted in the managed `Saved assistant memories` section of this file and loaded again when the application starts. Nova writes that section when the user says `remember that ...` and leaves the rest of this document intact. The user can ask `what do you remember?` or say `forget all memories`.

## Build and run

From `/home/ray/Data`:

```bash
javac helper/*.java
java -cp helper App
```

Run the regression test:

```bash
java -cp helper App --self-test
```

Test one question without opening the GUI:

```bash
java -cp helper App --ask "How does photosynthesis work?"
```

Test word completion:

```bash
java -cp helper Unfinished "I like prog"
```

## Last verified state

- All files in `helper/*.java` compile together.
- The Swing window launches successfully.
- All ten trained neural intents pass.
- Unknown-word rejection passes.
- Arithmetic evaluation passes.
- Known factual questions pass.
- Unknown what/who/where/how questions receive direct responses.
- General guidance questions receive actionable steps.
- Word completion and slang completion pass.

Restart an already-running Java process after recompiling so it loads updated class files.

## Honest technical boundary

The assistant always produces a response, but its factual knowledge is limited to information encoded in its source and locally available resources. Because it is deliberately offline and dependency-free, it cannot retrieve current events or facts that were never stored locally. Unknown factual answers must not be invented.

<!-- NOVA_MEMORY_START -->

## Saved assistant memories

AI requirement: pure Java, offline, with no Ollama and no external dependencies
Name: Ray

## Question history

- Question: My name is Ray
- Question: What is my name?
- Question: How should I improve this project?
- Question: my name is Ray
- Question: You remember?
- Question: My name is Alex
- Question: So you already know my name
- Question: What is my name?
- Question: no my name is Ray please remember my
- Question: what is my name?
- Question: how far away is the moon from earth
- Question: in inches
- Question: what is my name
- Question: There is a different person now, now know me as Cathy
- Question: what is my name
- Question: my name is Mom
- Question: what is the circumference of Earth?
- Question: Hello
- Question: What is my name, do you remember
- Question: now my name is Ray
- Question: What is my name?
- Question: What's up
- Question: idk what the moons length is can you tell me
- Question: do you know what 'idk' mens
- Question: means
- Question: what does idk mean
- Question: do you kniw?
- Question: how were you made? idk
- Question: how were you made
- Question: what are you
- Question: whats ten minus 20
- Question: 20 - 0
- Question: 100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000 + 10E10000000000000000000000000000000000
- Question: 10000 + 10000000000
- Question: what do you remember
- Question: change that to Name: Dad
- Question: my name is Dad
- Question: my name is Glen
- Question: my name is Dad
- Question: yes
- Question: yes
- Question: yew
- Question: yes
- Question: you are part of my company Datacenter
- Question: what is Dataceter
- Question: Hello
- Question: Hello
- Question: Earth
- Question: Bitch
- Question: what is my name
- Question: my nam is ray
- Question: My name is Ray
- Question: what is my name
- Question: hello
- Question: Olah'
- Question: Hola
- Question: iNT
- Question: Int
- Question: Lo'
- Question: LOVE
- Question: What does LOVE Stand for in undertale
- Question: What is my name

<!-- NOVA_MEMORY_END -->
