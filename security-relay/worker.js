const MAX_BODY_BYTES = 16 * 1024;
const MAX_TEXT_LENGTH = 160;
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 5;

const inMemoryRateLimit = new Map();

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname !== "/report") {
      return new Response("Not Found", {
        status: 404,
        headers: securityHeaders(),
      });
    }

    if (request.method !== "POST") {
      return new Response("Method Not Allowed", {
        status: 405,
        headers: securityHeaders(),
      });
    }

    if (!env.DISCORD_WEBHOOK_URL) {
      return new Response("Server not configured", {
        status: 503,
        headers: securityHeaders(),
      });
    }

    if (!env.SUS_PLAYERS) {
      return new Response("SUS player storage is not configured", {
        status: 503,
        headers: securityHeaders(),
      });
    }

    const contentType = request.headers.get("Content-Type") ?? "";
    if (!contentType.toLowerCase().includes("application/json")) {
      return new Response("Unsupported Media Type", {
        status: 415,
        headers: securityHeaders(),
      });
    }

    const clientIp = request.headers.get("CF-Connecting-IP") ?? "unknown";
    if (!allowRequest(clientIp)) {
      return new Response("Rate limit exceeded", {
        status: 429,
        headers: {
          ...securityHeaders(),
          "Retry-After": "60",
        },
      });
    }

    const contentLength = Number(request.headers.get("Content-Length") ?? "0");
    if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) {
      return new Response("Payload too large", {
        status: 413,
        headers: securityHeaders(),
      });
    }

    let rawBody;
    try {
      rawBody = await request.text();
    } catch {
      return new Response("Invalid body", {
        status: 400,
        headers: securityHeaders(),
      });
    }

    if (new TextEncoder().encode(rawBody).byteLength > MAX_BODY_BYTES) {
      return new Response("Payload too large", {
        status: 413,
        headers: securityHeaders(),
      });
    }

    let data;
    try {
      data = JSON.parse(rawBody);
    } catch {
      return new Response("Invalid JSON", {
        status: 400,
        headers: securityHeaders(),
      });
    }

    const report = validateReport(data);
    if (!report.ok) {
      return new Response(report.error, {
        status: 400,
        headers: securityHeaders(),
      });
    }

    const now = new Date();
    const reportKey = `report/${encodeURIComponent(report.value.reportId)}`;
    try {
      const alreadyProcessed = await env.SUS_PLAYERS.get(reportKey);
      if (alreadyProcessed) {
        return new Response("Report already processed", {
          status: 200,
          headers: securityHeaders(),
        });
      }
    } catch (error) {
      console.error("Failed to check report deduplication key", error);
      return new Response("Could not check report status", {
        status: 503,
        headers: securityHeaders(),
      });
    }

    const susResult = await recordSusPlayer(env.SUS_PLAYERS, report.value, now);
    if (!susResult.ok) {
      return new Response("Could not save SUS player", {
        status: 503,
        headers: securityHeaders(),
      });
    }

    const detectionFields = report.value.detections.map((entry, index) => ({
      name: index === 0 ? "Detected Client" : `Detected Client ${index + 1}`,
      value:
        `**Name:** ${entry.modName}\n` +
        `**Mod ID:** ${entry.modId}\n` +
        `**Version:** ${entry.modVersion}`,
      inline: false,
    }));

    const detectionSummary = report.value.detections
      .map((entry) => `**${entry.method}:** ${entry.action}`)
      .join("\n");

    const embed = {
      title: "AcquiredUtils Security Report",
      description: "**Hacked-Client Detected**",
      color: 0xE74C3C,
      fields: [
        {
          name: "Player name",
          value: report.value.playerName,
          inline: false,
        },
        ...detectionFields,
        {
          name: "Minecraft",
          value:
            `**Version:** ${report.value.minecraftVersion}\n` +
            `**Loader:** ${report.value.loaderVersion}`,
          inline: true,
        },
        {
          name: "AcquiredUtils",
          value: `**Version:** ${report.value.acquiredUtilsVersion}`,
          inline: true,
        },
        {
          name: "Detection",
          value:
            `${detectionSummary}\n` +
            `**Status:** ${report.value.status}\n` +
            `**Previous reports:** ${susResult.value.count - 1}`,
          inline: false,
        },
      ],
      timestamp: now.toISOString(),
      footer: {
        text: "AcquiredUtils Security",
      },
    };

    let discordResponse;
    try {
      discordResponse = await fetch(env.DISCORD_WEBHOOK_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: "AcquiredUtils Security",
          embeds: [embed],
        }),
      });
    } catch {
      return new Response("Discord request failed", {
        status: 502,
        headers: securityHeaders(),
      });
    }

    if (!discordResponse.ok) {
      return new Response("Discord webhook failed", {
        status: 502,
        headers: securityHeaders(),
      });
    }

    try {
      await env.SUS_PLAYERS.put(reportKey, "processed", { expirationTtl: 86400 });
    } catch (error) {
      console.error("Failed to persist report deduplication key", error);
      // The Discord message was already delivered, so acknowledge the request
      // rather than forcing the client to resend and potentially duplicate it.
    }

    return new Response("Report sent", {
      status: 200,
      headers: securityHeaders(),
    });
  },
};

