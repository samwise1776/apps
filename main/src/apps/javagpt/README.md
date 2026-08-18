# JavaGPT — A Generative Transformer Language Model in Pure Java

A GPT-style autoregressive language model implemented entirely in pure Java 17+ with no external AI libraries. The neural network is real: it learns through mathematical operations, backpropagation, and gradient descent.

## Quick Start

```bash
# Train the tiny model (fast, ~1 minute)
./train.sh

# Or compile and train manually
javac -d build -sourcepath src src/javagpt/*.java
java -cp build javagpt.Main train tiny

# Generate text
java -cp build javagpt.Main generate "Java is"

# Interactive chat
java -cp build javagpt.Main chat

# Run tests
java -cp build javagpt.Main test

# Show model info
java -cp build javagpt.Main info
```

## Architecture

This model implements the core concepts behind GPT and other modern language models:

### Tokens
Text is broken into discrete units called tokens. Our tokenizer uses **character-level tokenization**: each unique character gets an integer ID. This is simpler than BPE but demonstrates the same principles.

### Embeddings
Each token is mapped to a learned dense vector (embedding). **Positional embeddings** are added so the model knows token order:

```
x = tokenEmbedding[token] + positionEmbedding[position]
```

### Causal Self-Attention
The model computes attention scores using query (Q), key (K), and value (V) matrices:

```
Q = X·Wq,  K = X·Wk,  V = X·Wv
attentionScores = (Q·Kᵀ) / √headDim
attentionScores = causalMask(attentionScores)  // hide future tokens
attentionWeights = softmax(attentionScores)
output = attentionWeights · V
```

### Multi-Head Attention
Multiple attention heads run in parallel, each learning different relationships:

```
head_i = Attention(X·Wq_i, X·Wk_i, X·Wv_i)
MultiHead = Concat(head_1, ..., head_h) · Wo
```

### Transformer Block
Each block combines attention with a feed-forward network:

```
x = x + MHA(LayerNorm(x))     // attention with residual
x = x + FFN(LayerNorm(x))     // feed-forward with residual
```

### Feed-Forward Network
Two linear layers with GELU activation:

```
FFN(x) = Linear₂(GELU(Linear₁(x)))
```

### Layer Normalization
Stabilizes training by normalizing activations:

```
LayerNorm(x) = γ · (x - μ) / √(σ² + ε) + β
```

### Output
Final projection produces vocabulary logits, softmax converts to probabilities:

```
logits = Linear(LayerNorm(x))
probs = softmax(logits)
```

### Loss
Cross-entropy loss measures prediction quality:

```
L = -log(p(target))
```

### Backpropagation
Gradients flow through every layer via the chain rule:

```
∂L/∂θ = ∂L/∂output · ∂output/∂θ
```

### AdamW Optimizer
Updates weights using adaptive learning rates with weight decay:

```
m = β₁·m + (1-β₁)·g
v = β₂·v + (1-β₂)·g²
m̂ = m / (1-β₁ᵗ)
v̂ = v / (1-β₂ᵗ)
θ = θ - lr · (m̂ / (√v̂ + ε) + λ·θ)
```

### Temperature & Top-k Sampling
Temperature scales logits to control randomness. Top-k limits sampling to the k most likely tokens.

## Configurations

| Config | Layers | Heads | Embed | Context | ~Params |
|--------|--------|-------|-------|---------|---------|
| tiny | 2 | 4 | 128 | 64 | ~500K |
| small | 6 | 8 | 256 | 128 | ~5M |
| medium | 8 | 12 | 384 | 256 | ~15M |

## Why This Is Not ChatGPT

This model uses the **same core transformer architecture** as GPT and ChatGPT, but differs enormously in scale:

| | JavaGPT (tiny) | ChatGPT |
|---|---|---|
| Parameters | ~500K | ~175B+ |
| Training data | ~10KB text | ~300B tokens |
| Training compute | CPU minutes | GPU clusters, months |
| Context length | 64 tokens | 4K-128K tokens |
| Tokenizer | Character-level | Tiktoken BPE |

The mathematics are the same. The scale is not. This is an educational implementation that demonstrates how transformers work, not a production language model.

## Project Structure

```
javagpt/
├── src/javagpt/
│   ├── Main.java              # CLI entry point
│   ├── GPT.java               # Full GPT model
│   ├── GPTConfig.java         # Configuration
│   ├── Tensor.java            # Math engine
│   ├── Linear.java            # Linear layer
│   ├── LayerNorm.java         # Layer normalization
│   ├── Embedding.java         # Token/positional embeddings
│   ├── MultiHeadAttention.java # Multi-head self-attention
│   ├── FeedForward.java       # FFN with GELU
│   ├── TransformerBlock.java  # Single transformer block
│   ├── AdamW.java             # AdamW optimizer
│   ├── Trainer.java           # Training loop
│   ├── Generator.java         # Text generation
│   ├── Dataset.java           # Data loading
│   ├── Tokenizer.java         # Character-level tokenizer
│   ├── Checkpoint.java        # Save/load
│   ├── MathUtil.java          # Math utilities
│   └── GUI.java               # Swing interface
├── data/training.txt          # Training text
├── models/                    # Saved models
├── train.sh                   # Train script
├── run.sh                     # Chat script
└── README.md
```

## Mathematical Tests

Run `java -cp build javagpt.Main test` to verify:

- Softmax sums to 1
- Causal mask hides future tokens
- Matrix multiplication dimensions
- Layer normalization
- Tokenizer encode/decode
- Model save/load consistency
- Loss decreases on repetitive data
- Gradients are finite (no NaN)
