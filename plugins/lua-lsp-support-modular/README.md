# Modular Lua LSP

This is a split-mode version of the Lua LSP sample. It keeps the same LuaLS
integration behavior as `lua-lsp-support`, but puts the complete integration
in the `frontend` content module.

## Module layout

- `frontend` contains the LSP provider, server settings, executable lookup,
  remote-host transfer, notifications, and frontend registration.
- There are no shared or backend modules because the integration has no
  cross-process RPC contracts or backend-side functionality.
- The root project assembles both local native variants and Linux distributions
  that the frontend can transfer to a remote execution host.

Run `:runIdeSplitMode` from the project root to start the split-mode IDE pair.
