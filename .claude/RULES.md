# Event Tool Rules

## File Placement
- UI components: `ui/components/`
- State management: `state/`
- Storage/persistence: `storage/`
- Utilities: `utils/`
- Data models: `model/`
- Entry point: `EventToolMain.kt`

## Naming
| Type | Convention | Example |
|------|------------|---------|
| Composable | PascalCase | `TimelineCanvas` |
| State | PascalCase + State | `GraphState` |
| ViewModel | PascalCase + ViewModel | `EventToolViewModel` |
| Loader | PascalCase + Loader | `EventLoader` |

## State Management
- All state in ViewModel
- State classes are immutable data classes
- Use `copy()` for updates

## Content Editing
- Year editing writes directly to TOML files via `EventWriter`
- All writes use targeted line replacement (not full TOML rewrite)
- Other fields remain read-only for now

## Code Quality
- [ ] No hardcoded colors (use theme)
- [ ] Graph calculations in utils
- [ ] State changes go through ViewModel