# LSP Integration tests samples

Sample projects used for **LSP performance and integration testing** in IntelliJ IDEA.

Each subdirectory is a small, self-contained project in a specific language. The
tests open these projects to exercise LSP Client integration
— diagnostics, completion, navigation — and to measure
performance on realistic-but-minimal codebases.

## Layout

Each sample lives in its own top-level directory named after the language:

```
lsp-integration-samples/
├── swift/      # Swift sample project
├── lua/        # Lua sample project
├── dart/       # Dart sample project
└── ...
```

## Projects

| Language | Project | Source |
|----------|---------|--------|
| Lua | `lua/inspect.lua` | [kikito/inspect.lua](https://github.com/kikito/inspect.lua) |
| Dart | `dart/equatable` | [felangel/equatable](https://github.com/felangel/equatable) |
| Swift | `swift/swift-tagged` | [pointfreeco/swift-tagged](https://github.com/pointfreeco/swift-tagged) |
