# MySQL Client Pro - 商用级专业数据库管理工具

一个功能强大的MySQL数据库客户端工具，使用JavaFX开发，提供现代化的用户界面和丰富的商用级功能。

## 🌟 核心特性

### 🔐 安全性
- **AES-256加密**：使用行业标准AES-256算法加密存储密码
- **安全连接**：支持SSL/TLS加密连接
- **连接池管理**：使用HikariCP高性能连接池

### 📊 数据管理
- **数据编辑器**：直接在表格中编辑数据（插入、更新、删除）
- **批量操作**：支持批量插入、批量删除
- **主键检测**：自动识别主键，安全执行更新和删除

### 📁 导入导出
- **CSV导入导出**：支持大文件CSV处理
- **Excel支持**：导入导出Excel文件（.xlsx）
- **SQL脚本**：导出为标准SQL INSERT语句
- **表结构导出**：导出完整的CREATE TABLE语句

### 🗄️ 数据库管理
- **表结构设计器**：可视化创建和修改表结构
- **索引管理**：创建和管理主键、索引
- **数据库浏览器**：树形结构展示数据库、表、列信息

### 💾 备份恢复
- **完整备份**：备份整个数据库（结构+数据）
- **一键恢复**：从备份文件快速恢复数据库
- **进度显示**：实时显示备份/恢复进度

### 📝 SQL编辑器
- **语法高亮**：基于RSyntaxTextArea的SQL语法高亮
- **多标签支持**：同时编辑多个SQL查询
- **执行计划**：查看EXPLAIN执行计划
- **性能分析**：查询性能分析和优化建议

### 📜 历史记录
- **查询历史**：自动保存最近500条查询历史
- **SQL收藏夹**：保存常用查询到收藏夹
- **历史搜索**：快速搜索历史查询

### 🎨 界面主题
- **浅色主题**：经典浅色界面
- **深色主题**：护眼深色界面
- **一键切换**：快速切换主题

## 技术栈

- **Java 17** + **JavaFX 21**
- **MySQL Connector/J 8.2.0**
- **HikariCP 5.1.0** - 高性能连接池
- **RSyntaxTextArea 3.3.4** - SQL语法高亮
- **Apache POI 5.2.5** - Excel处理
- **Apache Commons CSV 1.10.0** - CSV处理
- **Jasypt 1.9.3** - AES-256加密
- **Jackson 2.16.0** - JSON处理
- **ControlsFX 11.1.2** - 高级UI控件

## 快速开始

### 构建项目

```bash
# 编译
mvn clean compile

# 打包
mvn clean package
```

### 运行应用

```bash
# 方式1：使用Maven
mvn javafx:run

# 方式2：直接运行JAR
java -jar target/mysql-client-1.0-SNAPSHOT.jar
```

## 使用指南

### 1. 创建数据库连接

1. 点击 **File → New Connection**
2. 填写连接信息（密码将使用AES-256加密）
3. 点击 **Connect** 测试连接

### 2. 编辑表数据

1. 双击表名打开数据视图
2. 直接在单元格中编辑
3. 点击 **Insert Row** 添加新行
4. 点击 **Delete Row** 删除选中行

### 3. 导入导出数据

- 导出：**Tools → Export** 选择格式（CSV/Excel/SQL）
- 导入：**Tools → Import** 选择文件

### 4. 备份数据库

1. 选择数据库
2. 点击 **Database → Backup Database**
3. 选择保存路径

### 5. 切换主题

- 点击 **View → Toggle Theme** 快速切换
- 或选择 **View → Light/Dark Theme**

## 配置文件

所有配置保存在 `~/.mysqlclient/`：

- **connections.json** - 连接配置（密码已加密）
- **query_history.json** - 查询历史
- **saved_queries.json** - 收藏的查询
- **theme.txt** - 主题偏好

## 键盘快捷键

- **Ctrl+T** - 新建查询标签
- **F5** - 执行查询
- **Ctrl+E** - 查看执行计划

## 版本历史

### v2.0 (商用级版本)
- ✨ AES-256密码加密
- ✨ 数据编辑功能
- ✨ CSV/Excel/SQL导入导出
- ✨ 数据库备份和恢复
- ✨ 表结构设计器
- ✨ SQL执行计划分析
- ✨ 查询历史和收藏
- ✨ 深色/浅色主题

### v1.0 (MVP版本)
- 基础连接管理
- SQL编辑器
- 数据库浏览
- 查询执行

---

**MySQL Client Pro** - 专业的MySQL管理工具