function validateReport(data) {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return { ok: false, error: "Invalid report object" };
  }

  const playerName = cleanText(data.playerName);
  const minecraftVersion = cleanText(data.minecraftVersion);
  const loaderVersion = cleanText(data.loaderVersion);
  const acquiredUtilsVersion = cleanText(data.acquiredUtilsVersion);
  const method = cleanText(data.method || data.detectionMethod);
  const status = cleanText(data.status || "SUS");
  const reportId = cleanText(data.reportId || `${Date.now()}-${playerName}`);

  if (!reportId || !playerName || !minecraftVersion || !loaderVersion || !acquiredUtilsVersion || !method) {
    return { ok: false, error: "Missing required report fields" };
  }

  if (status !== "SUS") {
    return { ok: false, error: "Invalid report status" };
  }

  // New format: detections[].
  // Backward compatibility: older clients may send modName/modId/modVersion at the top level.
  let rawDetections = Array.isArray(data.detections) ? data.detections : null;

  if (!rawDetections || rawDetections.length === 0) {
    const legacyName = cleanText(data.modName);
    const legacyId = cleanText(data.modId);
    const legacyVersion = cleanText(data.modVersion);
    const legacyMethod = cleanText(data.detectionMethod || data.method);
    const legacyAction = cleanText(data.detectionAction || data.action || "Detected; no files modified");

    if (legacyName && legacyId && legacyVersion && legacyMethod && legacyAction) {
      rawDetections = [{
        modName: legacyName,
        modId: legacyId,
        modVersion: legacyVersion,
        method: legacyMethod,
        action: legacyAction,
      }];
    }
  }

  if (!rawDetections || rawDetections.length === 0 || rawDetections.length > 8) {
    return { ok: false, error: "Invalid detection list" };
  }

  const detections = [];
  for (const detection of rawDetections) {
    if (!detection || typeof detection !== "object" || Array.isArray(detection)) {
      return { ok: false, error: "Invalid detection entry" };
    }

    const modName = cleanText(detection.modName);
    const modId = cleanText(detection.modId);
    const modVersion = cleanText(detection.modVersion);
    const detectionMethod = cleanText(detection.method || data.detectionMethod);
    const action = cleanText(detection.action || data.detectionAction || "Detected; no files modified");

    if (!modName || !modId || !modVersion || !detectionMethod || !action) {
      return { ok: false, error: "Incomplete detection entry" };
    }

    detections.push({
      modName,
      modId,
      modVersion,
      method: detectionMethod,
      action,
    });
  }

  return {
    ok: true,
    value: {
      reportId,
      playerName,
      minecraftVersion,
      loaderVersion,
      acquiredUtilsVersion,
      method,
      status,
      detections,
    },
  };
}

async function recordSusPlayer(namespace, report, now) {
  const key = `sus/${encodeURIComponent(report.playerName)}`;
  let existing = null;

  try {
    const raw = await namespace.get(key);
    if (raw) {
      try {
        existing = JSON.parse(raw);
      } catch {
        existing = null;
      }
    }

    const count = Number.isInteger(existing?.count) && existing.count >= 0
      ? existing.count + 1
      : 1;

    const record = {
      playerName: report.playerName,
      count,
      lastSeen: now.toISOString(),
      minecraftVersion: report.minecraftVersion,
      loaderVersion: report.loaderVersion,
      acquiredUtilsVersion: report.acquiredUtilsVersion,
      method: report.method,
      detections: report.detections,
    };

    await namespace.put(key, JSON.stringify(record));
    return { ok: true, value: { count } };
  } catch (error) {
    console.error("Failed to persist SUS player", error);
    return { ok: false };
  }
}

function cleanText(value) {
  if (typeof value !== "string") {
    return "";
  }

  const cleaned = value
    .replace(/[\u0000-\u001F\u007F]/g, " ")
    .replace(/@everyone|@here/gi, "@")
    .trim();

  if (cleaned.length === 0) {
    return "";
  }

  return cleaned.slice(0, MAX_TEXT_LENGTH);
}

function allowRequest(ip) {
  const now = Date.now();
  const existing = inMemoryRateLimit.get(ip);

  if (inMemoryRateLimit.size > 5000) {
    for (const [key, value] of inMemoryRateLimit) {
      if (now - value.startedAt >= RATE_LIMIT_WINDOW_MS) {
        inMemoryRateLimit.delete(key);
      }
    }
  }

  if (!existing || now - existing.startedAt >= RATE_LIMIT_WINDOW_MS) {
    inMemoryRateLimit.set(ip, { startedAt: now, count: 1 });
    return true;
  }

  if (existing.count >= RATE_LIMIT_MAX) {
    return false;
  }

  existing.count += 1;
  return true;
}

function securityHeaders() {
  return {
    "Content-Type": "text/plain; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
  };
}
