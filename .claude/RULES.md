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

## Read-Only Tool
This tool only reads content files - no writing/editing.

## Code Quality
- [ ] No hardcoded colors (use theme)
- [ ] Graph calculations in utils
- [ ] State changes go through ViewModel