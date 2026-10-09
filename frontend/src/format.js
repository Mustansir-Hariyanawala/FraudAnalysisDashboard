export const fmtDateTime = (s) =>
  s ? new Date(/(Z|[+-]\d\d:?\d\d)$/.test(s) ? s : s + "Z")
        .toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })
    : "—";