# Basileus Event Tool

@.claude/RULES.md

Visualization and editing tool for content creators to explore event relationships via outcomes.

## Tech Stack
- Kotlin + Compose Desktop
- Standalone (no dependencies on other basileus modules)

## Quick Commands
```bash
./gradlew run      # Run the tool
./gradlew build    # Build
```

## Content Path
Reads from `../basileus-content/`:
- `events/` - Event TOML files (crisis, situation, opportunity, narrative)
- `outcomes/` - Outcome TOML files

## Features
- Timeline View: Events on year axis with dependency arrows
- Graph View: Force-directed layout showing outcome connections
- Filter by year range, event type, tier, historicity
- Search events and outcomes
- Timeline presets (Maniakes, Schism, etc.)
- Detail panel showing requirements/productions
- Edit event min/max year from detail panel (saves to TOML)

## Key Files
- `EventToolMain.kt` - Entry point
- `EventToolViewModel.kt` - State coordinator
- `ui/components/TimelineCanvas.kt` - Timeline rendering
- `ui/components/GraphCanvas.kt` - Graph rendering
- `storage/EventLoader.kt` - Parse event TOMLs
- `storage/GraphBuilder.kt` - Build dependency graph
- `storage/EventWriter.kt` - Write year changes to TOML

## Future Features
- **Validation Mode** - Report content errors:
  - Orphan outcomes (produced but never required)
  - Missing producers (required but never produced)
  - Year conflicts (event requires outcome from later event)
- **TOML Library** - Replace manual parsing with `tomlkt` for robustness
