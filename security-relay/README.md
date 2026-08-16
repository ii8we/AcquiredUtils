# AcquiredUtils Security Relay

This Worker forwards AcquiredUtils security reports to the private Discord `#reports` webhook and keeps a persistent SUS-player record in Cloudflare Workers KV.

## Secrets / Bindings

Create this Worker secret:

- `DISCORD_WEBHOOK_URL` — your private Discord webhook URL.

Create a Workers KV namespace and bind it to the Worker with the binding name:

- `SUS_PLAYERS`

Cloudflare documents KV bindings under **Workers & Pages → your Worker → Settings → Bindings → Add → KV Namespace**. The binding is exposed to Worker code as `env.SUS_PLAYERS`. citehttps://developers.cloudflare.com/kv/concepts/kv-namespaces/

## Endpoint

`POST /report`

The client sends JSON containing:

- `playerName`
- `minecraftVersion`
- `loaderVersion`
- `acquiredUtilsVersion`
- `method`
- `status` (`SUS`)
- `detections[]` with `modName`, `modId`, `modVersion`, `method`, and `action`

## SUS storage

The Worker stores the latest record for each player under:

`SUS_PLAYERS` → `sus/<encoded-player-name>`

Each record includes the player's latest detection data, last-seen time, and detection count.

## Production notes

The Worker has payload validation, length limits, mention neutralization, and a best-effort per-IP in-memory rate limiter. Cloudflare KV is used for persistent SUS records; KV bindings are available to Workers through the `env` object. citehttps://developers.cloudflare.com/kv/concepts/kv-bindings/

For a public production deployment, also configure Cloudflare dashboard-side rate limiting/WAF for `/report`. The client is not treated as a trusted secret-bearing environment, so the relay should be treated as a best-effort reporting endpoint rather than cryptographic proof of cheating.


### Deployment note
Deploy `worker.js` after changing it. The worker expects the client payload's `detections` array and renders the first detection as `Detected Client`, with its real Name, Mod ID, and Version.


The relay accepts both the current `detections[]` payload and the older single-detection fields (`modName`, `modId`, `modVersion`) for backward compatibility.
