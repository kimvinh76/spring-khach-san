// backup-copilot.js
const fs = require("fs");
const path = require("path");

// 🔹 Thư mục log Copilot trên máy bạn
const logDir = "C:\\Users\\ASUS\\AppData\\Roaming\\Code\\User\\globalStorage\\github.copilot-chat";

// Hàm lấy tên file theo ngày
function getOutputFile() {
  const today = new Date().toISOString().split("T")[0]; // YYYY-MM-DD
  return path.join(__dirname, `copilot-history-${today}.md`);
}

// Hàm ghi log
function backupFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, "utf8");
    const outputFile = getOutputFile();

    // Nếu file chưa tồn tại thì tạo file + thêm tiêu đề ngày
    if (!fs.existsSync(outputFile)) {
      const header = `# Lịch sử ngày ${new Date().toLocaleDateString()}\n\n`;
      fs.writeFileSync(outputFile, header, "utf8");
    }

    const log = `\n\n---\n📅 Backup lúc: ${new Date().toLocaleString()}\n\n${content}\n`;
    fs.appendFileSync(outputFile, log, "utf8");

    console.log(`[+] Đã backup vào: ${outputFile}`);
  } catch (err) {
    console.error("❌ Lỗi khi backup:", err.message);
  }
}

// Theo dõi thư mục log
fs.watch(logDir, (event, filename) => {
  if (filename) {
    const fullPath = path.join(logDir, filename);
    if (fs.existsSync(fullPath) && fs.statSync(fullPath).isFile()) {
      backupFile(fullPath);
    }
  }
});

console.log("🚀 Đang theo dõi log Copilot...");
console.log("📂 Log sẽ lưu trong file theo ngày: copilot-history-YYYY-MM-DD.md");
