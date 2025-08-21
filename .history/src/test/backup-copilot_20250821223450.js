// backup-copilot.js
const fs = require("fs");
const path = require("path");

// Thư mục log Copilot
const logDir = path.join(process.env.APPDATA, "Code", "User", "globalStorage", "github.copilot-chat");

// File đích
const outputFile = path.join(__dirname, "copilot-history.md");

// Hàm append nội dung
function backupFile(filePath) {
  const content = fs.readFileSync(filePath, "utf8");
  const log = `\n\n---\n📅 Backup lúc: ${new Date().toLocaleString()}\n\n${content}\n`;
  fs.appendFileSync(outputFile, log, "utf8");
  console.log(`[+] Đã backup: ${filePath}`);
}

// Theo dõi thư mục
fs.watch(logDir, (event, filename) => {
  if (filename) {
    const fullPath = path.join(logDir, filename);
    if (fs.existsSync(fullPath) && fs.statSync(fullPath).isFile()) {
      backupFile(fullPath);
    }
  }
});

console.log("🚀 Đang theo dõi log Copilot...");
console.log("📄 File backup:", outputFile);
