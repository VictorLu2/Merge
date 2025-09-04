// 可在此覆蓋 API Base URL，或在瀏覽器 localStorage 設置 key: apiBaseUrl
// 例如：localStorage.setItem('apiBaseUrl', 'http://localhost:8080')
window.CMS_CONFIG = window.CMS_CONFIG || {
  // 將 VS Code 開啟的 CMS 指向 IntelliJ 運行的 Spring Boot
  API_BASE_URL: 'http://localhost:8080'
};

