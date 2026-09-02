On the RAG question

Short answer: not for the app itself — the data doesn't want it.

RAG earns its place when you have a large corpus of unstructured text and no way to query it
precisely. Your cancer project is the textbook case: 4,634 trials of free-text eligibility criteria
where "trials studying a BRCA mutation" can't be expressed as SQL.

CPSS is the opposite. ~100 foods with fully structured numeric attributes — crunch, punch, sweet,
savory, foundation, per-100g macros. "Crunchy foundation ingredients under 50 calories" is a WHERE
clause that returns an exact answer. Embedding that into a vector store would make it slower, fuzzier,
and wrong at the edges — and your own cancer notes document exactly how embeddings fail on this kind
of data: "Embedding similarity cannot distinguish receptor polarity." The same weakness would hit "low
sugar" vs "high sugar."

Two things that would genuinely work, though:

1. A natural-language salad builder. Not retrieval — tool use. "Build me something crunchy and savory
   with no nuts" → an LLM calls your existing filtered queries and composes a salad. That's the CPSS
   quadrant doing the work, with the LLM as the interface. No vector store.
2. RAG over the cookbook, not the food data. You have a real corpus sitting unused — Cookbook Business
   Proposal.md, all_parts.md (62KB of ingredient guidance), the technical docs. "How do I cut a bell
   pepper for salad?" is a genuine retrieval question. That's the one place a vector store pays for
   itself.

For interviews specifically, option 1 demonstrates more: it shows you know when not to reach for RAG,
which is a sharper signal than having bolted one on. And you can say "I built RAG properly on the
cancer project where the data justified it" — the contrast is the story.

Want me to sketch either? I'd want to check what AI provider config you already have — the cancer
project has an ai-provider module that's currently shelved.
