# BrickDeck AI Strategy

AI should make BrickDeck more useful, but it should not be the foundation of the MVP.

The project should first build a strong catalog, inventory, comparison, and recommendation system. AI can then improve piece classification and user experience.

---

## AI Vision

BrickDeck should eventually act like an intelligent LEGO assistant:

- Identify pieces from photos
- Suggest likely part numbers
- Detect colors
- Organize loose pieces
- Recommend sets and builds
- Explain differences between set versions
- Help users decide what to buy or build next

---

## AI Feature Levels

### Level 1 — AI Text Assistance

Use AI to generate explanations and summaries.

Examples:

- Summarize differences between two sets.
- Explain why one version may be better than another.
- Recommend which set is a better buy.
- Generate user-friendly missing piece summaries.

This can be done earlier because it does not require computer vision.

---

### Level 2 — Assisted Piece Classification

Use AI to classify a single piece from an uploaded photo.

Expected output:

- Candidate part IDs
- Candidate colors
- Confidence score
- Similar-looking alternatives
- User confirmation step

Important: the system should not auto-save uncertain results without confirmation.

---

### Level 3 — Multi-Piece Detection

Detect and classify multiple pieces in one image.

Challenges:

- Overlapping pieces
- Shadows
- Similar colors
- Similar part shapes
- Camera distortion
- Lighting differences

This should be treated as a later premium feature.

---

### Level 4 — Smart Organization

Suggest how to organize pieces physically.

Examples:

- Group by type
- Group by color
- Group by build usage
- Group rare pieces separately
- Suggest storage bins

---

## Dataset Strategy

AI piece recognition requires a dataset.

Possible sources:

- Public catalog images where licensing allows
- User-uploaded confirmed images
- Synthetic images generated from 3D models, if allowed
- Manually labeled personal collection images

All dataset usage must respect licenses and platform rules.

---

## Human-in-the-Loop Design

The user should confirm AI suggestions.

Recommended flow:

1. User uploads image.
2. AI returns top candidates.
3. User confirms or corrects result.
4. Confirmed result is saved.
5. Confirmation improves future classification data.

---

## Recommended AI Architecture

Initial approach:

```text
Next.js Web → Spring Boot API → Python AI Service → Model / Vision Pipeline
```

The AI service should be optional during early development.

---

## AI MVP

The first AI-related feature should be text-based, not image-based.

Recommended first AI feature:

- Generate a plain-English comparison summary between two sets.

Recommended second AI feature:

- Given a list of loose pieces, suggest organization categories.

Recommended third AI feature:

- Single-piece image recognition prototype.

---

## AI Risks

- Poor classification accuracy
- Confusing similar LEGO parts
- Color detection errors
- Lighting issues
- Dataset licensing limitations
- High compute cost
- User trust issues if confidence is not shown

---

## AI Product Rule

AI should assist, not pretend to be perfect.

Every visual classification result should include confidence and confirmation.
